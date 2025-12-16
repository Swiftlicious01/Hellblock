package com.swiftlicious.hellblock.generation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagBlock;
import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.entity.armorstand.FakeArmorStand;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.AdventureMetadata;
import com.swiftlicious.hellblock.schematic.SchematicMetadata;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockTypes;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

/**
 * Handles the generation and animation of Hellblock islands, schematics, and
 * their interactive elements such as chests, trees, and cinematic camera
 * sequences.
 * <p>
 * This class manages both instant and animated island generation processes,
 * including block placement effects, player camera control, and cleanup logic
 * for incomplete or interrupted animations. It also enforces interaction
 * restrictions for players during animation sequences to prevent unintended
 * behavior.
 * </p>
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 * <li>Loading and unloading generation-related listeners and state
 * trackers.</li>
 * <li>Generating Hellblock schematics or predefined island variants with
 * optional animation.</li>
 * <li>Creating decorative elements such as glowstone trees and starter
 * chests.</li>
 * <li>Controlling camera anchor points using invisible armor stands for
 * cinematic views.</li>
 * <li>Tracking and cleaning up player-specific animation sessions safely.</li>
 * <li>Restricting player input (movement, commands, interactions) during
 * animations.</li>
 * </ul>
 *
 * <p>
 * This class acts as a central controller for all visual and structural aspects
 * of island creation and presentation within the Hellblock plugin.
 * </p>
 */
public class IslandGenerator implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	protected final Set<UUID> generatingPlayers = ConcurrentHashMap.newKeySet();

	protected final Set<UUID> animatingPlayers = ConcurrentHashMap.newKeySet();
	protected final Map<UUID, FakeArmorStand> cameraAnchors = new ConcurrentHashMap<>();
	protected final Map<UUID, List<SchedulerTask>> runningAnimations = new ConcurrentHashMap<>();

	protected final Map<UUID, SchedulerTask> treeAnimations = new ConcurrentHashMap<>();

	public IslandGenerator(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public void disable() {
		unload();
		generatingPlayers.clear();
		animatingPlayers.clear();

		cameraAnchors.forEach((uuid, fake) -> {
			if (fake != null && !fake.isDead()) {
				Player player = Bukkit.getPlayer(uuid);
				if (player != null && player.isOnline()) {
					fake.resetCamera(player);
					fake.destroy(player);
				}
			}
		});
		cameraAnchors.clear();

		runningAnimations.values().stream().flatMap(List::stream) // Flatten list of tasks
				.filter(Objects::nonNull).filter(task -> !task.isCancelled()).forEach(SchedulerTask::cancel);
		runningAnimations.clear();

		treeAnimations.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
		treeAnimations.clear();
	}

	/**
	 * Generates a hellblock island from a schematic at the specified location.
	 * Searches for the first chest in the schematic to place items.
	 * 
	 * @param request   The variant of the island being generated.
	 * @param world     The world where the island will be generated.
	 * @param location  The base location for the island (bottom center).
	 * @param userData  The player for chest orientation and sound effects.
	 * @param schematic The name of the schematic to paste.
	 * @param animated  Whether the animation should play for schematic placement.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	@Nullable
	public CompletableFuture<Location> generateHellblockSchematic(@NotNull IslandGenerationRequest request,
			@NotNull HellblockWorld<?> world, @NotNull Location location, @NotNull UserData userData,
			@NotNull String schematic, boolean ignoreAirBlock, boolean animated) {
		BoundingBox boundingBox = userData.getHellblockData().getBoundingBox();
		String playerName = userData.getName();

		if (boundingBox == null) {
			instance.getPluginLogger().warn("generateHellblockSchematic: Null bounding box for player " + playerName);
			return CompletableFuture.completedFuture(null);
		}

		instance.debug("generateHellblockSchematic: Starting schematic generation for " + playerName
				+ " using schematic '" + schematic + "' (ignoreAir=" + ignoreAirBlock + ", animated=" + animated + ")");

		SchematicMetadata metadata = instance.getSchematicManager().loadSchematicMetadata(schematic);
		if (metadata == null) {
			instance.getPluginLogger()
					.warn("generateHellblockSchematic: Failed to load metadata for schematic '" + schematic + "'");
			return CompletableFuture.completedFuture(null);
		}

		instance.debug("generateHellblockSchematic: Loaded metadata → container=" + metadata.getContainer() + ", home="
				+ metadata.getHome() + ", tree=" + metadata.getTree());

		Location pasteLocation = boundingBox.getCenter().toLocation(world.bukkitWorld());
		pasteLocation.setY(instance.getConfigManager().height());

		instance.debug("generateHellblockSchematic: Calculated paste location at [world="
				+ pasteLocation.getWorld().getName() + ", x=" + pasteLocation.getBlockX() + ", y="
				+ pasteLocation.getBlockY() + ", z=" + pasteLocation.getBlockZ() + "]");

		return instance.getSchematicManager().pasteSchematic(userData.getUUID(), world.bukkitWorld(), schematic,
				pasteLocation, metadata, ignoreAirBlock, animated).thenComposeAsync(spawnLocation -> {
					if (spawnLocation == null) {
						instance.getPluginLogger()
								.warn("generateHellblockSchematic: Schematic paste returned null spawn location for "
										+ playerName);
						if (animated)
							cleanupAnimation(userData);
						endGeneration(userData.getUUID());
						return CompletableFuture.completedFuture(null);
					}

					instance.debug("generateHellblockSchematic: Schematic pasted successfully for " + playerName
							+ " — evaluating container placement...");

					CompletableFuture<Location> containerLocationFuture;
					if (metadata.getContainer() != null) {
						Location containerLoc = pasteLocation.clone().add(metadata.getContainer());
						containerLocationFuture = CompletableFuture.completedFuture(containerLoc);
						instance.debug("generateHellblockSchematic: Using metadata container offset at [x="
								+ containerLoc.getBlockX() + ", y=" + containerLoc.getBlockY() + ", z="
								+ containerLoc.getBlockZ() + "]");
					} else {
						instance.debug("generateHellblockSchematic: Metadata missing — searching nearest container...");
						containerLocationFuture = findNearestContainerNear(world, pasteLocation, 8, container -> true);
					}

					return containerLocationFuture.thenCompose(containerLoc -> {
						if (containerLoc == null) {
							instance.getPluginLogger().warn(
									"generateHellblockSchematic: No valid container location found for schematic: "
											+ schematic);
							if (animated)
								cleanupAnimation(userData);
							endGeneration(userData.getUUID());
							return CompletableFuture.completedFuture(spawnLocation);
						}

						Block containerBlock = containerLoc.getBlock();
						if (!(containerBlock.getState() instanceof Container)) {
							instance.getPluginLogger()
									.warn("generateHellblockSchematic: Expected container block not found at "
											+ containerLoc + " for schematic '" + schematic
											+ "'. Vector may be outdated.");
							if (animated)
								cleanupAnimation(userData);
							endGeneration(userData.getUUID());
							return CompletableFuture.completedFuture(spawnLocation);
						}

						instance.debug("generateHellblockSchematic: Valid container block found at [x="
								+ containerLoc.getBlockX() + ", y=" + containerLoc.getBlockY() + ", z="
								+ containerLoc.getBlockZ() + "]");

						// Wait for container generation to complete
						return generateHellblockContainer(world, containerLoc, userData, containerBlock.getType(),
								animated, request).thenApply(v -> spawnLocation);
					});
				});
	}

	/**
	 * Generates a defined hellblock island variant at the specified location with
	 * animation, particle effects, and sound effects.
	 * 
	 * @param request  The variant of the hellblock to generate.
	 * @param world    The world where the island will be generated.
	 * @param location The base location for the island (bottom center).
	 * @param userData The player for chest orientation and sound effects.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	public CompletableFuture<Void> generateAnimatedHellblockIsland(@NotNull IslandGenerationRequest request,
			@NotNull HellblockWorld<?> world, @NotNull Location location, @NotNull UserData userData) {
		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();
		String playerName = userData.getName();

		instance.debug("generateAnimatedHellblockIsland: Starting animated generation for " + playerName
				+ " using variant " + request.options() + " at [x=" + x + ", y=" + y + ", z=" + z + "]");

		// Step 1: Build the island structure
		return buildHellblockStructure(world, x, y, z, request.options()).thenCompose(stages -> {
			instance.debug(
					"generateAnimatedHellblockIsland: Built structure with " + stages.size() + " animation stages.");

			// Step 2: Preload the chunks
			return preloadIslandChunks(world, stages, userData, 10,
					progress -> instance.debug(
							"generateAnimatedHellblockIsland: Preload progress: " + (int) (progress * 100) + "%"),
					300L, true, 3, 10,
					() -> instance
							.debug("generateAnimatedHellblockIsland: Finished preloading island chunks successfully!"))

					// Step 3: Small delay before animation
					.thenCompose(v -> delayTicks(1L, location))

					// Step 4: Run the actual animation logic
					.thenCompose(v -> {
						FakeArmorStand camera = createCameraAnchor(userData, location);
						if (camera != null) {
							List<Location> flyPath = buildFlyPath(location, stages.size(), 10.0);
							animateFreeFly(camera, flyPath, userData.getPlayer(), 2L); // 2 ticks per step
							instance.debug(
									"generateAnimatedHellblockIsland: Free-fly camera path started for " + playerName);
						}

						// Wait 2 seconds before block animation begins
						return delayTicks(40L, location)
								.thenCompose(ignored -> animateBlockPlacement(userData, stages, location, 10L));
					})

					// Step 5: Generate the glowstone tree
					.thenCompose(v -> {
						Location treeLoc = location.clone().add(request.tree());
						instance.debug("generateAnimatedHellblockIsland: Animation complete. Generating tree at [x="
								+ treeLoc.getBlockX() + ", y=" + treeLoc.getBlockY() + ", z=" + treeLoc.getBlockZ()
								+ "]");
						return generateHellblockGlowstoneTree(world, treeLoc, true);
					})

					// Step 6: Delay for visual pacing
					.thenCompose(v -> delayTicks(10L, location))

					// Step 7: Generate the container
					.thenCompose(v -> {
						Location containerLoc = location.clone().add(request.container());
						instance.debug("generateAnimatedHellblockIsland: Generating container at [x="
								+ containerLoc.getBlockX() + ", y=" + containerLoc.getBlockY() + ", z="
								+ containerLoc.getBlockZ() + "]");
						return generateHellblockContainer(world, containerLoc, userData, Material.CHEST, true, request);
					})

					// Step 8: Final delay (smooth ending)
					.thenCompose(v -> delayTicks(10L, location));
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("generateAnimatedHellblockIsland: Generation failed for " + playerName
					+ " (" + request.options() + "): " + ex.getMessage(), ex);
			cleanupAnimation(userData);
			endGeneration(userData.getUUID());
			return null;
		});
	}

	private CompletableFuture<Void> delayTicks(long ticks, Location loc) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		instance.getScheduler().sync().runLater(() -> future.complete(null), ticks, loc);
		return future;
	}

	@NotNull
	public List<Location> buildFlyPath(@NotNull Location target, int stageCount, double radius) {
		List<Location> path = new ArrayList<>();
		World world = target.getWorld();
		if (world == null)
			return path;

		int hoverFrames = Math.max(5, (int) (stageCount * 0.1)); // 10% of stages for hover (min 5)
		int moveFrames = stageCount - (hoverFrames * 2);
		if (moveFrames <= 0)
			moveFrames = Math.max(1, stageCount); // fallback

		double fullCircle = 2 * Math.PI;

		// --- Hover at the start ---
		Location startHoverLoc = new Location(world, target.getX() + radius, target.getY(), target.getZ());
		startHoverLoc.setDirection(target.clone().subtract(startHoverLoc).toVector());
		for (int i = 0; i < hoverFrames; i++) {
			path.add(startHoverLoc.clone());
		}

		// --- Main circular orbit ---
		for (int i = 0; i < moveFrames; i++) {
			double t = i / (double) (moveFrames - 1);
			double eased = easeInOutSine(t);
			double angle = eased * fullCircle;

			double x = target.getX() + radius * Math.cos(angle);
			double z = target.getZ() + radius * Math.sin(angle);
			double y = target.getY(); // same height

			Location loc = new Location(world, x, y, z);
			loc.setDirection(target.clone().subtract(loc).toVector());
			path.add(loc);
		}

		// --- Hover at the end ---
		Location endHoverLoc = new Location(world, target.getX() + radius * Math.cos(fullCircle), target.getY(),
				target.getZ() + radius * Math.sin(fullCircle));
		endHoverLoc.setDirection(target.clone().subtract(endHoverLoc).toVector());
		for (int i = 0; i < hoverFrames; i++) {
			path.add(endHoverLoc.clone());
		}

		return path;
	}

	// Smooth easing in/out
	private double easeInOutSine(double t) {
		return -(Math.cos(Math.PI * t) - 1) / 2.0;
	}

	/**
	 * Instantly generates a defined hellblock island variant at the specified
	 * location.
	 * 
	 * @param request  The variant of the hellblock to generate.
	 * @param world    The world where the island will be generated.
	 * @param location The base location for the island (bottom center).
	 * @param userData The player for chest orientation and sound effects.
	 */
	public CompletableFuture<Void> generateInstantHellblockIsland(@NotNull IslandGenerationRequest request,
			@NotNull HellblockWorld<?> world, @NotNull Location location, @NotNull UserData userData) {
		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();
		String playerName = userData.getName();

		instance.debug("generateInstantHellblockIsland: Starting instant generation for " + playerName
				+ " using variant " + request.options() + " at [x=" + x + ", y=" + y + ", z=" + z + "]");

		return buildHellblockStructure(world, x, y, z, request.options()).thenCompose(layers -> {
			instance.debug("generateInstantHellblockIsland: Built structure with " + layers.size() + " layers.");

			return preloadIslandChunks(world, layers, userData, 10,
					progress -> instance
							.debug("generateInstantHellblockIsland: Preload progress: " + (int) (progress * 100) + "%"),
					300L, true, 3, 10,
					() -> instance
							.debug("generateInstantHellblockIsland: Finished preloading island chunks successfully!"))
					.thenCompose(v -> {
						// Place structure synchronously
						CompletableFuture<Void> structureFuture = new CompletableFuture<>();
						instance.getScheduler().executeSync(() -> {
							layers.forEach(layer -> layer.forEach(world::updateBlockState));
							instance.debug("generateInstantHellblockIsland: Block layers set for " + playerName);
							structureFuture.complete(null);
						}, location);
						return structureFuture;
					}).thenCompose(v -> {
						// Generate tree
						Location treeLoc = location.clone().add(request.tree());
						instance.debug("generateInstantHellblockIsland: Generating tree at [x=" + treeLoc.getBlockX()
								+ ", y=" + treeLoc.getBlockY() + ", z=" + treeLoc.getBlockZ() + "]");
						return generateHellblockGlowstoneTree(world, treeLoc, false);
					}).thenCompose(v -> {
						// Generate container
						Location containerLoc = location.clone().add(request.container());
						instance.debug(
								"generateInstantHellblockIsland: Generating container at [x=" + containerLoc.getBlockX()
										+ ", y=" + containerLoc.getBlockY() + ", z=" + containerLoc.getBlockZ() + "]");
						return generateHellblockContainer(world, containerLoc, userData, Material.CHEST, false,
								request);
					});
		});
	}

	/**
	 * Generates a glowstone tree with a gravel trunk at the specified location. The
	 * tree is built in stages with optional animation, particle effects, and sound
	 * effects.
	 * 
	 * @param world    The world where the tree will be generated.
	 * @param location The base location for the tree (bottom center of the trunk).
	 * @param animated If true, the tree is built in stages with visual and sound
	 *                 effects.
	 * @return A CompletableFuture that completes when the tree generation is done.
	 */
	public CompletableFuture<Void> generateHellblockGlowstoneTree(@NotNull HellblockWorld<?> world,
			@NotNull Location location, boolean animated) {
		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();

		instance.debug(
				"generateHellblockGlowstoneTree: Begin generation at base [x= " + x + ", y=" + y + ", z=" + z + "]");

		Map<Pos3, CustomBlockState> allBlocks = new LinkedHashMap<>();
		CustomBlock gravelBlock = CustomBlockTypes.fromMaterial(Material.GRAVEL);
		CustomBlock glowstoneBlock = CustomBlockTypes.fromMaterial(Material.GLOWSTONE);

		// Collect async futures for all pattern fills
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// Base gravel column
		for (int i = 0; i < 3; i++) {
			allBlocks.put(new Pos3(x, y + i, z), gravelBlock.createBlockState());
		}

		// Layer 3 glowstone ring + gravel center
		futures.add(fillPattern(allBlocks, world, x, y + 3, z, 2, Material.GLOWSTONE));
		allBlocks.put(new Pos3(x, y + 3, z), gravelBlock.createBlockState());

		// Layer 4 glowstone ring + gravel center
		futures.add(fillPattern(allBlocks, world, x, y + 4, z, 1, Material.GLOWSTONE));
		allBlocks.put(new Pos3(x, y + 4, z), gravelBlock.createBlockState());

		// Decorative edges
		allBlocks.put(new Pos3(x - 2, y + 4, z), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x + 2, y + 4, z), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 4, z - 2), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 4, z + 2), glowstoneBlock.createBlockState());

		// Upper glowstone cap
		allBlocks.put(new Pos3(x - 1, y + 5, z), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x + 1, y + 5, z), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 5, z - 1), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 5, z + 1), glowstoneBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 5, z), gravelBlock.createBlockState());
		allBlocks.put(new Pos3(x, y + 6, z), glowstoneBlock.createBlockState());

		// --- Wait for all async pattern fills ---
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenCompose(v -> {
			instance.debug("generateHellblockGlowstoneTree: Structure map fully prepared.");

			// Split into gravel vs glowstone stages
			Map<Pos3, CustomBlockState> gravelStage = new LinkedHashMap<>();
			Map<Pos3, CustomBlockState> glowstoneBlocks = new LinkedHashMap<>();

			allBlocks.forEach((pos, state) -> {
				Material mat = Optional.ofNullable(resolveFallbackMaterial(state)).orElse(Material.AIR);
				if (mat == Material.GRAVEL)
					gravelStage.put(pos, state);
				else if (mat == Material.GLOWSTONE)
					glowstoneBlocks.put(pos, state);
			});

			instance.debug("generateHellblockGlowstoneTree: Structure built - Gravel: " + gravelStage.size()
					+ " | Glowstone: " + glowstoneBlocks.size());

			if (!animated) {
				instance.debug("generateHellblockGlowstoneTree: Non-animated placement starting...");
				instance.getScheduler().executeSync(() -> {
					gravelStage.forEach(world::updateBlockState);
					glowstoneBlocks.forEach(world::updateBlockState);
				}, location);
				return CompletableFuture.completedFuture(null);
			}

			// --- Animated: gravel falls, then glowstone grows layer by layer ---
			return simulateFallingGravel(world, gravelStage)
					.thenRun(() -> instance.debug(
							"generateHellblockGlowstoneTree: Gravel fall complete — starting glowstone layering..."))
					.thenCompose(v2 -> {
						// Group glowstone blocks by Y height
						Map<Integer, Map<Pos3, CustomBlockState>> layers = new TreeMap<>();
						for (var entry : glowstoneBlocks.entrySet()) {
							int layerY = entry.getKey().y();
							layers.computeIfAbsent(layerY, k -> new LinkedHashMap<>()).put(entry.getKey(),
									entry.getValue());
						}

						// Convert to list of stages (one per Y)
						List<Map<Pos3, CustomBlockState>> glowstoneStages = new ArrayList<>(layers.values());
						return runGlowTreeStage(world, location, glowstoneStages, 0);
					});
		});
	}

	@SuppressWarnings("deprecation")
	private CompletableFuture<Void> simulateFallingGravel(@NotNull HellblockWorld<?> world,
			@NotNull Map<Pos3, CustomBlockState> gravelBlocks) {
		CompletableFuture<Void> completion = new CompletableFuture<>();
		World bukkitWorld = world.bukkitWorld();

		// Sort gravel by Y coordinate (lowest first) for nice sequential wave
		List<Map.Entry<Pos3, CustomBlockState>> sorted = gravelBlocks.entrySet().stream()
				.sorted(Comparator.comparingInt(e -> e.getKey().y())).toList();

		AtomicInteger index = new AtomicInteger(0);
		long perBlockDelay = 8L; // ticks between each falling gravel start (~0.4s)

		Runnable[] next = new Runnable[1];
		next[0] = () -> {
			int i = index.getAndIncrement();
			if (i >= sorted.size()) {
				instance.debug("generateHellblockGlowstoneTree: All gravel blocks placed sequentially.");
				instance.getGlowstoneTreeHandler().markGlowTreeGravel(world, new ArrayList<>(gravelBlocks.keySet()));
				completion.complete(null);
				return;
			}

			Map.Entry<Pos3, CustomBlockState> entry = sorted.get(i);
			Pos3 pos = entry.getKey();
			CustomBlockState gravelBlock = entry.getValue();

			Location targetLoc = pos.toLocation(bukkitWorld).add(0.5, 0, 0.5);
			Location spawnLoc = targetLoc.clone().add(0, 5.0, 0);

			instance.debug("generateHellblockGlowstoneTree: Gravel fall (wave) [" + (i + 1) + "/" + sorted.size()
					+ "] from " + Pos3.from(spawnLoc) + " -> " + Pos3.from(targetLoc));

			// Spawn falling entity
			FallingBlock falling = bukkitWorld.spawnFallingBlock(spawnLoc, Material.GRAVEL.createBlockData());
			falling.setDropItem(false);
			falling.setHurtEntities(false);
			falling.setPersistent(false);
			falling.setGravity(true);
			falling.addScoreboardTag("hellblock_gravel");

			// Approximate fall time
			long fallTicks = (long) (spawnLoc.getY() - targetLoc.getY()) * 4L;

			// Schedule block placement after the fall
			instance.getScheduler().sync().runLater(() -> {
				if (falling.isValid())
					falling.remove();

				world.updateBlockState(pos, gravelBlock).thenRun(() -> {
					bukkitWorld.spawnParticle(Particle.CLOUD, targetLoc, 8, 0.2, 0.2, 0.2, 0.01);
					AdventureHelper.playPositionalSound(bukkitWorld, targetLoc,
							Sound.sound(Key.key("minecraft:block.gravel.place"), Source.BLOCK, 1f, 0.9f));

					// Queue next gravel block to fall after a small delay
					instance.getScheduler().sync().runLater(next[0], perBlockDelay,
							LocationUtils.getAnyLocationInstance());
				});
			}, fallTicks, LocationUtils.getAnyLocationInstance());
		};

		// Start the wave
		next[0].run();
		return completion;
	}

	@EventHandler(ignoreCancelled = true)
	public void onFallingBlockChange(EntityChangeBlockEvent event) {
		if (event.getEntity().getScoreboardTags().contains("hellblock_gravel")) {
			event.setCancelled(true);
		}
	}

	private CompletableFuture<Void> runGlowTreeStage(@NotNull HellblockWorld<?> world, @NotNull Location location,
			@NotNull List<Map<Pos3, CustomBlockState>> stages, int index) {
		if (index >= stages.size()) {
			Location top = location.clone().add(0.5, 6.5, 0.5);
			World bukkitWorld = world.bukkitWorld();

			instance.debug("generateHellblockGlowstoneTree: Animation complete — triggering final effects.");

			bukkitWorld.spawnParticle(Particle.CLOUD, top, 30, 0.4, 0.3, 0.4, 0.02);
			bukkitWorld.spawnParticle(Particle.END_ROD, top, 20, 0.2, 0.3, 0.2, 0.01);
			bukkitWorld.strikeLightningEffect(top);
			AdventureHelper.playPositionalSound(bukkitWorld, top,
					Sound.sound(Key.key("minecraft:entity.lightning_bolt.thunder"), Source.BLOCK, 0.8f, 1.0f));
			return CompletableFuture.completedFuture(null);
		}

		Map<Pos3, CustomBlockState> stage = stages.get(index);
		instance.debug("generateHellblockGlowstoneTree: Starting stage " + (index + 1) + " with " + stage.size()
				+ " block" + (stage.size() == 1 ? "" : "s"));

		List<CompletableFuture<Void>> futures = stage.entrySet().stream().map(entry -> {
			Pos3 pos = entry.getKey();
			CustomBlockState state = entry.getValue();

			return world.updateBlockState(pos, state).thenRun(() -> {
				instance.getScheduler().executeSync(() -> {
					Location particleLoc = pos.toLocation(world.bukkitWorld()).add(0.5, 0.5, 0.5);
					world.bukkitWorld().spawnParticle(Particle.END_ROD, particleLoc, 8, 0.1, 0.1, 0.1, 0.01);
					AdventureHelper.playPositionalSound(world.bukkitWorld(), particleLoc,
							Sound.sound(Key.key("minecraft:block.amethyst_block.hit"), Source.BLOCK, 0.6f, 1.4f));
				});
			});
		}).toList();

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			instance.debug("generateHellblockGlowstoneTree: Completed stage " + (index + 1) + ", "
					+ (index == stages.size() - 1 ? "finishing up..." : "scheduling next..."));
		}).thenCompose(v -> delayTicks(3L, location)) // helper to wait 3 ticks
				.thenCompose(v -> runGlowTreeStage(world, location, stages, index + 1)).exceptionally(ex -> {
					instance.getPluginLogger().warn(
							"generateHellblockGlowstoneTree: Glowstone tree animation error at stage " + index, ex);
					return null;
				});
	}

	/**
	 * Generates a container (chest/barrel/shulker box) at the specified location
	 * with configured items and name. Supports animated particle and suction
	 * effects.
	 *
	 * @param world    The world where the container will be placed.
	 * @param location The location to place the container.
	 * @param userData The player used for orientation and context.
	 * @param material The container block type to use (e.g. Material.BARREL).
	 * @param animated Whether to play particle and suction animation.
	 * @param request  The island generation request for data regarding the specific
	 *                 island being generated.
	 */
	private CompletableFuture<Void> generateHellblockContainer(@NotNull HellblockWorld<?> world,
			@NotNull Location location, @NotNull UserData userData, @NotNull Material material, boolean animated,
			@NotNull IslandGenerationRequest request) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Pos3 pos = Pos3.from(location);
		instance.debug(
				"generateHellblockContainer: Starting container generation at " + pos + " for " + userData.getName());

		// Grab the old block's facing direction (if any)
		Block existing = world.bukkitWorld().getBlockAt(location);
		AtomicReference<BlockFace> originalFacing = new AtomicReference<>();
		BlockData existingData = existing.getBlockData();
		if (existingData instanceof Directional directional) {
			originalFacing.set(directional.getFacing());
		}

		// Now replace it
		CustomBlock blockType = CustomBlockTypes.fromMaterial(material);
		CustomBlockState blockState = blockType.createBlockState();
		world.updateBlockState(pos, blockState).thenRunAsync(() -> {
			try {
				instance.getScheduler().executeSync(() -> {
					try {
						instance.debug("generateHellblockContainer: Block state updated to " + material + " at " + pos);

						Block bukkitBlock = world.bukkitWorld().getBlockAt(location);
						if (!(bukkitBlock.getState() instanceof Container container)) {
							instance.debug("generateHellblockContainer: Block is not a container at " + pos
									+ ", skipping setup.");
							existing.setType(Material.AIR);
							future.completeExceptionally(new IllegalStateException("Not a container"));
							return;
						}

						BlockFace facing;

						if (request.isSchematic()) {
							// Use the facing direction that existed before replacement
							facing = originalFacing.get();
						} else if (request.containerFacing() != null) {
							// Predefined facing for classic/default islands
							facing = request.containerFacing();
						} else {
							// Fallback
							facing = BlockFace.NORTH;
						}

						instance.debug("generateHellblockContainer: Applying facing direction = " + facing + " for "
								+ material);

						// Apply facing logic
						BlockData data = bukkitBlock.getBlockData();
						if (data instanceof Directional directional && facing != null) {
							if (material == Material.CHEST || material == Material.TRAPPED_CHEST) {
								directional.setFacing(facing);
							} else {
								// Barrels & shulkers: fixed upward orientation
								directional.setFacing(BlockFace.UP);
							}
							bukkitBlock.setBlockData(directional);
						}

						Player player = userData.getPlayer();
						// Context-safe fallback
						Context<Player> context = player != null && player.isOnline() ? Context.player(player)
								: Context.playerEmpty();
						String name = instance.getConfigManager().chestName();
						if (name != null && !name.isEmpty()) {
							// legacy Bukkit color codes
							String colorStripped = name.replaceAll("(?i)&[0-9A-FK-ORX]", "")
									.replaceAll("(?i)<(/?\\w+)>", "") // MiniMessage tags like <red>, <bold>
									.replaceAll("[<>]", ""); // leftover brackets
							if (!colorStripped.isEmpty() && colorStripped.length() <= 32) {
								TextValue<Player> text = TextValue.auto(name);
								Component centered = AdventureHelper
										.parseCenteredTitleMultiline(text.render(context, true));
								RtagBlock containerTag = new RtagBlock(bukkitBlock);
								containerTag.setCustomName(AdventureHelper.componentToLegacy(centered));
								bukkitBlock = containerTag.load(); // Commit changes
								instance.debug("generateHellblockContainer: Applied custom name to container at " + pos
										+ " of title: " + name);
							}
						}

						// Final state update
						bukkitBlock.getState().update();

						instance.debug("generateHellblockContainer: Animation " + (animated ? "enabled" : "disabled")
								+ " for " + userData.getName());

						if (!animated) {
							fillContainerInventory(container, context);
							completeOnce(future, null);
							return;
						}

						Location center = location.clone().add(0.5, 0.5, 0.5);

						if (container.getType() == Material.CHEST || container.getType() == Material.TRAPPED_CHEST) {
							BlockData blockData = container.getBlock().getBlockData();
							if (blockData instanceof Directional dir) {
								BlockFace face = dir.getFacing();
								// shift the visual center slightly forward to match chest lid
								Vector offset = face.getDirection().clone().normalize().multiply(0.3);
								center.add(offset);
							}
						}

						if (player != null && player.isOnline()) {
							FakeArmorStand anchor = cameraAnchors.get(player.getUniqueId());
							if (anchor != null && !anchor.isDead()) {
								instance.debug("generateHellblockContainer: Found active camera anchor for "
										+ player.getName());

								BlockFace containerFacing;
								Material containerType = container.getType();

								BlockData blockData = container.getBlockData();
								if (blockData instanceof Directional directional) {
								    containerFacing = directional.getFacing();
								} else {
								    containerFacing = BlockFace.NORTH;
								}

								// Compute offset depending on container type
								Vector offset;
								if (containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST
								        || isShulkerBox(containerType)) {
								    // Chest & Shulker: camera should be IN FRONT of the opening face
								    offset = containerFacing.getDirection().clone().normalize().multiply(+1.8);
								} else if (containerType == Material.BARREL) {
								    // Barrel opens upward — put camera slightly in front horizontally
								    offset = containerFacing.getDirection().clone().normalize().multiply(+1.2).add(new Vector(0, 0.8, 0));
								} else {
								    offset = new Vector(0, 0, 1.8);
								}

								// Always compute the offset so the player starts in front of the “open” face.
								Location cameraLoc = center.clone().add(offset);
								cameraLoc.setDirection(center.clone().subtract(cameraLoc).toVector());

								// Ensure player starts there too
								ChunkUtils.teleportAsync(player, cameraLoc).thenRun(() -> {
									player.setRotation(cameraLoc.getYaw(), cameraLoc.getPitch());
								});

								// Animate the camera to that spot
								animateCameraEased(anchor, anchor.getLocation(), cameraLoc, player, 30L);

								instance.debug("generateHellblockContainer: Animating camera toward container at [x="
										+ cameraLoc.getBlockX() + ", y=" + cameraLoc.getBlockY() + ", z="
										+ cameraLoc.getBlockZ() + "]");

								instance.getScheduler().sync().runLater(() -> {
									Location slightOffset = center.clone().add(0.5, 0.3, 0);
									animateRotationOnly(anchor, slightOffset, player, 40L);
								}, 30L, location);
							}
						}

						// === ANIMATION ===
						instance.getScheduler().sync().runLater(() -> {
							instance.debug("generateHellblockContainer: Starting particle + chest animation at [x="
									+ center.getBlockX() + ", y=" + center.getBlockY() + ", z=" + center.getBlockZ()
									+ "]");
							center.getWorld().spawnParticle(ParticleUtils.getParticle("ENCHANTMENT_TABLE"), center, 40,
									0.5, 0.5, 0.5, 0.2);
							if (player != null && player.isOnline()) {
								Material containerType = container.getType();

								if (containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST) {
									VersionHelper.getNMSManager().playChestAnimation(player, container.getLocation(),
											true);
								} else if (containerType == Material.BARREL) {
									// Play subtle particle and sound for barrels
									center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(0, 0.6, 0), 8,
											0.15, 0.1, 0.15, 0.01);
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player), Sound
											.sound(Key.key("minecraft:block.barrel.open"), Source.BLOCK, 1.0f, 1.0f));
								} else if (isShulkerBox(containerType)) {
									// Shulker animation effect
									center.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_WITCH"),
											center.clone().add(0, 0.7, 0), 15, 0.2, 0.3, 0.2, 0.01);
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(Key.key("minecraft:block.shulker_box.open"), Source.BLOCK, 1.0f,
													1.0f));
								}
							}

							instance.debug(
									"generateHellblockContainer: Preparing item suction animation for container at [x="
											+ center.getBlockX() + ", y=" + center.getBlockY() + ", z="
											+ center.getBlockZ() + "]");

							// Collect chest items first (but don't spawn them yet)
							List<ItemStack> displayItems = instance.getConfigManager().chestItems().values().stream()
									.map(entry -> setChestData(entry.right().build(context), false))
									.filter(Objects::nonNull).toList();

							if (displayItems.isEmpty()) {
								fillContainerInventory(container, context);
								return;
							}

							AtomicInteger currentIndex = new AtomicInteger(0);

							Runnable runNextItem = new Runnable() {
								@Override
								public void run() {
									int index = currentIndex.getAndIncrement();
									if (index >= displayItems.size()) {
										instance.debug(
												"generateHellblockContainer: Completed item suction animation at [x="
														+ center.getBlockX() + ", y=" + center.getBlockY() + ", z="
														+ center.getBlockZ() + "]");
										// All items done → finalize
										fillContainerInventory(container, context);

										instance.getScheduler().sync().runLater(() -> {
											if (player != null && player.isOnline()) {
												playContainerCloseEffect(container, center, player);
											}
										}, 10L, location);
										completeOnce(future, null);
										return;
									}

									ItemStack displayItem = displayItems.get(index);
									double height = 1.3D;
									if (material == Material.BARREL || isShulkerBox(material)) {
										height = 1.1D;
									}
									Location dropLoc = center.clone().add((Math.random() - 0.5) * 0.4, height,
											(Math.random() - 0.5) * 0.4);
									Item item = world.bukkitWorld().dropItem(dropLoc, displayItem);
									item.setGravity(false);
									item.setCustomNameVisible(true);
									AdventureMetadata.setEntityCustomName(item, AdventureHelper
											.plainTextToComponent(StringUtils.prettifyText(displayItem)));
									item.setPickupDelay(Integer.MAX_VALUE);
									item.setVelocity(new Vector(0, 0, 0));

									AtomicInteger ticks = new AtomicInteger(0);
									AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

									SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
										if (!item.isValid() || item.isDead()) {
											SchedulerTask t = taskRef.get();
											if (t != null && !t.isCancelled())
												t.cancel();
											instance.getScheduler().sync().runLater(this, 10L, location);
											return;
										}

										int t = ticks.incrementAndGet();
										Location current = item.getLocation();
										Vector toChest;

										if (isShulkerBox(material) || material == Material.BARREL) {
											toChest = center.clone().add(0, 0.4, 0).subtract(current).toVector()
													.multiply(0.25);
										} else {
											toChest = center.clone().subtract(current).toVector().multiply(0.25);
										}

										double progress = Math.min(t / 12.0, 1.0); // 0 → 1
										double speedFactor = 0.25 * (1.0 - Math.pow(progress, 2)); // slows down near
																									// the
																									// end
										item.setVelocity(toChest.normalize().multiply(speedFactor));

										item.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_WITCH"),
												item.getLocation().clone().add(0, 0.2, 0), 3, 0.05, 0.05, 0.05, 0.01);
										AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
												Sound.sound(Key.key("minecraft:entity.item.pickup"), Source.PLAYER,
														0.8f, 1.2f));

										if (t >= 7) { // last ~0.5s before suction completes
											item.setCustomNameVisible(false);
										}

										if (t >= 14) { // ~1.2s for smoother ease-in/out
											SchedulerTask tsk = taskRef.get();
											if (tsk != null && !tsk.isCancelled())
												tsk.cancel();

											item.remove();

											// Always wait ~0.25s before starting next
											instance.getScheduler().sync().runLater(this, 5L, location);
										}
									}, 0L, 2L, location);

									taskRef.set(task);
								}
							};

							// Start the first item suction
							runNextItem.run();
						}, 20L, location);
					} catch (Exception ex) {
						instance.getPluginLogger()
								.warn("generateHellblockContainer: Exception at " + pos + " — " + ex.getMessage(), ex);
						completeOnce(future, ex);
					}
				}, location);
			} catch (Exception ex) {
				instance.getPluginLogger()
						.warn("generateHellblockContainer: Exception at " + pos + " — " + ex.getMessage(), ex);
				completeOnce(future, ex);
			}
		});

		return future;
	}

	private void completeOnce(CompletableFuture<Void> f, Throwable error) {
		if (f.isDone())
			return;
		if (error == null)
			f.complete(null);
		else
			f.completeExceptionally(error);
	}

	private void playContainerCloseEffect(Container container, Location center, Player player) {
		Material type = container.getType();
		if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
			VersionHelper.getNMSManager().playChestAnimation(player, container.getLocation(), false);
		} else if (type == Material.BARREL) {
			center.getWorld().spawnParticle(ParticleUtils.getParticle("SMOKE_NORMAL"), center.clone().add(0, 0.6, 0), 6,
					0.15, 0.1, 0.15, 0.01);
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(Key.key("minecraft:block.barrel.close"), Source.BLOCK, 1.0f, 1.0f));
		} else if (isShulkerBox(type)) {
			center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 0.7, 0), 12, 0.2, 0.25, 0.2,
					0.01);
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(Key.key("minecraft:block.shulker_box.close"), Source.BLOCK, 1.0f, 1.0f));
		}

		center.getWorld().spawnParticle(Particle.CRIT, center.clone().add(0, 0.8, 0), 10, 0.3, 0.2, 0.3, 0.02);
	}

	/**
	 * Fills a container block's inventory with pre-configured items from the plugin
	 * configuration. Respects slot assignments when possible, and falls back to the
	 * first empty slot if a specific slot is invalid or unavailable.
	 *
	 * @param container The target block container (e.g. chest, barrel, shulker
	 *                  box).
	 * @param context   The rendering context used to resolve dynamic item data
	 *                  (e.g. player-based names).
	 */
	private void fillContainerInventory(@NotNull Container container, @NotNull Context<Player> context) {
		Inventory inv = container.getInventory();
		inv.clear();

		instance.getConfigManager().chestItems().values().forEach(entry -> {
			ItemStack item = setChestData(entry.right().build(context), true);
			if (item == null)
				return;
			int slot = entry.left();

			if (slot >= 0 && slot < inv.getSize()) {
				inv.setItem(slot, item);
			} else if (inv.firstEmpty() != -1) {
				inv.addItem(item);
			} else {
				instance.getPluginLogger().warn("Item '%s' could not be added to container at %s — full."
						.formatted(item.getType().name(), container.getLocation()));
			}
		});
		container.getBlock().getState().update();
	}

	/**
	 * Checks if the given material is any color variant of a shulker box.
	 *
	 * @param material The material to check.
	 * @return {@code true} if the material is a shulker box; otherwise
	 *         {@code false}.
	 */
	private boolean isShulkerBox(@NotNull Material material) {
		return material.name().endsWith("_SHULKER_BOX");
	}

	/**
	 * Checks if the given material is a container type.
	 *
	 * @param material The material to check.
	 * @return {@code true} if the material is a container; otherwise {@code false}.
	 */
	private boolean isPotentialContainer(@NotNull Material material) {
		return material == Material.CHEST || material == Material.BARREL || material == Material.TRAPPED_CHEST
				|| isShulkerBox(material);
	}

	/**
	 * Searches for the nearest container that matches a given filter within a
	 * radius of a location.
	 *
	 * @param world           The world to search in.
	 * @param center          The center point of the search area.
	 * @param radius          The radius (in blocks) to search in all directions.
	 * @param containerFilter A predicate to match specific container types (e.g.
	 *                        only chests, only barrels).
	 * @return The location of the nearest matching container, or {@code null} if
	 *         none was found.
	 */
	@Nullable
	private CompletableFuture<Location> findNearestContainerNear(@NotNull HellblockWorld<?> world,
			@NotNull Location center, int radius, @NotNull Predicate<Container> containerFilter) {
		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();

		List<CompletableFuture<Location>> candidateFutures = new ArrayList<>();

		for (int y = cy - radius; y <= cy + radius; y++) {
			for (int x = cx - radius; x <= cx + radius; x++) {
				for (int z = cz - radius; z <= cz + radius; z++) {

					// Copy values to effectively final variables
					final int fx = x;
					final int fy = y;
					final int fz = z;

					Pos3 pos = new Pos3(fx, fy, fz);

					candidateFutures.add(world.getBlockState(pos).thenApply(optionalState -> {
						if (optionalState.isEmpty())
							return null;

						Material fallback = resolveFallbackMaterial(optionalState.get());
						if (fallback == null || !isPotentialContainer(fallback))
							return null;

						Block bukkitBlock = world.bukkitWorld().getBlockAt(fx, fy, fz);
						BlockState blockState = bukkitBlock.getState();
						if (!(blockState instanceof Container container))
							return null;

						if (!containerFilter.test(container))
							return null;

						return bukkitBlock.getLocation();
					}));
				}
			}
		}

		return CompletableFuture.allOf(candidateFutures.toArray(CompletableFuture[]::new))
				.thenApply(v -> candidateFutures.stream().map(CompletableFuture::join).filter(Objects::nonNull)
						.min(Comparator.comparingDouble(loc -> loc.distanceSquared(center))).orElse(null));
	}

	/**
	 * Builds the block structure for the specified island type at the given
	 * coordinates. Returns a list of stages, where each stage is a map of positions
	 * to their target block states.
	 * 
	 * @param world The world where the structure will be built.
	 * @param x     The x-coordinate of the center of the structure.
	 * @param y     The y-coordinate of the base of the structure.
	 * @param z     The z-coordinate of the center of the structure.
	 * @param type  The type of island structure to build.
	 * @return A list of stages for animated block placement.
	 */
	@NotNull
	private CompletableFuture<List<Map<Pos3, CustomBlockState>>> buildHellblockStructure(
			@NotNull HellblockWorld<?> world, int x, int y, int z, @NotNull IslandOptions type) {
		List<CompletableFuture<Map<Pos3, CustomBlockState>>> stageFutures = new ArrayList<>();

		switch (type) {
		case DEFAULT -> {
			// Layer 0: Bedrock base
			Map<Pos3, CustomBlockState> bedrockBase = new LinkedHashMap<>();
			Pos3 basePos = new Pos3(x, y, z);
			bedrockBase.put(basePos, CustomBlockTypes.fromMaterial(Material.BEDROCK).createBlockState());
			stageFutures.add(CompletableFuture.completedFuture(bedrockBase));

			// Layer 4
			CompletableFuture<Map<Pos3, CustomBlockState>> layer4Future = fillLayer(world, y + 4, x - 3, x + 3, z - 3,
					z + 3, Material.SOUL_SAND).thenCompose(
							layer -> CompletableFuture
									.allOf(putBlock(layer, world, x - 3, y + 4, z - 3, Material.AIR),
											putBlock(layer, world, x - 3, y + 4, z + 3, Material.AIR),
											putBlock(layer, world, x + 3, y + 4, z - 3, Material.AIR),
											putBlock(layer, world, x + 3, y + 4, z + 3, Material.AIR))
									.thenApply(v -> layer));
			stageFutures.add(layer4Future);

			// Layer 3
			CompletableFuture<Map<Pos3, CustomBlockState>> layer3Future = fillLayer(world, y + 3, x - 2, x + 2, z - 2,
					z + 2, Material.SOUL_SAND).thenCompose(
							layer -> CompletableFuture
									.allOf(putBlock(layer, world, x, y + 3, z, Material.GRASS_BLOCK),
											putBlock(layer, world, x - 3, y + 3, z, Material.SOUL_SAND),
											putBlock(layer, world, x + 3, y + 3, z, Material.SOUL_SAND),
											putBlock(layer, world, x, y + 3, z - 3, Material.SOUL_SAND),
											putBlock(layer, world, x, y + 3, z + 3, Material.SOUL_SAND))
									.thenApply(v -> layer));
			stageFutures.add(layer3Future);

			// Layer 2
			CompletableFuture<Map<Pos3, CustomBlockState>> layer2Future = fillLayer(world, y + 2, x - 1, x + 1, z - 1,
					z + 1, Material.SOUL_SAND).thenCompose(
							layer -> CompletableFuture
									.allOf(putBlock(layer, world, x, y + 2, z, Material.DIRT),
											putBlock(layer, world, x - 2, y + 2, z, Material.SOUL_SAND),
											putBlock(layer, world, x + 2, y + 2, z, Material.SOUL_SAND),
											putBlock(layer, world, x, y + 2, z - 2, Material.SOUL_SAND),
											putBlock(layer, world, x, y + 2, z + 2, Material.SOUL_SAND))
									.thenApply(v -> layer));
			stageFutures.add(layer2Future);

			// Layer 1
			CompletableFuture<Map<Pos3, CustomBlockState>> layer1Future = CompletableFuture.allOf(
					putBlock(world, x, y + 1, z, Material.DIRT), putBlock(world, x - 1, y + 1, z, Material.SOUL_SAND),
					putBlock(world, x + 1, y + 1, z, Material.SOUL_SAND),
					putBlock(world, x, y + 1, z - 1, Material.SOUL_SAND),
					putBlock(world, x, y + 1, z + 1, Material.SOUL_SAND)).thenApply(v -> {
						Map<Pos3, CustomBlockState> map = new LinkedHashMap<>();
						map.put(new Pos3(x, y + 1, z), CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState());
						map.put(new Pos3(x - 1, y + 1, z),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						map.put(new Pos3(x + 1, y + 1, z),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						map.put(new Pos3(x, y + 1, z - 1),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						map.put(new Pos3(x, y + 1, z + 1),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						return map;
					});
			stageFutures.add(layer1Future);
		}

		case CLASSIC -> {
			// Base layer
			CompletableFuture<Map<Pos3, CustomBlockState>> baseFuture = fillLayer(world, y, x - 5, x, z - 2, z,
					Material.SOUL_SAND)
					.thenCombine(fillLayer(world, y, x - 2, x, z - 5, z, Material.SOUL_SAND), (layerA, layerB) -> {
						Map<Pos3, CustomBlockState> map = new LinkedHashMap<>(layerA);
						map.putAll(layerB);
						return map;
					}).thenCompose(map -> putBlock(map, world, x, y, z, Material.SOUL_SAND).thenApply(v -> map));
			stageFutures.add(baseFuture);

			// Layer 1
			CompletableFuture<Map<Pos3, CustomBlockState>> l1Future = fillLayer(world, y + 1, x - 5, x, z - 2, z,
					Material.SOUL_SAND)
					.thenCombine(fillLayer(world, y + 1, x - 2, x, z - 5, z, Material.SOUL_SAND), (layerA, layerB) -> {
						Map<Pos3, CustomBlockState> map = new LinkedHashMap<>(layerA);
						map.putAll(layerB);
						return map;
					});
			stageFutures.add(l1Future);

			// Layer 2
			CompletableFuture<Map<Pos3, CustomBlockState>> l2Future = fillLayer(world, y + 2, x - 5, x, z - 2, z,
					Material.SOUL_SAND)
					.thenCombine(fillLayer(world, y + 2, x - 2, x, z - 5, z, Material.SOUL_SAND), (layerA, layerB) -> {
						Map<Pos3, CustomBlockState> map = new LinkedHashMap<>(layerA);
						map.putAll(layerB);
						return map;
					});
			stageFutures.add(l2Future);

			// Decorations
			CompletableFuture<Map<Pos3, CustomBlockState>> decoFuture = CompletableFuture
					.allOf(putBlock(world, x - 1, y + 1, z - 1, Material.GRASS_BLOCK),
							putBlock(world, x - 3, y + 1, z - 1, Material.SOUL_SAND),
							putBlock(world, x - 4, y + 1, z - 1, Material.DIRT),
							putBlock(world, x - 1, y + 1, z - 3, Material.SOUL_SAND),
							putBlock(world, x - 1, y + 1, z - 4, Material.DIRT),
							putBlock(world, x - 1, y, z - 1, Material.BEDROCK))
					.thenApply(v -> {
						Map<Pos3, CustomBlockState> map = new LinkedHashMap<>();
						map.put(new Pos3(x - 1, y + 1, z - 1),
								CustomBlockTypes.fromMaterial(Material.GRASS_BLOCK).createBlockState());
						map.put(new Pos3(x - 3, y + 1, z - 1),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						map.put(new Pos3(x - 4, y + 1, z - 1),
								CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState());
						map.put(new Pos3(x - 1, y + 1, z - 3),
								CustomBlockTypes.fromMaterial(Material.SOUL_SAND).createBlockState());
						map.put(new Pos3(x - 1, y + 1, z - 4),
								CustomBlockTypes.fromMaterial(Material.DIRT).createBlockState());
						map.put(new Pos3(x - 1, y, z - 1),
								CustomBlockTypes.fromMaterial(Material.BEDROCK).createBlockState());
						return map;
					});
			stageFutures.add(decoFuture);
		}

		default -> throw new IllegalArgumentException("Unsupported island type: " + type);
		}

		// Combine all stages into one final future list
		return CompletableFuture.allOf(stageFutures.toArray(CompletableFuture[]::new))
				.thenApply(v -> stageFutures.stream().map(CompletableFuture::join).toList());
	}

	/**
	 * Animates block placements in stages with a delay between each stage. If the
	 * player disconnects during the animation, it stops and completes
	 * exceptionally.
	 * 
	 * @param userData   The player to check for disconnection.
	 * @param stages     A list of block changes to apply in sequence.
	 * @param reference  A location used to schedule the task in the correct world
	 *                   context.
	 * @param ticksDelay Delay in ticks between each stage.
	 * @return A CompletableFuture that completes when the animation is done or
	 *         exceptionally if interrupted.
	 */
	private CompletableFuture<Void> animateBlockPlacement(@NotNull UserData userData,
			@NotNull List<Map<Pos3, CustomBlockState>> stages, @NotNull Location reference, long ticksDelay) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline()) {
			instance.debug("animateBlockPlacement: Player offline. Skipping animation for " + userData.getName());

			// Fallback to instant placement
			instance.getScheduler().executeSync(() -> {
				Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(reference.getWorld());
				if (worldOpt.isEmpty()) {
					instance.getPluginLogger().warn("animateBlockPlacement: Could not find HellblockWorld for "
							+ reference.getWorld().getName());
					return;
				}
				HellblockWorld<?> world = worldOpt.get();
				stages.forEach(stage -> stage.forEach(world::updateBlockState));
			}, reference);

			return CompletableFuture.completedFuture(null);
		}

		long animationStart = System.nanoTime();

		instance.debug("animateBlockPlacement: Scheduled animation for " + userData.getName() + " with ticksDelay="
				+ ticksDelay);

		return runNextStage(userData, stages, reference, 0, ticksDelay, animationStart);
	}

	private CompletableFuture<Void> runNextStage(@NotNull UserData userData,
			@NotNull List<Map<Pos3, CustomBlockState>> stages, @NotNull Location reference, int index, long ticksDelay,
			long animationStartTime) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline()) {
			cleanupAnimation(userData);
			endGeneration(userData.getUUID());
			return CompletableFuture.failedFuture(new IllegalStateException("Player disconnected during animation."));
		}

		if (index >= stages.size()) {
			long totalDuration = (System.nanoTime() - animationStartTime) / 1_000_000;
			instance.debug("animateBlockPlacement: Animation completed for " + userData.getName());
			instance.debug("Total animation time for " + userData.getName() + ": " + totalDuration + " ms");
			return CompletableFuture.completedFuture(null);
		}

		Map<Pos3, CustomBlockState> stage = stages.get(index);
		long stageStart = System.nanoTime();
		AtomicInteger blocksPlaced = new AtomicInteger();

		Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(reference.getWorld());
		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			instance.getPluginLogger()
					.warn("animateBlockPlacement: Could not find HellblockWorld for animation stage at "
							+ reference.getWorld().getName());
			return CompletableFuture.failedFuture(new IllegalStateException("Missing HellblockWorld"));
		}

		HellblockWorld<?> hellblockWorld = worldOpt.get();

		// Track particle spawn locations + materials
		List<Map.Entry<Location, org.bukkit.block.data.BlockData>> particleEntries = Collections
				.synchronizedList(new ArrayList<>());

		List<CompletableFuture<Void>> blockFutures = stage.entrySet().stream().<CompletableFuture<Void>>map(entry -> {
			Pos3 pos = entry.getKey();
			CustomBlockState state = entry.getValue();

			if (pos == null || state == null) {
				instance.getPluginLogger()
						.warn("animateBlockPlacement: Null pos/state in animation stage for " + userData.getName());
				return CompletableFuture.completedFuture(null);
			}

			Material fallback = Optional.ofNullable(resolveFallbackMaterial(state)).orElse(Material.AIR);
			if (fallback.isAir()) {
				instance.debug("animateBlockPlacement: Skipping air block at " + pos);
				return CompletableFuture.completedFuture(null);
			}

			blocksPlaced.incrementAndGet();

			// Collect particle info
			org.bukkit.block.data.BlockData data = fallback.createBlockData();
			Location loc = pos.toLocation(reference.getWorld()).clone().add(0.5, 0.5, 0.5);
			particleEntries.add(Map.entry(loc, data));

			// Run async block update
			return hellblockWorld.updateBlockState(pos, state);
		}).toList();

		// Wait for this stage’s all blocks, then delay before next
		return CompletableFuture.allOf(blockFutures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			long stageDuration = (System.nanoTime() - stageStart) / 1_000_000;
			instance.debug("animateBlockPlacement: Stage " + (index + 1) + " → placed " + blocksPlaced.get()
					+ " blocks in " + stageDuration + " ms");

			instance.getScheduler().executeSync(() -> {
				if (particleEntries.isEmpty())
					return;

				World world = reference.getWorld();
				Particle particle = ParticleUtils.getParticle("BLOCK_CRACK");

				// Compute wave direction across X or Z axis (pick the longer dimension)
				double minX = particleEntries.stream().mapToDouble(e -> e.getKey().getX()).min().orElse(0);
				double maxX = particleEntries.stream().mapToDouble(e -> e.getKey().getX()).max().orElse(0);
				double minZ = particleEntries.stream().mapToDouble(e -> e.getKey().getZ()).min().orElse(0);
				double maxZ = particleEntries.stream().mapToDouble(e -> e.getKey().getZ()).max().orElse(0);

				boolean waveAlongX = (maxX - minX) >= (maxZ - minZ); // pick axis with greater span
				double minAxis = waveAlongX ? minX : minZ;
				double maxAxis = waveAlongX ? maxX : maxZ;
				double range = Math.max(1, maxAxis - minAxis);

				// Sort by axis position for smooth wave
				particleEntries
						.sort(Comparator.comparingDouble(e -> waveAlongX ? e.getKey().getX() : e.getKey().getZ()));

				int totalSteps = 10; // number of wave passes
				long totalWaveDurationTicks = 20L; // total animation time (1s wave)
				long delayPerStep = totalWaveDurationTicks / totalSteps;

				// Divide particles into wave bands
				for (int i = 0; i < totalSteps; i++) {
					double bandStart = minAxis + (i / (double) totalSteps) * range;
					double bandEnd = minAxis + ((i + 1) / (double) totalSteps) * range;

					final int stepIndex = i;
					instance.getScheduler().sync().runLater(() -> {
						particleEntries.stream().filter(e -> {
							double axis = waveAlongX ? e.getKey().getX() : e.getKey().getZ();
							return axis >= bandStart && axis < bandEnd;
						}).forEach(e -> {
							world.spawnParticle(particle, e.getKey(), 10, 0.25, 0.25, 0.25, 0.0, e.getValue());
						});
					}, delayPerStep * stepIndex, LocationUtils.getAnyLocationInstance());
				}
			});

			AdventureHelper.playPositionalSound(player.getWorld(), player.getLocation(),
					Sound.sound(Key.key("minecraft:block.stone.place"), Source.BLOCK, 0.4f, 1.2f));
		}).thenCompose(v -> delayTicks(ticksDelay, reference)) // wait ticks before next stage
				.thenCompose(v -> runNextStage(userData, stages, reference, index + 1, ticksDelay, animationStartTime))
				.exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("animateBlockPlacement: Block animation placement error at stage " + index, ex);
					return null;
				});
	}

	/**
	 * Attempts to resolve a vanilla Bukkit {@link Material} equivalent for the
	 * provided {@link CustomBlockState}.
	 *
	 * <p>
	 * This method inspects the block's namespaced key (for example,
	 * {@code hellblock:farmland} or {@code minecraft:farmland}) and extracts the
	 * material name portion after the colon. It then attempts to match that name to
	 * a corresponding {@link Material} enum constant, such as
	 * {@link Material#FARMLAND} in this example.
	 * </p>
	 *
	 * <p>
	 * This provides automatic compatibility between custom Hellblock block types
	 * and their vanilla Bukkit {@link Material} counterparts without requiring a
	 * manual mapping for each block. If no matching material is found, the method
	 * returns {@code null}.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * </p>
	 * 
	 * <pre>{@code
	 * CustomBlockState state = CustomBlockTypes.FARMLAND.createBlockState();
	 * Material material = resolveFallbackMaterial(state); // returns Material.FARMLAND
	 * }</pre>
	 *
	 * @param state the custom block state whose underlying type should be resolved
	 * @return the resolved Bukkit {@link Material}, or {@code null} if no match
	 *         exists
	 */
	@Nullable
	public Material resolveFallbackMaterial(@NotNull CustomBlockState state) {
		String fullKey = state.type().type().asString(); // e.g., "hellblock:gravel" or "minecraft:gravel"
		String[] parts = fullKey.split(":");

		if (parts.length == 2) {
			String vanillaName = parts[1].toUpperCase(Locale.ROOT); // "GRAVEL"
			try {
				return Material.valueOf(vanillaName); // Try resolving to vanilla material
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
				// Material not found
			}
		}
		return null;
	}

	/**
	 * Preloads all {@link CustomChunk}s necessary for an island's generation by
	 * either using the {@link BoundingBox} (if available) or falling back to the
	 * block stage data.
	 * 
	 * <p>
	 * This method should only be used during initial island generation to ensure
	 * that all {@link CustomChunk}s exist and are fully initialized.
	 * </p>
	 * 
	 * <p>
	 * Chunks are loaded in a center-outward order, prioritizing chunks near the
	 * island's bounding box center (if available) to ensure smooth visual
	 * generation.
	 * </p>
	 * 
	 * <p>
	 * It will call {@link HellblockWorld#getOrCreateChunk(ChunkPos)} to ensure
	 * chunks are created, and then load both the plugin and Bukkit chunk via
	 * {@link CustomChunk#load(boolean)}. Chunk loading is batched per tick and
	 * supports retry logic on failure.
	 * </p>
	 *
	 * @param world              The {@link HellblockWorld} where chunks belong
	 * @param stages             The animation stages containing block positions
	 *                           (used if bounding box is absent)
	 * @param ownerData          The island owner’s user data (used for bounding box
	 *                           and logging)
	 * @param chunksPerTick      Max number of chunks to load each tick
	 * @param progressCallback   Optional callback reporting load progress (0.0 to
	 *                           1.0)
	 * @param timeoutTicks       Timeout in ticks after which loading will abort
	 * @param verboseLogging     If true, logs chunk loading and failures verbosely
	 * @param maxRetries         Max number of retry attempts per chunk on failure
	 * @param retryDelayTicks    Delay between retry attempts (in ticks)
	 * @param onCompleteCallback Optional callback run when all chunks have finished
	 *                           loading or failed
	 * 
	 * @return A future that completes with the list of failed {@link ChunkPos}
	 *         after all processing
	 */
	public CompletableFuture<List<ChunkPos>> preloadIslandChunks(@NotNull HellblockWorld<?> world,
			@Nullable List<Map<Pos3, CustomBlockState>> stages, @NotNull UserData ownerData, int chunksPerTick,
			@Nullable Consumer<Double> progressCallback, long timeoutTicks, boolean verboseLogging, int maxRetries,
			int retryDelayTicks, @Nullable Runnable onCompleteCallback) {

		if (chunksPerTick <= 0) {
			throw new IllegalArgumentException("chunksPerTick must be > 0");
		}

		Set<ChunkPos> neededChunks = new HashSet<>();
		BoundingBox box = Optional.of(ownerData.getHellblockData()).map(HellblockData::getBoundingBox).orElse(null);

		String username = ownerData.getName();
		int islandId = Optional.of(ownerData.getHellblockData()).map(HellblockData::getIslandId).orElse(-1);

		int centerX = 0;
		int centerZ = 0;

		String logPrefix = "[Hellblock-" + islandId + "|" + username + "]";

		if (box != null) {
			int minChunkX = (int) Math.floor(box.getMinX() / 16.0);
			int maxChunkX = (int) Math.ceil(box.getMaxX() / 16.0);
			int minChunkZ = (int) Math.floor(box.getMinZ() / 16.0);
			int maxChunkZ = (int) Math.ceil(box.getMaxZ() / 16.0);

			centerX = (int) Math.floor((box.getMinX() + box.getMaxX()) / 2.0 / 16.0);
			centerZ = (int) Math.floor((box.getMinZ() + box.getMaxZ()) / 2.0 / 16.0);

			for (int cx = minChunkX; cx <= maxChunkX; cx++) {
				for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
					ChunkPos chunkPos = new ChunkPos(cx, cz);
					if (!world.isChunkLoaded(chunkPos)) {
						neededChunks.add(chunkPos);
					}
				}
			}

			instance.debug(() -> logPrefix + " preloadIslandChunks: Using bounding box: " + box);
			instance.debug(
					() -> logPrefix + " preloadIslandChunks: Chunks to load from bounding box: " + neededChunks.size());
		} else {
			// Fallback: derive from block positions
			stages.forEach(stage -> stage.keySet().forEach(pos -> {
				if (pos != null) {
					ChunkPos chunkPos = ChunkPos.fromPos3(pos);
					if (!world.isChunkLoaded(chunkPos)) {
						neededChunks.add(chunkPos);
					}
				}
			}));

			instance.debug(() -> logPrefix + " preloadIslandChunks: Bounding box not available, using block stages");
			instance.debug(
					() -> logPrefix + " preloadIslandChunks: Chunks to load from stages: " + neededChunks.size());
		}

		List<ChunkPos> sortedChunks = generateSpiralChunks(centerX, centerZ, neededChunks);
		Deque<ChunkPos> queue = new ArrayDeque<>(sortedChunks);
		int totalChunks = queue.size();

		AtomicInteger loadedChunks = new AtomicInteger(0);
		List<ChunkPos> finalFailures = Collections.synchronizedList(new ArrayList<>());
		Map<ChunkPos, Integer> failedAttempts = new ConcurrentHashMap<>();
		Map<Class<? extends Throwable>, Integer> failureReasons = new ConcurrentHashMap<>();
		Set<ChunkPos> loadingChunks = ConcurrentHashMap.newKeySet();

		CompletableFuture<List<ChunkPos>> future = new CompletableFuture<>();
		AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		AtomicBoolean completed = new AtomicBoolean(false);

		Runnable checkCompletion = () -> {
			if (loadedChunks.get() + finalFailures.size() >= totalChunks) {
				if (completed.getAndSet(true)) {
					return; // Already completed once, do nothing
				}

				SchedulerTask task = taskRef.get();
				if (task != null && !task.isCancelled()) {
					task.cancel();
				}

				if (!failureReasons.isEmpty()) {
					instance.debug(logPrefix + " preloadIslandChunks: Failure summary:");
					failureReasons.forEach((type, count) -> instance.debug(
							"  - " + type.getSimpleName() + ": " + count + " failure" + (count == 1 ? "" : "s")));
				}

				if (onCompleteCallback != null) {
					try {
						onCompleteCallback.run();
					} catch (Exception ignored) {
					}
				}

				future.complete(finalFailures);
			}
		};

		AtomicInteger lastProgress = new AtomicInteger(-1);

		Runnable tickTask = () -> {
			int processed = 0;

			while (!queue.isEmpty() && processed < chunksPerTick) {
				ChunkPos chunkPos = queue.poll();
				if (chunkPos == null || !loadingChunks.add(chunkPos)) {
					continue;
				}

				try {
					world.getOrCreateChunk(chunkPos).thenAccept(chunk -> {
						try {
							loadedChunks.incrementAndGet();
							if (verboseLogging) {
								instance.debug(() -> "preloadIslandChunks: Loaded chunk " + chunk.chunkPos());
							}
						} catch (Throwable t) {
							finalFailures.add(chunk.chunkPos());
							failureReasons.merge(t.getClass(), 1, Integer::sum);
							instance.getPluginLogger()
									.warn(logPrefix + " Exception during chunk load: " + chunk.chunkPos(), t);
						} finally {
							loadingChunks.remove(chunk.chunkPos());
							if (progressCallback != null) {
								double progress = loadedChunks.get() / (double) totalChunks;
								int newProgress = (int) (progress * 100);
								if (newProgress != lastProgress.getAndSet(newProgress)) {
									progressCallback.accept(progress);
								}
							}
							checkCompletion.run();
						}
					}).exceptionally(ex -> {
						int attempts = failedAttempts.getOrDefault(chunkPos, 0) + 1;
						failedAttempts.put(chunkPos, attempts);
						failureReasons.merge(ex.getClass(), 1, Integer::sum);

						if (attempts <= maxRetries) {
							instance.debug(
									() -> logPrefix + " Retrying chunk " + chunkPos + " (attempt " + attempts + ")");
							instance.getScheduler().sync().runLater(() -> queue.addLast(chunkPos), retryDelayTicks,
									null);
						} else {
							finalFailures.add(chunkPos);
							instance.getPluginLogger().warn(logPrefix + " Chunk permanently failed: " + chunkPos, ex);
						}

						loadingChunks.remove(chunkPos);
						checkCompletion.run();
						return null;
					});

				} catch (Throwable t) {
					finalFailures.add(chunkPos);
					failureReasons.merge(t.getClass(), 1, Integer::sum);
					instance.getPluginLogger().warn(logPrefix + " Exception during chunk load: " + chunkPos, t);
					loadingChunks.remove(chunkPos);
					checkCompletion.run();
				}

				processed++;
			}

			// Progress check (for ticks where nothing processed)
			if (progressCallback != null) {
				double progress = loadedChunks.get() / (double) totalChunks;
				int newProgress = (int) (progress * 100);
				if (newProgress != lastProgress.getAndSet(newProgress)) {
					progressCallback.accept(progress);
				}
			}
		};

		// Schedule main loading loop
		SchedulerTask scheduled = instance.getScheduler().sync().runRepeating(tickTask, 0L, 1L, null);
		taskRef.set(scheduled);

		// Timeout
		instance.getScheduler().sync().runLater(() -> {
			if (!future.isDone() && completed.getAndSet(true)) {
				SchedulerTask task = taskRef.get();
				if (task != null && !task.isCancelled()) {
					task.cancel();
				}
				future.completeExceptionally(new TimeoutException(
						logPrefix + " Chunk preload exceeded timeout of " + timeoutTicks + " ticks"));
			}
		}, timeoutTicks, LocationUtils.getAnyLocationInstance());

		return future;
	}

	/**
	 * Generates a list of {@link ChunkPos} sorted in a square spiral pattern
	 * starting from the given center chunk coordinates. Only the chunks present in
	 * the provided candidate set are included in the final list.
	 *
	 * <p>
	 * This method is typically used to preload chunks in a visually coherent or
	 * performance-friendly order, such as during island generation or block
	 * animation.
	 * </p>
	 *
	 * @param centerX    The X coordinate of the spiral center (in chunk units)
	 * @param centerZ    The Z coordinate of the spiral center (in chunk units)
	 * @param candidates A set of candidate {@link ChunkPos} to include
	 * @return A list of chunk positions sorted in outward spiral order from the
	 *         center
	 */
	@NotNull
	private List<ChunkPos> generateSpiralChunks(int centerX, int centerZ, @NotNull Set<ChunkPos> candidates) {
		List<ChunkPos> spiral = new ArrayList<>();
		Set<ChunkPos> remaining = new HashSet<>(candidates);

		int x = 0, z = 0;
		int dx = 0, dz = -1;
		int maxRadius = (int) Math.ceil(Math.sqrt(candidates.size())) + 4; // Max steps from center (safety net)

		for (int i = 0; i < maxRadius * maxRadius; i++) {
			ChunkPos pos = new ChunkPos(centerX + x, centerZ + z);
			if (remaining.contains(pos)) {
				spiral.add(pos);
				remaining.remove(pos);

				if (remaining.isEmpty())
					break;
			}

			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
				// Change direction
				int temp = dx;
				dx = -dz;
				dz = temp;
			}

			x += dx;
			z += dz;
		}

		return spiral;
	}

	/**
	 * Creates an invisible, non-collidable armor stand to serve as a camera anchor
	 * for the player. The player is set to spectator mode and forced to spectate
	 * the armor stand.
	 * 
	 * @param userData   The player who will spectate the armor stand.
	 * @param focusPoint The point the camera should focus on.
	 * @return The created armor stand used as the camera anchor.
	 */
	@Nullable
	public FakeArmorStand createCameraAnchor(@NotNull UserData userData, @NotNull Location focusPoint) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline()) {
			instance.getPluginLogger()
					.warn("createCameraAnchor: Player offline, skipping camera setup for " + userData.getName());
			return null; // Or null, or skip entirely
		}

		Location anchorLocation = focusPoint.clone().add(-8, 6, -8);
		anchorLocation.setDirection(focusPoint.clone().subtract(anchorLocation).toVector());

		World world = focusPoint.getWorld();
		if (world == null)
			return null;

		FakeArmorStand camera = VersionHelper.getNMSManager().createFakeArmorStand(anchorLocation);
		camera.invisible(true);
		camera.gravity(false);
		camera.basePlate(false);
		camera.marker(true);
		camera.spawn(player);

		cameraAnchors.put(userData.getUUID(), camera);

		ChunkUtils.teleportAsync(player, anchorLocation).thenRun(() -> {
			// Wait 1 tick to ensure player is fully loaded into world/chunk
			instance.getScheduler().sync().runLater(() -> {
				if (player == null || !player.isOnline()) {
					cleanupAnimation(userData);
					return;
				}
				camera.setCamera(player);

				startAnimationFor(userData.getUUID());
				enableFlightSafely(player);
			}, 1L, anchorLocation);
		});

		return camera;
	}

	private void enableFlightSafely(@NotNull Player viewer) {
		viewer.setGameMode(GameMode.SPECTATOR);
		viewer.setAllowFlight(true);
		viewer.setVelocity(new Vector(0, 0, 0));
		viewer.setFallDistance(0.0F);

		// Apply flying 1 tick later for client sync
		instance.getScheduler().sync().runLater(() -> {
			if (viewer.isOnline()) {
				viewer.setFlying(true);
			}
		}, 1L, viewer.getLocation());
	}

	/**
	 * Smoothly rotates a camera anchor (ArmorStand) to face a target location over
	 * time.
	 * <p>
	 * This method gradually adjusts the armor stand’s facing direction toward the
	 * target each tick, allowing for cinematic camera panning during island
	 * generation sequences.
	 * </p>
	 *
	 * @param stand         The armor stand acting as the camera anchor.
	 * @param target        The location to face during the animation.
	 * @param viewer        The player the armor stnad is hooked to.
	 * @param durationTicks The total duration of the animation in ticks.
	 */
	public void animateCamera(@NotNull FakeArmorStand stand, @NotNull Location target, @NotNull Player viewer,
			long durationTicks) {
		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		Runnable runnable = new Runnable() {
			long elapsed = 0;

			@Override
			public void run() {
				if (elapsed >= durationTicks || stand.isDead() || viewer == null || !viewer.isOnline()) {
					SchedulerTask task = taskRef.get();
					if (task != null && !task.isCancelled()) {
						task.cancel();
					}
					return;
				}

				Location current = stand.getLocation();
				Vector diff = target.clone().subtract(current).toVector();
				if (diff.lengthSquared() > 0.0001) {
					current.setDirection(diff.normalize());
				}
				sanitizeDirection(current);

				stand.moveAdaptive(viewer, current); // Use player-aware teleport
				stand.keepClientCameraStable(viewer);
				elapsed++;
			}
		};

		SchedulerTask task = instance.getScheduler().sync().runRepeating(runnable, 1L, 1L, target);
		taskRef.set(task);
		trackAnimation(viewer.getUniqueId(), task);
	}

	public void animateCameraEased(@NotNull FakeArmorStand stand, @NotNull Location from, @NotNull Location to,
			@NotNull Player viewer, long durationTicks) {
		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		Runnable runnable = new Runnable() {
			long elapsed = 0;

			@Override
			public void run() {
				if (elapsed >= durationTicks || stand.isDead() || !viewer.isOnline()) {
					SchedulerTask task = taskRef.get();
					if (task != null && !task.isCancelled()) {
						task.cancel();
					}
					return;
				}

				double t = (double) elapsed / durationTicks;
				// easeInOutQuad interpolation (feel free to change)
				double ease = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;

				double x = from.getX() + (to.getX() - from.getX()) * ease;
				double y = from.getY() + (to.getY() - from.getY()) * ease;
				double z = from.getZ() + (to.getZ() - from.getZ()) * ease;

				Location intermediate = new Location(from.getWorld(), x, y, z);
				Vector diff = to.clone().subtract(intermediate).toVector();
				if (diff.lengthSquared() > 0.0001) {
					intermediate.setDirection(diff.normalize());
				}
				sanitizeDirection(intermediate);

				stand.moveAdaptive(viewer, intermediate);
				stand.keepClientCameraStable(viewer);
				elapsed++;
			}
		};

		SchedulerTask task = instance.getScheduler().sync().runRepeating(runnable, 1L, 1L, to);
		taskRef.set(task);
		trackAnimation(viewer.getUniqueId(), task);
	}

	public void animateRotationOnly(@NotNull FakeArmorStand stand, @NotNull Location lookTarget, @NotNull Player viewer,
			long durationTicks) {
		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		Runnable runnable = new Runnable() {
			long elapsed = 0;

			@Override
			public void run() {
				if (elapsed >= durationTicks || stand.isDead() || !viewer.isOnline()) {
					SchedulerTask task = taskRef.get();
					if (task != null && !task.isCancelled()) {
						task.cancel();
					}
					return;
				}

				Location current = stand.getLocation();
				Vector diff = lookTarget.clone().subtract(current).toVector();
				if (diff.lengthSquared() > 0.0001) {
					current.setDirection(diff.normalize());
				}
				sanitizeDirection(current);

				stand.moveAdaptive(viewer, current);
				stand.keepClientCameraStable(viewer);
				elapsed++;
			}
		};

		SchedulerTask task = instance.getScheduler().sync().runRepeating(runnable, 1L, 1L, lookTarget);
		taskRef.set(task);
		trackAnimation(viewer.getUniqueId(), task);
	}

	public void animateFreeFly(@NotNull FakeArmorStand stand, @NotNull List<Location> path, @NotNull Player viewer,
			long tickInterval) {
		if (path.isEmpty())
			return;

		final Iterator<Location> iterator = path.iterator();
		final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (!iterator.hasNext() || stand.isDead() || !viewer.isOnline()) {
					SchedulerTask task = taskRef.get();
					if (task != null && !task.isCancelled())
						task.cancel();
					return;
				}

				Location next = iterator.next();
				Location current = stand.getLocation();
				Vector diff = next.clone().subtract(current).toVector();
				if (diff.lengthSquared() > 0.0001)
					next.setDirection(diff.normalize());
				sanitizeDirection(next);

				// Smooth movement via velocity instead of teleport
				stand.moveSmoothly(viewer, next, 1.0); // speed = 1 block/tick, adjust as needed
				stand.keepClientCameraStable(viewer);
			}
		};

		SchedulerTask task = instance.getScheduler().sync().runRepeating(runnable, tickInterval, tickInterval,
				path.get(0));
		taskRef.set(task);
		trackAnimation(viewer.getUniqueId(), task);
	}

	private void sanitizeDirection(Location loc) {
		if (!Double.isFinite(loc.getPitch())) {
			loc.setPitch(0f);
		}
		if (!Double.isFinite(loc.getYaw())) {
			loc.setYaw(0f);
		}
	}

	/**
	 * Starts a cinematic camera sequence for schematic placement.
	 * <p>
	 * Creates an invisible armor stand as the player's camera anchor, fades the
	 * player's screen, and optionally pans the camera around the focus point to add
	 * visual polish to schematic placement animations.
	 * </p>
	 *
	 * @param userData      The user data of the player whose camera will animate.
	 * @param focus         The focus location for the camera.
	 * @param durationTicks The total duration of the camera movement.
	 * @param panCamera     Whether to perform a camera panning motion or keep it
	 *                      fixed.
	 */
	public void startSchematicCameraAnimation(@NotNull UserData userData, @NotNull Location focus, long durationTicks,
			boolean panCamera) {
		Player player = userData.getPlayer();
		if (player == null || !player.isOnline())
			return;

		fadeScreen(player, 10, 30, 10);

		FakeArmorStand camera = createCameraAnchor(userData, focus);
		if (camera == null) {
			instance.getPluginLogger().warn("Failed to create camera anchor for: " + userData.getName());
			return;
		}

		if (panCamera) {
			animateCamera(camera, focus.clone().add(0, 2, 0), player, durationTicks);
		}
	}

	/**
	 * Applies a black fade effect to the player's screen.
	 * <p>
	 * Uses the NMS title packet to create a fade-in, stay, and fade-out visual
	 * transition for immersive animation sequences.
	 * </p>
	 *
	 * @param viewer  The player to apply the screen fade to.
	 * @param fadeIn  The duration (in ticks) of the fade-in effect.
	 * @param stay    The duration (in ticks) the screen remains fully black.
	 * @param fadeOut The duration (in ticks) of the fade-out effect.
	 */
	public void fadeScreen(@NotNull Player viewer, int fadeIn, int stay, int fadeOut) {
		VersionHelper.getNMSManager().sendTitle(viewer, "", "", fadeIn, stay, fadeOut);
	}

	/**
	 * Marks a player as currently undergoing an island or schematic animation.
	 * <p>
	 * This flag is used to temporarily restrict player actions and interactions
	 * during cinematic or animated sequences.
	 * </p>
	 *
	 * @param playerId The unique ID of the player to mark as animating.
	 */
	private void startAnimationFor(@NotNull UUID playerId) {
		animatingPlayers.add(playerId);
	}

	/**
	 * Removes a player's animation state, restoring their normal interaction
	 * ability.
	 *
	 * @param playerId The unique ID of the player to unmark.
	 */
	public boolean stopAnimationFor(@NotNull UUID playerId) {
		return animatingPlayers.remove(playerId);
	}

	/**
	 * Checks if a player is currently in an animation sequence.
	 *
	 * @param playerId The unique ID of the player to check.
	 * @return True if the player is animating, false otherwise.
	 */
	public boolean isAnimating(@NotNull UUID playerId) {
		return animatingPlayers.contains(playerId);
	}

	/**
	 * Marks a player as currently undergoing an island or schematic generation.
	 * <p>
	 * This flag is used to temporarily restrict player ownership checks and
	 * interactions during generation of their island.
	 * </p>
	 *
	 * @param playerId The unique ID of the player to mark as generating.
	 */
	public void startGeneration(@NotNull UUID playerId) {
		generatingPlayers.add(playerId);
	}

	/**
	 * Removes a player's generation state, restoring their normal interaction
	 * ability.
	 *
	 * @param playerId The unique ID of the player to unmark.
	 */
	public boolean endGeneration(@NotNull UUID playerId) {
		return generatingPlayers.remove(playerId);
	}

	/**
	 * Checks if a player is currently generating an island.
	 *
	 * @param playerId The unique ID of the player to check.
	 * @return True if the player is generating, false otherwise.
	 */
	public boolean isGenerating(@NotNull UUID playerId) {
		return generatingPlayers.contains(playerId);
	}

	/**
	 * Cleans up an active player animation, restoring their game state and camera.
	 * <p>
	 * This method removes camera anchors, cancels any running animation tasks,
	 * resets the player's spectator mode, and teleports them back to a safe
	 * location (typically spawn). If the player is offline, a mailbox flag is
	 * queued for later cleanup.
	 * </p>
	 *
	 * @param ownerData The unique data of the player whose animation should be
	 *                  cleaned up.
	 */
	public void cleanupAnimation(@NotNull UserData ownerData) {
		UUID ownerId = ownerData.getUUID();
		boolean stopped = stopAnimationFor(ownerId);

		instance.getScheduler().executeSync(() -> {
			Player player = ownerData.getPlayer();

			// Cancel and remove any animation tasks
			stopAllAnimations(ownerId);

			// Destroy camera anchor (if present and visible)
			FakeArmorStand anchor = cameraAnchors.remove(ownerId);
			if (anchor != null && !anchor.isDead() && player != null && player.isOnline()) {
				anchor.resetCamera(player);
				anchor.destroy(player);
			}

			if (!stopped) {
				return;
			}

			if (player != null && player.isOnline()) {
				player.setGameMode(GameMode.SURVIVAL);
				player.setAllowFlight(false);
				player.setFlying(false);
				player.setVelocity(new Vector(0, 0, 0));
				if (ownerData.getHellblockData().getHomeLocation() != null) {
					instance.getHellblockHandler().teleportPlayerToHome(ownerData);
				} else {
					instance.getHellblockHandler().teleportToSpawn(player, true);
				}
			} else {
				instance.getMailboxManager().queueMailbox(ownerId, MailboxFlag.RESET_GAMEMODE);
			}
		});
	}

	/**
	 * Tracks an ongoing animation task for a player.
	 * <p>
	 * Ensures that only one active animation task exists per player by cancelling
	 * any previously running task before replacing it with the new one.
	 * </p>
	 *
	 * @param playerId The unique ID of the player.
	 * @param task     The new animation task to track.
	 */
	public void trackAnimation(@NotNull UUID playerId, @NotNull SchedulerTask task) {
		runningAnimations.computeIfAbsent(playerId, __ -> new ArrayList<>()).add(task);
	}

	public void stopAllAnimations(@NotNull UUID playerId) {
		List<SchedulerTask> tasks = runningAnimations.remove(playerId);
		if (tasks != null) {
			tasks.forEach(task -> {
				if (task != null && !task.isCancelled()) {
					task.cancel();
				}
			});
		}
	}

	/**
	 * Tracks a scheduled animation task for a glowstone tree or other non-player
	 * animation.
	 * <p>
	 * This method ensures only one animation task is active per tree animation ID
	 * by cancelling any existing task associated with the same ID before tracking
	 * the new one.
	 * <p>
	 * Tree animation IDs are typically derived from world-relative locations using
	 * {@link UUID#nameUUIDFromBytes(byte[])} to guarantee uniqueness across
	 * different trees.
	 *
	 * @param animationId A unique identifier for the animation (e.g., based on tree
	 *                    location).
	 * @param task        The scheduled task responsible for the animation sequence.
	 */
	public void trackTreeAnimation(@NotNull UUID animationId, @NotNull SchedulerTask task) {
		SchedulerTask previous = treeAnimations.remove(animationId);
		if (previous != null && !previous.isCancelled()) {
			previous.cancel();
		}
		treeAnimations.put(animationId, task);
	}

	/**
	 * Cancels and removes a running glowstone tree animation task using its
	 * animation ID.
	 * <p>
	 * This is used when an animation completes naturally or needs to be aborted
	 * early due to an error or external event (e.g., plugin reload).
	 *
	 * @param animationId The UUID used to track the animation task, typically
	 *                    derived from tree location.
	 */
	public void cleanupTreeAnimation(@NotNull UUID animationId) {
		SchedulerTask task = treeAnimations.remove(animationId);
		if (task != null && !task.isCancelled()) {
			task.cancel();
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (isAnimating(player.getUniqueId())) {
			instance.getStorageManager().getOnlineUser(player.getUniqueId())
					.ifPresent(userData -> cleanupAnimation(userData));
		}
		if (isGenerating(player.getUniqueId()))
			endGeneration(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!isAnimating(player.getUniqueId()))
			return;

		String msg = event.getMessage().toLowerCase(Locale.ROOT);
		boolean allowed = instance.getConfigManager().commandWhitelist().stream().anyMatch(msg::startsWith);
		if (!allowed) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!isAnimating(player.getUniqueId()))
			return;

		// allow looking around but cancel position changes
		if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY()
				|| event.getFrom().getZ() != event.getTo().getZ()) {
			event.setTo(event.getFrom());
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (isAnimating(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		if (isAnimating(player.getUniqueId())) {
			instance.getStorageManager().getOnlineUser(player.getUniqueId())
					.ifPresent(userData -> cleanupAnimation(userData));
		}
		if (isGenerating(player.getUniqueId()))
			endGeneration(player.getUniqueId());
	}

	/**
	 * Inserts a single block change into the provided change map asynchronously.
	 * <p>
	 * This utility method retrieves the current block state and, if the position is
	 * air or empty, schedules it to be replaced with the given material in the
	 * provided map. This is suitable for async pre-generation and structure
	 * blueprints.
	 * </p>
	 *
	 * @param changes  The map storing pos3-to-customblockstate changes for a build
	 *                 stage.
	 * @param world    The world containing the target block.
	 * @param x        The X-coordinate of the block.
	 * @param y        The Y-coordinate of the block.
	 * @param z        The Z-coordinate of the block.
	 * @param material The material type to set for the block.
	 * @return A CompletableFuture that completes once the block mapping is done.
	 */
	private CompletableFuture<Void> putBlock(@NotNull Map<Pos3, CustomBlockState> changes,
			@NotNull HellblockWorld<?> world, int x, int y, int z, @NotNull Material material) {
		Pos3 pos = new Pos3(x, y, z);

		return world.getBlockState(pos).thenAccept(optionalState -> {
			// Skip replacing non-air blocks (ensures safe blueprint generation)
			if (optionalState.isPresent() && !optionalState.get().isAir()) {
				return;
			}

			CustomBlock block = CustomBlockTypes.fromMaterial(material);
			CustomBlockState state = block.createBlockState();
			changes.put(pos, state);
		});
	}

	/**
	 * Creates a new map representing a single block placement asynchronously.
	 * <p>
	 * This overload is useful for cases where a full map isn’t yet built, such as
	 * for isolated decorations or single-block additions in structure layers.
	 * </p>
	 *
	 * @param world    The world containing the target block.
	 * @param x        The X-coordinate of the block.
	 * @param y        The Y-coordinate of the block.
	 * @param z        The Z-coordinate of the block.
	 * @param material The material type to set for the block.
	 * @return A CompletableFuture completing with a map containing a single block
	 *         entry.
	 */
	private CompletableFuture<Map<Pos3, CustomBlockState>> putBlock(@NotNull HellblockWorld<?> world, int x, int y,
			int z, @NotNull Material material) {
		Map<Pos3, CustomBlockState> map = new LinkedHashMap<>();
		return putBlock(map, world, x, y, z, material).thenApply(v -> map);
	}

	/**
	 * Fills a rectangular area at a fixed height asynchronously with the given
	 * material type.
	 * <p>
	 * Iterates from {@code (minX, minZ)} to {@code (maxX, maxZ)} at the specified
	 * Y-coordinate, adding all air blocks in that region to a resulting map. This
	 * is non-blocking and suitable for use in async pre-generation.
	 * </p>
	 *
	 * @param world    The world containing the target area.
	 * @param y        The Y-level (height) at which to fill.
	 * @param minX     The minimum X-coordinate of the rectangular region.
	 * @param maxX     The maximum X-coordinate of the rectangular region.
	 * @param minZ     The minimum Z-coordinate of the rectangular region.
	 * @param maxZ     The maximum Z-coordinate of the rectangular region.
	 * @param material The material type to fill the area with.
	 * @return A CompletableFuture completing with the map of all filled block
	 *         states.
	 */
	private CompletableFuture<Map<Pos3, CustomBlockState>> fillLayer(@NotNull HellblockWorld<?> world, int y, int minX,
			int maxX, int minZ, int maxZ, @NotNull Material material) {
		Map<Pos3, CustomBlockState> map = new LinkedHashMap<>();
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		CustomBlock block = CustomBlockTypes.fromMaterial(material);

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				Pos3 pos = new Pos3(x, y, z);
				CompletableFuture<Void> future = world.getBlockState(pos).thenAccept(optional -> {
					if (optional.isEmpty() || optional.get().isAir()) {
						map.put(pos, block.createBlockState());
					}
				});
				futures.add(future);
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> map);
	}

	/**
	 * Fills a square-shaped pattern centered at the given coordinates
	 * asynchronously with the specified material type.
	 * <p>
	 * Only air blocks within the given radius are included to prevent overwriting
	 * existing structures. Ideal for decorative layers or pattern-based shapes.
	 * </p>
	 *
	 * @param changes  The map storing pos3-to-customblockstate changes for a build
	 *                 stage.
	 * @param world    The world containing the target area.
	 * @param centerX  The X-coordinate of the center point.
	 * @param y        The Y-level (height) at which to fill.
	 * @param centerZ  The Z-coordinate of the center point.
	 * @param radius   The radius (in blocks) from the center to fill.
	 * @param material The material type to use for filling.
	 * @return A CompletableFuture that completes once all eligible blocks are added
	 *         to the map.
	 */
	private CompletableFuture<Void> fillPattern(@NotNull Map<Pos3, CustomBlockState> changes,
			@NotNull HellblockWorld<?> world, int centerX, int y, int centerZ, int radius, @NotNull Material material) {
		CustomBlock block = CustomBlockTypes.fromMaterial(material);
		CustomBlockState state = block.createBlockState();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				Pos3 pos = new Pos3(centerX + dx, y, centerZ + dz);
				CompletableFuture<Void> future = world.getBlockState(pos).thenAccept(optional -> {
					if (optional.isEmpty() || optional.get().isAir()) {
						changes.put(pos, state);
					}
				});
				futures.add(future);
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	/**
	 * Checks if the given ItemStack has the "isStarterChestItem" tag.
	 * 
	 * @param item The ItemStack to check.
	 * @return True if the tag is present and true, false otherwise.
	 */
	public boolean checkChestData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).hasTag("HellblockChest", "isStarterChestItem");
	}

	/**
	 * Retrieves the "isStarterChestItem" tag from the given ItemStack.
	 * 
	 * @param item The ItemStack to check.
	 * @return True if the tag is present and true, false otherwise.
	 */
	public boolean getChestData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).getOptional("HellblockChest", "isStarterChestItem").asBoolean();
	}

	/**
	 * Sets or removes the "isStarterChestItem" tag on the given ItemStack.
	 * 
	 * @param item The ItemStack to modify.
	 * @param data If true, sets the tag; if false, removes it.
	 * @return The modified ItemStack, or null if the input was null or AIR.
	 */
	@Nullable
	public ItemStack setChestData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.<ItemStack>edit(item,
				(Consumer<RtagItem>) tag -> tag.set(data, "HellblockChest", "isStarterChestItem"));
	}
}