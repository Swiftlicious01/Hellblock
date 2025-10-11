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

public class HellblockLockCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockLockCommand(HellblockCommandManager<CommandSender> commandManager) {
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

			// Player does not have a hellblock
			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			// Validate ownership
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

			// Toggle locked status
			data.setLockedStatus(!data.isLocked());

			handleFeedback(context, data.isLocked() ? MessageConstants.MSG_HELLBLOCK_LOCK_SUCCESS
					: MessageConstants.MSG_HELLBLOCK_UNLOCK_SUCCESS);
			
			// Side effects
			HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(user.getUUID());
			HellblockPlugin.getInstance().getProtectionManager().changeLockStatus(player.getWorld(), user.getUUID());
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_lock";
	}
}