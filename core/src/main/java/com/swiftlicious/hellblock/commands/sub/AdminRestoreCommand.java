package com.swiftlicious.hellblock.commands.sub;

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

public class AdminRestoreCommand extends BukkitCommandFeature<CommandSender> {

	public AdminRestoreCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context,
						input) -> CompletableFuture.completedFuture(HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUsers().stream().filter(user -> user.getHellblockData().isAbandoned())
								.map(UserData::getName).map(Suggestion::suggestion).toList())))
				.handler(context -> {
					final String targetName = context.get("player");
					final UUID targetId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetId, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
									return;
								}

								final UserData offlineUser = result.get();
								final HellblockData data = offlineUser.getHellblockData();

								if (!data.hasHellblock()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
									return;
								}

								if (!data.isAbandoned()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_RESTORE_NOT_ABANDONED
											.arguments(Component.text(targetName)));
									return;
								}

								final UUID ownerUUID = data.getOwnerUUID();
								if (ownerUUID == null) {
									HellblockPlugin.getInstance().getPluginLogger()
											.severe("Hellblock owner UUID was null for player " + targetName + " ("
													+ targetId + "). This indicates corrupted data.");
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

											// Perform restoration
											HellblockPlugin.getInstance().getProtectionManager()
													.restoreIsland(ownerData.getHellblockData());

											handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_RESTORE_SUCCESS
													.arguments(Component.text(targetName)));
										});
							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("Failed to restore abandoned hellblock for " + targetName + ": "
												+ ex.getMessage());
								return null;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_restore";
	}
}