package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.events.generator.GeneratorGenerateEvent;
import com.swiftlicious.hellblock.events.generator.PlayerBreakGeneratedBlock;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.generator.GenBlock;
import com.swiftlicious.hellblock.listeners.generator.GenMode;
import com.swiftlicious.hellblock.listeners.generator.GenPiston;
import com.swiftlicious.hellblock.listeners.generator.GeneratorManager;
import com.swiftlicious.hellblock.listeners.generator.GeneratorModeManager;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class NetherGeneratorHandler implements Listener {

	protected final HellblockPlugin instance;
	private final GeneratorManager genManager;
	private final GeneratorModeManager genModeManager;

	private final static BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	public NetherGeneratorHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
		this.genModeManager.loadFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public GeneratorManager getGeneratorManager() {
		return this.genManager;
	}

	public GeneratorModeManager getGeneratorModeManager() {
		return this.genModeManager;
	}

	@EventHandler
	public void onNetherrackGeneration(BlockFromToEvent event) {
		Block fromBlock = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(fromBlock.getWorld()))
			return;

		BlockFace face = event.getFace();
		if (!Arrays.asList(FACES).contains(face)) {
			return;
		}

		GenMode mode = genModeManager.getGenMode();
		Block toBlock = event.getToBlock();
		Material toBlockMaterial = toBlock.getType();
		boolean normalGenerator = isFlowing(fromBlock.getRelative(face, 2)) || isSource(fromBlock.getRelative(face, 2));
		boolean diagonalGenerator = isFlowing(toBlock.getRelative(face));

		if (isSource(fromBlock) || isFlowing(fromBlock)) {
			// TODO: make other positioned generators work
			if (toBlockMaterial.isAir() && !isLavaPool(toBlock.getLocation())
					&& (normalGenerator || diagonalGenerator)) {
				Location l = toBlock.getLocation();
				if (l.getWorld() == null)
					return;
				// Checks if the block has been broken before and if it is a known gen location
				if (!genManager.isGenLocationKnown(l) && mode.isSearchingForPlayersNearby()) {
					Collection<Entity> playersNearby = l.getWorld()
							.getNearbyEntities(l, instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius(),
									instance.getConfigManager().searchRadius())
							.stream().filter(e -> e.getType() == EntityType.PLAYER).toList();
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

					if (!(mode.canGenerateWhileLavaRaining())) {
						Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
								.filter(task -> toBlock.getWorld().getName()
										.equalsIgnoreCase(task.getWorld().worldName()))
								.findAny();
						if (lavaRain.isPresent() && lavaRain.get().isLavaRaining()) {
							event.setCancelled(true);
							if (toBlock.getLocation().getBlock().getType() != mode.getFallbackMaterial())
								toBlock.getLocation().getBlock().setType(mode.getFallbackMaterial());
							return;
						}
					}

					float soundVolume = 2F;
					float pitch = 1F;
					Material result = null;
					Context<Player> context = Context.player(Bukkit.getPlayer(uuid));

					if (getRandomResult(context) != null) {
						result = getRandomResult(context);
					} else if (mode.hasFallBackMaterial()) {
						result = mode.getFallbackMaterial();
					} else {
						result = getResults(context).keySet().iterator().next();
					}

					GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, result, uuid,
							toBlock.getLocation());
					Bukkit.getPluginManager().callEvent(genEvent);
					if (genEvent.isCancelled())
						return;
					event.setCancelled(true);
					if (genEvent.getResult() == null) {
						instance.getPluginLogger().severe(String.format("Unknown material: %s.", result.toString()));
						return;
					}
					genEvent.getGenerationLocation().getBlock().setType(genEvent.getResult());

					if (mode.hasGenSound()) {
						for (Entity entity : l.getWorld()
								.getNearbyEntities(l, instance.getConfigManager().searchRadius(),
										instance.getConfigManager().searchRadius(),
										instance.getConfigManager().searchRadius())
								.stream().filter(e -> e.getType() == EntityType.PLAYER).toList()) {
							if (entity instanceof Player player) {
								Audience audience = instance.getSenderFactory().getAudience(player);
								audience.playSound(
										Sound.sound(Key.key(mode.getGenSound()), Source.AMBIENT, soundVolume, pitch));
							}
						}
					}

					if (mode.hasParticleEffect())
						mode.displayGenerationParticles(l);
				} else {
					genManager.addKnownGenLocation(l);
					return;
				}
			}
		}
	}

	public @Nullable Player getClosestPlayer(Location l, Collection<Entity> playersNearby) {
		Player closestPlayer = null;
		double closestDistance = 100D;
		for (Entity entity : playersNearby) {
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
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld()))
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
		if (!instance.getConfigManager().pistonAutomation())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld()))
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
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld()))
			return;

		Location location = event.getBlock().getLocation();
		if (location.getWorld() == null)
			return;
		if (genManager.isGenLocationKnown(location)) {
			genManager.getKnownGenLocations().remove(location);
		}

		if (!instance.getConfigManager().pistonAutomation())
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
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld()))
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
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
					|| !onlineUser.get().getHellblockData().hasHellblock())
				return;
			if (!onlineUser.get().getChallengeData()
					.isChallengeActive(instance.getChallengeManager().getByActionType(ActionType.BREAK))
					&& !onlineUser.get().getChallengeData()
							.isChallengeCompleted(instance.getChallengeManager().getByActionType(ActionType.BREAK))) {
				onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
						instance.getChallengeManager().getByActionType(ActionType.BREAK));
			} else {
				onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
						instance.getChallengeManager().getByActionType(ActionType.BREAK), 1);
				if (onlineUser.get().getChallengeData()
						.isChallengeCompleted(instance.getChallengeManager().getByActionType(ActionType.BREAK))) {
					onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.BREAK));
				}
			}
			genManager.setPlayerForLocation(player.getUniqueId(), location, false);
		}
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!instance.getConfigManager().pistonAutomation())
			return;
		for (Block block : event.blockList()) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;
			if (block.getType() != Material.PISTON)
				continue;

			Location location = block.getLocation();
			if (genManager.getKnownGenPistons().containsKey(location)) {
				genManager.getKnownGenPistons().remove(location);
				return;
			}
		}
	}

	private boolean isSource(@NotNull Block block) {
		FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		boolean isLavaSource = isLava && lava.isSource();
		return isLavaSource;
	}

	private boolean isFlowing(@NotNull Block block) {
		FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		boolean isLavaFlowing = isLava && (!(lava instanceof FallingFluidData)) && !lava.isSource();
		return isLavaFlowing;
	}

	private @Nullable Vector getFlowDirection(@NotNull Block block) {
		FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		if (isLava) {
			return lava.computeFlowDirection(block.getLocation());
		}

		return null;
	}

	private boolean isLavaPool(@NotNull Location location) {
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

	public @NotNull Map<Material, Double> getResults(@NotNull Context<Player> context) {
		Map<Material, Double> results = new HashMap<>();
		for (Map.Entry<Material, MathValue<Player>> result : instance.getConfigManager().generationResults()
				.entrySet()) {
			Material type = result.getKey();
			if (type == null || type == Material.AIR)
				continue;
			results.put(type, result.getValue().evaluate(context));
		}
		return results;
	}

	public @Nullable Material getRandomResult(@NotNull Context<Player> context) {
		double r = Math.random() * 100;
		double prev = 0;
		for (Material m : this.getResults(context).keySet()) {
			double chance = this.getResults(context).get(m) + prev;
			if (r > prev && r <= chance)
				return m;
			else
				prev = chance;
			continue;
		}
		return null;
	}

	public void savePistons(@NotNull UUID id) {
		if (!instance.getConfigManager().pistonAutomation())
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
				Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
				if (onlineUser.isEmpty())
					return;
				onlineUser.get().getLocationCacheData().setPistonLocations(locations);
			}
		}
	}

	public void loadPistons(@NotNull UUID id) {
		if (!instance.getConfigManager().pistonAutomation())
			return;
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;
		List<String> locations = onlineUser.get().getLocationCacheData().getPistonLocations();

		if (locations != null) {
			for (String stringLoc : locations) {
				Location loc = StringUtils.deserializeLoc(stringLoc);
				if (loc == null) {
					instance.getPluginLogger()
							.warn(String.format("Unknown piston location under UUID: %s: ", id) + stringLoc);
					continue;
				}
				World world = loc.getWorld();
				if (world == null) {
					instance.getPluginLogger()
							.warn(String.format("Unknown piston world under UUID: %s: ", id) + stringLoc);
					continue;
				}
				if (loc.getWorld().getBlockAt(loc).getType() != Material.PISTON)
					continue;
				genManager.getKnownGenPistons().remove(loc);
				GenPiston piston = new GenPiston(loc, id);
				piston.setHasBeenUsed(true);
				genManager.addKnownGenPiston(piston);
				onlineUser.get().getLocationCacheData().setPistonLocations(new ArrayList<>());
			}
		}
	}
}