package com.swiftlicious.hellblock.generation;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a request to generate an island with specific configuration
 * options.
 * <p>
 * This can either be a predefined island variant (e.g., DEFAULT or CLASSIC), or
 * a schematic-based island with custom schematic data and author information.
 * <p>
 * For DEFAULT and CLASSIC types, {@code schematicName} and {@code author} are
 * null.
 *
 * @param options         the island options used for generation
 * @param container       the relative position of the container from the paste
 *                        origin
 * @param containerFacing the direction of the container
 * @param tree            the relative position of the tree from the paste
 *                        origin
 * @param home            the relative position of the home from the paste
 *                        origin
 * @param homeYaw         the yaw (rotation) of the home
 * @param biome           optional Hell biome; NETHER_WASTES for predefined
 *                        variants
 * @param schematicName   optional schematic name; null for predefined variants
 * @param author          optional schematic author; null for predefined
 *                        variants
 */
public record IslandGenerationRequest(IslandOptions options, Vector container, @Nullable BlockFace containerFacing,
		Vector tree, Vector home, float homeYaw, HellBiome biome, @Nullable String schematicName,
		@Nullable String author) {

	/**
	 * Creates an {@code IslandGenerationRequest} from a predefined
	 * {@link IslandVariant}.
	 * <p>
	 * This will result in a request with null schematic-related fields.
	 *
	 * @param variant the island variant to convert
	 * @return an {@code IslandGenerationRequest} representing the variant
	 */
	public static IslandGenerationRequest fromVariant(IslandVariant variant) {
		return new IslandGenerationRequest(variant.getOptions(), variant.getContainerOffset(),
				variant.getContainerFacing(), variant.getTreeOffset(), variant.getHomeOffset(), variant.getHomeYaw(),
				variant.getHellBiome(), null, // schematic name
				null // author
		);
	}

	/**
	 * Returns whether this island generation request is based on a schematic.
	 *
	 * @return {@code true} if {@code schematicName} is not null and not empty,
	 *         {@code false} otherwise
	 */
	public boolean isSchematic() {
		return schematicName != null && !schematicName.isEmpty();
	}
}