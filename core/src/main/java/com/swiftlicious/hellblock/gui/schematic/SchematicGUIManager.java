package com.swiftlicious.hellblock.gui.schematic;

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
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class SchematicGUIManager implements SchematicGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected final Map<Integer, TextValue<Player>> pageTitles = new HashMap<>();
	protected String[] layout;
	protected final Map<Integer, String[]> pageLayouts = new HashMap<>();
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>>> schematicIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, SchematicGUI> schematicGUICache = new ConcurrentHashMap<>();

	protected char backSlot;

	protected CustomItem backIcon;
	protected Action<Player>[] backActions;

	protected char leftSlot;
	protected char rightSlot;

	protected CustomItem leftIcon;
	protected Action<Player>[] leftActions;
	protected CustomItem rightIcon;
	protected Action<Player>[] rightActions;

	public SchematicGUIManager(HellblockPlugin plugin) {
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
		this.schematicIcons.clear();
		this.pageLayouts.clear();
		this.pageTitles.clear();
	}

	private void loadConfig() {
		Section configFile = instance.getConfigManager().getGUIConfig("schematics.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for schematics.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("schematic.gui");
		if (config == null) {
			instance.getPluginLogger()
					.severe("schematic.gui returned null, please regenerate your schematics.yml GUI file.");
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

		this.title = TextValue.auto(config.getString("title", "schematic.title"));

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

		Section schematicSection = config.getSection("schematic-icons");
		if (schematicSection != null) {
			for (Map.Entry<String, Object> entry : schematicSection.getStringRouteMappedValues(false).entrySet()) {
				try {
					if (entry.getValue() instanceof Section innerSection) {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Schematic icon missing symbol in entry: " + entry.getKey());
							continue;
						}
						char symbol = symbolStr.charAt(0);

						String schematic = innerSection.getString("schematic");
						if (schematic == null || schematic.isEmpty()) {
							instance.getPluginLogger()
									.severe("Schematic icon missing schematic name for symbol: " + symbol);
							continue;
						}

						schematicIcons.add(Tuple.of(symbol, schematic, Tuple.of(
								new SingleItemParser(schematic.toLowerCase() + "_schematic", innerSection,
										instance.getConfigManager().getItemFormatFunctions()).getItem(),
								instance.getActionManager(Player.class).parseActions(innerSection.getSection("action")),
								instance.getRequirementManager(Player.class)
										.parseRequirements(innerSection.getSection("requirement"), true))));
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"Failed to load schematic icon entry: " + entry.getKey() + " due to: " + e.getMessage());
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
	public boolean openSchematicGUI(Player player, boolean isReset) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}

		UserData userData = optionalUserData.get();
		HellblockData hellblockData = userData.getHellblockData();

		if (!checkForSchematics()) {
			instance.debug("No island schematics available → denying schematic GUI open action.");
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			return false;
		}

		if (isReset && hellblockData.getResetCooldown() > 0) {
			instance.debug("Reset denied: cooldown = " + hellblockData.getResetCooldown());
			Sender audience = instance.getSenderFactory().wrap(player);
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper.miniMessageToComponent(
							instance.getCooldownManager().getFormattedCooldown(hellblockData.getResetCooldown())))
							.build()));
			return false;
		}

		Context<Player> context = Context.player(player);

		if (Boolean.TRUE.equals(context.arg(ContextKeys.HELLBLOCK_GENERATION))) {
			instance.debug("Island generation already in progress for player " + player.getName() + ".");
			return false;
		}

		SchematicGUI gui = new SchematicGUI(this, context, userData, hellblockData, isReset, leftIcon, leftActions,
				rightIcon, rightActions);
		gui.addElement(new SchematicDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		schematicIcons.forEach(schematic -> gui
				.addElement(new SchematicDynamicGUIElement(schematic.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new SchematicGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		schematicGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof SchematicGUIHolder))
			return;
		SchematicGUI gui = schematicGUICache.remove(player.getUniqueId());
		if (gui == null)
			return;

		if (Boolean.TRUE.equals(gui.context.arg(ContextKeys.HELLBLOCK_GUI_SWITCHING))) {
			gui.context.remove(ContextKeys.HELLBLOCK_GUI_SWITCHING);
			return;
		}

		if (!gui.hellblockData.hasHellblock() && !instance.getIslandGenerator().isGenerating(player.getUniqueId())
				&& !Boolean.TRUE.equals(gui.context.arg(ContextKeys.HELLBLOCK_GENERATION))) {
			instance.getScheduler().sync().runLater(() -> {
				if (!player.isOnline())
					return;
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				openSchematicGUI(player, gui.isReset);
			}, 2L, player.getLocation());
		}
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		SchematicGUI gui = schematicGUICache.remove(event.getPlayer().getUniqueId());
		if (gui == null)
			return;
		if (Boolean.TRUE.equals(gui.context.arg(ContextKeys.HELLBLOCK_GUI_SWITCHING))) {
			gui.context.remove(ContextKeys.HELLBLOCK_GUI_SWITCHING);
		}
		if (Boolean.TRUE.equals(gui.context.arg(ContextKeys.HELLBLOCK_GENERATION))) {
			gui.context.remove(ContextKeys.HELLBLOCK_GENERATION);
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
		if (!(inventory.getHolder() instanceof SchematicGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		SchematicGUI gui = schematicGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a SchematicGUI
		if (!(event.getInventory().getHolder() instanceof SchematicGUIHolder))
			return;

		SchematicGUI gui = schematicGUICache.get(player.getUniqueId());
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
			SchematicGUIElement element = gui.getElement(slot);
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

			if (Boolean.TRUE.equals(gui.context.arg(ContextKeys.HELLBLOCK_GENERATION))) {
				event.setCancelled(true);
				return; // already in progress
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

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				gui.context.arg(ContextKeys.HELLBLOCK_GUI_SWITCHING, true);
				boolean opened = instance.getIslandChoiceGUIManager().openIslandChoiceGUI(gui.context.holder(),
						gui.isReset);
				if (!opened) {
					gui.context.remove(ContextKeys.HELLBLOCK_GUI_SWITCHING);
				}
				ActionManager.trigger(gui.context, backActions);
				return;
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (gui.isReset && gui.hellblockData.getResetCooldown() > 0) {
				gui.context.arg(ContextKeys.RESET_COOLDOWN, gui.hellblockData.getResetCooldown()).arg(
						ContextKeys.RESET_COOLDOWN_FORMATTED,
						instance.getCooldownManager().getFormattedCooldown(gui.hellblockData.getResetCooldown()));
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN
								.arguments(AdventureHelper.miniMessageToComponent(instance.getCooldownManager()
										.getFormattedCooldown(gui.hellblockData.getResetCooldown())))
								.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			for (Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>> schematic : schematicIcons) {
				if (element.getSymbol() == schematic.left()) {
					event.setCancelled(true);
					if (instance.getSchematicManager().schematicExists(schematic.mid())) {
						triggerDecorativeFallbackActions(schematic.left(), gui.context);
						return;
					}
					if (RequirementManager.isSatisfied(gui.context, schematic.right().right())) {
						gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, true);
						player.closeInventory();
						instance.getHellblockHandler()
								.createHellblock(gui.userData, IslandOptions.SCHEMATIC, schematic.mid(), gui.isReset)
								.thenRun(() -> gui.context.remove(ContextKeys.HELLBLOCK_GENERATION));
						ActionManager.trigger(gui.context, schematic.right().mid());
					} else {
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_NO_SCHEMATIC_PERMISSION.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					}
					break;
				}
			}
		}

		// Refresh the GUI
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000)) {
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public boolean isGeneratingSchematic(UUID playerId) {
		SchematicGUI gui = schematicGUICache.get(playerId);
		if (gui == null) {
			return false;
		}

		boolean generationContext = gui.context.arg(ContextKeys.HELLBLOCK_GENERATION);
		return Boolean.TRUE.equals(generationContext);
	}

	public boolean checkForSchematics() {
		return instance.getConfigManager().islandOptions().contains(IslandOptions.SCHEMATIC)
				&& !instance.getSchematicManager().schematicFiles.keySet().isEmpty();
	}

	private void triggerDecorativeFallbackActions(char symbol, Context<Player> context) {
		// 1. Try symbol-specific decorative action
		Pair<CustomItem, Action<Player>[]> mapped = decorativeIcons.get(symbol);
		if (mapped != null && mapped.right() != null) {
			ActionManager.trigger(context, mapped.right());
			return;
		}

		// 2. Fallback: use the first decorative icon that has actions
		for (Pair<CustomItem, Action<Player>[]> pair : decorativeIcons.values()) {
			if (pair != null && pair.right() != null && pair.right().length > 0) {
				ActionManager.trigger(context, pair.right());
				return;
			}
		}

		// 3. Final fallback: no actions available, maybe play a sound?
		Player player = context.holder();
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
	}
}