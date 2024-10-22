package com.swiftlicious.hellblock.creation.item;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.effects.EffectCarrier;
import com.swiftlicious.hellblock.events.fishing.FishingLootPreSpawnEvent;
import com.swiftlicious.hellblock.events.fishing.FishingLootSpawnEvent;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.NBTUtils;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Tuple;
import com.swiftlicious.hellblock.utils.extras.Value;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

public class ItemManager implements ItemManagerInterface, Listener {

	private final HellblockPlugin instance;
	private final Map<Key, BuildableItem> buildableItemMap;
	private final Map<String, ItemLibrary> itemLibraryMap;
	private ItemLibrary[] itemDetectionArray;

	public ItemManager(HellblockPlugin plugin) {
		instance = plugin;
		this.itemLibraryMap = new LinkedHashMap<>();
		this.buildableItemMap = new HashMap<>();
		this.registerItemLibrary(new LavaFishingItem());
		this.registerItemLibrary(new VanillaItem());
	}

	public void load() {
		this.loadItemsFromPluginFolder();
		Bukkit.getPluginManager().registerEvents(this, instance);
		this.resetItemDetectionOrder();
	}

	public void unload() {
		HandlerList.unregisterAll(this);
		Map<Key, BuildableItem> tempMap = new HashMap<>(this.buildableItemMap);
		this.buildableItemMap.clear();
		for (Map.Entry<Key, BuildableItem> entry : tempMap.entrySet()) {
			if (entry.getValue().persist()) {
				tempMap.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private void resetItemDetectionOrder() {
		List<ItemLibrary> list = new ArrayList<>();
		for (String plugin : HBConfig.itemDetectOrder) {
			ItemLibrary library = itemLibraryMap.get(plugin);
			if (library != null) {
				list.add(library);
			}
		}
		this.itemDetectionArray = list.toArray(new ItemLibrary[0]);
	}

	public Collection<String> getItemLibraries() {
		return itemLibraryMap.keySet();
	}

	/**
	 * Get a set of all item keys for lava fishing.
	 *
	 * @return A set of item keys.
	 */
	@Override
	public Set<Key> getAllItemsKey() {
		return buildableItemMap.keySet();
	}

	public void disable() {
		HandlerList.unregisterAll(this);
		this.buildableItemMap.clear();
		this.itemLibraryMap.clear();
	}

	/**
	 * Loads items from the plugin folder. This method scans various item types
	 * (item, bait, rod, util, hook) in the plugin's content folder and loads their
	 * configurations.
	 */
	public void loadItemsFromPluginFolder() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (String type : List.of("item", "bait", "rod", "util", "hook")) {
			File typeFolder = new File(instance.getDataFolder() + File.separator + "contents" + File.separator + type);
			if (!typeFolder.exists()) {
				if (!typeFolder.mkdirs())
					return;
				instance.saveResource("contents" + File.separator + type + File.separator + "default.yml", false);
			}
			fileDeque.push(typeFolder);
			while (!fileDeque.isEmpty()) {
				File file = fileDeque.pop();
				File[] files = file.listFiles();
				if (files == null)
					continue;
				for (File subFile : files) {
					if (subFile.isDirectory()) {
						fileDeque.push(subFile);
					} else if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
						this.loadSingleFile(subFile, type);
					}
				}
			}
		}
	}

	/**
	 * Loads a single item configuration file.
	 *
	 * @param file      The YAML configuration file to load.
	 * @param namespace The namespace of the item type (item, bait, rod, util,
	 *                  hook).
	 */
	private void loadSingleFile(File file, String namespace) {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		for (Map.Entry<String, Object> entry : yaml.getValues(false).entrySet()) {
			String value = entry.getKey();
			if (entry.getValue() instanceof ConfigurationSection section) {
				Key key = Key.of(namespace, value);
				if (buildableItemMap.containsKey(key)) {
					LogUtils.severe(String.format("Duplicated item key found: %s.", key));
					continue;
				} else {
					buildableItemMap.put(key, getItemBuilder(section, namespace, value));
				}
			}
		}
	}

	/**
	 * Build an ItemStack with a specified namespace and value for a player.
	 *
	 * @param player    The player for whom the ItemStack is being built.
	 * @param namespace The namespace of the item.
	 * @param value     The value of the item.
	 * @return The constructed ItemStack.
	 */
	@Override
	public ItemStack build(Player player, String namespace, String value) {
		return build(player, namespace, value, new HashMap<>());
	}

	/**
	 * Build an ItemStack with a specified namespace and value, replacing
	 * placeholders, for a player.
	 *
	 * @param player       The player for whom the ItemStack is being built.
	 * @param namespace    The namespace of the item.
	 * @param value        The value of the item.
	 * @param placeholders The placeholders to replace in the item's attributes.
	 * @return The constructed ItemStack, or null if the item doesn't exist.
	 */
	@Override
	public ItemStack build(Player player, String namespace, String value, Map<String, String> placeholders) {
		BuildableItem buildableItem = buildableItemMap.get(Key.of(namespace, value));
		if (buildableItem == null)
			return null;
		return buildableItem.build(player, placeholders);
	}

	/**
	 * Build an ItemStack using an ItemBuilder for a player.
	 *
	 * @param player  The player for whom the ItemStack is being built.
	 * @param builder The ItemBuilder used to construct the ItemStack.
	 * @return The constructed ItemStack.
	 */
	@NotNull
	@Override
	public ItemStack build(Player player, ItemBuilder builder) {
		return build(player, builder, new HashMap<>());
	}

	/**
	 * Retrieve a BuildableItem by its namespace and value.
	 *
	 * @param namespace The namespace of the BuildableItem.
	 * @param value     The value of the BuildableItem.
	 * @return The BuildableItem with the specified namespace and value, or null if
	 *         not found.
	 */
	@Override
	@Nullable
	public BuildableItem getBuildableItem(String namespace, String value) {
		return buildableItemMap.get(Key.of(namespace, value));
	}

	/**
	 * Get the item ID associated with the given ItemStack by checking all available
	 * item libraries. The detection order is determined by the configuration.
	 *
	 * @param itemStack The ItemStack to retrieve the item ID from.
	 * @return The item ID or "AIR" if not found or if the ItemStack is null or
	 *         empty.
	 */
	@NotNull
	@Override
	public String getAnyPluginItemID(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return "AIR";
		for (ItemLibrary library : this.itemDetectionArray) {
			String id = library.getItemID(itemStack);
			if (id != null)
				return id;
		}
		// should not reach this because vanilla library would always work
		return "AIR";
	}

	/**
	 * Build an ItemStack for a player based on the provided item ID.
	 *
	 * @param player The player for whom the ItemStack is being built.
	 * @param id     The item ID, which may include a namespace (e.g.,
	 *               "namespace:id").
	 * @return The constructed ItemStack or null if the ID is not valid.
	 */
	@Override
	public ItemStack buildAnyPluginItemByID(Player player, String id) {
		if (id.contains(":")) {
			String[] split = id.split(":", 2);
			return itemLibraryMap.get(split[0]).buildItem(player, split[1]);
		} else {
			try {
				return new ItemStack(Material.valueOf(id.toUpperCase(Locale.ENGLISH)));
			} catch (IllegalArgumentException e) {
				return new ItemStack(Material.COD);
			}
		}
	}

	/**
	 * Checks if the provided ItemStack is a lava fishing item.
	 *
	 * @param itemStack The ItemStack to check.
	 * @return True if the ItemStack is a lava fishing item; otherwise, false.
	 */
	@Override
	public boolean isLavaFishingItem(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return false;
		return instance.getFishingManager().checkFishingID(itemStack)
				&& (!instance.getFishingManager().getFishingID(itemStack).isEmpty());
	}

	/**
	 * Get the item ID associated with the given ItemStack, if available.
	 *
	 * @param itemStack The ItemStack to retrieve the item ID from.
	 * @return The item ID or null if not found or if the ItemStack is null or
	 *         empty.
	 */
	@Nullable
	@Override
	public String getLavaFishingItemID(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return null;
		if (!isLavaFishingItem(itemStack))
			return null;
		return (String) instance.getFishingManager().getFishingID(itemStack);
	}

	/**
	 * Create a HBBuilder instance for an item configuration section
	 *
	 * @param section The configuration section containing item settings.
	 * @param type    The type of the item (e.g., "rod", "bait").
	 * @param id      The unique identifier for the item.
	 * @return A HBBuilder instance representing the configured item, or null if the
	 *         section is null.
	 */
	@Nullable
	@Override
	public HBBuilder getItemBuilder(ConfigurationSection section, String type, String id) {
		if (section == null)
			return null;
		String material = section.getString("material", type.equals("rod") ? "FISHING_ROD" : "PAPER");
		HBBuilder itemHBBuilder;
		if (material.contains(":")) {
			String[] split = material.split(":", 2);
			itemHBBuilder = HBBuilder.of(split[0], split[1]);
		} else {
			itemHBBuilder = HBBuilder.of("vanilla", material.toUpperCase(Locale.ENGLISH));
		}
		itemHBBuilder.stackable(section.getBoolean("stackable", true))
				.size(instance.getConfigUtils().getFloatPair(section.getString("size")))
				.price((float) section.getDouble("price.base"), (float) section.getDouble("price.bonus"))
				.customModelData(section.getInt("custom-model-data")).maxDurability(section.getInt("max-durability"))
				.enchantment(
						instance.getConfigUtils().getEnchantmentPair(section.getConfigurationSection("enchantments")),
						false)
				.enchantment(instance.getConfigUtils()
						.getEnchantmentPair(section.getConfigurationSection("stored-enchantments")), true)
				.enchantmentPool(
						instance.getConfigUtils()
								.getEnchantAmountPair(section.getConfigurationSection("enchantment-pool.amount")),
						instance.getConfigUtils()
								.getEnchantPoolPair(section.getConfigurationSection("enchantment-pool.pool")),
						false)
				.enchantmentPool(
						instance.getConfigUtils().getEnchantAmountPair(
								section.getConfigurationSection("stored-enchantment-pool.amount")),
						instance.getConfigUtils().getEnchantPoolPair(
								section.getConfigurationSection("stored-enchantment-pool.pool")),
						true)
				.randomEnchantments(instance.getConfigUtils()
						.getEnchantmentTuple(section.getConfigurationSection("random-enchantments")), false)
				.randomEnchantments(instance.getConfigUtils()
						.getEnchantmentTuple(section.getConfigurationSection("random-stored-enchantments")), true)
				.tag(section.getBoolean("tag", true), type, id)
				.randomDamage(section.getBoolean("random-durability", false))
				.unbreakable(section.getBoolean("unbreakable", false))
				.preventGrabbing(section.getBoolean("prevent-grabbing", true)).head(section.getString("head64"))
				.name(section.getString("display.name")).lore(section.getStringList("display.lore"));
		if (section.get("amount") instanceof String s) {
			Pair<Integer, Integer> pair = instance.getConfigUtils().getIntegerPair(s);
			itemHBBuilder.amount(pair.left(), pair.right());
		} else {
			itemHBBuilder.amount(section.getInt("amount", 1));
		}
		return itemHBBuilder;
	}

	/**
	 * Build an ItemStack using the provided ItemBuilder, player, and placeholders.
	 *
	 * @param player       The player for whom the item is being built.
	 * @param builder      The ItemBuilder that defines the item's properties.
	 * @param placeholders A map of placeholders and their corresponding values to
	 *                     be applied to the item.
	 * @return The constructed ItemStack.
	 */
	@Override
	@NotNull
	public ItemStack build(Player player, ItemBuilder builder, Map<String, String> placeholders) {
		ItemStack temp = itemLibraryMap.get(builder.getLibrary()).buildItem(player, builder.getId());
		if (temp.getType() == Material.AIR) {
			return temp;
		}
		int amount = builder.getAmount();
		temp.setAmount(amount);
		for (ItemBuilder.ItemPropertyEditor editor : builder.getEditors()) {
			editor.edit(player, temp, placeholders);
		}
		RtagItem tagItem = ItemUtils.updateNBTItemLore(new RtagItem(temp));
		temp.setItemMeta(tagItem.getItem().getItemMeta());
		return tagItem.load();
	}

	@Override
	public ItemStack getItemStackAppearance(Player player, String material) {
		if (material != null) {
			ItemStack itemStack = buildAnyPluginItemByID(player, material);
			if (itemStack != null) {
				NBTUtils.removeNBTItemComponentData(itemStack, "custom_name");
				NBTUtils.removeNBTItemComponentData(itemStack, "lore");
				return itemStack;
			} else {
				return new ItemStack(Material.BARRIER);
			}
		} else {
			return new ItemStack(Material.STRUCTURE_VOID);
		}
	}

	/**
	 * Register an item library.
	 *
	 * @param itemLibrary The item library to register.
	 * @return True if the item library was successfully registered, false if it
	 *         already exists.
	 */
	@Override
	public boolean registerItemLibrary(ItemLibrary itemLibrary) {
		if (itemLibraryMap.containsKey(itemLibrary.identification()))
			return false;
		itemLibraryMap.put(itemLibrary.identification(), itemLibrary);
		this.resetItemDetectionOrder();
		return true;
	}

	/**
	 * Unregister an item library.
	 *
	 * @param identification The item library to unregister.
	 * @return True if the item library was successfully unregistered, false if it
	 *         doesn't exist.
	 */
	@Override
	public boolean unRegisterItemLibrary(String identification) {
		boolean success = itemLibraryMap.remove(identification) != null;
		if (success)
			this.resetItemDetectionOrder();
		return success;
	}

	@Override
	public void dropItem(Player player, Location hookLocation, Location playerLocation, ItemStack item,
			Condition condition) {
		if (item.getType() == Material.AIR) {
			return;
		}

		FishingLootPreSpawnEvent preSpawnEvent = new FishingLootPreSpawnEvent(player, hookLocation, item);
		Bukkit.getPluginManager().callEvent(preSpawnEvent);
		if (preSpawnEvent.isCancelled()) {
			return;
		}

		Item itemEntity = hookLocation.getWorld().dropItem(hookLocation, item);
		FishingLootSpawnEvent spawnEvent = new FishingLootSpawnEvent(player, hookLocation, itemEntity);
		Bukkit.getPluginManager().callEvent(spawnEvent);
		if (spawnEvent.isCancelled()) {
			itemEntity.remove();
			return;
		}

		itemEntity.setInvulnerable(true);
		instance.getScheduler().runTaskAsyncLater(() -> {
			if (itemEntity.isValid()) {
				itemEntity.setInvulnerable(false);
			}
		}, 1, TimeUnit.SECONDS);

		Vector vector = playerLocation.subtract(hookLocation).toVector().multiply(0.105);
		vector = vector.setY((vector.getY() + 0.22) * 1.18);
		itemEntity.setVelocity(vector);
	}

	/**
	 * Decreases the durability of an ItemStack by a specified amount and optionally
	 * updates its lore.
	 *
	 * @param player     Player
	 * @param itemStack  The ItemStack to modify.
	 * @param amount     The amount by which to decrease the durability.
	 * @param updateLore Whether to update the lore of the ItemStack.
	 */
	@Override
	public void decreaseDurability(Player player, ItemStack itemStack, int amount, boolean updateLore) {
		ItemUtils.decreaseDurability(player, itemStack, amount, updateLore);
	}

	/**
	 * Increases the durability of an ItemStack by a specified amount and optionally
	 * updates its lore.
	 *
	 * @param itemStack  The ItemStack to modify.
	 * @param amount     The amount by which to increase the durability.
	 * @param updateLore Whether to update the lore of the ItemStack.
	 */
	@Override
	public void increaseDurability(ItemStack itemStack, int amount, boolean updateLore) {
		ItemUtils.increaseDurability(itemStack, amount, updateLore);
	}

	/**
	 * Sets the durability of an ItemStack to a specific amount and optionally
	 * updates its lore.
	 *
	 * @param itemStack  The ItemStack to modify.
	 * @param amount     The new durability value.
	 * @param updateLore Whether to update the lore of the ItemStack.
	 */
	@Override
	public void setDurability(ItemStack itemStack, int amount, boolean updateLore) {
		ItemUtils.setDurability(itemStack, amount, updateLore);
	}

	public static class HBBuilder implements ItemBuilder {

		private final String library;
		private final String id;
		private int minAmount;
		private int maxAmount;
		private final LinkedHashMap<String, ItemPropertyEditor> editors;

		public HBBuilder(String library, String id) {
			this.id = id;
			this.library = library;
			this.editors = new LinkedHashMap<>();
			this.minAmount = (maxAmount = 1);
		}

		public static HBBuilder of(String library, String id) {
			return new HBBuilder(library, id);
		}

		@Override
		public ItemStack build(Player player, Map<String, String> placeholders) {
			return HellblockPlugin.getInstance().getItemManager().build(player, this, placeholders);
		}

		@Override
		public boolean persist() {
			return false;
		}

		@Override
		public ItemBuilder customModelData(int value) {
			if (value == 0)
				return this;
			editors.put("custom-model-data", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				tagItem.setComponent("minecraft:custom_model_data", value);
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder name(String name) {
			if (name == null)
				return this;
			editors.put("name", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				tagItem.setComponent("minecraft:custom_name",
						HellblockPlugin.getInstance().getAdventureManager()
								.componentToJson(HellblockPlugin.getInstance().getAdventureManager()
										.getComponentFromMiniMessage("<!i>" + HellblockPlugin.getInstance()
												.getPlaceholderManager().parse(player, name, placeholders))));
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder amount(int amount) {
			this.minAmount = amount;
			this.maxAmount = amount;
			return this;
		}

		@Override
		public ItemBuilder amount(int minAmount, int maxAmount) {
			this.minAmount = minAmount;
			this.maxAmount = maxAmount;
			return this;
		}

		@Override
		public ItemBuilder tag(boolean tag, String type, String id) {
			editors.put("tag", (player, item, placeholders) -> {
				if (!tag)
					return;
				item = HellblockPlugin.getInstance().getFishingManager().setFishingType(item, type);
				item = HellblockPlugin.getInstance().getFishingManager().setFishingID(item, id);
			});
			return this;
		}

		@Override
		public ItemBuilder unbreakable(boolean unbreakable) {
			editors.put("unbreakable", (player, item, placeholders) -> {
				if (!unbreakable)
					return;
				RtagItem tagItem = new RtagItem(item);
				tagItem.setComponent("minecraft:unbreakable", unbreakable);
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder lore(List<String> lore) {
			if (lore.size() == 0)
				return this;
			editors.put("lore", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				tagItem.setComponent("minecraft:lore", lore
						.stream().map(
								s -> HellblockPlugin.getInstance().getAdventureManager()
										.componentToJson(HellblockPlugin.getInstance().getAdventureManager()
												.getComponentFromMiniMessage("<!i>" + HellblockPlugin.getInstance()
														.getPlaceholderManager().parse(player, s, placeholders))))
						.toList());
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder enchantment(List<Pair<String, Short>> enchantments, boolean store) {
			if (enchantments.size() == 0)
				return this;
			editors.put("enchantment", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				Map<String, Integer> enchants = new HashMap<>();
				for (Pair<String, Short> pair : enchantments) {
					enchants.put(pair.left(), Integer.valueOf(pair.right()));
				}
				tagItem.setComponent(store ? "minecraft:stored_enchantments" : "minecraft:enchantments", enchants);
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder randomEnchantments(List<Tuple<Double, String, Short>> enchantments, boolean store) {
			if (enchantments.size() == 0)
				return this;
			editors.put("random-enchantment", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				HashSet<String> ids = new HashSet<>();
				Map<String, Integer> enchants = new HashMap<>();
				for (Tuple<Double, String, Short> pair : enchantments) {
					if (Math.random() < pair.left() && !ids.contains(pair.mid())) {
						enchants.put(pair.mid(), Integer.valueOf(pair.right()));
						ids.add(pair.mid());
					}
				}
				tagItem.setComponent(store ? "minecraft:stored_enchantments" : "minecraft:enchantments", enchants);
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder enchantmentPool(List<Pair<Integer, Value>> amountPairs,
				List<Pair<Pair<String, Short>, Value>> enchantments, boolean store) {
			if (enchantments.size() == 0 || amountPairs.size() == 0)
				return this;
			editors.put("enchantment-pool", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				List<Pair<Integer, Double>> parsedAmountPair = new ArrayList<>(amountPairs.size());
				Map<String, String> map = new HashMap<>();
				for (Pair<Integer, Value> rawValue : amountPairs) {
					parsedAmountPair.add(Pair.of(rawValue.left(), rawValue.right().get(player, map)));
				}

				int amount = HellblockPlugin.getInstance().getWeightUtils().getRandom(parsedAmountPair);
				if (amount <= 0)
					return;

				HashSet<Enchantment> addedEnchantments = new HashSet<>();
				Map<String, Integer> enchants = new HashMap<>();

				List<Pair<Pair<String, Short>, Double>> cloned = new ArrayList<>(enchantments.size());
				for (Pair<Pair<String, Short>, Value> rawValue : enchantments) {
					cloned.add(Pair.of(rawValue.left(), rawValue.right().get(player, map)));
				}

				// Fetch the enchantment registry from the registry access
				final Registry<Enchantment> enchantmentRegistry = RegistryAccess.registryAccess()
						.getRegistry(RegistryKey.ENCHANTMENT);

				int i = 0;
				outer: while (i < amount && cloned.size() != 0) {
					Pair<String, Short> enchantPair = HellblockPlugin.getInstance().getWeightUtils().getRandom(cloned);

					// Get the sharpness enchantment using its key.
					// getOrThrow may be replaced with get if the registry may not contain said
					// value
					Enchantment enchantment = enchantmentRegistry
							.getOrThrow(NamespacedKey.fromString(enchantPair.left().toLowerCase()));

					if (enchantment == null) {
						throw new NullPointerException(
								"Enchantment: " + enchantPair.left() + " doesn't exist on your server.");
					}
					for (Enchantment added : addedEnchantments) {
						if (enchantment.conflictsWith(added)) {
							cloned.removeIf(pair -> pair.left().left().equals(enchantPair.left()));
							continue outer;
						}
					}
					addedEnchantments.add(enchantment);
					cloned.removeIf(pair -> pair.left().left().equals(enchantPair.left()));
					enchants.put(enchantPair.left(), Integer.valueOf(enchantPair.right()));
					i++;
				}
				tagItem.setComponent(store ? "minecraft:stored_enchantments" : "minecraft:enchantments", enchants);
				tagItem.load();
				tagItem.update();
			});
			return this;
		}

		@Override
		public ItemBuilder maxDurability(int max) {
			if (max == 0)
				return this;
			editors.put("durability", (player, item, placeholders) -> {
				item = HellblockPlugin.getInstance().getFishingManager().setMaxDurability(item, max);
				item = HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(item, max);
			});
			return this;
		}

		@Override
		public ItemBuilder price(float base, float bonus) {
			if (base == 0 && bonus == 0)
				return this;
			editors.put("price", (player, item, placeholders) -> {
				placeholders.put("{base}", String.format("%.2f", base));
				placeholders.put("{BASE}", String.valueOf(base));
				placeholders.put("{bonus}", String.format("%.2f", bonus));
				placeholders.put("{BONUS}", String.valueOf(bonus));
				double price;
				price = HellblockPlugin.getInstance().getMarketManager().getFishPrice(player, placeholders);
				item = HellblockPlugin.getInstance().getMarketManager().setMarketPrice(item, price);
				placeholders.put("{price}", String.format("%.2f", price));
				placeholders.put("{PRICE}", String.valueOf(price));
			});
			return this;
		}

		@Override
		public ItemBuilder size(Pair<Float, Float> size) {
			if (size == null)
				return this;
			editors.put("size", (player, item, placeholders) -> {
				float random = size.left() + (size.left() >= size.right() ? 0
						: ThreadLocalRandom.current().nextFloat(size.right() - size.left()));
				float bonus = Float.parseFloat(placeholders.getOrDefault("{size-multiplier}", "1.0"));
				double fixed = Double.parseDouble(placeholders.getOrDefault("{size-fixed}", "0.0"));
				random *= bonus;
				random += fixed;
				if (HBConfig.restrictedSizeRange) {
					if (random > size.right()) {
						random = size.right();
					} else if (random < size.left()) {
						random = size.left();
					}
				}
				item = HellblockPlugin.getInstance().getMarketManager().setSize(item, random);
				placeholders.put("{size}", String.format("%.2f", random));
				placeholders.put("{SIZE}", String.valueOf(random));
				placeholders.put("{min_size}", String.valueOf(size.left()));
				placeholders.put("{max_size}", String.valueOf(size.right()));
			});
			return this;
		}

		@Override
		public ItemBuilder stackable(boolean stackable) {
			if (stackable)
				return this;
			editors.put("stackable", (player, item, placeholders) -> {
				item = HellblockPlugin.getInstance().getMarketManager().setUUID(item, UUID.randomUUID());
			});
			return this;
		}

		@Override
		public ItemBuilder preventGrabbing(boolean prevent) {
			if (!prevent)
				return this;
			editors.put("grabbing", (player, item, placeholders) -> {
				item = ItemUtils.setOwnerData(item, placeholders.get("player"));
			});
			return this;
		}

		@Override
		public ItemBuilder head(String base64) {
			if (base64 == null)
				return this;
			return (ItemBuilder) editors.put("head", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				final Map<String, Object> profile = Map.of("minecraft:profile",
						List.of(Map.of("id", UUID.nameUUIDFromBytes(id.getBytes())),
								Map.of("properties", List.of(Map.of("value", base64)))));
				tagItem.setComponent(profile);
				tagItem.load();
				tagItem.update();
			});
		}

		@Override
		public ItemBuilder randomDamage(boolean damage) {
			if (!damage)
				return this;
			editors.put("damage", (player, item, placeholders) -> {
				RtagItem tagItem = new RtagItem(item);
				if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(tagItem.getItem())) {
					int i = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(tagItem.getItem());
					if (i != 0) {
						int dur = ThreadLocalRandom.current().nextInt(i);
						item = HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(tagItem.getItem(),
								dur);
						tagItem.setComponent("minecraft:damage",
								(short) (tagItem.getItem().getType().getMaxDurability() * ((double) dur / i)));
						tagItem.load();
						tagItem.update();
					} else {
						tagItem.setComponent("minecraft:damage", (short) ThreadLocalRandom.current()
								.nextInt(tagItem.getItem().getType().getMaxDurability()));
						tagItem.load();
						tagItem.update();
					}
				} else {
					tagItem.setComponent("minecraft:damage", (short) ThreadLocalRandom.current()
							.nextInt(tagItem.getItem().getType().getMaxDurability()));
					tagItem.load();
					tagItem.update();
				}
			});
			return this;
		}

		@Override
		public @NotNull String getId() {
			return id;
		}

		@Override
		public @NotNull String getLibrary() {
			return library;
		}

		@Override
		public int getAmount() {
			return ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
		}

		@Override
		public Collection<ItemPropertyEditor> getEditors() {
			return editors.values();
		}

		@Override
		public ItemBuilder removeEditor(String type) {
			editors.remove(type);
			return this;
		}

		@Override
		public ItemBuilder registerCustomEditor(String type, ItemPropertyEditor editor) {
			editors.put(type, editor);
			return this;
		}
	}

	/**
	 * Handles item pickup by players.
	 *
	 * @param event The PlayerAttemptPickupEvent.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPickUp(PlayerAttemptPickupItemEvent event) {
		if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Player player = event.getPlayer();
		ItemStack itemStack = event.getItem().getItemStack();
		RtagItem tagItem = new RtagItem(itemStack);
		if (!ItemUtils.checkOwnerData(itemStack))
			return;
		if (!Objects.equals(ItemUtils.getOwnerData(itemStack), player.getName())) {
			event.setCancelled(true);
		} else {
			ItemUtils.removeOwnerData(itemStack);
			itemStack.setItemMeta(tagItem.getItem().getItemMeta());
		}
	}

	/**
	 * Handles item movement in inventories.
	 *
	 * @param event The InventoryPickupItemEvent.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInvPickItem(InventoryPickupItemEvent event) {
		ItemStack itemStack = event.getItem().getItemStack();
		RtagItem tagItem = new RtagItem(itemStack);
		if (!ItemUtils.checkOwnerData(itemStack))
			return;
		ItemUtils.removeOwnerData(itemStack);
		itemStack.setItemMeta(tagItem.getItem().getItemMeta());
	}

	/**
	 * Handles item consumption by players.
	 *
	 * @param event The PlayerItemConsumeEvent.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onConsumeItem(PlayerItemConsumeEvent event) {
		if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		ItemStack itemStack = event.getItem();
		String id = getAnyPluginItemID(itemStack);
		if (id.equalsIgnoreCase("AIR"))
			return;
		Loot loot = instance.getLootManager().getLoot(id);
		if (loot != null) {
			Condition condition = new Condition(event.getPlayer());
			if (!loot.disableGlobalAction())
				instance.getGlobalSettings().triggerLootActions(ActionTrigger.CONSUME, condition);
			loot.triggerActions(ActionTrigger.CONSUME, condition);
		}
	}

	/**
	 * Handles the repair of custom items in an anvil.
	 *
	 * @param event The PrepareAnvilEvent.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onRepairItem(PrepareAnvilEvent event) {
		ItemStack result = event.getInventory().getItem(2);
		if (result == null || result.getType() == Material.AIR)
			return;
		if (!isLavaFishingItem(result))
			return;
		if (!instance.getFishingManager().checkMaxDurability(result))
			return;
		if (!(result.getItemMeta() instanceof Damageable damageable)) {
			return;
		}
		int maxDur = instance.getFishingManager().getMaxDurability(result);
		result = instance.getFishingManager().setCurrentDurability(result,
				(int) (maxDur * (1 - (double) damageable.getDamage() / result.getType().getMaxDurability())));
		event.setResult(result);
	}

	/**
	 * Handles the mending of custom items.
	 *
	 * @param event The PlayerItemMendEvent.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onMending(PlayerItemMendEvent event) {
		if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		ItemStack itemStack = event.getItem();
		if (!isLavaFishingItem(itemStack))
			return;
		event.setCancelled(true);
		ItemUtils.increaseDurability(itemStack, event.getRepairAmount(), true);
	}

	/**
	 * Handles interactions with custom utility items.
	 *
	 * @param event The PlayerInteractEvent.
	 */
	@EventHandler
	public void onInteractWithItems(PlayerInteractEvent event) {
		if (!event.getPlayer().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY)
			return;
		if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND)
			return;
		ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
		if (itemStack.getType() == Material.AIR)
			return;
		if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;
		String id = getAnyPluginItemID(itemStack);
		if (id.equalsIgnoreCase("AIR"))
			return;
		Condition condition = new Condition(event.getPlayer());

		Loot loot = instance.getLootManager().getLoot(id);
		if (loot != null) {
			loot.triggerActions(ActionTrigger.INTERACT, condition);
			return;
		}

		// because the id can be from other plugins, so we can't infer the type of the
		// item
		for (String type : List.of("util", "bait", "hook")) {
			EffectCarrier carrier = instance.getEffectManager().getEffectCarrier(type, id);
			if (carrier != null) {
				Action[] actions = carrier.getActions(ActionTrigger.INTERACT);
				if (actions != null)
					for (Action action : actions) {
						action.trigger(condition);
					}
				break;
			}
		}
	}
}
