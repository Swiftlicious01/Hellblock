package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
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

public class CoopOwnerCommand extends BukkitCommandFeature<CommandSender> {

	public CoopOwnerCommand(HellblockCommandManager<CommandSender> commandManager) {
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

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// No data loaded — show nothing
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					UserData userData = userOpt.get();
					HellblockData data = userData.getHellblockData();

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

					if (data.getTransferCooldown() > 0) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					// Suggest online players who belong to the sender’s hellblock
					final List<String> suggestions = data.getPartyMembers().stream()
							.map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).filter(Objects::nonNull).toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					if (!plugin.getConfigManager().transferIslands()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
						return;
					}

					final UserData currentOwner = onlineUserOpt.get();
					final HellblockData data = currentOwner.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!data.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					if (data.getTransferCooldown() > 0) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN,
								AdventureHelper.miniMessageToComponent(
										plugin.getCooldownManager().getFormattedCooldown(data.getTransferCooldown())));
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

					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					if (!data.getPartyMembers().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY);
						return;
					}
					// Load target user data async
					plugin.getStorageManager()
							.getCachedUserDataWithFallback(targetId, plugin.getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
											AdventureHelper.miniMessageToComponent(targetName));
									return;
								}

								final UserData targetUser = result.get();

								try {
									// Perform ownership transfer synchronously
									plugin.getCoopManager().transferOwnershipOfHellblock(currentOwner, targetUser,
											false);
								} catch (Exception ex) {
									plugin.getPluginLogger().warn("transferOwnershipOfHellblock failed from "
											+ player.getName() + " to " + targetName + ": " + ex.getMessage());
									return;
								}
							}).exceptionally(ex -> {
								plugin.getPluginLogger()
										.warn("getCachedUserDataWithFallback failed for owner transfership of "
												+ targetName + ": " + ex.getMessage());
								return null;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_owner";
	}
}