package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

public class HellblockLockCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockLockCommand(HellblockCommandManager<CommandSender> commandManager) {
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

			// Player does not have a hellblock
			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			// Validate ownership
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

			final int islandId = data.getIslandId();
			final String worldName = plugin.getWorldManager().getHellblockWorldFormat(islandId);

			final Optional<HellblockWorld<?>> worldOpt = plugin.getWorldManager().getWorld(worldName);
			if (worldOpt.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
				plugin.getPluginLogger().warn("World not found for unlock/lock command: " + worldName + " (Island ID: "
						+ islandId + ", Owner UUID: " + ownerUUID + ")");
				return;
			}

			final HellblockWorld<?> world = worldOpt.get();
			if (world.bukkitWorld() == null) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
				plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName + " (Island ID: " + islandId
						+ ", Owner UUID: " + ownerUUID + ")");
				return;
			}

			// Toggle locked status
			data.setLockedStatus(!data.isLocked());

			handleFeedback(context, data.isLocked() ? MessageConstants.MSG_HELLBLOCK_LOCK_SUCCESS
					: MessageConstants.MSG_HELLBLOCK_UNLOCK_SUCCESS);

			// Side effects
			plugin.getProtectionManager().changeLockStatus(world, ownerUUID).thenCompose(lockStatus -> {

				// Kick out visitors if locked
				if (data.isLocked()) {
					// fire and forget, explicitly
					return plugin.getCoopManager().kickVisitorsIfLocked(ownerUUID).exceptionally(ex -> {
						plugin.getPluginLogger().warn(
								"Failed to kick visitors for locked island " + ownerUUID + ": " + ex.getMessage(), ex);
						return null;
					});
				}

				return CompletableFuture.completedFuture(null);
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn(
						"Failed to change island lock status for " + player.getName() + ": " + ex.getMessage(), ex);
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_lock";
	}
}