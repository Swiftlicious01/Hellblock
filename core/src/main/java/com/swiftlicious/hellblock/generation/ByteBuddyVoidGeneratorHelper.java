package com.swiftlicious.hellblock.generation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.util.Random;

import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;

/**
 * Dynamically generates a ByteBuddy-based proxy subclass of
 * {@link ChunkGenerator} that supports {@code WorldInfo}-based method
 * signatures introduced in Minecraft 1.17.1+.
 * <p>
 * This allows {@link VoidGenerator} to support cross-version compatibility
 * without directly referencing post-1.17 APIs, delegating methods via
 * reflection (for generation) and MethodHandles (for biome access).
 */
public final class ByteBuddyVoidGeneratorHelper {

	/**
	 * Creates a dynamic {@link ChunkGenerator} proxy that wraps the given
	 * {@link VoidGenerator} and delegates method calls (including {@code getBiome},
	 * {@code getBiomes}, and {@code generateNoise}, etc.) to it using runtime
	 * proxying via ByteBuddy.
	 *
	 * @param delegate       the base {@link VoidGenerator} implementation to
	 *                       delegate to
	 * @param worldInfoClass the WorldInfo class (resolved reflectively for
	 *                       cross-version support)
	 * @return a dynamically generated {@link ChunkGenerator} instance
	 * @throws Exception if proxy generation or instantiation fails
	 */
	@NotNull
	public static ChunkGenerator create(@NotNull VoidGenerator delegate, @NotNull Class<?> worldInfoClass)
			throws Exception {
		// Single forwarder instance handles both generation and biome forwarding
		VoidGeneratorMethodForwarder forwarder = new VoidGeneratorMethodForwarder(delegate);

		// STEP 1: Create a dynamic BiomeProvider proxy.
		Class<?> biomeProviderClass = Class.forName("org.bukkit.generator.BiomeProvider");

		Class<?> proxyBiomeProviderClass = new ByteBuddy().subclass(biomeProviderClass)
				.name("com.swiftlicious.hellblock.generation.ByteBuddyBiomeProvider")

				// Intercept: Biome getBiome(WorldInfo, x, y, z)
				.method(named("getBiome").and(takesArguments(worldInfoClass, int.class, int.class, int.class)))
				.intercept(MethodDelegation.withDefaultConfiguration()
						.filter(m -> "forwardGetBiome".equals(m.getName())).to(forwarder))

				// Intercept: List<Biome> getBiomes(WorldInfo)
				.method(named("getBiomes").and(takesArguments(worldInfoClass)))
				.intercept(MethodDelegation.withDefaultConfiguration()
						.filter(m -> "forwardGetBiomes".equals(m.getName())).to(forwarder))

				.make()
				.load(ByteBuddyVoidGeneratorHelper.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();

		// Instantiate the proxy BiomeProvider
		Object biomeProvider = proxyBiomeProviderClass.getDeclaredConstructor().newInstance();

		// STEP 2: Create the ChunkGenerator proxy subclass.
		Class<? extends ChunkGenerator> proxyChunkGeneratorClass = new ByteBuddy().subclass(ChunkGenerator.class)
				.name("com.swiftlicious.hellblock.generation.VoidGenerator$ByteBuddy")

				// Intercept: generateNoise(WorldInfo, Random, int, int, ChunkData)
				.method(named("generateNoise").and(takesArguments(worldInfoClass, Random.class, int.class, int.class,
						ChunkGenerator.ChunkData.class)))
				.intercept(MethodDelegation.to(forwarder, "forwardGeneration"))

				// Intercept: generateSurface(...)
				.method(named("generateSurface").and(takesArguments(worldInfoClass, Random.class, int.class, int.class,
						ChunkGenerator.ChunkData.class)))
				.intercept(MethodDelegation.to(forwarder, "forwardGeneration"))

				// Intercept: generateBedrock(...)
				.method(named("generateBedrock").and(takesArguments(worldInfoClass, Random.class, int.class, int.class,
						ChunkGenerator.ChunkData.class)))
				.intercept(MethodDelegation.to(forwarder, "forwardGeneration"))

				// Intercept: generateCaves(...)
				.method(named("generateCaves").and(takesArguments(worldInfoClass, Random.class, int.class, int.class,
						ChunkGenerator.ChunkData.class)))
				.intercept(MethodDelegation.to(forwarder, "forwardGeneration"))

				// Override: getDefaultBiomeProvider(WorldInfo) → return proxy instance
				.method(named("getDefaultBiomeProvider").and(takesArguments(worldInfoClass)))
				.intercept(FixedValue.value(biomeProvider))

				.make()
				.load(ByteBuddyVoidGeneratorHelper.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
				.getLoaded();

		// STEP 3: Instantiate and return the ChunkGenerator proxy
		return proxyChunkGeneratorClass.getDeclaredConstructor().newInstance();
	}
}