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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.ItemManager;
import com.swiftlicious.hellblock.effects.EffectCarrier;
import com.swiftlicious.hellblock.handlers.RequirementManagerInterface;
import com.swiftlicious.hellblock.utils.ItemUtils;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.Condition;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;

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

		return NBT.modify(rod, rodNBTItem -> {
			ReadWriteNBT hbCompound = rodNBTItem.getOrCreateCompound("LavaFishing");

			hbCompound.setString("hook_id", hookID);
			hbCompound.setItemStack("hook_item", hook);
			hbCompound.setInteger("hook_dur", curDurability.right());

			ItemUtils.updateNBTItemLore(rodNBTItem);
			rod.setItemMeta(NBT.itemStackFromNBT(rodNBTItem).getItemMeta());
			return true;
		});
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

		return NBT.modify(rod, rodNBTItem -> {
			ReadWriteNBT hbCompound = rodNBTItem.getCompound("LavaFishing");
			if (hbCompound == null)
				return null;

			ItemStack hook = hbCompound.getItemStack("hook_item");
			if (hook != null) {
				hbCompound.removeKey("hook_item");
				hbCompound.removeKey("hook_id");
				hbCompound.removeKey("hook_dur");
				ItemUtils.updateNBTItemLore(rodNBTItem);
				rod.setItemMeta(NBT.itemStackFromNBT(rodNBTItem).getItemMeta());
			}

			return hook;
		});
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
		final Player player = (Player) event.getWhoClicked();
		if (event.getClickedInventory() != player.getInventory())
			return;
		ItemStack clicked = event.getCurrentItem();
		if (clicked == null || clicked.getType() != Material.FISHING_ROD)
			return;
		if (player.getGameMode() != GameMode.SURVIVAL)
			return;
		if (instance.getFishingManager().hasPlayerCastHook(player.getUniqueId()))
			return;

		ItemStack cursor = event.getCursor();
		if (cursor == null || cursor.getType() == Material.AIR) {
			if (event.getClick() == ClickType.RIGHT) {
				NBT.modify(clicked, nbtItem -> {
					ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
					if (hbCompound == null)
						return;
					if (hbCompound.hasTag("hook_id")) {
						event.setCancelled(true);
						ItemStack hook = hbCompound.getItemStack("hook_item");
						ItemUtils.setDurability(hook, hbCompound.getInteger("hook_dur"), true);
						hbCompound.removeKey("hook_id");
						hbCompound.removeKey("hook_item");
						hbCompound.removeKey("hook_dur");
						player.setItemOnCursor(hook);
						ItemUtils.updateNBTItemLore(nbtItem);
						clicked.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
					}
				});
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
				NBT.modify(cursor, nbtItem -> {
					ReadWriteNBT compound = nbtItem.getOrCreateCompound("LavaFishing");
					compound.setInteger("max_dur", pair.left());
					compound.setInteger("cur_dur", pair.right());
					compound.setString("type", "hook");
					compound.setString("id", hookID);
					cursor.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
				});
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
		NBT.modify(clicked, rodNBTItem -> {
			ReadWriteNBT hbCompound = rodNBTItem.getOrCreateCompound("LavaFishing");
			String previousHookID = hbCompound.getString("hook_id");

			ItemStack clonedHook = cursor.clone();
			clonedHook.setAmount(1);
			cursor.setAmount(cursor.getAmount() - 1);

			if (previousHookID != null && !previousHookID.equals("")) {
				int previousHookDurability = hbCompound.getInteger("hook_dur");
				ItemStack previousItemStack = hbCompound.getItemStack("hook_item");
				ItemUtils.setDurability(previousItemStack, previousHookDurability, true);
				if (cursor.getAmount() == 0) {
					player.setItemOnCursor(previousItemStack);
				} else {
					ItemUtils.giveItem(player, previousItemStack, 1);
				}
			}

			hbCompound.setString("hook_id", hookID);
			hbCompound.setItemStack("hook_item", clonedHook);
			hbCompound.setInteger("hook_dur", hookDuration);

			ItemUtils.updateNBTItemLore(rodNBTItem);
			clicked.setItemMeta(NBT.itemStackFromNBT(rodNBTItem).getItemMeta());
		});
	}
}
