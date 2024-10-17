package com.swiftlicious.hellblock.api.compatibility;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitWorld;
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
			HellblockPlayer pi = (HellblockPlayer) instance.getHellblockHandler().getActivePlayers()
					.get(player.getUniqueId());
			ProtectedRegion region = null;
			RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(new BukkitWorld(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe("Could not get the WorldGuard region manager for the world: "
						+ instance.getHellblockHandler().getWorldName());
				return;
			}
			DefaultDomain owners = new DefaultDomain();
			region = new ProtectedCuboidRegion(player.getName() + "Hellblock",
					getProtectionVectorLeft(pi.getHellblockLocation()),
					getProtectionVectorRight(pi.getHellblockLocation()));
			owners.addPlayer(player.getName());
			region.setOwners(owners);
			region.setParent(regionManager.getRegion("__GLOBAL__"));
			region.setPriority(100);
			region.setFlag(Flags.PVP,
					StateFlag.State.valueOf(instance.getHellblockHandler().getAllowPvP().toUpperCase()));
			region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
			region.setFlag(Flags.USE, StateFlag.State.DENY);
			region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
			region.setFlag(Flags.ENTITY_ITEM_FRAME_DESTROY, StateFlag.State.DENY);
			region.setFlag(Flags.ENTITY_PAINTING_DESTROY, StateFlag.State.DENY);
			region.setFlag(Flags.BUILD, StateFlag.State.DENY);
			region.setFlag(Flags.DAMAGE_ANIMALS, StateFlag.State.DENY);
			region.setFlag(Flags.DESTROY_VEHICLE, StateFlag.State.DENY);
			region.setFlag(Flags.FALL_DAMAGE, StateFlag.State.DENY);
			region.setFlag(Flags.FIREWORK_DAMAGE, StateFlag.State.DENY);
			region.setFlag(Flags.HUNGER_DRAIN, StateFlag.State.DENY);
			region.setFlag(Flags.ITEM_FRAME_ROTATE, StateFlag.State.DENY);
			region.setFlag(Flags.TRAMPLE_BLOCKS, StateFlag.State.DENY);
			region.setFlag(Flags.PLACE_VEHICLE, StateFlag.State.DENY);
			region.setFlag(Flags.USE_ANVIL, StateFlag.State.DENY);
			region.setFlag(Flags.USE_DRIPLEAF, StateFlag.State.DENY);
			region.setFlag(Flags.USE, StateFlag.State.DENY);
			region.setFlag(Flags.SLEEP, StateFlag.State.DENY);
			region.setFlag(Flags.GREET_MESSAGE, "&cYou're entering &4" + player.getName() + "&c's Hellblock!");
			region.setFlag(Flags.FAREWELL_MESSAGE, "&cYou're leaving &4" + player.getName() + "&c's Hellblock!");
			ApplicableRegionSet set = regionManager
					.getApplicableRegions(BlockVector3.at(pi.getHellblockLocation().getX(),
							pi.getHellblockLocation().getY(), pi.getHellblockLocation().getZ()));
			if (set.size() > 0) {
				Iterator<ProtectedRegion> var6 = set.iterator();

				while (var6.hasNext()) {
					ProtectedRegion regions = (ProtectedRegion) var6.next();
					if (!regions.getId().equalsIgnoreCase("__GLOBAL__")) {
						regionManager.removeRegion(regions.getId());
					}
				}
			}

			regionManager.addRegion(region);
			regionManager.save();
		} catch (Exception var7) {
			LogUtils.severe("Unable to protect " + player.getName() + "'s hellblock!", var7);
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

	public void unprotectHellblock(Player player) {
		HellblockPlayer pi = (HellblockPlayer) instance.getHellblockHandler().getActivePlayers()
				.get(player.getUniqueId());
		RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
		RegionManager regionManager = regionContainer
				.get(new BukkitWorld(instance.getHellblockHandler().getHellblockWorld()));
		if (regionManager == null) {
			LogUtils.severe("Could not get the WorldGuard region manager for the world: "
					+ instance.getHellblockHandler().getWorldName());
			return;
		}
		regionManager.removeRegion(pi.getPlayer().getName() + "Hellblock");
	}

	public void protectSpawn(Player player) {
		try {
			ProtectedRegion region = null;
			BlockVector3 pos1 = BlockVector3.at((double) instance.getHellblockHandler().getSpawnSize(), 255.0D,
					(double) instance.getHellblockHandler().getSpawnSize());
			BlockVector3 pos2 = BlockVector3.at((double) (0 - instance.getHellblockHandler().getSpawnSize()), 0.0D,
					(double) (0 - instance.getHellblockHandler().getSpawnSize()));
			region = new ProtectedCuboidRegion("Spawn", pos1, pos2);
			RegionContainer regionContainer = getWorldGuardPlatform().getRegionContainer();
			RegionManager regionManager = regionContainer
					.get(new BukkitWorld(instance.getHellblockHandler().getHellblockWorld()));
			if (regionManager == null) {
				LogUtils.severe("Could not get the WorldGuard region manager for the world: "
						+ instance.getHellblockHandler().getWorldName());
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
