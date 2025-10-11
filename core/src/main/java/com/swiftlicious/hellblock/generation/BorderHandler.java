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
import org.bukkit.World;
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

public final class BorderHandler implements Reloadable {

	protected final HellblockPlugin instance;

	// Rendering constants (used by particle border)
	private static final int Y_HEIGHT_DIFFERENCE = 10;
	private static final double Z_GAP = 2;
	private static final double X_GAP = 2;
	private static final double PLAYER_DISTANCE = 50;

	// Caches & state
	private final Map<UUID, CachedBorder> activeBorders = new ConcurrentHashMap<>();
	private final Set<UUID> playersWithActiveAnimation = ConcurrentHashMap.newKeySet();
	private final Map<UUID, PlayerBorderTask> playerTasks = new ConcurrentHashMap<>();

	public BorderHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		// Start player tasks for currently online users in the correct world
		for (UserData data : instance.getStorageManager().getOnlineUsers()) {
			final Player player = data.getPlayer();
			if (player == null) {
				continue;
			}
			if (instance.getHellblockHandler().isInCorrectWorld(player)) {
				startBorderTask(player);
			}
		}

		// Start a periodic failsafe to restart missing tasks
		startFailsafeWatcher();
	}

	@Override
	public void unload() {
		activeBorders.clear();
		playersWithActiveAnimation.clear();

		// Stop all player tasks
		new HashSet<>(playerTasks.keySet()).forEach(this::stopBorderTask);
	}

	/**
	 * Start per-player border update task (runs periodically to show/hide borders).
	 */
	public void startBorderTask(Player player) {
		final UUID uuid = player.getUniqueId();
		if (playerTasks.containsKey(uuid)) {
			return; // already running
		}

		final PlayerBorderTask task = new PlayerBorderTask(uuid);
		task.start();
		playerTasks.put(uuid, task);
	}

	/**
	 * Stop and remove per-player task.
	 */
	public void stopBorderTask(UUID uuid) {
		final PlayerBorderTask task = playerTasks.remove(uuid);
		if (task != null) {
			task.cancelBorderTask();
		}
	}

	/**
	 * Failsafe watcher: every 60s ensure tasks exist for online players in correct
	 * world.
	 */
	public void startFailsafeWatcher() {
		instance.getScheduler().sync().runRepeating(() -> {
			for (UserData data : instance.getStorageManager().getOnlineUsers()) {
				final Player player = data.getPlayer();
				if (player == null) {
					continue;
				}
				if (instance.getHellblockHandler().isInCorrectWorld(player)) {
					final UUID uuid = player.getUniqueId();
					if (!playerTasks.containsKey(uuid)) {
						instance.debug("Restarting missing border task for " + player.getName());
						startBorderTask(player);
					}
				}
			}
		}, 20L * 60, 20L * 60, LocationUtils.getAnyLocationInstance()); // every 60s
	}

	public class PlayerBorderTask implements Runnable {

		private final UUID playerUUID;
		private SchedulerTask borderCheckTask = null;

		/**
		 * Creates an async repeating task that checks whether the player should see
		 * their border. Runs once per second (you can change 1 -> smaller value if you
		 * want snappier updates).
		 */
		public PlayerBorderTask(@NotNull UUID playerUUID) {
			this.playerUUID = playerUUID;
		}

		public void start() {
			// Prevent double-start
			if (this.borderCheckTask != null)
				return;

			// Schedule repeating task first
			this.borderCheckTask = instance.getScheduler().asyncRepeating(this, 0, 250, TimeUnit.MILLISECONDS);

			// Only register the task *after* it’s actually scheduled
			playerTasks.put(playerUUID, this);
		}

		@Override
		public void run() {
			final Player player = Bukkit.getPlayer(playerUUID);
			if (player == null || !player.isOnline()) {
				cancelBorderTask();
				return;
			}

			// If wrong world, stop task (onWorldChange will restart if they re-enter
			// correct world)
			if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
				cancelBorderTask();
				return;
			}

			// If currently animating expansion for this player, skip normal updates
			if (isAnimating(playerUUID)) {
				return;
			}

			final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(playerUUID);
			if (onlineUser.isEmpty()) {
				return;
			}

			final UserData user = onlineUser.get();
			final HellblockData data = user.getHellblockData();
			if (data == null || data.getBoundingBox() == null) {
				// no hellblock or invalid bounds -> nothing to show
				return;
			}

			final BoundingBox bounds = data.getBoundingBox();
			final UUID ownerUUID = data.getOwnerUUID();

			// Show RED border only if player is owner or in party and is inside their
			// island bounds
			if (bounds.contains(player.getBoundingBox())
					&& (ownerUUID.equals(playerUUID) || data.getParty().contains(playerUUID))) {

				setWorldBorder(player, bounds, BorderColor.RED);
			} else {
				// Non-members or owners outside bounds -> clear any border
				clearPlayerBorder(player);
			}
		}

		/**
		 * Cancel the scheduled task, clear border for player, and remove from tracking
		 * map.
		 */
		public void cancelBorderTask() {
			if (!borderCheckTask.isCancelled()) {
				borderCheckTask.cancel();
			}

			final Player player = Bukkit.getPlayer(playerUUID);
			if (player != null && player.isOnline()) {
				clearPlayerBorder(player);
			}

			playerTasks.remove(playerUUID);
		}

		/**
		 * Send or render the world border for the player. Uses particle border if
		 * configured; otherwise sends NMS world-border packet. Caches the active border
		 * so we don't repeatedly send identical updates.
		 */
		private void setWorldBorder(Player player, BoundingBox bounds, BorderColor borderColor) {
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
		 * Clears any active world border for a player and removes it from cache.
		 */
		private void clearPlayerBorder(Player player) {
			activeBorders.remove(player.getUniqueId());

			if (!instance.getConfigManager().useParticleBorder()) {
				VersionHelper.getNMSManager().clearWorldBorder(player);
			}
		}

		/**
		 * If called externally: clear border when the player is outside given bounds.
		 */
		public void clearIfOutside(Player player, BoundingBox bounds) {
			if (bounds == null) {
				clearPlayerBorder(player);
				return;
			}

			if (!bounds.contains(player.getBoundingBox())) {
				clearPlayerBorder(player);
			}
		}

		/**
		 * Wrapper to spawn particle border for a number of seconds (default 10
		 * seconds).
		 */
		public void spawnBorderParticles(Player player, BoundingBox bounds, PlayerBorder playerBorder, int seconds) {
			final ChunkOutlineEntry entry = new ChunkOutlineEntry(player, bounds, player.getLocation().getBlockY(),
					playerBorder);
			entry.display(seconds);
		}

		public void spawnBorderParticles(Player player, BoundingBox bounds, PlayerBorder playerBorder) {
			spawnBorderParticles(player, bounds, playerBorder, 10);
		}
	} // end PlayerBorderTask

	// ----------------- PlayerBorder abstraction -----------------
	public interface PlayerBorder {
		/**
		 * Called when engine decides to show a single particle/vertex at x,y,z for the
		 * specified player.
		 */
		void sendBorderDisplay(Player player, double x, double y, double z);
	}

	/**
	 * ChunkOutlineEntry draws a rectangular outline (by spawning particles at
	 * edges). It is scheduled to display repeatedly for a given number of seconds.
	 */
	public record ChunkOutlineEntry(Player player, BoundingBox bounds, int yHeight, PlayerBorder playerBorder) {

		/**
		 * Display the outline for given seconds. Schedules tasks on the plugin
		 * scheduler.
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
		 * Called each tick to spawn particles for the rectangle edges. Uses X_GAP /
		 * Z_GAP to reduce particle density and checks player distance.
		 */
		private void onParticle(Location min, Location max, Location defaultLocation) {
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
		 * Spawn a particle vertex at the given block coordinate (converted to center of
		 * the block). Only calls the PlayerBorder sender if within PLAYER_DISTANCE of
		 * the player.
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
	 * ColoredBorder uses particle API to spawn dust particles in the chosen color.
	 * Used for particle-based borders only.
	 */
	public class ColoredBorder implements PlayerBorder {
		private final Color color;

		public ColoredBorder(Color color) {
			this.color = color;
		}

		@Override
		public void sendBorderDisplay(Player player, double x, double y, double z) {
			// particle: dust/redstone (DustOptions) - size 1.5 as used earlier
			player.spawnParticle(ParticleUtils.getParticle(Particle.DUST.name()), x, y, z, 0, 0, 0, 0, 0,
					new Particle.DustOptions(color, 1.5F));
		}
	}

	// Simple cache holder for active border per player
	private class CachedBorder {
		final BoundingBox bounds;
		final BorderColor color;

		CachedBorder(BoundingBox bounds, BorderColor color) {
			this.bounds = bounds;
			this.color = color;
		}
	}

	// Animation state helpers
	public boolean isAnimating(UUID uuid) {
		return playersWithActiveAnimation.contains(uuid);
	}

	public void setAnimating(UUID uuid, boolean animating) {
		if (animating) {
			playersWithActiveAnimation.add(uuid);
		} else {
			playersWithActiveAnimation.remove(uuid);
		}
	}

	/**
	 * Animate an island range expansion. - Members (owner + party) inside island
	 * will see GREEN expansion animation. - Others see nothing. After expansion
	 * finishes, members' borders revert to RED.
	 */
	public void animateRangeExpansion(HellblockData data, int oldRange, int newRange) {
		if (oldRange >= newRange) {
			return;
		}

		final Location center = data.getHellblockLocation();
		final World world = center.getWorld();

		final double startSize = oldRange * 2.0;
		final double endSize = newRange * 2.0;
		final int steps = 40;
		final int tickDelay = 2;
		final long durationTicks = (long) steps * tickDelay;
		final long durationMs = durationTicks * 50L;

		final boolean useParticleBorder = instance.getConfigManager().useParticleBorder();
		final BoundingBox islandBounds = data.getBoundingBox();
		final UUID ownerUUID = data.getOwnerUUID();

		final List<Player> players = new ArrayList<>(world.getPlayers());
		players.forEach(p -> setAnimating(p.getUniqueId(), true));

		// Determine which players will see the GREEN expansion; others see nothing
		final Map<UUID, CompletableFuture<BorderColor>> colorFutures = new HashMap<>();
		players.forEach(player -> {
			final UUID playerUUID = player.getUniqueId();
			if (islandBounds != null && islandBounds.contains(player.getBoundingBox())
					&& (ownerUUID.equals(playerUUID) || data.getParty().contains(playerUUID))) {
				// Owner & party inside island -> GREEN during expansion
				colorFutures.put(playerUUID, CompletableFuture.completedFuture(BorderColor.GREEN));
			} else {
				// Non-members -> no border during expansion
				colorFutures.put(playerUUID, CompletableFuture.completedFuture(null));
			}
		});

		final CompletableFuture<Void> all = CompletableFuture
				.allOf(colorFutures.values().toArray(CompletableFuture[]::new));

		all.thenRun(() -> {
			final Map<UUID, BorderColor> resolved = new HashMap<>();
			colorFutures.forEach((k, v) -> resolved.put(k, v.join()));

			if (useParticleBorder) {
				// Particle animation for expansion
				final AtomicInteger stepCounter = new AtomicInteger(0);
				final SchedulerTask[] taskHolder = new SchedulerTask[1];

				taskHolder[0] = instance.getScheduler().sync().runRepeating(() -> {
					final int step = stepCounter.incrementAndGet();
					final double progress = (double) step / steps;
					final double size = startSize + (endSize - startSize) * progress;

					final BoundingBox currentBox = BoundingBox.of(center, size / 2.0, 256, size / 2.0);

					players.forEach(player -> {
						final BorderColor borderColor = resolved.get(player.getUniqueId());
						if (borderColor == null) {
							clearPlayerBorder(player);
							return;
						}

						// Expansion color is GREEN
						final Color color = Color.LIME;
						final PlayerBorder playerBorder = new ColoredBorder(color);
						final ChunkOutlineEntry outline = new ChunkOutlineEntry(player, currentBox,
								player.getLocation().getBlockY(), playerBorder);

						outline.display(1);
					});

					if (step >= steps) {
						taskHolder[0].cancel();
						players.forEach(p -> {
							setAnimating(p.getUniqueId(), false);
							// After expansion -> revert to RED for members still inside the island
							if (islandBounds.contains(p.getBoundingBox()) && (ownerUUID.equals(p.getUniqueId())
									|| data.getParty().contains(p.getUniqueId()))) {
								setWorldBorder(p, islandBounds, BorderColor.RED);
							} else {
								clearPlayerBorder(p);
							}
						});
					}
				}, 0L, tickDelay, center);
			} else {
				// Packet-based world border animation
				players.forEach(player -> {
					final BorderColor borderColor = resolved.get(player.getUniqueId());
					if (borderColor == null) {
						clearPlayerBorder(player);
						return;
					}

					VersionHelper.getNMSManager().updateWorldBorder(player, center, startSize, endSize, durationMs,
							borderColor);
				});

				// After animation -> revert to RED for members inside island
				players.forEach(p -> {
					setAnimating(p.getUniqueId(), false);
					if (islandBounds.contains(p.getBoundingBox())
							&& (ownerUUID.equals(p.getUniqueId()) || data.getParty().contains(p.getUniqueId()))) {
						setWorldBorder(p, islandBounds, BorderColor.RED);
					} else {
						clearPlayerBorder(p);
					}
				});
			}
		}).exceptionally(ex -> {
			instance.getPluginLogger().severe("Error preparing border animation", ex);
			players.forEach(p -> setAnimating(p.getUniqueId(), false));
			return null;
		});
	}

	/**
	 * Utility: clear any particle-based border for player (used inside lambdas).
	 * Delegates to the PlayerBorderTask.clearPlayerBorder where appropriate, but we
	 * also need a convenience method here when called from outside (e.g., animation
	 * code).
	 */
	private void clearPlayerBorder(@Nullable Player player) {
		if (player == null) {
			return;
		}
		if (!instance.getConfigManager().useParticleBorder()) {
			VersionHelper.getNMSManager().clearWorldBorder(player);
		}
	}

	/**
	 * Utility: set a player's world border using NMS or particle method. This
	 * mirrors the logic in PlayerBorderTask.setWorldBorder, but provided for
	 * convenience when called from other places (e.g., after animations).
	 *
	 * Note: prefer using the PlayerBorderTask.setWorldBorder instance method where
	 * possible.
	 */
	private void setWorldBorder(@Nullable Player player, BoundingBox bounds, BorderColor color) {
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