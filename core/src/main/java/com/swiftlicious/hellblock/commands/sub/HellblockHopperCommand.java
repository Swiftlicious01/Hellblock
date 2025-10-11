package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

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
			final Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(playerUUID);

			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData user = onlineUser.get();
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = user.getHellblockData().getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
								+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			if (onlineUser.get().getHellblockData().isAbandoned()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return;
			}

			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept(userDataOpt -> {
						if (userDataOpt.isEmpty()) {
							return;
						}

						final UserData owner = userDataOpt.get();
						final BoundingBox bounds = owner.getHellblockData().getBoundingBox();
						if (bounds == null) {
							return;
						}

						final int placed = HellblockPlugin.getInstance().getHopperHandler().countHoppers(bounds);
						final int max = owner.getHellblockData().getMaxHopperLimit();

						HellblockPlugin.getInstance().getSenderFactory().wrap(player)
								.sendMessage(MessageConstants.MSG_HELLBLOCK_HOPPER_INFO
										.arguments(Component.text(placed), Component.text(max)).build());
					}).exceptionally(ex -> {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("getOfflineUserData failed for hopper check of " + player.getName() + ": "
										+ ex.getMessage());
						return null;
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_hopper";
	}
}