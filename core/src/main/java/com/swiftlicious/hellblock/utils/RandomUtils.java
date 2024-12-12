package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.generation.HellBiome;

/**
 * Utility class for generating random values.
 */
public class RandomUtils {

	private final Random random;

	private RandomUtils() {
		random = ThreadLocalRandom.current();
	}

	/**
	 * Static inner class to hold the singleton instance of RandomUtils.
	 */
	private static class SingletonHolder {
		private static final RandomUtils INSTANCE = new RandomUtils();
	}

	/**
	 * Retrieves the singleton instance of RandomUtils.
	 *
	 * @return the singleton instance
	 */
	private static RandomUtils getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Generates a random integer between the specified minimum and maximum values
	 * (inclusive).
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return a random integer between min and max (inclusive)
	 */
	public static int generateRandomInt(int min, int max) {
		return getInstance().random.nextInt(max - min + 1) + min;
	}

	/**
	 * Generates a random double between the specified minimum and maximum values
	 * (inclusive).
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return a random double between min and max (inclusive)
	 */
	public static double generateRandomDouble(double min, double max) {
		return min + (max - min) * getInstance().random.nextDouble();
	}

	/**
	 * Generates a random float between the specified minimum and maximum values
	 * (inclusive).
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return a random float between min and max (inclusive)
	 */
	public static float generateRandomFloat(float min, float max) {
		return min + (max - min) * getInstance().random.nextFloat();
	}

	/**
	 * Generates a random boolean value.
	 *
	 * @return a random boolean value
	 */
	public static boolean generateRandomBoolean() {
		return getInstance().random.nextBoolean();
	}

	/**
	 * Generates a random integer under the bound value (inclusive).
	 *
	 * @param bound the bound
	 * @return a random integer within the bound (inclusive)
	 */
	public static int generateRandomInt(int bound) {
		return getInstance().random.nextInt(bound);
	}

	/**
	 * Generates a random double value.
	 *
	 * @return a random double value
	 */
	public static double generateRandomDouble() {
		return getInstance().random.nextDouble();
	}

	/**
	 * Selects a random element from the specified array.
	 *
	 * @param array the array to select a random element from
	 * @param <T>   the type of the elements in the array
	 * @return a random element from the array
	 */
	public static <T> T getRandomElementFromArray(T[] array) {
		int index = getInstance().random.nextInt(array.length);
		return array[index];
	}

	/**
	 * Generates a random value based on a triangular distribution.
	 *
	 * @param mode      the mode (peak) of the distribution
	 * @param deviation the deviation from the mode
	 * @return a random value based on a triangular distribution
	 */
	public static double triangle(double mode, double deviation) {
		return mode + deviation * (generateRandomDouble(0, 1) - generateRandomDouble(0, 1));
	}

	/**
	 * Selects a specified number of random elements from the given array.
	 *
	 * @param array the array to select random elements from
	 * @param count the number of random elements to select
	 * @param <T>   the type of the elements in the array
	 * @return an array containing the selected random elements
	 * @throws IllegalArgumentException if the count is greater than the array
	 *                                  length
	 */
	public static <T> T[] getRandomElementsFromArray(T[] array, int count) {
		if (count > array.length) {
			throw new IllegalArgumentException("Count cannot be greater than array length");
		}

		@SuppressWarnings("unchecked")
		T[] result = (T[]) new Object[count];

		for (int i = 0; i < count; i++) {
			int index = getInstance().random.nextInt(array.length - i);
			result[i] = array[index];
			array[index] = array[array.length - i - 1];
		}

		return result;
	}

	/**
	 * Generates a random biome for starting islands.
	 *
	 * @return a random biome
	 */
	public static @NotNull Biome generateRandomBiome() {
		List<Biome> biomes = Arrays.asList(HellBiome.values()).stream().map(HellBiome::getConvertedBiome).toList();
		int randomIndex = getInstance().random.nextInt(biomes.size());
		return biomes.get(randomIndex);
	}

	/**
	 * Spawns a random animal.
	 *
	 * @return a random animal
	 */
	public static @NotNull EntityType spawnRandomAnimal() {
		List<EntityType> entities = List.of(EntityType.PIG, EntityType.COW, EntityType.CHICKEN, EntityType.SHEEP,
				EntityType.RABBIT, EntityType.MOOSHROOM);
		int randomIndex = getInstance().random.nextInt(entities.size());
		return entities.get(randomIndex);
	}

	/**
	 * Spawns a random fortress mob.
	 *
	 * @return a random fortress mob
	 */
	public static @NotNull EntityType spawnFortressMob() {
		List<EntityType> entities = List.of(EntityType.BLAZE, EntityType.WITHER_SKELETON);
		int randomIndex = getInstance().random.nextInt(entities.size());
		return entities.get(randomIndex);
	}

	/**
	 * Picks a random sapling.
	 * 
	 * @return a random sapling
	 */
	public static @NotNull Material pickRandomSapling() {
		List<Material> saplings = List.of(Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
				Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING);
		int randomIndex = getInstance().random.nextInt(saplings.size());
		return saplings.get(randomIndex);
	}
}