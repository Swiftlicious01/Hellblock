package com.swiftlicious.hellblock.utils.extras;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * Utility class for handling operations with {@link CompletableFuture}.
 */
public class CompletableFutures {

	private CompletableFutures() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * A collector for collecting a stream of CompletableFuture instances into a
	 * single CompletableFuture that completes when all of the input
	 * CompletableFutures complete.
	 *
	 * @param <T> The type of CompletableFuture.
	 * @return A collector for CompletableFuture instances.
	 */
	public static <T extends CompletableFuture<?>> Collector<T, ImmutableList.Builder<T>, CompletableFuture<Void>> collector() {
		return Collector.of(ImmutableList.Builder::new, ImmutableList.Builder::add, (l, r) -> l.addAll(r.build()),
				builder -> allOf(builder.build()));
	}

	/**
	 * Combines multiple CompletableFuture instances into a single CompletableFuture
	 * that completes when all of the input CompletableFutures complete.
	 *
	 * @param futures A stream of CompletableFuture instances.
	 * @return A CompletableFuture that completes when all input CompletableFutures
	 *         complete.
	 */
	public static CompletableFuture<Void> allOf(Stream<? extends CompletableFuture<?>> futures) {
		final CompletableFuture<?>[] arr = futures.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(arr);
	}

	/**
	 * Combines multiple CompletableFuture instances into a single CompletableFuture
	 * that completes when all of the input CompletableFutures complete.
	 *
	 * @param futures A collection of CompletableFuture instances.
	 * @return A CompletableFuture that completes when all input CompletableFutures
	 *         complete.
	 */
	public static CompletableFuture<Void> allOf(Collection<? extends CompletableFuture<?>> futures) {
		final CompletableFuture<?>[] arr = futures.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(arr);
	}
}