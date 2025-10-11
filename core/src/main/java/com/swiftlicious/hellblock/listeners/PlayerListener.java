package com.swiftlicious.hellblock.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
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
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.database.RedisManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

public class PlayerListener implements Listener, Reloadable {

	protected final HellblockPlugin instance;
	private final Map<UUID, SchedulerTask> cancellablePortal = new ConcurrentHashMap<>();
	private final Set<UUID> linkPortalCatcher = ConcurrentHashMap.newKeySet();

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
		}
		cancellablePortal.keySet().forEach(this::cancelPortalTask);
		cancellablePortal.clear();
		linkPortalCatcher.clear();
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

	public Map<UUID, SchedulerTask> getCancellablePortalMap() {
		return this.cancellablePortal;
	}

	public Set<UUID> getLinkPortalCatcherSet() {
		return this.linkPortalCatcher;
	}

	private void runSync(Runnable r) {
		instance.getScheduler().executeSync(r);
	}

	private void cancelPortalTask(UUID id) {
		final SchedulerTask task = cancellablePortal.remove(id);
		if (task != null && !task.isCancelled()) {
			task.cancel();
		}
	}

	/**
	 * Sends the link portal tutorial message and plays linking sound (if
	 * configured).
	 */
	private void sendLinkTutorial(Player player, @Nullable String ownerName) {
		final Sender audience = instance.getSenderFactory().wrap(player);
		final Component arg = ownerName != null ? AdventureHelper.miniMessage(ownerName)
				: AdventureHelper.miniMessage(instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key()));
		audience.sendMessage(instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_LINK_TUTORIAL.arguments(arg).build()));
		if (instance.getConfigManager().linkingHellblockSound() != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					instance.getConfigManager().linkingHellblockSound());
		}
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
				runSync(() -> {
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

			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
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
							runSync(() -> {
								ChunkUtils.teleportAsync(player, home, TeleportCause.PLUGIN);
								instance.getWorldManager().markWorldAccess(home.getWorld().getName());
							});
						} else {
							teleportVisitorToOwnIslandOrSpawn(player, visitor); // fallback if all else fails
						}
					});
		});
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		outOfBoundsHandledTimestamps.remove(uuid);
	}

	private void teleportVisitorToOwnIslandOrSpawn(Player player, UserData visitor) {
		HellblockData data = visitor.getHellblockData();
		if (data.hasHellblock()) {
			Location ownHome = data.getHomeLocation();
			final Sender audience = instance.getSenderFactory().wrap(player);
			if (ownHome != null) {
				runSync(() -> {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
					ChunkUtils.teleportAsync(player, ownHome, TeleportCause.PLUGIN);
					instance.getWorldManager().markWorldAccess(ownHome.getWorld().getName());
				});
				return;
			}
		}

		// Fallback: spawn
		runSync(() -> instance.getHellblockHandler().teleportToSpawn(player, true));
	}

	@EventHandler
	public void onBedClick(PlayerBedEnterEvent event) {
		if (!instance.getConfigManager().disableBedExplosions()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block bed = event.getBed();
		final boolean sleepCondition = Tag.BEDS.isTagged(bed.getType())
				&& event.getBedEnterResult() == BedEnterResult.NOT_POSSIBLE_HERE && !bed.getWorld().isBedWorks()
				&& bed.getWorld().getEnvironment() == Environment.NETHER;
		if (!sleepCondition) {
			return;
		}
		event.setCancelled(true);
		event.setUseBed(Result.DENY);
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
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		// ignore small movement (still within same block)
		if (movedWithinBlock(event)) {
			return;
		}

		final UUID id = player.getUniqueId();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(
							result -> result.ifPresent(ownerUser -> handleOutOfBoundsForPlayer(player, id, ownerUser)));
		});
	}

	@SuppressWarnings("removal")
	@EventHandler
	public void onTeleportOutsideBorders(PlayerTeleportEvent event) {
		// only care about ender pearl / chorus fruit / dismount teleports
		final Set<PlayerTeleportEvent.TeleportCause> allowedCauses = EnumSet.of(
				PlayerTeleportEvent.TeleportCause.ENDER_PEARL, PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT,
				PlayerTeleportEvent.TeleportCause.DISMOUNT);
		if (!allowedCauses.contains(event.getCause())) {
			return;
		}

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final UUID id = player.getUniqueId();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(
							result -> result.ifPresent(ownerUser -> handleOutOfBoundsForPlayer(player, id, ownerUser)));
		});
	}

	@EventHandler
	public void onUsePortal(PlayerPortalEvent event) {
		final Player player = event.getPlayer();
		if (!(player.getWorld().getEnvironment() == Environment.NETHER
				&& event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
			return;
		}
		event.setCancelled(true);

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (onlineUser.isEmpty()) {
			return;
		}

		final SchedulerTask portalTask = instance.getScheduler().sync().runLater(() -> {
			if (!player.isOnline()) {
				return;
			}

			if (onlineUser.get().getHellblockData().hasHellblock()) {
				// Teleport to linked island if valid
				final UUID linked = onlineUser.get().getHellblockData().getLinkedUUID();
				if (instance.getConfigManager().linkHellblocks() && linked != null) {
					instance.getStorageManager().getOfflineUserData(linked, instance.getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									return;
								}
								final UserData linkedOwner = result.get();

								if (!linkedOwner.getHellblockData().hasHellblock()
										|| linkedOwner.getHellblockData().isAbandoned()
										|| linkedOwner.getHellblockData().getHomeLocation() == null) {
									instance.getScheduler().sync()
											.run(() -> instance.getSenderFactory().wrap(player)
													.sendMessage(instance.getTranslationManager().render(
															MessageConstants.MSG_HELLBLOCK_UNSAFE_LINKING.build())));
									onlineUser.get().getHellblockData().setLinkedUUID(null);
									return;
								}

								final Location home = linkedOwner.getHellblockData().getHomeLocation();
								if (home == null) {
									return;
								}

								LocationUtils.isSafeLocationAsync(home).thenAccept(safe -> {
									if (!safe) {
										final UUID ownerId = onlineUser.get().getHellblockData().getOwnerUUID();
										if (ownerId != null) {
											instance.getStorageManager()
													.getOfflineUserData(ownerId, instance.getConfigManager().lockData())
													.thenAccept(owner -> {
														if (owner.isPresent()) {
															instance.getCoopManager().makeHomeLocationSafe(owner.get(),
																	onlineUser.get());
														}
													});
										}
									}
									instance.getScheduler().sync().run(() -> {
										if (player.isOnline()) {
											ChunkUtils.teleportAsync(player, home,
													PlayerTeleportEvent.TeleportCause.PLUGIN);
										}
									});
								});
							});
				} else {
					// teleport to own safe home
					final UUID ownerId = onlineUser.get().getHellblockData().getOwnerUUID();
					if (ownerId != null) {
						instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
								.thenAccept(owner -> {
									if (owner.isPresent()) {
										instance.getCoopManager().makeHomeLocationSafe(owner.get(), onlineUser.get());
									}
								});
					}
				}
			} else {
				instance.getScheduler().sync().run(() -> {
					if (player.isOnline()) {
						instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
					}
				});
			}
		}, 5 * 20, onlineUser.get().getHellblockData().getHomeLocation());

		this.cancellablePortal.putIfAbsent(player.getUniqueId(), portalTask);
	}

	@EventHandler
	public void onLinkPortalCreate(PortalCreateEvent event) {
		if (!instance.getConfigManager().linkHellblocks()) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getWorld())) {
			return;
		}
		if (event.getWorld().getEnvironment() != Environment.NETHER) {
			return;
		}
		if (event.getReason() != CreateReason.FIRE) {
			return;
		}
		final Entity entity = event.getEntity();
		if (entity == null || !(entity instanceof Player player)) {
			return;
		}

		final UUID id = player.getUniqueId();
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty()) {
			return;
		}
		final UserData user = onlineUser.get();
		if (!user.getHellblockData().hasHellblock() || linkPortalCatcher.contains(id)) {
			return;
		}

		final UUID linked = user.getHellblockData().getLinkedUUID();
		final String owner = linked != null ? (Bukkit.getPlayer(linked) != null ? Bukkit.getPlayer(linked).getName()
				: Bukkit.getOfflinePlayer(linked).getName()) : null;

		sendLinkTutorial(player, owner);
		linkPortalCatcher.add(id);
	}

	@EventHandler
	public void onLinkPortalInteract(PlayerInteractEvent event) {
		if (!instance.getConfigManager().linkHellblocks()) {
			return;
		}
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		if (player.getWorld().getEnvironment() != Environment.NETHER) {
			return;
		}
		final Block block = event.getClickedBlock();
		if (block == null || block.getType() != Material.NETHER_PORTAL) {
			return;
		}
		final UUID id = player.getUniqueId();
		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty()) {
			return;
		}
		final UserData user = onlineUser.get();
		if (!user.getHellblockData().hasHellblock() || linkPortalCatcher.contains(id)) {
			return;
		}

		final UUID linked = user.getHellblockData().getLinkedUUID();
		final String owner = linked != null ? (Bukkit.getPlayer(linked) != null ? Bukkit.getPlayer(linked).getName()
				: Bukkit.getOfflinePlayer(linked).getName()) : null;

		sendLinkTutorial(player, owner);
		linkPortalCatcher.add(id);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onLinkPortalChat(AsyncPlayerChatEvent event) {
		if (!instance.getConfigManager().linkHellblocks()) {
			return;
		}

		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (!linkPortalCatcher.contains(id)) {
			return;
		}

		final Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty()) {
			return;
		}

		event.setCancelled(true);
		final String username = event.getMessage();
		final Sender audience = instance.getSenderFactory().wrap(player);

		if ("none".equalsIgnoreCase(username) || username.equalsIgnoreCase(player.getName())) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
			linkPortalCatcher.remove(id);
			return;
		}

		final Player linkedOnline = Bukkit.getPlayer(username);
		if (linkedOnline != null) {
			final Optional<UserData> linked = instance.getStorageManager().getOnlineUser(linkedOnline.getUniqueId());
			if (linked.isEmpty()) {
				linkPortalCatcher.remove(id);
				return;
			}
			final UserData linkedUser = linked.get();
			if (!linkedUser.getHellblockData().hasHellblock() || linkedUser.getHellblockData().isAbandoned()
					|| linkedUser.getHellblockData().getHomeLocation() == null) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build()));
				linkPortalCatcher.remove(id);
				return;
			}
			if (linkedUser.getHellblockData().isLocked()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build()));
				linkPortalCatcher.remove(id);
				return;
			}
			if (linkedUser.getHellblockData().getBanned().contains(id)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build()));
				linkPortalCatcher.remove(id);
				return;
			}
			onlineUser.get().getHellblockData().setLinkedUUID(linkedOnline.getUniqueId());
			audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
					.arguments(AdventureHelper.miniMessage(username)).build()));
			linkPortalCatcher.remove(id);
			return;
		}

		// if not online, try fetch offline by username
		final UUID linkID = UUIDFetcher.getUUID(username);
		if (linkID == null || !Bukkit.getOfflinePlayer(linkID).hasPlayedBefore()) {
			audience.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_PLAYER_NOT_FOUND.build()));
			linkPortalCatcher.remove(id);
			return;
		}

		instance.getStorageManager().getOfflineUserData(linkID, instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_PLAYER_NOT_FOUND.build())));
						linkPortalCatcher.remove(id);
						return;
					}
					final UserData offlineUser = result.get();
					if (!offlineUser.getHellblockData().hasHellblock() || offlineUser.getHellblockData().isAbandoned()
							|| offlineUser.getHellblockData().getHomeLocation() == null) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build())));
						linkPortalCatcher.remove(id);
						return;
					}
					if (offlineUser.getHellblockData().isLocked()) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build())));
						linkPortalCatcher.remove(id);
						return;
					}
					if (offlineUser.getHellblockData().getBanned().contains(id)) {
						runSync(() -> audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build())));
						linkPortalCatcher.remove(id);
						return;
					}
					onlineUser.get().getHellblockData().setLinkedUUID(linkID);
					runSync(() -> audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
									.arguments(AdventureHelper.miniMessage(username)).build())));
					linkPortalCatcher.remove(id);
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

		if (instance.getHellblockHandler().isInCorrectWorld(player)) {
			instance.getBorderHandler().startBorderTask(player);
		} else {
			instance.getBorderHandler().stopBorderTask(player.getUniqueId());
		}

		// --- LEFT the Hellblock world: remove NV and clear flags if they had the
		// effect ---
		if (instance.getHellblockHandler().isInCorrectWorld(event.getFrom())) {
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

		instance.getScheduler().sync().runLater(() -> applyNightVisionIfEligible(player, user), 2L,
				player.getLocation()); // Delay by 2 ticks (~100ms)
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
	public void onMoveIfInPortal(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}
		// ignore same-block moves
		if (event.getTo().getBlockX() == event.getFrom().getBlockX()
				&& event.getTo().getBlockY() == event.getFrom().getBlockY()
				&& event.getTo().getBlockZ() == event.getFrom().getBlockZ()) {
			return;
		}
		if (playerInPortal(player)) {
			return;
		}
		final UUID id = player.getUniqueId();
		if (cancellablePortal.containsKey(id)) {
			cancelPortalTask(id);
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		// prevent non-player entities from using nether portals in hellblock world
		final Entity ent = event.getEntity();
		if (ent instanceof LivingEntity && ent.getWorld().getEnvironment() == Environment.NETHER) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBreed(EntityBreedEvent event) {
		if (!(event.getBreeder() instanceof Player player)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Entity bred = event.getEntity();
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(
				user -> instance.getChallengeManager().handleChallengeProgression(player, ActionType.BREED, bred));
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

		instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(
				user -> instance.getChallengeManager().handleChallengeProgression(killer, ActionType.SLAY, entity));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (player.getWorld().isRespawnAnchorWorks() && getRespawnLocation(player) == null) {
			return;
		}
		if (event.getRespawnReason() != RespawnReason.DEATH) {
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

		instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
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

			instance.getStorageManager().getOfflineUserData(ownerId, instance.getConfigManager().lockData())
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

	private boolean playerInPortal(Player player) {
		try {
			final Block target = player.getTargetBlockExact(1);
			if (target == null) {
				return false;
			}
			final Material portal = target.getType();
			final Material standing = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			return portal == Material.NETHER_PORTAL && (standing == Material.OBSIDIAN
					|| standing == Material.NETHER_PORTAL || standing == Material.AIR);
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	/**
	 * Safely retrieves the player's respawn location. Uses getRespawnLocation() if
	 * available, otherwise falls back to getBedSpawnLocation().
	 *
	 * @param player The player.
	 * @return The respawn location, or null if not set.
	 */
	@SuppressWarnings("deprecation")
	public Location getRespawnLocation(Player player) {
		try {
			// Try to call getRespawnLocation() reflectively
			final Method method = Player.class.getMethod("getRespawnLocation");
			final Object result = method.invoke(player);
			return result instanceof Location ? (Location) result : null;
		} catch (NoSuchMethodException e) {
			// Fallback for older versions
			return player.getBedSpawnLocation(); // Deprecated, but safe here
		} catch (Exception e) {
			// Unexpected error
			e.printStackTrace();
			return null;
		}
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