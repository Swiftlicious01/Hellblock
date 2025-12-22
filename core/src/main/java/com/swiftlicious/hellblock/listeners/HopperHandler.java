package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.sound.Sound;

/**
 * Manages hopper tracking for Hellblock islands, including registration,
 * counting, cleanup, and enforcement of per-island hopper limits.
 * <p>
 * This class maintains an in-memory registry of all hoppers placed within each
 * island's bounding box and provides methods for:
 * <ul>
 * <li>Registering and unregistering hoppers in real time</li>
 * <li>Scanning and reindexing hoppers after world edits or server restarts</li>
 * <li>Tracking and enforcing hopper limits on a per-island basis</li>
 * <li>Sending warnings to players when limits are exceeded</li>
 * <li>Handling cleanup of hoppers removed due to explosions or island
 * changes</li>
 * </ul>
 * <p>
 * Hopper warnings are throttled to avoid spamming players, and scheduled checks
 * ensure that long-term violations are caught. Most operations are asynchronous
 * and designed to integrate with persistent user data and world state.
 * <p>
 * This class assumes that all hopper-related actions occur within valid
 * Hellblock worlds and coordinates are tied to valid island bounding boxes.
 *
 */
public class HopperHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final Set<String> HOPPER_KEYS = Set.of("minecraft:hopper", "hellblock:hopper");

	private final Map<BoundingBox, Set<Pos3>> islandHoppers = new HashMap<>();
	// Throttle map: per-island last warning timestamp
	private final Map<Integer, Long> lastHopperWarnAt = new HashMap<>();

	// Throttle duration (5 minutes)
	private static final long HOPPER_WARN_THROTTLE = 5 * 60 * 1000L;

	// Store task id or handle so we can cancel it later
	private SchedulerTask hopperWarningTask;

	public HopperHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		instance.getIslandManager().getAllIslandIds().thenCompose(this::scanAndCountHoppersOnLoad)
				.thenAccept(successfulCount -> instance.getPluginLogger().info("Finished scanning hoppers for "
						+ successfulCount + " island" + (successfulCount == 1 ? "" : "s") + "."))
				.exceptionally(ex -> {
					instance.getPluginLogger().severe("Failed to scan hoppers", ex);
					return null;
				}).thenRun(() -> startHopperWarningScheduler());
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		clearAllHoppers();
		lastHopperWarnAt.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onHopperPlace(BlockPlaceEvent event) {
		final Player player = event.getPlayer();

		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlock();
		if (block.getType() != Material.HOPPER) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			// Fetch user data async
			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();
				final BoundingBox bounds = hellblockData.getBoundingBox();
				if (bounds == null) {
					return CompletableFuture.completedFuture(false);
				}

				// Back to main thread for Bukkit API
				return instance.getScheduler().callSync(() -> {
					final int placed = countHoppers(bounds);
					final int max = hellblockData.getMaxHopperLimit();

					if (placed > max) {
						event.setCancelled(true);
						return triggerImmediateHopperWarning(hellblockData.getIslandId(), player);
					}

					return CompletableFuture.completedFuture(registerHopper(Pos3.from(block.getLocation()), bounds));
				});
			});
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onHopperBreak(BlockBreakEvent event) {
		final Player player = event.getPlayer();

		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlock();
		if (block.getType() != Material.HOPPER) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			// Trigger immediate warning (actor gets msg, limits from owner)
			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();
				final BoundingBox bounds = hellblockData.getBoundingBox();
				if (bounds == null) {
					return CompletableFuture.completedFuture(false);
				}

				return instance.getScheduler().callSync(() -> {
					unregisterHopper(Pos3.from(block.getLocation()), bounds);
					return triggerImmediateHopperWarning(hellblockData.getIslandId(), player);
				});
			});
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		if (event.blockList().isEmpty()) {
			return;
		}

		handleExplodedBlocks(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.blockList().isEmpty()) {
			return;
		}

		handleExplodedBlocks(event.blockList());
	}

	/**
	 * Handles hopper cleanup when blocks are destroyed by an explosion.
	 * <p>
	 * This method checks all exploded blocks and unregisters any hoppers that were
	 * destroyed, ensuring the hopper registry stays in sync with the world state.
	 * Only blocks in the correct Hellblock world are processed, and ownership is
	 * resolved to update the appropriate island's hopper list.
	 *
	 * @param explodedBlocks The list of blocks affected by the explosion event.
	 */
	private void handleExplodedBlocks(@NotNull List<Block> explodedBlocks) {
		for (Block block : explodedBlocks) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;

			if (block.getType() != Material.HOPPER) {
				continue;
			}

			instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(ownerUUID -> {
				if (ownerUUID == null) {
					return CompletableFuture.completedFuture(null);
				}

				return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false)
						.thenCompose(optData -> {
							if (optData.isEmpty()) {
								return CompletableFuture.completedFuture(false);
							}

							final UserData ownerData = optData.get();
							final HellblockData hellblockData = ownerData.getHellblockData();
							final BoundingBox bounds = hellblockData.getBoundingBox();
							if (bounds == null) {
								return CompletableFuture.completedFuture(false);
							}

							// Always unregister on main thread
							return instance.getScheduler().callSync(() -> CompletableFuture
									.completedFuture(unregisterHopper(Pos3.from(block.getLocation()), bounds)));
						});
			});
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		final InventoryHolder holder = event.getInitiator().getHolder();
		if (holder == null || !(holder instanceof Hopper hopper)) {
			return;
		}

		final Location loc = hopper.getLocation();
		final Block block = loc.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
			return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				return CompletableFuture.completedFuture(null);
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optData -> {
				if (optData.isEmpty()) {
					return;
				}

				final UserData ownerData = optData.get();
				final HellblockData hellblockData = ownerData.getHellblockData();
				final BoundingBox bounds = hellblockData.getBoundingBox();
				if (bounds == null) {
					return;
				}

				instance.getScheduler().runSync(() -> {
					final int allowed = hellblockData.getMaxHopperLimit();
					final int total = countHoppers(bounds);

					if (total > allowed) {
						final List<Pos3> hopperList = new ArrayList<>(getHoppers(bounds));
						final Block placed = block;
						for (int i = allowed; i < hopperList.size(); i++) {
							if (hopperList.get(i).equals(Pos3.from(placed.getLocation()))) {
								event.setCancelled(true);
								return;
							}
						}
					}
				});
			});
		});
	}

	/**
	 * Sends warning messages to island owners who have exceeded their allowed
	 * hopper limits.
	 * <p>
	 * This method checks all provided island IDs for hopper overuse and sends
	 * throttled in-game notifications to online owners. Warnings are rate-limited
	 * to prevent spamming, and only sent if the island exceeds the hopper cap and
	 * the owner is online.
	 * <p>
	 * This is typically used in a scheduled task to periodically remind players to
	 * reduce their hopper usage.
	 *
	 * @param islandIds A collection of island IDs to check and possibly warn.
	 * @return A {@link CompletableFuture} completing with {@code true} if at least
	 *         one warning was sent; {@code false} if no warnings were issued or all
	 *         were throttled/skipped.
	 */
	@NotNull
	private CompletableFuture<Boolean> sendHopperWarnings(@NotNull Collection<Integer> islandIds) {
		final long now = System.currentTimeMillis();
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (int islandId : islandIds) {
			CompletableFuture<Boolean> future = getHopperCount(islandId)
					.thenCompose(placed -> instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false)
							.thenApply(optData -> Map.entry(placed, optData)).thenCompose(pair -> {
								int placedHoppers = pair.getKey();
								Optional<UserData> optData = pair.getValue();

								if (optData.isEmpty())
									return CompletableFuture.completedFuture(false);

								final UserData ownerData = optData.get();
								final HellblockData hellblockData = ownerData.getHellblockData();
								final int allowed = hellblockData.getMaxHopperLimit();

								final boolean shouldWarn = placedHoppers > allowed
										&& shouldSendHopperWarning(hellblockData, now, HOPPER_WARN_THROTTLE);

								if (!shouldWarn)
									return CompletableFuture.completedFuture(false);

								final UUID ownerUUID = hellblockData.getOwnerUUID();
								if (ownerUUID == null)
									return CompletableFuture.completedFuture(false);

								final Player owner = Bukkit.getPlayer(ownerUUID);
								if (owner == null || !owner.isOnline())
									return CompletableFuture.completedFuture(false);

								return instance.getScheduler().callSync(() -> {
									instance.getSenderFactory().wrap(owner).sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_HOPPER_EXCESS_WARNING.arguments(
													AdventureHelper
															.miniMessageToComponent(String.valueOf(placedHoppers)),
													AdventureHelper.miniMessageToComponent(String.valueOf(allowed)))
													.build()));

									AdventureHelper.playSound(instance.getSenderFactory().getAudience(owner),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
									return CompletableFuture.completedFuture(true);
								});
							}).exceptionally(ex -> {
								instance.getPluginLogger()
										.severe("Failed to send hopper warning for islandId=" + islandId, ex);
								return false;
							}));

			futures.add(future);
		}

		// Combine all futures and collect results
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> futures.stream().map(CompletableFuture::join).anyMatch(Boolean::booleanValue));
	}

	/**
	 * Immediately warns a player if the island they interacted with has exceeded
	 * its hopper limit.
	 * <p>
	 * This bypasses throttling and is used to directly notify the triggering player
	 * (not necessarily the island owner) when their action results in a hopper
	 * overflow.
	 * <p>
	 * The warning includes an in-game message and a sound effect.
	 *
	 * @param islandId The ID of the island being checked.
	 * @param actor    The player who triggered the warning (will receive the
	 *                 message).
	 * @return A {@link CompletableFuture} completing with {@code true} if a warning
	 *         was sent; {@code false} otherwise (e.g., limit not exceeded or data
	 *         missing).
	 */
	private CompletableFuture<Boolean> triggerImmediateHopperWarning(int islandId, @NotNull Player actor) {
		return getHopperCount(islandId).thenCompose(placed -> {
			return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false)
					.thenApply(optData -> Map.entry(placed, optData)).thenCompose(pair -> {
						int placedHoppers = pair.getKey();
						Optional<UserData> optData = pair.getValue();

						if (optData.isEmpty()) {
							return CompletableFuture.completedFuture(false);
						}

						final UserData ownerData = optData.get();
						final HellblockData hellblockData = ownerData.getHellblockData();
						final int allowed = hellblockData.getMaxHopperLimit();

						if (placedHoppers > allowed) {
							final Sender sender = instance.getSenderFactory().wrap(actor);

							instance.getScheduler().executeSync(() -> {
								sender.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_HOPPER_LIMIT_REACHED
												.arguments(
														AdventureHelper.miniMessageToComponent(String.valueOf(allowed)))
												.build()));

								AdventureHelper.playSound(instance.getSenderFactory().getAudience(actor),
										Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
												net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
							});

							// Missing return added here
							return CompletableFuture.completedFuture(true);
						}

						// Hopper limit not exceeded
						return CompletableFuture.completedFuture(false);
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe(
								"Failed to trigger hopper warning for max hopper count exceeding island's count for islandId="
										+ islandId + " to player " + actor.getName(),
								ex);
						ex.printStackTrace();
						return false;
					});
		});
	}

	/**
	 * Determines whether a hopper warning should be sent to the island owner based
	 * on the last warning time and a throttle duration.
	 *
	 * @param data           The Hellblock data containing island information.
	 * @param now            The current time in milliseconds.
	 * @param throttleMillis The minimum interval (in milliseconds) between
	 *                       warnings.
	 * @return {@code true} if a warning should be sent; {@code false} otherwise.
	 */
	private boolean shouldSendHopperWarning(@NotNull HellblockData data, long now, long throttleMillis) {
		final int islandId = data.getIslandId();
		if (islandId <= 0) {
			return false;
		}
		final long last = lastHopperWarnAt.getOrDefault(islandId, 0L);

		if (!(now - last >= throttleMillis)) {
			return false;
		}
		lastHopperWarnAt.put(islandId, now);
		return true;
	}

	/**
	 * Starts or restarts the scheduled task responsible for sending hopper warnings
	 * to island owners. This task runs periodically on the main thread. Existing
	 * scheduled tasks are cancelled before a new one is started.
	 */
	private void startHopperWarningScheduler() {
		// Cancel the existing task if it's running
		if (hopperWarningTask != null && !hopperWarningTask.isCancelled()) {
			hopperWarningTask.cancel();
			hopperWarningTask = null;
		}

		hopperWarningTask = instance.getScheduler().sync().runRepeating(
				() -> instance.getCoopManager().getCachedIslandOwnerData().thenAccept(userDataCollection -> {
					List<Integer> islandIds = userDataCollection.stream()
							.map(userData -> userData.getHellblockData().getIslandId()).filter(Objects::nonNull)
							.toList();

					sendHopperWarnings(islandIds).thenAccept(result -> {
						if (result) {
							instance.debug("Hopper warnings sent to at least one island.");
						}
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe("Error while sending hopper warnings", ex);
						return null;
					});
				}), 0L, 20L * 60 * 5, LocationUtils.getAnyLocationInstance());
	}

	/**
	 * Registers a hopper block within the specified island bounding box.
	 *
	 * @param hopperPos The position of the hopper block.
	 * @param bounds    The bounding box representing the island.
	 * @return {@code true} if the hopper was not already registered and was added;
	 *         {@code false} if it was already present.
	 */
	public boolean registerHopper(@NotNull Pos3 hopperPos, @NotNull BoundingBox bounds) {
		return islandHoppers.computeIfAbsent(bounds, k -> new HashSet<>()).add(hopperPos);
	}

	/**
	 * Unregisters a hopper block from the specified island bounding box.
	 *
	 * @param hopperPos The position of the hopper block.
	 * @param bounds    The bounding box representing the island.
	 * @return {@code true} if the hopper was found and removed; {@code false}
	 *         otherwise.
	 */
	public boolean unregisterHopper(@NotNull Pos3 hopperPos, @NotNull BoundingBox bounds) {
		final Set<Pos3> hoppers = islandHoppers.get(bounds);
		if (hoppers == null) {
			return false;
		}

		boolean removed = hoppers.remove(hopperPos);
		if (hoppers.isEmpty()) {
			islandHoppers.remove(bounds);
		}

		return removed;
	}

	/**
	 * Returns the number of registered hopper blocks within the given bounding box.
	 *
	 * @param bounds The bounding box of the island.
	 * @return The total count of hoppers in the bounding box.
	 */
	public int countHoppers(@NotNull BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet()).size();
	}

	/**
	 * Retrieves all registered hopper positions within the given bounding box.
	 *
	 * @param bounds The bounding box of the island.
	 * @return A set of all registered hopper positions for the island.
	 */
	@NotNull
	public Set<Pos3> getHoppers(@NotNull BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet());
	}

	/**
	 * Asynchronously retrieves the number of registered hoppers for a given island
	 * ID. This method uses offline user data to resolve the island's bounding box.
	 *
	 * @param islandId The ID of the island.
	 * @return A {@link CompletableFuture} containing the hopper count.
	 */
	@NotNull
	public CompletableFuture<Integer> getHopperCount(int islandId) {
		return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false).thenApply(optData -> {
			if (optData.isEmpty()) {
				return 0;
			}

			final UserData ownerData = optData.get();
			final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();
			return bounds == null ? 0 : countHoppers(bounds);
		});
	}

	/**
	 * Transfers all hoppers from one bounding box to another. Typically used when
	 * an island is resized or moved.
	 *
	 * @param from The source bounding box (old location).
	 * @param to   The destination bounding box (new location).
	 * @return {@code true} if hoppers were transferred; {@code false} if there were
	 *         none.
	 */
	public boolean transferHoppers(@NotNull BoundingBox from, @NotNull BoundingBox to) {
		final Set<Pos3> fromSet = islandHoppers.remove(from);
		if (fromSet == null || fromSet.isEmpty()) {
			return false;
		}

		return islandHoppers.computeIfAbsent(to, k -> new HashSet<>()).addAll(fromSet);
	}

	/**
	 * Clears all registered hoppers from all islands. Use this method before
	 * performing a full rescan to prevent duplicates.
	 */
	public void clearAllHoppers() {
		islandHoppers.clear();
	}

	/**
	 * Clears all hopper positions associated with the specified bounding box.
	 *
	 * @param bounds The bounding box to clear hoppers from. If {@code null}, no
	 *               action is taken.
	 * @return A set of removed hopper positions, or an empty set if none were
	 *         found.
	 */
	@NotNull
	public Set<Pos3> clearHoppers(@Nullable BoundingBox bounds) {
		return bounds == null ? new HashSet<>() : islandHoppers.remove(bounds);
	}

	/**
	 * Removes a specific hopper block from all registered bounding boxes, if
	 * present.
	 *
	 * @param hopperPos The position of the hopper block to remove.
	 */
	public void clearHopper(@NotNull Pos3 hopperPos) {
		islandHoppers.values().forEach(hopperSet -> hopperSet.remove(hopperPos));
	}

	/**
	 * Invalidates the cached warning timestamp for a specific island.
	 *
	 * @param islandId The ID of the island.
	 * @return The previous timestamp when a hopper warning was last sent, or
	 *         {@code 0} if none existed.
	 */
	public long invalidateHopperWarningCache(int islandId) {
		return lastHopperWarnAt.remove(islandId);
	}

	/**
	 * Rebuilds and reindexes the hopper registry for a specific island.
	 * <p>
	 * This method should be used after external modifications to the world (e.g.,
	 * WorldEdit operations, rollbacks, etc.) to ensure that all hopper blocks
	 * within the island's bounding box are correctly registered again.
	 * <p>
	 * It fetches the owner's island data, clears existing hopper registrations,
	 * scans the saved block positions for hoppers, and registers any valid ones
	 * found.
	 *
	 * @param world    The world context in which the island exists.
	 * @param islandId The ID of the island to reindex hoppers for.
	 * @return A {@link CompletableFuture} that completes with the number of hoppers
	 *         reindexed.
	 */
	@NotNull
	public CompletableFuture<Integer> reindexHoppers(@NotNull HellblockWorld<?> world, int islandId) {
		instance.debug("Starting hopper reindex for islandId=" + islandId);

		return instance.getStorageManager().getOfflineUserDataByIslandId(islandId, false).thenCompose(optData -> {
			if (optData.isEmpty())
				return CompletableFuture.completedFuture(0);

			final UserData ownerData = optData.get();
			final HellblockData hellblockData = ownerData.getHellblockData();
			final UUID ownerId = hellblockData.getOwnerUUID();
			final BoundingBox bounds = hellblockData.getBoundingBox();
			if (ownerId == null || bounds == null) {
				instance.debug("Skipping reindex: ownerId or bounding box is null for islandId=" + islandId);
				return CompletableFuture.completedFuture(0);
			}

			clearHoppers(bounds);

			return instance.getProtectionManager().getHellblockBlocks(world, ownerId).thenCompose(positions -> {
				List<CompletableFuture<Void>> futures = positions.stream()
						.map(pos -> world.getBlockState(pos).thenAccept(stateOpt -> {
							if (stateOpt.isPresent()) {
								String key = stateOpt.get().type().type().value();
								if (HOPPER_KEYS.contains(key.toLowerCase())) {
									registerHopper(pos, bounds);
								}
							}
						})).toList();

				return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
					final int count = countHoppers(bounds);
					instance.debug(
							"Reindexed " + count + " hopper" + (count == 1 ? "" : "s") + " for islandId=" + islandId);
					return count;
				});
			});
		});
	}

	/**
	 * Performs a full scan of all specified islands to detect and register hopper
	 * blocks when the server or world is loaded.
	 * <p>
	 * This is typically used during startup or island data recovery to ensure the
	 * in-memory hopper registry reflects the actual state of the world. Any
	 * existing hopper data is cleared before the scan begins.
	 * <p>
	 * Islands that cannot be resolved (due to missing world, owner, or bounding
	 * box) are skipped, and failures are logged for troubleshooting.
	 *
	 * @param islandIds A collection of island IDs to scan and reindex hoppers for.
	 * @return A {@link CompletableFuture} completing with the number of islands
	 *         successfully processed.
	 */
	@NotNull
	private CompletableFuture<Integer> scanAndCountHoppersOnLoad(@NotNull Collection<Integer> islandIds) {
		clearAllHoppers();
		List<CompletableFuture<Integer>> islandFutures = new ArrayList<>();

		for (int islandId : islandIds) {
			CompletableFuture<Integer> future = instance.getStorageManager()
					.getOfflineUserDataByIslandId(islandId, false).thenCompose(optData -> {
						if (optData.isEmpty())
							return CompletableFuture.completedFuture(0);

						final UserData ownerData = optData.get();
						final HellblockData hellblockData = ownerData.getHellblockData();
						final int storedIslandId = hellblockData.getIslandId();
						if (storedIslandId != islandId) {
							instance.getPluginLogger().warn("Mismatched island ID: expected=" + islandId + ", found="
									+ storedIslandId + " in UserData. Skipping.");
							return CompletableFuture.completedFuture(0);
						}

						final UUID ownerId = hellblockData.getOwnerUUID();
						if (ownerId == null) {
							instance.getPluginLogger()
									.warn("Missing owner UUID for islandId=" + islandId + ". Skipping.");
							return CompletableFuture.completedFuture(0);
						}

						final BoundingBox bounds = hellblockData.getBoundingBox();
						if (bounds == null) {
							instance.getPluginLogger()
									.warn("Missing bounding box for islandId=" + islandId + ". Skipping.");
							return CompletableFuture.completedFuture(0);
						}

						Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
								.getWorld(instance.getWorldManager().getHellblockWorldFormat(storedIslandId));
						if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
							instance.getPluginLogger().severe("Hellblock world not found for islandId=" + islandId
									+ ". Try regenerating the world.");
							return CompletableFuture.completedFuture(0);
						}

						final HellblockWorld<?> hellWorld = worldOpt.get();

						return instance.getProtectionManager().getHellblockBlocks(hellWorld, ownerId)
								.thenCompose(positions -> {
									List<CompletableFuture<Void>> futures = new ArrayList<>();
									AtomicInteger count = new AtomicInteger(0);

									for (Pos3 pos : positions) {
										futures.add(hellWorld.getBlockState(pos).thenAccept(stateOpt -> {
											if (stateOpt.isPresent()) {
												String key = stateOpt.get().type().type().value();
												if (HOPPER_KEYS.contains(key.toLowerCase())) {
													count.incrementAndGet();
													registerHopper(pos, bounds);
												}
											}
										}));
									}

									return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
											.thenApply(v -> {
												instance.debug("Hellblock for islandId=" + islandId + " has "
														+ count.get() + " hopper" + (count.get() == 1 ? "" : "s")
														+ " (loaded)");
												return count.get(); // Success
											});
								});
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe("Error scanning hoppers for islandId=" + islandId, ex);
						return 0; // Failed
					});

			islandFutures.add(future);
		}

		// Combine all futures and sum their results
		return CompletableFuture.allOf(islandFutures.toArray(CompletableFuture[]::new))
				.thenApply(v -> islandFutures.stream().mapToInt(CompletableFuture::join).sum());
	}
}