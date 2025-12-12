package com.swiftlicious.hellblock.gui.challenges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import com.swiftlicious.hellblock.challenges.ChallengeType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.ChallengeData;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class ChallengesGUIManager implements ChallengesGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected final Map<Integer, TextValue<Player>> pageTitles = new HashMap<>();
	protected String[] layout;
	protected final Map<Integer, String[]> pageLayouts = new HashMap<>();
	protected boolean highlightCompletion;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, ChallengeType, Action<Player>[]>>> challengeIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, ChallengesGUI> challengesGUICache = new ConcurrentHashMap<>();

	protected char backSlot;
	protected char closeSlot;

	protected CustomItem backIcon;
	protected Action<Player>[] backActions;
	protected CustomItem closeIcon;
	protected Action<Player>[] closeActions;

	protected char leftSlot;
	protected char rightSlot;

	protected CustomItem leftIcon;
	protected Action<Player>[] leftActions;
	protected CustomItem rightIcon;
	protected Action<Player>[] rightActions;

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
		this.pageLayouts.clear();
		this.pageTitles.clear();
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("challenges.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for challenges.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("challenges.gui");
		if (config == null) {
			instance.getPluginLogger()
					.severe("challenges.gui returned null, please regenerate your challenges.yml GUI file.");
			return;
		}

		// check if there’s a `pages:` section
		Section pagesSection = config.getSection("pages");
		if (pagesSection != null) {
			for (Map.Entry<String, Object> entry : pagesSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section pageSection) {
					try {
						int pageIndex = Integer.parseInt(entry.getKey());
						List<String> layoutLines = pageSection.getStringList("layout", new ArrayList<>());
						pageLayouts.put(pageIndex - 1, layoutLines.toArray(new String[0]));
						
						// Optional per-page title
						if (pageSection.contains("title")) {
							pageTitles.put(pageIndex - 1, TextValue.auto(pageSection.getString("title")));
						}
						
					} catch (NumberFormatException e) {
						instance.getPluginLogger().severe("Invalid page number: " + entry.getKey());
					}
				}
			}
		} else {
			// fallback to single-layout mode
			this.layout = config.getStringList("layout", new ArrayList<>()).toArray(new String[0]);
		}

		this.title = TextValue.auto(config.getString("title", "challenges.title"));
		this.highlightCompletion = config.getBoolean("highlight-completed-challenges", true);

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section closeSection = config.getSection("close-icon");
		if (closeSection != null) {
			closeSlot = closeSection.getString("symbol", "X").charAt(0);

			closeIcon = new SingleItemParser("close", closeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			closeActions = instance.getActionManager(Player.class).parseActions(closeSection.getSection("action"));
		}

		Section leftSection = config.getSection("left-icon");
		if (leftSection != null) {
			leftSlot = leftSection.getString("symbol", "L").charAt(0);

			leftIcon = new SingleItemParser("left", leftSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			leftActions = instance.getActionManager(Player.class).parseActions(leftSection.getSection("action"));
		}

		Section rightSection = config.getSection("right-icon");
		if (rightSection != null) {
			rightSlot = rightSection.getString("symbol", "R").charAt(0);

			rightIcon = new SingleItemParser("right", rightSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			rightActions = instance.getActionManager(Player.class).parseActions(rightSection.getSection("action"));
		}

		Section challengesSection = config.getSection("challenge-icons");
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
										new SingleItemParser(challenge.getChallengeId().toLowerCase(), innerSection,
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

	@Override
	public boolean openChallengesGUI(Player player, int islandId, boolean isOwner, boolean showBackIcon) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		ChallengesGUI gui = new ChallengesGUI(this, context, islandContext, optionalUserData.get().getHellblockData(),
				optionalUserData.get().getChallengeData(), isOwner, showBackIcon, leftIcon, leftActions, rightIcon,
				rightActions);
		gui.addElement(
				new ChallengesDynamicGUIElement(showBackIcon ? backSlot : closeSlot, new ItemStack(Material.AIR)));
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

		// Handle shift-clicks, number keys, or clicking outside GUI
		if (event.getClick().isShiftClick() || event.getClick().isKeyboardClick()) {
			event.setCancelled(true);
			return;
		}

		if (!Objects.equals(clickedInv, player.getInventory())) {
			int slot = event.getSlot();
			ChallengesGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (gui.showBackIcon) {
				if (element.getSymbol() == backSlot) {
					event.setCancelled(true);
					instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(), gui.islandContext.holder(),
							gui.isOwner);
					ActionManager.trigger(gui.context, backActions);
					return;
				}
			} else {
				if (element.getSymbol() == closeSlot) {
					event.setCancelled(true);
					ActionManager.trigger(gui.context, closeActions);
					return;
				}
			}

			if (slot == gui.getLeftIconSlot()) {
				event.setCancelled(true);
				gui.previousPage();
				return;
			}
			if (slot == gui.getRightIconSlot()) {
				event.setCancelled(true);
				gui.nextPage();
				return;
			}

			HellblockData hellblockData = gui.hellblockData;
			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !hellblockData.hasHellblock() || hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				instance.getSenderFactory().wrap(player).sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
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
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}