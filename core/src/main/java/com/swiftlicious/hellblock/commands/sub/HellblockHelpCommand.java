package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;

public class HellblockHelpCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockHelpCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<dark_red>Hellblock Commands:");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock create: Create your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock reset: Reset your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock info: See information about your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock home: Teleport to your island home");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock fixhome: Fix your home location if it has been set to spawn");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock sethome: Set the new home location of your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock lock/unlock: Change whether or not visitors can access your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock setbiome <biome>: Change the biome of your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock ban/unban <player>: Deny access to this player to your island");
			;
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock visit <player>: Visit another player's island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellblock top: View the hellblocks with the top levels");
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_help";
	}
}