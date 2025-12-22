package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();

			final Optional<UserData> onlineUserOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

			if (onlineUserOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData userData = onlineUserOpt.get();
			final HellblockData data = userData.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, true).thenCompose(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username != null ? username
									: plugin.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())));
					return CompletableFuture.completedFuture(false);
				}

				final UserData ownerData = optOwnerData.get();

				if (ownerData.getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return CompletableFuture.completedFuture(false);
				}

				if (ownerData.getHellblockData().getHomeLocation() == null) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
					plugin.getPluginLogger().severe("Hellblock home location was null for owner " + ownerData.getName()
							+ " (" + ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
					return CompletableFuture.failedFuture(new IllegalStateException(
							"Hellblock home location returned null. This should never happen — please report to the developer."));
				}

				return plugin.getCoopManager().makeHomeLocationSafe(ownerData, userData).thenCompose(result -> {
					switch (result) {
					case ALREADY_SAFE:
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_TELEPORT);
						return plugin.getHellblockHandler().teleportPlayerToHome(userData,
								ownerData.getHellblockData().getHomeLocation());
					case FIXED_AND_TELEPORTED:
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_TELEPORT);
						return CompletableFuture.completedFuture(true);
					case FAILED_TO_FIX:
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
						if (ownerData.getPlayer() == null)
							return CompletableFuture.completedFuture(false);
						return CompletableFuture.completedFuture(
								plugin.getHellblockHandler().teleportToSpawn(ownerData.getPlayer(), true));
					}
					return CompletableFuture.completedFuture(false); // fallback
				});
			}).handle((result, ex) -> {
				if (ex != null) {
					plugin.getPluginLogger()
							.warn("Error during home teleport for " + player.getName() + ": " + ex.getMessage(), ex);
				}
				return result != null && result;
			}).thenCompose(
					success -> plugin.getStorageManager().unlockUserData(ownerUUID).thenApply(unused -> success));
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_home";
	}
}