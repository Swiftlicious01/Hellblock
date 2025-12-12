package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

public class AdminRestoreCommand extends BukkitCommandFeature<CommandSender> {

	public AdminRestoreCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final Set<UUID> allKnownUUIDs = plugin.getStorageManager().getDataSource().getUniqueUsers();

					final List<String> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get)
							.filter(user -> user.getHellblockData().hasHellblock()
									&& user.getHellblockData().isAbandoned())
							.map(UserData::getName).filter(Objects::nonNull).toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final String targetName = context.get("player");
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

					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenAccept(result -> {
						if (result.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return;
						}

						final UserData offlineUser = result.get();
						final HellblockData data = offlineUser.getHellblockData();

						if (!data.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return;
						}

						if (!data.isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_RESTORE_NOT_ABANDONED,
									AdventureHelper.miniMessageToComponent(targetName));
							return;
						}

						final UUID ownerUUID = data.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + targetName
									+ " (" + targetId + "). This indicates corrupted data.");
							throw new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer.");
						}

						plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false)
								.thenAccept(ownerOpt -> {
									if (ownerOpt.isEmpty()) {
										final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(username != null ? username
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())));
										return;
									}

									final UserData ownerData = ownerOpt.get();

									// Perform restoration
									plugin.getProtectionManager().restoreIsland(ownerData.getHellblockData());

									// Save changes
									plugin.getStorageManager().saveUserData(ownerData,
											plugin.getConfigManager().lockData());

									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_RESTORE_SUCCESS,
											AdventureHelper.miniMessageToComponent(targetName));
								});
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn(
								"Failed to restore abandoned hellblock for " + targetName + ": " + ex.getMessage());
						return null;
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_restore";
	}
}