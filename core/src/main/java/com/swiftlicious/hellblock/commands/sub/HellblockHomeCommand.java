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

public class HellblockHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HellblockPlugin.getInstance().getTranslationManager()
								.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
				return;
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
								HellblockPlugin.getInstance().getConfigManager().lockData())
						.thenAccept((owner) -> {
							UserData ownerUser = owner.orElseThrow();
							if (ownerUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
												MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
								return;
							}
							if (ownerUser.getHellblockData().getHomeLocation() != null) {
								HellblockPlugin.getInstance().getCoopManager()
										.makeHomeLocationSafe(ownerUser, onlineUser.get())
										.thenRun(() -> HellblockPlugin.getInstance().getAdventureManager()
												.sendMessageWithPrefix(player,
														"<red>Teleporting you to your hellblock!"));
							} else {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										"<red>Error teleporting you to your hellblock!");
								throw new NullPointerException(
										"Hellblock home location returned null, please report this to the developer.");
							}
						});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_home";
	}
}