package com.swiftlicious.hellblock.gui.invite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final ConcurrentMap<UUID, InviteGUI> inviteGUICache = new ConcurrentHashMap<>();

	protected final List<Integer> headSlots = new ArrayList<>();

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
		Section configFile = instance.getConfigManager().getGUIConfig("invites.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for invites.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("invitation.gui");
		if (config == null) {
			instance.getPluginLogger()
					.severe("invitation.gui returned null, please regenerate your invites.yml GUI file.");
			return;
		}

		this.layout = config.getStringList("layout", new ArrayList<>()).toArray(new String[0]);
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
					try {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Decorative icon missing symbol in entry: " + entry.getKey());
							continue;
						}

						char symbol = symbolStr.charAt(0);

						decorativeIcons.put(symbol,
								Pair.of(new SingleItemParser("gui", innerSection,
										instance.getConfigManager().getItemFormatFunctions()).getItem(),
										instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action"))));
					} catch (Exception e) {
						instance.getPluginLogger().severe("Failed to load decorative icon entry: " + entry.getKey()
								+ " due to: " + e.getMessage());
					}
				}
			}
		}

		parseHeadSlotsFromLayout(Arrays.asList(this.layout), this.playerSlot);
	}

	/**
	 * Parse the layout and cache which slots are player head slots. This must be
	 * called once after config is loaded.
	 */
	private void parseHeadSlotsFromLayout(List<String> layout, char playerSlotSymbol) {
		headSlots.clear();
		for (int row = 0; row < layout.size(); row++) {
			String line = layout.get(row);
			for (int col = 0; col < line.length(); col++) {
				char symbol = line.charAt(col);
				int slot = row * 9 + col;
				if (symbol == playerSlotSymbol) {
					headSlots.add(slot);
				}
			}
		}
	}

	protected int getPageSize() {
		return headSlots.size();
	}

	@Override
	public boolean openInvitationGUI(Player player, int islandId, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		InviteGUI gui = new InviteGUI(this, context, islandContext, optionalUserData.get().getHellblockData(), isOwner);
		gui.addElement(new InviteDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new InviteDynamicGUIElement(leftSlot, new ItemStack(Material.AIR)));
		gui.addElement(new InviteDynamicGUIElement(rightSlot, new ItemStack(Material.AIR)));
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new InviteGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.saveItems(player);
		gui.clearPlayerInventory(player);
		// start polling search and populate heads
		gui.startSearchPolling();
		gui.build().show();
		gui.currentPage = 0;
		gui.refreshPlayerHeads(gui.currentPage);
		gui.refreshSearch(); // initial
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
			gui.cancelSearchPolling();
			// if they close GUI but we want to return items
			gui.returnItems(player);
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
			gui.cancelSearchPolling();
			// if they quit but we want to return items
			gui.returnItems(event.getPlayer());
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

		// Check if any dragged slot is in the GUI (top inventory)
		for (int slot : event.getRawSlots()) {
			if (slot < event.getInventory().getSize()) {
				event.setCancelled(true);
				return;
			}
		}

		// If the drag is only in the player inventory, do nothing special
		// but still make sure no ghost items appear
		event.setCancelled(true);

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

		if (!(event.getInventory().getHolder() instanceof InviteGUIHolder))
			return;

		Player player = (Player) event.getWhoClicked();
		InviteGUI gui = inviteGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		// Handle shift-clicks, number keys, or clicking outside GUI
		if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
			event.setCancelled(true);
			return;
		}

		// unify common checks for UserData / hellblock ownership / abandonment
		Optional<UserData> optionalUser = instance.getStorageManager()
				.getOnlineUser(gui.context.holder().getUniqueId());

		if (optionalUser.isEmpty() || !gui.hellblockData.hasHellblock() || gui.hellblockData.getOwnerUUID() == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		// determine clicked element based on clicked slot mapping
		int slot = event.getSlot();
		InviteGUIElement element = gui.getElement(slot);
		if (element == null) {
			event.setCancelled(true);
			return;
		}

		// decorative icons (shortcut)
		Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
		if (decorativeIcon != null) {
			event.setCancelled(true);
			ActionManager.trigger(gui.context, decorativeIcon.right());
			return;
		}

		// navigation slots
		if (element.getSymbol() == backSlot) {
			event.setCancelled(true);
			// cleanup
			gui.cancelSearchPolling();
			gui.returnItems(player);
			inviteGUICache.remove(player.getUniqueId());
			instance.getPartyGUIManager().openPartyGUI(gui.context.holder(), gui.islandContext.holder(), gui.isOwner);
			ActionManager.trigger(gui.context, backActions);
			return;
		}

		if (element.getSymbol() == leftSlot) {
			event.setCancelled(true);
			gui.refreshPlayerHeads(Math.max(0, gui.currentPage - 1));
			ActionManager.trigger(gui.context, leftActions);
			return;
		}

		if (element.getSymbol() == rightSlot) {
			event.setCancelled(true);
			gui.refreshPlayerHeads(gui.currentPage + 1);
			ActionManager.trigger(gui.context, rightActions);
			return;
		}

		// owner checks (only after navigation/decorative)
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

		// If clicking the player's own inventory (hotbar area) - previous code
		// specially handled this,
		// but we've already retrieved element by slot so we treat uniformly.
		event.setCancelled(true); // we always cancel when interacting with the invite GUI UI

		// If element represents a player head result in the anvil result slot (slot 2)
		if (element.getSymbol() == playerSlot && element.getUUID() != null) {
			// set the search & result UI (like original behavior)
			Item<ItemStack> searchItem = instance.getItemManager().wrap(searchIcon.build(gui.context));
			String name = Bukkit.getPlayer(element.getUUID()).getName();
			searchItem.displayName(AdventureHelper.miniMessageToJson(name));
			gui.inventory.setItem(0, searchItem.load());

			Item<ItemStack> playerItem = instance.getItemManager().wrap(playerFoundIcon.build(gui.context));
			String username = AdventureHelper.miniMessageToJson(playerName.replace("{player}", name));
			playerItem.displayName(username);
			gui.inventory.setItem(2, playerItem.load());

			ActionManager.trigger(gui.context, playerActions);
			// We don't return here; allow invite flow below if clicking into result to send
			// invite
		}

		// If top result slot exists and has an item (player found case) and the clicked
		// element maps to that result:
		if (gui.inventory.getItem(2) != null && gui.inventory.getItem(2).getType() != Material.AIR) {
			// Ensure valid searchedName (the anvil rename text)
			if (gui.searchedName != null && gui.searchedName.matches("^[a-zA-Z0-9_]+$")) {
				// Check online presence
				boolean present = instance.getStorageManager().getOnlineUsers().stream()
						.filter(user -> user.isOnline() && !user.getUUID().equals(gui.context.holder().getUniqueId()))
						.map(UserData::getName).anyMatch(n -> n.equalsIgnoreCase(gui.searchedName));

				if (!present) {
					ActionManager.trigger(gui.context, playerNotFoundActions);
					return;
				}

				// Double-check player object is online
				Player target = Bukkit.getPlayer(gui.searchedName);
				if (target == null || !target.isOnline()) {
					ActionManager.trigger(gui.context, playerNotFoundActions);
					return;
				}

				Optional<UserData> invitingPlayer = instance.getStorageManager().getOnlineUser(target.getUniqueId());
				if (invitingPlayer.isEmpty()) {
					ActionManager.trigger(gui.context, playerNotFoundActions);
					return;
				}

				// send invite
				instance.getCoopManager().sendInvite(optionalUser.get(), invitingPlayer.get());
				ActionManager.trigger(gui.context, playerFoundActions);

				// cleanup and go back to party GUI
				gui.cancelSearchPolling();
				gui.returnItems(player);
				inviteGUICache.remove(player.getUniqueId());
				instance.getPartyGUIManager().openPartyGUI(gui.context.holder(), gui.islandContext.holder(),
						gui.hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId()));
				return;
			} else {
				ActionManager.trigger(gui.context, playerNotFoundActions);
				return;
			}
		}

		// Otherwise if clicking any head in the bottom area (cachedHeads), invite that
		// player if possible:
		// Check if this slot is one of the dynamic head slots
		if (headSlots.contains(slot)) {
			InviteDynamicGUIElement clicked = gui.cachedHeads.get(slot);
			if (clicked == null || clicked.getUUID() == null) {
				// No head or invalid entry
				return;
			}

			UUID targetUUID = clicked.getUUID();

			// Ensure target is online & eligible
			Player target = Bukkit.getPlayer(targetUUID);
			if (target == null || !target.isOnline()) {
				ActionManager.trigger(gui.context, playerNotFoundActions);
				return;
			}

			Optional<UserData> targetUD = instance.getStorageManager().getOnlineUser(targetUUID);
			if (targetUD.isEmpty()) {
				ActionManager.trigger(gui.context, playerNotFoundActions);
				return;
			}

			// Prevent double-inviting (shouldn't happen since filtered, but safe-guard)
			if (targetUD.get().getHellblockData().hasInvite(gui.context.holder().getUniqueId())) {
				ActionManager.trigger(gui.context, playerNotFoundActions);
				return;
			}

			// Perform invite
			instance.getCoopManager().sendInvite(optionalUser.get(), targetUD.get());
			ActionManager.trigger(gui.context, playerFoundActions);

			// Close GUI, restore items and return to previous menu
			gui.cancelSearchPolling();
			gui.returnItems(player);
			inviteGUICache.remove(player.getUniqueId());
			instance.getPartyGUIManager().openPartyGUI(gui.context.holder(), gui.islandContext.holder(), gui.isOwner);
			return;
		}

		// default refresh (if any) - keep previous behavior: schedule a refresh next
		// tick
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}