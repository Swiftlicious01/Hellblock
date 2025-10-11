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

public class HellblockFixHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockFixHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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

			final UserData user = onlineUserOpt.get();
			final HellblockData data = user.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			// Owner validation (kept exactly as requested)
			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger().severe("Hellblock owner UUID was null for player "
						+ player.getName() + " (" + player.getUniqueId() + "). This indicates corrupted data.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			if (!data.isOwner(ownerUUID)) {
				handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
				return;
			}

			if (data.isAbandoned()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return;
			}

			if (data.getHomeLocation() != null && data.getBoundingBox().contains(data.getHomeLocation().toVector())) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_NOT_BROKEN);
				return;
			}

			HellblockPlugin.getInstance().getHellblockHandler().locateBedrock(player.getUniqueId())
					.thenAccept(bedrock -> {
						bedrock.setY(player.getWorld().getHighestBlockYAt(bedrock));
						data.setHomeLocation(bedrock);

						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK);
					}).exceptionally(ex -> {
						HellblockPlugin.getInstance().getPluginLogger().severe("Failed to reset home to bedrock for "
								+ player.getName() + " (" + player.getUniqueId() + "): " + ex.getMessage());
						ex.printStackTrace();
						return null;
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_fixhome";
	}
}