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
import com.swiftlicious.hellblock.events.fishing.FishingHookStateEvent;
import com.swiftlicious.hellblock.events.fishing.RodCastEvent;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.mechanics.fishing.CustomFishingHook;
import com.swiftlicious.hellblock.mechanics.fishing.FishingGears;
import com.swiftlicious.hellblock.mechanics.fishing.hook.LavaFishingMechanic;
import com.swiftlicious.hellblock.player.Context;
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
		if (hook.getShooter() != null && hook.getShooter() instanceof Player player) {
			return Optional.ofNullable(Bukkit.getPlayer(player.getUniqueId()));
		}
		return Optional.empty();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFishMONITOR(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.MONITOR)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishHIGHEST(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.HIGHEST)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFishHIGH(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.HIGH)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onFishNORMAL(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.NORMAL)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onFishLOW(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.LOW)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onFishLOWEST(PlayerFishEvent event) {
		if (instance.getConfigManager().eventPriority() != EventPriority.LOWEST)
			return;
		this.selectState(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		if (getFishHook(event.getPlayer()).isPresent()) {
			this.destroyHook(event.getPlayer().getUniqueId());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSwapItem(PlayerSwapHandItemsEvent event) {
		getFishHook(event.getPlayer()).ifPresent(hook -> {
			this.destroyHook(event.getPlayer().getUniqueId());
		});
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
		// case CAUGHT_FISH -> onCaughtFish(event);
		case BITE -> onBite(event);
		case IN_GROUND -> onInGround(event);
		case FAILED_ATTEMPT -> onFailedAttempt(event);
		// case LURED 1.20.5+
		default -> throw new IllegalArgumentException("Unexpected value: " + event.getState());
		}
	}

	// for vanilla mechanics
	private void onFailedAttempt(PlayerFishEvent event) {
		Player player = event.getPlayer();
		getFishHook(player).ifPresent(hook -> {
			if (hook.getCurrentHookMechanic() instanceof LavaFishingMechanic vanillaMechanic) {
				vanillaMechanic.onFailedAttempt();
			}
		});
	}

	// for vanilla mechanics
	private void onBite(PlayerFishEvent event) {
		Player player = event.getPlayer();
		getFishHook(player).ifPresent(hook -> {
			if (hook.getCurrentHookMechanic() instanceof LavaFishingMechanic vanillaMechanic) {
				vanillaMechanic.onBite();
			}
		});
	}

	private void onCaughtEntity(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		Optional<CustomFishingHook> hook = getFishHook(player);
		if (hook.isPresent()) {
			Entity entity = event.getCaught();
			if (entity != null && entity.getPersistentDataContainer().get(
					Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)),
					PersistentDataType.STRING) != null) {
				event.setCancelled(true);
				hook.get().onReelIn();
				return;
			}

			if (player.getGameMode() != GameMode.CREATIVE) {
				ItemStack itemStack = player.getInventory().getItemInMainHand();
				if (itemStack.getType() != Material.FISHING_ROD)
					itemStack = player.getInventory().getItemInOffHand();
				if (instance.getItemManager().hasCustomMaxDamage(itemStack)) {
					event.setCancelled(true);
					event.getHook().pullHookedEntity();
					hook.get().destroy();
					instance.getItemManager().increaseDamage(player, itemStack,
							event.getCaught() instanceof Item ? 3 : 5, true);
				}
			}
		}
	}

	private void onReelIn(PlayerFishEvent event) {
		Player player = event.getPlayer();
		getFishHook(player).ifPresent(hook -> {
			event.setCancelled(true);
			hook.onReelIn();
		});
	}

//    private void onCaughtFish(PlayerFishEvent event) {
//        Player player = event.getPlayer();
//        getFishHook(player).ifPresent(hook -> {
//            event.setCancelled(true);
//            hook.onReelIn();
//        });
//    }

	private void onCastRod(PlayerFishEvent event) {
		FishHook hook = event.getHook();
		Player player = event.getPlayer();
		Context<Player> context = Context.player(player);
		FishingGears gears = new FishingGears(context);
		if (!RequirementManagerInterface.isSatisfied(context, instance.getConfigManager().fishingRequirements())) {
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
		CustomFishingHook customHook = new CustomFishingHook(instance, hook, gears, context);
		CustomFishingHook previous = this.castHooks.put(player.getUniqueId(), customHook);
		if (previous != null) {
			instance.debug("Previous hook is still in cache, which is not an expected behavior");
			previous.stop();
		}
	}

	private void onInGround(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			if (itemStack.getType() == Material.FISHING_ROD) {
				if (instance.getItemManager().hasCustomMaxDamage(itemStack)) {
					event.setCancelled(true);
					event.getHook().remove();
					instance.getItemManager().increaseDamage(player, itemStack, 2, true);
				}
			}
		}
	}

	@EventHandler
	public void onHookStateChange(FishingHookStateEvent event) {
		Player player = event.getPlayer();
		getFishHook(player).ifPresent(hook -> {
			switch (event.getState()) {
			case BITE -> hook.onBite();
			case LAND -> hook.onLand();
			case ESCAPE -> hook.onEscape();
			case LURE -> hook.onLure();
			}
		});
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractAtEntityEvent event) {
		Entity entity = event.getRightClicked();
		if (entity.getType() != EntityType.ARMOR_STAND)
			return;
		if (entity.getPersistentDataContainer()
				.has(Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		Entity entity = event.getRightClicked();
		if (entity.getType() != EntityType.ARMOR_STAND)
			return;
		if (entity.getPersistentDataContainer()
				.has(Objects.requireNonNull(NamespacedKey.fromString("temp-entity", instance)))) {
			event.setCancelled(true);
		}
	}

	@Override
	public void destroyHook(UUID uuid) {
		CustomFishingHook hook = this.castHooks.remove(uuid);
		if (hook != null) {
			hook.stop();
		}
	}
}