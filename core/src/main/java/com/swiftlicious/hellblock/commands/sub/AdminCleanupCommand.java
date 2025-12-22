package com.swiftlicious.hellblock.commands.sub;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

			// Timeout safeguard (10 seconds)
			plugin.getScheduler().asyncLater(() -> {
				if (finished.compareAndSet(false, true)) {
					plugin.getPluginLogger()
							.warn("Hellblock cleanup command timed out before completing all user checks.");
					complete.run();
				}
			}, 10L, TimeUnit.SECONDS); // 10s = 200 ticks

			// Process all users asynchronously
			for (UUID id : userIds) {
				plugin.getStorageManager().getCachedUserDataWithFallback(id, false).thenCompose(optData -> {
					// CASE 1: No user data — orphaned island
					if (optData.isEmpty()) {
						plugin.getPluginLogger().warn("Orphaned hellblock found for UUID " + id);
						return plugin.getHellblockHandler().resetHellblock(id, true, "HBConsole").thenRun(() -> {
							totalReset.incrementAndGet();
							plugin.getScheduler()
									.executeSync(() -> handleFeedback(context,
											MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_ORPHAN,
											AdventureHelper.miniMessageToComponent(id.toString())));
							checkIfDone.run();
						}).handle((result, ex) -> {
							if (ex != null) {
								plugin.getPluginLogger()
										.warn("resetHellblock failed for " + id.toString() + ": " + ex.getMessage());
							}
							return false;
						});
					}

					// CASE 2: User data exists
					UserData data = optData.get();
					if (data.getHellblockData().hasHellblock() && data.getHellblockData().getOwnerUUID() == null) {
						plugin.getPluginLogger()
								.warn("Hellblock with null owner detected for " + data.getName() + " (" + id + ")");
						return plugin.getHellblockHandler().resetHellblock(id, true, "HBConsole").thenRun(() -> {
							totalReset.incrementAndGet();
							plugin.getScheduler()
									.executeSync(() -> handleFeedback(context,
											MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_CORRUPT,
											AdventureHelper.miniMessageToComponent(data.getName())));
							checkIfDone.run();
						}).handle((result, ex) -> {
							if (ex != null) {
								plugin.getPluginLogger()
										.warn("resetHellblock failed for " + data.getName() + ": " + ex.getMessage());
							}
							return false;
						});
					} else {
						// Nothing to clean up
						checkIfDone.run();
						return CompletableFuture.completedFuture(false);
					}
				}).handle((result, ex) -> {
					if (ex != null) {
						plugin.getPluginLogger().warn("Cleanup error for UUID " + id + ": " + ex.getMessage());
					}
					checkIfDone.run();
					return false;
				}).thenCompose(v -> plugin.getStorageManager().unlockUserData(id).thenApply(x -> true));
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_cleanup";
	}
}