package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;

import net.kyori.adventure.text.Component;

public class HellblockWarpsCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockWarpsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).optional("sort", StringParser.stringComponent()
				.suggestionProvider((context, input) -> CompletableFuture.completedFuture(
						Arrays.stream(VisitSorter.values()).map(Enum::toString).map(Suggestion::suggestion).toList())))
				.handler(context -> {
					Player player = context.sender();
					String sortArg = ((String) context.optional("sort").orElse(VisitSorter.FEATURED.toString()))
							.toUpperCase(Locale.ENGLISH);

					VisitGUIManager.VisitSorter sorter;

					try {
						sorter = VisitGUIManager.VisitSorter.valueOf(sortArg);
					} catch (IllegalArgumentException e) {
						handleFeedback(context,
								MessageConstants.COMMAND_INVALID_SORT_TYPE.arguments(Component.text(sortArg)));
						return;
					}

					boolean success = HellblockPlugin.getInstance().getVisitGUIManager().openVisitGUI(player, sorter,
							false);

					if (!success) {
						handleFeedback(context, MessageConstants.COMMAND_VISIT_GUI_FAILED);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_warps";
	}
}