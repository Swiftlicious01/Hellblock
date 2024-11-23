package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;

public class CoopHelpCommand extends BukkitCommandFeature<CommandSender> {

	public CoopHelpCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<dark_red>Hellblock Coop Commands:");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop invite <player>: Invite another player to your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop accept <player>: Accept an invite to another player's island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop decline <player>: Reject an invite to another player's island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop invitations: See a list of invitations sent to you in the past 24 hours!");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop cancel <player>: Cancel an invite you sent to a player to join your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop kick <player>: Kick the player from your party");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop leave: Leave the island you're apart of");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop setowner <player>: Set the new owner of your island");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop trust <player>: Trust a player to your island without inviting them");
			HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
					"<red>/hellcoop untrust <player>: Untrusts a player from your island");
		});
	}

	@Override
	public String getFeatureID() {
		return "coop_help";
	}
}