package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
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
		return getHelpCommand().withSubcommands(getOwnerCommand("setowner"), getRemoveCommand("kick"),
				getInviteCommand("invite"), getLeaveCommand("leave"), getTrustCommand("trust"),
				getUntrustCommand("untrust"), getAcceptInviteCommand("accept"), getRejectInviteCommand("reject"),
				getInviteListCommand("invitations"), getCancelInviteCommand("cancel"));
	}

	private CommandAPICommand getHelpCommand() {
		CommandAPICommand command = new CommandAPICommand("hellcoop");
		if (command.getArguments().isEmpty()) {
			command.withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
					.executesPlayer((player, args) -> {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
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
		return command;
	}

	private CommandAPICommand getOwnerCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("transferowner").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.coop").withArguments(
						new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock()
												&& hbPlayer.getHellblockOwner() != null
												&& hbPlayer.getHellblockOwner().equals(player.getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
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
						HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(pi, transferPlayer);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getRemoveCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("remove").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.coop").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && hbPlayer.hasHellblock()
												&& hbPlayer.getHellblockOwner() != null
												&& hbPlayer.getHellblockOwner().equals(player.getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to kick from your hellblock doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
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
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.executesPlayer((player, args) -> {
					HellblockPlayer leavingPlayer = HellblockPlugin.getInstance().getHellblockHandler()
							.getActivePlayer(player);
					HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer);
				});
	}

	private CommandAPICommand getInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("add").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.coop").withArguments(
						new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && !hbPlayer.hasHellblock()
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
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
						if (addPlayer.hasHellblock()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player already has their own hellblock!");
							return;
						}
						if (addPlayer.hasInvite(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You already invited this player, wait for them to accept or decline!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().sendInvite(pi, addPlayer);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getAcceptInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && !hbPlayer.hasHellblock()
												&& hbPlayer.getInvitations() != null
												&& hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.findFirst()
										.orElse(HellblockPlugin.getInstance().getHellblockHandler()
												.getActivePlayer(player))
										.getInvitations().keySet().stream()
										.map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
												: Bukkit.getOfflinePlayer(id).getName()))
										.collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						String user = (String) args.getOrDefault("player", player);
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to accept an invite from doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to accept an invite from doesn't exist!");
							return;
						}
						if (pi.getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (!pi.hasInvite(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (pi.hasInviteExpired(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your invitation from this player has expired!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(id, pi);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
					}
				});
	}

	private CommandAPICommand getInviteListCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						if (pi.getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have any invitations!");
							return;
						}
						if (pi.getInvitations() != null && pi.getInvitations().isEmpty()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have any invitations!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().listInvitations(pi);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
					}
				});
	}

	private CommandAPICommand getRejectInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withAliases("decline").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null && !hbPlayer.hasHellblock()
												&& hbPlayer.getInvitations() != null
												&& hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.findFirst()
										.orElse(HellblockPlugin.getInstance().getHellblockHandler()
												.getActivePlayer(player))
										.getInvitations().keySet().stream()
										.map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
												: Bukkit.getOfflinePlayer(id).getName()))
										.collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (!pi.hasHellblock()) {
						String user = (String) args.getOrDefault("player", player);
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to decline an invite from doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to decline an invite from doesn't exist!");
							return;
						}
						if (pi.getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (!pi.hasInvite(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (pi.hasInviteExpired(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your invitation from this player has expired!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().rejectInvite(id, pi);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
					}
				});
	}

	private CommandAPICommand getCancelInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withAliases("revoke").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null
												&& hbPlayer.getInvitations() != null
												&& hbPlayer.getInvitations().keySet().contains(player.getUniqueId())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						String user = (String) args.getOrDefault("player", player);
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to cancel an invite from doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to cancel an invite from doesn't exist!");
							return;
						}
						HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
						if (ti.getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player doesn't have an invite from you!");
							return;
						}
						if (!ti.hasInvite(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player doesn't have an invite from you!");
							return;
						}
						if (ti.hasInviteExpired(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your invitation already expired for this player!");
							return;
						}
						ti.removeInvitation(player.getUniqueId());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You've cancelled your invitation to <dark_red>" + user + "<red>!");
						if (Bukkit.getPlayer(id) != null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
									Bukkit.getPlayer(id),
									"<dark_red>" + player.getName() + " <red> has revoked their invitation to you!");
						}
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You don't have a hellblock!");
					}
				});
	}

	private CommandAPICommand getTrustCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user").withArguments(
						new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(player);
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null
												&& !pi.getWhoTrusted().contains(hbPlayer.getUUID())
												&& !pi.getHellblockParty().contains(hbPlayer.getUUID())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
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
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler()
										.getActivePlayer(player);
								return HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().values()
										.stream()
										.filter(hbPlayer -> hbPlayer.getPlayer() != null
												&& pi.getWhoTrusted().contains(hbPlayer.getUUID())
												&& !pi.getHellblockParty().contains(hbPlayer.getUUID())
												&& !hbPlayer.getPlayer().getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(player);
					if (pi.hasHellblock()) {
						if (pi.isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
							return;
						}
						if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Only the owner of the hellblock island can do this!");
							return;
						}
						String user = (String) args.getOrDefault("player", player);
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to untrust doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
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
						HellblockPlayer ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
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
