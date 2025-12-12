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
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VisitManager;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockVisitLogCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockVisitLogCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {

		return builder.senderType(Player.class)
				.optional("page", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					HellblockData data = userOpt.get().getHellblockData();
					UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.isAbandoned()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					// Suggest all pages, or maybe filter by input
					String userInput = input.peekString();

					return plugin.getVisitManager().getIslandVisitLog(ownerUUID).thenApply(log -> {
						int pages = (int) Math.ceil(log.size() / 10.0);
						return IntStream.rangeClosed(1, pages).mapToObj(String::valueOf)
								.filter(s -> userInput.isEmpty() || s.startsWith(userInput)).map(Suggestion::suggestion)
								.toList();
					});
				})).handler(context -> {
					final Player player = context.sender();

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

					plugin.getStorageManager()
							.getCachedUserDataWithFallback(ownerUUID, plugin.getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
											.orElse(plugin.getTranslationManager().miniMessageTranslation(
													MessageConstants.FORMAT_UNKNOWN.build().key()));
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
											AdventureHelper.miniMessageToComponent(username));
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

									int page;
									// Extract the page argument as a string (if present), or default to "1"
									String pageInput = context.<String>optional("page").orElse("1");
									try {
										page = Integer.parseInt(pageInput);
									} catch (NumberFormatException e) {
										handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
												AdventureHelper.miniMessageToComponent(pageInput),
												AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
										return;
									}

									if (page < 1 || page > totalPages) {
										handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
												AdventureHelper.miniMessageToComponent(String.valueOf(page)),
												AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
										return;
									}

									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_LOG_HEADER,
											AdventureHelper.miniMessageToComponent(String.valueOf(page)),
											AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));

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
										String formattedAgo = plugin.getCooldownManager()
												.getFormattedCooldown(secondsAgo);

										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_LOG_ENTRY,
												AdventureHelper.miniMessageToComponent(name),
												AdventureHelper.miniMessageToComponent(formattedAgo));
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