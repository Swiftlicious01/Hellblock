package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

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
				.required("player",
						StringParser.stringComponent().suggestionProvider(this::suggestOnlineHellblockPlayers))
				.handler(context -> {
					final Player executor = context.sender();
					final String targetName = context.get("player");

					final UUID targetUUID = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetUUID == null || !Bukkit.getOfflinePlayer(targetUUID).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					// First run → ask for confirmation
					final UUID cachedTarget = deleteConfirmCache.getIfPresent(executor.getUniqueId());
					if (cachedTarget == null || !cachedTarget.equals(targetUUID)) {
						deleteConfirmCache.put(executor.getUniqueId(), targetUUID);
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_DELETE_CONFIRM
								.arguments(Component.text(targetName)));
						return;
					}

					// Confirmed → delete
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
									return;
								}

								final UserData targetUser = result.get();
								final HellblockData targetData = targetUser.getHellblockData();

								if (!targetData.hasHellblock()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
									return;
								}

								final UUID ownerUUID = targetData.getOwnerUUID();
								if (ownerUUID == null) {
									HellblockPlugin.getInstance().getPluginLogger()
											.severe("Hellblock owner UUID was null for player " + targetUser.getName()
													+ " (" + targetUser.getUUID()
													+ "). This indicates corrupted data.");
									throw new IllegalStateException(
											"Owner reference was null. This should never happen — please report to the developer.");
								}

								HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(ownerUUID, true, executor.getName())
										.thenRun(() -> {
											HellblockPlugin.getInstance()
													.debug("%s's hellblock has been forcefully deleted by %s."
															.formatted(targetName, executor.getName()));
											handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ISLAND_DELETED
													.arguments(Component.text(targetName)));
										}).exceptionally(ex -> {
											HellblockPlugin.getInstance().getPluginLogger()
													.warn("resetHellblock failed for " + targetUser.getName() + ": "
															+ ex.getMessage());
											return null;
										});
							});
				});
	}

	private @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestOnlineHellblockPlayers(
			@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
		final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
				.filter(user -> user.isOnline() && user.getHellblockData().hasHellblock()).map(UserData::getName)
				.toList();

		return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
	}

	@Override
	public String getFeatureID() {
		return "admin_delete";
	}
}