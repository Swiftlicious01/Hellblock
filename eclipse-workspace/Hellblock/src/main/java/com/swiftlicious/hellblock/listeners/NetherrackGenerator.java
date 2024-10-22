package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.events.generator.GeneratorGenerateEvent;
import com.swiftlicious.hellblock.events.generator.PlayerBreakGeneratedBlock;
import com.swiftlicious.hellblock.listeners.generator.GenBlock;
import com.swiftlicious.hellblock.listeners.generator.GenMode;
import com.swiftlicious.hellblock.listeners.generator.GenPiston;
import com.swiftlicious.hellblock.listeners.generator.GeneratorManager;
import com.swiftlicious.hellblock.listeners.generator.GeneratorModeManager;
import com.swiftlicious.hellblock.utils.LogUtils;

public class NetherrackGenerator implements Listener {

	private final HellblockPlugin instance;
	private final GeneratorManager genManager;
	private final GeneratorModeManager genModeManager;

	public NetherrackGenerator(HellblockPlugin plugin) {
		instance = plugin;
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
		this.genModeManager.loadFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void checkFlow(BlockFromToEvent event) { // Does not respond if the toBlock is Lava
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Block from = event.getBlock();
		if (from.getType() != Material.LAVA)
			return;
		int fromLevel = ((Levelled) from.getBlockData()).getLevel();

		Block toBlock = event.getToBlock();
		BlockFace face = event.getFace();

		Block nextBlock = toBlock.getRelative(face, 1);
		if (nextBlock.getType() != Material.LAVA)
			return;

		int nextBlockLevel = ((Levelled) nextBlock.getBlockData()).getLevel();
		if (fromLevel < nextBlockLevel) {
			nextBlock.setType(Material.NETHERRACK);
		}
		if (fromLevel == nextBlockLevel) {
			toBlock.setType(Material.NETHERRACK);
		}
	}

	@EventHandler
	public void onBlockFlow(BlockFromToEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Block b = event.getBlock();

		Material m = b.getType();

		List<GenMode> modes = genModeManager.getModesContainingMaterial(m);
		for (GenMode mode : modes) {
			if (!mode.containsLiquidBlock() || !mode.isValid())
				continue;

			Block toBlock = event.getToBlock();
			Material toBlockMaterial = toBlock.getType();

			if (toBlockMaterial.equals(Material.AIR) || mode.containsBlock(toBlockMaterial)) {
				if (isGenerating(mode, m, toBlock)) {
					Location l = toBlock.getLocation();
					if (l.getWorld() == null)
						return;
					// Checks if the block has been broken before and if it is a known gen location
					if (!genManager.isGenLocationKnown(l) && mode.isSearchingForPlayersNearby()) {
						double searchRadius = instance.getConfig("config.yml")
								.getDouble("generator-options.playerSearchRadius", 4D);
						if (l.getWorld() == null)
							return;
						Collection<Entity> entitiesNearby = l.getWorld().getNearbyEntities(l, searchRadius,
								searchRadius, searchRadius);
						Player closestPlayer = getClosestPlayer(l, entitiesNearby);
						if (closestPlayer != null) {
							genManager.addKnownGenLocation(l);
							genManager.setPlayerForLocation(closestPlayer.getUniqueId(), l, false);
						}
					}
					if (genManager.isGenLocationKnown(l)) {
						// it is a Known gen location
						if (!genManager.getGenBreaks().containsKey(l))
							return; // A player has not prev broken a block here
						// A player has prev broken a block here
						GenBlock gb = genManager.getGenBreaks().get(l); // Get the GenBlock in this location
						if (gb.hasExpired()) {
							LogUtils.severe("GB has expired " + gb.getLocation());
							genManager.removeKnownGenLocation(l);
							return;
						}

						UUID uuid = gb.getUUID(); // Get the uuid of the player who broke the blocks

						if (!(mode.canGenerateWhileLavaRaining()) && instance.getLavaRain().getLavaRainTask() != null
								&& instance.getLavaRain().getLavaRainTask().isLavaRaining()) {
							event.setCancelled(true);
							if (!toBlock.getLocation().getBlock().getType().equals(Material.NETHERRACK))
								toBlock.getLocation().getBlock().setType(Material.NETHERRACK);
							return;
						}

						float soundVolume = 2F;
						float pitch = 1F;
						Material result = null;
						if (mode.hasFallBackMaterial()) {
							result = mode.getFallbackMaterial();
						}

						GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, result, uuid,
								toBlock.getLocation());
						Bukkit.getPluginManager().callEvent(event);
						if (genEvent.isCancelled())
							return;
						event.setCancelled(true);
						if (genEvent.getResult() == null) {
							LogUtils.severe("Unknown material " + result.name());
							return;
						}
						genEvent.getGenerationLocation().getBlock().setType(genEvent.getResult());
						
						if (mode.hasGenSound())
							l.getWorld().playSound(l, mode.getGenSound(), soundVolume, pitch);
						
						if (mode.hasParticleEffect())
							mode.displayGenerationParticles(l);
					} else {
						genManager.addKnownGenLocation(l);
						return;
					}
				}
			}
		}
	}

	@Nullable
	public Player getClosestPlayer(Location l, Collection<Entity> entitiesNearby) {
		Player closestPlayer = null;
		double closestDistance = 100D;
		for (Entity entity : entitiesNearby) {
			if (entity instanceof Player p) {
				double distance = l.distance(p.getLocation());
				if (closestPlayer != null && !(closestDistance > distance)) {
					continue;
				}
				closestPlayer = p;
				closestDistance = distance;
			}
		}
		return closestPlayer;
	}

	@EventHandler
	public void onBlockChange(EntityChangeBlockEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!event.getEntityType().equals(EntityType.FALLING_BLOCK)
				|| !(event.getTo().equals(Material.AIR) || event.getTo().name().contains("LAVA")))
			return;

		Location loc = event.getBlock().getLocation();
		if (genManager.isGenLocationKnown(loc)) {
			event.setCancelled(true);
			event.getBlock().getState().update(false, false);
		}
	}

	@EventHandler
	public void onPistonPush(BlockPistonExtendEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!instance.getConfig("config.yml").getConfigurationSection("generator-options.automation")
				.getBoolean("pistons", false))
			return;
		if (!genManager.getKnownGenPistons().containsKey(event.getBlock().getLocation()))
			return;
		GenPiston piston = genManager.getKnownGenPistons().get(event.getBlock().getLocation());
		Location genBlockLoc = event.getBlock().getRelative(event.getDirection()).getLocation();
		if (genManager.isGenLocationKnown(genBlockLoc)) {
			piston.setHasBeenUsed(true);
			genManager.setPlayerForLocation(piston.getUUID(), genBlockLoc, true);
		}

	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!instance.getConfig("config.yml").getConfigurationSection("generator-options.automation")
				.getBoolean("pistons", false))
			return;
		if (event.getBlock().getType() != Material.PISTON)
			return;
		Player p = event.getPlayer();
		if (!p.isOnline())
			return;
		UUID uuid = p.getUniqueId();
		GenPiston piston = new GenPiston(event.getBlock().getLocation(), uuid);
		genManager.addKnownGenPiston(piston);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Location l = event.getBlock().getLocation();
		Player p = event.getPlayer();

		if (genManager.getKnownGenPistons().containsKey(l)) {
			genManager.getKnownGenPistons().remove(l);
			return;
		}

		if (l.getWorld() == null)
			return;
		if (genManager.isGenLocationKnown(l)) {
			PlayerBreakGeneratedBlock genEvent = new PlayerBreakGeneratedBlock(p, l);
			Bukkit.getPluginManager().callEvent(genEvent);
			if (genEvent.isCancelled())
				return;
			genManager.setPlayerForLocation(p.getUniqueId(), l, false);
		}
	}

	private static final BlockFace[] faces = new BlockFace[] { BlockFace.SELF, BlockFace.UP, BlockFace.DOWN,
			BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

	private boolean isGenerating(GenMode mode, Material fromM, Block toB) {
		if (!mode.isValid())
			return false;
		int blocksFound = 0; /* We need all blocks to be correct */
		List<BlockFace> testedFaces = new ArrayList<>();
		if (mode.getFixedBlocks() != null && !mode.getFixedBlocks().isEmpty()) {
			for (Entry<BlockFace, Material> entry : mode.getFixedBlocks().entrySet()) {
				if (testedFaces.contains(entry.getKey()))
					continue;
				testedFaces.add(entry.getKey());
				if (this.isSameMaterial(entry.getValue().name(), fromM.name()))
					continue; // Should not check for the original block
				Block r = toB.getRelative(entry.getKey(), 1);
				Material rm = r.getType();

				if (this.isSameMaterial(rm.name(), entry.getValue().name())) {
					blocksFound++; /* This block is positioned correctly; */
				} else {
					return false; /* This block is not positioned correctly so we stop testing */
				}
			}
		}

		if (mode.getBlocks() != null && !mode.getBlocks().isEmpty()) {
			for (BlockFace face : faces) {
				if (testedFaces.contains(face))
					continue;
				testedFaces.add(face);
				Block r = toB.getRelative(face, 1);
				Material rm = r.getType();
				/*
				 * This also sadly disables LAVA and LAVA generators
				 */
				if (this.isSameMaterial(rm.name(), fromM.name())) { // Should not check for
					// the original block
					continue;
				}

				for (Material mirrorMaterial : mode.getBlocks()) {
					if (this.isSameMaterial(rm.name(), mirrorMaterial.name())) {
						blocksFound++; /* This block is positioned correctly; */
					}
				}
			}
		}

		blocksFound++;
		int blocksNeeded = (mode.getBlocks().size() + mode.getFixedBlocks().size());
		return blocksFound >= blocksNeeded;
	}

	private boolean isSameMaterial(String materialName1, String materialName2) {
		/*
		 * Version 1.12 and under have multiple names for lava so both needs to be
		 * tested for
		 */
		if (materialName1.equalsIgnoreCase(materialName2))
			return true;
		else
			return isLava(materialName1) && isLava(materialName2);
	}

	private boolean isLava(String materialName) {
		return materialName.equalsIgnoreCase("LAVA") || materialName.equalsIgnoreCase("STATIONARY_LAVA");
	}
}