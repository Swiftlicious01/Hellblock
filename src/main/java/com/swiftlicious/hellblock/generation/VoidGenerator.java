package com.swiftlicious.hellblock.generation;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

public class VoidGenerator extends ChunkGenerator {

	/**
	 * Generates an empty world!
	 *
	 * @param worldInfo the world to generate chunks in
	 * @param random    a pseudo random number generator
	 * @param x         the chunk's x coordinate
	 * @param z         the chunk's z coordinate
	 * @param chunkData the ChunkData being generated
	 */
	@Override
	public void generateNoise(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunkData) {
		chunkData.setRegion(0, chunkData.getMinHeight(), 0, 16, chunkData.getMaxHeight(), 16, Material.VOID_AIR);
	}

	@Override
	public boolean shouldGenerateNoise() {
		return false;
	}

	@Override
	public boolean shouldGenerateSurface() {
		return false;
	}

	@Override
	public boolean shouldGenerateBedrock() {
		return false;
	}

	@Override
	public boolean shouldGenerateCaves() {
		return false;
	}

	@Override
	public boolean shouldGenerateDecorations() {
		return false;
	}

	@Override
	public boolean shouldGenerateMobs() {
		return false;
	}

	@Override
	public boolean shouldGenerateStructures() {
		return false;
	}

	/**
	 * Gets the fixed spawn location of a world.
	 *
	 * @param world  the world from which to get the spawn location
	 * @param random a pseudo random number generator
	 * @return the spawn location of the world
	 */
	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		return new Location(world, 0, 70, 0);
	}
}
