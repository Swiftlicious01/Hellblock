package com.swiftlicious.hellblock.commands;

import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.CommandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public interface CommandFeature<C> {

	Command<C> registerCommand(CommandManager<C> cloudCommandManager, Command.Builder<C> builder);

	String getFeatureID();

	void registerRelatedFunctions();

	void unregisterRelatedFunctions();

	void handleFeedback(CommandContext<?> context, TranslatableComponent.Builder key, Component... args);

	void handleFeedback(C sender, TranslatableComponent.Builder key, Component... args);

	void handleFeedbackRaw(CommandContext<?> context, Component... components);

	void handleFeedbackRaw(C sender, Component... components);

	HellblockCommandManager<C> getHellblockCommandManager();

	CommandConfig<C> getCommandConfig();
}