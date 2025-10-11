package com.swiftlicious.hellblock.gui.schematic;

import java.util.ArrayList;
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
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.RequirementManager;
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
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>>> schematicIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, SchematicGUI> schematicGUICache = new ConcurrentHashMap<>();

	protected char backSlot;

	protected CustomItem backIcon;
	protected Action<Player>[] backActions;

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
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getGuiConfig().getSection("schematic.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "schematic.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "B").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
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
								new SingleItemParser("schematic", innerSection,
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

	/**
	 * Open the Schematic GUI for a player
	 *
	 * @param player  player
	 * @param isReset is reset or not
	 */
	@Override
	public boolean openSchematicGUI(Player player, boolean isReset) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		if (!checkForSchematics()) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
			return false;
		}
		if (isReset && optionalUserData.get().getHellblockData().getResetCooldown() > 0) {
			Sender audience = instance.getSenderFactory().wrap(player);
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper.miniMessage(instance
							.getFormattedCooldown(optionalUserData.get().getHellblockData().getResetCooldown())))
							.build()));
			return false;
		}
		Context<Player> context = Context.player(player);
		SchematicGUI gui = new SchematicGUI(this, context, optionalUserData.get().getHellblockData(), isReset);
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
		if (!gui.hellblockData.hasHellblock() && !instance.getIslandGenerator().isAnimating(player)
				&& !gui.context.arg(ContextKeys.HELLBLOCK_GENERATION)) {
			instance.getScheduler().sync().runLater(() -> openSchematicGUI(player, gui.isReset), 2L,
					player.getLocation());
		}
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		schematicGUICache.remove(event.getPlayer().getUniqueId());
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

		// Check if the clicked inventory is a SchematicGUI
		if (!(event.getInventory().getHolder() instanceof SchematicGUIHolder))
			return;

		SchematicGUI gui = schematicGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			SchematicGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (gui.context.arg(ContextKeys.HELLBLOCK_GENERATION)) {
				event.setCancelled(true);
				return; // already in progress
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getIslandChoiceGUIManager().openIslandChoiceGUI(gui.context.holder(), gui.isReset);
				ActionManager.trigger(gui.context, backActions);
				return;
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (gui.isReset && gui.hellblockData.getResetCooldown() > 0) {
				gui.context.arg(ContextKeys.RESET_COOLDOWN, gui.hellblockData.getResetCooldown()).arg(
						ContextKeys.RESET_COOLDOWN_FORMATTED,
						instance.getFormattedCooldown(gui.hellblockData.getResetCooldown()));
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper
								.miniMessage(instance.getFormattedCooldown(gui.hellblockData.getResetCooldown())))
								.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			for (Tuple<Character, String, Tuple<CustomItem, Action<Player>[], Requirement<Player>[]>> schematic : schematicIcons) {
				if (element.getSymbol() == schematic.left()
						&& (schematic.mid().endsWith(".schem") || schematic.mid().endsWith(".schematic"))) {
					event.setCancelled(true);
					if (RequirementManager.isSatisfied(gui.context, schematic.right().right())) {
						player.closeInventory();
						gui.context.clearCustomData();
						gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, true);
						instance.getHellblockHandler()
								.createHellblock(gui.context.holder(), IslandOptions.SCHEMATIC, schematic.mid(),
										gui.isReset)
								.thenRun(() -> gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, false));
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
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public boolean checkForSchematics() {
		return instance.getConfigManager().islandOptions().contains(IslandOptions.SCHEMATIC)
				&& !instance.getSchematicManager().schematicFiles.keySet().isEmpty();
	}
}