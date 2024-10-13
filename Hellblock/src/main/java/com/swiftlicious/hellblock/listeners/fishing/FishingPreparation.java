package com.swiftlicious.hellblock.listeners.fishing;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.effects.EffectCarrier;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.effects.FishingEffect;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;

public class FishingPreparation extends FishingPreparationInterface {

	private boolean hasBait = false;
	private boolean hasHook = false;
	private @Nullable ItemStack baitItemStack;
	private final @NotNull ItemStack rodItemStack;
	private final List<EffectCarrier> effects;
	private boolean canFish = true;

	public FishingPreparation(Player player, HellblockPlugin plugin) {
		super(player);

		PlayerInventory playerInventory = player.getInventory();
		ItemStack mainHandItem = playerInventory.getItemInMainHand();
		ItemStack offHandItem = playerInventory.getItemInOffHand();

		this.effects = new ArrayList<>();
		boolean rodOnMainHand = mainHandItem.getType() == Material.FISHING_ROD;
		this.rodItemStack = rodOnMainHand ? mainHandItem : offHandItem;
		String rodItemID = HellblockPlugin.getInstance().getItemManager().getAnyPluginItemID(this.rodItemStack);
		EffectCarrier rodEffect = HellblockPlugin.getInstance().getEffectManager().getEffectCarrier("rod", rodItemID);
		if (rodEffect != null)
			effects.add(rodEffect);
		super.insertArg("{rod}", rodItemID);

		NBT.get(rodItemStack, nbtItem -> {
			ReadableNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("hook_id")) {
				String hookID = hbCompound.getString("hook_id");
				super.insertArg("{hook}", hookID);
				this.hasHook = true;
				EffectCarrier carrier = HellblockPlugin.getInstance().getEffectManager().getEffectCarrier("hook",
						hookID);
				if (carrier != null) {
					this.effects.add(carrier);
				}
			}
		});

		String baitItemID = HellblockPlugin.getInstance().getItemManager()
				.getAnyPluginItemID(rodOnMainHand ? offHandItem : mainHandItem);
		EffectCarrier baitEffect = HellblockPlugin.getInstance().getEffectManager().getEffectCarrier("bait",
				baitItemID);

		if (baitEffect != null) {
			this.baitItemStack = rodOnMainHand ? offHandItem : mainHandItem;
			this.effects.add(baitEffect);
			this.hasBait = true;
			super.insertArg("{bait}", baitItemID);
		}

		for (String enchant : HellblockPlugin.getInstance().getIntegrationManager().getEnchantments(rodItemStack)) {
			EffectCarrier enchantEffect = HellblockPlugin.getInstance().getEffectManager().getEffectCarrier("enchant",
					enchant);
			if (enchantEffect != null) {
				this.effects.add(enchantEffect);
			}
		}

		for (EffectCarrier effectCarrier : effects) {
			if (!effectCarrier.isConditionMet(this)) {
				this.canFish = false;
				return;
			}
		}
	}

	/**
	 * Retrieves the ItemStack representing the fishing rod.
	 *
	 * @return The ItemStack representing the fishing rod.
	 */
	@NotNull
	public ItemStack getRodItemStack() {
		return rodItemStack;
	}

	/**
	 * Retrieves the ItemStack representing the bait (if available).
	 *
	 * @return The ItemStack representing the bait, or null if no bait is set.
	 */
	@Nullable
	public ItemStack getBaitItemStack() {
		return baitItemStack;
	}

	/**
	 * Checks if player meet the requirements for fishing gears
	 *
	 * @return True if can fish, false otherwise.
	 */
	public boolean canFish() {
		return this.canFish;
	}

	/**
	 * Merges a FishingEffect into this fishing rod, applying effect modifiers.
	 *
	 * @param effect The FishingEffect to merge into this rod.
	 */
	public void mergeEffect(FishingEffect effect) {
		for (EffectModifier modifier : HellblockPlugin.getInstance().getGlobalSettings().getEffectModifiers()) {
			modifier.modify(effect, this);
		}
		for (EffectCarrier effectCarrier : effects) {
			for (EffectModifier modifier : effectCarrier.getEffectModifiers()) {
				modifier.modify(effect, this);
			}
		}
	}

	/**
	 * Triggers actions associated with a specific action trigger.
	 *
	 * @param actionTrigger The action trigger that initiates the actions.
	 */
	public void triggerActions(ActionTrigger actionTrigger) {
		HellblockPlugin.getInstance().getGlobalSettings().triggerRodActions(actionTrigger, this);
		if (hasBait)
			HellblockPlugin.getInstance().getGlobalSettings().triggerBaitActions(actionTrigger, this);
		if (hasHook)
			HellblockPlugin.getInstance().getGlobalSettings().triggerHookActions(actionTrigger, this);
		for (EffectCarrier effectCarrier : effects) {
			Action[] actions = effectCarrier.getActions(actionTrigger);
			if (actions != null)
				for (Action action : actions) {
					action.trigger(this);
				}
		}
	}
}