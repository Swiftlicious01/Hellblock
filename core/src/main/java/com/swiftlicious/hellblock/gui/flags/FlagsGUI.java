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

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class FlagsGUI {

	private final Map<Character, FlagsGUIElement> itemsCharMap;
	private final Map<Integer, FlagsGUIElement> itemsSlotMap;
	private final FlagsGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public FlagsGUI(FlagsGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
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
		itemsSlotMap.entrySet()
				.forEach(entry -> this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone()));
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
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
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
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}
		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// Back button
				FlagsDynamicGUIElement backElement = (FlagsDynamicGUIElement) getElement(manager.backSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					backElement.setItemStack(manager.backIcon.build(context));
				}

				// Flag icons
				for (var flag : manager.flagIcons) {
					char symbol = flag.left();
					Section config = flag.mid();
					CustomItem itemDef = flag.right().left();
					FlagType flagType = flag.right().mid();

					FlagsDynamicGUIElement flagElement = (FlagsDynamicGUIElement) getElement(symbol);
					if (flagElement == null || flagElement.getSlots().isEmpty())
						continue;

					Item<ItemStack> item = manager.instance.getItemManager().wrap(itemDef.build(context));
					AccessType value = hellblockData.getProtectionValue(flagType);

					List<String> lore = new ArrayList<>();
					config.getStringList("display.lore").forEach(raw -> lore.add(AdventureHelper
							.miniMessageToJson(raw.replace("{flag_value}", String.valueOf(value.getReturnValue())))));
					item.lore(lore);

					if (value == AccessType.ALLOW && manager.highlightSelection) {
						item.glint(true);
					}

					flagElement.setItemStack(item.loadCopy());
				}

				// Update inventory items
				itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof FlagsDynamicGUIElement)
						.forEach(entry -> {
							FlagsDynamicGUIElement dynamicElement = (FlagsDynamicGUIElement) entry.getValue();
							this.inventory.setItem(entry.getKey(), dynamicElement.getItemStack().clone());
						});
			} finally {
				refreshInProgress = false;
				if (refreshQueued) {
					refreshQueued = false;
					manager.instance.getScheduler().sync().run(this::refresh, context.holder().getLocation());
				}
			}
		}, context.holder().getLocation());

		return this;
	}
}