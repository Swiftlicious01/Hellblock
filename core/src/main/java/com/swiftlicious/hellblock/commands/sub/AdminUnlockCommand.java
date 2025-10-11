package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminUnlockCommand extends BukkitCommandFeature<CommandSender> {

	public AdminUnlockCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers()
							.stream().filter(user -> user.isOnline() && user.getHellblockData().hasHellblock())
							.map(UserData::getName).toList();
					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).handler(context -> {
					final Player executor = context.sender();
					final String userArg = context.get("player");

					final UUID id = Bukkit.getPlayer(userArg) != null ? Bukkit.getPlayer(userArg).getUniqueId()
							: UUIDFetcher.getUUID(userArg);

					if (id == null || !Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(userArg)));
									return;
								}

								final UserData targetUser = result.get();
								final HellblockData data = targetUser.getHellblockData();

								if (!data.hasHellblock()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
									return;
								}

								final UUID ownerUUID = data.getOwnerUUID();
								if (ownerUUID == null) {
									HellblockPlugin.getInstance().getPluginLogger()
											.severe("Hellblock owner UUID was null for player " + targetUser.getName()
													+ " (" + targetUser.getUUID()
													+ "). This indicates corrupted data.");
									throw new IllegalStateException(
											"Owner reference was null. This should never happen — please report to the developer.");
								}

								HellblockPlugin.getInstance().getStorageManager()
										.getOfflineUserData(ownerUUID,
												HellblockPlugin.getInstance().getConfigManager().lockData())
										.thenAccept(ownerOpt -> {
											if (ownerOpt.isEmpty()) {
												final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																.arguments(Component
																		.text(username != null ? username : "???")));
												return;
											}

											final UserData ownerData = ownerOpt.get();

											if (ownerData.getHellblockData().isAbandoned()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
												return;
											}

											// Toggle lock
											ownerData.getHellblockData()
													.setLockedStatus(!ownerData.getHellblockData().isLocked());
											if (ownerData.getHellblockData().isLocked()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_LOCKED
														.arguments(Component.text(userArg)));
											} else {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_UNLOCKED
														.arguments(Component.text(userArg)));
											}

											// Update protections
											HellblockPlugin.getInstance().getProtectionManager()
													.changeLockStatus(executor.getWorld(), ownerUUID);

											// Kick out visitors if locked
											if (ownerData.getHellblockData().isLocked()) {
												HellblockPlugin.getInstance().getCoopManager()
														.kickVisitorsIfLocked(ownerUUID);
											}
										});
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_unlock";
	}
}
