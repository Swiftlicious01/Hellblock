package com.swiftlicious.hellblock.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

import java.util.Map;

public class PlayerUtils {

	public static void dropItem(@NotNull Player player, @NotNull ItemStack itemStack, boolean retainOwnership,
			boolean noPickUpDelay, boolean throwRandomly) {
		requireNonNull(player, "player");
		requireNonNull(itemStack, "itemStack");
		final Location location = player.getLocation().clone();
		final Item item = player.getWorld().dropItem(player.getEyeLocation().clone().subtract(new Vector(0, 0.3, 0)),
				itemStack);
		item.setPickupDelay(noPickUpDelay ? 0 : 40);
		item.setOwner(player.getUniqueId());
		if (retainOwnership) {
			item.setThrower(player.getUniqueId());
		}
		if (throwRandomly) {
			final double d1 = RandomUtils.generateRandomDouble(0, 1) * 0.5f;
			final double d2 = RandomUtils.generateRandomDouble(0, 1) * (Math.PI * 2);
			item.setVelocity(new Vector(-Math.sin(d2) * d1, 0.2f, Math.cos(d2) * d1));
		} else {
			final double d1 = Math.sin(location.getPitch() * (Math.PI / 180));
			final double d2 = RandomUtils.generateRandomDouble(0, 0.02);
			final double d3 = RandomUtils.generateRandomDouble(0, 1) * (Math.PI * 2);
			final Vector vector = location.getDirection().clone().multiply(0.3).setY(-d1 * 0.3 + 0.1
					+ (RandomUtils.generateRandomDouble(0, 1) - RandomUtils.generateRandomDouble(0, 1)) * 0.1);
			vector.clone().add(new Vector(Math.cos(d3) * d2, 0, Math.sin(d3) * d2));
			item.setVelocity(vector);
		}
	}

	public static int putItemsToInventory(Inventory inventory, ItemStack itemStack, int amount) {
		final ItemMeta meta = itemStack.getItemMeta();
		final int maxStackSize = itemStack.getMaxStackSize();
		for (ItemStack other : inventory.getStorageContents()) {
			final boolean inventoryCondition = other != null && other.getType() == itemStack.getType()
					&& other.getItemMeta().equals(meta) && other.getAmount() < maxStackSize;
			if (inventoryCondition) {
				final int delta = maxStackSize - other.getAmount();
				if (amount > delta) {
					other.setAmount(maxStackSize);
					amount -= delta;
				} else {
					other.setAmount(amount + other.getAmount());
					return 0;
				}
			}
		}

		if (amount > 0) {
			for (ItemStack other : inventory.getStorageContents()) {
				if (other == null) {
					if (amount > maxStackSize) {
						amount -= maxStackSize;
						final ItemStack cloned = itemStack.clone();
						cloned.setAmount(maxStackSize);
						inventory.addItem(cloned);
					} else {
						final ItemStack cloned = itemStack.clone();
						cloned.setAmount(amount);
						inventory.addItem(cloned);
						return 0;
					}
				}
			}
		}

		return amount;
	}

	public static int giveItem(Player player, ItemStack itemStack, int amount) {
		final PlayerInventory inventory = player.getInventory();
		final ItemMeta meta = itemStack.getItemMeta();
		final int maxStackSize = itemStack.getMaxStackSize();
		if (amount > maxStackSize * 100) {
			amount = maxStackSize * 100;
		}
		final int actualAmount = amount;
		for (ItemStack other : inventory.getStorageContents()) {
			final boolean giveCondition = other != null && other.getType() == itemStack.getType()
					&& other.getItemMeta().equals(meta) && other.getAmount() < maxStackSize;
			if (giveCondition) {
				final int delta = maxStackSize - other.getAmount();
				if (amount > delta) {
					other.setAmount(maxStackSize);
					amount -= delta;
				} else {
					other.setAmount(amount + other.getAmount());
					return actualAmount;
				}
			}
		}
		if (amount > 0) {
			for (ItemStack other : inventory.getStorageContents()) {
				if (other == null) {
					if (amount > maxStackSize) {
						amount -= maxStackSize;
						final ItemStack cloned = itemStack.clone();
						cloned.setAmount(maxStackSize);
						inventory.addItem(cloned);
					} else {
						final ItemStack cloned = itemStack.clone();
						cloned.setAmount(amount);
						inventory.addItem(cloned);
						return actualAmount;
					}
				}
			}
		}

		if (amount > 0) {
			for (int i = 0; i < amount / maxStackSize; i++) {
				final ItemStack cloned = itemStack.clone();
				cloned.setAmount(maxStackSize);
				player.getWorld().dropItem(player.getLocation(), cloned);
			}
			final int left = amount % maxStackSize;
			if (left != 0) {
				final ItemStack cloned = itemStack.clone();
				cloned.setAmount(left);
				player.getWorld().dropItem(player.getLocation(), cloned);
			}
		}

		return actualAmount;
	}

	/**
	 * Removes the items of type from an inventory.
	 * 
	 * @param inventory Inventory to modify
	 * @param type      The type of Material to remove
	 * @param amount    The amount to remove, or {@link Integer.MAX_VALUE} to remove
	 *                  all
	 * @return The amount of items that could not be removed, 0 for success, or -1
	 *         for failures
	 */
	public static int removeItems(Inventory inventory, Material type, int amount) {
		if (type == null || inventory == null) {
			return -1;
		}
		if (amount <= 0) {
			return -1;
		}

		if (amount == Integer.MAX_VALUE) {
			inventory.remove(type);
			return 0;
		}

		final Map<Integer, ItemStack> retVal = inventory.removeItem(new ItemStack(type, amount));

		int notRemoved = 0;
		for (ItemStack item : retVal.values()) {
			notRemoved += item.getAmount();
		}
		return notRemoved;
	}
}