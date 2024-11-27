package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import net.kyori.adventure.audience.Audience;

public class PlayerListener implements Listener {

	protected final HellblockPlugin instance;
	private final Map<UUID, SchedulerTask> cancellablePortal;
	private final Set<UUID> linkPortalCatcher;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		this.cancellablePortal = new HashMap<>();
		this.linkPortalCatcher = new HashSet<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public Map<UUID, SchedulerTask> getCancellablePortalMap() {
		return this.cancellablePortal;
	}

	public Set<UUID> getLinkPortalCatcherSet() {
		return this.linkPortalCatcher;
	}

	@EventHandler
	public void onBedClick(PlayerInteractEvent event) {
		if (!instance.getConfigManager().disableBedExplosions())
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;

		Block bed = event.getClickedBlock();
		if (bed != null && Tag.BEDS.isTagged(bed.getType())) {
			if (bed.getWorld().getEnvironment() == Environment.NETHER) {
				event.setCancelled(true);
				event.setUseInteractedBlock(Result.DENY);
			}
		}
	}

	@EventHandler
	public void onFireworkDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
				return;
			if (event.getDamager() instanceof Firework firework) {
				if (firework instanceof FakeFirework) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onFallingBlockLandOnSoulSand(EntityDropItemEvent event) {
		if (event.getEntity() instanceof FallingBlock fallingBlock) {
			if (!fallingBlock.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
				return;
			if (fallingBlock.getLocation().getBlock().getType() == Material.SOUL_SAND
					&& fallingBlock.getLocation().getBlock().getRelative(BlockFace.UP).getType() != Material.LAVA) {
				event.setCancelled(true);
				fallingBlock.getLocation().getBlock().getRelative(BlockFace.UP)
						.setType(fallingBlock.getBlockData().getMaterial());
				fallingBlock.setDropItem(false);
			}
		}
	}

	@EventHandler
	public void onLeaveHellblockBorders(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		final UUID id = player.getUniqueId();
		if (player.getLocation() == null)
			return;
		Audience audience = instance.getSenderFactory().getAudience(player);
		if (instance.getConfigManager().worldguardProtect()) {
			if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
				return;
			if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) == null)
				return;
			instance.getStorageManager()
					.getOfflineUserData(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player),
							instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						BoundingBox hellblockBounds = offlineUser.getHellblockData().getBoundingBox();
						if (hellblockBounds != null) {
							BoundingBox ignoreYValueBounds = new BoundingBox(hellblockBounds.getMinX(),
									Double.MIN_VALUE, hellblockBounds.getMinZ(), hellblockBounds.getMaxX(),
									Double.MAX_VALUE, hellblockBounds.getMaxZ());
							BoundingBox playerBounds = player.getBoundingBox();
							if (!ignoreYValueBounds.contains(playerBounds)) {
								Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
								if (onlineUser.isEmpty())
									return;
								LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation())
										.thenAccept((tiResult) -> {
											if (!tiResult.booleanValue()) {
												audience.sendMessage(instance.getTranslationManager()
														.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_TELEPORT_TO_ISLAND
																.build()));

												if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
													throw new NullPointerException(
															"Owner reference returned null, please report this to the developer.");
												}
												instance.getStorageManager()
														.getOfflineUserData(
																onlineUser.get().getHellblockData().getOwnerUUID(),
																instance.getConfigManager().lockData())
														.thenAccept((owner) -> {
															if (owner.isEmpty())
																return;
															UserData ownerUser = owner.get();
															instance.getCoopManager()
																	.makeHomeLocationSafe(ownerUser, onlineUser.get())
																	.thenRun(() -> audience.sendMessage(
																			instance.getTranslationManager().render(
																					MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER
																							.build())));
														});
											}
											return;
										}).thenRunAsync(() -> {
											ChunkUtils.teleportAsync(player,
													offlineUser.getHellblockData().getHomeLocation(),
													TeleportCause.PLUGIN);
											audience.sendMessage(instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
										});
							}
						}
					});
		}
	}

	@EventHandler
	public void onTeleportOutsideBorders(PlayerTeleportEvent event) {
		if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT
				|| event.getCause() == TeleportCause.DISMOUNT) {
			final Player player = event.getPlayer();
			if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
				return;
			final UUID id = player.getUniqueId();
			if (player.getLocation() == null)
				return;
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (instance.getConfigManager().worldguardProtect()) {
				if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
					return;
				if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) == null)
					return;
				instance.getStorageManager()
						.getOfflineUserData(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player),
								instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							BoundingBox hellblockBounds = offlineUser.getHellblockData().getBoundingBox();
							if (hellblockBounds != null) {
								BoundingBox ignoreYValueBounds = new BoundingBox(hellblockBounds.getMinX(),
										Double.MIN_VALUE, hellblockBounds.getMinZ(), hellblockBounds.getMaxX(),
										Double.MAX_VALUE, hellblockBounds.getMaxZ());
								BoundingBox playerBounds = player.getBoundingBox();
								if (!ignoreYValueBounds.contains(playerBounds)) {
									Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
									if (onlineUser.isEmpty())
										return;
									LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation())
											.thenAccept((tiResult) -> {
												if (!tiResult.booleanValue()) {
													audience.sendMessage(instance.getTranslationManager().render(
															MessageConstants.MSG_HELLBLOCK_UNSAFE_TELEPORT_TO_ISLAND
																	.build()));

													if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
														throw new NullPointerException(
																"Owner reference returned null, please report this to the developer.");
													}
													instance.getStorageManager()
															.getOfflineUserData(
																	onlineUser.get().getHellblockData().getOwnerUUID(),
																	instance.getConfigManager().lockData())
															.thenAccept((owner) -> {
																if (owner.isEmpty())
																	return;
																UserData ownerUser = owner.get();
																instance.getCoopManager()
																		.makeHomeLocationSafe(ownerUser,
																				onlineUser.get())
																		.thenRun(() -> audience.sendMessage(
																				instance.getTranslationManager().render(
																						MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER
																								.build())));
															});
												}
												return;
											}).thenRunAsync(() -> {
												ChunkUtils.teleportAsync(player,
														offlineUser.getHellblockData().getHomeLocation(),
														TeleportCause.PLUGIN);
												audience.sendMessage(instance.getTranslationManager().render(
														MessageConstants.MSG_HELLBLOCK_NO_LEAVING_BORDER.build()));
											});
								}
							}
						});
			}
		}
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
		final Player player = event.getPlayer();
		if (player.getWorld().getEnvironment() == Environment.NETHER
				&& event.getCause() == TeleportCause.NETHER_PORTAL) {
			event.setCancelled(true);
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			Audience audience = instance.getSenderFactory().getAudience(player);
			SchedulerTask portalTask = instance.getScheduler().sync().runLater(() -> {
				if (onlineUser.get().getHellblockData().hasHellblock()) {
					if (onlineUser.get().getHellblockData().getLinkedUUID() != null) {
						instance.getStorageManager()
								.getOfflineUserData(onlineUser.get().getHellblockData().getLinkedUUID(),
										instance.getConfigManager().lockData())
								.thenAccept((result) -> {
									if (result.isEmpty())
										return;
									UserData offlineUser = result.get();
									if (!offlineUser.getHellblockData().hasHellblock()
											|| offlineUser.getHellblockData().isAbandoned()) {
										audience.sendMessage(instance.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_LINKING.build()));
										onlineUser.get().getHellblockData().setLinkedUUID(null);
										return;
									}
									if (offlineUser.getHellblockData().isLocked()) {
										audience.sendMessage(instance.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));
										return;
									}
									LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation())
											.thenAccept((tiResult) -> {
												if (!tiResult.booleanValue()) {
													audience.sendMessage(instance.getTranslationManager().render(
															MessageConstants.MSG_HELLBLOCK_UNSAFE_TELEPORT_TO_ISLAND
																	.build()));

													if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
														throw new NullPointerException(
																"Owner reference returned null, please report this to the developer.");
													}
													instance.getStorageManager()
															.getOfflineUserData(
																	onlineUser.get().getHellblockData().getOwnerUUID(),
																	instance.getConfigManager().lockData())
															.thenAccept((owner) -> {
																if (owner.isEmpty())
																	return;
																UserData ownerUser = owner.get();
																instance.getCoopManager().makeHomeLocationSafe(
																		ownerUser, onlineUser.get());
															});
												}
												return;
											}).thenRunAsync(() -> {
												ChunkUtils.teleportAsync(player,
														offlineUser.getHellblockData().getHomeLocation(),
														TeleportCause.PLUGIN);
											});
								});
					} else {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						instance.getStorageManager()
								.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
										instance.getConfigManager().lockData())
								.thenAccept((owner) -> {
									if (owner.isEmpty())
										return;
									UserData ownerUser = owner.get();
									instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get());
								});
					}
				} else {
					// TODO: stuff
				}
			}, 5 * 20, onlineUser.get().getHellblockData().getHomeLocation());
			this.cancellablePortal.putIfAbsent(player.getUniqueId(), portalTask);
		}
	}

	@EventHandler
	public void onLinkPortal(PortalCreateEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		if (event.getWorld().getEnvironment() != Environment.NETHER)
			return;
		if (event.getReason() != CreateReason.FIRE)
			return;
		if (event.getEntity() != null && event.getEntity() instanceof Player player) {
			final UUID id = player.getUniqueId();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (!this.linkPortalCatcher.contains(id)) {
					Audience audience = instance.getSenderFactory().getAudience(player);
					String owner = onlineUser.get().getHellblockData().getLinkedUUID() != null
							? (Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()) != null
									? Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()).getName()
									: Bukkit.getOfflinePlayer(onlineUser.get().getHellblockData().getLinkedUUID())
											.getName())
							: null;
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_TUTORIAL.arguments(owner != null
									? AdventureHelper.miniMessage(owner)
									: AdventureHelper.miniMessage(instance.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key())))
									.build()));
					if (instance.getConfigManager().linkingHellblockSound() != null)
						audience.playSound(instance.getConfigManager().linkingHellblockSound());
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@EventHandler
	public void onLinkPortal(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		if (player.getWorld().getEnvironment() != Environment.NETHER)
			return;
		Block block = event.getClickedBlock();
		if (block != null && block.getType() == Material.NETHER_PORTAL) {
			final UUID id = player.getUniqueId();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (!this.linkPortalCatcher.contains(id)) {
					Audience audience = instance.getSenderFactory().getAudience(player);
					String owner = onlineUser.get().getHellblockData().getLinkedUUID() != null
							? (Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()) != null
									? Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()).getName()
									: Bukkit.getOfflinePlayer(onlineUser.get().getHellblockData().getLinkedUUID())
											.getName())
							: null;
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_TUTORIAL.arguments(owner != null
									? AdventureHelper.miniMessage(owner)
									: AdventureHelper.miniMessage(instance.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key())))
									.build()));
					if (instance.getConfigManager().linkingHellblockSound() != null)
						audience.playSound(instance.getConfigManager().linkingHellblockSound());
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onLinkPortalChat(AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (this.linkPortalCatcher.contains(id)) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			event.setCancelled(true);
			String username = event.getMessage();
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (username.equalsIgnoreCase("none") || username.equalsIgnoreCase(player.getName())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
				this.linkPortalCatcher.remove(id);
				return;
			}
			Player link = Bukkit.getPlayer(username);
			if (link != null) {
				if ((onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& onlineUser.get().getHellblockData().getOwnerUUID().equals(link.getUniqueId()))
						|| onlineUser.get().getHellblockData().getParty().contains(link.getUniqueId())) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_SAME_PARTY.build()));
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
					this.linkPortalCatcher.remove(id);
					return;
				}
				Optional<UserData> linked = instance.getStorageManager().getOnlineUser(link.getUniqueId());
				if (linked.isEmpty())
					return;
				if (!linked.get().getHellblockData().hasHellblock() || linked.get().getHellblockData().isAbandoned()
						|| linked.get().getHellblockData().getHomeLocation() == null) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build()));
					return;
				}
				if (linked.get().getHellblockData().isLocked()) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build()));
					return;
				}
				if (linked.get().getHellblockData().getBanned().contains(id)) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build()));
					return;
				}
				onlineUser.get().getHellblockData().setLinkedUUID(link.getUniqueId());
				audience.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
						.arguments(AdventureHelper.miniMessage(username)).build()));
			} else {
				UUID linkID = UUIDFetcher.getUUID(username);
				if (linkID == null) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_PLAYER_NOT_FOUND.build()));
					return;
				}
				if (!Bukkit.getOfflinePlayer(linkID).hasPlayedBefore()) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_PLAYER_NOT_FOUND.build()));
					return;
				}
				if ((onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& onlineUser.get().getHellblockData().getOwnerUUID().equals(linkID))
						|| onlineUser.get().getHellblockData().getParty().contains(linkID)) {
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_SAME_PARTY.build()));
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_OWN.build()));
					this.linkPortalCatcher.remove(id);
					return;
				}
				instance.getStorageManager().getOfflineUserData(linkID, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							if (!offlineUser.getHellblockData().hasHellblock()
									|| offlineUser.getHellblockData().isAbandoned()
									|| offlineUser.getHellblockData().getHomeLocation() == null) {
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_NO_ISLAND.build()));
								return;
							}
							if (offlineUser.getHellblockData().isLocked()) {
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_LOCKED.build()));
								return;
							}
							if (offlineUser.getHellblockData().getBanned().contains(id)) {
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_LINK_FAILURE_BANNED.build()));
								return;
							}
							onlineUser.get().getHellblockData().setLinkedUUID(linkID);
							audience.sendMessage(
									instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_LINK_SUCCESS
											.arguments(AdventureHelper.miniMessage(username)).build()));
						});
			}
			this.linkPortalCatcher.remove(id);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (event.getFrom().getName().equals(instance.getConfigManager().worldName())) {
			final Player player = event.getPlayer();
			final UUID id = player.getUniqueId();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().hasGlowstoneToolEffect() || onlineUser.get().hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					onlineUser.get().isHoldingGlowstoneTool(false);
					onlineUser.get().isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	@EventHandler
	public void onMoveIfInPortal(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		if (playerInPortal(player))
			return;
		final UUID id = player.getUniqueId();
		if (this.cancellablePortal.containsKey(id) && this.cancellablePortal.get(id) != null) {
			this.cancellablePortal.get(id).cancel();
			this.cancellablePortal.remove(id);
		}
	}

	@EventHandler
	public void onEntityPortal(EntityPortalEvent event) {
		if (event.getEntity() instanceof LivingEntity entity) {
			if (entity.getWorld().getEnvironment() == Environment.NETHER) {
				event.setCancelled(true);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (player.getBedSpawnLocation() != null
				&& player.getBedSpawnLocation().getBlock().getType() == Material.RESPAWN_ANCHOR)
			return;
		if (event.getRespawnReason() == RespawnReason.DEATH) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				instance.getStorageManager().getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
						instance.getConfigManager().lockData()).thenAccept((owner) -> {
							if (owner.isEmpty())
								return;
							UserData ownerUser = owner.get();
							instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get())
									.thenRun(() -> event
											.setRespawnLocation(onlineUser.get().getHellblockData().getHomeLocation()));
						});
			}
		}
	}

	@EventHandler
	public void onFallInVoid(PlayerMoveEvent event) {
		if (!instance.getConfigManager().voidTeleport())
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		final UUID id = player.getUniqueId();
		if (player.getLocation().getY() <= 0) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				instance.getStorageManager().getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
						instance.getConfigManager().lockData()).thenAccept((owner) -> {
							if (owner.isEmpty())
								return;
							UserData ownerUser = owner.get();
							instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get())
									.thenRun(() -> player.setFallDistance(0.0F));
						});
			} else {
				instance.getHellblockHandler().teleportToSpawn(player, true);
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getConfigManager().worldName()))
			return;
		if (player.getLocation() != null)
			player.getWorld().strikeLightningEffect(player.getLocation());
		if (!event.getKeepInventory()) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty())
				return;
			if (onlineUser.get().hasGlowstoneToolEffect() || onlineUser.get().hasGlowstoneArmorEffect()) {
				if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					onlineUser.get().isHoldingGlowstoneTool(false);
					onlineUser.get().isWearingGlowstoneArmor(false);
				}
			}
		}
	}

	public boolean playerInPortal(@NotNull Player player) {
		try {
			Material portal = player.getTargetBlock(null, 1).getType();
			Material standing = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
			return portal == Material.NETHER_PORTAL && (standing == Material.OBSIDIAN
					|| standing == Material.NETHER_PORTAL || standing == Material.AIR);
		} catch (IllegalStateException ex) {
			// ignored.. some weird block iterator error but can't be fixed.
			return false;
		}
	}
}