package com.swiftlicious.hellblock.gui.challenges;

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

import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.gui.PaginatedGUI;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ChallengesGUI extends PaginatedGUI<ChallengesGUIElement> {

	private final Map<Character, ChallengesGUIElement> itemsCharMap;
	private final Map<Integer, ChallengesGUIElement> itemsSlotMap;
	private final ChallengesGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final ChallengeData challengeData;
	protected final boolean isOwner;
	protected final boolean showBackIcon;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public ChallengesGUI(ChallengesGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, ChallengeData challengeData, boolean isOwner, boolean showBackIcon,
			@Nullable CustomItem leftIcon, @Nullable Action<Player>[] leftActions, @Nullable CustomItem rightIcon,
			@Nullable Action<Player>[] rightActions) {
		super(leftIcon, leftActions, rightIcon, rightActions);
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.challengeData = challengeData;
		this.isOwner = isOwner;
		this.showBackIcon = showBackIcon;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new ChallengesGUIHolder();
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
				ChallengesGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * width);
					itemsSlotMap.put(index + line * width, element);
				}
			}
			line++;
		}

		// Only auto-paginate if NOT using multi-page mode
		if (manager.pageLayouts.isEmpty() && (rightIconItem != null || leftIconItem != null)) {
			List<ChallengesGUIElement> challengeElements = new ArrayList<>();

			for (ChallengesGUIElement element : itemsCharMap.values()) {
				if (element.getSymbol() != manager.backSlot && element.getSymbol() != manager.closeSlot) {
					challengeElements.add(element);
				}
			}

			int itemsPerPage = manager.layout.length * 9 - 2; // reserve for arrows
			List<List<ChallengesGUIElement>> pages = new ArrayList<>();

			for (int i = 0; i < challengeElements.size(); i += itemsPerPage) {
				pages.add(challengeElements.subList(i, Math.min(i + itemsPerPage, challengeElements.size())));
			}

			setPages(pages); // PaginatedGUI base method
		}

		// Set initial inventory items
		itemsSlotMap.forEach((slot, element) -> this.inventory.setItem(slot, element.getItemStack().clone()));
	}

	private void buildPaginated() {
		List<List<ChallengesGUIElement>> allPages = new ArrayList<>();
		int width = (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9);

		// Each entry in pageLayouts is one "page"
		for (Map.Entry<Integer, String[]> entry : manager.pageLayouts.entrySet()) {
			List<ChallengesGUIElement> pageElements = new ArrayList<>();
			String[] layout = entry.getValue();
			int line = 0;

			for (String content : layout) {
				for (int index = 0; index < width; index++) {
					char symbol = (index < content.length()) ? content.charAt(index) : ' ';
					ChallengesGUIElement element = itemsCharMap.get(symbol);
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

	public ChallengesGUI addElement(ChallengesGUIElement... elements) {
		for (ChallengesGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public ChallengesGUI build() {
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
	public ChallengesGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public ChallengesGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The ChallengesGUI instance.
	 */
	public ChallengesGUI refresh() {
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}

		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// === Back Button ===
				ChallengesDynamicGUIElement backElement = (ChallengesDynamicGUIElement) getElement(
						showBackIcon ? manager.backSlot : manager.closeSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					if (showBackIcon) {
						backElement.setItemStack(manager.backIcon.build(context));
					} else {
						backElement.setItemStack(manager.closeIcon.build(context));
					}
				}

				// === Challenge Icons ===
				for (Tuple<Character, Section, Tuple<CustomItem, ChallengeType, Action<Player>[]>> challenge : manager.challengeIcons) {
					char symbol = challenge.left();
					Section config = challenge.mid();
					Tuple<CustomItem, ChallengeType, Action<Player>[]> data = challenge.right();
					CustomItem itemDef = data.left();
					ChallengeType type = data.mid();

					ChallengesDynamicGUIElement element = (ChallengesDynamicGUIElement) getElement(symbol);
					if (element == null || element.getSlots().isEmpty())
						continue;

					Item<ItemStack> item = manager.instance.getItemManager().wrap(itemDef.build(context));
					List<String> lore = new ArrayList<>();
					String name;

					double current = challengeData.getChallengeProgress(type);
					int needed = type.getNeededAmount();

					if (challengeData.isChallengeCompleted(type)) {
						name = config.getString("display.completed-name", "").replace("{current_amount}", ProgressBar
								.formatValue((needed)).replace("{needed_amount}", ProgressBar.formatValue(needed)));
						if (challengeData.isChallengeRewardClaimed(type)) {
							config.getStringList("display.claimed-lore", new ArrayList<>())
									.forEach(line -> lore.add(AdventureHelper.miniMessageToJson(
											line.replace("{current_amount}", ProgressBar.formatValue(needed))
													.replace("{needed_amount}", ProgressBar.formatValue(needed)))));
						} else {
							config.getStringList("display.unclaimed-lore", new ArrayList<>())
									.forEach(line -> lore.add(AdventureHelper.miniMessageToJson(
											line.replace("{current_amount}", ProgressBar.formatValue(needed))
													.replace("{needed_amount}", ProgressBar.formatValue(needed)))));
						}
						if (manager.highlightCompletion) {
							item.glint(true);
						}
					} else {
						name = config.getString("display.uncompleted-name", "")
								.replace("{current_amount}", ProgressBar.formatValue(current))
								.replace("{needed_amount}", ProgressBar.formatValue(needed));
						config.getStringList("display.uncompleted-lore", new ArrayList<>()).forEach(line -> {

							double percent = (needed == 0) ? 0 : current / needed;
							List<String> gradient;

							// Dynamic gradient selection based on completion %
							if (percent < 0.33) {
								gradient = List.of("<#FF0000>", "<#FF8000>"); // red → orange
							} else if (percent < 0.66) {
								gradient = List.of("<#FF8000>", "<#FFFF00>"); // orange → yellow
							} else {
								gradient = List.of("<#00FF00>", "<#ADFF2F>", "<#FFFF00>"); // green → lime → yellow
							}

							// Smooth animation phase when near completion
							double intensity = Math.min(1.0, Math.max(0.0, (percent - 0.9) / 0.1)); // fade in 90–100%
							double phase = (Math.sin(System.currentTimeMillis() / 500.0) + 1) / 2.0 * intensity;

							// Choose animated or static gradient depending on completion %
							String progressBar = (percent >= 0.9)
									? ProgressBar.getAnimatedGradientBar(new ProgressBar(needed, current), 25, gradient,
											phase)
									: ProgressBar.getMultiGradientBar(new ProgressBar(needed, current), 25, gradient);

							lore.add(AdventureHelper.miniMessageToJson(
									line.replace("{current_amount}", ProgressBar.formatValue(current))
											.replace("{needed_amount}", ProgressBar.formatValue(needed))
											.replace("{progress_bar}", progressBar)));
						});
					}

					item.displayName(AdventureHelper.miniMessageToJson(name));
					item.lore(lore);
					element.setItemStack(item.loadCopy());
				}

				// === Apply Updated Items to Inventory ===
				itemsSlotMap.entrySet().stream()
						.filter(entry -> entry.getValue() instanceof ChallengesDynamicGUIElement).forEach(entry -> {
							ChallengesDynamicGUIElement dynamicElement = (ChallengesDynamicGUIElement) entry.getValue();
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

	@Override
	protected int getLeftIconSlot() {
		ChallengesGUIElement element = getElement(manager.leftSlot);
		return element != null && !element.getSlots().isEmpty() ? element.getSlots().get(0) : -1;
	}

	@Override
	protected int getRightIconSlot() {
		ChallengesGUIElement element = getElement(manager.rightSlot);
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