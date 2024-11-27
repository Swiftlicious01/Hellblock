package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockFixHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockFixHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}
				if (onlineUser.get().getHellblockData().getHomeLocation() != null && !HellblockPlugin.getInstance()
						.getHellblockHandler().checkIfInSpawn(onlineUser.get().getHellblockData().getHomeLocation())) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_NOT_BROKEN);
					return;
				}

				HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(player.getUniqueId())
						.thenAccept((result) -> {
							Location bedrock = result.getBedrockLocation();
							bedrock.setY(HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld()
									.getHighestBlockYAt(bedrock));
							onlineUser.get().getHellblockData().setHomeLocation(bedrock);
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK);
						});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_fixhome";
	}
}