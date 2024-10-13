package com.swiftlicious.hellblock.listeners;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.util.WorldEditRegionConverter;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class NetherFarming {

	private final HellblockPlugin instance;

	public NetherFarming(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void trackNetherFarms(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player == null)
			return;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		instance.getScheduler().runTaskAsyncTimer(() -> {
			Set<Block> farmBlocks = getFarmlandOnHellblock(hbPlayer);
			if (farmBlocks != null) {
				Iterator<Block> islandBlocks = farmBlocks.iterator();
				while (true) {
					Block islandBlock;
					do {
						if (!islandBlocks.hasNext()) {
							return;
						}

						islandBlock = (Block) islandBlocks.next();
						Farmland farm = (Farmland) islandBlock.getBlockData();
						if (checkForLavaAroundFarm(farmBlocks)) {
							farm.setMoisture(farm.getMoisture() < farm.getMaximumMoisture() ? farm.getMoisture() + 1
									: farm.getMaximumMoisture());
						} else {
							farm.setMoisture(farm.getMoisture() > 0 ? farm.getMoisture() - 1 : 0);
						}
					} while (hasUngrownCrops(farmBlocks));
				}
			}
		}, 1, 1, TimeUnit.MINUTES);
	}

	public boolean hasUngrownCrops(@Nullable Set<Block> blocks) {
		if (blocks == null)
			return false;

		for (Block farm : blocks) {
			if (!farm.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return false;

			Farmland farmland = (Farmland) farm.getBlockData();
			return farmland.getMoisture() < farmland.getMaximumMoisture();
		}
		return false;
	}

	public boolean checkForLavaAroundFarm(@Nullable Set<Block> blocks) {
		if (blocks == null)
			return false;

		for (Block block : blocks) {
			if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return false;

			int centerX = block.getLocation().getBlockX();
			int centerY = block.getLocation().getBlockY();
			int centerZ = block.getLocation().getBlockZ();
			for (int x = centerX - 3; x <= centerX + 3; x++) {
				for (int z = centerZ - 3; z <= centerZ + 3; z++) {
					Block b = block.getWorld().getBlockAt(x, centerY, z);
					if (b.getType() == Material.LAVA) {
						return b.getType() == Material.LAVA;
					}
				}
			}
		}
		return false;
	}

	public @Nullable Set<Block> getFarmlandOnHellblock(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player == null)
			return null;
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return null;
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			Set<Block> regionBlocks = new HashSet<>();
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe("Could not load WorldGuard regions for hellblock world: " + world.getName());
				return null;
			}
			BlockVector3 vector = BlockVector3.at(player.getX(), player.getY(), player.getZ());
			ApplicableRegionSet wgRegion = regions.getApplicableRegions(vector);
			if (wgRegion == null) {
				return null;
			}
			for (ProtectedRegion region : wgRegion) {
				if (region == null) {
					return null;
				}
				// weRegion is null if wgRegion is a global region
				Region weRegion = WorldEditRegionConverter.convertToRegion(region);
				if (weRegion == null) {
					return null;
				}
				Iterator<BlockVector3> iterator = weRegion.iterator();
				while (iterator.hasNext()) {
					BlockVector3 block = iterator.next();
					Block regionBlock = instance.getHellblockHandler().getHellblockWorld().getBlockAt(block.x(),
							block.y(), block.z());
					if (regionBlock.getType() != Material.FARMLAND)
						continue;
					if (!(regionBlock.getBlockData() instanceof Farmland))
						continue;
					regionBlocks.add(regionBlock);
				}
			}
			return regionBlocks;
		} else {
			// TODO: plugin protection
		}
		return null;
	}
}
