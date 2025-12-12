package com.swiftlicious.hellblock.gui.notification;

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

import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.UserData;

public class NotificationGUI {

	private final Map<Character, NotificationGUIElement> itemsCharMap;
	private final Map<Integer, NotificationGUIElement> itemsSlotMap;
	private final NotificationGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final Context<Integer> islandContext;
	protected final UserData userData;
	protected final boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public NotificationGUI(NotificationGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			UserData userData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
		this.userData = userData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new NotificationGUIHolder();
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
				NotificationGUIElement element = itemsCharMap.get(symbol);
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

	public NotificationGUI addElement(NotificationGUIElement... elements) {
		for (NotificationGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public NotificationGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public NotificationGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public NotificationGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The NotificationGUI instance.
	 */
	public NotificationGUI refresh() {
		if (refreshInProgress) {
			refreshQueued = true;
			return this;
		}

		refreshInProgress = true;
		refreshQueued = false;

		manager.instance.getScheduler().executeSync(() -> {
			try {
				// === Back Button ===
				NotificationDynamicGUIElement backElement = (NotificationDynamicGUIElement) getElement(
						manager.backSlot);
				if (backElement != null && !backElement.getSlots().isEmpty()) {
					backElement.setItemStack(manager.backIcon.build(context));
				}

				// === Invitation Notification ===
				NotificationDynamicGUIElement invitationElement = (NotificationDynamicGUIElement) getElement(
						manager.invitationSlot);
				if (invitationElement != null && !invitationElement.getSlots().isEmpty()) {
					Item<ItemStack> item = manager.instance.getItemManager()
							.wrap(manager.invitationIcon.build(context));
					String status = userData.getNotificationSettings().hasInviteNotifications()
							? manager.instance.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_ON.build().key())
							: manager.instance.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_OFF.build().key());

					List<String> lore = new ArrayList<>();
					manager.invitationSection.getStringList("display.lore").forEach(
							raw -> lore.add(AdventureHelper.miniMessageToJson(raw.replace("{status}", status))));
					item.lore(lore);
					invitationElement.setItemStack(item.loadCopy());
				}

				// === Join Notification ===
				NotificationDynamicGUIElement joinElement = (NotificationDynamicGUIElement) getElement(
						manager.joinSlot);
				if (joinElement != null && !joinElement.getSlots().isEmpty()) {
					Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.joinIcon.build(context));
					String status = userData.getNotificationSettings().hasJoinNotifications()
							? manager.instance.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_ON.build().key())
							: manager.instance.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_OFF.build().key());

					List<String> lore = new ArrayList<>();
					manager.joinSection.getStringList("display.lore").forEach(
							raw -> lore.add(AdventureHelper.miniMessageToJson(raw.replace("{status}", status))));
					item.lore(lore);
					joinElement.setItemStack(item.loadCopy());
				}

				// === Apply Updates to Inventory ===
				itemsSlotMap.entrySet().stream()
						.filter(entry -> entry.getValue() instanceof NotificationDynamicGUIElement).forEach(entry -> {
							NotificationDynamicGUIElement dynamicElement = (NotificationDynamicGUIElement) entry
									.getValue();
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
}