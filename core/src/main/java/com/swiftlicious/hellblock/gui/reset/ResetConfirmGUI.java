package com.swiftlicious.hellblock.gui.reset;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;

public class ResetConfirmGUI {

	private final Map<Character, ResetConfirmGUIElement> itemsCharMap;
	private final Map<Integer, ResetConfirmGUIElement> itemsSlotMap;
	private final ResetConfirmGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;

	public ResetConfirmGUI(ResetConfirmGUIManager manager, Context<Player> context, HellblockData hellblockData) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new ResetConfirmGUIHolder();
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
				ResetConfirmGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, ResetConfirmGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public ResetConfirmGUI addElement(ResetConfirmGUIElement... elements) {
		for (ResetConfirmGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public ResetConfirmGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context))));
	}

	@Nullable
	public ResetConfirmGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public ResetConfirmGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The ResetConfirmGUI instance.
	 */
	public ResetConfirmGUI refresh() {
		context.arg(ContextKeys.RESET_COOLDOWN, hellblockData.getResetCooldown()).arg(
				ContextKeys.RESET_COOLDOWN_FORMATTED,
				manager.instance.getFormattedCooldown(hellblockData.getResetCooldown()));
		ResetConfirmDynamicGUIElement denyElement = (ResetConfirmDynamicGUIElement) getElement(manager.denySlot);
		if (denyElement != null && !denyElement.getSlots().isEmpty()) {
			denyElement.setItemStack(manager.denyIcon.build(context));
		}
		ResetConfirmDynamicGUIElement confirmElement = (ResetConfirmDynamicGUIElement) getElement(manager.confirmSlot);
		if (confirmElement != null && !confirmElement.getSlots().isEmpty()) {
			confirmElement.setItemStack(manager.confirmIcon.build(context));
		}
		for (Map.Entry<Integer, ResetConfirmGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof ResetConfirmDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}