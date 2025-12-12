package com.swiftlicious.hellblock.schematic;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.generation.HellBiome;

/**
 * Represents metadata associated with a schematic used for island generation.
 *
 * <p>
 * This metadata includes structural points of interest (e.g., home, container,
 * tree), the facing direction (yaw), the author of the schematic, and the
 * intended biome.
 *
 * <p>
 * Used during schematic pasting to correctly configure gameplay elements and
 * environmental attributes for the generated island.
 *
 * @see Vector
 * @see HellBiome
 */
public class SchematicMetadata {

	private final @Nullable Vector home;
	private final @Nullable Vector container;
	private final @Nullable Vector tree;
	private final float yaw;
	private final @Nullable String author;
	private final @NotNull HellBiome biome;

	public SchematicMetadata(@Nullable Vector home, @Nullable Vector container, @Nullable Vector tree, float yaw,
			@Nullable String author, @NotNull HellBiome biome) {
		this.home = home;
		this.container = container;
		this.tree = tree;
		this.yaw = yaw;
		this.author = author;
		this.biome = biome;
	}

	/**
	 * Gets the vector location representing the home/spawn point within the
	 * schematic.
	 *
	 * @return the home position, or {@code null} if not defined
	 */
	@Nullable
	public Vector getHome() {
		return home;
	}

	/**
	 * Gets the vector location representing the container position within the
	 * schematic.
	 *
	 * @return the container position, or {@code null} if not defined
	 */
	@Nullable
	public Vector getContainer() {
		return container;
	}

	/**
	 * Gets the vector location representing the tree position within the schematic.
	 *
	 * @return the tree position, or {@code null} if not defined
	 */
	@Nullable
	public Vector getTree() {
		return tree;
	}

	/**
	 * Gets the yaw (rotation angle) to apply to the player upon spawning.
	 *
	 * @return the yaw in degrees
	 */
	public float getYaw() {
		return yaw;
	}

	/**
	 * Gets the author name of the schematic file.
	 *
	 * @return the author's name, or {@code null} if not specified
	 */
	@Nullable
	public String getAuthor() {
		return author;
	}

	/**
	 * Gets the biome to apply to the schematic area after pasting.
	 *
	 * @return the configured {@link HellBiome}, never {@code null}
	 */
	@NotNull
	public HellBiome getBiome() {
		return biome;
	}
}