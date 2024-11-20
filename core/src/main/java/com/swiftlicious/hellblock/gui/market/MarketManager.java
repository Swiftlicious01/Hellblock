package com.swiftlicious.hellblock.gui.market;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.creation.item.LavaFishingItem;
import com.swiftlicious.hellblock.creation.item.factory.BukkitItemFactory;
import com.swiftlicious.hellblock.handlers.ActionManagerInterface;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.player.ContextKeys;
import com.swiftlicious.hellblock.player.EarningData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.TextValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class MarketManager implements MarketManagerInterface, Listener {

	protected final HellblockPlugin instance;

	private final Map<String, MathValue<Player>> priceMap;
	private String formula;
	private MathValue<Player> earningsLimit;
	private boolean allowItemWithNoPrice;

	protected TextValue<Player> title;
	protected String[] layout;
	protected final Map<Character, LavaFishingItem> decorativeIcons;
	protected final ConcurrentMap<UUID, MarketGUI> marketGUICache;

	protected char itemSlot;
	protected char sellSlot;
	protected char sellAllSlot;

	protected LavaFishingItem sellIconAllowItem;
	protected LavaFishingItem sellIconDenyItem;
	protected LavaFishingItem sellIconLimitItem;
	protected LavaFishingItem sellAllIconAllowItem;
	protected LavaFishingItem sellAllIconDenyItem;
	protected LavaFishingItem sellAllIconLimitItem;
	protected Action<Player>[] sellDenyActions;
	protected Action<Player>[] sellAllowActions;
	protected Action<Player>[] sellLimitActions;
	protected Action<Player>[] sellAllDenyActions;
	protected Action<Player>[] sellAllAllowActions;
	protected Action<Player>[] sellAllLimitActions;

	private SchedulerTask resetEarningsTask;
	private int cachedDate;

	private boolean allowBundle;
	private boolean allowShulkerBox;

	public MarketManager(HellblockPlugin plugin) {
		this.instance = plugin;
		this.priceMap = new HashMap<>();
		this.decorativeIcons = new HashMap<>();
		this.marketGUICache = new ConcurrentHashMap<>();
		this.cachedDate = getRealTimeDate();
	}

	@Override
	public void load() {
		this.loadConfig();
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.resetEarningsTask = instance.getScheduler().asyncRepeating(() -> {
			int now = getRealTimeDate();
			if (this.cachedDate != now) {
				this.cachedDate = now;
				for (UserData userData : instance.getStorageManager().getOnlineUsers()) {
					userData.getEarningData().refresh();
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	private int getRealTimeDate() {
		Calendar calendar = Calendar.getInstance();
		return (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DATE);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.priceMap.clear();
		this.decorativeIcons.clear();
		if (this.resetEarningsTask != null)
			this.resetEarningsTask.cancel();
	}

	private void loadConfig() {
		Section config = instance.getConfigManager().getMainConfig().getSection("lava-fishing-options.market");

		this.formula = config.getString("price-formula", "{base} + {bonus} * {size}");
		this.layout = config.getStringList("layout").toArray(new String[0]);
		this.title = TextValue.auto(config.getString("title", "market.title"));
		this.itemSlot = config.getString("item-slot.symbol", "I").charAt(0);
		this.allowItemWithNoPrice = config.getBoolean("item-slot.allow-items-with-no-price", true);
		this.allowBundle = config.getBoolean("allow-bundle", true);
		this.allowShulkerBox = config.getBoolean("allow-shulker-box", true);

		Section sellAllSection = config.getSection("sell-all-icons");
		if (sellAllSection != null) {
			this.sellAllSlot = sellAllSection.getString("symbol", "S").charAt(0);

			this.sellAllIconAllowItem = new SingleItemParser("allow", sellAllSection.getSection("allow-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			this.sellAllIconDenyItem = new SingleItemParser("deny", sellAllSection.getSection("deny-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			this.sellAllIconLimitItem = new SingleItemParser("limit", sellAllSection.getSection("limit-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();

			this.sellAllAllowActions = instance.getActionManager()
					.parseActions(sellAllSection.getSection("allow-icon.action"));
			this.sellAllDenyActions = instance.getActionManager()
					.parseActions(sellAllSection.getSection("deny-icon.action"));
			this.sellAllLimitActions = instance.getActionManager()
					.parseActions(sellAllSection.getSection("limit-icon.action"));
		}

		Section sellSection = config.getSection("sell-icons");
		if (sellSection == null) {
			// for old config compatibility
			sellSection = config.getSection("functional-icons");
		}
		if (sellSection != null) {
			this.sellSlot = sellSection.getString("symbol", "B").charAt(0);

			this.sellIconAllowItem = new SingleItemParser("allow", sellSection.getSection("allow-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			this.sellIconDenyItem = new SingleItemParser("deny", sellSection.getSection("deny-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			this.sellIconLimitItem = new SingleItemParser("limit", sellSection.getSection("limit-icon"),
					instance.getConfigManager().getItemFormatFunctions()).getItem();

			this.sellAllowActions = instance.getActionManager()
					.parseActions(sellSection.getSection("allow-icon.action"));
			this.sellDenyActions = instance.getActionManager().parseActions(sellSection.getSection("deny-icon.action"));
			this.sellLimitActions = instance.getActionManager()
					.parseActions(sellSection.getSection("limit-icon.action"));
		}

		this.earningsLimit = config.getBoolean("limitation.enable", true)
				? MathValue.auto(config.getString("limitation.earnings", "10000"))
				: MathValue.plain(-1);

		// Load item prices from the configuration
		Section priceSection = config.getSection("item-price");
		if (priceSection != null) {
			for (Map.Entry<String, Object> entry : priceSection.getStringRouteMappedValues(false).entrySet()) {
				this.priceMap.put(entry.getKey(), MathValue.auto(entry.getValue()));
			}
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
	 * Open the market GUI for a player
	 *
	 * @param player player
	 */
	@Override
	public boolean openMarketGUI(Player player) {
		Optional<UserData> optionalUserData = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (optionalUserData.isEmpty()) {
			instance.getPluginLogger().warn("Player " + player.getName() + "'s market data has not been loaded yet.");
			return false;
		}
		Context<Player> context = Context.player(player);
		MarketGUI gui = new MarketGUI(this, context, optionalUserData.get().getEarningData());
		gui.addElement(new MarketGUIElement(itemSlot, new ItemStack(Material.AIR)));
		gui.addElement(new MarketDynamicGUIElement(sellSlot, new ItemStack(Material.AIR)));
		gui.addElement(new MarketDynamicGUIElement(sellAllSlot, new ItemStack(Material.AIR)));
		for (Map.Entry<Character, LavaFishingItem> entry : decorativeIcons.entrySet()) {
			gui.addElement(new MarketGUIElement(entry.getKey(), entry.getValue().build(context)));
		}
		gui.build().refresh().show();
		marketGUICache.put(player.getUniqueId(), gui);
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
		if (!(event.getInventory().getHolder() instanceof MarketGUIHolder))
			return;
		MarketGUI gui = marketGUICache.remove(player.getUniqueId());
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
		MarketGUI gui = marketGUICache.remove(event.getPlayer().getUniqueId());
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
		MarketGUI gui = marketGUICache.get(player.getUniqueId());
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

		// Check if the clicked inventory is a MarketGUI
		if (!(event.getInventory().getHolder() instanceof MarketGUIHolder))
			return;

		MarketGUI gui = marketGUICache.get(player.getUniqueId());
		if (gui == null) {
			event.setCancelled(true);
			player.closeInventory();
			return;
		}

		EarningData earningData = gui.earningData;
		earningData.refresh();
		double earningLimit = earningLimit(gui.context);

		if (clickedInv != player.getInventory()) {
			int slot = event.getSlot();
			MarketGUIElement element = gui.getElement(slot);
			if (element == null) {
				event.setCancelled(true);
				return;
			}

			if (element.getSymbol() == itemSlot) {
				if (!allowItemWithNoPrice) {
					if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
						ItemStack moved = player.getInventory().getItem(event.getHotbarButton());
						double price = getItemPrice(gui.context, moved);
						if (price <= 0) {
							event.setCancelled(true);
							return;
						}
					}
				}
			} else {
				event.setCancelled(true);
			}

			if (element.getSymbol() == sellSlot) {

				Pair<Integer, Double> pair = getItemsToSell(gui.context, gui.getItemsInGUI());
				double totalWorth = pair.right();
				gui.context.arg(ContextKeys.MONEY, money(totalWorth))
						.arg(ContextKeys.MONEY_FORMATTED, String.format("%.2f", totalWorth))
						.arg(ContextKeys.REST, money(earningLimit - earningData.getEarnings()))
						.arg(ContextKeys.REST_FORMATTED,
								String.format("%.2f", (earningLimit - earningData.getEarnings())))
						.arg(ContextKeys.SOLD_ITEM_AMOUNT, pair.left());

				if (totalWorth > 0) {
					if (earningLimit != -1 && (earningLimit - earningData.getEarnings()) < totalWorth) {
						// Can't earn more money
						ActionManagerInterface.trigger(gui.context, sellLimitActions);
					} else {
						// Clear items and update earnings
						clearWorthyItems(gui.context, gui.getItemsInGUI());
						earningData.setEarnings(earningData.getEarnings() + totalWorth);
						gui.context.arg(ContextKeys.REST, money(earningLimit - earningData.getEarnings()));
						gui.context.arg(ContextKeys.REST_FORMATTED,
								String.format("%.2f", (earningLimit - earningData.getEarnings())));
						ActionManagerInterface.trigger(gui.context, sellAllowActions);
					}
				} else {
					// Nothing to sell
					ActionManagerInterface.trigger(gui.context, sellDenyActions);
				}
			} else if (element.getSymbol() == sellAllSlot) {
				List<ItemStack> itemStacksToSell = storageContentsToList(
						gui.context.holder().getInventory().getStorageContents());
				Pair<Integer, Double> pair = getItemsToSell(gui.context, itemStacksToSell);
				double totalWorth = pair.right();
				gui.context.arg(ContextKeys.MONEY, money(totalWorth))
						.arg(ContextKeys.MONEY_FORMATTED, String.format("%.2f", totalWorth))
						.arg(ContextKeys.REST, money(earningLimit - earningData.getEarnings()))
						.arg(ContextKeys.REST_FORMATTED,
								String.format("%.2f", (earningLimit - earningData.getEarnings())))
						.arg(ContextKeys.SOLD_ITEM_AMOUNT, pair.left());

				if (totalWorth > 0) {
					if (earningLimit != -1 && (earningLimit - earningData.getEarnings()) < totalWorth) {
						// Can't earn more money
						ActionManagerInterface.trigger(gui.context, sellAllLimitActions);
					} else {
						// Clear items and update earnings
						clearWorthyItems(gui.context, itemStacksToSell);
						earningData.setEarnings(earningData.getEarnings() + totalWorth);
						gui.context.arg(ContextKeys.REST, money(earningLimit - earningData.getEarnings()));
						gui.context.arg(ContextKeys.REST_FORMATTED,
								String.format("%.2f", (earningLimit - earningData.getEarnings())));
						ActionManagerInterface.trigger(gui.context, sellAllAllowActions);
					}
				} else {
					// Nothing to sell
					ActionManagerInterface.trigger(gui.context, sellAllDenyActions);
				}
			}
		} else {
			// Handle interactions with the player's inventory
			ItemStack current = event.getCurrentItem();
			if (!allowItemWithNoPrice) {
				double price = getItemPrice(gui.context, current);
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
					ItemStack itemStack = gui.inventory.getItem(slot);
					if (itemStack != null && itemStack.getType() != Material.AIR) {
						if (current.getType() == itemStack.getType()
								&& itemStack.getAmount() != itemStack.getMaxStackSize()
								&& current.getItemMeta().equals(itemStack.getItemMeta())) {
							int left = itemStack.getMaxStackSize() - itemStack.getAmount();
							if (current.getAmount() <= left) {
								itemStack.setAmount(itemStack.getAmount() + current.getAmount());
								current.setAmount(0);
								break;
							} else {
								current.setAmount(current.getAmount() - left);
								itemStack.setAmount(itemStack.getMaxStackSize());
							}
						}
					} else {
						gui.inventory.setItem(slot, current.clone());
						current.setAmount(0);
						break;
					}
				}
			}
		}

		// Refresh the GUI
		instance.getScheduler().sync().runLater(gui::refresh, 1, player.getLocation());
	}

	@Override
	public double getItemPrice(Context<Player> context, ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return 0;

		Item<ItemStack> wrapped = ((BukkitItemFactory) instance.getItemManager().getFactory()).wrap(itemStack);
		double price = (double) wrapped.getTag("Price").orElse(0d);
		if (price != 0) {
			// If a custom price is defined in the ItemStack's NBT data, use it.
			return price * itemStack.getAmount();
		}

		if (allowBundle && itemStack.getItemMeta() instanceof BundleMeta bundleMeta) {
			Pair<Integer, Double> pair = getItemsToSell(context, bundleMeta.getItems());
			return pair.right();
		}

		if (allowShulkerBox && itemStack.getItemMeta() instanceof BlockStateMeta stateMeta) {
			if (stateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
				Pair<Integer, Double> pair = getItemsToSell(context, Arrays
						.stream(shulkerBox.getInventory().getStorageContents()).filter(Objects::nonNull).toList());
				return pair.right();
			}
		}

		// If no custom price is defined, attempt to fetch the price from a predefined
		// price map.
		String itemID = itemStack.getType().name();
		Optional<Integer> optionalCMD = wrapped.customModelData();
		if (optionalCMD.isPresent()) {
			itemID = itemID + ":" + optionalCMD.get();
		}

		MathValue<Player> formula = priceMap.get(itemID);
		if (formula == null)
			return 0;

		return formula.evaluate(context) * itemStack.getAmount();
	}

	@Override
	public String getFormula() {
		return formula;
	}

	@Override
	public double earningLimit(Context<Player> context) {
		return earningsLimit.evaluate(context);
	}

	public Pair<Integer, Double> getItemsToSell(Context<Player> context, List<ItemStack> itemStacks) {
		int amount = 0;
		double worth = 0d;
		for (ItemStack itemStack : itemStacks) {
			double price = getItemPrice(context, itemStack);
			if (price > 0 && itemStack != null) {
				amount += itemStack.getAmount();
				worth += price;
			}
		}
		return Pair.of(amount, worth);
	}

	public void clearWorthyItems(Context<Player> context, List<ItemStack> itemStacks) {
		for (ItemStack itemStack : itemStacks) {
			double price = getItemPrice(context, itemStack);
			if (price > 0 && itemStack != null) {
				if (allowBundle && itemStack.getItemMeta() instanceof BundleMeta bundleMeta) {
					clearWorthyItems(context, bundleMeta.getItems());
					List<ItemStack> newItems = new ArrayList<>(bundleMeta.getItems());
					newItems.removeIf(item -> {
						return item.getAmount() == 0 || item.getType() == Material.AIR;
					});
					bundleMeta.setItems(newItems);
					itemStack.setItemMeta(bundleMeta);
					continue;
				}
				if (allowShulkerBox && itemStack.getItemMeta() instanceof BlockStateMeta stateMeta) {
					if (stateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
						clearWorthyItems(context, Arrays.stream(shulkerBox.getInventory().getStorageContents())
								.filter(Objects::nonNull).toList());
						stateMeta.setBlockState(shulkerBox);
						itemStack.setItemMeta(stateMeta);
						continue;
					}
				}
				itemStack.setAmount(0);
			}
		}
	}

	protected String money(double money) {
		String str = String.format("%.2f", money);
		return str.replace(",", ".");
	}

	protected List<ItemStack> storageContentsToList(ItemStack[] itemStacks) {
		List<ItemStack> list = new ArrayList<>();
		if (itemStacks != null) {
			for (ItemStack itemStack : itemStacks) {
				if (itemStack != null && itemStack.getType() != Material.AIR) {
					list.add(itemStack);
				}
			}
		}
		return list;
	}
}