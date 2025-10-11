package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

/**
 * Tracks hoppers being placed/broken inside islands. Also prevents placing if
 * over limit.
 */
public class HopperHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Map<BoundingBox, Set<Block>> islandHoppers = new HashMap<>();
	// Throttle map: per-owner last warning timestamp
	private final Map<UUID, Long> lastHopperWarnAt = new HashMap<>();

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
		instance.getCoopManager().getCachedIslandOwners().thenAccept(this::scanAndCountHoppersOnLoad);
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
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData user = userDataOpt.get();
						final BoundingBox bounds = user.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						// Back to main thread for Bukkit API
						instance.getScheduler().executeSync(() -> {
							final int placed = countHoppers(bounds);
							final int max = user.getHellblockData().getMaxHopperLimit();

							if (placed > max) {
								event.setCancelled(true);
								triggerImmediateHopperWarning(ownerUUID, player);
								return;
							}

							registerHopper(block, bounds);
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
			instance.getScheduler().executeSync(() -> instance.getStorageManager()
					.getOfflineUserData(ownerUUID, instance.getConfigManager().lockData()).thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}
						final UserData user = userDataOpt.get();
						final BoundingBox bounds = user.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}
						unregisterHopper(block, bounds);
						triggerImmediateHopperWarning(ownerUUID, player);
					}));
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		if (event.blockList().isEmpty()) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getExplodedBlockState().getWorld())) {
			return;
		}
		handleExplodedBlocks(event.blockList());
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.blockList().isEmpty()) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getLocation().getWorld())) {
			return;
		}
		handleExplodedBlocks(event.blockList());
	}

	private void handleExplodedBlocks(List<Block> explodedBlocks) {
		for (Block block : explodedBlocks) {
			if (block.getType() != Material.HOPPER) {
				continue;
			}

			instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(ownerUUID -> {
				if (ownerUUID == null) {
					return;
				}

				instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
						.thenAccept(userDataOpt -> {
							if (userDataOpt.isEmpty()) {
								return;
							}

							final UserData user = userDataOpt.get();
							final BoundingBox bounds = user.getHellblockData().getBoundingBox();
							if (bounds == null) {
								return;
							}

							// Always unregister on main thread
							instance.getScheduler().executeSync(() -> unregisterHopper(block, bounds));
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

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData user = userDataOpt.get();
						final BoundingBox bounds = user.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						instance.getScheduler().executeSync(() -> {
							final int allowed = user.getHellblockData().getMaxHopperLimit();
							final int total = countHoppers(bounds);

							if (total > allowed) {
								final List<Block> hopperList = new ArrayList<>(getHoppers(bounds));
								for (int i = allowed; i < hopperList.size(); i++) {
									final Block extraHopper = hopperList.get(i);
									if (extraHopper.equals(block)) {
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
	private void sendHopperWarnings(Collection<UUID> islandOwners) {
		final long now = System.currentTimeMillis();

		for (UUID ownerUUID : islandOwners) {
			final int placed = getHopperCount(ownerUUID);

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData user = userDataOpt.get();
						final int allowed = user.getHellblockData().getMaxHopperLimit();

						final boolean hopperWarnCondition = placed > allowed
								&& shouldSendHopperWarning(user.getHellblockData(), now, HOPPER_WARN_THROTTLE);
						if (hopperWarnCondition) {
							final Player owner = Bukkit.getPlayer(ownerUUID);
							if (owner != null && owner.isOnline()) {
								instance.getScheduler().executeSync(() -> instance.getSenderFactory().wrap(owner)
										.sendMessage(instance.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_HOPPER_EXCESS_WARNING
														.arguments(Component.text(placed), Component.text(allowed))
														.build())));
							}
						}
					});
		}
	}

	/**
	 * Triggers an immediate hopper warning if an island exceeds its limit. Looks up
	 * the island owner for limits, but sends the message to the actor.
	 *
	 * @param ownerUUID the island owner's UUID
	 * @param actor     the player who triggered the action
	 */
	private void triggerImmediateHopperWarning(UUID ownerUUID, Player actor) {
		final int placed = getHopperCount(ownerUUID);

		instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
				.thenAccept(userDataOpt -> {
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
											.arguments(Component.text(allowed)).build()));
							AdventureHelper.playSound(instance.getSenderFactory().getAudience(actor),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						});
					}
				});
	}

	/**
	 * Checks whether a hopper warning should be sent to this island owner.
	 */
	private boolean shouldSendHopperWarning(HellblockData data, long now, long throttleMillis) {
		final UUID ownerUUID = data.getOwnerUUID();
		final long last = lastHopperWarnAt.getOrDefault(ownerUUID, 0L);

		if (!(now - last >= throttleMillis)) {
			return false;
		}
		lastHopperWarnAt.put(ownerUUID, now);
		return true;
	}

	private void startHopperWarningScheduler() {
		// Cancel old one if running
		if (hopperWarningTask != null && !hopperWarningTask.isCancelled()) {
			hopperWarningTask.cancel();
			hopperWarningTask = null;
		}

		hopperWarningTask = instance.getScheduler().sync().runRepeating(
				() -> instance.getCoopManager().getCachedIslandOwners().thenAccept(this::sendHopperWarnings), 0L,
				20L * 60 * 5, null);
	}

	public void registerHopper(Block hopperBlock, BoundingBox bounds) {
		islandHoppers.computeIfAbsent(bounds, k -> new HashSet<>()).add(hopperBlock);
	}

	public void unregisterHopper(Block hopperBlock, BoundingBox bounds) {
		final Set<Block> hoppers = islandHoppers.get(bounds);
		if (hoppers == null) {
			return;
		}
		hoppers.remove(hopperBlock);
		if (hoppers.isEmpty()) {
			islandHoppers.remove(bounds);
		}
	}

	public int countHoppers(BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet()).size();
	}

	public Set<Block> getHoppers(BoundingBox bounds) {
		return islandHoppers.getOrDefault(bounds, Collections.emptySet());
	}

	/**
	 * Gets the current hopper count for an island owned by the given UUID. Looks up
	 * the bounding box from user data and counts hoppers from the registry.
	 *
	 * @param ownerUUID the island owner's UUID
	 * @return the number of registered hoppers for this island, or 0 if unavailable
	 */
	public int getHopperCount(UUID ownerUUID) {
		final Optional<UserData> userDataOpt = instance.getStorageManager().getOnlineUser(ownerUUID);
		if (userDataOpt.isEmpty()) {
			return 0;
		}

		final UserData user = userDataOpt.get();
		final BoundingBox bounds = user.getHellblockData().getBoundingBox();
		return bounds == null ? 0 : countHoppers(bounds);
	}

	/**
	 * Asynchronously gets the hopper count for an island owned by the given UUID.
	 * Uses offline user data to resolve the bounding box.
	 */
	public CompletableFuture<Integer> getHopperCountAsync(UUID ownerUUID) {
		return instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
				.thenApply(userDataOpt -> {
					if (userDataOpt.isEmpty()) {
						return 0;
					}

					final UserData user = userDataOpt.get();
					final BoundingBox bounds = user.getHellblockData().getBoundingBox();
					return bounds == null ? 0 : countHoppers(bounds);
				});
	}

	/**
	 * Transfers all hoppers from one bounding box to another. Useful when an island
	 * is resized or moved.
	 */
	public void transferHoppers(BoundingBox from, BoundingBox to) {
		final Set<Block> fromSet = islandHoppers.remove(from);
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
	public void clearHoppers(BoundingBox bounds) {
		islandHoppers.remove(bounds);
	}

	/**
	 * Removes a specific hopper block from all bounding boxes if present.
	 */
	public void clearHopper(Block hopperBlock) {
		islandHoppers.values().forEach(hopperSet -> hopperSet.remove(hopperBlock));
	}

	/**
	 * Rebuilds hopper registry for an island by scanning its saved blocks. Use this
	 * after external edits (WorldEdit, rollback, etc.).
	 */
	public void reindexHoppers(World world, UUID ownerUUID) {
		instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
				.thenAccept(userDataOpt -> {
					if (userDataOpt.isEmpty()) {
						return;
					}

					final UserData user = userDataOpt.get();
					final BoundingBox bounds = user.getHellblockData().getBoundingBox();
					if (bounds == null) {
						return;
					}

					// Clear old hoppers first
					clearHoppers(bounds);

					// Get stored blocks for this island
					instance.getProtectionManager().getHellblockBlocks(world, ownerUUID)
							.thenAccept(blocks -> instance.getScheduler().executeSync(() -> {
								blocks.forEach(block -> {
									if (block.getType() == Material.HOPPER) {
										registerHopper(block, bounds);
									}
								});

								final int count = countHoppers(bounds);
								instance.getPluginLogger()
										.info("Reindexed " + count + " hoppers for island " + ownerUUID);
							}));
				});
	}

	private void scanAndCountHoppersOnLoad(Collection<UUID> islandOwners) {
		// Clear all before scanning to avoid duplicates on reload
		clearAllHoppers();

		for (UUID ownerUUID : islandOwners) {
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData user = userDataOpt.get();

						// Ensure this user really owns the island
						final UUID storedOwner = user.getHellblockData().getOwnerUUID();
						if (storedOwner != null && !storedOwner.equals(ownerUUID)) {
							return;
						}

						final BoundingBox bounds = user.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						// Resolve world safely
						final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager().getWorld(
								instance.getWorldManager().getHellblockWorldFormat(user.getHellblockData().getID()));
						if (worldOpt.isEmpty()) {
							instance.getPluginLogger().severe("Hellblock world not found for island " + ownerUUID
									+ ". Try regenerating the world.");
							return;
						}

						final World world = worldOpt.get().bukkitWorld();

						// Fetch island blocks asynchronously
						instance.getProtectionManager().getHellblockBlocks(world, ownerUUID)
								.thenAccept(blocks -> instance.getScheduler().executeSync(() -> {
									int count = 0;
									for (Block block : blocks) {
										if (block.getType() == Material.HOPPER) {
											count++;
											registerHopper(block, bounds);
										}
									}
									instance.getPluginLogger()
											.info("Hellblock for " + ownerUUID + " has " + count + " hoppers (loaded)");
								}));
					});
		}
	}
}