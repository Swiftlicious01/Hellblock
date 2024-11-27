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

public class HellblockHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
								HellblockPlugin.getInstance().getConfigManager().lockData())
						.thenAccept((owner) -> {
							if (owner.isEmpty()) {
								String username = Bukkit
										.getOfflinePlayer(onlineUser.get().getHellblockData().getOwnerUUID())
										.getName() != null ? Bukkit
												.getOfflinePlayer(onlineUser.get().getHellblockData().getOwnerUUID())
												.getName() : "???";
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
										.arguments(Component.text(username)));
								return;
							}
							UserData ownerUser = owner.get();
							if (ownerUser.getHellblockData().isAbandoned()) {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
								return;
							}
							if (ownerUser.getHellblockData().getHomeLocation() != null) {
								HellblockPlugin.getInstance().getCoopManager()
										.makeHomeLocationSafe(ownerUser, onlineUser.get())
										.thenRun(() -> handleFeedback(context,
												MessageConstants.MSG_HELLBLOCK_HOME_TELEPORT));
							} else {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
								throw new NullPointerException(
										"Hellblock home location returned null, please report this to the developer.");
							}
						});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_home";
	}
}