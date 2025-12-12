package com.swiftlicious.hellblock.world;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

/**
 * Interface representing the state of a custom block in the Hellblock plugin.
 */
public interface CustomBlockStateInterface {

	/**
	 * Retrieves the type of the custom block associated with this state.
	 *
	 * @return The {@link CustomBlock} type of this block state.
	 */
	@NotNull
	CustomBlock type();

	/**
	 * Determines whether this block state represents an air-like block.
	 * <p>
	 * This checks the internal {@link CustomBlock}'s {@link Key} value against
	 * known identifiers for air or empty blocks.
	 *
	 * @return {@code true} if this block state represents air, {@code false}
	 *         otherwise.
	 */
	boolean isAir();

	/**
	 * Determines whether this block state has an inventory associated with it.
	 * <p>
	 * For example, chests, barrels, hoppers, and furnaces would typically contain
	 * an inventory, while most other blocks would not.
	 *
	 * @return {@code true} if this block state represents a block that contains an
	 *         inventory, {@code false} otherwise.
	 */
	default boolean hasInventory() {
		return false; // default: no inventory
	}

	/**
	 * Clears the inventory data associated with this block state, if any.
	 * <p>
	 * Implementations should remove all stored item data (usually from the "Items"
	 * NBT tag or a similar compound) while preserving other block state data.
	 * <p>
	 * Calling this on a non-inventory block should have no effect.
	 */
	void clearInventory();

	/**
	 * Creates a new instance of {@link CustomBlockState} with the given block type
	 * and NBT data.
	 *
	 * @param owner    The custom block type that owns this state.
	 * @param compound The NBT data associated with this block state.
	 * @return A new instance of {@link CustomBlockState}.
	 */
	static CustomBlockState create(CustomBlock owner, CompoundBinaryTag compound) {
		return new CustomBlockState(owner, compound);
	}

	/**
	 * Creates a new instance of {@link CustomBlockState} by deserializing the
	 * provided NBT byte array.
	 *
	 * @param owner    The custom block type that owns this state.
	 * @param nbtBytes The serialized NBT data as a byte array.
	 * @return A new instance of {@link CustomBlockState}.
	 * @throws RuntimeException If deserialization fails.
	 */
	@ApiStatus.Internal
	static CustomBlockState create(CustomBlock owner, byte[] nbtBytes) {
		try (ByteArrayInputStream input = new ByteArrayInputStream(nbtBytes)) {
			final CompoundBinaryTag compound = BinaryTagIO.reader().read(input);
			return new CustomBlockState(owner, compound);
		} catch (IOException e) {
			throw new RuntimeException("Failed to deserialize NBT", e);
		}
	}

	/**
	 * Serializes the NBT data of this block state to a byte array.
	 *
	 * @return The serialized NBT data as a byte array.
	 */
	@ApiStatus.Internal
	byte[] getNBTDataAsBytes();

	/**
	 * Provides a string representation of this block state, including its type and
	 * NBT data.
	 * 
	 * @return A string representation of this block state.
	 */
	@ApiStatus.Internal
	String asString();
}