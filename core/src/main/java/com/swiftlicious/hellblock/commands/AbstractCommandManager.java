package com.swiftlicious.hellblock.commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.handling.ExceptionContext;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.HellblockCaptionFormatter;
import com.swiftlicious.hellblock.config.locale.HellblockCaptionProvider;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.ArrayUtils;
import com.swiftlicious.hellblock.utils.extras.TriConsumer;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;

public abstract class AbstractCommandManager<C> implements HellblockCommandManager<C> {

	protected final Set<CommandComponent<C>> registeredRootCommandComponents = new HashSet<>();
	protected final Set<CommandFeature<C>> registeredFeatures = new HashSet<>();
	protected final CommandManager<C> commandManager;
	protected final HellblockPlugin plugin;
	private final HellblockCaptionFormatter<C> captionFormatter = new HellblockCaptionFormatter<C>();
	private final MinecraftExceptionHandler.Decorator<C> decorator = (formatter, ctx, msg) -> msg;

	private TriConsumer<C, String, Component> feedbackConsumer;

	public AbstractCommandManager(HellblockPlugin plugin, CommandManager<C> commandManager) {
		this.commandManager = commandManager;
		this.plugin = plugin;
		this.inject();
		this.feedbackConsumer = defaultFeedbackConsumer();
	}

	@Override
	public void setFeedbackConsumer(@NotNull TriConsumer<C, String, Component> feedbackConsumer) {
		this.feedbackConsumer = feedbackConsumer;
	}

	@Override
	public TriConsumer<C, String, Component> defaultFeedbackConsumer() {
		return ((sender, node, component) -> {
			wrapSender(sender).sendMessage(component, true);
		});
	}

	protected abstract Sender wrapSender(C c);

	private void inject() {
		getCommandManager().captionRegistry().registerProvider(new HellblockCaptionProvider<>());
		injectExceptionHandler(InvalidSyntaxException.class,
				MinecraftExceptionHandler.createDefaultInvalidSyntaxHandler(),
				StandardCaptionKeys.EXCEPTION_INVALID_SYNTAX);
		injectExceptionHandler(InvalidCommandSenderException.class,
				MinecraftExceptionHandler.createDefaultInvalidSenderHandler(),
				StandardCaptionKeys.EXCEPTION_INVALID_SENDER);
		injectExceptionHandler(NoPermissionException.class,
				MinecraftExceptionHandler.createDefaultNoPermissionHandler(),
				StandardCaptionKeys.EXCEPTION_NO_PERMISSION);
		injectExceptionHandler(ArgumentParseException.class,
				MinecraftExceptionHandler.createDefaultArgumentParsingHandler(),
				StandardCaptionKeys.EXCEPTION_INVALID_ARGUMENT);
		injectExceptionHandler(CommandExecutionException.class,
				MinecraftExceptionHandler.createDefaultCommandExecutionHandler(),
				StandardCaptionKeys.EXCEPTION_UNEXPECTED);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void injectExceptionHandler(Class<? extends Throwable> type,
			MinecraftExceptionHandler.MessageFactory<C, ?> factory, Caption key) {
		getCommandManager().exceptionController().registerHandler(type, ctx -> {
			final @Nullable ComponentLike message = factory.message(captionFormatter, (ExceptionContext) ctx);
			if (message != null) {
				handleCommandFeedback(ctx.context().sender(), key.key(),
						decorator.decorate(captionFormatter, ctx, message.asComponent()).asComponent());
			}
		});
	}

	@Override
	public CommandConfig<C> getCommandConfig(YamlDocument document, String featureID) {
		Section section = document.getSection(featureID);
		if (section == null)
			return null;
		return new CommandConfig.Builder<C>().permission(section.getString("permission"))
				.usages(section.getStringList("usage")).enable(section.getBoolean("enable", false)).build();
	}

	@Override
	public Collection<Command.Builder<C>> buildCommandBuilders(CommandConfig<C> config) {
		List<Command.Builder<C>> list = new ArrayList<>();
		for (String usage : config.getUsages()) {
			if (!usage.startsWith("/"))
				continue;
			String command = usage.substring(1).trim();
			String[] split = command.split(" ");
			Command.Builder<C> builder = new CommandBuilder.BasicCommandBuilder<>(getCommandManager(), split[0])
					.setCommandNode(ArrayUtils.subArray(split, 1)).setPermission(config.getPermission())
					.getBuiltCommandBuilder();
			list.add(builder);
		}
		return list;
	}

	@Override
	public void registerFeature(CommandFeature<C> feature, CommandConfig<C> config) {
		if (!config.isEnabled())
			throw new RuntimeException("Registering a disabled command feature is not allowed");
		for (Command.Builder<C> builder : buildCommandBuilders(config)) {
			Command<C> command = feature.registerCommand(commandManager, builder);
			this.registeredRootCommandComponents.add(command.rootComponent());
		}
		feature.registerRelatedFunctions();
		this.registeredFeatures.add(feature);
		((AbstractCommandFeature<C>) feature).setCommandConfig(config);
	}

	@Override
	public void registerDefaultFeatures() {
		YamlDocument document = plugin.getConfigManager().loadConfig(commandsFile);
		try {
			document.save(new File(plugin.getDataFolder().toPath().toAbsolutePath().toFile(), "commands.yml"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.getFeatures().values().forEach(feature -> {
			CommandConfig<C> config = getCommandConfig(document, feature.getFeatureID());
			if (config.isEnabled()) {
				registerFeature(feature, config);
			}
		});
	}

	@Override
	public void unregisterFeatures() {
		this.registeredRootCommandComponents.forEach(
				component -> this.commandManager.commandRegistrationHandler().unregisterRootCommand(component));
		this.registeredRootCommandComponents.clear();
		this.registeredFeatures.forEach(CommandFeature::unregisterRelatedFunctions);
		this.registeredFeatures.clear();
	}

	@Override
	public CommandManager<C> getCommandManager() {
		return commandManager;
	}

	@Override
	public void handleCommandFeedback(C sender, TranslatableComponent.Builder key, Component... args) {
		TranslatableComponent component = key.arguments(args).build();
		this.feedbackConsumer.accept(sender, component.key(), plugin.getTranslationManager().render(component));
	}

	@Override
	public void handleCommandFeedback(C sender, String node, Component component) {
		this.feedbackConsumer.accept(sender, node, component);
	}
}