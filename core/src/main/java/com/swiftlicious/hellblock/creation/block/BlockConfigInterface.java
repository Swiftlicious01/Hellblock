package com.swiftlicious.hellblock.creation.block;

import java.util.List;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.creation.block.BlockConfig.Builder;
import com.swiftlicious.hellblock.utils.extras.MathValue;

/**
 * Interface representing the configuration for a block loot.
 */
public interface BlockConfigInterface {

	MathValue<Player> DEFAULT_HORIZONTAL_VECTOR = MathValue.plain(1.1);
	MathValue<Player> DEFAULT_VERTICAL_VECTOR = MathValue.plain(1.2);

	/**
	 * Gets the ID
	 *
	 * @return the ID.
	 */
	String id();

	/**
	 * Gets the unique identifier for the block.
	 *
	 * @return The block's unique identifier.
	 */
	String blockID();

	/**
	 * Retrieves the horizontal vector value for the block.
	 *
	 * @return the horizontal vector value as a double
	 */
	MathValue<Player> horizontalVector();

	/**
	 * Retrieves the vertical vector value for the block.
	 *
	 * @return the vertical vector value as a double
	 */
	MathValue<Player> verticalVector();

	/**
	 * Gets the list of data modifiers applied to the block.
	 *
	 * @return A list of {@link BlockDataModifier} objects.
	 */
	List<BlockDataModifier> dataModifier();

	/**
	 * Gets the list of state modifiers applied to the block.
	 *
	 * @return A list of {@link BlockStateModifier} objects.
	 */
	List<BlockStateModifier> stateModifiers();

	/**
	 * Creates a new builder instance for constructing a {@link BlockConfig}.
	 *
	 * @return A new {@link Builder} instance.
	 */
	static Builder builder() {
		return new BlockConfig.Builder();
	}

	/**
	 * Builder interface for constructing a {@link BlockConfig} instance.
	 */
	interface BuilderInterface {

		/**
		 * Sets the ID
		 *
		 * @return the current Builder instance
		 */
		Builder id(String id);

		/**
		 * Sets the block ID for the configuration.
		 *
		 * @param blockID The block's unique identifier.
		 * @return The current {@link Builder} instance.
		 */
		Builder blockID(String blockID);

		/**
		 * Sets the vertical vector value for the BlockConfig being built.
		 *
		 * @param value the vertical vector value as a double
		 * @return the current Builder instance
		 */
		Builder verticalVector(MathValue<Player> value);

		/**
		 * Sets the horizontal vector value for the BlockConfig being built.
		 *
		 * @param value the horizontal vector value as a double
		 * @return the current Builder instance
		 */
		Builder horizontalVector(MathValue<Player> value);

		/**
		 * Sets the list of data modifiers for the configuration.
		 *
		 * @param dataModifierList A list of {@link BlockDataModifier} objects.
		 * @return The current {@link Builder} instance.
		 */
		Builder dataModifierList(List<BlockDataModifier> dataModifierList);

		/**
		 * Sets the list of state modifiers for the configuration.
		 *
		 * @param stateModifierList A list of {@link BlockStateModifier} objects.
		 * @return The current {@link Builder} instance.
		 */
		Builder stateModifierList(List<BlockStateModifier> stateModifierList);

		/**
		 * Builds and returns the configured {@link BlockConfig} instance.
		 *
		 * @return The constructed {@link BlockConfig} instance.
		 */
		BlockConfig build();
	}
}