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

public class HellblockCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				// Player doesn’t have an island yet → open island choice GUI
				plugin.getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username != null ? username
									: plugin.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())));
					return;
				}

				final UserData ownerData = optOwnerData.get();
				if (ownerData.getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}

				final boolean isOwner = ownerData.getHellblockData().isOwner(ownerUUID);
				final int islandId = ownerData.getHellblockData().getIslandId();
				final boolean opened = plugin.getHellblockGUIManager().openHellblockGUI(player, islandId, isOwner);

				if (opened) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_SUCCESS,
							AdventureHelper.miniMessageToComponent(player.getName()));
				} else {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_FAILURE_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(player.getName()));
				}
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for opening hellblock GUI of "
						+ player.getName() + ": " + ex.getMessage());
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock";
	}
}