package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminActivityCommand extends BukkitCommandFeature<CommandSender> {

	public AdminActivityCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).required("player", StringParser.stringComponent()).handler(context -> {
			final String userArg = context.get("player");

			final UUID id = Bukkit.getPlayer(userArg) != null ? Bukkit.getPlayer(userArg).getUniqueId()
					: UUIDFetcher.getUUID(userArg);

			if (id == null || !Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
				return;
			}

			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
									.arguments(Component.text(userArg)));
							return;
						}

						final UserData targetUser = result.get();

						final long lastPlayed = Bukkit.getOfflinePlayer(id).getLastPlayed();
						final String formattedLastLogin = HellblockPlugin.getInstance()
								.getFormattedCooldown(System.currentTimeMillis() - lastPlayed);

						final String lastIslandActivity = targetUser.getLastActivity() > 0
								? HellblockPlugin.getInstance()
										.getFormattedCooldown(System.currentTimeMillis() - targetUser.getLastActivity())
								: HellblockPlugin.getInstance().getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_NEVER.build().key());

						handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_ADMIN_ACTIVITY.arguments(
										Component.text(targetUser.getName()), Component.text(formattedLastLogin),
										Component.text(lastIslandActivity)));
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_activity";
	}
}