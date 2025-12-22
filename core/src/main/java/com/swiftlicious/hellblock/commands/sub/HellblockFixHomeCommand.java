package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class HellblockFixHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockFixHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			// Owner validation (kept exactly as requested)
			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data.");
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

			final int islandId = data.getIslandId();
			final String worldName = plugin.getWorldManager().getHellblockWorldFormat(islandId);

			final Optional<HellblockWorld<?>> worldOpt = plugin.getWorldManager().getWorld(worldName);
			if (worldOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
				plugin.getPluginLogger().warn("World not found for fix home command: " + worldName + " (Island ID: "
						+ islandId + ", Owner UUID: " + ownerUUID + ")");
				return;
			}

			final World bukkitWorld = worldOpt.get().bukkitWorld();
			if (bukkitWorld == null) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
				plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName + " (Island ID: " + islandId
						+ ", Owner UUID: " + ownerUUID + ")");
				return;
			}

			plugin.getHellblockHandler().locateNearestBedrock(userData).thenAccept(bedrock -> {
				if (bedrock == null) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BEDROCK_ERROR);
					plugin.getPluginLogger().warn("Bedrock was not found for: " + worldName + " (Island ID: " + islandId
							+ ", Owner UUID: " + ownerUUID + ")");
					return;
				}
				bedrock.setY(bukkitWorld.getHighestBlockYAt(bedrock));
				data.setHomeLocation(bedrock);

				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK);
			}).exceptionally(ex -> {
				plugin.getPluginLogger().severe("Failed to reset home to bedrock for " + player.getName() + " ("
						+ ownerUUID + "): " + ex.getMessage());
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