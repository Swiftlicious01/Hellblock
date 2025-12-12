package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

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
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final Optional<UserData> onlineUser = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUser.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					UserData userData = onlineUser.get();
					HellblockData data = userData.getHellblockData();

					// Suggestions: list names from invitation senders
					final long now = System.currentTimeMillis();

					final List<String> suggestions = data.getInvitations().entrySet().stream()
							.filter(entry -> entry.getValue() != null && entry.getValue() > now).map(Map.Entry::getKey)
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).sorted(Comparator.comparingLong((UserData u) -> {
								long activity = u.getHellblockData().getLastIslandActivity();
								return activity > 0 ? activity : Long.MIN_VALUE; // push unknowns to end
							}).reversed()).map(UserData::getName).filter(Objects::nonNull).distinct().toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData playerToAdd = userOpt.get();
					final HellblockData data = playerToAdd.getHellblockData();

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

					UUID targetId;

					Player onlinePlayer = Bukkit.getPlayer(targetName);
					if (onlinePlayer != null) {
						targetId = onlinePlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(targetName);
						if (fetchedId.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						targetId = fetchedId.get();
					}

					if (!Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
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

					// Accept invite
					plugin.getCoopManager().addMemberToHellblock(targetId, playerToAdd);
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_accept";
	}
}