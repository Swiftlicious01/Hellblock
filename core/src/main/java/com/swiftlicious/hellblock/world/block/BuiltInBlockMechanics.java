package com.swiftlicious.hellblock.world.block;

import java.util.Objects;

import com.flowpowered.nbt.CompoundMap;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.registry.InternalRegistries;
import com.swiftlicious.hellblock.world.HellblockBlockState;

/**
 * BuiltInBlockMechanics defines a set of standard block mechanics for the
 * Hellblock plugin.
 */
public class BuiltInBlockMechanics {

	public static final BuiltInBlockMechanics CROP = create("crop");
	public static final BuiltInBlockMechanics DEAD_CROP = create("dead_crop");

	private final Key key;

	/**
	 * Constructs a new BuiltInBlockMechanics with a unique key.
	 *
	 * @param key the unique key for this mechanic
	 */
	private BuiltInBlockMechanics(Key key) {
		this.key = key;
	}

	/**
	 * Factory method to create a new BuiltInBlockMechanics instance with the
	 * specified ID.
	 *
	 * @param id the ID of the mechanic
	 * @return a new BuiltInBlockMechanics instance
	 */
	static BuiltInBlockMechanics create(String id) {
		return new BuiltInBlockMechanics(Key.of("hellblock", id));
	}

	/**
	 * Retrieves the unique key associated with this block mechanic.
	 *
	 * @return the key
	 */
	public Key key() {
		return key;
	}

	/**
	 * Creates a new HellblockBlockState using the associated block mechanic.
	 *
	 * @return a new HellblockBlockState
	 */
	public HellblockBlockState createBlockState() {
		return mechanic().createBlockState();
	}

	/**
	 * Creates a new HellblockBlockState using the associated block mechanic and
	 * provided data.
	 *
	 * @param data the compound map data for the block state
	 * @return a new HellblockBlockState
	 */
	public HellblockBlockState createBlockState(CompoundMap data) {
		return mechanic().createBlockState(data);
	}

	/**
	 * Retrieves the HellblockBlock associated with this block mechanic.
	 *
	 * @return the HellblockBlock
	 */
	@SuppressWarnings("deprecation")
	public HellblockBlock mechanic() {
		return Objects.requireNonNull(InternalRegistries.BLOCK.get(key));
	}
}