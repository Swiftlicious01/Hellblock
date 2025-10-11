package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class DefaultProtection implements IslandProtection {

	protected final HellblockPlugin instance;

	public DefaultProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull World world, @NotNull UserData owner) {
		final CompletableFuture<Void> protection = new CompletableFuture<>();
		try {
			Location l1 = getProtectionVectorUpperCorner(owner).toLocation(world);
			Location l2 = getProtectionVectorLowerCorner(owner).toLocation(world);

			double minX = Math.min(l1.getX(), l2.getX());
			double minY = Math.min(l1.getY(), l2.getY());
			double minZ = Math.min(l1.getZ(), l2.getZ());
			double maxX = Math.max(l1.getX(), l2.getX());
			double maxY = Math.max(l1.getY(), l2.getY());
			double maxZ = Math.max(l1.getZ(), l2.getZ());

			updateHellblockMessages(world, owner.getUUID());

			BoundingBox boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
			owner.getHellblockData().setBoundingBox(boundingBox);

			instance.getProtectionManager().changeLockStatus(world, owner.getUUID());
			protection.complete(null);
		} catch (Exception ex) {
			instance.getPluginLogger().severe("Unable to protect %s's hellblock!".formatted(owner.getName()), ex);
			protection.completeExceptionally(ex);
		}
		return protection;
	}

	@Override
	public CompletableFuture<Void> unprotectHellblock(@NotNull World world, @NotNull UUID id) {
		final CompletableFuture<Void> unprotection = new CompletableFuture<>();
		try {
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						try {
							if (result.isEmpty()) {
								unprotection.complete(null);
								return;
							}
							final UserData offlineUser = result.get();
							instance.getProtectionManager().clearHellblockEntities(world,
									offlineUser.getHellblockData().getBoundingBox());

							unprotection.complete(null);
						} catch (Exception ex) {
							instance.getPluginLogger()
									.severe("Error while unprotecting hellblock for UUID: %s".formatted(id), ex);
							unprotection.completeExceptionally(ex);
						}
					});
		} catch (Exception ex) {
			instance.getPluginLogger().severe("Unable to unprotect hellblock for UUID: %s".formatted(id), ex);
			unprotection.completeExceptionally(ex);
		}
		return unprotection;
	}

	@Override
	public CompletableFuture<Void> reprotectHellblock(@NotNull World world, @NotNull UserData owner,
			@NotNull UserData transferee) {
		final CompletableFuture<Void> reprotection = new CompletableFuture<>();
		try {
			Location l1 = getProtectionVectorUpperCorner(owner).toLocation(world);
			Location l2 = getProtectionVectorLowerCorner(owner).toLocation(world);

			double minX = Math.min(l1.getX(), l2.getX());
			double minY = Math.min(l1.getY(), l2.getY());
			double minZ = Math.min(l1.getZ(), l2.getZ());
			double maxX = Math.max(l1.getX(), l2.getX());
			double maxY = Math.max(l1.getY(), l2.getY());
			double maxZ = Math.max(l1.getZ(), l2.getZ());

			BoundingBox boundingBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
			transferee.getHellblockData().setBoundingBox(boundingBox);

			updateHellblockMessages(world, transferee.getUUID());
			instance.getProtectionManager().changeLockStatus(world, transferee.getUUID());
			reprotection.complete(null);
		} catch (Exception ex) {
			instance.getPluginLogger().severe("Unable to reprotect %s's hellblock!".formatted(transferee.getName()),
					ex);
			reprotection.completeExceptionally(ex);
		}
		return reprotection;
	}

	@Override
	public void updateHellblockMessages(@NotNull World world, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					try {
						final UserData user = result.orElse(null);

						if (user == null) {
							instance.getPluginLogger().warn(
									"Failed to retrieve player's username to update hellblock entry/farewell messages.");
							return;
						}

						updateMessagesForUser(user, id, user.getName());
					} catch (Exception ex) {
						instance.getPluginLogger()
								.severe("Error updating hellblock messages for UUID: %s".formatted(id), ex);
					}
				});
	}

	private void updateMessagesForUser(UserData user, UUID id, String name) {
		final boolean abandoned = user.getHellblockData().isAbandoned();

		// Greeting flag
		createAndSetMessageFlag(HellblockFlag.FlagType.GREET_MESSAGE, instance.getConfigManager().entryMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_ENTRY_MESSAGE.arguments(Component.text(name)).build().key());

		// Farewell flag
		createAndSetMessageFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE,
				instance.getConfigManager().farewellMessageEnabled(),
				abandoned ? MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key()
						: MessageConstants.HELLBLOCK_FAREWELL_MESSAGE.arguments(Component.text(name)).build().key());
	}

	private void createAndSetMessageFlag(HellblockFlag.FlagType type, boolean enabled, String messageKey) {
		final HellblockFlag flag = new HellblockFlag(type,
				enabled ? HellblockFlag.AccessType.ALLOW : HellblockFlag.AccessType.DENY);
		flag.getFlag().setData(enabled ? instance.getTranslationManager()
				.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(messageKey)) : null);
	}

	@Override
	public void abandonIsland(@NotNull World world, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().isAbandoned()) {
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.ALLOW));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.ALLOW));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.DENY));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.DENY));
					}
				});
	}

	@Override
	public void restoreFlags(@NotNull World world, @NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						return;
					}
					final UserData offlineUser = result.get();
					if (!offlineUser.getHellblockData().isAbandoned()) {
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.PVP, HellblockFlag.AccessType.DENY));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.BUILD, HellblockFlag.AccessType.DENY));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.ENTRY, HellblockFlag.AccessType.ALLOW));
						offlineUser.getHellblockData().setProtectionValue(
								new HellblockFlag(HellblockFlag.FlagType.MOB_SPAWNING, HellblockFlag.AccessType.ALLOW));
					}
				});
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull World world, @NotNull UUID ownerID) {
		final CompletableFuture<Set<UUID>> members = new CompletableFuture<>();
		instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						members.complete(Set.of());
						return;
					}
					members.complete(result.get().getHellblockData().getIslandMembers());
				});
		return members;
	}

	public @NotNull Vector getProtectionVectorUpperCorner(@NotNull UserData owner) {
		final BoundingBox bounds = owner.getHellblockData().getBoundingBox();
		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}
		return new Vector(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
	}

	public @NotNull Vector getProtectionVectorLowerCorner(@NotNull UserData owner) {
		final BoundingBox bounds = owner.getHellblockData().getBoundingBox();
		if (bounds == null) {
			throw new IllegalStateException("Bounding box not set. Did you call expandBoundingBox()?");
		}
		return new Vector(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
	}
}