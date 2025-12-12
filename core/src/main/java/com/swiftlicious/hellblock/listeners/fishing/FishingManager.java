package com.swiftlicious.hellblock.listeners.fishing;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.events.fishing.FishingHookStateEvent;
import com.swiftlicious.hellblock.events.fishing.RodCastEvent;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.mechanics.fishing.CustomFishingHook;
import com.swiftlicious.hellblock.mechanics.fishing.FishingGears;
import com.swiftlicious.hellblock.mechanics.fishing.hook.LavaFishingMechanic;
import com.swiftlicious.hellblock.utils.EventUtils;

public class FishingManager implements Listener, FishingManagerInterface {

	protected final HellblockPlugin instance;
	private final ConcurrentMap<UUID, CustomFishingHook> castHooks = new ConcurrentHashMap<>();

	public FishingManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public Optional<CustomFishingHook> getFishHook(Player player) {
		return getFishHook(player.getUniqueId());
	}

	@Override
	public Optional<CustomFishingHook> getFishHook(UUID player) {
		return Optional.ofNullable(castHooks.get(player));
	}

	@Override
	public Optional<Player> getOwner(FishHook hook) {
		final UUID ownerUUID = VersionHelper.getNMSManager().getFishingHookOwner(hook);
		if (ownerUUID != null) {
			return Optional.ofNullable(Bukkit.getPlayer(ownerUUID));
		}
		return Optional.empty();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFishMONITOR(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.MONITOR) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishHIGHEST(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.HIGHEST) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFishHIGH(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.HIGH) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onFishNORMAL(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.NORMAL) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFishLOW(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.LOW) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onFishLOWEST(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.LOWEST) {
			return;
		}
		this.selectState(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		if (getFishHook(player).isPresent()) {
			this.destroyHook(player.getUniqueId());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSwapItem(PlayerSwapHandItemsEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		getFishHook(player).ifPresent(hook -> this.destroyHook(player.getUniqueId()));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.destroyHook(event.getPlayer().getUniqueId());
	}

	private void selectState(PlayerFishEvent event) {
		switch (event.getState()) {
		case FISHING -> onCastRod(event);
		case REEL_IN, CAUGHT_FISH -> onReelIn(event);
		case CAUGHT_ENTITY -> onCaughtEntity(event);
		case BITE -> onBite(event);
		case IN_GROUND -> onInGround(event);
		case FAILED_ATTEMPT -> onFailedAttempt(event);
		default -> {
			// Dynamically check for LURED (1.20.5+)
			if (VersionHelper.isPaperFork() && VersionHelper.isVersionNewerThan1_20_5()) {
				try {
					if (event.getState() == PlayerFishEvent.State.valueOf("LURED")) {
						onLured(event);
					}
				} catch (Exception ex) {
					instance.getPluginLogger()
							.warn("PlayerFishEvent couldn't find the LURED state: " + ex.getMessage());
				}
			} else {
				throw new IllegalArgumentException("Unexpected value: " + event.getState());
			}
		}
		}
	}

	private void onFailedAttempt(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		getFishHook(player).filter(hook -> hook.getCurrentHookMechanic() instanceof LavaFishingMechanic)
				.map(hook -> (LavaFishingMechanic) hook.getCurrentHookMechanic())
				.ifPresent(LavaFishingMechanic::onFailedAttempt);

		instance.debug("FAILED ATTEMPT event triggered for player: " + player.getName());
	}

	private void onBite(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		getFishHook(player).filter(hook -> hook.getCurrentHookMechanic() instanceof LavaFishingMechanic)
				.map(hook -> (LavaFishingMechanic) hook.getCurrentHookMechanic())
				.ifPresent(LavaFishingMechanic::onBite);

		instance.debug("BITE event triggered for player: " + player.getName());
	}

	private void onCaughtEntity(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		final Optional<CustomFishingHook> hook = getFishHook(player);
		if (!hook.isPresent()) {
			return;
		}
		final Entity entity = event.getCaught();
		if (entity != null && entity.getPersistentDataContainer().get(
				Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)),
				PersistentDataType.STRING) != null) {
			event.setCancelled(true);
			hook.get().onReelIn();
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		if (itemStack.getType() != Material.FISHING_ROD) {
			itemStack = player.getInventory().getItemInOffHand();
		}
		if (!instance.getItemManager().hasCustomMaxDamage(itemStack)) {
			return;
		}
		event.setCancelled(true);
		event.getHook().pullHookedEntity();
		hook.get().destroy();
		instance.getItemManager().increaseDamage(player, itemStack, event.getCaught() instanceof Item ? 3 : 5, true);
	}

	private void onReelIn(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		getFishHook(player).ifPresent(hook -> {
			event.setCancelled(true);
			hook.onReelIn();
		});
	}

//    private void onCaughtFish(PlayerFishEvent event) {
//        final Player player = event.getPlayer();
//		  if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
//		      return;
//        }
//        getFishHook(player).ifPresent(hook -> {
//            event.setCancelled(true);
//            hook.onReelIn();
//        });
//    }

	private void onLured(PlayerFishEvent event) {
		final Player player = event.getPlayer();

		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}

		getFishHook(player).filter(hook -> hook.getCurrentHookMechanic() instanceof LavaFishingMechanic)
				.map(hook -> (LavaFishingMechanic) hook.getCurrentHookMechanic())
				.ifPresent(LavaFishingMechanic::onLured);

		instance.debug("LURED event triggered for player: " + player.getName());
	}

	private void onCastRod(PlayerFishEvent event) {
		final FishHook hook = event.getHook();
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		final Context<Player> context = Context.player(player);
		final Optional<Integer> optIslandId = instance.getIslandManager().getLastTrackedIsland(player.getUniqueId());
		optIslandId.filter(Objects::nonNull).ifPresent(islandId -> {
			final Context<Integer> islandContext = Context.island(islandId);
			final FishingGears gears = new FishingGears();
			gears.init(context);
			context.arg(ContextKeys.HOOK_ENTITY, hook);
			if (!RequirementManager.isSatisfied(context, instance.getConfigManager().fishingPlayerRequirements())
					|| !RequirementManager.isSatisfied(islandContext,
							instance.getConfigManager().fishingIslandRequirements())) {
				this.destroyHook(player.getUniqueId());
				return;
			}
			if (!gears.canFish()) {
				event.setCancelled(true);
				return;
			}
			if (EventUtils.fireAndCheckCancel(new RodCastEvent(event, gears))) {
				return;
			}
			instance.debug(context.toString());
			final CustomFishingHook customHook = new CustomFishingHook(instance, hook, gears, context);
			customHook.init();
			final CustomFishingHook previous = this.castHooks.put(player.getUniqueId(), customHook);
			if (previous == null) {
				return;
			}
			instance.debug("Previous hook is still in cache, which is not an expected behavior");
			previous.stop();
		});
	}

	private void onInGround(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		ItemStack itemStack = player.getInventory().getItemInMainHand();
		if (itemStack.getType() != Material.FISHING_ROD) {
			itemStack = player.getInventory().getItemInOffHand();
		}
		final boolean groundCondition = itemStack.getType() == Material.FISHING_ROD
				&& instance.getItemManager().hasCustomMaxDamage(itemStack);
		if (!groundCondition) {
			return;
		}
		event.setCancelled(true);
		event.getHook().remove();
		instance.getItemManager().increaseDamage(player, itemStack, 2, true);
	}

	@EventHandler
	public void onHookStateChange(FishingHookStateEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player.getWorld())) {
			return;
		}
		getFishHook(player).ifPresent(hook -> {
			switch (event.getState()) {
			case BITE -> hook.onBite();
			case LAND -> hook.onLand();
			case ESCAPE -> hook.onEscape();
			case LURE -> hook.onLure();
			case HOOK -> hook.onHook();
			}
		});
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractAtEntityEvent event) {
		final Entity entity = event.getRightClicked();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld())) {
			return;
		}
		if (entity.getType() != EntityType.ARMOR_STAND) {
			return;
		}
		if (entity.getPersistentDataContainer().has(
				Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)), PersistentDataType.STRING)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		final Entity entity = event.getRightClicked();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld())) {
			return;
		}
		if (entity.getType() != EntityType.ARMOR_STAND) {
			return;
		}
		if (entity.getPersistentDataContainer().has(
				Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)), PersistentDataType.STRING)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void destroyHook(UUID uuid) {
		final CustomFishingHook hook = this.castHooks.remove(uuid);
		if (hook != null) {
			hook.stop();
		}
	}
}