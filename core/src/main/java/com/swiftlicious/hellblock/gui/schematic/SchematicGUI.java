package com.swiftlicious.hellblock.gui.schematic;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.Tuple;

public class SchematicGUI {

	private final Map<Character, SchematicGUIElement> itemsCharMap;
	private final Map<Integer, SchematicGUIElement> itemsSlotMap;
	private final SchematicGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final boolean isReset;

	public SchematicGUI(SchematicGUIManager manager, Context<Player> context, HellblockData hellblockData,
			boolean isReset) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.isReset = isReset;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new SchematicGUIHolder();
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
				SchematicGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, SchematicGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public SchematicGUI addElement(SchematicGUIElement... elements) {
		for (SchematicGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public SchematicGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context, true))));
	}

	@Nullable
	public SchematicGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public SchematicGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The SchematicGUI instance.
	 */
	public SchematicGUI refresh() {
		context.arg(ContextKeys.RESET_COOLDOWN, hellblockData.getResetCooldown()).arg(
				ContextKeys.RESET_COOLDOWN_FORMATTED,
				manager.instance.getFormattedCooldown(hellblockData.getResetCooldown()));
		SchematicDynamicGUIElement backElement = (SchematicDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		if (manager.checkForSchematics()) {
			for (Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>> schematic : manager.schematicIcons) {
				SchematicDynamicGUIElement schematicElement = (SchematicDynamicGUIElement) getElement(schematic.left());
				if (schematicElement != null && !schematicElement.getSlots().isEmpty()) {
					schematicElement.setItemStack(schematic.right().left().build(context));
				}
			}
		} else {
			for (Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>> schematic : manager.schematicIcons) {
				SchematicDynamicGUIElement schematicElement = (SchematicDynamicGUIElement) getElement(schematic.left());
				if (schematicElement != null && !schematicElement.getSlots().isEmpty()) {
					schematicElement.setItemStack(new ItemStack(Material.AIR));
				}
			}
		}
		for (Map.Entry<Integer, SchematicGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof SchematicDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}