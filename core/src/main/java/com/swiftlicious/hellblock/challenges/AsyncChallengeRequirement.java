package com.swiftlicious.hellblock.challenges;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

public interface AsyncChallengeRequirement extends ChallengeRequirement {

	/**
	 * Evaluates whether a given context object satisfies this challenge
	 * requirement.
	 * <p>
	 * The type and structure of the context depend on the action type associated
	 * with the requirement (see class-level documentation for details).
	 * </p>
	 *
	 * @param context The event or data object being evaluated — such as a
	 *                {@code Block}, {@code Entity}, {@code ItemStack},
	 *                {@code Number}, or a custom context like
	 *                {@link com.swiftlicious.hellblock.listeners.GlowTreeHandler.TreeGrowContext}.
	 * @return {@code true} if the context fulfills the requirement, otherwise
	 *         {@code false}.
	 */
	CompletableFuture<Boolean> matchesAsync(@NotNull Object context);
}