package com.swiftlicious.hellblock.generation;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * The {@code IslandVariant} enum defines different island configurations for a
 * game or simulation. Each variant includes a specific set of island options
 * and positional offsets for features such as the container, tree, and home
 * location, default biome, container direction along with the orientation (yaw)
 * of the home.
 * 
 * This enum allows flexible management and retrieval of predefined island
 * variants.
 */
public enum IslandVariant {

	DEFAULT(IslandOptions.DEFAULT, new Vector(0, 5, 1), // container offset
			BlockFace.SOUTH, // container direction
			new Vector(0, 5, 0), // tree offset
			new Vector(0.5, 5, 2.5), // home offset
			-175f, // yaw
			HellBiome.NETHER_WASTES// biome
	),

	CLASSIC(IslandOptions.CLASSIC, new Vector(-5, 3, -1), BlockFace.EAST, new Vector(0, 3, -5),
			new Vector(-0.5, 3, -0.5), 90f, HellBiome.NETHER_WASTES);

	private final IslandOptions options;
	private final Vector containerOffset;
	private final BlockFace containerFacing;
	private final Vector treeOffset;
	private final Vector homeOffset;
	private final float homeYaw;
	private final HellBiome hellBiome;

	/**
	 * Constructs an {@code IslandVariant} with the specified configuration.
	 *
	 * @param options         the island options to use
	 * @param containerOffset the offset position for the container
	 * @param containerFacing the direction for the container
	 * @param treeOffset      the offset position for the tree
	 * @param homeOffset      the offset position for the home
	 * @param homeYaw         the yaw (rotation) of the home
	 * @param hellBiome       the Hell biome associated with this variant
	 */
	IslandVariant(IslandOptions options, Vector containerOffset, BlockFace containerFacing, Vector treeOffset,
			Vector homeOffset, float homeYaw, HellBiome hellBiome) {
		this.options = options;
		this.containerOffset = containerOffset;
		this.containerFacing = containerFacing;
		this.treeOffset = treeOffset;
		this.homeOffset = homeOffset;
		this.homeYaw = homeYaw;
		this.hellBiome = hellBiome;
	}

	/**
	 * Returns the {@link IslandOptions} associated with this island variant.
	 *
	 * @return the island options
	 */
	public IslandOptions getOptions() {
		return options;
	}

	/**
	 * Returns the offset vector for the container location.
	 *
	 * @return the container offset
	 */
	public Vector getContainerOffset() {
		return containerOffset;
	}

	/**
	 * Returns the blockface direction for the container.
	 *
	 * @return the container facing direction
	 */
	public BlockFace getContainerFacing() {
		return containerFacing;
	}

	/**
	 * Returns the offset vector for the tree location.
	 *
	 * @return the tree offset
	 */
	public Vector getTreeOffset() {
		return treeOffset;
	}

	/**
	 * Returns the offset vector for the home location.
	 *
	 * @return the home offset
	 */
	public Vector getHomeOffset() {
		return homeOffset;
	}

	/**
	 * Returns the yaw (rotation) value for the home location.
	 *
	 * @return the home yaw in degrees
	 */
	public float getHomeYaw() {
		return homeYaw;
	}

	/**
	 * Returns the Hell biome associated with this island variant.
	 *
	 * @return the Hell biome
	 */
	public HellBiome getHellBiome() {
		return hellBiome;
	}
}