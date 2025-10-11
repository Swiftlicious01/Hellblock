package com.swiftlicious.hellblock.gui.party;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class PartyGUI {

	private final Map<Character, PartyGUIElement> itemsCharMap;
	private final Map<Integer, PartyGUIElement> itemsSlotMap;
	private final PartyGUIManager manager;
	protected final Inventory inventory;
	protected final Context<Player> context;
	protected final HellblockData hellblockData;
	protected final boolean isOwner;

	public PartyGUI(PartyGUIManager manager, Context<Player> context, HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.hellblockData = hellblockData;
		this.isOwner = isOwner;
		this.itemsCharMap = new HashMap<>();
		this.itemsSlotMap = new HashMap<>();
		var holder = new PartyGUIHolder();
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
				PartyGUIElement element = itemsCharMap.get(symbol);
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

	public PartyGUI addElement(PartyGUIElement... elements) {
		for (PartyGUIElement element : elements) {
			itemsCharMap.put(element.getSymbol(), element);
		}
		return this;
	}

	public PartyGUI build() {
		init();
		return this;
	}

	public void show() {
		context.holder().openInventory(inventory);
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(), AdventureHelper
				.componentToJson(AdventureHelper.parseCenteredTitleMultiline(manager.title.render(context, true))));
	}

	@Nullable
	public PartyGUIElement getElement(int slot) {
		return itemsSlotMap.get(slot);
	}

	@Nullable
	public PartyGUIElement getElement(char slot) {
		return itemsCharMap.get(slot);
	}

	/**
	 * Refresh the GUI, updating the display based on current data.
	 * 
	 * @return The PartyGUI instance.
	 */
	/**
	 * Refresh the GUI, updating the display based on current data.
	 *
	 * @return The PartyGUI instance.
	 */
	public PartyGUI refresh() {
		// --- Back button ---
		PartyDynamicGUIElement backElement = (PartyDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
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

					context.arg(ContextKeys.HELLBLOCK_PARTY_COUNT, ownerData.getHellblockData().getParty().size());

					// --- Owner icon ---
					PartyDynamicGUIElement ownerElement = (PartyDynamicGUIElement) getElement(manager.ownerSlot);
					if (ownerElement != null && !ownerElement.getSlots().isEmpty()) {
						try {
							UUID ownerUUID = ownerData.getUUID();
							Item<ItemStack> item = manager.instance.getItemManager()
									.wrap(manager.ownerIcon.build(context));

							String username = Bukkit.getPlayer(ownerUUID) != null
									? Bukkit.getPlayer(ownerUUID).getName()
									: Bukkit.getOfflinePlayer(ownerUUID).hasPlayedBefore()
											&& Bukkit.getOfflinePlayer(ownerUUID).getName() != null
													? Bukkit.getOfflinePlayer(ownerUUID).getName()
													: null;

							boolean isOnline = Bukkit.getPlayer(ownerUUID) != null
									&& Bukkit.getPlayer(ownerUUID).isOnline();

							if (username != null) {
								item.displayName(AdventureHelper.miniMessageToJson(
										manager.ownerName.replace("{player}", username).replace("{login_status}",
												isOnline ? manager.onlineStatus : manager.offlineStatus)));

								List<String> newLore = new ArrayList<>();
								manager.ownerLore.forEach(lore -> newLore.add(AdventureHelper
										.miniMessageToJson(lore.replace("{player}", username).replace("{login_status}",
												isOnline ? manager.onlineStatus : manager.offlineStatus))));
								item.lore(newLore);
							}

							GameProfile profile = GameProfileBuilder.fetch(ownerUUID);
							item.skull(profile.getProperties().get("textures").iterator().next().getValue());

							ownerElement.setUUID(ownerUUID);
							ownerElement.setItemStack(item.load());
						} catch (IllegalArgumentException | IOException ex) {
							// ignored
						}
					}

					// --- Party members ---
					Set<UUID> party = ownerData.getHellblockData().getParty();
					if (party.isEmpty())
						party = Collections.emptySet();

					// Render existing members
					party.forEach(id -> manager.memberIcons
							.forEach(entry -> renderSlot(entry.left(), entry.right().left(), entry.mid(), id, false)));

					// Fill newMemberIcons: interactive up to maxSize, filler beyond maxSize
					int maxSize = manager.instance.getCoopManager().getMaxPartySize(ownerData);

					manager.newMemberIcons.entrySet().forEach(entry -> {
						Character symbol = entry.getKey();
						int index = manager.getMemberSlotIndex(symbol);
						boolean beyondLimit = index >= maxSize;

						renderSlot(symbol, entry.getValue().left(), null, null, beyondLimit);
					});

					// --- Push all dynamic elements into inventory ---
					itemsSlotMap.entrySet().stream().filter(entry -> entry.getValue() instanceof PartyDynamicGUIElement)
							.forEach(entry -> {
								PartyDynamicGUIElement dynamicGUIElement = (PartyDynamicGUIElement) entry.getValue();
								this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
							});
				});
		return this;
	}

	/**
	 * Render a single member slot (with placeholder replacement and skulls).
	 */
	private void renderSlot(Character symbol, CustomItem baseItem, @Nullable Section config, @Nullable UUID memberId,
			boolean beyondLimit) {

		PartyDynamicGUIElement element = (PartyDynamicGUIElement) getElement(symbol);
		if (element == null || element.getSlots().isEmpty())
			return;

		Item<ItemStack> item = manager.instance.getItemManager().wrap(baseItem.build(context));

		// username (if member)
		String username = null;
		boolean isOnline = false;
		if (memberId != null) {
			Player online = Bukkit.getPlayer(memberId);
			if (online != null) {
				username = online.getName();
				if (online.isOnline())
					isOnline = true;
			} else {
				OfflinePlayer offline = Bukkit.getOfflinePlayer(memberId);
				if (offline.hasPlayedBefore() && offline.getName() != null)
					username = offline.getName();
			}
		}

		// determine config to use: prefer passed config, else ask manager for section
		// for this symbol
		Section cfg = config != null ? config : manager.getSectionForSlotChar(symbol);

		// Display name: prefer cfg.display.name (with {player}) if present; otherwise
		// keep the CustomItem built name.
		if (cfg != null) {
			String displayName = cfg.getString("display.name");
			if (displayName != null) {
				if (username != null)
					displayName = displayName.replace("{player}", username).replace("{login_status}",
							isOnline ? manager.onlineStatus : manager.offlineStatus);
				item.displayName(AdventureHelper.miniMessageToJson(displayName));
			}
		}

		// Lore
		if (beyondLimit) {
			// beyond maxSize => clear lore completely
			item.lore(Collections.emptyList());
		} else if (cfg != null) {
			List<String> loreList = isOwner ? cfg.getStringList("display.owner-lore")
					: cfg.getStringList("display.member-lore");
			if (loreList != null && !loreList.isEmpty()) {
				List<String> parsed = new ArrayList<>(loreList.size());
				for (String lore : loreList) {
					if (username != null)
						lore = lore.replace("{player}", username).replace("{login_status}",
								isOnline ? manager.onlineStatus : manager.offlineStatus);
					parsed.add(AdventureHelper.miniMessageToJson(lore));
				}
				item.lore(parsed);
			}
		}
		// else: keep the CustomItem's built lore (if any) when cfg == null and not
		// beyondLimit

		// Skull (if member)
		if (memberId != null) {
			try {
				GameProfile profile = GameProfileBuilder.fetch(memberId);
				item.skull(profile.getProperties().get("textures").iterator().next().getValue());
				element.setUUID(memberId);
			} catch (IllegalArgumentException | IOException ignored) {
			}
		} else {
			element.setUUID(null);
		}

		element.setItemStack(item.load());
	}
}