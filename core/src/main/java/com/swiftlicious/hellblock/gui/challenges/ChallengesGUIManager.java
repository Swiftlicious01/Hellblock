package com.swiftlicious.hellblock.gui.challenges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ChallengesGUIManager implements ChallengesGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected boolean highlightCompletion;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, ChallengeType, Action<Player>[]>>> challengeIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, ChallengesGUI> challengesGUICache = new ConcurrentHashMap<>();

	protected char backSlot;

	protected CustomItem backIcon;
	protected Action<Player>[] backActions;

	public ChallengesGUIManager(HellblockPlugin plugin) {
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
		this.challengeIcons.clear();
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getGuiConfig().getSection("challenges.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "challenges.title"));
		this.highlightCompletion = config.getBoolean("highlight-completed-challenges", true);

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section challengesSection = config.getSection("challenges-icons");
		if (challengesSection != null) {
			for (Map.Entry<String, Object> entry : challengesSection.getStringRouteMappedValues(false).entrySet()) {
				try {
					if (entry.getValue() instanceof Section innerSection) {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Challenge icon missing symbol in entry: " + entry.getKey());
							continue;
						}
						char symbol = symbolStr.charAt(0);

						String challengeStr = innerSection.getString("challenge-type");
						if (challengeStr == null) {
							instance.getPluginLogger()
									.severe("Challenge icon missing challenge-type for symbol: " + symbol);
							continue;
						}

						ChallengeType challenge = instance.getChallengeManager()
								.getById(challengeStr.toUpperCase(Locale.ENGLISH));
						if (challenge == null) {
							instance.getPluginLogger()
									.severe("Invalid challenge-type: " + challengeStr + " for symbol: " + symbol);
							continue;
						}

						challengeIcons.add(Tuple.of(symbol, innerSection,
								Tuple.of(
										new SingleItemParser("challenge", innerSection,
												instance.getConfigManager().getItemFormatFunctions()).getItem(),
										challenge, instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action")))));
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"Failed to load challenge icon entry: " + entry.getKey() + " due to: " + e.getMessage());
				}
			}
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
	}

	/**
	 * Open the Challenges GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public boolean openChallengesGUI(Player player) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		ChallengesGUI gui = new ChallengesGUI(this, context, optionalUserData.get().getHellblockData(),
				optionalUserData.get().getChallengeData());
		gui.addElement(new ChallengesDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		challengeIcons.forEach(challenge -> gui
				.addElement(new ChallengesDynamicGUIElement(challenge.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new ChallengesGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		challengesGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof ChallengesGUIHolder))
			return;
		challengesGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		challengesGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof ChallengesGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		ChallengesGUI gui = challengesGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a ChallengesGUI
		if (!(event.getInventory().getHolder() instanceof ChallengesGUIHolder))
			return;

		ChallengesGUI gui = challengesGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			ChallengesGUIElement element = gui.getElement(slot);
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
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(),
						gui.context.holder().getUniqueId().equals(hellblockData.getOwnerUUID()));
				ActionManager.trigger(gui.context, backActions);
				return;
			}

			ChallengeData challengeData = gui.challengeData;

			for (Tuple<Character, Section, Tuple<CustomItem, ChallengeType, Action<Player>[]>> challenge : challengeIcons) {
				if (element.getSymbol() == challenge.left()) {
					event.setCancelled(true);
					ChallengeType challengeType = challenge.right().mid();
					if (challengeData.isChallengeCompleted(challengeType)
							&& !challengeData.isChallengeRewardClaimed(challengeType)) {
						instance.getChallengeManager().performChallengeRewardActions(gui.context.holder(),
								challengeType);
						ActionManager.trigger(gui.context, challenge.right().right());
						break;
					}
				}
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}