package com.swiftlicious.hellblock.handlers.builtin;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.utils.extras.Action;
import com.swiftlicious.hellblock.utils.extras.MathValue;

public abstract class AbstractBuiltInAction<T> implements Action<T> {

	protected final HellblockPlugin plugin;
	protected final MathValue<T> chance;

	protected AbstractBuiltInAction(HellblockPlugin plugin, MathValue<T> chance) {
		this.plugin = plugin;
		this.chance = chance;
	}

	public HellblockPlugin plugin() {
		return plugin;
	}

	public MathValue<T> chance() {
		return chance;
	}

	@Override
	public void trigger(Context<T> context) {
		if (Math.random() > chance.evaluate(context))
			return;
		triggerAction(context);
	}

	protected abstract void triggerAction(Context<T> context);
}