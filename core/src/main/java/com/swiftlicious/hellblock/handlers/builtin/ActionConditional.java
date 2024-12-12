package com.swiftlicious.hellblock.handlers.builtin;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Requirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionConditional<T> extends AbstractBuiltInAction<T> {

	private final Action<T>[] actions;
	private final Requirement<T>[] requirements;

	public ActionConditional(HellblockPlugin plugin, AbstractActionManager<T> manager, Class<T> tClass, Section section,
			MathValue<T> chance) {
		super(plugin, chance);
		this.actions = manager.parseActions(section.getSection("actions"));
		this.requirements = plugin.getRequirementManager(tClass).parseRequirements(section.getSection("conditions"),
				true);
	}

	@Override
	protected void triggerAction(Context<T> context) {
		for (Requirement<T> requirement : requirements) {
			if (!requirement.isSatisfied(context)) {
				return;
			}
		}
		for (Action<T> action : actions) {
			action.trigger(context);
		}
	}

	public Action<T>[] actions() {
		return actions;
	}

	public Requirement<T>[] requirements() {
		return requirements;
	}
}