package com.swiftlicious.hellblock.gui.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import com.swiftlicious.hellblock.commands.sub.HellblockIslandBioCommand;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.handlers.ActionManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DisplaySettingsGUIManager implements DisplaySettingsGUIManagerInterface, Listener {

	protected final HellblockPlugin instance;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, Pair<CustomItem, Action<Player>[]>> decorativeIcons = new HashMap<>();
	protected final ConcurrentMap<UUID, DisplaySettingsGUI> displaySettingsGUICache = new ConcurrentHashMap<>();

	protected char backSlot;
	protected char nameSlot;
	protected char bioSlot;
	protected char toggleSlot;

	protected TextValue<Player> anvilNameTitle;
	protected TextValue<Player> anvilBioTitle;

	protected Section nameSection;
	protected Section bioSection;
	protected Section toggleSection;

	protected CustomItem backIcon;
	protected CustomItem nameIcon;
	protected CustomItem bioIcon;
	protected CustomItem toggleIcon;
	protected Action<Player>[] backActions;
	protected Action<Player>[] nameActions;
	protected Action<Player>[] bioActions;
	protected Action<Player>[] toggleActions;

	public DisplaySettingsGUIManager(HellblockPlugin plugin) {
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
		Section config = instance.getConfigManager().getGuiConfig().getSection("display.gui");

		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "display.title"));

		Section backSection = config.getSection("back-icon");
		if (backSection != null) {
			backSlot = backSection.getString("symbol", "X").charAt(0);

			backIcon = new SingleItemParser("back", backSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			backActions = instance.getActionManager(Player.class).parseActions(backSection.getSection("action"));
		}

		nameSection = config.getSection("name-icon");
		if (nameSection != null) {
			anvilNameTitle = TextValue.auto(nameSection.getString("anvil-settings.title", "display.anvil.name"));
			nameSlot = nameSection.getString("symbol", "N").charAt(0);

			nameIcon = new SingleItemParser("name", nameSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			nameActions = instance.getActionManager(Player.class).parseActions(nameSection.getSection("action"));
		}

		bioSection = config.getSection("bio-icon");
		if (bioSection != null) {
			anvilBioTitle = TextValue.auto(bioSection.getString("anvil-settings.title", "display.anvil.bio"));
			bioSlot = bioSection.getString("symbol", "B").charAt(0);

			bioIcon = new SingleItemParser("bio", bioSection, instance.getConfigManager().getItemFormatFunctions())
					.getItem();
			bioActions = instance.getActionManager(Player.class).parseActions(bioSection.getSection("action"));
		}

		toggleSection = config.getSection("toggle-icon");
		if (toggleSection != null) {
			toggleSlot = toggleSection.getString("symbol", "T").charAt(0);

			toggleIcon = new SingleItemParser("toggle", toggleSection,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			toggleActions = instance.getActionManager(Player.class).parseActions(toggleSection.getSection("action"));
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
	 * Open the DisplaySettings GUI for a player
	 *
	 * @param player  player
	 * @param isOwner is player owner
	 */
	@Override
	public boolean openDisplaySettingsGUI(Player player, boolean isOwner) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger()
					.warn("Player " + player.getName() + "'s hellblock data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		DisplaySettingsGUI gui = new DisplaySettingsGUI(this, context, optionalUserData.get().getHellblockData(),
				isOwner);
		gui.addElement(new DisplaySettingsDynamicGUIElement(backSlot, new ItemStack(Material.AIR)));
		gui.addElement(new DisplaySettingsDynamicGUIElement(nameSlot, new ItemStack(Material.AIR)));
		gui.addElement(new DisplaySettingsDynamicGUIElement(bioSlot, new ItemStack(Material.AIR)));
		gui.addElement(new DisplaySettingsDynamicGUIElement(toggleSlot, new ItemStack(Material.AIR)));
		decorativeIcons.entrySet().forEach(entry -> gui
				.addElement(new DisplaySettingsGUIElement(entry.getKey(), entry.getValue().left().build(context))));
		gui.build().show();
		gui.refresh();
		displaySettingsGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof DisplaySettingsGUIHolder))
			return;
		displaySettingsGUICache.remove(player.getUniqueId());
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		displaySettingsGUICache.remove(event.getPlayer().getUniqueId());
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
		if (!(inventory.getHolder() instanceof DisplaySettingsGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		DisplaySettingsGUI gui = displaySettingsGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a DisplaySettingsGUI
		if (!(event.getInventory().getHolder() instanceof DisplaySettingsGUIHolder))
			return;

		DisplaySettingsGUI gui = displaySettingsGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			DisplaySettingsGUIElement element = gui.getElement(slot);
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
				instance.getHellblockGUIManager().openHellblockGUI(gui.context.holder(), gui.isOwner);
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

			if (element.getSymbol() == nameSlot) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, nameActions);
				String currentName = gui.hellblockData.getDisplaySettings().getIslandName();
				DisplayNameInputGUI input = new DisplayNameInputGUI(instance, gui, player, currentName,
						gui.hellblockData);
				input.init();
				input.open();
				return;
			}

			if (element.getSymbol() == bioSlot) {
				event.setCancelled(true);
				ActionManager.trigger(gui.context, bioActions);
				String currentBio = gui.hellblockData.getDisplaySettings().getIslandBio();
				DisplayBioInputGUI input = new DisplayBioInputGUI(instance, gui, player, currentBio, gui.hellblockData);
				input.init();
				input.open();
				return;
			}

			if (element.getSymbol() == toggleSlot) {
				event.setCancelled(true);
				DisplaySettings displaySettings = gui.hellblockData.getDisplaySettings();
				DisplayChoice current = displaySettings.getDisplayChoice();
				// Toggle between CHAT and TITLE
				DisplayChoice newChoice = (current == DisplayChoice.CHAT) ? DisplayChoice.TITLE : DisplayChoice.CHAT;
				displaySettings.setDisplayChoice(newChoice);
				gui.context.arg(ContextKeys.HELLBLOCK_DISPLAY_CHOICE, displaySettings.getDisplayChoice());
				ActionManager.trigger(gui.context, toggleActions);
				return;
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	public class DisplayNameInputGUI extends AbstractAnvilInputGUI {

		private final HellblockData data;
		private final DisplaySettingsGUI gui;

		public DisplayNameInputGUI(HellblockPlugin plugin, DisplaySettingsGUI gui, Player player, String initialText,
				HellblockData data) {
			super(plugin, player, anvilNameTitle.render(Context.player(player), true), initialText);
			this.data = data;
			this.gui = gui;
		}

		@Override
		protected void onInput(String input) {
			String name = input.trim();

			// Cancel keyword
			if ("cancel".equalsIgnoreCase(name)) {
				onCancel();
				return;
			}

			// Reset / clear keyword
			if ("reset".equalsIgnoreCase(name) || "clear".equalsIgnoreCase(name)) {
				data.getDisplaySettings().setIslandName(data.getDefaultIslandName());
				data.getDisplaySettings().setAsDefaultIslandName();
				gui.context.arg(ContextKeys.HELLBLOCK_NAME, data.getDisplaySettings().getIslandName());
				instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
				return;
			}

			// Validation
			int maxLength = instance.getConfigManager().maxNameCharLength();
			List<String> bannedWords = instance.getConfigManager().bannedWords();

			// Basic checks
			if (name.isEmpty()) {
				updateInputDisplay(nameSection.getString("anvil-settings.empty"));
				return;
			}

			if (!name.matches(".*[A-Za-z0-9].*")) {
				updateInputDisplay(nameSection.getString("anvil-settings.invalid"));
				return;
			}

			if (name.length() > maxLength) {
				updateInputDisplay(
						nameSection.getString("anvil-settings.length").replace("{length}", String.valueOf(maxLength)));
				return;
			}

			// Banned word detection
			Set<String> detected = HellblockIslandBioCommand.findBannedWords(name, bannedWords);
			if (!detected.isEmpty()) {
				updateInputDisplay(nameSection.getString("anvil-settings.banned").replace("{banned}",
						String.join(", ", detected)));
				return;
			}

			// Unchanged
			String current = data.getDisplaySettings().getIslandName();
			if (current.equals(name)) {
				updateInputDisplay(nameSection.getString("anvil-settings.unchanged"));
				return;
			}

			// Save new name
			data.getDisplaySettings().setIslandName(name);
			data.getDisplaySettings().isNotDefaultIslandName();
			gui.context.arg(ContextKeys.HELLBLOCK_NAME, data.getDisplaySettings().getIslandName());

			// Reopen settings GUI
			instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
		}

		@Override
		protected void onCancel() {
			instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
		}
	}

	public class DisplayBioInputGUI extends AbstractAnvilInputGUI {

		private final HellblockData data;
		private final DisplaySettingsGUI gui;

		public DisplayBioInputGUI(HellblockPlugin plugin, DisplaySettingsGUI gui, Player player, String initialText,
				HellblockData data) {
			super(plugin, player, anvilBioTitle.render(Context.player(player), true), initialText);
			this.data = data;
			this.gui = gui;
		}

		@Override
		protected void onInput(String input) {
			String rawBio = input.trim();

			if ("cancel".equalsIgnoreCase(rawBio)) {
				onCancel();
				return;
			}

			if ("reset".equalsIgnoreCase(rawBio) || "clear".equalsIgnoreCase(rawBio)) {
				data.getDisplaySettings().setIslandBio(data.getDefaultIslandBio());
				data.getDisplaySettings().setAsDefaultIslandBio();
				gui.context.arg(ContextKeys.HELLBLOCK_BIO, data.getDisplaySettings().getIslandBio());
				instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
				return;
			}

			int maxLength = instance.getConfigManager().maxBioCharLength();
			int maxLines = instance.getConfigManager().maxNewLines();
			int maxColorCodes = instance.getConfigManager().maxColorCodes();
			List<String> bannedWords = instance.getConfigManager().bannedWords();

			String preProcessed = rawBio.replace("\\n", "\n").trim();

			String formattedBio = AdventureHelper.getMiniMessage()
					.serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(preProcessed));

			String plain = PlainTextComponentSerializer.plainText()
					.serialize(AdventureHelper.getMiniMessage().deserialize(preProcessed));

			// Line count
			if (preProcessed.split("\n").length > maxLines) {
				updateInputDisplay(
						bioSection.getString("anvil-settings.lines").replace("{lines}", String.valueOf(maxLines)));
				return;
			}

			// Basic text checks
			boolean emptyAfterTrim = plain.trim().isEmpty();
			boolean hasNoVisibleChars = plain.replaceAll("\\s+", "").isEmpty();
			boolean hasNoAlphanumeric = !plain.matches(".*[A-Za-z0-9].*");
			String colorStripped = rawBio.replaceAll("(?i)&[0-9A-FK-ORX]", "").replaceAll("(?i)<(/?\\w+)>", "")
					.replaceAll("[<>]", "").replaceAll("\\s+", "");
			boolean onlyFormatting = colorStripped.isEmpty();

			if (emptyAfterTrim || hasNoVisibleChars || hasNoAlphanumeric || onlyFormatting) {
				updateInputDisplay(bioSection.getString("anvil-settings.empty"));
				return;
			}

			// Length
			if (plain.length() > maxLength) {
				updateInputDisplay(
						bioSection.getString("anvil-settings.length").replace("{length}", String.valueOf(maxLength)));
				return;
			}

			// Color code spam
			int colorCodeCount = countColorCodes(rawBio);
			if (colorCodeCount > maxColorCodes && plain.length() < maxColorCodes) {
				updateInputDisplay(bioSection.getString("anvil-settings.color-codes"));
				return;
			}

			// Banned words
			Set<String> detected = HellblockIslandBioCommand.findBannedWords(plain, bannedWords);
			if (!detected.isEmpty()) {
				updateInputDisplay(
						bioSection.getString("anvil-settings.banned").replace("{banned}", String.join(", ", detected)));
				return;
			}

			// Unchanged
			String current = data.getDisplaySettings().getIslandBio();
			if (current.equals(formattedBio)) {
				updateInputDisplay(bioSection.getString("anvil-settings.unchanged"));
				return;
			}

			// Save
			data.getDisplaySettings().setIslandBio(formattedBio);
			data.getDisplaySettings().isNotDefaultIslandBio();
			gui.context.arg(ContextKeys.HELLBLOCK_BIO, data.getDisplaySettings().getIslandBio());
			
			instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
		}

		@Override
		protected void onCancel() {
			instance.getScheduler().executeSync(() -> openDisplaySettingsGUI(player, gui.isOwner));
		}

		private int countColorCodes(String input) {
			int legacy = 0;
			for (int i = 0; i < input.length() - 1; i++) {
				if (input.charAt(i) == '&'
						&& "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(input.charAt(i + 1))) != -1) {
					legacy++;
				}
			}
			int mini = (int) input.chars().filter(ch -> ch == '<').count();
			return legacy + mini;
		}
	}
}