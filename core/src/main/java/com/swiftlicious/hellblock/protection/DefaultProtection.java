package com.swiftlicious.hellblock.protection;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class DefaultProtection implements IslandProtection {

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull World world, @NotNull UserData owner) {
		CompletableFuture<Void> protection = new CompletableFuture<>();
		try {
			HellblockCuboid cuboid = new HellblockCuboid(
					String.format("%s_%s", owner.getUUID().toString(), owner.getHellblockData().getID()),
					getProtectionVectorLeft(world, owner.getHellblockData().getHellblockLocation()).toLocation(world),
					getProtectionVectorRight(world, owner.getHellblockData().getHellblockLocation()).toLocation(world));
			updateHellblockMessages(world, owner.getUUID());
			owner.getHellblockData()
					.setBoundingBox(new BoundingBox((double) cuboid.getLowerX(), (double) cuboid.getLowerY(),
							(double) cuboid.getLowerZ(), (double) cuboid.getUpperX(), (double) cuboid.getUpperY(),
							(double) cuboid.getUpperZ()));
			HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().putIfAbsent(owner.getUUID(),
					cuboid);
			HellblockPlugin.getInstance().getProtectionManager().changeLockStatus(world, owner.getUUID());
			protection.complete(null);
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe(String.format("Unable to protect %s's hellblock!", owner.getName()), ex);
			protection.completeExceptionally(ex);
		}
		return protection;
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull World world, @NotNull UUID id) {
		CompletableFuture<Void> unprotection = new CompletableFuture<>();
		try {
			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						HellblockPlugin.getInstance().getProtectionManager().clearHellblockEntities(world,
								offlineUser.getHellblockData().getBoundingBox());
						HellblockCuboid cuboid = getHellblock(id);
						if (cuboid != null) {
							HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().remove(id,
									cuboid);
							unprotection.complete(null);
						}
					});
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe(String.format("Unable to unprotect %s's hellblock!", Bukkit.getPlayer(id).getName()), ex);
			unprotection.completeExceptionally(ex);
		}
		return unprotection;
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull World world, @NotNull UserData owner,
			@NotNull UserData transferee) {
		CompletableFuture<Void> reprotection = new CompletableFuture<>();
		try {
			HellblockCuboid cuboid = getHellblock(owner.getUUID());
			if (cuboid == null)
				throw new NullPointerException(
						String.format("Could not get the Hellblock cuboid for the player: %s", owner.getUUID()));
			HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().remove(owner.getUUID(), cuboid);

			HellblockCuboid renamedCuboid = new HellblockCuboid(
					String.format("%s_%s", transferee.getUUID().toString(), transferee.getHellblockData().getID()),
					getProtectionVectorLeft(world, transferee.getHellblockData().getHellblockLocation())
							.toLocation(world),
					getProtectionVectorRight(world, transferee.getHellblockData().getHellblockLocation())
							.toLocation(world));
			HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().putIfAbsent(transferee.getUUID(),
					renamedCuboid);
			updateHellblockMessages(world, transferee.getUUID());
			HellblockPlugin.getInstance().getProtectionManager().changeLockStatus(world, transferee.getUUID());
			reprotection.complete(null);
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe(String.format("Unable to reprotect %s's hellblock!", transferee.getPlayer().getName()), ex);
			reprotection.completeExceptionally(ex);
		}
		return reprotection;
	}

	@Override
	public void updateHellblockMessages(@NotNull World world, @NotNull UUID id) {
		Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager().getOnlineUser(id);
		if (!onlineUser.isEmpty()) {
			String name = onlineUser.get().getName();
			if (name == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.warn("Failed to retrieve player's username to update hellblock entry and farewell messages.");
				return;
			}
			HellblockCuboid cuboid = getHellblock(id);
			if (cuboid == null)
				throw new NullPointerException(
						String.format("Could not get the Hellblock cuboid for the player: %s", id));
			HellblockFlag greetFlag = new HellblockFlag(HellblockFlag.FlagType.GREET_MESSAGE,
					HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()
							? HellblockFlag.AccessType.ALLOW
							: HellblockFlag.AccessType.DENY);
			if (HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					greetFlag.getFlag()
							.setData(HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
									AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_ENTRY_MESSAGE
											.arguments(Component.text(name)).build().key())));
				} else {
					greetFlag.getFlag()
							.setData(HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
											MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key())));
				}
			} else {
				greetFlag.getFlag().setData(null);
			}
			HellblockFlag farewellFlag = new HellblockFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE,
					HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()
							? HellblockFlag.AccessType.ALLOW
							: HellblockFlag.AccessType.DENY);
			if (HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					farewellFlag.getFlag()
							.setData(HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
									AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
											.arguments(Component.text(name)).build().key())));
				} else {
					farewellFlag.getFlag()
							.setData(HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
											MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key())));
				}
			} else {
				farewellFlag.getFlag().setData(null);
			}
		} else {
			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						String name = offlineUser.getName();
						if (name == null) {
							HellblockPlugin.getInstance().getPluginLogger().warn(
									"Failed to retrieve player's username to update hellblock entry and farewell messages.");
							return;
						}
						HellblockCuboid cuboid = getHellblock(id);
						if (cuboid == null)
							throw new NullPointerException(
									String.format("Could not get the Hellblock cuboid for the player: %s", id));
						HellblockFlag greetFlag = new HellblockFlag(HellblockFlag.FlagType.GREET_MESSAGE,
								HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()
										? HellblockFlag.AccessType.ALLOW
										: HellblockFlag.AccessType.DENY);
						if (HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()) {
							if (!offlineUser.getHellblockData().isAbandoned()) {
								greetFlag.getFlag()
										.setData(HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(AdventureHelper
														.legacyToMiniMessage(MessageConstants.HELLBLOCK_ENTRY_MESSAGE
																.arguments(Component.text(name)).build().key())));
							} else {
								greetFlag.getFlag().setData(HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
												MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key())));
							}
						} else {
							greetFlag.getFlag().setData(null);
						}
						HellblockFlag farewellFlag = new HellblockFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE,
								HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()
										? HellblockFlag.AccessType.ALLOW
										: HellblockFlag.AccessType.DENY);
						if (HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()) {
							if (!offlineUser.getHellblockData().isAbandoned()) {
								farewellFlag.getFlag()
										.setData(HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(AdventureHelper
														.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
																.arguments(Component.text(name)).build().key())));
							} else {
								farewellFlag.getFlag().setData(HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
												MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key())));
							}
						} else {
							farewellFlag.getFlag().setData(null);
						}
					});
		}
	}

	@Override
	public void lockHellblock(@NotNull World world, @NotNull UserData owner) {
		// TODO: Auto-generated method stub
	}

	@Override
	public void abandonIsland(@NotNull World world, @NotNull UUID id) {
		// TODO: Auto-generated method stub
	}

	@Override
	public void changeHellblockFlag(@NotNull World world, @NotNull UserData owner, @NotNull HellblockFlag flag) {
		// TODO: Auto-generated method stub
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull World world, @NotNull UUID ownerID,
			@NotNull UUID id) {
		CompletableFuture<Set<UUID>> members = new CompletableFuture<>();
		return members;
	}

	@Override
	public void addMemberToHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		// TODO: Auto-generated method stub
	}

	@Override
	public void removeMemberFromHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		// TODO: Auto-generated method stub
	}

	private @Nullable HellblockCuboid getHellblock(@NotNull UUID playerUUID) {
		if (!HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().containsKey(playerUUID))
			return null;

		return HellblockPlugin.getInstance().getProtectionManager().getCachedHellblocks().get(playerUUID);
	}

	private @NotNull Vector getProtectionVectorLeft(@NotNull World world, @NotNull Location loc) {
		return new Vector(
				loc.getX() + (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2),
				world.getMaxHeight(),
				loc.getZ() + (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2));
	}

	private @NotNull Vector getProtectionVectorRight(@NotNull World world, @NotNull Location loc) {
		return new Vector(
				loc.getX() - (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2),
				world.getMinHeight(),
				loc.getZ() - (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2));
	}
}