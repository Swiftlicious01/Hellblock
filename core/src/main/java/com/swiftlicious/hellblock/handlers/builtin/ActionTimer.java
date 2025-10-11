package com.swiftlicious.hellblock.handlers.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.handlers.AbstractActionManager;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ActionTimer<T> extends AbstractBuiltInAction<T> {

	private final List<Action<T>> actions;
	private final int delay;
	private final int duration;
	private final int period;
	private final boolean async;

	public ActionTimer(HellblockPlugin plugin, AbstractActionManager<T> manager, Object args, MathValue<T> chance) {
		super(plugin, chance);
		this.actions = new ArrayList<>();
		if (args instanceof Section section) {
			delay = section.getInt("delay", 2);
			duration = section.getInt("duration", 20);
			period = section.getInt("period", 2);
			async = section.getBoolean("async", false);
			final Section actionSection = section.getSection("actions");
			if (actionSection != null) {
				actionSection.getStringRouteMappedValues(false).entrySet().stream()
						.filter(entry -> entry.getValue() instanceof Section).map(entry -> (Section) entry.getValue())
						.forEach(innerSection -> actions.add(manager.parseAction(innerSection)));
			}
		} else {
			delay = 1;
			period = 1;
			async = false;
			duration = 20;
		}
	}

	@Override
	protected void triggerAction(Context<T> context) {
		final Location location = context.arg(ContextKeys.LOCATION);
		final SchedulerTask task;
		if (async) {
			task = plugin.getScheduler().asyncRepeating(() -> actions.forEach(action -> action.trigger(context)),
					delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
		} else {
			task = plugin.getScheduler().sync().runRepeating(() -> actions.forEach(action -> action.trigger(context)),
					delay, period, location);
		}
		plugin.getScheduler().asyncLater(task::cancel, duration * 50L, TimeUnit.MILLISECONDS);
	}

	public List<Action<T>> actions() {
		return actions;
	}

	public int delay() {
		return delay;
	}

	public int duration() {
		return duration;
	}

	public int period() {
		return period;
	}

	public boolean async() {
		return async;
	}
}