package com.swiftlicious.hellblock.challenges;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a single logical requirement used in a Hellblock challenge.
 * <p>
 * Each implementation defines a specific condition that must be satisfied for a
 * player's action or event to contribute toward challenge progression.
 * </p>
 *
 * <p>
 * <b>Context Types:</b>
 * </p>
 * <p>
 * The {@code context} object passed into {@link #matches(Object)} varies
 * depending on the type of challenge action being processed:
 * </p>
 * <ul>
 * <li>{@link org.bukkit.block.Block} — used for:
 * <ul>
 * <li>Block breaking challenges (<b>BREAK</b>)</li>
 * <li>Farming or crop growth challenges (<b>FARM</b>)</li>
 * <li>Tree growth challenges (<b>GROW</b>)</li>
 * </ul>
 * </li>
 *
 * <li>{@link org.bukkit.entity.Entity} — used for:
 * <ul>
 * <li>Entity slaying challenges (<b>SLAY</b>)</li>
 * <li>Animal breeding challenges (<b>BREED</b>)</li>
 * </ul>
 * </li>
 *
 * <li>{@link org.bukkit.inventory.ItemStack} — used for:
 * <ul>
 * <li>Bartering challenges (<b>BARTER</b>)</li>
 * <li>Crafting challenges (<b>CRAFT</b>)</li>
 * <li>Brewing challenges (<b>BREW</b>)</li>
 * </ul>
 * </li>
 *
 * <li>{@link com.swiftlicious.hellblock.listeners.GlowTreeHandler.CustomTreeGrowContext}
 * — used for:
 * <ul>
 * <li>Tree growth challenges (<b>GROW</b>)</li>
 * </ul>
 * </li>
 *
 * <li>{@code Number} or {@code Integer} — used exclusively for:
 * <ul>
 * <li>Island level up-based challenges (<b>LEVELUP</b>)</li>
 * </ul>
 * </li>
 *
 * <li>{@code Object} (generic) — used for fishing challenges (<b>FISH</b>),
 * which may accept any of the above context types depending on how the event is
 * triggered.</li>
 * </ul>
 *
 * <p>
 * Implementations may safely cast and handle only the context types they
 * support.
 * </p>
 */
public interface ChallengeRequirement {

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
	boolean matches(@NotNull Object context);
}