package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.FloatParser;
import org.incendo.cloud.parser.standard.IntegerParser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.hellblock.HellblockAbandonEvent;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.Component;

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
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_CONFIRM
								.arguments(Component.text(days), Component.text(belowLevel)));
					}
				});
	}

	@SuppressWarnings("deprecation")
	private void runPurge(CommandContext<Player> context, int days, float belowLevel) {
		final Set<UUID> allUsers = HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers();

		if (allUsers.isEmpty()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_UNAVAILABLE);
			return;
		}

		final long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
		final AtomicInteger purgeCount = new AtomicInteger(0);
		final List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (UUID id : allUsers) {
			final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(id);

			if (!offlinePlayer.hasPlayedBefore() || offlinePlayer.getLastPlayed() == 0) {
				continue;
			}
			if (offlinePlayer.getLastPlayed() > cutoffMillis) {
				continue;
			}

			final CompletableFuture<Void> future = HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}

						final UserData offlineUser = result.get();
						final HellblockData hellblock = offlineUser.getHellblockData();

						if (hellblock.hasHellblock() && id.equals(hellblock.getOwnerUUID())
								&& hellblock.getLevel() <= belowLevel) {
							purgeCount.incrementAndGet();
							final Optional<HellblockWorld<?>> world = HellblockPlugin.getInstance().getWorldManager()
									.getWorld(HellblockPlugin.getInstance().getWorldManager()
											.getHellblockWorldFormat(hellblock.getID()));
							if (world.isPresent()) {
								final World bukkitWorld = world.get().bukkitWorld();
								hellblock.setAsAbandoned(true);
								HellblockPlugin.getInstance().getProtectionManager().getIslandProtection()
										.updateHellblockMessages(bukkitWorld, hellblock.getOwnerUUID());
								HellblockPlugin.getInstance().getProtectionManager().getIslandProtection()
										.abandonIsland(bukkitWorld, hellblock.getOwnerUUID());

								final HellblockAbandonEvent abandonEvent = new HellblockAbandonEvent(id, hellblock);
								Bukkit.getPluginManager().callEvent(abandonEvent);
							}
						}
					});
			futures.add(future);
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
			if (purgeCount.get() > 0) {
				handleFeedback(context,
						MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_SUCCESS.arguments(Component.text(purgeCount.get())));
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_PURGE_FAILURE);
			}
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_purge";
	}
}