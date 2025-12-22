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
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class AdminTransferCommand extends BukkitCommandFeature<CommandSender> {

	public AdminTransferCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("currentOwner", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final String lowerInput = input.input().toLowerCase(Locale.ROOT);
					final Set<UUID> allKnownUUIDs = new HashSet<>(
							plugin.getStorageManager().getDataSource().getUniqueUsers());

					final List<Suggestion> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get)
							.filter(user -> user.getHellblockData().hasHellblock()
									&& !user.getHellblockData().isAbandoned()
									&& user.getHellblockData().isOwner(user.getUUID()))
							.map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
							.sorted(String.CASE_INSENSITIVE_ORDER).limit(64).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).required("newOwner", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final String currentOwnerName = context.getOrDefault("currentOwner", null);
					if (currentOwnerName == null) {
						// Can't get party if currentOwner isn't filled yet
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					UUID currentOwnerId;

					Player onlinePlayer = Bukkit.getPlayer(currentOwnerName);
					if (onlinePlayer != null) {
						currentOwnerId = onlinePlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(currentOwnerName);
						if (fetchedId.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}
						currentOwnerId = fetchedId.get();
					}

					if (!Bukkit.getOfflinePlayer(currentOwnerId).hasPlayedBefore()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final Optional<UserData> currentOwnerOpt = plugin.getStorageManager()
							.getCachedUserData(currentOwnerId);

					if (currentOwnerOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final HellblockData data = currentOwnerOpt.get().getHellblockData();
					final Set<UUID> partyMembers = data.getPartyMembers(); // Only members, not owner

					final String lowerInput = input.input().toLowerCase(Locale.ROOT);

					// Add all cached users who are in the party
					final List<Suggestion> suggestions = partyMembers.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
							.sorted(String.CASE_INSENSITIVE_ORDER).limit(partyMembers.size())
							.map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final String currentName = context.get("currentOwner");
					final String newName = context.get("newOwner");

					// Resolve UUIDs
					UUID currentId;
					UUID newId;

					Player currentPlayer = Bukkit.getPlayer(currentName);
					if (currentPlayer != null) {
						currentId = currentPlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(currentName);
						if (fetchedId.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						currentId = fetchedId.get();
					}

					Player newPlayer = Bukkit.getPlayer(newName);
					if (newPlayer != null) {
						newId = newPlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(newName);
						if (fetchedId.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						newId = fetchedId.get();
					}

					if (!Bukkit.getOfflinePlayer(currentId).hasPlayedBefore()
							|| !Bukkit.getOfflinePlayer(newId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (currentId.equals(newId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND,
								AdventureHelper.miniMessageToComponent(currentName));
						return;
					}

					// Load current owner
					plugin.getStorageManager().getCachedUserDataWithFallback(currentId, true)
							.thenCompose(currentOpt -> {
								if (currentOpt.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
											AdventureHelper.miniMessageToComponent(currentName));
									return CompletableFuture.completedFuture(null);
								}

								// Load new owner
								return plugin.getStorageManager().getCachedUserDataWithFallback(newId, true)
										.thenCompose(newOpt -> {
											if (newOpt.isEmpty()) {
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
														AdventureHelper.miniMessageToComponent(newName));
												return CompletableFuture.completedFuture(null);
											}

											final UserData currentOwner = currentOpt.get();
											final UserData newOwner = newOpt.get();

											if (!currentOwner.getHellblockData().hasHellblock()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
												return CompletableFuture.completedFuture(null);
											}

											if (currentOwner.getHellblockData().isAbandoned()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
												return CompletableFuture.completedFuture(null);
											}

											if (!currentOwner.getHellblockData().getPartyMembers().contains(newId)) {
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY);
												return CompletableFuture.completedFuture(null);
											}

											try {
												// Perform transfer
												return plugin.getCoopManager()
														.transferOwnershipOfHellblock(currentOwner, newOwner, true)
														.thenCompose(transferResult -> {
															if (!transferResult) {
																plugin.getPluginLogger().warn(
																		"Admin force transfer did not succeed between currentOwner="
																				+ currentName + " and newOwner="
																				+ newName);
																return CompletableFuture.completedFuture(null);
															}

															// Feedback to executor
															handleFeedback(context,
																	MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_SUCCESS,
																	AdventureHelper.miniMessageToComponent(currentName),
																	AdventureHelper.miniMessageToComponent(newName));

															// Notify both players if online
															final Player currentOnline = Bukkit.getPlayer(currentId);
															final Player newOnline = Bukkit.getPlayer(newId);

															if (currentOnline != null) {
																handleFeedback(currentOnline,
																		MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_LOST,
																		AdventureHelper
																				.miniMessageToComponent(newName));
															}
															if (newOnline != null) {
																handleFeedback(newOnline,
																		MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED,
																		AdventureHelper
																				.miniMessageToComponent(currentName));
															}

															// Save changes
															return plugin.getStorageManager().getDataSource()
																	.updateManyPlayersData(
																			Set.of(currentOwner, newOwner), false);
														});
											} catch (Exception ex) {
												plugin.getPluginLogger().warn("Admin force transfer failed from "
														+ currentName + " to " + newName + ": " + ex.getMessage());
												return null;
											}
										}).exceptionally(ex -> {
											plugin.getPluginLogger().warn(
													"Failed to load new owner " + newName + ": " + ex.getMessage());
											return null;
										});
							}).exceptionally(ex -> {
								plugin.getPluginLogger()
										.warn("Failed to load current owner " + currentName + ": " + ex.getMessage());
								return null;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_transfer";
	}
}