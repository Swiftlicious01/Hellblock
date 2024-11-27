package com.swiftlicious.hellblock.gui.reset;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

public class ResetConfirmGUIManager implements ResetConfirmGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, CustomItem> decorativeIcons;
	protected final ConcurrentMap<UUID, ResetConfirmGUI> resetConfirmGUICache;

	protected char denySlot;
	protected char confirmSlot;

	protected CustomItem denyIcon;
	protected CustomItem confirmIcon;
	protected Action<Player>[] denyActions;
	protected Action<Player>[] confirmActions;

	public ResetConfirmGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.decorativeIcons = new HashMap<>();
		this.resetConfirmGUICache = new ConcurrentHashMap<>();
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
		Section config = instance.getConfigManager().getMainConfig().getSection("confirm-reset.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "confirm-reset.title"));

		Section denySection = config.getSection("deny-icon");
		if (denySection != null) {
			denySlot = denySection.getString("symbol", "D").charAt(0);

			denyIcon = new SingleItemParser("deny", denySection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			denyActions = instance.getActionManager().parseActions(denySection.getSection("action"));
		}

		Section confirmSection = config.getSection("confirm-icon");
		if (confirmSection != null) {
			confirmSlot = confirmSection.getString("symbol", "C").charAt(0);

			confirmIcon = new SingleItemParser("confirm", confirmSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			confirmActions = instance.getActionManager().parseActions(confirmSection.getSection("action"));
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					decorativeIcons.put(symbol, new SingleItemParser("gui", innerSection,
							instance.getConfigManager().getItemFormatFunctions()).getItem());
				}
			}
		}
	}

	/**
	 * Open the ResetConfirm GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public boolean openResetConfirmGUI(Player player) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		if (optionalUserData.get().getHellblockData().getResetCooldown() > 0) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper.miniMessage(instance
							.getFormattedCooldown(optionalUserData.get().getHellblockData().getResetCooldown())))
							.build()));
			return false;
		}
		Context<Player> context = Context.player(player);
		ResetConfirmGUI gui = new ResetConfirmGUI(this, context, optionalUserData.get().getHellblockData());
		gui.addElement(new ResetConfirmDynamicGUIElement(denySlot, denyIcon.build(context)));
		gui.addElement(new ResetConfirmDynamicGUIElement(confirmSlot, confirmIcon.build(context)));
		for (Map.Entry<Character, CustomItem> entry : decorativeIcons.entrySet()) {
			gui.addElement(new ResetConfirmGUIElement(entry.getKey(), entry.getValue().build(context)));
		}
		gui.build().refresh().show();
		resetConfirmGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof ResetConfirmGUIHolder))
			return;
		resetConfirmGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		resetConfirmGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof ResetConfirmGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		ResetConfirmGUI gui = resetConfirmGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a ResetConfirmGUI
		if (!(event.getInventory().getHolder() instanceof ResetConfirmGUIHolder))
			return;

		ResetConfirmGUI gui = resetConfirmGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			ResetConfirmGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			if (element.getSymbol() == denySlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(player, true);
				ActionManagerInterface.trigger(gui.context, denyActions);
			}

			if (gui.hellblockData.getResetCooldown() > 0) {
				Audience audience = instance.getSenderFactory().getAudience(player);
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(AdventureHelper
								.miniMessage(instance.getFormattedCooldown(gui.hellblockData.getResetCooldown())))
								.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (element.getSymbol() == confirmSlot) {
				event.setCancelled(true);
				instance.getHellblockHandler().resetHellblock(player.getUniqueId(), false)
						.thenRun(() -> player.closeInventory());
				ActionManagerInterface.trigger(gui.context, confirmActions);
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}