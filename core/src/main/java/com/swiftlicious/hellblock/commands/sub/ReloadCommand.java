package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;

import net.kyori.adventure.text.Component;

public class ReloadCommand extends BukkitCommandFeature<CommandSender> {

	public ReloadCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.flag(manager.flagBuilder("silent").withAliases("s")).handler(context -> {
			long time = System.currentTimeMillis();
			HellblockPlugin.getInstance().reload();
			handleFeedback(context, MessageConstants.COMMAND_RELOAD_SUCCESS,
					Component.text(System.currentTimeMillis() - time));
		});
	}

	@Override
	public String getFeatureID() {
		return "reload";
	}
}