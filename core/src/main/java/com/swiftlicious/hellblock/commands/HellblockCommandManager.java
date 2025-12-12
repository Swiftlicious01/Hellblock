package com.swiftlicious.hellblock.commands;

import java.util.Collection;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.util.Index;

public interface HellblockCommandManager<C> {

	String commandsFile = "commands.yml";

	HellblockPlugin getPlugin();

	void unregisterFeatures();

	void registerFeature(CommandFeature<C> feature, CommandConfig<C> config);

	void registerDefaultFeatures();

	Index<String, CommandFeature<C>> getFeatures();

	void setFeedbackConsumer(@NotNull TriConsumer<C, String, Component> feedbackConsumer);

	TriConsumer<C, String, Component> defaultFeedbackConsumer();

	CommandConfig<C> getCommandConfig(YamlDocument document, String featureID);

	Collection<Command.Builder<C>> buildCommandBuilders(CommandConfig<C> config);

	CommandManager<C> getCommandManager();

	void handleCommandFeedback(C sender, TranslatableComponent.Builder key, Component... args);

	void handleCommandFeedback(C sender, String node, Component component);
}