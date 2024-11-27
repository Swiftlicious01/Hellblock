package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

public class CoopInvitationsCommand extends BukkitCommandFeature<CommandSender> {

	public CoopInvitationsCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				if (onlineUser.get().getHellblockData().getInvitations() == null
						|| (onlineUser.get().getHellblockData().getInvitations() != null
								&& onlineUser.get().getHellblockData().getInvitations().isEmpty())) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
					return;
				}
				HellblockPlugin.getInstance().getCoopManager().listInvitations(onlineUser.get());
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS);
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "coop_invites";
	}
}