package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.player.UserData;

public class CoopToggleCommand extends BukkitCommandFeature<CommandSender> {

	public CoopToggleCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("setting",
						StringParser.stringComponent()
								.suggestionProvider((context, input) -> CompletableFuture.completedFuture(
										List.of("invites", "join").stream().map(Suggestion::suggestion).toList())))
				.handler(context -> {
					Player player = context.sender();
					String setting = context.get("setting");

					Optional<UserData> userOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					PlayerData playerData = userOpt.get().toPlayerData();

					switch (setting.toLowerCase()) {
					case "invites", "invitations" -> {
						if (userOpt.get().getHellblockData().getInvitations().isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
							break;
						}
						boolean newValue = !playerData.hasHellblockInviteNotifications();
						playerData.setHellblockInviteNotifications(newValue);
						handleFeedback(context, newValue ? MessageConstants.MSG_HELLBLOCK_COOP_TOGGLE_INVITES_ON
								: MessageConstants.MSG_HELLBLOCK_COOP_TOGGLE_INVITES_OFF);
					}
					case "join", "login" -> {
						if (!userOpt.get().getHellblockData().hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							break;
						}
						boolean newValue = !playerData.hasHellblockJoinNotifications();
						playerData.setHellblockJoinNotifications(newValue);
						handleFeedback(context, newValue ? MessageConstants.MSG_HELLBLOCK_COOP_TOGGLE_JOIN_ON
								: MessageConstants.MSG_HELLBLOCK_COOP_TOGGLE_JOIN_OFF);
					}
					default -> {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TOGGLE_INVALID_ARGUMENT);
					}
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_toggle";
	}
}