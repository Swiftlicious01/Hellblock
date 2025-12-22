package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockHopperCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockHopperCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			final UUID playerUUID = player.getUniqueId();
			final Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(playerUUID);

			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData userData = onlineUser.get();
			final HellblockData data = userData.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
							.orElse(plugin.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key()));
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username));
					return;
				}

				final UserData ownerData = optOwnerData.get();

				if (ownerData.getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}

				final BoundingBox boundingBox = ownerData.getHellblockData().getBoundingBox();
				if (boundingBox == null) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
					plugin.getPluginLogger().severe("Hellblock bounds returned null for player " + player.getName()
							+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
					throw new IllegalStateException(
							"Hellblock bounds reference was null. This should never happen — please report to the developer.");
				}

				final int placed = plugin.getHopperHandler().countHoppers(boundingBox);
				final int max = ownerData.getHellblockData().getMaxHopperLimit();

				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_HOPPER_INFO,
						AdventureHelper.miniMessageToComponent(String.valueOf(placed)),
						AdventureHelper.miniMessageToComponent(String.valueOf(max)));
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for hopper check of "
						+ player.getName() + ": " + ex.getMessage());
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_hopper";
	}
}