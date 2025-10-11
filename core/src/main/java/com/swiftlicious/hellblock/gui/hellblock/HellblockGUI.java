package com.swiftlicious.hellblock.gui.hellblock;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
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
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	public HellblockGUI(HellblockGUIManager manager, Context<Player> context, HellblockData hellblockData,
			boolean isOwner) {
		this.manager = manager;
		this.context = context;
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
		HellblockDynamicGUIElement teleportElement = (HellblockDynamicGUIElement) getElement(manager.teleportSlot);
		if (teleportElement != null && !teleportElement.getSlots().isEmpty()) {
			teleportElement.setItemStack(manager.teleportIcon.build(context));
		}
		HellblockDynamicGUIElement challengeElement = (HellblockDynamicGUIElement) getElement(manager.challengeSlot);
		if (challengeElement != null && !challengeElement.getSlots().isEmpty()) {
			challengeElement.setItemStack(manager.challengeIcon.build(context));
		}
		HellblockDynamicGUIElement partyElement = (HellblockDynamicGUIElement) getElement(manager.partySlot);
		if (partyElement != null && !partyElement.getSlots().isEmpty()) {
			partyElement.setItemStack(manager.partyIcon.build(context));
		}
		HellblockDynamicGUIElement visitElement = (HellblockDynamicGUIElement) getElement(manager.visitSlot);
		if (visitElement != null && !visitElement.getSlots().isEmpty()) {
			visitElement.setItemStack(manager.visitIcon.build(context));
		}
		manager.instance.getStorageManager()
				.getOfflineUserData(hellblockData.getOwnerUUID(), manager.instance.getConfigManager().lockData())
				.thenAccept(result -> {
					if (result.isEmpty()) {
						// Close inventory on the main thread
						manager.instance.getScheduler().executeSync(() -> context.holder().closeInventory());
						return;
					}
					UserData ownerData = result.get();

					manager.instance.getIslandLevelManager().getLevelRank(ownerData.getUUID()).thenAccept(rank -> {
						context.arg(ContextKeys.HELLBLOCK_LEVEL, ownerData.getHellblockData().getLevel())
								.arg(ContextKeys.HELLBLOCK_RANK,
										rank.intValue() > 0 ? String.valueOf(rank)
												: manager.instance.getTranslationManager().miniMessageTranslation(
														MessageConstants.FORMAT_UNRANKED.build().key()))
								.arg(ContextKeys.HELLBLOCK_NAME,
										ownerData.getHellblockData().getDisplaySettings().getIslandName())
								.arg(ContextKeys.HELLBLOCK_BIO,
										ownerData.getHellblockData().getDisplaySettings().getIslandBio())
								.arg(ContextKeys.HELLBLOCK_DISPLAY_CHOICE,
										ownerData.getHellblockData().getDisplaySettings().getDisplayChoice())
								.arg(ContextKeys.HELLBLOCK_HOPPER_TIER,
										ownerData.getHellblockData().getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
								.arg(ContextKeys.HELLBLOCK_RANGE_TIER,
										ownerData.getHellblockData()
												.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
								.arg(ContextKeys.HELLBLOCK_GENERATOR_TIER,
										ownerData.getHellblockData()
												.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
								.arg(ContextKeys.HELLBLOCK_PARTY_TIER,
										ownerData.getHellblockData().getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
								.arg(ContextKeys.HELLBLOCK_BARTERING_TIER,
										ownerData.getHellblockData()
												.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
								.arg(ContextKeys.HELLBLOCK_CROP_TIER,
										ownerData.getHellblockData().getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
								.arg(ContextKeys.HELLBLOCK_MOB_TIER,
										ownerData.getHellblockData().getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE))
								.arg(ContextKeys.FEATURED_TIME,
										ownerData.getHellblockData().getVisitData().getFeaturedUntil())
								.arg(ContextKeys.FEATURED_TIME_FORMATTED,
										manager.instance.getFormattedCooldown(
												ownerData.getHellblockData().getVisitData().getFeaturedUntil()))
								.arg(ContextKeys.RESET_COOLDOWN, ownerData.getHellblockData().getResetCooldown())
								.arg(ContextKeys.BIOME_COOLDOWN, ownerData.getHellblockData().getBiomeCooldown())
								.arg(ContextKeys.TRANSFER_COOLDOWN, ownerData.getHellblockData().getTransferCooldown())
								.arg(ContextKeys.RESET_COOLDOWN_FORMATTED,
										manager.instance
												.getFormattedCooldown(ownerData.getHellblockData().getResetCooldown()))
								.arg(ContextKeys.BIOME_COOLDOWN_FORMATTED,
										manager.instance
												.getFormattedCooldown(ownerData.getHellblockData().getBiomeCooldown()))
								.arg(ContextKeys.TRANSFER_COOLDOWN_FORMATTED,
										manager.instance.getFormattedCooldown(
												ownerData.getHellblockData().getTransferCooldown()))
								.arg(ContextKeys.HELLBLOCK_BIOME, ownerData.getHellblockData().getBiome())
								.arg(ContextKeys.HELLBLOCK_ID, ownerData.getHellblockData().getID())
								.arg(ContextKeys.HELLBLOCK_PARTY_COUNT, ownerData.getHellblockData().getParty().size())
								.arg(ContextKeys.CREATION_TIME, ownerData.getHellblockData().getCreation())
								.arg(ContextKeys.CREATION_TIME_FORMATTED,
										ownerData.getHellblockData().getCreationTime())
								.arg(ContextKeys.HELLBLOCK_CHOICE, ownerData.getHellblockData().getIslandChoice())
								.arg(ContextKeys.HELLBLOCK_HOME_LOCATION,
										ownerData.getHellblockData().getHomeLocation())
								.arg(ContextKeys.HELLBLOCK_LOCATION,
										ownerData.getHellblockData().getHellblockLocation())
								.arg(ContextKeys.HELLBLOCK_OVERALL_VISITORS,
										ownerData.getHellblockData().getVisitData().getTotalVisits())
								.arg(ContextKeys.HELLBLOCK_DAILY_VISITORS,
										ownerData.getHellblockData().getVisitData().getDailyVisits())
								.arg(ContextKeys.HELLBLOCK_WEEKLY_VISITORS,
										ownerData.getHellblockData().getVisitData().getWeeklyVisits())
								.arg(ContextKeys.HELLBLOCK_MONTHLY_VISITORS,
										ownerData.getHellblockData().getVisitData().getMonthlyVisits());
						HellblockDynamicGUIElement levelElement = (HellblockDynamicGUIElement) getElement(
								manager.levelSlot);
						if (levelElement != null && !levelElement.getSlots().isEmpty()) {
							levelElement.setItemStack(manager.levelIcon.build(context));
						}
						if (isOwner) {
							HellblockDynamicGUIElement flagElement = (HellblockDynamicGUIElement) getElement(
									manager.flagSlot);
							if (flagElement != null && !flagElement.getSlots().isEmpty()) {
								flagElement.setItemStack(manager.flagIcon.build(context));
							}
							HellblockDynamicGUIElement upgradeElement = (HellblockDynamicGUIElement) getElement(
									manager.upgradeSlot);
							if (upgradeElement != null && !upgradeElement.getSlots().isEmpty()) {
								upgradeElement.setItemStack(manager.upgradeIcon.build(context));
							}
							HellblockDynamicGUIElement displayElement = (HellblockDynamicGUIElement) getElement(
									manager.displaySlot);
							if (displayElement != null && !displayElement.getSlots().isEmpty()) {
								displayElement.setItemStack(manager.displayIcon.build(context));
							}
							if (ownerData.getHellblockData().getBiomeCooldown() <= 0) {
								HellblockDynamicGUIElement biomeElement = (HellblockDynamicGUIElement) getElement(
										manager.biomeSlot);
								if (biomeElement != null && !biomeElement.getSlots().isEmpty()) {
									biomeElement.setItemStack(manager.biomeIcon.build(context));
								}
							} else {
								HellblockDynamicGUIElement biomeElement = (HellblockDynamicGUIElement) getElement(
										manager.biomeCooldownSlot);
								if (biomeElement != null && !biomeElement.getSlots().isEmpty()) {
									biomeElement.setItemStack(manager.biomeCooldownIcon.build(context));
								}
							}
							if (ownerData.getHellblockData().isLocked()) {
								HellblockDynamicGUIElement lockElement = (HellblockDynamicGUIElement) getElement(
										manager.unlockSlot);
								if (lockElement != null && !lockElement.getSlots().isEmpty()) {
									lockElement.setItemStack(manager.unlockIcon.build(context));
								}
							} else {
								HellblockDynamicGUIElement lockElement = (HellblockDynamicGUIElement) getElement(
										manager.lockSlot);
								if (lockElement != null && !lockElement.getSlots().isEmpty()) {
									lockElement.setItemStack(manager.lockIcon.build(context));
								}
							}
							if (ownerData.getHellblockData().getResetCooldown() <= 0) {
								HellblockDynamicGUIElement resetElement = (HellblockDynamicGUIElement) getElement(
										manager.resetSlot);
								if (resetElement != null && !resetElement.getSlots().isEmpty()) {
									resetElement.setItemStack(manager.resetIcon.build(context));
								}
							} else {
								HellblockDynamicGUIElement resetElement = (HellblockDynamicGUIElement) getElement(
										manager.resetCooldownSlot);
								if (resetElement != null && !resetElement.getSlots().isEmpty()) {
									resetElement.setItemStack(manager.resetCooldownIcon.build(context));
								}
							}
						}
					});
				});
		itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof HellblockDynamicGUIElement)
				.forEach(entry -> {
					HellblockDynamicGUIElement dynamicGUIElement = (HellblockDynamicGUIElement) entry.getValue();
					this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
				});
		return this;
	}
}