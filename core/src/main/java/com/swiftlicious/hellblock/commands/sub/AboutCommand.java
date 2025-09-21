package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.sender.Sender;

public class AboutCommand extends BukkitCommandFeature<CommandSender> {

	public AboutCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			Sender audience = HellblockPlugin.getInstance().getSenderFactory().wrap(player);
			String version = VersionHelper.getPluginVersion();
			String desc = HellblockPlugin.getInstance().getDescription().getDescription();
			String author = HellblockPlugin.getInstance().getDescription().getAuthors().getFirst();
			String link = HellblockPlugin.getInstance().getDescription().getWebsite();
			audience.sendMessage(AdventureHelper
					.miniMessage(String.format("<#00BFFF>\uD83C\uDFDD Hellblock <gray>- <#87CEEB> %s", version)));
			audience.sendMessage(AdventureHelper.miniMessage(String.format("<#B0C4DE> %s", desc)));
			audience.sendMessage(
					AdventureHelper.miniMessage(String.format("<#DA70D6>\uD83E\uDDEA Author: <#FFC0CB> %s", author)));
			audience.sendMessage(
					AdventureHelper.miniMessage(String.format("<#FAFAD2>⛏ <click:open_url:%s>Github</click>", link)));
		});
	}

	@Override
	public String getFeatureID() {
		return "about";
	}
}
