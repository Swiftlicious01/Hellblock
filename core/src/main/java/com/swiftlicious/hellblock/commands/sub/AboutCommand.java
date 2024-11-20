package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;

public class AboutCommand extends BukkitCommandFeature<CommandSender> {

	public AboutCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<#00BFFF>\uD83C\uDFDD Hellblock <gray>- <#87CEEB>"
							+ HellblockPlugin.getInstance().getVersionManager().getPluginVersion());
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<#B0C4DE>" + HellblockPlugin.getInstance().getPluginMeta().getDescription());
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<#DA70D6>\uD83E\uDDEA Author: <#FFC0CB>"
							+ HellblockPlugin.getInstance().getPluginMeta().getAuthors().getFirst());
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<#FAFAD2>⛏ <click:open_url:https://github.com/Swiftlicious01/Hellblock>Github</click>");
		});
	}

	@Override
	public String getFeatureID() {
		return "about";
	}
}
