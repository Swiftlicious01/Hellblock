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
	private void handleOutOfBoundsForPlayer(Player player, UUID visitorId, UserData islandOwner) {
		final BoundingBox bounds = islandOwner.getHellblockData().getBoundingBox();
		if (bounds == null)
			return;

		// Ignore Y in boundary check
		final BoundingBox ignoreYBounds = new BoundingBox(bounds.getMinX(), Double.MIN_VALUE, bounds.getMinZ(),
				bounds.getMaxX(), Double.MAX_VALUE, bounds.getMaxZ());

		// If still inside bounds, no action needed
		if (ignoreYBounds.contains(player.getBoundingBox()))
			return;

		// Prevent repeated handling — apply cooldown
		final long now = System.currentTimeMillis();
		final long lastHandled = outOfBoundsHandledTimestamps.getOrDefault(visitorId, 0L);
		if (now - lastHandled < OUT_OF_BOUNDS_COOLDOWN_MS) {
			return; // Still on cooldown, skip
		}
		outOfBoundsHandledTimestamps.put(visitorId, now);

		final Optional<UserData> visitorOpt = instance.getStorageManager().getOnlineUser(visitorId);
		if (visitorOpt.isEmpty())
			return;

		final UserData visitor = visitorOpt.get();
		final Sender audience = instance.getSenderFactory().wrap(player);
		final Location home = islandOwner.getHellblockData().getHomeLocation();

		if (home == null) {
			// Fallback: teleport visitor to their own island or spawn
			teleportVisitorToOwnIslandOrSpawn(player, visitor);
			return;
		}

		// Check if owner's home is safe
		LocationUtils.isSafeLocationAsync(home).thenAccept(isSafe -> {
			if (isSafe) {
				// Safe: teleport back to the island home location
				instance.getScheduler().executeSync(() -> {
					ChunkUtils.teleportAsync(player, home, TeleportCause.PLUGIN);
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
					instance.getWorldManager().markWorldAccess(home.getWorld().getName());
				});
				return;
			}

			// Not safe: try to fix the owner's island home
			UUID ownerUUID = visitor.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				instance.getPluginLogger().severe("Owner UUID was null while handling out-of-bounds for " + visitorId);
				teleportVisitorToOwnIslandOrSpawn(player, visitor); // fallback
				return;
			}

			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenCompose(ownerOpt -> {
						if (ownerOpt.isEmpty()) {
							return CompletableFuture.completedFuture(false);
						}
						return instance.getCoopManager().makeHomeLocationSafe(ownerOpt.get(), visitor)
								.thenApply(__ -> true);
					}).thenAccept(success -> {
						if (success) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));

							// After attempting to fix, teleport again to home (now assumed safe)
							instance.getScheduler().executeSync(() -> {
								ChunkUtils.teleportAsync(player, home, TeleportCause.PLUGIN);
								instance.getWorldManager().markWorldAccess(home.getWorld().getName());
							});
						} else {
							teleportVisitorToOwnIslandOrSpawn(player, visitor); // fallback if all else fails
						}
					});
		});
	}

	private void teleportVisitorToOwnIslandOrSpawn(Player player, UserData visitor) {
		HellblockData data = visitor.getHellblockData();
		if (data.hasHellblock()) {
			Location ownHome = data.getHomeLocation();
			final Sender audience = instance.getSenderFactory().wrap(player);
			if (ownHome != null) {
				instance.getScheduler().executeSync(() -> {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
					ChunkUtils.teleportAsync(player, ownHome, TeleportCause.PLUGIN);
					instance.getWorldManager().markWorldAccess(ownHome.getWorld().getName());
				});
				return;
			}
		}

		// Fallback: spawn
		instance.getScheduler().executeSync(() -> instance.getHellblockHandler().teleportToSpawn(player, true));
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

		instance.getScheduler().sync().run(() -> {
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
		instance.getCoopManager().getHellblockOwnerOfLocation(location).thenAccept(currentOwnerId -> {
			if (currentOwnerId == null)
				return;

			// If it's not their own island, skip enforcement
			if (!currentOwnerId.equals(playerId)
					&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
				return;
			}

			// Check if player is still within the island bounds
			instance.getProtectionManager().isInsideIsland2D(currentOwnerId, location).thenAccept(isInside -> {
				if (isInside)
					return; // Still inside — no action needed

				// Run boundary enforcement
				instance.getStorageManager()
						.getCachedUserDataWithFallback(currentOwnerId, instance.getConfigManager().lockData())
						.thenAccept(result -> result
								.ifPresent(ownerUser -> handleOutOfBoundsForPlayer(player, playerId, ownerUser)));
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

		instance.getCoopManager().getHellblockOwnerOfLocation(origin).thenAccept(currentOwnerId -> {
			if (currentOwnerId == null)
				return;

			// If it's not their own island, allow teleport
			if (!currentOwnerId.equals(playerId)
					&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
				return;
			}

			// Check if teleport target is still inside the island
			instance.getProtectionManager().isInsideIsland2D(currentOwnerId, event.getTo()).thenAccept(isInside -> {
				if (isInside)
					return; // Allowed teleport

				// Enforce teleport boundary
				instance.getStorageManager()
						.getCachedUserDataWithFallback(currentOwnerId, instance.getConfigManager().lockData())
						.thenAccept(result -> result
								.ifPresent(ownerUser -> handleOutOfBoundsForPlayer(player, playerId, ownerUser)));
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
		instance.getCoopManager().getHellblockOwnerOfLocation(location).thenAccept(currentOwnerId -> {
			if (currentOwnerId == null)
				return;

			// Only enforce if this is the player's own island
			if (!currentOwnerId.equals(playerId)
					&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
				return;
			}

			// Check if they are leaving their own island's bounds
			instance.getProtectionManager().isInsideIsland2D(currentOwnerId, location).thenAccept(isInside -> {
				if (isInside)
					return;

				// Confirm: this is their own island
				instance.getStorageManager()
						.getCachedUserDataWithFallback(playerId, instance.getConfigManager().lockData())
						.thenAccept(optUser -> {
							if (optUser.isEmpty())
								return;

							UserData userData = optUser.get();

							// Enforce only if the player is the island *owner* or coop member of current
							// island
							if (!currentOwnerId.equals(playerId)
									&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
								return; // Not their island — ignore
							}

							instance.getScheduler().sync().run(() -> {
								if (!player.isOnline())
									return;

								player.setGliding(false);

								Location fallback = instance.getHellblockHandler().getSafeSpawnLocation(userData);
								if (fallback == null) {
									instance.getPluginLogger().warn("Could not find a valid spawn location for player "
											+ player.getName() + " — teleport cancelled.");
									return;
								}

								player.teleport(fallback);
								instance.debug("Teleported " + player.getName() + " to their Hellblock home.");
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
			instance.getCoopManager().getHellblockOwnerOfLocation(location).thenAccept(currentOwnerId -> {
				if (currentOwnerId == null)
					return;

				// Only enforce border for owners or coop members
				if (!currentOwnerId.equals(playerId)
						&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
					return;
				}

				// Check if they're still inside their own island
				instance.getProtectionManager().isInsideIsland2D(currentOwnerId, location).thenAccept(isInside -> {
					if (isInside)
						return;

					// Player left their island via vehicle
					instance.getStorageManager()
							.getCachedUserDataWithFallback(currentOwnerId, instance.getConfigManager().lockData())
							.thenAccept(result -> result
									.ifPresent(ownerUser -> handleOutOfBoundsForPlayer(player, playerId, ownerUser)));
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
		instance.getCoopManager().getHellblockOwnerOfLocation(respawnLoc).thenAccept(currentOwnerId -> {
			if (currentOwnerId == null)
				return;

			// Only care if it's their own island (owner or member)
			if (!currentOwnerId.equals(playerId)
					&& !instance.getCoopManager().isIslandMember(currentOwnerId, playerId)) {
				return;
			}

			// If their respawn location is outside bounds, redirect
			instance.getProtectionManager().isInsideIsland2D(currentOwnerId, respawnLoc).thenAccept(isInside -> {
				if (isInside)
					return;

				// Teleport to spawn or fallback location
				instance.getStorageManager()
						.getCachedUserDataWithFallback(currentOwnerId, instance.getConfigManager().lockData())
						.thenAccept(optUser -> {
							if (optUser.isEmpty())
								return;
							UserData userData = optUser.get();

							instance.getScheduler().sync().run(() -> {
								Location fallback = instance.getHellblockHandler().getSafeSpawnLocation(userData);

								if (fallback == null) {
									instance.getPluginLogger().warn("Could not determine a safe respawn location for "
											+ player.getName() + " (home or world may be null)");
									return;
								}

								event.setRespawnLocation(fallback);
								instance.debug("Redirected respawn for " + player.getName()
										+ " to their Hellblock home (was outside their island bounds).");
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

		final UserData user = onlineUser.get();

		// --- LEFT the Hellblock world: remove NV and clear flags if they had the
		// effect ---
		if (instance.getHellblockHandler().isInCorrectWorld(event.getFrom())) {
			instance.getBorderHandler().clearPlayerBorder(player);

			// If they were mid-expansion, mark it as completed
			if (instance.getBorderHandler().isBorderExpanding(player.getUniqueId())) {
				instance.debug("Player " + player.getName() + " changed world during border animation; finalizing.");
				instance.getBorderHandler().setBorderExpanding(player.getUniqueId(), false);

				// You can also show the final border if applicable:
				if (user.getHellblockData().getBoundingBox() != null
						&& user.getHellblockData().getHellblockLocation() != null) {
					BoundingBox bounds = user.getHellblockData().getBoundingBox();
					if (player.getWorld().getName()
							.equalsIgnoreCase(user.getHellblockData().getHellblockLocation().getWorld().getName())
							&& bounds.contains(player.getLocation().toVector())) {
						instance.getBorderHandler().setWorldBorder(player, bounds, BorderColor.RED);
					}
				}
			}

			instance.getBorderHandler().stopBorderTask(player.getUniqueId());

			if ((user.hasGlowstoneToolEffect() || user.hasGlowstoneArmorEffect())
					&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
				user.isHoldingGlowstoneTool(false);
				user.isWearingGlowstoneArmor(false);
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
			applyNightVisionIfEligible(player, user);
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

		final UserData user = onlineUser.get();

		if (instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, user), 2L,
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

		final UserData user = onlineUser.get();

		// Only restore inside Hellblock world
		if (instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, user), 2L,
					player.getLocation()); // Delay by 2 ticks (~100ms)
		}
	}

	private void applyNightVisionIfEligible(Player player, UserData user) {
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
			user.isHoldingGlowstoneTool(true);
		}
		// check off hand if main hand is invalid tool
		if (!shouldRestore && offHand.getType() != Material.AIR
				&& instance.getNetherToolsHandler().isNetherToolEnabled(offHand)
				&& instance.getNetherToolsHandler().isNetherToolNightVisionAllowed(offHand)
				&& instance.getNetherToolsHandler().checkNightVisionToolStatus(offHand)
				&& instance.getNetherToolsHandler().getNightVisionToolStatus(offHand)) {
			shouldRestore = true;
			user.isHoldingGlowstoneTool(true);
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
				user.isWearingGlowstoneArmor(true);
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
		final UUID ownerUUID = userData.getHellblockData().getOwnerUUID();
		if (ownerUUID == null) {
			instance.getPluginLogger()
					.severe("Owner reference returned null in respawn event for " + player.getUniqueId());
			return;
		}

		instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
				.thenAccept(ownerOpt -> ownerOpt.ifPresent(
						ownerUser -> instance.getCoopManager().makeHomeLocationSafe(ownerUser, userData).thenRun(() -> {
							event.setRespawnLocation(userData.getHellblockData().getHomeLocation());
							if (instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
								instance.getScheduler().sync().runLater(
										() -> applyNightVisionIfEligible(player, userData), 2L, player.getLocation());
							}
						})));
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

		if (onlineUser.get().getHellblockData().hasHellblock()) {
			final UUID ownerId = onlineUser.get().getHellblockData().getOwnerUUID();
			if (ownerId == null) {
				return;
			}

			instance.getStorageManager().getCachedUserDataWithFallback(ownerId, instance.getConfigManager().lockData())
					.thenAccept(owner -> {
						if (owner.isPresent()) {
							instance.getCoopManager().makeHomeLocationSafe(owner.get(), onlineUser.get())
									.thenRun(() -> instance.getScheduler().sync().run(() -> {
										if (player.isOnline()) {
											player.setFallDistance(0.0F);
										}
									}));
						}
					});
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

		if (!(onlineUser.get().hasGlowstoneToolEffect() || onlineUser.get().hasGlowstoneArmorEffect())
				&& player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
			player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			onlineUser.get().isHoldingGlowstoneTool(false);
			onlineUser.get().isWearingGlowstoneArmor(false);
		}
	}

	private boolean movedWithinBlock(PlayerMoveEvent e) {
		return e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY()
				&& e.getTo().getBlockZ() == e.getFrom().getBlockZ();
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

	public void updatePlayerCount(UUID uuid, int count) {
		playerCountMap.put(uuid, new PlayerCount(count, System.currentTimeMillis()));
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