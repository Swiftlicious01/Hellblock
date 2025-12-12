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
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class CoopCancelCommand extends BukkitCommandFeature<CommandSender> {

	public CoopCancelCommand(HellblockCommandManager<CommandSender> commandManager) {
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

					UUID senderId = player.getUniqueId();
					final Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(senderId);

					if (onlineUser.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					long now = System.currentTimeMillis();

					List<Suggestion> suggestions = plugin.getStorageManager().getDataSource().getUniqueUsers().stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(userData -> {
								Map<UUID, Long> invites = userData.getHellblockData().getInvitations();
								Long expiry = invites.get(senderId);
								return expiry != null && expiry > now;
							}).filter(userData -> !userData.getUUID().equals(senderId))
							.sorted(Comparator.comparingLong((UserData u) -> {
								long activity = u.getHellblockData().getLastIslandActivity();
								return activity > 0 ? activity : Long.MIN_VALUE; // push unknowns to end
							}).reversed()).map(UserData::getName).filter(Objects::nonNull).map(Suggestion::suggestion)
							.toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

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

					// Async fetch for offline data
					plugin.getStorageManager()
							.getCachedUserDataWithFallback(targetId, plugin.getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
											AdventureHelper.miniMessageToComponent(targetName));
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
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_CANCELLED,
										AdventureHelper.miniMessageToComponent(targetName));

								// Feedback to target if online
								final Player targetOnline = Bukkit.getPlayer(targetId);
								if (targetOnline != null) {
									handleFeedback(targetOnline, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REVOKED,
											AdventureHelper.miniMessageToComponent(player.getName()));
								}
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_cancel";
	}
}