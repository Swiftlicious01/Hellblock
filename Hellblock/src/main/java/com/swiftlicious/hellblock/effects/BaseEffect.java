package com.swiftlicious.hellblock.effects;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.utils.extras.Value;

public class BaseEffect {

	private final Value waitTime;
	private final Value waitTimeMultiplier;
	private final Value difficulty;
	private final Value difficultyMultiplier;

	public BaseEffect(Value waitTime, Value waitTimeMultiplier, Value difficulty, Value difficultyMultiplier) {
		this.waitTime = waitTime;
		this.waitTimeMultiplier = waitTimeMultiplier;
		this.difficulty = difficulty;
		this.difficultyMultiplier = difficultyMultiplier;
	}

	public Effect build(Player player, Map<String, String> values) {
		return new FishingEffect(waitTime.get(player, values), waitTimeMultiplier.get(player, values),
				difficulty.get(player, values), difficultyMultiplier.get(player, values));
	}
}
