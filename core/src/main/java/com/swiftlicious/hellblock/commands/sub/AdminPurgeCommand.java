package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminPurgeCommand extends BukkitCommandFeature<CommandSender> {

	public AdminPurgeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@SuppressWarnings("deprecation")
	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.required("days", IntegerParser.integerParser(1, 30)).handler(context -> {
			if (HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers().size() == 0) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE);
				return;
			}
			AtomicInteger purgeCount = new AtomicInteger(0);
			for (UUID id : HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers()) {
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastPlayed() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastPlayed()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(context.getOrDefault("days", 1))) {
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty()) {
									handleFeedback(context,
											MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD.arguments(Component
													.text(player.getName() != null ? player.getName() : "???")));
									return;
								}
								UserData offlineUser = result.get();
								if (offlineUser.getHellblockData().hasHellblock()
										&& offlineUser.getHellblockData().getOwnerUUID() != null) {
									if (HellblockPlugin.getInstance().getHellblockHandler().isHellblockOwner(id,
											offlineUser.getHellblockData().getOwnerUUID())) {
										float level = offlineUser.getHellblockData().getLevel();
										if (level == HellblockData.DEFAULT_LEVEL) {

											offlineUser.getHellblockData().setAsAbandoned(true);
											int hellblockID = offlineUser.getHellblockData().getID();
											if (HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(
													offlineUser.getHellblockData().getOwnerUUID(),
													hellblockID) != null) {
												HellblockPlugin.getInstance().getWorldGuardHandler()
														.updateHellblockMessages(
																offlineUser.getHellblockData().getOwnerUUID(),
																HellblockPlugin.getInstance().getWorldGuardHandler()
																		.getRegion(offlineUser.getHellblockData()
																				.getOwnerUUID(), hellblockID));
												HellblockPlugin.getInstance().getWorldGuardHandler().abandonIsland(
														offlineUser.getHellblockData().getOwnerUUID(),
														HellblockPlugin.getInstance().getWorldGuardHandler().getRegion(
																offlineUser.getHellblockData().getOwnerUUID(),
																hellblockID));
												purgeCount.getAndIncrement();
											}
										}
									}
								}
							}).join();
				}
			}

			if (purgeCount.get() > 0) {
				handleFeedback(context,
						MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS.arguments(Component.text(purgeCount.get())));
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_FAILURE);
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_purge";
	}
}