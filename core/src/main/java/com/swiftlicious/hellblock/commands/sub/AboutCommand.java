package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

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
			final Sender audience = plugin.getSenderFactory().wrap(player);

			// Gather plugin metadata
			final String version = VersionHelper.getPluginVersion();
			final String description = plugin.getDescription().getDescription();
			final String author = plugin.getDescription().getAuthors().isEmpty() ? "Unknown"
					: plugin.getDescription().getAuthors().getFirst();
			final String website = plugin.getDescription().getWebsite();

			// Messages
			audience.sendMessage(AdventureHelper
					.miniMessageToComponent("<#00BFFF>\uD83C\uDFDD Hellblock <gray>- <#87CEEB>%s".formatted(version)));
			audience.sendMessage(AdventureHelper.miniMessageToComponent("<#B0C4DE>%s".formatted(description)));
			audience.sendMessage(AdventureHelper
					.miniMessageToComponent("<#DA70D6>\uD83E\uDDEA Author: <#FFC0CB>%s".formatted(author)));

			if (website != null && !website.isBlank()) {
				audience.sendMessage(AdventureHelper
						.miniMessageToComponent("<#FAFAD2>⛏ <click:open_url:%s>Github</click>".formatted(website)));
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "about";
	}
}