package com.swiftlicious.hellblock.generation;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.utils.RandomUtils;

public class VoidGenerator extends ChunkGenerator {

	@Override
	public List<BlockPopulator> getDefaultPopulators(World world) {
		return List.of();
	}

	@Override
	public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// No need to generate noise, we want an empty world
	}

	@Override
	public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// No need to generate surface, we want an empty world
	}

	@Override
	public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// No need to generate bedrock, we want an empty world
	}

	@Override
	public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ,
			@NotNull ChunkData chunkData) {
		// No need to generate caves, we want an empty world
	}

	@Override
	@Nullable
	public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
		return new VoidBiomeProvider();
	}

	@Override
	public boolean canSpawn(World world, int x, int z) {
		return true;
	}

	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		return new Location(world, 0, 100, 0);
	}

	public class VoidBiomeProvider extends BiomeProvider {

		@Override
		public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
			return RandomUtils.generateRandomBiome();
		}

		@Override
		public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
			return List.of(Biome.NETHER_WASTES, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST, Biome.SOUL_SAND_VALLEY,
					Biome.BASALT_DELTAS);
		}
	}
}
