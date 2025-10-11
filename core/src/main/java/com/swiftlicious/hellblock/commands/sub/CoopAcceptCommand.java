package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
						if (!(context.sender() instanceof Player player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}
						final Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(player.getUniqueId());
						if (onlineUser.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}
						// Suggestions: list names from invitation senders
						final Set<UUID> invitations = !onlineUser.get().getHellblockData().getInvitations().isEmpty()
								? onlineUser.get().getHellblockData().getInvitations().keySet()
								: Collections.emptySet();
						final List<String> suggestions = invitations.stream().map(id -> {
							final Player online = Bukkit.getPlayer(id);
							if (online != null) {
								return online.getName();
							}
							final OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
							return offline != null ? offline.getName() : null;
						}).filter(Objects::nonNull).toList();
						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
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
					final HellblockData data = user.getHellblockData();

					// Must not already have a Hellblock
					if (data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS);
						return;
					}

					final String targetName = context.get("player");
					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					final UUID inviterId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (inviterId == null || !Bukkit.getOfflinePlayer(inviterId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (!data.hasInvite(inviterId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND);
						return;
					}

					if (data.hasInviteExpired(inviterId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXPIRED);
						return;
					}

					// Accept invite
					HellblockPlugin.getInstance().getCoopManager().addMemberToHellblock(inviterId, user);
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_accept";
	}
}