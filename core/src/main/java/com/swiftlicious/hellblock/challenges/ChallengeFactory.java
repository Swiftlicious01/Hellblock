package com.swiftlicious.hellblock.challenges;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.BarterRequirement;
import com.swiftlicious.hellblock.challenges.requirement.BlockBreakRequirement;
import com.swiftlicious.hellblock.challenges.requirement.BreedRequirement;
import com.swiftlicious.hellblock.challenges.requirement.BrewRequirement;
import com.swiftlicious.hellblock.challenges.requirement.CraftRequirement;
import com.swiftlicious.hellblock.challenges.requirement.FarmRequirement;
import com.swiftlicious.hellblock.challenges.requirement.FishRequirement;
import com.swiftlicious.hellblock.challenges.requirement.GrowRequirement;
import com.swiftlicious.hellblock.challenges.requirement.LevelUpRequirement;
import com.swiftlicious.hellblock.challenges.requirement.SlayRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Factory responsible for constructing {@link ChallengeRequirement} instances
 * based on a given {@link ActionType} and configuration data.
 * <p>
 * This class acts as the central bridge between configuration and runtime
 * logic, dynamically instantiating requirement types such as block breaking,
 * crafting, or fishing challenges. It delegates item-specific resolution to an
 * internal {@link ItemResolver}.
 * </p>
 */
public final class ChallengeFactory {

	private final HellblockPlugin instance;
	private final ItemResolver itemResolver;

	public ChallengeFactory(HellblockPlugin plugin) {
		this.instance = plugin;
		this.itemResolver = new ItemResolver(instance, instance.getConfigManager().getRegistry());
	}

	/**
	 * Creates a new {@link ChallengeRequirement} instance based on the specified
	 * action type and configuration data.
	 * <p>
	 * The returned requirement defines the logic for how a player satisfies a
	 * particular challenge, such as "break 100 blocks" or "fish a specific item".
	 * </p>
	 *
	 * @param action The action type this requirement belongs to (e.g. BREAK, FISH,
	 *               CRAFT).
	 * @param data   The configuration section containing requirement-specific
	 *               parameters.
	 * @return A fully constructed {@link ChallengeRequirement}.
	 * @throws IllegalArgumentException      If the data section is missing.
	 * @throws UnsupportedOperationException If the action type is not recognized.
	 */
	@NotNull
	public ChallengeRequirement create(@NotNull ActionType action, @Nullable Section data) {
		if (data == null) {
			throw new IllegalArgumentException("Missing data section for " + action);
		}

		return switch (action) {
		case BREAK -> new BlockBreakRequirement(data);
		case FARM -> new FarmRequirement(data);
		case SLAY -> new SlayRequirement(data);
		case BREED -> new BreedRequirement(data);
		case LEVELUP -> new LevelUpRequirement(data);
		case GROW -> new GrowRequirement(data);
		case BREW -> new BrewRequirement(data);

		// Item-based requirements (AbstractItemRequirement subclasses)
		case BARTER -> new BarterRequirement(ensureSection(data), itemResolver);
		case CRAFT -> new CraftRequirement(data, itemResolver);
		case FISH -> new FishRequirement(data, itemResolver);

		default -> throw new UnsupportedOperationException("Unsupported action type: " + action);
		};
	}

	/**
	 * Converts scalar "data" values (like "data: 1") into a fake Section.
	 */
	@NotNull
	private Section ensureSection(@NotNull Section data) {
		Object raw = data.get("");
		if (raw instanceof Section)
			return (Section) raw;

		// Convert scalar values into a fake section
		Section wrapper = data.createSection("normalized");
		if (raw instanceof Number num)
			wrapper.set("number", num.intValue());
		else if (raw != null)
			wrapper.set("barter-item", raw.toString());
		return wrapper;
	}
}