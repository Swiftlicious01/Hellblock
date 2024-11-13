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
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.effects.EffectCarrier;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.Condition;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class HookManager implements Listener, HookManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, HookSetting> hookSettingMap;

	public HookManager(HellblockPlugin plugin) {
		instance = plugin;
		this.hookSettingMap = new HashMap<>();
	}
	
	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		loadConfig();
	}
	
	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		hookSettingMap.clear();
	}
	
	@Override
	public void disable() {
		unload();
	}

	public boolean checkHookDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellFishing", "hook_dur");
	}

	public int getHookDurability(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return -1;

		return new RtagItem(item).getOptional("HellFishing", "hook_dur").asInt();
	}

	public boolean checkHookItem(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellFishing", "hook_item");
	}

	public @Nullable ItemStack getHookItem(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return ItemUtils.fromBase64(new RtagItem(item).getOptional("HellFishing", "hook_item").as(byte[].class));
	}

	public boolean checkHookID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellFishing", "hook_id");
	}

	public @Nullable String getHookID(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return new RtagItem(item).getOptional("HellFishing", "hook_id").asString();
	}

	public @Nullable ItemStack setHookDurability(@Nullable ItemStack item, int data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellFishing", "hook_dur");
		});
	}

	public @Nullable ItemStack setHookItem(@Nullable ItemStack item, @Nullable byte[] data) {
		if (item == null || item.getType() == Material.AIR || data == null || data.length == 0)
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellFishing", "hook_item");
		});
	}

	public @Nullable ItemStack setHookID(@Nullable ItemStack item, @Nullable String data) {
		if (item == null || item.getType() == Material.AIR || data == null || data.isEmpty())
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellFishing", "hook_id");
		});
	}

	public boolean removeHookData(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).remove("HellFishing");
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
		YamlDocument config = instance.getConfigManager().loadData(file);
		for (Map.Entry<String, Object> entry : config.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section section) {
				if (!section.contains("max-durability")) {
					LogUtils.warn(String.format("Please set max-durability to hook: %s", entry.getKey()));
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

		rod = setHookDurability(rod, curDurability.right());
		rod = setHookItem(rod, ItemUtils.toBase64(hook));
		rod = setHookID(rod, hookID);
		RtagItem tagItem = ItemUtils.updateNBTItemLore(new RtagItem(rod));
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

		ItemStack hook = getHookItem(rod);
		if (hook != null) {
			removeHookData(rod);
			RtagItem tagItem = ItemUtils.updateNBTItemLore(new RtagItem(rod));
			rod.setItemMeta(tagItem.getItem().getItemMeta());
		}
		return hook;
	}

	/**
	 * Handles the event when a player clicks on a fishing rod in their inventory.
	 *
	 * @param event The InventoryClickEvent to handle.
	 */
	@EventHandler
	public void onDragDrop(InventoryClickEvent event) {
		if (event.isCancelled())
			return;
		if (!(event.getWhoClicked() instanceof Player))
			return;
		final Player player = (Player) event.getWhoClicked();
		if (!player.getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
				if (checkHookID(clicked)) {
					event.setCancelled(true);
					ItemStack hook = getHookItem(clicked);
					if (hook != null) {
						ItemUtils.setDurability(hook, getHookDurability(clicked), true);
						removeHookData(clicked);
						player.setItemOnCursor(hook);
						RtagItem tagItem = ItemUtils.updateNBTItemLore(new RtagItem(clicked));
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
				cursor = instance.getFishingManager().setMaxDurability(cursor, pair.left());
				cursor = instance.getFishingManager().setCurrentDurability(cursor, pair.right());
				cursor = instance.getFishingManager().setFishingType(cursor, "hook");
				cursor = instance.getFishingManager().setFishingID(cursor, hookID);
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
		String previousHookID = getHookID(clicked);

		ItemStack clonedHook = cursor.clone();
		clonedHook.setAmount(1);
		cursor.setAmount(cursor.getAmount() - 1);

		if (previousHookID != null && !previousHookID.isEmpty()) {
			int previousHookDurability = getHookDurability(clicked);
			ItemStack previousItemStack = getHookItem(clicked);
			ItemUtils.setDurability(previousItemStack, previousHookDurability, true);
			if (cursor.getAmount() == 0) {
				player.setItemOnCursor(previousItemStack);
			} else {
				ItemUtils.giveItem(player, previousItemStack, 1);
			}
		}

		clicked = setHookDurability(clicked, hookDuration);
		clicked = setHookItem(clicked, ItemUtils.toBase64(clonedHook));
		clicked = setHookID(clicked, hookID);
		RtagItem tagItem = ItemUtils.updateNBTItemLore(new RtagItem(clicked));
		clicked.setItemMeta(tagItem.getItem().getItemMeta());
	}
}
