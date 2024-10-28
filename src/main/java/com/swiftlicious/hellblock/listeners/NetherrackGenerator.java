package com.swiftlicious.hellblock.listeners;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
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
	@Getter
	private final List<String> generationResults;

	private final static BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	public NetherrackGenerator(HellblockPlugin plugin) {
		instance = plugin;
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
		this.genModeManager.loadFromConfig();
		this.generationResults = instance.getConfig("config.yml")
				.getConfigurationSection("netherrack-generator-options.generation").getStringList("blocks");
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onBlockFlow(BlockFromToEvent event) {
		Block fromBlock = event.getBlock();
		Material fromBlockMaterial = fromBlock.getType();
		if (!fromBlock.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (!Arrays.asList(FACES).contains(event.getFace())) {
			return;
		}
		GenMode mode = genModeManager.getGenMode();
		Block toBlock = event.getToBlock();
		Material toBlockMaterial = toBlock.getType();

		if (fromBlockMaterial == Material.LAVA) {
			if (toBlockMaterial.isAir() && !isLavaPool(toBlock.getLocation())
					&& (fromBlock.getRelative(event.getFace(), 2).getType() == Material.LAVA
							&& isFlowing(fromBlock.getRelative(event.getFace(), 2)))) {
				Location l = toBlock.getLocation();
				if (l.getWorld() == null)
					return;
				// Checks if the block has been broken before and if it is a known gen location
				if (!genManager.isGenLocationKnown(l) && mode.isSearchingForPlayersNearby()) {
					double searchRadius = instance.getConfig("config.yml")
							.getDouble("netherrack-generator-options.playerSearchRadius", 4D);
					if (l.getWorld() == null)
						return;
					Collection<Entity> entitiesNearby = l.getWorld().getNearbyEntities(l, searchRadius, searchRadius,
							searchRadius);
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
						instance.debug(String.format("GB has expired %s", gb.getLocation()));
						genManager.removeKnownGenLocation(l);
						return;
					}

					UUID uuid = gb.getUUID(); // Get the uuid of the player who broke the blocks

					if (!(mode.canGenerateWhileLavaRaining()) && instance.getLavaRain().getLavaRainTask() != null
							&& instance.getLavaRain().getLavaRainTask().isLavaRaining()) {
						event.setCancelled(true);
						if (toBlock.getLocation().getBlock().getType() != mode.getFallbackMaterial())
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
					Bukkit.getPluginManager().callEvent(genEvent);
					if (genEvent.isCancelled())
						return;
					event.setCancelled(true);
					if (genEvent.getResult() == null) {
						LogUtils.severe(String.format("Unknown material %s.", result.name()));
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

	public @Nullable Player getClosestPlayer(Location l, Collection<Entity> entitiesNearby) {
		Player closestPlayer = null;
		double closestDistance = 100D;
		for (Entity entity : entitiesNearby) {
			if (entity instanceof Player player) {
				double distance = l.distance(player.getLocation());
				if (closestPlayer != null && !(closestDistance > distance)) {
					continue;
				}
				closestPlayer = player;
				closestDistance = distance;
			}
		}
		return closestPlayer;
	}

	@EventHandler
	public void onBlockChange(EntityChangeBlockEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getEntityType() != EntityType.FALLING_BLOCK
				|| !(event.getTo() == Material.AIR || event.getTo() == Material.LAVA))
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

	private boolean isFlowing(@NonNull Block block) {
		boolean isFlowing = false;
		Levelled flowingData = (Levelled) block.getBlockData();
		isFlowing = flowingData.getLevel() > 0 && flowingData.getLevel() <= 7;
		return isFlowing;
	}

	private boolean isLavaPool(@NonNull Location location) {
		if (location.getWorld() == null)
			return false;
		int lavaCount = 0;
		int centerX = location.getBlockX();
		int centerY = location.getBlockY();
		int centerZ = location.getBlockZ();
		for (int x = centerX - 2; x <= centerX + 2; x++) {
			for (int y = centerY - 1; y <= centerY + 1; y++) {
				for (int z = centerZ - 2; z <= centerZ + 2; z++) {
					Block b = location.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.AIR)
						continue;
					if (b.getType() == Material.LAVA) {
						lavaCount++;
					}
				}
			}
		}
		return lavaCount > 4;
	}

	public @NonNull Map<Material, Double> getResults() {
		Map<Material, Double> results = new HashMap<>();
		for (String result : this.getGenerationResults()) {
			String[] split = result.split(":");
			Material type = Material.getMaterial(split[0].toUpperCase());
			double chance = 0.0D;
			try {
				chance = Double.parseDouble(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.warn(String.format("Could not define the given chance %s for the block type %s.", split[1],
						split[0]), ex);
				continue;
			}
			results.put(type != null ? type : Material.NETHERRACK, chance);
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