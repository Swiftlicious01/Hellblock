package com.swiftlicious.hellblock.gui.upgrade;

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
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeCostProcessor;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;
import com.swiftlicious.hellblock.utils.extras.Tuple;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

public class UpgradeGUIManager implements UpgradeGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected boolean highlightMaxUpgrades;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final List<Tuple<Character, Section, Tuple<CustomItem, IslandUpgradeType, Action<Player>[]>>> upgradeIcons = new ArrayList<>();
	protected final ConcurrentMap<UUID, UpgradeGUI> upgradeGUICache = new ConcurrentHashMap<>();

	protected char backSlot;

	protected CustomItem backIcon;
	protected Action<Player>[] backActions;

	public UpgradeGUIManager(HellblockPlugin plugin) {
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
		this.upgradeIcons.clear();
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getGuiConfig().getSection("upgrade.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "upgrade.title"));
		this.highlightMaxUpgrades = config.getBoolean("hightlight-max-tier-upgrades", true);

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "B").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		Section upgradeSection = config.getSection("upgrade-icons");
		if (upgradeSection != null) {
			for (Map.Entry<String, Object> entry : upgradeSection.getStringRouteMappedValues(false).entrySet()) {
				try {
					if (entry.getValue() instanceof Section innerSection) {
						String symbolStr = innerSection.getString("symbol");
						if (symbolStr == null || symbolStr.isEmpty()) {
							instance.getPluginLogger()
									.severe("Upgrade icon missing symbol in entry: " + entry.getKey());
							continue;
						}
						char symbol = symbolStr.charAt(0);

						String upgradeStr = innerSection.getString("upgrade-type");
						if (upgradeStr == null) {
							instance.getPluginLogger()
									.severe("Upgrade icon missing upgrade-type for symbol: " + symbol);
							continue;
						}

						IslandUpgradeType upgrade;
						try {
							upgrade = IslandUpgradeType.valueOf(upgradeStr.toUpperCase(Locale.ENGLISH));
						} catch (IllegalArgumentException e) {
							instance.getPluginLogger()
									.severe("Invalid upgrade-type: " + upgradeStr + " for symbol: " + symbol);
							continue;
						}

						upgradeIcons.add(Tuple.of(symbol, innerSection,
								Tuple.of(
										new SingleItemParser("upgrade", innerSection,
												instance.getConfigManager().getItemFormatFunctions()).getItem(),
										upgrade, instance.getActionManager(Player.class)
												.parseActions(innerSection.getSection("action")))));
					}
				} catch (Exception e) {
					instance.getPluginLogger().severe(
							"Failed to load upgrade icon entry: " + entry.getKey() + " due to: " + e.getMessage());
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
	 * Open the Upgrade GUI for a player
	 *
	 * @param player  player
	 * @param isOwner is owner of hellblock
	 */
	@Override
	public boolean openUpgradeGUI(Player player, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		UpgradeGUI gui = new UpgradeGUI(this, context, optionalUserData.get().getHellblockData(), isOwner);
		gui.addElement(new UpgradeDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		upgradeIcons.forEach(
				flag -> gui.addElement(new UpgradeDynamicGUIElement(flag.left(), new ItemStack(Material.AIR))));
		decorativeIcons.entrySet().forEach(
				entry -> gui.addElement(new UpgradeGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		upgradeGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof UpgradeGUIHolder))
			return;
		upgradeGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		upgradeGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof UpgradeGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		UpgradeGUI gui = upgradeGUICache.get(player.getUniqueId());
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
		if (!(event.getInventory().getHolder() instanceof UpgradeGUIHolder))
			return;

		UpgradeGUI gui = upgradeGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			UpgradeGUIElement element = gui.getElement(slot);
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

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(player, gui.isOwner);
				ActionManager.trigger(gui.context, backActions);
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

			for (Tuple<Character, Section, Tuple<CustomItem, IslandUpgradeType, Action<Player>[]>> upgrade : upgradeIcons) {
				if (element.getSymbol() == upgrade.left()) {
					event.setCancelled(true);
					IslandUpgradeType upgradeType = upgrade.right().mid();
					if (!gui.hellblockData.canUpgrade(upgradeType)) {
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MAX_TIER
										.arguments(Component.text(
												StringUtils.toProperCase(upgradeType.toString().replace("_", " "))))
										.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						return;
					}

					final UpgradeCostProcessor payment = new UpgradeCostProcessor(player);
					if (!payment.canAfford(gui.hellblockData.getNextCosts(upgradeType))) {
						audience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CANNOT_AFFORD.build()));
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
						return;
					}
					if (instance.getUpgradeManager().attemptPurchase(gui.hellblockData, player, upgradeType)) {
						gui.context
								.arg(ContextKeys.HELLBLOCK_RANGE_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
								.arg(ContextKeys.HELLBLOCK_HOPPER_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
								.arg(ContextKeys.HELLBLOCK_PARTY_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
								.arg(ContextKeys.HELLBLOCK_GENERATOR_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
								.arg(ContextKeys.HELLBLOCK_BARTERING_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
								.arg(ContextKeys.HELLBLOCK_CROP_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
								.arg(ContextKeys.HELLBLOCK_MOB_TIER,
										gui.hellblockData.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE));
					}
					ActionManager.trigger(gui.context, upgrade.right().right());
					break;
				}
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}