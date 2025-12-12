package com.swiftlicious.hellblock.handlers;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.utils.ListUtils;

public class IslandRequirementManager extends AbstractRequirementManager<Integer> {

	public IslandRequirementManager(HellblockPlugin plugin) {
		super(plugin, Integer.class);
	}

	@Override
	public void registerBuiltInRequirements() {
		super.registerBuiltInRequirements();
		this.registerWeatherRequirement();
	}

	@Override
	public void load() {
		loadExpansions(Integer.class);
	}

	protected void registerWeatherRequirement() {
		registerRequirement((args, actions, runActions) -> {
			final Set<String> weathers = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final boolean hasLavaRain = requireNonNull(context.arg(ContextKeys.LAVA_RAIN));
				final boolean hasAshStorm = requireNonNull(context.arg(ContextKeys.ASH_STORM));
				final boolean hasEmberFog = requireNonNull(context.arg(ContextKeys.EMBER_FOG));
				final boolean hasMagmaWind = requireNonNull(context.arg(ContextKeys.MAGMA_WIND));
				final String currentWeather;

				if (hasLavaRain) {
					currentWeather = "lavarain";
				} else if (hasAshStorm) {
					currentWeather = "ashstorm";
				} else if (hasEmberFog) {
					currentWeather = "emberfog";
				} else if (hasMagmaWind) {
					currentWeather = "magmawind";
				} else {
					currentWeather = "clear";
				}

				if (weathers.contains(currentWeather)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "weather");

		registerRequirement((args, actions, runActions) -> {
			final Set<String> weathers = new HashSet<>(ListUtils.toList(args));
			return context -> {
				final boolean hasLavaRain = requireNonNull(context.arg(ContextKeys.LAVA_RAIN));
				final boolean hasAshStorm = requireNonNull(context.arg(ContextKeys.ASH_STORM));
				final boolean hasEmberFog = requireNonNull(context.arg(ContextKeys.EMBER_FOG));
				final boolean hasMagmaWind = requireNonNull(context.arg(ContextKeys.MAGMA_WIND));
				final String currentWeather;

				if (hasLavaRain) {
					currentWeather = "lavarain";
				} else if (hasAshStorm) {
					currentWeather = "ashstorm";
				} else if (hasEmberFog) {
					currentWeather = "emberfog";
				} else if (hasMagmaWind) {
					currentWeather = "magmawind";
				} else {
					currentWeather = "clear";
				}

				// Inverted logic for !weather
				if (!weathers.contains(currentWeather)) {
					return true;
				}
				if (runActions) {
					ActionManager.trigger(context, actions);
				}
				return false;
			};
		}, "!weather");
	}
}