package com.swiftlicious.hellblock.gui.hellblock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;

public class HellblockGUI {

	private final Map<Character, HellblockGUIElement> itemsCharMap;
	private final Map<Integer, HellblockGUIElement> itemsSlotMap;
	private final HellblockGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public HellblockGUI(HellblockGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new HellblockGUIHolder();
		if (isOwner) {
			if (manager.ownerLayout.length == 1 && manager.ownerLayout[0].length() == 4) {
				this.inventory = Bukkit.createInventory(holder, InventoryType.HOPPER);
			} else {
				this.inventory = Bukkit.createInventory(holder, manager.ownerLayout.length * 9);
			}
		} else {
			if (manager.memberLayout.length == 1 && manager.memberLayout[0].length() == 4) {
				this.inventory = Bukkit.createInventory(holder, InventoryType.HOPPER);
			} else {
				this.inventory = Bukkit.createInventory(holder, manager.memberLayout.length * 9);
			}
		}
		holder.setInventory(this.inventory);
	}

	private void init() {
		int line = 0;
		for (String content : isOwner ? manager.ownerLayout : manager.memberLayout) {
			for (int index = 0; index < (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9); index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				HellblockGUIElement element = itemsCharMap.get(symbol);
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

	public HellblockGUI addElement(HellblockGUIElement... elements) {
		for (HellblockGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public HellblockGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public HellblockGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public HellblockGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The HellblockGUI instance.
	 */
	public HellblockGUI refresh() {
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}

		refreshInProgress = true;
		refreshQueued = false;

		// Static elements (always shown)
		setIfAvailable(manager.teleportSlot, manager.teleportIcon);
		setIfAvailable(manager.challengeSlot, manager.challengeIcon);
		setIfAvailable(manager.partySlot, manager.partyIcon);
		setIfAvailable(manager.visitSlot, manager.visitIcon);
		setIfAvailable(manager.eventSlot, manager.eventIcon);
		setIfAvailable(manager.notificationSlot, manager.notificationIcon);

		UUID ownerUUID = hellblockData.getOwnerUUID();
		if (ownerUUID == null) {
			return this;
		}

		manager.instance.getStorageManager()
				.getCachedUserDataWithFallback(ownerUUID, manager.instance.getConfigManager().lockData())
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						manager.instance.getScheduler().executeSync(() -> context.holder().closeInventory());
						return;
					}

					UserData ownerData = optional.get();
					HellblockData owner = ownerData.getHellblockData();

					manager.instance.getIslandLevelManager().getLevelRank(owner.getIslandId()).thenAccept(rank -> {
						// Inject dynamic context
						try {
							islandContext.arg(ContextKeys.ISLAND_LEVEL, owner.getIslandLevel())
									.arg(ContextKeys.ISLAND_RANK,
											rank.intValue() > 0 ? String.valueOf(rank)
													: manager.instance.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_UNRANKED.build().key()))
									.arg(ContextKeys.ISLAND_NAME, owner.getDisplaySettings().getIslandName())
									.arg(ContextKeys.ISLAND_BIO, owner.getDisplaySettings().getIslandBio())
									.arg(ContextKeys.ISLAND_DISPLAY_CHOICE,
											owner.getDisplaySettings().getDisplayChoice())
									.arg(ContextKeys.ISLAND_HOPPER_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
									.arg(ContextKeys.ISLAND_RANGE_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
									.arg(ContextKeys.ISLAND_GENERATOR_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
									.arg(ContextKeys.ISLAND_PARTY_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
									.arg(ContextKeys.ISLAND_BARTERING_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
									.arg(ContextKeys.ISLAND_CROP_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
									.arg(ContextKeys.ISLAND_MOB_TIER,
											owner.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE))
									.arg(ContextKeys.FEATURED_TIME, owner.getVisitData().getFeaturedUntil())
									.arg(ContextKeys.FEATURED_TIME_FORMATTED,
											manager.instance.getCooldownManager()
													.getFormattedCooldown(owner.getVisitData().getFeaturedUntil()))
									.arg(ContextKeys.RESET_COOLDOWN, owner.getResetCooldown())
									.arg(ContextKeys.BIOME_COOLDOWN, owner.getBiomeCooldown())
									.arg(ContextKeys.TRANSFER_COOLDOWN, owner.getTransferCooldown())
									.arg(ContextKeys.RESET_COOLDOWN_FORMATTED,
											manager.instance.getCooldownManager()
													.getFormattedCooldown(owner.getResetCooldown()))
									.arg(ContextKeys.BIOME_COOLDOWN_FORMATTED,
											manager.instance.getCooldownManager()
													.getFormattedCooldown(owner.getBiomeCooldown()))
									.arg(ContextKeys.TRANSFER_COOLDOWN_FORMATTED,
											manager.instance.getCooldownManager()
													.getFormattedCooldown(owner.getTransferCooldown()))
									.arg(ContextKeys.ISLAND_BIOME, owner.getBiome())
									.arg(ContextKeys.ISLAND_PARTY_COUNT, owner.getPartyMembers().size())
									.arg(ContextKeys.CREATION_TIME, owner.getCreationTime())
									.arg(ContextKeys.CREATION_TIME_FORMATTED, owner.getCreationTimeFormatted())
									.arg(ContextKeys.ISLAND_CHOICE, owner.getIslandChoice())
									.arg(ContextKeys.ISLAND_HOME_LOCATION, owner.getHomeLocation())
									.arg(ContextKeys.ISLAND_LOCATION, owner.getHellblockLocation())
									.arg(ContextKeys.ISLAND_OVERALL_VISITORS, owner.getVisitData().getTotalVisits())
									.arg(ContextKeys.ISLAND_DAILY_VISITORS, owner.getVisitData().getDailyVisits())
									.arg(ContextKeys.ISLAND_WEEKLY_VISITORS, owner.getVisitData().getWeeklyVisits())
									.arg(ContextKeys.ISLAND_MONTHLY_VISITORS, owner.getVisitData().getMonthlyVisits());

							// Dynamic elements
							setIfAvailable(manager.levelSlot, manager.levelIcon);

							if (isOwner) {
								setIfAvailable(manager.flagSlot, manager.flagIcon);
								setIfAvailable(manager.upgradeSlot, manager.upgradeIcon);
								setIfAvailable(manager.displaySlot, manager.displayIcon);

								if (owner.getBiomeCooldown() <= 0) {
									setIfAvailable(manager.biomeSlot, manager.biomeIcon);
								} else {
									setIfAvailable(manager.biomeCooldownSlot, manager.biomeCooldownIcon);
								}

								if (owner.isLocked()) {
									setIfAvailable(manager.unlockSlot, manager.unlockIcon);
								} else {
									setIfAvailable(manager.lockSlot, manager.lockIcon);
								}

								if (owner.getResetCooldown() <= 0) {
									setIfAvailable(manager.resetSlot, manager.resetIcon);
								} else {
									setIfAvailable(manager.resetCooldownSlot, manager.resetCooldownIcon);
								}
							}

							// Apply updated items to GUI
							itemsSlotMap.entrySet().forEach(entry -> {
								if (entry.getValue() instanceof HellblockDynamicGUIElement dynamicElement) {
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
					}).exceptionally(ex -> {
						manager.instance.getPluginLogger().severe("Failed to refresh HellblockGUI for "
								+ context.holder().getName() + ": " + ex.getMessage());
						refreshInProgress = false;
						return null;
					});
				}).exceptionally(ex -> {
					manager.instance.getPluginLogger().severe("Failed to refresh HellblockGUI for "
							+ context.holder().getName() + ": " + ex.getMessage());
					refreshInProgress = false;
					return null;
				});

		return this;
	}

	private void setIfAvailable(char slotSymbol, CustomItem item) {
		Context<Player> combinedCtx = context.merge(islandContext);
		HellblockDynamicGUIElement element = (HellblockDynamicGUIElement) getElement(slotSymbol);
		if (element != null && !element.getSlots().isEmpty()) {
			element.setItemStack(item.build(combinedCtx));
		}
	}
}