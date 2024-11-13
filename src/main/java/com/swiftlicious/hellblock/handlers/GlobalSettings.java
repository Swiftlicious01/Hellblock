package com.swiftlicious.hellblock.handlers;

import java.util.HashMap;
import java.util.Map;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.effects.EffectModifier;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.ActionTrigger;
import com.swiftlicious.hellblock.utils.extras.Condition;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents global settings for actions related to fishing, loot, rods, and
 * bait.
 */
public class GlobalSettings implements Reloadable {

	private Map<ActionTrigger, Action[]> lootActions = new HashMap<>();
	private Map<ActionTrigger, Action[]> rodActions = new HashMap<>();
	private Map<ActionTrigger, Action[]> baitActions = new HashMap<>();
	private Map<ActionTrigger, Action[]> hookActions = new HashMap<>();
	private EffectModifier[] effectModifiers;

	/**
	 * Loads global settings from a configuration section.
	 *
	 * @param section The configuration section to load settings from.
	 */
	public void loadEvents(Section section) {
		if (section == null)
			return;
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				Map<ActionTrigger, Action[]> map = HellblockPlugin.getInstance().getActionManager().getActionMap(inner);
				switch (entry.getKey()) {
				case "loot" -> lootActions = map;
				case "rod" -> rodActions = map;
				case "bait" -> baitActions = map;
				case "hook" -> hookActions = map;
				}
			}
		}
	}

	public EffectModifier[] getEffectModifiers() {
		return effectModifiers;
	}

	public void setEffects(EffectModifier[] modifiers) {
		effectModifiers = modifiers;
	}

	/**
	 * Unloads global settings, clearing all action maps.
	 */
	@Override
	public void unload() {
		lootActions.clear();
		rodActions.clear();
		baitActions.clear();
		hookActions.clear();
	}

	/**
	 * Triggers loot-related actions for a specific trigger and condition.
	 *
	 * @param trigger   The trigger to activate actions for.
	 * @param condition The condition that triggered the actions.
	 */
	public void triggerLootActions(ActionTrigger trigger, Condition condition) {
		Action[] actions = lootActions.get(trigger);
		if (actions != null) {
			for (Action action : actions) {
				action.trigger(condition);
			}
		}
	}

	/**
	 * Triggers rod-related actions for a specific trigger and condition.
	 *
	 * @param trigger   The trigger to activate actions for.
	 * @param condition The condition that triggered the actions.
	 */
	public void triggerRodActions(ActionTrigger trigger, Condition condition) {
		Action[] actions = rodActions.get(trigger);
		if (actions != null) {
			for (Action action : actions) {
				action.trigger(condition);
			}
		}
	}

	/**
	 * Triggers bait-related actions for a specific trigger and condition.
	 *
	 * @param trigger   The trigger to activate actions for.
	 * @param condition The condition that triggered the actions.
	 */
	public void triggerBaitActions(ActionTrigger trigger, Condition condition) {
		Action[] actions = baitActions.get(trigger);
		if (actions != null) {
			for (Action action : actions) {
				action.trigger(condition);
			}
		}
	}

	/**
	 * Triggers hook-related actions for a specific trigger and condition.
	 *
	 * @param trigger   The trigger to activate actions for.
	 * @param condition The condition that triggered the actions.
	 */
	public void triggerHookActions(ActionTrigger trigger, Condition condition) {
		Action[] actions = hookActions.get(trigger);
		if (actions != null) {
			for (Action action : actions) {
				action.trigger(condition);
			}
		}
	}
}
