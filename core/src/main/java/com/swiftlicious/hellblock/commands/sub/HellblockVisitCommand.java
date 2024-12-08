package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class HellblockVisitCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, Boolean> visitCache = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

	public HellblockVisitCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty())
								return CompletableFuture.completedFuture(Collections.emptyList());
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(user -> user != null && user.isOnline()
											&& user.getHellblockData().hasHellblock()
											&& user.getHellblockData().getHomeLocation() != null
											&& !onlineUser.get().getHellblockData().getParty().contains(user.getUUID())
											&& !user.getName().equalsIgnoreCase(onlineUser.get().getName()))
									.map(user -> user.getName()).collect(Collectors.toList());
							return CompletableFuture
									.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}
					String user = context.get("player");
					UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
							: UUIDFetcher.getUUID(user);
					if (id == null) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}
					HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(id, player.getUniqueId())
							.thenAccept((banned) -> {
								if (banned) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY);
									return;
								}
								HellblockPlugin.getInstance().getStorageManager()
										.getOfflineUserData(id,
												HellblockPlugin.getInstance().getConfigManager().lockData())
										.thenAccept((result) -> {
											if (result.isEmpty()) {
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																.arguments(Component.text(user)));
												return;
											}
											UserData offlineUser = result.get();
											if (id.equals(player.getUniqueId()) || offlineUser.getHellblockData()
													.getParty().contains(player.getUniqueId())) {
												if (!onlineUser.get().getHellblockData().hasHellblock()) {
													handleFeedback(context,
															MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
													return;
												}
												if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
													throw new NullPointerException(
															"Owner reference returned null, please report this to the developer.");
												}
												HellblockPlugin.getInstance().getStorageManager().getOfflineUserData(
														onlineUser.get().getHellblockData().getOwnerUUID(),
														HellblockPlugin.getInstance().getConfigManager().lockData())
														.thenAccept((owner) -> {
															if (owner.isEmpty()) {
																String username = Bukkit
																		.getOfflinePlayer(onlineUser.get()
																				.getHellblockData().getOwnerUUID())
																		.getName() != null
																				? Bukkit.getOfflinePlayer(onlineUser
																						.get().getHellblockData()
																						.getOwnerUUID()).getName()
																				: "???";
																handleFeedback(context,
																		MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																				.arguments(Component.text(username)));
																return;
															}
															UserData ownerUser = owner.get();
															if (ownerUser.getHellblockData().isAbandoned()) {
																handleFeedback(context,
																		MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
																return;
															}
															if (ownerUser.getHellblockData()
																	.getHomeLocation() != null) {
																HellblockPlugin.getInstance().getCoopManager()
																		.makeHomeLocationSafe(ownerUser,
																				onlineUser.get())
																		.thenRun(() -> handleFeedback(context,
																				MessageConstants.MSG_HELLBLOCK_HOME_TELEPORT));
															} else {
																handleFeedback(context,
																		MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
																throw new NullPointerException(
																		"Hellblock home location returned null, please report this to the developer.");
															}
														});
												return;
											}

											if (offlineUser.getHellblockData().hasHellblock()) {
												if (offlineUser.getHellblockData().getOwnerUUID() == null) {
													throw new NullPointerException(
															"Owner reference returned null, please report this to the developer.");
												}
												HellblockPlugin.getInstance().getStorageManager().getOfflineUserData(
														offlineUser.getHellblockData().getOwnerUUID(),
														HellblockPlugin.getInstance().getConfigManager().lockData())
														.thenAccept((owner) -> {
															if (owner.isEmpty()) {
																String username = Bukkit
																		.getOfflinePlayer(offlineUser.getHellblockData()
																				.getOwnerUUID())
																		.getName() != null
																				? Bukkit.getOfflinePlayer(
																						offlineUser.getHellblockData()
																								.getOwnerUUID())
																						.getName()
																				: "???";
																handleFeedback(context,
																		MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																				.arguments(Component.text(username)));
																return;
															}
															UserData ownerUser = owner.get();
															if (ownerUser.getHellblockData().isAbandoned()) {
																handleFeedback(context,
																		MessageConstants.MSG_HELLBLOCK_VISIT_ABANDONED);
																return;
															}
															HellblockPlugin.getInstance().getCoopManager()
																	.checkIfVisitorIsWelcome(player,
																			ownerUser.getUUID())
																	.thenAccept((status) -> {
																		if (!ownerUser.getHellblockData().isLocked()
																				&& status) {

																			if (ownerUser.getHellblockData()
																					.getHomeLocation() != null) {
																				LocationUtils
																						.isSafeLocationAsync(ownerUser
																								.getHellblockData()
																								.getHomeLocation())
																						.thenAccept((safe) -> {
																							if (!safe.booleanValue()) {
																								handleFeedback(context,
																										MessageConstants.MSG_HELLBLOCK_UNSAFE_TO_VISIT);
																								return;
																							}
																						}).thenRunAsync(() -> {
																							ChunkUtils.teleportAsync(
																									player,
																									ownerUser
																											.getHellblockData()
																											.getHomeLocation(),
																									TeleportCause.PLUGIN)
																									.thenRun(() -> {
																										if (!visitCache
																												.getIfPresent(
																														player.getUniqueId())) {
																											if (ownerUser
																													.getHellblockData()
																													.getOwnerUUID() == null) {
																												throw new NullPointerException(
																														"Owner reference returned null, please report this to the developer.");
																											}
																											if (!(ownerUser
																													.getHellblockData()
																													.getOwnerUUID()
																													.equals(player
																															.getUniqueId())
																													|| ownerUser
																															.getHellblockData()
																															.getParty()
																															.contains(
																																	player.getUniqueId())
																													|| ownerUser
																															.getHellblockData()
																															.getTrusted()
																															.contains(
																																	player.getUniqueId()))) {
																												ownerUser
																														.getHellblockData()
																														.addTotalVisit();
																												visitCache
																														.put(player
																																.getUniqueId(),
																																true);
																											}
																										}
																										handleFeedback(
																												context,
																												MessageConstants.MSG_HELLBLOCK_VISIT_ENTRY
																														.arguments(
																																Component
																																		.text(ownerUser
																																				.getName())));
																									});
																							return;
																						});
																			} else {
																				handleFeedback(context,
																						MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
																				throw new NullPointerException(
																						"Hellblock home location returned null, please report this to the developer.");
																			}
																		} else {
																			handleFeedback(context,
																					MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS
																							.arguments(Component
																									.text(user)));
																			return;
																		}
																	});
														});
											}

											handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
											return;
										});
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visit";
	}
}
