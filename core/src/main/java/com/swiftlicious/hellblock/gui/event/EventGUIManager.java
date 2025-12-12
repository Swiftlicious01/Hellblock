package com.swiftlicious.hellblock.gui.event;

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
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class EventGUIManager implements EventGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, EventType, Action<Player>[]>>> eventIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, EventGUI> eventGUICache = new ConcurrentHashMap<>();

	protected char backSlot;

	protected CustomItem backIcon;
	protected CustomItem lockedLevelIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] lockedLevelActions;

	public EventGUIManager(HellblockPlugin plugin) {
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
		this.eventIcons.clear();
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("events.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for events.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("event.gui");
		if (config == null) {
			instance.getPluginLogger().severe("event.gui returned null, please regenerate your events.yml GUI file.");
			return;
		}

		this.layout = config.getStringList("layout", new ArrayList<>()).toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "event.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section lockedLevelSection = config.getSection("locked-level-icon");
		if (lockedLevelSection != null) {
			lockedLevelIcon = new SingleItemParser("locked_level", lockedLevelSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			lockedLevelActions = instance.getActionManager(Player.class)
					.parseActions(lockedLevelSection.getSection("action"));
		}

		Section eventSection = config.getSection("event-icons");
		if (eventSection != null) {
			for (Map.Entry<String, Object> entry : eventSection.getStringRouteMappedValues(false).entrySet()) {
				try {
					if (entry.getValue() instanceof Section innerSection) {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger().severe("Event icon missing symbol in entry: " + entry.getKey());
							continue;
						}
						char symbol = symbolStr.charAt(0);

						String eventStr = innerSection.getString("event-type");
						if (eventStr == null) {
							instance.getPluginLogger().severe("Event icon missing event-type for symbol: " + symbol);
							continue;
						}

						EventType event;
						try {
							event = EventType.valueOf(eventStr.toUpperCase(Locale.ENGLISH));
						} catch (IllegalArgumentException e) {
							instance.getPluginLogger()
									.severe("Invalid event-type: " + eventStr + " for symbol: " + symbol);
							continue;
						}

						eventIcons.add(Tuple.of(symbol, innerSection,
								Tuple.of(
										new SingleItemParser(event.toString().toLowerCase(), innerSection,
												instance.getConfigManager().getItemFormatFunctions()).getItem(),
										event, instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action")))));
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"Failed to load event icon entry: " + entry.getKey() + " due to: " + e.getMessage());
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

	public ItemStack buildLockedLevelIcon(Context<Player> context, EventType type) {
		float requiredLevel = getRequiredLevelForEvent(type);

		// Build the item normally
		Item<ItemStack> item = instance.getItemManager().wrap(lockedLevelIcon.build(context));

		// Replace placeholder in display name and lore
		List<String> updatedLore = new ArrayList<>();
		item.lore().ifPresent(lore -> lore.forEach(line -> updatedLore.add(
				AdventureHelper.miniMessageToJson(line.replace("{required_level}", String.valueOf(requiredLevel))))));

		item.lore(updatedLore);

		item.displayName().ifPresent(name -> item.displayName(
				AdventureHelper.miniMessageToJson(name.replace("{required_level}", String.valueOf(requiredLevel)))));

		return item.loadCopy();
	}

	@Override
	public boolean openEventGUI(Player player, int islandId, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		EventGUI gui = new EventGUI(this, context, islandContext, optionalUserData.get().getHellblockData(), isOwner);
		gui.addElement(new EventDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		eventIcons.forEach(
				event -> gui.addElement(new EventDynamicGUIElement(event.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new EventGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		eventGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof EventGUIHolder))
			return;
		eventGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		eventGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof EventGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		EventGUI gui = eventGUICache.get(player.getUniqueId());
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

		// Ensure this is an Event GUI
		if (!(event.getInventory().getHolder() instanceof EventGUIHolder))
			return;

		EventGUI gui = eventGUICache.get(player.getUniqueId());
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
			EventGUIElement element = gui.getElement(slot);
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

			// Decorative icon
			Pair<CustomItem, Action<Player>[]> decorativeIcon = decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			// Back button
			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(player, gui.islandContext.holder(), gui.isOwner);
				ActionManager.trigger(gui.context, backActions);
				return;
			}

			// Async retrieval of up-to-date Hellblock data
			instance.getStorageManager().getCachedUserDataWithFallback(gui.hellblockData.getOwnerUUID(),
					instance.getConfigManager().lockData()).thenAccept(ownerDataOpt -> {
						if (ownerDataOpt.isEmpty()) {
							instance.getScheduler().executeSync(() -> {
								event.setCancelled(true);
								player.closeInventory();
							}, player.getLocation());
							return;
						}

						UserData ownerData = ownerDataOpt.get();
						HellblockData ownerHellblockData = ownerData.getHellblockData();

						if (ownerHellblockData.isAbandoned()) {
							instance.getScheduler().executeSync(() -> {
								Sender audience = instance.getSenderFactory().wrap(player);
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
										Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
												net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
							}, player.getLocation());
							return;
						}

						for (Tuple<Character, Section, Tuple<CustomItem, EventType, Action<Player>[]>> eventClick : eventIcons) {
							if (element.getSymbol() != eventClick.left())
								continue;

							EventType type = eventClick.right().mid();

							if (!isEventUnlocked(type, ownerHellblockData)) {
								float requiredLevel = getRequiredLevelForEvent(type);
								gui.islandContext.arg(ContextKeys.REQUIRED_LEVEL, requiredLevel);
								instance.getScheduler().executeSync(() -> {
									event.setCancelled(true);
									ActionManager.trigger(gui.context, lockedLevelActions);
								}, player.getLocation());
								break;
							}

							instance.getScheduler().executeSync(() -> {
								event.setCancelled(true);
								ActionManager.trigger(gui.context, eventClick.right().right());
							}, player.getLocation());
							break;
						}
					});
		}

		// Refresh the GUI (1 tick later)
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public boolean isEventUnlocked(EventType type, HellblockData data) {
		float islandLevel = data.getIslandLevel();
		float requiredLevel = switch (type) {
		case WITHER -> instance.getConfigManager().witherEventSettings().levelRequired();
		case INVASION -> instance.getConfigManager().invasionEventSettings().levelRequired();
		case SKYSIEGE -> instance.getConfigManager().skysiegeEventSettings().levelRequired();
		};
		return islandLevel >= requiredLevel;
	}

	public float getRequiredLevelForEvent(EventType type) {
		return switch (type) {
		case WITHER -> instance.getConfigManager().witherEventSettings().levelRequired();
		case INVASION -> instance.getConfigManager().invasionEventSettings().levelRequired();
		case SKYSIEGE -> instance.getConfigManager().skysiegeEventSettings().levelRequired();
		};
	}

	public enum EventType {
		WITHER, INVASION, SKYSIEGE;
	}
}