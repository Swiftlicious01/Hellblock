package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionChain<T> extends AbstractBuiltInAction<T> {

	private final List<Action<T>> actions;

	public ActionChain(HellblockPlugin plugin, AbstractActionManager<T> manager, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.actions = new ArrayList<>();
		if (args instanceof Section section) {
			section.getStringRouteMappedValues(false).entrySet().stream()
					.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
					.forEach(innerSection -> actions.add(manager.parseAction(innerSection)));
		}
	}

	@Override
	protected void triggerAction(Context<T> context) {
		actions.forEach(action -> action.trigger(context));
	}

	public List<Action<T>> actions() {
		return actions;
	}
}