package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.events.generator.GeneratorGenerateEvent;
import com.swiftlicious.hellblock.events.generator.PlayerBreakGeneratedBlock;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.generator.GenBlock;
import com.swiftlicious.hellblock.listeners.generator.GenMode;
import com.swiftlicious.hellblock.listeners.generator.GenPiston;
import com.swiftlicious.hellblock.listeners.generator.GeneratorManager;
import com.swiftlicious.hellblock.listeners.generator.GeneratorModeManager;
import com.swiftlicious.hellblock.listeners.rain.LavaRainTask;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class NetherGeneratorHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;
	private final GeneratorManager genManager;
	private final GeneratorModeManager genModeManager;

	private static final BlockFace[] FACES = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
			BlockFace.WEST };

	private final Map<UUID, Double> islandGeneratorBonusCache = new ConcurrentHashMap<>();

	private final Set<LocationKey> playerPlacedGenBlocks = ConcurrentHashMap.newKeySet();

	public NetherGeneratorHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
	}

	@Override
	public void load() {
		this.genModeManager.loadFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		islandGeneratorBonusCache.clear();
	}

	public GeneratorManager getGeneratorManager() {
		return this.genManager;
	}

	public GeneratorModeManager getGeneratorModeManager() {
		return this.genModeManager;
	}

	// Add this field to allow deterministic testing if desired (can be injected via
	// constructor)
	private final DoubleSupplier randomSupplier = Math::random;

	// Add this field somewhere at the top of the listener:
	private static final double FLOW_DIRECTION_THRESHOLD = 0.15; // lower = looser, higher = stricter
	private static final int GENERATOR_NEIGHBORHOOD_RADIUS = 3; // search +/- 3 blocks in X/Z, Y +/- 1

	@EventHandler
	public void onNetherrackGeneration(BlockFromToEvent event) {
		final Block fromBlock = event.getBlock();
		final World world = fromBlock.getWorld();
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return;
		}

		final BlockFace face = event.getFace();
		if (!Arrays.asList(FACES).contains(face)) {
			return;
		}

		final GenMode mode = genModeManager.getGenMode();
		final Block toBlock = event.getToBlock();
		if (toBlock == null) {
			return;
		}

		// Target must be air
		if (!toBlock.getType().isAir()) {
			return;
		}

		// Avoid generation in lava pools
		if (isLavaPool(toBlock.getLocation())) {
			return;
		}

		// The new, strict collision check:
		// Two lava blocks whose flow (or source) will reach the `toBlock`, and there is
		// only AIR between each lava block and the target.
		if (!isValidTwoLavaCollision(toBlock)) {
			return;
		}

		final Location l = toBlock.getLocation();
		final LocationKey lk = new LocationKey(l);
		if (l.getWorld() == null) {
			return;
		}

		// --- existing logic to track known gen locations & players nearby ---
		if (!genManager.isGenLocationKnown(lk) && mode.isSearchingForPlayersNearby()) {
			final double radius = instance.getConfigManager().searchRadius();
			final Collection<Entity> playersNearby = l.getWorld().getNearbyEntities(l, radius, radius, radius).stream()
					.filter(e -> e.getType() == EntityType.PLAYER).toList();
			final Player closestPlayer = getClosestPlayer(l, playersNearby);
			if (closestPlayer != null) {
				genManager.addKnownGenLocation(lk);
				genManager.setPlayerForLocation(closestPlayer.getUniqueId(), lk, false);
			}
		}

		if (!genManager.isGenLocationKnown(lk)) {
			genManager.addKnownGenLocation(lk);
			return;
		}

		// If known but nobody previously broke it -> nothing to generate
		if (!genManager.getGenBreaks().containsKey(lk)) {
			return;
		}

		final GenBlock gb = genManager.getGenBreaks().get(lk);
		if (gb.hasExpired()) {
			instance.debug("GB has expired %s".formatted(gb.getLocation()));
			genManager.removeKnownGenLocation(lk);
			return;
		}

		final UUID uuid = gb.getUUID();

		// respect lava rain mode
		if (!mode.canGenerateWhileLavaRaining()) {
			final Optional<LavaRainTask> lavaRain = instance.getLavaRainHandler().getLavaRainingWorlds().stream()
					.filter(task -> l.getWorld().getName().equalsIgnoreCase(task.getWorld().worldName())).findAny();
			if (lavaRain.isPresent() && lavaRain.get().isLavaRaining()) {
				event.setCancelled(true);
				if (l.getBlock().getType() != mode.getFallbackMaterial()) {
					l.getBlock().setType(mode.getFallbackMaterial());
				}
				return;
			}
		}

		final float soundVolume = 2F;
		final float pitch = 1F;

		final Player owner = Bukkit.getPlayer(uuid);
		final Context<Player> context = owner != null ? Context.player(owner) : Context.empty();

		getResultsAsync(context, l, results -> {
			AtomicReference<Material> chosenRef = new AtomicReference<>(chooseRandomResult(results));

			if (chosenRef.get() == null) {
				instance.getPluginLogger()
						.severe("No generation results found for location, falling back to fallback material: " + l);
				chosenRef.set(mode.getFallbackMaterial());
			}

			instance.getScheduler().executeSync(() -> {
				final GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, chosenRef.get(), uuid, l);
				Bukkit.getPluginManager().callEvent(genEvent);
				if (genEvent.isCancelled()) {
					return;
				}

				event.setCancelled(true);
				genEvent.getGenerationLocation().getBlock().setType(genEvent.getResult());

				// sound & particles
				if (mode.hasGenSound()) {
					final double radius = instance.getConfigManager().searchRadius();
					l.getWorld().getNearbyEntities(l, radius, radius, radius).stream()
							.filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
							.forEach(player -> AdventureHelper.playSound(
									instance.getSenderFactory().getAudience(player),
									Sound.sound(Key.key(mode.getGenSound()), Source.AMBIENT, soundVolume, pitch)));
				}

				if (mode.hasParticleEffect()) {
					mode.displayGenerationParticles(l);
				}
			});
		});
	}

	/**
	 * Returns true if exactly two lava blocks (source or flowing) have a clear AIR
	 * path into `toBlock`. Allows direct and diagonal generators. Avoids lava pools
	 * by requiring exactly 2.
	 */
	private boolean isValidTwoLavaCollision(@NotNull Block toBlock) {
		final Location loc = toBlock.getLocation();
		final World world = loc.getWorld();
		if (world == null) {
			return false;
		}

		final int centerX = loc.getBlockX();
		final int centerY = loc.getBlockY();
		final int centerZ = loc.getBlockZ();

		final List<Block> candidates = new ArrayList<>();

		for (int x = centerX - GENERATOR_NEIGHBORHOOD_RADIUS; x <= centerX + GENERATOR_NEIGHBORHOOD_RADIUS; x++) {
			for (int y = centerY - 1; y <= centerY + 1; y++) {
				for (int z = centerZ - GENERATOR_NEIGHBORHOOD_RADIUS; z <= centerZ
						+ GENERATOR_NEIGHBORHOOD_RADIUS; z++) {
					if (x == centerX && y == centerY && z == centerZ) {
						continue;
					}

					final Block b = world.getBlockAt(x, y, z);
					if (!(isSource(b) || isFlowing(b))) {
						continue;
					}

					if (hasClearPath(b, toBlock)) {
						// For flowing lava, check flow direction roughly points toward target
						if (isFlowing(b)) {
							final Vector flowDir = getFlowDirection(b);
							if (flowDir != null) {
								final Vector toTarget = new Vector(centerX + 0.5 - (x + 0.5), centerY + 0.5 - (y + 0.5),
										centerZ + 0.5 - (z + 0.5));
								final double dot = flowDir.normalize().dot(toTarget.normalize());
								if (dot < FLOW_DIRECTION_THRESHOLD) {
									continue;
								}
							}
						}
						candidates.add(b);
					}
				}
			}
		}

		// Only generate when exactly 2 lava sources are valid
		return candidates.size() == 2;
	}

	/**
	 * Checks that the path between a lava block and the target block consists only
	 * of AIR.
	 */
	private boolean hasClearPath(@NotNull Block lava, @NotNull Block target) {
		final Location lavaLoc = lava.getLocation();
		final Location targetLoc = target.getLocation();
		final World world = lavaLoc.getWorld();
		if (world == null) {
			return false;
		}

		final int dx = targetLoc.getBlockX() - lavaLoc.getBlockX();
		final int dy = targetLoc.getBlockY() - lavaLoc.getBlockY();
		final int dz = targetLoc.getBlockZ() - lavaLoc.getBlockZ();

		final int stepX = Integer.signum(dx);
		final int stepY = Integer.signum(dy);
		final int stepZ = Integer.signum(dz);

		int curX = lavaLoc.getBlockX();
		int curY = lavaLoc.getBlockY();
		int curZ = lavaLoc.getBlockZ();

		while (true) {
			curX += stepX;
			curY += stepY;
			curZ += stepZ;
			if (curX == targetLoc.getBlockX() && curY == targetLoc.getBlockY() && curZ == targetLoc.getBlockZ()) {
				break;
			}
			final Block intermediate = world.getBlockAt(curX, curY, curZ);
			if (!intermediate.getType().isAir()) {
				return false;
			}
		}
		return true;
	}

	public @Nullable Player getClosestPlayer(Location l, Collection<Entity> playersNearby) {
		Player closestPlayer = null;
		double closestDistance = Double.POSITIVE_INFINITY;
		for (Entity entity : playersNearby) {
			if (!(entity instanceof Player player)) {
				continue;
			}
			final double distance = l.distance(player.getLocation());
			if (distance < closestDistance) {
				closestPlayer = player;
				closestDistance = distance;
			}
		}
		return closestPlayer;
	}

	@EventHandler
	public void onBlockChange(EntityChangeBlockEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.FALLING_BLOCK
				|| !(event.getTo() == Material.AIR || event.getTo() == Material.LAVA)) {
			return;
		}

		final Location loc = event.getBlock().getLocation();
		final LocationKey locKey = new LocationKey(loc);
		if (!genManager.isGenLocationKnown(locKey)) {
			return;
		}
		event.setCancelled(true);
		event.getBlock().getState().update(false, false);
	}

	@EventHandler
	public void onPistonPush(BlockPistonExtendEvent event) {
		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}
		final LocationKey locKey = new LocationKey(event.getBlock().getLocation());
		if (!genManager.getKnownGenPistons().containsKey(locKey)) {
			return;
		}
		final GenPiston piston = genManager.getKnownGenPistons().get(locKey);
		final Location genBlockLoc = event.getBlock().getRelative(event.getDirection()).getLocation();
		final LocationKey genLocKey = new LocationKey(genBlockLoc);
		if (!genManager.isGenLocationKnown(genLocKey)) {
			return;
		}
		piston.setHasBeenUsed(true);
		genManager.setPlayerForLocation(piston.getUUID(), genLocKey, true);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		final Location location = event.getBlock().getLocation();
		final LocationKey locKey = new LocationKey(location);

		// track if placed in a generator location
		if (genManager.isGenLocationKnown(locKey)) {
			playerPlacedGenBlocks.add(locKey);
		}

		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}
		if (event.getBlock().getType() != Material.PISTON) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.isOnline()) {
			return;
		}

		final UUID uuid = player.getUniqueId();
		final GenPiston piston = new GenPiston(locKey, uuid);
		genManager.addKnownGenPiston(piston);
	}

	@EventHandler
	public void onGeneratedBlockBreak(BlockBreakEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}
		final Location location = event.getBlock().getLocation();
		final LocationKey locKey = new LocationKey(location);
		final Player player = event.getPlayer();

		// ignore piston cleanup
		if (genManager.getKnownGenPistons().containsKey(locKey)) {
			genManager.getKnownGenPistons().remove(locKey);
			return;
		}

		if (location.getWorld() == null) {
			return;
		}
		if (!genManager.isGenLocationKnown(locKey)) {
			return;
		}

		// Prevent abuse: skip progression if block was manually placed
		if (playerPlacedGenBlocks.contains(locKey)) {
			playerPlacedGenBlocks.remove(locKey); // cleanup
			return;
		}

		final PlayerBreakGeneratedBlock genEvent = new PlayerBreakGeneratedBlock(player, location);
		Bukkit.getPluginManager().callEvent(genEvent);
		if (genEvent.isCancelled()) {
			return;
		}

		genManager.setPlayerForLocation(player.getUniqueId(), locKey, false);
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(user -> instance
				.getChallengeManager().handleChallengeProgression(player, ActionType.BREAK, event.getBlock()));
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}
		for (Block block : event.blockList()) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
				continue;
			}
			if (block.getType() != Material.PISTON) {
				continue;
			}

			final Location location = block.getLocation();
			final LocationKey locKey = new LocationKey(location);
			if (genManager.getKnownGenPistons().containsKey(locKey)) {
				genManager.getKnownGenPistons().remove(locKey);
				return;
			}
		}
	}

	private boolean isSource(@NotNull Block block) {
		final FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		final boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		return isLava && lava.isSource();
	}

	private boolean isFlowing(@NotNull Block block) {
		final FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		final boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		return isLava && (!(lava instanceof FallingFluidData)) && !lava.isSource();
	}

	private @Nullable Vector getFlowDirection(@NotNull Block block) {
		final FluidData lava = VersionHelper.getNMSManager().getFluidData(block.getLocation());
		final boolean isLava = lava.getFluidType() == Fluid.LAVA || lava.getFluidType() == Fluid.FLOWING_LAVA;
		if (isLava) {
			return lava.computeFlowDirection(block.getLocation());
		}

		return null;
	}

	private boolean isLavaPool(@NotNull Location location) {
		if (location.getWorld() == null) {
			return false;
		}
		int lavaCount = 0;
		final int centerX = location.getBlockX();
		final int centerY = location.getBlockY();
		final int centerZ = location.getBlockZ();
		for (int x = centerX - 2; x <= centerX + 2; x++) {
			for (int y = centerY - 1; y <= centerY + 1; y++) {
				for (int z = centerZ - 2; z <= centerZ + 2; z++) {
					final Block b = location.getWorld().getBlockAt(x, y, z);
					if (b.getType() == Material.LAVA && ++lavaCount > 4) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public @NotNull Map<Material, Double> getResults(@NotNull Context<Player> context) {
		final Map<Material, Double> results = new HashMap<>();
		for (Map.Entry<Material, MathValue<Player>> result : instance.getConfigManager().generationResults()
				.entrySet()) {
			final Material type = result.getKey();
			if (type == null || type == Material.AIR) {
				continue;
			}
			results.put(type, result.getValue().evaluate(context));
		}
		return results;
	}

	public void getResultsAsync(@NotNull Context<Player> context, @NotNull Location generatorLocation,
			@NotNull Consumer<Map<Material, Double>> callback) {
		Player player = context.holder();
		if (player == null) {
			callback.accept(getResults(context)); // fallback
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(generatorLocation.getBlock()).thenAcceptAsync(ownerUUID -> {
			if (ownerUUID == null) {
				callback.accept(getResults(context)); // not on an island
				return;
			}

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						// Build base generation map first
						Map<Material, Double> results = new HashMap<>();
						for (Map.Entry<Material, MathValue<Player>> entry : instance.getConfigManager()
								.generationResults().entrySet()) {
							Material mat = entry.getKey();
							if (mat == null || mat == Material.AIR)
								continue;

							double baseChance = entry.getValue().evaluate(context);
							results.put(mat, baseChance);
						}

						if (userDataOpt.isEmpty()) {
							callback.accept(results);
							return;
						}

						HellblockData hellblockData = userDataOpt.get().getHellblockData();
						Set<UUID> partyPlusOwner = hellblockData.getPartyPlusOwner();
						BoundingBox box = hellblockData.getBoundingBox();

						boolean isMember = partyPlusOwner.contains(player.getUniqueId());
						boolean isInsideIsland = box != null && box.contains(generatorLocation.toVector());

						if (!(isMember && isInsideIsland)) {
							// Player not in their own island or outside boundary → no bonus
							callback.accept(results);
							return;
						}

						// Player is inside their island → calculate total generator bonus
						double totalBonus = getCachedGeneratorBonus(hellblockData);

						// Apply only to blocks with base value under 10
						results.entrySet().forEach(entry -> {
							Material material = entry.getKey();
							double base = entry.getValue();

							if (base < 10.0) {
								results.put(material, base + totalBonus);
							}
						});

						callback.accept(results);
					});
		});
	}

	public double getCachedGeneratorBonus(@NotNull HellblockData data) {
		UUID ownerUUID = data.getOwnerUUID();

		// If not cached yet, calculate and store
		return islandGeneratorBonusCache.computeIfAbsent(ownerUUID, id -> calculateGeneratorBonus(data));
	}

	public void updateGeneratorBonusCache(@NotNull HellblockData data) {
		islandGeneratorBonusCache.put(data.getOwnerUUID(), calculateGeneratorBonus(data));
	}

	public void invalidateGeneratorBonusCache(@NotNull UUID ownerUUID) {
		islandGeneratorBonusCache.remove(ownerUUID);
	}

	private double calculateGeneratorBonus(@NotNull HellblockData data) {
		int currentLevel = data.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE);
		double total = 0.0;

		for (int i = 0; i <= currentLevel; i++) {
			UpgradeTier tier = instance.getUpgradeManager().getTier(i);
			if (tier == null) {
				continue;
			}

			UpgradeData upgrade = tier.getUpgrade(IslandUpgradeType.GENERATOR_CHANCE);
			if (upgrade != null && upgrade.getValue() != null) {
				total += upgrade.getValue().doubleValue();
			}
		}

		return total;
	}

	/**
	 * Improved deterministic-friendly random chooser for generation results. Caches
	 * getResults(context) once.
	 */
	private @Nullable Material chooseRandomResult(@NotNull Map<Material, Double> results) {
		double total = 0.0;
		for (Double v : results.values()) {
			if (v != null && v > 0) {
				total += v;
			}
		}
		if (total <= 0.0) {
			return null;
		}

		final double r = randomSupplier.getAsDouble() * total;
		double cumulative = 0.0;
		for (Map.Entry<Material, Double> e : results.entrySet()) {
			final double weight = e.getValue() != null ? e.getValue() : 0.0;
			cumulative += weight;
			if (r <= cumulative) {
				return e.getKey();
			}
		}
		// fallback to last material
		return results.keySet().stream().reduce((first, second) -> second).orElse(null);
	}

	public void savePistons(@NotNull UUID id) {
		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}
		final GenPiston[] generatedPistons = genManager.getGenPistonsByUUID(id);

		if (!(generatedPistons != null && generatedPistons.length > 0)) {
			return;
		}
		final List<String> locations = new ArrayList<>();
		for (GenPiston piston : generatedPistons) {
			if (piston == null || piston.getLoc() == null || !piston.hasBeenUsed()
					|| piston.getLoc().getBlock().getType() != Material.PISTON) {
				continue;
			}

			final String serializedLoc = StringUtils.serializeLoc(piston.getLoc().asLocation());
			if (!locations.contains(serializedLoc)) {
				locations.add(serializedLoc);
			}
		}
		if (locations.isEmpty()) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty()) {
			return;
		}
		onlineUser.get().getLocationCacheData().setPistonLocations(locations);
	}

	public void loadPistons(@NotNull UUID id) {
		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty()) {
			return;
		}
		final List<String> locations = onlineUser.get().getLocationCacheData().getPistonLocations();

		if (locations != null) {
			for (String stringLoc : locations) {
				final Location loc = StringUtils.deserializeLoc(stringLoc);
				if (loc == null) {
					instance.getPluginLogger()
							.warn("Unknown piston location under UUID: %s: ".formatted(id) + stringLoc);
					continue;
				}
				final World world = loc.getWorld();
				if (world == null) {
					instance.getPluginLogger().warn("Unknown piston world under UUID: %s: ".formatted(id) + stringLoc);
					continue;
				}
				if (loc.getWorld().getBlockAt(loc).getType() != Material.PISTON) {
					continue;
				}
				final LocationKey locKey = new LocationKey(loc);
				genManager.getKnownGenPistons().remove(locKey);
				final GenPiston piston = new GenPiston(locKey, id);
				piston.setHasBeenUsed(true);
				genManager.addKnownGenPiston(piston);
				onlineUser.get().getLocationCacheData().setPistonLocations(new ArrayList<>());
			}
		}
	}

	public final class LocationKey {
		private final String world;
		private final int x;
		private final int y;
		private final int z;

		public LocationKey(@NotNull Location loc) {
			this.world = loc.getWorld() != null ? loc.getWorld().getName() : "";
			this.x = loc.getBlockX();
			this.y = loc.getBlockY();
			this.z = loc.getBlockZ();
		}

		public LocationKey(String world, int x, int y, int z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public @Nullable Block getBlock() {
			final World bukkitWorld = Bukkit.getWorld(world);
			if (bukkitWorld == null) {
				return null;
			}
			return bukkitWorld.getBlockAt(x, y, z);
		}

		public @Nullable Location asLocation() {
			final World bukkitWorld = Bukkit.getWorld(world);
			if (bukkitWorld == null) {
				return null;
			}
			return new Location(bukkitWorld, x, y, z);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof LocationKey k)) {
				return false;
			}
			return x == k.x && y == k.y && z == k.z && Objects.equals(world, k.world);
		}

		@Override
		public int hashCode() {
			return Objects.hash(world, x, y, z);
		}

		@Override
		public String toString() {
			return world + ":" + x + "," + y + "," + z;
		}
	}
}