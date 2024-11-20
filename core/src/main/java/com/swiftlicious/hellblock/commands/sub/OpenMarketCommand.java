package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;

import net.kyori.adventure.text.Component;

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
					if (HellblockPlugin.getInstance().getMarketManager().openMarketGUI(player)) {
						handleFeedback(context, MessageConstants.COMMAND_MARKET_OPEN_SUCCESS,
								Component.text(player.getName()));
					} else {
						handleFeedback(context, MessageConstants.COMMAND_MARKET_OPEN_FAILURE_NOT_LOADED,
								Component.text(player.getName()));
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "open_market";
	}
}