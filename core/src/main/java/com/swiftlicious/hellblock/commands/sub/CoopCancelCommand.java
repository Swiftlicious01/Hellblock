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

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class CoopCancelCommand extends BukkitCommandFeature<CommandSender> {

	public CoopCancelCommand(HellblockCommandManager<CommandSender> commandManager) {
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
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(onlineUser -> onlineUser.isOnline()
											&& onlineUser.getHellblockData().getInvitations() != null
											&& onlineUser.getHellblockData().getInvitations().keySet()
													.contains(player.getUniqueId())
											&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
									.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
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
					if (onlineUser.get().getHellblockData().hasHellblock()) {
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
						HellblockPlugin.getInstance().getStorageManager()
								.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
								.thenAccept((result) -> {
									if (result.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
												.arguments(Component.text(user)));
										return;
									}
									UserData offlineUser = result.get();
									if (offlineUser.getHellblockData().getInvitations() == null) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
										return;
									}
									if (!offlineUser.getHellblockData().hasInvite(player.getUniqueId())) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
										return;
									}
									if (offlineUser.getHellblockData().hasInviteExpired(player.getUniqueId())) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRED);
										return;
									}
									offlineUser.getHellblockData().removeInvitation(player.getUniqueId());
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_CANCELLED
											.arguments(Component.text(user)));
									if (Bukkit.getPlayer(id) != null) {
										handleFeedback(Bukkit.getPlayer(id),
												MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REVOKED
														.arguments(Component.text(player.getName())));
									}
								});
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_cancel";
	}
}