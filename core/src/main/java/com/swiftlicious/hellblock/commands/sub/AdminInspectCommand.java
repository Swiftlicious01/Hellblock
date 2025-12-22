package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

public class AdminInspectCommand extends BukkitCommandFeature<CommandSender> {

	public AdminInspectCommand(HellblockCommandManager<CommandSender> commandManager) {
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

					final String lowerInput = input.input().toLowerCase(Locale.ROOT);
					final Set<UUID> allKnownUUIDs = new HashSet<>(
							plugin.getStorageManager().getDataSource().getUniqueUsers());

					final List<Suggestion> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
							.sorted(String.CASE_INSENSITIVE_ORDER).limit(64).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
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

					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenCompose(targetOpt -> {
						if (targetOpt.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return CompletableFuture.completedFuture(null);
						}

						final UserData targetUserData = targetOpt.get();
						final HellblockData data = targetUserData.getHellblockData();

						if (!data.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
							return CompletableFuture.completedFuture(null);
						}

						final UUID ownerUUID = data.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUserData.getName()
											+ " (" + targetUserData.getUUID() + "). This indicates corrupted data.");
							return CompletableFuture.failedFuture(new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer."));
						}

						return plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false)
								.thenCompose(ownerOpt -> {
									if (ownerOpt.isEmpty()) {
										final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(username != null ? username
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())));
										return CompletableFuture.completedFuture(null);
									}

									final UserData ownerData = ownerOpt.get();
									final HellblockData hellblockData = ownerData.getHellblockData();
									// Collect members
									String members = hellblockData.getPartyMembers().stream().map(uuid -> {
										final OfflinePlayer member = Bukkit.getOfflinePlayer(uuid);
										return member.hasPlayedBefore() && member.getName() != null ? member.getName()
												: uuid.toString();
									}).collect(Collectors.joining(", "));

									if (members.isEmpty()) {
										members = plugin.getTranslationManager()
												.miniMessageTranslation(MessageConstants.FORMAT_NONE.build().key());
									}

									// Prepare feedback message
									// arg:0 target name
									// arg:1 owner uuid
									// arg:2 level
									// arg:3 lock status
									// arg:4 abandoned
									// arg:5 members
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_INFO,
											AdventureHelper.miniMessageToComponent(targetUserData.getName()),
											AdventureHelper.miniMessageToComponent(ownerUUID.toString()),
											AdventureHelper.miniMessageToComponent(
													String.valueOf(hellblockData.getIslandLevel())),
											AdventureHelper.miniMessageToComponent(hellblockData.isLocked()
													? plugin.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_LOCKED.build().key())
													: plugin.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_UNLOCKED.build().key())),
											AdventureHelper.miniMessageToComponent(hellblockData.isAbandoned()
													? plugin.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_YES.build().key())
													: plugin.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_NO.build().key())),
											AdventureHelper.miniMessageToComponent(members));
									return CompletableFuture.completedFuture(null);
								}).exceptionally(ex -> {
									plugin.getPluginLogger().warn("Admin inspect command failed (Could not read owner "
											+ ownerUUID + "'s data): " + ex.getMessage());
									return null;
								});
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("Admin inspect command failed (Could not read target "
								+ targetName + "'s data): " + ex.getMessage());
						return null;
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_inspect";
	}
}