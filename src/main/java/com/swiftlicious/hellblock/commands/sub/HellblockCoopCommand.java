package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.config.HBLocale;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

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
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser ->  onlineUser.isOnline()
												&& onlineUser.getHellblockData().hasHellblock()
												&& onlineUser.getHellblockData().getOwnerUUID() != null
												&& onlineUser.getHellblockData().getOwnerUUID()
														.equals(player.getUniqueId())
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
							return;
						}
						if (onlineUser.get().getHellblockData().getTransferCooldown() > 0) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									String.format(
											"<red>You've recently transferred ownership already, you must wait for %s!",
											HellblockPlugin.getInstance().getFormattedCooldown(
													onlineUser.get().getHellblockData().getTransferCooldown())));
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
						Optional<UserData> transferPlayer = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(user.getUniqueId());
						if (transferPlayer.isEmpty()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<red>Still loading %s's data... please try again in a few seconds.",
											user.getName()));
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(onlineUser.get(),
								transferPlayer.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	private CommandAPICommand getRemoveCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("remove").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.coop").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser -> onlineUser.isOnline()
												&& onlineUser.getHellblockData().hasHellblock()
												&& onlineUser.getHellblockData().getOwnerUUID() != null
												&& onlineUser.getHellblockData().getOwnerUUID()
														.equals(player.getUniqueId())
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
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
						if (!onlineUser.get().getHellblockData().getParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to kick is not in your hellblock party!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(onlineUser.get(), user,
								id);
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	private CommandAPICommand getLeaveCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.executesPlayer((player, args) -> {
					Optional<UserData> leavingPlayer = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (leavingPlayer.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer.get());
				});
	}

	private CommandAPICommand getInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withAliases("add").withPermission(CommandPermission.NONE)
				.withPermission("hellblock.coop").withArguments(
						new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser -> onlineUser.isOnline()
												&& !onlineUser.getHellblockData().hasHellblock()
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
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
						if (onlineUser.get().getHellblockData().getParty().contains(user.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to invite is already a member of your party!");
							return;
						}
						Optional<UserData> addPlayer = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(user.getUniqueId());
						if (addPlayer.isEmpty()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<red>Still loading %s's data... please try again in a few seconds.",
											user.getName()));
							return;
						}
						if (addPlayer.get().getHellblockData().hasHellblock()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player already has their own hellblock!");
							return;
						}
						if (addPlayer.get().getHellblockData().hasInvite(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You already invited this player, wait for them to accept or decline!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().sendInvite(onlineUser.get(), addPlayer.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	private CommandAPICommand getAcceptInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty())
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user != null && user.isOnline()
												&& !user.getHellblockData().hasHellblock()
												&& user.getHellblockData().getInvitations() != null
												&& user.getName().equalsIgnoreCase(player.getName()))
										.findFirst().orElse(onlineUser.get()).getHellblockData().getInvitations()
										.keySet().stream()
										.map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
												: Bukkit.getOfflinePlayer(id).getName()))
										.collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
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
						if (onlineUser.get().getHellblockData().getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (!onlineUser.get().getHellblockData().hasInvite(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (onlineUser.get().getHellblockData().hasInviteExpired(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your invitation from this player has expired!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(id, onlineUser.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
						return;
					}
				});
	}

	private CommandAPICommand getInviteListCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withAliases("invites").executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getInvitations() == null
								|| (onlineUser.get().getHellblockData().getInvitations() != null
										&& onlineUser.get().getHellblockData().getInvitations().isEmpty())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have any invitations!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().listInvitations(onlineUser.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
						return;
					}
				});
	}

	private CommandAPICommand getRejectInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withAliases("decline").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty())
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user != null && user.isOnline()
												&& !user.getHellblockData().hasHellblock()
												&& user.getHellblockData().getInvitations() != null
												&& user.getName().equalsIgnoreCase(player.getName()))
										.findFirst().orElse(onlineUser.get()).getHellblockData().getInvitations()
										.keySet().stream()
										.map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
												: Bukkit.getOfflinePlayer(id).getName()))
										.collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
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
						if (onlineUser.get().getHellblockData().getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (!onlineUser.get().getHellblockData().hasInvite(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (onlineUser.get().getHellblockData().hasInviteExpired(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>Your invitation from this player has expired!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().rejectInvite(id, onlineUser.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								"<red>You already have a hellblock!");
						return;
					}
				});
	}

	private CommandAPICommand getCancelInviteCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withAliases("revoke").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(onlineUser -> onlineUser.isOnline()
												&& onlineUser.getHellblockData().getInvitations() != null
												&& onlineUser.getHellblockData().getInvitations().keySet()
														.contains(player.getUniqueId())
												&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
										.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
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
						HellblockPlugin.getInstance().getStorageManager().getOfflineUserData(id, HBConfig.lockData)
								.thenAccept((result) -> {
									UserData offlineUser = result.orElseThrow();
									if (offlineUser.getHellblockData().getInvitations() == null) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>This player doesn't have an invite from you!");
										return;
									}
									if (!offlineUser.getHellblockData().hasInvite(player.getUniqueId())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>This player doesn't have an invite from you!");
										return;
									}
									if (offlineUser.getHellblockData().hasInviteExpired(player.getUniqueId())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>Your invitation already expired for this player!");
										return;
									}
									offlineUser.getHellblockData().removeInvitation(player.getUniqueId());
									HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
											"<red>You've cancelled your invitation to <dark_red>" + user + "<red>!");
									if (Bukkit.getPlayer(id) != null) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												Bukkit.getPlayer(id), "<dark_red>" + player.getName()
														+ " <red> has revoked their invitation to you!");
									}
								});
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	private CommandAPICommand getTrustCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user").withArguments(
						new PlayerArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty())
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user.isOnline()
												&& !user.getHellblockData().getTrusted()
														.contains(onlineUser.get().getUUID())
												&& !user.getHellblockData().getParty()
														.contains(onlineUser.get().getUUID())
												&& !user.getName().equalsIgnoreCase(onlineUser.get().getName()))
										.map(user -> user.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
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
						if (onlineUser.get().getHellblockData().getParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to apply this to is already a member of your party!");
							return;
						}
						Optional<UserData> trustedPlayer = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(user.getUniqueId());
						if (trustedPlayer.isEmpty()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<red>Still loading %s's data... please try again in a few seconds.",
											user.getName()));
							return;
						}
						if (trustedPlayer.get().getHellblockData().getTrusted().contains(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>This player is already trusted on your hellblock!");
							return;
						}
						trustedPlayer.get().getHellblockData().addTrustPermission(player.getUniqueId());
						HellblockPlugin.getInstance().getCoopManager().addTrustAccess(onlineUser.get(), user.getName(),
								id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								String.format("<red>You've given trust access to <dark_red>%s <red>on your hellblock!",
										user.getName()));
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(user,
								String.format("<red>You've been given trust access to <dark_red>%s<red>'s hellblock!",
										player.getName()));
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}

	private CommandAPICommand getUntrustCommand(String namespace) {
		return new CommandAPICommand(namespace).withPermission(CommandPermission.NONE).withPermission("hellblock.coop")
				.withPermission(CommandPermission.NONE).withPermission("hellblock.user").withArguments(
						new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
							if (info.sender() instanceof Player player) {
								Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(player.getUniqueId());
								if (onlineUser.isEmpty())
									return Collections.emptyList();
								return HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
										.filter(user -> user.isOnline()
												&& user.getHellblockData().getTrusted()
														.contains(onlineUser.get().getUUID())
												&& !user.getHellblockData().getParty()
														.contains(onlineUser.get().getUUID())
												&& !user.getName().equalsIgnoreCase(onlineUser.get().getName()))
										.map(user -> user.getName()).collect(Collectors.toList());
							} else {
								return Collections.emptyList();
							}
						})))
				.executesPlayer((player, args) -> {
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Not_Owner_Of_Hellblock);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									HBLocale.MSG_Hellblock_Is_Abandoned);
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
						if (onlineUser.get().getHellblockData().getParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The player you're trying to apply this to is already a member of your party!");
							return;
						}
						HellblockPlugin.getInstance().getStorageManager().getOfflineUserData(id, HBConfig.lockData)
								.thenAccept((result) -> {
									UserData offlineUser = result.orElseThrow();
									if (!offlineUser.getHellblockData().getTrusted().contains(player.getUniqueId())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												player, "<red>This player isn't trusted on your hellblock!");
										return;
									}
									offlineUser.getHellblockData().removeTrustPermission(player.getUniqueId());
									HellblockPlugin.getInstance().getCoopManager().removeTrustAccess(onlineUser.get(),
											user, id);
									HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
											String.format(
													"<red>You've revoked trust access from <dark_red>%s <red>on your hellblock!",
													user));
									if (offlineUser.isOnline()) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(
												Bukkit.getPlayer(offlineUser.getUUID()),
												String.format(
														"<red>You've lost trust access to <dark_red>%s<red>'s hellblock!",
														offlineUser.getName()));
									}
								});
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
								HBLocale.MSG_Hellblock_Not_Found);
						return;
					}
				});
	}
}
