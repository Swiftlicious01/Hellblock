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

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BiomeGUI {

	private final Map<Character, BiomeGUIElement> itemsCharMap;
	private final Map<Integer, BiomeGUIElement> itemsSlotMap;
	private final BiomeGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public BiomeGUI(BiomeGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
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
		itemsSlotMap.entrySet()
				.forEach(entry -> this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone()));
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
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
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
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}
		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// Provide biome data to context
				HellBiome biome = hellblockData.getBiome();
				islandContext.arg(ContextKeys.BIOME_COOLDOWN, hellblockData.getBiomeCooldown())
						.arg(ContextKeys.BIOME_COOLDOWN_FORMATTED,
								manager.instance.getCooldownManager()
										.getFormattedCooldown(hellblockData.getBiomeCooldown()))
						.arg(ContextKeys.ISLAND_BIOME, biome);

				// Back icon
				BiomeDynamicGUIElement backElement = (BiomeDynamicGUIElement) getElement(manager.backSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					backElement.setItemStack(manager.backIcon.build(context));
				}

				// Biome icons
				for (var entry : manager.biomeIcons.entrySet()) {
					char symbol = entry.getKey();
					Section config = entry.getValue().left();
					CustomItem itemDef = entry.getValue().right().left();
					HellBiome type = entry.getValue().mid();

					BiomeDynamicGUIElement biomeElement = (BiomeDynamicGUIElement) getElement(symbol);
					if (biomeElement == null || biomeElement.getSlots().isEmpty())
						continue;

					Context<Player> combinedCtx = context.merge(islandContext);
					
					Item<ItemStack> item = manager.instance.getItemManager().wrap(itemDef.build(combinedCtx));
					List<String> lore = new ArrayList<>();
					List<String> rawLore = biome == type ? config.getStringList("display.selected-lore")
							: config.getStringList("display.unselected-lore");

					rawLore.forEach(raw -> lore.add(AdventureHelper.miniMessageToJson(raw)));
					item.lore(lore);

					if (biome == type && manager.highlightSelection) {
						item.glint(true);
					}

					biomeElement.setItemStack(item.loadCopy());
				}

				// Update inventory
				itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof BiomeDynamicGUIElement)
						.forEach(entry -> {
							BiomeDynamicGUIElement dynamicElement = (BiomeDynamicGUIElement) entry.getValue();
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