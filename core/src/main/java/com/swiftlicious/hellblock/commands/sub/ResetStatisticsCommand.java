package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;

public class ResetStatisticsCommand extends BukkitCommandFeature<CommandSender> {

	public ResetStatisticsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s"))
				.required("player", PlayerParser.playerParser()).handler(context -> {
					Player player = context.get("player");
					plugin.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresentOrElse(userData -> {
						userData.getStatisticData().reset();
						handleFeedback(context, MessageConstants.COMMAND_STATISTICS_RESET_SUCCESS,
								AdventureHelper.miniMessageToComponent(player.getName()));
					}, () -> handleFeedback(context, MessageConstants.COMMAND_STATISTICS_FAILURE_NOT_LOADED));
				});
	}

	@Override
	public String getFeatureID() {
		return "statistics_reset";
	}
}