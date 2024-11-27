package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import org.jetbrains.annotations.NotNull;

public class CoopAcceptCommand extends BukkitCommandFeature<CommandSender> {

	public CoopAcceptCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty())
								return CompletableFuture.completedFuture(Collections.emptyList());
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(user -> user != null && user.isOnline()
											&& !user.getHellblockData().hasHellblock()
											&& user.getHellblockData().getInvitations() != null
											&& user.getName().equalsIgnoreCase(player.getName()))
									.findFirst().orElse(onlineUser.get()).getHellblockData().getInvitations().keySet()
									.stream().map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
											: Bukkit.getOfflinePlayer(id).getName()))
									.collect(Collectors.toList());
							return CompletableFuture
									.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
						String user = context.get("player");
						if (user.equalsIgnoreCase(player.getName())) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						if (onlineUser.get().getHellblockData().getInvitations() == null) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
							return;
						}
						if (!onlineUser.get().getHellblockData().hasInvite(id)) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
							return;
						}
						if (onlineUser.get().getHellblockData().hasInviteExpired(id)) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRED);
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(id, onlineUser.get());
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS);
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_accept";
	}
}
