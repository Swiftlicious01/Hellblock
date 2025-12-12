package com.swiftlicious.hellblock.protection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Trident;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.EntityUnleashEvent.UnleashReason;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.addons.pet.PetProvider;
import com.swiftlicious.hellblock.creation.addons.shop.sign.ShopSignProvider;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.SignReflectionHelper;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.EntityTypeUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

/**
 * Manages in-game protection events for islands using the internal protection
 * system.
 * <p>
 * Handles entity interactions, block placement/breaking, item pickups, and
 * explosion tracking within island boundaries. Only active when internal
 * protection is in use.
 * <p>
 * Implements {@link Reloadable} for dynamic registration and cleanup.
 */
public class ProtectionEvents implements Listener, Reloadable {

	protected final HellblockPlugin instance;
	private final ExplosionTracker explosionTracker;
	private final CollisionManager collisionManager;

	private final Map<UUID, Integer> endermanIslandCache = new ConcurrentHashMap<>();

	private final Map<UUID, BoundingBox> hellblockCache = new ConcurrentHashMap<>();

	// Keeps track of whether the player was inside the island bounds last time
	private final Map<UUID, Boolean> playerInBoundsState = new ConcurrentHashMap<>();

	private static final String METADATA_DROPPED_BY = "hellblock:droppedBy";
	private static final String METADATA_DROP_TIME = "hellblock:dropTime";
	private static final long ITEM_PICKUP_GRACE_PERIOD_MS = 10_000;

	private Listener dynamicMountListener;

	public ProtectionEvents(HellblockPlugin plugin) {
		instance = plugin;
		this.collisionManager = new CollisionManager();
		this.explosionTracker = new ExplosionTracker(instance);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		registerMountListeners();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		unregisterMountListeners();
		this.hellblockCache.clear();
		this.playerInBoundsState.clear();
	}

	@Override
	public void disable() {
		unload();
		collisionManager.resetAll();
	}

	public void registerMountListeners() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends Event> mountEventClass = (Class<? extends Event>) Class
					.forName("org.bukkit.event.entity.EntityMountEvent");
			@SuppressWarnings("unchecked")
			Class<? extends Event> dismountEventClass = (Class<? extends Event>) Class
					.forName("org.bukkit.event.entity.EntityDismountEvent");

			Method getEntityMethod = mountEventClass.getMethod("getEntity"); // same for dismount
			Method getMountMethod = mountEventClass.getMethod("getMount");
			Method getDismountedMethod = dismountEventClass.getMethod("getDismounted");

			dynamicMountListener = new Listener() {
			}; // dummy listener for Bukkit bookkeeping

			// One executor handles both events
			EventExecutor executor = (listener, event) -> {
				try {
					if (mountEventClass.isInstance(event)) {
						Player player = asPlayer(getEntityMethod.invoke(event));
						if (player == null)
							return;

						Entity mount = (Entity) getMountMethod.invoke(event);

						// Pet logic
						for (PetProvider petProvider : instance.getIntegrationManager().getPetProviders()) {
							if (petProvider != null && petProvider.getOwnerUUID(player) != null
									&& player.getUniqueId().equals(petProvider.getOwnerUUID(player))
									&& petProvider.isPet(mount)) {
								return;
							}
						}

						if (event instanceof Cancellable cancellable) {
							denyIfNotAllowed(player, mount.getLocation(), cancellable,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(),
									HellblockFlag.FlagType.RIDE).thenAccept(denied -> {
										if (denied) {
											cancellable.setCancelled(true);

											// Fallback dismount if event cancellation doesn't prevent it
											instance.getScheduler().executeSync(() -> {
												if (mount.getPassengers().contains(player)) {
													mount.removePassenger(player);
												}
												if (player.isInsideVehicle()) {
													player.leaveVehicle();
												}
											});
										}
									});
						}

					} else if (dismountEventClass.isInstance(event)) {
						Player player = asPlayer(getEntityMethod.invoke(event));
						if (player == null)
							return;

						Location loc = ((Entity) getDismountedMethod.invoke(event)).getLocation();

						if (event instanceof Cancellable cancellable) {
							denyIfNotAllowed(player, loc, cancellable,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
									HellblockFlag.FlagType.ENTRY).thenAccept(denied -> {
										if (!denied) {
											checkFlag(player, loc, cancellable,
													MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(),
													HellblockFlag.FlagType.RIDE);
										}
									});
						}

						if (!player.hasPermission("hellblock.bypass.lock")) {
							if (event instanceof Cancellable cancellable) {
								denyIfNotAllowed(player, loc, cancellable,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
										HellblockFlag.FlagType.ENTRY);
							}
						}

						handleHellblockMessage(player);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			};

			// Register both dynamically
			Bukkit.getPluginManager().registerEvent(mountEventClass, dynamicMountListener, EventPriority.NORMAL,
					executor, instance, true);
			Bukkit.getPluginManager().registerEvent(dismountEventClass, dynamicMountListener, EventPriority.NORMAL,
					executor, instance, true);

			instance.debug("Registered dynamic EntityMount/Dismount listener.");

		} catch (ClassNotFoundException ignored) {
			// Fallback to legacy implementation
			LegacyMountListener legacy = new LegacyMountListener();
			legacy.startDismountCheckTask();
			dynamicMountListener = legacy;
			Bukkit.getPluginManager().registerEvents(dynamicMountListener, instance);
			instance.debug("Registered legacy mount listener.");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private Player asPlayer(Object entity) {
		return (entity instanceof Player p) ? p : null;
	}

	public void unregisterMountListeners() {
		if (dynamicMountListener != null) {
			HandlerList.unregisterAll(dynamicMountListener);
			if (dynamicMountListener instanceof LegacyMountListener legacy) {
				legacy.stopDismountCheckTask();
			}
			dynamicMountListener = null;
			instance.debug("Unregistered mount listener.");
		}
	}

	// Block break: BUILD master -> fallback to BLOCK_BREAK
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getBlock().getLocation();

		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_BUILD_DENY.build(),
				HellblockFlag.FlagType.BUILD).thenAccept(denied -> {
					if (!denied) {
						checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_BREAK_DENY.build(),
								HellblockFlag.FlagType.BLOCK_BREAK);
					}
				});

		if (event.getBlock().getType() == Material.TNT) {
			explosionTracker.track(event.getBlock(), player);
		}
	}

	// Block place: BUILD master -> fallback to BLOCK_PLACE
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getBlock().getLocation();

		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_BUILD_DENY.build(),
				HellblockFlag.FlagType.BUILD).thenAccept(denied -> {
					if (!denied) {
						checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PLACE_DENY.build(),
								HellblockFlag.FlagType.BLOCK_PLACE);
					}
				});
	}

	private final Map<UUID, Location> lecternEditMap = new HashMap<>();

	// Block / item USE master handling: if USE allowed -> skip specifics; if USE
	// denied -> cancel;
	// if not denied (owner didn't cancel), then check specifics (this follows your
	// existing pattern).
	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) {
			return;
		}

		final Player player = event.getPlayer();
		final Block block = event.getClickedBlock();
		final Location loc = block.getLocation();

		// Allow right-click on shop signs (ChestShop/QuickShop)
		if (isShopSign(block)) {
			return; // Don't cancel — allow interaction
		}

		// Check USE master flag first
		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
				HellblockFlag.FlagType.USE).thenAccept(denied -> {
					if (!denied) {
						final Material type = block.getType();
						if (type == Material.LECTERN) {
							lecternEditMap.put(player.getUniqueId(), loc);
						}

						// Campfire retrieval protection
						if ((type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE)
								&& event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() == null) {
							checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
									HellblockFlag.FlagType.USE);
						}

						// Beds / respawn anchors
						if (Tag.BEDS.isTagged(type) || type == Material.RESPAWN_ANCHOR) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_RESPAWN_ANCHOR_DENY.build(),
									HellblockFlag.FlagType.RESPAWN_ANCHORS);
							explosionTracker.track(block, player);
						}
						// Storage blocks: chest, barrel, trapped chest, shulker boxes
						else if (type == Material.CHEST || type == Material.TRAPPED_CHEST
								|| Tag.SHULKER_BOXES.isTagged(type) || type == Material.BARREL) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY.build(),
									HellblockFlag.FlagType.CHEST_ACCESS);
						}
						// Anvil usage
						else if (Tag.ANVIL.isTagged(type)) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_ANVIL_DENY.build(),
									HellblockFlag.FlagType.USE_ANVIL);
						}
						// Dripleaf usage
						else if (type == Material.SMALL_DRIPLEAF || type == Material.BIG_DRIPLEAF) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DRIPLEAF_DENY.build(),
									HellblockFlag.FlagType.USE_DRIPLEAF);
						}

						// Item-based checks (lighter, vehicle placement)
						final ItemStack hand = event.getItem();
						if (hand != null) {
							final Material mat = hand.getType();
							if (type.name().contains("SIGN")) {
								return;
							}

							if (mat == Material.FLINT_AND_STEEL || mat == Material.FIRE_CHARGE) {
								checkFlag(player, loc, event,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_LIGHTER_DENY.build(),
										HellblockFlag.FlagType.LIGHTER);
							}

							if (mat.name().endsWith("_SPAWN_EGG")) {
								checkFlag(player, loc, event,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_MOB_SPAWN_DENY.build(),
										HellblockFlag.FlagType.MOB_SPAWNING);
							}

							if (mat.name().endsWith("_BOAT") || mat.name().endsWith("_MINECART")
									|| mat == Material.MINECART || mat.name().endsWith("_RAFT")) {
								checkFlag(player, loc, event,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_VEHICLE_PLACE_DENY.build(),
										HellblockFlag.FlagType.PLACE_VEHICLE);
							}
						}

						// Physical trample check
						if (event.getAction() == Action.PHYSICAL && type == Material.FARMLAND) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_TRAMPLE_DENY.build(),
									HellblockFlag.FlagType.TRAMPLE_BLOCKS);
						}
					}
				});

		// Track TNT block placements
		if (block.getType() == Material.TNT) {
			explosionTracker.track(block, player);
		}

		// Detect TNT minecart use
		if (event.getMaterial() == Material.TNT_MINECART) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_EXPLOSION_DENY.build(),
					HellblockFlag.FlagType.TNT);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityPlace(EntityPlaceEvent event) {
		Player player = event.getPlayer();

		Entity placed = event.getEntity();
		Location loc = placed.getLocation();
		if (placed instanceof Boat || placed instanceof Minecart) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_VEHICLE_PLACE_DENY.build(),
					HellblockFlag.FlagType.PLACE_VEHICLE);
		}

		if (placed instanceof ArmorStand || placed instanceof EnderCrystal) {
			denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_BUILD_DENY.build(),
					HellblockFlag.FlagType.BUILD).thenAccept(denied -> {
						if (!denied) {
							checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PLACE_DENY.build(),
									HellblockFlag.FlagType.BLOCK_PLACE);
						}
					});
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Location loc = player.getLocation();

		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ITEM_DROP_DENY.build(),
				HellblockFlag.FlagType.ITEM_DROP).thenAccept(denied -> {
					if (!denied) {
						// Tag the item with dropper info
						Item droppedItem = event.getItemDrop();
						droppedItem.setMetadata(METADATA_DROPPED_BY,
								new FixedMetadataValue(instance, player.getUniqueId().toString()));
						droppedItem.setMetadata(METADATA_DROP_TIME,
								new FixedMetadataValue(instance, System.currentTimeMillis()));
					}
				});
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		Item item = event.getItem();
		Location loc = item.getLocation();

		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ITEM_PICKUP_DENY.build(),
				HellblockFlag.FlagType.ITEM_PICKUP).thenAccept(denied -> {
					if (denied)
						return;

					// Handle recently dropped item pickup protection
					if (item.hasMetadata(METADATA_DROPPED_BY) && item.hasMetadata(METADATA_DROP_TIME)) {
						String dropperUUID = item.getMetadata(METADATA_DROPPED_BY).get(0).asString();
						long dropTime = item.getMetadata(METADATA_DROP_TIME).get(0).asLong();
						long now = System.currentTimeMillis();

						if (!player.getUniqueId().toString().equals(dropperUUID)) {
							long age = now - dropTime;

							if (age < ITEM_PICKUP_GRACE_PERIOD_MS) {
								// Block pickup by anyone except original dropper
								event.setCancelled(true);

								// Optional: send action bar message
								OfflinePlayer dropper = Bukkit.getOfflinePlayer(UUID.fromString(dropperUUID));
								Component title = instance.getTranslationManager()
										.render(MessageConstants.MSG_ITEM_RECENTLY_DROPPED_PICKUP_DENY
												.arguments(AdventureHelper.miniMessageToComponent(
														dropper.hasPlayedBefore() && dropper.getName() != null
																? dropper.getName()
																: MessageConstants.FORMAT_UNKNOWN.build().key()))
												.build());
								VersionHelper.getNMSManager().sendActionBar(player,
										AdventureHelper.componentToJson(title));
								return;
							} else {
								// Grace period expired → cleanup metadata
								item.removeMetadata(METADATA_DROPPED_BY, instance);
								item.removeMetadata(METADATA_DROP_TIME, instance);
							}
						}
					}
				});
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemMerge(ItemMergeEvent event) {
		Item source = event.getEntity();
		Item target = event.getTarget();

		// If either doesn't have metadata, skip
		if (!source.hasMetadata(METADATA_DROPPED_BY) || !source.hasMetadata(METADATA_DROP_TIME)) {
			return;
		}

		// Only apply metadata if the target doesn't already have it
		if (!target.hasMetadata(METADATA_DROPPED_BY)) {
			target.setMetadata(METADATA_DROPPED_BY, source.getMetadata(METADATA_DROPPED_BY).get(0));
		}
		if (!target.hasMetadata(METADATA_DROP_TIME)) {
			target.setMetadata(METADATA_DROP_TIME, source.getMetadata(METADATA_DROP_TIME).get(0));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTame(EntityTameEvent event) {
		if (!(event.getOwner() instanceof Player player)) {
			return;
		}

		Location loc = event.getEntity().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		final Player player = event.getPlayer();
		final Block block = event.getBlock();
		final Location loc = block.getLocation();

		// Block unauthorized editing of signs (both new and existing ones)
		denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEditBook(PlayerEditBookEvent event) {
		Player player = event.getPlayer();
		Location loc = lecternEditMap.remove(player.getUniqueId()); // Optional fallback
		if (loc == null) {
			loc = player.getLocation(); // fallback
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onArmorStandEdit(PlayerArmorStandManipulateEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getRightClicked().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getRightClicked().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onShear(PlayerShearEntityEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getEntity().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLeashEntity(PlayerLeashEntityEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getEntity().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTeleport(EntityTeleportEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			Location to = event.getTo();

			// Prevent mobs from being pulled or teleported across protected areas
			checkOwnerFlag(to, HellblockFlag.FlagType.INTERACT,
					() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityPortal(EntityPortalEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			Location to = event.getTo();

			checkOwnerFlag(to, HellblockFlag.FlagType.INTERACT,
					() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityUnleash(EntityUnleashEvent event) {
		if (event.getReason() == UnleashReason.PLAYER_UNLEASH) {
			final Location loc = event.getEntity().getLocation();

			checkOwnerFlag(loc, HellblockFlag.FlagType.INTERACT, () -> instance.getScheduler().executeSync(() -> {
				if (event instanceof Cancellable cancellable) {
					cancellable.setCancelled(true);
				} // else: event cannot be cancelled on this platform
			}));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getEntity().getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent event) {
		if (!(event instanceof HangingBreakByEntityEvent entityBreak)) {
			return; // Not broken by entity (e.g., physics, explosion, etc.)
		}

		if (!(entityBreak.getRemover() instanceof Player player)) {
			return;
		}

		final Location loc = event.getEntity().getLocation();

		if (entityBreak.getEntity() instanceof ItemFrame) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ITEM_FRAME_DESTROY_DENY.build(),
					HellblockFlag.FlagType.ENTITY_ITEM_FRAME_DESTROY);
			return;
		}

		if (entityBreak.getEntity() instanceof Painting) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PAINTING_DESTROY_DENY.build(),
					HellblockFlag.FlagType.ENTITY_PAINTING_DESTROY);
			return;
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	// Entity interaction master (INTERACT) — covers entity interactions (villagers,
	// frames, mounts etc.)
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getRightClicked().getLocation();
		if (event.getRightClicked().hasMetadata("NPC")) {
			return;
		}

		Set<PetProvider> petProviders = instance.getIntegrationManager().getPetProviders();
		for (PetProvider petProvider : petProviders) {
			if (petProvider != null && petProvider.getOwnerUUID(player) != null
					&& petProvider.getOwnerUUID(player).equals(player.getUniqueId())
					&& petProvider.isPet(event.getRightClicked()))
				return;
		}

		// Item frame rotation / editing
		if (event.getRightClicked() instanceof ItemFrame) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ITEM_FRAME_ROTATE_DENY.build(),
					HellblockFlag.FlagType.ITEM_FRAME_ROTATE);
			return;
		}

		if (event.getRightClicked() instanceof InventoryHolder holder && !(holder instanceof Player)) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
				HellblockFlag.FlagType.USE);

		// Vehicle mount attempt is handled below via mount/dismount events
	}

	@EventHandler(ignoreCancelled = true)
	public void onEquipEntityWithArmor(PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked().hasMetadata("NPC")) {
			return;
		}

		Set<PetProvider> petProviders = instance.getIntegrationManager().getPetProviders();
		for (PetProvider petProvider : petProviders) {
			if (petProvider != null && petProvider.getOwnerUUID(event.getPlayer()) != null
					&& petProvider.getOwnerUUID(event.getPlayer()).equals(event.getPlayer().getUniqueId())
					&& petProvider.isPet(event.getRightClicked()))
				return;
		}

		if (event.getRightClicked() instanceof LivingEntity living && living.getEquipment() != null) {
			// Prevent right-click armor equip (horse, armor stand)
			final Player player = event.getPlayer();
			final Location loc = living.getLocation();

			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
					HellblockFlag.FlagType.INTERACT);
		}
	}

	// Bucket events (treated as interaction)
	@EventHandler(ignoreCancelled = true)
	public void onBucketFill(PlayerBucketFillEvent event) {
		checkFlag(event.getPlayer(), event.getBlock().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_BUCKET_FILL_DENY.build(), HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		checkFlag(event.getPlayer(), event.getBlock().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_BUCKET_EMPTY_DENY.build(), HellblockFlag.FlagType.INTERACT);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBucketEntity(PlayerBucketEntityEvent event) {
		checkFlag(event.getPlayer(), event.getEntity().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(), HellblockFlag.FlagType.INTERACT);
	}

	// Inventory open -> chest access
	@EventHandler(ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		final HumanEntity human = event.getPlayer();
		if (!(human instanceof Player player)) {
			return;
		}

		Inventory topInv = event.getView().getTopInventory();
		InventoryHolder holder = topInv.getHolder();

		// Only proceed if the top inventory is a physical block container
		if (!(holder instanceof BlockState blockState)) {
			// This is likely a virtual inventory (plugin GUI), skip protection
			return;
		}

		Location loc = blockState.getLocation();

		final InventoryType type = event.getView().getTopInventory().getType();

		switch (type) {
		case CHEST, BARREL, ENDER_CHEST, SHULKER_BOX:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY.build(),
					HellblockFlag.FlagType.CHEST_ACCESS);
			break;

		case FURNACE, BLAST_FURNACE, SMOKER:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
			break;

		case BREWING:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
			break;

		case HOPPER, DROPPER, DISPENSER:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
			break;

		case ANVIL, SMITHING:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_ANVIL_DENY.build(),
					HellblockFlag.FlagType.USE_ANVIL);
			break;

		case ENCHANTING:
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
			break;

		default:
			// Fallback: general USE for any other inventories
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
			break;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onHorseInventoryOpen(InventoryOpenEvent event) {
		// Already implemented, just ensure it handles entity holders like horses
		InventoryHolder holder = event.getInventory().getHolder();
		if (holder == null)
			return;

		if (holder instanceof AbstractHorse || holder instanceof Llama) {
			checkFlag((Player) event.getPlayer(), event.getPlayer().getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(), HellblockFlag.FlagType.USE);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		Inventory clickedInventory = event.getClickedInventory();
		if (clickedInventory == null)
			return;

		InventoryHolder holder = clickedInventory.getHolder();
		if (!(holder instanceof BlockState blockState))
			return;

		Location loc = blockState.getLocation();

		InventoryType type = event.getView().getTopInventory().getType();
		if (type == InventoryType.BEACON) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_USE_DENY.build(),
					HellblockFlag.FlagType.USE);
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY.build(),
				HellblockFlag.FlagType.CHEST_ACCESS);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		Inventory inventory = event.getInventory();
		InventoryHolder holder = inventory.getHolder();
		if (!(holder instanceof BlockState blockState))
			return;

		Location loc = blockState.getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY.build(),
				HellblockFlag.FlagType.CHEST_ACCESS);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDispenseArmor(BlockDispenseArmorEvent event) {
		Block block = event.getBlock();
		Location loc = block.getLocation();

		checkOwnerFlag(loc, HellblockFlag.FlagType.INTERACT,
				() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		final Entity damagerEntity = event.getDamager();
		final Entity victim = event.getEntity();
		final Location loc = victim.getLocation();

		// Firework damage case
		if (damagerEntity instanceof Firework firework && firework.getShooter() != null
				&& firework.getShooter() instanceof Player shooter) {
			checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_FIREWORK_DAMAGE_DENY.build(),
					HellblockFlag.FlagType.FIREWORK_DAMAGE);
			return;
		}

		// Only handle player-caused direct melee damage below
		if (!(damagerEntity instanceof Player damager)) {
			return;
		}

		if (victim instanceof Player) {
			checkFlag(damager, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PVP_DENY.build(),
					HellblockFlag.FlagType.PVP);
			return;
		}

		if (victim instanceof Animals) {
			checkFlag(damager, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ANIMAL_DAMAGE_DENY.build(),
					HellblockFlag.FlagType.DAMAGE_ANIMALS);
			return;
		}

		// Generic mob damage
		checkFlag(damager, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_MOB_DAMAGE_DENY.build(),
				HellblockFlag.FlagType.MOB_DAMAGE);
	}

	// Projectile hit: enforce PVP for players and generic interact for others
	@EventHandler(ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event) {
		final Projectile proj = event.getEntity();
		final ProjectileSource source = proj.getShooter();
		if (source == null) {
			return;
		}
		if (!(source instanceof Player shooter)) {
			return;
		}
		if (event.getHitBlock() == null && event.getHitEntity() == null) {
			return;
		}

		final Location loc = (event.getHitBlock() != null) ? event.getHitBlock().getLocation()
				: event.getHitEntity().getLocation();

		if (proj instanceof Trident && event.getHitEntity() instanceof Player) {
			checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
					HellblockFlag.FlagType.PVP);
		} else {
			if (event.getHitEntity() instanceof Player) {
				checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
						HellblockFlag.FlagType.PVP);
			} else {
				// Expanded: deny if projectile hits armor stand, item frame, etc.
				if (event.getHitEntity() instanceof ArmorStand || event.getHitEntity() instanceof ItemFrame
						|| event.getHitEntity() instanceof Painting) {
					checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
							HellblockFlag.FlagType.INTERACT);
				} else {
					checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
							HellblockFlag.FlagType.INTERACT);
				}
			}
		}
	}

	// Explosion handling using ExplosionTracker to find responsible player where
	// possible

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		final Entity exploder = event.getEntity();
		final List<Block> blocks = new ArrayList<>(event.blockList());

		if (exploder instanceof Player player) {
			CompletableFuture.allOf(blocks.stream().map(
					block -> canInteract(player, block.getLocation(), HellblockFlag.FlagType.TNT).thenAccept(can -> {
						if (!can && !bypass(player)) {
							instance.getScheduler().executeSync(() -> event.blockList().remove(block));
						}
					})).toArray(CompletableFuture[]::new));
			return;
		}

		if (exploder instanceof ExplosiveMinecart tntMinecart) {
			final Player owner = explosionTracker.getResponsiblePlayer(tntMinecart);
			if (owner != null) {
				blocks.forEach(
						block -> canInteract(owner, block.getLocation(), HellblockFlag.FlagType.TNT).thenAccept(can -> {
							if (!can && !bypass(owner)) {
								instance.getScheduler().executeSync(() -> event.blockList().remove(block));
							}
						}));
				return;
			}

			// No owner -> fallback to check island owner
			blocks.forEach(block -> checkOwnerFlag(block.getLocation(), HellblockFlag.FlagType.TNT,
					() -> instance.getScheduler().executeSync(() -> event.blockList().remove(block))));
			return;
		}

		if (exploder instanceof Creeper) {
			blocks.forEach(block -> checkOwnerFlag(block.getLocation(), HellblockFlag.FlagType.CREEPER_EXPLOSION,
					() -> instance.getScheduler().executeSync(() -> event.blockList().remove(block))));
			return;
		}

		// Catch other entities (e.g., primed TNT not player-owned, or unknown sources)
		event.blockList().clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		final Player sourcePlayer = explosionTracker.getResponsiblePlayer(event.getBlock());
		final List<Block> blocks = new ArrayList<>(event.blockList());

		if (sourcePlayer == null) {
			blocks.forEach(block -> instance.getCoopManager().getHellblockOwnerOfBlock(block).thenAccept(island -> {
				if (island != null) {
					instance.getScheduler().executeSync(() -> event.blockList().remove(block));
				}
			}));
			return;
		}

		for (Block block : blocks) {
			instance.getCoopManager().getHellblockOwnerOfBlock(block).thenCompose(owner -> {
				if (owner == null) {
					return CompletableFuture.completedFuture(true);
				}
				return canInteract(sourcePlayer, block.getLocation(), HellblockFlag.FlagType.TNT);
			}).thenAccept(can -> {
				if (!can && !bypass(sourcePlayer)) {
					instance.getScheduler().executeSync(() -> event.blockList().remove(block));
				}
			});
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onTntMinecartSpawn(EntitySpawnEvent event) {
		if (event.getEntity().getType() != EntityTypeUtils.getCompatibleEntityType("TNT_MINECART", "MINECART_TNT"))
			return;

		final Player player = event.getEntity().getWorld()
				.getNearbyEntities(event.getLocation(), 1.5, 1.5, 1.5, e -> e instanceof Player).stream()
				.map(e -> (Player) e).findFirst().orElse(null);

		if (player != null) {
			explosionTracker.track(event.getEntity(), player);
		}
	}

	// Track primed TNT owner mapping
	@EventHandler(ignoreCancelled = true)
	public void onTntPrime(EntitySpawnEvent event) {
		if (!(event.getEntity() instanceof TNTPrimed tnt)) {
			return;
		}
		final Block block = tnt.getLocation().getBlock();
		final Player owner = explosionTracker.getResponsiblePlayer(block);
		if (owner != null) {
			explosionTracker.track(block, owner);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onTNTExplode(EntityExplodeEvent event) {
		if (!(event.getEntity() instanceof TNTPrimed tnt)) {
			return;
		}
		final Block sourceBlock = tnt.getLocation().getBlock();
		final Player owner = explosionTracker.getResponsiblePlayer(sourceBlock);
		if (owner == null) {
			return;
		}
		event.blockList().stream().filter(b -> b.getType() == Material.TNT)
				.forEach(b -> explosionTracker.track(b, owner));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.getBlock().getType() == Material.TNT && event.getPlayer() != null) {
			explosionTracker.track(event.getBlock(), event.getPlayer());
		}
		if (event.getPlayer() != null) {
			checkFlag(event.getPlayer(), event.getBlock().getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_LIGHTER_DENY.build(), HellblockFlag.FlagType.LIGHTER);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		final Location loc = event.getLocation();
		final EntityType type = event.getEntityType();
		final CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

		Set<PetProvider> petProviders = instance.getIntegrationManager().getPetProviders();
		for (PetProvider petProvider : petProviders) {
			if (petProvider != null && petProvider.isPet(event.getEntity()))
				return;
		}

		instance.getCoopManager().getHellblockOwnerOfBlock(loc.getBlock()).thenAccept(ownerUUID -> {
			if (ownerUUID == null)
				return;

			// Wither build: always deny (and message nearby players)
			if (type == EntityType.WITHER && reason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
				instance.getScheduler().executeSync(() -> event.setCancelled(true));
				loc.getWorld().getNearbyEntities(loc, 5, 5, 5, e -> e instanceof Player).forEach(e -> {
					Player p = (Player) e;
					canInteract(p, loc, HellblockFlag.FlagType.MOB_SPAWNING).thenAccept(can -> {
						if (!can && !bypass(p)) {
							instance.getSenderFactory().wrap(p).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_PROTECTION_WITHER_SPAWN_DENY.build()));
						}
					});
				});
				return;
			}

			// Check MOB_SPAWNING flag
			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty())
							return;

						final UserData ownerData = result.get();
						final HellblockFlag.AccessType val = ownerData.getHellblockData()
								.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING);

						if (val == HellblockFlag.AccessType.DENY) {
							instance.getScheduler().executeSync(() -> event.setCancelled(true));

							// Only message the player who used the egg if available
							if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
								loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5, e -> e instanceof Player).stream()
										.map(e -> (Player) e).filter(p -> !bypass(p)).findFirst()
										.ifPresent(p -> instance.getSenderFactory().wrap(p)
												.sendMessage(instance.getTranslationManager()
														.render(MessageConstants.MSG_HELLBLOCK_PROTECTION_MOB_SPAWN_DENY
																.build())));
							} else if (isPlayerCausedSpawn(reason)) {
								sendNearbyMessage(loc,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_MOB_SPAWN_DENY.build());
							}
						}
					});
		});
	}

	private boolean isPlayerCausedSpawn(CreatureSpawnEvent.SpawnReason reason) {
		return switch (reason) {
		case DISPENSE_EGG, BUILD_IRONGOLEM, BUILD_SNOWMAN, BREEDING, CURED, INFECTION -> true;
		default -> false;
		};
	}

	private void sendNearbyMessage(Location loc, Component message) {
		loc.getWorld().getNearbyEntities(loc, 5, 5, 5, e -> e instanceof Player).stream().map(e -> (Player) e)
				.forEach(p -> canInteract(p, loc, HellblockFlag.FlagType.MOB_SPAWNING).thenAccept(can -> {
					if (!can && !bypass(p)) {
						instance.getSenderFactory().wrap(p)
								.sendMessage(instance.getTranslationManager().render(message));
					}
				}));
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof Player target))
			return;

		Location loc = target.getLocation();
		checkFlag(target, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_MOB_DAMAGE_DENY.build(),
				HellblockFlag.FlagType.MOB_DAMAGE);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		final Entity entity = event.getEntity();
		final EntityType type = event.getEntityType();
		final Location loc = event.getBlock().getLocation();

		Map<EntityType, HellblockFlag.FlagType> flagMap = new HashMap<>();
		flagMap.put(EntityTypeUtils.getCompatibleEntityType("SNOW_GOLEM", "SNOWMAN"),
				HellblockFlag.FlagType.SNOWMAN_TRAILS);
		flagMap.put(EntityType.ENDERMAN, HellblockFlag.FlagType.ENDER_BUILD);
		flagMap.put(EntityType.RAVAGER, HellblockFlag.FlagType.RAVAGER_RAVAGE);

		HellblockFlag.FlagType flag = flagMap.getOrDefault(type, null);

		if (flag == null)
			return;

		// Special handling for Enderman placement rules
		if (type == EntityType.ENDERMAN) {
			Integer islandId = instance.getPlacementDetector().getIslandIdAt(loc);

			if (event.getTo() == Material.AIR) {
				// Picking up block → record the island
				if (islandId != null) {
					endermanIslandCache.put(entity.getUniqueId(), islandId);
				}
			} else {
				// Placing block → ensure same island
				Integer cachedIslandId = endermanIslandCache.get(entity.getUniqueId());
				if (cachedIslandId != null && islandId != null && !cachedIslandId.equals(islandId)) {
					instance.getScheduler().executeSync(() -> event.setCancelled(true));
					instance.debug("Enderman prevented from placing block in islandID=" + islandId
							+ " across island boundaries that originally belonged to islandID=" + cachedIslandId + ".");
					return;
				}
			}
		}

		checkOwnerFlag(loc, flag, () -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
	}

	@EventHandler
	public void onEndermanDeath(EntityDeathEvent event) {
		final Entity entity = event.getEntity();
		if (entity instanceof Enderman enderman && endermanIslandCache.containsKey(enderman.getUniqueId())) {
			endermanIslandCache.remove(enderman.getUniqueId());
		}
	}

	// Handles all experience gain sources controlled by EXP_DROPS flag
	@EventHandler(ignoreCancelled = true)
	public void onExpDrops(EntityDeathEvent event) {
		final Location loc = event.getEntity().getLocation();

		checkOwnerFlag(loc, HellblockFlag.FlagType.EXP_DROPS,
				() -> instance.getScheduler().executeSync(() -> event.setDroppedExp(0)));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExp(BlockExpEvent event) {
		final Location loc = event.getBlock().getLocation();

		checkOwnerFlag(loc, HellblockFlag.FlagType.EXP_DROPS,
				() -> instance.getScheduler().executeSync(() -> event.setExpToDrop(0)));
	}

	@EventHandler(ignoreCancelled = true)
	public void onFishExp(PlayerFishEvent event) {
		if (event.getExpToDrop() <= 0)
			return; // no exp reward

		final Player player = event.getPlayer();
		final Location loc = player.getLocation();

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_EXP_DROP_DENY.build(),
				HellblockFlag.FlagType.EXP_DROPS);
	}

	// Ghast fireball impact -> owner flag
	@EventHandler(ignoreCancelled = true)
	public void onGhastFireball(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof Fireball fb)) {
			return;
		}
		if (fb.getShooter() == null) {
			return;
		}
		if (!(fb.getShooter() instanceof Ghast)) {
			return;
		}

		final Location loc = event.getHitBlock() != null ? event.getHitBlock().getLocation() : fb.getLocation();
		checkOwnerFlag(loc, HellblockFlag.FlagType.GHAST_FIREBALL,
				() -> instance.getScheduler().executeSync(fb::remove));
	}

	// Fall damage -> flag
	@EventHandler(ignoreCancelled = true)
	public void onFallDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
			return;
		}

		checkFlag(player, player.getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_FALL_DAMAGE_DENY.build(), HellblockFlag.FlagType.FALL_DAMAGE);
	}

	// Natural health regen -> flag
	@EventHandler(ignoreCancelled = true)
	public void onNaturalRegen(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		checkFlag(player, player.getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_HEALTH_REGEN_DENY.build(),
				HellblockFlag.FlagType.HEALTH_REGEN);
	}

	// Hunger drain -> flag
	@EventHandler(ignoreCancelled = true)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		checkFlag(player, player.getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_HUNGER_DRAIN_DENY.build(),
				HellblockFlag.FlagType.HUNGER_DRAIN);
	}

	// Invincibility -> cancel damage for players if owner's flag denies damage
	@EventHandler(ignoreCancelled = true)
	public void onInvincibility(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		checkFlag(player, player.getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_INVINCIBLE_DENY.build(),
				HellblockFlag.FlagType.INVINCIBILITY);
	}

	// Wind charge burst (1.20+) -> owner flag
	@EventHandler(ignoreCancelled = true)
	public void onWindChargeBurst(ProjectileHitEvent event) {
		if (!VersionHelper.isVersionNewerThan1_20_6()) {
			return;
		}

		try {
			if (event.getEntityType() != EntityType.valueOf("BREEZE_WIND_CHARGE"))
				return;
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			return;
		}

		if (event.getHitBlock() != null) {
			checkOwnerFlag(event.getHitBlock().getLocation(), HellblockFlag.FlagType.WIND_CHARGE_BURST,
					() -> instance.getScheduler().executeSync(() -> event.getEntity().remove()));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBreezeWindChargeExplode(ExplosionPrimeEvent event) {
		if (!VersionHelper.isVersionNewerThan1_20_6()) {
			return;
		}

		try {
			if (event.getEntityType() != EntityType.valueOf("WIND_CHARGE"))
				return;
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			return;
		}

		final Location loc = event.getEntity().getLocation();

		checkOwnerFlag(loc, HellblockFlag.FlagType.BREEZE_WIND_CHARGE,
				() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBreezeWindChargeBlockExplosion(EntityExplodeEvent event) {
		if (!VersionHelper.isVersionNewerThan1_20_6()) {
			return;
		}

		try {
			if (event.getEntityType() != EntityType.valueOf("WIND_CHARGE"))
				return;
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			return;
		}

		final Location loc = event.getEntity().getLocation();

		checkOwnerFlag(loc, HellblockFlag.FlagType.BREEZE_WIND_CHARGE, () -> event.blockList().clear());
	}

	// Potion splash -> flag
	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent event) {
		final ThrownPotion potion = event.getEntity();
		final ProjectileSource source = potion.getShooter();
		if (source == null) {
			return;
		}
		if (!(source instanceof Player thrower)) {
			return;
		}

		event.getAffectedEntities()
				.forEach(affected -> checkFlag(thrower, affected.getLocation(), event,
						MessageConstants.MSG_HELLBLOCK_PROTECTION_POTION_SPLASH_DENY.build(),
						HellblockFlag.FlagType.POTION_SPLASH));
	}

	// Sleep (bed enter)
	@EventHandler(ignoreCancelled = true)
	public void onPlayerBedEnter(PlayerBedEnterEvent event) {
		checkFlag(event.getPlayer(), event.getBed().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_SLEEP_DENY.build(), HellblockFlag.FlagType.SLEEP);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMoveAndTeleport(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		// Movement between blocks
		if (movedWithinBlock(event)) {
			return;
		}
		if (!player.hasPermission("hellblock.bypass.lock")) {
			denyIfNotAllowed(player, event.getTo(), event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
					HellblockFlag.FlagType.ENTRY);
		}

		handleHellblockMessage(player);
	}

	@SuppressWarnings("removal")
	@EventHandler(ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		// Global ENTRY flag
		if (!player.hasPermission("hellblock.bypass.lock")) {
			denyIfNotAllowed(player, event.getTo(), event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
					HellblockFlag.FlagType.ENTRY);
		}

		// Specific teleport causes
		TeleportCause cause = event.getCause();
		if (cause == TeleportCause.ENDER_PEARL) {
			checkFlag(player, event.getTo(), event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ENDERPEARL_DENY.build(),
					HellblockFlag.FlagType.ENDERPEARL);
		} else if (cause == TeleportCause.CHORUS_FRUIT) {
			checkFlag(player, event.getTo(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_CHORUS_TELEPORT_DENY.build(),
					HellblockFlag.FlagType.CHORUS_TELEPORT);
		}

		handleHellblockMessage(player);
	}

	// Vehicle destroy -> DESTROY_VEHICLE
	@EventHandler(ignoreCancelled = true)
	public void onVehicleDestroy(VehicleDestroyEvent event) {
		if (event.getAttacker() instanceof Player player) {
			checkFlag(player, event.getVehicle().getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_VEHICLE_DESTROY_DENY.build(),
					HellblockFlag.FlagType.DESTROY_VEHICLE);
		}
	}

	// Vehicle enter -> PLACE_VEHICLE (placing via item covered in interact)
	@EventHandler(ignoreCancelled = true)
	public void onVehicleCreate(VehicleCreateEvent event) {
		// No player context here — skip; placement via item is handled in onInteract
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicleEnter(VehicleEnterEvent event) {
		if (!(event.getEntered() instanceof Player player))
			return;

		if (!player.hasPermission("hellblock.bypass.lock")) {
			Location loc = event.getVehicle().getLocation();
			denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
					HellblockFlag.FlagType.ENTRY);
		}

		handleHellblockMessage(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (!player.hasPermission("hellblock.bypass.lock")) {
			String msg = event.getMessage().toLowerCase(Locale.ROOT);
			if (msg.startsWith("/home") || msg.startsWith("/warp") || msg.startsWith("/tp")
					|| msg.startsWith("/visit")) {
				// Predict destination if possible (e.g., via external APIs)
				// Or deny preemptively if ENTRY is not allowed
				denyIfNotAllowed(player, player.getLocation(), event,
						MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(), HellblockFlag.FlagType.ENTRY);
			}
		}

		handleHellblockMessage(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player))
			return;

		if (movedWithinBlock(event))
			return;

		Location to = event.getTo();
		instance.getCoopManager().getHellblockOwnerOfBlock(to.getBlock()).thenAccept(ownerUUID -> {
			boolean shouldCollide;

			if (ownerUUID == null) {
				// Not on any island → always allow collision
				shouldCollide = true;
			} else if (ownerUUID.equals(player.getUniqueId())) {
				// Player is owner
				shouldCollide = true;
			} else if (instance.getCoopManager().isIslandMember(ownerUUID, player.getUniqueId())) {
				// Player is trusted or in the same party
				shouldCollide = true;
			} else {
				// Visitor — no collision
				shouldCollide = false;
			}

			boolean currentlyNonCollidable = collisionManager.isNonCollidable(player);

			if (!shouldCollide && !currentlyNonCollidable) {
				instance.getScheduler().executeSync(() -> collisionManager.setNonCollidable(player, true));
			} else if (shouldCollide && currentlyNonCollidable) {
				instance.getScheduler().executeSync(() -> collisionManager.setNonCollidable(player, false));
			}
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onLiquidFlow(BlockFromToEvent event) {
		Block from = event.getBlock();
		Block to = event.getToBlock();

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		BlockFace dir = event.getDirection();

		event.getBlocks().forEach(moved -> {
			Block to = moved.getRelative(dir);
			cancelIfLeavingIsland(moved, to, event);
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (!event.isSticky())
			return;

		BlockFace dir = event.getDirection();

		event.getBlocks().forEach(moved -> {
			Block to = moved.getRelative(dir.getOppositeFace());
			cancelIfLeavingIsland(moved, to, event);
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDispense(BlockDispenseEvent event) {
		Block from = event.getBlock();
		if (!(from.getBlockData() instanceof Directional directional)) {
			return;
		}

		BlockFace facing = directional.getFacing();
		Block to = from.getRelative(facing);

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		Block to = event.getBlock();
		Block from = to.getRelative(BlockFace.DOWN);

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onChangeBlock(EntityChangeBlockEvent event) {
		Block to = event.getBlock();
		Block from = to.getRelative(BlockFace.UP); // falling blocks come from above

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockGrow(BlockGrowEvent event) {
		Block to = event.getBlock();
		Block from = getGrowthSource(to); // Assumes block below is source

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		Block from = event.getSource();
		Block to = event.getBlock();

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onLivingEntityExplode(EntityExplodeEvent event) {
		Block origin = event.getLocation().getBlock();
		List<Block> toRemove = new ArrayList<>();

		event.blockList().forEach(block -> {
			instance.getCoopManager().getHellblockOwnerOfBlock(origin).thenCombine(
					instance.getCoopManager().getHellblockOwnerOfBlock(block), (originOwner, blockOwner) -> {
						if (originOwner != null && blockOwner != null && !originOwner.equals(blockOwner)) {
							toRemove.add(block);
						}
						return null;
					}).thenRun(() -> instance.getScheduler().executeSync(() -> event.blockList().removeAll(toRemove)));
		});
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityBlockForm(EntityBlockFormEvent event) {
		Block to = event.getBlock();
		Block from = to.getRelative(BlockFace.DOWN);

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onStructureGrow(StructureGrowEvent event) {
		final Block origin = event.getLocation().getBlock();

		instance.getCoopManager().getHellblockOwnerOfBlock(origin).thenAccept(originOwner -> {
			if (originOwner == null)
				return;

			// Fetch all future owner checks asynchronously
			List<BlockState> blockStates = event.getBlocks();
			List<CompletableFuture<Boolean>> checks = new ArrayList<>();
			List<BlockState> allowedStates = Collections.synchronizedList(new ArrayList<>());

			blockStates.forEach(state -> {
				Block to = state.getBlock();
				CompletableFuture<Boolean> future = instance.getCoopManager().getHellblockOwnerOfBlock(to)
						.thenApply(originOwner::equals);
				future.thenAccept(allowed -> {
					if (allowed)
						allowedStates.add(state);
				});
				checks.add(future);
			});

			// Once all futures complete, update the event
			CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
					.thenRun(() -> instance.getScheduler().executeSync(() -> {
						if (!event.isCancelled()) {
							event.getBlocks().clear();
							event.getBlocks().addAll(allowedStates);
						}
					}));
		});
	}

	private Block getGrowthSource(Block to) {
		Material type = to.getType();

		// Handle based on known growth behaviors
		return switch (type) {
		// Vertical “stacking” growths (grow upwards)
		case CACTUS, SUGAR_CANE, KELP, BAMBOO, CHORUS_PLANT -> to.getRelative(BlockFace.DOWN);

		// Horizontal spreads (vines, lichen)
		case VINE, GLOW_LICHEN -> {
			// Use any attached face if possible
			for (BlockFace face : BlockFace.values()) {
				if (to.getRelative(face).getType() == type)
					yield to.getRelative(face);
			}
			yield to; // fallback
		}

		// Mushroom and grass-type spreads (handled in BlockSpreadEvent anyway)
		case RED_MUSHROOM, BROWN_MUSHROOM, GRASS_BLOCK, MYCELIUM -> to;

		// Everything else grows in-place (crop maturity, etc.)
		default -> to;
		};
	}

	private void cancelIfLeavingIsland(Block from, Block to, Cancellable event) {
		// ---- Basic sanity checks ----
		if (from == null || to == null)
			return;
		World worldFrom = from.getWorld();
		World worldTo = to.getWorld();
		if (worldFrom == null || worldTo == null || !worldFrom.equals(worldTo))
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(worldFrom))
			return;

		// ---- Fast synchronous pre-check (cached ownership or island ID) ----
		try {
			// Attempt to resolve cached island IDs (cheap synchronous check)
			Integer fromIslandId = instance.getPlacementDetector().getIslandIdAt(from.getLocation());
			Integer toIslandId = instance.getPlacementDetector().getIslandIdAt(to.getLocation());

			if (fromIslandId != null && toIslandId != null && !fromIslandId.equals(toIslandId)) {
				instance.debug("Immediate cancel (cached check) for " + event.getClass().getSimpleName() + " from="
						+ blockInfo(from) + ", to=" + blockInfo(to));
				event.setCancelled(true);
				return; // Already determined; no need for async resolution
			}
		} catch (Exception e) {
			instance.getPluginLogger().warn("Fast island check failed: " + e.getMessage(), e);
		}

		// ---- Async ownership resolution fallback ----
		CompletableFuture<UUID> fromFuture = withTimeout(instance.getCoopManager().getHellblockOwnerOfBlock(from), 3,
				TimeUnit.SECONDS);
		CompletableFuture<UUID> toFuture = withTimeout(instance.getCoopManager().getHellblockOwnerOfBlock(to), 3,
				TimeUnit.SECONDS);

		fromFuture.thenCombine(toFuture, (fromOwner, toOwner) -> {
			// Only cancel if both are known and belong to different owners
			if (fromOwner != null && toOwner != null && !fromOwner.equals(toOwner)) {
				instance.debug("Async cancel " + event.getClass().getSimpleName() + " due to island boundary: from="
						+ blockInfo(from) + ", to=" + blockInfo(to));
				instance.getScheduler().executeSync(() -> event.setCancelled(true));
			}
			return null;
		}).exceptionally(ex -> {
			if (ex instanceof CancellationException) {
				instance.debug("cancelIfLeavingIsland timed out normally (future aborted).");
			} else {
				instance.getPluginLogger().warn("cancelIfLeavingIsland failed: " + ex.getMessage(), ex);
			}
			return null;
		});
	}

	private <T> CompletableFuture<T> withTimeout(CompletableFuture<T> original, long timeout, TimeUnit unit) {
		CompletableFuture<T> timeoutFuture = new CompletableFuture<>();

		// Schedule an async timeout completion — use async scheduler, not sync.
		long delay = unit.toSeconds(timeout);
		instance.getScheduler().asyncLater(() -> {
			if (!timeoutFuture.isDone()) {
				timeoutFuture.completeExceptionally(new TimeoutException(
						"Owner lookup timed out after " + timeout + " " + unit.toString().toLowerCase()));
			}
		}, delay, TimeUnit.SECONDS);

		// Return whichever completes first: original or timeout
		return original.applyToEither(timeoutFuture, Function.identity()).exceptionally(ex -> {
			if (ex instanceof TimeoutException) {
				instance.debug("withTimeout: async lookup exceeded " + timeout + " " + unit);
			} else if (!(ex instanceof CancellationException)) {
				instance.getPluginLogger().warn("withTimeout: async failure - " + ex.getMessage(), ex);
			}
			return null;
		});
	}

	private String blockInfo(Block block) {
		Location loc = block.getLocation();
		return "[world=" + loc.getWorld().getName() + ", x=" + loc.getBlockX() + ", y=" + loc.getBlockY() + ", z="
				+ loc.getBlockZ() + "]";
	}

	private boolean bypass(Player player) {
		return player != null && (player.hasPermission("hellblock.bypass.interact")
				|| player.hasPermission("hellblock.admin") || player.isOp());
	}

	/**
	 * Checks owner-based flag and runs the runnable on main thread if owner denies
	 * that flag. Used for non-player-caused events where a Player isn't present
	 * (snowman trails, enderman grief, etc.).
	 */
	private void checkOwnerFlag(Location loc, HellblockFlag.FlagType flag, Runnable onDenied) {
		instance.getCoopManager().getHellblockOwnerOfBlock(loc.getBlock()).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}
			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}
						final UserData ownerData = result.get();
						final HellblockFlag.AccessType value = ownerData.getHellblockData().getProtectionValue(flag);
						if (value == HellblockFlag.AccessType.DENY) {
							instance.getScheduler().executeSync(onDenied::run);
						}
					});
		});
	}

	private CompletableFuture<Boolean> canInteract(Player player, Location location, HellblockFlag.FlagType flag) {
		// Always check against the island owner's flag settings
		if (location.getWorld() == null || !instance.getHellblockHandler().isInCorrectWorld(location.getWorld())) {
			return CompletableFuture.completedFuture(false);
		}

		return instance.getCoopManager().getHellblockOwnerOfBlock(location.getBlock()).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				instance.debug("Protection: Skipped non-island location at " + location.getWorld().getName() + " ("
						+ location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ") by "
						+ (player != null ? player.getName() : "unknown"));
				return CompletableFuture.completedFuture(false);
			}

			return instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenApply(result -> {
						if (result.isEmpty()) {
							return false;
						}
						final UserData data = result.get();
						final HellblockData hellblockData = data.getHellblockData();

						// Handle abandoned island flag rules
						if (hellblockData.isAbandoned()) {
							return switch (flag) {
							case PVP, ENTRY, BUILD -> true; // Always allow
							case MOB_SPAWNING -> false; // Always deny
							default -> false; // Deny all other flags
							};
						}

						// Co-op/owner access OR explicit ALLOW by owner
						return player != null && (hellblockData.canAccess(player.getUniqueId())
								|| hellblockData.getProtectionValue(flag) == HellblockFlag.AccessType.ALLOW);
					});
		}).orTimeout(3, TimeUnit.SECONDS).exceptionally(ex -> {
			instance.getPluginLogger().warn("canInteract failed: " + ex.getMessage());
			return false;
		});
	}

	private CompletableFuture<Boolean> denyIfNotAllowed(Player player, Location loc, Cancellable event,
			Component message, HellblockFlag.FlagType flag) {

		final CompletableFuture<Boolean> canInteractFuture = canInteract(player, loc, flag);

		if (flag == HellblockFlag.FlagType.ENTRY) {
			return instance.getCoopManager().getHellblockOwnerOfBlock(loc.getBlock()).thenCompose(islandOwnerUUID -> {
				if (islandOwnerUUID == null) {
					// No island at this location, fall back to normal check
					return canInteractFuture;
				}

				final CompletableFuture<Boolean> bannedFuture = instance.getCoopManager()
						.isPlayerBannedInLocation(player.getUniqueId(), islandOwnerUUID, loc);

				final CompletableFuture<Boolean> welcomeFuture = instance.getCoopManager()
						.checkIfVisitorsAreWelcome(player, islandOwnerUUID);

				return canInteractFuture.thenCombine(bannedFuture, EntryCheck::new)
						.thenCombine(welcomeFuture, EntryCheck::withWelcome).thenApply(entryCheck -> {
							if (!entryCheck.isAllowed() && !bypass(player)) {
								instance.getScheduler().executeSync(() -> {
									event.setCancelled(true);
									if (player != null) {
										final Component denyMessage;
										if (entryCheck.banned) {
											denyMessage = instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY.build());
										} else if (!entryCheck.welcome) {
											denyMessage = instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build());
										} else {
											denyMessage = instance.getTranslationManager().render(message);
										}
										instance.getSenderFactory().wrap(player).sendMessage(denyMessage);
									}
									instance.debug(
											"Protection: Entry denied for %s (%s) at [world=%s, x=%d, y=%d, z=%d]. Reason=%s, Event=%s"
													.formatted(player != null ? player.getName() : "unknown",
															player != null ? player.getUniqueId() : null,
															loc.getWorld() != null ? loc.getWorld().getName()
																	: "unknown",
															loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
															entryCheck.reason(), event.getClass().getSimpleName()));
								});
								return true;
							}
							return false;
						});
			}).exceptionally(ex -> {
				instance.getPluginLogger().warn("denyIfNotAllowed failed for "
						+ (player != null ? player.getName() : "unknown") + ": " + ex.getMessage());
				return false;
			});
		}

		// Non-ENTRY
		return canInteractFuture.thenApply(can -> {
			if (!can && !bypass(player)) {
				instance.getScheduler().executeSync(() -> {
					event.setCancelled(true);
					if (player != null) {
						instance.getSenderFactory().wrap(player)
								.sendMessage(instance.getTranslationManager().render(message));
					}
					instance.debug("Protection: Denied %s (%s) at [world=%s, x=%d, y=%d, z=%d]. Event=%s".formatted(
							player != null ? player.getName() : "unknown", player != null ? player.getUniqueId() : null,
							loc.getWorld() != null ? loc.getWorld().getName() : "unknown", loc.getBlockX(),
							loc.getBlockY(), loc.getBlockZ(), event.getClass().getSimpleName()));
				});
				return true;
			}
			return false;
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("denyIfNotAllowed failed for "
					+ (player != null ? player.getName() : "unknown") + ": " + ex.getMessage());
			return false;
		});
	}

	private void checkFlag(Player player, Location loc, Cancellable event, Component message,
			HellblockFlag.FlagType flag) {
		denyIfNotAllowed(player, loc, event, message, flag);
	}

	private boolean movedWithinBlock(PlayerMoveEvent e) {
		return e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY()
				&& e.getTo().getBlockZ() == e.getFrom().getBlockZ();
	}

	private void handleHellblockMessage(Player player) {
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID == null) {
				return;
			}

			final BoundingBox cachedBounds = hellblockCache.get(ownerUUID);
			if (cachedBounds != null) {
				checkAndSendMessageAsync(player, ownerUUID, cachedBounds);
				return;
			}

			// Asynchronously fetch the bounding box from offline user data
			instance.getStorageManager()
					.getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData offlineUser = result.get();
						final BoundingBox hellblockBounds = offlineUser.getHellblockData().getBoundingBox();
						if (hellblockBounds == null) {
							return;
						}

						hellblockCache.putIfAbsent(ownerUUID, hellblockBounds);
						checkAndSendMessageAsync(player, ownerUUID, hellblockBounds);
					}).exceptionally(ex -> {
						instance.getPluginLogger()
								.warn("Failed to fetch hellblock bounds for " + ownerUUID + ": " + ex.getMessage());
						return null;
					});
		});
	}

	private void checkAndSendMessageAsync(Player player, UUID ownerUUID, BoundingBox hellblockBounds) {
		final UUID playerId = player.getUniqueId();
		final BoundingBox playerBounds = player.getBoundingBox();

		final boolean isInside = hellblockBounds.contains(playerBounds);
		final Boolean wasInside = playerInBoundsState.get(playerId);

		// If player's state hasn't changed, don't send message
		if (wasInside != null && wasInside == isInside) {
			return;
		}

		// Update player state
		playerInBoundsState.put(playerId, isInside);

		// Fetch the appropriate message from owner data asynchronously
		instance.getStorageManager().getCachedUserDataWithFallback(ownerUUID, instance.getConfigManager().lockData())
				.thenAccept(optData -> {
					if (optData.isEmpty()) {
						return;
					}

					final UserData ownerData = optData.get();
					final HellblockFlag.FlagType flagType = isInside ? HellblockFlag.FlagType.GREET_MESSAGE
							: HellblockFlag.FlagType.FAREWELL_MESSAGE;

					String messageText = ownerData.getHellblockData().getProtectionData(flagType);

					if (messageText != null && !messageText.isEmpty()) {
						messageText = messageText.replace("<arg:0>", ownerData.getName());
						final Component message = AdventureHelper.miniMessageToComponent(messageText);

						// Schedule sending the message on the main thread
						instance.getScheduler().executeSync(() -> {
							final Sender audience = instance.getSenderFactory().wrap(player);
							audience.sendMessage(instance.getTranslationManager().render(message));
						});
					}
				}).exceptionally(ex -> {
					instance.getPluginLogger()
							.warn("Failed to fetch greet/farewell message for " + ownerUUID + ": " + ex.getMessage());
					return null;
				});
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID playerId = event.getPlayer().getUniqueId();
		playerInBoundsState.remove(playerId);
		collisionManager.setNonCollidable(event.getPlayer(), false);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		UUID playerId = event.getPlayer().getUniqueId();
		if (instance.getHellblockHandler().isInCorrectWorld(event.getFrom())) {
			playerInBoundsState.remove(playerId);
			collisionManager.setNonCollidable(event.getPlayer(), false);
		}
	}

	private boolean isShopSign(Block block) {
		if (!(block.getState() instanceof Sign sign)) {
			return false;
		}

		Set<ShopSignProvider> shopSignProviders = instance.getIntegrationManager().getShopSignProviders();
		for (ShopSignProvider shopSignProvider : shopSignProviders) {
			if (shopSignProvider != null) {
				return shopSignProvider.isShopSign(sign);
			}
		}

		// === Fallback: Heuristic based on Adventure sign lines ===
		try {
			// Retrieves all sign lines, dual-sided if supported (1.20+), or 4 lines
			// otherwise
			List<Component> lines = SignReflectionHelper.getSignLines(sign);

			for (Component line : lines) {
				if (line == null)
					continue;

				// Convert Adventure Component to plain text (no formatting or colors)
				// Normalize case for comparison
				String plain = AdventureHelper.componentToPlainText(line).toLowerCase(Locale.ROOT);

				// Check for common shop sign patterns
				if (plain.contains("[shop]") || plain.contains("[buy]") || plain.contains("[sell]")
						|| plain.contains("[trade]") || plain.contains("quickshop") || plain.contains("[tradeshop]")
						|| plain.contains("[ultimateshop]")) {
					return true;
				}
			}
		} catch (Exception ex) {
			Location loc = sign.getLocation();
			instance.getPluginLogger().warn("Failed to read sign lines at " + loc + ": " + ex.getMessage());
		}

		return false;
	}

	public class ExplosionTracker {
		private final Map<Block, UUID> blockOwnerMap = new ConcurrentHashMap<>();
		private final Map<Entity, UUID> entityOwnerMap = new ConcurrentHashMap<>();
		private final Map<UUID, Long> expiryMap = new ConcurrentHashMap<>();
		private final HellblockPlugin instance;
		private static final long EXPIRY_TICKS = 20L * 60; // 1 minute

		public ExplosionTracker(HellblockPlugin plugin) {
			this.instance = plugin;
		}

		public void track(Block block, Player player) {
			final UUID uuid = player.getUniqueId();
			blockOwnerMap.put(block, uuid);
			expiryMap.put(uuid, System.currentTimeMillis() + (EXPIRY_TICKS * 50));

			instance.getScheduler().sync().runLater(() -> blockOwnerMap.remove(block), EXPIRY_TICKS,
					block.getLocation());
		}

		public void track(Entity entity, Player player) {
			final UUID uuid = player.getUniqueId();
			entityOwnerMap.put(entity, uuid);
			expiryMap.put(uuid, System.currentTimeMillis() + (EXPIRY_TICKS * 50));

			instance.getScheduler().sync().runLater(() -> entityOwnerMap.remove(entity), EXPIRY_TICKS,
					entity.getLocation());
		}

		@Nullable
		public Player getResponsiblePlayer(Block block) {
			final UUID uuid = blockOwnerMap.get(block);
			if (uuid == null) {
				return null;
			}

			final Long expiry = expiryMap.get(uuid);
			if (expiry == null || System.currentTimeMillis() > expiry) {
				blockOwnerMap.remove(block);
				expiryMap.remove(uuid);
				return null;
			}

			return Bukkit.getPlayer(uuid);
		}

		@Nullable
		public Player getResponsiblePlayer(Entity entity) {
			final UUID uuid = entityOwnerMap.get(entity);
			if (uuid == null) {
				return null;
			}

			final Long expiry = expiryMap.get(uuid);
			if (expiry == null || System.currentTimeMillis() > expiry) {
				entityOwnerMap.remove(entity);
				expiryMap.remove(uuid);
				return null;
			}

			return Bukkit.getPlayer(uuid);
		}
	}

	private record EntryCheck(boolean canInteract, boolean banned, boolean welcome) {
		EntryCheck(boolean canInteract, boolean banned) {
			this(canInteract, banned, true); // default welcome=true until combined
		}

		EntryCheck withWelcome(boolean welcome) {
			return new EntryCheck(this.canInteract, this.banned, welcome);
		}

		boolean isAllowed() {
			return canInteract && !banned && welcome;
		}

		String reason() {
			if (banned)
				return "banned";
			if (!welcome)
				return "not welcome";
			if (!canInteract)
				return "no permission";
			return "unknown";
		}
	}

	public class LegacyMountListener implements Listener {

		private final Map<UUID, Long> recentMounts = new ConcurrentHashMap<>();
		private static final long MOUNT_TRACK_DURATION_MS = 10_000; // 10 seconds

		private SchedulerTask dismountTask;

		// Approximate mounting attempt by player interacting with mountable entity
		@EventHandler(ignoreCancelled = true)
		public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
			Player player = event.getPlayer();
			Entity clicked = event.getRightClicked();

			// Check if the entity is likely mountable
			if (!isLikelyMountable(clicked))
				return;

			Set<PetProvider> petProviders = instance.getIntegrationManager().getPetProviders();
			for (PetProvider petProvider : petProviders) {
				if (petProvider != null && player.getUniqueId().equals(petProvider.getOwnerUUID(player))
						&& petProvider.isPet(clicked)) {
					return;
				}
			}

			denyIfNotAllowed(player, clicked.getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(), HellblockFlag.FlagType.RIDE)
					.thenAccept(denied -> {
						if (!denied) {
							instance.getScheduler().sync().runLater(() -> {
								// Track mount attempt time
								if (player.isInsideVehicle()) {
									recentMounts.put(player.getUniqueId(), System.currentTimeMillis());
								}
							}, 2L, clicked.getLocation());
						}
					});
		}

		// Approximate dismount using VehicleExitEvent (only for vehicles, not animals)
		@EventHandler(ignoreCancelled = true)
		public void onVehicleExit(VehicleExitEvent event) {
			if (!(event.getExited() instanceof Player player)) {
				return;
			}

			final Location loc = event.getVehicle().getLocation();

			denyIfNotAllowed(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
					HellblockFlag.FlagType.ENTRY).thenAccept(denied -> {
						if (!denied) {
							checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(),
									HellblockFlag.FlagType.RIDE);
						}
					});
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			recentMounts.remove(event.getPlayer().getUniqueId());
		}

		// Fallback for other dismounts (e.g., from horses, pigs, etc.)
		// Run a periodic check for dismounted players
		public void startDismountCheckTask() {
			dismountTask = instance.getScheduler().sync().runRepeating(() -> {
				recentMounts.entrySet()
						.removeIf(entry -> System.currentTimeMillis() - entry.getValue() > MOUNT_TRACK_DURATION_MS);

				instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer).filter(Objects::nonNull)
						.forEach(player -> {
							if (!player.isInsideVehicle() && shouldCheckDismount(player)) {
								Location loc = player.getLocation();
								if (!player.hasPermission("hellblock.bypass.lock")) {
									denyIfNotAllowed(player, loc, null,
											MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(),
											HellblockFlag.FlagType.ENTRY);
								}
								handleHellblockMessage(player);
							}
						});
			}, 20L, 40L, LocationUtils.getAnyLocationInstance()); // every 2 seconds
		}

		public void stopDismountCheckTask() {
			if (dismountTask != null && !dismountTask.isCancelled()) {
				dismountTask.cancel();
				dismountTask = null;
			}
		}

		// Heuristic: only run checks on players who recently mounted
		private boolean shouldCheckDismount(Player player) {
			Long mountTime = recentMounts.get(player.getUniqueId());
			if (mountTime == null) {
				return false;
			}

			long elapsed = System.currentTimeMillis() - mountTime;
			if (elapsed > MOUNT_TRACK_DURATION_MS) {
				recentMounts.remove(player.getUniqueId()); // Cleanup
				return false;
			}
			return true;
		}

		private boolean isLikelyMountable(Entity entity) {
			return MOUNTABLE_TYPES.contains(entity.getType());
		}

		private static final Set<EntityType> MOUNTABLE_TYPES = buildMountableTypes();

		private static Set<EntityType> buildMountableTypes() {
			Set<EntityType> types = EnumSet.noneOf(EntityType.class);

			// Always present
			Collections.addAll(types, EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.PIG,
					EntityType.STRIDER, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.MINECART);

			// Camel (Present in 1.19.3+ only)
			try {
				types.add(EntityType.valueOf("CAMEL"));
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			}

			// BOAT (legacy)
			try {
				types.add(EntityType.valueOf("CHEST_BOAT"));
				types.add(EntityType.valueOf("BOAT"));
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			}

			// New boat types
			addEntityIfPresent(types, "OAK_BOAT");
			addEntityIfPresent(types, "SPRUCE_BOAT");
			addEntityIfPresent(types, "BIRCH_BOAT");
			addEntityIfPresent(types, "JUNGLE_BOAT");
			addEntityIfPresent(types, "ACACIA_BOAT");
			addEntityIfPresent(types, "DARK_OAK_BOAT");
			addEntityIfPresent(types, "MANGROVE_BOAT");
			addEntityIfPresent(types, "CHERRY_BOAT");
			addEntityIfPresent(types, "PALE_OAK_BOAT");
			addEntityIfPresent(types, "BAMBOO_RAFT");

			// Chest variants
			addEntityIfPresent(types, "OAK_CHEST_BOAT");
			addEntityIfPresent(types, "SPRUCE_CHEST_BOAT");
			addEntityIfPresent(types, "BIRCH_CHEST_BOAT");
			addEntityIfPresent(types, "JUNGLE_CHEST_BOAT");
			addEntityIfPresent(types, "ACACIA_CHEST_BOAT");
			addEntityIfPresent(types, "DARK_OAK_CHEST_BOAT");
			addEntityIfPresent(types, "MANGROVE_CHEST_BOAT");
			addEntityIfPresent(types, "CHERRY_CHEST_BOAT");
			addEntityIfPresent(types, "PALE_OAK_CHEST_BOAT");
			addEntityIfPresent(types, "BAMBOO_CHEST_RAFT");

			return Collections.unmodifiableSet(types);
		}

		private static void addEntityIfPresent(Set<EntityType> set, String name) {
			try {
				EntityType type = EntityType.valueOf(name);
				set.add(type);
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			}
		}
	}

	public class CollisionManager {
		private final Set<UUID> nonCollidablePlayers = Collections.synchronizedSet(new HashSet<>());

		public void setNonCollidable(Player player, boolean value) {
			player.setCollidable(!value);
			if (value) {
				nonCollidablePlayers.add(player.getUniqueId());
			} else {
				nonCollidablePlayers.remove(player.getUniqueId());
			}
		}

		public boolean isNonCollidable(Player player) {
			return nonCollidablePlayers.contains(player.getUniqueId());
		}

		public void resetAll() {
			for (UUID uuid : new HashSet<>(nonCollidablePlayers)) {
				Player player = Bukkit.getPlayer(uuid);
				if (player != null) {
					player.setCollidable(true);
				}
			}
			nonCollidablePlayers.clear();
		}
	}
}