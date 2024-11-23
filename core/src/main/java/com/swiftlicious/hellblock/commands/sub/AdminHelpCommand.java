package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;

public class AdminHelpCommand extends BukkitCommandFeature<CommandSender> {

	public AdminHelpCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<dark_red>Hellblock Admin Commands:");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock admin goto <player>: Forcefully teleports you to this player's island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock admin delete <player>: Forcefully deletes this player's island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock admin purge <days>: Forcefully abandon the islands that have been inactive for this many days");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock admin genspawn: Generate the spawn area if it was deleted for any reason");
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_help";
	}
}