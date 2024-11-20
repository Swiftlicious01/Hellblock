package com.swiftlicious.hellblock.utils.extras;

/**
 * A generic class representing a pair of values. This class provides methods to
 * create and access pairs of values.
 *
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
public record Pair<L, R>(L left, R right) {

	/**
	 * Creates a new {@link Pair} with the specified left and right values.
	 *
	 * @param left  the left value
	 * @param right the right value
	 * @param <L>   the type of the left value
	 * @param <R>   the type of the right value
	 * @return a new {@link Pair} with the specified values
	 */
	public static <L, R> Pair<L, R> of(final L left, final R right) {
		return new Pair<>(left, right);
	}
}