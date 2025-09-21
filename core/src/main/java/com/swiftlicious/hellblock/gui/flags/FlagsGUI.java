package com.swiftlicious.hellblock.gui.flags;

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
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class FlagsGUI {

	private final Map<Character, FlagsGUIElement> itemsCharMap;
	private final Map<Integer, FlagsGUIElement> itemsSlotMap;
	private final FlagsGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;

	public FlagsGUI(FlagsGUIManager manager, Context<Player> context, HellblockData hellblockData) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new FlagsGUIHolder();
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
				FlagsGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, FlagsGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public FlagsGUI addElement(FlagsGUIElement... elements) {
		for (FlagsGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public FlagsGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context, true))));
	}

	@Nullable
	public FlagsGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public FlagsGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The FlagsGUI instance.
	 */
	public FlagsGUI refresh() {
		FlagsDynamicGUIElement backElement = (FlagsDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		for (Tuple<Character, Section, Tuple<CustomItem, FlagType, Action<Player>[]>> flag : manager.flagIcons) {
			FlagsDynamicGUIElement flagElement = (FlagsDynamicGUIElement) getElement(flag.left());
			if (flagElement != null && !flagElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(flag.right().left().build(context));
				FlagType flagType = flag.right().mid();
				List<String> newLore = new ArrayList<>();
				for (String lore : flag.mid().getStringList("display.lore")) {
					newLore.add(AdventureHelper.miniMessageToJson(lore.replace("{flag_value}",
							String.valueOf(hellblockData.getProtectionValue(flagType).getReturnValue()))));
				}
				item.lore(newLore);
				if (hellblockData.getProtectionValue(flagType) == AccessType.ALLOW)
					if (manager.highlightSelection)
						item.glint(true);

				flagElement.setItemStack(item.load());
			}
		}
		for (Map.Entry<Integer, FlagsGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof FlagsDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}