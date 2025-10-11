package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
		final int index = getInstance().random.nextInt(array.length);
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
		final T[] result = (T[]) new Object[count];

		for (int i = 0; i < count; i++) {
			final int index = getInstance().random.nextInt(array.length - i);
			result[i] = array[index];
			array[index] = array[array.length - i - 1];
		}

		return result;
	}

	public static <T> T getRandomElement(@NotNull Collection<T> collection) {
		if (collection == null || collection.isEmpty()) {
			throw new IllegalArgumentException("Collection must not be null or empty");
		}
		final int index = ThreadLocalRandom.current().nextInt(collection.size());
		if (collection instanceof List) {
			return ((List<T>) collection).get(index);
		}
		final Iterator<T> iter = collection.iterator();
		for (int i = 0; i < index; i++) {
			iter.next();
		}
		return iter.next();
	}

	public static EntityType getWeightedRandom(Map<EntityType, Double> weights) {
		double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		double r = getInstance().random.nextDouble() * totalWeight;

		for (Map.Entry<EntityType, Double> entry : weights.entrySet()) {
			r -= entry.getValue();
			if (r <= 0) {
				return entry.getKey();
			}
		}
		// Fallback (shouldn’t reach here)
		return weights.keySet().iterator().next();
	}

	/**
	 * Generates a random biome for starting islands.
	 *
	 * @return a random biome
	 */
	public static @NotNull Biome generateRandomBiome() {
		// Exclude NETHER_FORTRESS to avoid recursion
		final List<Biome> biomes = Stream.of(HellBiome.values()).filter(b -> b != HellBiome.NETHER_FORTRESS)
				.map(HellBiome::getConvertedBiome).toList();
		final int randomIndex = getInstance().random.nextInt(biomes.size());
		return biomes.get(randomIndex);
	}

	/**
	 * Spawns a random animal.
	 *
	 * @return a random animal
	 */
	public static @Nullable EntityType spawnRandomAnimal(World world, Chunk chunk, double bonus,
			Set<Location> spawnCache) {
		// Define base weights with dynamic scaling
		Map<EntityType, Double> baseWeights = new HashMap<>();
		baseWeights.put(EntityType.SHEEP, 20.0 + (bonus * 3));
		baseWeights.put(EntityType.COW, 20.0 + (bonus * 3));
		baseWeights.put(EntityType.PIG, 15.0 + (bonus * 2));
		baseWeights.put(EntityType.CHICKEN, 15.0 + (bonus * 2));
		baseWeights.put(EntityType.MOOSHROOM, bonus >= 1.0 ? 5.0 + (bonus * 1.5) : 1.0);
		baseWeights.put(EntityType.HORSE, bonus >= 2.0 ? 3.0 + (bonus * 1.2) : 0.5);
		baseWeights.put(EntityType.RABBIT, 1.0 + (bonus * 0.8));

		if (bonus >= 2.5) {
			baseWeights.put(EntityType.FOX, 2.0 + (bonus * 0.5));
		}

		// Only allow Strider if near lava
		if (bonus >= 3.0 && isChunkNearLava(chunk)) {
			baseWeights.put(EntityType.STRIDER, 3.0 + (bonus * 0.7));
		}

		// Per-entity chunk caps
		Map<EntityType, Integer> maxPerChunk = Map.of(EntityType.MOOSHROOM, 2, EntityType.HORSE, 2, EntityType.FOX, 3,
				EntityType.STRIDER, 3);

		// Count existing animals in chunk
		Map<EntityType, Long> currentCounts = Arrays.stream(chunk.getEntities()).filter(e -> e instanceof Animals)
				.collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));

		// Filter by cap
		baseWeights.entrySet().removeIf(entry -> {
			EntityType type = entry.getKey();
			if (!maxPerChunk.containsKey(type))
				return false;
			long count = currentCounts.getOrDefault(type, 0L);
			return count >= maxPerChunk.get(type);
		});

		// Remove 0-weighted entries
		baseWeights.entrySet().removeIf(entry -> entry.getValue() <= 0.0);

		if (baseWeights.isEmpty()) {
			return null;
		}

		// Roll for chance
		double baseChance = 10.0;
		double totalChance = baseChance + (bonus * 100.0);
		if (generateRandomInt(1, 100) > totalChance) {
			return null;
		}

		return getWeightedRandom(baseWeights);
	}

	private static boolean isChunkNearLava(Chunk chunk) {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < chunk.getWorld().getMaxHeight(); y++) {
					Block block = chunk.getBlock(x, y, z);
					if (block.getType() == Material.LAVA) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Spawns a random fortress mob.
	 *
	 * @return a random fortress mob
	 */
	public static @Nullable EntityType spawnFortressMob(World world, Chunk chunk, double bonus,
			Set<Location> spawnCache) {
		// Dynamic weight scaling
		Map<EntityType, Double> baseWeights = new HashMap<>();
		baseWeights.put(EntityType.BLAZE, 20.0 + (bonus * 5)); // Bonus increases chance
		baseWeights.put(EntityType.WITHER_SKELETON, 2.0 + (bonus * 10)); // Strong bonus scaling

		// Optional per-entity spawn caps in chunk
		Map<EntityType, Integer> maxPerChunk = Map.of(EntityType.WITHER_SKELETON, 4, EntityType.BLAZE, 6);

		// Count mobs in chunk
		Map<EntityType, Long> currentCounts = Arrays.stream(chunk.getEntities())
				.filter(e -> e.getType() == EntityType.BLAZE || e.getType() == EntityType.WITHER_SKELETON)
				.collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));

		// Remove types that exceed cap
		baseWeights.entrySet().removeIf(entry -> {
			EntityType type = entry.getKey();
			if (!maxPerChunk.containsKey(type))
				return false;
			return currentCounts.getOrDefault(type, 0L) >= maxPerChunk.get(type);
		});

		// Exit early if nothing valid to spawn
		if (baseWeights.isEmpty()) {
			return null;
		}

		// Roll chance to spawn
		double baseChance = 10.0;
		double totalChance = baseChance + (bonus * 100.0); // bonus of 0.25 = +25%

		if (generateRandomInt(1, 100) > totalChance) {
			return null;
		}

		// Pick mob based on weighted random
		return getWeightedRandom(baseWeights);
	}

	/**
	 * Picks a random sapling.
	 * 
	 * @return a random sapling
	 */
	public static @NotNull Material pickRandomSapling() {
		final List<Material> saplings = List.of(Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
				Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING);
		final int randomIndex = getInstance().random.nextInt(saplings.size());
		return saplings.get(randomIndex);
	}
}