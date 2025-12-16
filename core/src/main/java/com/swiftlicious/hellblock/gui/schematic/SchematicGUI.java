package com.swiftlicious.hellblock.gui.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.swiftlicious.hellblock.gui.PaginatedGUI;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.TextValue;

public class SchematicGUI extends PaginatedGUI<SchematicGUIElement> {

	private final Map<Character, SchematicGUIElement> itemsCharMap;
	private final Map<Integer, SchematicGUIElement> itemsSlotMap;
	private final SchematicGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final UserData userData;
	protected final HellblockData hellblockData;
	protected final boolean isReset;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public SchematicGUI(SchematicGUIManager manager, Context<Player> context, UserData userData,
			HellblockData hellblockData, boolean isReset, @Nullable CustomItem leftIcon,
			@Nullable Action<Player>[] leftActions, @Nullable CustomItem rightIcon,
			@Nullable Action<Player>[] rightActions) {
		super(leftIcon, leftActions, rightIcon, rightActions);
		this.manager = manager;
		this.context = context;
		this.userData = userData;
		this.hellblockData = hellblockData;
		this.isReset = isReset;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new SchematicGUIHolder();
		int rows;
		if (manager.hasPageLayouts()) {
			// assume all pages have equal row count for consistency
			String[] firstPage = manager.getActiveLayouts()[0];
			rows = firstPage.length;
		} else {
			rows = manager.layout.length;
		}

		if (rows == 1 && manager.layout[0].length() == 4) {
			this.inventory = Bukkit.createInventory(holder, InventoryType.HOPPER);
		} else {
			this.inventory = Bukkit.createInventory(holder, rows * 9);
		}
		holder.setInventory(this.inventory);

		super.player = context.holder();
		// Since PaginatedGUI also stores `inventory`, assign it
		super.inventory = this.inventory; // safely overwrite it
	}

	private void init() {
		int line = 0;
		int width = (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9);

		// Use single layout (only when not in pageLayouts mode)
		for (String content : manager.layout) {
			for (int index = 0; index < width; index++) {
				char symbol = (index < content.length()) ? content.charAt(index) : ' ';
				SchematicGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * width);
					itemsSlotMap.put(index + line * width, element);
				}
			}
			line++;
		}

		// Only auto-paginate if NOT using multi-page mode
		if (manager.pageLayouts.isEmpty() && (rightIconItem != null || leftIconItem != null)) {
			List<SchematicGUIElement> schematicElements = new ArrayList<>();

			for (SchematicGUIElement element : itemsCharMap.values()) {
				if (element.getSymbol() != manager.backSlot) {
					schematicElements.add(element);
				}
			}

			int itemsPerPage = manager.layout.length * 9 - 2; // reserve for arrows
			List<List<SchematicGUIElement>> pages = new ArrayList<>();

			for (int i = 0; i < schematicElements.size(); i += itemsPerPage) {
				pages.add(schematicElements.subList(i, Math.min(i + itemsPerPage, schematicElements.size())));
			}

			setPages(pages); // PaginatedGUI base method
		}

		// Set initial inventory items
		itemsSlotMap.forEach((slot, element) -> this.inventory.setItem(slot, element.getItemStack().clone()));
	}

	private void buildPaginated() {
		List<List<SchematicGUIElement>> allPages = new ArrayList<>();
		int width = (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9);

		// Each entry in pageLayouts is one "page"
		for (Map.Entry<Integer, String[]> entry : manager.pageLayouts.entrySet()) {
			List<SchematicGUIElement> pageElements = new ArrayList<>();
			String[] layout = entry.getValue();
			int line = 0;

			for (String content : layout) {
				for (int index = 0; index < width; index++) {
					char symbol = (index < content.length()) ? content.charAt(index) : ' ';
					SchematicGUIElement element = itemsCharMap.get(symbol);
					if (element != null) {
						element.addSlot(index + line * width);
						pageElements.add(element);
					}
				}
				line++;
			}
			allPages.add(pageElements);
		}

		setPages(allPages);
		// Initialize the inventory with page 0 visible
		openPage(0);
	}

	public SchematicGUI addElement(SchematicGUIElement... elements) {
		for (SchematicGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public SchematicGUI build() {
		if (!manager.pageLayouts.isEmpty()) {
			// multi-page mode
			buildPaginated();
		} else {
			// single-layout mode (legacy behavior)
			init();
		}
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
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

				// Back icon
				SchematicDynamicGUIElement backElement = (SchematicDynamicGUIElement) getElement(manager.backSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					backElement.setItemStack(manager.backIcon.build(context));
				}

				// Handle schematics display
				if (manager.checkForSchematics()) {
					manager.schematicIcons.forEach(entry -> {
						SchematicDynamicGUIElement element = (SchematicDynamicGUIElement) getElement(entry.left());
						if (element != null && !element.getSlots().isEmpty()) {
							element.setItemStack(entry.right().left().build(context));
						}
					});
				} else {
					manager.schematicIcons.stream().map(entry -> (SchematicDynamicGUIElement) getElement(entry.left()))
							.filter(e -> e != null && !e.getSlots().isEmpty())
							.forEach(e -> e.setItemStack(new ItemStack(Material.AIR)));
				}

				// Inject
				itemsSlotMap.forEach((slot, element) -> {
					if (element instanceof SchematicDynamicGUIElement dynamic) {
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

	@Override
	protected int getLeftIconSlot() {
		SchematicGUIElement element = getElement(manager.leftSlot);
		return element != null && !element.getSlots().isEmpty() ? element.getSlots().get(0) : -1;
	}

	@Override
	protected int getRightIconSlot() {
		SchematicGUIElement element = getElement(manager.rightSlot);
		return element != null && !element.getSlots().isEmpty() ? element.getSlots().get(0) : -1;
	}

	@Override
	public void openPage(int index) {
		if (index < 0 || index > maxPage)
			return;
		this.currentPage = index;
		refreshPage();

		// Use per-page title if defined, otherwise fallback to global title
		TextValue<Player> titleValue = manager.pageTitles.getOrDefault(index, manager.title);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(titleValue.render(context, true))));
	}
}