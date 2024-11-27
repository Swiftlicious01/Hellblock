package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;

import org.bukkit.Material;
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

import net.kyori.adventure.text.Component;

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
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}
				if (onlineUser.get().getHellblockData().getHomeLocation() != null) {
					LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept((result) -> {
						if (!result.booleanValue() || player.getLocation().getBlock().getType() == Material.LAVA
								|| player.getLocation().getBlock().getType() == Material.POWDER_SNOW) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_UNSAFE_STANDING_LOCATION);
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
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOME_SAME_LOCATION);
							return;
						}
						onlineUser.get().getHellblockData().setHomeLocation(player.getLocation());
						handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_HOME_NEW_LOCATION.arguments(
										Component.text(player.getLocation().getBlockX()),
										Component.text(player.getLocation().getBlockY()),
										Component.text(player.getLocation().getBlockZ()),
										Component.text(LocationUtils.getFacing(player))));
					});
				} else {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ERROR_HOME_LOCATION);
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
