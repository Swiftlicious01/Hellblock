package com.swiftlicious.hellblock.commands.sub;

import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class HellblockCoopCommand {

	public static HellblockCoopCommand INSTANCE = new HellblockCoopCommand();

	public CommandAPICommand getCoopCommand() {
		return new CommandAPICommand("coop").withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withSubcommands(ownerCommand("setowner"), removeCommand("kick"), inviteCommand("invite"),
						leaveCommand("leave"));
	}

	private CommandAPICommand ownerCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					HellblockPlayer transferPlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(user);
					HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(owner, transferPlayer);
				});
	}

	private CommandAPICommand removeCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					HellblockPlayer removePlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(user);
					HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(owner, removePlayer);
				});
	}

	private CommandAPICommand leaveCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer leavingPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer);
		});
	}

	private CommandAPICommand inviteCommand(String namespace) {
		return new CommandAPICommand(namespace)
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					Player user = (Player) args.getOrDefault("player", player);
					HellblockPlayer owner = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					HellblockPlayer addPlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(user);
					HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(owner, addPlayer);
				});
	}
}
