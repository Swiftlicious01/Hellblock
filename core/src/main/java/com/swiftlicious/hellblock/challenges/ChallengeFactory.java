package com.swiftlicious.hellblock.challenges;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.challenges.requirement.*;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public final class ChallengeFactory {
	private final HellblockPlugin instance;
	private final ItemResolver itemResolver;

	public ChallengeFactory(HellblockPlugin plugin) {
		this.instance = plugin;
		this.itemResolver = new ItemResolver(instance, instance.getConfigManager().getRegistry());
	}

	public ChallengeRequirement create(ActionType action, Section data) {
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
		case BARTER -> new BarterRequirement(data, itemResolver);
		case CRAFT -> new CraftRequirement(data, itemResolver);
		case FISH -> new FishRequirement(data, itemResolver);

		default -> throw new UnsupportedOperationException("Unsupported action type: " + action);
		};
	}
}