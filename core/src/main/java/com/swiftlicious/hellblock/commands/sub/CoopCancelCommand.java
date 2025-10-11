package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

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
							return CompletableFuture.completedFuture(
									HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
											.filter(onlineUser -> onlineUser.isOnline()
													&& onlineUser.getHellblockData().getInvitations().keySet()
															.contains(player.getUniqueId())
													&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
											.map(hbPlayer -> hbPlayer.getPlayer().getName()).map(Suggestion::suggestion)
											.toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> userOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = userOpt.get();
					if (!user.getHellblockData().hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final String targetName = context.get("player");
					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					final UUID targetId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					// Async fetch for offline data
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetId, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
									return;
								}

								final UserData targetUser = result.get();
								final HellblockData targetData = targetUser.getHellblockData();

								if (!targetData.hasInvite(player.getUniqueId())) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
									return;
								}

								if (targetData.hasInviteExpired(player.getUniqueId())) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRED);
									return;
								}

								// Cancel invite
								targetData.removeInvitation(player.getUniqueId());

								// Feedback to executor
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_CANCELLED
										.arguments(Component.text(targetName)));

								// Feedback to target if online
								final Player targetOnline = Bukkit.getPlayer(targetId);
								if (targetOnline != null) {
									handleFeedback(targetOnline, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REVOKED
											.arguments(Component.text(player.getName())));
								}
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_cancel";
	}
}