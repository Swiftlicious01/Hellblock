package com.swiftlicious.hellblock.gui.visit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.gui.PaginatedGUI;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitEntry;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.TextWrapUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import net.kyori.adventure.sound.Sound;

public class VisitGUI extends PaginatedGUI<VisitGUIElement> {

	private final Map<Character, VisitGUIElement> itemsCharMap;
	private final Map<Integer, VisitGUIElement> itemsSlotMap;
	private final VisitGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected VisitSorter currentSorter;
	protected final boolean isOwner;
	protected final boolean showBackIcon;

	private RefreshReason refreshReason = RefreshReason.OPENING;

	private final Map<UUID, String> cachedSkullTextures = new ConcurrentHashMap<>(); // Skull cache

	public VisitGUI(VisitGUIManager manager, VisitSorter currentSorter, Context<Player> context,
			Context<Integer> islandContext, HellblockData hellblockData, boolean isOwner, boolean showBackIcon,
			@Nullable CustomItem leftIcon, @Nullable Action<Player>[] leftActions, @Nullable CustomItem rightIcon,
			@Nullable Action<Player>[] rightActions) {
		super(leftIcon, leftActions, rightIcon, rightActions);
		this.manager = manager;
		this.currentSorter = currentSorter;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.showBackIcon = showBackIcon;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new VisitGUIHolder();
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
				VisitGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * width);
					itemsSlotMap.put(index + line * width, element);
				}
			}
			line++;
		}

		// Only auto-paginate if NOT using multi-page mode
		if (manager.pageLayouts.isEmpty() && (rightIconItem != null || leftIconItem != null)) {
			List<VisitGUIElement> visitElements = new ArrayList<>();

			for (VisitGUIElement element : itemsCharMap.values()) {
				if (element.getSymbol() != manager.backSlot) {
					visitElements.add(element);
				}
			}

			int itemsPerPage = manager.layout.length * 9 - 2; // reserve for arrows
			List<List<VisitGUIElement>> pages = new ArrayList<>();

			for (int i = 0; i < visitElements.size(); i += itemsPerPage) {
				pages.add(visitElements.subList(i, Math.min(i + itemsPerPage, visitElements.size())));
			}

			setPages(pages); // PaginatedGUI base method
		}

		// Set initial inventory items
		itemsSlotMap.forEach((slot, element) -> this.inventory.setItem(slot, element.getItemStack().clone()));
	}

	private void buildPaginated() {
		List<List<VisitGUIElement>> allPages = new ArrayList<>();
		int width = (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9);

		// Each entry in pageLayouts is one "page"
		for (Map.Entry<Integer, String[]> entry : manager.pageLayouts.entrySet()) {
			List<VisitGUIElement> pageElements = new ArrayList<>();
			String[] layout = entry.getValue();
			int line = 0;

			for (String content : layout) {
				for (int index = 0; index < width; index++) {
					char symbol = (index < content.length()) ? content.charAt(index) : ' ';
					VisitGUIElement element = itemsCharMap.get(symbol);
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

	public VisitGUI addElement(VisitGUIElement... elements) {
		for (VisitGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public VisitGUI build() {
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
		setRefreshReason(RefreshReason.OPENING);
		context.holder().openInventory(inventory);
		updateTitle();
	}

	private void updateTitle() {
		String titleJson = AdventureHelper.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title
				.render(context, true).replace("{sort_type}", StringUtils.toProperCase(currentSorter.toString()))));
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), titleJson);
	}

	@Nullable
	public VisitGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public VisitGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	public void clearDynamicElements() {
		itemsSlotMap.entrySet().removeIf(entry -> entry.getValue() instanceof VisitDynamicGUIElement
				&& ((VisitDynamicGUIElement) entry.getValue()).getSymbol() == manager.filledSlot);
	}

	@NotNull
	public VisitSorter getCurrentSorter() {
		return currentSorter;
	}

	public void setCurrentSorter(@NotNull VisitSorter sorter) {
		this.currentSorter = sorter;
	}

	@NotNull
	public RefreshReason getRefreshReason() {
		return refreshReason;
	}

	public void setRefreshReason(@NotNull RefreshReason refreshReason) {
		this.refreshReason = refreshReason;
	}

	public CompletableFuture<Void> populateVisitEntries(@NotNull List<VisitEntry> entries, @NotNull VisitSorter sorter,
			int pageIndex) {
		// Prevent redundant sort triggers
		setCurrentSorter(sorter);

		return manager.instance.getCoopManager().getCachedIslandOwnerData().thenCompose(ownerSet -> {
			Map<UUID, UserData> ownerMap = ownerSet.stream()
					.collect(Collectors.toMap(UserData::getUUID, Function.identity()));

			List<CompletableFuture<VisitDynamicGUIElement>> futures = new ArrayList<>();

			Set<UUID> alreadyAdded = new HashSet<>();
			Map<UUID, Integer> existingSlotAssignments = new HashMap<>();

			// Build map of UUID -> slotIndex (resolved slot handling)
			int assignedIndex = 0;
			for (VisitEntry entry : entries) {
				if (!alreadyAdded.add(entry.ownerId()))
					continue; // Skip duplicates
				existingSlotAssignments.put(entry.ownerId(), assignedIndex++);
			}

			String[][] layouts = manager.hasPageLayouts() ? new String[][] { manager.getActiveLayouts()[pageIndex] }
					: new String[][] { manager.layout };

			int layoutSlotIndex = 0;

			for (String[] pageLayout : layouts) {
				for (int row = 0; row < pageLayout.length; row++) {
					for (int col = 0; col < pageLayout[row].length(); col++) {
						char symbol = pageLayout[row].charAt(col);
						if (symbol != manager.filledSlot)
							continue;

						final int currentIndex = layoutSlotIndex;
						Item<ItemStack> baseEmptyItem = manager.instance.getItemManager()
								.wrap(manager.emptyIcon.build(context));

						boolean isFeatured = sorter == VisitSorter.FEATURED;

						List<String> compiledLore = new ArrayList<>();

						if (isFeatured) {
							// Conditionally add featured lore (only if in FEATURED view as is owner)
							List<String> featuredLore = manager.emptySection.getStringList("display.featured-lore",
									new ArrayList<>()); // featured-only lore
							if (isOwner && featuredLore != null && !featuredLore.isEmpty()) {
								featuredLore.forEach(line -> compiledLore.add(AdventureHelper.miniMessageToJson(line)));
							} else {
								// else add the non owner lore
								List<String> nonOwnerLore = manager.emptySection
										.getStringList("display.featured-member-lore", new ArrayList<>());
								if (nonOwnerLore != null && !nonOwnerLore.isEmpty()) {
									nonOwnerLore
											.forEach(line -> compiledLore.add(AdventureHelper.miniMessageToJson(line)));
								}
							}
						} else {
							List<String> baseLore = manager.emptySection.getStringList("display.lore",
									new ArrayList<>());
							// Always add general lore if defined
							if (baseLore != null && !baseLore.isEmpty()) {
								baseLore.forEach(line -> compiledLore.add(AdventureHelper.miniMessageToJson(line)));
							}
						}

						// Apply the composed lore (or clear if empty)
						baseEmptyItem.lore(compiledLore.isEmpty() ? Collections.emptyList() : compiledLore);

						final VisitDynamicGUIElement element = new VisitDynamicGUIElement(symbol,
								baseEmptyItem.loadCopy());

						// Skip if we've assigned all entries already
						if (currentIndex >= entries.size()) {
							futures.add(CompletableFuture.completedFuture(element));
							layoutSlotIndex++;
							continue;
						}

						VisitEntry entry = entries.get(currentIndex);
						UUID uuid = entry.ownerId();

						// Ensure this entry is supposed to be placed in this layout slot
						if (existingSlotAssignments.get(uuid) != currentIndex) {
							layoutSlotIndex++;
							continue;
						}

						int rank = currentIndex + 1;

						UserData userData = ownerMap.get(uuid);
						if (userData == null || !userData.getHellblockData().hasHellblock()
								|| userData.getHellblockData().getOwnerUUID() == null) {
							futures.add(CompletableFuture.completedFuture(element));
							layoutSlotIndex++;
							continue;
						}

						HellblockData data = userData.getHellblockData();

						if (data.isLocked()) {
							futures.add(CompletableFuture.completedFuture(element));
							layoutSlotIndex++;
							continue;
						}

						element.setFeaturedUntil(data.getVisitData().getFeaturedUntil());

						String playerName = data.getResolvedOwnerName();
						String islandName = data.getDisplaySettings().getIslandName();
						String islandBio = data.getDisplaySettings().getIslandBio();

						// Set context args
						Context<Integer> perIslandCtx = Context.island(data.getIslandId())
								.arg(ContextKeys.VISIT_UUID, uuid.toString()).arg(ContextKeys.VISIT_NAME, playerName)
								.arg(ContextKeys.VISIT_LEVEL, data.getIslandLevel())
								.arg(ContextKeys.VISIT_COUNT, entry.visits()).arg(ContextKeys.VISIT_RANK, rank)
								.arg(ContextKeys.VISIT_ISLAND_NAME, islandName)
								.arg(ContextKeys.VISIT_ISLAND_BIO, islandBio);

						Context<Player> combinedCtx = context.merge(perIslandCtx);
						Item<ItemStack> filledItem = manager.instance.getItemManager()
								.wrap(manager.filledIcon.build(combinedCtx));

						// Check if this entry belongs to the current player
						boolean isOwnerEntry = uuid.equals(context.holder().getUniqueId());

						if (isOwnerEntry) {
							// Tag the item for identification
							filledItem.setTag("custom", "visit_gui", "owner_entry", 1);

							// Append custom lore if defined
							if (manager.filledSection.contains("display.self-indicator-additional-lore")) {
								List<String> extraLore = manager.filledSection
										.getStringList("display.self-indicator-additional-lore");
								List<String> existingLore = new ArrayList<>(
										filledItem.lore().orElseGet(ArrayList::new));
								extraLore.forEach(line -> existingLore.add(AdventureHelper.miniMessageToJson(line)));
								filledItem.lore(existingLore);
							}
						}

						List<String> newLore = new ArrayList<>();

						// Use plain text version of the bio for wrapping
						String plainBio = AdventureHelper
								.componentToPlainText(AdventureHelper.miniMessageToComponent(islandBio));

						// Determine max characters per line (or use pixel-accurate wrapping if needed)
						int wrapWidth = manager.instance.getConfigManager().wrapLength(); // or any appropriate width

						for (String line : manager.filledSection.getStringList("display.lore")) {
							if (!line.contains("{island_bio}")) {
								// Simple replace for all other lines
								newLore.add(AdventureHelper.miniMessageToJson(line.replace("{player}", playerName)
										.replace("{visits}", String.valueOf(entry.visits()))
										.replace("{uuid}", uuid.toString()).replace("{rank}", String.valueOf(rank))
										.replace("{level}", String.valueOf(data.getIslandLevel()))
										.replace("{island_name}", islandName)));
								continue;
							}

							// --- Special case: line contains {island_bio} ---
							String[] split = line.split("\\{island_bio\\}", 2);
							String prefix = split[0];
							String suffix = split.length > 1 ? split[1] : "";

							// Wrap the plain text bio into lines
							List<String> wrapped = TextWrapUtils.wrapLineWithIndent(plainBio, wrapWidth, 0);

							for (int i = 0; i < wrapped.size(); i++) {
								String wrappedLine = wrapped.get(i);

								// Only append suffix on first line (if needed)
								String fullLine = prefix + wrappedLine + (i == 0 ? suffix : "");
								// Re-apply placeholders
								fullLine = fullLine.replace("{player}", playerName)
										.replace("{visits}", String.valueOf(entry.visits()))
										.replace("{uuid}", uuid.toString()).replace("{rank}", String.valueOf(rank))
										.replace("{level}", String.valueOf(data.getIslandLevel()))
										.replace("{island_name}", islandName);

								newLore.add(AdventureHelper.miniMessageToJson(fullLine));
							}
						}

						String name = manager.filledSection.getString("display.name").replace("{player}", playerName)
								.replace("{visits}", String.valueOf(entry.visits())).replace("{uuid}", uuid.toString())
								.replace("{rank}", String.valueOf(rank))
								.replace("{level}", String.valueOf(data.getIslandLevel()))
								.replace("{island_name}", islandName).replace("{island_bio}", islandBio);

						filledItem.displayName(AdventureHelper.miniMessageToJson(name));
						filledItem.lore(newLore);

						CompletableFuture<VisitDynamicGUIElement> future = CompletableFuture.supplyAsync(() -> {
							try {
								if (filledItem.getItem().getType() == Material.PLAYER_HEAD) {
									String texture = cachedSkullTextures.computeIfAbsent(uuid, id -> {
										try {
											GameProfile profile = GameProfileBuilder.fetch(id);
											return profile.getProperties().get("textures").iterator().next().getValue();
										} catch (Exception ex) {
											manager.instance.getPluginLogger().warn("Failed to fetch profile for " + id,
													ex);
											return null;
										}
									});

									if (texture != null) {
										filledItem.skull(texture);
										element.setSkullTexture(texture); // <- store for reuse
									}
								}

								ItemStack loaded = filledItem.loadCopy();
								element.setItemStack(loaded);
								element.setUUID(uuid);
							} catch (Exception e) {
								manager.instance.getPluginLogger().warn("Error processing skull for " + uuid, e);
							}
							return element;
						});
						futures.add(future);
						layoutSlotIndex++;
					}
				}
			}

			// When all skull elements are loaded, update the GUI once
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
				List<VisitDynamicGUIElement> elements = futures.stream().map(CompletableFuture::join).toList();

				manager.instance.getScheduler().executeSync(() -> {
					clearDynamicElements(); // reset before repopulating
					elements.forEach(this::addElement);
					init(); // rebuild slot map and inventory placement
					refresh(); // update visuals
				});
			});
		});
	}

	@NotNull
	public CompletableFuture<Boolean> updateFeaturedCountdownLore() {
		if (getCurrentSorter() != VisitSorter.FEATURED)
			return CompletableFuture.completedFuture(false);

		setRefreshReason(RefreshReason.PERIODIC_UPDATE);

		AtomicBoolean expiredFound = new AtomicBoolean(false);
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Map.Entry<Integer, VisitGUIElement> entry : itemsSlotMap.entrySet()) {
			VisitGUIElement element = entry.getValue();
			if (!(element instanceof VisitDynamicGUIElement dynElem))
				continue;

			UUID uuid = dynElem.getUUID();
			if (uuid == null)
				continue;

			long until = dynElem.getFeaturedUntil();
			if (until > 0 && until <= System.currentTimeMillis()) {
				expiredFound.set(true);
				continue; // still allow other entries to update
			}

			// Add fetch task to futures list
			CompletableFuture<Void> future = manager.instance.getStorageManager()
					.getCachedUserDataWithFallback(uuid, false).thenAccept(optData -> {
						if (optData.isEmpty())
							return;

						UserData userData = optData.get();
						VisitData visitData = userData.getHellblockData().getVisitData();

						long remainingMillis = visitData.getFeaturedUntil() - System.currentTimeMillis();
						if (remainingMillis <= 0)
							return;

						long remainingSeconds = remainingMillis / 1000L;
						String countdown = manager.instance.getCooldownManager().getFormattedCooldown(remainingSeconds);

						// Build a fresh featured context for this island
						Context<Integer> featuredCtx = Context.island(userData.getHellblockData().getIslandId())
								.arg(ContextKeys.FEATURED_TIME, remainingSeconds)
								.arg(ContextKeys.FEATURED_TIME_FORMATTED, countdown);

						// Combine with player context when rendering placeholders
						context.merge(featuredCtx);

						// Wrap the *existing* item in the GUI
						Item<ItemStack> wrapped = manager.instance.getItemManager()
								.wrap(dynElem.getItemStack().clone());
						List<String> lore = new ArrayList<>(wrapped.lore().orElseGet(ArrayList::new));

						// Remove previous countdown lines
						lore.removeIf(line -> line.contains("{featured_time}"));

						// Append featured lore if defined
						if (manager.filledSection.contains("display.featured-additional-lore")) {
							List<String> featuredLore = manager.filledSection
									.getStringList("display.featured-additional-lore");
							featuredLore.forEach(line -> {
								line = line.replace("{featured_time}", countdown);
								lore.add(AdventureHelper.miniMessageToJson(line));
							});
						}

						wrapped.lore(lore);

						// Preserve skull texture
						String skullTexture = dynElem.getSkullTexture();
						if (skullTexture != null) {
							wrapped.skull(skullTexture);
						}

						dynElem.setItemStack(wrapped.loadCopy());

						manager.instance.getScheduler()
								.executeSync(() -> inventory.setItem(entry.getKey(), dynElem.getItemStack().clone()));
					}).exceptionally(ex -> {
						manager.instance.getPluginLogger().severe("Failed to update featured countdown lore for "
								+ context.holder().getName() + ": " + ex.getMessage());
						return null;
					});

			futures.add(future);
		}

		// Wait for all updates, then apply result
		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			if (expiredFound.get()) {
				manager.instance.getScheduler().executeSync(() -> {
					this.refreshAndRepopulate();

					Player viewer = context.holder();
					AdventureHelper.playSound(manager.instance.getSenderFactory().getAudience(viewer),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.note_block.bell"),
									Sound.Source.MASTER, 1.0f, 1.0f));
				});
			}
			return expiredFound.get();
		});
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The VisitGUI instance.
	 */
	public VisitGUI refresh() {
		// Only update title when first opened or when sorter changes
		// Skip it on quick refreshes triggered by repopulation
		if (getRefreshReason() == RefreshReason.OPENING || getRefreshReason() == RefreshReason.SORT_CHANGE) {
			updateTitle();
		}

		VisitDynamicGUIElement backElement = (VisitDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			if (showBackIcon) {
				backElement.setItemStack(manager.backIcon.build(context));
			} else {
				backElement.setItemStack(getDecorativePlaceholderForSlot(manager.backSlot));
			}
		}
		manager.sortingIcons.forEach(visit -> {
			VisitDynamicGUIElement visitElement = (VisitDynamicGUIElement) getElement(visit.left());
			if (visitElement != null && !visitElement.getSlots().isEmpty()) {
				visitElement.setItemStack(visit.right().left().build(context));
			}
		});
		itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof VisitDynamicGUIElement)
				.forEach(entry -> {
					VisitDynamicGUIElement dynamicGUIElement = (VisitDynamicGUIElement) entry.getValue();
					this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});

		setRefreshReason(RefreshReason.MANUAL);
		return this;
	}

	/**
	 * Returns an ItemStack to use as the decorative placeholder for `slot`.
	 * Preferred order: 1) decorativeIcons.get(symbolForSlot) if present 2) first
	 * entry in decorativeIcons 3) hard fallback gray pane
	 */
	protected ItemStack getDecorativePlaceholderForSlot(int slot) {
		final VisitGUIElement element = getElement(slot);
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

	public void refreshAndRepopulate() {
		Consumer<List<VisitEntry>> callback = entries -> manager.instance.getScheduler().executeSync(() -> {
			setRefreshReason(RefreshReason.REPOPULATE);
			populateVisitEntries(entries, currentSorter, getCurrentPage()).thenRun(() -> refresh());
		});

		if (getCurrentSorter() == VisitSorter.FEATURED) {
			manager.instance.getVisitManager().getFeaturedIslands(manager.getFilledSlotCount()).thenAccept(callback);
		} else {
			manager.instance.getVisitManager().getTopIslands(currentSorter.getVisitFunction(),
					manager.getFilledSlotCount(), callback);
		}
	}

	@Override
	protected int getLeftIconSlot() {
		VisitGUIElement element = getElement(manager.leftSlot);
		return element != null && !element.getSlots().isEmpty() ? element.getSlots().get(0) : -1;
	}

	@Override
	protected int getRightIconSlot() {
		VisitGUIElement element = getElement(manager.rightSlot);
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

	public enum RefreshReason {
		/** Initial GUI opening */
		OPENING,

		/** Sorting type changed (e.g. clicked a sort icon) */
		SORT_CHANGE,

		/** Repopulating data after a fetch or update */
		REPOPULATE,

		/** Routine refresh like countdown or minor update */
		PERIODIC_UPDATE,

		/** Manual refresh (e.g. from code logic) */
		MANUAL;
	}
}