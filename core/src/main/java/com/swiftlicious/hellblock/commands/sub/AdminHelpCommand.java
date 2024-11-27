package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;

public class AdminHelpCommand extends BukkitCommandFeature<CommandSender> {

	public AdminHelpCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_HELP);
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_help";
	}
}