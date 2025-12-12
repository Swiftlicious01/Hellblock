package com.swiftlicious.hellblock.gui.party;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	protected final Context<Integer> islandContext;
	protected final HellblockData hellblockData;
	protected boolean isOwner;

	private volatile boolean refreshInProgress = false;
	private volatile boolean refreshQueued = false;

	public PartyGUI(PartyGUIManager manager, Context<Player> context, Context<Integer> islandContext,
			HellblockData hellblockData, boolean isOwner) {
		this.manager = manager;
		this.context = context;
		this.islandContext = islandContext;
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
	public PartyGUI refresh() {
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

		manager.instance.getScheduler().executeAsync(() -> {
			try {
				// --- Load Owner Data Asynchronously ---
				manager.instance.getStorageManager()
						.getCachedUserDataWithFallback(ownerUUID, manager.instance.getConfigManager().lockData())
						.thenAccept(result -> {
							if (result.isEmpty()) {
								manager.instance.getScheduler().executeSync(() -> context.holder().closeInventory());
								finishRefresh();
								return;
							}

							UserData ownerData = result.get();
							islandContext.arg(ContextKeys.ISLAND_PARTY_COUNT,
									ownerData.getHellblockData().getPartyMembers().size());

							manager.instance.getScheduler().executeSync(() -> {
								try {
									// --- Back button ---
									PartyDynamicGUIElement backElement = (PartyDynamicGUIElement) getElement(
											manager.backSlot);
									if (backElement != null && !backElement.getSlots().isEmpty()) {
										backElement.setItemStack(manager.backIcon.build(context));
									}

									// --- Trusted icon ---
									PartyDynamicGUIElement trustedElement = (PartyDynamicGUIElement) getElement(
											manager.trustedSlot);
									if (trustedElement != null && !trustedElement.getSlots().isEmpty()) {
										trustedElement.setItemStack(manager.trustedIcon.build(context));
									}

									// --- Banned icon ---
									PartyDynamicGUIElement bannedElement = (PartyDynamicGUIElement) getElement(
											manager.bannedSlot);
									if (bannedElement != null && !bannedElement.getSlots().isEmpty()) {
										bannedElement.setItemStack(manager.bannedIcon.build(context));
									}

									// --- Owner icon ---
									PartyDynamicGUIElement ownerElement = (PartyDynamicGUIElement) getElement(
											manager.ownerSlot);
									if (ownerElement != null && !ownerElement.getSlots().isEmpty()) {
										try {
											UUID ownerID = ownerData.getUUID();
											Item<ItemStack> item = manager.instance.getItemManager()
													.wrap(manager.ownerIcon.build(context));

											String username = Bukkit.getPlayer(ownerID) != null
													? Bukkit.getPlayer(ownerID).getName()
													: Bukkit.getOfflinePlayer(ownerID).hasPlayedBefore()
															&& Bukkit.getOfflinePlayer(ownerID).getName() != null
																	? Bukkit.getOfflinePlayer(ownerID).getName()
																	: null;

											boolean isOnline = Bukkit.getPlayer(ownerID) != null
													&& Bukkit.getPlayer(ownerID).isOnline();

											if (username != null) {
												item.displayName(AdventureHelper.miniMessageToJson(
														manager.ownerName.replace("{player}", username).replace(
																"{login_status}", isOnline ? manager.onlineStatus
																		: manager.offlineStatus)));

												List<String> newLore = new ArrayList<>();
												manager.ownerLore
														.forEach(
																lore -> newLore.add(AdventureHelper.miniMessageToJson(
																		lore.replace("{player}", username)
																				.replace("{login_status}", isOnline
																						? manager.onlineStatus
																						: manager.offlineStatus))));
												item.lore(newLore);
											}

											GameProfile profile = GameProfileBuilder.fetch(ownerID);
											item.skull(profile.getProperties().get("textures").iterator().next()
													.getValue());

											ownerElement.setUUID(ownerID);
											ownerElement.setItemStack(item.load());
										} catch (IllegalArgumentException | IOException ignored) {
										}
									}

									// --- Party Members ---
									Set<UUID> party = new HashSet<>(ownerData.getHellblockData().getPartyMembers());
									int maxSize = manager.instance.getCoopManager().getMaxPartySize(ownerData);

									// Render members
									party.forEach(id -> manager.memberIcons.forEach(entry -> renderSlot(entry.left(),
											entry.right().left(), entry.mid(), id, false)));

									// Render new-member slots
									manager.newMemberIcons.forEach((symbol, tuple) -> {
										int index = manager.getMemberSlotIndex(symbol);
										boolean beyondLimit = index >= maxSize;
										renderSlot(symbol, tuple.left(), null, null, beyondLimit);
									});

									// --- Apply Updated Elements ---
									itemsSlotMap.forEach((slot, element) -> {
										if (element instanceof PartyDynamicGUIElement dynamic) {
											inventory.setItem(slot, dynamic.getItemStack().clone());
										}
									});
								} finally {
									finishRefresh();
								}
							});
						}).exceptionally(ex -> {
							manager.instance.getPluginLogger().severe("Failed to refresh PartyGUI: " + ex.getMessage());
							finishRefresh();
							return null;
						});
			} catch (Exception ex) {
				manager.instance.getPluginLogger().severe("Error during PartyGUI refresh: " + ex.getMessage());
				finishRefresh();
			}
		});

		return this;
	}

	/**
	 * Helper to handle debounce finalization and queued refreshes.
	 */
	private void finishRefresh() {
		refreshInProgress = false;
		if (refreshQueued) {
			refreshQueued = false;
			manager.instance.getScheduler().sync().run(this::refresh, context.holder().getLocation());
		}
	}

	/**
	 * Render a single member slot (with placeholder replacement and skulls).
	 */
	private void renderSlot(Character symbol, CustomItem baseItem, @Nullable Section config, @Nullable UUID memberId,
			boolean beyondLimit) {

		PartyDynamicGUIElement element = (PartyDynamicGUIElement) getElement(symbol);
		if (element == null || element.getSlots().isEmpty())
			return;
		
		Context<Player> combinedCtx = context.merge(islandContext);

		Item<ItemStack> item = manager.instance.getItemManager().wrap(baseItem.build(combinedCtx));

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

		element.setItemStack(item.loadCopy());
	}
}