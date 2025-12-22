package com.swiftlicious.hellblock.gui.leaderboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.gui.PaginatedGUI;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.format.NamedTextColor;

public class LeaderboardGUI extends PaginatedGUI<LeaderboardGUIElement> {

	private final Map<Character, LeaderboardGUIElement> itemsCharMap;
	private final Map<Integer, LeaderboardGUIElement> itemsSlotMap;
	private final LeaderboardGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;
	protected final boolean showBackIcon;

	private Map<Integer, Float> lastTopSnapshot = new HashMap<>();
	protected SchedulerTask refreshTask;

	private volatile long lastUpdateTimestamp = System.currentTimeMillis();

	private final Map<UUID, Long> lastFlashTime = new ConcurrentHashMap<>();

	// Tracks the last known rank for each player
	private final Map<UUID, Integer> previousRanks = new ConcurrentHashMap<>();

	private final Map<UUID, String> cachedSkullTextures = new ConcurrentHashMap<>(); // Skull cache

	public LeaderboardGUI(LeaderboardGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner, boolean showBackIcon, @Nullable CustomItem leftIcon,
			@Nullable Action<Player>[] leftActions, @Nullable CustomItem rightIcon,
			@Nullable Action<Player>[] rightActions) {
		super(leftIcon, leftActions, rightIcon, rightActions);
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.showBackIcon = showBackIcon;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new LeaderboardGUIHolder();
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
				LeaderboardGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * width);
					itemsSlotMap.put(index + line * width, element);
				}
			}
			line++;
		}

		// Only auto-paginate if NOT using multi-page mode
		if (manager.pageLayouts.isEmpty() && (rightIconItem != null || leftIconItem != null)) {
			List<LeaderboardGUIElement> leaderboardElements = new ArrayList<>();

			for (LeaderboardGUIElement element : itemsCharMap.values()) {
				if (element.getSymbol() != manager.backSlot) {
					leaderboardElements.add(element);
				}
			}

			int itemsPerPage = manager.layout.length * 9 - 2; // reserve for arrows
			List<List<LeaderboardGUIElement>> pages = new ArrayList<>();

			for (int i = 0; i < leaderboardElements.size(); i += itemsPerPage) {
				pages.add(leaderboardElements.subList(i, Math.min(i + itemsPerPage, leaderboardElements.size())));
			}

			setPages(pages); // PaginatedGUI base method
		}

		// Set initial inventory items
		itemsSlotMap.forEach((slot, element) -> this.inventory.setItem(slot, element.getItemStack().clone()));
	}

	private void buildPaginated() {
		List<List<LeaderboardGUIElement>> allPages = new ArrayList<>();
		int width = (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9);

		// Each entry in pageLayouts is one "page"
		for (Map.Entry<Integer, String[]> entry : manager.pageLayouts.entrySet()) {
			List<LeaderboardGUIElement> pageElements = new ArrayList<>();
			String[] layout = entry.getValue();
			int line = 0;

			for (String content : layout) {
				for (int index = 0; index < width; index++) {
					char symbol = (index < content.length()) ? content.charAt(index) : ' ';
					LeaderboardGUIElement element = itemsCharMap.get(symbol);
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

	public LeaderboardGUI addElement(LeaderboardGUIElement... elements) {
		for (LeaderboardGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public LeaderboardGUI build() {
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
	public LeaderboardGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public LeaderboardGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	public void clearDynamicElements() {
		Set<Character> excludeSymbols = Set.of(manager.backSlot, manager.leftSlot, manager.rightSlot);

		itemsSlotMap.entrySet().removeIf(entry -> {
			LeaderboardGUIElement element = entry.getValue();
			boolean shouldRemove = element instanceof LeaderboardDynamicGUIElement
					&& !excludeSymbols.contains(element.getSymbol());

			if (shouldRemove) {
				inventory.setItem(entry.getKey(), null);
			}
			return shouldRemove;
		});

		itemsCharMap.entrySet().removeIf(entry -> entry.getValue() instanceof LeaderboardDynamicGUIElement
				&& !excludeSymbols.contains(entry.getValue().getSymbol()));
	}

	public CompletableFuture<Void> refreshTopIslands(int limit) {
		return manager.instance.getIslandLevelManager().getTopHellblocks(limit).thenCompose(topIslands -> {
			if (!hasTopIslandsChanged(topIslands)) {
				return CompletableFuture.completedFuture(null); // skip rebuild
			}

			lastTopSnapshot = new HashMap<>(topIslands);
			lastUpdateTimestamp = System.currentTimeMillis();

			List<CompletableFuture<LeaderboardDynamicGUIElement>> futures = new ArrayList<>();
			int rank = 1;

			for (Map.Entry<Integer, Float> entry : topIslands.entrySet()) {
				int islandId = entry.getKey();
				float level = entry.getValue();
				int position = rank++;

				CompletableFuture<LeaderboardDynamicGUIElement> future = manager.instance.getStorageManager()
						.getOfflineUserDataByIslandId(islandId, false)
						.thenApply(optData -> buildElement(optData, islandId, level, position));

				futures.add(future);
			}

			// Fill unclaimed slots (if less than layout {T} count)
			int placeholdersNeeded = manager.getTopSlotCount() - topIslands.size();
			for (int i = 0; i < placeholdersNeeded; i++) {
				ItemStack unclaimed = buildPlaceholderElement(i);
				futures.add(CompletableFuture
						.completedFuture(new LeaderboardDynamicGUIElement(manager.topSlot, unclaimed)));
			}

			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
				List<LeaderboardDynamicGUIElement> elements = futures.stream().map(CompletableFuture::join).toList();

				manager.instance.getScheduler().executeSync(() -> {
					clearDynamicElements();
					elements.forEach(this::addElement);
					init();
					refresh();
				});
			});
		});
	}

	public void startAutoRefresh(long intervalTicks, int limit) {
		if (refreshTask != null && !refreshTask.isCancelled()) {
			refreshTask.cancel();
		}

		// Schedule async task
		refreshTask = manager.instance.getScheduler().asyncRepeating(() -> {
			refreshTopIslands(limit).exceptionally(ex -> {
				manager.instance.getPluginLogger().warn("Failed to refresh top islands", ex);
				return null;
			});
		}, intervalTicks, intervalTicks, TimeUnit.SECONDS);
	}

	private LeaderboardDynamicGUIElement buildElement(Optional<UserData> optData, int islandId, float level,
			int position) {
		if (optData.isEmpty()) {
			ItemStack placeholder = buildPlaceholderElement(position);
			return new LeaderboardDynamicGUIElement(manager.topSlot, placeholder);
		}

		UserData userData = optData.get();
		UUID uuid = userData.getUUID();
		String name = userData.getName();

		int oldRank = previousRanks.getOrDefault(uuid, position);
		int rankDelta = oldRank - position;
		previousRanks.put(uuid, position);

		String rankChangeSymbol = rankDelta > 0 ? "▲" : rankDelta < 0 ? "▼" : "—";
		String rankChangeColor = rankDelta > 0 ? "<green>" : rankDelta < 0 ? "<red>" : "<gray>";
		String rankChangeAmount = rankDelta != 0 ? String.valueOf(Math.abs(rankDelta)) : "0";

		String timeAgo = formatTimeAgo(lastUpdateTimestamp);

		Map<String, String> placeholders = Map.of("position", String.valueOf(position), "player", name, "level",
				String.valueOf(level), "rank_change", rankChangeSymbol, "rank_color", rankChangeColor, "rank_delta",
				rankChangeAmount, "last_updated", timeAgo);

		Item<ItemStack> wrapped = manager.instance.getItemManager().wrap(manager.topIcon.build(context));

		String rawName = manager.topSection.getString("display.name", "");
		wrapped.displayName(AdventureHelper.miniMessageToJson(replacePlaceholders(rawName, placeholders)));

		List<String> loreLines = manager.topSection.getStringList("display.lore");
		if (loreLines != null && !loreLines.isEmpty()) {
			List<String> processedLore = loreLines.stream()
					.map(line -> AdventureHelper.miniMessageToJson(replacePlaceholders(line, placeholders))).toList();
			wrapped.lore(processedLore);
		}

		if (wrapped.getItem().getType() == Material.PLAYER_HEAD) {
			String texture = cachedSkullTextures.computeIfAbsent(uuid, id -> {
				try {
					GameProfile profile = GameProfileBuilder.fetch(id);
					return profile.getProperties().get("textures").iterator().next().getValue();
				} catch (Exception ex) {
					manager.instance.getPluginLogger().warn("Failed to fetch skull for " + id, ex);
					return null;
				}
			});
			if (texture != null) {
				wrapped.skull(texture);
			}
		}

		LeaderboardDynamicGUIElement element = new LeaderboardDynamicGUIElement(manager.topSlot, wrapped.loadCopy(),
				uuid);
		element.setUUID(uuid);
		element.setSkullTexture(cachedSkullTextures.get(uuid));
		return element;
	}

	private boolean hasTopIslandsChanged(Map<Integer, Float> newTop) {
		if (newTop.size() != lastTopSnapshot.size())
			return true;
		for (Map.Entry<Integer, Float> e : newTop.entrySet()) {
			Float oldVal = lastTopSnapshot.get(e.getKey());
			if (oldVal == null || Math.abs(oldVal - e.getValue()) > 0.001F) {
				return true;
			}
		}
		return false;
	}

	public void handleLiveLeaderboardUpdate(Map<Integer, Float> newTopIslands) {
		// Skip if no real change
		if (!hasTopIslandsChanged(newTopIslands))
			return;

		lastTopSnapshot = new HashMap<>(newTopIslands);
		lastUpdateTimestamp = System.currentTimeMillis();

		List<CompletableFuture<LeaderboardDynamicGUIElement>> futures = new ArrayList<>();
		int rank = 1;

		for (Map.Entry<Integer, Float> entry : newTopIslands.entrySet()) {
			int islandId = entry.getKey();
			float level = entry.getValue();
			int position = rank++;

			CompletableFuture<LeaderboardDynamicGUIElement> future = manager.instance.getStorageManager()
					.getOfflineUserDataByIslandId(islandId, false).thenApply(optData -> {
						if (optData.isEmpty()) {
							return new LeaderboardDynamicGUIElement(manager.topSlot, buildPlaceholderElement(position));
						}

						UserData userData = optData.get();
						UUID uuid = userData.getUUID();
						String name = userData.getName();

						// --- Compute Rank Change ---
						int oldRank = previousRanks.getOrDefault(uuid, position);
						int rankDelta = oldRank - position;
						previousRanks.put(uuid, position);

						String rankChangeSymbol;
						String rankChangeColor;

						if (rankDelta > 0) { // moved up
							rankChangeSymbol = "▲";
							rankChangeColor = "<green>";
						} else if (rankDelta < 0) { // moved down
							rankChangeSymbol = "▼";
							rankChangeColor = "<red>";
						} else { // same
							rankChangeSymbol = "—";
							rankChangeColor = "<gray>";
						}

						if (rankDelta != 0) {
							Player p = context.holder();
							AdventureHelper.playSound(manager.instance.getSenderFactory().getAudience(p), rankDelta > 0
									? Sound.sound(Key.key("minecraft:entity.player.levelup"), Source.PLAYER, 1.0f, 1.2f)
									: Sound.sound(Key.key("minecraft:entity.villager.no"), Source.PLAYER, 1.0f, 1.2f));

							long now = System.currentTimeMillis();
							long last = lastFlashTime.getOrDefault(uuid, 0L);
							if (now - last > 1500) { // 1.5 seconds cooldown per player
								lastFlashTime.put(uuid, now);

								manager.instance.getScheduler().executeSync(() -> {
									itemsSlotMap.forEach((slot, element) -> {
										if (manager.decorativeIcons.containsKey(element.getSymbol())) {
											flashSlotColor(slot, rankDelta);
										}
									});
								});
							}
						}

						String timeAgo = formatTimeAgo(lastUpdateTimestamp);

						// --- Placeholders ---
						Map<String, String> placeholders = Map.of("position", String.valueOf(position), "player", name,
								"level", String.format("%.1f", level), "rank_change", rankChangeSymbol, "rank_color",
								rankChangeColor, "rank_delta",
								(rankDelta != 0 ? String.valueOf(Math.abs(rankDelta)) : "0"), "last_updated", timeAgo);

						// --- Build Item ---
						Item<ItemStack> wrapped = manager.instance.getItemManager()
								.wrap(manager.topIcon.build(context));

						String rawName = manager.topSection.getString("display.name", "");
						// Add color + arrow before the name
						String modifiedName = rankChangeColor + rankChangeSymbol + " "
								+ replacePlaceholders(rawName, placeholders);
						wrapped.displayName(AdventureHelper.miniMessageToJson(modifiedName));

						List<String> loreLines = manager.topSection.getStringList("display.lore");
						if (loreLines != null && !loreLines.isEmpty()) {
							List<String> processedLore = loreLines.stream().map(line -> {
								// Add delta note if not already in lore
								if (line.contains("{rank_change}")) {
									return AdventureHelper.miniMessageToJson(replacePlaceholders(line, placeholders));
								}
								return AdventureHelper.miniMessageToJson(replacePlaceholders(
										line + " " + rankChangeColor + rankChangeSymbol, placeholders));
							}).toList();
							wrapped.lore(processedLore);
						}

						if (wrapped.getItem().getType() == Material.PLAYER_HEAD) {
							String texture = cachedSkullTextures.computeIfAbsent(uuid, id -> {
								try {
									GameProfile profile = GameProfileBuilder.fetch(id);
									return profile.getProperties().get("textures").iterator().next().getValue();
								} catch (Exception ex) {
									manager.instance.getPluginLogger().warn("Failed to fetch skull for " + id, ex);
									return null;
								}
							});
							if (texture != null)
								wrapped.skull(texture);
						}

						// Build element
						LeaderboardDynamicGUIElement element = new LeaderboardDynamicGUIElement(manager.topSlot,
								wrapped.loadCopy(), uuid);
						element.setUUID(uuid);
						element.setSkullTexture(cachedSkullTextures.get(uuid));
						return element;
					});

			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			List<LeaderboardDynamicGUIElement> elements = futures.stream().map(CompletableFuture::join).toList();

			manager.instance.getScheduler().executeSync(() -> {
				clearDynamicElements();
				elements.forEach(this::addElement);
				init();
				refresh();
			});
		});
	}

	private String replacePlaceholders(@Nullable String line, Map<String, String> placeholders) {
		if (line == null)
			return "";
		String result = line;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			result = result.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		return result;
	}

	private String formatTimeAgo(long past) {
		long diff = System.currentTimeMillis() - past;

		String recentColor = "<yellow>";
		String staleColor = "<gray>";

		String color;

		if (diff < 2000) {
			// "just now"
			color = recentColor;
			String text = manager.instance.getTranslationManager()
					.miniMessageTranslation(MessageConstants.FORMAT_RECENT.build().key());
			return color + text;
		}

		long seconds = diff / 1000;
		long minutes = seconds / 60;

		// Decide color based on staleness threshold (>=5 minutes)
		color = (minutes >= 5) ? staleColor : recentColor;

		String hourFormat = manager.instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
		String minuteFormat = manager.instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key());
		String secondFormat = manager.instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());
		String agoFormat = manager.instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_AGO.build().key());

		if (minutes == 0) {
			return color + seconds + secondFormat + " " + agoFormat;
		} else if (minutes < 2) {
			return color + String.format("%d" + minuteFormat + "%02d" + secondFormat + " " + agoFormat, minutes,
					seconds % 60);
		} else if (minutes < 60) {
			return color + String.format("%d" + minuteFormat + " " + agoFormat, minutes);
		} else {
			long hours = minutes / 60;
			return color
					+ String.format("%d" + hourFormat + "%02d" + minuteFormat + " " + agoFormat, hours, minutes % 60);
		}
	}

	private ItemStack buildPlaceholderElement(int position) {
		Map<String, String> placeholders = Map.of("position", String.valueOf(position));

		Item<ItemStack> wrapped = manager.instance.getItemManager().wrap(manager.placeholderIcon.build(context));

		// Display name
		String rawName = manager.placeholderSection.getString("display.name", "");
		wrapped.displayName(AdventureHelper.miniMessageToJson(replacePlaceholders(rawName, placeholders)));

		// Lore
		List<String> loreLines = manager.topSection.getStringList("display.lore");
		if (loreLines != null && !loreLines.isEmpty()) {
			List<String> processedLore = new ArrayList<>();
			for (String line : loreLines) {
				processedLore.add(AdventureHelper.miniMessageToJson(replacePlaceholders(line, placeholders)));
			}
			wrapped.lore(processedLore);
		}

		return wrapped.loadCopy();
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The VisitGUI instance.
	 */
	public LeaderboardGUI refresh() {
		LeaderboardDynamicGUIElement backElement = (LeaderboardDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			if (showBackIcon) {
				backElement.setItemStack(manager.backIcon.build(context));
			} else {
				backElement.setItemStack(getDecorativePlaceholderForSlot(manager.backSlot));
			}
		}
		itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof LeaderboardDynamicGUIElement)
				.forEach(entry -> {
					LeaderboardDynamicGUIElement dynamicGUIElement = (LeaderboardDynamicGUIElement) entry.getValue();
					this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});
		return this;
	}

	/**
	 * Returns an ItemStack to use as the decorative placeholder for `slot`.
	 * Preferred order: 1) decorativeIcons.get(symbolForSlot) if present 2) first
	 * entry in decorativeIcons 3) hard fallback gray pane
	 */
	protected ItemStack getDecorativePlaceholderForSlot(int slot) {
		final LeaderboardGUIElement element = getElement(slot);
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

	private void flashSlotColor(int slot, int rankDelta) {
		boolean up = rankDelta > 0;
		int pulses = Math.min(Math.abs(rankDelta), 5); // cap at 5 flashes
		Material paneMaterial = up ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

		// Build flash pane using wrapped item system
		Item<ItemStack> flashPane = manager.instance.getItemManager().wrap(new ItemStack(paneMaterial));
		String symbol = up ? "▲ +" : "▼ -";
		flashPane.displayName((up ? NamedTextColor.GREEN : NamedTextColor.RED) + symbol + Math.abs(rankDelta));

		ItemStack flashCopy = flashPane.loadCopy();
		ItemStack original = inventory.getItem(slot);
		if (original == null)
			return;

		// Schedule pulsing animation
		for (int i = 0; i < pulses * 2; i++) {
			final boolean showFlash = i % 2 == 0;
			manager.instance.getScheduler().sync().runLater(() -> {
				inventory.setItem(slot, showFlash ? flashCopy : original);
			}, i * 5L, LocationUtils.getAnyLocationInstance());
		}

		// Ensure it resets cleanly to original
		manager.instance.getScheduler().sync().runLater(() -> {
			inventory.setItem(slot, original);
		}, pulses * 10L, LocationUtils.getAnyLocationInstance());
	}

	@Override
	protected int getLeftIconSlot() {
		LeaderboardGUIElement element = getElement(manager.leftSlot);
		return element != null && !element.getSlots().isEmpty() ? element.getSlots().get(0) : -1;
	}

	@Override
	protected int getRightIconSlot() {
		LeaderboardGUIElement element = getElement(manager.rightSlot);
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