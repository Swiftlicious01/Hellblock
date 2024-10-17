package com.swiftlicious.hellblock.listeners.fishing;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.effects.EffectCarrier;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.Condition;

public class HookManager implements Listener, HookManagerInterface {

	private final HellblockPlugin instance;
	private final HashMap<String, HookSetting> hookSettingMap;

	public HookManager(HellblockPlugin plugin) {
		instance = plugin;
		this.hookSettingMap = new HashMap<>();
	}

	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		loadConfig();
	}

	public void unload() {
		HandlerList.unregisterAll(this);
		hookSettingMap.clear();
	}

	public void disable() {
		unload();
	}

	public boolean checkHookID(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("HellFishing", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 0);
			for (String key : map.keySet()) {
				if (key.equals("hook_id")) {
					data = true;
				}
			}
		}
		return data;
	}

	public @Nullable String getHookID(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 0) == null)
			return null;

		String data = null;
		if (tag.get("HellFishing", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 0);
			for (String key : map.keySet()) {
				if (key.equals("hook_id")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							data = (String) value;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkHookItem(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 1) == null)
			return false;

		boolean data = false;
		if (tag.get("HellFishing", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 1);
			for (String key : map.keySet()) {
				if (key.equals("hook_item")) {
					data = true;
				}
			}
		}
		return data;
	}

	public @Nullable ItemStack getHookItem(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 1) == null)
			return null;

		ItemStack data = null;
		if (tag.get("HellFishing", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 1);
			for (String key : map.keySet()) {
				if (key.equals("hook_item")) {
					for (Object value : map.values()) {
						if (value instanceof ItemStack) {
							data = (ItemStack) value;
						}
					}
				}
			}
		}
		return data;
	}

	public boolean checkHookDurability(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 2) == null)
			return false;

		boolean data = false;
		if (tag.get("HellFishing", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 2);
			for (String key : map.keySet()) {
				if (key.equals("hook_dur")) {
					data = true;
				}
			}
		}
		return data;
	}

	public int getHookDurability(RtagItem tag) {
		if (tag == null || tag.get("HellFishing", 2) == null)
			return -1;

		int data = -1;
		if (tag.get("HellFishing", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 2);
			for (String key : map.keySet()) {
				if (key.equals("hook_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							data = (int) value;
						}
					}
				}
			}
		}
		return data;
	}

	public @Nullable ItemStack setHookData(RtagItem tag, @Nullable Object... data) {
		if (tag == null || data == null || data.length < 3)
			return null;

		Map<String, Object> map = Map.of("HellFishing",
				List.of(Map.of("hook_id", data[0]), Map.of("hook_item", data[1]), Map.of("hook_dur", data[2])));
		tag.set(map);
		return tag.load();
	}

	public @Nullable ItemStack setHookID(RtagItem tag, @Nullable String data) {
		if (tag == null || data == null || data.isEmpty())
			return null;

		if (tag.get("HellFishing", 0) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_id", data))), tag.get());
			} else {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_id", data))));
			}
			return tag.load();
		}

		if (tag.get("HellFishing", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 0);
			for (String key : map.keySet()) {
				if (key.equals("hook_id")) {
					for (Object value : map.values()) {
						if (value instanceof String) {
							value = data;
							tag.set(List.of(Map.of("hook_id", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public @Nullable ItemStack setHookItem(RtagItem tag, @Nullable ItemStack data) {
		if (tag == null || data == null)
			return null;

		if (tag.get("HellFishing", 1) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_item", data))), tag.get());
			} else {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_item", data))));
			}
			return tag.load();
		}

		if (tag.get("HellFishing", 1) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 1);
			for (String key : map.keySet()) {
				if (key.equals("hook_item")) {
					for (Object value : map.values()) {
						if (value instanceof ItemStack) {
							value = data;
							tag.set(List.of(Map.of("hook_item", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public @Nullable ItemStack setHookDurability(RtagItem tag, int data) {
		if (tag == null)
			return null;

		if (tag.get("HellFishing", 2) == null) {
			if (tag.get() != null) {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_dur", data))), tag.get());
			} else {
				tag.add(Map.of("HellFishing", List.of(Map.of("hook_dur", data))));
			}
			return tag.load();
		}

		if (tag.get("HellFishing", 2) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellFishing", 2);
			for (String key : map.keySet()) {
				if (key.equals("hook_dur")) {
					for (Object value : map.values()) {
						if (value instanceof Integer) {
							value = data;
							tag.set(List.of(Map.of("hook_dur", value)), map);
						}
					}
				}
			}
		}
		return tag.load();
	}

	public boolean removeHookData(RtagItem tag) {
		if (tag == null)
			return false;

		Map<String, Object> map = tag.get();
		return tag.remove(map);
	}

	/**
	 * Loads configuration files for the specified types.
	 */
	private void loadConfig() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (String type : List.of("hook")) {
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
						this.loadSingleFile(subFile);
					}
				}
			}
		}
	}

	/**
	 * Loads data from a single configuration file.
	 *
	 * @param file The configuration file to load.
	 */
	private void loadSingleFile(File file) {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection section) {
				if (!section.contains("max-durability")) {
					LogUtils.warn("Please set max-durability to hook: " + entry.getKey());
					continue;
				}
				var setting = new HookSetting.Builder(entry.getKey()).durability(section.getInt("max-durability", 16))
						.lore(section.getStringList("lore-on-rod").stream().map(it -> "<!i>" + it).toList()).build();
				hookSettingMap.put(entry.getKey(), setting);
			}
		}
	}

	/**
	 * Get the hook setting by its ID.
	 *
	 * @param id The ID of the hook setting to retrieve.
	 * @return The hook setting with the given ID, or null if not found.
	 */
	@Nullable
	@Override
	public HookSetting getHookSetting(String id) {
		return hookSettingMap.get(id);
	}

	/**
	 * Decreases the durability of a fishing hook by a specified amount and
	 * optionally updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The amount by which to decrease the durability.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	@Override
	public void decreaseHookDurability(ItemStack rod, int amount, boolean updateLore) {
		ItemUtils.decreaseHookDurability(rod, amount, updateLore);
	}

	/**
	 * Increases the durability of a fishing hook by a specified amount and
	 * optionally updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The amount by which to increase the durability.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	@Override
	public void increaseHookDurability(ItemStack rod, int amount, boolean updateLore) {
		ItemUtils.increaseHookDurability(rod, amount, updateLore);
	}

	/**
	 * Sets the durability of a fishing hook to a specific amount and optionally
	 * updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The new durability value to set.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	@Override
	public void setHookDurability(ItemStack rod, int amount, boolean updateLore) {
		ItemUtils.setHookDurability(rod, amount, updateLore);
	}

	/**
	 * Equips a fishing hook on a fishing rod.
	 *
	 * @param rod  The fishing rod ItemStack.
	 * @param hook The fishing hook ItemStack.
	 * @return True if the hook was successfully equipped, false otherwise.
	 */
	@Override
	public boolean equipHookOnRod(ItemStack rod, ItemStack hook) {
		if (rod == null || hook == null || hook.getType() == Material.AIR || hook.getAmount() != 1)
			return false;
		if (rod.getType() != Material.FISHING_ROD)
			return false;

		String hookID = instance.getItemManager().getAnyPluginItemID(hook);
		HookSetting setting = getHookSetting(hookID);
		if (setting == null)
			return false;

		var curDurability = ItemUtils.getCustomDurability(hook);
		if (curDurability.left() == 0)
			return false;

		RtagItem tagItem = new RtagItem(rod);
		setHookData(tagItem, hookID, hook, curDurability.right());
		tagItem = ItemUtils.updateNBTItemLore(tagItem);
		rod.setItemMeta(tagItem.getItem().getItemMeta());
		return true;
	}

	/**
	 * Removes the fishing hook from a fishing rod.
	 *
	 * @param rod The fishing rod ItemStack.
	 * @return The removed fishing hook ItemStack, or null if no hook was found.
	 */
	@Override
	public ItemStack removeHookFromRod(ItemStack rod) {
		if (rod == null || rod.getType() != Material.FISHING_ROD)
			return null;

		RtagItem tagItem = new RtagItem(rod);
		ItemStack hook = (ItemStack) getHookItem(tagItem);
		if (hook != null) {
			removeHookData(tagItem);
			tagItem = ItemUtils.updateNBTItemLore(tagItem);
			rod.setItemMeta(tagItem.getItem().getItemMeta());
		}
		return hook;
	}

	/**
	 * Handles the event when a player clicks on a fishing rod in their inventory.
	 *
	 * @param event The InventoryClickEvent to handle.
	 */
	// TODO: not properly applying data, try to fix.
	@EventHandler
	public void onDragDrop(InventoryClickEvent event) {
		if (event.isCancelled())
			return;
		if (!(event.getWhoClicked() instanceof Player)) 
			return;
		final Player player = (Player) event.getWhoClicked();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getClickedInventory() == null || event.getClickedInventory() != player.getInventory())
			return;
		ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getType() != Material.FISHING_ROD)
			return;
		if (player.getGameMode() != GameMode.SURVIVAL)
			return;
		if (instance.getFishingManager().hasPlayerCastHook(player.getUniqueId()))
			return;

		ItemStack cursor = event.getCursor();
		if (cursor.getType() == Material.AIR) {
			if (event.getClick() == ClickType.RIGHT) {
				RtagItem tagItem = new RtagItem(clicked);
				if (checkHookID(tagItem)) {
					event.setCancelled(true);
					ItemStack hook = getHookItem(tagItem);
					if (hook != null) {
						ItemUtils.setDurability(hook, getHookDurability(tagItem), true);
						removeHookData(tagItem);
						player.setItemOnCursor(hook);
						tagItem = ItemUtils.updateNBTItemLore(tagItem);
						clicked.setItemMeta(tagItem.getItem().getItemMeta());
					}
				}
			}
			return;
		}

		String hookID = instance.getItemManager().getAnyPluginItemID(cursor);
		HookSetting setting = getHookSetting(hookID);
		if (setting == null)
			return;

		var cursorDurability = ItemUtils.getCustomDurability(cursor);
		if (cursorDurability.left() == 0) {
			if (instance.getItemManager().getBuildableItem("hook", hookID) instanceof ItemManager.HBBuilder hbBuilder) {
				ItemStack itemStack = hbBuilder.build(player, new HashMap<>());
				var pair = ItemUtils.getCustomDurability(itemStack);
				cursorDurability = pair;
				RtagItem tagItem = new RtagItem(cursor);
				instance.getFishingManager().setFishingData(tagItem, pair.left(), pair.right(), "hook", hookID);
				cursor.setItemMeta(tagItem.getItem().getItemMeta());
			} else {
				return;
			}
		}

		Condition condition = new Condition(player, new HashMap<>());
		condition.insertArg("{rod}", instance.getItemManager().getAnyPluginItemID(clicked));
		EffectCarrier effectCarrier = instance.getEffectManager().getEffectCarrier("hook", hookID);
		if (effectCarrier != null) {
			if (!RequirementManagerInterface.isRequirementMet(condition, effectCarrier.getRequirements())) {
				return;
			}
		}

		event.setCancelled(true);

		int hookDuration = cursorDurability.right();
		RtagItem tagItem = new RtagItem(clicked);
		String previousHookID = getHookID(tagItem);

		ItemStack clonedHook = cursor.clone();
		clonedHook.setAmount(1);
		cursor.setAmount(cursor.getAmount() - 1);

		if (previousHookID != null && !previousHookID.equals("")) {
			int previousHookDurability = getHookDurability(tagItem);
			ItemStack previousItemStack = getHookItem(tagItem);
			ItemUtils.setDurability(previousItemStack, previousHookDurability, true);
			if (cursor.getAmount() == 0) {
				player.setItemOnCursor(previousItemStack);
			} else {
				ItemUtils.giveItem(player, previousItemStack, 1);
			}
		}

		setHookData(tagItem, hookID, clonedHook, hookDuration);
		tagItem = ItemUtils.updateNBTItemLore(tagItem);
		clicked.setItemMeta(tagItem.getItem().getItemMeta());
	}
}
