package com.swiftlicious.hellblock.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.database.RedisManager;
import com.swiftlicious.hellblock.nms.border.BorderColor;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RespawnUtil;

public class PlayerListener implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	// Cooldown tracking: prevents repeated out-of-bounds handling per player
	private final Map<UUID, Long> outOfBoundsHandledTimestamps = new ConcurrentHashMap<>();
	private static final long OUT_OF_BOUNDS_COOLDOWN_MS = 5000; // 5 seconds cooldown

	private int interval;
	private final UUID identifier;
	private final ConcurrentMap<UUID, PlayerCount> playerCountMap = new ConcurrentHashMap<>();
	private RedisPlayerCount redisPlayerCount = null;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		this.identifier = UUID.randomUUID();
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.interval = 10;
		if (instance.getConfigManager().redisRanking()) {
			if (this.redisPlayerCount == null) {
				this.redisPlayerCount = new RedisPlayerCount(this.interval);
			}
		} else {
			if (this.redisPlayerCount != null) {
				this.redisPlayerCount.cancel();
				this.redisPlayerCount = null;
			}
		}
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		if (this.redisPlayerCount != null) {
			this.redisPlayerCount.cancel();
			this.redisPlayerCount = null;
		}
		outOfBoundsHandledTimestamps.clear();
	}

	@Override
	public void disable() {
		unload();
		if (this.redisPlayerCount == null) {
			return;
		}
		this.redisPlayerCount.cancel();
		this.redisPlayerCount = null;
	}

	/**
	 * Shared logic when a player tries to leave visiting island bounds: ensure safe
	 * teleports, send messages, run makeHomeLocationSafe if necessary.
	 */
	@NotNull
	private CompletableFuture<Boolean> handleOutOfBoundsForPlayer(@NotNull Player visitor,
			@NotNull UserData ownerData) {
		final BoundingBox bounds = ownerData.getHellblockData().getBoundingBox();
		if (bounds == null)
			return CompletableFuture.completedFuture(false);

		// Ignore Y in boundary check
		final BoundingBox ignoreYBounds = new BoundingBox(bounds.getMinX(), Double.MIN_VALUE, bounds.getMinZ(),
				bounds.getMaxX(), Double.MAX_VALUE, bounds.getMaxZ());

		// If still inside bounds, no action needed
		if (ignoreYBounds.contains(visitor.getBoundingBox()))
			return CompletableFuture.completedFuture(false);

		final UUID visitorId = visitor.getUniqueId();
		// Prevent repeated handling — apply cooldown
		final long now = System.currentTimeMillis();
		final long lastHandled = outOfBoundsHandledTimestamps.getOrDefault(visitorId, 0L);
		if (now - lastHandled < OUT_OF_BOUNDS_COOLDOWN_MS) {
			// Still on cooldown - skip
			return CompletableFuture.completedFuture(false);
		}
		outOfBoundsHandledTimestamps.put(visitorId, now);

		final Optional<UserData> visitorOpt = instance.getStorageManager().getOnlineUser(visitorId);
		if (visitorOpt.isEmpty())
			return instance.getScheduler().callSync(() -> CompletableFuture
					.completedFuture(instance.getHellblockHandler().teleportToSpawn(visitor, true)));

		final UserData visitorData = visitorOpt.get();
		final Sender audience = instance.getSenderFactory().wrap(visitor);
		final Location home = ownerData.getHellblockData().getHomeLocation();

		if (home == null) {
			// Fallback: teleport visitor to their own island or spawn
			return teleportVisitorToOwnIslandOrSpawn(visitorData);
		}

		// Check if owner's home is safe
		return LocationUtils.isSafeLocationAsync(home).thenCompose(isSafe -> {
			if (isSafe) {
				// Safe: teleport back to the island home location
				return instance.getScheduler().callSync(() -> {
					return ChunkUtils.teleportAsync(visitor, home, TeleportCause.PLUGIN).thenApply(result -> {
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
						instance.getWorldManager().markWorldAccess(home.getWorld().getName());
						return result != null && result;
					});
				});
			}

			// Not safe: try to fix the visitor's own island home
			UUID ownerUUID = visitorData.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				instance.getPluginLogger().severe("Owner UUID was null while handling out-of-bounds for " + visitorId);
				return teleportVisitorToOwnIslandOrSpawn(visitorData); // fallback
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return teleportVisitorToOwnIslandOrSpawn(visitorData); // fallback
				}

				final UserData visitorOwnerData = optData.get();
				return instance.getCoopManager().makeHomeLocationSafe(visitorOwnerData, visitorData)
						.thenCompose(safetyResult -> {
							return switch (safetyResult) {
							case ALREADY_SAFE:
								instance.debug("Home is already safe, teleporting complete for " + visitor.getName());
								yield instance.getHellblockHandler().teleportPlayerToHome(visitorData,
										visitorOwnerData.getHellblockData().getHomeLocation());
							case FIXED_AND_TELEPORTED:
								instance.debug("Home fixed and teleport complete for " + visitor.getName());
								yield CompletableFuture.completedFuture(true);
							case FAILED_TO_FIX:
								instance.getPluginLogger().warn(
										"Failed to fix home for " + visitor.getName() + ", teleporting to spawn.");
								yield instance.getScheduler().callSync(() -> {
									return CompletableFuture.completedFuture(
											instance.getHellblockHandler().teleportToSpawn(visitor, true));
								});
							};
						}).thenCompose(success -> {
							if (success) {
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));

								// After attempting to fix, teleport again to home (now assumed safe)
								return instance.getScheduler().callSync(() -> {
									return ChunkUtils.teleportAsync(visitor, home, TeleportCause.PLUGIN)
											.thenApply(result -> {
												instance.getWorldManager().markWorldAccess(home.getWorld().getName());
												return result != null && result;
											});
								});
							} else {
								// fallback if all else fails
								return teleportVisitorToOwnIslandOrSpawn(visitorData);
							}
						});
			}).handle((res, ex) -> {
				return instance.getStorageManager().unlockUserData(ownerUUID).thenApply(unused -> {
					if (ex != null) {
						instance.getPluginLogger().severe(
								"Exception when teleporting player " + visitor.getName() + " for leaving island bounds",
								ex);
					}

					return res != null && res;
				});
			}).thenCompose(Function.identity()); // Flatten nested CompletableFuture
		});
	}

	@NotNull
	private CompletableFuture<Boolean> teleportVisitorToOwnIslandOrSpawn(@NotNull UserData visitorData) {
		final Player visitor = visitorData.getPlayer();
		final Sender audience = instance.getSenderFactory().wrap(visitor);
		final HellblockData data = visitorData.getHellblockData();
		if (data.hasHellblock()) {
			final UUID ownerId = data.getOwnerUUID();
			if (ownerId == null) {
				return instance.getScheduler().callSync(() -> CompletableFuture
						.completedFuture(instance.getHellblockHandler().teleportToSpawn(visitor, true)));
			}

			return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return instance.getScheduler().callSync(() -> CompletableFuture
							.completedFuture(instance.getHellblockHandler().teleportToSpawn(visitor, true)));
				}

				final UserData ownerData = optData.get();
				final Location homeLocation = ownerData.getHellblockData().getHomeLocation();
				if (homeLocation == null || homeLocation.getWorld() == null) {
					return instance.getScheduler().callSync(() -> CompletableFuture
							.completedFuture(instance.getHellblockHandler().teleportToSpawn(visitor, true)));
				}

				return instance.getScheduler().callSync(() -> {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
					return ChunkUtils.teleportAsync(visitor, homeLocation, TeleportCause.PLUGIN).thenApply(result -> {
						instance.getWorldManager().markWorldAccess(homeLocation.getWorld().getName());
						return result != null && result;
					});
				});
			});
		}

		// Fallback: spawn
		return instance.getScheduler().callSync(
				() -> CompletableFuture.completedFuture(instance.getHellblockHandler().teleportToSpawn(visitor, true)));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		outOfBoundsHandledTimestamps.remove(uuid);
	}

	@EventHandler
	public void onBedClick(PlayerBedEnterEvent event) {
		if (!instance.getConfigManager().disableBedExplosions()) {
			return;
		}

		final Player player = event.getPlayer();

		// Check if the player is in the Hellblock world
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block bed = event.getBed();

		// Only proceed if the block is tagged as a bed
		if (!Tag.BEDS.isTagged(bed.getType())) {
			return;
		}

		// Only prevent if we're in the Nether and beds don't work
		World world = bed.getWorld();
		if (world.getEnvironment() != Environment.NETHER || RespawnUtil.isBedWorks(world)) {
			return;
		}

		// Cancel usage and prevent explosion
		event.setCancelled(true);
		event.setUseBed(Event.Result.DENY);
	}

	@EventHandler
	public void onFireworkDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (event.getDamager() instanceof FakeFirework) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onFallingBlockLandOnSoulSand(EntityDropItemEvent event) {
		if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(fallingBlock.getWorld())) {
			return;
		}

		final Block block = fallingBlock.getLocation().getBlock();
		if (block.getType() != Material.SOUL_SAND) {
			return;
		}

		final Block above = block.getRelative(BlockFace.UP);
		if (above.getType() != Material.LAVA) {
			return;
		}

		event.setCancelled(true);

		instance.getScheduler().executeSync(() -> {
			above.setType(fallingBlock.getBlockData().getMaterial());
			fallingBlock.setDropItem(false);
		});
	}

	@EventHandler
	public void onLeaveHellblockBorders(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;
		if (movedWithinBlock(event))
			return;

		final UUID playerId = player.getUniqueId();
		final Location location = player.getLocation();

		// Get the island owner at the current location
		instance.getCoopManager().getHellblockOwnerOfLocation(location).thenCompose(ownerId -> {
			if (ownerId == null)
				return CompletableFuture.completedFuture(null);

			// If it's not their own island, skip enforcement
			if (!ownerId.equals(playerId) && !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
				return CompletableFuture.completedFuture(null);
			}

			// Check if player is still within the island bounds
			return instance.getProtectionManager().isInsideIsland2D(ownerId, location).thenCompose(isInside -> {
				// Still inside - no action required
				if (isInside)
					return CompletableFuture.completedFuture(null);

				// Run boundary enforcement
				return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
						.thenCompose(optData -> {
							if (optData.isEmpty()) {
								return CompletableFuture.completedFuture(null);
							}

							final UserData ownerData = optData.get();
							return handleOutOfBoundsForPlayer(player, ownerData);
						});
			});
		});
	}

	@EventHandler
	public void onTeleportOutsideBorders(PlayerTeleportEvent event) {
		if (!getAllowedTeleportCauses().contains(event.getCause()))
			return;

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		final UUID playerId = player.getUniqueId();
		final Location origin = event.getFrom();

		instance.getCoopManager().getHellblockOwnerOfLocation(origin).thenCompose(ownerId -> {
			if (ownerId == null)
				return CompletableFuture.completedFuture(null);

			// If it's not their own island, allow teleport
			if (!ownerId.equals(playerId) && !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
				return CompletableFuture.completedFuture(null);
			}

			// Check if teleport target is still inside the island
			return instance.getProtectionManager().isInsideIsland2D(ownerId, event.getTo()).thenCompose(isInside -> {
				// Allowed teleport
				if (isInside)
					return CompletableFuture.completedFuture(null);

				// Enforce teleport boundary
				return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
						.thenCompose(optData -> {
							if (optData.isEmpty()) {
								return CompletableFuture.completedFuture(null);
							}

							final UserData ownerData = optData.get();
							return handleOutOfBoundsForPlayer(player, ownerData);
						});
			});
		});
	}

	@SuppressWarnings("removal")
	@NotNull
	private Set<PlayerTeleportEvent.TeleportCause> getAllowedTeleportCauses() {
		Set<PlayerTeleportEvent.TeleportCause> causes = EnumSet.of(PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
				PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
		try {
			causes.add(PlayerTeleportEvent.TeleportCause.valueOf("DISMOUNT"));
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Not present on older versions
		}
		return causes;
	}

	@EventHandler
	public void onElytraLeaveIsland(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.isGliding())
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		final UUID playerId = player.getUniqueId();
		final Location location = event.getTo();

		// First, determine whose island the player is currently in
		instance.getCoopManager().getHellblockOwnerOfLocation(location).thenCompose(ownerId -> {
			if (ownerId == null)
				return CompletableFuture.completedFuture(null);

			// Only enforce if this is the player's own island
			if (!ownerId.equals(playerId) && !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
				return CompletableFuture.completedFuture(null);
			}

			// Check if they are leaving their own island's bounds
			return instance.getProtectionManager().isInsideIsland2D(ownerId, location).thenCompose(isInside -> {
				if (isInside)
					return CompletableFuture.completedFuture(null);

				// Confirm: this is their own island
				return instance.getStorageManager().getCachedUserDataWithFallback(playerId, false)
						.thenCompose(optData -> {
							if (optData.isEmpty())
								return CompletableFuture.completedFuture(null);

							final UserData userData = optData.get();

							// Enforce only if the player is the island *owner* or coop member of current
							// island
							if (!ownerId.equals(playerId)
									&& !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
								// not their island - ignore
								return CompletableFuture.completedFuture(null);
							}

							return instance.getScheduler().callSync(() -> {
								if (!player.isOnline())
									return CompletableFuture.completedFuture(null);

								player.setGliding(false);

								Location fallback = instance.getHellblockHandler().getSafeSpawnLocation(userData);
								if (fallback == null) {
									instance.getPluginLogger().warn("Could not find a valid spawn location for player "
											+ player.getName() + " — teleport cancelled.");
									return CompletableFuture.completedFuture(null);
								}

								return ChunkUtils.teleportAsync(player, fallback, TeleportCause.PLUGIN)
										.thenCompose(v -> {
											instance.debug(
													"Teleported " + player.getName() + " to their Hellblock home.");
											return CompletableFuture.completedFuture(null);
										});
							});
						});
			});
		});
	}

	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		final List<Entity> passengers = event.getVehicle().getPassengers();
		if (passengers.isEmpty())
			return;

		for (Entity passenger : passengers) {
			if (!(passenger instanceof Player player))
				continue;

			if (!instance.getHellblockHandler().isInCorrectWorld(player))
				continue;

			final UUID playerId = player.getUniqueId();
			final Location location = event.getTo();

			// Check whose island the player is currently in
			instance.getCoopManager().getHellblockOwnerOfLocation(location).thenCompose(ownerId -> {
				if (ownerId == null)
					return CompletableFuture.completedFuture(null);

				// Only enforce border for owners or coop members
				if (!ownerId.equals(playerId) && !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
					return CompletableFuture.completedFuture(null);
				}

				// Check if they're still inside their own island
				return instance.getProtectionManager().isInsideIsland2D(ownerId, location).thenCompose(isInside -> {
					if (isInside)
						return CompletableFuture.completedFuture(null);

					// Player left their island via vehicle
					return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
							.thenCompose(optData -> {
								if (optData.isEmpty()) {
									return CompletableFuture.completedFuture(null);
								}

								final UserData ownerData = optData.get();
								return handleOutOfBoundsForPlayer(player, ownerData);
							});
				});
			});
		}
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		final UUID playerId = player.getUniqueId();
		final Location respawnLoc = event.getRespawnLocation();

		if (!instance.getHellblockHandler().isInCorrectWorld(respawnLoc.getWorld()))
			return;

		// Find which island the respawn location is in
		instance.getCoopManager().getHellblockOwnerOfLocation(respawnLoc).thenCompose(ownerId -> {
			if (ownerId == null)
				return CompletableFuture.completedFuture(null);

			// Only care if it's their own island (owner or member)
			if (!ownerId.equals(playerId) && !instance.getCoopManager().isIslandMember(ownerId, playerId)) {
				return CompletableFuture.completedFuture(null);
			}

			// If their respawn location is outside bounds, redirect
			return instance.getProtectionManager().isInsideIsland2D(ownerId, respawnLoc).thenCompose(isInside -> {
				if (isInside)
					return CompletableFuture.completedFuture(null);

				// Teleport to spawn or fallback location
				return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false)
						.thenCompose(optData -> {
							if (optData.isEmpty())
								return CompletableFuture.completedFuture(null);

							final UserData userData = optData.get();
							return instance.getScheduler().callSync(() -> {
								Location fallback = instance.getHellblockHandler().getSafeSpawnLocation(userData);

								if (fallback == null) {
									instance.getPluginLogger().warn("Could not determine a safe respawn location for "
											+ player.getName() + " (home or world may be null)");
									return CompletableFuture.completedFuture(null);
								}

								event.setRespawnLocation(fallback);
								instance.debug("Redirected respawn for " + player.getName()
										+ " to their Hellblock home (was outside their island bounds).");
								return CompletableFuture.completedFuture(null);
							});
						});
			});
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		final Player player = event.getPlayer();
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();

		// --- LEFT the Hellblock world: remove NV and clear flags if they had the
		// effect ---
		if (instance.getHellblockHandler().isInCorrectWorld(event.getFrom())) {
			instance.getBorderHandler().clearPlayerBorder(player);

			// If they were mid-expansion, mark it as completed
			if (instance.getBorderHandler().isBorderExpanding(player.getUniqueId())) {
				instance.debug("Player " + player.getName() + " changed world during border animation; finalizing.");
				instance.getBorderHandler().setBorderExpanding(player.getUniqueId(), false);

				final BoundingBox bounds = userData.getHellblockData().getBoundingBox();
				final Location hellblockLocation = userData.getHellblockData().getHellblockLocation();
				if (bounds == null || hellblockLocation == null || hellblockLocation.getWorld() == null) {
					return;
				}

				// Show the final border if applicable:
				if (player.getWorld().getUID().equals(hellblockLocation.getWorld().getUID())
						&& bounds.contains(player.getLocation().toVector())) {
					instance.getBorderHandler().setWorldBorder(player, bounds, BorderColor.RED);
				}
			}

			instance.getBorderHandler().stopBorderTask(player.getUniqueId());

			if ((userData.hasGlowstoneToolEffect() || userData.hasGlowstoneArmorEffect())
					&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				userData.isHoldingGlowstoneTool(false);
				userData.isWearingGlowstoneArmor(false);
			}
		}

		// --- ENTERED the Hellblock world: restore NV only if current tool/armor
		// qualifies ---
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		instance.getScheduler().sync().runLater(() -> {
			instance.debug("Player " + player.getName() + " entered Hellblock world; restarting border + NV check.");
			instance.getBorderHandler().startBorderTask(player.getUniqueId());
			applyNightVisionIfEligible(player, userData);
		}, 2L, player.getLocation()); // Delay by 2 ticks (~100ms)
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onMilkDrink(PlayerItemConsumeEvent event) {
		final Player player = event.getPlayer();
		if (event.getItem().getType() != Material.MILK_BUCKET) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();

		if (instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, userData), 2L,
					player.getLocation()); // Delay by 2 ticks (~100ms)
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPotionEffectRemoved(EntityPotionEffectEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (event.getOldEffect() == null) {
			return;
		}

		// Only react when NV is removed
		if (event.getOldEffect().getType() != PotionEffectType.NIGHT_VISION) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();

		// Only restore inside Hellblock world
		if (instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, userData), 2L,
					player.getLocation()); // Delay by 2 ticks (~100ms)
		}
	}

	private void applyNightVisionIfEligible(@NotNull Player player, @NotNull UserData userData) {
		boolean shouldRestore = false;

		// Check main hand and offhand tools
		final ItemStack mainHand = player.getInventory().getItemInMainHand();
		final ItemStack offHand = player.getInventory().getItemInOffHand();

		// check main hand for tool if so restore
		if (mainHand.getType() != Material.AIR && instance.getNetherToolsHandler().isNetherToolEnabled(mainHand)
				&& instance.getNetherToolsHandler().isNetherToolNightVisionAllowed(mainHand)
				&& instance.getNetherToolsHandler().checkNightVisionToolStatus(mainHand)
				&& instance.getNetherToolsHandler().getNightVisionToolStatus(mainHand)) {
			shouldRestore = true;
			userData.isHoldingGlowstoneTool(true);
		}

		// check off hand if main hand is invalid tool
		if (!shouldRestore && offHand.getType() != Material.AIR
				&& instance.getNetherToolsHandler().isNetherToolEnabled(offHand)
				&& instance.getNetherToolsHandler().isNetherToolNightVisionAllowed(offHand)
				&& instance.getNetherToolsHandler().checkNightVisionToolStatus(offHand)
				&& instance.getNetherToolsHandler().getNightVisionToolStatus(offHand)) {
			shouldRestore = true;
			userData.isHoldingGlowstoneTool(true);
		}

		// Check armor contents regardless of if both above is true
		for (ItemStack armor : player.getInventory().getArmorContents()) {
			if (armor == null) {
				continue;
			}
			if (instance.getNetherArmorHandler().isNetherArmorEnabled(armor)
					&& instance.getNetherArmorHandler().isNetherArmorNightVisionAllowed(armor)
					&& instance.getNetherArmorHandler().checkNightVisionArmorStatus(armor)
					&& instance.getNetherArmorHandler().getNightVisionArmorStatus(armor)) {
				shouldRestore = true;
				userData.isWearingGlowstoneArmor(true);
				break;
			}
		}

		if (shouldRestore) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
		}
	}

	@EventHandler
	public void onBreed(EntityBreedEvent event) {
		if (event.getBreeder() == null) {
			return;
		}
		if (!(event.getBreeder() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Entity bred = event.getEntity();
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
			if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
				userData.getHellblockData().updateLastIslandActivity();
			}

			instance.getChallengeManager().handleChallengeProgression(userData, ActionType.BREED, bred);
		});
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		final LivingEntity entity = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld())) {
			return;
		}

		final Player killer = entity.getKiller();
		if (killer == null) {
			return;
		}

		instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(userData -> {
			if (instance.getCooldownManager().shouldUpdateActivity(killer.getUniqueId(), 5000)) {
				userData.getHellblockData().updateLastIslandActivity();
			}

			instance.getChallengeManager().handleChallengeProgression(userData, ActionType.SLAY, entity);
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (RespawnUtil.isRespawnAnchorWorks(player.getWorld()) && RespawnUtil.getRespawnLocation(player) == null) {
			return;
		}
		if (!RespawnUtil.isDeathRespawn(event)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();
		if (!userData.getHellblockData().hasHellblock()) {
			return;
		}

		final UUID ownerId = userData.getHellblockData().getOwnerUUID();
		if (ownerId == null) {
			instance.getPluginLogger()
					.severe("Owner reference returned null in respawn event for " + player.getUniqueId());
			return;
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			final UserData ownerData = optData.get();
			return instance.getCoopManager().makeHomeLocationSafe(ownerData, userData).thenCompose(safetyResult -> {
				return switch (safetyResult) {
				case ALREADY_SAFE:
					instance.debug("Home is already safe, teleporting complete for " + player.getName());
					yield instance.getHellblockHandler().teleportPlayerToHome(userData,
							ownerData.getHellblockData().getHomeLocation());
				case FIXED_AND_TELEPORTED:
					instance.debug("Home fixed and teleport complete for " + player.getName());
					yield CompletableFuture.completedFuture(true);
				case FAILED_TO_FIX:
					instance.getPluginLogger()
							.warn("Failed to fix home for " + player.getName() + ", teleporting to spawn.");
					yield instance.getScheduler().callSync(() -> {
						return CompletableFuture
								.completedFuture(instance.getHellblockHandler().teleportToSpawn(player, true));
					});
				};
			}).thenCompose(success -> {
				if (success) {
					World world = player.getWorld();
					instance.getWorldManager().markWorldAccess(world.getName());
					instance.debug("Teleported " + player.getName() + " to safe location on their island.");
				}
				return instance.getScheduler().callSync(() -> {
					event.setRespawnLocation(ownerData.getHellblockData().getHomeLocation());
					instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, userData), 2L,
							player.getLocation());
					return CompletableFuture.completedFuture(success);
				});
			});
		}).handle((res, ex) -> {
			// Always unlock and end reset process
			return instance.getStorageManager().unlockUserData(ownerId).thenRun(() -> {
				if (ex != null) {
					instance.getPluginLogger()
							.severe("Exception when respawning player " + player.getName() + " on their island", ex);
				}
			});
		}).thenCompose(Function.identity()); // Flatten nested CompletableFuture
	}

	@EventHandler
	public void onFallInVoid(PlayerMoveEvent event) {
		if (!instance.getConfigManager().voidTeleport()) {
			return;
		}

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (movedWithinBlock(event)) {
			return;
		}
		if (player.getLocation().getY() > 0) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();
		if (userData.getHellblockData().hasHellblock()) {
			final UUID ownerId = userData.getHellblockData().getOwnerUUID();
			if (ownerId == null) {
				return;
			}

			instance.getStorageManager().getCachedUserDataWithFallback(ownerId, true).thenCompose(optData -> {
				if (optData.isEmpty()) {
					return CompletableFuture.completedFuture(null);
				}

				final UserData ownerData = optData.get();
				return instance.getCoopManager().makeHomeLocationSafe(ownerData, userData).thenCompose(safetyResult -> {
					return switch (safetyResult) {
					case ALREADY_SAFE:
						instance.debug("Home is already safe, teleporting complete for " + player.getName());
						yield instance.getHellblockHandler().teleportPlayerToHome(userData,
								ownerData.getHellblockData().getHomeLocation());
					case FIXED_AND_TELEPORTED:
						instance.debug("Home fixed and teleport complete for " + player.getName());
						yield CompletableFuture.completedFuture(true);
					case FAILED_TO_FIX:
						instance.getPluginLogger()
								.warn("Failed to fix home for " + player.getName() + ", teleporting to spawn.");
						yield instance.getScheduler().callSync(() -> {
							return CompletableFuture
									.completedFuture(instance.getHellblockHandler().teleportToSpawn(player, true));
						});
					};
				}).thenCompose(success -> {
					if (success) {
						World world = player.getWorld();
						instance.getWorldManager().markWorldAccess(world.getName());
						instance.debug("Teleported " + player.getName() + " to safe location on their island.");
					}
					return instance.getScheduler().callSync(() -> {
						if (player.isOnline()) {
							player.setFallDistance(0.0F);
						}
						return CompletableFuture.completedFuture(success);
					});
				});
			}).handle((res, ex) -> {
				// Always unlock and end reset process
				return instance.getStorageManager().unlockUserData(ownerId).thenRun(() -> {
					if (ex != null) {
						instance.getPluginLogger().severe(
								"Exception when teleporting player " + player.getName() + " when falling in void", ex);
					}
				});
			}).thenCompose(Function.identity()); // Flatten nested CompletableFuture
		} else {
			instance.getHellblockHandler().teleportToSpawn(player, true);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		if (instance.getConfigManager().lightningOnDeath() && player.getLocation() != null) {
			player.getWorld().strikeLightningEffect(player.getLocation());
		}

		if (event.getKeepInventory()) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final UserData userData = onlineUser.get();
		if (!(userData.hasGlowstoneToolEffect() || userData.hasGlowstoneArmorEffect())
				&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			userData.isHoldingGlowstoneTool(false);
			userData.isWearingGlowstoneArmor(false);
		}
	}

	private boolean movedWithinBlock(PlayerMoveEvent event) {
		return event.getTo().getBlockX() == event.getFrom().getBlockX()
				&& event.getTo().getBlockY() == event.getFrom().getBlockY()
				&& event.getTo().getBlockZ() == event.getFrom().getBlockZ();
	}

	public int onlinePlayerCountProvider() {
		if (!instance.getConfigManager().redisRanking()) {
			return Bukkit.getOnlinePlayers().size();
		}
		int count = 0;
		final List<UUID> toRemove = new ArrayList<>();
		for (Map.Entry<UUID, PlayerCount> entry : playerCountMap.entrySet()) {
			final PlayerCount playerCount = entry.getValue();
			if ((System.currentTimeMillis() - playerCount.time) < interval * 1000L + 2333L) {
				count += playerCount.count;
			} else {
				toRemove.add(entry.getKey());
			}
		}
		toRemove.forEach(playerCountMap::remove);
		return count;
	}

	public PlayerCount updatePlayerCount(@NotNull UUID uuid, int count) {
		return playerCountMap.put(uuid, new PlayerCount(count, System.currentTimeMillis()));
	}

	private final class RedisPlayerCount implements Runnable {
		private final SchedulerTask task;

		public RedisPlayerCount(int interval) {
			task = instance.getScheduler().asyncRepeating(this, interval, interval, TimeUnit.SECONDS);
		}

		@Override
		public void run() {
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
				out.writeUTF(instance.getConfigManager().serverGroup());
				out.writeUTF("online");
				out.writeUTF(String.valueOf(identifier));
				out.writeInt(Bukkit.getOnlinePlayers().size());
				RedisManager.getInstance().publishRedisMessage(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			if (task != null && !task.isCancelled())
				task.cancel();
		}
	}

	private static class PlayerCount {
		int count;
		long time;

		public PlayerCount(int count, long time) {
			this.count = count;
			this.time = time;
		}
	}
}