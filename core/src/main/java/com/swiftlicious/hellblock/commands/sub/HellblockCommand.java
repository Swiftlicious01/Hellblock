package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
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
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
								HellblockPlugin.getInstance().getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty()) {
								String username = Bukkit
										.getOfflinePlayer(onlineUser.get().getHellblockData().getOwnerUUID()).getName();
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
										username != null ? Component.text(username) : Component.empty());
								return;
							}
							UserData offlineUser = result.get();
							if (offlineUser.getHellblockData().isAbandoned()) {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
								return;
							}
						}).thenRun(() -> {
							if (HellblockPlugin.getInstance().getHellblockGUIManager().openHellblockGUI(player,
									onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId()))) {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_SUCCESS,
										Component.text(player.getName()));
							} else {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OPEN_FAILURE_NOT_LOADED,
										Component.text(player.getName()));
							}
						});
			} else {
				HellblockPlugin.getInstance().getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock";
	}
}