package com.swiftlicious.hellblock.loot;

import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.effects.BaseEffect;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;

public class HBLoot implements Loot {

	private final String id;
	private final LootType type;
	private final HashMap<ActionTrigger, Action[]> actionMap;
	private final HashMap<Integer, Action[]> successTimesActionMap;
	private String nick;
	private boolean showInFinder;
	private boolean disableGlobalAction;
	private String[] lootGroup;
	private BaseEffect effect;

	public HBLoot(String id, LootType type) {
		this.id = id;
		this.type = type;
		this.actionMap = new HashMap<>();
		this.successTimesActionMap = new HashMap<>();
	}

	public Builder builder(String id, LootType type) {
		return new Builder(id, type);
	}

	/**
	 * Builder class for HBLoot.
	 */
	public static class Builder {

		private final HBLoot loot;

		public Builder(String id, LootType type) {
			this.loot = new HBLoot(id, type);
		}

		/**
		 * Set the file path for this loot.
		 *
		 * @param path file path
		 * @return The builder.
		 */
		public Builder filePath(String path) {
			return this;
		}

		/**
		 * Set the nickname for this loot.
		 *
		 * @param nick The nickname.
		 * @return The builder.
		 */
		public Builder nick(String nick) {
			this.loot.nick = nick;
			return this;
		}

		/**
		 * Set whether this loot should be shown in the finder.
		 *
		 * @param show True if it should be shown, false otherwise.
		 * @return The builder.
		 */
		public Builder showInFinder(boolean show) {
			this.loot.showInFinder = show;
			return this;
		}

		/**
		 * Set whether global actions are disabled for this loot.
		 *
		 * @param disable True if statistics are disabled, false otherwise.
		 * @return The builder.
		 */
		public Builder disableGlobalActions(boolean disable) {
			this.loot.disableGlobalAction = disable;
			return this;
		}

		/**
		 * Set the loot group for this loot.
		 *
		 * @param groups The loot group.
		 * @return The builder.
		 */
		public Builder lootGroup(String[] groups) {
			this.loot.lootGroup = groups;
			return this;
		}

		/**
		 * Set the effects for the loot
		 *
		 * @param effect effect
		 * @return The builder.
		 */
		public Builder baseEffect(BaseEffect effect) {
			this.loot.effect = effect;
			return this;
		}

		/**
		 * Add actions triggered by a specific trigger.
		 *
		 * @param trigger The trigger for the actions.
		 * @param actions The actions to add.
		 * @return The builder.
		 */
		public Builder addActions(ActionTrigger trigger, Action[] actions) {
			this.loot.actionMap.put(trigger, actions);
			return this;
		}

		/**
		 * Add actions triggered by multiple triggers.
		 *
		 * @param actionMap A map of triggers to actions.
		 * @return The builder.
		 */
		public Builder addActions(HashMap<ActionTrigger, Action[]> actionMap) {
			this.loot.actionMap.putAll(actionMap);
			return this;
		}

		/**
		 * Add actions triggered by the number of successes.
		 *
		 * @param times   The number of successes for triggering the actions.
		 * @param actions The actions to add.
		 * @return The builder.
		 */
		public Builder addTimesActions(int times, Action[] actions) {
			this.loot.successTimesActionMap.put(times, actions);
			return this;
		}

		/**
		 * Add actions triggered by multiple numbers of successes.
		 *
		 * @param actionMap A map of numbers of successes to actions.
		 * @return The builder.
		 */
		public Builder addTimesActions(HashMap<Integer, Action[]> actionMap) {
			this.loot.successTimesActionMap.putAll(actionMap);
			return this;
		}

		/**
		 * Build the HBLoot object.
		 *
		 * @return The built HBLoot object.
		 */
		public HBLoot build() {
			return loot;
		}
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public LootType getType() {
		return this.type;
	}

	@Override
	public @NotNull String getNick() {
		return this.nick;
	}

	@Override
	public boolean showInFinder() {
		return this.showInFinder;
	}

	@Override
	public boolean disableGlobalAction() {
		return this.disableGlobalAction;
	}

	@Override
	public String[] getLootGroup() {
		return lootGroup;
	}

	@Override
	public Action[] getActions(ActionTrigger actionTrigger) {
		return actionMap.get(actionTrigger);
	}

	@Override
	public void triggerActions(ActionTrigger actionTrigger, Condition condition) {
		Action[] actions = getActions(actionTrigger);
		if (actions != null) {
			for (Action action : actions) {
				action.trigger(condition);
			}
		}
	}

	@Override
	public BaseEffect getBaseEffect() {
		return effect;
	}

	@Override
	public Action[] getSuccessTimesActions(int times) {
		return successTimesActionMap.get(times);
	}

	@Override
	public HashMap<Integer, Action[]> getSuccessTimesActionMap() {
		return successTimesActionMap;
	}
}