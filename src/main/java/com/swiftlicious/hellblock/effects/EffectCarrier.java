package com.swiftlicious.hellblock.effects;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.Requirement;

public class EffectCarrier {

	private Key key;
	private Requirement[] requirements;
	private EffectModifier[] effect;
	private Map<ActionTrigger, Action[]> actionMap;
	private boolean persist;

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final EffectCarrier item;

		public Builder() {
			this.item = new EffectCarrier();
		}

		public Builder persist(boolean persist) {
			item.persist = persist;
			return this;
		}

		public Builder key(Key key) {
			item.key = key;
			return this;
		}

		public Builder requirements(Requirement[] requirements) {
			item.requirements = requirements;
			return this;
		}

		public Builder effect(EffectModifier[] effect) {
			item.effect = effect;
			return this;
		}

		public Builder actionMap(Map<ActionTrigger, Action[]> actionMap) {
			item.actionMap = actionMap;
			return this;
		}

		public EffectCarrier build() {
			return item;
		}
	}

	public Key getKey() {
		return key;
	}

	public Requirement[] getRequirements() {
		return requirements;
	}

	public EffectModifier[] getEffectModifiers() {
		return effect;
	}

	public Map<ActionTrigger, Action[]> getActionMap() {
		return actionMap;
	}

	@Nullable
	public Action[] getActions(ActionTrigger trigger) {
		return actionMap.get(trigger);
	}

	public boolean isPersist() {
		return persist;
	}

	public boolean isConditionMet(Condition condition) {
		if (requirements == null)
			return true;
		for (Requirement requirement : requirements) {
			if (!requirement.isConditionMet(condition)) {
				return false;
			}
		}
		return true;
	}
}
