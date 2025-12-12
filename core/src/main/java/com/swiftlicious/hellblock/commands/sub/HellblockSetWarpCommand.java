package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.LocationUtils;

public class HellblockSetWarpCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockSetWarpCommand(HellblockCommandManager<CommandSender> commandManager) {
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

			final UserData user = onlineUserOpt.get();
			final HellblockData data = user.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

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

			// Validate that the player is standing in a safe location
			LocationUtils.isSafeLocationAsync(player.getLocation()).thenAccept(isSafe -> {
				if (!isSafe || player.getLocation().getBlock().getType() == Material.LAVA
						|| player.getLocation().getBlock().getType() == Material.POWDER_SNOW) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WARP_UNSAFE_STANDING_LOCATION);
					return;
				}

				// Check if the new location is identical to the current warp
				final Location currentWarp = data.getVisitData().getWarpLocation();
				final Location newLoc = player.getLocation();
				if (currentWarp.getWorld() != null
						&& currentWarp.getWorld().getName().equals(newLoc.getWorld().getName())
						&& currentWarp.getX() == newLoc.getX() && currentWarp.getY() == newLoc.getY()
						&& currentWarp.getZ() == newLoc.getZ() && currentWarp.getYaw() == newLoc.getYaw()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WARP_SAME_LOCATION);
					return;
				}

				// Save new warp
				data.getVisitData().setWarpLocation(newLoc);
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WARP_NEW_LOCATION,
						AdventureHelper.miniMessageToComponent(String.valueOf(newLoc.getBlockX())),
						AdventureHelper.miniMessageToComponent(String.valueOf(newLoc.getBlockY())),
						AdventureHelper.miniMessageToComponent(String.valueOf(newLoc.getBlockZ())),
						AdventureHelper.miniMessageToComponent(LocationUtils.getFacing(player)));
			}).exceptionally(ex -> {
				plugin.getPluginLogger().severe("Failed to check if warp location is safe " + player.getName() + " ("
						+ player.getUniqueId() + "): " + ex.getMessage());
				ex.printStackTrace();
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_setwarp";
	}
}