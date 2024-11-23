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
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class AdminPurgeCommand extends BukkitCommandFeature<CommandSender> {

	public AdminPurgeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.required("days", IntegerParser.integerParser(1, 30)).handler(context -> {
			if (HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers().size() == 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(context.sender(),
						"<red>No hellblock player data to purge available!");
				return;
			}
			AtomicInteger purgeCount = new AtomicInteger(0);
			for (UUID id : HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers()) {
				if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore())
					continue;

				OfflinePlayer player = Bukkit.getOfflinePlayer(id);
				if (player.getLastLogin() == 0)
					continue;
				long millisSinceLastLogin = (System.currentTimeMillis() - player.getLastLogin()) -
				// Account for a timezone difference
						TimeUnit.MILLISECONDS.toHours(19);
				if (millisSinceLastLogin > TimeUnit.DAYS.toMillis(context.getOrDefault("days", 1))) {
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								UserData offlineUser = result.orElseThrow();
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
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(context.sender(),
						String.format("<red>You purged a total of %s hellblocks!", purgeCount.get()));
			} else {
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(context.sender(),
						"<red>No hellblock data was purged with your inputted settings!");
				return;
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_purge";
	}
}