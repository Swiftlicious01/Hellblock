package com.swiftlicious.hellblock.gui.visit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitEntry;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.utils.TextWrapUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class VisitGUI {

	private final Map<Character, VisitGUIElement> itemsCharMap;
	private final Map<Integer, VisitGUIElement> itemsSlotMap;
	private final VisitGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected VisitSorter currentSorter;
	protected boolean showBackIcon;

	private final Map<UUID, String> cachedSkullTextures = new ConcurrentHashMap<>(); // Skull cache

	public VisitGUI(VisitGUIManager manager, VisitSorter currentSorter, Context<Player> context,
			HellblockData hellblockData, boolean showBackIcon) {
		this.manager = manager;
		this.currentSorter = currentSorter;
		this.context = context;
		this.hellblockData = hellblockData;
		this.showBackIcon = showBackIcon;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new VisitGUIHolder();
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
				VisitGUIElement element = itemsCharMap.get(symbol);
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

	public VisitGUI addElement(VisitGUIElement... elements) {
		for (VisitGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public VisitGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
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
		itemsSlotMap.entrySet().removeIf(entry -> entry.getValue() instanceof VisitDynamicGUIElement);
	}

	public VisitSorter getCurrentSorter() {
		return currentSorter;
	}

	public void setCurrentSorter(VisitSorter sorter) {
		this.currentSorter = sorter;
	}

	public void populateVisitEntries(List<VisitEntry> entries, VisitSorter sorter) {
		// Prevent redundant sort triggers
		if (sorter == currentSorter) {
			AdventureHelper.playSound(manager.instance.getSenderFactory().getAudience(context.holder()),
					Sound.sound(Key.key("minecraft:entity.villager.no"), Sound.Source.PLAYER, 1f, 1f));
			return;
		}
		this.currentSorter = sorter;

		manager.instance.getCoopManager().getCachedIslandOwnerData().thenAccept(ownerSet -> {
			Map<UUID, UserData> ownerMap = ownerSet.stream()
					.collect(Collectors.toMap(UserData::getUUID, Function.identity()));

			List<CompletableFuture<VisitDynamicGUIElement>> futures = new ArrayList<>();
			int slotIndex = 0;

			for (int row = 0; row < manager.layout.length; row++) {
				for (int col = 0; col < manager.layout[row].length(); col++) {
					char symbol = manager.layout[row].charAt(col);
					if (symbol != manager.filledSlot)
						continue;

					final int currentIndex = slotIndex;
					Item<ItemStack> baseEmptyItem = manager.instance.getItemManager()
							.wrap(manager.emptyIcon.build(context));

					boolean isFeatured = sorter == VisitSorter.FEATURED;
					boolean isOwner = hellblockData.getOwnerUUID() != null
							&& context.holder().getUniqueId().equals(hellblockData.getOwnerUUID());

					// Only retain lore if FEATURED view and player is island owner
					if (!(isFeatured && isOwner)) {
						baseEmptyItem.lore(Collections.emptyList());
					}

					final VisitDynamicGUIElement element = new VisitDynamicGUIElement(symbol, baseEmptyItem.loadCopy());

					if (currentIndex >= entries.size()) {
						futures.add(CompletableFuture.completedFuture(element));
						slotIndex++;
						continue;
					}

					VisitEntry entry = entries.get(currentIndex);
					UUID uuid = entry.islandOwner();
					int rank = currentIndex + 1;

					UserData user = ownerMap.get(uuid);
					if (user == null || user.getHellblockData().getOwnerUUID() == null) {
						futures.add(CompletableFuture.completedFuture(element));
						slotIndex++;
						continue;
					}

					HellblockData data = user.getHellblockData();

					if (data.isLocked()) {
						futures.add(CompletableFuture.completedFuture(element));
						slotIndex++;
						continue;
					}

					String playerName = data.getResolvedOwnerName();
					String islandName = data.getDisplaySettings().getIslandName();
					String islandBio = data.getDisplaySettings().getIslandBio();

					// Set context args
					context.arg(ContextKeys.VISIT_UUID, uuid.toString()).arg(ContextKeys.VISIT_NAME, playerName)
							.arg(ContextKeys.VISIT_LEVEL, data.getLevel()).arg(ContextKeys.VISIT_COUNT, entry.visits())
							.arg(ContextKeys.VISIT_RANK, rank).arg(ContextKeys.VISIT_ISLAND_NAME, islandName)
							.arg(ContextKeys.VISIT_ISLAND_BIO, islandBio);

					Item<ItemStack> filledItem = manager.instance.getItemManager()
							.wrap(manager.filledIcon.build(context));

					List<String> newLore = new ArrayList<>();

					// Use plain text version of the bio for wrapping
					String plainBio = PlainTextComponentSerializer.plainText()
							.serialize(AdventureHelper.getMiniMessage().deserialize(islandBio));

					// Determine max characters per line (or use pixel-accurate wrapping if needed)
					int wrapWidth = manager.instance.getConfigManager().wrapLength(); // or any appropriate width

					for (String line : manager.filledSection.getStringList("display.lore")) {
						if (!line.contains("{island_bio}")) {
							// Simple replace for all other lines
							newLore.add(AdventureHelper.miniMessageToJson(line.replace("{player}", playerName)
									.replace("{visits}", String.valueOf(entry.visits()))
									.replace("{uuid}", uuid.toString()).replace("{rank}", String.valueOf(rank))
									.replace("{level}", String.valueOf(data.getLevel()))
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
									.replace("{level}", String.valueOf(data.getLevel()))
									.replace("{island_name}", islandName);

							newLore.add(AdventureHelper.miniMessageToJson(fullLine));
						}
					}

					String name = manager.filledSection.getString("display.name").replace("{player}", playerName)
							.replace("{visits}", String.valueOf(entry.visits())).replace("{uuid}", uuid.toString())
							.replace("{rank}", String.valueOf(rank)).replace("{level}", String.valueOf(data.getLevel()))
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
					slotIndex++;
				}
			}

			// When all skull elements are loaded, update the GUI once
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
				List<VisitDynamicGUIElement> elements = futures.stream().map(CompletableFuture::join).toList();

				manager.instance.getScheduler().executeSync(() -> {
					clearDynamicElements(); // Optional if you want to reset before repopulating
					elements.forEach(this::addElement);
					refresh();
				});
			});
		});
	}

	public void updateFeaturedCountdownLore() {
		if (currentSorter != VisitSorter.FEATURED)
			return;

		for (Map.Entry<Integer, VisitGUIElement> entry : itemsSlotMap.entrySet()) {
			VisitGUIElement element = entry.getValue();
			if (!(element instanceof VisitDynamicGUIElement dynElem))
				continue;

			UUID uuid = dynElem.getUUID();
			if (uuid == null)
				continue;

			manager.instance.getStorageManager()
					.getOfflineUserData(uuid, manager.instance.getConfigManager().lockData()).thenAccept(opt -> {
						if (opt.isEmpty())
							return;

						UserData user = opt.get();
						VisitData visitData = user.getHellblockData().getVisitData();

						long remainingMillis = visitData.getFeaturedUntil() - System.currentTimeMillis();
						if (remainingMillis <= 0)
							return;

						long remainingSeconds = remainingMillis / 1000L;
						String countdown = manager.instance.getFormattedCooldown(remainingSeconds);

						context.arg(ContextKeys.FEATURED_TIME, remainingSeconds)
								.arg(ContextKeys.FEATURED_TIME_FORMATTED, countdown);

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
					});
		}
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The VisitGUI instance.
	 */
	public VisitGUI refresh() {
		if (showBackIcon) {
			VisitDynamicGUIElement backElement = (VisitDynamicGUIElement) getElement(manager.backSlot);
			if (backElement != null && !backElement.getSlots().isEmpty()) {
				backElement.setItemStack(manager.backIcon.build(context));
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
		return this;
	}

	public void refreshAndRepopulate() {
		manager.instance.getVisitManager().getTopIslands(currentSorter.getVisitFunction(), manager.getFilledSlotCount(),
				entries -> manager.instance.getScheduler().executeSync(() -> {
					populateVisitEntries(entries, currentSorter);
					refresh();
				}));
	}
}