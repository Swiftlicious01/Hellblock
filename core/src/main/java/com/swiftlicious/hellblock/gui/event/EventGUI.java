package com.swiftlicious.hellblock.gui.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.gui.event.EventGUIManager.EventType;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.invasion.InvasionDifficulty;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class EventGUI {

	private final Map<Character, EventGUIElement> itemsCharMap;
	private final Map<Integer, EventGUIElement> itemsSlotMap;
	private final EventGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public EventGUI(EventGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new EventGUIHolder();
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
				EventGUIElement element = itemsCharMap.get(symbol);
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

	public EventGUI addElement(EventGUIElement... elements) {
		for (EventGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public EventGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public EventGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public EventGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The EventGUI instance.
	 */
	public EventGUI refresh() {
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}

		refreshInProgress = true;
		refreshQueued = false;

		UUID ownerUUID = hellblockData.getOwnerUUID();
		if (ownerUUID == null) {
			refreshInProgress = false;
			return this;
		}

		manager.instance.getStorageManager()
				.getCachedUserDataWithFallback(ownerUUID, manager.instance.getConfigManager().lockData())
				.thenAccept(optionalOwnerData -> {
					if (optionalOwnerData.isEmpty()) {
						refreshInProgress = false;
						return;
					}

					UserData ownerData = optionalOwnerData.get();
					HellblockData ownerHellblockData = ownerData.getHellblockData();

					manager.instance.getScheduler().executeSync(() -> {
						try {
							// === Back Button ===
							var backElement = (EventDynamicGUIElement) getElement(manager.backSlot);
							if (backElement != null && !backElement.getSlots().isEmpty()) {
								backElement.setItemStack(manager.backIcon.build(context));
							}

							// === Event Icons ===
							for (var event : manager.eventIcons) {
								char symbol = event.left();
								Section configSection = event.mid();
								var eventData = event.right();
								CustomItem customItem = eventData.left();
								EventType type = eventData.mid();

								var element = (EventDynamicGUIElement) getElement(symbol);
								if (element == null || element.getSlots().isEmpty())
									continue;

								if (!manager.isEventUnlocked(type, ownerHellblockData)) {
									float requiredLevel = manager.getRequiredLevelForEvent(type);
									islandContext.arg(ContextKeys.REQUIRED_LEVEL, requiredLevel);
									Context<Player> combinedCtx = context.merge(islandContext);
									element.setItemStack(manager.buildLockedLevelIcon(combinedCtx, type));
									continue;
								}

								var item = manager.instance.getItemManager().wrap(customItem.build(context));
								Map<String, String> placeholders = getEventStatsPlaceholders(type, ownerHellblockData);
								List<String> newLore = new ArrayList<>();

								configSection.getStringList("display.lore").forEach(rawLine -> {
									String line = rawLine;
									for (Map.Entry<String, String> entry : placeholders.entrySet()) {
										line = line.replace("{" + entry.getKey() + "}", entry.getValue());
									}
									newLore.add(AdventureHelper.miniMessageToJson(line));
								});

								item.lore(newLore);
								element.setItemStack(item.loadCopy());
							}

							// === Apply All GUI Elements ===
							itemsSlotMap.entrySet().forEach(entry -> {
								if (entry.getValue() instanceof EventDynamicGUIElement dynamicElement) {
									inventory.setItem(entry.getKey(), dynamicElement.getItemStack().clone());
								}
							});
						} finally {
							refreshInProgress = false;
							if (refreshQueued) {
								refreshQueued = false;
								manager.instance.getScheduler().sync().run(this::refresh,
										context.holder().getLocation());
							}
						}
					}, context.holder().getLocation());
				}).exceptionally(ex -> {
					manager.instance.getPluginLogger().severe(
							"Failed to refresh EventGUI for " + context.holder().getName() + ": " + ex.getMessage());
					refreshInProgress = false;
					return null;
				});

		return this;
	}

	private Map<String, String> getEventStatsPlaceholders(EventType eventKey, HellblockData data) {
		Map<String, String> values = new HashMap<>();
		switch (eventKey) {
		case EventType.WITHER -> {
			values.put("total_spawns", String.valueOf(data.getWitherData().getTotalSpawns()));
			values.put("kills", String.valueOf(data.getWitherData().getKills()));
			values.put("despawns", String.valueOf(data.getWitherData().getDespawns()));
			values.put("total_heals", String.valueOf(data.getWitherData().getTotalHeals()));
			values.put("total_minion_waves", String.valueOf(data.getWitherData().getTotalMinionWaves()));
			values.put("longest_battle", String.valueOf(data.getWitherData().getLongestFightMillis()));
			values.put("shorest_battle", String.valueOf(data.getWitherData().getShortestFightMillis()));
			values.put("last_wither_spawn", String.valueOf(data.getWitherData().getLastSpawnTime()));
			values.put("longest_battle_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getWitherData().getLongestFightMillis()));
			values.put("shorest_battle_formated", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getWitherData().getShortestFightMillis()));
			values.put("last_wither_spawn_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getWitherData().getLastSpawnTime()));
		}
		case EventType.INVASION -> {
			values.put("total_invasions", String.valueOf(data.getInvasionData().getTotalInvasions()));
			values.put("successful_invasions", String.valueOf(data.getInvasionData().getSuccessfulInvasions()));
			values.put("failed_invasions", String.valueOf(data.getInvasionData().getFailedInvasions()));
			values.put("boss_kills", String.valueOf(data.getInvasionData().getBossKills()));
			values.put("current_streak", String.valueOf(data.getInvasionData().getCurrentStreak()));
			values.put("current_tier", String.valueOf(data.getInvasionData().getHighestDifficultyTierReached()));
			values.put("max_tier", String.valueOf(InvasionDifficulty.getMaxTier()));
			values.put("last_invasion_time", String.valueOf(data.getInvasionData().getLastInvasionTime()));
			values.put("last_invasion_time_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getInvasionData().getLastInvasionTime()));
		}
		case EventType.SKYSIEGE -> {
			values.put("total_skysieges", String.valueOf(data.getSkysiegeData().getTotalSkysieges()));
			values.put("successful_skysieges", String.valueOf(data.getSkysiegeData().getSuccessfulSkysieges()));
			values.put("failed_skysieges", String.valueOf(data.getSkysiegeData().getFailedSkysieges()));
			values.put("queen_kills", String.valueOf(data.getSkysiegeData().getQueenKills()));
			values.put("total_ghasts_killed", String.valueOf(data.getSkysiegeData().getTotalGhastsKilled()));
			values.put("total_waves_completed", String.valueOf(data.getSkysiegeData().getTotalWavesCompleted()));
			values.put("longest_skysiege", String.valueOf(data.getSkysiegeData().getLongestDurationMillis()));
			values.put("longest_skysiege_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getSkysiegeData().getLongestDurationMillis()));
			values.put("shortest_skysiege", String.valueOf(data.getSkysiegeData().getShortestDurationMillis()));
			values.put("shortest_skysiege_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getSkysiegeData().getShortestDurationMillis()));
			values.put("last_skysiege", String.valueOf(data.getSkysiegeData().getLastSkysiegeTime()));
			values.put("last_skysiege_formatted", manager.instance.getCooldownManager()
					.getFormattedCooldown(data.getSkysiegeData().getLastSkysiegeTime()));
		}
		}
		return values;
	}

}