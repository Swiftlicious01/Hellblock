package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.parser.standard.EnumParser;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.FishingStatistics;

public class QueryStatisticsCommand extends BukkitCommandFeature<CommandSender> {

	public QueryStatisticsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s"))
				.required("player", PlayerParser.playerParser())
				.required("type", EnumParser.enumParser(FishingStatistics.Type.class)).handler(context -> {
					Player player = context.get("player");
					FishingStatistics.Type type = context.get("type");
					plugin.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresentOrElse(userData -> {
						if (type == FishingStatistics.Type.AMOUNT_OF_FISH_CAUGHT) {
							handleFeedback(context, MessageConstants.COMMAND_STATISTICS_QUERY_AMOUNT,
									AdventureHelper.miniMessageToComponent(userData.getStatisticData().amountMap().toString()));
						} else if (type == FishingStatistics.Type.MAX_SIZE) {
							handleFeedback(context, MessageConstants.COMMAND_STATISTICS_QUERY_SIZE,
									AdventureHelper.miniMessageToComponent(userData.getStatisticData().sizeMap().toString()));
						}
					}, () -> handleFeedback(context, MessageConstants.COMMAND_STATISTICS_FAILURE_NOT_LOADED));
				});
	}

	@Override
	public String getFeatureID() {
		return "statistics_query";
	}
}