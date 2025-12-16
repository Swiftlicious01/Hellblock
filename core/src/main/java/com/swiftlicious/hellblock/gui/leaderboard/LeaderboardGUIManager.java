package com.swiftlicious.hellblock.gui.leaderboard;

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
import com.swiftlicious.hellblock.events.leaderboard.LeaderboardUpdateEvent;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class LeaderboardGUIManager implements LeaderboardGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected final Map<Integer, TextValue<Player>> pageTitles = new HashMap<>();
	protected String[] layout;
	protected final Map<Integer, String[]> pageLayouts = new HashMap<>();
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final ConcurrentMap<UUID, LeaderboardGUI> leaderboardGUICache = new ConcurrentHashMap<>();

	protected char backSlot;
	protected char topSlot;
	protected char placeholderSlot;

	protected CustomItem backIcon;
	protected CustomItem topIcon;
	protected CustomItem placeholderIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] topActions;
	protected Action<Player>[] placeholderActions;

	protected Section topSection;
	protected Section placeholderSection;

	protected char leftSlot;
	protected char rightSlot;

	protected CustomItem leftIcon;
	protected Action<Player>[] leftActions;
	protected CustomItem rightIcon;
	protected Action<Player>[] rightActions;

	public LeaderboardGUIManager(HellblockPlugin plugin) {
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
		this.pageLayouts.clear();
		this.pageTitles.clear();
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("leaderboard.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for leaderboard.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("leaderboard.gui");
		if (config == null) {
			instance.getPluginLogger()
					.severe("leaderboard.gui returned null, please regenerate your leaderboard.yml GUI file.");
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

		this.title = TextValue.auto(config.getString("title", "leaderboard.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "B").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section topSection = config.getSection("top-icon");
		if (topSection != null) {
			char symbol = topSection.getString("symbol", "T").charAt(0);
			topSlot = symbol;
			placeholderSlot = symbol;

			topIcon = new SingleItemParser("top", topSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			topActions = instance.getActionManager(Player.class).parseActions(topSection.getSection("action"));

			this.topSection = topSection;
		}

		Section placeholderSection = config.getSection("placeholder-icon");
		if (placeholderSection != null) {
			placeholderIcon = new SingleItemParser("placeholder", placeholderSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			placeholderActions = instance.getActionManager(Player.class)
					.parseActions(placeholderSection.getSection("action"));
			this.placeholderSection = placeholderSection;
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
	public boolean openLeaderboardGUI(Player player, int islandId, boolean isOwner, boolean showBackIcon) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		LeaderboardGUI gui = new LeaderboardGUI(this, context, islandContext, optionalUserData.get().getHellblockData(),
				isOwner, showBackIcon, leftIcon, leftActions, rightIcon, rightActions);
		gui.addElement(new LeaderboardDynamicGUIElement(backSlot,
				showBackIcon ? new ItemStack(Material.AIR) : gui.getDecorativePlaceholderForSlot(backSlot)));
		gui.addElement(new LeaderboardDynamicGUIElement(topSlot, new ItemStack(Material.AIR)));
		gui.addElement(new LeaderboardDynamicGUIElement(placeholderSlot, new ItemStack(Material.AIR)));
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new LeaderboardGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.populateTopIslands(getTopSlotCount());
		gui.startAutoRefresh(15L, getTopSlotCount()); // refresh every 15 seconds
		leaderboardGUICache.put(player.getUniqueId(), gui);
		return true;
	}
	
	@EventHandler
	public void onLeaderboardUpdate(LeaderboardUpdateEvent event) {
	    Map<Integer, Float> newTopIslands = event.getTopIslands();

	    // Update all open GUIs
	    for (LeaderboardGUI gui : leaderboardGUICache.values()) {
	        gui.handleLiveLeaderboardUpdate(newTopIslands);
	    }
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
		if (!(event.getInventory().getHolder() instanceof LeaderboardGUIHolder))
			return;
		LeaderboardGUI gui = leaderboardGUICache.remove(player.getUniqueId());
		if (gui != null && gui.refreshTask != null && !gui.refreshTask.isCancelled()) {
			gui.refreshTask.cancel();
		}
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		LeaderboardGUI gui = leaderboardGUICache.remove(event.getPlayer().getUniqueId());
		if (gui != null && gui.refreshTask != null && !gui.refreshTask.isCancelled()) {
			gui.refreshTask.cancel();
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
		if (!(inventory.getHolder() instanceof LeaderboardGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		LeaderboardGUI gui = leaderboardGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a LeaderboardGUI
		if (!(event.getInventory().getHolder() instanceof LeaderboardGUIHolder))
			return;

		LeaderboardGUI gui = leaderboardGUICache.get(player.getUniqueId());
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
			LeaderboardGUIElement element = gui.getElement(slot);
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

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				if (!gui.showBackIcon) {
					return;
				}
				instance.getHellblockGUIManager().openHellblockGUI(player, gui.islandContext.holder(), gui.isOwner);
				ActionManager.trigger(gui.context, backActions);
				return;
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

			if (element.getSymbol() == topSlot && element.getUUID() != null) {
				event.setCancelled(true);
				UUID targetUUID = element.getUUID();
				instance.getStorageManager()
						.getCachedUserDataWithFallback(targetUUID, instance.getConfigManager().lockData())
						.thenAccept(ownerOpt -> {
							if (ownerOpt.isEmpty()) {
								final String username = Bukkit.getOfflinePlayer(targetUUID).getName();
								instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
												.arguments(AdventureHelper.miniMessageToComponent(username != null
														? username
														: instance.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())))
												.build()));
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
										Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
												Sound.Source.PLAYER, 1, 1));
								return;
							}

							final UserData ownerUser = ownerOpt.get();
							final HellblockData ownerData = ownerUser.getHellblockData();

							if (ownerData.isAbandoned()) {
								instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_VISIT_ABANDONED.build()));
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
										Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
												Sound.Source.PLAYER, 1, 1));
								return;
							}

							if (ownerData.getBannedMembers().contains(player.getUniqueId())
									&& (!(player.isOp() || player.hasPermission("hellblock.admin")
											|| player.hasPermission("hellblock.bypass.interact")))) {
								instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
												.arguments(AdventureHelper.miniMessageToComponent(ownerUser.getName()))
												.build()));
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
										Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
												Sound.Source.PLAYER, 1, 1));
								return;
							}

							instance.getCoopManager().checkIfVisitorsAreWelcome(player, ownerUser.getUUID())
									.thenAccept(status -> {
										if (ownerData.isLocked() || !status) {
											instance.getSenderFactory().wrap(player).sendMessage(instance
													.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS
															.arguments(AdventureHelper
																	.miniMessageToComponent(ownerUser.getName()))
															.build()));
											AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
													Sound.sound(
															net.kyori.adventure.key.Key
																	.key("minecraft:entity.villager.no"),
															Sound.Source.PLAYER, 1, 1));
											return;
										}

										instance.getVisitManager().handleVisit(player, targetUUID);
										ActionManager.trigger(gui.context, topActions);
									});
						});
				return;
			}

			if (element.getSymbol() == placeholderSlot && element.getUUID() == null) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, placeholderActions);
				return;
			}
		}

		// Refresh the GUI
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public int getTopSlotCount() {
		int count = 0;

		if (hasPageLayouts()) {
			for (String[] page : getActiveLayouts()) {
				for (String row : page) {
					for (char c : row.toCharArray()) {
						if (c == topSlot)
							count++;
					}
				}
			}
		} else {
			for (String row : layout) {
				for (char c : row.toCharArray()) {
					if (c == topSlot)
						count++;
				}
			}
		}
		return count;
	}

	public boolean hasPageLayouts() {
		return !this.pageLayouts.isEmpty();
	}

	public String[][] getActiveLayouts() {
		if (this.pageLayouts.isEmpty()) {
			return new String[][] { this.layout }; // single layout wrapped in 2D array
		}
		return this.pageLayouts.values().toArray(new String[0][]); // multi-page layouts
	}
}