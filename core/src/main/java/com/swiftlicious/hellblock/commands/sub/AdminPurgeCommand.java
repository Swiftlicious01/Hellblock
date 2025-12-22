package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.jetbrains.annotations.NotNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.hellblock.HellblockAbandonEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.ProtectionManager;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.WorldManager;

public class AdminPurgeCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, Boolean> purgeConfirmations = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
			.build();

	public AdminPurgeCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.required("days", IntegerParser.integerParser(1, 30))
				.optional("below-level", FloatParser.floatParser(0, 100000)).flag(manager.flagBuilder("force"))
				.senderType(Player.class).handler(context -> {
					final Player player = context.sender();
					final UUID playerId = player.getUniqueId();

					final int days = context.getOrDefault("days", 1);
					final float belowLevel = context.<Float>optional("below-level").orElse(HellblockData.DEFAULT_LEVEL);
					final boolean force = context.flags().isPresent("force");

					// force flag skips confirmation
					if (force) {
						runPurge(context, days, belowLevel);
						return;
					}

					// confirmation via cache
					if (purgeConfirmations.getIfPresent(playerId) != null) {
						purgeConfirmations.invalidate(playerId);
						runPurge(context, days, belowLevel);
					} else {
						purgeConfirmations.put(playerId, true);
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_CONFIRM,
								AdventureHelper.miniMessageToComponent(String.valueOf(days)),
								AdventureHelper.miniMessageToComponent(String.valueOf(belowLevel)));
					}
				});
	}

	private void runPurge(@NotNull CommandContext<Player> context, int days, float belowLevel) {
		final Set<UUID> allUsers = plugin.getStorageManager().getDataSource().getUniqueUsers();

		if (allUsers.isEmpty()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE);
			return;
		}

		final WorldManager worldManager = plugin.getWorldManager();
		final ProtectionManager protectionManager = plugin.getProtectionManager();
		final long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
		final AtomicInteger purgeCount = new AtomicInteger(0);
		final List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (UUID id : allUsers) {
			final CompletableFuture<Void> future = plugin.getStorageManager().getCachedUserDataWithFallback(id, true)
					.thenCompose(optData -> {
						if (optData.isEmpty()) {
							return CompletableFuture.completedFuture(false);
						}

						final UserData userData = optData.get();
						final long lastActivity = userData.getHellblockData().getLastIslandActivity();

						if (lastActivity == 0 || lastActivity > cutoffMillis) {
							return CompletableFuture.completedFuture(false);
						}

						final HellblockData hellblockData = userData.getHellblockData();

						final int islandId = hellblockData.getIslandId();

						final String worldName = worldManager.getHellblockWorldFormat(islandId);

						final Optional<HellblockWorld<?>> worldOpt = worldManager.getWorld(worldName);
						if (!worldOpt.isPresent()) {
							plugin.getPluginLogger().warn("World not found for purge command: " + worldName
									+ " (Island ID: " + islandId + ", Owner UUID: " + id + ")");
							return CompletableFuture.completedFuture(false);
						}

						final HellblockWorld<?> world = worldOpt.get();
						if (world.bukkitWorld() == null) {
							plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName + " (Island ID: "
									+ islandId + ", Owner UUID: " + id + ")");
							return CompletableFuture.completedFuture(false);
						}

						if (hellblockData.hasHellblock() && hellblockData.getOwnerUUID() != null
								&& hellblockData.isOwner(id) && hellblockData.getIslandLevel() <= belowLevel) {

							purgeCount.incrementAndGet();
							hellblockData.setAsAbandoned(true);

							return protectionManager.getIslandProtection()
									.updateHellblockMessages(world, hellblockData.getOwnerUUID())
									.thenCombine(protectionManager.getIslandProtection().abandonIsland(world, id),
											(updated, abandoned) -> updated && abandoned)
									.thenCompose(success -> {
										if (!success) {
											return CompletableFuture.completedFuture(false);
										}

										// Fire event
										final HellblockAbandonEvent abandonEvent = new HellblockAbandonEvent(id,
												hellblockData);
										EventUtils.fireAndForget(abandonEvent);

										// Save changes
										return plugin.getStorageManager().saveUserData(userData, true)
												.thenApply(saved -> saved);
									});
						}

						return CompletableFuture.completedFuture(false);
					}).handle((result, ex) -> {
						if (ex != null) {
							plugin.getPluginLogger().warn("Failed to purge " + id + ": " + ex.getMessage(), ex);
						}
						// Always unlock the data (success or failure)
						return plugin.getStorageManager().unlockUserData(id);
					}).thenCompose(Function.identity());

			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			if (purgeCount.get() > 0) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS,
						AdventureHelper.miniMessageToComponent(String.valueOf(purgeCount.get())));
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_FAILURE);
			}
		}).exceptionally(ex -> {
			plugin.getPluginLogger().warn("Failed to finish purging islands: " + ex.getMessage(), ex);
			return null;
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_purge";
	}
}