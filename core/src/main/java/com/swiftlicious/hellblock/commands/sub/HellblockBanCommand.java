package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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

public class HellblockBanCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockBanCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// No data loaded — show nothing
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();

					if (!data.hasHellblock()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (!data.isOwner(ownerUUID)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.isAbandoned()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Set<UUID> excludedUUIDs = new HashSet<>();
					excludedUUIDs.add(player.getUniqueId());
					excludedUUIDs.addAll(data.getPartyMembers());
					excludedUUIDs.addAll(data.getTrustedMembers());
					excludedUUIDs.addAll(data.getBannedMembers());

					List<String> suggestions = plugin.getStorageManager().getDataSource().getUniqueUsers().stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> !excludedUUIDs.contains(user.getUUID()))
							.sorted(Comparator.comparingLong((UserData u) -> {
								long activity = u.getHellblockData().getLastIslandActivity();
								return activity > 0 ? activity : Long.MIN_VALUE; // push unknowns to end
							}).reversed()).map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> !name.equalsIgnoreCase(userData.getName())).distinct().toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = onlineUserOpt.get();
					final HellblockData data = user.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!data.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					final String targetName = context.get("player");
					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

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

					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					if (data.getPartyMembers().contains(targetId) || data.getTrustedMembers().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_PARTY);
						return;
					}

					if (data.getBannedMembers().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ALREADY_BANNED);
						return;
					}

					// Perform ban
					data.banPlayer(targetId);

					final Player targetOnline = Bukkit.getPlayer(targetName);
					if (targetOnline != null) {
						plugin.getCoopManager()
								.isPlayerBannedInLocation(player.getUniqueId(), targetId, targetOnline.getLocation())
								.thenAccept(status -> {
									if (!status || (targetOnline.hasPermission("hellblock.admin")
											|| targetOnline.hasPermission("hellblock.bypass.interact")
											|| targetOnline.isOp())) {
										return;
									}

									final Optional<UserData> bannedPlayerOpt = plugin.getStorageManager()
											.getOnlineUser(targetId);

									if (bannedPlayerOpt.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(targetName));
										return;
									}

									final UserData bannedPlayer = bannedPlayerOpt.get();
									if (!bannedPlayer.getHellblockData().hasHellblock()) {
										plugin.getHellblockHandler().teleportToSpawn(targetOnline, false);
										return;
									}

									final UUID bannedOwnerUUID = bannedPlayer.getHellblockData().getOwnerUUID();
									if (bannedOwnerUUID == null) {
										plugin.getPluginLogger()
												.severe("Hellblock owner UUID was null for player "
														+ bannedPlayer.getName() + " (" + bannedPlayer.getUUID()
														+ "). This indicates corrupted data.");
										throw new IllegalStateException(
												"Owner reference was null. This should never happen — please report to the developer.");
									}

									plugin.getStorageManager().getCachedUserDataWithFallback(bannedOwnerUUID,
											plugin.getConfigManager().lockData()).thenAccept(ownerOpt -> {
												if (ownerOpt.isEmpty()) {
													final String username = Bukkit.getOfflinePlayer(bannedOwnerUUID)
															.getName() != null
																	? Bukkit.getOfflinePlayer(bannedOwnerUUID).getName()
																	: plugin.getTranslationManager()
																			.miniMessageTranslation(
																					MessageConstants.FORMAT_UNKNOWN
																							.build().key());
													handleFeedback(context,
															MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
															AdventureHelper.miniMessageToComponent(username));
													return;
												}

												plugin.getCoopManager()
														.makeHomeLocationSafe(ownerOpt.get(), bannedPlayer)
														.thenRun(() -> handleFeedback(targetOnline,
																MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY))
														.exceptionally(ex -> {
															plugin.getPluginLogger().warn("Ban handling failed for "
																	+ targetName + ": " + ex.getMessage());
															return null;
														});
											}).exceptionally(ex -> {
												plugin.getPluginLogger()
														.warn("getCachedUserDataWithFallback failed for ban of "
																+ player.getName() + ": " + ex.getMessage());
												return null;
											});
								}).exceptionally(ex -> {
									plugin.getPluginLogger().warn("isPlayerBannedInLocation failed for " + targetName
											+ ": " + ex.getMessage());
									return null;
								});
					}

					// Feedback for command executor
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BANNED_PLAYER,
							AdventureHelper.miniMessageToComponent(targetName));
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_ban";
	}
}