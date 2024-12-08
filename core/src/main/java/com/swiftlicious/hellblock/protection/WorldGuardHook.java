package com.swiftlicious.hellblock.protection;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

import net.kyori.adventure.text.Component;

public class WorldGuardHook implements IslandProtection {

	private static WorldGuardPlatform worldGuardPlatform = null;

	public static boolean isWorking() {
		try {
			worldGuardPlatform = WorldGuard.getInstance().getPlatform();
			return worldGuardPlatform != null;
		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"WorldGuard threw an error during initializing, make sure it's updated and API compatible(Must be 7.0 or higher)",
					t);
			return false;
		}
	}

	@Override
	public CompletableFuture<Void> protectHellblock(@NotNull World world, @NotNull UserData owner) {
		CompletableFuture<Void> protection = new CompletableFuture<>();
		if (worldGuardPlatform == null)
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
		try {
			RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
			if (regionManager == null)
				throw new NullPointerException(String
						.format("Could not get the WorldGuard region manager for the world: %s", world.getName()));
			DefaultDomain owners = new DefaultDomain();
			ProtectedRegion region = new ProtectedCuboidRegion(
					String.format("%s_%s", owner.getUUID().toString(), owner.getHellblockData().getID()),
					getProtectionVectorLeft(world, owner.getHellblockData().getHellblockLocation()),
					getProtectionVectorRight(world, owner.getHellblockData().getHellblockLocation()));
			region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
			owners.addPlayer(owner.getUUID());
			region.setOwners(owners);
			region.setPriority(100);
			updateHellblockMessages(world, owner.getUUID());
			owner.getHellblockData()
					.setBoundingBox(new BoundingBox((double) region.getMinimumPoint().x(),
							(double) region.getMinimumPoint().y(), (double) region.getMinimumPoint().z(),
							(double) region.getMaximumPoint().x(), (double) region.getMaximumPoint().y(),
							(double) region.getMaximumPoint().z()));
			ApplicableRegionSet set = regionManager
					.getApplicableRegions(BlockVector3.at(owner.getHellblockData().getHellblockLocation().getX(),
							owner.getHellblockData().getHellblockLocation().getY(),
							owner.getHellblockData().getHellblockLocation().getZ()));
			if (set.size() > 0) {
				Iterator<ProtectedRegion> regions = set.iterator();

				while (regions.hasNext()) {
					ProtectedRegion regionCheck = regions.next();
					if (!regionCheck.getId().equalsIgnoreCase(ProtectedRegion.GLOBAL_REGION)) {
						regionManager.removeRegion(regionCheck.getId());
					}
				}
			}

			regionManager.addRegion(region);
			regionManager.save();
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
		if (worldGuardPlatform == null)
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
		try {
			RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
			if (regionManager == null)
				throw new NullPointerException(String
						.format("Could not get the WorldGuard region manager for the world: %s", world.getName()));
			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						HellblockPlugin.getInstance().getProtectionManager().clearHellblockEntities(world,
								offlineUser.getHellblockData().getBoundingBox());
						String regionName = String.format("%s_%s", id.toString(),
								offlineUser.getHellblockData().getID());
						ProtectedRegion region = regionManager.getRegion(regionName);
						if (region != null) {
							regionManager.removeRegion(regionName, RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
							try {
								regionManager.save();
							} catch (Exception ex) {
								HellblockPlugin.getInstance().getPluginLogger().severe(String.format(
										"Unable to unprotect %s's hellblock!", Bukkit.getPlayer(id).getName()), ex);
								unprotection.completeExceptionally(ex);
							}
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
		if (worldGuardPlatform == null)
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		RegionContainer regionContainer = worldGuardPlatform.getRegionContainer();
		try {
			RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
			if (regionManager == null)
				throw new NullPointerException(String
						.format("Could not get the WorldGuard region manager for the world: %s", world.getName()));

			ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
			if (region == null)
				throw new NullPointerException(
						String.format("Could not get the WorldGuard region for the player: %s", owner.getUUID()));

			if (!region.getOwners().getUniqueIds().contains(transferee.getUUID()))
				region.getOwners().getUniqueIds().add(transferee.getUUID());
			if (region.getOwners().getUniqueIds().contains(owner.getUUID()))
				region.getOwners().getUniqueIds().remove(owner.getUUID());
			if (!region.getMembers().getUniqueIds().contains(owner.getUUID()))
				region.getMembers().getUniqueIds().add(owner.getUUID());
			if (region.getMembers().getUniqueIds().contains(transferee.getUUID()))
				region.getMembers().getUniqueIds().remove(transferee.getUUID());

			ProtectedRegion renamedRegion = new ProtectedCuboidRegion(
					String.format("%s_%s", transferee.getUUID().toString(), transferee.getHellblockData().getID()),
					getProtectionVectorLeft(world, transferee.getHellblockData().getHellblockLocation()),
					getProtectionVectorRight(world, transferee.getHellblockData().getHellblockLocation()));

			renamedRegion.setOwners(region.getOwners());
			renamedRegion.setMembers(region.getMembers());
			renamedRegion.setFlags(region.getFlags());
			renamedRegion.setPriority(region.getPriority());
			renamedRegion.setParent(region.getParent());
			regionManager.addRegion(renamedRegion);
			regionManager.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
			regionManager.save();

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
			ProtectedRegion region = getRegion(world, id, onlineUser.get().getHellblockData().getID());
			if (region == null)
				throw new NullPointerException(
						String.format("Could not get the WorldGuard region for the player: %s", id));
			StringFlag greetFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
			if (HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					region.setFlag(greetFlag,
							HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
									AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_ENTRY_MESSAGE
											.arguments(Component.text(name)).build().key())));
				} else {
					region.setFlag(greetFlag,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
											MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key())));
				}
			} else {
				region.setFlag(greetFlag, null);
			}
			StringFlag farewellFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE);
			if (HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()) {
				if (!onlineUser.get().getHellblockData().isAbandoned()) {
					region.setFlag(farewellFlag,
							HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
									AdventureHelper.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
											.arguments(Component.text(name)).build().key())));
				} else {
					region.setFlag(farewellFlag,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
											MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key())));
				}
			} else {
				region.setFlag(farewellFlag, null);
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
						ProtectedRegion region = getRegion(world, id, offlineUser.getHellblockData().getID());
						if (region == null)
							throw new NullPointerException(
									String.format("Could not get the WorldGuard region for the player: %s", id));
						StringFlag greetFlag = convertToWorldGuardStringFlag(HellblockFlag.FlagType.GREET_MESSAGE);
						if (HellblockPlugin.getInstance().getConfigManager().entryMessageEnabled()) {
							if (!offlineUser.getHellblockData().isAbandoned()) {
								region.setFlag(greetFlag,
										HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(AdventureHelper
														.legacyToMiniMessage(MessageConstants.HELLBLOCK_ENTRY_MESSAGE
																.arguments(Component.text(name)).build().key())));
							} else {
								region.setFlag(greetFlag, HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
												MessageConstants.HELLBLOCK_ABANDONED_ENTRY_MESSAGE.build().key())));
							}
						} else {
							region.setFlag(greetFlag, null);
						}
						StringFlag farewellFlag = convertToWorldGuardStringFlag(
								HellblockFlag.FlagType.FAREWELL_MESSAGE);
						if (HellblockPlugin.getInstance().getConfigManager().farewellMessageEnabled()) {
							if (!offlineUser.getHellblockData().isAbandoned()) {
								region.setFlag(farewellFlag,
										HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(AdventureHelper
														.legacyToMiniMessage(MessageConstants.HELLBLOCK_FAREWELL_MESSAGE
																.arguments(Component.text(name)).build().key())));
							} else {
								region.setFlag(farewellFlag, HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(AdventureHelper.legacyToMiniMessage(
												MessageConstants.HELLBLOCK_ABANDONED_FAREWELL_MESSAGE.build().key())));
							}
						} else {
							region.setFlag(farewellFlag, null);
						}
					});
		}
	}

	@Override
	public void lockHellblock(@NotNull World world, @NotNull UserData owner) {
		ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
		if (region == null) {
			throw new NullPointerException("Region returned null.");
		}
		region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY),
				(!owner.getHellblockData().isLocked() ? null : StateFlag.State.DENY));
	}

	@Override
	public void abandonIsland(@NotNull World world, @NotNull UUID id) {
		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					if (offlineUser.getHellblockData().isAbandoned()) {
						ProtectedRegion region = getRegion(world, id, offlineUser.getHellblockData().getID());
						if (region == null)
							throw new NullPointerException(
									String.format("Could not get the WorldGuard region for the player: %s", id));
						region.getOwners().clear();
						region.getMembers().clear();
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.PVP), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.BUILD), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY), StateFlag.State.DENY);
						region.setFlag(convertToWorldGuardFlag(HellblockFlag.FlagType.MOB_SPAWNING),
								StateFlag.State.DENY);
					}
				});
	}

	@Override
	public void changeHellblockFlag(@NotNull World world, @NotNull UserData owner, @NotNull HellblockFlag flag) {
		ProtectedRegion region = getRegion(world, owner.getUUID(), owner.getHellblockData().getID());
		if (region == null) {
			throw new NullPointerException("Region returned null.");
		}
		if (convertToWorldGuardFlag(flag.getFlag()) != null)
			region.setFlag(convertToWorldGuardFlag(flag.getFlag()),
					(flag.getStatus() == AccessType.ALLOW ? null : StateFlag.State.DENY));
	}

	@Override
	public CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull World world, @NotNull UUID ownerID,
			@NotNull UUID id) {
		CompletableFuture<Set<UUID>> members = new CompletableFuture<>();
		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(ownerID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					ProtectedRegion region = getRegion(world, ownerID, offlineUser.getHellblockData().getID());
					if (region == null)
						throw new NullPointerException(
								String.format("Could not get the WorldGuard region for the player: %s", id));
					members.complete(region.getMembers().getUniqueIds());
				});
		return members;
	}

	@Override
	public void addMemberToHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(ownerID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					ProtectedRegion region = getRegion(world, ownerID, offlineUser.getHellblockData().getID());
					if (region == null)
						throw new NullPointerException(
								String.format("Could not get the WorldGuard region for the player: %s", id));
					if (!region.getMembers().getUniqueIds().contains(id))
						region.getMembers().getUniqueIds().add(id);
				});
	}

	@Override
	public void removeMemberFromHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(ownerID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
					ProtectedRegion region = getRegion(world, ownerID, offlineUser.getHellblockData().getID());
					if (region == null)
						throw new NullPointerException(
								String.format("Could not get the WorldGuard region for the player: %s", id));
					if (region.getMembers().getUniqueIds().contains(id))
						region.getMembers().getUniqueIds().remove(id);
				});
	}

	private @NotNull BlockVector3 getProtectionVectorLeft(@NotNull World world, @NotNull Location loc) {
		return BlockVector3.at(
				loc.getX() + (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2),
				world.getMaxHeight(),
				loc.getZ() + (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2));
	}

	private @NotNull BlockVector3 getProtectionVectorRight(@NotNull World world, @NotNull Location loc) {
		return BlockVector3.at(
				loc.getX() - (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2),
				world.getMinHeight(),
				loc.getZ() - (double) (HellblockPlugin.getInstance().getConfigManager().protectionRange() / 2));
	}

	private @Nullable ProtectedRegion getRegion(@NotNull World world, @NotNull UUID playerUUID, int hellblockID) {
		if (worldGuardPlatform == null)
			throw new NullPointerException("Could not retrieve WorldGuard platform.");
		RegionManager regionManager = worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null)
			throw new NullPointerException(
					String.format("Could not get the WorldGuard region manager for the world: %s", world.getName()));
		ProtectedRegion region = regionManager.getRegion(String.format("%s_%s", playerUUID.toString(), hellblockID));
		if (region == null)
			return null;

		return region;
	}

	private @Nullable StateFlag convertToWorldGuardFlag(@NotNull FlagType flag) {
		StateFlag wgFlag = null;
		switch (flag) {
		case BLOCK_BREAK:
			wgFlag = Flags.BLOCK_BREAK;
			break;
		case BLOCK_PLACE:
			wgFlag = Flags.BLOCK_PLACE;
			break;
		case BUILD:
			wgFlag = Flags.BUILD;
			break;
		case CHEST_ACCESS:
			wgFlag = Flags.CHEST_ACCESS;
			break;
		case CHORUS_TELEPORT:
			wgFlag = Flags.CHORUS_TELEPORT;
			break;
		case DAMAGE_ANIMALS:
			wgFlag = Flags.DAMAGE_ANIMALS;
			break;
		case DESTROY_VEHICLE:
			wgFlag = Flags.DESTROY_VEHICLE;
			break;
		case ENDERPEARL:
			wgFlag = Flags.ENDERPEARL;
			break;
		case ENDER_BUILD:
			wgFlag = Flags.ENDER_BUILD;
			break;
		case ENTRY:
			wgFlag = Flags.ENTRY;
			break;
		case FALL_DAMAGE:
			wgFlag = Flags.FALL_DAMAGE;
			break;
		case FIREWORK_DAMAGE:
			wgFlag = Flags.FIREWORK_DAMAGE;
			break;
		case GHAST_FIREBALL:
			wgFlag = Flags.GHAST_FIREBALL;
			break;
		case HEALTH_REGEN:
			wgFlag = Flags.HEALTH_REGEN;
			break;
		case HUNGER_DRAIN:
			wgFlag = Flags.HUNGER_DRAIN;
			break;
		case INTERACT:
			wgFlag = Flags.INTERACT;
			break;
		case INVINCIBILITY:
			wgFlag = Flags.INVINCIBILITY;
			break;
		case ITEM_FRAME_ROTATE:
			wgFlag = Flags.ITEM_FRAME_ROTATE;
			break;
		case LIGHTER:
			wgFlag = Flags.LIGHTER;
			break;
		case MOB_DAMAGE:
			wgFlag = Flags.MOB_DAMAGE;
			break;
		case MOB_SPAWNING:
			wgFlag = Flags.MOB_SPAWNING;
			break;
		case PLACE_VEHICLE:
			wgFlag = Flags.PLACE_VEHICLE;
			break;
		case POTION_SPLASH:
			wgFlag = Flags.POTION_SPLASH;
			break;
		case PVP:
			wgFlag = Flags.PVP;
			break;
		case RESPAWN_ANCHORS:
			wgFlag = Flags.RESPAWN_ANCHORS;
			break;
		case RIDE:
			wgFlag = Flags.RIDE;
			break;
		case SLEEP:
			wgFlag = Flags.SLEEP;
			break;
		case SNOWMAN_TRAILS:
			wgFlag = Flags.SNOWMAN_TRAILS;
			break;
		case TNT:
			wgFlag = Flags.TNT;
			break;
		case TRAMPLE_BLOCKS:
			wgFlag = Flags.TRAMPLE_BLOCKS;
			break;
		case USE:
			wgFlag = Flags.USE;
			break;
		case USE_ANVIL:
			wgFlag = Flags.USE_ANVIL;
			break;
		case USE_DRIPLEAF:
			wgFlag = Flags.USE_DRIPLEAF;
			break;
		case WIND_CHARGE_BURST:
			wgFlag = Flags.WIND_CHARGE_BURST;
			break;
		default:
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}

	@SuppressWarnings("deprecation")
	private @Nullable StringFlag convertToWorldGuardStringFlag(@NotNull FlagType flag) {
		StringFlag wgFlag = null;
		switch (flag) {
		case GREET_MESSAGE:
			wgFlag = Flags.GREET_MESSAGE;
			break;
		case FAREWELL_MESSAGE:
			wgFlag = Flags.FAREWELL_MESSAGE;
			break;
		default:
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}
}