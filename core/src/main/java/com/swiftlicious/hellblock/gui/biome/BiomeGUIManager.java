package com.swiftlicious.hellblock.gui.biome;

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
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;

public class BiomeGUIManager implements BiomeGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected boolean highlightSelection;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons;
	protected final ConcurrentMap<UUID, BiomeGUI> biomeGUICache;

	protected char backSlot;
	protected char soulSandValleySlot;
	protected char warpedForestSlot;
	protected char crimsonForestSlot;
	protected char netherWastesSlot;
	protected char basaltDeltasSlot;

	protected List<String> soulSandValleySelectedLore;
	protected List<String> soulSandValleyUnSelectedLore;
	protected List<String> warpedForestSelectedLore;
	protected List<String> warpedForestUnSelectedLore;
	protected List<String> crimsonForestSelectedLore;
	protected List<String> crimsonForestUnSelectedLore;
	protected List<String> netherWastesSelectedLore;
	protected List<String> netherWastesUnSelectedLore;
	protected List<String> basaltDeltasSelectedLore;
	protected List<String> basaltDeltasUnSelectedLore;

	protected CustomItem backIcon;
	protected CustomItem soulSandValleyIcon;
	protected CustomItem warpedForestIcon;
	protected CustomItem crimsonForestIcon;
	protected CustomItem netherWastesIcon;
	protected CustomItem basaltDeltasIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] soulSandValleyActions;
	protected Action<Player>[] warpedForestActions;
	protected Action<Player>[] crimsonForestActions;
	protected Action<Player>[] netherWastesActions;
	protected Action<Player>[] basaltDeltasActions;

	public BiomeGUIManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.decorativeIcons = new HashMap<>();
		this.biomeGUICache = new ConcurrentHashMap<>();
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
		Section config = instance.getConfigManager().getMainConfig().getSection("biome.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "biome.title"));
		this.highlightSelection = config.getBoolean("highlight-selected-biome", true);

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager().parseActions(backSection.getSection("action"));
		}

		Section soulSandValleySection = config.getSection("soul-sand-valley-icon");
		if (soulSandValleySection != null) {
			soulSandValleySlot = soulSandValleySection.getString("symbol", "S").charAt(0);
			soulSandValleySelectedLore = soulSandValleySection.getStringList("display.selected-lore");
			soulSandValleyUnSelectedLore = soulSandValleySection.getStringList("display.unselected-lore");

			soulSandValleyIcon = new SingleItemParser("soulsandvalley", soulSandValleySection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			soulSandValleyActions = instance.getActionManager()
					.parseActions(soulSandValleySection.getSection("action"));
		}

		Section warpedForestSection = config.getSection("warped-forest-icon");
		if (warpedForestSection != null) {
			warpedForestSlot = warpedForestSection.getString("symbol", "W").charAt(0);
			warpedForestSelectedLore = warpedForestSection.getStringList("display.selected-lore");
			warpedForestUnSelectedLore = warpedForestSection.getStringList("display.unselected-lore");

			warpedForestIcon = new SingleItemParser("warpedforest", warpedForestSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			warpedForestActions = instance.getActionManager().parseActions(warpedForestSection.getSection("action"));
		}

		Section crimsonForestSection = config.getSection("crimson-forest-icon");
		if (crimsonForestSection != null) {
			crimsonForestSlot = crimsonForestSection.getString("symbol", "C").charAt(0);
			crimsonForestSelectedLore = crimsonForestSection.getStringList("display.selected-lore");
			crimsonForestUnSelectedLore = crimsonForestSection.getStringList("display.unselected-lore");

			crimsonForestIcon = new SingleItemParser("crimsonforest", crimsonForestSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			crimsonForestActions = instance.getActionManager().parseActions(crimsonForestSection.getSection("action"));
		}

		Section netherWastesSection = config.getSection("nether-wastes-icon");
		if (netherWastesSection != null) {
			netherWastesSlot = netherWastesSection.getString("symbol", "N").charAt(0);
			netherWastesSelectedLore = netherWastesSection.getStringList("display.selected-lore");
			netherWastesUnSelectedLore = netherWastesSection.getStringList("display.unselected-lore");

			netherWastesIcon = new SingleItemParser("netherwastes", netherWastesSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			netherWastesActions = instance.getActionManager().parseActions(netherWastesSection.getSection("action"));
		}

		Section basaltDeltasSection = config.getSection("basalt-deltas-icon");
		if (basaltDeltasSection != null) {
			basaltDeltasSlot = basaltDeltasSection.getString("symbol", "B").charAt(0);
			basaltDeltasSelectedLore = basaltDeltasSection.getStringList("display.selected-lore");
			basaltDeltasUnSelectedLore = basaltDeltasSection.getStringList("display.unselected-lore");

			basaltDeltasIcon = new SingleItemParser("basaltdeltas", basaltDeltasSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			basaltDeltasActions = instance.getActionManager().parseActions(basaltDeltasSection.getSection("action"));
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					decorativeIcons.put(symbol,
							Pair.of(new SingleItemParser("gui", innerSection,
									instance.getConfigManager().getItemFormatFunctions()).getItem(),
									instance.getActionManager().parseActions(innerSection.getSection("action"))));
				}
			}
		}
	}

	/**
	 * Open the Biome GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public boolean openBiomeGUI(Player player) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		if (optionalUserData.get().getHellblockData().getBiomeCooldown() > 0) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN.arguments(AdventureHelper.miniMessage(instance
							.getFormattedCooldown(optionalUserData.get().getHellblockData().getBiomeCooldown())))
							.build()));
			return false;
		}
		Context<Player> context = Context.player(player);
		BiomeGUI gui = new BiomeGUI(this, context, optionalUserData.get().getHellblockData());
		gui.addElement(new BiomeDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new BiomeDynamicGUIElement(soulSandValleySlot, new ItemStack(Material.AIR)));
		gui.addElement(new BiomeDynamicGUIElement(warpedForestSlot, new ItemStack(Material.AIR)));
		gui.addElement(new BiomeDynamicGUIElement(crimsonForestSlot, new ItemStack(Material.AIR)));
		gui.addElement(new BiomeDynamicGUIElement(netherWastesSlot, new ItemStack(Material.AIR)));
		gui.addElement(new BiomeDynamicGUIElement(basaltDeltasSlot, new ItemStack(Material.AIR)));
		for (Map.Entry<Character, Pair<CustomItem, Action<Player>[]>> entry : decorativeIcons.entrySet()) {
			gui.addElement(new BiomeGUIElement(entry.getKey(), entry.getValue().left().build(context)));
		}
		gui.build().refresh().show();
		biomeGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof BiomeGUIHolder))
			return;
		biomeGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		biomeGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof BiomeGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		BiomeGUI gui = biomeGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a BiomeGUI
		if (!(event.getInventory().getHolder() instanceof BiomeGUIHolder))
			return;

		BiomeGUI gui = biomeGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			BiomeGUIElement element = gui.getElement(slot);
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
				ActionManagerInterface.trigger(gui.context, decorativeIcon.right());
				return;
			}

			if (element.getSymbol() == backSlot) {
				event.setCancelled(true);
				instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(),
						gui.context.holder().getUniqueId().equals(gui.hellblockData.getOwnerUUID()));
				ActionManagerInterface.trigger(gui.context, backActions);
				return;
			}

			Audience audience = instance.getSenderFactory().getAudience(gui.context.holder());

			if (!gui.hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (gui.hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (gui.hellblockData.getBiomeCooldown() > 0) {
				gui.context.arg(ContextKeys.BIOME_COOLDOWN, gui.hellblockData.getBiomeCooldown()).arg(
						ContextKeys.BIOME_COOLDOWN_FORMATTED,
						instance.getFormattedCooldown(gui.hellblockData.getBiomeCooldown()));
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_BIOME_ON_COOLDOWN.arguments(AdventureHelper
								.miniMessage(instance.getFormattedCooldown(gui.hellblockData.getBiomeCooldown())))
								.build()));
				audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
						net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}
			
			HellBiome biome = gui.hellblockData.getBiome();

			if (element.getSymbol() == soulSandValleySlot) {
				event.setCancelled(true);
				if (biome == HellBiome.SOUL_SAND_VALLEY) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					return;
				}
				instance.getBiomeHandler().changeHellblockBiome(userData.get(), HellBiome.SOUL_SAND_VALLEY);
				gui.context.arg(ContextKeys.HELLBLOCK_BIOME, gui.hellblockData.getBiome());
				ActionManagerInterface.trigger(gui.context, soulSandValleyActions);
			}

			if (element.getSymbol() == warpedForestSlot) {
				event.setCancelled(true);
				if (biome == HellBiome.WARPED_FOREST) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					return;
				}
				instance.getBiomeHandler().changeHellblockBiome(userData.get(), HellBiome.WARPED_FOREST);
				gui.context.arg(ContextKeys.HELLBLOCK_BIOME, gui.hellblockData.getBiome());
				ActionManagerInterface.trigger(gui.context, warpedForestActions);
			}

			if (element.getSymbol() == crimsonForestSlot) {
				event.setCancelled(true);
				if (biome == HellBiome.CRIMSON_FOREST) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					return;
				}
				instance.getBiomeHandler().changeHellblockBiome(userData.get(), HellBiome.CRIMSON_FOREST);
				gui.context.arg(ContextKeys.HELLBLOCK_BIOME, gui.hellblockData.getBiome());
				ActionManagerInterface.trigger(gui.context, crimsonForestActions);
			}

			if (element.getSymbol() == netherWastesSlot) {
				event.setCancelled(true);
				if (biome == HellBiome.NETHER_WASTES) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					return;
				}
				instance.getBiomeHandler().changeHellblockBiome(userData.get(), HellBiome.NETHER_WASTES);
				gui.context.arg(ContextKeys.HELLBLOCK_BIOME, gui.hellblockData.getBiome());
				ActionManagerInterface.trigger(gui.context, netherWastesActions);
			}

			if (element.getSymbol() == basaltDeltasSlot) {
				event.setCancelled(true);
				if (biome == HellBiome.BASALT_DELTAS) {
					audience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_BIOME_SAME_BIOME
									.arguments(AdventureHelper.miniMessage(biome.getName())).build()));
					audience.playSound(Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
					return;
				}
				instance.getBiomeHandler().changeHellblockBiome(userData.get(), HellBiome.BASALT_DELTAS);
				gui.context.arg(ContextKeys.HELLBLOCK_BIOME, gui.hellblockData.getBiome());
				ActionManagerInterface.trigger(gui.context, basaltDeltasActions);
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}