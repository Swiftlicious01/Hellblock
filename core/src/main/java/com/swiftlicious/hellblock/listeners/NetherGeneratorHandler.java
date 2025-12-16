package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

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
import com.swiftlicious.hellblock.listeners.weather.WeatherType;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockTypes;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class NetherGeneratorHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;
	private GeneratorManager genManager;
	private GeneratorModeManager genModeManager;

	// for deterministic testing
	private final DoubleSupplier randomSupplier = Math::random;

	private static final double FLOW_DIRECTION_THRESHOLD = 0.15; // lower = looser, higher = stricter
	private static final int GENERATOR_NEIGHBORHOOD_RADIUS = 3; // search +/- 3 blocks in X/Z, Y +/- 1

	private SchedulerTask cleanupTask = null;

	private final Map<Integer, Double> islandGeneratorBonusCache = new ConcurrentHashMap<>();

	private final Set<Pos3> playerPlacedGenBlocks = ConcurrentHashMap.newKeySet();

	private final Set<Integer> loadedIslandPistonCaches = ConcurrentHashMap.newKeySet();

	private static final Set<String> LAVA_KEY = Set.of("minecraft:lava");

	private final Map<Pos3, Long> lastGenCheck = new ConcurrentHashMap<>();
	private static final long GEN_THROTTLE_TICKS = 5; // e.g. 5 ticks = 0.25s

	private static final int NO_CLEANUP_LOG_INTERVAL = 10; // log only every 10 no-cleanup runs
	private final AtomicInteger consecutiveEmptyRuns = new AtomicInteger(0);
	private volatile boolean wasSilent = false;

	public NetherGeneratorHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		this.genManager = new GeneratorManager();
		this.genModeManager = new GeneratorModeManager(instance);
		this.genModeManager.loadFromConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
		cleanupPistonsTask();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.islandGeneratorBonusCache.clear();
		this.loadedIslandPistonCaches.clear();
		if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
			this.cleanupTask.cancel();
			this.cleanupTask = null;
		}
		this.consecutiveEmptyRuns.set(0);
		this.wasSilent = false;
		this.genManager = null;
		this.genModeManager = null;
	}

	private void cleanupPistonsTask() {
		this.cleanupTask = instance.getScheduler().sync().runRepeating(() -> {
			Collection<HellblockWorld<?>> worlds = instance.getWorldManager().getManagedWorlds();
			List<CompletableFuture<Pair<HellblockWorld<?>, Boolean>>> futures = new ArrayList<>();

			for (HellblockWorld<?> world : worlds) {
				if (world == null || world.bukkitWorld() == null)
					continue;

				CompletableFuture<Pair<HellblockWorld<?>, Boolean>> future = getGeneratorManager()
						.cleanupAllExpiredPistons(world).thenApply(cleaned -> Pair.of(world, cleaned));

				futures.add(future);
			}

			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
				List<Pair<HellblockWorld<?>, Boolean>> results = futures.stream().map(CompletableFuture::join)
						.collect(Collectors.toList());

				List<HellblockWorld<?>> cleanedWorlds = results.stream().filter(Pair::right).map(Pair::left)
						.collect(Collectors.toList());

				if (!cleanedWorlds.isEmpty()) {
					// Reset counters and log immediately
					int emptyRuns = consecutiveEmptyRuns.getAndSet(0);
					boolean wasQuiet = wasSilent;
					wasSilent = false;

					String worldNames = cleanedWorlds.stream().map(HellblockWorld::worldName)
							.collect(Collectors.joining(", "));

					if (wasQuiet && emptyRuns > 0) {
						instance.debug("Piston cleanup resumed after " + emptyRuns + " quiet run"
								+ (emptyRuns == 1 ? "" : "s") + ".");
					}

					instance.debug("Global piston cleanup completed: expired pistons removed in " + cleanedWorlds.size()
							+ " world" + (cleanedWorlds.size() == 1 ? "" : "s") + ": " + worldNames);
				} else {
					int count = consecutiveEmptyRuns.incrementAndGet();
					if (count % NO_CLEANUP_LOG_INTERVAL == 0) {
						instance.debug("Global piston cleanup: no expired pistons found (x" + count + " in a row)");
						wasSilent = true;
					}
				}
			});
		}, 30 * 60 * 20, 30 * 60 * 20, LocationUtils.getAnyLocationInstance());
	}

	public GeneratorManager getGeneratorManager() {
		return this.genManager;
	}

	public GeneratorModeManager getGeneratorModeManager() {
		return this.genModeManager;
	}

	@EventHandler(ignoreCancelled = true)
	public void onNetherrackGeneration(BlockFromToEvent event) {
		final Block fromBlock = event.getBlock();
		final World world = fromBlock.getWorld();
		if (!instance.getHellblockHandler().isInCorrectWorld(world)) {
			return;
		}

		if (!fromBlock.isLiquid()) {
			return;
		}

		final BlockFace face = event.getFace();
		if (!Arrays.asList(FarmingHandler.FACES).contains(face)) {
			return;
		}

		final GenMode mode = getGeneratorModeManager().getGenMode();
		final Block toBlock = event.getToBlock();
		// Target must be air
		if (!toBlock.getType().isAir()) {
			return;
		}

		final Location loc = toBlock.getLocation();
		if (loc.getWorld() == null) {
			return;
		}

		final Integer islandId = instance.getPlacementDetector().getIslandIdAt(loc);
		if (islandId == null) {
			return; // Outside of any known island
		}

		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			return;
		}

		final HellblockWorld<?> hellWorld = worldOpt.get();
		final Pos3 pos = Pos3.from(loc);

		if (!shouldProcessGenerationBlock(pos))
			return;

		// Run async checks for lava pool, valid collision, and sign presence
		CompletableFuture<Boolean> validGenFuture = isLavaPool(hellWorld, pos)
				.thenCombine(isValidTwoLavaCollision(hellWorld, pos),
						(isPool, validCollision) -> !isPool && validCollision)
				.thenCombine(hasNearbySign(hellWorld, pos), (lavaValid, hasSign) -> lavaValid && hasSign);

		validGenFuture.thenAccept(valid -> {
			if (!valid)
				return;

			// Continue logic on the Bukkit main thread
			instance.getScheduler().executeSync(() -> {
				// --- existing logic to track known gen locations & players nearby ---
				if (!getGeneratorManager().isGenLocationKnown(pos) && mode.isSearchingForPlayersNearby()) {
					final double radius = instance.getConfigManager().searchRadius();
					final Collection<Entity> playersNearby = loc.getWorld()
							.getNearbyEntities(loc, radius, radius, radius).stream()
							.filter(e -> e.getType() == EntityType.PLAYER).toList();
					final Player closestPlayer = getClosestPlayer(loc, playersNearby);
					if (closestPlayer != null) {
						getGeneratorManager().addKnownGenPosition(pos);
						getGeneratorManager().setPlayerForLocation(closestPlayer.getUniqueId(), pos, false);
					}
				}

				if (!getGeneratorManager().isGenLocationKnown(pos)) {
					getGeneratorManager().addKnownGenPosition(pos);
					return;
				}

				// If known but nobody previously broke it -> nothing to generate
				if (!getGeneratorManager().getGenBreaks().containsKey(pos)) {
					return;
				}

				final GenBlock gb = getGeneratorManager().getGenBreaks().get(pos);
				if (gb.hasExpired()) {
					instance.debug("GB has expired %s".formatted(gb.getPosition().toLocation(loc.getWorld())));
					getGeneratorManager().removeKnownGenPosition(pos);
					return;
				}

				final UUID uuid = gb.getUUID();

				// respect lava rain mode
				if (!mode.canGenerateWhileLavaRaining()) {
					if (instance.getNetherWeatherManager().isWeatherActive(islandId, WeatherType.LAVA_RAIN)) {
						event.setCancelled(true);

						CustomBlockState fallbackState = CustomBlockTypes.fromMaterial(mode.getFallbackMaterial())
								.createBlockState();

						hellWorld.updateBlockState(pos, fallbackState).exceptionally(ex -> {
							Block block = loc.getBlock();
							if (block.getType() != mode.getFallbackMaterial()) {
								block.setType(mode.getFallbackMaterial());
							}
							return null;
						});
						return;
					}
				}

				final float soundVolume = 2F;
				final float pitch = 1F;

				final Player owner = Bukkit.getPlayer(uuid);
				final Context<Player> context = owner != null ? Context.player(owner) : Context.playerEmpty();

				processGenerationResults(context, loc, results -> {
					AtomicReference<Material> chosenRef = new AtomicReference<>(chooseRandomResult(results));

					if (chosenRef.get() == null) {
						instance.getPluginLogger().severe(
								"No generation results found for location, falling back to fallback material: " + loc);
						chosenRef.set(mode.getFallbackMaterial());
					}

					instance.getScheduler().executeSync(() -> {
						final GeneratorGenerateEvent genEvent = new GeneratorGenerateEvent(mode, chosenRef.get(), uuid,
								loc);
						if (EventUtils.fireAndCheckCancel(genEvent)) {
							return;
						}

						event.setCancelled(true);
						final Pos3 genPos = Pos3.from(genEvent.getGenerationLocation());

						CustomBlockState customState = CustomBlockTypes.fromMaterial(genEvent.getResult())
								.createBlockState();

						hellWorld.updateBlockState(genPos, customState).exceptionally(ex -> {
							genEvent.getGenerationLocation().getBlock().setType(genEvent.getResult());
							return null;
						});

						if (mode.hasGenSound()) {
							final double radius = instance.getConfigManager().searchRadius();
							loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
									.filter(entity -> entity instanceof Player).map(entity -> (Player) entity)
									.forEach(player -> AdventureHelper
											.playSound(instance.getSenderFactory().getAudience(player), Sound.sound(
													Key.key(mode.getGenSound()), Source.AMBIENT, soundVolume, pitch)));
						}

						if (mode.hasParticleEffect()) {
							mode.displayGenerationParticles(loc);
						}
					});
				});
			});
		});
	}

	private boolean shouldProcessGenerationBlock(Pos3 pos) {
		long now = System.currentTimeMillis();
		long last = lastGenCheck.getOrDefault(pos, 0L);
		if (now - last < GEN_THROTTLE_TICKS * 50L) {
			return false;
		}
		lastGenCheck.put(pos, now);
		return true;
	}

	/**
	 * Returns true if exactly two lava blocks (source or flowing) have a clear AIR
	 * path into `toBlock`. Allows direct and diagonal generators. Avoids lava pools
	 * by requiring exactly 2.
	 */
	private CompletableFuture<Boolean> isValidTwoLavaCollision(@NotNull HellblockWorld<?> world, @NotNull Pos3 to) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		final int centerX = to.x();
		final int centerY = to.y();
		final int centerZ = to.z();

		List<CompletableFuture<Boolean>> checks = new ArrayList<>();

		for (int x = centerX - GENERATOR_NEIGHBORHOOD_RADIUS; x <= centerX + GENERATOR_NEIGHBORHOOD_RADIUS; x++) {
			for (int y = centerY - 1; y <= centerY + 1; y++) {
				for (int z = centerZ - GENERATOR_NEIGHBORHOOD_RADIUS; z <= centerZ
						+ GENERATOR_NEIGHBORHOOD_RADIUS; z++) {
					if (x == centerX && y == centerY && z == centerZ)
						continue;

					final Pos3 checkPos = new Pos3(x, y, z);

					// chain async
					CompletableFuture<Boolean> check = world.getBlockState(checkPos).thenCompose(stateOpt -> {
						if (stateOpt.isEmpty())
							return CompletableFuture.completedFuture(false);

						CustomBlockState state = stateOpt.get();
						if (!LAVA_KEY.contains(state.type().type().value()))
							return CompletableFuture.completedFuture(false);

						// chain to lava checks
						return isLavaSource(world, checkPos).thenCombine(isLavaFlowing(world, checkPos),
								(isSource, isFlowing) -> (isSource || isFlowing)).thenCompose(valid -> {
									if (!valid)
										return CompletableFuture.completedFuture(false);

									// For flowing lava, check flow direction roughly points toward target
									return hasClearPath(world, checkPos, to).thenCombine(
											getLavaFlowDirection(world, checkPos), (clearPath, flowDir) -> {
												if (!clearPath)
													return false;

												// For flowing lava, validate direction
												if (flowDir != null
														&& isLavaFlowingDirectionWrong(checkPos, to, flowDir))
													return false;

												return true;
											});
								});
					});

					checks.add(check);
				}
			}
		}

		// Only generate when exactly 2 lava sources are valid
		return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new)).thenApply(
				v -> (int) checks.stream().map(CompletableFuture::join).filter(Boolean::booleanValue).count() == 2);
	}

	/**
	 * Checks that the path between a lava block and the target block consists only
	 * of AIR.
	 */
	private CompletableFuture<Boolean> hasClearPath(@NotNull HellblockWorld<?> world, @NotNull Pos3 from,
			@NotNull Pos3 to) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		int dx = to.x() - from.x();
		int dy = to.y() - from.y();
		int dz = to.z() - from.z();

		int stepX = Integer.signum(dx);
		int stepY = Integer.signum(dy);
		int stepZ = Integer.signum(dz);

		List<CompletableFuture<Optional<CustomBlockState>>> futures = new ArrayList<>();
		int curX = from.x(), curY = from.y(), curZ = from.z();

		while (true) {
			curX += stepX;
			curY += stepY;
			curZ += stepZ;

			if (curX == to.x() && curY == to.y() && curZ == to.z())
				break;

			Pos3 current = new Pos3(curX, curY, curZ);
			futures.add(world.getBlockState(current));
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> futures.stream()
				.map(CompletableFuture::join).noneMatch(opt -> opt.isPresent() && !opt.get().isAir()));
	}

	@Nullable
	public Player getClosestPlayer(@NotNull Location l, @NotNull Collection<Entity> playersNearby) {
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
		final Pos3 pos = Pos3.from(loc);
		if (!genManager.isGenLocationKnown(pos)) {
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

		final Pos3 pistonPos = Pos3.from(event.getBlock().getLocation());

		if (!genManager.getKnownGenPistons().containsKey(pistonPos)) {
			return;
		}

		final GenPiston piston = genManager.getKnownGenPistons().get(pistonPos);

		final Location genBlockLoc = event.getBlock().getRelative(event.getDirection()).getLocation();
		final Pos3 genPos = Pos3.from(genBlockLoc);

		if (!genManager.isGenLocationKnown(genPos)) {
			return;
		}

		piston.setHasBeenUsed(true);

		// Use radius-based search to find closest player
		final World world = genBlockLoc.getWorld();
		if (world == null)
			return;

		final double searchRadius = instance.getConfigManager().searchRadius();
		final Collection<Entity> nearbyEntities = world.getNearbyEntities(genBlockLoc, searchRadius, searchRadius,
				searchRadius);
		final Player triggeringPlayer = getClosestPlayer(genBlockLoc, nearbyEntities);

		if (triggeringPlayer != null) {
			genManager.setPlayerForLocation(triggeringPlayer.getUniqueId(), genPos, true);
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		final Location location = event.getBlock().getLocation();
		final Pos3 pos = Pos3.from(location);

		// Track if placed in a generator location
		if (genManager.isGenLocationKnown(pos)) {
			playerPlacedGenBlocks.add(pos);
		}

		if (!instance.getConfigManager().pistonAutomation()) {
			return;
		}

		if (event.getBlock().getType() != Material.PISTON) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(location.getBlock()).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userOpt -> {
						if (userOpt.isEmpty())
							return;

						UserData user = userOpt.get();
						int islandId = user.getHellblockData().getIslandId();

						GenPiston piston = new GenPiston(pos, islandId);
						genManager.addKnownGenPiston(piston);
					});
		});
	}

	@EventHandler
	public void onGeneratedBlockBreak(BlockBreakEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}
		final Location location = event.getBlock().getLocation();
		final Pos3 pos = Pos3.from(location);
		final Player player = event.getPlayer();

		// ignore piston cleanup
		if (genManager.getKnownGenPistons().containsKey(pos)) {
			genManager.getKnownGenPistons().remove(pos);
			return;
		}

		if (location.getWorld() == null) {
			return;
		}
		if (!genManager.isGenLocationKnown(pos)) {
			return;
		}

		// Prevent abuse: skip progression if block was manually placed
		if (playerPlacedGenBlocks.contains(pos)) {
			playerPlacedGenBlocks.remove(pos); // cleanup
			return;
		}

		final PlayerBreakGeneratedBlock genEvent = new PlayerBreakGeneratedBlock(player, location);
		if (EventUtils.fireAndCheckCancel(genEvent)) {
			return;
		}

		genManager.setPlayerForLocation(player.getUniqueId(), pos, false);
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
			if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
				userData.getHellblockData().updateLastIslandActivity();
			}
			instance.getChallengeManager().handleChallengeProgression(userData, ActionType.BREAK, event.getBlock());
		});
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
			final Pos3 pos = Pos3.from(location);
			if (genManager.getKnownGenPistons().containsKey(pos)) {
				genManager.getKnownGenPistons().remove(pos);
				return;
			}
		}
	}

	/**
	 * Determines whether a block is a stationary lava source.
	 *
	 * This method inspects the block’s fluid data to confirm that: - It contains
	 * lava or flowing lava. - The fluid is currently marked as a *source* (i.e.,
	 * not flowing or falling).
	 *
	 * Source blocks represent the "origin" of lava flow and can be used as anchors
	 * for detecting infinite lava patterns or other generation features.
	 *
	 * @param world the Hellblock world context
	 * @param pos   the block position to evaluate
	 * @return a {@link CompletableFuture} resolving to {@code true} if the block is
	 *         a lava source, otherwise {@code false}
	 */
	public CompletableFuture<Boolean> isLavaSource(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty() || !LAVA_KEY.contains(stateOpt.get().type().type().value()))
				return false;

			Location loc = pos.toLocation(world.bukkitWorld());
			FluidData fluidData = VersionHelper.getNMSManager().getFluidData(loc);
			org.bukkit.Fluid type = fluidData.getFluidType();

			return (type == Fluid.LAVA || type == Fluid.FLOWING_LAVA) && fluidData.isSource();
		});
	}

	/**
	 * Determines whether a block contains horizontally flowing (non-source,
	 * non-falling) lava.
	 *
	 * This excludes source and falling states and is used for identifying the “side
	 * flow” behavior of lava streams — e.g., how lava travels outward from source
	 * blocks.
	 *
	 * @param world the Hellblock world context
	 * @param pos   the block position to evaluate
	 * @return a {@link CompletableFuture} resolving to {@code true} if the block is
	 *         a flowing lava block, otherwise {@code false}
	 */
	public CompletableFuture<Boolean> isLavaFlowing(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty() || !LAVA_KEY.contains(stateOpt.get().type().type().value()))
				return false;

			Location loc = pos.toLocation(world.bukkitWorld());
			FluidData fluidData = VersionHelper.getNMSManager().getFluidData(loc);
			org.bukkit.Fluid type = fluidData.getFluidType();

			return (type == Fluid.LAVA || type == Fluid.FLOWING_LAVA) && !(fluidData instanceof FallingFluidData)
					&& !fluidData.isSource();
		});
	}

	/**
	 * Computes the directional vector of lava flow at a given position.
	 *
	 * This uses the internal fluid data to determine how lava moves across blocks —
	 * i.e., its velocity direction in 3D space.
	 *
	 * Useful for verifying flow direction consistency in patterned structures, such
	 * as infinite lava formations or lava stream generation.
	 *
	 * @param world the Hellblock world context
	 * @param pos   the block position to evaluate
	 * @return a {@link CompletableFuture} resolving to a {@link Vector}
	 *         representing the flow direction, or {@code null} if not applicable
	 */
	@Nullable
	public CompletableFuture<Vector> getLavaFlowDirection(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(null);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty() || !LAVA_KEY.contains(stateOpt.get().type().type().value()))
				return null;

			Location loc = pos.toLocation(world.bukkitWorld());
			FluidData fluidData = VersionHelper.getNMSManager().getFluidData(loc);
			org.bukkit.Fluid type = fluidData.getFluidType();

			if (type != Fluid.LAVA && type != Fluid.FLOWING_LAVA)
				return null;

			return fluidData.computeFlowDirection(loc);
		});
	}

	/**
	 * Checks whether the lava flow direction deviates too strongly from the
	 * intended target direction.
	 *
	 * This is used to validate whether the lava is flowing correctly between two
	 * points in structured formations (e.g., infinite lava setups).
	 *
	 * A dot product below a defined threshold indicates that the flow direction is
	 * “wrong” (i.e., misaligned).
	 *
	 * @param from    origin position of the flow
	 * @param to      intended destination position
	 * @param flowDir the lava’s computed flow direction
	 * @return {@code true} if the direction is incorrect, otherwise {@code false}
	 */
	private boolean isLavaFlowingDirectionWrong(Pos3 from, Pos3 to, Vector flowDir) {
		Vector toTarget = new Vector(to.x() + 0.5 - (from.x() + 0.5), to.y() + 0.5 - (from.y() + 0.5),
				to.z() + 0.5 - (from.z() + 0.5));
		double dot = flowDir.normalize().dot(toTarget.normalize());
		return dot < FLOW_DIRECTION_THRESHOLD;
	}

	/**
	 * Checks if there is at least one sign block in front of or behind the target
	 * air block where the generation occurs. This includes both wall signs and
	 * standing signs.
	 */
	private CompletableFuture<Boolean> hasNearbySign(@NotNull HellblockWorld<?> world, @NotNull Pos3 to) {
		final boolean requiresSignToGen = getGeneratorModeManager().getGenMode().requiresSignToGenerate();
		if (!requiresSignToGen)
			return CompletableFuture.completedFuture(true);

		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		List<CompletableFuture<Optional<CustomBlockState>>> futures = new ArrayList<>();

		// Check the "front" (air target + direction of flow) and "behind" (opposite
		// side)
		// In this context we don’t know flow direction, so check 4 cardinal directions
		// around it.
		for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
			Pos3 checkPos = new Pos3(to.x() + face.getModX(), to.y() + face.getModY(), to.z() + face.getModZ());
			futures.add(world.getBlockState(checkPos));
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> futures.stream().map(CompletableFuture::join).filter(Optional::isPresent)
						.map(Optional::get)
						.anyMatch(state -> state.type().type().value().toLowerCase(Locale.ROOT).contains("sign")));
	}

	private CompletableFuture<Boolean> isLavaPool(@NotNull HellblockWorld<?> world, @NotNull Pos3 center) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		List<CompletableFuture<Optional<CustomBlockState>>> futures = new ArrayList<>();

		for (int x = center.x() - 2; x <= center.x() + 2; x++) {
			for (int y = center.y() - 1; y <= center.y() + 1; y++) {
				for (int z = center.z() - 2; z <= center.z() + 2; z++) {
					Pos3 checkPos = new Pos3(x, y, z);
					// Avoid checking the center block itself
					if (checkPos.equals(center))
						continue;

					futures.add(world.getBlockState(checkPos));
				}
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			long lavaCount = futures.stream().map(CompletableFuture::join)
					.filter(opt -> opt.isPresent() && LAVA_KEY.contains(opt.get().type().type().value())).count();

			// Considered a lava pool
			return lavaCount > 4;
		});
	}

	@NotNull
	public Map<Material, Double> getGenerationResults(@NotNull Context<Player> context) {
		Map<Material, Double> results = new HashMap<>();

		// Defensive: ensure we have a valid configuration map
		Map<Material, MathValue<Player>> configuredResults = instance.getConfigManager().generationResults();
		if (configuredResults == null || configuredResults.isEmpty()) {
			instance.getPluginLogger().warn("No generation results configured — returning empty map.");
			return results;
		}

		for (Map.Entry<Material, MathValue<Player>> entry : configuredResults.entrySet()) {
			Material material = entry.getKey();
			if (material == null || material == Material.AIR)
				continue;

			MathValue<Player> mathValue = entry.getValue();
			double chance;

			try {
				chance = mathValue.evaluate(context);
				// Ensure numeric stability — avoid NaN or negative weights
				if (Double.isNaN(chance) || chance < 0.0) {
					instance.debug("Invalid generation value for " + material + ": " + chance);
					chance = 0.0;
				}
			} catch (Throwable ex) {
				instance.getPluginLogger().warn("Error evaluating generation value for " + material, ex);
				chance = 0.0;
			}

			results.put(material, chance);
		}

		return results;
	}

	public void processGenerationResults(@NotNull Context<Player> context, @NotNull Location generatorLocation,
			@NotNull Consumer<Map<Material, Double>> callback) {
		final Player player = context.holder();

		// Early fallback when context is empty
		if (player == null) {
			instance.getScheduler().executeAsync(() -> callback.accept(getGenerationResults(context)));
			return;
		}

		// Begin async chain
		instance.getCoopManager().getHellblockOwnerOfBlock(generatorLocation.getBlock()).thenComposeAsync(ownerUUID -> {
			if (ownerUUID == null) {
				// No owner — return immediately with defaults
				return CompletableFuture.completedFuture(Optional.<HellblockData>empty());
			}

			// Fetch user data asynchronously
			return instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenApply(opt -> opt.map(data -> data.getHellblockData()));
		}, instance.getScheduler().async()) // use plugin’s async executor if available
				.thenAcceptAsync(hellblockDataOpt -> {
					// Always start from shared base generation map
					Map<Material, Double> results = new HashMap<>(getGenerationResults(context));

					if (hellblockDataOpt.isEmpty()) {
						callback.accept(results);
						return;
					}

					HellblockData hellblockData = hellblockDataOpt.get();
					Set<UUID> partyPlusOwner = hellblockData.getPartyPlusOwner();
					BoundingBox box = hellblockData.getBoundingBox();

					boolean isMember = partyPlusOwner.contains(player.getUniqueId());
					boolean isInsideIsland = box != null && box.contains(generatorLocation.toVector());

					if (!(isMember && isInsideIsland)) {
						// No bonus if not part of island
						callback.accept(results);
						return;
					}

					// Player is inside their island → apply bonuses
					double totalBonus = getCachedGeneratorBonus(hellblockData);

					results.replaceAll((material, base) -> base < 10.0 ? base + totalBonus : base);

					// Run callback async-safe (no sync assumptions)
					callback.accept(results);

				}, instance.getScheduler().async()) // avoid default ForkJoin threads
				.exceptionally(ex -> {
					instance.getPluginLogger().warn("Error while processing generation results", ex);
					// Always fallback to safe results even if something fails
					callback.accept(getGenerationResults(context));
					return null;
				});
	}

	public double getCachedGeneratorBonus(@NotNull HellblockData data) {
		int islandId = data.getIslandId();

		// If not cached yet, calculate and store
		return islandGeneratorBonusCache.computeIfAbsent(islandId, id -> calculateGeneratorBonus(data));
	}

	public void updateGeneratorBonusCache(@NotNull HellblockData data) {
		islandGeneratorBonusCache.put(data.getIslandId(), calculateGeneratorBonus(data));
	}

	public void invalidateGeneratorBonusCache(int islandId) {
		islandGeneratorBonusCache.remove(islandId);
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
	@Nullable
	private Material chooseRandomResult(@NotNull Map<Material, Double> results) {
		if (results.isEmpty()) {
			return null;
		}

		// Compute total weight (ignore invalid or zero weights)
		double totalWeight = results.values().stream().filter(v -> v != null && v > 0).mapToDouble(Double::doubleValue)
				.sum();

		// Nothing valid to choose from
		if (totalWeight <= 0.0) {
			return null;
		}

		// Roll a random value in [0, totalWeight)
		double randomValue = randomSupplier.getAsDouble() * totalWeight;
		double cumulative = 0.0;

		for (Map.Entry<Material, Double> entry : results.entrySet()) {
			double weight = entry.getValue() != null ? entry.getValue() : 0.0;
			if (weight <= 0.0)
				continue; // skip zero/negative weights

			cumulative += weight;
			if (randomValue <= cumulative + 1e-9) { // small epsilon for floating-point rounding
				return entry.getKey();
			}
		}

		// Fallback: due to rounding, return the last non-null material
		return results.keySet().stream().filter(Objects::nonNull).reduce((first, second) -> second).orElse(null);
	}

	public void savePistonsByIsland(int islandId, @NotNull HellblockWorld<?> hellWorld) {
		if (!instance.getConfigManager().pistonAutomation())
			return;

		// Get all pistons at this island
		GenPiston[] pistons = genManager.getGenPistonsByIslandId(islandId);

		List<String> serialized = Arrays.stream(pistons).filter(p -> p != null && p.hasBeenUsed()).map(p -> {
			Location loc = p.getPos().toLocation(hellWorld.bukkitWorld());
			return StringUtils.serializeLoc(loc);
		}).filter(Objects::nonNull).distinct().collect(Collectors.toList());

		if (serialized.isEmpty())
			return;

		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty())
						return;

					UserData user = optUser.get();
					Map<Integer, List<String>> map = user.getLocationCacheData().getPistonLocationsByIsland();
					if (map == null) {
						map = new HashMap<>();
					}
					map.put(islandId, serialized);
					user.getLocationCacheData().setPistonLocationsByIsland(map);
				});
	}

	public void loadPistonsByIsland(int islandId) {
		if (!instance.getConfigManager().pistonAutomation())
			return;

		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty())
						return;
					UserData user = optUser.get();
					Map<Integer, List<String>> stored = user.getLocationCacheData().getPistonLocationsByIsland();
					if (stored == null)
						return;

					List<String> locations = stored.get(islandId);
					if (locations == null || locations.isEmpty())
						return;

					for (String stringLoc : locations) {
						Location loc = StringUtils.deserializeLoc(stringLoc);
						if (loc == null || loc.getBlock().getType() != Material.PISTON)
							continue;

						Pos3 pos = Pos3.from(loc);
						GenPiston piston = new GenPiston(pos, islandId);
						piston.setHasBeenUsed(true);
						genManager.addKnownGenPiston(piston);
					}

					// Clear once loaded
					stored.remove(islandId);
					user.getLocationCacheData().setPistonLocationsByIsland(stored);
				});
	}

	public void loadIslandPistonsIfNeeded(int islandId) {
		if (loadedIslandPistonCaches.contains(islandId))
			return;

		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(optUser -> {
					if (optUser.isEmpty())
						return;

					UserData user = optUser.get();
					Map<Integer, List<String>> pistons = user.getLocationCacheData().getPistonLocationsByIsland();

					if (pistons != null && pistons.containsKey(islandId)) {
						List<String> serialized = pistons.get(islandId);

						for (String stringLoc : serialized) {
							Location loc = StringUtils.deserializeLoc(stringLoc);
							if (loc == null || loc.getBlock().getType() != Material.PISTON)
								continue;

							Pos3 pos = Pos3.from(loc);
							GenPiston piston = new GenPiston(pos, islandId);
							piston.setHasBeenUsed(true);
							getGeneratorManager().addKnownGenPiston(piston);
						}
					}

					loadedIslandPistonCaches.add(islandId);
					instance.debug("Loaded piston cache for island ID: " + islandId);
				});
	}

	public void clearIslandPistonCache(int islandId) {
		loadedIslandPistonCaches.remove(islandId);
	}
}