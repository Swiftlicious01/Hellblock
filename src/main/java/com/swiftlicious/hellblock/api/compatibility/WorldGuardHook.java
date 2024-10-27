package com.swiftlicious.hellblock.api.compatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class WorldGuardHook {

	private final HellblockPlugin instance;
	@Getter
	@Setter
	private WorldGuardPlatform worldGuardPlatform;

	public WorldGuardHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void protectHellblock(Player player) {
		try {
			if (this.worldGuardPlatform == null) {
				LogUtils.severe("Could not retrieve WorldGuard platform.");
				return;
			}
			RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
						instance.getHellblockHandler().getWorldName()));
				return;
			}
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			DefaultDomain owners = new DefaultDomain();
			ProtectedRegion region = new ProtectedCuboidRegion(String.format("%sHellblock", player.getName()),
					getProtectionVectorLeft(pi.getHellblockLocation()),
					getProtectionVectorRight(pi.getHellblockLocation()));
			owners.addPlayer(player.getUniqueId());
			region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
			region.setOwners(owners);
			region.setPriority(100);
			updateHellblockMessages(player.getUniqueId(), region);
			ApplicableRegionSet set = regionManager
					.getApplicableRegions(BlockVector3.at(pi.getHellblockLocation().getX(),
							pi.getHellblockLocation().getY(), pi.getHellblockLocation().getZ()));
			if (set.size() > 0) {
				Iterator<ProtectedRegion> regions = set.iterator();

				while (regions.hasNext()) {
					ProtectedRegion regionCheck = (ProtectedRegion) regions.next();
					if (!regionCheck.getId().equalsIgnoreCase("__GLOBAL__")
							&& !regionCheck.getId().equalsIgnoreCase("Spawn")) {
						regionManager.removeRegion(regionCheck.getId());
					}
				}
			}

			regionManager.addRegion(region);
			regionManager.save();
			instance.getCoopManager().changeLockStatus(player);
		} catch (Exception ex) {
			LogUtils.severe(String.format("Unable to protect %s's hellblock!", player.getName()), ex);
		}
	}

	public void unprotectHellblock(UUID id, boolean force) {
		HellblockPlayer pi;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		if (this.worldGuardPlatform == null) {
			LogUtils.severe("Could not retrieve WorldGuard platform.");
			return;
		}
		RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
		RegionManager regionManager = regionContainer
				.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
		if (regionManager == null) {
			LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
					instance.getHellblockHandler().getWorldName()));
			return;
		}
		if (!force && pi.getPlayer() == null) {
			LogUtils.severe("Could not find the player restarting their hellblock at this time.");
			return;
		}
		String regionName = String.format("%sHellblock",
				(!force ? pi.getPlayer().getName()
						: Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
								? Bukkit.getOfflinePlayer(id).getName()
								: null));
		if (regionName != null) {
			regionManager.removeRegion(regionName);
		}
	}

	public void updateHellblockMessages(UUID id, ProtectedRegion region) {
		HellblockPlayer pi;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		String name = Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
				? Bukkit.getOfflinePlayer(id).getName()
				: null;
		if (name == null) {
			LogUtils.warn("Failed to retrieve player's username to update hellblock entry and farewell messages.");
			return;
		}
		StringFlag greetFlag = new StringFlag(HellblockFlag.FlagType.GREETING_MESSAGE.getName(), RegionGroup.ALL);
		if (instance.getHellblockHandler().isEntryMessageEnabled()) {
			if (!pi.isAbandoned()) {
				region.setFlag(greetFlag, String.format("&cYou're entering &4%s&c's Hellblock!", name));
			} else {
				region.setFlag(greetFlag, "&4** &cYou're entering an abandoned Hellblock! &4**");
			}
		} else {
			region.setFlag(greetFlag, null);
		}
		StringFlag farewellFlag = new StringFlag(HellblockFlag.FlagType.FAREWELL_MESSAGE.getName(), RegionGroup.ALL);
		if (instance.getHellblockHandler().isFarewellMessageEnabled()) {
			if (!pi.isAbandoned()) {
				region.setFlag(farewellFlag, String.format("&cYou're leaving &4%s&c's Hellblock!", name));
			} else {
				region.setFlag(farewellFlag, "&4** &cYou're leaving an abandoned Hellblock! &4**");
			}
		} else {
			region.setFlag(farewellFlag, null);
		}
	}

	public void abandonIsland(UUID id, ProtectedRegion region) {
		HellblockPlayer pi;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		String name = Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
				? Bukkit.getOfflinePlayer(id).getName()
				: null;
		if (name == null) {
			LogUtils.warn("Failed to retrieve player's username to update hellblock region groups.");
			return;
		}
		if (pi.isAbandoned()) {
			region.getOwners().clear();
			region.getMembers().clear();
			StateFlag pvpFlag = new StateFlag(HellblockFlag.FlagType.PVP.getName(), false, RegionGroup.ALL);
			region.setFlag(pvpFlag, StateFlag.State.DENY);
			StateFlag entryFlag = new StateFlag(HellblockFlag.FlagType.ENTRY.getName(), false, RegionGroup.ALL);
			region.setFlag(entryFlag, StateFlag.State.DENY);
		}
	}

	public BlockVector3 getProtectionVectorLeft(Location loc) {
		return BlockVector3.at(loc.getX() + (double) (instance.getHellblockHandler().getProtectionRange() / 2),
				instance.getHellblockHandler().getHellblockWorld().getMaxHeight(),
				loc.getZ() + (double) (instance.getHellblockHandler().getProtectionRange() / 2));
	}

	public BlockVector3 getProtectionVectorRight(Location loc) {
		return BlockVector3.at(loc.getX() - (double) (instance.getHellblockHandler().getProtectionRange() / 2),
				instance.getHellblockHandler().getHellblockWorld().getMinHeight(),
				loc.getZ() - (double) (instance.getHellblockHandler().getProtectionRange() / 2));
	}

	public boolean isRegionProtected(Location location) {
		com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
		if (this.worldGuardPlatform == null) {
			LogUtils.severe("Could not retrieve WorldGuard platform.");
			return false;
		}
		RegionContainer container = this.worldGuardPlatform.getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(loc);
		if (set.size() != 0)
			return true;
		return false;
	}

	public List<Location> getRegionBlocks(UUID id) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		if (this.worldGuardPlatform == null) {
			LogUtils.severe("Could not retrieve WorldGuard platform.");
			return new ArrayList<>();
		}
		RegionManager regionManager = this.worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
					instance.getHellblockHandler().getWorldName()));
			return new ArrayList<>();
		}
		ProtectedRegion region = regionManager.getRegion(String.format("%sHellblock",
				Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
						? Bukkit.getOfflinePlayer(id).getName()
						: null));
		if (region == null) {
			return new ArrayList<>();
		}

		Location min = BukkitAdapter.adapt(world, region.getMinimumPoint());
		Location max = BukkitAdapter.adapt(world, region.getMaximumPoint());

		List<Location> locations = new ArrayList<>();
		for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
			for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
				for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
					locations.add(new Location(world, x, y, z));
				}
			}
		}
		return locations;
	}

	public ProtectedRegion getRegion(UUID id) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		if (this.worldGuardPlatform == null) {
			LogUtils.severe("Could not retrieve WorldGuard platform.");
			return null;
		}
		RegionManager regionManager = this.worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		if (regionManager == null) {
			LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
					instance.getHellblockHandler().getWorldName()));
			return null;
		}
		ProtectedRegion region = regionManager.getRegion(String.format("%sHellblock",
				Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
						? Bukkit.getOfflinePlayer(id).getName()
						: null));
		if (region == null) {
			return null;
		}

		return region;
	}

	public void protectSpawn() {
		try {
			if (this.worldGuardPlatform == null) {
				LogUtils.severe("Could not retrieve WorldGuard platform.");
				return;
			}
			ProtectedRegion region = null;
			BlockVector3 pos1 = BlockVector3.at((double) instance.getHellblockHandler().getSpawnSize(), 255.0D,
					(double) instance.getHellblockHandler().getSpawnSize());
			BlockVector3 pos2 = BlockVector3.at((double) (0 - instance.getHellblockHandler().getSpawnSize()), 0.0D,
					(double) (0 - instance.getHellblockHandler().getSpawnSize()));
			region = new ProtectedCuboidRegion("Spawn", pos1, pos2);
			RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
						instance.getHellblockHandler().getWorldName()));
				return;
			}
			region.setParent(regionManager.getRegion(ProtectedRegion.GLOBAL_REGION));
			region.setPriority(100);
			StateFlag invincibilityFlag = new StateFlag(HellblockFlag.FlagType.INVINCIBILITY.getName(), false,
					RegionGroup.ALL);
			region.setFlag(invincibilityFlag, StateFlag.State.ALLOW);
			StateFlag pvpFlag = new StateFlag(HellblockFlag.FlagType.PVP.getName(), false, RegionGroup.ALL);
			region.setFlag(pvpFlag, StateFlag.State.DENY);
			regionManager.addRegion(region);
			regionManager.save();
		} catch (Exception ex) {
			LogUtils.severe("Unable to protect spawn area!", ex);
		}
	}

	/**
	 * Gets the regions a player is currently in.
	 *
	 * @param playerUUID UUID of the player in question.
	 * @return Set of WorldGuard protected regions that the player is currently in.
	 */
	@NonNull
	public Set<ProtectedRegion> getRegions(UUID playerUUID) {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null || !player.isOnline() || this.worldGuardPlatform == null)
			return Collections.emptySet();

		RegionQuery query = this.worldGuardPlatform.getRegionContainer().createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
		return set.getRegions();
	}

	/**
	 * Gets the regions names a player is currently in.
	 *
	 * @param playerUUID UUID of the player in question.
	 * @return Set of Strings with the names of the regions the player is currently
	 *         in.
	 */
	@NonNull
	public Set<String> getRegionsNames(UUID playerUUID) {
		return getRegions(playerUUID).stream().map(ProtectedRegion::getId).collect(Collectors.toSet());
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID  UUID of the player in question.
	 * @param regionNames Set of regions to check.
	 * @return True if the player is in (all) the named region(s).
	 */
	public boolean isPlayerInAllRegions(UUID playerUUID, Set<String> regionNames) {
		Set<String> regions = getRegionsNames(playerUUID);
		if (regionNames.isEmpty())
			throw new IllegalArgumentException("You need to check for at least one region!");

		return regions.containsAll(regionNames.stream().map(String::toLowerCase).collect(Collectors.toSet()));
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID  UUID of the player in question.
	 * @param regionNames Set of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAnyRegion(UUID playerUUID, Set<String> regionNames) {
		Set<String> regions = getRegionsNames(playerUUID);
		if (regionNames.isEmpty())
			throw new IllegalArgumentException("You need to check for at least one region!");
		for (String region : regionNames) {
			if (regions.contains(region.toLowerCase()))
				return true;
		}
		return false;
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID UUID of the player in question.
	 * @param regionName List of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAnyRegion(UUID playerUUID, String... regionName) {
		return isPlayerInAnyRegion(playerUUID, new HashSet<>(Arrays.asList(regionName)));
	}

	/**
	 * Checks whether a player is in one or several regions
	 *
	 * @param playerUUID UUID of the player in question.
	 * @param regionName List of regions to check.
	 * @return True if the player is in (any of) the named region(s).
	 */
	public boolean isPlayerInAllRegions(UUID playerUUID, String... regionName) {
		return isPlayerInAllRegions(playerUUID, new HashSet<>(Arrays.asList(regionName)));
	}
}
