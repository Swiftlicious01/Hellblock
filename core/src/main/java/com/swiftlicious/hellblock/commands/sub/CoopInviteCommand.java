package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

public class CoopInviteCommand extends BukkitCommandFeature<CommandSender> {

	public CoopInviteCommand(HellblockCommandManager<CommandSender> commandManager) {
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
					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(senderId);

					if (userOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();

					if (!data.hasHellblock()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (!data.isOwner(ownerUUID)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.isAbandoned()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.getPartyMembers().size() >= plugin.getCoopManager().getMaxPartySize(userData)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					long now = System.currentTimeMillis();

					// Collect UUIDs that already have a valid invite from sender
					Set<UUID> alreadyInvited = plugin.getStorageManager().getDataSource().getUniqueUsers().stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(target -> {
								Map<UUID, Long> invites = target.getHellblockData().getInvitations();
								Long expiry = invites.get(senderId);
								return expiry != null && expiry > now;
							}).map(UserData::getUUID).collect(Collectors.toSet());

					Set<UUID> excludedUUIDs = new HashSet<>();
					excludedUUIDs.add(senderId); // Don't suggest yourself
					excludedUUIDs.addAll(data.getPartyMembers());
					excludedUUIDs.addAll(data.getTrustedMembers());
					excludedUUIDs.addAll(data.getBannedMembers());
					excludedUUIDs.addAll(alreadyInvited); // Already invited by sender

					// Suggest any known players not excluded
					List<Suggestion> suggestions = plugin.getStorageManager().getDataSource().getUniqueUsers().stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> !excludedUUIDs.contains(user.getUUID()))
							.sorted(Comparator.comparingLong((UserData u) -> {
								long activity = u.getHellblockData().getLastIslandActivity();
								return activity > 0 ? activity : Long.MIN_VALUE; // push unknowns to end
							}).reversed()).map(UserData::getName).filter(Objects::nonNull).distinct()
							.map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> senderOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (senderOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData sender = senderOpt.get();
					final HellblockData senderData = sender.getHellblockData();

					// Must own a Hellblock
					if (!senderData.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TUTORIAL);
						return;
					}

					// Must be the owner
					final UUID ownerUUID = senderData.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!senderData.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (senderData.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					// Target
					final String targetName = context.get("player");

					// Offline UUID resolution
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

					// Prevent self-invite
					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					// Check party
					if (senderData.getPartyMembers().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY,
								AdventureHelper.miniMessageToComponent(targetName));
						return;
					}

					if (senderData.getPartyMembers().size() >= plugin.getCoopManager().getMaxPartySize(sender)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL);
						return;
					}

					// Async load for offline target
					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, true).thenCompose(targetOpt -> {
						if (targetOpt.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return CompletableFuture.completedFuture(null);
						}

						final UserData targetUserData = targetOpt.get();

						if (targetUserData.getHellblockData().hasInvite(player.getUniqueId())) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS);
							return CompletableFuture.completedFuture(null);
						}

						return plugin.getCoopManager().sendInvite(sender, targetUserData).handle((inviteSent, ex) -> {
							if (ex != null) {
								plugin.getPluginLogger().warn("sendInvite failed for " + targetName, ex);
							}
							return inviteSent;
						}).thenCompose(__ -> plugin.getStorageManager().unlockUserData(targetId));
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("Coop invite command failed (could not read target " + targetName
								+ "'s data): " + ex.getMessage());
						return null;
					});
				});

	}

	@Override
	public String getFeatureID() {
		return "coop_invite";
	}
}