package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;

public class ReloadCommand extends BukkitCommandFeature<CommandSender> {

	public ReloadCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s")).handler(context -> {
			if (!plugin.isReloading()) {
				final long time = System.currentTimeMillis();
				plugin.reload();
				handleFeedback(context, MessageConstants.COMMAND_RELOAD_SUCCESS,
						AdventureHelper.miniMessageToComponent(String.valueOf(System.currentTimeMillis() - time)));
			} else {
				handleFeedback(context, MessageConstants.COMMAND_RELOADING);
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "reload";
	}
}