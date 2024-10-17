package com.swiftlicious.hellblock.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.kyori.adventure.text.Component;

/**
 * Utility class for working with Bukkit Inventories and item stacks.
 */
public class InventoryUtils {

	private InventoryUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Create a custom inventory with a specified size and title component.
	 *
	 * @param inventoryHolder The holder of the inventory.
	 * @param size            The size of the inventory.
	 * @param component       The title component of the inventory.
	 * @return The created Inventory instance.
	 */
	public static Inventory createInventory(InventoryHolder inventoryHolder, int size, Component component) {
		try {
			boolean isSpigot = HellblockPlugin.getInstance().getVersionManager().isSpigot();
			Method createInvMethod = (isSpigot ? Bukkit.class : ReflectionUtils.bukkitClass).getMethod(
					"createInventory", InventoryHolder.class, int.class,
					isSpigot ? String.class : ReflectionUtils.componentClass);
			return (Inventory) createInvMethod.invoke(null, inventoryHolder, size,
					isSpigot ? HellblockPlugin.getInstance().getAdventureManager().componentToLegacy(component)
							: HellblockPlugin.getInstance().getAdventureManager()
									.shadedComponentToOriginalComponent(component));
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	/**
	 * Create a custom inventory with a specified type and title component.
	 *
	 * @param inventoryHolder The holder of the inventory.
	 * @param type            The type of the inventory.
	 * @param component       The title component of the inventory.
	 * @return The created Inventory instance.
	 */
	public static Inventory createInventory(InventoryHolder inventoryHolder, InventoryType type, Component component) {
		try {
			boolean isSpigot = HellblockPlugin.getInstance().getVersionManager().isSpigot();
			Method createInvMethod = (isSpigot ? Bukkit.class : ReflectionUtils.bukkitClass).getMethod(
					"createInventory", InventoryHolder.class, InventoryType.class,
					isSpigot ? String.class : ReflectionUtils.componentClass);
			return (Inventory) createInvMethod.invoke(null, inventoryHolder, type,
					isSpigot ? HellblockPlugin.getInstance().getAdventureManager().componentToLegacy(component)
							: HellblockPlugin.getInstance().getAdventureManager()
									.shadedComponentToOriginalComponent(component));
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException exception) {
			exception.printStackTrace();
			return null;
		}
	}

	/**
	 * Serialize an array of ItemStacks to a Base64-encoded string.
	 *
	 * @param contents The ItemStack array to serialize.
	 * @return The Base64-encoded string representing the serialized ItemStacks.
	 */
	public static @NotNull String stacksToBase64(ItemStack[] contents) {
		if (contents.length == 0) {
			return "";
		}
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream);
			dataOutput.writeInt(contents.length);
			for (ItemStack itemStack : contents) {
				dataOutput.writeObject(itemStack);
			}
			dataOutput.close();
			byte[] byteArr = outputStream.toByteArray();
			outputStream.close();
			return Base64Coder.encodeLines(byteArr);
		} catch (IOException e) {
			LogUtils.warn("Encoding error", e);
		}
		return "";
	}

	/**
	 * Deserialize an ItemStack array from a Base64-encoded string.
	 *
	 * @param base64 The Base64-encoded string representing the serialized
	 *               ItemStacks.
	 * @return An array of ItemStacks deserialized from the input string.
	 */
	@Nullable
	public static ItemStack[] getInventoryItems(String base64) {
		ItemStack[] itemStacks = null;
		try {
			itemStacks = stacksFromBase64(base64);
		} catch (IllegalArgumentException exception) {
			exception.printStackTrace();
		}
		return itemStacks;
	}

	private static ItemStack[] stacksFromBase64(String data) {
		if (data == null || data.equals(""))
			return new ItemStack[] {};

		ByteArrayInputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
		} catch (IllegalArgumentException e) {
			return new ItemStack[] {};
		}
		ObjectInputStream dataInput = null;
		ItemStack[] stacks = null;
		try {
			dataInput = new ObjectInputStream(inputStream);
			stacks = new ItemStack[dataInput.readInt()];
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (stacks == null)
			return new ItemStack[] {};
		for (int i = 0; i < stacks.length; i++) {
			try {
				stacks[i] = (ItemStack) dataInput.readObject();
			} catch (IOException | ClassNotFoundException | NullPointerException e) {
				try {
					dataInput.close();
				} catch (IOException exception) {
					LogUtils.severe("Failed to read data");
				}
				return null;
			}
		}
		try {
			dataInput.close();
		} catch (IOException ignored) {
		}
		return stacks;
	}
}
