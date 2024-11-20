package com.swiftlicious.hellblock.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.Getter;
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
	 * Serialize an array of ItemStacks to a Base64-encoded byte array.
	 *
	 * @param contents The ItemStack array to serialize.
	 * @return The Base64-encoded byte array representing the serialized ItemStacks.
	 */
	public static @Nullable byte[] stacksToBase64(ItemStack[] contents) {
		if (contents.length == 0)
			return new byte[] { 0 };
		return ItemStack.serializeItemsAsBytes(contents);
	}

	/**
	 * Deserialize an ItemStack array from a Base64-encoded byte array.
	 *
	 * @param base64 The Base64-encoded byte array representing the serialized
	 *               ItemStacks.
	 * @return An array of ItemStacks deserialized from the input byte array.
	 */
	public static @Nullable ItemStack[] getInventoryItems(byte[] base64) {
		ItemStack[] itemStacks = stacksFromBase64(base64);
		return itemStacks;
	}

	/**
	 * Deserialize an array of ItemStacks from a Base64-encoded byte array.
	 *
	 * @param contents The byte array to deserialize.
	 * @return The Base64-encoded ItemStack array representing the serialized
	 *         ItemStacks.
	 */
	private static @Nullable ItemStack[] stacksFromBase64(byte[] data) {
		if (data == null || data.length == 0)
			return new ItemStack[] {};
		return ItemStack.deserializeItemsFromBytes(data);
	}

	public static PacketContainer getOpenWindowPacket(int windowID, EnumInventoryType type, String title, int slots) {
		PacketContainer openWindowPacket = new PacketContainer(PacketType.Play.Server.OPEN_WINDOW);
		openWindowPacket.getIntegers().write(0, windowID);
		openWindowPacket.getStrings().write(0, type.minecraft());
		openWindowPacket.getChatComponents().write(0, WrappedChatComponent.fromText(title));
		openWindowPacket.getIntegers().write(1, slots);
		return openWindowPacket;
	}

	@Getter
	public enum EnumInventoryType {
		ENDER_CHEST("minecraft:container", InventoryType.ENDER_CHEST), CHEST("minecraft:chest", InventoryType.CHEST),
		ANVIL("minecraft:anvil", InventoryType.ANVIL), HOPPER("minecraft:hopper", InventoryType.HOPPER);

		private final String minecraft;
		private final InventoryType bukkit;

		EnumInventoryType(String minecraft, InventoryType bukkit) {
			this.minecraft = minecraft;
			this.bukkit = bukkit;
		}

		public String minecraft() {
			return this.minecraft;
		}
	}
}