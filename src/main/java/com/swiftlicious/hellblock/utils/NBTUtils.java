package com.swiftlicious.hellblock.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;

/**
 * Utility class for working with NBT (Named Binary Tag) data.
 */
public class NBTUtils {

	private NBTUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Retrieves the item's nbt data
	 * 
	 * @param <T>         The type of data it is
	 * @param bukkitStack The item to get data from
	 * @param key         The path to the data
	 * @return the data in the specific form used
	 */
	public static @Nullable <T> Object getNBTItemComponentData(@Nullable ItemStack bukkitStack, @Nullable String key) {
		if (bukkitStack == null || bukkitStack.getType() == Material.AIR || key == null || key.isEmpty())
			return null;
		RtagItem tag = new RtagItem(bukkitStack);
		Object component = tag.getComponent("minecraft:" + key);
		return ComponentType.encodeJava("minecraft:" + key, component).orElse(null);
	}

	/**
	 * Updates the item's nbt data
	 * 
	 * @param <T>         The type of data it is
	 * @param bukkitStack The item to update
	 * @param value       The data type you want to set
	 * @param key         The path to the data
	 */
	public static <T> void setNBTItemComponentData(@Nullable ItemStack bukkitStack, @Nullable T value,
			@Nullable String key) {
		if (bukkitStack == null || bukkitStack.getType() == Material.AIR || key == null || key.isEmpty()
				|| value == null)
			return;
		RtagItem tag = new RtagItem(bukkitStack);
		tag.setComponent("minecraft:" + key, value);
		tag.load();
		tag.update();
	}

	/**
	 * Removes data from the item
	 * 
	 * @param bukkitStack The item to update
	 * @param key         The path to the data
	 */
	public static void removeNBTItemComponentData(@Nullable ItemStack bukkitStack, @Nullable String key) {
		if (bukkitStack == null || bukkitStack.getType() == Material.AIR || key == null || key.isEmpty())
			return;
		RtagItem tag = new RtagItem(bukkitStack);
		tag.removeComponent("minecraft:" + key);
		tag.load();
		tag.update();
	}

	/**
	 * Checks for data on the item
	 * 
	 * @param bukkitStack The item to check
	 * @param key         The path to the data
	 * @return Whether or not there was data on the item
	 */
	public static boolean hasNBTItemComponentData(@Nullable ItemStack bukkitStack, @Nullable String key) {
		if (bukkitStack == null || bukkitStack.getType() == Material.AIR || key == null || key.isEmpty())
			return false;
		RtagItem tag = new RtagItem(bukkitStack);
		return tag.hasComponent("minecraft:" + key) && tag.getComponent("minecraft:" + key) != null;
	}
}
