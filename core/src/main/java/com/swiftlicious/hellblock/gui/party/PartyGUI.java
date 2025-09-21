package com.swiftlicious.hellblock.gui.party;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.mojang.authlib.GameProfile;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.GameProfileBuilder;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Tuple;

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
		for (Map.Entry<Integer, PartyGUIElement> entry : itemsSlotMap.entrySet()) {
			this.inventory.setItem(entry.getKey(), entry.getValue().getItemStack().clone());
		}
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
		VersionHelper.getNMSManager().updateInventoryTitle(context.holder(),
				AdventureHelper.componentToJson(AdventureHelper.miniMessage(manager.title.render(context, true))));
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
		PartyDynamicGUIElement backElement = (PartyDynamicGUIElement) getElement(manager.backSlot);
		if (backElement != null && !backElement.getSlots().isEmpty()) {
			backElement.setItemStack(manager.backIcon.build(context));
		}
		PartyDynamicGUIElement ownerElement = (PartyDynamicGUIElement) getElement(manager.ownerSlot);
		if (ownerElement != null && !ownerElement.getSlots().isEmpty()) {
			try {
				UUID ownerUUID = hellblockData.getOwnerUUID();
				Item<ItemStack> item = manager.instance.getItemManager().wrap(manager.ownerIcon.build(context));
				String username = Bukkit.getPlayer(ownerUUID) != null ? Bukkit.getPlayer(ownerUUID).getName()
						: Bukkit.getOfflinePlayer(ownerUUID).hasPlayedBefore()
								&& Bukkit.getOfflinePlayer(ownerUUID).getName() != null
										? Bukkit.getOfflinePlayer(ownerUUID).getName()
										: null;
				if (username != null) {
					String newName = AdventureHelper.miniMessageToJson(manager.ownerName.replace("{player}", username));
					item.displayName(newName);
					List<String> newLore = new ArrayList<>();
					for (String lore : manager.ownerLore) {
						newLore.add(AdventureHelper.miniMessageToJson(lore.replace("{player}", username)));
					}
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
		Set<UUID> party = hellblockData.getParty();
		if (party == null || party.isEmpty()) {
			for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : manager.newMemberIcons.entrySet()) {
				PartyDynamicGUIElement memberElement = (PartyDynamicGUIElement) getElement(entry.getKey());
				if (memberElement != null && !memberElement.getSlots().isEmpty()) {
					memberElement.setItemStack(entry.getValue().left().build(context));
				}
			}
		} else {
			for (UUID id : party) {
				for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> entry : manager.memberIcons) {
					PartyDynamicGUIElement memberElement = (PartyDynamicGUIElement) getElement(entry.left());
					if (memberElement != null && !memberElement.getSlots().isEmpty()) {
						Item<ItemStack> item = manager.instance.getItemManager()
								.wrap(entry.right().left().build(context));
						String username = Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
								: Bukkit.getOfflinePlayer(id).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(id).getName() != null
												? Bukkit.getOfflinePlayer(id).getName()
												: null;
						if (username != null) {
							String newName = AdventureHelper.miniMessageToJson(
									entry.mid().getString("display.name").replace("{player}", username));
							item.displayName(newName);
							List<String> newLore = new ArrayList<>();
							if (isOwner) {
								for (String lore : entry.mid().getStringList("display.owner-lore")) {
									newLore.add(AdventureHelper.miniMessageToJson(lore.replace("{player}", username)));
								}
							} else {
								for (String lore : entry.mid().getStringList("display.member-lore")) {
									newLore.add(AdventureHelper.miniMessageToJson(lore.replace("{player}", username)));
								}
							}
							item.lore(newLore);
						}
						try {
							GameProfile profile = GameProfileBuilder.fetch(id);
							item.skull(profile.getProperties().get("textures").iterator().next().getValue());
							memberElement.setUUID(id);
							memberElement.setItemStack(item.load());
						} catch (IllegalArgumentException | IOException ex) {
							// ignored
						}
					}
				}
			}
			for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : manager.newMemberIcons.entrySet()) {
				PartyDynamicGUIElement memberElement = (PartyDynamicGUIElement) getElement(entry.getKey());
				if (memberElement != null && memberElement.getItemStack().getType() == Material.AIR) {
					memberElement.setItemStack(entry.getValue().left().build(context));
				}
			}
		}
		for (Map.Entry<Integer, PartyGUIElement> entry : itemsSlotMap.entrySet()) {
			if (entry.getValue() instanceof PartyDynamicGUIElement dynamicGUIElement) {
				this.inventory.setItem(entry.getKey(), dynamicGUIElement.getItemStack().clone());
			}
		}
		return this;
	}
}