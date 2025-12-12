package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class AdminDeleteCommand extends BukkitCommandFeature<CommandSender> {

	// confirmation valid for 30 seconds
	private final Cache<UUID, UUID> deleteConfirmCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
			.build();

	public AdminDeleteCommand(HellblockCommandManager<CommandSender> commandManager) {
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
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull).toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final Player executor = context.sender();
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

					// First run → ask for confirmation
					final UUID cachedTarget = deleteConfirmCache.getIfPresent(executor.getUniqueId());
					if (cachedTarget == null || !cachedTarget.equals(targetId)) {
						deleteConfirmCache.put(executor.getUniqueId(), targetId);
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_DELETE_CONFIRM,
								AdventureHelper.miniMessageToComponent(targetName));
						return;
					}

					// Confirmed → delete
					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenAccept(result -> {
						if (result.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return;
						}

						final UserData targetUserData = result.get();
						final HellblockData targetData = targetUserData.getHellblockData();

						if (!targetData.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
							return;
						}

						final UUID ownerUUID = targetData.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUserData.getName()
											+ " (" + targetUserData.getUUID() + "). This indicates corrupted data.");
							throw new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer.");
						}

						plugin.getHellblockHandler().resetHellblock(ownerUUID, true, executor.getName()).thenRun(() -> {
							// Save changes
							plugin.getStorageManager().saveUserData(targetUserData,
									plugin.getConfigManager().lockData());
							plugin.debug("%s's hellblock has been forcefully deleted by %s.".formatted(targetName,
									executor.getName()));
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ISLAND_DELETED,
									AdventureHelper.miniMessageToComponent(targetName));
						}).exceptionally(ex -> {
							plugin.getPluginLogger().warn(
									"resetHellblock failed for " + targetUserData.getName() + ": " + ex.getMessage());
							return null;
						});
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_delete";
	}
}