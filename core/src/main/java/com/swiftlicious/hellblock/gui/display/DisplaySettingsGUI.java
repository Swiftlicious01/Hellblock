package com.swiftlicious.hellblock.gui.display;

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

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.HellblockData;

public class DisplaySettingsGUI {

	private final Map<Character, DisplaySettingsGUIElement> itemsCharMap;
	private final Map<Integer, DisplaySettingsGUIElement> itemsSlotMap;
	private final DisplaySettingsGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	public DisplaySettingsGUI(DisplaySettingsGUIManager manager, Context<Player> context, HellblockData hellblockData,
			boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new DisplaySettingsGUIHolder();
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
				DisplaySettingsGUIElement element = itemsCharMap.get(symbol);
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

	public DisplaySettingsGUI addElement(DisplaySettingsGUIElement... elements) {
		for (DisplaySettingsGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public DisplaySettingsGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public DisplaySettingsGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public DisplaySettingsGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The DisplaySettingsGUI instance.
	 */
	public DisplaySettingsGUI refresh() {
		DisplaySettings displaySettings = hellblockData.getDisplaySettings();
		context.arg(ContextKeys.HELLBLOCK_NAME, displaySettings.getIslandName())
				.arg(ContextKeys.HELLBLOCK_BIO, displaySettings.getIslandBio())
				.arg(ContextKeys.HELLBLOCK_DISPLAY_CHOICE, displaySettings.getDisplayChoice());
		DisplaySettingsDynamicGUIElement backElement = (DisplaySettingsDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}

		String currentName = displaySettings.getIslandName();
		String currentBio = displaySettings.getIslandBio();
		String currentSetting = displaySettings.getDisplayChoice().name();
		String nextSetting = displaySettings.getDisplayChoice() == DisplayChoice.CHAT ? "TITLE" : "CHAT";

		DisplaySettingsDynamicGUIElement nameElement = (DisplaySettingsDynamicGUIElement) getElement(manager.nameSlot);
		if (nameElement != null && !nameElement.getSlots().isEmpty()) {
			Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.nameIcon.build(context));

			List<String> newLore = new ArrayList<>();
			manager.nameSection.getStringList("display.lore").forEach(lore -> newLore
					.add(AdventureHelper.miniMessageToJson(lore.replace("{current_name}", currentName))));
			item.lore(newLore);

			nameElement.setItemStack(item.load());
		}
		DisplaySettingsDynamicGUIElement bioElement = (DisplaySettingsDynamicGUIElement) getElement(manager.bioSlot);
		if (bioElement != null && !bioElement.getSlots().isEmpty()) {
			Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.bioIcon.build(context));

			List<String> newLore = new ArrayList<>();
			manager.bioSection.getStringList("display.lore").forEach(
					lore -> newLore.add(AdventureHelper.miniMessageToJson(lore.replace("{current_bio}", currentBio))));
			item.lore(newLore);

			bioElement.setItemStack(item.load());
		}
		DisplaySettingsDynamicGUIElement toggleElement = (DisplaySettingsDynamicGUIElement) getElement(
				manager.toggleSlot);
		if (toggleElement != null && !toggleElement.getSlots().isEmpty()) {
			Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.toggleIcon.build(context));

			List<String> newLore = new ArrayList<>();
			manager.toggleSection.getStringList("display.lore")
					.forEach(lore -> newLore.add(AdventureHelper.miniMessageToJson(
							lore.replace("{current_setting}", currentSetting).replace("{setting}", nextSetting))));
			item.lore(newLore);

			toggleElement.setItemStack(item.load());
		}
		itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof DisplaySettingsDynamicGUIElement)
				.forEach(entry -> {
					DisplaySettingsDynamicGUIElement dynamicGUIElement = (DisplaySettingsDynamicGUIElement) entry
							.getValue();
					this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});
		return this;
	}
}