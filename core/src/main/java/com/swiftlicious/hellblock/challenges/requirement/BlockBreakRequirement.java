package com.swiftlicious.hellblock.challenges.requirement;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where the player must break a specific
 * type of block.
 * <p>
 * The challenge is triggered when the player successfully breaks a block
 * matching the configured material type.
 * </p>
 *
 * <p>
 * <b>Example configuration:</b>
 * </p>
 * 
 * <pre>
 *   BREAK_NETHERRACK_1:
 *     needed-amount: 100
 *     action: BREAK
 *     data:
 *       block: NETHERRACK
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: STONE_PICKAXE
 * </pre>
 *
 * <p>
 * This challenge requires the player to break 100 Netherrack blocks to complete
 * it.
 * </p>
 */
public class BlockBreakRequirement implements ChallengeRequirement {

	private final Material requiredBlock;

	public BlockBreakRequirement(@NotNull Section data) {
		String blockName = data.getString("block");
		if (blockName == null)
			throw new IllegalArgumentException("BREAK requires 'block' in data");

		Material m = Material.matchMaterial(blockName.toUpperCase(Locale.ROOT));
		if (m == null)
			throw new IllegalArgumentException("Invalid block material: " + blockName);

		this.requiredBlock = m;
	}

	/**
	 * Checks whether the given context is a {@link Block} and matches the
	 * configured material.
	 *
	 * @param context The event context (expected to be a {@link Block}).
	 * @return {@code true} if the block matches the required material.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		return context instanceof Block block && block.getType() == requiredBlock;
	}
}