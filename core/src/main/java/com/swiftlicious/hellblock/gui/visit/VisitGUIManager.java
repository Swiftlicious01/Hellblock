package com.swiftlicious.hellblock.gui.visit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

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
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VisitManager.VisitEntry;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.player.VisitData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class VisitGUIManager implements VisitGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected final Map<Integer, TextValue<Player>> pageTitles = new HashMap<>();
	protected String[] layout;
	protected final Map<Integer, String[]> pageLayouts = new HashMap<>();
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, VisitSorter, Action<Player>[]>>> sortingIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, VisitGUI> visitGUICache = new ConcurrentHashMap<>();

	private final Map<UUID, SchedulerTask> guiUpdateTasks = new ConcurrentHashMap<>();

	protected char backSlot;
	protected char filledSlot;
	protected char emptySlot;

	protected Section filledSection;
	protected Section emptySection;

	protected CustomItem backIcon;
	protected CustomItem filledIcon;
	protected CustomItem emptyIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] filledActions;
	protected Action<Player>[] emptyActions;
	protected Requirement<Player>[] emptyRequirements;

	protected char leftSlot;
	protected char rightSlot;

	protected CustomItem leftIcon;
	protected Action<Player>[] leftActions;
	protected CustomItem rightIcon;
	protected Action<Player>[] rightActions;

	public VisitGUIManager(HellblockPlugin plugin) {
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
		this.sortingIcons.clear();
		this.pageLayouts.clear();
		this.pageTitles.clear();
	}

	@Override
	public void disable() {
		unload();
		this.guiUpdateTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("visit.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for visit.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("visit.gui");
		if (config == null) {
			instance.getPluginLogger().severe("visit.gui returned null, please regenerate your visit.yml GUI file.");
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

		this.title = TextValue.auto(config.getString("title", "visit.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "B").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
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

		filledSection = config.getSection("filled-icon");
		if (filledSection != null) {
			char symbol = filledSection.getString("symbol", "S").charAt(0);
			filledSlot = symbol;
			emptySlot = symbol;

			filledIcon = new SingleItemParser("filled", filledSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			filledActions = instance.getActionManager(Player.class).parseActions(filledSection.getSection("action"));
		}

		emptySection = config.getSection("empty-icon");
		if (emptySection != null) {
			emptyIcon = new SingleItemParser("empty", emptySection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			emptyActions = instance.getActionManager(Player.class).parseActions(emptySection.getSection("action"));
			emptyRequirements = instance.getRequirementManager(Player.class)
					.parseRequirements(emptySection.getSection("requirement"), true);
		}

		Section sortingSection = config.getSection("sorting-icons");
		if (sortingSection != null) {
			for (Map.Entry<String, Object> entry : sortingSection.getStringRouteMappedValues(false).entrySet()) {
				try {
					if (entry.getValue() instanceof Section innerSection) {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Sorting icon missing symbol in entry: " + entry.getKey());
							continue;
						}
						char symbol = symbolStr.charAt(0);

						String sortStr = innerSection.getString("sort-type");
						if (sortStr == null) {
							instance.getPluginLogger().severe("Sorting icon missing sort-type for symbol: " + symbol);
							continue;
						}

						VisitSorter sortingType;
						try {
							sortingType = VisitSorter.valueOf(sortStr.toUpperCase(Locale.ENGLISH));
						} catch (IllegalArgumentException e) {
							instance.getPluginLogger()
									.severe("Invalid sort-type: " + sortStr + " for symbol: " + symbol);
							continue;
						}

						sortingIcons.add(Tuple.of(symbol, innerSection,
								Tuple.of(
										new SingleItemParser(sortingType.toString().toLowerCase(), innerSection,
												instance.getConfigManager().getItemFormatFunctions()).getItem(),
										sortingType, instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action")))));
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"Failed to load sorting icon entry: " + entry.getKey() + " due to: " + e.getMessage());
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
	public boolean openVisitGUI(Player player, int islandId, VisitSorter visitSorter, boolean isOwner,
			boolean showBackIcon) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		VisitGUI gui = new VisitGUI(this, visitSorter, context, islandContext,
				optionalUserData.get().getHellblockData(), isOwner, showBackIcon, leftIcon, leftActions, rightIcon,
				rightActions);
		gui.addElement(new VisitDynamicGUIElement(backSlot,
				showBackIcon ? new ItemStack(Material.AIR) : gui.getDecorativePlaceholderForSlot(backSlot)));
		gui.addElement(new VisitDynamicGUIElement(filledSlot, new ItemStack(Material.AIR)));
		gui.addElement(new VisitDynamicGUIElement(emptySlot, new ItemStack(Material.AIR)));
		sortingIcons.forEach(
				sortType -> gui.addElement(new VisitDynamicGUIElement(sortType.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new VisitGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refreshAndRepopulate();
		if (visitSorter == VisitSorter.FEATURED) {
			startFeaturedGUIUpdate(player);
		}
		visitGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof VisitGUIHolder))
			return;
		visitGUICache.remove(player.getUniqueId());
		cancelGUIUpdate(player);
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		visitGUICache.remove(event.getPlayer().getUniqueId());
		cancelGUIUpdate(event.getPlayer());
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
		if (!(inventory.getHolder() instanceof VisitGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		VisitGUI gui = visitGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a VisitGUI
		if (!(event.getInventory().getHolder() instanceof VisitGUIHolder))
			return;

		VisitGUI gui = visitGUICache.get(player.getUniqueId());
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
			VisitGUIElement element = gui.getElement(slot);
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
				boolean opened = instance.getHellblockGUIManager().openHellblockGUI(player, gui.islandContext.holder(),
						gui.isOwner);
				if (opened)
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

			for (Tuple<Character, Section, Tuple<CustomItem, VisitSorter, Action<Player>[]>> sort : sortingIcons) {
				if (element.getSymbol() == sort.left()) {
					event.setCancelled(true);
					VisitSorter sortType = sort.right().mid();
					if (gui.getCurrentSorter() == sortType) {
						// already viewing this sorter
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										Sound.Source.PLAYER, 1, 1));
						return;
					}

					// Update GUI state to reflect new sorter
					gui.setCurrentSorter(sortType);
					gui.setRefreshReason(VisitGUI.RefreshReason.SORT_CHANGE);

					int limit = getFilledSlotCount();

					Consumer<List<VisitEntry>> callback = entries -> instance.getScheduler().executeSync(() -> {
						gui.populateVisitEntries(entries, sortType, gui.getCurrentPage());
						gui.refresh();
					});

					if (sortType == VisitSorter.FEATURED) {
						instance.getVisitManager().getFeaturedIslands(limit).thenAccept(callback);
					} else {
						instance.getVisitManager().getTopIslands(sortType.getVisitFunction(), limit, callback);
					}

					ActionManager.trigger(gui.context, sort.right().right());
					return;
				}
			}

			if (element.getSymbol() == filledSlot && element.getUUID() != null) {
				event.setCancelled(true);
				UUID targetUUID = element.getUUID();
				instance.getStorageManager().getCachedUserDataWithFallback(targetUUID, false).thenCompose(optData -> {
					if (optData.isEmpty()) {
						instance.getScheduler().executeSync(() -> {
							final String username = Bukkit.getOfflinePlayer(targetUUID).getName();
							instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(
													AdventureHelper.miniMessageToComponent(username != null ? username
															: instance.getTranslationManager().miniMessageTranslation(
																	MessageConstants.FORMAT_UNKNOWN.build().key())))
											.build()));
							AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											Sound.Source.PLAYER, 1, 1));
						});
						return CompletableFuture.completedFuture(null);
					}

					final UserData ownerData = optData.get();
					final HellblockData hellblockData = ownerData.getHellblockData();

					if (hellblockData.isAbandoned()) {
						instance.getScheduler().executeSync(() -> {
							instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_VISIT_ABANDONED.build()));
							AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											Sound.Source.PLAYER, 1, 1));
						});
						return CompletableFuture.completedFuture(null);
					}

					if (hellblockData.getBannedMembers().contains(player.getUniqueId())
							&& (!(player.isOp() || player.hasPermission("hellblock.admin")
									|| player.hasPermission("hellblock.bypass.interact")))) {
						instance.getScheduler().executeSync(() -> {
							instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
											.arguments(AdventureHelper.miniMessageToComponent(ownerData.getName()))
											.build()));
							AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
									Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
											Sound.Source.PLAYER, 1, 1));
						});
						return CompletableFuture.completedFuture(null);
					}

					return instance.getCoopManager().checkIfVisitorsAreWelcome(player, ownerData.getUUID())
							.thenCompose(status -> {
								if (ownerData.isLocked() || !status) {
									instance.getScheduler().executeSync(() -> {
										instance.getSenderFactory().wrap(player).sendMessage(instance
												.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS.arguments(
														AdventureHelper.miniMessageToComponent(ownerData.getName()))
														.build()));
										AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
												Sound.sound(
														net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
														Sound.Source.PLAYER, 1, 1));
									});
									return CompletableFuture.completedFuture(null);
								}

								return instance.getVisitManager().handleVisit(player, targetUUID)
										.thenRun(() -> ActionManager.trigger(gui.context, filledActions))
										.exceptionally(ex -> {
											instance.getPluginLogger()
													.warn("Error handling visit for " + player.getName(), ex);
											return null;
										});
							});
				}).exceptionally(ex -> {
					instance.getPluginLogger().warn("Failed to handle island visit for " + targetUUID, ex);
					return null;
				});
				return;
			}

			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !gui.hellblockData.hasHellblock() || gui.hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				if (gui.getCurrentSorter() == VisitSorter.FEATURED) {
					instance.getSenderFactory().wrap(player).sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				}
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

			if (element.getSymbol() == emptySlot && element.getUUID() == null
					&& gui.getCurrentSorter() == VisitSorter.FEATURED) {
				event.setCancelled(true);
				int maxSlots = getFilledSlotCount(); // Total allowed
				instance.getVisitManager().getFeaturedIslands(maxSlots).thenAccept(currentFeatured -> {
					if (currentFeatured.size() >= maxSlots) {
						// All slots filled
						instance.getSenderFactory().wrap(player).sendMessage(
								instance.getTranslationManager().render(MessageConstants.MSG_FEATURED_FULL.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										Sound.Source.PLAYER, 1f, 1f));
						return;
					}

					// Proceed to purchase
					instance.getVisitManager().attemptFeaturedSlotPurchase(player, emptyRequirements)
							.thenAccept(success -> {
								if (success) {
									gui.refreshAndRepopulate();
									startFeaturedGUIUpdate(player);
									ActionManager.trigger(gui.context, emptyActions);
								}
							});
				});

				return;
			}
		}

		// Refresh the GUI
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public int getFilledSlotCount() {
		int count = 0;

		if (hasPageLayouts()) {
			for (String[] page : getActiveLayouts()) {
				for (String row : page) {
					for (char c : row.toCharArray()) {
						if (c == filledSlot)
							count++;
					}
				}
			}
		} else {
			for (String row : layout) {
				for (char c : row.toCharArray()) {
					if (c == filledSlot)
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

	private void startFeaturedGUIUpdate(Player player) {
		cancelGUIUpdate(player); // in case there's an old one

		SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
			VisitGUI gui = visitGUICache.get(player.getUniqueId());
			if (gui == null) {
				player.closeInventory();
				return;
			}
			if (!player.isOnline()) {
				cancelGUIUpdate(player);
				return;
			}

			// Only update dynamic lore for FEATURED
			gui.updateFeaturedCountdownLore().exceptionally(ex -> {
				instance.getPluginLogger().warn("Failed to update featured GUI for " + player.getName(), ex);
				return false;
			});

		}, 20L, 20L, LocationUtils.getAnyLocationInstance()); // 1 tick delay, repeat every second

		guiUpdateTasks.put(player.getUniqueId(), task);
	}

	private void cancelGUIUpdate(Player player) {
		SchedulerTask task = guiUpdateTasks.remove(player.getUniqueId());
		if (task != null && !task.isCancelled())
			task.cancel();
	}

	public enum VisitSorter {
		DAILY(VisitData::getDailyVisits), WEEKLY(VisitData::getWeeklyVisits), MONTHLY(VisitData::getMonthlyVisits),
		OVERALL(VisitData::getTotalVisits), FEATURED(VisitData::getFeaturedRanking);

		private final Function<VisitData, Integer> visitFunction;

		VisitSorter(Function<VisitData, Integer> visitFunction) {
			this.visitFunction = visitFunction;
		}

		public Function<VisitData, Integer> getVisitFunction() {
			return visitFunction;
		}
	}
}