package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class HellblockCoopCommand {

	public static HellblockCoopCommand INSTANCE = new HellblockCoopCommand();

	public CommandAPICommand getCoopCommand() {
		return new CommandAPICommand("coop").withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withSubcommands(ownerCommand("setowner"), removeCommand("kick"), inviteCommand("invite"), leaveCommand("leave"));
	}

	private CommandAPICommand ownerCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new PlayerArgument("player"))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = new HellblockPlayer(player.getUniqueId());
					HellblockPlayer transferPlayer = new HellblockPlayer(user.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(owner, transferPlayer);
				});
	}

	private CommandAPICommand removeCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new PlayerArgument("player"))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = new HellblockPlayer(player.getUniqueId());
					HellblockPlayer removePlayer = new HellblockPlayer(user.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(owner, removePlayer);
				});
	}

	private CommandAPICommand leaveCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer leavingPlayer = new HellblockPlayer(player.getUniqueId());
			HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer);
		});
	}

	private CommandAPICommand inviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withArguments(new PlayerArgument("player"))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = new HellblockPlayer(player.getUniqueId());
					HellblockPlayer addPlayer = new HellblockPlayer(user.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(owner, addPlayer);
				});
	}
}
