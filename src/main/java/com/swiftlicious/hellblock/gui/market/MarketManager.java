package com.swiftlicious.hellblock.gui.market;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.BuildableItem;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.NBTUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.Condition;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.objecthunter.exp4j.ExpressionBuilder;

public class MarketManager implements MarketManagerInterface, Listener {

	protected final HellblockPlugin instance;
	private final Map<String, Double> priceMap;
	private String[] layout;
	private String title;
	private String formula;
	private final Map<Character, BuildableItem> decorativeIcons;
	private char itemSlot;
	private char sellSlot;
	private char sellAllSlot;
	private BuildableItem sellIconAllowBuilder;
	private BuildableItem sellIconDenyBuilder;
	private BuildableItem sellIconLimitBuilder;
	private BuildableItem sellAllIconAllowBuilder;
	private BuildableItem sellAllIconDenyBuilder;
	private BuildableItem sellAllIconLimitBuilder;
	private Action[] sellDenyActions;
	private Action[] sellAllowActions;
	private Action[] sellLimitActions;
	private Action[] sellAllDenyActions;
	private Action[] sellAllAllowActions;
	private Action[] sellAllLimitActions;
	private String earningLimitExpression;
	private boolean allowItemWithNoPrice;
	private final ConcurrentMap<UUID, MarketGUI> marketGUIMap;
	private boolean enable;
	private SchedulerTask resetEarningsTask;
	private int date;

	public MarketManager(HellblockPlugin plugin) {
		instance = plugin;
		this.priceMap = new HashMap<>();
		this.decorativeIcons = new HashMap<>();
		this.marketGUIMap = new ConcurrentHashMap<>();
		this.date = getDate();
	}

	@Override
	public void load() {
		this.loadConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
		if (!enable)
			return;
		this.resetEarningsTask = instance.getScheduler().asyncRepeating(() -> {
			int now = getDate();
			if (this.date != now) {
				this.date = now;
				for (UserData onlineUser : instance.getStorageManager().getOnlineUsers()) {
					onlineUser.getEarningData().setDate(now);
					onlineUser.getEarningData().setEarnings(0.0D);
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.priceMap.clear();
		this.decorativeIcons.clear();
		if (this.resetEarningsTask != null) {
			this.resetEarningsTask.cancel();
			this.resetEarningsTask = null;
		}
	}
	
	@Override
	public void disable() {
		unload();
	}

	public boolean checkMarketPrice(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellMarket", "price");
	}

	public double getMarketPrice(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return -1;

		return new RtagItem(item).getOptional("HellMarket", "price").asDouble();
	}

	public boolean checkSize(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellMarket", "size");
	}

	public float getSize(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return -1;

		return new RtagItem(item).getOptional("HellMarket", "size").asFloat();
	}

	public boolean checkUUID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellMarket", "uuid");
	}

	public @Nullable UUID getUUID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return new RtagItem(item).getOptional("HellMarket", "uuid").asUuid();
	}

	public @Nullable ItemStack setMarketPrice(@Nullable ItemStack item, double data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellMarket", "price");
		});
	}

	public @Nullable ItemStack setSize(@Nullable ItemStack item, float data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellMarket", "size");
		});
	}

	public @Nullable ItemStack setUUID(@Nullable ItemStack item, @Nullable UUID data) {
		if (item == null || item.getType() == Material.AIR || data == null)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellMarket", "uuid");
		});
	}

	public boolean removeMarketData(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).remove("HellMarket");
	}

	// Load configuration from the plugin's config file
	private void loadConfig() {
		File marketFile = new File(instance.getDataFolder(), "market.yml");
		if (!marketFile.exists())
			marketFile.mkdirs();
		YamlDocument config = instance.getConfigManager().loadData(marketFile);
		this.enable = config.getBoolean("enable", true);
		this.formula = config.getString("price-formula", "{base} + {bonus} * {size}");
		if (!this.enable)
			return;

		// Load various configuration settings
		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = config.getString("title", "market.title");
		this.itemSlot = config.getString("item-slot.symbol", "I").charAt(0);
		this.allowItemWithNoPrice = config.getBoolean("item-slot.allow-items-with-no-price", true);

		Section sellAllSection = config.getSection("sell-all-icons");
		if (sellAllSection != null) {
			this.sellAllSlot = sellAllSection.getString("symbol", "S").charAt(0);

			this.sellAllIconAllowBuilder = instance.getItemManager()
					.getItemBuilder(sellAllSection.getSection("allow-icon"), "gui", "sell-all");
			this.sellAllIconDenyBuilder = instance.getItemManager()
					.getItemBuilder(sellAllSection.getSection("deny-icon"), "gui", "sell-all");
			this.sellAllIconLimitBuilder = instance.getItemManager()
					.getItemBuilder(sellAllSection.getSection("limit-icon"), "gui", "sell-all");

			this.sellAllAllowActions = instance.getActionManager()
					.getActions(sellAllSection.getSection("allow-icon.action"));
			this.sellAllDenyActions = instance.getActionManager()
					.getActions(sellAllSection.getSection("deny-icon.action"));
			this.sellAllLimitActions = instance.getActionManager()
					.getActions(sellAllSection.getSection("limit-icon.action"));
		}

		Section sellSection = config.getSection("sell-icons");
		if (sellSection == null) {
			// for old config compatibility
			sellSection = config.getSection("functional-icons");
		}
		if (sellSection != null) {
			this.sellSlot = sellSection.getString("symbol", "B").charAt(0);

			this.sellIconAllowBuilder = instance.getItemManager().getItemBuilder(sellSection.getSection("allow-icon"),
					"gui", "allow");
			this.sellIconDenyBuilder = instance.getItemManager().getItemBuilder(sellSection.getSection("deny-icon"),
					"gui", "deny");
			this.sellIconLimitBuilder = instance.getItemManager().getItemBuilder(sellSection.getSection("limit-icon"),
					"gui", "limit");

			this.sellAllowActions = instance.getActionManager().getActions(sellSection.getSection("allow-icon.action"));
			this.sellDenyActions = instance.getActionManager().getActions(sellSection.getSection("deny-icon.action"));
			this.sellLimitActions = instance.getActionManager().getActions(sellSection.getSection("limit-icon.action"));
		}

		this.earningLimitExpression = config.getBoolean("limitation.enable", true)
				? config.getString("limitation.earnings", "10000")
				: "-1";

		// Load item prices from the configuration
		Section priceSection = config.getSection("item-price");
		if (priceSection != null) {
			for (Map.Entry<String, Object> entry : priceSection.getStringRouteMappedValues(false).entrySet()) {
				this.priceMap.put(entry.getKey(), instance.getConfigUtils().getDoubleValue(entry.getValue()));
			}
		}

		// Load decorative icons from the configuration
		Section decorativeSection = config.getSection("decorative-icons");
		if (decorativeSection != null) {
			for (Map.Entry<String, Object> entry : decorativeSection.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					char symbol = Objects.requireNonNull(innerSection.getString("symbol")).charAt(0);
					var builder = instance.getItemManager().getItemBuilder(innerSection, "gui", entry.getKey());
					decorativeIcons.put(symbol, builder);
				}
			}
		}
	}

	/**
	 * Open the market GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public void openMarketGUI(Player player) {
		if (!isEnable())
			return;
		Optional<UserData> user = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (user.isEmpty()) {
			LogUtils.warn(String.format("Player %s's market data is not loaded yet.", player.getName()));
			return;
		}

		MarketGUI gui = new MarketGUI(this, player, user.get().getEarningData());
		gui.addElement(new MarketGUIElement(getItemSlot(), new ItemStack(Material.AIR)));
		gui.addElement(new MarketDynamicGUIElement(getSellSlot(), new ItemStack(Material.AIR)));
		gui.addElement(new MarketDynamicGUIElement(getSellAllSlot(), new ItemStack(Material.AIR)));
		for (Map.Entry<Character, BuildableItem> entry : decorativeIcons.entrySet()) {
			gui.addElement(new MarketGUIElement(entry.getKey(), entry.getValue().build(player)));
		}
		gui.build().refresh().show(player);
		marketGUIMap.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof MarketGUIHolder))
			return;
		MarketGUI gui = marketGUIMap.remove(player.getUniqueId());
		if (gui != null)
			gui.returnItems();
	}

	/**
	 * This method handles a player quitting the server.
	 *
	 * @param event The PlayerQuitEvent that triggered this method.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		MarketGUI gui = marketGUIMap.remove(event.getPlayer().getUniqueId());
		if (gui != null)
			gui.returnItems();
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
		if (!(inventory.getHolder() instanceof MarketGUIHolder))
			return;
		Player player = (Player) event.getWhoClicked();
		MarketGUI gui = marketGUIMap.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		MarketGUIElement element = gui.getElement(itemSlot);
		if (element == null) {
			event.setCancelled(true);
			return;
		}

		List<Integer> slots = element.getSlots();
		for (int dragSlot : event.getRawSlots()) {
			if (!slots.contains(dragSlot)) {
				event.setCancelled(true);
				return;
			}
		}

		instance.getScheduler().sync().runLater(gui::refresh, 50, player.getLocation());
	}

	/**
	 * This method handles inventory click events.
	 *
	 * @param event The InventoryClickEvent that triggered this method.
	 */
	@EventHandler
	public void onClickInv(InventoryClickEvent event) {
		if (event.isCancelled())
			return;

		Inventory clickedInv = event.getClickedInventory();
		if (clickedInv == null)
			return;

		Player player = (Player) event.getWhoClicked();

		// Check if the clicked inventory is a MarketGUI
		if (!(event.getInventory().getHolder() instanceof MarketGUIHolder))
			return;

		MarketGUI gui = marketGUIMap.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		if (clickedInv != player.getInventory()) {
			EarningData data = gui.getEarningData();
			if (data.getDate() != getCachedDate()) {
				data.setDate(getCachedDate());
				data.setEarnings(0.0D);
			}

			int slot = event.getSlot();
			MarketGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			if (element.getSymbol() != itemSlot) {
				event.setCancelled(true);
			}

			if (element.getSymbol() == sellSlot) {
				double worth = gui.getTotalWorthInMarketGUI();
				int amount = gui.getSoldAmount();
				double earningLimit = getEarningLimit(player);
				Condition condition = new Condition(player,
						new HashMap<>(Map.of("{money}", instance.getNumberUtils().money(worth), "{rest}",
								instance.getNumberUtils().money(earningLimit - data.getEarnings()), "{money_formatted}",
								String.format("%.2f", worth), "{rest_formatted}",
								String.format("%.2f", (earningLimit - data.getEarnings())), "{sold-item-amount}",
								String.valueOf(amount))));
				if (worth > 0) {
					if (earningLimit != -1 && (earningLimit - data.getEarnings()) < worth) {
						// Can't earn more money
						if (getSellLimitActions() != null) {
							for (Action action : getSellLimitActions()) {
								action.trigger(condition);
							}
						}
					} else {
						// Clear items and update earnings
						gui.clearWorthyItems();
						data.setEarnings(data.getEarnings() + worth);
						condition.insertArg("{rest}",
								instance.getNumberUtils().money(earningLimit - data.getEarnings()));
						condition.insertArg("{rest_formatted}",
								String.format("%.2f", (earningLimit - data.getEarnings())));
						if (getSellAllowActions() != null) {
							for (Action action : getSellAllowActions()) {
								action.trigger(condition);
							}
						}
					}
				} else {
					// Nothing to sell
					if (getSellDenyActions() != null) {
						for (Action action : getSellDenyActions()) {
							action.trigger(condition);
						}
					}
				}
			} else if (element.getSymbol() == sellAllSlot) {
				double worth = getInventoryTotalWorth(player.getInventory());
				int amount = getInventorySellAmount(player.getInventory());
				double earningLimit = getEarningLimit(player);
				Condition condition = new Condition(player,
						new HashMap<>(Map.of("{money}", instance.getNumberUtils().money(worth), "{rest}",
								instance.getNumberUtils().money(earningLimit - data.getEarnings()), "{money_formatted}",
								String.format("%.2f", worth), "{rest_formatted}",
								String.format("%.2f", (earningLimit - data.getEarnings())), "{sold-item-amount}",
								String.valueOf(amount))));
				if (worth > 0) {
					if (earningLimit != -1 && (earningLimit - data.getEarnings()) < worth) {
						// Can't earn more money
						if (getSellAllLimitActions() != null) {
							for (Action action : getSellAllLimitActions()) {
								action.trigger(condition);
							}
						}
					} else {
						// Clear items and update earnings
						clearWorthyItems(player.getInventory());
						data.setEarnings(data.getEarnings() + worth);
						condition.insertArg("{rest}",
								instance.getNumberUtils().money(earningLimit - data.getEarnings()));
						condition.insertArg("{rest_formatted}",
								String.format("%.2f", (earningLimit - data.getEarnings())));
						if (getSellAllAllowActions() != null) {
							for (Action action : getSellAllAllowActions()) {
								action.trigger(condition);
							}
						}
					}
				} else {
					// Nothing to sell
					if (getSellAllDenyActions() != null) {
						for (Action action : getSellAllDenyActions()) {
							action.trigger(condition);
						}
					}
				}
			}
		} else {
			// Handle interactions with the player's inventory
			ItemStack current = event.getCurrentItem();
			if (!allowItemWithNoPrice) {
				double price = getItemPrice(current);
				if (price <= 0) {
					event.setCancelled(true);
					return;
				}
			}

			if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)
					&& (current != null && current.getType() != Material.AIR)) {
				event.setCancelled(true);
				MarketGUIElement element = gui.getElement(itemSlot);
				if (element == null)
					return;
				for (int slot : element.getSlots()) {
					ItemStack itemStack = gui.getInventory().getItem(slot);
					if (itemStack != null && itemStack.getType() != Material.AIR) {
						if (current.getType() == itemStack.getType()
								&& itemStack.getAmount() != itemStack.getType().getMaxStackSize()
								&& current.getItemMeta().equals(itemStack.getItemMeta())) {
							int left = itemStack.getType().getMaxStackSize() - itemStack.getAmount();
							if (current.getAmount() <= left) {
								itemStack.setAmount(itemStack.getAmount() + current.getAmount());
								current.setAmount(0);
								break;
							} else {
								current.setAmount(current.getAmount() - left);
								itemStack.setAmount(itemStack.getType().getMaxStackSize());
							}
						}
					} else {
						gui.getInventory().setItem(slot, current.clone());
						current.setAmount(0);
						break;
					}
				}
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 50, player.getLocation());
	}

	@Override
	public int getCachedDate() {
		return date;
	}

	@Override
	public int getDate() {
		Calendar calendar = Calendar.getInstance();
		return (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DATE);
	}

	@Override
	public double getItemPrice(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return 0;

		if (checkMarketPrice(itemStack)) {
			Double price = getMarketPrice(itemStack);
			if (price != null && price != 0) {
				// If a custom price is defined in the ItemStack's NBT data, use it.
				return price * itemStack.getAmount();
			}
		}

		// If no custom price is defined, attempt to fetch the price from a predefined
		// price map.
		String itemID = itemStack.getType().name();
		if (NBTUtils.hasNBTItemComponentData(itemStack, "custom_model_data")) {
			itemID = itemID + ":" + ((int) NBTUtils.getNBTItemComponentData(itemStack, "custom_model_data"));
		}

		// Use the price from the price map, or default to 0 if not found.
		return priceMap.getOrDefault(itemID, 0d) * itemStack.getAmount();

	}

	@Override
	public String getFormula() {
		return formula;
	}

	@Override
	public double getFishPrice(Player player, Map<String, String> vars) {
		String temp = instance.getPlaceholderManager().parse(player, formula, vars);
		var placeholders = instance.getPlaceholderManager().detectPlaceholders(temp);
		for (String placeholder : placeholders) {
			temp = temp.replace(placeholder, "0");
		}
		return new ExpressionBuilder(temp).build().evaluate();
	}

	@Override
	public char getItemSlot() {
		return itemSlot;
	}

	@Override
	public char getSellSlot() {
		return sellSlot;
	}

	@Override
	public char getSellAllSlot() {
		return sellAllSlot;
	}

	@Override
	public String[] getLayout() {
		return layout;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public double getEarningLimit(Player player) {
		return new ExpressionBuilder(
				instance.getPlaceholderManager().parse(player, earningLimitExpression, new HashMap<>())).build()
				.evaluate();
	}

	public BuildableItem getSellIconLimitBuilder() {
		return sellIconLimitBuilder;
	}

	public BuildableItem getSellIconAllowBuilder() {
		return sellIconAllowBuilder;
	}

	public BuildableItem getSellIconDenyBuilder() {
		return sellIconDenyBuilder;
	}

	public BuildableItem getSellAllIconAllowBuilder() {
		return sellAllIconAllowBuilder;
	}

	public BuildableItem getSellAllIconDenyBuilder() {
		return sellAllIconDenyBuilder;
	}

	public BuildableItem getSellAllIconLimitBuilder() {
		return sellAllIconLimitBuilder;
	}

	public Action[] getSellDenyActions() {
		return sellDenyActions;
	}

	public Action[] getSellAllowActions() {
		return sellAllowActions;
	}

	public Action[] getSellLimitActions() {
		return sellLimitActions;
	}

	public Action[] getSellAllDenyActions() {
		return sellAllDenyActions;
	}

	public Action[] getSellAllAllowActions() {
		return sellAllAllowActions;
	}

	public Action[] getSellAllLimitActions() {
		return sellAllLimitActions;
	}

	@Override
	public boolean isEnable() {
		return enable;
	}

	@Override
	public double getInventoryTotalWorth(Inventory inventory) {
		double total = 0d;
		for (ItemStack itemStack : inventory.getStorageContents()) {
			double price = getItemPrice(itemStack);
			total += price;
		}
		return total;
	}

	@Override
	public int getInventorySellAmount(Inventory inventory) {
		int amount = 0;
		for (ItemStack itemStack : inventory.getStorageContents()) {
			double price = getItemPrice(itemStack);
			if (price > 0 && itemStack != null) {
				amount += itemStack.getAmount();
			}
		}
		return amount;
	}

	public void clearWorthyItems(Inventory inventory) {
		for (ItemStack itemStack : inventory.getStorageContents()) {
			double price = getItemPrice(itemStack);
			if (price > 0 && itemStack != null) {
				itemStack.setAmount(0);
			}
		}
	}
}