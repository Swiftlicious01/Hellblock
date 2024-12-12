package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.player.Context;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionChain<T> extends AbstractBuiltInAction<T> {

	private final List<Action<T>> actions;

	public ActionChain(HellblockPlugin plugin, AbstractActionManager<T> manager, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.actions = new ArrayList<>();
		if (args instanceof Section section) {
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				if (entry.getValue() instanceof Section innerSection) {
					actions.add(manager.parseAction(innerSection));
				}
			}
		}
	}

	@Override
	protected void triggerAction(Context<T> context) {
		for (Action<T> action : actions) {
			action.trigger(context);
		}
	}

	public List<Action<T>> actions() {
		return actions;
	}
}