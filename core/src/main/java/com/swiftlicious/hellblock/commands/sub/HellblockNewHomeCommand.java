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
import com.swiftlicious.hellblock.utils.LocationUtils;

public class HellblockNewHomeCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockNewHomeCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				if (onlineUser.get().getHellblockData().getHomeLocation() != null) {
					LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept((result) -> {
						if (!result.booleanValue() || player.isInLava() || player.isInPowderedSnow()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The location you're standing at is not safe for a new home!");
							return;
						}
					}).thenRunAsync(() -> {
						if (onlineUser.get().getHellblockData().getHomeLocation().getWorld() != null
								&& onlineUser.get().getHellblockData().getHomeLocation().getWorld().getName()
										.equals(player.getWorld().getName())
								&& onlineUser.get().getHellblockData().getHomeLocation().getX() == player.getLocation()
										.getX()
								&& onlineUser.get().getHellblockData().getHomeLocation().getY() == player.getLocation()
										.getY()
								&& onlineUser.get().getHellblockData().getHomeLocation().getZ() == player.getLocation()
										.getZ()
								&& onlineUser.get().getHellblockData().getHomeLocation().getYaw() == player
										.getLocation().getYaw()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
									"<red>The location you're standing at is already set as your home!");
							return;
						}
						onlineUser.get().getHellblockData().setHomeLocation(player.getLocation());
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player, String.format(
								"<red>You've set your new hellblock home location to x:%s, y:%s, z:%s facing %s!",
								player.getLocation().getBlockX(), player.getLocation().getBlockY(),
								player.getLocation().getBlockZ(), LocationUtils.getFacing(player)));
					});
				} else {
					HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
							"<red>Error setting your home location!");
					throw new NullPointerException(
							String.format("Home location for %s returned null, please report this to the developer.",
									onlineUser.get().getName()));
				}
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_sethome";
	}

}
