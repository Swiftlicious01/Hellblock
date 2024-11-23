package com.swiftlicious.hellblock.mechanics.fishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.swiftlicious.hellblock.effects.Effect;
import com.swiftlicious.hellblock.effects.EffectInterface;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.events.fishing.FishingEffectApplyEvent;
import com.swiftlicious.hellblock.events.fishing.FishingLootSpawnEvent;
import com.swiftlicious.hellblock.events.fishing.FishingResultEvent;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.listeners.fishing.BaitAnimationTask;
import com.swiftlicious.hellblock.loot.LootInterface;
import com.swiftlicious.hellblock.loot.LootType;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.mechanics.fishing.hook.HookMechanic;
import com.swiftlicious.hellblock.mechanics.fishing.hook.LavaFishingMechanic;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;
import com.swiftlicious.hellblock.utils.extras.TriFunction;

/**
 * Represents a custom fishing hook.
 */
public class CustomFishingHook {

	protected final HellblockPlugin instance;
	private final FishHook hook;
	private final SchedulerTask task;
	private final FishingGears gears;
	private final Context<Player> context;
	private Effect tempFinalEffect;
	private HookMechanic hookMechanic;
	private LootInterface nextLoot;
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
			List<HookMechanic> mechanics = new ArrayList<>();
			if (HellblockPlugin.getInstance().getConfigManager().lavaFishingEnabled())
				mechanics.add(new LavaFishingMechanic(h, e, c));
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
		this.gears = gears;
		// enable bait animation
		if (instance.getConfigManager().baitAnimation() && !gears.getItem(FishingGears.GearType.BAIT).isEmpty()) {
			this.baitAnimationTask = new BaitAnimationTask(plugin, context.holder(), hook,
					gears.getItem(FishingGears.GearType.BAIT).get(0).right());
		}
		this.gears.trigger(ActionTrigger.CAST, context);
		this.hook = hook;
		// once it becomes a custom hook, the wait time is controlled by plugin
		this.context = context;
		Effect effect = EffectInterface.newInstance();
		// The effects impact mechanism at this stage
		for (EffectModifier modifier : gears.effectModifiers()) {
			for (TriConsumer<Effect, Context<Player>, Integer> consumer : modifier.modifiers()) {
				consumer.accept(effect, context, 0);
			}
		}

		// trigger event
		EventUtils.fireAndForget(new FishingEffectApplyEvent(this, effect, FishingEffectApplyEvent.Stage.CAST));

		List<HookMechanic> enabledMechanics = mechanicProviders.apply(hook, context, effect);
		this.task = plugin.getScheduler().sync().runRepeating(() -> {
			// destroy if hook is invalid
			if (!hook.isValid()) {
				plugin.getFishingManager().destroyHook(context.holder().getUniqueId());
				return;
			}
			if (this.hookMechanic != null) {
				if (this.hookMechanic.shouldStop()) {
					this.hookMechanic.destroy();
					this.hookMechanic = null;
				}
			}
			for (HookMechanic mechanic : enabledMechanics) {
				// find the first available mechanic
				if (mechanic.canStart()) {
					if (this.hookMechanic != mechanic) {
						if (this.hookMechanic != null)
							this.hookMechanic.destroy();
						this.hookMechanic = mechanic;

						// remove bait animation if there exists
						if (this.baitAnimationTask != null) {
							this.baitAnimationTask.cancelAnimation();
							this.baitAnimationTask = null;
						}

						// to update some properties
						mechanic.preStart();
						Effect tempEffect = effect.copy();

						for (EffectModifier modifier : gears.effectModifiers()) {
							for (TriConsumer<Effect, Context<Player>, Integer> consumer : modifier.modifiers()) {
								consumer.accept(tempEffect, context, 1);
							}
						}

						// trigger event
						EventUtils.fireAndForget(
								new FishingEffectApplyEvent(this, tempEffect, FishingEffectApplyEvent.Stage.LOOT));

						context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
						context.arg(ContextKeys.OTHER_X, hook.getLocation().getBlockX());
						context.arg(ContextKeys.OTHER_Y, hook.getLocation().getBlockY());
						context.arg(ContextKeys.OTHER_Z, hook.getLocation().getBlockZ());

						// get the next loot

						LootInterface loot;
						try {
							loot = plugin.getLootManager().getNextLoot(tempEffect, context);
						} catch (Exception e) {
							loot = null;
							plugin.getPluginLogger().warn("Error occurred when getting next loot.", e);
						}
						if (loot != null) {
							this.nextLoot = loot;

							context.arg(ContextKeys.ID, loot.id());
							context.arg(ContextKeys.NICK, loot.nick());
							context.arg(ContextKeys.LOOT, loot.type());

							context.clearCustomData();
							for (Map.Entry<String, TextValue<Player>> entry : loot.customData().entrySet()) {
								context.arg(ContextKeys.of("data_" + entry.getKey(), String.class),
										entry.getValue().render(context));
							}

							plugin.debug("Next loot: " + loot.id());
							plugin.debug(context);
							// get its basic properties
							Effect baseEffect = loot.baseEffect().toEffect(context);
							plugin.debug(baseEffect);
							tempEffect.combine(baseEffect);
							// apply the gears' effects
							for (EffectModifier modifier : gears.effectModifiers()) {
								for (TriConsumer<Effect, Context<Player>, Integer> consumer : modifier.modifiers()) {
									consumer.accept(tempEffect, context, 2);
								}
							}

							// trigger event
							EventUtils.fireAndForget(new FishingEffectApplyEvent(this, tempEffect,
									FishingEffectApplyEvent.Stage.FISHING));

							// start the mechanic
							mechanic.start(tempEffect);

							this.tempFinalEffect = tempEffect;
						} else {
							mechanic.start(tempEffect);
							this.tempFinalEffect = tempEffect;
							// to prevent players from getting any loot
							mechanic.freeze();
						}
					}
				}
			}
		}, 1, 1, hook.getLocation());
	}

	/**
	 * stops the custom fishing hook. In most cases, you should use
	 * {@link CustomFishingHook#destroy()} instead
	 */
	@ApiStatus.Internal
	public void stop() {
		if (!this.valid)
			return;
		this.valid = false;
		if (this.task != null)
			this.task.cancel();
		if (this.hook.isValid())
			this.hook.remove();
		if (this.hookMechanic != null)
			hookMechanic.destroy();
		if (this.baitAnimationTask != null) {
			this.baitAnimationTask.cancelAnimation();
			this.baitAnimationTask = null;
		}
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

	/**
	 * Gets the next loot.
	 *
	 * @return the next Loot, or null if none.
	 */
	@Nullable
	public LootInterface getNextLoot() {
		return nextLoot;
	}

	public boolean isHookValid() {
		if (hook == null)
			return false;
		return hook.isValid() && valid;
	}

	/**
	 * Starts a fishing action.
	 */
	public void startFishing() {
		if (!hook.isValid())
			return;

		handleSuccessfulFishing();
		destroy();
	}

	// auto fishing
	private void scheduleNextFishing() {
		final Player player = context.holder();
		instance.getScheduler().sync().runLater(() -> {
			if (player.isOnline()) {
				ItemStack item = player.getInventory()
						.getItem(gears.getRodSlot() == HandSlot.MAIN ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
				if (item.getType() == Material.FISHING_ROD) {
					instance.getVersionManager().getNMSManager().useItem(player, gears.getRodSlot(), item);
					instance.getVersionManager().getNMSManager().swingHand(context.holder(), gears.getRodSlot());
				}
			}
		}, 20, player.getLocation());
	}

	/**
	 * Handles the reel-in action.
	 */
	public void onReelIn() {
		if (!hook.isValid())
			return;
		if (hookMechanic != null) {
			if (!hookMechanic.isHooked()) {
				gears.trigger(ActionTrigger.REEL, context);
				destroy();
			} else {
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
		if (!hook.isValid())
			return;
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.BITE);
		gears.trigger(ActionTrigger.BITE, context);
		if (RequirementManagerInterface.isSatisfied(context, instance.getConfigManager().autoFishingRequirements())) {
			handleSuccessfulFishing();
			instance.getVersionManager().getNMSManager().swingHand(context.holder(), gears.getRodSlot());
			destroy();
			scheduleNextFishing();
			return;
		}
	}

	/**
	 * Handles the landing action.
	 */
	public void onLand() {
		if (!hook.isValid())
			return;
		gears.trigger(ActionTrigger.LAND, context);
	}

	/**
	 * Handles the escape action.
	 */
	public void onEscape() {
		if (!hook.isValid())
			return;
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.ESCAPE);
		gears.trigger(ActionTrigger.ESCAPE, context);
	}

	/**
	 * Handles the lure action.
	 */
	public void onLure() {
		if (!hook.isValid())
			return;
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.LURE);
		gears.trigger(ActionTrigger.LURE, context);
	}

	/**
	 * Handles a failed fishing attempt.
	 */
	public void handleFailedFishing() {

		if (!valid)
			return;

		// update the hook location
		context.arg(ContextKeys.OTHER_LOCATION, hook.getLocation());
		context.arg(ContextKeys.OTHER_X, hook.getLocation().getBlockX());
		context.arg(ContextKeys.OTHER_Y, hook.getLocation().getBlockY());
		context.arg(ContextKeys.OTHER_Z, hook.getLocation().getBlockZ());

		gears.trigger(ActionTrigger.FAILURE, context);
		instance.getEventManager().trigger(context, nextLoot.id(), MechanicType.LOOT, ActionTrigger.FAILURE);
	}

	/**
	 * Handles a successful fishing attempt.
	 */
	public void handleSuccessfulFishing() {

		if (!valid)
			return;

		// update the hook location
		Location hookLocation = hook.getLocation();
		context.arg(ContextKeys.OTHER_LOCATION, hookLocation);
		context.arg(ContextKeys.OTHER_X, hookLocation.getBlockX());
		context.arg(ContextKeys.OTHER_Y, hookLocation.getBlockY());
		context.arg(ContextKeys.OTHER_Z, hookLocation.getBlockZ());

		LootType lootType = context.arg(ContextKeys.LOOT);
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

		FishingResultEvent event = new FishingResultEvent(context, FishingResultEvent.Result.SUCCESS, hook, nextLoot);
		if (EventUtils.fireAndCheckCancel(event)) {
			return;
		}

		gears.trigger(ActionTrigger.SUCCESS, context);

		switch (lootType) {
		case ITEM -> {
			context.arg(ContextKeys.SIZE_MULTIPLIER, tempFinalEffect.sizeMultiplier());
			context.arg(ContextKeys.SIZE_ADDER, tempFinalEffect.sizeAdder());
			boolean directlyToInventory = nextLoot.toInventory().evaluate(context) != 0;
			for (int i = 0; i < amount; i++) {
				instance.getScheduler().sync().runLater(() -> {
					if (directlyToInventory) {
						ItemStack stack = instance.getItemManager().getItemLoot(context,
								gears.getItem(FishingGears.GearType.ROD).stream().findAny().orElseThrow().right(),
								hook);
						if (stack.getType() != Material.AIR) {
							if (Objects.equals(context.arg(ContextKeys.NICK), "UNDEFINED")) {
								Optional<String> displayName = instance.getItemManager().wrap(stack).displayName();
								if (displayName.isPresent()) {
									context.arg(ContextKeys.NICK,
											instance.getAdventureManager().jsonToMiniMessage(displayName.get()));
								} else {
									context.arg(ContextKeys.NICK, "<lang:" + stack.getType().translationKey() + ">");
								}
							}
							PlayerUtils.giveItem(context.holder(), stack, stack.getAmount());
						}
					} else {
						Item item = instance.getItemManager().dropItemLoot(context,
								gears.getItem(FishingGears.GearType.ROD).stream().findAny().orElseThrow().right(),
								hook);
						if (item != null && Objects.equals(context.arg(ContextKeys.NICK), "UNDEFINED")) {
							ItemStack stack = item.getItemStack();
							Optional<String> displayName = instance.getItemManager().wrap(stack).displayName();
							if (displayName.isPresent()) {
								context.arg(ContextKeys.NICK,
										instance.getAdventureManager().jsonToMiniMessage(displayName.get()));
							} else {
								context.arg(ContextKeys.NICK, "<lang:" + stack.getType().translationKey() + ">");
							}
						}
						if (item != null) {
							FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hookLocation,
									nextLoot, item);
							Bukkit.getPluginManager().callEvent(spawnEvent);
							if (!spawnEvent.summonEntity())
								item.remove();
							if (spawnEvent.skipActions())
								return;
							if (item.isValid() && nextLoot.preventGrabbing()) {
								item.getPersistentDataContainer().set(
										Objects.requireNonNull(NamespacedKey.fromString("owner", instance)),
										PersistentDataType.STRING, context.holder().getName());
							}
						}
					}
					doSuccessActions();
				}, (long) instance.getConfigManager().multipleLootSpawnDelay() * i, hookLocation);
			}
		}
		case BLOCK -> {
			FallingBlock fallingBlock = instance.getBlockManager().summonBlockLoot(context);
			FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hook.getLocation(), nextLoot,
					fallingBlock);
			Bukkit.getPluginManager().callEvent(spawnEvent);
			if (!spawnEvent.summonEntity())
				fallingBlock.remove();
			if (spawnEvent.skipActions())
				return;
			doSuccessActions();
		}
		case ENTITY -> {
			Entity entity = instance.getEntityManager().summonEntityLoot(context);
			FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(context, hook.getLocation(), nextLoot, entity);
			Bukkit.getPluginManager().callEvent(spawnEvent);
			if (!spawnEvent.summonEntity())
				entity.remove();
			if (spawnEvent.skipActions())
				return;
			doSuccessActions();
		}
		}
	}

	private void doSuccessActions() {
		String id = context.arg(ContextKeys.ID);
		Player player = context.holder();

		if (!nextLoot.disableStats()) {
			instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
				userData.getStatisticData().addAmount(nextLoot.statisticKey().amountKey(), 1);
				context.arg(ContextKeys.TOTAL_AMOUNT,
						userData.getStatisticData().getAmount(nextLoot.statisticKey().amountKey()));
				Optional.ofNullable(context.arg(ContextKeys.SIZE)).ifPresent(size -> {
					float max = Math.max(0, userData.getStatisticData().getMaxSize(nextLoot.statisticKey().sizeKey()));
					context.arg(ContextKeys.RECORD, max);
					context.arg(ContextKeys.RECORD_FORMATTED, String.format("%.2f", max));
					if (userData.getStatisticData().updateSize(nextLoot.statisticKey().sizeKey(), size)) {
						instance.getEventManager().trigger(context, id, MechanicType.LOOT,
								ActionTrigger.NEW_SIZE_RECORD);
					}
				});
			});
		}

		instance.getEventManager().trigger(context, id, MechanicType.LOOT, ActionTrigger.SUCCESS);
		player.setStatistic(Statistic.FISH_CAUGHT, player.getStatistic(Statistic.FISH_CAUGHT) + 1);
	}
}