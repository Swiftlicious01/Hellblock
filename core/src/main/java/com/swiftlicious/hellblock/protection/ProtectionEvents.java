package com.swiftlicious.hellblock.protection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.EntityUnleashEvent.UnleashReason;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
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
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.schematic.AdventureMetadata;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ProtectionEvents implements Listener, Reloadable {

	protected final HellblockPlugin instance;
	private final ExplosionTracker explosionTracker;

	private final Map<UUID, BoundingBox> hellblockCache = new ConcurrentHashMap<>();

	// Keeps track of whether the player was inside the island bounds last time
	private final Map<UUID, Boolean> playerInBoundsState = new ConcurrentHashMap<>();

	public ProtectionEvents(HellblockPlugin plugin) {
		instance = plugin;
		this.explosionTracker = new ExplosionTracker(instance);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.hellblockCache.clear();
		this.playerInBoundsState.clear();
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
						if (event.getClickedBlock().getType() == Material.LECTERN) {
							UUID playerId = event.getPlayer().getUniqueId();
							// Store this temporarily for the player
							lecternEditMap.put(playerId, loc);
						}

						final Material type = block.getType();

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
									|| mat == Material.MINECART || mat == Material.BAMBOO_RAFT) {
								checkFlag(player, loc, event,
										MessageConstants.MSG_HELLBLOCK_PROTECTION_VEHICLE_PLACE_DENY.build(),
										HellblockFlag.FlagType.PLACE_VEHICLE);
							}
						}

						// Physical trample check
						if (event.getAction() == Action.PHYSICAL && block.getType() == Material.FARMLAND) {
							checkFlag(player, loc, event,
									MessageConstants.MSG_HELLBLOCK_PROTECTION_TRAMPLE_DENY.build(),
									HellblockFlag.FlagType.TRAMPLE_BLOCKS);
						}
					}
				});

		// Track TNT for later explosion attribution
		if (block.getType() == Material.TNT) {
			explosionTracker.track(block, player);
		}
	}

	private boolean isShopSign(Block block) {
		if (!(block.getState() instanceof Sign sign)) {
			return false;
		}

		Location loc = sign.getLocation();

		// === Check QuickShop API ===
		if (instance.getIntegrationManager().isHooked("QuickShop")) {
			try {
				Class<?> apiClass = Class.forName("org.maxgamer.quickshop.api.QuickShopAPI");
				Object api = apiClass.getMethod("getAPI").invoke(null);
				Object shop = apiClass.getMethod("getShop", Location.class).invoke(api, loc);
				if (shop != null) {
					return true;
				}
			} catch (Exception ignored) {
				// Fallback continues below
			}
		}

		// === Check ChestShop API ===
		if (instance.getIntegrationManager().isHooked("ChestShop")) {
			try {
				Class<?> utilsClass = Class.forName("com.Acrobot.ChestShop.Shop.ShopUtils");
				Object shop = utilsClass.getMethod("getShop", Location.class).invoke(null, loc);
				if (shop != null) {
					return true;
				}
			} catch (Exception ignored) {
				// Fallback continues below
			}
		}

		// === Check TradeShop API ==
		if (instance.getIntegrationManager().isHooked("TradeShop")) {
			try {
				Class<?> shopUtilsClass = Class.forName("org.shanerx.tradeshop.utils.ShopUtils");
				// ShopUtils is a singleton: ShopUtils.getInstance().isShop(loc)
				Object instance = shopUtilsClass.getMethod("getInstance").invoke(null);
				boolean isShop = (boolean) shopUtilsClass.getMethod("isShop", Location.class).invoke(instance,
						block.getLocation());
				if (isShop) {
					return true;
				}
			} catch (Exception ex) {
				// Fallback continues below
			}
		}

		// === Fallback: Heuristic based on Adventure sign lines ===
		try {
			List<Component> lines = AdventureMetadata.getSignLines(sign.getSide(Side.FRONT));
			for (Component line : lines) {
				if (line == null)
					continue;

				// Convert Adventure Component to plain text without formatting
				String plain = PlainTextComponentSerializer.plainText().serialize(line).toLowerCase(Locale.ROOT);

				// Very common shop patterns
				if (plain.contains("[shop]") || plain.contains("[buy]") || plain.contains("[sell]")
						|| plain.contains("[trade]") || plain.contains("quickshop") || plain.contains("[tradeshop]")
						|| plain.contains("[ultimateshop]")) {
					return true;
				}
			}
		} catch (Exception ex) {
			instance.getPluginLogger().warn("Failed to read sign lines for shop detection: " + ex.getMessage());
		}

		return false;
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
	public void onEntityUnleash(EntityUnleashEvent event) {
		// Optional: only care about player-unleashing (if possible to detect)
		if (event.getReason() == UnleashReason.PLAYER_UNLEASH) {
			final Location loc = event.getEntity().getLocation();

			// Use checkOwnerFlag since no Player involved in the event
			checkOwnerFlag(loc, HellblockFlag.FlagType.INTERACT,
					() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
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

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);
	}

	// Entity interaction master (INTERACT) — covers entity interactions (villagers,
	// frames, mounts etc.)
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		final Player player = event.getPlayer();
		final Location loc = event.getRightClicked().getLocation();

		// Item frame rotation / editing
		if (event.getRightClicked() instanceof ItemFrame) {
			checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_ITEM_FRAME_ROTATE_DENY.build(),
					HellblockFlag.FlagType.ITEM_FRAME_ROTATE);
			return;
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_INTERACT_DENY.build(),
				HellblockFlag.FlagType.INTERACT);

		// Vehicle mount attempt is handled below via mount/dismount events
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

	// Inventory open -> chest access
	@EventHandler(ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		final HumanEntity human = event.getPlayer();
		if (!(human instanceof Player player)) {
			return;
		}
		Location loc = event.getView().getTopInventory().getLocation();
		if (loc == null) {
			loc = player.getLocation();
		}

		checkFlag(player, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_CHEST_ACCESS_DENY.build(),
				HellblockFlag.FlagType.CHEST_ACCESS);
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

		if (event.getHitEntity() instanceof Player) {
			checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
					HellblockFlag.FlagType.PVP);
		} else {
			checkFlag(shooter, loc, event, MessageConstants.MSG_HELLBLOCK_PROTECTION_PROJECTILE_DENY.build(),
					HellblockFlag.FlagType.INTERACT);
		}
	}

	// Explosion handling using ExplosionTracker to find responsible player where
	// possible
	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		final Entity exploder = event.getEntity();
		final Player sourcePlayer = (exploder instanceof Player) ? (Player) exploder : null;

		if (sourcePlayer == null) {
			// No player involved -> protect all blocks in islands
			event.blockList().clear();
			return;
		}

		final List<Block> blocks = new ArrayList<>(event.blockList());
		CompletableFuture.allOf(blocks.stream().map(
				block -> canInteract(sourcePlayer, block.getLocation(), HellblockFlag.FlagType.TNT).thenAccept(can -> {
					if (!can && !bypass(sourcePlayer)) {
						instance.getScheduler().executeSync(() -> event.blockList().remove(block));
					}
				})).toArray(CompletableFuture[]::new));
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
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
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

	// Ride (mount/dismount) checks
	@EventHandler(ignoreCancelled = true)
	public void onEntityMount(EntityMountEvent event) {
		if (event.getEntity() instanceof Player player) {
			checkFlag(player, event.getMount().getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(), HellblockFlag.FlagType.RIDE);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDismount(EntityDismountEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		// Check ENTRY flag first (covers dismounting into island area)
		denyIfNotAllowed(player, event.getDismounted().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(), HellblockFlag.FlagType.ENTRY);

		// Then check RIDE flag
		checkFlag(player, event.getDismounted().getLocation(), event,
				MessageConstants.MSG_HELLBLOCK_PROTECTION_RIDE_DENY.build(), HellblockFlag.FlagType.RIDE);
	}

	// Snowman trails -> owner flag
	@EventHandler(ignoreCancelled = true)
	public void onSnowmanTrails(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.SNOW_GOLEM) {
			checkOwnerFlag(event.getBlock().getLocation(), HellblockFlag.FlagType.SNOWMAN_TRAILS,
					() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
		}
	}

	// Enderman griefing -> owner flag
	@EventHandler(ignoreCancelled = true)
	public void onEndermanGrief(EntityChangeBlockEvent event) {
		if (event.getEntityType() == EntityType.ENDERMAN) {
			checkOwnerFlag(event.getBlock().getLocation(), HellblockFlag.FlagType.ENDER_BUILD,
					() -> instance.getScheduler().executeSync(() -> event.setCancelled(true)));
		}
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
		// best-effort detection by type name; adapt if you have a direct class
		if (!VersionHelper.isVersionNewerThan1_20_6()) {
			return;
		}
		final boolean windChargeCondition = event.getEntity().getType() == EntityType.BREEZE_WIND_CHARGE
				&& event.getHitBlock() != null;
		if (windChargeCondition) {
			checkOwnerFlag(event.getHitBlock().getLocation(), HellblockFlag.FlagType.WIND_CHARGE_BURST,
					() -> instance.getScheduler().executeSync(() -> event.getEntity().remove()));
		}
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
	public void onPlayerDismount(EntityDismountEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		if (!player.hasPermission("hellblock.bypass.lock")) {
			denyIfNotAllowed(player, event.getDismounted().getLocation(), event,
					MessageConstants.MSG_HELLBLOCK_PROTECTION_ENTRY_DENY.build(), HellblockFlag.FlagType.ENTRY);
		}

		handleHellblockMessage(player);
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
	public void onBlockGrow(BlockGrowEvent event) {
		Block to = event.getBlock();
		Block from = to.getRelative(BlockFace.DOWN); // Assumes block below is source

		cancelIfLeavingIsland(from, to, event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		Block from = event.getSource();
		Block to = event.getBlock();

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
						event.getBlocks().clear();
						event.getBlocks().addAll(allowedStates);
					}));
		});
	}

	private void cancelIfLeavingIsland(Block from, Block to, Cancellable event) {
		instance.getCoopManager().getHellblockOwnerOfBlock(from)
				.thenCombine(instance.getCoopManager().getHellblockOwnerOfBlock(to), (fromOwner, toOwner) -> {
					if (fromOwner != null && !fromOwner.equals(toOwner)) {
						instance.debug("Cancelled event " + event.getClass().getSimpleName()
								+ " due to island boundary: from=" + blockInfo(from) + ", to=" + blockInfo(to));
						instance.getScheduler().executeSync(() -> event.setCancelled(true));
					}
					return null;
				}).exceptionally(ex -> {
					instance.getPluginLogger().warn("cancelIfLeavingIsland failed: " + ex.getMessage());
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
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
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
		return instance.getCoopManager().getHellblockOwnerOfBlock(location.getBlock()).thenCompose(ownerUUID -> {
			if (ownerUUID == null) {
				instance.debug("Protection: Skipped non-island location at " + location.getWorld().getName() + " ("
						+ location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")");
				return CompletableFuture.completedFuture(false);
			}

			return instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
					.thenApply(result -> {
						if (result.isEmpty()) {
							return false;
						}
						final UserData data = result.get();
						// Co-op/owner access OR explicit ALLOW by owner
						return player != null && (data.getHellblockData().canAccess(player.getUniqueId())
								|| data.getHellblockData().getProtectionValue(flag) == HellblockFlag.AccessType.ALLOW);
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
						.trackBannedPlayer(player.getUniqueId(), islandOwnerUUID);

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
			instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
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
		instance.getStorageManager().getOfflineUserData(ownerUUID, instance.getConfigManager().lockData())
				.thenAccept(optData -> {
					if (optData.isEmpty()) {
						return;
					}

					final UserData userData = optData.get();
					final HellblockFlag.FlagType flagType = isInside ? HellblockFlag.FlagType.GREET_MESSAGE
							: HellblockFlag.FlagType.FAREWELL_MESSAGE;

					final String messageText = userData.getHellblockData().getProtectionData(flagType);

					if (messageText != null && !messageText.isEmpty()) {
						final Component message = Component.text(messageText);

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
	}

	public class ExplosionTracker {
		private final Map<Block, UUID> blockOwnerMap = new ConcurrentHashMap<>();
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

		@Nullable
		public Player getResponsiblePlayer(Block block) {
			final UUID uuid = blockOwnerMap.get(block);
			if (uuid == null) {
				return null;
			}

			final Long expiry = expiryMap.get(uuid);
			if (!(expiry != null && System.currentTimeMillis() > expiry)) {
				return Bukkit.getPlayer(uuid);
			}
			blockOwnerMap.remove(block);
			expiryMap.remove(uuid);
			return null;
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
}