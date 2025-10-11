package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionDelay<T> extends AbstractBuiltInAction<T> {

	private final List<Action<T>> actions;
	private final int delay;
	private final boolean async;

	public ActionDelay(HellblockPlugin plugin, AbstractActionManager<T> manager, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.actions = new ArrayList<>();
		if (args instanceof Section section) {
			delay = section.getInt("delay", 1);
			async = section.getBoolean("async", false);
			final Section actionSection = section.getSection("actions");
			if (actionSection != null) {
				actionSection.getStringRouteMappedValues(false).entrySet().stream()
						.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
						.forEach(innerSection -> actions.add(manager.parseAction(innerSection)));
			}
		} else {
			delay = 1;
			async = false;
		}
	}

	@Override
	protected void triggerAction(Context<T> context) {
		final Location location = context.arg(ContextKeys.LOCATION);
		if (async) {
			plugin.getScheduler().asyncLater(() -> actions.forEach(action -> action.trigger(context)), delay * 50L,
					TimeUnit.MILLISECONDS);
		} else {
			plugin.getScheduler().sync().runLater(() -> actions.forEach(action -> action.trigger(context)), delay,
					location);
		}
	}

	public List<Action<T>> actions() {
		return actions;
	}

	public int delay() {
		return delay;
	}

	public boolean async() {
		return async;
	}
}