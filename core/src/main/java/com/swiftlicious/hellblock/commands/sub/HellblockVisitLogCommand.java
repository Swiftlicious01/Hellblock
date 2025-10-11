package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class HellblockVisitLogCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockVisitLogCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {

		return builder.senderType(Player.class)
				.optional("page", IntegerParser.integerComponent().suggestionProvider((context, input) -> {
					// Suggest up to N pages based on total visits
					Player player = (Player) context.sender();

					Optional<UserData> userOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (userOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					HellblockData data = userOpt.get().getHellblockData();
					UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					return HellblockPlugin.getInstance().getVisitManager().getIslandVisitLog(ownerUUID)
							.thenApply(log -> {
								int pages = (int) Math.ceil(log.size() / 10.0);
								return IntStream.rangeClosed(1, pages)
										.mapToObj(i -> Suggestion.suggestion(String.valueOf(i))).toList();
							});
				})).handler(context -> {
					final Player player = context.sender();
					final int page = (int) context.optional("page").orElse(1); // Ensure page ≥ 1

					final HellblockPlugin plugin = HellblockPlugin.getInstance();

					final Optional<UserData> onlineUserOpt = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData onlineUser = onlineUserOpt.get();
					final HellblockData hellblockData = onlineUser.getHellblockData();

					if (!hellblockData.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = hellblockData.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					plugin.getStorageManager().getOfflineUserData(ownerUUID, plugin.getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
											.orElse("???");
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(username)));
									return;
								}

								final HellblockData offlineData = result.get().getHellblockData();

								if (offlineData.isAbandoned()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
									return;
								}

								plugin.getVisitManager().getIslandVisitLog(ownerUUID).thenAccept(records -> {
									if (records.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_LOG_EMPTY);
										return;
									}

									// Sort by newest -> oldest, then by name (numbers before letters)
									records.sort((r1, r2) -> {
										int cmp = Long.compare(r2.getTimestamp(), r1.getTimestamp());
										if (cmp != 0)
											return cmp;

										String n1 = Optional
												.ofNullable(Bukkit.getOfflinePlayer(r1.getVisitorId()).getName())
												.orElse(r1.getVisitorId().toString());
										String n2 = Optional
												.ofNullable(Bukkit.getOfflinePlayer(r2.getVisitorId()).getName())
												.orElse(r2.getVisitorId().toString());
										return n1.compareToIgnoreCase(n2);
									});

									final int entriesPerPage = 10;
									final int totalPages = (int) Math.ceil((double) records.size() / entriesPerPage);

									if (page < 1 || page > totalPages) {
										handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT
												.arguments(Component.text(page), Component.text(totalPages)));
										return;
									}

									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_LOG_HEADER
											.arguments(Component.text(page), Component.text(totalPages)));

									int start = (page - 1) * entriesPerPage;
									int end = Math.min(start + entriesPerPage, records.size());
									long now = System.currentTimeMillis();

									for (int i = start; i < end; i++) {
										VisitManager.VisitRecord record = records.get(i);
										UUID visitorId = record.getVisitorId();
										long timestamp = record.getTimestamp();

										String name = Optional.ofNullable(Bukkit.getOfflinePlayer(visitorId).getName())
												.orElse(visitorId.toString());

										long secondsAgo = (now - timestamp) / 1000;
										String formattedAgo = plugin.getFormattedCooldown(secondsAgo);

										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_LOG_ENTRY
												.arguments(Component.text(name), Component.text(formattedAgo)));
									}
								});
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visit_log";
	}
}