package com.swiftlicious.hellblock.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import com.google.common.collect.Lists;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Utility class for managing armor stands and sending related packets.
 */
public class ArmorStandUtils {

	private ArmorStandUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Creates a destroy packet for removing an armor stand entity.
	 *
	 * @param id The ID of the armor stand entity to destroy
	 * @return The PacketContainer representing the destroy packet
	 */
	public static PacketContainer getDestroyPacket(int id) {
		PacketContainer destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		destroyPacket.getIntLists().write(0, List.of(id));
		return destroyPacket;
	}

	/**
	 * Creates a spawn packet for an armor stand entity at the specified location.
	 *
	 * @param id       The ID of the armor stand entity to spawn
	 * @param location The location where the armor stand entity should be spawned
	 * @return The PacketContainer representing the spawn packet
	 */
	public static PacketContainer getSpawnPacket(int id, Location location) {
		PacketContainer entityPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		try {
			entityPacket.getModifier().write(0, id);
			entityPacket.getModifier().write(1, UUID.randomUUID());
			entityPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
			entityPacket.getDoubles().write(0, location.getX());
			entityPacket.getDoubles().write(1, location.getY());
			entityPacket.getDoubles().write(2, location.getZ());
			entityPacket.getBytes().write(0, (byte) ((location.getYaw() % 360) * 128 / 180));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return entityPacket;
	}

	/**
	 * Creates a metadata packet for updating the metadata of an armor stand entity.
	 *
	 * @param id The ID of the armor stand entity
	 * @return The PacketContainer representing the metadata packet
	 */
	public static PacketContainer getMetaPacket(int id) {
		PacketContainer metaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metaPacket.getIntegers().write(0, id);
		WrappedDataWatcher wrappedDataWatcher = createDataWatcher();
		setValueList(metaPacket, wrappedDataWatcher);
		return metaPacket;
	}

	/**
	 * Sets the value list in a PacketContainer's DataWatcher from a
	 * WrappedDataWatcher.
	 *
	 * @param metaPacket         The PacketContainer representing the metadata
	 *                           packet
	 * @param wrappedDataWatcher The WrappedDataWatcher containing the value list
	 */
	private static void setValueList(PacketContainer metaPacket, WrappedDataWatcher wrappedDataWatcher) {
		List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
		wrappedDataWatcher.getWatchableObjects().stream().filter(Objects::nonNull).forEach(entry -> {
			final WrappedDataWatcher.WrappedDataWatcherObject dataWatcherObject = entry.getWatcherObject();
			wrappedDataValueList.add(new WrappedDataValue(dataWatcherObject.getIndex(),
					dataWatcherObject.getSerializer(), entry.getRawValue()));
		});
		metaPacket.getDataValueCollectionModifier().write(0, wrappedDataValueList);
	}

	/**
	 * Creates a metadata packet for updating the metadata of an armor stand entity
	 * with a custom Component.
	 *
	 * @param id        The ID of the armor stand entity
	 * @param component The Component to set as metadata
	 * @return The PacketContainer representing the metadata packet
	 */
	public static PacketContainer getMetaPacket(int id, Component component) {
		PacketContainer metaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metaPacket.getIntegers().write(0, id);
		WrappedDataWatcher wrappedDataWatcher = createDataWatcher(component);
		setValueList(metaPacket, wrappedDataWatcher);
		return metaPacket;
	}

	/**
	 * Creates a DataWatcher for an invisible armor stand entity.
	 *
	 * @return The created DataWatcher
	 */
	public static WrappedDataWatcher createDataWatcher() {
		WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
		WrappedDataWatcher.Serializer serializer1 = WrappedDataWatcher.Registry.get(Boolean.class);
		WrappedDataWatcher.Serializer serializer2 = WrappedDataWatcher.Registry.get(Byte.class);
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, serializer1), false);
		byte flag = 0x20;
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, serializer2), flag);
		return wrappedDataWatcher;
	}

	/**
	 * Creates a DataWatcher for an invisible armor stand entity with a custom
	 * Component.
	 *
	 * @param component The Component to set in the DataWatcher
	 * @return The created DataWatcher
	 */
	public static WrappedDataWatcher createDataWatcher(Component component) {
		WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
		WrappedDataWatcher.Serializer serializer1 = WrappedDataWatcher.Registry.get(Boolean.class);
		WrappedDataWatcher.Serializer serializer2 = WrappedDataWatcher.Registry.get(Byte.class);
		wrappedDataWatcher.setObject(
				new WrappedDataWatcher.WrappedDataWatcherObject(2,
						WrappedDataWatcher.Registry.getChatComponentSerializer(true)),
				Optional.of(WrappedChatComponent.fromJson(GsonComponentSerializer.gson().serialize(component))
						.getHandle()));
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, serializer1), true);
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(15, serializer2), (byte) 0x01);
		byte flag = 0x20;
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, serializer2), flag);
		return wrappedDataWatcher;
	}

	/**
	 * Creates an equipment packet for equipping an armor stand with an ItemStack.
	 *
	 * @param id        The ID of the armor stand entity
	 * @param itemStack The ItemStack to equip
	 * @return The PacketContainer representing the equipment packet
	 */
	public static PacketContainer getEquipPacket(int id, ItemStack itemStack) {
		PacketContainer equipPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
		equipPacket.getIntegers().write(0, id);
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = new ArrayList<>();
		pairs.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, itemStack));
		equipPacket.getSlotStackPairLists().write(0, pairs);
		return equipPacket;
	}

	/**
	 * Sends a fake armor stand entity with item on head to a player at the
	 * specified location.
	 *
	 * @param player    The player to send the entity to
	 * @param location  The location where the entity should appear
	 * @param itemStack The ItemStack to represent the entity
	 * @param seconds   The duration (in seconds) the entity should be displayed
	 */
	public static void sendFakeItem(Player player, Location location, ItemStack itemStack, int seconds) {
		int id = new Random().nextInt(Integer.MAX_VALUE);
		HellblockPlugin.getInstance().sendPackets(player, getSpawnPacket(id, location.clone().subtract(0, 1, 0)),
				getMetaPacket(id), getEquipPacket(id, itemStack));
		HellblockPlugin.getInstance().getScheduler().runTaskAsyncLater(
				() -> HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, getDestroyPacket(id)),
				seconds * 50L, TimeUnit.MILLISECONDS);
	}

	/**
	 * Sends a hologram (armor stand with custom text) to a player at the specified
	 * location.
	 *
	 * @param player    The player to send the hologram to
	 * @param location  The location where the hologram should appear
	 * @param component The Component representing the hologram's text
	 * @param seconds   The duration (in seconds) the hologram should be displayed
	 */
	public static void sendHologram(Player player, Location location, Component component, int seconds) {
		int id = new Random().nextInt(Integer.MAX_VALUE);
		HellblockPlugin.getInstance().sendPackets(player, getSpawnPacket(id, location.clone().subtract(0, 1, 0)),
				getMetaPacket(id, component));
		HellblockPlugin.getInstance().getScheduler().runTaskAsyncLater(
				() -> HellblockPlugin.getInstance().getProtocolManager().sendServerPacket(player, getDestroyPacket(id)),
				seconds * 50L, TimeUnit.MILLISECONDS);
	}
}