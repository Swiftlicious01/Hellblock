package com.swiftlicious.hellblock.challenges.requirement;

import com.swiftlicious.hellblock.challenges.ChallengeRequirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class LevelUpRequirement implements ChallengeRequirement {
	private final int requiredLevel;

	public LevelUpRequirement(Section data) {
		this.requiredLevel = data.getInt("level");
	}

	@Override
	public boolean matches(Object context) {
		if (context instanceof Integer current)
			return current >= requiredLevel;
		if (context instanceof Number n)
			return n.intValue() >= requiredLevel;
		return false;
	}
}