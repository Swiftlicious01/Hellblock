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
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ChallengesGUI {

	private final Map<Character, ChallengesGUIElement> itemsCharMap;
	private final Map<Integer, ChallengesGUIElement> itemsSlotMap;
	private final ChallengesGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final ChallengeData challengeData;

	public ChallengesGUI(ChallengesGUIManager manager, Context<Player> context, HellblockData hellblockData,
			ChallengeData challengeData) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.challengeData = challengeData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new ChallengesGUIHolder();
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
				ChallengesGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9));
					itemsSlotMap.put(index + line * (this.inventory.getType() == InventoryType.HOPPER ? 5 : 9),
							element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, ChallengesGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
	}

	public ChallengesGUI addElement(ChallengesGUIElement... elements) {
		for (ChallengesGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public ChallengesGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context, true))));
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
		ChallengesDynamicGUIElement backElement = (ChallengesDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}

		for (Tuple<Character, Section, Tuple<CustomItem, ChallengeType, Action<Player>[]>> challenge : manager.challengeIcons) {
			ChallengesDynamicGUIElement challengeElement = (ChallengesDynamicGUIElement) getElement(challenge.left());
			if (challengeElement != null && !challengeElement.getSlots().isEmpty()) {
				Item<ItemStack> item = manager.instance.getItemManager().wrap(challenge.right().left().build(context));
				ChallengeType challengeType = challenge.right().mid();
				if (challengeData.isChallengeCompleted(challengeType)) {
					String newName = AdventureHelper
							.miniMessageToJson(challenge.mid().getString("display.completed-name")
									.replace("{current_amount}", String.valueOf(challengeType.getNeededAmount()))
									.replace("{needed_amount}", String.valueOf(challengeType.getNeededAmount())));
					item.displayName(newName);
					List<String> newLore = new ArrayList<>();
					if (challengeData.isChallengeRewardClaimed(challengeType)) {
						for (String lore : challenge.mid().getStringList("display.claimed-lore")) {
							newLore.add(AdventureHelper.miniMessageToJson(lore
									.replace("{current_amount}", String.valueOf(challengeType.getNeededAmount()))
									.replace("{needed_amount}", String.valueOf(challengeType.getNeededAmount()))));
						}
					} else {
						for (String lore : challenge.mid().getStringList("display.unclaimed-lore")) {
							newLore.add(AdventureHelper.miniMessageToJson(lore
									.replace("{current_amount}", String.valueOf(challengeType.getNeededAmount()))
									.replace("{needed_amount}", String.valueOf(challengeType.getNeededAmount()))));
						}
					}
					item.lore(newLore);
					if (manager.highlightCompletion)
						item.glint(true);
				} else {
					String newName = AdventureHelper
							.miniMessageToJson(challenge.mid().getString("display.uncompleted-name")
									.replace("{current_amount}",
											String.valueOf(challengeData.getChallengeProgress(challengeType)))
									.replace("{needed_amount}", String.valueOf(challengeType.getNeededAmount())));
					item.displayName(newName);
					List<String> newLore = new ArrayList<>();
					for (String lore : challenge.mid().getStringList("display.uncompleted-lore")) {
						newLore.add(
								AdventureHelper
										.miniMessageToJson(lore
												.replace("{current_amount}",
														String.valueOf(
																challengeData.getChallengeProgress(challengeType)))
												.replace("{needed_amount}",
														String.valueOf(challengeType.getNeededAmount()))
												.replace("{progress_bar}",
														ProgressBar.getProgressBar(
																new ProgressBar(challengeType.getNeededAmount(),
																		challengeData
																				.getChallengeProgress(challengeType)),
																25))));
					}
					item.lore(newLore);
				}
				challengeElement.setItemStack(item.load());
			}
		}
		for (Map.Entry<Integer, ChallengesGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof ChallengesDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}