package com.swiftlicious.hellblock.utils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;

/**
 * Utility class for managing firework rockets using PacketContainers.
 */
public class FireworkUtils {

	private FireworkUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static PacketContainer getSpawnFireworkPacket(int id, Location location) {
		PacketContainer fireworkSpawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		fireworkSpawnPacket.getIntegers().write(0, id);
		fireworkSpawnPacket.getUUIDs().write(0, UUID.randomUUID());
		fireworkSpawnPacket.getEntityTypeModifier().write(0, EntityType.FIREWORK_ROCKET);
		fireworkSpawnPacket.getDoubles().write(0, location.getX());
		fireworkSpawnPacket.getDoubles().write(1, location.getY() - 0.5D);
		fireworkSpawnPacket.getDoubles().write(2, location.getZ());
		return fireworkSpawnPacket;
	}

	public static PacketContainer getFireworkMetaPacket(int id, ItemStack itemStack) {
		PacketContainer fireworkMetaPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		fireworkMetaPacket.getIntegers().write(0, id);
		WrappedDataWatcher wrappedDataWatcher = createDataWatcher(itemStack);
		setValueList(fireworkMetaPacket, wrappedDataWatcher);
		return fireworkMetaPacket;
	}

	public static PacketContainer getFireworkStatusPacket(int id) {
		PacketContainer fireworkStatusPacket = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);
		fireworkStatusPacket.getIntegers().write(0, id);
		fireworkStatusPacket.getBytes().write(0, (byte) 17);
		return fireworkStatusPacket;
	}

	public static PacketContainer getFireworkDestroyPacket(int id) {
		PacketContainer fireworkDestroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		fireworkDestroyPacket.getIntLists().write(0, List.of(id));
		return fireworkDestroyPacket;
	}

	/**
	 * Creates a DataWatcher for a given ItemStack.
	 *
	 * @param itemStack The ItemStack to create the DataWatcher for
	 * @return The created DataWatcher
	 */
	public static WrappedDataWatcher createDataWatcher(ItemStack itemStack) {
		WrappedDataWatcher wrappedDataWatcher = new WrappedDataWatcher();
		wrappedDataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(8,
				WrappedDataWatcher.Registry.getItemStackSerializer(false)), itemStack);
		return wrappedDataWatcher;
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
}
