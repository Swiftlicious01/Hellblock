package com.swiftlicious.hellblock.gui.hellblock;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class HellblockGUIManager implements HellblockGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, CustomItem> decorativeIcons;
	protected final ConcurrentMap<UUID, HellblockGUI> hellblockGUICache;

	protected char teleportSlot;
	protected char challengeSlot;
	protected char levelSlot;
	protected char partySlot;
	protected char lockSlot;
	protected char unlockSlot;
	protected char biomeSlot;
	protected char flagSlot;
	protected char resetSlot;
	protected char resetCooldownSlot;
	protected char biomeCooldownSlot;
	protected char closeSlot;

	protected CustomItem teleportIcon;
	protected CustomItem challengeIcon;
	protected CustomItem levelIcon;
	protected CustomItem partyIcon;
	protected CustomItem lockIcon;
	protected CustomItem unlockIcon;
	protected CustomItem biomeIcon;
	protected CustomItem flagIcon;
	protected CustomItem resetIcon;
	protected CustomItem resetCooldownIcon;
	protected CustomItem biomeCooldownIcon;
	protected CustomItem closeIcon;
	protected Action<Player>[] teleportActions;
	protected Action<Player>[] challengeActions;
	protected Action<Player>[] levelActions;
	protected Action<Player>[] partyActions;
	protected Action<Player>[] lockActions;
	protected Action<Player>[] unlockActions;
	protected Action<Player>[] biomeActions;
	protected Action<Player>[] flagActions;
	protected Action<Player>[] resetActions;
	protected Action<Player>[] resetCooldownActions;
	protected Action<Player>[] biomeCooldownActions;
	protected Action<Player>[] closeActions;

	public HellblockGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.decorativeIcons = new HashMap<>();
		this.hellblockGUICache = new ConcurrentHashMap<>();
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
		Section config = instance.getConfigManager().getMainConfig().getSection("hellblock.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "hellblock.title"));

		Section teleportSection = config.getSection("teleport-icon");
		if (teleportSection != null) {
			teleportSlot = teleportSection.getString("symbol", "T").charAt(0);

			teleportIcon = new SingleItemParser("teleport", teleportSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			teleportActions = instance.getActionManager().parseActions(teleportSection.getSection("action"));
		}

		Section challengeSection = config.getSection("challenge-icon");
		if (challengeSection != null) {
			challengeSlot = challengeSection.getString("symbol", "C").charAt(0);

			challengeIcon = new SingleItemParser("challenge", challengeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			challengeActions = instance.getActionManager().parseActions(challengeSection.getSection("action"));
		}

		Section levelSection = config.getSection("level-icon");
		if (levelSection != null) {
			levelSlot = levelSection.getString("symbol", "E").charAt(0);

			levelIcon = new SingleItemParser("level", levelSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			levelActions = instance.getActionManager().parseActions(levelSection.getSection("action"));
		}

		Section partySection = config.getSection("party-icon");
		if (partySection != null) {
			partySlot = partySection.getString("symbol", "P").charAt(0);

			partyIcon = new SingleItemParser("party", partySection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			partyActions = instance.getActionManager().parseActions(partySection.getSection("action"));
		}

		Section lockSection = config.getSection("lock-icon");
		if (lockSection != null) {
			lockSlot = lockSection.getString("symbol", "L").charAt(0);

			lockIcon = new SingleItemParser("lock", lockSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			lockActions = instance.getActionManager().parseActions(lockSection.getSection("action"));
		}

		Section unlockSection = config.getSection("unlock-icon");
		if (unlockSection != null) {
			unlockSlot = unlockSection.getString("symbol", "L").charAt(0);

			unlockIcon = new SingleItemParser("unlock", unlockSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			unlockActions = instance.getActionManager().parseActions(unlockSection.getSection("action"));
		}

		Section biomeSection = config.getSection("biome-icon");
		if (biomeSection != null) {
			biomeSlot = biomeSection.getString("symbol", "B").charAt(0);

			biomeIcon = new SingleItemParser("biome", biomeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			biomeActions = instance.getActionManager().parseActions(biomeSection.getSection("action"));
		}

		Section flagSection = config.getSection("flag-icon");
		if (flagSection != null) {
			flagSlot = flagSection.getString("symbol", "F").charAt(0);

			flagIcon = new SingleItemParser("flag", flagSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			flagActions = instance.getActionManager().parseActions(flagSection.getSection("action"));
		}

		Section resetSection = config.getSection("reset-icon");
		if (resetSection != null) {
			resetSlot = resetSection.getString("symbol", "R").charAt(0);

			resetIcon = new SingleItemParser("reset", resetSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			resetActions = instance.getActionManager().parseActions(resetSection.getSection("action"));
		}

		Section resetCooldownSection = config.getSection("reset-cooldown-icon");
		if (resetCooldownSection != null) {
			resetCooldownSlot = resetCooldownSection.getString("symbol", "!").charAt(0);

			resetCooldownIcon = new SingleItemParser("resetcooldown", resetCooldownSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			resetCooldownActions = instance.getActionManager().parseActions(resetCooldownSection.getSection("action"));
		}

		Section biomeCooldownSection = config.getSection("biome-cooldown-icon");
		if (biomeCooldownSection != null) {
			biomeCooldownSlot = biomeCooldownSection.getString("symbol", "*").charAt(0);

			biomeCooldownIcon = new SingleItemParser("biomecooldown", biomeCooldownSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			biomeCooldownActions = instance.getActionManager().parseActions(biomeCooldownSection.getSection("action"));
		}

		Section closeSection = config.getSection("close-icon");
		if (closeSection != null) {
			closeSlot = closeSection.getString("symbol", "X").charAt(0);

			closeIcon = new SingleItemParser("close", closeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			closeActions = instance.getActionManager().parseActions(closeSection.getSection("action"));
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
	 * Open the hellblock GUI for a player
	 *
	 * @param player  player
	 * @param isOwner is owner of hellblock
	 */
	@Override
	public boolean openHellblockGUI(Player player, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		HellblockGUI gui = new HellblockGUI(this, context, optionalUserData.get().getHellblockData());
		gui.addElement(new HellblockDynamicGUIElement(teleportSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(challengeSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(levelSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(partySlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(closeSlot, new ItemStack(Material.AIR)));
		if (isOwner) {
			if (optionalUserData.get().getHellblockData().getBiomeCooldown() <= 0)
				gui.addElement(new HellblockDynamicGUIElement(biomeSlot, new ItemStack(Material.AIR)));
			else
				gui.addElement(new HellblockDynamicGUIElement(biomeCooldownSlot, new ItemStack(Material.AIR)));
			if (optionalUserData.get().getHellblockData().isLocked())
				gui.addElement(new HellblockDynamicGUIElement(unlockSlot, new ItemStack(Material.AIR)));
			else
				gui.addElement(new HellblockDynamicGUIElement(lockSlot, new ItemStack(Material.AIR)));
			gui.addElement(new HellblockDynamicGUIElement(flagSlot, new ItemStack(Material.AIR)));
			if (optionalUserData.get().getHellblockData().getResetCooldown() <= 0)
				gui.addElement(new HellblockDynamicGUIElement(resetSlot, new ItemStack(Material.AIR)));
			else
				gui.addElement(new HellblockDynamicGUIElement(resetCooldownSlot, new ItemStack(Material.AIR)));
		}
		for (Map.Entry<Character, CustomItem> entry : decorativeIcons.entrySet()) {
			gui.addElement(new HellblockGUIElement(entry.getKey(), entry.getValue().build(context)));
		}
		gui.build().refresh().show();
		hellblockGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof HellblockGUIHolder))
			return;
		hellblockGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		hellblockGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof HellblockGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		HellblockGUI gui = hellblockGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a HellblockGUI
		if (!(event.getInventory().getHolder() instanceof HellblockGUIHolder))
			return;

		HellblockGUI gui = hellblockGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		HellblockData hellblockData = gui.hellblockData;
		Optional<UserData> userData = instance.getStorageManager().getOnlineUser(gui.context.holder().getUniqueId());

		if (userData.isEmpty() || !hellblockData.hasHellblock() || hellblockData.getOwnerUUID() == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			HellblockGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			if (element.getSymbol() == closeSlot) {
				event.setCancelled(true);
				player.closeInventory();
				ActionManagerInterface.trigger(gui.context, closeActions);
				return;
			}

			if (element.getSymbol() == resetCooldownSlot) {
				event.setCancelled(true);
				ActionManagerInterface.trigger(gui.context, resetCooldownActions);
				return;
			}

			if (element.getSymbol() == biomeCooldownSlot) {
				event.setCancelled(true);
				ActionManagerInterface.trigger(gui.context, biomeCooldownActions);
				return;
			}

			if (element.getSymbol() == levelSlot) {
				event.setCancelled(true);
				ActionManagerInterface.trigger(gui.context, levelActions);
				return;
			}

			if (element.getSymbol() == teleportSlot) {
				event.setCancelled(true);
				instance.getStorageManager()
						.getOfflineUserData(hellblockData.getOwnerUUID(), instance.getConfigManager().lockData())
						.thenAccept(result -> {
							if (result.isEmpty()) {
								event.setCancelled(true);
								player.closeInventory();
								return;
							}
							UserData ownerData = result.get();
							if (ownerData.getHellblockData().isAbandoned()) {
								instance.getSenderFactory().wrap(player).sendMessage(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.asComponent());
								instance.getAdventureManager().playSound(player,
										net.kyori.adventure.sound.Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
								return;
							}
							if (ownerData.getHellblockData().getHomeLocation() != null) {
								instance.getCoopManager().makeHomeLocationSafe(ownerData, userData.get())
										.thenRun(() -> {
											ActionManagerInterface.trigger(gui.context, teleportActions);
										});
							} else {
								instance.getSenderFactory().wrap(player).sendMessage(instance.getAdventureManager()
										.getComponentFromMiniMessage("<red>Error teleporting you to your hellblock!"));
								instance.getAdventureManager().playSound(player,
										net.kyori.adventure.sound.Sound.Source.PLAYER,
										net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"), 1, 1);
								throw new NullPointerException(
										"Hellblock home location returned null, please report this to the developer.");
							}
						});
				return;
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}