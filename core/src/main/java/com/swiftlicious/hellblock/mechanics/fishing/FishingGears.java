package com.swiftlicious.hellblock.mechanics.fishing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.effects.EffectModifierInterface;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.mechanics.hook.HookConfig;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;

/**
 * Represents the fishing gears used by a player.
 */
public class FishingGears {

	private static final Map<ActionTrigger, TriConsumer<GearType, Context<Player>, ItemStack>> triggers = new HashMap<>();

	static {
		triggers.put(ActionTrigger.CAST, ((type, context, itemStack) -> type.castFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.REEL, ((type, context, itemStack) -> type.reelFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.LAND, ((type, context, itemStack) -> type.landFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.ESCAPE,
				((type, context, itemStack) -> type.escapeFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.LURE, ((type, context, itemStack) -> type.lureFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.SUCCESS,
				((type, context, itemStack) -> type.successFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.FAILURE,
				((type, context, itemStack) -> type.failureFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.BITE, ((type, context, itemStack) -> type.biteFunction.accept(context, itemStack)));
		triggers.put(ActionTrigger.HOOK, ((type, context, itemStack) -> type.hookFunction.accept(context, itemStack)));
	}

	private static BiConsumer<Context<Player>, FishingGears> fishingGearsConsumers = defaultFishingGearsConsumers();
	private final Map<GearType, List<Pair<String, ItemStack>>> gears = new HashMap<>();
	private final List<EffectModifier> modifiers = new ArrayList<>();
	private boolean canFish = true;
	private HandSlot rodSlot;

	/**
	 * Sets the fishing gears consumers.
	 *
	 * @param fishingGearsConsumers the BiConsumer to set.
	 */
	public static void fishingGearsConsumers(BiConsumer<Context<Player>, FishingGears> fishingGearsConsumers) {
		FishingGears.fishingGearsConsumers = fishingGearsConsumers;
	}

	/**
	 * Constructs a new FishingGears instance.
	 *
	 * @param context the context of the player.
	 */
	public FishingGears(Context<Player> context) {
		fishingGearsConsumers.accept(context, this);
	}

	/**
	 * Checks if the player can fish.
	 *
	 * @return true if the player can fish, false otherwise.
	 */
	public boolean canFish() {
		return canFish;
	}

	/**
	 * Triggers an action based on the specified trigger.
	 *
	 * @param trigger the ActionTrigger.
	 * @param context the context of the player.
	 */
	public void trigger(ActionTrigger trigger, Context<Player> context) {
		for (Map.Entry<GearType, List<Pair<String, ItemStack>>> entry : gears.entrySet()) {
			for (Pair<String, ItemStack> itemPair : entry.getValue()) {
				HellblockPlugin.getInstance().debug(entry.getKey() + " | " + itemPair.left() + " | " + trigger);
				Optional.ofNullable(triggers.get(trigger)).ifPresent(tri -> {
					tri.accept(entry.getKey(), context, itemPair.right());
				});
				HellblockPlugin.getInstance().getEventManager().trigger(context, itemPair.left(),
						entry.getKey().getType(), trigger);
			}
		}
	}

	/**
	 * Gets the list of effect modifiers.
	 *
	 * @return the list of effect modifiers.
	 */
	@NotNull
	public List<EffectModifier> effectModifiers() {
		return modifiers;
	}

	/**
	 * Gets the hand slot of the fishing rod.
	 *
	 * @return the hand slot of the fishing rod.
	 */
	public HandSlot getRodSlot() {
		return rodSlot;
	}

	/**
	 * Gets the items for the specified gear type.
	 *
	 * @param type the gear type.
	 * @return the collection of items for the specified gear type.
	 */
	@NotNull
	public List<Pair<String, ItemStack>> getItem(GearType type) {
		return gears.getOrDefault(type, List.of());
	}

	/**
	 * Provides the default fishing gears consumers.
	 *
	 * @return the BiConsumer for default fishing gears consumers.
	 */
	public static BiConsumer<Context<Player>, FishingGears> defaultFishingGearsConsumers() {
		return (context, fishingGears) -> {
			Player player = context.holder();
			PlayerInventory playerInventory = player.getInventory();
			ItemStack mainHandItem = playerInventory.getItemInMainHand();
			ItemStack offHandItem = playerInventory.getItemInOffHand();
			// set rod
			boolean rodOnMainHand = mainHandItem.getType() == Material.FISHING_ROD;
			ItemStack rodItem = rodOnMainHand ? mainHandItem : offHandItem;
			String rodID = HellblockPlugin.getInstance().getItemManager().getItemID(rodItem);
			fishingGears.gears.put(GearType.ROD, List.of(Pair.of(rodID, rodItem)));
			context.arg(ContextKeys.ROD, rodID);
			fishingGears.rodSlot = rodOnMainHand ? HandSlot.MAIN : HandSlot.OFF;
			HellblockPlugin.getInstance().getEffectManager().getEffectModifier(rodID, MechanicType.ROD)
					.ifPresent(fishingGears.modifiers::add);

			// set enchantments
			List<Pair<String, Short>> enchants = HellblockPlugin.getInstance().getIntegrationManager()
					.getEnchantments(rodItem);
			for (Pair<String, Short> enchantment : enchants) {
				String effectID = enchantment.left() + ":" + enchantment.right();
				HellblockPlugin.getInstance().getEffectManager().getEffectModifier(effectID, MechanicType.ENCHANT)
						.ifPresent(fishingGears.modifiers::add);
			}

			// set hook
			HellblockPlugin.getInstance().getHookManager().getHookID(rodItem).ifPresent(hookID -> {
				fishingGears.gears.put(GearType.HOOK, List.of(Pair.of(hookID, rodItem)));
				context.arg(ContextKeys.HOOK, hookID);
				HellblockPlugin.getInstance().getEffectManager().getEffectModifier(hookID, MechanicType.HOOK)
						.ifPresent(fishingGears.modifiers::add);
			});

			// set bait if it is
			String anotherItemID = HellblockPlugin.getInstance().getItemManager()
					.getItemID(rodOnMainHand ? offHandItem : mainHandItem);
			List<MechanicType> type = MechanicType.getTypeByID(anotherItemID);
			if (type != null && type.contains(MechanicType.BAIT)) {
				fishingGears.gears.put(GearType.BAIT,
						List.of(Pair.of(anotherItemID, rodOnMainHand ? offHandItem : mainHandItem)));
				context.arg(ContextKeys.BAIT, anotherItemID);
				HellblockPlugin.getInstance().getEffectManager().getEffectModifier(anotherItemID, MechanicType.BAIT)
						.ifPresent(fishingGears.modifiers::add);
			}

			// check requirements before checking totems
			for (EffectModifier modifier : fishingGears.modifiers) {
				if (!RequirementManagerInterface.isSatisfied(context, modifier.requirements())) {
					fishingGears.canFish = false;
				}
			}

			// add global effects
			fishingGears.modifiers
					.add(EffectModifierInterface.builder().id("__GLOBAL__").modifiers(HellblockPlugin.getInstance().getConfigManager().globalEffects()).build());
		};
	}

	public static class GearType {

		public static final GearType ROD = new GearType(MechanicType.ROD, ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
			if (context.holder().getGameMode() != GameMode.CREATIVE)
				HellblockPlugin.getInstance().getItemManager().increaseDamage(context.holder(), itemStack, 1, false);
		}), ((context, itemStack) -> {
			if (context.holder().getGameMode() != GameMode.CREATIVE)
				HellblockPlugin.getInstance().getItemManager().increaseDamage(context.holder(), itemStack, 1, false);
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}));

		public static final GearType BAIT = new GearType(MechanicType.BAIT, ((context, itemStack) -> {
			if (context.holder().getGameMode() != GameMode.CREATIVE)
				itemStack.setAmount(itemStack.getAmount() - 1);
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}));

		public static final GearType HOOK = new GearType(MechanicType.HOOK, ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
			if (context.holder().getGameMode() != GameMode.CREATIVE) {
				Item<ItemStack> wrapped = HellblockPlugin.getInstance().getItemManager().wrap(itemStack);
				String hookID = (String) wrapped.getTag("HellFishing", "hook_id")
						.orElseThrow(() -> new RuntimeException("This error should never occur"));
				wrapped.getTag("HellFishing", "hook_max_damage").ifPresent(max -> {
					int maxDamage = (int) max;
					int hookDamage = (int) wrapped.getTag("HellFishing", "hook_damage").orElse(0) + 1;
					if (hookDamage >= maxDamage) {
						wrapped.removeTag("HellFishing", "hook_damage");
						wrapped.removeTag("HellFishing", "hook_id");
						wrapped.removeTag("HellFishing", "hook_stack");
						wrapped.removeTag("HellFishing", "hook_max_damage");
						List<String> durabilityLore = new ArrayList<>();
						List<String> newLore = new ArrayList<>();
						List<String> previousLore = wrapped.lore().orElse(new ArrayList<>());
						for (String previous : previousLore) {
							Component component = HellblockPlugin.getInstance().getAdventureManager()
									.jsonToComponent(previous);
							if (component instanceof ScoreComponent scoreComponent
									&& scoreComponent.name().equals("hb")) {
								if (scoreComponent.objective().equals("hook")) {
									continue;
								} else if (scoreComponent.objective().equals("durability")) {
									durabilityLore.add(previous);
									continue;
								}
							}
							newLore.add(previous);
						}
						newLore.addAll(durabilityLore);
						wrapped.lore(newLore);
						HellblockPlugin.getInstance().getAdventureManager().playSound(context.holder(),
								Sound.sound(Key.key("minecraft:entity.item.break"), Sound.Source.PLAYER, 1, 1));
					} else {
						wrapped.setTag(hookDamage, "HellFishing", "hook_damage");
						HookConfig hookConfig = HellblockPlugin.getInstance().getHookManager().getHook(hookID)
								.orElseThrow();
						List<String> previousLore = wrapped.lore().orElse(new ArrayList<>());
						List<String> newLore = new ArrayList<>();
						List<String> durabilityLore = new ArrayList<>();
						for (String previous : previousLore) {
							Component component = HellblockPlugin.getInstance().getAdventureManager()
									.jsonToComponent(previous);
							if (component instanceof ScoreComponent scoreComponent
									&& scoreComponent.name().equals("hb")) {
								if (scoreComponent.objective().equals("hook")) {
									continue;
								} else if (scoreComponent.objective().equals("durability")) {
									durabilityLore.add(previous);
									continue;
								}
							}
							newLore.add(previous);
						}
						for (String lore : hookConfig.lore()) {
							ScoreComponent.Builder builder = Component.score().name("hb").objective("hook");
							builder.append(HellblockPlugin.getInstance().getAdventureManager()
									.jsonToComponent(lore.replace("{dur}", String.valueOf(maxDamage - hookDamage))
											.replace("{max}", String.valueOf(maxDamage))));
							newLore.add(HellblockPlugin.getInstance().getAdventureManager()
									.componentToJson(builder.build()));
						}
						newLore.addAll(durabilityLore);
						wrapped.lore(newLore);
					}
					wrapped.load();
				});
			}
		}), ((context, itemStack) -> {
			if (context.holder().getGameMode() != GameMode.CREATIVE) {
				Item<ItemStack> wrapped = HellblockPlugin.getInstance().getItemManager().wrap(itemStack);
				String hookID = (String) wrapped.getTag("HellFishing", "hook_id")
						.orElseThrow(() -> new RuntimeException("This error should never occur"));
				wrapped.getTag("HellFishing", "hook_max_damage").ifPresent(max -> {
					int maxDamage = (int) max;
					int hookDamage = (int) wrapped.getTag("HellFishing", "hook_damage").orElse(0) + 1;
					if (hookDamage >= maxDamage) {
						wrapped.removeTag("HellFishing", "hook_damage");
						wrapped.removeTag("HellFishing", "hook_id");
						wrapped.removeTag("HellFishing", "hook_stack");
						wrapped.removeTag("HellFishing", "hook_max_damage");
						HellblockPlugin.getInstance().getAdventureManager().playSound(context.holder(),
								Sound.sound(Key.key("minecraft:entity.item.break"), Sound.Source.PLAYER, 1, 1));
					} else {
						wrapped.setTag(hookDamage, "HellFishing", "hook_damage");
						HookConfig hookConfig = HellblockPlugin.getInstance().getHookManager().getHook(hookID)
								.orElseThrow();
						List<String> previousLore = wrapped.lore().orElse(new ArrayList<>());
						List<String> newLore = new ArrayList<>();
						List<String> durabilityLore = new ArrayList<>();
						for (String previous : previousLore) {
							Component component = HellblockPlugin.getInstance().getAdventureManager()
									.jsonToComponent(previous);
							if (component instanceof ScoreComponent scoreComponent
									&& scoreComponent.name().equals("hb")) {
								if (scoreComponent.objective().equals("hook")) {
									continue;
								} else if (scoreComponent.objective().equals("durability")) {
									durabilityLore.add(previous);
									continue;
								}
							}
							newLore.add(previous);
						}
						for (String lore : hookConfig.lore()) {
							ScoreComponent.Builder builder = Component.score().name("hb").objective("hook");
							builder.append(HellblockPlugin.getInstance().getAdventureManager()
									.jsonToComponent(lore.replace("{dur}", String.valueOf(maxDamage - hookDamage))
											.replace("{max}", String.valueOf(maxDamage))));
							newLore.add(HellblockPlugin.getInstance().getAdventureManager()
									.componentToJson(builder.build()));
						}
						newLore.addAll(durabilityLore);
						wrapped.lore(newLore);
					}
					wrapped.load();
				});
			}
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}));

		public static final GearType UTIL = new GearType(MechanicType.UTIL, ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}), ((context, itemStack) -> {
		}));

		private final MechanicType type;
		private BiConsumer<Context<Player>, ItemStack> castFunction;
		private BiConsumer<Context<Player>, ItemStack> reelFunction;
		private BiConsumer<Context<Player>, ItemStack> biteFunction;
		private BiConsumer<Context<Player>, ItemStack> successFunction;
		private BiConsumer<Context<Player>, ItemStack> failureFunction;
		private BiConsumer<Context<Player>, ItemStack> lureFunction;
		private BiConsumer<Context<Player>, ItemStack> escapeFunction;
		private BiConsumer<Context<Player>, ItemStack> landFunction;
		private BiConsumer<Context<Player>, ItemStack> hookFunction;

		public GearType(MechanicType type, BiConsumer<Context<Player>, ItemStack> castFunction,
				BiConsumer<Context<Player>, ItemStack> reelFunction,
				BiConsumer<Context<Player>, ItemStack> biteFunction,
				BiConsumer<Context<Player>, ItemStack> successFunction,
				BiConsumer<Context<Player>, ItemStack> failureFunction,
				BiConsumer<Context<Player>, ItemStack> lureFunction,
				BiConsumer<Context<Player>, ItemStack> escapeFunction,
				BiConsumer<Context<Player>, ItemStack> landFunction,
				BiConsumer<Context<Player>, ItemStack> hookFunction) {
			this.type = type;
			this.castFunction = castFunction;
			this.reelFunction = reelFunction;
			this.biteFunction = biteFunction;
			this.successFunction = successFunction;
			this.failureFunction = failureFunction;
			this.landFunction = landFunction;
			this.lureFunction = lureFunction;
			this.escapeFunction = escapeFunction;
			this.hookFunction = hookFunction;
		}

		public void castFunction(BiConsumer<Context<Player>, ItemStack> castFunction) {
			this.castFunction = castFunction;
		}

		public void reelFunction(BiConsumer<Context<Player>, ItemStack> reelFunction) {
			this.reelFunction = reelFunction;
		}

		public void biteFunction(BiConsumer<Context<Player>, ItemStack> biteFunction) {
			this.biteFunction = biteFunction;
		}

		public void successFunction(BiConsumer<Context<Player>, ItemStack> successFunction) {
			this.successFunction = successFunction;
		}

		public void failureFunction(BiConsumer<Context<Player>, ItemStack> failureFunction) {
			this.failureFunction = failureFunction;
		}

		public void escapeFunction(BiConsumer<Context<Player>, ItemStack> escapeFunction) {
			this.escapeFunction = escapeFunction;
		}

		public void lureFunction(BiConsumer<Context<Player>, ItemStack> lureFunction) {
			this.lureFunction = lureFunction;
		}

		public void landFunction(BiConsumer<Context<Player>, ItemStack> landFunction) {
			this.landFunction = landFunction;
		}

		public void hookFunction(BiConsumer<Context<Player>, ItemStack> hookFunction) {
			this.hookFunction = hookFunction;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (object == null || getClass() != object.getClass())
				return false;
			GearType gearType = (GearType) object;
			return Objects.equals(type, gearType.type);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(type);
		}

		@Override
		public String toString() {
			return type.toString();
		}

		public MechanicType getType() {
			return type;
		}
	}
}