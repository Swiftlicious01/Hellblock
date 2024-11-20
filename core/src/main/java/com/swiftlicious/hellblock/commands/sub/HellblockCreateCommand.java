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
import com.swiftlicious.hellblock.gui.hellblock.IslandChoiceMenu;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockCreateCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockCreateCommand(HellblockCommandManager<CommandSender> commandManager) {
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
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(
											onlineUser.get().getHellblockData().getResetCooldown())));
					return;
				}
				new IslandChoiceMenu(player, false);
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
								HellblockPlugin.getInstance().getConfigManager().lockData())
						.thenAccept((result) -> {
							UserData offlineUser = result.orElseThrow();
							if (offlineUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
												MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
								return;
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>You already have a hellblock or are in a co-op! Use <dark_red>/hellblock home <red>to teleport to it.");
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>If you wish to leave use <dark_red>/hellcoop leave <red>to leave and start your own.");
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player, String.format(
									"<red>Your hellblock is located at x: <dark_red>%s <red>z: <dark_red>%s<red>.",
									offlineUser.getHellblockData().getHellblockLocation().getBlockX(),
									offlineUser.getHellblockData().getHellblockLocation().getBlockZ()));
						});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_create";
	}

}
