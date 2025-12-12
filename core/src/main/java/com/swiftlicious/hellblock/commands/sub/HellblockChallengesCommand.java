package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockChallengesCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockChallengesCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			Player player = context.sender();

			final Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

			if (userOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			boolean isOwner = false;
			int islandId = -1;

			final UserData userData = userOpt.get();
			final HellblockData data = userData.getHellblockData();
			if (data.hasHellblock()) {
				final UUID ownerUUID = data.getOwnerUUID();
				islandId = data.getIslandId();
				isOwner = ownerUUID != null && data.isOwner(player.getUniqueId());
			}

			boolean success = plugin.getChallengesGUIManager().openChallengesGUI(player, islandId, isOwner, false);

			if (!success) {
				handleFeedback(context, MessageConstants.COMMAND_VISIT_GUI_FAILED);
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_challenges";
	}
}