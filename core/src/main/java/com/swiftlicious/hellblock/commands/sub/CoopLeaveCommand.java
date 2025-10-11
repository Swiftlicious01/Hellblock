package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

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

public class CoopLeaveCommand extends BukkitCommandFeature<CommandSender> {

	public CoopLeaveCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			final Optional<UserData> leavingPlayerOpt = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());

			if (leavingPlayerOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData leavingPlayer = leavingPlayerOpt.get();
			final HellblockData data = leavingPlayer.getHellblockData();

			// Validation: must be in a party
			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			// Must be the owner
			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			if (data.isOwner(leavingPlayer.getUUID())) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE);
				return;
			}

			if (!data.isInParty(leavingPlayer.getUUID())) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY);
				return;
			}

			// Perform leave
			HellblockPlugin.getInstance().getCoopManager().leaveHellblockParty(leavingPlayer);

			// Feedback
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_LEFT_PARTY);
		});
	}

	@Override
	public String getFeatureID() {
		return "coop_leave";
	}
}