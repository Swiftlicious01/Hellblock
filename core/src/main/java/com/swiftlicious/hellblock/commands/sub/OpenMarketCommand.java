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

public class OpenMarketCommand extends BukkitCommandFeature<CommandSender> {

	public OpenMarketCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.required("player", PlayerParser.playerParser())
				.flag(manager.flagBuilder("silent").withAliases("s").build()).handler(context -> {
					final Player player = context.get("player");
					if (plugin.getMarketManager().openMarketGUI(player)) {
						handleFeedback(context, MessageConstants.COMMAND_MARKET_OPEN_SUCCESS,
								AdventureHelper.miniMessageToComponent(player.getName()));
					} else {
						handleFeedback(context, MessageConstants.COMMAND_MARKET_OPEN_FAILURE_NOT_LOADED,
								AdventureHelper.miniMessageToComponent(player.getName()));
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "open_market";
	}
}