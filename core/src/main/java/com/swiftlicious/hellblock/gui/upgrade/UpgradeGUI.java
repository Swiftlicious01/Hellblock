package com.swiftlicious.hellblock.gui.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCostProcessor;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class UpgradeGUI {

	private final Map<Character, UpgradeGUIElement> itemsCharMap;
	private final Map<Integer, UpgradeGUIElement> itemsSlotMap;
	private final UpgradeGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public UpgradeGUI(UpgradeGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new UpgradeGUIHolder();
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
				UpgradeGUIElement element = itemsCharMap.get(symbol);
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

	public UpgradeGUI addElement(UpgradeGUIElement... elements) {
		for (UpgradeGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public UpgradeGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public UpgradeGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public UpgradeGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The UpgradeGUI instance.
	 */
	public UpgradeGUI refresh() {
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}
		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// Update context values for tier placeholders
				islandContext
						.arg(ContextKeys.ISLAND_HOPPER_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
						.arg(ContextKeys.ISLAND_RANGE_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
						.arg(ContextKeys.ISLAND_PARTY_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
						.arg(ContextKeys.ISLAND_GENERATOR_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
						.arg(ContextKeys.ISLAND_BARTERING_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
						.arg(ContextKeys.ISLAND_CROP_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
						.arg(ContextKeys.ISLAND_MOB_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE));

				// Back button
				UpgradeDynamicGUIElement backElement = (UpgradeDynamicGUIElement) getElement(manager.backSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					backElement.setItemStack(manager.backIcon.build(context));
				}

				// Upgrade icons
				for (var upgrade : manager.upgradeIcons) {
					char symbol = upgrade.left();
					Section config = upgrade.mid();
					CustomItem customItem = upgrade.right().left();
					IslandUpgradeType type = upgrade.right().mid();

					UpgradeDynamicGUIElement upgradeElement = (UpgradeDynamicGUIElement) getElement(symbol);
					if (upgradeElement == null || upgradeElement.getSlots().isEmpty())
						continue;

					Item<ItemStack> item = manager.instance.getItemManager().wrap(customItem.build(context));

					String currentTier = String.valueOf(hellblockData.getUpgradeLevel(type));
					String maxTier = String.valueOf(manager.instance.getUpgradeManager().getMaxTierFor(type));
					String currentValue = String.valueOf(
							manager.instance.getUpgradeManager().getEffectiveUpgradeValue(hellblockData, type));
					String overallValue = String.valueOf(
							manager.instance.getUpgradeManager().calculateTotalUpgradeValue(hellblockData, type));
					String costs = hellblockData.getNextCosts(type) != null
							? new UpgradeCostProcessor(context.holder())
									.getCostSummary(hellblockData.getNextCosts(type))
							: manager.instance.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_MAXED.build().key());

					List<String> lore = config.getStringList("display.lore").stream()
							.map(loreLine -> AdventureHelper.miniMessageToJson(loreLine
									.replace("{current_tier}", currentTier).replace("{max_tier}", maxTier)
									.replace("{current_value}", currentValue).replace("{overall_value}", overallValue)
									.replace("{requirement_costs}", costs)))
							.toList();

					item.lore(lore);

					if (!hellblockData.canUpgrade(type) && manager.highlightMaxUpgrades) {
						item.glint(true);
					}

					upgradeElement.setItemStack(item.loadCopy());
				}

				// Inject updated items into GUI
				itemsSlotMap.forEach((slot, element) -> {
					if (element instanceof UpgradeDynamicGUIElement dynamic) {
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
}