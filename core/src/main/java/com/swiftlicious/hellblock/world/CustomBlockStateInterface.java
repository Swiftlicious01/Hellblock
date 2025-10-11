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