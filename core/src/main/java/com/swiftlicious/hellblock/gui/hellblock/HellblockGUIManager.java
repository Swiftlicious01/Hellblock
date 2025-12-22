package com.swiftlicious.hellblock.gui.hellblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
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
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;

public class HellblockGUIManager implements HellblockGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] ownerLayout;
	protected String[] memberLayout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final ConcurrentMap<UUID, HellblockGUI> hellblockGUICache = new ConcurrentHashMap<>();

	protected char teleportSlot;
	protected char challengeSlot;
	protected char levelSlot;
	protected char partySlot;
	protected char displaySlot;
	protected char lockSlot;
	protected char unlockSlot;
	protected char biomeSlot;
	protected char flagSlot;
	protected char upgradeSlot;
	protected char resetSlot;
	protected char visitSlot;
	protected char eventSlot;
	protected char notificationSlot;
	protected char leaderboardSlot;
	protected char resetCooldownSlot;
	protected char biomeCooldownSlot;

	protected CustomItem teleportIcon;
	protected CustomItem challengeIcon;
	protected CustomItem levelIcon;
	protected CustomItem partyIcon;
	protected CustomItem displayIcon;
	protected CustomItem lockIcon;
	protected CustomItem unlockIcon;
	protected CustomItem biomeIcon;
	protected CustomItem flagIcon;
	protected CustomItem upgradeIcon;
	protected CustomItem resetIcon;
	protected CustomItem visitIcon;
	protected CustomItem eventIcon;
	protected CustomItem notificationIcon;
	protected CustomItem leaderboardIcon;
	protected CustomItem resetCooldownIcon;
	protected CustomItem biomeCooldownIcon;
	protected Action<Player>[] teleportActions;
	protected Action<Player>[] challengeActions;
	protected Action<Player>[] levelActions;
	protected Action<Player>[] partyActions;
	protected Action<Player>[] displayActions;
	protected Action<Player>[] lockActions;
	protected Action<Player>[] unlockActions;
	protected Action<Player>[] biomeActions;
	protected Action<Player>[] flagActions;
	protected Action<Player>[] upgradeActions;
	protected Action<Player>[] resetActions;
	protected Action<Player>[] visitActions;
	protected Action<Player>[] eventActions;
	protected Action<Player>[] notificationActions;
	protected Action<Player>[] leaderboardActions;
	protected Action<Player>[] resetCooldownActions;
	protected Action<Player>[] biomeCooldownActions;

	public HellblockGUIManager(HellblockPlugin plugin) {
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
		Section configFile = instance.getConfigManager().getGUIConfig("main_menu.yml");
		if (configFile == null) {
			instance.getPluginLogger().severe("GUI for main_menu.yml was unable to load correctly!");
			return;
		}
		Section config = configFile.getSection("hellblock.gui");
		if (config == null) {
			instance.getPluginLogger()
					.severe("hellblock.gui returned null, please regenerate your main_menu.yml GUI file.");
			return;
		}

		this.ownerLayout = config.getStringList("owner-layout", new ArrayList<>()).toArray(new String[0]);
		this.memberLayout = config.getStringList("member-layout", new ArrayList<>()).toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "hellblock.title"));

		Section teleportSection = config.getSection("teleport-icon");
		if (teleportSection != null) {
			teleportSlot = teleportSection.getString("symbol", "T").charAt(0);

			teleportIcon = new SingleItemParser("teleport", teleportSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			teleportActions = instance.getActionManager(Player.class)
					.parseActions(teleportSection.getSection("action"));
		}

		Section challengeSection = config.getSection("challenge-icon");
		if (challengeSection != null) {
			challengeSlot = challengeSection.getString("symbol", "C").charAt(0);

			challengeIcon = new SingleItemParser("challenge", challengeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			challengeActions = instance.getActionManager(Player.class)
					.parseActions(challengeSection.getSection("action"));
		}

		Section levelSection = config.getSection("level-icon");
		if (levelSection != null) {
			levelSlot = levelSection.getString("symbol", "E").charAt(0);

			levelIcon = new SingleItemParser("level", levelSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			levelActions = instance.getActionManager(Player.class).parseActions(levelSection.getSection("action"));
		}

		Section partySection = config.getSection("party-icon");
		if (partySection != null) {
			partySlot = partySection.getString("symbol", "P").charAt(0);

			partyIcon = new SingleItemParser("party", partySection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			partyActions = instance.getActionManager(Player.class).parseActions(partySection.getSection("action"));
		}

		Section lockSection = config.getSection("lock-icon");
		if (lockSection != null) {
			char symbol = lockSection.getString("symbol", "L").charAt(0);
			lockSlot = symbol;
			unlockSlot = symbol;

			lockIcon = new SingleItemParser("lock", lockSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			lockActions = instance.getActionManager(Player.class).parseActions(lockSection.getSection("action"));
		}

		Section unlockSection = config.getSection("unlock-icon");
		if (unlockSection != null) {
			unlockIcon = new SingleItemParser("unlock", unlockSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			unlockActions = instance.getActionManager(Player.class).parseActions(unlockSection.getSection("action"));
		}

		Section biomeSection = config.getSection("biome-icon");
		if (biomeSection != null) {
			biomeSlot = biomeSection.getString("symbol", "B").charAt(0);

			biomeIcon = new SingleItemParser("biome", biomeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			biomeActions = instance.getActionManager(Player.class).parseActions(biomeSection.getSection("action"));
		}

		Section flagSection = config.getSection("flag-icon");
		if (flagSection != null) {
			flagSlot = flagSection.getString("symbol", "F").charAt(0);

			flagIcon = new SingleItemParser("flag", flagSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			flagActions = instance.getActionManager(Player.class).parseActions(flagSection.getSection("action"));
		}

		Section displaySection = config.getSection("display-icon");
		if (displaySection != null) {
			displaySlot = displaySection.getString("symbol", "D").charAt(0);

			displayIcon = new SingleItemParser("display", displaySection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			displayActions = instance.getActionManager(Player.class).parseActions(displaySection.getSection("action"));
		}

		Section upgradeSection = config.getSection("upgrade-icon");
		if (upgradeSection != null) {
			upgradeSlot = upgradeSection.getString("symbol", "U").charAt(0);

			upgradeIcon = new SingleItemParser("upgrade", upgradeSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			upgradeActions = instance.getActionManager(Player.class).parseActions(upgradeSection.getSection("action"));
		}

		Section resetSection = config.getSection("reset-icon");
		if (resetSection != null) {
			resetSlot = resetSection.getString("symbol", "R").charAt(0);

			resetIcon = new SingleItemParser("reset", resetSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			resetActions = instance.getActionManager(Player.class).parseActions(resetSection.getSection("action"));
		}

		Section visitSection = config.getSection("visit-icon");
		if (visitSection != null) {
			visitSlot = visitSection.getString("symbol", "V").charAt(0);

			visitIcon = new SingleItemParser("visit", visitSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			visitActions = instance.getActionManager(Player.class).parseActions(visitSection.getSection("action"));
		}

		Section eventSection = config.getSection("event-icon");
		if (eventSection != null) {
			eventSlot = eventSection.getString("symbol", "I").charAt(0);

			eventIcon = new SingleItemParser("event", eventSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			eventActions = instance.getActionManager(Player.class).parseActions(eventSection.getSection("action"));
		}

		Section notificationSection = config.getSection("notifications-icon");
		if (notificationSection != null) {
			notificationSlot = notificationSection.getString("symbol", "N").charAt(0);

			notificationIcon = new SingleItemParser("notification", notificationSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			notificationActions = instance.getActionManager(Player.class)
					.parseActions(notificationSection.getSection("action"));
		}

		Section leaderboardSection = config.getSection("leaderboard-icon");
		if (leaderboardSection != null) {
			leaderboardSlot = leaderboardSection.getString("symbol", "!").charAt(0);

			leaderboardIcon = new SingleItemParser("leaderboard", leaderboardSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			leaderboardActions = instance.getActionManager(Player.class)
					.parseActions(leaderboardSection.getSection("action"));
		}

		Section resetCooldownSection = config.getSection("reset-cooldown-icon");
		if (resetCooldownSection != null) {
			resetCooldownSlot = resetSlot;

			resetCooldownIcon = new SingleItemParser("reset_cooldown", resetCooldownSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			resetCooldownActions = instance.getActionManager(Player.class)
					.parseActions(resetCooldownSection.getSection("action"));
		}

		Section biomeCooldownSection = config.getSection("biome-cooldown-icon");
		if (biomeCooldownSection != null) {
			biomeCooldownSlot = biomeSlot;

			biomeCooldownIcon = new SingleItemParser("biome_cooldown", biomeCooldownSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			biomeCooldownActions = instance.getActionManager(Player.class)
					.parseActions(biomeCooldownSection.getSection("action"));
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
	public boolean openHellblockGUI(Player player, int islandId, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		Context<Integer> islandContext = Context.island(islandId);
		HellblockGUI gui = new HellblockGUI(this, context, islandContext, optionalUserData.get().getHellblockData(),
				isOwner);
		gui.addElement(new HellblockDynamicGUIElement(teleportSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(challengeSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(partySlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(levelSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(displaySlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(biomeSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(lockSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(flagSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(upgradeSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(resetSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(visitSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(notificationSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(leaderboardSlot, new ItemStack(Material.AIR)));
		gui.addElement(new HellblockDynamicGUIElement(eventSlot, new ItemStack(Material.AIR)));
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new HellblockGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
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

		// Check if the clicked inventory is a HellblockGUI
		if (!(event.getInventory().getHolder() instanceof HellblockGUIHolder))
			return;

		HellblockGUI gui = hellblockGUICache.get(player.getUniqueId());
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
			HellblockGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			HellblockData hellblockData = gui.hellblockData;
			Optional<UserData> userData = instance.getStorageManager()
					.getOnlineUser(gui.context.holder().getUniqueId());

			if (userData.isEmpty() || !hellblockData.hasHellblock() || hellblockData.getOwnerUUID() == null) {
				event.setCancelled(true);
				player.closeInventory();
				return;
			}

			Pair<CustomItem, Action<Player>[]> decorativeIcon = this.decorativeIcons.get(element.getSymbol());
			if (decorativeIcon != null) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, decorativeIcon.right());
				return;
			}

			Sender audience = instance.getSenderFactory().wrap(gui.context.holder());

			if (element.getSymbol() == levelSlot) {
				event.setCancelled(true);
				instance.getStorageManager().getCachedUserDataWithFallback(hellblockData.getOwnerUUID(), false)
						.thenCompose(optData -> {
							if (optData.isEmpty()) {
								// Close inventory on the main thread
								instance.getScheduler().executeSync(player::closeInventory);
								return CompletableFuture.completedFuture(null);
							}

							UserData ownerData = optData.get();

							if (ownerData.getHellblockData().isAbandoned()) {
								// Send abandoned message and play sound on the main thread
								instance.getScheduler().executeSync(() -> {
									audience.sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													Sound.Source.PLAYER, 1, 1));
								});
								return CompletableFuture.completedFuture(null);
							}

							// Update context and trigger actions on the main thread
							// Fetch rank asynchronously
							return instance.getIslandLevelManager()
									.getLevelRank(ownerData.getHellblockData().getIslandId())
									.thenAccept(rank -> instance.getScheduler().executeSync(() -> {
										gui.islandContext
												.arg(ContextKeys.ISLAND_LEVEL,
														ownerData.getHellblockData().getIslandLevel())
												.arg(ContextKeys.ISLAND_RANK, rank.intValue() > 0 ? String.valueOf(rank)
														: instance.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNRANKED.build().key()));
										ActionManager.trigger(gui.context, levelActions);
									})).exceptionally(ex -> {
										instance.getPluginLogger().severe("Failed to get level rank on demand for "
												+ gui.context.holder().getName() + ": " + ex.getMessage());
										return null;
									});
						}).exceptionally(ex -> {
							instance.getPluginLogger().severe("Failed to refresh island level for "
									+ gui.context.holder().getName() + ": " + ex.getMessage());
							return null;
						});
				return;
			}

			if (element.getSymbol() == challengeSlot) {
				event.setCancelled(true);
				boolean opened = instance.getChallengesGUIManager().openChallengesGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner, true);
				if (opened)
					ActionManager.trigger(gui.context, challengeActions);
				return;
			}

			if (element.getSymbol() == notificationSlot) {
				event.setCancelled(true);
				boolean opened = instance.getNotificationGUIManager().openNotificationGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner);
				if (opened)
					ActionManager.trigger(gui.context, notificationActions);
				return;
			}

			if (element.getSymbol() == leaderboardSlot) {
				event.setCancelled(true);
				boolean opened = instance.getLeaderboardGUIManager().openLeaderboardGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner, true);
				if (opened)
					ActionManager.trigger(gui.context, leaderboardActions);
				return;
			}

			if (element.getSymbol() == visitSlot) {
				event.setCancelled(true);
				boolean opened = instance.getVisitGUIManager().openVisitGUI(gui.context.holder(),
						gui.islandContext.holder(), VisitSorter.FEATURED, gui.isOwner, true);
				if (opened)
					ActionManager.trigger(gui.context, visitActions);
				return;
			}

			if (element.getSymbol() == eventSlot) {
				event.setCancelled(true);
				instance.getStorageManager().getCachedUserDataWithFallback(hellblockData.getOwnerUUID(), false)
						.thenAccept(optData -> {
							if (optData.isEmpty()) {
								// Close inventory on the main thread
								instance.getScheduler().executeSync(player::closeInventory);
								return;
							}

							UserData ownerData = optData.get();

							if (ownerData.getHellblockData().isAbandoned()) {
								// Send abandoned message and play sound on the main thread
								instance.getScheduler().executeSync(() -> {
									audience.sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													Sound.Source.PLAYER, 1, 1));
								});
								return;
							}

							instance.getScheduler().executeSync(() -> {
								boolean opened = instance.getEventGUIManager().openEventGUI(gui.context.holder(),
										gui.islandContext.holder(), gui.isOwner);
								if (opened)
									ActionManager.trigger(gui.context, eventActions);
							});
						}).exceptionally(ex -> {
							instance.getPluginLogger().severe("Failed to open EventGUI for "
									+ gui.context.holder().getName() + ": " + ex.getMessage());
							return null;
						});
				return;
			}

			if (element.getSymbol() == partySlot) {
				event.setCancelled(true);
				instance.getStorageManager().getCachedUserDataWithFallback(hellblockData.getOwnerUUID(), false)
						.thenAccept(optData -> {
							if (optData.isEmpty()) {
								// Close inventory on the main thread
								instance.getScheduler().executeSync(player::closeInventory);
								return;
							}

							UserData ownerData = optData.get();

							if (ownerData.getHellblockData().isAbandoned()) {
								// Send abandoned message and play sound on the main thread
								instance.getScheduler().executeSync(() -> {
									audience.sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													Sound.Source.PLAYER, 1, 1));
								});
								return;
							}

							gui.islandContext.arg(ContextKeys.ISLAND_PARTY_COUNT,
									ownerData.getHellblockData().getPartyMembers().size());
							instance.getScheduler().executeSync(() -> {
								boolean opened = instance.getPartyGUIManager().openPartyGUI(gui.context.holder(),
										gui.islandContext.holder(), gui.isOwner);
								if (opened)
									ActionManager.trigger(gui.context, partyActions);
							});
						}).exceptionally(ex -> {
							instance.getPluginLogger().severe("Failed to open PartyGUI for "
									+ gui.context.holder().getName() + ": " + ex.getMessage());
							return null;
						});
				return;
			}

			if (element.getSymbol() == teleportSlot) {
				event.setCancelled(true);
				instance.getStorageManager().getCachedUserDataWithFallback(hellblockData.getOwnerUUID(), true)
						.thenCompose(optData -> {
							if (optData.isEmpty()) {
								instance.getScheduler().executeSync(player::closeInventory);
								return CompletableFuture.completedFuture(false);
							}

							UserData ownerData = optData.get();

							if (ownerData.getHellblockData().isAbandoned()) {
								instance.getScheduler().executeSync(() -> {
									audience.sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
								});
								return CompletableFuture.completedFuture(false);
							}

							if (ownerData.getHellblockData().getHomeLocation() == null) {
								instance.getScheduler().executeSync(() -> {
									audience.sendMessage(instance.getTranslationManager()
											.render(MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION.build()));
									AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
													net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
								});
								return CompletableFuture.failedFuture(new NullPointerException(
										"Hellblock home location returned null, please report this to the developer."));
							}

							return instance.getWorldManager()
									.ensureHellblockWorldLoaded(ownerData.getHellblockData().getIslandId())
									.thenCompose(loadedWorld -> {
										World world = loadedWorld.bukkitWorld();
										if (world == null) {
											instance.getScheduler().executeSync(() -> {
												audience.sendMessage(instance.getTranslationManager().render(
														MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION.build()));
												AdventureHelper.playSound(
														instance.getSenderFactory().getAudience(player),
														Sound.sound(
																net.kyori.adventure.key.Key
																		.key("minecraft:entity.villager.no"),
																net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
											});
											return CompletableFuture.failedFuture(new IllegalStateException(
													"Failed to load world for home location."));
										}

										return instance.getCoopManager().makeHomeLocationSafe(ownerData, userData.get())
												.thenCompose(result -> {
													switch (result) {
													case ALREADY_SAFE:
														instance.getScheduler().executeSync(player::closeInventory);
														gui.islandContext.arg(ContextKeys.ISLAND_HOME_LOCATION,
																ownerData.getHellblockData().getHomeLocation());
														ActionManager.trigger(gui.context, teleportActions);
														return instance.getHellblockHandler().teleportPlayerToHome(
																userData.get(),
																ownerData.getHellblockData().getHomeLocation());
													case FIXED_AND_TELEPORTED:
														instance.getScheduler().executeSync(player::closeInventory);
														gui.islandContext.arg(ContextKeys.ISLAND_HOME_LOCATION,
																ownerData.getHellblockData().getHomeLocation());
														ActionManager.trigger(gui.context, teleportActions);
														return CompletableFuture.completedFuture(true);
													case FAILED_TO_FIX:
													default:
														instance.getScheduler().executeSync(player::closeInventory);
														return CompletableFuture.completedFuture(instance
																.getHellblockHandler().teleportToSpawn(player, true));
													}
												});
									});
						}).handle((result, ex) -> {
							// Always unlock user data
							instance.getStorageManager().unlockUserData(hellblockData.getOwnerUUID())
									.thenAccept(unused -> {
										if (ex != null) {
											instance.getPluginLogger().warn(
													"Failed to perform teleportation to hellblock home for player: "
															+ gui.context.holder().getName(),
													ex);
										}
									});
							return null;
						});
				return;
			}

			if (!hellblockData.getOwnerUUID().equals(gui.context.holder().getUniqueId())) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (hellblockData.isAbandoned()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.villager.no"),
								net.kyori.adventure.sound.Sound.Source.PLAYER, 1, 1));
				return;
			}

			if (element.getSymbol() == resetSlot) {
				event.setCancelled(true);
				if (hellblockData.getResetCooldown() > 0) {
					gui.context.arg(ContextKeys.RESET_COOLDOWN, hellblockData.getResetCooldown()).arg(
							ContextKeys.RESET_COOLDOWN_FORMATTED,
							instance.getCooldownManager().getFormattedCooldown(hellblockData.getResetCooldown()));
					ActionManager.trigger(gui.context, resetCooldownActions);
				} else {
					boolean opened = instance.getResetConfirmGUIManager().openResetConfirmGUI(gui.context.holder(),
							gui.islandContext.holder(), gui.isOwner);
					if (opened)
						ActionManager.trigger(gui.context, resetActions);
				}
				return;
			}

			if (element.getSymbol() == biomeSlot) {
				event.setCancelled(true);
				if (hellblockData.getBiomeCooldown() > 0) {
					gui.islandContext.arg(ContextKeys.BIOME_COOLDOWN, hellblockData.getBiomeCooldown()).arg(
							ContextKeys.BIOME_COOLDOWN_FORMATTED,
							instance.getCooldownManager().getFormattedCooldown(hellblockData.getBiomeCooldown()));
					ActionManager.trigger(gui.context, biomeCooldownActions);
				} else {
					gui.islandContext.arg(ContextKeys.ISLAND_BIOME, hellblockData.getBiome());
					boolean opened = instance.getBiomeGUIManager().openBiomeGUI(gui.context.holder(),
							gui.islandContext.holder(), gui.isOwner);
					if (opened)
						ActionManager.trigger(gui.context, biomeActions);
				}
				return;
			}

			if (element.getSymbol() == lockSlot) {
				event.setCancelled(true);

				boolean newLockState = !hellblockData.isLocked(); // Toggle
				hellblockData.setLockedStatus(newLockState);
				gui.islandContext.arg(ContextKeys.ISLAND_STATUS, newLockState);

				instance.getWorldManager().getWorld(gui.context.holder().getWorld()).ifPresent(world -> {
					instance.getProtectionManager().changeLockStatus(world, userData.get().getUUID())
							.thenCompose(status -> {
								if (!status) {
									Action<Player>[] actions = newLockState ? lockActions : unlockActions;
									ActionManager.trigger(gui.context, actions);
								}
								return instance.getCoopManager().kickVisitorsIfLocked(userData.get().getUUID());
							}).exceptionally(ex -> {
								instance.getPluginLogger().warn("Failed to change island lock status for "
										+ userData.get().getName() + ": " + ex.getMessage(), ex);
								return null;
							});
				});
			}

			if (element.getSymbol() == flagSlot) {
				event.setCancelled(true);
				boolean opened = instance.getFlagsGUIManager().openFlagsGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner);
				if (opened)
					ActionManager.trigger(gui.context, flagActions);
				return;
			}

			if (element.getSymbol() == displaySlot) {
				event.setCancelled(true);
				gui.islandContext.arg(ContextKeys.ISLAND_NAME, hellblockData.getDisplaySettings().getIslandName())
						.arg(ContextKeys.ISLAND_BIO, hellblockData.getDisplaySettings().getIslandBio())
						.arg(ContextKeys.ISLAND_DISPLAY_CHOICE, hellblockData.getDisplaySettings().getDisplayChoice());
				boolean opened = instance.getDisplaySettingsGUIManager().openDisplaySettingsGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner);
				if (opened)
					ActionManager.trigger(gui.context, displayActions);
				return;
			}

			if (element.getSymbol() == upgradeSlot) {
				event.setCancelled(true);
				gui.islandContext
						.arg(ContextKeys.ISLAND_HOPPER_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.HOPPER_LIMIT))
						.arg(ContextKeys.ISLAND_RANGE_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.PROTECTION_RANGE))
						.arg(ContextKeys.ISLAND_GENERATOR_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.GENERATOR_CHANCE))
						.arg(ContextKeys.ISLAND_PARTY_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.PARTY_SIZE))
						.arg(ContextKeys.ISLAND_BARTERING_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.PIGLIN_BARTERING))
						.arg(ContextKeys.ISLAND_CROP_TIER, hellblockData.getUpgradeLevel(IslandUpgradeType.CROP_GROWTH))
						.arg(ContextKeys.ISLAND_MOB_TIER,
								hellblockData.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE));
				boolean opened = instance.getUpgradeGUIManager().openUpgradeGUI(gui.context.holder(),
						gui.islandContext.holder(), gui.isOwner);
				if (opened)
					ActionManager.trigger(gui.context, upgradeActions);
				return;
			}
		}

		// Refresh the GUI
		if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 15000))

		{
			gui.hellblockData.updateLastIslandActivity();
		}
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}
}