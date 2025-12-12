package com.swiftlicious.hellblock.challenges.requirement;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.listeners.LevelHandler.LevelProgressContext;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where an island must gain a specific
 * amount of Hellblock level threshold.
 * <p>
 * Unlike player XP or level systems, this requirement uses the island's overall
 * progression level (a double-precision value) to determine completion. It
 * supports both integer and fractional level goals (e.g., {@code 250} or
 * {@code 250.5}).
 * </p>
 *
 * <p>
 * <b>Example configuration (in challenges.yml):</b>
 * </p>
 * 
 * <pre>
 *   REACH_ISLAND_LEVEL_250:
 *     needed-amount: 250
 *     action: LEVELUP
 *     data:
 *       level: 250.5
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: EXPERIENCE_BOTTLE
 * </pre>
 *
 * <p>
 * In this example, The island must gain at least an additional level
 * <code>250.5</code>.
 * </p>
 */
public class LevelUpRequirement implements ChallengeRequirement {

	private final double requiredGain;
	private final boolean relative;

	public LevelUpRequirement(@NotNull Section data) {
		this.requiredGain = data.getDouble("level");
		this.relative = data.getBoolean("relative", false);
	}

	public boolean isRelative() {
		return this.relative;
	}

	/**
	 * Checks if the player's island level gain since challenge start meets or
	 * exceeds the required gain.
	 *
	 * @param context a LevelProgressContext containing startingLevel and
	 *                currentLevel
	 * @return {@code true} if the island gained >= required level, otherwise
	 *         {@code false}.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		if (relative && context instanceof LevelProgressContext ctx)
			return (ctx.currentLevel() - ctx.startLevel()) >= requiredGain;
		else if (context instanceof Number n)
			return n.doubleValue() >= requiredGain;
		return false;
	}
}