package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class HellblockCreateCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockCreateCommand(HellblockCommandManager<CommandSender> commandManager) {
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

			final UserData userData = onlineUserOpt.get();
			final HellblockData data = userData.getHellblockData();

			// Player does not have a hellblock yet → create flow
			if (!data.hasHellblock()) {
				if (data.getResetCooldown() > 0) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN,
							AdventureHelper.miniMessageToComponent(
									plugin.getCooldownManager().getFormattedCooldown(data.getResetCooldown())));
					return;
				}

				plugin.getIslandChoiceGUIManager().openIslandChoiceGUI(player, false);
				return;
			}

			// Player has a hellblock already → reset/ownership flow
			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException(
						"Owner reference was null. This should never happen — please report to the developer.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username != null ? username
									: plugin.getTranslationManager()
											.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())));
					return;
				}

				final UserData ownerData = optOwnerData.get();
				if (ownerData.getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}

				// Already has a Hellblock → failure message
				final Location hellblockLoc = ownerData.getHellblockData().getHellblockLocation();
				if (hellblockLoc == null) {
					plugin.getPluginLogger().severe("Hellblock location returned null for owner " + ownerData.getName()
							+ " (" + ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
					throw new IllegalStateException(
							"Hellblock location reference was null. This should never happen — please report to the developer.");
				}

				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_CREATION_FAILURE_ALREADY_EXISTS,
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockLoc.getBlockX())),
						AdventureHelper.miniMessageToComponent(String.valueOf(hellblockLoc.getBlockZ())));
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for hellblock creation of "
						+ player.getName() + ": " + ex.getMessage());
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_create";
	}
}