package com.swiftlicious.hellblock.gui.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

public class PartyGUIManager implements PartyGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons;
	protected final List<Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>>> memberIcons;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> newMemberIcons;
	protected final ConcurrentMap<UUID, PartyGUI> partyGUICache;

	protected char backSlot;
	protected char ownerSlot;

	protected String ownerName;
	protected List<String> ownerLore;

	protected CustomItem backIcon;
	protected CustomItem ownerIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] ownerActions;

	public PartyGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.decorativeIcons = new HashMap<>();
		this.memberIcons = new ArrayList<>();
		this.newMemberIcons = new HashMap<>();
		this.partyGUICache = new ConcurrentHashMap<>();
	}

	@Override
	public void load() {
		this.loadConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.decorativeIcons.clear();
		this.memberIcons.clear();
		this.newMemberIcons.clear();
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getMainConfig().getSection("party.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "party.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager().parseActions(backSection.getSection("action"));
		}

		Section ownerSection = config.getSection("owner-icon");
		if (ownerSection != null) {
			ownerSlot = ownerSection.getString("symbol", "O").charAt(0);
			ownerName = ownerSection.getString("display.name");
			ownerLore = ownerSection.getStringList("display.lore");

			ownerIcon = new SingleItemParser("owner", ownerSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			ownerActions = instance.getActionManager().parseActions(ownerSection.getSection("action"));
		}

		Section memberSection = config.getSection("member-icon");
		if (memberSection != null) {
			for (Map.Entry<String, Object> entry : memberSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					memberIcons.add(Tuple.of(symbol, innerSection,
							Tuple.of(
									new SingleItemParser("member", innerSection,
											instance.getConfigManager().getItemFormatFunctions()).getItem(),
									null,
									instance.getActionManager().parseActions(innerSection.getSection("action")))));
				}
			}
		}

		Section newMemberSection = config.getSection("new-member-icon");
		if (newMemberSection != null) {
			for (Map.Entry<String, Object> entry : newMemberSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					newMemberIcons.put(symbol,
							Pair.of(new SingleItemParser("new_member", innerSection,
									instance.getConfigManager().getItemFormatFunctions()).getItem(),
									instance.getActionManager().parseActions(innerSection.getSection("action"))));
				}
			}
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					decorativeIcons.put(symbol,
							Pair.of(new SingleItemParser("gui", innerSection,
									instance.getConfigManager().getItemFormatFunctions()).getItem(),
									instance.getActionManager().parseActions(innerSection.getSection("action"))));
				}
			}
		}
	}

	/**
	 * Open the Party GUI for a player
	 *
	 * @param player  player
	 * @param isOwner is owner or not
	 */
	@Override
	public boolean openPartyGUI(Player player, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		if (optionalUserData.get().getHellblockData().getOwnerUUID() == null) {
			instance.getPluginLogger()
					.warn("Owner UUID for player " + player.getName() + "'s was unable to be retrieved.");
			return false;
		}
		Context<Player> context = Context.player(player);
		PartyGUI gui = new PartyGUI(this, context, optionalUserData.get().getHellblockData(), isOwner);
		gui.addElement(new PartyDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new PartyDynamicGUIElement(ownerSlot, new ItemStack(Material.AIR)));
		Optional<UserData> optionalOwnerData = instance.getStorageManager()
				.getOnlineUser(optionalUserData.get().getHellblockData().getOwnerUUID());
		if (optionalOwnerData.isEmpty()) {
			instance.getPluginLogger().warn("Player " + player.getName() + "'s hellblock owner data for "
					+ optionalUserData.get().getHellblockData().getOwnerUUID() + "  has not been loaded yet.");
			return false;
		}
		for (int i = 0; i < instance.getCoopManager().getMaxPartySize(optionalOwnerData.get()); i++) {
			for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> entry : memberIcons) {
				gui.addElement(new PartyDynamicGUIElement(entry.left(), new ItemStack(Material.AIR)));
			}
			if (isOwner) {
				for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : newMemberIcons.entrySet()) {
					gui.addElement(new PartyDynamicGUIElement(entry.getKey(), new ItemStack(Material.AIR)));
				}
			}
		}
		for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : decorativeIcons.entrySet()) {
			gui.addElement(new PartyGUIElement(entry.getKey(), entry.getValue().left().build(context)));
		}
		gui.build().refresh().show();
		partyGUICache.put(player.getUniqueId(), gui);
		return true;
	}

	/**
	 * This method handles the closing of an inventory.
	 *
	 * @param event The InventoryCloseEvent that triggered this method.
	 */
	@EventHandler
	public void onCloseInv(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;
		if (!(event.getInventory().getHolder() instanceof PartyGUIHolder))
			return;
		partyGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		partyGUICache.remove(event.getPlayer().getUniqueId());
	}

	/**
	 * This method handles dragging items in an inventory.
	 *
	 * @param event The InventoryDragEvent that triggered this method.
	 */
	@EventHandler
	public void onDragInv(InventoryDragEvent event) {
		if (event.isCancelled())
			return;
		Inventory inventory = event.getInventory();
		if (!(inventory.getHolder() instanceof PartyGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		PartyGUI gui = partyGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		event.setResult(Result.DENY);

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	/**
	 * This method handles inventory click events.
	 *
	 * @param event The InventoryClickEvent that triggered this method.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onClickInv(InventoryClickEvent event) {
		Inventory clickedInv = event.getClickedInventory();
		if (clickedInv == null)
			return;

		Player player = (Player) event.getWhoClicked();

		// Check if the clicked inventory is a PartyGUI
		if (!(event.getInventory().getHolder() instanceof PartyGUIHolder))
			return;

		PartyGUI gui = partyGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			PartyGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			HellblockData hellblockData = gui.hellblockData;
			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !hellblockData.hasHellblock() || hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManagerInterface.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(), gui.isOwner);
				ActionManagerInterface.trigger(gui.context, backActions);
				return;
			}

			Audience audience = instance.getSenderFactory().getAudience(gui.context.holder());

			if (!hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (element.getSymbol() == ownerSlot) {
				event.setCancelled(true);
				ActionManagerInterface.trigger(gui.context, ownerActions);
			}

			for (Tuple<Character, Section, Tuple<CustomItem, UUID, Action<Player>[]>> memberIcon : memberIcons) {
				if (element.getSymbol() == memberIcon.left() && element.getUUID() != null) {
					event.setCancelled(true);
					String username = Bukkit.getPlayer(element.getUUID()) != null
							? Bukkit.getPlayer(element.getUUID()).getName()
							: Bukkit.getOfflinePlayer(element.getUUID()).hasPlayedBefore()
									&& Bukkit.getOfflinePlayer(element.getUUID()).getName() != null
											? Bukkit.getOfflinePlayer(element.getUUID()).getName()
											: null;
					if (username != null) {
						instance.getCoopManager().removeMemberFromHellblock(userData.get(), username,
								memberIcon.right().mid());
						ActionManagerInterface.trigger(gui.context, memberIcon.right().right());
						break;
					}
				}
			}

			Pair<CustomItem, Action<Player>[]> newMemberIcon = this.newMemberIcons.get(element.getSymbol());
			if (newMemberIcon != null && element.getUUID() == null) {
				event.setCancelled(true);
				instance.getInviteGUIManager().openInvitationGUI(player);
				ActionManagerInterface.trigger(gui.context, newMemberIcon.right());
				return;
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}