package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class HellblockCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();

			final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());

			if (onlineUserOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData onlineUser = onlineUserOpt.get();
			final HellblockData data = onlineUser.getHellblockData();

			if (!data.hasHellblock()) {
				// Player doesn’t have an island yet → open island choice GUI
				HellblockPlugin.getInstance().getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
									username != null ? Component.text(username) : Component.empty());
							return;
						}

						final UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
							return;
						}

						final boolean isOwner = data.isOwner(ownerUUID);
						final boolean opened = HellblockPlugin.getInstance().getHellblockGUIManager()
								.openHellblockGUI(player, isOwner);

						if (opened) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_SUCCESS,
									Component.text(player.getName()));
						} else {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_FAILURE_NOT_LOADED,
									Component.text(player.getName()));
						}
					}).exceptionally(ex -> {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("getOfflineUserData failed for opening hellblock GUI of " + player.getName() + ": "
										+ ex.getMessage());
						return null;
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock";
	}
}