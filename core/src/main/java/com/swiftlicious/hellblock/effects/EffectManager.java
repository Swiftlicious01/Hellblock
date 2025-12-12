package com.swiftlicious.hellblock.effects;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.mechanics.MechanicType;

public class EffectManager implements EffectManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, EffectModifier> effectModifiers = new HashMap<>();

	public EffectManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void unload() {
		this.effectModifiers.clear();
	}

	@Override
	public void load() {
		instance.debug(effectModifiers.size() > 0
				? "Loaded " + effectModifiers.size() + " effect" + (effectModifiers.size() == 1 ? "" : "s")
				: "No effects found to load");
	}

	@Override
	public boolean registerEffectModifier(EffectModifier effect, MechanicType type) {
		if (effectModifiers.containsKey(effect.id())) {
			return false;
		}
		this.effectModifiers.put(type.getType() + ":" + effect.id(), effect);
		return true;
	}

	@Override
	public Optional<EffectModifier> getEffectModifier(String id, MechanicType type) {
		return Optional.ofNullable(this.effectModifiers.get(type.getType() + ":" + id));
	}
}