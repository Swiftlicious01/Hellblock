package com.swiftlicious.hellblock.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.listeners.fishing.HookSetting;
import com.swiftlicious.hellblock.utils.extras.Pair;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;

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
	public static @Nullable RtagItem updateNBTItemLore(@Nullable RtagItem rodNBTItem) {
		if (rodNBTItem == null)
			return null;

		if (rodNBTItem.hasComponent("minecraft:lore")) {
			Object component = rodNBTItem.getComponent("minecraft:lore");
			Object encoded = ComponentType.encodeJava("minecraft:lore", component).orElse(new ArrayList<>());
			if (encoded instanceof List<?> conversion) {
				List<String> lore = conversion.stream().filter(Objects::nonNull)
						.map(object -> Objects.toString(object, null)).collect(Collectors.toList());
				lore.removeIf(it -> HellblockPlugin.getInstance().getAdventureManager()
						.jsonToComponent(it) instanceof ScoreComponent scoreComponent
						&& scoreComponent.name().equals("hb"));

				ItemStack item = rodNBTItem.getItem();
				if (HellblockPlugin.getInstance().getHookManager().checkHookID(item)) {
					String hookID = HellblockPlugin.getInstance().getHookManager().getHookID(item);
					HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(hookID);
					if (setting == null) {
						HellblockPlugin.getInstance().getHookManager().removeHookData(item);
					} else {
						int hookDurability = HellblockPlugin.getInstance().getHookManager().getHookDurability(item);
						for (String newLore : setting.getLore()) {
							ScoreComponent.Builder builder = Component.score().name("hb").objective("hook");
							builder.append(
									HellblockPlugin.getInstance().getAdventureManager().getComponentFromMiniMessage(
											newLore.replace("{dur}", String.valueOf(hookDurability)).replace("{max}",
													String.valueOf(setting.getMaxDurability()))));
							lore.add(HellblockPlugin.getInstance().getAdventureManager()
									.componentToJson(builder.build()));
						}
					}
				}

				if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(item)) {
					int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(item);
					int current = HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(item);
					for (String newLore : HBConfig.durabilityLore) {
						ScoreComponent.Builder builder = Component.score().name("hb").objective("durability");
						builder.append(HellblockPlugin.getInstance().getAdventureManager()
								.getComponentFromMiniMessage(newLore.replace("{dur}", String.valueOf(current))
										.replace("{max}", String.valueOf(max))));
						lore.add(HellblockPlugin.getInstance().getAdventureManager().componentToJson(builder.build()));
					}
				}

				if (!lore.isEmpty()) {
					rodNBTItem.setComponent("minecraft:lore", lore);
					rodNBTItem.load();
					rodNBTItem.update();
				}
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
		RtagItem tagItem = updateNBTItemLore(new RtagItem(itemStack));
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
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(rod)) {
			int hookDur = HellblockPlugin.getInstance().getHookManager().getHookDurability(rod);
			if (hookDur != -1) {
				hookDur = Math.max(0, hookDur - amount);
				if (hookDur > 0) {
					rod = HellblockPlugin.getInstance().getHookManager().setHookDurability(rod, hookDur);
				} else {
					HellblockPlugin.getInstance().getHookManager().removeHookData(rod);
				}
			}
		}
		if (updateLore) {
			RtagItem tagItem = updateNBTItemLore(new RtagItem(rod));
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
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(rod)) {
			int hookDur = (int) HellblockPlugin.getInstance().getHookManager().getHookDurability(rod);
			if (hookDur != -1) {
				String id = HellblockPlugin.getInstance().getHookManager().getHookID(rod);
				HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
				if (setting == null) {
					HellblockPlugin.getInstance().getHookManager().removeHookData(rod);
				} else {
					hookDur = Math.min(setting.getMaxDurability(), hookDur + amount);
					rod = HellblockPlugin.getInstance().getHookManager().setHookDurability(rod, hookDur);
				}
			}
		}
		if (updateLore) {
			RtagItem tagItem = updateNBTItemLore(new RtagItem(rod));
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
		if (HellblockPlugin.getInstance().getHookManager().checkHookDurability(rod)) {
			int hookDur = HellblockPlugin.getInstance().getHookManager().getHookDurability(rod);
			if (hookDur != -1) {
				String id = HellblockPlugin.getInstance().getHookManager().getHookID(rod);
				HookSetting setting = HellblockPlugin.getInstance().getHookManager().getHookSetting(id);
				if (setting == null) {
					HellblockPlugin.getInstance().getHookManager().removeHookData(rod);
				} else {
					hookDur = Math.min(setting.getMaxDurability(), amount);
					rod = HellblockPlugin.getInstance().getHookManager().setHookDurability(rod, hookDur);
				}
			}
		}
		if (updateLore) {
			RtagItem tagItem = updateNBTItemLore(new RtagItem(rod));
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
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(itemStack)) {
			int unBreakingLevel = itemStack.getEnchantmentLevel(Enchantment.UNBREAKING);
			if (Math.random() > (double) 1 / (unBreakingLevel + 1)) {
				return;
			}
			if (NBTUtils.hasNBTItemComponentData(itemStack, "unbreakable")
					&& ((byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable")) == 1) {
				return;
			}
			int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(itemStack);
			int current = (int) HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(itemStack)
					- amount;
			itemStack = HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(itemStack, current);
			int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) current / max)));
			NBTUtils.setNBTItemComponentData(itemStack, (short) damage, "damage");
			if (current > 0) {
				if (updateLore) {
					RtagItem tagItem = updateNBTItemLore(new RtagItem(itemStack));
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
				NBTUtils.setNBTItemComponentData(itemStack, (short) damage, "damage");
				if (updateLore) {
					RtagItem tagItem = updateNBTItemLore(new RtagItem(itemStack));
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
		if (NBTUtils.hasNBTItemComponentData(itemStack, "unbreakable")
				&& ((byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable")) == 1) {
			return;
		}
		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(itemStack)) {
			int max = (int) HellblockPlugin.getInstance().getFishingManager().getMaxDurability(itemStack);
			int current = Math.min(max,
					(int) HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(itemStack) + amount);
			itemStack = HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(itemStack, current);
			int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) current / max)));
			NBTUtils.setNBTItemComponentData(itemStack, (short) damage, "damage");
			if (updateLore) {
				RtagItem tagItem = updateNBTItemLore(new RtagItem(itemStack));
				itemStack.setItemMeta(tagItem.getItem().getItemMeta());
			}
		} else {
			int damage = Math.max((int) NBTUtils.getNBTItemComponentData(itemStack, "damage") - amount, 0);
			NBTUtils.setNBTItemComponentData(itemStack, (short) damage, "damage");
		}
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
				&& ((byte) NBTUtils.getNBTItemComponentData(itemStack, "unbreakable")) == 1) {
			return;
		}
		int finalAmount = amount;

		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(itemStack)) {
			int max = HellblockPlugin.getInstance().getFishingManager().getMaxDurability(itemStack);
			finalAmount = Math.min(amount, max);
			itemStack = HellblockPlugin.getInstance().getFishingManager().setCurrentDurability(itemStack, finalAmount);
			int damage = (int) (itemStack.getType().getMaxDurability() * (1 - ((double) finalAmount / max)));
			NBTUtils.setNBTItemComponentData(itemStack, (short) damage, "damage");
			if (updateLore) {
				RtagItem tagItem = updateNBTItemLore(new RtagItem(itemStack));
				itemStack.setItemMeta(tagItem.getItem().getItemMeta());
			}
		} else {
			NBTUtils.setNBTItemComponentData(itemStack, (short) (itemStack.getType().getMaxDurability() - finalAmount),
					"damage");
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

		if (HellblockPlugin.getInstance().getFishingManager().checkMaxDurability(itemStack)) {
			return Pair.of(HellblockPlugin.getInstance().getFishingManager().getMaxDurability(itemStack),
					HellblockPlugin.getInstance().getFishingManager().getCurrentDurability(itemStack));
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
			LogUtils.warn(
					String.format("Detected too many items spawning. Lowering the amount to %s", (maxStackSize * 100)));
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
	
	@Nullable
	public static byte[] toBase64(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return new byte[] { 0 };

		return itemStack.serializeAsBytes();
	}

	@Nullable
	public static ItemStack fromBase64(byte[] base64) {
		if (base64 == null || base64.length == 0)
			return new ItemStack(Material.AIR);

		return ItemStack.deserializeBytes(base64);
	}

	public static boolean checkOwnerData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).hasTag("HellblockData", "owner");
	}

	public static @Nullable String getOwnerData(@Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return new RtagItem(item).getOptional("HellblockData", "owner").asString();
	}

	public static @Nullable ItemStack setOwnerData(@Nullable ItemStack item, String data) {
		if (item == null || item.getType() == Material.AIR || data == null || data.isEmpty())
			return null;

		return RtagItem.edit(item, tag -> {
			tag.set(data, "HellblockData", "owner");
		});
	}

	public static boolean removeOwnerData(ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;

		return new RtagItem(item).remove("HellblockData");
	}

	public static RtagItem removeOwner(ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR)
			return null;

		RtagItem tagItem = new RtagItem(itemStack);
		if (checkOwnerData(itemStack)) {
			removeOwnerData(itemStack);
		}

		return tagItem;
	}
}