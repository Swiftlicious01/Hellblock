package com.swiftlicious.hellblock.gui.visit;

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
import java.util.function.Consumer;
import java.util.function.Function;

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
	protected String[] layout;
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
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getGuiConfig().getSection("visit.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "visit.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "B").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		filledSection = config.getSection("filled-icon");
		if (filledSection != null) {
			filledSlot = filledSection.getString("symbol", "S").charAt(0);

			filledIcon = new SingleItemParser("filled", filledSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			filledActions = instance.getActionManager(Player.class).parseActions(filledSection.getSection("action"));
		}

		emptySection = config.getSection("empty-icon");
		if (emptySection != null) {
			emptySlot = emptySection.getString("symbol", "S").charAt(0);

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
										new SingleItemParser("sorting", innerSection,
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

	/**
	 * Open the Visit GUI for a player
	 *
	 * @param player       player
	 * @param visitSorter  the current sorter to open on.
	 * @param showBackIcon show back menu icon or not
	 */
	@Override
	public boolean openVisitGUI(Player player, VisitSorter visitSorter, boolean showBackIcon) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		boolean checkFilledSlots = Objects.equals(filledSlot, emptySlot);
		if (!checkFilledSlots) {
			instance.getPluginLogger().warn(
					"Filled and empty slots don't equal the same variable. Please update them to the same symbols.");
			return false;
		}
		Context<Player> context = Context.player(player);
		VisitGUI gui = new VisitGUI(this, visitSorter, context, optionalUserData.get().getHellblockData(),
				showBackIcon);
		if (gui.showBackIcon) {
			gui.addElement(new VisitDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		}
		gui.addElement(new VisitDynamicGUIElement(filledSlot, new ItemStack(Material.AIR)));
		gui.addElement(new VisitDynamicGUIElement(emptySlot, new ItemStack(Material.AIR)));
		sortingIcons
				.forEach(flag -> gui.addElement(new VisitDynamicGUIElement(flag.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new VisitGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refreshAndRepopulate();
		if (visitSorter == VisitSorter.FEATURED) {
			startFeaturedGuiUpdate(player);
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
		cancelGuiUpdate(player);
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		visitGUICache.remove(event.getPlayer().getUniqueId());
		cancelGuiUpdate(event.getPlayer());
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

		// Check if the clicked inventory is a VisitGUI
		if (!(event.getInventory().getHolder() instanceof VisitGUIHolder))
			return;

		VisitGUI gui = visitGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			VisitGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (gui.showBackIcon && element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(player,
						gui.context.holder().getUniqueId().equals(gui.hellblockData.getOwnerUUID()));
				ActionManager.trigger(gui.context, backActions);
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

					int limit = getFilledSlotCount();

					Consumer<List<VisitEntry>> callback = entries -> instance.getScheduler().executeSync(() -> {
						gui.populateVisitEntries(entries, sortType);
						gui.refresh();
					});

					if (sortType == VisitSorter.FEATURED) {
						instance.getVisitManager().getFeaturedIslands(limit).thenAccept(callback);
					} else {
						instance.getVisitManager().getTopIslands(sortType.getVisitFunction(), limit, callback);
					}

					ActionManager.trigger(gui.context, sort.right().right());
					break;
				}
			}

			if (element.getSymbol() == filledSlot && element.getUUID() != null) {
				event.setCancelled(true);
				UUID targetUUID = element.getUUID();
				instance.getStorageManager().getOfflineUserData(targetUUID, instance.getConfigManager().lockData())
						.thenAccept(ownerOpt -> {
							if (ownerOpt.isEmpty()) {
								final String username = Bukkit.getOfflinePlayer(targetUUID).getName();
								instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD.arguments(
												AdventureHelper.miniMessage(username != null ? username : "???"))
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

							if (ownerData.getBanned().contains(player.getUniqueId())) {
								instance.getSenderFactory().wrap(player)
										.sendMessage(instance.getTranslationManager()
												.render(MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY
														.arguments(AdventureHelper.miniMessage(ownerUser.getName()))
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
															.arguments(AdventureHelper.miniMessage(ownerUser.getName()))
															.build()));
											AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
													Sound.sound(
															net.kyori.adventure.key.Key
																	.key("minecraft:entity.villager.no"),
															Sound.Source.PLAYER, 1, 1));
											return;
										}

										instance.getVisitManager().handleVisit(player, targetUUID);
										ActionManager.trigger(gui.context, filledActions);
									});
						});
				return;
			}

			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !gui.hellblockData.hasHellblock() || gui.hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
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
				instance.getVisitManager().attemptFeaturedSlotPurchase(player, emptyRequirements)
						.thenAccept(success -> {
							if (success) {
								gui.refreshAndRepopulate();
								startFeaturedGuiUpdate(player);
								ActionManager.trigger(gui.context, emptyActions);
							}
						});
				return;
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public int getFilledSlotCount() {
		int count = 0;
		for (String row : layout) {
			for (char c : row.toCharArray()) {
				if (c == filledSlot)
					count++;
			}
		}
		return count;
	}

	private void startFeaturedGuiUpdate(Player player) {
		cancelGuiUpdate(player); // in case there's an old one

		SchedulerTask task = instance.getScheduler().sync().runRepeating(() -> {
			VisitGUI gui = visitGUICache.get(player.getUniqueId());
			if (gui == null) {
				player.closeInventory();
				return;
			}
			if (!player.isOnline()) {
				cancelGuiUpdate(player);
				return;
			}

			// Only update dynamic lore for FEATURED
			gui.updateFeaturedCountdownLore();

		}, 20L, 20L, LocationUtils.getAnyLocationInstance()); // 1 tick delay, repeat every second

		guiUpdateTasks.put(player.getUniqueId(), task);
	}

	private void cancelGuiUpdate(Player player) {
		SchedulerTask task = guiUpdateTasks.remove(player.getUniqueId());
		if (task != null && !task.isCancelled())
			task.cancel();
	}

	public enum VisitSorter {
		DAILY(VisitData::getDailyVisits), WEEKLY(VisitData::getWeeklyVisits), MONTHLY(VisitData::getMonthlyVisits),
		OVERALL(VisitData::getTotalVisits), FEATURED(data -> data.getFeaturedRanking());

		private final Function<VisitData, Integer> visitFunction;

		VisitSorter(Function<VisitData, Integer> visitFunction) {
			this.visitFunction = visitFunction;
		}

		public Function<VisitData, Integer> getVisitFunction() {
			return visitFunction;
		}
	}
}