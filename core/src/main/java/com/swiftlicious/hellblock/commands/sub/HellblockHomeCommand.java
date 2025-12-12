package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

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

			final UserData user = onlineUserOpt.get();
			final HellblockData data = user.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, plugin.getConfigManager().lockData())
					.thenAccept(ownerOpt -> {
						if (ownerOpt.isEmpty()) {
							final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
									AdventureHelper.miniMessageToComponent(username != null ? username
											: plugin.getTranslationManager().miniMessageTranslation(
													MessageConstants.FORMAT_UNKNOWN.build().key())));
							return;
						}

						final UserData ownerUser = ownerOpt.get();
						if (ownerUser.getHellblockData().isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
							return;
						}

						if (ownerUser.getHellblockData().getHomeLocation() == null) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
							plugin.getPluginLogger().severe("Hellblock home location was null for owner "
									+ ownerUser.getName() + " (" + ownerUser.getUUID() + ")");
							throw new IllegalStateException(
									"Hellblock home location returned null. This should never happen — please report to the developer.");
						}

						plugin.getCoopManager().makeHomeLocationSafe(ownerUser, user)
								.thenRun(() -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_TELEPORT))
								.exceptionally(ex -> {
									plugin.getPluginLogger().warn("makeHomeLocationSafe failed for " + player.getName()
											+ ": " + ex.getMessage());
									return null;
								});
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for home teleport of "
								+ player.getName() + ": " + ex.getMessage());
						return null;
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_home";
	}
}