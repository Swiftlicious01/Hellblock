package com.swiftlicious.hellblock.commands.sub;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

public class HellblockTopCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockTopCommand(HellblockCommandManager<CommandSender> commandManager) {
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
			if (!HellblockPlugin.getInstance().getIslandLevelManager().getTopTenHellblocks().entrySet().isEmpty()) {
				int i = 0;
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_HEADER);
				for (Map.Entry<UUID, Float> ten : HellblockPlugin.getInstance().getIslandLevelManager()
						.getTopTenHellblocks().reversed().entrySet()) {

					UUID id = ten.getKey();
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
						continue;
					if (Bukkit.getOfflinePlayer(id).getName() == null)
						continue;
					float level = ten.getValue().floatValue();
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_FORMAT.arguments(Component.text(++i),
							Component.text(Bukkit.getOfflinePlayer(id).getName()), Component.text(level)));
					if (i >= 10) {
						break;
					}
				}
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_FOUND);
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_top";
	}
}