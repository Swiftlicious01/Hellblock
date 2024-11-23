package com.swiftlicious.hellblock.gui.hellblock;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.HellblockData;

public class HellblockGUI {

	private final Map<Character, HellblockGUIElement> itemsCharMap;
	private final Map<Integer, HellblockGUIElement> itemsSlotMap;
	private final HellblockGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;

	public HellblockGUI(HellblockGUIManager manager, Context<Player> context, HellblockData hellblockData) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new HellblockGUIHolder();
		this.inventory = Bukkit.createInventory(holder, manager.layout.length * 9);
		holder.setInventory(this.inventory);
	}

	private void init() {
		int line = 0;
		for (String content : manager.layout) {
			for (int index = 0; index < 9; index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				HellblockGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * 9);
					itemsSlotMap.put(index + line * 9, element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, HellblockGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public HellblockGUI addElement(HellblockGUIElement... elements) {
		for (HellblockGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public HellblockGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
				HellblockPlugin.getInstance().getAdventureManager().componentToJson(HellblockPlugin.getInstance()
						.getAdventureManager().getComponentFromMiniMessage(manager.title.render(context))));
	}

	@Nullable
	public HellblockGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public HellblockGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The HellblockGUI instance.
	 */
	public HellblockGUI refresh() {
		for (Map.Entry<Integer, HellblockGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof HellblockDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}