package com.swiftlicious.hellblock.creation.block;

/**
 * Functional interface for creating {@link BlockStateModifier} instances.
 */
@FunctionalInterface
public interface BlockStateModifierFactory {

	/**
	 * Creates a {@link BlockStateModifier} based on the provided arguments.
	 *
	 * @param args the arguments used to create the modifier.
	 * @return a BlockStateModifier instance.
	 */
	BlockStateModifier process(Object args);
}