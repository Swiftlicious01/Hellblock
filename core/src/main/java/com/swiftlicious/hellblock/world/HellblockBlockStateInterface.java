package com.swiftlicious.hellblock.world;

import org.jetbrains.annotations.NotNull;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.swiftlicious.hellblock.utils.TagUtils;

import org.jetbrains.annotations.ApiStatus;

/**
 * Interface representing the state of a custom block in the Hellblock plugin.
 */
public interface HellblockBlockStateInterface extends DataBlock {

	/**
	 * Retrieves the type of the custom block associated with this state.
	 *
	 * @return The {@link HellblockBlock} type of this block state.
	 */
	@NotNull
	HellblockBlock type();

	/**
	 * Creates a new instance of {@link HellblockBlockState} with the given block
	 * type and NBT data.
	 *
	 * @param owner       The custom block type that owns this state.
	 * @param compoundMap The NBT data associated with this block state.
	 * @return A new instance of {@link HellblockBlockState} representing the
	 *         specified block type and state.
	 */
	static HellblockBlockState create(HellblockBlock owner, CompoundMap compoundMap) {
		return new HellblockBlockState(owner, compoundMap);
	}

	@ApiStatus.Internal
	static HellblockBlockState create(HellblockBlock owner, byte[] nbtBytes) {
		return new HellblockBlockState(owner, ((CompoundTag) TagUtils.fromBytes(nbtBytes)).getValue());
	}

	@ApiStatus.Internal
	byte[] getNBTDataAsBytes();

	String asString();
}