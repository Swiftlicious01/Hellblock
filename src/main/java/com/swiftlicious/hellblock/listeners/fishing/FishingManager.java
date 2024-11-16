package com.swiftlicious.hellblock.listeners.fishing;

import java.util.Objects;
import java.util.Optional;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.events.fishing.FishingResultEvent;
import com.swiftlicious.hellblock.events.fishing.LavaFishingEvent;
import com.swiftlicious.hellblock.events.fishing.RodCastEvent;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.sound.Sound;

public class FishingManager implements Listener, FishingManagerInterface {

	protected final HellblockPlugin instance;
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

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
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

	@Override
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

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final UUID uuid = player.getUniqueId();
		this.removeHook(uuid);
		this.removeTempFishingState(player);
		this.removeHookCheckTask(player);
		this.vanillaLootMap.remove(uuid);
	}

	public boolean checkMaxDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("FishingData", "max_dur");
	}

	public int getMaxDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return -1;

		return new RtagItem(item).getOptional("FishingData", "max_dur").asInt();
	}

	public boolean checkCurrentDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("FishingData", "cur_dur");
	}

	public int getCurrentDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return -1;

		return new RtagItem(item).getOptional("FishingData", "cur_dur").asInt();
	}

	public boolean checkFishingType(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("FishingData", "type");
	}

	public @Nullable String getFishingType(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return new RtagItem(item).getOptional("FishingData", "type").asString();
	}

	public boolean checkFishingID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("FishingData", "id");
	}

	public @Nullable String getFishingID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return new RtagItem(item).getOptional("FishingData", "id").asString();
	}

	public @Nullable ItemStack setMaxDurability(@Nullable ItemStack item, int data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "FishingData", "max_dur");
		});
	}

	public @Nullable ItemStack setCurrentDurability(@Nullable ItemStack item, int data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "FishingData", "cur_dur");
		});
	}

	public @Nullable ItemStack setFishingType(@Nullable ItemStack item, @Nullable String data) {
		if (item == null || item.getType() == Material.AIR || data == null || data.isEmpty())
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "FishingData", "type");
		});
	}

	public @Nullable ItemStack setFishingID(@Nullable ItemStack item, @Nullable String data) {
		if (item == null || item.getType() == Material.AIR || data == null || data.isEmpty())
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "FishingData", "id");
		});
	}

	public boolean removeFishingData(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).remove("FishingData");
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
			instance.getScheduler().executeSync(hook::remove, hook.getLocation());
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
			return;
		}
		FishHook hook = event.getHook();
		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			if (itemStack.getType() == Material.FISHING_ROD) {
				if (checkMaxDurability(itemStack)) {
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
			return;
		}
		FishHook hook = event.getHook();
		if (player.getGameMode() != GameMode.CREATIVE) {
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			if (itemStack.getType() != Material.FISHING_ROD)
				itemStack = player.getInventory().getItemInOffHand();
			if (itemStack.getType() == Material.FISHING_ROD) {
				if (checkMaxDurability(itemStack)) {
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
			return;
		}
		var fishingPreparation = new FishingPreparation(player, instance);
		if (!fishingPreparation.canFish()) {
			event.setCancelled(true);
			return;
		}
		// Check mechanic requirements
		if (!RequirementManagerInterface.isRequirementMet(fishingPreparation,
				instance.getRequirementManager().fishingRequirements)) {
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
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
			if (checkMaxDurability(itemStack)) {
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
			return;
		}
		final UUID uuid = player.getUniqueId();
		if (!(event.getCaught() instanceof Item item))
			return;

		var temp = this.getTempFishingState(uuid);
		if (temp != null) {
			var loot = temp.getLoot();
			if (loot.getID().equals("vanilla")) {
				// put vanilla loot in map
				this.vanillaLootMap.put(uuid, Pair.of(item.getItemStack(), event.getExpToDrop()));
			}
			var fishingPreparation = temp.getPreparation();
			if (!loot.disableGlobalAction())
				instance.getGlobalSettings().triggerLootActions(ActionTrigger.HOOK, fishingPreparation);
			loot.triggerActions(ActionTrigger.HOOK, fishingPreparation);
			fishingPreparation.triggerActions(ActionTrigger.HOOK);
			// remove temp state if fishing game not exists
			this.removeTempFishingState(player);
			var hook = event.getHook();
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
			return;
		}
		final UUID uuid = player.getUniqueId();

		// If the loot's game is instant
		TempFishingState temp = getTempFishingState(uuid);
		if (temp != null) {
			var loot = temp.getLoot();
			var fishingPreparation = temp.getPreparation();
			fishingPreparation.setLocation(event.getHook().getLocation());
			if (!loot.disableGlobalAction())
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
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName)) {
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
				if (!loot.disableGlobalAction())
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

		if (!loot.disableGlobalAction())
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
						instance.getScheduler().sync().runLater(() -> {
							instance.getItemManager().dropItem(player, hook.getLocation(), player.getLocation(),
									pair.left().clone(), fishingPreparation);
							doSuccessActions(loot, fishingPreparation, player);
							if (pair.right() > 0) {
								player.giveExp(pair.right().intValue());
								instance.getAdventureManager().sendSound(player, Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.experience_orb.pickup"), 1,
										1);
							}
						}, (long) HBConfig.multipleLootSpawnDelay * i, hook.getLocation());
					}
				}
			} else {
				for (int i = 0; i < amount; i++) {
					instance.getScheduler().sync().runLater(() -> {
						ItemStack item = instance.getItemManager().build(player, "item", loot.getID(),
								fishingPreparation.getArgs());
						if (item == null) {
							LogUtils.warn(String.format("Item %s does not exist.", loot.getID()));
							return;
						}
						instance.getItemManager().dropItem(player, hook.getLocation(), player.getLocation(), item,
								fishingPreparation);
						doSuccessActions(loot, fishingPreparation, player);
					}, (long) HBConfig.multipleLootSpawnDelay * i, hook.getLocation());
				}
			}

			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
				return;
			if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.LAVA_FISHING_CHALLENGE)
					&& !onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)) {
				onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
						ChallengeType.LAVA_FISHING_CHALLENGE);
			} else {
				onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
						ChallengeType.LAVA_FISHING_CHALLENGE, 1);
				if (onlineUser.get().getChallengeData().isChallengeCompleted(ChallengeType.LAVA_FISHING_CHALLENGE)) {
					onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
							ChallengeType.LAVA_FISHING_CHALLENGE);
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
		if (!loot.disableGlobalAction())
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