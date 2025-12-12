package com.swiftlicious.hellblock.commands.sub;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;

public class AdminCleanupCommand extends BukkitCommandFeature<CommandSender> {

	public AdminCleanupCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.handler(context -> {
			Set<UUID> userIds = plugin.getStorageManager().getDataSource().getUniqueUsers();

			if (userIds.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_NONE_FOUND);
				return;
			}

			AtomicInteger totalChecked = new AtomicInteger(0);
			int totalToCheck = userIds.size();
			AtomicInteger totalReset = new AtomicInteger(0);
			AtomicBoolean finished = new AtomicBoolean(false); // to prevent double feedback

			Runnable complete = () -> {
				if (finished.compareAndSet(false, true)) {
					if (totalReset.get() == 0) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_NONE_FOUND);
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_COMPLETED,
								AdventureHelper.miniMessageToComponent(String.valueOf(totalReset.get())));
					}
				}
			};

			Runnable checkIfDone = () -> {
				if (totalChecked.incrementAndGet() >= totalToCheck) {
					complete.run();
				}
			};

			// Start timeout countdown (e.g., 10 seconds)
			ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.schedule(() -> {
				if (finished.compareAndSet(false, true)) {
					plugin.getPluginLogger().warn(
							"Hellblock cleanup command timed out before completing all user checks. Some results may be missing.");
				}
				complete.run(); // force complete if timeout
				scheduler.shutdown();
			}, 10, TimeUnit.SECONDS);

			for (UUID id : userIds) {
				plugin.getStorageManager().getCachedUserDataWithFallback(id, false).thenAccept(result -> {
					if (result.isEmpty()) {
						plugin.getPluginLogger().warn("Orphaned hellblock found for UUID " + id);
						plugin.getHellblockHandler().resetHellblock(id, true, "Console").thenRun(() -> {
							totalReset.incrementAndGet();
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_ORPHAN,
									AdventureHelper.miniMessageToComponent(id.toString()));
							checkIfDone.run();
						});
						return;
					}

					UserData data = result.get();
					if (data.getHellblockData().hasHellblock() && data.getHellblockData().getOwnerUUID() == null) {
						plugin.getPluginLogger()
								.warn("Hellblock with null owner detected for " + data.getName() + " (" + id + ")");
						plugin.getHellblockHandler().resetHellblock(id, true, "Console").thenRun(() -> {
							plugin.getStorageManager().saveUserData(data, plugin.getConfigManager().lockData());
							totalReset.incrementAndGet();
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_CORRUPT,
									AdventureHelper.miniMessageToComponent(data.getName()));
							checkIfDone.run();
						});
					} else {
						checkIfDone.run();
					}
				});
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_cleanup";
	}
}