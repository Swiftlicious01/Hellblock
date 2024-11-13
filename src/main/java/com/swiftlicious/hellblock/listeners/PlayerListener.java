package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.PortalType;
import org.bukkit.Tag;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.events.worldguard.RegionEnteredEvent;
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerBedFailEnterEvent;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PlayerListener implements Listener {

	protected final HellblockPlugin instance;
	@Getter
	private final Map<UUID, SchedulerTask> cancellablePortal;
	@Getter
	private final Set<UUID> linkPortalCatcher;

	public PlayerListener(HellblockPlugin plugin) {
		instance = plugin;
		this.cancellablePortal = new HashMap<>();
		this.linkPortalCatcher = new HashSet<>();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onBedClick(PlayerBedFailEnterEvent event) {
		if (!HBConfig.disableBedExplosions)
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		Block bed = event.getBed();
		if (bed != null && Tag.BEDS.isTagged(bed.getType())) {
			if (bed.getWorld().getEnvironment() == Environment.NETHER) {
				event.setWillExplode(false);
			}
		}
	}

	@EventHandler
	public void onFireworkDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player player) {
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
				return;
			if (event.getDamager() instanceof Firework firework) {
				if (firework.getFireworkMeta().getPersistentDataContainer()
						.has(new NamespacedKey(instance, "challenge-firework"), PersistentDataType.BOOLEAN)) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onFallingBlockLandOnSoulSand(EntityDropItemEvent event) {
		if (event.getEntity() instanceof FallingBlock fallingBlock) {
			if (!fallingBlock.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
	public void onRegionEnterIfBanned(RegionEnteredEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;
		final UUID id = player.getUniqueId();
		Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
		if (onlineUser.isEmpty())
			return;
		UUID ownerUUID = event.getRegion().getOwners().getUniqueIds().stream().findFirst().orElse(null);
		if (ownerUUID == null)
			return;
		if (instance.getCoopManager().trackBannedPlayer(ownerUUID, id)) {
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				instance.getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get());
						});
			} else {
				instance.getHellblockHandler().teleportToSpawn(player, true);
			}
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>You're banned from this hellblock and aren't allowed entry!");
		}

		if (!instance.getCoopManager().checkIfVisitorIsWelcome(player, ownerUUID)) {
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				instance.getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get());
						});
			} else {
				instance.getHellblockHandler().teleportToSpawn(player, true);
			}
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>This hellblock is locked at the moment, you're not allowed entry!");
		}
	}

	@EventHandler
	public void onLeaveHellblockBorders(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;
		final UUID id = player.getUniqueId();
		if (player.getLocation() == null)
			return;
		if (HBConfig.worldguardProtected) {
			if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
				return;
			if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) == null)
				return;
			instance.getStorageManager()
					.getOfflineUserData(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player),
							HBConfig.lockData)
					.thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
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
												instance.getAdventureManager().sendMessageWithPrefix(player,
														"<red>This hellblock home location was deemed not safe, teleporting to your hellblock instead!");

												if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
													throw new NullPointerException(
															"Owner reference returned null, please report this to the developer.");
												}
												instance.getStorageManager()
														.getOfflineUserData(
																onlineUser.get().getHellblockData().getOwnerUUID(),
																HBConfig.lockData)
														.thenAccept((owner) -> {
															UserData ownerUser = owner.orElseThrow();
															instance.getCoopManager()
																	.makeHomeLocationSafe(ownerUser, onlineUser.get())
																	.thenRun(() -> instance.getAdventureManager()
																			.sendMessage(player,
																					"<red>You can't leave the hellblock borders."));
														});
											}
											return;
										}).thenRunAsync(() -> {
											ChunkUtils.teleportAsync(player,
													offlineUser.getHellblockData().getHomeLocation(),
													TeleportCause.PLUGIN);
											instance.getAdventureManager().sendMessage(player,
													"<red>You can't leave the hellblock borders.");
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
			if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
				return;
			final UUID id = player.getUniqueId();
			if (player.getLocation() == null)
				return;
			if (HBConfig.worldguardProtected) {
				if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
					return;
				if (instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player) == null)
					return;
				instance.getStorageManager()
						.getOfflineUserData(instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player),
								HBConfig.lockData)
						.thenAccept((result) -> {
							UserData offlineUser = result.orElseThrow();
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
													instance.getAdventureManager().sendMessageWithPrefix(player,
															"<red>This hellblock home location was deemed not safe, teleporting to your hellblock instead!");

													if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
														throw new NullPointerException(
																"Owner reference returned null, please report this to the developer.");
													}
													instance.getStorageManager()
															.getOfflineUserData(
																	onlineUser.get().getHellblockData().getOwnerUUID(),
																	HBConfig.lockData)
															.thenAccept((owner) -> {
																UserData ownerUser = owner.orElseThrow();
																instance.getCoopManager()
																		.makeHomeLocationSafe(ownerUser,
																				onlineUser.get())
																		.thenRun(() -> instance.getAdventureManager()
																				.sendMessage(player,
																						"<red>You can't leave the hellblock borders."));
															});
												}
												return;
											}).thenRunAsync(() -> {
												ChunkUtils.teleportAsync(player,
														offlineUser.getHellblockData().getHomeLocation(),
														TeleportCause.PLUGIN);
												instance.getAdventureManager().sendMessage(player,
														"<red>You can't leave the hellblock borders.");
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
			SchedulerTask portalTask = instance.getScheduler().sync().runLater(() -> {
				if (onlineUser.get().getHellblockData().hasHellblock()) {
					if (onlineUser.get().getHellblockData().getLinkedUUID() != null) {
						instance.getStorageManager()
								.getOfflineUserData(onlineUser.get().getHellblockData().getLinkedUUID(),
										HBConfig.lockData)
								.thenAccept((result) -> {
									UserData offlineUser = result.orElseThrow();
									if (!offlineUser.getHellblockData().hasHellblock()
											|| offlineUser.getHellblockData().isAbandoned()) {
										instance.getAdventureManager().sendMessageWithPrefix(player,
												"<red>This hellblock was reset or abandoned and can't be linked with.");
										onlineUser.get().getHellblockData().setLinkedUUID(null);
										return;
									}
									if (offlineUser.getHellblockData().isLocked()) {
										instance.getAdventureManager().sendMessageWithPrefix(player,
												"<red>This hellblock is locked at the moment for visitors.");
										return;
									}
									LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation())
											.thenAccept((tiResult) -> {
												if (!tiResult.booleanValue()) {
													instance.getAdventureManager().sendMessageWithPrefix(player,
															"<red>This hellblock home location was deemed not safe, teleporting to your hellblock instead!");

													if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
														throw new NullPointerException(
																"Owner reference returned null, please report this to the developer.");
													}
													instance.getStorageManager()
															.getOfflineUserData(
																	onlineUser.get().getHellblockData().getOwnerUUID(),
																	HBConfig.lockData)
															.thenAccept((owner) -> {
																UserData ownerUser = owner.orElseThrow();
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
										HBConfig.lockData)
								.thenAccept((owner) -> {
									UserData ownerUser = owner.orElseThrow();
									instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get());
								});
					}
				} else {
					new IslandChoiceMenu(player, false);
				}
			}, 5 * 20, onlineUser.get().getHellblockData().getHomeLocation());
			this.cancellablePortal.putIfAbsent(player.getUniqueId(), portalTask);
		}
	}

	@EventHandler
	public void onLinkPortal(PortalCreateEvent event) {
		if (!event.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
					instance.getAdventureManager().sendMessage(player,
							"<red>Link your home nether portal to another player's hellblock if you wish or keep it as your own!");
					instance.getAdventureManager().sendMessage(player,
							"<red>You can always change this just by clicking on your nether portal!");
					String owner = onlineUser.get().getHellblockData().getLinkedUUID() != null
							? (Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()) != null
									? Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()).getName()
									: Bukkit.getOfflinePlayer(onlineUser.get().getHellblockData().getLinkedUUID())
											.getName())
							: null;
					instance.getAdventureManager().sendMessage(player,
							"<red>Current Linked Hellblock: " + (owner != null ? owner : "None"));
					instance.getAdventureManager().sendMessage(player,
							"<red>Type the player's username of the hellblock you wish to link (Type none for yourself):");
					instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:block.end_portal.spawn"), 1.0F, 1.0F);
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@EventHandler
	public void onLinkPortal(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
					instance.getAdventureManager().sendMessage(player,
							"<red>Link your home nether portal to another player's hellblock if you wish or keep it as your own!");
					instance.getAdventureManager().sendMessage(player,
							"<red>You can always change this just by clicking on your nether portal!");
					String owner = onlineUser.get().getHellblockData().getLinkedUUID() != null
							? (Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()) != null
									? Bukkit.getPlayer(onlineUser.get().getHellblockData().getLinkedUUID()).getName()
									: Bukkit.getOfflinePlayer(onlineUser.get().getHellblockData().getLinkedUUID())
											.getName())
							: null;
					instance.getAdventureManager().sendMessage(player,
							"<red>Current Linked Hellblock: " + (owner != null ? owner : "None"));
					instance.getAdventureManager().sendMessage(player,
							"<red>Type the player's username of the hellblock you wish to link (Type none for yourself):");
					instance.getAdventureManager().sendSound(player, net.kyori.adventure.sound.Sound.Source.PLAYER,
							net.kyori.adventure.key.Key.key("minecraft:block.end_portal.spawn"), 1.0F, 1.0F);
					this.linkPortalCatcher.add(id);
				}
			}
		}
	}

	@EventHandler
	public void onLinkPortalChat(AsyncChatEvent event) {
		final Player player = event.getPlayer();
		final UUID id = player.getUniqueId();
		if (this.linkPortalCatcher.contains(id)) {
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(id);
			if (onlineUser.isEmpty())
				return;
			event.setCancelled(true);
			String username = PlainTextComponentSerializer.plainText().serialize(event.message());
			if (username.equalsIgnoreCase("none") || username.equalsIgnoreCase(player.getName())) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>The portal has kept it's link to your own hellblock!");
				this.linkPortalCatcher.remove(id);
				return;
			}
			Player link = Bukkit.getPlayer(username);
			if (link != null) {
				if ((onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& onlineUser.get().getHellblockData().getOwnerUUID().equals(link.getUniqueId()))
						|| onlineUser.get().getHellblockData().getParty().contains(link.getUniqueId())) {
					instance.getAdventureManager().sendMessage(player,
							"<red>The player you typed is a part of your hellblock already.");
					instance.getAdventureManager().sendMessage(player,
							"<red>The portal has kept it's link to your own hellblock!");
					this.linkPortalCatcher.remove(id);
					return;
				}
				Optional<UserData> linked = instance.getStorageManager().getOnlineUser(link.getUniqueId());
				if (linked.isEmpty())
					return;
				if (!linked.get().getHellblockData().hasHellblock() || linked.get().getHellblockData().isAbandoned()
						|| linked.get().getHellblockData().getHomeLocation() == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't have a hellblock to link to!");
					return;
				}
				if (linked.get().getHellblockData().isLocked()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>This hellblock is locked at the moment so you can't link to it!");
					return;
				}
				if (linked.get().getHellblockData().getBanned().contains(id)) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You're banned from this hellblock and can't link to it!");
					return;
				}
				onlineUser.get().getHellblockData().setLinkedUUID(link.getUniqueId());
				instance.getAdventureManager().sendMessageWithPrefix(player, String
						.format("<red>You have linked your nether portal to <dark_red>%s<red>'s hellblock!", username));
			} else {
				UUID linkID = UUIDFetcher.getUUID(username);
				if (linkID == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't exist!");
					return;
				}
				if (!Bukkit.getOfflinePlayer(linkID).hasPlayedBefore()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The player you typed in doesn't exist!");
					return;
				}
				if ((onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& onlineUser.get().getHellblockData().getOwnerUUID().equals(linkID))
						|| onlineUser.get().getHellblockData().getParty().contains(linkID)) {
					instance.getAdventureManager().sendMessage(player,
							"<red>The player you typed is a part of your hellblock already.");
					instance.getAdventureManager().sendMessage(player,
							"<red>The portal has kept it's link to your own hellblock!");
					this.linkPortalCatcher.remove(id);
					return;
				}
				instance.getStorageManager().getOfflineUserData(linkID, HBConfig.lockData).thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					if (!offlineUser.getHellblockData().hasHellblock() || offlineUser.getHellblockData().isAbandoned()
							|| offlineUser.getHellblockData().getHomeLocation() == null) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>The player you typed in doesn't have a hellblock to link to!");
						return;
					}
					if (offlineUser.getHellblockData().isLocked()) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock is locked at the moment so you can't link to it!");
						return;
					}
					if (offlineUser.getHellblockData().getBanned().contains(id)) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>You're banned from this hellblock and can't link to it!");
						return;
					}
					onlineUser.get().getHellblockData().setLinkedUUID(linkID);
					instance.getAdventureManager().sendMessageWithPrefix(player, String.format(
							"<red>You've linked your nether portal to <dark_red>%s<red>'s hellblock!", username));
				});
			}
			this.linkPortalCatcher.remove(id);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		if (event.getFrom().getName().equals(HBConfig.worldName)) {
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
			if (entity.getWorld().getEnvironment() == Environment.NETHER
					&& event.getPortalType() == PortalType.NETHER) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		if (player.getPotentialBedLocation() != null
				&& player.getPotentialBedLocation().getBlock().getType() == Material.RESPAWN_ANCHOR)
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
				instance.getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							instance.getCoopManager().makeHomeLocationSafe(ownerUser, onlineUser.get())
									.thenRun(() -> event
											.setRespawnLocation(onlineUser.get().getHellblockData().getHomeLocation()));
						});
			}
		}
	}

	@EventHandler
	public void onFallInVoid(PlayerMoveEvent event) {
		if (!HBConfig.voidTeleport)
			return;
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
				instance.getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(), HBConfig.lockData)
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
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
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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

	public boolean playerInPortal(@NonNull Player player) {
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