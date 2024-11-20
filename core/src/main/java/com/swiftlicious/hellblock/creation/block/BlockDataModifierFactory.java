package com.swiftlicious.hellblock.creation.block;

/**
 * A functional interface for creating {@link BlockDataModifier} instances.
 */
@FunctionalInterface
public interface BlockDataModifierFactory {

	/**
	 * Creates a {@link BlockDataModifier} based on the provided arguments.
	 *
	 * @param args the arguments used to create the modifier.
	 * @return a BlockDataModifier instance.
	 */
	BlockDataModifier process(Object args);
}