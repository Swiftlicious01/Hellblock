package com.swiftlicious.hellblock.generation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

/**
 * Byte Buddy method forwarder for {@link VoidGenerator}.
 *
 * This class is bound to the proxy generator (and biome provider proxy) and is
 * responsible for intercepting all relevant method calls and delegating them to
 * the underlying {@link VoidGenerator} or its {@code VoidBiomeProvider}.
 */
public class VoidGeneratorMethodForwarder {

	/** Backing instance that handles generation logic. */
	private final VoidGenerator delegate;

	/** Inner forwarder responsible for handling biome-related methods. */
	private final BiomeMethodForwarder biomeForwarder;

	/**
	 * Allowed chunk generator method names to forward using reflection.
	 */
	private static final List<String> ALLOWED_GENERATION_METHODS = List.of("generateNoise", "generateSurface",
			"generateBedrock", "generateCaves");

	/**
	 * Constructs the method forwarder with the target {@link VoidGenerator}.
	 * 
	 * Instantiates the biome forwarder with a concrete instance of
	 * {@link VoidGenerator.VoidBiomeProvider}.
	 *
	 * @param delegate the user-defined generator that handles actual logic
	 * @throws Exception if method handles for biome methods can't be resolved
	 */
	public VoidGeneratorMethodForwarder(@NotNull VoidGenerator delegate) throws Exception {
		this.delegate = delegate;

		// We eagerly construct the biome provider proxy and prepare handles for its
		// methods
		this.biomeForwarder = new BiomeMethodForwarder(new VoidGenerator.VoidBiomeProvider());
	}

	/**
	 * Exposed method to retrieve the dynamically created biome provider proxy.
	 * 
	 * This is returned from the `getDefaultBiomeProvider(WorldInfo)` method in the
	 * proxy generator class.
	 */
	@NotNull
	public Object getBiomeProvider() {
		return biomeForwarder.getInstance();
	}

	/**
	 * Byte Buddy interceptor for generation methods.
	 *
	 * This is dynamically bound to methods like `generateNoise(...)`, etc. The
	 * method name is validated against an allowed list, and reflection is used to
	 * invoke the same-named method on the delegate instance.
	 *
	 * @param method the reflected method being intercepted
	 * @param args   the arguments passed to that method
	 * @return the result of the call
	 * @throws Exception if reflection fails or an invalid method is intercepted
	 */
	@NotNull
	@RuntimeType
	public Object forwardGeneration(@Origin @NotNull Method method, @AllArguments @NotNull Object[] args)
			throws Exception {
		String name = method.getName();

		// Only allow known generation methods to be called
		if (!ALLOWED_GENERATION_METHODS.contains(name)) {
			throw new IllegalStateException("Unexpected generation method: " + name);
		}

		// Look up the corresponding method on the delegate using reflection
		Method target = delegate.getClass().getMethod(name, Object.class, Random.class, int.class, int.class,
				ChunkGenerator.ChunkData.class);

		try {
			// Forward the call to the delegate generator
			return target.invoke(delegate, args);
		} catch (InvocationTargetException e) {
			// Unwrap and rethrow the real underlying exception
			Throwable cause = e.getCause();
			if (cause instanceof Exception)
				throw (Exception) cause;
			throw new RuntimeException("Unexpected throwable from generator method", cause);
		}
	}

	/**
	 * Byte Buddy hook to forward biome lookups.
	 * 
	 * This intercepts `getBiome(WorldInfo, x, y, z)` and dispatches via
	 * MethodHandle.
	 *
	 * @param args [WorldInfo, x, y, z]
	 * @return Biome for the given coordinates
	 * @throws Throwable if the method handle invocation fails
	 */
	@NotNull
	@RuntimeType
	public Object forwardGetBiome(@AllArguments @NotNull Object[] args) throws Throwable {
		return biomeForwarder.callGetBiome(args);
	}

	/**
	 * Byte Buddy hook to forward biome list queries.
	 * 
	 * This intercepts `getBiomes(WorldInfo)` and dispatches via MethodHandle.
	 *
	 * @param args [WorldInfo]
	 * @return list of biomes supported by the provider
	 * @throws Throwable if the method handle invocation fails
	 */
	@NotNull
	@RuntimeType
	public Object forwardGetBiomes(@AllArguments @NotNull Object[] args) throws Throwable {
		return biomeForwarder.callGetBiomes(args);
	}

	/**
	 * Inner helper responsible for biome-related method dispatch.
	 *
	 * Uses cached {@link MethodHandle}s to avoid reflection and improve
	 * performance.
	 */
	private static class BiomeMethodForwarder {
		/** Backing provider instance, e.g. {@link VoidGenerator.VoidBiomeProvider} */
		private final Object instance;

		/** Cached handle to getBiome(Object, int, int, int) */
		private final MethodHandle getBiomeHandle;

		/** Cached handle to getBiomes(Object) */
		private final MethodHandle getBiomesHandle;

		/**
		 * Initializes biome method forwarding by resolving {@link MethodHandle}s.
		 *
		 * @param instance the actual provider instance (VoidBiomeProvider)
		 * @throws Exception if method resolution fails
		 */
		public BiomeMethodForwarder(@NotNull Object instance) throws Exception {
			this.instance = instance;

			// Grab the actual class of the provider
			Class<?> clazz = instance.getClass();
			MethodHandles.Lookup lookup = MethodHandles.lookup();

			// Resolve getBiome(Object, int, int, int) -> Biome
			this.getBiomeHandle = lookup.findVirtual(clazz, "getBiome",
					MethodType.methodType(Biome.class, Object.class, int.class, int.class, int.class));

			// Resolve getBiomes(Object) -> List<Biome>
			this.getBiomesHandle = lookup.findVirtual(clazz, "getBiomes",
					MethodType.methodType(List.class, Object.class));
		}

		/**
		 * Returns the backing provider instance.
		 */
		@NotNull
		public Object getInstance() {
			return instance;
		}

		/**
		 * Forwards a call to `getBiome(...)` via the cached handle.
		 *
		 * @param args [worldInfo, x, y, z]
		 * @return biome at the given position
		 * @throws Throwable if invocation fails
		 */
		@NotNull
		public Object callGetBiome(@NotNull Object[] args) throws Throwable {
			return getBiomeHandle.invoke(instance, args[0], args[1], args[2], args[3]);
		}

		/**
		 * Forwards a call to `getBiomes(...)` via the cached handle.
		 *
		 * @param args [worldInfo]
		 * @return list of available biomes
		 * @throws Throwable if invocation fails
		 */
		@NotNull
		public Object callGetBiomes(@NotNull Object[] args) throws Throwable {
			return getBiomesHandle.invoke(instance, args[0]);
		}
	}
}