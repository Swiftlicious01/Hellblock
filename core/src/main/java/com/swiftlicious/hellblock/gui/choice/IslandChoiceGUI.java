package com.swiftlicious.hellblock.gui.choice;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.HellblockData;

public class IslandChoiceGUI {

	private final Map<Character, IslandChoiceGUIElement> itemsCharMap;
	private final Map<Integer, IslandChoiceGUIElement> itemsSlotMap;
	private final IslandChoiceGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final boolean isReset;

	public IslandChoiceGUI(IslandChoiceGUIManager manager, Context<Player> context, HellblockData hellblockData,
			boolean isReset) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.isReset = isReset;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new IslandChoiceGUIHolder();
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
			for (int index = 0; index < 9; index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				IslandChoiceGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * 9);
					itemsSlotMap.put(index + line * 9, element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, IslandChoiceGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public IslandChoiceGUI addElement(IslandChoiceGUIElement... elements) {
		for (IslandChoiceGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public IslandChoiceGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context))));
	}

	@Nullable
	public IslandChoiceGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public IslandChoiceGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The IslandChoiceGUI instance.
	 */
	public IslandChoiceGUI refresh() {
		for (Map.Entry<Integer, IslandChoiceGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof IslandChoiceDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}