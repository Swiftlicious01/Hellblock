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

public class HellblockCreateCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockCreateCommand(HellblockCommandManager<CommandSender> commandManager) {
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
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN,
							Component.text(HellblockPlugin.getInstance()
									.getFormattedCooldown(onlineUser.get().getHellblockData().getResetCooldown())));
					return;
				}
				// TODO: stuff
			} else {
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

							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_CREATION_FAILURE_ALREADY_EXISTS,
									Component.text(offlineUser.getHellblockData().getHellblockLocation().getBlockX()),
									Component.text(offlineUser.getHellblockData().getHellblockLocation().getBlockZ()));
						});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_create";
	}
}