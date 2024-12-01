package com.swiftlicious.hellblock.gui.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class MarketGUI {

	private final Map<Character, MarketGUIElement> itemsCharMap;
	private final Map<Integer, MarketGUIElement> itemsSlotMap;
	private final MarketManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final EarningData earningData;

	public MarketGUI(MarketManager manager, Context<Player> context, EarningData earningData) {
		this.manager = manager;
		this.context = context;
		this.earningData = earningData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new MarketGUIHolder();
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
				MarketGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, MarketGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public MarketGUI addElement(MarketGUIElement... elements) {
		for (MarketGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public MarketGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		manager.instance.getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context))));
	}

	@Nullable
	public MarketGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public MarketGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The MarketGUI instance.
	 */
	public MarketGUI refresh() {
		double earningLimit = manager.earningLimit(context);
		MarketDynamicGUIElement sellElement = (MarketDynamicGUIElement) getElement(manager.sellSlot);
		if (sellElement != null && !sellElement.getSlots().isEmpty()) {
			Pair<Integer, Double> pair = manager.getItemsToSell(context, getItemsInGUI());
			double totalWorth = pair.right() * manager.earningsMultiplier(context);
			int soldAmount = pair.left();
			context.arg(ContextKeys.MONEY, manager.money(totalWorth))
					.arg(ContextKeys.MONEY_FORMATTED, String.format("%.2f", totalWorth))
					.arg(ContextKeys.REST, manager.money(earningLimit - earningData.getEarnings()))
					.arg(ContextKeys.REST_FORMATTED, String.format("%.2f", (earningLimit - earningData.getEarnings())))
					.arg(ContextKeys.SOLD_ITEM_AMOUNT, soldAmount);
			if (totalWorth <= 0) {
				sellElement.setItemStack(manager.sellIconDenyItem.build(context));
			} else if (earningLimit != -1 && (earningLimit - earningData.getEarnings() < totalWorth)) {
				sellElement.setItemStack(manager.sellIconLimitItem.build(context));
			} else {
				sellElement.setItemStack(manager.sellIconAllowItem.build(context));
			}
		}

		MarketDynamicGUIElement sellAllElement = (MarketDynamicGUIElement) getElement(manager.sellAllSlot);
		if (sellAllElement != null && !sellAllElement.getSlots().isEmpty()) {
			List<ItemStack> itemStacksToSell = manager
					.storageContentsToList(context.holder().getInventory().getStorageContents());
			Pair<Integer, Double> pair = manager.getItemsToSell(context, itemStacksToSell);
			double totalWorth = pair.right() * manager.earningsMultiplier(context);
			int soldAmount = pair.left();
			context.arg(ContextKeys.MONEY, manager.money(totalWorth))
					.arg(ContextKeys.MONEY_FORMATTED, String.format("%.2f", totalWorth))
					.arg(ContextKeys.REST, manager.money(earningLimit - earningData.getEarnings()))
					.arg(ContextKeys.REST_FORMATTED, String.format("%.2f", (earningLimit - earningData.getEarnings())))
					.arg(ContextKeys.SOLD_ITEM_AMOUNT, soldAmount);
			if (totalWorth <= 0) {
				sellAllElement.setItemStack(manager.sellAllIconAllowItem.build(context));
			} else if (earningLimit != -1 && (earningLimit - earningData.getEarnings() < totalWorth)) {
				sellAllElement.setItemStack(manager.sellAllIconLimitItem.build(context));
			} else {
				sellAllElement.setItemStack(manager.sellAllIconAllowItem.build(context));
			}
		}

		for (Map.Entry<Integer, MarketGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof MarketDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}

	public List<ItemStack> getItemsInGUI() {
		MarketGUIElement itemElement = getElement(manager.itemSlot);
		if (itemElement == null)
			return List.of();
		return itemElement.getSlots().stream().map(inventory::getItem).filter(Objects::nonNull).toList();
	}

	public int getEmptyItemSlot() {
		MarketGUIElement itemElement = getElement(manager.itemSlot);
		if (itemElement == null) {
			return -1;
		}
		for (int slot : itemElement.getSlots()) {
			ItemStack itemStack = inventory.getItem(slot);
			if (itemStack == null || itemStack.getType() == Material.AIR) {
				return slot;
			}
		}
		return -1;
	}

	public void returnItems() {
		MarketGUIElement itemElement = getElement(manager.itemSlot);
		if (itemElement == null) {
			return;
		}
		for (int slot : itemElement.getSlots()) {
			ItemStack itemStack = inventory.getItem(slot);
			if (itemStack != null && itemStack.getType() != Material.AIR) {
				PlayerUtils.giveItem(context.holder(), itemStack, itemStack.getAmount());
				inventory.setItem(slot, new ItemStack(Material.AIR));
			}
		}
	}
}