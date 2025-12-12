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
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.sound.Sound;

/**
 * Tracks hoppers being placed/broken inside islands. Also prevents placing if
 * over limit.
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
		instance.getIslandManager().getAllIslandIds().thenAccept(this::scanAndCountHoppersOnLoad);
		startHopperWarningScheduler();
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

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			// Fetch user data async
			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData userData = userDataOpt.get();
						final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						// Back to main thread for Bukkit API
						instance.getScheduler().executeSync(() -> {
							final int placed = countHoppers(bounds);
							final int max = userData.getHellblockData().getMaxHopperLimit();

							if (placed > max) {
								event.setCancelled(true);
								triggerImmediateHopperWarning(userData.getHellblockData().getIslandId(), player);
								return;
							}

							registerHopper(Pos3.from(block.getLocation()), bounds);
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

		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			// Trigger immediate warning (actor gets msg, limits from owner)
			instance.getScheduler()
					.executeSync(() -> instance.getStorageManager()
							.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
							.thenAccept(userDataOpt -> {
								if (userDataOpt.isEmpty()) {
									return;
								}
								final UserData userData = userDataOpt.get();
								final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
								if (bounds == null) {
									return;
								}
								unregisterHopper(Pos3.from(block.getLocation()), bounds);
								triggerImmediateHopperWarning(userData.getHellblockData().getIslandId(), player);
							}));
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

	private void handleExplodedBlocks(@NotNull List<Block> explodedBlocks) {
		for (Block block : explodedBlocks) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld()))
				continue;

			if (block.getType() != Material.HOPPER) {
				continue;
			}

			instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
				if (ownerUUID == null) {
					return;
				}

				instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(userDataOpt -> {
							if (userDataOpt.isEmpty()) {
								return;
							}

							final UserData userData = userDataOpt.get();
							final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
							if (bounds == null) {
								return;
							}

							// Always unregister on main thread
							instance.getScheduler()
									.executeSync(() -> unregisterHopper(Pos3.from(block.getLocation()), bounds));
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

		instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData userData = userDataOpt.get();
						final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						instance.getScheduler().executeSync(() -> {
							final int allowed = userData.getHellblockData().getMaxHopperLimit();
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
	 * Checks hopper count and warns the owner if needed. Respects throttling to
	 * avoid spam.
	 */
	private void sendHopperWarnings(@NotNull Collection<Integer> islandIds) {
		final long now = System.currentTimeMillis();

		for (int islandId : islandIds) {
			getHopperCount(islandId).thenCompose(placed -> instance.getStorageManager()
					.getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
					.thenApply(userDataOpt -> Map.entry(placed, userDataOpt))).thenAccept(pair -> {
						int placed = pair.getKey();
						Optional<UserData> userDataOpt = pair.getValue();

						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData userData = userDataOpt.get();
						final int allowed = userData.getHellblockData().getMaxHopperLimit();

						final boolean shouldWarn = placed > allowed
								&& shouldSendHopperWarning(userData.getHellblockData(), now, HOPPER_WARN_THROTTLE);

						if (!shouldWarn) {
							return;
						}

						final UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
						if (ownerUUID == null) {
							return;
						}

						final Player owner = Bukkit.getPlayer(ownerUUID);
						if (owner == null || !owner.isOnline()) {
							return;
						}

						instance.getScheduler().executeSync(() -> {
							instance.getSenderFactory().wrap(owner).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_HOPPER_EXCESS_WARNING
											.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(placed)),
													AdventureHelper.miniMessageToComponent(String.valueOf(allowed)))
											.build()));
							AdventureHelper.playSound(instance.getSenderFactory().getAudience(owner),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						});
					}).exceptionally(ex -> {
						instance.getPluginLogger().severe(
								"Failed to send hopper warnings for max hopper count exceeding island's count for island ID="
										+ islandId,
								ex);
						ex.printStackTrace();
						return null;
					});
		}
	}

	/**
	 * Triggers an immediate hopper warning if an island exceeds its limit. Looks up
	 * the island owner for limits, but sends the message to the actor.
	 *
	 * @param islandId the island id
	 * @param actor    the player who triggered the action
	 */
	private void triggerImmediateHopperWarning(int islandId, @NotNull Player actor) {
		getHopperCount(islandId).thenCompose(placed -> instance.getStorageManager()
				.getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenApply(userDataOpt -> Map.entry(placed, userDataOpt))).thenAccept(pair -> {
					int placed = pair.getKey();
					Optional<UserData> userDataOpt = pair.getValue();

					if (userDataOpt.isEmpty()) {
						return;
					}

					final UserData ownerData = userDataOpt.get();
					final int allowed = ownerData.getHellblockData().getMaxHopperLimit();

					if (placed > allowed) {
						final Sender sender = instance.getSenderFactory().wrap(actor);

						instance.getScheduler().executeSync(() -> {
							sender.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_HOPPER_LIMIT_REACHED
											.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(allowed)))
											.build()));

							AdventureHelper.playSound(instance.getSenderFactory().getAudience(actor),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						});
					}
				}).exceptionally(ex -> {
					instance.getPluginLogger().severe(
							"Failed to trigger hopper warning for max hopper count exceeding island's count for island ID="
									+ islandId + " to player " + actor.getName(),
							ex);
					ex.printStackTrace();
					return null;
				});
	}

	/**
	 * Checks whether a hopper warning should be sent to this island owner.
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

	private void startHopperWarningScheduler() {
		// Cancel the existing task if it's running
		if (hopperWarningTask != null && !hopperWarningTask.isCancelled()) {
			hopperWarningTask.cancel();
			hopperWarningTask = null;
		}

		hopperWarningTask = instance.getScheduler().sync().runRepeating(
				() -> instance.getCoopManager().getCachedIslandOwnerData().thenAccept(userDataCollection -> {
					// Extract island IDs from HellblockData
					List<Integer> islandIds = userDataCollection.stream()
							.map(userData -> userData.getHellblockData().getIslandId()).filter(Objects::nonNull)
							.toList();

					sendHopperWarnings(islandIds);
				}), 0L, 20L * 60 * 5, // Every 5 minutes
				null);
	}

	public void registerHopper(@NotNull Pos3 hopperPos, @NotNull BoundingBox bounds) {
		islandHoppers.computeIfAbsent(bounds, k -> new HashSet<>()).add(hopperPos);
	}

	public void unregisterHopper(@NotNull Pos3 hopperPos, @NotNull BoundingBox bounds) {
		final Set<Pos3> hoppers = islandHoppers.get(bounds);
		if (hoppers == null) {
			return;
		}
		hoppers.remove(hopperPos);
		if (hoppers.isEmpty()) {
			islandHoppers.remove(bounds);
		}
	}

	public int countHoppers(@NotNull BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet()).size();
	}

	@NotNull
	public Set<Pos3> getHoppers(@NotNull BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet());
	}

	/**
	 * Asynchronously gets the hopper count for an island owned by the given island
	 * ID. Uses offline user data to resolve the bounding box.
	 */
	public CompletableFuture<Integer> getHopperCount(int islandId) {
		return instance.getStorageManager()
				.getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenApply(userDataOpt -> {
					if (userDataOpt.isEmpty()) {
						return 0;
					}

					final UserData userData = userDataOpt.get();
					final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
					return bounds == null ? 0 : countHoppers(bounds);
				});
	}

	/**
	 * Transfers all hoppers from one bounding box to another. Useful when an island
	 * is resized or moved.
	 */
	public void transferHoppers(@NotNull BoundingBox from, @NotNull BoundingBox to) {
		final Set<Pos3> fromSet = islandHoppers.remove(from);
		if (fromSet == null || fromSet.isEmpty()) {
			return;
		}
		islandHoppers.computeIfAbsent(to, k -> new HashSet<>()).addAll(fromSet);
	}

	/**
	 * Clears all registered hoppers for all islands. Use before a full rescan to
	 * avoid duplicates.
	 */
	public void clearAllHoppers() {
		islandHoppers.clear();
	}

	/**
	 * Removes all hoppers associated with the given bounding box.
	 */
	public void clearHoppers(@Nullable BoundingBox bounds) {
		if (bounds == null)
			return;
		islandHoppers.remove(bounds);
	}

	/**
	 * Removes a specific hopper block from all bounding boxes if present.
	 */
	public void clearHopper(@NotNull Pos3 hopperPos) {
		islandHoppers.values().forEach(hopperSet -> hopperSet.remove(hopperPos));
	}

	public boolean invalidateHopperWarningCache(int islandId) {
		return lastHopperWarnAt.remove(islandId) != null;
	}

	/**
	 * Rebuilds hopper registry for an island by scanning its saved blocks. Use this
	 * after external edits (WorldEdit, rollback, etc.).
	 */
	public void reindexHoppers(@NotNull HellblockWorld<?> world, int islandId) {
		instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
				.thenAccept(userDataOpt -> {
					if (userDataOpt.isEmpty())
						return;

					final UserData userData = userDataOpt.get();
					final HellblockData hellblockData = userData.getHellblockData();
					final UUID ownerId = hellblockData.getOwnerUUID();
					final BoundingBox bounds = hellblockData.getBoundingBox();
					if (ownerId == null || bounds == null)
						return;

					clearHoppers(bounds);

					instance.getProtectionManager().getHellblockBlocks(world, ownerId).thenAccept(positions -> {
						List<CompletableFuture<Void>> futures = positions.stream()
								.map(pos -> world.getBlockState(pos).thenAccept(stateOpt -> {
									if (stateOpt.isPresent()) {
										String key = stateOpt.get().type().type().value();
										if (HOPPER_KEYS.contains(key.toLowerCase())) {
											registerHopper(pos, bounds);
										}
									}
								})).toList();

						CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
							final int count = countHoppers(bounds);
							instance.debug("Reindexed " + count + " hopper" + (count == 1 ? "" : "s") + " for islandID="
									+ islandId);
						});
					});
				});
	}

	private void scanAndCountHoppersOnLoad(@NotNull Collection<Integer> islandIds) {
		clearAllHoppers();

		for (int islandId : islandIds) {
			instance.getStorageManager().getOfflineUserDataByIslandId(islandId, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty())
							return;

						final UserData userData = userDataOpt.get();
						final HellblockData hellblockData = userData.getHellblockData();
						final int storedIslandId = hellblockData.getIslandId();
						if (storedIslandId != islandId)
							return;

						final UUID ownerId = hellblockData.getOwnerUUID();
						final BoundingBox bounds = hellblockData.getBoundingBox();
						if (ownerId == null || bounds == null)
							return;

						Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
								.getWorld(instance.getWorldManager().getHellblockWorldFormat(storedIslandId));
						if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
							instance.getPluginLogger().severe("Hellblock world not found for islandID=" + islandId
									+ ". Try regenerating the world.");
							return;
						}

						final HellblockWorld<?> hellWorld = worldOpt.get();

						instance.getProtectionManager().getHellblockBlocks(hellWorld, ownerId).thenAccept(positions -> {
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

							CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
									.thenRun(() -> instance.debug("Hellblock for islandID=" + islandId + " has "
											+ count.get() + " hopper" + (count.get() == 1 ? "" : "s") + " (loaded)"));
						});
					});
		}
	}
}