package com.swiftlicious.hellblock.generation;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * A custom {@link ChunkGenerator} that creates a completely empty (void) world.
 * <p>
 * This generator disables all terrain generation, resulting in chunks that are
 * entirely empty—ideal for skyblock-style worlds or custom plugin-driven island
 * generation. Blocks are only placed when explicitly populated by external
 * logic.
 * <p>
 * Compatibility: Works with Minecraft 1.17 through 1.21+. Uses dynamic
 * ByteBuddy proxying to support version-specific APIs like {@code WorldInfo}
 * and {@code BiomeProvider} where available.
 */
public class VoidGenerator extends ChunkGenerator {

	/**
	 * Returns no populators, as the world is void and block population is
	 * unnecessary.
	 *
	 * @param world the world being generated
	 * @return an empty list of {@link BlockPopulator}s
	 */
	@NotNull
	@Override
	public List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
		return List.of();
	}

	/**
	 * Generates an empty chunk with no terrain or biome features.
	 *
	 * @param world  the world being generated
	 * @param random the RNG instance
	 * @param chunkX the chunk's X coordinate
	 * @param chunkZ the chunk's Z coordinate
	 * @param biome  the biome grid (not used)
	 * @return an empty {@link ChunkData} object
	 */
	@SuppressWarnings("deprecation")
	@NotNull
	@Override
	public ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull BiomeGrid biome) {
		return createChunkData(world); // empty chunk, no blocks placed
	}

	/**
	 * Skips terrain noise generation to ensure the chunk remains empty.
	 */
	public void generateNoise(@NotNull Object worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// Intentionally empty: no terrain noise
	}

	/**
	 * Skips surface generation (e.g., grass, sand).
	 */
	public void generateSurface(@NotNull Object worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// Intentionally empty: no surface blocks
	}

	/**
	 * Skips bedrock generation entirely.
	 */
	public void generateBedrock(@NotNull Object worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// Intentionally empty: no bedrock
	}

	/**
	 * Skips cave generation entirely.
	 */
	public void generateCaves(@NotNull Object worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// Intentionally empty: no caves
	}

	/**
	 * Returns a custom {@link BiomeProvider} that assigns
	 * {@link Biome#NETHER_WASTES} to all coordinates.
	 * <p>
	 * This is accessed reflectively by ByteBuddy-injected proxy classes on
	 * supported versions.
	 *
	 * @param worldInfo the world information object (may be null)
	 * @return an instance of {@link VoidBiomeProvider}, or null on failure
	 */
	@Nullable
	public Object getDefaultBiomeProvider(@NotNull Object worldInfo) {
		try {
			Class<?> clazz = Class.forName("com.swiftlicious.hellblock.generation.VoidGenerator$VoidBiomeProvider");
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Throwable ignored) {
			return null;
		}
	}

	/**
	 * Allows spawning anywhere within the world. There are no spawn restrictions.
	 *
	 * @param world the world
	 * @param x     the X coordinate
	 * @param z     the Z coordinate
	 * @return always {@code true}
	 */
	@Override
	public boolean canSpawn(@NotNull World world, int x, int z) {
		return true;
	}

	/**
	 * Returns a fixed spawn location at the center of the world (0, Y, 0), where Y
	 * is configured externally by the plugin.
	 *
	 * @param world  the world
	 * @param random the RNG instance
	 * @return a {@link Location} object representing the spawn point
	 */

	@NotNull
	@Override
	public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
		int y = HellblockPlugin.getInstance().getConfigManager().height();
		return new Location(world, 0, y, 0);
	}

	/**
	 * A simple {@link BiomeProvider} implementation that returns
	 * {@link Biome#NETHER_WASTES} for all coordinates.
	 * <p>
	 * This is used via a ByteBuddy proxy to dynamically implement the
	 * {@code BiomeProvider} interface where supported.
	 */
	public static class VoidBiomeProvider {

		/**
		 * Always returns {@link Biome#NETHER_WASTES}, regardless of position.
		 *
		 * @param worldInfo unused world info
		 * @param x         the X coordinate
		 * @param y         the Y coordinate
		 * @param z         the Z coordinate
		 * @return {@link Biome#NETHER_WASTES}
		 */
		@NotNull
		public Biome getBiome(@NotNull Object worldInfo, int x, int y, int z) {
			return Biome.NETHER_WASTES;
		}

		/**
		 * Returns a singleton list containing only {@link Biome#NETHER_WASTES}. This
		 * defines the complete set of biomes used by the provider.
		 *
		 * @param worldInfo unused world info
		 * @return a list with a single {@link Biome#NETHER_WASTES} element
		 */
		@NotNull
		public List<Biome> getBiomes(@NotNull Object worldInfo) {
			return List.of(Biome.NETHER_WASTES);
		}
	}
}