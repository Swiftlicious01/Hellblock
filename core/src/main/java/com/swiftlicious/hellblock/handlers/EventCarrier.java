package com.swiftlicious.hellblock.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.mechanics.MechanicType;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;

import static java.util.Objects.requireNonNull;

public class EventCarrier implements EventCarrierInterface {

	private final Map<ActionTrigger, Action<Player>[]> actionMap;
	private final Map<ActionTrigger, TreeMap<Integer, Action<Player>[]>> actionTimesMap;
	private final MechanicType type;
	private final boolean disableGlobalActions;
	private final String id;

	public EventCarrier(String id, MechanicType type, boolean disableGlobalActions,
			Map<ActionTrigger, Action<Player>[]> actionMap,
			Map<ActionTrigger, TreeMap<Integer, Action<Player>[]>> actionTimesMap) {
		this.actionMap = actionMap;
		this.actionTimesMap = actionTimesMap;
		this.type = type;
		this.disableGlobalActions = disableGlobalActions;
		this.id = id;
	}

	@Override
	public MechanicType type() {
		return type;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public boolean disableGlobalActions() {
		return disableGlobalActions;
	}

	@Override
	public void trigger(Context<Player> context, ActionTrigger trigger) {
		Optional.ofNullable(actionMap.get(trigger)).ifPresent(actions -> ActionManager.trigger(context, actions));
	}

	@Override
	public void trigger(Context<Player> context, ActionTrigger trigger, int previousTimes, int afterTimes) {
		Optional.ofNullable(actionTimesMap.get(trigger)).ifPresent(integerTreeMap -> {
			for (Map.Entry<Integer, Action<Player>[]> entry : integerTreeMap.entrySet()) {
				if (entry.getKey() <= previousTimes) {
					continue;
				}
				if (entry.getKey() > afterTimes) {
					return;
				}
				ActionManager.trigger(context, entry.getValue());
			}
		});
	}

	public static class Builder implements BuilderInterface {
		private final Map<ActionTrigger, Action<Player>[]> actionMap = new HashMap<>();
		private final Map<ActionTrigger, TreeMap<Integer, Action<Player>[]>> actionTimesMap = new HashMap<>();
		private MechanicType type = null;
		private boolean disableGlobalActions = false;
		private String id;

		@Override
		public Builder id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder actionMap(Map<ActionTrigger, Action<Player>[]> actionMap) {
			this.actionMap.putAll(actionMap);
			return this;
		}

		@Override
		public Builder action(ActionTrigger trigger, Action<Player>[] actions) {
			this.actionMap.put(trigger, actions);
			return this;
		}

		@Override
		public Builder actionTimesMap(Map<ActionTrigger, TreeMap<Integer, Action<Player>[]>> actionTimesMap) {
			this.actionTimesMap.putAll(actionTimesMap);
			return this;
		}

		@Override
		public Builder actionTimes(ActionTrigger trigger, TreeMap<Integer, Action<Player>[]> actions) {
			this.actionTimesMap.put(trigger, actions);
			return this;
		}

		@Override
		public Builder type(MechanicType type) {
			this.type = type;
			return this;
		}

		@Override
		public Builder disableGlobalActions(boolean value) {
			this.disableGlobalActions = value;
			return this;
		}

		@Override
		public EventCarrier build() {
			return new EventCarrier(requireNonNull(id), requireNonNull(type), disableGlobalActions, actionMap,
					actionTimesMap);
		}
	}
}