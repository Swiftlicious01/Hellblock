package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.UUIDFetcher;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class HellblockCoopCommand {

	public static HellblockCoopCommand INSTANCE = new HellblockCoopCommand();

	public CommandAPICommand getCoopCommand() {
		return new CommandAPICommand("coop").withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withSubcommands(getOwnerCommand("setowner"), getRemoveCommand("kick"), getInviteCommand("invite"),
						getLeaveCommand("leave"), getTrustCommand("trust"), getUntrustCommand("untrust"));
	}

	private CommandAPICommand getOwnerCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("transferowner")
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						Player user = (Player) args.getOrDefault("player", player);
						if (user == null || !user.isOnline()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you entered is either not online or doesn't exist!");
							return;
						}
						if (user.getUniqueId().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						HellblockPlayer transferPlayer = HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayer(user);
						HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(pi,
								transferPlayer);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getRemoveCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("remove")
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						if (user.equals(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to kick from your hellblock doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (!pi.getHellblockParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to kick is not in your hellblock party!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(pi, user, id);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getLeaveCommand(String namespace) {
		return new CommandAPICommand(namespace).executesPlayer((player, args) -> {
			HellblockPlayer leavingPlayer = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
			HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer);
		});
	}

	private CommandAPICommand getInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("add")
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						Player user = (Player) args.getOrDefault("player", player);
						if (user == null || !user.isOnline()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you entered is either not online or doesn't exist!");
							return;
						}
						if (user.getUniqueId().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (pi.getHellblockParty().contains(user.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to invite is already a member of your party!");
							return;
						}
						HellblockPlayer addPlayer = HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayer(user);
						HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(pi, addPlayer);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getTrustCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						Player user = (Player) args.getOrDefault("player", player);
						if (user == null || !user.isOnline()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you entered is either not online or doesn't exist!");
							return;
						}
						UUID id = user.getUniqueId();
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (pi.getHellblockParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to apply this to is already a member of your party!");
							return;
						}
						HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
						if (ti.getWhoTrusted().contains(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is already trusted on your hellblock!");
							return;
						}
						ti.addTrustPermission(player.getUniqueId());
						ti.saveHellblockPlayer();
						HellblockPlugin.getInstance().getCoopManager().addTrustAccess(pi, user.getName(), id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've given trust access to <dark_red>%s <red>on your hellblock!",
										user.getName()));
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(user,
								String.format("<red>You've been given trust access to <dark_red>%s<red>'s hellblock!",
										player.getName()));
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getUntrustCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.user")
				.withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions
						.stringCollection(collection -> HellblockPlugin.getInstance().getHellblockHandler()
								.getActivePlayers().values().stream().filter(hbPlayer -> hbPlayer.getPlayer() != null)
								.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList()))))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						if (user.equals(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to untrust doesn't exist!");
							return;
						}
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (pi.getHellblockParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to apply this to is already a member of your party!");
							return;
						}
						HellblockPlayer ti = null;
						if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
							ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
						} else {
							ti = new HellblockPlayer(id);
						}
						if (!ti.getWhoTrusted().contains(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player isn't trusted on your hellblock!");
							return;
						}
						ti.removeTrustPermission(player.getUniqueId());
						ti.saveHellblockPlayer();
						HellblockPlugin.getInstance().getCoopManager().removeTrustAccess(pi, user, id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player, String.format(
								"<red>You've revoked trust access from <dark_red>%s <red>on your hellblock!", user));
						if (Bukkit.getPlayer(user) != null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
									Bukkit.getPlayer(user),
									String.format("<red>You've lost trust access to <dark_red>%s<red>'s hellblock!",
											player.getName()));
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}
}
