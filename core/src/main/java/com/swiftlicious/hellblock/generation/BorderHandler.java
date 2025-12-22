package com.swiftlicious.hellblock.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.border.BorderColor;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;

/**
 * Handles the initialization, scheduling, and lifecycle of per-player world
 * border tasks.
 * <p>
 * The {@code BorderHandler} is responsible for:
 * <ul>
 * <li>Starting and stopping per-player border update tasks.</li>
 * <li>Maintaining a failsafe mechanism to ensure all eligible online players
 * have active border tasks.</li>
 * <li>Responding to changes in world or player state to control border
 * visibility dynamically.</li>
 * </ul>
 *
 * <p>
 * This handler ensures that players only see borders when they are in the
 * correct world and meet criteria such as ownership or party membership of an
 * island. It supports both particle-based and NMS packet-based border
 * rendering.
 *
 * <p>
 * Implements {@link Reloadable} to support runtime reloading of configuration
 * or scheduled tasks.
 *
 * <p>
 * Thread-safe for task control and designed for efficient border visibility
 * updates.
 */
public final class BorderHandler implements Reloadable {

	protected final HellblockPlugin instance;

	// Rendering constants (used by particle border)

	/**
	 * Vertical range (above and below player Y) within which border particles are
	 * drawn.
	 * <p>
	 * For example, if {@code yHeight} is 70 and this value is 10, particles are
	 * drawn from Y=60 to Y=80.
	 */
	private static final int Y_HEIGHT_DIFFERENCE = 10;

	/**
	 * Horizontal spacing (in blocks) between spawned particles along the Z-axis.
	 * <p>
	 * Higher values reduce particle density for performance.
	 */
	private static final double Z_GAP = 2;

	/**
	 * Horizontal spacing (in blocks) between spawned particles along the X-axis.
	 * <p>
	 * Higher values reduce particle density for performance.
	 */
	private static final double X_GAP = 2;

	/**
	 * Maximum distance (in blocks) a player can be from the requested border
	 * location before particle rendering is skipped.
	 * <p>
	 * Prevents unnecessary rendering when the player has moved too far away.
	 */
	private static final double PLAYER_DISTANCE = 50;

	// Caches & state
	private final Map<UUID, CachedBorder> activeBorders = new ConcurrentHashMap<>();
	private final Set<UUID> playersWithActiveAnimation = ConcurrentHashMap.newKeySet();
	private final Map<UUID, PlayerBorderTask> playerTasks = new ConcurrentHashMap<>();

	private SchedulerTask failsafeBorderTask = null;

	public BorderHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		// Start player tasks for currently online users in the correct world
		for (UserData data : instance.getStorageManager().getOnlineUsers()) {
			final Player player = data.getPlayer();
			if (player == null || !player.isOnline()) {
				continue;
			}
			if (instance.getHellblockHandler().isInCorrectWorld(player)) {
				startBorderTask(data.getUUID());
			}
		}

		// Start a periodic failsafe to restart missing tasks
		startFailsafeWatcher();
	}

	@Override
	public void unload() {
		// Instantly complete any active animations to final size
		for (UUID playerId : new HashSet<>(this.playersWithActiveAnimation)) {
			Player player = Bukkit.getPlayer(playerId);
			if (player != null && player.isOnline()) {
				HellblockData data = instance.getStorageManager().getOnlineUser(playerId)
						.map(UserData::getHellblockData).orElse(null);
				if (data != null && data.getBoundingBox() != null) {
					// Show final border instantly at new expanded range
					setWorldBorder(player, data.getBoundingBox(), BorderColor.RED);
				}
			}
		}
		this.playersWithActiveAnimation.clear();
		this.activeBorders.clear();
		if (this.failsafeBorderTask != null && !this.failsafeBorderTask.isCancelled())
			this.failsafeBorderTask.cancel();

		// Stop all player tasks
		new HashSet<>(this.playerTasks.keySet()).forEach(this::stopBorderTask);
	}

	/**
	 * Starts a periodic task that manages the visibility of a border for a specific
	 * player.
	 * <p>
	 * If a task is already running for the given player, this method will return
	 * without doing anything.
	 *
	 * @param playerId The UUID of the player for whom the border task should be
	 *                 started. Must not be null.
	 */
	public PlayerBorderTask startBorderTask(@NotNull UUID playerId) {
		if (playerTasks.containsKey(playerId)) {
			return playerTasks.get(playerId); // already running
		}

		final PlayerBorderTask task = new PlayerBorderTask(playerId);
		task.start();
		return playerTasks.put(playerId, task);
	}

	/**
	 * Stops and removes the running border task for the given player, if one
	 * exists.
	 * <p>
	 * This method safely cancels the border task associated with the player's UUID.
	 *
	 * @param playerId The UUID of the player for whom the border task should be
	 *                 stopped. Must not be null.
	 */
	public void stopBorderTask(@NotNull UUID playerId) {
		final PlayerBorderTask task = playerTasks.remove(playerId);
		if (task != null) {
			task.cancelBorderTask();
		}
	}

	/**
	 * Starts a failsafe watcher task that runs every 60 seconds to ensure that all
	 * online players who are in the correct world have an active border task.
	 * <p>
	 * This task helps to recover from potential failures where a border task might
	 * have stopped unexpectedly. It ensures consistency by restarting any missing
	 * border tasks for players who are online and in the correct game world as
	 * defined by {@link HellblockHandler#isInCorrectWorld(Player)}.
	 */
	public void startFailsafeWatcher() {
		failsafeBorderTask = instance.getScheduler().sync().runRepeating(() -> {
			for (UserData data : instance.getStorageManager().getOnlineUsers()) {
				final Player player = data.getPlayer();
				if (player == null || !player.isOnline()) {
					continue;
				}
				if (instance.getHellblockHandler().isInCorrectWorld(player)) {
					final UUID uuid = player.getUniqueId();
					if (!playerTasks.containsKey(uuid)) {
						instance.debug("Restarting missing border task for " + player.getName());
						startBorderTask(uuid);
					}
				}
			}
		}, 20L * 60, 20L * 60, LocationUtils.getAnyLocationInstance()); // every 60s
	}

	/**
	 * A task that manages a player's personal world border display.
	 * <p>
	 * This task runs periodically (default: every 250ms) and checks if a player is
	 * in the correct world, online, and eligible to see their world border.
	 * Depending on the configuration, it either shows a particle border or sends an
	 * NMS-based border update.
	 *
	 * <p>
	 * If the player is offline or in the wrong world, the task cancels itself. It
	 * also caches border state to avoid redundant updates.
	 */
	public class PlayerBorderTask implements Runnable {

		private final UUID playerId;
		private SchedulerTask borderCheckTask = null;

		/**
		 * Constructs a new PlayerBorderTask for a given player's UUID. This task will
		 * later be scheduled to run periodically to update the player's border.
		 *
		 * @param playerId The UUID of the player to manage.
		 */
		public PlayerBorderTask(@NotNull UUID playerId) {
			this.playerId = playerId;
		}

		/**
		 * Starts the scheduled task that updates the player's border visibility.
		 * <p>
		 * Prevents multiple starts by checking if the task is already running.
		 */
		public PlayerBorderTask start() {
			// Prevent double-start
			if (this.borderCheckTask != null)
				return playerTasks.get(playerId);

			// Schedule repeating task first
			this.borderCheckTask = instance.getScheduler().asyncRepeating(this, 0, 250, TimeUnit.MILLISECONDS);

			// Only register the task *after* it’s actually scheduled
			return playerTasks.put(playerId, this);
		}

		/**
		 * Periodically called to update the player's border.
		 * <p>
		 * Cancels the task if the player is offline or in the wrong world. Shows a red
		 * border if the player is within bounds and is either the owner or in the
		 * party.
		 */
		@Override
		public void run() {
			final Player player = Bukkit.getPlayer(playerId);
			if (player == null || !player.isOnline()) {
				cancelBorderTask();
				instance.getBorderHandler().setBorderExpanding(playerId, false); // ensure cleanup
				return;
			}

			// If wrong world, stop task (onWorldChange will restart if they re-enter
			// correct world)
			if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
				cancelBorderTask();
				return;
			}

			// If currently animating expansion for this player, skip normal updates
			if (isBorderExpanding(playerId)) {
				return;
			}

			final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerId);
			if (onlineUser.isEmpty()) {
				return;
			}

			if (instance.getPlacementDetector().getIslandIdAt(player.getLocation()) == null) {
				return;
			}

			final UserData user = onlineUser.get();
			final HellblockData data = user.getHellblockData();
			if (data.getBoundingBox() == null || instance.getIslandGenerator().isAnimating(playerId)
					|| instance.getHellblockHandler().creationProcessing(playerId)
					|| instance.getHellblockHandler().resetProcessing(playerId)
					|| instance.getIslandGenerator().isGenerating(playerId)
					|| instance.getIslandChoiceGUIManager().isGeneratingIsland(playerId)
					|| instance.getSchematicGUIManager().isGeneratingSchematic(playerId)) {
				// no hellblock or invalid bounds -> nothing to show
				return;
			}

			final BoundingBox bounds = data.getBoundingBox();

			// Show RED border only if player is owner or in party and is inside their
			// island bounds
			instance.getScheduler().executeSync(() -> {
				if (bounds.contains(player.getLocation().toVector()) && data.getPartyPlusOwner().contains(playerId)) {
					setWorldBorder(player, bounds, BorderColor.RED);
				} else {
					// Non-members or owners outside bounds -> clear any border
					clearPlayerBorder(player);
				}
			});
		}

		/**
		 * Cancels the scheduled task, clears any displayed border for the player, and
		 * removes the player from the task tracking map.
		 */
		public PlayerBorderTask cancelBorderTask() {
			if (borderCheckTask != null && !borderCheckTask.isCancelled()) {
				borderCheckTask.cancel();
			}

			final Player player = Bukkit.getPlayer(playerId);
			if (player != null && player.isOnline()) {
				clearPlayerBorder(player);
			}

			return playerTasks.remove(playerId);
		}

		/**
		 * Sets the world border for the given player using either particles or packets.
		 * <p>
		 * Caches the border state and avoids resending if there are no changes.
		 *
		 * @param player      The player to show the border to.
		 * @param bounds      The bounding box representing the border limits.
		 * @param borderColor The color to use (RED/GREEN).
		 */
		private void setWorldBorder(@NotNull Player player, @NotNull BoundingBox bounds,
				@NotNull BorderColor borderColor) {
			final CachedBorder current = activeBorders.get(player.getUniqueId());
			if (current != null && current.color == borderColor && current.bounds.equals(bounds)) {
				// nothing changed
				return;
			}

			activeBorders.put(player.getUniqueId(), new CachedBorder(bounds, borderColor));

			if (instance.getConfigManager().useParticleBorder()) {
				final Color color = switch (borderColor) {
				case GREEN -> Color.LIME;
				case RED -> Color.RED;
				default -> Color.RED;
				};
				spawnBorderParticles(player, bounds, new ColoredBorder(color));
			} else {
				VersionHelper.getNMSManager().sendWorldBorder(player, bounds, borderColor);
			}
		}

		/**
		 * Clears any existing border for the player and removes it from the active
		 * cache.
		 * <p>
		 * If using packet borders, it will also send a border clear packet.
		 *
		 * @param player The player whose border should be cleared.
		 */
		private void clearPlayerBorder(@NotNull Player player) {
			activeBorders.remove(player.getUniqueId());

			if (!instance.getConfigManager().useParticleBorder()) {
				VersionHelper.getNMSManager().clearWorldBorder(player);
			}
		}

		/**
		 * External helper method to clear the player's border if they are outside the
		 * given bounds.
		 *
		 * @param player The player to check and clear border for.
		 * @param bounds The bounds to test the player's location against.
		 */
		public void clearIfOutside(@NotNull Player player, @Nullable BoundingBox bounds) {
			if (bounds == null) {
				clearPlayerBorder(player);
				return;
			}

			if (!bounds.contains(player.getLocation().toVector())) {
				clearPlayerBorder(player);
			}
		}

		/**
		 * Spawns a particle-based border around the given bounding box for the player
		 * for a specific number of seconds.
		 *
		 * @param player       The player to display the border to.
		 * @param bounds       The bounds of the border.
		 * @param playerBorder The rendering logic implementation.
		 * @param seconds      How long to display the particle border.
		 */
		public void spawnBorderParticles(@NotNull Player player, @NotNull BoundingBox bounds,
				@NotNull PlayerBorder playerBorder, int seconds) {
			final ChunkOutlineEntry entry = new ChunkOutlineEntry(player, bounds, player.getLocation().getBlockY(),
					playerBorder);
			entry.display(seconds);
		}

		/**
		 * Overload of
		 * {@link #spawnBorderParticles(Player, BoundingBox, PlayerBorder, int)}.
		 * <p>
		 * Displays the border for the default duration (10 seconds).
		 *
		 * @param player       The player to show the border to.
		 * @param bounds       The bounds to display.
		 * @param playerBorder The particle rendering logic.
		 */
		public void spawnBorderParticles(@NotNull Player player, @NotNull BoundingBox bounds,
				@NotNull PlayerBorder playerBorder) {
			spawnBorderParticles(player, bounds, playerBorder, 10);
		}
	}

	/**
	 * Interface for rendering individual border points.
	 * <p>
	 * Implementations of this interface define how each border vertex is rendered
	 * for the player.
	 */
	public interface PlayerBorder {
		/**
		 * Called when the border rendering engine decides to display a particle or
		 * effect at a specific point.
		 *
		 * @param player The player to show the particle to.
		 * @param x      The X-coordinate of the vertex.
		 * @param y      The Y-coordinate of the vertex.
		 * @param z      The Z-coordinate of the vertex.
		 */
		void sendBorderDisplay(@NotNull Player player, double x, double y, double z);
	}

	/**
	 * ChunkOutlineEntry draws a rectangular outline (by spawning particles at the
	 * edges of a bounding box). It is scheduled to display particles periodically
	 * for a given number of seconds.
	 * <p>
	 * Used for particle-based border visualization around a region such as an
	 * island.
	 */
	public record ChunkOutlineEntry(Player player, BoundingBox bounds, int yHeight, PlayerBorder playerBorder) {

		/**
		 * Schedules this border outline to be displayed every second for the given
		 * number of seconds.
		 * <p>
		 * Uses the plugin scheduler to run the particle display logic once per second.
		 *
		 * @param seconds Number of seconds to show the border.
		 */
		public void display(int seconds) {
			final Location min = new Location(player.getWorld(), bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
			final Location max = new Location(player.getWorld(), bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
			final Location defaultLocation = player.getLocation();

			for (int i = 0; i < seconds; i++) {
				final int delayTicks = i * 20;
				// Use the plugin scheduler to run particle display per second
				HellblockPlugin.getInstance().getScheduler().sync()
						.runLater(() -> onParticle(min, max, defaultLocation), delayTicks, player.getLocation());
			}
		}

		/**
		 * Called once per second to spawn particles at the rectangular border edges.
		 * <p>
		 * Skips rendering if the player has moved too far from the original location
		 * where the border was requested.
		 *
		 * @param min             The minimum location of the bounding box.
		 * @param max             The maximum location of the bounding box.
		 * @param defaultLocation The location of the player when the border was
		 *                        requested (used for distance check).
		 */
		private void onParticle(@NotNull Location min, @NotNull Location max, @NotNull Location defaultLocation) {
			if (!Objects.requireNonNull(defaultLocation.getWorld()).equals(player.getWorld())) {
				return;
			}

			// If player moved too far from where the outline was requested, skip this tick.
			if (defaultLocation.distanceSquared(player.getLocation()) > 250) {
				return;
			}

			for (int y = yHeight - Y_HEIGHT_DIFFERENCE; y <= yHeight + Y_HEIGHT_DIFFERENCE; y++) {
				for (int x = min.getBlockX(), xCount = 0; x <= max.getBlockX(); x++, xCount++) {
					if (xCount % X_GAP == 0) {
						spawnParticle(x, y, min.getBlockZ());
						spawnParticle(x, y, max.getBlockZ());
					}
				}

				for (int z = min.getBlockZ(), zCount = 0; z <= max.getBlockZ(); z++, zCount++) {
					if (zCount % Z_GAP == 0) {
						spawnParticle(min.getBlockX(), y, z);
						spawnParticle(max.getBlockX(), y, z);
					}
				}
			}
		}

		/**
		 * Spawns a particle at the specified block coordinates if within viewing
		 * distance of the player.
		 * <p>
		 * Particle is positioned at the center of the block using +0.5 offsets.
		 *
		 * @param x The X-coordinate of the block.
		 * @param y The Y-coordinate of the block.
		 * @param z The Z-coordinate of the block.
		 */
		private void spawnParticle(double x, double y, double z) {
			final Location particleLocation = new Location(player.getWorld(), x + 0.5, y + 0.5, z + 0.5);
			final double playerDistance = player.getLocation().distance(particleLocation);

			if (playerDistance <= PLAYER_DISTANCE) {
				playerBorder.sendBorderDisplay(player, x + 0.5, y + 0.5, z + 0.5);
			}
		}
	}

	/**
	 * ColoredBorder is an implementation of {@link PlayerBorder} that uses colored
	 * dust particles to visually render border vertices. Only used when particle
	 * borders are enabled in config.
	 */
	public class ColoredBorder implements PlayerBorder {
		private final Color color;

		/**
		 * Constructs a ColoredBorder with the specified color.
		 *
		 * @param color The color of the dust particle to display.
		 */
		public ColoredBorder(@NotNull Color color) {
			this.color = color;
		}

		/**
		 * Sends a colored particle to the player at the specified location using
		 * redstone dust.
		 *
		 * @param player The player to display the border to.
		 * @param x      The X-coordinate to spawn the particle at.
		 * @param y      The Y-coordinate to spawn the particle at.
		 * @param z      The Z-coordinate to spawn the particle at.
		 */
		@Override
		public void sendBorderDisplay(@NotNull Player player, double x, double y, double z) {
			// particle: dust/redstone (DustOptions) - size 1.5 as used earlier
			player.spawnParticle(ParticleUtils.getParticle("REDSTONE"), x, y, z, 0, 0, 0, 0, 0,
					new Particle.DustOptions(color, 1.5F));
		}
	}

	/**
	 * A simple internal data class used to cache a player’s currently active
	 * border.
	 * <p>
	 * Helps prevent sending redundant border updates.
	 */
	private class CachedBorder {
		private final BoundingBox bounds;
		private final BorderColor color;

		/**
		 * Creates a new CachedBorder for the specified bounding box and color.
		 *
		 * @param bounds The bounding box of the border.
		 * @param color  The color of the border.
		 */

		CachedBorder(@NotNull BoundingBox bounds, BorderColor color) {
			this.bounds = bounds;
			this.color = color;
		}
	}

	/**
	 * Checks whether the specified player is currently undergoing a border
	 * animation.
	 * <p>
	 * Animated borders temporarily suppress the default border update logic.
	 *
	 * @param playerId The UUID of the player.
	 * @return True if the player has an active animation; false otherwise.
	 */
	public boolean isBorderExpanding(@NotNull UUID playerId) {
		return playersWithActiveAnimation.contains(playerId);
	}

	/**
	 * Sets the animation state for a player. While animating, standard border
	 * rendering is suppressed.
	 *
	 * @param playerId  The UUID of the player.
	 * @param animating True to mark as animating; false to clear animation state.
	 */
	public boolean setBorderExpanding(@NotNull UUID playerId, boolean animating) {
		return animating ? playersWithActiveAnimation.add(playerId) : playersWithActiveAnimation.remove(playerId);
	}

	/**
	 * Animates a visual island range expansion for all players in the world.
	 * <p>
	 * Behavior:
	 * <ul>
	 * <li>Island members (owner + party) currently inside the island see a GREEN
	 * border animation expanding to the new size.</li>
	 * <li>Non-members or those outside the island during animation see no border
	 * during the animation.</li>
	 * <li>After the animation finishes, the members' borders revert to RED if still
	 * inside the island; others get their border cleared.</li>
	 * </ul>
	 * <p>
	 * The animation respects the configured border rendering mode (particle-based
	 * or NMS-based).
	 * <p>
	 * <b>Note:</b> This method ensures border consistency post-animation and
	 * sets/reset animation state per player.
	 *
	 * @param data     The HellblockData representing the island and its members.
	 * @param oldRange The previous range (radius) of the island.
	 * @param newRange The new expanded range (radius) to animate to.
	 */
	public void animateRangeExpansion(@NotNull HellblockData data, int oldRange, int newRange) {
		if (oldRange >= newRange) {
			instance.debug("Skipping animation: oldRange >= newRange (" + oldRange + " >= " + newRange + ")");
			return;
		}

		final Location center = data.getHellblockLocation();
		if (center == null) {
			throw new IllegalArgumentException(
					"Failed to retrieve center of hellblock island for islandId=" + data.getIslandId());
		}

		final double startSize = oldRange * 2.0;
		final double endSize = newRange * 2.0;
		final int steps = 40;
		final int tickDelay = 2;
		final long durationTicks = (long) steps * tickDelay;
		final long durationMs = durationTicks * 50L;

		final boolean useParticleBorder = instance.getConfigManager().useParticleBorder();
		final BoundingBox islandBounds = data.getBoundingBox();
		final Set<UUID> islandMembers = data.getPartyPlusOwner();

		final List<Player> players = new ArrayList<>(islandMembers.stream().map(Bukkit::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline).toList());
		players.forEach(p -> setBorderExpanding(p.getUniqueId(), true));
		instance.debug("Starting range expansion animation from " + startSize + " to " + endSize
				+ " blocks for island at " + center);

		final Map<UUID, CompletableFuture<BorderColor>> colorFutures = new HashMap<>();
		players.forEach(player -> {
			final UUID playerUUID = player.getUniqueId();
			if (islandBounds != null && islandBounds.contains(player.getLocation().toVector())
					&& islandMembers.contains(playerUUID)) {
				colorFutures.put(playerUUID, CompletableFuture.completedFuture(BorderColor.GREEN));
				instance.debug("Player " + player.getName() + " will see GREEN expansion animation.");
			} else {
				colorFutures.put(playerUUID, CompletableFuture.completedFuture(null));
				instance.debug("Player " + player.getName()
						+ " will NOT see border animation (not inside island or not a member).");
			}
		});

		final CompletableFuture<Void> all = CompletableFuture
				.allOf(colorFutures.values().toArray(CompletableFuture[]::new));

		all.thenRun(() -> {
			final Map<UUID, BorderColor> resolved = new HashMap<>();
			colorFutures.forEach((uuid, future) -> resolved.put(uuid, future.join()));

			if (useParticleBorder) {
				// Particle animation
				instance.debug("Using PARTICLE-based border expansion animation.");

				final AtomicInteger stepCounter = new AtomicInteger(0);
				final SchedulerTask[] taskHolder = new SchedulerTask[1];

				taskHolder[0] = instance.getScheduler().sync().runRepeating(() -> {
					final int step = stepCounter.incrementAndGet();
					final double progress = (double) step / steps;
					final double size = startSize + (endSize - startSize) * progress;

					final BoundingBox currentBox = BoundingBox.of(center, size / 2.0, 256, size / 2.0);

					players.forEach(player -> {
						if (!player.getWorld().getName().equalsIgnoreCase(center.getWorld().getName())) {
							clearPlayerBorder(player);
							setBorderExpanding(player.getUniqueId(), false);
							return; // skip this player
						}
						if (islandBounds != null && !islandBounds.contains(player.getLocation().toVector())) {
							clearPlayerBorder(player);
							return; // skip particle frame
						}
						final BorderColor borderColor = resolved.get(player.getUniqueId());
						if (borderColor == null) {
							clearPlayerBorder(player);
							return;
						}

						final Color color = Color.LIME;
						final PlayerBorder playerBorder = new ColoredBorder(color);
						final ChunkOutlineEntry outline = new ChunkOutlineEntry(player, currentBox,
								player.getLocation().getBlockY(), playerBorder);

						outline.display(1);
					});

					if (step >= steps) {
						if (taskHolder[0] != null && !taskHolder[0].isCancelled())
							taskHolder[0].cancel();
						instance.debug("Expansion animation complete. Reverting player borders to RED or clearing.");
						players.forEach(p -> {
							setBorderExpanding(p.getUniqueId(), false);
							if (islandBounds != null && islandBounds.contains(p.getLocation().toVector())
									&& islandMembers.contains(p.getUniqueId())) {
								setWorldBorder(p, islandBounds, BorderColor.RED);
							} else {
								clearPlayerBorder(p);
							}
						});
					}
				}, 0L, tickDelay, center);

			} else {
				// NMS packet animation
				instance.debug("Using NMS packet-based border expansion animation.");

				players.forEach(player -> {
					if (!player.getWorld().getName().equalsIgnoreCase(center.getWorld().getName())) {
						clearPlayerBorder(player);
						setBorderExpanding(player.getUniqueId(), false);
						return; // skip this player
					}
					if (islandBounds != null && !islandBounds.contains(player.getLocation().toVector())) {
						clearPlayerBorder(player);
						return; // skip particle frame
					}
					final BorderColor borderColor = resolved.get(player.getUniqueId());
					if (borderColor == null) {
						clearPlayerBorder(player);
						return;
					}

					VersionHelper.getNMSManager().updateWorldBorder(player, center, startSize, endSize, durationMs,
							borderColor);
				});

				instance.getScheduler().sync().runLater(() -> {
					instance.debug("Expansion animation complete (NMS). Reverting borders.");
					players.forEach(p -> {
						setBorderExpanding(p.getUniqueId(), false);
						if (islandBounds != null && islandBounds.contains(p.getLocation().toVector())
								&& islandMembers.contains(p.getUniqueId())) {
							setWorldBorder(p, islandBounds, BorderColor.RED);
						} else {
							clearPlayerBorder(p);
						}
					});
				}, durationTicks, center);
			}
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Error preparing border animation", ex);
			players.forEach(p -> setBorderExpanding(p.getUniqueId(), false));
			return null;
		});
	}

	/**
	 * Utility: clear any particle-based border for a player (used inside lambdas).
	 * <p>
	 * This method is a convenience wrapper to clear the player's border from
	 * outside the {@code PlayerBorderTask}, such as during or after animations.
	 * <p>
	 * It delegates to the underlying NMS method only if particle borders are
	 * disabled.
	 * <p>
	 * <b>Note:</b> If you are calling from within a PlayerBorderTask, prefer
	 * {@code PlayerBorderTask.clearPlayerBorder()} for accurate tracking.
	 *
	 * @param player The player whose border should be cleared. Null-safe.
	 */
	public void clearPlayerBorder(@Nullable Player player) {
		if (player == null) {
			return;
		}
		if (!instance.getConfigManager().useParticleBorder()) {
			VersionHelper.getNMSManager().clearWorldBorder(player);
		}
	}

	/**
	 * Utility: sets a player's world border using either NMS packets or particle
	 * rendering.
	 * <p>
	 * This method mirrors {@code PlayerBorderTask.setWorldBorder()} but is provided
	 * as a convenience for use outside task scheduling (e.g., after animations or
	 * on events).
	 * <p>
	 * It checks whether the player’s current border is already identical to the new
	 * one, avoiding unnecessary updates. The chosen method of rendering depends on
	 * the config.
	 * <p>
	 * <b>Note:</b> Prefer {@code PlayerBorderTask.setWorldBorder()} when working
	 * inside player tasks.
	 *
	 * @param player The player to whom the border should be shown. Null-safe.
	 * @param bounds The bounding box defining the area to outline.
	 * @param color  The color of the border (e.g., RED or GREEN).
	 */
	public void setWorldBorder(@Nullable Player player, BoundingBox bounds, BorderColor color) {
		if (player == null) {
			return;
		}
		final CachedBorder current = activeBorders.get(player.getUniqueId());
		if (current != null && current.color == color && current.bounds.equals(bounds)) {
			return;
		}
		activeBorders.put(player.getUniqueId(), new CachedBorder(bounds, color));

		if (instance.getConfigManager().useParticleBorder()) {
			final Color mapped = (color == BorderColor.GREEN) ? Color.LIME : Color.RED;
			final PlayerBorder pb = new ColoredBorder(mapped);
			new ChunkOutlineEntry(player, bounds, player.getLocation().getBlockY(), pb).display(10);
		} else {
			VersionHelper.getNMSManager().sendWorldBorder(player, bounds, color);
		}
	}
}