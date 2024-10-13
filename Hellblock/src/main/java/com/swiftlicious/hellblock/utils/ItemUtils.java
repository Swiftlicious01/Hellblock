package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.listeners.fishing.HookSetting;
import com.swiftlicious.hellblock.utils.extras.Pair;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Utility class for various item-related operations.
 */
public class ItemUtils {

	private ItemUtils() {
	}

	/**
	 * Updates the lore of an ReadWriteNBT based on its custom NBT tags.
	 *
	 * @param rodNBTItem The NBTItem to update
	 * @return The updated NBTItem
	 */
	public static ReadWriteNBT updateNBTItemLore(ReadWriteNBT rodNBTItem) {
		ReadWriteNBT hbCompound = rodNBTItem.getCompound("LavaFishing");
		if (hbCompound == null)
			return rodNBTItem;

		ReadWriteNBT displayCompound = rodNBTItem.getOrCreateCompound("display");
		ReadWriteNBTList<String> lore = displayCompound.getStringList("Lore");
		lore.removeIf(it -> GsonComponentSerializer.gson().deserialize(it) instanceof ScoreComponent scoreComponent
				&& scoreComponent.name().equals("hb"));

		if (hbCompound.hasTag("hook_id")) {
			String hookID = hbCompound.getString("hook_id");
			HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(hookID);
			if (setting == null) {
				hbCompound.removeKey("hook_id");
				hbCompound.removeKey("hook_item");
				hbCompound.removeKey("hook_dur");
			} else {
				for (String newLore : setting.getLore()) {
					ScoreComponent.Builder builder = Component.score().name("hb").objective("hook");
					builder.append(HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
							newLore.replace("{dur}", String.valueOf(hbCompound.getInteger("hook_dur"))).replace("{max}",
									String.valueOf(setting.getMaxDurability()))));
					lore.add(GsonComponentSerializer.gson().serialize(builder.build()));
				}
			}
		}

		if (hbCompound.hasTag("max_dur")) {
			int max = hbCompound.getInteger("max_dur");
			int current = hbCompound.getInteger("cur_dur");
			for (String newLore : HBConfig.durabilityLore) {
				ScoreComponent.Builder builder = Component.score().name("hb").objective("durability");
				builder.append(HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
						newLore.replace("{dur}", String.valueOf(current)).replace("{max}", String.valueOf(max))));
				lore.add(GsonComponentSerializer.gson().serialize(builder.build()));
			}
		}
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
		NBT.modify(itemStack, nbtItem -> {
			updateNBTItemLore(nbtItem);
			itemStack.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.modify(rod, nbtItem -> {
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("hook_dur")) {
				int hookDur = hbCompound.getInteger("hook_dur");
				if (hookDur != -1) {
					hookDur = Math.max(0, hookDur - amount);
					if (hookDur > 0) {
						hbCompound.setInteger("hook_dur", hookDur);
					} else {
						hbCompound.removeKey("hook_id");
						hbCompound.removeKey("hook_dur");
						hbCompound.removeKey("hook_item");
					}
				}
			}
			if (updateLore)
				updateNBTItemLore(nbtItem);
			rod.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.modify(rod, nbtItem -> {
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("hook_dur")) {
				int hookDur = hbCompound.getInteger("hook_dur");
				if (hookDur != -1) {
					String id = hbCompound.getString("hook_id");
					HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
					if (setting == null) {
						hbCompound.removeKey("hook_id");
						hbCompound.removeKey("hook_dur");
						hbCompound.removeKey("hook_item");
					} else {
						hookDur = Math.min(setting.getMaxDurability(), hookDur + amount);
						hbCompound.setInteger("hook_dur", hookDur);
					}
				}
			}
			if (updateLore)
				updateNBTItemLore(nbtItem);
			rod.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.modify(rod, nbtItem -> {
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("hook_dur")) {
				int hookDur = hbCompound.getInteger("hook_dur");
				if (hookDur != -1) {
					String id = hbCompound.getString("hook_id");
					HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
					if (setting == null) {
						hbCompound.removeKey("hook_id");
						hbCompound.removeKey("hook_dur");
						hbCompound.removeKey("hook_item");
					} else {
						hookDur = Math.min(setting.getMaxDurability(), amount);
						hbCompound.setInteger("hook_dur", hookDur);
					}
				}
			}
			if (updateLore)
				updateNBTItemLore(nbtItem);
			rod.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.modify(itemStack, nbtItem -> {
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("max_dur")) {
				int unBreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
				if (Math.random() > (double) 1 / (unBreakingLevel + 1)) {
					return;
				}
				if (nbtItem.getByte("Unbreakable") == 1) {
					return;
				}
				int max = hbCompound.getInteger("max_dur");
				int current = hbCompound.getInteger("cur_dur") - amount;
				hbCompound.setInteger("cur_dur", current);
				int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) current / max)));
				nbtItem.setInteger("Damage", damage);
				if (current > 0) {
					if (updateLore)
						updateNBTItemLore(nbtItem);
					itemStack.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
				} else {
					itemStack.setAmount(0);
				}
			} else {
				ItemMeta previousMeta = itemStack.getItemMeta().clone();
				PlayerItemDamageEvent itemDamageEvent = new PlayerItemDamageEvent(player, itemStack, amount, (previousMeta instanceof Damageable ? ((Damageable)previousMeta).getDamage() : 0));
				Bukkit.getPluginManager().callEvent(itemDamageEvent);
				if (!itemStack.getItemMeta().equals(previousMeta) || itemDamageEvent.isCancelled()) {
					return;
				}
				int unBreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
				if (Math.random() > (double) 1 / (unBreakingLevel + 1)) {
					return;
				}
				if (nbtItem.getByte("Unbreakable") == 1) {
					return;
				}
				int damage = nbtItem.getInteger("Damage") + amount;
				if (damage > itemStack.getType().getMaxDurability()) {
					itemStack.setAmount(0);
				} else {
					nbtItem.setInteger("Damage", damage);
					if (updateLore)
						updateNBTItemLore(nbtItem);
					itemStack.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
				}
			}
		});
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
		NBT.modify(itemStack, nbtItem -> {
			if (nbtItem.getByte("Unbreakable") == 1) {
				return;
			}
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("max_dur")) {
				int max = hbCompound.getInteger("max_dur");
				int current = Math.min(max, hbCompound.getInteger("cur_dur") + amount);
				hbCompound.setInteger("cur_dur", current);
				int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) current / max)));
				nbtItem.setInteger("Damage", damage);
				if (updateLore)
					updateNBTItemLore(nbtItem);
			} else {
				int damage = Math.max(nbtItem.getInteger("Damage") - amount, 0);
				nbtItem.setInteger("Damage", damage);
			}
			itemStack.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.modify(itemStack, nbtItem -> {
			if (nbtItem.getByte("Unbreakable") == 1) {
				return;
			}
			int finalAmount = amount;
			ReadWriteNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("max_dur")) {
				int max = hbCompound.getInteger("max_dur");
				finalAmount = Math.min(amount, max);
				hbCompound.setInteger("cur_dur", finalAmount);
				int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) finalAmount / max)));
				nbtItem.setInteger("Damage", damage);
				if (updateLore)
					updateNBTItemLore(nbtItem);
			} else {
				nbtItem.setInteger("Damage", itemStack.getType().getMaxDurability() - finalAmount);
			}
			itemStack.setItemMeta(NBT.itemStackFromNBT(nbtItem).getItemMeta());
		});
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
		NBT.get(itemStack, nbtItem -> {
			ReadableNBT hbCompound = nbtItem.getCompound("LavaFishing");
			if (hbCompound != null && hbCompound.hasTag("max_dur")) {
				return Pair.of(hbCompound.getInteger("max_dur"), hbCompound.getInteger("cur_dur"));
			} else {
				return Pair.of(0, 0);
			}
		});

		return Pair.of(0, 0);
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

	public static ItemStack removeOwner(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return itemStack;
		return NBT.modify(itemStack, nbtItem -> {
			if (nbtItem.hasTag("owner")) {
				nbtItem.removeKey("owner");
				return NBT.itemStackFromNBT(nbtItem);
			}
			return itemStack;
		});
	}
}