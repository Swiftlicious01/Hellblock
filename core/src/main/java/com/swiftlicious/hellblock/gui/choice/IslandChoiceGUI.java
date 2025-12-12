package com.swiftlicious.hellblock.gui.choice;

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
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;

public class IslandChoiceGUI {

	private final Map<Character, IslandChoiceGUIElement> itemsCharMap;
	private final Map<Integer, IslandChoiceGUIElement> itemsSlotMap;
	private final IslandChoiceGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final UserData userData;
	protected final HellblockData hellblockData;
	protected final boolean isReset;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public IslandChoiceGUI(IslandChoiceGUIManager manager, Context<Player> context, UserData userData,
			HellblockData hellblockData, boolean isReset) {
		this.manager = manager;
		this.context = context;
		this.userData = userData;
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
			for (int index = 0; index < (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9); index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				IslandChoiceGUIElement element = itemsCharMap.get(symbol);
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
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
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
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}
		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// Update context
				context.arg(ContextKeys.RESET_COOLDOWN, hellblockData.getResetCooldown()).arg(
						ContextKeys.RESET_COOLDOWN_FORMATTED,
						manager.instance.getCooldownManager().getFormattedCooldown(hellblockData.getResetCooldown()));

				// Default icon
				if (manager.instance.getConfigManager().islandOptions().contains(IslandOptions.DEFAULT)) {
					IslandChoiceDynamicGUIElement defaultElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.defaultSlot);
					if (defaultElement != null && !defaultElement.getSlots().isEmpty()) {
						defaultElement.setItemStack(manager.defaultIcon.build(context));
					}
				} else {
					IslandChoiceDynamicGUIElement defaultElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.defaultSlot);
					if (defaultElement != null && !defaultElement.getSlots().isEmpty()) {
						defaultElement.setItemStack(getDecorativePlaceholderForSlot(manager.defaultSlot));
					}
				}

				// Classic icon
				if (manager.instance.getConfigManager().islandOptions().contains(IslandOptions.CLASSIC)) {
					IslandChoiceDynamicGUIElement classicElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.classicSlot);
					if (classicElement != null && !classicElement.getSlots().isEmpty()) {
						classicElement.setItemStack(manager.classicIcon.build(context));
					}
				} else {
					IslandChoiceDynamicGUIElement classicElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.classicSlot);
					if (classicElement != null && !classicElement.getSlots().isEmpty()) {
						classicElement.setItemStack(getDecorativePlaceholderForSlot(manager.classicSlot));
					}
				}

				// Schematic icon
				if (manager.instance.getConfigManager().islandOptions().contains(IslandOptions.SCHEMATIC)
						&& manager.instance.getSchematicGUIManager().checkForSchematics()) {
					IslandChoiceDynamicGUIElement schematicElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.schematicSlot);
					if (schematicElement != null && !schematicElement.getSlots().isEmpty()) {
						schematicElement.setItemStack(manager.schematicIcon.build(context));
					}
				} else {
					IslandChoiceDynamicGUIElement schematicElement = (IslandChoiceDynamicGUIElement) getElement(
							manager.schematicSlot);
					if (schematicElement != null && !schematicElement.getSlots().isEmpty()) {
						schematicElement.setItemStack(getDecorativePlaceholderForSlot(manager.schematicSlot));
					}
				}

				// Inject
				itemsSlotMap.forEach((slot, element) -> {
					if (element instanceof IslandChoiceDynamicGUIElement dynamic) {
						inventory.setItem(slot, dynamic.getItemStack().clone());
					}
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

	/**
	 * Returns an ItemStack to use as the decorative placeholder for `slot`.
	 * Preferred order: 1) decorativeIcons.get(symbolForSlot) if present 2) first
	 * entry in decorativeIcons 3) hard fallback gray pane
	 */
	private ItemStack getDecorativePlaceholderForSlot(int slot) {
		final IslandChoiceGUIElement element = getElement(slot);
		if (element != null) {
			final char symbol = element.getSymbol();
			final Pair<CustomItem, Action<Player>[]> mapped = manager.decorativeIcons.get(symbol);
			if (mapped != null && mapped.left() != null) {
				try {
					return mapped.left().build(context);
				} catch (Exception ignored) {
					/* fall through to next option */ }
			}
		}

		// fallback: use the first decorative icon configured (if any)
		for (Pair<CustomItem, Action<Player>[]> pair : manager.decorativeIcons.values()) {
			if (pair != null && pair.left() != null) {
				try {
					return pair.left().build(context);
				} catch (Exception ignored) {
					/* try next */ }
			}
		}

		// final fallback: a plain black pane so slot isn't empty
		final ItemStack fallback = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		return fallback;
	}
}