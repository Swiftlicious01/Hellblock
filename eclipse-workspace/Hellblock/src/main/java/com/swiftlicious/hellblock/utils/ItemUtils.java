package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.listeners.fishing.HookSetting;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.text.Component;

/**
 * Utility class for various item-related operations.
 */
public class ItemUtils {

	private ItemUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Updates the lore of an item based on its custom NBT tags.
	 *
	 * @param rodNBTItem The ItemStack to update
	 * @return The updated ItemStack
	 */
	@SuppressWarnings("unchecked")
	public static RtagItem updateNBTItemLore(RtagItem rodNBTItem) {
		if (rodNBTItem == null)
			return null;

		ItemStack copy = rodNBTItem.getItem();
		if (NBTUtils.hasNBTItemComponentData(copy, "lore")) {
			List<Component> lore = (List<Component>) NBTUtils.getNBTItemComponentData(copy, "lore");

			if (HellblockPlugin.getInstance().getHookManager().checkHookID(rodNBTItem)) {
				String hookID = HellblockPlugin.getInstance().getHookManager().getHookID(rodNBTItem);
				HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(hookID);
				if (setting == null) {
					HellblockPlugin.getInstance().getHookManager().removeHookData(rodNBTItem);
				} else {
					for (String newLore : setting.getLore()) {
						lore.add(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(newLore
										.replace("{dur}",
												String.valueOf(HellblockPlugin.getInstance().getHookManager()
														.getHookDurability(rodNBTItem)))
										.replace("{max}", String.valueOf(setting.getMaxDurability()))));
					}
				}
			}

			if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(rodNBTItem)) {
				int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(rodNBTItem);
				int current = HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(rodNBTItem);
				for (String newLore : HBConfig.durabilityLore) {
					lore.add(HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
							newLore.replace("{dur}", String.valueOf(current)).replace("{max}", String.valueOf(max))));
				}
			}
		}
		
		rodNBTItem.load();
		rodNBTItem.update();
		return rodNBTItem;
	}

	/**
	 * Updates the lore of an ItemStack based on its custom NBT tags.
	 *
	 * @param itemStack The ItemStack to update
	 */
	public static void updateItemLore(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return;
		RtagItem tagItem = new RtagItem(itemStack);
		tagItem = updateNBTItemLore(tagItem);
		itemStack.setItemMeta(tagItem.getItem().getItemMeta());
	}

	/**
	 * Reduces the durability of a fishing hook item.
	 *
	 * @param rod        The fishing rod ItemStack
	 * @param updateLore Whether to update the lore after reducing durability
	 */
	public static void decreaseHookDurability(ItemStack rod, int amount, boolean updateLore) {
		if (rod == null || rod.getType() != Material.FISHING_ROD)
			return;
		RtagItem tagItem = new RtagItem(rod);
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(tagItem)) {
			int hookDur = HellblockPlugin.getInstance().getHookManager().getHookDurability(tagItem);
			if (hookDur != -1) {
				hookDur = Math.max(0, hookDur - amount);
				if (hookDur > 0) {
					HellblockPlugin.getInstance().getHookManager().setHookDurability(tagItem, hookDur);
				} else {
					HellblockPlugin.getInstance().getHookManager().removeHookData(tagItem);
				}
			}
		}
		if (updateLore) {
			tagItem = updateNBTItemLore(tagItem);
			rod.setItemMeta(tagItem.getItem().getItemMeta());
		}
	}

	/**
	 * Increases the durability of a fishing hook by a specified amount and
	 * optionally updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The amount by which to increase the durability.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	public static void increaseHookDurability(ItemStack rod, int amount, boolean updateLore) {
		if (rod == null || rod.getType() != Material.FISHING_ROD)
			return;
		RtagItem tagItem = new RtagItem(rod);
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(tagItem)) {
			int hookDur = (int) HellblockPlugin.getInstance().getHookManager().getHookDurability(tagItem);
			if (hookDur != -1) {
				String id = HellblockPlugin.getInstance().getHookManager().getHookID(tagItem);
				HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
				if (setting == null) {
					HellblockPlugin.getInstance().getHookManager().removeHookData(tagItem);
				} else {
					hookDur = Math.min(setting.getMaxDurability(), hookDur + amount);
					HellblockPlugin.getInstance().getHookManager().setHookDurability(tagItem, hookDur);
				}
			}
		}
		if (updateLore) {
			tagItem = updateNBTItemLore(tagItem);
			rod.setItemMeta(tagItem.getItem().getItemMeta());
		}
	}

	/**
	 * Sets the durability of a fishing hook to a specific amount and optionally
	 * updates its lore.
	 *
	 * @param rod        The fishing rod ItemStack to modify.
	 * @param amount     The new durability value to set.
	 * @param updateLore Whether to update the lore of the fishing rod.
	 */
	public static void setHookDurability(ItemStack rod, int amount, boolean updateLore) {
		if (rod == null || rod.getType() != Material.FISHING_ROD)
			return;
		RtagItem tagItem = new RtagItem(rod);
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(tagItem)) {
			int hookDur = HellblockPlugin.getInstance().getHookManager().getHookDurability(tagItem);
			if (hookDur != -1) {
				String id = HellblockPlugin.getInstance().getHookManager().getHookID(tagItem);
				HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
				if (setting == null) {
					HellblockPlugin.getInstance().getHookManager().removeHookData(tagItem);
				} else {
					hookDur = Math.min(setting.getMaxDurability(), amount);
					HellblockPlugin.getInstance().getHookManager().setHookDurability(tagItem, hookDur);
				}
			}
		}
		if (updateLore) {
			tagItem = updateNBTItemLore(tagItem);
			rod.setItemMeta(tagItem.getItem().getItemMeta());
		}
	}

	/**
	 * Decreases the durability of an item and updates its lore.
	 *
	 * @param itemStack  The ItemStack to reduce durability for
	 * @param amount     The amount by which to reduce durability
	 * @param updateLore Whether to update the lore after reducing durability
	 */
	public static void decreaseDurability(Player player, ItemStack itemStack, int amount, boolean updateLore) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return;
		RtagItem tagItem = new RtagItem(itemStack);
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(tagItem)) {
			int unBreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
			if (Math.random() > (double) 1 / (unBreakingLevel + 1)) {
				return;
			}
			if (NBTUtils.hasNBTItemComponentData(itemStack, "unbreakable")
					&& (byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable") == 1) {
				return;
			}
			int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(tagItem);
			int current = (int) HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(tagItem)
					- amount;
			HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(tagItem, current);
			int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) current / max)));
			NBTUtils.setNBTItemComponentData(itemStack, damage, "damage");
			if (current > 0) {
				if (updateLore) {
					tagItem = updateNBTItemLore(tagItem);
					itemStack.setItemMeta(tagItem.getItem().getItemMeta());
				}
			} else {
				itemStack.setAmount(0);
			}
		} else {
			ItemMeta previousMeta = itemStack.getItemMeta().clone();
			PlayerItemDamageEvent itemDamageEvent = new PlayerItemDamageEvent(player, itemStack, amount,
					(previousMeta instanceof Damageable ? ((Damageable) previousMeta).getDamage() : 0));
			Bukkit.getPluginManager().callEvent(itemDamageEvent);
			if (!itemStack.getItemMeta().equals(previousMeta) || itemDamageEvent.isCancelled()) {
				return;
			}
			int unBreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
			if (Math.random() > (double) 1 / (unBreakingLevel + 1)) {
				return;
			}
			if (NBTUtils.hasNBTItemComponentData(itemStack, "unbreakable")
					&& (byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable") == 1) {
				return;
			}
			int damage = (int) NBTUtils.getNBTItemComponentData(itemStack, "damage") + amount;
			if (damage > itemStack.getType().getMaxDurability()) {
				itemStack.setAmount(0);
			} else {
				NBTUtils.setNBTItemComponentData(itemStack, damage, "damage");
				if (updateLore) {
					tagItem = updateNBTItemLore(tagItem);
					itemStack.setItemMeta(tagItem.getItem().getItemMeta());
				}
			}
		}
	}

	/**
	 * Increases the durability of an item and updates its lore.
	 *
	 * @param itemStack  The ItemStack to increase durability for
	 * @param amount     The amount by which to increase durability
	 * @param updateLore Whether to update the lore after increasing durability
	 */
	public static void increaseDurability(ItemStack itemStack, int amount, boolean updateLore) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return;
		RtagItem tagItem = new RtagItem(itemStack);
		ItemStack copy = tagItem.loadCopy();
		if (NBTUtils.hasNBTItemComponentData(copy, "unbreakable")
				&& (byte) NBTUtils.getNBTItemComponentData(copy, "unbreakable") == 1) {
			return;
		}
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(tagItem)) {
			int max = (int) HellblockPlugin.getInstance().getFishingManager().getMaxDurability(tagItem);
			int current = Math.min(max,
					(int) HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(tagItem) + amount);
			HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(tagItem, current);
			int damage = (int) (copy.getType().getMaxDurability() * (1 - ((double) current / max)));
			NBTUtils.setNBTItemComponentData(copy, damage, "damage");
			if (updateLore)
				updateNBTItemLore(tagItem);
		} else {
			int damage = Math.max((int) NBTUtils.getNBTItemComponentData(copy, "damage") - amount, 0);
			NBTUtils.setNBTItemComponentData(copy, damage, "damage");
		}
		itemStack.setItemMeta(tagItem.getItem().getItemMeta());
	}

	/**
	 * Sets the durability of an item and updates its lore.
	 *
	 * @param itemStack  The ItemStack to set durability for
	 * @param amount     The new durability value
	 * @param updateLore Whether to update the lore after setting durability
	 */
	public static void setDurability(ItemStack itemStack, int amount, boolean updateLore) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return;
		if (amount <= 0) {
			itemStack.setAmount(0);
			return;
		}
		if (NBTUtils.hasNBTItemComponentData(itemStack, "unbreakable")
				&& (byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable") == 1) {
			return;
		}
		int finalAmount = amount;

		RtagItem tagItem = new RtagItem(itemStack);
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(tagItem)) {
			int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(tagItem);
			finalAmount = Math.min(amount, max);
			HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(tagItem, finalAmount);
			int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) finalAmount / max)));
			NBTUtils.setNBTItemComponentData(itemStack, damage, "damage");
			if (updateLore) {
				tagItem = updateNBTItemLore(tagItem);
				itemStack.setItemMeta(tagItem.getItem().getItemMeta());
			}
		} else {
			NBTUtils.setNBTItemComponentData(itemStack, itemStack.getType().getMaxDurability() - finalAmount, "damage");
		}
	}

	/**
	 * Retrieves the current durability of an item.
	 *
	 * @param itemStack The ItemStack to get durability from
	 * @return The current durability value
	 */
	public static Pair<Integer, Integer> getCustomDurability(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return Pair.of(0, 0);
		if (itemStack.getItemMeta() instanceof Damageable damageable && damageable.isUnbreakable())
			return Pair.of(-1, -1);

		RtagItem tagItem = new RtagItem(itemStack);
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(tagItem)) {
			return Pair.of(HellblockPlugin.getInstance().getFishingManager().getMaxDurability(tagItem),
					HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(tagItem));
		} else {
			return Pair.of(0, 0);
		}
	}

	/**
	 * Gives a certain amount of an item to a player, handling stacking and item
	 * drops.
	 *
	 * @param player    The player to give the item to
	 * @param itemStack The ItemStack to give
	 * @param amount    The amount of items to give
	 * @return The actual amount of items given
	 */
	public static int giveItem(Player player, ItemStack itemStack, int amount) {
		PlayerInventory inventory = player.getInventory();
		ItemMeta meta = itemStack.getItemMeta();
		int maxStackSize = itemStack.getMaxStackSize();

		if (amount > maxStackSize * 100) {
			LogUtils.warn("Detected too many items spawning. Lowering the amount to " + (maxStackSize * 100));
			amount = maxStackSize * 100;
		}

		int actualAmount = amount;

		for (ItemStack other : inventory.getStorageContents()) {
			if (other != null) {
				if (other.getType() == itemStack.getType() && other.getItemMeta().equals(meta)) {
					if (other.getAmount() < maxStackSize) {
						int delta = maxStackSize - other.getAmount();
						if (amount > delta) {
							other.setAmount(maxStackSize);
							amount -= delta;
						} else {
							other.setAmount(amount + other.getAmount());
							return actualAmount;
						}
					}
				}
			}
		}

		if (amount > 0) {
			for (ItemStack other : inventory.getStorageContents()) {
				if (other == null) {
					if (amount > maxStackSize) {
						amount -= maxStackSize;
						ItemStack cloned = itemStack.clone();
						cloned.setAmount(maxStackSize);
						inventory.addItem(cloned);
					} else {
						ItemStack cloned = itemStack.clone();
						cloned.setAmount(amount);
						inventory.addItem(cloned);
						return actualAmount;
					}
				}
			}
		}

		if (amount > 0) {
			for (int i = 0; i < amount / maxStackSize; i++) {
				ItemStack cloned = itemStack.clone();
				cloned.setAmount(maxStackSize);
				player.getWorld().dropItem(player.getLocation(), cloned);
			}
			int left = amount % maxStackSize;
			if (left != 0) {
				ItemStack cloned = itemStack.clone();
				cloned.setAmount(left);
				player.getWorld().dropItem(player.getLocation(), cloned);
			}
		}

		return actualAmount;
	}

	public static String toBase64(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return "";
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {
			dataOutput.writeObject(itemStack);
			byte[] byteArr = outputStream.toByteArray();
			dataOutput.close();
			outputStream.close();
			return Base64Coder.encodeLines(byteArr);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public static ItemStack fromBase64(String base64) {
		if (base64 == null || base64.equals(""))
			return new ItemStack(Material.AIR);
		ByteArrayInputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
		} catch (IllegalArgumentException e) {
			return new ItemStack(Material.AIR);
		}
		ItemStack stack = null;
		try (ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {
			stack = (ItemStack) dataInput.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return stack;
	}

	public static boolean checkOwnerData(RtagItem tag) {
		if (tag == null || tag.get("HellblockData", 0) == null)
			return false;

		boolean data = false;
		if (tag.get("HellblockData", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockData", 0);
			for (String key : map.keySet()) {
				if (key.equals("owner")) {
					data = true;
				}
			}
		}
		return data;
	}

	public static @Nullable String getOwnerData(RtagItem tag) {
		if (tag == null || tag.get("HellblockData", 0) == null)
			return null;

		String data = null;
		if (tag.get("HellblockData", 0) instanceof HashMap) {
			HashMap<String, Object> map = tag.get("HellblockData", 0);
			for (String key : map.keySet()) {
				if (key.equals("owner")) {
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

	public static @Nullable ItemStack setOwnerData(RtagItem tag, @Nullable String name) {
		if (tag == null || name == null || name.isEmpty())
			return null;

		Map<String, Object> data = Map.of("HellblockData", List.of(Map.of("owner", name)));
		tag.set(data);
		return tag.load();
	}

	public static boolean removeOwnerData(RtagItem tag) {
		if (tag == null)
			return false;

		Map<String, Object> map = tag.get();
		return tag.remove(map);
	}

	public static RtagItem removeOwner(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return null;

		RtagItem tagItem = new RtagItem(itemStack);
		if (checkOwnerData(tagItem)) {
			removeOwnerData(tagItem);
		}

		return tagItem;
	}
}