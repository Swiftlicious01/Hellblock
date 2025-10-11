package com.swiftlicious.hellblock.mechanics.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.events.fishing.FishingEffectApplyEvent;
import com.swiftlicious.hellblock.events.fishing.FishingHookStateEvent;
import com.swiftlicious.hellblock.events.fishing.FishingLootSpawnEvent;
import com.swiftlicious.hellblock.events.fishing.FishingResultEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.fishing.BaitAnimationTask;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.mechanics.fishing.hook.HookMechanic;
import com.swiftlicious.hellblock.mechanics.fishing.hook.LavaFishingMechanic;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TriFunction;

/**
 * Represents a custom fishing hook.
 */
public class CustomFishingHook {

	protected final HellblockPlugin instance;
	private final FishHook hook;
	private SchedulerTask task;
	private final FishingGears gears;
	private final Context<Player> context;
	private Effect tempFinalEffect;
	private HookMechanic hookMechanic;
	private Loot nextLoot;
	private BaitAnimationTask baitAnimationTask;
	private boolean valid = true;

	private static TriFunction<FishHook, Context<Player>, Effect, List<HookMechanic>> mechanicProviders = defaultMechanicProviders();

	/**
	 * Provides the default mechanic providers for the custom fishing hook.
	 *
	 * @return a TriFunction that provides a list of HookMechanic instances.
	 */
	public static TriFunction<FishHook, Context<Player>, Effect, List<HookMechanic>> defaultMechanicProviders() {
		return (h, c, e) -> {
			final List<HookMechanic> mechanics = new ArrayList<>();
			if (HellblockPlugin.getInstance().getConfigManager().lavaFishingEnabled()) {
				mechanics.add(new LavaFishingMechanic(h, e, c));
			}
			return mechanics;
		};
	}

	/**
	 * Sets the mechanic providers for the custom fishing hook.
	 *
	 * @param mechanicProviders the TriFunction to set.
	 */
	public static void mechanicProviders(
			TriFunction<FishHook, Context<Player>, Effect, List<HookMechanic>> mechanicProviders) {
		CustomFishingHook.mechanicProviders = mechanicProviders;
	}

	/**
	 * Constructs a new CustomFishingHook.
	 *
	 * @param plugin  the HellblockPlugin instance.
	 * @param hook    the FishHook entity.
	 * @param gears   the FishingGears instance.
	 * @param context the context of the player.
	 */
	public CustomFishingHook(HellblockPlugin plugin, FishHook hook, FishingGears gears, Context<Player> context) {
		this.instance = plugin;
		this.hook = hook;
		this.gears = gears;
		// once it becomes a custom hook, the wait time is controlled by plugin
		this.context = context;
	}

	public void init() {
		// enable bait animation
		if (instance.getConfigManager().baitAnimation() && !gears.getItem(FishingGears.GearType.BAIT).isEmpty()) {
			this.baitAnimationTask = new BaitAnimationTask(instance, context.holder(), hook,
					gears.getItem(FishingGears.GearType.BAIT).get(0).right());
		}

		this.gears.trigger(ActionTrigger.CAST, context);

		final Effect effect = EffectInterface.newInstance();

		// The effects impact mechanism at this stage
		gears.effectModifiers()
				.forEach(modifier -> modifier.modifiers().forEach(consumer -> consumer.accept(effect, context, 0)));

		// Fire event
		EventUtils.fireAndForget(new FishingEffectApplyEvent(this, effect, FishingEffectApplyEvent.Stage.CAST));

		final List<HookMechanic> enabledMechanics = mechanicProviders.apply(hook, context, effect);

		this.task = instance.getScheduler().sync().runRepeating(() -> tickHook(enabledMechanics, effect), 1, 1,
				hook.getLocation());
	}

	private void tickHook(List<HookMechanic> enabledMechanics, Effect effect) {
		// destroy if hook is invalid
		if (!hook.isValid()) {
			instance.getFishingManager().destroyHook(context.holder().getUniqueId());
			return;
		}

		if (this.hookMechanic != null && this.hookMechanic.shouldStop()) {
			this.hookMechanic.destroy();
			this.hookMechanic = null;
		}

		enabledMechanics.forEach(mechanic -> {
			final boolean startCondition = mechanic.canStart() && this.hookMechanic != mechanic;
			// find the first available mechanic
			if (startCondition) {
				if (this.hookMechanic != null) {
					this.hookMechanic.destroy();
				}
				this.hookMechanic = mechanic;

				// remove bait animation if there exists
				if (this.baitAnimationTask != null) {
					this.baitAnimationTask.cancelAnimation();
					this.baitAnimationTask = null;
				}

				// to update some properties
				mechanic.preStart();
				final Effect tempEffect = effect.copy();

				gears.effectModifiers().forEach(
						modifier -> modifier.modifiers().forEach(consumer -> consumer.accept(tempEffect, context, 1)));

				// trigger event
				EventUtils.fireAndForget(
						new FishingEffectApplyEvent(this, tempEffect, FishingEffectApplyEvent.Stage.LOOT));

				context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
				context.arg(ContextKeys.OTHER_X, hook.getLocation().getBlockX());
				context.arg(ContextKeys.OTHER_Y, hook.getLocation().getBlockY());
				context.arg(ContextKeys.OTHER_Z, hook.getLocation().getBlockZ());

				// get the next loot
				Loot loot;
				try {
					loot = instance.getLootManager().getNextLoot(tempEffect, context);
				} catch (Exception e) {
					loot = null;
					instance.getPluginLogger().warn("Error occurred when getting next loot.", e);
				}
				if (loot != null) {
					this.nextLoot = loot;

					context.arg(ContextKeys.ID, loot.id());
					context.arg(ContextKeys.NICK, loot.nick());
					context.arg(ContextKeys.LOOT, loot.type());

					context.clearCustomData();
					loot.customData().entrySet()
							.forEach(entry -> context.arg(ContextKeys.of("data_" + entry.getKey(), String.class),
									entry.getValue().render(context)));

					instance.debug("Next Loot: " + loot.id());
					instance.debug(context);
					// get its basic properties
					final Effect baseEffect = loot.baseEffect().toEffect(context);
					instance.debug("Loot Base Effect:" + baseEffect);
					tempEffect.combine(baseEffect);
					// apply the gears' effects
					gears.effectModifiers().forEach(modifier -> modifier.modifiers()
							.forEach(consumer -> consumer.accept(tempEffect, context, 2)));

					// trigger event
					EventUtils.fireAndForget(
							new FishingEffectApplyEvent(this, tempEffect, FishingEffectApplyEvent.Stage.FISHING));
					context.arg(ContextKeys.EFFECT, tempEffect);

					// start the mechanic
					instance.debug("Final Effect:" + tempEffect);
					mechanic.start(tempEffect);

					this.tempFinalEffect = tempEffect;
				} else {
					mechanic.start(tempEffect);
					this.tempFinalEffect = tempEffect;
					// to prevent players from getting any loot
					mechanic.freeze();
				}
			}
		});
	}

	/**
	 * stops the custom fishing hook. In most cases, you should use
	 * {@link CustomFishingHook#destroy()} instead
	 */
	@ApiStatus.Internal
	public void stop() {
		if (!this.valid) {
			return;
		}
		this.valid = false;
		if (!this.task.isCancelled()) {
			this.task.cancel();
		}
		if (this.hook.isValid()) {
			this.hook.remove();
		}
		if (this.hookMechanic != null) {
			hookMechanic.destroy();
		}
		if (this.baitAnimationTask == null) {
			return;
		}
		this.baitAnimationTask.cancelAnimation();
		this.baitAnimationTask = null;
	}

	/**
	 * Ends the life of the custom fishing hook.
	 */
	public void destroy() {
		// if the hook exists in cache
		this.instance.getFishingManager().destroyHook(this.context.holder().getUniqueId());
		// if not, then destroy the tasks. This should never happen
		if (this.valid) {
			stop();
		}
	}

	/**
	 * Gets the context of the player.
	 *
	 * @return the context.
	 */
	public Context<Player> getContext() {
		return this.context;
	}

	/**
	 * Gets the FishHook entity.
	 *
	 * @return the FishHook entity.
	 */
	@NotNull
	public FishHook getHookEntity() {
		return this.hook;
	}

	/**
	 * Gets the current hook mechanic.
	 *
	 * @return the current HookMechanic, or null if none.
	 */
	@Nullable
	public HookMechanic getCurrentHookMechanic() {
		return hookMechanic;
	}

	public FishingGears gears() {
		return gears;
	}

	/**
	 * Gets the next loot.
	 *
	 * @return the next Loot, or null if none.
	 */
	@Nullable
	public Loot getNextLoot() {
		return nextLoot;
	}

	public boolean isHookValid() {
		return hook == null ? false : hook.isValid() && valid;
	}

	/**
	 * Starts a fishing action.
	 */
	public void startFishing() {
		if (!hook.isValid()) {
			return;
		}

		handleSuccessfulFishing();
		destroy();
	}

	// auto fishing
	private void scheduleNextFishing() {
		final Player player = context.holder();
		instance.getScheduler().sync().runLater(() -> {
			if (player.isOnline()) {
				final ItemStack item = player.getInventory()
						.getItem(gears.getRodSlot() == HandSlot.MAIN ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
				if (item.getType() == Material.FISHING_ROD) {
					VersionHelper.getNMSManager().useItem(player, gears.getRodSlot(), item);
					VersionHelper.getNMSManager().swingHand(context.holder(), gears.getRodSlot());
				}
			}
		}, 20, player.getLocation());
	}

	/**
	 * Handles the reel-in action.
	 */
	public void onReelIn() {
		if (!hook.isValid()) {
			return;
		}
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		if (hookMechanic != null) {
			if (!hookMechanic.isHooked()) {
				gears.trigger(ActionTrigger.REEL, context);
				destroy();
			} else {
				EventUtils.fireAndForget(
						new FishingHookStateEvent(context.holder(), hook, FishingHookStateEvent.State.HOOK));
				instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.HOOK);
				gears.trigger(ActionTrigger.HOOK, context);
				startFishing();
			}
		} else {
			gears.trigger(ActionTrigger.REEL, context);
			destroy();
		}
	}

	/**
	 * Handles the bite action.
	 */
	public void onBite() {
		if (!hook.isValid()) {
			return;
		}
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.BITE);
		gears.trigger(ActionTrigger.BITE, context);
		if (!RequirementManager.isSatisfied(context, instance.getConfigManager().autoFishingRequirements())) {
			return;
		}
		handleSuccessfulFishing();
		VersionHelper.getNMSManager().swingHand(context.holder(), gears.getRodSlot());
		destroy();
		scheduleNextFishing();
		return;
	}

	/**
	 * Handles the landing action.
	 */
	public void onLand() {
		if (!hook.isValid()) {
			return;
		}
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		gears.trigger(ActionTrigger.LAND, context);
	}

	/**
	 * Handles the escape action.
	 */
	public void onEscape() {
		if (!hook.isValid()) {
			return;
		}
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.ESCAPE);
		gears.trigger(ActionTrigger.ESCAPE, context);
	}

	/**
	 * Handles the lure action.
	 */
	public void onLure() {
		if (!hook.isValid()) {
			return;
		}
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.LURE);
		gears.trigger(ActionTrigger.LURE, context);
	}

	/**
	 * Handles a failed fishing attempt.
	 */
	public void handleFailedFishing() {

		if (!valid) {
			return;
		}

		// update the hook location
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		context.arg(ContextKeys.OTHER_X, hook.getLocation().getBlockX());
		context.arg(ContextKeys.OTHER_Y, hook.getLocation().getBlockY());
		context.arg(ContextKeys.OTHER_Z, hook.getLocation().getBlockZ());

		final FishingResultEvent event = new FishingResultEvent(context, FishingResultEvent.Result.FAILURE, hook,
				nextLoot);
		if (EventUtils.fireAndCheckCancel(event)) {
			return;
		}

		gears.trigger(ActionTrigger.FAILURE, context);
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.FAILURE);
	}

	/**
	 * Handles a successful fishing attempt.
	 */
	@SuppressWarnings("removal")
	public void handleSuccessfulFishing() {

		if (!valid) {
			return;
		}

		// update the hook location
		final Location hookLocation = hook.getLocation();
		context.arg(ContextKeys.OTHER_LOCATION, hookLocation);
		context.arg(ContextKeys.OTHER_X, hookLocation.getBlockX());
		context.arg(ContextKeys.OTHER_Y, hookLocation.getBlockY());
		context.arg(ContextKeys.OTHER_Z, hookLocation.getBlockZ());

		final LootType lootType = context.arg(ContextKeys.LOOT);
		Objects.requireNonNull(lootType, "Missing loot type");
		Objects.requireNonNull(tempFinalEffect, "Missing final effects");

		int amount;
		if (lootType == LootType.ITEM) {
			amount = (int) tempFinalEffect.multipleLootChance();
			amount += Math.random() < (tempFinalEffect.multipleLootChance() - amount) ? 2 : 1;
		} else {
			amount = 1;
		}
		// set the amount of loot
		context.arg(ContextKeys.AMOUNT, amount);

		final FishingResultEvent event = new FishingResultEvent(context, FishingResultEvent.Result.SUCCESS, hook,
				nextLoot);
		if (EventUtils.fireAndCheckCancel(event)) {
			return;
		}

		amount = event.getAmount();

		gears.trigger(ActionTrigger.SUCCESS, context);

		switch (lootType) {
		case ITEM -> {
			context.arg(ContextKeys.SIZE_MULTIPLIER, tempFinalEffect.sizeMultiplier());
			context.arg(ContextKeys.SIZE_ADDER, tempFinalEffect.sizeAdder());
			final boolean directlyToInventory = nextLoot.toInventory().evaluate(context) != 0;
			for (int i = 0; i < amount; i++) {
				final int order = i;
				instance.getScheduler().sync().runLater(() -> {
					context.arg(ContextKeys.LOOT_ORDER, order);
					if (directlyToInventory) {
						final ItemStack stack = instance.getItemManager().getItemLoot(context,
								gears.getItem(FishingGears.GearType.ROD).stream().findAny().orElseThrow().right(),
								hook);
						if (stack.getType() != Material.AIR) {
							if (Objects.equals(context.arg(ContextKeys.NICK), "UNDEFINED")) {
								final Optional<String> displayName = instance.getItemManager().wrap(stack)
										.displayName();
								if (displayName.isPresent()) {
									context.arg(ContextKeys.NICK, AdventureHelper.jsonToMiniMessage(displayName.get()));
								} else {
									context.arg(ContextKeys.NICK, "<lang:" + stack.getType().getTranslationKey() + ">");
								}
							}
							PlayerUtils.giveItem(context.holder(), stack, stack.getAmount());
						}
						instance.getStorageManager().getOnlineUser(context.holder().getUniqueId())
								.ifPresent(user -> instance.getChallengeManager()
										.handleChallengeProgression(context.holder(), ActionType.FISH, stack));
					} else {
						final Item item = instance.getItemManager().dropItemLoot(context,
								gears.getItem(FishingGears.GearType.ROD).stream().findAny().orElseThrow().right(),
								hook);
						if (item != null && Objects.equals(context.arg(ContextKeys.NICK), "UNDEFINED")) {
							final ItemStack stack = item.getItemStack();
							final Optional<String> displayName = instance.getItemManager().wrap(stack).displayName();
							if (displayName.isPresent()) {
								context.arg(ContextKeys.NICK, AdventureHelper.jsonToMiniMessage(displayName.get()));
							} else {
								context.arg(ContextKeys.NICK, "<lang:" + stack.getType().getTranslationKey() + ">");
							}
						}
						if (item != null) {
							final FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hookLocation,
									nextLoot, item);
							Bukkit.getPluginManager().callEvent(spawnEvent);
							if (!spawnEvent.summonEntity()) {
								item.remove();
							}
							if (spawnEvent.skipActions()) {
								return;
							}
							if (item.isValid() && nextLoot.preventGrabbing()) {
								item.getPersistentDataContainer().set(
										Objects.requireNonNull(NamespacedKey.fromString("owner", instance)),
										PersistentDataType.STRING, context.holder().getName());
							}
						}
						instance.getStorageManager().getOnlineUser(context.holder().getUniqueId())
								.ifPresent(user -> instance.getChallengeManager()
										.handleChallengeProgression(context.holder(), ActionType.FISH, item));
					}
					doSuccessActions();
				}, (long) instance.getConfigManager().multipleLootSpawnDelay() * i, hookLocation);
			}
		}
		case BLOCK -> {
			context.arg(ContextKeys.LOOT_ORDER, 1);
			final FallingBlock fallingBlock = instance.getBlockManager().summonBlockLoot(context);
			final FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hook.getLocation(), nextLoot,
					fallingBlock);
			Bukkit.getPluginManager().callEvent(spawnEvent);
			if (!spawnEvent.summonEntity()) {
				fallingBlock.remove();
			}
			if (spawnEvent.skipActions()) {
				return;
			}
			instance.getStorageManager().getOnlineUser(context.holder().getUniqueId())
					.ifPresent(user -> instance.getChallengeManager().handleChallengeProgression(context.holder(),
							ActionType.FISH, fallingBlock.getBlockData()));
			doSuccessActions();
		}
		case ENTITY -> {
			context.arg(ContextKeys.LOOT_ORDER, 1);
			final Entity entity = instance.getEntityManager().summonEntityLoot(context);
			final FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hook.getLocation(), nextLoot,
					entity);
			Bukkit.getPluginManager().callEvent(spawnEvent);
			if (!spawnEvent.summonEntity()) {
				entity.remove();
			}
			if (spawnEvent.skipActions()) {
				return;
			}
			instance.getStorageManager().getOnlineUser(context.holder().getUniqueId()).ifPresent(user -> instance
					.getChallengeManager().handleChallengeProgression(context.holder(), ActionType.FISH, entity));
			doSuccessActions();
		}
		}
	}

	private void doSuccessActions() {
		final String id = context.arg(ContextKeys.ID);
		final Player player = context.holder();

		if (!nextLoot.disableStats()) {
			instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
				final Pair<Integer, Integer> result = userData.getStatisticData()
						.addAmount(nextLoot.statisticKey().amountKey(), 1);
				userData.getStatisticData().addAmount(nextLoot.statisticKey().amountKey(), 1);
				context.arg(ContextKeys.TOTAL_AMOUNT,
						userData.getStatisticData().getAmount(nextLoot.statisticKey().amountKey()));
				Optional.ofNullable(context.arg(ContextKeys.SIZE)).ifPresentOrElse(size -> {
					final float currentRecord = userData.getStatisticData()
							.getMaxSize(nextLoot.statisticKey().sizeKey());
					final float max = Math.max(size, currentRecord);
					context.arg(ContextKeys.RECORD, max);
					context.arg(ContextKeys.PREVIOUS_RECORD, currentRecord);
					context.arg(ContextKeys.RECORD_FORMATTED, "%.2f".formatted(max));
					context.arg(ContextKeys.PREVIOUS_RECORD_FORMATTED, "%.2f".formatted(currentRecord));
					if (userData.getStatisticData().updateSize(nextLoot.statisticKey().sizeKey(), size)) {
						context.arg(ContextKeys.IS_NEW_SIZE_RECORD, true);
						instance.getEventManager().trigger(context, id, MechanicType.LOOT, ActionTrigger.SUCCESS,
								result.left(), result.right());
						instance.getEventManager().trigger(context, id, MechanicType.LOOT,
								ActionTrigger.NEW_SIZE_RECORD);
					}
				}, () -> instance.getEventManager().trigger(context, id, MechanicType.LOOT, ActionTrigger.SUCCESS,
						result.left(), result.right()));
			});
		}

		instance.getEventManager().trigger(context, id, MechanicType.LOOT, ActionTrigger.SUCCESS);
		player.setStatistic(Statistic.FISH_CAUGHT, player.getStatistic(Statistic.FISH_CAUGHT) + 1);
	}
}