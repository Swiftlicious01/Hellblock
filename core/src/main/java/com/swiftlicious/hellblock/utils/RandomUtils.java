package com.swiftlicious.hellblock.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

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
	@NotNull
	private static RandomUtils getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Returns a random integer between the specified minimum and maximum values
	 * (inclusive).
	 *
	 * @param min The minimum value (inclusive).
	 * @param max The maximum value (inclusive).
	 * @return A randomly selected integer in the range [min, max].
	 */
	public static int range(int min, int max) {
		return getInstance().random.nextInt(min, max + 1);
	}

	/**
	 * Performs a probability check based on a given chance value.
	 *
	 * @param chance A float between 0.0 and 1.0 representing the success
	 *               probability.
	 * @return true if the roll succeeds (i.e., a random value is less than or equal
	 *         to the chance), false otherwise.
	 */
	public static boolean roll(float chance) {
		return getInstance().random.nextFloat() <= chance;
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
	 * Generates a random long between the specified minimum and maximum values
	 * (inclusive).
	 *
	 * @param min the minimum value
	 * @param max the maximum value
	 * @return a random long between min and max (inclusive)
	 */
	public static long generateRandomLong(long min, long max) {
		return min + (max - min) * getInstance().random.nextLong();
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
	 * Generates a random float value.
	 *
	 * @return a random float value
	 */
	public static float generateRandomFloat() {
		return getInstance().random.nextFloat();
	}

	/**
	 * Selects a random element from the specified array.
	 *
	 * @param array the array to select a random element from
	 * @param <T>   the type of the elements in the array
	 * @return a random element from the array
	 */
	@NotNull
	public static <T> T getRandomElementFromArray(@NotNull T[] array) {
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
	@NotNull
	public static <T> T[] getRandomElementsFromArray(@NotNull T[] array, int count) {
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

	/**
	 * Returns a random element from the given collection.
	 * <p>
	 * If the collection is a {@link List}, this uses constant-time index-based
	 * access. For other types of collections, it iterates to the selected index.
	 *
	 * @param collection The non-null, non-empty collection to pick a random element
	 *                   from.
	 * @param <T>        The type of elements in the collection.
	 * @return A randomly selected element from the collection.
	 * @throws IllegalArgumentException if the collection is null or empty.
	 */
	@NotNull
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

	/**
	 * Returns a random {@link EntityType} from the given weight map using weighted
	 * probability.
	 * <p>
	 * Each entry's value represents its selection weight. The higher the weight,
	 * the more likely it is to be chosen.
	 *
	 * @param weights A map of EntityType to their corresponding selection weights.
	 * @return A randomly selected EntityType based on the weight distribution. If
	 *         the weights are invalid or sum to zero, falls back to returning the
	 *         first key.
	 */
	@NotNull
	public static EntityType getWeightedRandom(@NotNull Map<EntityType, Double> weights) {
		double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
		double r = getInstance().random.nextDouble() * totalWeight;

		for (Map.Entry<EntityType, Double> entry : weights.entrySet()) {
			r -= entry.getValue();
			if (r <= 0) {
				return entry.getKey();
			}
		}
		// Fallback (shouldn't happen if weights are valid)
		return weights.keySet().iterator().next();
	}

	/**
	 * Generates a random {@link Biome} for use in starting islands.
	 * <p>
	 * Excludes specific biomes (e.g., {@code NETHER_FORTRESS}) that may cause
	 * issues such as recursive generation. The available biomes are derived from
	 * the {@link HellBiome} enum via {@code getConvertedBiome()}.
	 *
	 * @return A randomly selected {@link Biome}, guaranteed to be non-null.
	 */
	@NotNull
	public static Biome generateRandomBiome() {
		// Exclude NETHER_FORTRESS to avoid recursion
		final List<Biome> biomes = Stream.of(HellBiome.values()).filter(b -> b != HellBiome.NETHER_FORTRESS)
				.map(HellBiome::getConvertedBiome).toList();
		final int randomIndex = getInstance().random.nextInt(biomes.size());
		return biomes.get(randomIndex);
	}

	/**
	 * Attempts to spawn a random passive animal in the given chunk, influenced by a
	 * bonus factor and current entity counts.
	 * <p>
	 * Animal selection is weighted and restricted by per-type spawn limits. The
	 * spawn chance increases with the bonus value. Lava-adjacent chunks may
	 * additionally allow spawning Striders at higher bonus levels.
	 *
	 * @param world      The Hellblock world in which to spawn.
	 * @param chunkPos   The position of the chunk being evaluated for spawning.
	 * @param bonus      A multiplier that increases spawn probability and
	 *                   influences weight distribution.
	 * @param spawnCache A cache of attempted spawn positions (not used in this
	 *                   method directly but likely relevant externally).
	 * @return The selected {@link EntityType} to spawn, or {@code null} if spawning
	 *         is not allowed or no valid types remain.
	 */
	@Nullable
	public static CompletableFuture<EntityType> spawnRandomAnimal(@NotNull HellblockWorld<?> world,
			@NotNull ChunkPos chunkPos, double bonus, @NotNull Set<Pos3> spawnCache) {
		return isChunkNearLava(world, chunkPos).thenCombine(getEntitiesInChunk(world, chunkPos),
				(isLava, chunkEntities) -> {
					Map<EntityType, Double> baseWeights = new HashMap<>();
					baseWeights.put(EntityType.SHEEP, 20.0 + (bonus * 3));
					baseWeights.put(EntityType.COW, 20.0 + (bonus * 3));
					baseWeights.put(EntityType.PIG, 15.0 + (bonus * 2));
					baseWeights.put(EntityType.CHICKEN, 15.0 + (bonus * 2));
					baseWeights.put(EntityTypeUtils.getCompatibleEntityType("MOOSHROOM", "MUSHROOM_COW"),
							bonus >= 1.0 ? 5.0 + (bonus * 1.5) : 1.0);
					baseWeights.put(EntityType.HORSE, bonus >= 2.0 ? 3.0 + (bonus * 1.2) : 0.5);
					baseWeights.put(EntityType.RABBIT, 1.0 + (bonus * 0.8));
					if (bonus >= 2.5)
						baseWeights.put(EntityType.FOX, 2.0 + (bonus * 0.5));
					if (bonus >= 3.0 && isLava)
						baseWeights.put(EntityType.STRIDER, 3.0 + (bonus * 0.7));

					Map<EntityType, Integer> maxPerChunk = Map.of(
							EntityTypeUtils.getCompatibleEntityType("MOOSHROOM", "MUSHROOM_COW"), 2, EntityType.HORSE,
							2, EntityType.FOX, 3, EntityType.STRIDER, 3);

					Map<EntityType, Long> currentCounts = chunkEntities.stream().filter(e -> e instanceof Animals)
							.collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));

					baseWeights.entrySet().removeIf(entry -> {
						EntityType type = entry.getKey();
						if (!maxPerChunk.containsKey(type))
							return false;
						return currentCounts.getOrDefault(type, 0L) >= maxPerChunk.get(type);
					});

					baseWeights.entrySet().removeIf(entry -> entry.getValue() <= 0.0);
					if (baseWeights.isEmpty())
						return null;

					double totalChance = 10.0 + (bonus * 100.0);
					if (generateRandomInt(1, 100) > totalChance)
						return null;

					return getWeightedRandom(baseWeights);
				});
	}

//	/**
//	 * Attempts to spawn a random fortress mob (e.g., Blaze or Wither Skeleton) in
//	 * the given chunk.
//	 * <p>
//	 * Spawn eligibility is determined by weight-based selection and capped
//	 * per-entity type limits within the chunk. The spawn probability scales with
//	 * the bonus value provided.
//	 *
//	 * @param world      The Hellblock world in which to spawn.
//	 * @param chunkPos   The position of the chunk being evaluated for spawning.
//	 * @param bonus      A multiplier that increases spawn probability and modifies
//	 *                   weight values.
//	 * @param spawnCache A cache of attempted spawn positions (not used in this
//	 *                   method directly but likely relevant externally).
//	 * @return The selected {@link EntityType} to spawn, or {@code null} if spawning
//	 *         conditions are not met.
//	 */
//	@Nullable
//	public static CompletableFuture<EntityType> spawnFortressMob(@NotNull HellblockWorld<?> world,
//			@NotNull ChunkPos chunkPos, double bonus, @NotNull Set<Pos3> spawnCache) {
//		return getEntitiesInChunk(world, chunkPos).thenApply(chunkEntities -> {
//			Map<EntityType, Double> baseWeights = new HashMap<>();
//			baseWeights.put(EntityType.BLAZE, 20.0 + (bonus * 4));
//			baseWeights.put(EntityType.WITHER_SKELETON, 10.0 + (bonus * 3));
//			baseWeights.put(EntityType.MAGMA_CUBE, 2.0 + (bonus * 1.5));
//
//			Map<EntityType, Integer> maxPerChunk = Map.of(EntityType.WITHER_SKELETON, 4, EntityType.BLAZE, 6,
//					EntityType.MAGMA_CUBE, 4);
//
//			Map<EntityType, Long> currentCounts = chunkEntities.stream()
//					.filter(e -> e.getType() == EntityType.BLAZE || e.getType() == EntityType.WITHER_SKELETON
//							|| e.getType() == EntityType.MAGMA_CUBE)
//					.collect(Collectors.groupingBy(Entity::getType, Collectors.counting()));
//
//			baseWeights.entrySet().removeIf(entry -> {
//				EntityType type = entry.getKey();
//				if (!maxPerChunk.containsKey(type))
//					return false;
//				return currentCounts.getOrDefault(type, 0L) >= maxPerChunk.get(type);
//			});
//
//			if (baseWeights.isEmpty())
//				return null;
//
//			double totalChance = 12.0 + (bonus * 25.0);
//			if (generateRandomInt(1, 100) > totalChance)
//				return null;
//
//			return getWeightedRandom(baseWeights);
//		});
//	}

	/**
	 * Checks if the given chunk contains any lava blocks.
	 * <p>
	 * Iterates through all blocks in the chunk (from Y = 0 to max height) and
	 * returns {@code true} if any block matches the Lava block type.
	 *
	 * @param world The Hellblock world instance to query.
	 * @param pos   The position of the chunk to scan.
	 * @return {@code true} if lava is found anywhere within the chunk,
	 *         {@code false} otherwise.
	 */
	public static CompletableFuture<Boolean> isChunkNearLava(HellblockWorld<?> world, ChunkPos pos) {
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < world.bukkitWorld().getMaxHeight(); y++) {
					Pos3 p = new Pos3((pos.x() << 4) + x, y, (pos.z() << 4) + z);
					futures.add(world.getBlockState(p)
							.thenApply(opt -> opt
									.map(state -> "minecraft:lava".equalsIgnoreCase(state.type().type().value()))
									.orElse(false)));
				}
			}
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> futures.stream().anyMatch(f -> f.join()));
	}

	/**
	 * Retrieves all entities currently present in the specified chunk.
	 *
	 * @param world    The Hellblock world containing the chunk.
	 * @param chunkPos The position of the chunk to retrieve entities from.
	 * @return A list of entities within the given chunk. May be empty but never
	 *         {@code null}.
	 */
	@NotNull
	private static CompletableFuture<List<Entity>> getEntitiesInChunk(@NotNull HellblockWorld<?> world,
			@NotNull ChunkPos chunkPos) {
		return CompletableFuture.supplyAsync(() -> {
			// You must switch to the main thread to access Bukkit safely
			CompletableFuture<List<Entity>> result = new CompletableFuture<>();

			HellblockPlugin.getInstance().getScheduler().executeSync(() -> {
				Chunk chunk = world.bukkitWorld().getChunkAt(chunkPos.x(), chunkPos.z());
				List<Entity> entities = Arrays.asList(chunk.getEntities());
				result.complete(entities);
			}, LocationUtils.getAnyLocationInstance());

			return result.join(); // Wait until the sync part completes
		});
	}

	/**
	 * Selects and returns a random sapling type from the list of available saplings
	 * supported by the current Minecraft version.
	 *
	 * <p>
	 * This method is version-safe — it dynamically includes sapling types only if
	 * they exist on the current server version. For example, it will include
	 * {@code MANGROVE_PROPAGULE}, {@code CHERRY_SAPLING}, or
	 * {@code PALE_OAK_SAPLING} only if available.
	 * </p>
	 *
	 * @return a randomly chosen {@link Material} representing a sapling
	 */
	@NotNull
	public static Material pickRandomSapling() {
		List<Material> saplings = new ArrayList<>();

		// Always available (since early versions)
		Collections.addAll(saplings, Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
				Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING);

		// Conditionally add newer saplings safely
		addIfPresent(saplings, "MANGROVE_PROPAGULE");
		addIfPresent(saplings, "CHERRY_SAPLING");
		addIfPresent(saplings, "PALE_OAK_SAPLING");

		// Fallback safeguard
		if (saplings.isEmpty()) {
			return Material.OAK_SAPLING;
		}

		int randomIndex = getInstance().random.nextInt(saplings.size());
		return saplings.get(randomIndex);
	}

	/**
	 * Utility helper that safely adds a {@link Material} to the list if it exists.
	 */
	private static void addIfPresent(@NotNull List<Material> list, @NotNull String materialName) {
		try {
			Material mat = Material.matchMaterial(materialName);
			if (mat != null) {
				list.add(mat);
			}
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Material doesn't exist in this server version
		}
	}
}