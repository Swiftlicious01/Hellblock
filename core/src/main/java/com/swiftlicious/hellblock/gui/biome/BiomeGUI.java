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

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

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
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context, true))));
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
		for (Map.Entry<Character, Tuple<Section, HellBiome, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>>> biomes : manager.biomeIcons
				.entrySet()) {
			if (biome == biomes.getValue().mid()) {
				BiomeDynamicGUIElement biomeElement = (BiomeDynamicGUIElement) getElement(biomes.getKey());
				if (biomeElement != null && !biomeElement.getSlots().isEmpty()) {
					Item<ItemStack> item = manager.instance.getItemManager()
							.wrap(biomes.getValue().right().left().build(context));
					List<String> newLore = new ArrayList<>();
					for (String lore : biomes.getValue().left().getStringList("display.selected-lore")) {
						newLore.add(AdventureHelper.miniMessageToJson(lore));
					}
					item.lore(newLore);
					if (manager.highlightSelection)
						item.glint(true);
					biomeElement.setItemStack(item.load());
				}
			} else {
				BiomeDynamicGUIElement biomeElement = (BiomeDynamicGUIElement) getElement(biomes.getKey());
				if (biomeElement != null && !biomeElement.getSlots().isEmpty()) {
					Item<ItemStack> item = manager.instance.getItemManager()
							.wrap(biomes.getValue().right().left().build(context));
					List<String> newLore = new ArrayList<>();
					for (String lore : biomes.getValue().left().getStringList("display.unselected-lore")) {
						newLore.add(AdventureHelper.miniMessageToJson(lore));
					}
					item.lore(newLore);
					biomeElement.setItemStack(item.load());
				}
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