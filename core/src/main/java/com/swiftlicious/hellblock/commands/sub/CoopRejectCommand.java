package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

public class CoopRejectCommand extends BukkitCommandFeature<CommandSender> {

	public CoopRejectCommand(HellblockCommandManager<CommandSender> commandManager) {
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
						if (!(context.sender() instanceof Player player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(player.getUniqueId());
						if (onlineUserOpt.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final HellblockData data = onlineUserOpt.get().getHellblockData();
						if (data.getInvitations().isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						// Map UUIDs of invitations into player names
						final List<String> suggestions = data.getInvitations().keySet().stream().map(id -> {
							final Player online = Bukkit.getPlayer(id);
							if (online != null) {
								return online.getName();
							}
							final OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
							return offline.getName() != null ? offline.getName() : id.toString();
						}).toList();

						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = onlineUserOpt.get();
					final HellblockData data = user.getHellblockData();

					final String targetName = context.get("player");
					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					// Resolve UUID
					final UUID targetId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (data.getInvitations().isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
						return;
					}

					if (!data.hasInvite(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
						return;
					}

					if (data.hasInviteExpired(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRED);
						return;
					}

					// Reject invite
					HellblockPlugin.getInstance().getCoopManager().rejectInvite(targetId, user);

					// Feedback to player
					handleFeedback(context,
							MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REJECTED.arguments(Component.text(targetName)));
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_reject";
	}
}