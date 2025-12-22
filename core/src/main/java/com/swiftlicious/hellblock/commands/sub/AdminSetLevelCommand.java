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

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class AdminSetLevelCommand extends BukkitCommandFeature<CommandSender> {

	public AdminSetLevelCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				})).required("level", FloatParser.floatParser(0f, Float.MAX_VALUE)).handler(context -> {
					final String targetName = context.get("player");
					final float newLevel = context.get("level");

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
							return CompletableFuture.completedFuture(false);
						}

						final UserData targetUserData = targetOpt.get();
						final HellblockData data = targetUserData.getHellblockData();

						if (!data.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
							return CompletableFuture.completedFuture(false);
						}

						final UUID ownerUUID = data.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUserData.getName()
											+ " (" + targetUserData.getUUID() + "). This indicates corrupted data.");
							return CompletableFuture.failedFuture(new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer."));
						}

						return plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true)
								.thenCompose(ownerOpt -> {
									if (ownerOpt.isEmpty()) {
										final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(username != null ? username
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())));
										return CompletableFuture.completedFuture(false);
									}

									final UserData ownerData = ownerOpt.get();

									// Update level
									ownerData.getHellblockData().setIslandLevel(newLevel);

									// Feedback
									// arg:0 target name
									// arg:1 new level
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_SET_LEVEL,
											AdventureHelper.miniMessageToComponent(targetUserData.getName()),
											AdventureHelper.miniMessageToComponent(String.valueOf(newLevel)));

									// Save changes
									return plugin.getStorageManager().saveUserData(ownerData, true);
								}).handle((result, ex) -> {
									if (ex != null) {
										plugin.getPluginLogger()
												.warn("Admin set level command failed (Could not read owner "
														+ ownerUUID + "'s data): " + ex.getMessage());
									}
									return false;
								}).thenCompose(
										v -> plugin.getStorageManager().unlockUserData(ownerUUID).thenApply(x -> true));
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("Admin set level command failed (Could not read target "
								+ targetName + "'s data): " + ex.getMessage());
						return false;
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_setlevel";
	}
}