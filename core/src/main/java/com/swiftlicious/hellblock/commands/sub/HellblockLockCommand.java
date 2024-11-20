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

public class HellblockLockCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockLockCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				return;
			}
			if (onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
					return;
				}
				onlineUser.get().getHellblockData().setLockedStatus(!onlineUser.get().getHellblockData().isLocked());
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You've just <dark_red>%s <red>your hellblock island!",
								(onlineUser.get().getHellblockData().isLocked() ? "locked" : "unlocked")));
				if (onlineUser.get().getHellblockData().isLocked()) {
					HellblockPlugin.getInstance().getCoopManager().kickVisitorsIfLocked(player.getUniqueId());
					HellblockPlugin.getInstance().getCoopManager().changeLockStatus(onlineUser.get());
				}
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HellblockPlugin.getInstance().getTranslationManager()
								.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_lock";
	}
}