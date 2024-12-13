package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;
import com.swiftlicious.hellblock.utils.extras.Pair;
import com.swiftlicious.hellblock.utils.extras.Requirement;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionPriority<T> extends AbstractBuiltInAction<T> {

	private final List<Pair<Requirement<T>[], Action<T>[]>> conditionActionPairList;

	public ActionPriority(HellblockPlugin plugin, AbstractActionManager<T> manager, Class<T> tClass, Section section,
			MathValue<T> chance) {
		super(plugin, chance);
		this.conditionActionPairList = new ArrayList<>();
		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (entry.getValue() instanceof Section inner) {
				Action<T>[] actions = manager.parseActions(inner.getSection("actions"));
				Requirement<T>[] requirements = plugin.getRequirementManager(tClass)
						.parseRequirements(inner.getSection("conditions"), false);
				conditionActionPairList.add(Pair.of(requirements, actions));
			}
		}
	}

	@Override
	protected void triggerAction(Context<T> context) {
		outer: for (Pair<Requirement<T>[], Action<T>[]> pair : conditionActionPairList) {
			if (pair.left() != null)
				for (Requirement<T> requirement : pair.left()) {
					if (!requirement.isSatisfied(context)) {
						continue outer;
					}
				}
			if (pair.right() != null)
				for (Action<T> action : pair.right()) {
					action.trigger(context);
				}
			return;
		}
	}

	public List<Pair<Requirement<T>[], Action<T>[]>> conditionalActions() {
		return conditionActionPairList;
	}
}