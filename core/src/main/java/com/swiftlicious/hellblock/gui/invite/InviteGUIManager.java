package com.swiftlicious.hellblock.gui.invite;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class InviteGUIManager implements InviteGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons;
	protected final ConcurrentMap<UUID, InviteGUI> inviteGUICache;

	protected char backSlot;
	protected char leftSlot;
	protected char rightSlot;
	protected char playerSlot;

	protected String playerFoundName;
	protected String playerName;

	protected CustomItem backIcon;
	protected CustomItem leftIcon;
	protected CustomItem rightIcon;
	protected CustomItem playerIcon;
	protected CustomItem playerFoundIcon;
	protected CustomItem playerNotFoundIcon;
	protected CustomItem searchIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] leftActions;
	protected Action<Player>[] rightActions;
	protected Action<Player>[] playerActions;
	protected Action<Player>[] playerFoundActions;
	protected Action<Player>[] playerNotFoundActions;

	public InviteGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.decorativeIcons = new HashMap<>();
		this.inviteGUICache = new ConcurrentHashMap<>();
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
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getMainConfig().getSection("invitation.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "invite.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section leftSection = config.getSection("scroll-left-icon");
		if (leftSection != null) {
			leftSlot = leftSection.getString("symbol", "L").charAt(0);

			leftIcon = new SingleItemParser("left", leftSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			leftActions = instance.getActionManager(Player.class).parseActions(leftSection.getSection("action"));
		}

		Section rightSection = config.getSection("scroll-right-icon");
		if (rightSection != null) {
			rightSlot = rightSection.getString("symbol", "R").charAt(0);

			rightIcon = new SingleItemParser("right", rightSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			rightActions = instance.getActionManager(Player.class).parseActions(rightSection.getSection("action"));
		}

		Section playerFoundSection = config.getSection("player-found-icon");
		if (playerFoundSection != null) {
			playerFoundName = playerFoundSection.getString("display.name");
			playerFoundIcon = new SingleItemParser("found", playerFoundSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			playerFoundActions = instance.getActionManager(Player.class)
					.parseActions(playerFoundSection.getSection("action"));
		}

		Section playerNotFoundSection = config.getSection("player-not-found-icon");
		if (playerNotFoundSection != null) {
			playerNotFoundIcon = new SingleItemParser("not_found", playerNotFoundSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			playerNotFoundActions = instance.getActionManager(Player.class)
					.parseActions(playerNotFoundSection.getSection("action"));
		}

		Section playerSection = config.getSection("player-icon");
		if (playerSection != null) {
			playerSlot = playerSection.getString("symbol", "P").charAt(0);
			playerName = playerSection.getString("display.name");

			playerIcon = new SingleItemParser("player", playerSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			playerActions = instance.getActionManager(Player.class).parseActions(playerSection.getSection("action"));
		}

		Section searchSection = config.getSection("search-icon");
		if (searchSection != null) {
			searchIcon = new SingleItemParser("search", searchSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					decorativeIcons.put(symbol, Pair.of(
							new SingleItemParser("gui", innerSection,
									instance.getConfigManager().getItemFormatFunctions()).getItem(),
							instance.getActionManager(Player.class).parseActions(innerSection.getSection("action"))));
				}
			}
		}
	}

	/**
	 * Open the Invitation GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public boolean openInvitationGUI(Player player) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		InviteGUI gui = new InviteGUI(this, context, optionalUserData.get().getHellblockData());
		gui.addElement(new InviteDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new InviteDynamicGUIElement(leftSlot, new ItemStack(Material.AIR)));
		gui.addElement(new InviteDynamicGUIElement(rightSlot, new ItemStack(Material.AIR)));
		for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : decorativeIcons.entrySet()) {
			gui.addElement(new InviteGUIElement(entry.getKey(), entry.getValue().left().build(context)));
		}
		gui.saveItems();
		gui.build().show();
		gui.refresh();
		inviteGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof InviteGUIHolder))
			return;
		InviteGUI gui = inviteGUICache.remove(player.getUniqueId());
		if (gui != null) {
			gui.returnItems();
			if (gui.searchTask.isCancelled())
				gui.searchTask.cancel();
		}
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		InviteGUI gui = inviteGUICache.remove(event.getPlayer().getUniqueId());
		if (gui != null) {
			gui.returnItems();
			if (gui.searchTask.isCancelled())
				gui.searchTask.cancel();
		}
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
		if (!(inventory.getHolder() instanceof InviteGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		InviteGUI gui = inviteGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a InviteGUI
		if (!(event.getInventory().getHolder() instanceof InviteGUIHolder))
			return;

		InviteGUI gui = inviteGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv == player.getInventory()) {
			int slot = event.getSlot();
			InviteGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !gui.hellblockData.hasHellblock() || gui.hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getPartyGUIManager().openPartyGUI(gui.context.holder(),
						gui.context.holder().getUniqueId().equals(gui.hellblockData.getOwnerUUID()));
				ActionManager.trigger(gui.context, backActions);
				return;
			}

			if (element.getSymbol() == leftSlot) {
				event.setCancelled(true);
				gui.refreshPlayerHeads(true);
				ActionManager.trigger(gui.context, leftActions);
			}

			if (element.getSymbol() == rightSlot) {
				event.setCancelled(true);
				gui.refreshPlayerHeads(false);
				ActionManager.trigger(gui.context, rightActions);
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (!gui.hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (gui.hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (element.getSymbol() == playerSlot && element.getUUID() != null) {
				event.setCancelled(true);
				Item<ItemStack> searchItem = instance.getItemManager().wrap(searchIcon.build(gui.context));
				String name = Bukkit.getPlayer(element.getUUID()).getName();
				searchItem.displayName(AdventureHelper.miniMessageToJson(name));
				gui.inventory.setItem(0, searchItem.load());
				Item<ItemStack> playerItem = instance.getItemManager().wrap(playerFoundIcon.build(gui.context));
				String username = AdventureHelper.miniMessageToJson(playerName.replace("{player}", name));
				playerItem.displayName(username);
				gui.inventory.setItem(2, playerItem.load());
				ActionManager.trigger(gui.context, playerActions);
			}

		} else {
			int slot = event.getSlot();
			InviteGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !gui.hellblockData.hasHellblock() || gui.hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (!gui.hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (gui.hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (gui.inventory.getItem(2) != null) {
				event.setCancelled(true);
				if (gui.searchedName != null && gui.searchedName.matches("^[a-zA-Z0-9_]+$")) {
					if (instance.getStorageManager().getOnlineUsers().stream()
							.filter(user -> user.isOnline()
									&& !user.getUUID().equals(gui.context.holder().getUniqueId()))
							.map(user -> user.getName()).collect(Collectors.toList()).contains(gui.searchedName)) {
						if (Bukkit.getPlayer(gui.searchedName) == null
								|| !Bukkit.getPlayer(gui.searchedName).isOnline()) {
							ActionManager.trigger(gui.context, playerNotFoundActions);
							return;
						}

						Optional<UserData> invitingPlayer = instance.getStorageManager()
								.getOnlineUser(Bukkit.getPlayer(gui.searchedName).getUniqueId());
						if (invitingPlayer.isEmpty())
							return;
						instance.getCoopManager().sendInvite(userData.get(), invitingPlayer.get());
						ActionManager.trigger(gui.context, playerFoundActions);
						instance.getPartyGUIManager().openPartyGUI(gui.context.holder(),
								gui.hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId()));
						return;
					} else {
						ActionManager.trigger(gui.context, playerNotFoundActions);
						return;
					}
				} else {
					ActionManager.trigger(gui.context, playerNotFoundActions);
					return;
				}
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}