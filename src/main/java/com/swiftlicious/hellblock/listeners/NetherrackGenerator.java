package com.swiftlicious.hellblock.listeners;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.events.generator.GeneratorGenerateEvent;
import com.swiftlicious.hellblock.events.generator.PlayerBreakGeneratedBlock;
import com.swiftlicious.hellblock.listeners.generator.GenBlock;
import com.swiftlicious.hellblock.listeners.generator.GenMode;
import com.swiftlicious.hellblock.listeners.generator.GenPiston;
import com.swiftlicious.hellblock.listeners.generator.GeneratorManager;
import com.swiftlicious.hellblock.listeners.generator.GeneratorModeManager;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;

public class NetherrackGenerator implements Listener {

	private final HellblockPlugin instance;
	@Getter
	private final GeneratorManager genManager;
	@Getter
	private final GeneratorModeManager genModeManager;

	public NetherrackGenerator(HellblockPlugin plugin) {
		instance = plugin;
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
		this.genModeManager.loadFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onBlockFlow(BlockFromToEvent event) {
		Block fromBlock = event.getBlock();
		if (!fromBlock.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		List<GenMode> modes = genModeManager.getModes();
		for (GenMode mode : modes) {
			if (!mode.isValid())
				continue;

			Block toBlock = event.getToBlock();
			Material toBlockMaterial = toBlock.getType();

			if (toBlockMaterial.equals(Material.AIR)) {
				if (isGenerating(mode, event.getFace(), fromBlock, toBlock)) {
					Location l = toBlock.getLocation();
					if (l.getWorld() == null)
						return;
					// Checks if the block has been broken before and if it is a known gen location
					if (!genManager.isGenLocationKnown(l) && mode.isSearchingForPlayersNearby()) {
						double searchRadius = instance.getConfig("config.yml")
								.getDouble("netherrack-generator-options.playerSearchRadius", 4D);
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
							LogUtils.severe(String.format("GB has expired %s", gb.getLocation()));
							genManager.removeKnownGenLocation(l);
							return;
						}

						UUID uuid = gb.getUUID(); // Get the uuid of the player who broke the blocks

						if (!(mode.canGenerateWhileLavaRaining()) && instance.getLavaRain().getLavaRainTask() != null
								&& instance.getLavaRain().getLavaRainTask().isLavaRaining()) {
							event.setCancelled(true);
							if (!toBlock.getLocation().getBlock().getType().equals(mode.getFallbackMaterial()))
								toBlock.getLocation().getBlock().setType(mode.getFallbackMaterial());
							return;
						}

						float soundVolume = 2F;
						float pitch = 1F;
						Material result = null;
						if (getRandomResult() != null) {
							result = getRandomResult();
						} else if (mode.hasFallBackMaterial()) {
							result = mode.getFallbackMaterial();
						}

						GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, result, uuid,
								toBlock.getLocation());
						Bukkit.getPluginManager().callEvent(event);
						if (genEvent.isCancelled())
							return;
						event.setCancelled(true);
						if (genEvent.getResult() == null) {
							LogUtils.severe(String.format("Unknown material %s", result.name()));
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

	public @Nullable Player getClosestPlayer(Location l, Collection<Entity> entitiesNearby) {
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
		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
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
		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
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

	private boolean isGenerating(GenMode mode, BlockFace face, Block fromB, Block toB) {
		if (!mode.isValid())
			return false;

		if (fromB.getType() != Material.LAVA)
			return false;
		int fromLevel = ((Levelled) fromB.getBlockData()).getLevel();

		Block nextBlock = toB.getRelative(face, 1);
		if (nextBlock.getType() != Material.LAVA)
			return false;

		int genCount = 0;
		int nextBlockLevel = ((Levelled) nextBlock.getBlockData()).getLevel();
		if (fromLevel < nextBlockLevel) {
			genCount++;
		}
		if (fromLevel == nextBlockLevel) {
			genCount++;
		}

		return genCount > 0;
	}

	public @NonNull Map<Material, Double> getResults() {
		List<String> materials = HellblockPlugin.getInstance().getConfig("config.yml")
				.getConfigurationSection("netherrack-generator-options.generation").getStringList("blocks");
		Map<Material, Double> results = new HashMap<>();
		for (String result : materials) {
			String[] split = result.split(":");
			Material type = Material.getMaterial(split[0]);
			double chance = 0.0D;
			try {
				chance = Double.parseDouble(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.warn(
						String.format("Could not define the given chance %s for the block type %s", split[1], split[0]),
						ex);
				continue;
			}
			results.put(type, chance);
		}
		return results;
	}

	public @Nullable Material getRandomResult() {
		double r = Math.random() * 100;
		double prev = 0;
		for (Material m : this.getResults().keySet()) {
			double chance = this.getResults().get(m) + prev;
			if (r > prev && r <= chance)
				return m;
			else
				prev = chance;
			continue;
		}
		return null;
	}
}