package com.swiftlicious.hellblock.listeners.fishing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.events.fishing.FishingResultEvent;
import com.swiftlicious.hellblock.events.fishing.LavaFishingEvent;
import com.swiftlicious.hellblock.events.fishing.RodCastEvent;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.sound.Sound;

public class FishingManager implements Listener, FishingManagerInterface {

	private final HellblockPlugin instance;
	private final ConcurrentHashMap<UUID, FishHook> hookCacheMap;
	private final ConcurrentHashMap<UUID, HookCheckTimerTask> hookCheckMap;
	private final ConcurrentHashMap<UUID, TempFishingState> tempFishingStateMap;
	private final ConcurrentHashMap<UUID, Pair<ItemStack, Integer>> vanillaLootMap;

	public FishingManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.hookCacheMap = new ConcurrentHashMap<>();
		this.tempFishingStateMap = new ConcurrentHashMap<>();
		this.hookCheckMap = new ConcurrentHashMap<>();
		this.vanillaLootMap = new ConcurrentHashMap<>();
	}

	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public void unload() {
		HandlerList.unregisterAll(this);
		for (FishHook hook : hookCacheMap.values()) {
			hook.remove();
		}
		for (HookCheckTimerTask task : hookCheckMap.values()) {
			task.destroy();
		}
		this.hookCacheMap.clear();
		this.tempFishingStateMap.clear();
		this.hookCheckMap.clear();
	}

	public void disable() {
		unload();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onFishMONITOR(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.MONITOR)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onFishHIGHEST(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.HIGHEST)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onFishHIGH(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.HIGH)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onFishNORMAL(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.NORMAL)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onFishLOW(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.LOW)
			return;
		this.selectState(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onFishLOWEST(PlayerFishEvent event) {
		if (HBConfig.eventPriority != EventPriority.LOWEST)
			return;
		this.selectState(event);
	}

	public boolean checkMaxDurability(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("FishingData", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 0);
			for (String key : map.keySet()) {
				if (key.equals("max_dur")) {
					data = true;
				}
			}
		}
		return data;
	}

	public int getMaxDurability(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 0) == null)
			return -1;

		int data = -1;
		if (tag.get("FishingData", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 0);
			for (String key : map.keySet()) {
				if (key.equals("max_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							data = (int) value;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkCurrentDurability(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 1) == null)
			return false;

		boolean data = false;
		if (tag.get("FishingData", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 1);
			for (String key : map.keySet()) {
				if (key.equals("cur_dur")) {
					data = true;
				}
			}
		}
		return data;
	}

	public int getCurrentDurability(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 1) == null)
			return -1;

		int data = -1;
		if (tag.get("FishingData", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 1);
			for (String key : map.keySet()) {
				if (key.equals("cur_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							data = (int) value;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkFishingType(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 2) == null)
			return false;

		boolean data = false;
		if (tag.get("FishingData", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 2);
			for (String key : map.keySet()) {
				if (key.equals("type")) {
					data = true;
				}
			}
		}
		return data;
	}

	public @Nullable String getFishingType(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 2) == null)
			return null;

		String data = null;
		if (tag.get("FishingData", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 2);
			for (String key : map.keySet()) {
				if (key.equals("type")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							data = (String) value;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkFishingID(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 3) == null)
			return false;

		boolean data = false;
		if (tag.get("FishingData", 3) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 3);
			for (String key : map.keySet()) {
				if (key.equals("id")) {
					data = true;
				}
			}
		}
		return data;
	}

	public @Nullable String getFishingID(RtagItem tag) {
		if (tag == null || tag.get("FishingData", 3) == null)
			return null;

		String data = null;
		if (tag.get("FishingData", 3) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 3);
			for (String key : map.keySet()) {
				if (key.equals("id")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							data = (String) value;
						}
					}
				}
			}
		}
		return data;
	}

	public @Nullable ItemStack setFishingData(RtagItem tag, @Nullable Object... data) {
		if (tag == null || data == null || data.length < 4)
			return null;

		Map<String, Object> map = Map.of("FishingData", List.of(Map.of("max_dur", data[0]), Map.of("cur_dur", data[1]),
				Map.of("type", data[2]), Map.of("id", data[3])));
		tag.set(map);
		return tag.load();
	}

	public @Nullable ItemStack setMaxDurability(RtagItem tag, int data) {
		if (tag == null)
			return null;

		if (tag.get("FishingData", 0) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("FishingData", List.of(Map.of("max_dur", data))), tag.get());
			} else {
				tag.add(Map.of("FishingData", List.of(Map.of("max_dur", data))));
			}
			return tag.load();
		}

		if (tag.get("FishingData", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 0);
			for (String key : map.keySet()) {
				if (key.equals("max_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							value = data;
							tag.set(List.of(Map.of("max_dur", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public @Nullable ItemStack setCurrentDurability(RtagItem tag, int data) {
		if (tag == null)
			return null;

		if (tag.get("FishingData", 1) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("FishingData", List.of(Map.of("cur_dur", data))), tag.get());
			} else {
				tag.add(Map.of("FishingData", List.of(Map.of("cur_dur", data))));
			}
			return tag.load();
		}

		if (tag.get("FishingData", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 1);
			for (String key : map.keySet()) {
				if (key.equals("cur_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							value = data;
							tag.set(List.of(Map.of("cur_dur", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public @Nullable ItemStack setFishingType(RtagItem tag, @Nullable String data) {
		if (tag == null || data == null || data.isEmpty())
			return null;

		if (tag.get("FishingData", 2) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("FishingData", List.of(Map.of("type", data))), tag.get());
			} else {
				tag.add(Map.of("FishingData", List.of(Map.of("type", data))));
			}
			return tag.load();
		}

		if (tag.get("FishingData", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 2);
			for (String key : map.keySet()) {
				if (key.equals("type")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							value = data;
							tag.set(List.of(Map.of("type", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public @Nullable ItemStack setFishingID(RtagItem tag, @Nullable String data) {
		if (tag == null || data == null || data.isEmpty())
			return null;

		if (tag.get("FishingData", 3) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("FishingData", List.of(Map.of("id", data))), tag.get());
			} else {
				tag.add(Map.of("FishingData", List.of(Map.of("id", data))));
			}
			return tag.load();
		}

		if (tag.get("FishingData", 3) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("FishingData", 3);
			for (String key : map.keySet()) {
				if (key.equals("id")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							value = data;
							tag.set(List.of(Map.of("id", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public boolean removeFishingData(RtagItem tag) {
		if (tag == null)
			return false;

		Map<String, Object> map = tag.get();
		return tag.remove(map);
	}

	/**
	 * Removes a fishing hook entity associated with a given UUID.
	 *
	 * @param uuid The UUID of the fishing hook entity to be removed.
	 * @return {@code true} if the fishing hook was successfully removed,
	 *         {@code false} otherwise.
	 */
	@Override
	public boolean removeHook(UUID uuid) {
		FishHook hook = hookCacheMap.remove(uuid);
		if (hook != null && hook.isValid()) {
			instance.getScheduler().runTaskSync(hook::remove, hook.getLocation());
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retrieves a FishHook object associated with the provided player's UUID
	 *
	 * @param uuid The UUID of the player
	 * @return fishhook entity, null if not exists
	 */
	@Override
	@Nullable
	public FishHook getHook(UUID uuid) {
		FishHook fishHook = hookCacheMap.get(uuid);
		if (fishHook != null) {
			if (!fishHook.isValid()) {
				hookCacheMap.remove(uuid);
				return null;
			} else {
				return fishHook;
			}
		}
		return null;
	}

	/**
	 * Selects the appropriate fishing state based on the provided PlayerFishEvent
	 * and triggers the corresponding action.
	 *
	 * @param event The PlayerFishEvent that represents the fishing action.
	 */
	public void selectState(PlayerFishEvent event) {
		if (event.isCancelled())
			return;
		switch (event.getState()) {
		case FISHING -> onCastRod(event);
		case REEL_IN -> onReelIn(event);
		case CAUGHT_ENTITY -> onCaughtEntity(event);
		case CAUGHT_FISH -> onCaughtFish(event);
		case BITE -> onBite(event);
		case IN_GROUND -> onInGround(event);
		default -> onFailAttempt(event);
		}
	}

	/**
	 * Handle the event when the fishing hook lands on the ground.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onInGround(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		FishHook hook = event.getHook();
		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			if (itemStack.getType() == Material.FISHING_ROD) {
				RtagItem tagItem = new RtagItem(itemStack);
				if (checkMaxDurability(tagItem)) {
					event.setCancelled(true);
					hook.remove();
					ItemUtils.decreaseDurability(player, itemStack, 2, true);
				}
			}
		}
	}

	/**
	 * Handle the event when the fishing hook fails.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onFailAttempt(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		FishHook hook = event.getHook();
		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			if (itemStack.getType() == Material.FISHING_ROD) {
				RtagItem tagItem = new RtagItem(itemStack);
				if (checkMaxDurability(tagItem)) {
					event.setCancelled(true);
					hook.remove();
					ItemUtils.decreaseDurability(player, itemStack, 2, true);
				}
			}
		}
	}

	/**
	 * Handle the event when a player casts a fishing rod.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	public void onCastRod(PlayerFishEvent event) {
		var player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		var fishingPreparation = new FishingPreparation(player, instance);
		if (!fishingPreparation.canFish()) {
			event.setCancelled(true);
			return;
		}
		// Check mechanic requirements
		if (!RequirementManagerInterface.isRequirementMet(fishingPreparation,
				instance.getRequirementManager().mechanismRequirements)) {
			this.removeTempFishingState(player);
			return;
		}

		// Call custom event
		RodCastEvent rodCastEvent = new RodCastEvent(event, fishingPreparation,
				instance.getEffectManager().getInitialEffect());
		Bukkit.getPluginManager().callEvent(rodCastEvent);
		if (rodCastEvent.isCancelled()) {
			return;
		}

		// Store fishhook entity and apply the effects
		final FishHook fishHook = event.getHook();
		this.hookCacheMap.put(player.getUniqueId(), fishHook);

		// Reduce amount & Send animation
		var baitItem = fishingPreparation.getBaitItemStack();
		if (baitItem != null) {
			ItemStack cloned = baitItem.clone();
			cloned.setAmount(1);
			new BaitAnimationTask(instance, player, fishHook, cloned);
			baitItem.setAmount(baitItem.getAmount() - 1);
		}

		// Arrange hook check task
		this.hookCheckMap.put(player.getUniqueId(), new HookCheckTimerTask(this, fishHook, fishingPreparation,
				instance.getEffectManager().getInitialEffect()));
		// trigger actions
		fishingPreparation.triggerActions(ActionTrigger.CAST);
	}

	/**
	 * Handle the event when a player catches an entity.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onCaughtEntity(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		final UUID uuid = player.getUniqueId();

		Entity entity = event.getCaught();
		if ((entity instanceof ArmorStand armorStand) && armorStand.getPersistentDataContainer().get(
				Objects.requireNonNull(NamespacedKey.fromString("lavafishing", instance)),
				PersistentDataType.STRING) != null) {
			// The hook is hooked into the temp entity
			// This might be called both not in game and in game
			LavaFishingEvent lavaFishingEvent = new LavaFishingEvent(player, LavaFishingEvent.State.REEL_IN,
					event.getHook());
			Bukkit.getPluginManager().callEvent(lavaFishingEvent);
			if (lavaFishingEvent.isCancelled()) {
				event.setCancelled(true);
				return;
			}

			// not in game
			HookCheckTimerTask task = hookCheckMap.get(uuid);
			if (task != null)
				task.destroy();
			else
				// should not reach this but in case
				entity.remove();
			return;
		}

		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			RtagItem tagItem = new RtagItem(itemStack);
			if (checkMaxDurability(tagItem)) {
				event.getHook().remove();
				event.setCancelled(true);
				ItemUtils.decreaseDurability(player, itemStack, 5, true);
			}
		}
	}

	/**
	 * Handle the event when a player catches a fish.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onCaughtFish(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		final UUID uuid = player.getUniqueId();
		if (!(event.getCaught() instanceof Item item))
			return;

		// If player is not playing the game
		var temp = this.getTempFishingState(uuid);
		if (temp != null) {
			var loot = temp.getLoot();
			if (loot.getID().equals("vanilla")) {
				// put vanilla loot in map
				this.vanillaLootMap.put(uuid, Pair.of(item.getItemStack(), event.getExpToDrop()));
			}
			var fishingPreparation = temp.getPreparation();
			instance.getGlobalSettings().triggerLootActions(ActionTrigger.HOOK, fishingPreparation);
			loot.triggerActions(ActionTrigger.HOOK, fishingPreparation);
			fishingPreparation.triggerActions(ActionTrigger.HOOK);
			// remove temp state if fishing game not exists
			this.removeTempFishingState(player);
			var hook = event.getHook();
			// If the game is disabled, then do success actions
			success(temp, hook);
			// Cancel the event because loots can be multiple and unique
			event.setCancelled(true);
			hook.remove();
			return;
		}
	}

	/**
	 * Handle the event when a player receives a bite on their fishing hook.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onBite(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		final UUID uuid = player.getUniqueId();

		// If the loot's game is instant
		TempFishingState temp = getTempFishingState(uuid);
		if (temp != null) {
			var loot = temp.getLoot();
			var fishingPreparation = temp.getPreparation();
			fishingPreparation.setLocation(event.getHook().getLocation());
			instance.getGlobalSettings().triggerLootActions(ActionTrigger.BITE, fishingPreparation);
			loot.triggerActions(ActionTrigger.BITE, fishingPreparation);
			fishingPreparation.triggerActions(ActionTrigger.BITE);
		}
	}

	/**
	 * Handle the event when a player reels in their fishing line.
	 *
	 * @param event The PlayerFishEvent that occurred.
	 */
	private void onReelIn(PlayerFishEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName())) {
			return;
		}
		final UUID uuid = player.getUniqueId();

		// If player is lava fishing
		HookCheckTimerTask hookTask = hookCheckMap.get(uuid);
		if (hookTask != null && hookTask.isFishHooked()) {
			LavaFishingEvent lavaFishingEvent = new LavaFishingEvent(player, LavaFishingEvent.State.CAUGHT_FISH,
					event.getHook());
			Bukkit.getPluginManager().callEvent(lavaFishingEvent);
			if (lavaFishingEvent.isCancelled()) {
				event.setCancelled(true);
				return;
			}

			var temp = getTempFishingState(uuid);
			if (temp != null) {
				Loot loot = temp.getLoot();
				var fishingPreparation = temp.getPreparation();
				instance.getGlobalSettings().triggerLootActions(ActionTrigger.HOOK, fishingPreparation);
				loot.triggerActions(ActionTrigger.HOOK, fishingPreparation);
				fishingPreparation.triggerActions(ActionTrigger.HOOK);
				success(temp, event.getHook());
			}
			return;
		}
	}

	/**
	 * Removes the temporary fishing state associated with a player.
	 *
	 * @param player The player whose temporary fishing state should be removed.
	 */
	@Override
	public TempFishingState removeTempFishingState(Player player) {
		return this.tempFishingStateMap.remove(player.getUniqueId());
	}

	public void fail(TempFishingState state, FishHook hook) {
		var loot = state.getLoot();
		var fishingPreparation = state.getPreparation();

		if (loot.getID().equals("vanilla")) {
			Pair<ItemStack, Integer> pair = this.vanillaLootMap.remove(fishingPreparation.getPlayer().getUniqueId());
			if (pair != null) {
				fishingPreparation.insertArg("{nick}",
						"<lang:item.minecraft." + pair.left().getType().toString().toLowerCase() + ">");
				fishingPreparation.insertArg("{loot}", pair.left().getType().toString());
			}
		}

		// call event
		FishingResultEvent fishingResultEvent = new FishingResultEvent(fishingPreparation.getPlayer(),
				FishingResultEvent.Result.FAILURE, hook, loot, fishingPreparation.getArgs());
		Bukkit.getPluginManager().callEvent(fishingResultEvent);
		if (fishingResultEvent.isCancelled()) {
			return;
		}

		instance.getGlobalSettings().triggerLootActions(ActionTrigger.FAILURE, fishingPreparation);
		loot.triggerActions(ActionTrigger.FAILURE, fishingPreparation);
		fishingPreparation.triggerActions(ActionTrigger.FAILURE);

		if (state.getPreparation().getPlayer().getGameMode() != GameMode.CREATIVE) {
			ItemUtils.decreaseHookDurability(fishingPreparation.getRodItemStack(), 1, true);
		}
	}

	/**
	 * Handle the success of a fishing attempt, including spawning loot, calling
	 * events, and executing success actions.
	 *
	 * @param state The temporary fishing state containing information about the
	 *              loot and effect.
	 * @param hook  The FishHook entity associated with the fishing attempt.
	 */
	public void success(TempFishingState state, FishHook hook) {
		var loot = state.getLoot();
		var effect = state.getEffect();
		var fishingPreparation = state.getPreparation();
		var player = fishingPreparation.getPlayer();
		fishingPreparation.insertArg("{size-multiplier}", String.valueOf(effect.getSizeMultiplier()));
		fishingPreparation.insertArg("{size-fixed}", String.valueOf(effect.getSize()));
		int amount;
		if (loot.getType() == LootType.ITEM) {
			amount = (int) effect.getMultipleLootChance();
			amount += Math.random() < (effect.getMultipleLootChance() - amount) ? 2 : 1;
		} else {
			amount = 1;
		}
		fishingPreparation.insertArg("{amount}", String.valueOf(amount));

		// call event
		FishingResultEvent fishingResultEvent = new FishingResultEvent(player, FishingResultEvent.Result.SUCCESS, hook,
				loot, fishingPreparation.getArgs());
		Bukkit.getPluginManager().callEvent(fishingResultEvent);
		if (fishingResultEvent.isCancelled()) {
			return;
		}

		switch (loot.getType()) {
		case ITEM -> {
			// build the items for multiple times instead of using setAmount() to make sure
			// that each item is unique
			if (loot.getID().equals("vanilla")) {
				Pair<ItemStack, Integer> pair = vanillaLootMap.remove(player.getUniqueId());
				if (pair != null) {
					fishingPreparation.insertArg("{nick}",
							"<lang:item.minecraft." + pair.left().getType().toString().toLowerCase() + ">");
					for (int i = 0; i < amount; i++) {
						instance.getScheduler().runTaskSyncLater(() -> {
							instance.getItemManager().dropItem(player, hook.getLocation(), player.getLocation(),
									pair.left().clone(), fishingPreparation);
							doSuccessActions(loot, fishingPreparation, player);
							if (pair.right() > 0) {
								player.giveExp(pair.right().intValue());
								instance.getAdventureManager().sendSound(player, Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.experience_orb.pickup"), 1,
										1);
							}
						}, hook.getLocation(), (long) HBConfig.multipleLootSpawnDelay * i);
					}
				}
			} else {
				for (int i = 0; i < amount; i++) {
					instance.getScheduler().runTaskSyncLater(() -> {
						ItemStack item = instance.getItemManager().build(player, "item", loot.getID(),
								fishingPreparation.getArgs());
						if (item == null) {
							LogUtils.warn(String.format("Item %s does not exist.", loot.getID()));
							return;
						}
						instance.getItemManager().dropItem(player, hook.getLocation(), player.getLocation(), item,
								fishingPreparation);
						doSuccessActions(loot, fishingPreparation, player);
					}, hook.getLocation(), (long) HBConfig.multipleLootSpawnDelay * i);
				}
			}
		}
		case ENTITY -> {
			instance.getEntityManager().summonEntity(hook.getLocation(), player.getLocation(), loot);
			doSuccessActions(loot, fishingPreparation, player);
		}
		case BLOCK -> {
			instance.getBlockManager().summonBlock(player, hook.getLocation(), player.getLocation(), loot);
			doSuccessActions(loot, fishingPreparation, player);
		}
		}

		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack rod = state.getPreparation().getRodItemStack();
			ItemUtils.decreaseHookDurability(rod, 1, false);
			ItemUtils.decreaseDurability(player, rod, 1, true);
		}
	}

	/**
	 * Execute success-related actions after a successful fishing attempt, including
	 * updating competition data, triggering events and actions, and updating player
	 * statistics.
	 *
	 * @param loot               The loot that was successfully caught.
	 * @param fishingPreparation The fishing preparation containing preparation
	 *                           data.
	 * @param player             The player who successfully caught the loot.
	 */
	private void doSuccessActions(Loot loot, FishingPreparation fishingPreparation, Player player) {
		// events and actions
		instance.getGlobalSettings().triggerLootActions(ActionTrigger.SUCCESS, fishingPreparation);
		loot.triggerActions(ActionTrigger.SUCCESS, fishingPreparation);
		fishingPreparation.triggerActions(ActionTrigger.SUCCESS);
		player.setStatistic(Statistic.FISH_CAUGHT, player.getStatistic(Statistic.FISH_CAUGHT) + 1);
	}

	/**
	 * Checks if a player with the given UUID has cast their fishing hook.
	 *
	 * @param uuid The UUID of the player to check.
	 * @return {@code true} if the player has cast their fishing hook, {@code false}
	 *         otherwise.
	 */
	@Override
	public boolean hasPlayerCastHook(UUID uuid) {
		FishHook fishHook = hookCacheMap.get(uuid);
		if (fishHook == null)
			return false;
		if (!fishHook.isValid()) {
			hookCacheMap.remove(uuid);
			return false;
		}
		return true;
	}

	/**
	 * Sets the temporary fishing state for a player.
	 *
	 * @param player           The player for whom to set the temporary fishing
	 *                         state.
	 * @param tempFishingState The temporary fishing state to set for the player.
	 */
	@Override
	public void setTempFishingState(Player player, TempFishingState tempFishingState) {
		tempFishingStateMap.put(player.getUniqueId(), tempFishingState);
	}

	public void removeHookCheckTask(Player player) {
		hookCheckMap.remove(player.getUniqueId());
	}

	/**
	 * Gets the {@link TempFishingState} object associated with the given UUID.
	 *
	 * @param uuid The UUID of the player.
	 * @return The {@link TempFishingState} object if found, or {@code null} if not
	 *         found.
	 */
	@Override
	@Nullable
	public TempFishingState getTempFishingState(UUID uuid) {
		return tempFishingStateMap.get(uuid);
	}
}