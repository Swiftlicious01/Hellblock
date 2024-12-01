package com.swiftlicious.hellblock.gui.invite;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

public class InviteGUI {

	private final Map<Character, InviteGUIElement> itemsCharMap;
	private final Map<Integer, InviteGUIElement> itemsSlotMap;
	private final Map<Integer, InviteGUIElement> inviteSlotsMap;
	private final Map<Integer, InviteGUIElement> cachedHeads;
	private final InviteGUIManager manager;
	protected final AnvilInventory inventory;
	protected final Map<Integer, ItemStack> savedItems;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected String searchedName;
	protected SchedulerTask searchTask;

	public InviteGUI(InviteGUIManager manager, Context<Player> context, HellblockData hellblockData) {
		this.manager = manager;
		this.context = context;
		this.searchedName = manager.instance.getItemManager().wrap(manager.searchIcon.build(context)).displayName()
				.orElse("<Type Name Here>");
		this.searchTask = manager.instance.getScheduler().sync().runRepeating(() -> refreshSearch(), 1 * 20, 1 * 20,
				context.holder().getLocation());
		this.hellblockData = hellblockData;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		this.inviteSlotsMap = new HashMap<>();
		this.cachedHeads = new HashMap<>();
		this.savedItems = new HashMap<>();
		var holder = new InviteGUIHolder();
		this.inventory = (AnvilInventory) Bukkit.createInventory(holder, InventoryType.ANVIL);
		holder.setInventory(this.inventory);
	}

	private void init() {
		int line = 0;
		if (manager.layout.length != 4) {
			manager.instance.getPluginLogger()
					.warn("Invitation GUI layout must be 4 rows, please fix this before continuing.");
			return;
		}
		for (String content : manager.layout) {
			for (int index = 0; index < 9; index++) {
				char symbol;
				if (index < content.length())
					symbol = content.charAt(index);
				else
					symbol = ' ';
				InviteGUIElement element = itemsCharMap.get(symbol);
				if (element != null) {
					element.addSlot(index + line * 9);
					itemsSlotMap.put(index + line * 9, element);
				}
			}
			line++;
		}
		for (Map.Entry<Integer, InviteGUIElement> entry : itemsSlotMap.entrySet()) {
			context.holder().getInventory().setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
		inventory.setItem(0, manager.searchIcon.build(context));
		inventory.setItem(2, manager.playerNotFoundIcon.build(context));
		inviteSlotsMap.put(0, new InviteDynamicGUIElement('S', inventory.getItem(0)));
		inviteSlotsMap.put(2, new InviteDynamicGUIElement('R', inventory.getItem(2)));
	}

	public InviteGUI addElement(InviteGUIElement... elements) {
		for (InviteGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public InviteGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		manager.instance.getVersionManager().getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context))));
	}

	@Nullable
	public InviteGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public InviteGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The InviteGUI instance.
	 */
	public InviteGUI refresh() {
		InviteDynamicGUIElement backElement = (InviteDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		InviteDynamicGUIElement leftElement = (InviteDynamicGUIElement) getElement(manager.leftSlot);
		if (leftElement != null && !leftElement.getSlots().isEmpty()) {
			leftElement.setItemStack(manager.leftIcon.build(context));
		}
		InviteDynamicGUIElement rightElement = (InviteDynamicGUIElement) getElement(manager.rightSlot);
		if (rightElement != null && !rightElement.getSlots().isEmpty()) {
			rightElement.setItemStack(manager.rightIcon.build(context));
		}
		for (Map.Entry<Integer, InviteGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof InviteDynamicGUIElement dynamicGUIElement) {
				context.holder().getInventory().setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}

	public InviteGUI refreshSearch() {
		InviteDynamicGUIElement searchElement = (InviteDynamicGUIElement) inviteSlotsMap.get(0);
		if (searchElement != null && !searchElement.getSlots().isEmpty()) {
			Item<ItemStack> search = manager.instance.getItemManager().wrap(manager.searchIcon.build(context));
			String username = inventory.getRenameText();
			if (username != null)
				search.displayName(AdventureHelper.miniMessageToJson(username));
			searchElement.setItemStack(search.load());
		}
		InviteDynamicGUIElement headElement = (InviteDynamicGUIElement) inviteSlotsMap.get(2);
		if (headElement != null && !headElement.getSlots().isEmpty()) {
			boolean playerFound = manager.instance.getStorageManager().getOnlineUsers().stream()
					.filter(p -> p.getName().equalsIgnoreCase(inventory.getRenameText())).findAny().isPresent();
			if (playerFound) {
				Item<ItemStack> head = manager.instance.getItemManager().wrap(manager.playerFoundIcon.build(context));
				String username = inventory.getRenameText();
				try {
					if (username != null) {
						String name = AdventureHelper
								.miniMessageToJson(manager.playerFoundName.replace("{player}", username));
						head.displayName(name);
						UUID id = UUIDFetcher.getUUID(username);
						if (id != null) {
							headElement.setUUID(id);
							GameProfile profile = new GameProfile(id, username);
							head.skull(profile.getProperties().get("textures").iterator().next().getValue());
							headElement.setItemStack(head.load());
						}
					}
				} catch (IllegalArgumentException ex) {
					// ignored
				}
			} else {
				headElement.setItemStack(manager.playerNotFoundIcon.build(context));
				headElement.setUUID(null);
			}
		}
		for (Entry<Integer, InviteGUIElement> entry : getOnlinePlayerHeads().entrySet()) {
			if (entry.getValue() instanceof InviteDynamicGUIElement dynamicGUIElement) {
				context.holder().getInventory().setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}

	public void saveItems() {
		for (int slot = 0; slot < context.holder().getInventory().getSize(); slot++) {
			ItemStack itemStack = context.holder().getInventory().getItem(slot);
			if (itemStack != null && itemStack.getType() != Material.AIR) {
				savedItems.put(slot, itemStack);
			}
		}
	}

	public void returnItems() {
		for (Map.Entry<Integer, ItemStack> items : savedItems.entrySet()) {
			ItemStack itemStack = items.getValue();
			if (itemStack != null && itemStack.getType() != Material.AIR) {
				context.holder().getInventory().setItem(items.getKey(), itemStack);
			}
		}
		context.holder().updateInventory();
		savedItems.clear();
	}

	public Map<Integer, InviteGUIElement> getOnlinePlayerHeads() {
		Map<Integer, InviteGUIElement> heads = new HashMap<>();
		for (UserData userData : manager.instance.getStorageManager().getOnlineUsers()) {
			UUID id = userData.getUUID();
			if (id.equals(context.holder().getUniqueId()))
				continue;
			if (!userData.isOnline() || userData.getHellblockData().hasHellblock())
				continue;
			if (userData.getName().equals(searchedName))
				continue;
			if (userData.getHellblockData().getInvitations() == null
					|| userData.getHellblockData().hasInvite(context.holder().getUniqueId()))
				continue;

			Item<ItemStack> head = manager.instance.getItemManager().wrap(manager.playerIcon.build(context));
			GameProfile profile = new GameProfile(id, userData.getName());
			String username = AdventureHelper
					.miniMessageToJson(manager.playerName.replace("{player}", userData.getName()));
			head.displayName(username);
			head.skull(profile.getProperties().get("textures").iterator().next().getValue());
			for (int i = 9; i <= 35; i++) {
				InviteDynamicGUIElement element = new InviteDynamicGUIElement(manager.playerSlot, head.load());
				element.setUUID(id);
				cachedHeads.put(i, element);
				heads.putIfAbsent(i, element);
			}
		}
		return heads;
	}

	public void refreshPlayerHeads(boolean previous) {
		for (int i = 9; i <= 35; i++) {
			if (previous) {
				context.holder().getInventory().setItem(i, cachedHeads.get(i).getItemStack());
				cachedHeads.remove(i);
			}
			context.holder().getInventory().setItem(i, new ItemStack(Material.AIR));
			for (UserData userData : manager.instance.getStorageManager().getOnlineUsers()) {
				UUID id = userData.getUUID();
				if (id.equals(context.holder().getUniqueId()))
					continue;
				if (!userData.isOnline() || userData.getHellblockData().hasHellblock())
					continue;
				if (userData.getName().equals(searchedName))
					continue;
				if (userData.getHellblockData().getInvitations() == null
						|| userData.getHellblockData().hasInvite(context.holder().getUniqueId()))
					continue;
				if (cachedHeads.get(i).getUUID().equals(id))
					continue;

				Item<ItemStack> head = manager.instance.getItemManager().wrap(manager.playerIcon.build(context));
				GameProfile profile = new GameProfile(id, userData.getName());
				String username = AdventureHelper
						.miniMessageToJson(manager.playerName.replace("{player}", userData.getName()));
				head.displayName(username);
				head.skull(profile.getProperties().get("textures").iterator().next().getValue());
				InviteDynamicGUIElement element = new InviteDynamicGUIElement(manager.playerSlot, head.load());
				element.setUUID(id);
				context.holder().getInventory().setItem(i, element.getItemStack());
			}
		}
	}
}