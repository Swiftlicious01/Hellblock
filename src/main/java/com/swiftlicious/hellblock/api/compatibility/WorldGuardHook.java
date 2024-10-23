package com.swiftlicious.hellblock.api.compatibility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.Setter;

public class WorldGuardHook {

	private final HellblockPlugin instance;
	@Getter
	@Setter
	private WorldGuardPlatform worldGuardPlatform;

	public WorldGuardHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	@SuppressWarnings("deprecation")
	public void protectHellblock(Player player) {
		try {
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			ProtectedRegion region = null;
			RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
						instance.getHellblockHandler().getWorldName()));
				return;
			}
			DefaultDomain owners = new DefaultDomain();
			region = new ProtectedCuboidRegion(String.format("%sHellblock", player.getName()),
					getProtectionVectorLeft(pi.getHellblockLocation()),
					getProtectionVectorRight(pi.getHellblockLocation()));
			owners.addPlayer(player.getUniqueId());
			region.setOwners(owners);
			region.setParent(regionManager.getRegion("__GLOBAL__"));
			region.setPriority(100);
			region.setFlag(Flags.PVP,
					StateFlag.State.valueOf(instance.getHellblockHandler().getAllowPvP().toUpperCase()));
			region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
			region.setFlag(Flags.USE, StateFlag.State.DENY);
			region.setFlag(Flags.ENTITY_ITEM_FRAME_DESTROY, StateFlag.State.DENY);
			region.setFlag(Flags.ENTITY_PAINTING_DESTROY, StateFlag.State.DENY);
			region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
			region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
			region.setFlag(Flags.DAMAGE_ANIMALS, StateFlag.State.DENY);
			region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
			region.setFlag(Flags.ITEM_FRAME_ROTATE, StateFlag.State.DENY);
			region.setFlag(Flags.PLACE_VEHICLE, StateFlag.State.DENY);
			region.setFlag(Flags.USE_ANVIL, StateFlag.State.DENY);
			region.setFlag(Flags.USE_DRIPLEAF, StateFlag.State.DENY);
			region.setFlag(Flags.SLEEP, StateFlag.State.DENY);
			region.setFlag(Flags.GREET_MESSAGE,
					String.format("&cYou're entering &4%s&c's Hellblock!", player.getName()));
			region.setFlag(Flags.FAREWELL_MESSAGE,
					String.format("&cYou're leaving &4%s&c's Hellblock!", player.getName()));
			ApplicableRegionSet set = regionManager
					.getApplicableRegions(BlockVector3.at(pi.getHellblockLocation().getX(),
							pi.getHellblockLocation().getY(), pi.getHellblockLocation().getZ()));
			if (set.size() > 0) {
				Iterator<ProtectedRegion> var6 = set.iterator();

				while (var6.hasNext()) {
					ProtectedRegion regions = (ProtectedRegion) var6.next();
					if (!regions.getId().equalsIgnoreCase("__GLOBAL__") && !regions.getId().equalsIgnoreCase("Spawn")) {
						regionManager.removeRegion(regions.getId());
					}
				}
			}

			regionManager.addRegion(region);
			regionManager.save();
		} catch (Exception var7) {
			LogUtils.severe(String.format("Unable to protect %s's hellblock!", player.getName()), var7);
		}
	}

	public void unprotectHellblock(UUID id, boolean force) {
		HellblockPlayer pi;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		RegionContainer regionContainer = this.worldGuardPlatform.getRegionContainer();
		RegionManager regionManager = regionContainer
				.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
		if (regionManager == null) {
			LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
					instance.getHellblockHandler().getWorldName()));
			return;
		}
		if (pi.getPlayer() == null && !force) {
			LogUtils.severe("Could not find the player restarting their hellblock at this time.");
			return;
		}
		regionManager.removeRegion(String.format("%sHellblock",
				(!force ? pi.getPlayer().getName()
						: Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
								? Bukkit.getOfflinePlayer(id).getName()
								: "?")));
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
		RegionContainer container = this.worldGuardPlatform.getRegionContainer();
		RegionQuery query = container.createQuery();
		ApplicableRegionSet set = query.getApplicableRegions(loc);
		if (set.size() != 0)
			return true;
		return false;
	}

	public List<Location> getRegionBlocks(Player player) {
		World world = instance.getHellblockHandler().getHellblockWorld();
		RegionManager regionManager = this.worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(world));
		ProtectedRegion region = regionManager.getRegion(String.format("%sHellblock", player.getName()));

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

	public void protectSpawn() {
		try {
			ProtectedRegion region = null;
			BlockVector3 pos1 = BlockVector3.at((double) instance.getHellblockHandler().getSpawnSize(), 255.0D,
					(double) instance.getHellblockHandler().getSpawnSize());
			BlockVector3 pos2 = BlockVector3.at((double) (0 - instance.getHellblockHandler().getSpawnSize()), 0.0D,
					(double) (0 - instance.getHellblockHandler().getSpawnSize()));
			region = new ProtectedCuboidRegion("Spawn", pos1, pos2);
			RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
						instance.getHellblockHandler().getWorldName()));
				return;
			}
			region.setParent(regionManager.getRegion("__GLOBAL__"));
			region.setPriority(100);
			region.setFlag(Flags.PVP, StateFlag.State.DENY);
			region.setFlag(Flags.INVINCIBILITY, StateFlag.State.ALLOW);
			region.setFlag(Flags.MOB_SPAWNING, StateFlag.State.DENY);
			region.setFlag(Flags.MOB_DAMAGE, StateFlag.State.DENY);
			region.setFlag(Flags.BUILD, StateFlag.State.DENY);
			regionManager.addRegion(region);
			regionManager.save();
		} catch (Exception var4) {
			LogUtils.severe("Unable to protect spawn area!", var4);
		}
	}
}
