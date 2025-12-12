package com.swiftlicious.hellblock.commands;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.sender.SenderFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public abstract class AbstractCommandFeature<C> implements CommandFeature<C> {

	protected final HellblockCommandManager<C> commandManager;
	protected final HellblockPlugin plugin;
	protected CommandConfig<C> commandConfig;

	public AbstractCommandFeature(HellblockCommandManager<C> commandManager) {
		this.commandManager = commandManager;
		this.plugin = commandManager.getPlugin();
	}

	protected abstract SenderFactory<?, C> getSenderFactory();

	public abstract Command.Builder<? extends C> assembleCommand(CommandManager<C> manager, Command.Builder<C> builder);

	@Override
	@SuppressWarnings("unchecked")
	public Command<C> registerCommand(CommandManager<C> manager, Command.Builder<C> builder) {
		final Command<C> command = (Command<C>) assembleCommand(manager, builder).build();
		manager.command(command);
		return command;
	}

	@Override
	public void registerRelatedFunctions() {
		// empty
	}

	@Override
	public void unregisterRelatedFunctions() {
		// empty
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleFeedbackRaw(CommandContext<?> context, Component... components) {
		if (context.flags().hasFlag("silent")) {
			return;
		}
		for (Component component : components) {
			// "dynamic" node — can be a fixed label like "raw" or your feature ID
			commandManager.handleCommandFeedback((C) context.sender(), getFeatureID(), component);
		}
	}

	@Override
	public void handleFeedbackRaw(C sender, Component... components) {
		for (Component component : components) {
			commandManager.handleCommandFeedback(sender, getFeatureID(), component);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handleFeedback(CommandContext<?> context, TranslatableComponent.Builder key, Component... args) {
		if (context.flags().hasFlag("silent")) {
			return;
		}
		commandManager.handleCommandFeedback((C) context.sender(), key, args);
	}

	@Override
	public void handleFeedback(C sender, TranslatableComponent.Builder key, Component... args) {
		commandManager.handleCommandFeedback(sender, key, args);
	}

	@Override
	public HellblockCommandManager<C> getHellblockCommandManager() {
		return commandManager;
	}

	@Override
	public CommandConfig<C> getCommandConfig() {
		return commandConfig;
	}

	public void setCommandConfig(CommandConfig<C> commandConfig) {
		this.commandConfig = commandConfig;
	}
}