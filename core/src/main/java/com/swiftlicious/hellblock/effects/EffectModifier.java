package com.swiftlicious.hellblock.effects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.Requirement;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

public class EffectModifier implements EffectModifierInterface {

	private final Requirement<Player>[] requirements;
	private final List<TriConsumer<Effect, Context<Player>, Integer>> modifiers;
	private final String id;
	private final MechanicType type;

	public EffectModifier(String id, MechanicType type, Requirement<Player>[] requirements,
			List<TriConsumer<Effect, Context<Player>, Integer>> modifiers) {
		this.requirements = requirements;
		this.modifiers = modifiers;
		this.id = id;
		this.type = type;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public Requirement<Player>[] requirements() {
		return requirements;
	}

	@Override
	public List<TriConsumer<Effect, Context<Player>, Integer>> modifiers() {
		return modifiers;
	}

	@Override
	public MechanicType type() {
		return type;
	}

	public static class Builder implements BuilderInterface {
		private final List<Requirement<Player>> requirements = new ArrayList<>();
		private final List<TriConsumer<Effect, Context<Player>, Integer>> modifiers = new ArrayList<>();
		private String id;
		private MechanicType type;

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder requirements(List<Requirement<Player>> requirements) {
			if (requirements == null) {
				return this;
			}
			this.requirements.addAll(requirements);
			return this;
		}

		@Override
		public Builder modifiers(List<TriConsumer<Effect, Context<Player>, Integer>> modifiers) {
			if (modifiers == null) {
				return this;
			}
			this.modifiers.addAll(modifiers);
			return this;
		}

		@Override
		public Builder type(MechanicType type) {
			this.type = type;
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public EffectModifier build() {
			return new EffectModifier(id, type, this.requirements.toArray(Requirement[]::new), this.modifiers);
		}
	}
}