package com.swiftlicious.hellblock.gui.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCostProcessor;

public class UpgradeGUI {

	private final Map<Character, UpgradeGUIElement> itemsCharMap;
	private final Map<Integer, UpgradeGUIElement> itemsSlotMap;
	private final UpgradeGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	public UpgradeGUI(UpgradeGUIManager manager, Context<Player> context, HellblockData hellblockData,
			boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new UpgradeGUIHolder();
		if (manager.layout.length == 1 && manager.layout[0].length() == 4) {
			this.inventory = Bukkit.createInventory(holder, InventoryType.HOPPER);
		} else {
			this.inventory = Bukkit.createInventory(holder, manager.layout.length * 9);
		}
		holder.setInventory(this.inventory);
	}

	private void init() {
		int line = 0;
		for (String content : manager.layout) {
			for (int index = 0; index < (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9); index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				UpgradeGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		itemsSlotMap.entrySet()
				.forEach(entry -> this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone()));
	}

	public UpgradeGUI addElement(UpgradeGUIElement... elements) {
		for (UpgradeGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public UpgradeGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public UpgradeGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public UpgradeGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The UpgradeGUI instance.
	 */
	public UpgradeGUI refresh() {
		context.arg(ContextKeys.HELLBLOCK_HOPPER_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
				.arg(ContextKeys.HELLBLOCK_RANGE_TIER,
						hellblockData.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
				.arg(ContextKeys.HELLBLOCK_PARTY_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
				.arg(ContextKeys.HELLBLOCK_GENERATOR_TIER,
						hellblockData.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
				.arg(ContextKeys.HELLBLOCK_BARTERING_TIER,
						hellblockData.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
				.arg(ContextKeys.HELLBLOCK_CROP_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
				.arg(ContextKeys.HELLBLOCK_MOB_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE));
		UpgradeDynamicGUIElement backElement = (UpgradeDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		manager.upgradeIcons.forEach(upgrade -> {
			UpgradeDynamicGUIElement upgradeElement = (UpgradeDynamicGUIElement) getElement(upgrade.left());
			if (upgradeElement != null && !upgradeElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(upgrade.right().left().build(context));
				IslandUpgradeType upgradeType = upgrade.right().mid();
				List<String> newLore = new ArrayList<>();
				upgrade.mid().getStringList("display.lore")
						.forEach(
								lore -> newLore
										.add(AdventureHelper
												.miniMessageToJson(lore
														.replace("{current_tier}",
																String.valueOf(
																		hellblockData.getUpgradeLevel(upgradeType)))
														.replace("{max_tier}",
																String.valueOf(manager.instance.getUpgradeManager()
																		.getMaxTierFor(upgradeType)))
														.replace("{current_value}",
																String.valueOf(manager.instance.getUpgradeManager()
																		.getEffectiveUpgradeValue(hellblockData,
																				upgradeType)))
														.replace("{overall_value}",
																String.valueOf(manager.instance.getUpgradeManager()
																		.calculateTotalUpgradeValue(hellblockData,
																				upgradeType)))
														.replace("{requirement_costs}",
																hellblockData.getNextCosts(upgradeType) != null
																		? String.valueOf(new UpgradeCostProcessor(
																				context.holder())
																				.getCostSummary(hellblockData
																						.getNextCosts(upgradeType)))
																		: manager.instance.getTranslationManager()
																				.miniMessageTranslation(
																						MessageConstants.FORMAT_MAXED
																								.build().key())))));
				item.lore(newLore);
				if (!hellblockData.canUpgrade(upgradeType))
					if (manager.highlightMaxUpgrades)
						item.glint(true);

				upgradeElement.setItemStack(item.load());
			}
		});
		itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof UpgradeDynamicGUIElement)
				.forEach(entry -> {
					UpgradeDynamicGUIElement dynamicGUIElement = (UpgradeDynamicGUIElement) entry.getValue();
					this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});
		return this;
	}
}