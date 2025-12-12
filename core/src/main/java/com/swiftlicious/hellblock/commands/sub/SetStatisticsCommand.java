package com.swiftlicious.hellblock.commands.sub;

import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.FishingStatistics;

public class SetStatisticsCommand extends BukkitCommandFeature<CommandSender> {

	public SetStatisticsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s"))
				.required("player", PlayerParser.playerParser())
				.required("type", EnumParser.enumParser(FishingStatistics.Type.class))
				.required("id",
						StringParser.stringComponent()
								.suggestionProvider((context,
										input) -> CompletableFuture.completedFuture(plugin.getLootManager()
												.getRegisteredLoots().stream().filter(loot -> !loot.disableStats())
												.map(loot -> Suggestion.suggestion(loot.id())).toList())))
				.required("value", DoubleParser.doubleParser(0)).handler(context -> {
					Player player = context.get("player");
					String id = context.get("id");
					FishingStatistics.Type type = context.get("type");
					double value = context.get("value");
					plugin.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresentOrElse(userData -> {
						if (type == FishingStatistics.Type.AMOUNT_OF_FISH_CAUGHT) {
							userData.getStatisticData().setAmount(id, (int) value);
							handleFeedback(context, MessageConstants.COMMAND_STATISTICS_MODIFY_SUCCESS,
									AdventureHelper.miniMessageToComponent(player.getName()));
						} else if (type == FishingStatistics.Type.MAX_SIZE) {
							userData.getStatisticData().setMaxSize(id, (float) value);
							handleFeedback(context, MessageConstants.COMMAND_STATISTICS_MODIFY_SUCCESS,
									AdventureHelper.miniMessageToComponent(player.getName()));
						}
					}, () -> handleFeedback(context, MessageConstants.COMMAND_STATISTICS_FAILURE_NOT_LOADED));
				});
	}

	@Override
	public String getFeatureID() {
		return "statistics_set";
	}
}