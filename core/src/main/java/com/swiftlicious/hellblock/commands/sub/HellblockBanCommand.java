package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class HellblockBanCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockBanCommand(HellblockCommandManager<CommandSender> commandManager) {
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
						if (!(context.sender() instanceof Player player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(player.getUniqueId());
						if (onlineUserOpt.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final HellblockData data = onlineUserOpt.get().getHellblockData();
						final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUsers().stream()
								.filter(user -> user != null && user.isOnline()
										&& !data.getTrusted().contains(user.getUUID())
										&& !data.getParty().contains(user.getUUID())
										&& !user.getName().equalsIgnoreCase(onlineUserOpt.get().getName()))
								.map(UserData::getName).toList();

						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
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
						HellblockPlugin.getInstance().getPluginLogger().severe("Hellblock owner UUID was null for player "
								+ player.getName() + " (" + player.getUniqueId() + "). This indicates corrupted data.");
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
					
					final UUID targetId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					if (data.getParty().contains(targetId) || data.getTrusted().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_PARTY);
						return;
					}

					if (data.getBanned().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ALREADY_BANNED);
						return;
					}

					// Perform ban
					data.banPlayer(targetId);

					final Player targetOnline = Bukkit.getPlayer(targetName);
					if (targetOnline != null) {
						HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(player.getUniqueId(), targetId)
								.thenAccept(status -> {
									if (!status) {
										return;
									}

									final Optional<UserData> bannedPlayerOpt = HellblockPlugin.getInstance()
											.getStorageManager().getOnlineUser(targetId);

									if (bannedPlayerOpt.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
												.arguments(Component.text(targetName)));
										return;
									}

									final UserData bannedPlayer = bannedPlayerOpt.get();
									if (!bannedPlayer.getHellblockData().hasHellblock()) {
										HellblockPlugin.getInstance().getHellblockHandler()
												.teleportToSpawn(targetOnline, false);
										return;
									}

									final UUID bannedOwnerUUID = bannedPlayer.getHellblockData().getOwnerUUID();
									if (bannedOwnerUUID == null) {
										HellblockPlugin.getInstance().getPluginLogger().severe(
												"Owner reference was null for banned player " + bannedPlayer.getName());
										throw new IllegalStateException("Owner reference was null for banned player.");
									}

									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUserData(bannedOwnerUUID,
													HellblockPlugin.getInstance().getConfigManager().lockData())
											.thenAccept(ownerOpt -> {
												if (ownerOpt.isEmpty()) {
													final String username = Bukkit.getOfflinePlayer(bannedOwnerUUID)
															.getName() != null
																	? Bukkit.getOfflinePlayer(bannedOwnerUUID).getName()
																	: "???";
													handleFeedback(context,
															MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																	.arguments(Component.text(username)));
													return;
												}

												HellblockPlugin.getInstance().getCoopManager()
														.makeHomeLocationSafe(ownerOpt.get(), bannedPlayer)
														.thenRun(() -> handleFeedback(targetOnline,
																MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY))
														.exceptionally(ex -> {
															HellblockPlugin.getInstance().getPluginLogger()
																	.warn("Ban handling failed for " + targetName
																			+ ": " + ex.getMessage());
															return null;
														});
											}).exceptionally(ex -> {
												HellblockPlugin.getInstance().getPluginLogger()
														.warn("getOfflineUserData failed for ban of "
																+ player.getName() + ": " + ex.getMessage());
												return null;
											});
								}).exceptionally(ex -> {
									HellblockPlugin.getInstance().getPluginLogger().warn(
											"trackBannedPlayer failed for " + targetName + ": " + ex.getMessage());
									return null;
								});
					}

					// Feedback for command executor
					handleFeedback(context,
							MessageConstants.MSG_HELLBLOCK_BANNED_PLAYER.arguments(Component.text(targetName)));
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_ban";
	}
}