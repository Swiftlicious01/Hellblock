package com.swiftlicious.hellblock.effects;

import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.utils.extras.Value;

public class BaseEffect {

	private final Value waitTime;
	private final Value waitTimeMultiplier;

	public BaseEffect(Value waitTime, Value waitTimeMultiplier) {
		this.waitTime = waitTime;
		this.waitTimeMultiplier = waitTimeMultiplier;
	}

	public Effect build(Player player, Map<String, String> values) {
		return new FishingEffect(waitTime.get(player, values), waitTimeMultiplier.get(player, values));
	}
}
