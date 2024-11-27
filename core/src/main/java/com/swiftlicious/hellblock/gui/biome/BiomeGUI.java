package com.swiftlicious.hellblock.gui.biome;

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

public class BiomeGUI {

	private final Map<Character, BiomeGUIElement> itemsCharMap;
	private final Map<Integer, BiomeGUIElement> itemsSlotMap;
	private final BiomeGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;

	public BiomeGUI(BiomeGUIManager manager, Context<Player> context, HellblockData hellblockData) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new BiomeGUIHolder();
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
				BiomeGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * 9);
					itemsSlotMap.put(index + line * 9, element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, BiomeGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public BiomeGUI addElement(BiomeGUIElement... elements) {
		for (BiomeGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public BiomeGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		HellblockPlugin.getInstance().getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context))));
	}

	@Nullable
	public BiomeGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public BiomeGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The BiomeGUI instance.
	 */
	public BiomeGUI refresh() {
		for (Map.Entry<Integer, BiomeGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof BiomeDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}