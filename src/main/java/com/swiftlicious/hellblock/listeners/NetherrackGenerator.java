package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.events.generator.GeneratorGenerateEvent;
import com.swiftlicious.hellblock.events.generator.PlayerBreakGeneratedBlock;
import com.swiftlicious.hellblock.listeners.generator.GenBlock;
import com.swiftlicious.hellblock.listeners.generator.GenMode;
import com.swiftlicious.hellblock.listeners.generator.GenPiston;
import com.swiftlicious.hellblock.listeners.generator.GeneratorManager;
import com.swiftlicious.hellblock.listeners.generator.GeneratorModeManager;
import com.swiftlicious.hellblock.player.OnlineUser;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.StringUtils;

import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound.Source;

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
	public void onNetherrackGeneration(BlockFromToEvent event) {
		Block fromBlock = event.getBlock();
		Material fromBlockMaterial = fromBlock.getType();
		if (!fromBlock.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		BlockFace face = event.getFace();
		if (!Arrays.asList(FACES).contains(face)) {
			return;
		}

		GenMode mode = genModeManager.getGenMode();
		Block toBlock = event.getToBlock();
		Material toBlockMaterial = toBlock.getType();
		boolean normalGenerator = fromBlock.getRelative(face, 2).getType() == Material.LAVA
				&& isFlowing(fromBlock.getRelative(face, 2));

		if (fromBlockMaterial == Material.LAVA) {
			// TODO: make other positioned generators work
			if (toBlockMaterial.isAir() && !isLavaPool(toBlock.getLocation()) && (normalGenerator)) {
				Location l = toBlock.getLocation();
				if (l.getWorld() == null)
					return;
				// Checks if the block has been broken before and if it is a known gen location
				if (!genManager.isGenLocationKnown(l) && mode.isSearchingForPlayersNearby()) {
					double searchRadius = instance.getConfig("config.yml")
							.getDouble("netherrack-generator-options.player-search-radius", 4D);
					Collection<Player> playersNearby = l.getWorld().getNearbyPlayers(l, searchRadius, searchRadius,
							searchRadius);
					Player closestPlayer = getClosestPlayer(l, playersNearby);
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

					if (!(mode.canGenerateWhileLavaRaining()) && instance.getLavaRainHandler().getLavaRainTask() != null
							&& instance.getLavaRainHandler().getLavaRainTask().isLavaRaining()) {
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
					} else {
						result = Material.NETHERRACK;
					}
					
					GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, result != null ? result : Material.NETHERRACK, uuid,
							toBlock.getLocation());
					Bukkit.getPluginManager().callEvent(genEvent);
					if (genEvent.isCancelled())
						return;
					event.setCancelled(true);
					if (genEvent.getResult() == null) {
						LogUtils.severe(
								String.format("Unknown material: %s.", result.toString()));
						return;
					}
					genEvent.getGenerationLocation().getBlock().setType(genEvent.getResult());

					if (mode.hasGenSound())
						instance.getAdventureManager().sendSound(l, Source.AMBIENT, Key.key(mode.getGenSound()),
								soundVolume, pitch);

					if (mode.hasParticleEffect())
						mode.displayGenerationParticles(l);
				} else {
					genManager.addKnownGenLocation(l);
					return;
				}
			}
		}
	}

	public @Nullable Player getClosestPlayer(Location l, Collection<Player> playersNearby) {
		Player closestPlayer = null;
		double closestDistance = 100D;
		for (Player player : playersNearby) {
			double distance = l.distance(player.getLocation());
			if (closestPlayer != null && !(closestDistance > distance)) {
				continue;
			}
			closestPlayer = player;
			closestDistance = distance;
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

		Location location = event.getBlock().getLocation();
		if (location.getWorld() == null)
			return;
		if (genManager.isGenLocationKnown(location)) {
			genManager.getKnownGenLocations().remove(location);
		}

		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
				.getBoolean("pistons", false))
			return;
		if (event.getBlock().getType() != Material.PISTON)
			return;
		final Player player = event.getPlayer();
		if (!player.isOnline())
			return;

		final UUID uuid = player.getUniqueId();
		GenPiston piston = new GenPiston(location, uuid);
		genManager.addKnownGenPiston(piston);
	}

	@EventHandler
	public void onGeneratedBlockBreak(BlockBreakEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Location location = event.getBlock().getLocation();
		final Player player = event.getPlayer();

		if (genManager.getKnownGenPistons().containsKey(location)) {
			genManager.getKnownGenPistons().remove(location);
			return;
		}

		if (location.getWorld() == null)
			return;
		if (genManager.isGenLocationKnown(location)) {
			PlayerBreakGeneratedBlock genEvent = new PlayerBreakGeneratedBlock(player, location);
			Bukkit.getPluginManager().callEvent(genEvent);
			if (genEvent.isCancelled())
				return;
			OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser == null)
				return;
			if (!onlineUser.getChallengeData().isChallengeActive(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)
					&& !onlineUser.getChallengeData()
							.isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
				onlineUser.getChallengeData().beginChallengeProgression(onlineUser.getPlayer(),
						ChallengeType.NETHERRACK_GENERATOR_CHALLENGE);
			} else {
				onlineUser.getChallengeData().updateChallengeProgression(onlineUser.getPlayer(),
						ChallengeType.NETHERRACK_GENERATOR_CHALLENGE, 1);
				if (onlineUser.getChallengeData().isChallengeCompleted(ChallengeType.NETHERRACK_GENERATOR_CHALLENGE)) {
					onlineUser.getChallengeData().completeChallenge(onlineUser.getPlayer(),
							ChallengeType.NETHERRACK_GENERATOR_CHALLENGE);
				}
			}
			genManager.setPlayerForLocation(player.getUniqueId(), location, false);
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!event.getBlock().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
				.getBoolean("pistons", false))
			return;
		if (event.getBlock().getType() != Material.PISTON)
			return;

		Location location = event.getBlock().getLocation();
		if (genManager.getKnownGenPistons().containsKey(location)) {
			genManager.getKnownGenPistons().remove(location);
			return;
		}
	}

	private boolean isFlowing(@NonNull Block block) {
		boolean isFlowing = false;
		Levelled flowingData = (Levelled) block.getBlockData();
		isFlowing = flowingData.getLevel() >= 1 && flowingData.getLevel() <= 7;
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
			if (type == null || type == Material.AIR)
				continue;
			double chance = 0.0D;
			try {
				chance = Double.parseDouble(split[1]);
			} catch (NumberFormatException ex) {
				LogUtils.warn(String.format("Could not define the given chance %s for the block type %s.", split[1],
						split[0]), ex);
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

	public void savePistons(@NonNull UUID id) {
		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
				.getBoolean("pistons", false))
			return;
		GenPiston[] generatedPistons = genManager.getGenPistonsByUUID(id);

		if (generatedPistons != null && generatedPistons.length > 0) {
			List<String> locations = new ArrayList<>();
			for (GenPiston piston : generatedPistons) {
				if (piston == null || piston.getLoc() == null || !piston.hasBeenUsed()
						|| !piston.getLoc().getBlock().getType().equals(Material.PISTON))
					continue;

				String serializedLoc = StringUtils.serializeLoc(piston.getLoc());
				if (!locations.contains(serializedLoc))
					locations.add(serializedLoc);
			}
			if (!locations.isEmpty()) {
				OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
				if (onlineUser == null)
					return;
				onlineUser.getPlayerData().setPistonLocations(locations);
			}
		}
	}

	public void loadPistons(@NonNull UUID id) {
		if (!instance.getConfig("config.yml").getConfigurationSection("netherrack-generator-options.automation")
				.getBoolean("pistons", false))
			return;
		OnlineUser onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser == null)
			return;
		List<String> locations = onlineUser.getPlayerData().getPistonLocations();

		if (locations != null) {
			for (String stringLoc : locations) {
				Location loc = StringUtils.deserializeLoc(stringLoc);
				if (loc == null) {
					LogUtils.warn(String.format("Unknown piston location under UUID: %s: ", id) + stringLoc);
					continue;
				}
				World world = loc.getWorld();
				if (world == null) {
					LogUtils.warn(String.format("Unknown piston world under UUID: %s: ", id) + stringLoc);
					continue;
				}
				if (loc.getWorld().getBlockAt(loc).getType() != Material.PISTON)
					continue;
				genManager.getKnownGenPistons().remove(loc);
				GenPiston piston = new GenPiston(loc, id);
				piston.setHasBeenUsed(true);
				genManager.addKnownGenPiston(piston);
				onlineUser.getPlayerData().setPistonLocations(new ArrayList<>());
			}
		}
	}
}