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
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class AdminActivityCommand extends BukkitCommandFeature<CommandSender> {

	public AdminActivityCommand(HellblockCommandManager<CommandSender> commandManager) {
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

					// Get all known user UUIDs (cached, not full database)
					final Set<UUID> allKnownUUIDs = new HashSet<>(
							plugin.getStorageManager().getDataSource().getUniqueUsers());
					final String lowerInput = input.input().toLowerCase(Locale.ROOT);

					// Generate filtered suggestions
					final List<Suggestion> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
							.sorted(String.CASE_INSENSITIVE_ORDER).limit(64) // safety limit
							.map(Suggestion::suggestion).toList();

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

					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenAccept(targetOpt -> {
						if (targetOpt.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return;
						}

						final UserData targetUser = targetOpt.get();
						final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetId);
						final long now = System.currentTimeMillis();

						final long lastActivity = targetUser.getHellblockData().getLastIslandActivity();
						final String duration = (lastActivity > 0)
								? plugin.getCooldownManager().getFormattedCooldown(now - lastActivity)
								: plugin.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_NEVER.build().key());

						plugin.debug("Activity check by " + context.sender().getName() + " → " + targetUser.getName()
								+ " (last active " + duration + ")");

						if (offlineTarget.isOnline()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ACTIVITY_ONLINE,
									AdventureHelper.miniMessageToComponent(targetUser.getName()));
						} else {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ACTIVITY_LAST_SEEN,
									AdventureHelper.miniMessageToComponent(targetUser.getName()),
									AdventureHelper.miniMessageToComponent(duration));
						}
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("Admin activity command failed (Could not read target "
								+ targetName + "'s data): " + ex.getMessage());
						return null;
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_activity";
	}
}