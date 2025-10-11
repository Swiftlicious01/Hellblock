package com.swiftlicious.hellblock.gui.choice;

import java.util.HashMap;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class IslandChoiceGUIManager implements IslandChoiceGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final ConcurrentMap<UUID, IslandChoiceGUI> islandChoiceGUICache = new ConcurrentHashMap<>();

	protected char defaultSlot;
	protected char classicSlot;
	protected char schematicSlot;

	protected CustomItem defaultIcon;
	protected CustomItem classicIcon;
	protected CustomItem schematicIcon;
	protected Action<Player>[] defaultActions;
	protected Action<Player>[] classicActions;
	protected Action<Player>[] schematicActions;

	public IslandChoiceGUIManager(HellblockPlugin plugin) {
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
		Section config = instance.getConfigManager().getGuiConfig().getSection("island-choice.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "island-choice.title"));

		Section defaultSection = config.getSection("default-icon");
		if (defaultSection != null) {
			defaultSlot = defaultSection.getString("symbol", "D").charAt(0);

			defaultIcon = new SingleItemParser("default", defaultSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			defaultActions = instance.getActionManager(Player.class).parseActions(defaultSection.getSection("action"));
		}

		Section classicSection = config.getSection("classic-icon");
		if (classicSection != null) {
			classicSlot = defaultSection.getString("symbol", "C").charAt(0);

			classicIcon = new SingleItemParser("classic", classicSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			classicActions = instance.getActionManager(Player.class).parseActions(classicSection.getSection("action"));
		}

		Section schematicSection = config.getSection("schematic-icon");
		if (schematicSection != null) {
			schematicSlot = schematicSection.getString("symbol", "S").charAt(0);

			schematicIcon = new SingleItemParser("schematic", schematicSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			schematicActions = instance.getActionManager(Player.class)
					.parseActions(schematicSection.getSection("action"));
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
	 * Open the IslandChoice GUI for a player
	 *
	 * @param player  player
	 * @param isReset is reset or not
	 */
	@Override
	public boolean openIslandChoiceGUI(Player player, boolean isReset) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
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
		if (noIslandOptionsAvailable()) {
			context.clearCustomData();
			context.arg(ContextKeys.HELLBLOCK_GENERATION, true);
			instance.getHellblockHandler().createHellblock(player, IslandOptions.CLASSIC, isReset)
					.thenRun(() -> context.arg(ContextKeys.HELLBLOCK_GENERATION, false));
			ActionManager.trigger(context, classicActions);
			return false;
		}
		if (noDefaultOrClassicChoiceAvailable()) {
			instance.getSchematicGUIManager().openSchematicGUI(context.holder(), isReset);
			ActionManager.trigger(context, schematicActions);
			return false;
		}
		IslandChoiceGUI gui = new IslandChoiceGUI(this, context, optionalUserData.get().getHellblockData(), isReset);
		if (instance.getConfigManager().islandOptions().contains(IslandOptions.DEFAULT)) {
			gui.addElement(new IslandChoiceDynamicGUIElement(defaultSlot, new ItemStack(Material.AIR)));
		}
		if (instance.getConfigManager().islandOptions().contains(IslandOptions.CLASSIC)) {
			gui.addElement(new IslandChoiceDynamicGUIElement(classicSlot, new ItemStack(Material.AIR)));
		}
		if (instance.getConfigManager().islandOptions().contains(IslandOptions.SCHEMATIC)) {
			gui.addElement(new IslandChoiceDynamicGUIElement(schematicSlot, new ItemStack(Material.AIR)));
		}
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new IslandChoiceGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		islandChoiceGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof IslandChoiceGUIHolder))
			return;
		IslandChoiceGUI gui = islandChoiceGUICache.remove(player.getUniqueId());
		if (!gui.hellblockData.hasHellblock() && !instance.getIslandGenerator().isAnimating(player)
				&& !gui.context.arg(ContextKeys.HELLBLOCK_GENERATION)) {
			instance.getScheduler().sync().runLater(() -> openIslandChoiceGUI(player, gui.isReset), 2L,
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
		islandChoiceGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof IslandChoiceGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		IslandChoiceGUI gui = islandChoiceGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a IslandChoiceGUI
		if (!(event.getInventory().getHolder() instanceof IslandChoiceGUIHolder))
			return;

		IslandChoiceGUI gui = islandChoiceGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			IslandChoiceGUIElement element = gui.getElement(slot);
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

			if (gui.isReset && gui.hellblockData.getResetCooldown() > 0) {
				gui.context.arg(ContextKeys.RESET_COOLDOWN, gui.hellblockData.getResetCooldown()).arg(
						ContextKeys.RESET_COOLDOWN_FORMATTED,
						instance.getFormattedCooldown(gui.hellblockData.getResetCooldown()));
				Sender audience = instance.getSenderFactory().wrap(gui.context.holder());
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper
								.miniMessage(instance.getFormattedCooldown(gui.hellblockData.getResetCooldown())))
								.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (instance.getConfigManager().islandOptions().contains(IslandOptions.DEFAULT)
					&& element.getSymbol() == defaultSlot) {
				event.setCancelled(true);
				player.closeInventory();
				gui.context.clearCustomData();
				gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, true);
				instance.getHellblockHandler().createHellblock(gui.context.holder(), IslandOptions.DEFAULT, gui.isReset)
						.thenRun(() -> gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, false));
				ActionManager.trigger(gui.context, defaultActions);
			}

			if (instance.getConfigManager().islandOptions().contains(IslandOptions.CLASSIC)
					&& element.getSymbol() == classicSlot) {
				event.setCancelled(true);
				player.closeInventory();
				gui.context.clearCustomData();
				gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, true);
				instance.getHellblockHandler().createHellblock(gui.context.holder(), IslandOptions.CLASSIC, gui.isReset)
						.thenRun(() -> gui.context.arg(ContextKeys.HELLBLOCK_GENERATION, false));
				ActionManager.trigger(gui.context, classicActions);
			}

			if (instance.getConfigManager().islandOptions().contains(IslandOptions.SCHEMATIC)
					&& element.getSymbol() == schematicSlot) {
				event.setCancelled(true);
				instance.getSchematicGUIManager().openSchematicGUI(gui.context.holder(), gui.isReset);
				ActionManager.trigger(gui.context, schematicActions);
				return;
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	private boolean noIslandOptionsAvailable() {
		return !noDefaultOrClassicChoiceAvailable() && !instance.getSchematicGUIManager().checkForSchematics();
	}

	private boolean noDefaultOrClassicChoiceAvailable() {
		return !instance.getConfigManager().islandOptions().contains(IslandOptions.DEFAULT)
				&& !instance.getConfigManager().islandOptions().contains(IslandOptions.CLASSIC);
	}
}