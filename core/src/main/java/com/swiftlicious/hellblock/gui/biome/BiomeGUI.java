package com.swiftlicious.hellblock.gui.biome;

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

import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
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
			for (int index = 0; index < (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9); index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				BiomeGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
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
		manager.instance.getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
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
		HellBiome biome = hellblockData.getBiome();
		context.arg(ContextKeys.BIOME_COOLDOWN, hellblockData.getBiomeCooldown())
				.arg(ContextKeys.BIOME_COOLDOWN_FORMATTED,
						manager.instance.getFormattedCooldown(hellblockData.getBiomeCooldown()))
				.arg(ContextKeys.HELLBLOCK_BIOME, biome);
		BiomeDynamicGUIElement backElement = (BiomeDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		if (biome == HellBiome.SOUL_SAND_VALLEY) {
			BiomeDynamicGUIElement soulSandValleyElement = (BiomeDynamicGUIElement) getElement(
					manager.soulSandValleySlot);
			if (soulSandValleyElement != null && !soulSandValleyElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager()
						.wrap(manager.soulSandValleyIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.soulSandValleySelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				if (manager.highlightSelection)
					item.glint(true);
				soulSandValleyElement.setItemStack(item.load());
			}
		} else {
			BiomeDynamicGUIElement soulSandValleyElement = (BiomeDynamicGUIElement) getElement(
					manager.soulSandValleySlot);
			if (soulSandValleyElement != null && !soulSandValleyElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager()
						.wrap(manager.soulSandValleyIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.soulSandValleyUnSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				soulSandValleyElement.setItemStack(item.load());
			}
		}
		if (biome == HellBiome.WARPED_FOREST) {
			BiomeDynamicGUIElement warpedForestElement = (BiomeDynamicGUIElement) getElement(manager.warpedForestSlot);
			if (warpedForestElement != null && !warpedForestElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.warpedForestIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.warpedForestSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				if (manager.highlightSelection)
					item.glint(true);
				warpedForestElement.setItemStack(item.load());
			}
		} else {
			BiomeDynamicGUIElement warpedForestElement = (BiomeDynamicGUIElement) getElement(manager.warpedForestSlot);
			if (warpedForestElement != null && !warpedForestElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.warpedForestIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.warpedForestUnSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				warpedForestElement.setItemStack(item.load());
			}
		}
		if (biome == HellBiome.CRIMSON_FOREST) {
			BiomeDynamicGUIElement crimsonForestElement = (BiomeDynamicGUIElement) getElement(
					manager.crimsonForestSlot);
			if (crimsonForestElement != null && !crimsonForestElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.crimsonForestIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.crimsonForestSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				if (manager.highlightSelection)
					item.glint(true);
				crimsonForestElement.setItemStack(item.load());
			}
		} else {
			BiomeDynamicGUIElement crimsonForestElement = (BiomeDynamicGUIElement) getElement(
					manager.crimsonForestSlot);
			if (crimsonForestElement != null && !crimsonForestElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.crimsonForestIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.crimsonForestUnSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				crimsonForestElement.setItemStack(item.load());
			}
		}
		if (biome == HellBiome.NETHER_WASTES) {
			BiomeDynamicGUIElement netherWastesElement = (BiomeDynamicGUIElement) getElement(manager.netherWastesSlot);
			if (netherWastesElement != null && !netherWastesElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.netherWastesIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.netherWastesSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				if (manager.highlightSelection)
					item.glint(true);
				netherWastesElement.setItemStack(item.load());
			}
		} else {
			BiomeDynamicGUIElement netherWastesElement = (BiomeDynamicGUIElement) getElement(manager.netherWastesSlot);
			if (netherWastesElement != null && !netherWastesElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.netherWastesIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.netherWastesUnSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				netherWastesElement.setItemStack(item.load());
			}
		}
		if (biome == HellBiome.BASALT_DELTAS) {
			BiomeDynamicGUIElement basaltDeltasElement = (BiomeDynamicGUIElement) getElement(manager.basaltDeltasSlot);
			if (basaltDeltasElement != null && !basaltDeltasElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.basaltDeltasIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.basaltDeltasSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				if (manager.highlightSelection)
					item.glint(true);
				basaltDeltasElement.setItemStack(item.load());
			}
		} else {
			BiomeDynamicGUIElement basaltDeltasElement = (BiomeDynamicGUIElement) getElement(manager.basaltDeltasSlot);
			if (basaltDeltasElement != null && !basaltDeltasElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.basaltDeltasIcon.build(context));
				List<String> newLore = new ArrayList<>();
				for (String lore : manager.basaltDeltasUnSelectedLore) {
					newLore.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(newLore);
				basaltDeltasElement.setItemStack(item.load());
			}
		}
		for (Map.Entry<Integer, BiomeGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof BiomeDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}