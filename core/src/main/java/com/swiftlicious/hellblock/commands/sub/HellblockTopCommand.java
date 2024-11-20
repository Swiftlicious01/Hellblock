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
import com.swiftlicious.hellblock.player.UserData;

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
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				return;
			}
			if (!HellblockPlugin.getInstance().getIslandLevelManager().getTopTenHellblocks().entrySet().isEmpty()) {
				int i = 0;
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>Top Ten Level Hellblocks:");
				for (Map.Entry<UUID, Float> ten : HellblockPlugin.getInstance().getIslandLevelManager()
						.getTopTenHellblocks().reversed().entrySet()) {

					UUID id = ten.getKey();
					if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
						continue;
					if (Bukkit.getOfflinePlayer(id).getName() == null)
						continue;
					float level = ten.getValue().floatValue();
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player, String.format(
							"<dark_red>%s. <red>%s <gray>(Lvl %s)", ++i, Bukkit.getOfflinePlayer(id).getName(), level));
					if (i >= 10) {
						break;
					}
				}
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						"<red>No hellblocks to list for the top ten!");
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_top";
	}
}