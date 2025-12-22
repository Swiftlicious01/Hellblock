package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

public class AdminAbandonedCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, String> abandonedListCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
			.build();

	public AdminAbandonedCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).optional("page", StringParser.stringParser()).handler(context -> {

			final int pageSize = 10;
			final Player sender = context.sender();

			// --- Step 1: use cached list if available ---
			Map<UUID, String> cached = abandonedListCache.asMap();
			if (!cached.isEmpty()) {
				displayAbandonedList(sender, new ArrayList<>(cached.values()), context, pageSize);
				return;
			}

			// --- Step 2: otherwise start async scan ---
			Set<UUID> allKnownUUIDs = plugin.getStorageManager().getDataSource().getUniqueUsers();
			ConcurrentMap<UUID, String> abandonedMap = new ConcurrentHashMap<>();
			List<CompletableFuture<Void>> futures = new ArrayList<>();

			for (UUID id : allKnownUUIDs) {
				CompletableFuture<Void> future = plugin.getStorageManager().getCachedUserDataWithFallback(id, false)
						.thenAccept(opt -> {
							if (opt.isEmpty())
								return;

							UserData userData = opt.get();
							HellblockData data = userData.getHellblockData();

							UUID ownerUUID = data.getOwnerUUID();
							if (ownerUUID == null) {
								plugin.getPluginLogger()
										.severe("Hellblock owner UUID was null for player " + userData.getName() + " ("
												+ userData.getUUID() + "). This indicates corrupted data.");
								throw new IllegalStateException(
										"Owner reference was null. This should never happen — please report to the developer.");
							}

							if (data.hasHellblock() && data.isAbandoned() && id.equals(ownerUUID)) {
								abandonedMap.put(id, userData.getName());
								abandonedListCache.put(id, userData.getName());
							}
						});
				futures.add(future);
			}

			// --- Step 3: progressively display results while scanning ---
			AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();
			taskRef.set(plugin.getScheduler().sync().runRepeating(new Runnable() {
				private int ticks = 0;

				@Override
				public void run() {
					ticks++;

					List<String> current = new ArrayList<>(abandonedMap.values());
					if (!current.isEmpty() && ticks % 40 == 0) { // every 2s
						displayAbandonedList(sender, current, context, pageSize);
					}
					if (ticks > 20 * 15) { // stop after 15s of polling
						SchedulerTask task = taskRef.get();
						if (task != null && !task.isCancelled()) {
							task.cancel();
						}
					}
				}
			}, 40L, 40L, sender.getLocation()));

			// --- Step 4: finalize once all lookups are complete ---
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRunAsync(() -> {
				SchedulerTask task = taskRef.get();
				if (task != null && !task.isCancelled()) {
					task.cancel(); // stop the polling early if done
				}

				List<String> finalList = new ArrayList<>(abandonedMap.values());
				displayAbandonedList(sender, finalList, context, pageSize);
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("Failed to display abandoned islands list: " + ex.getMessage(), ex);
				return null;
			});
		});
	}

	private void displayAbandonedList(Player sender, List<String> abandoned, CommandContext<Player> context,
			int pageSize) {
		abandoned.sort(String::compareToIgnoreCase);
		if (abandoned.isEmpty()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_NO_ABANDONED_FOUND);
			return;
		}

		String pageInput = context.<String>optional("page").orElse("1");
		int totalPages = (int) Math.ceil((double) abandoned.size() / pageSize);

		int page;
		try {
			page = Integer.parseInt(pageInput);
		} catch (NumberFormatException e) {
			handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
					AdventureHelper.miniMessageToComponent(pageInput),
					AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
			return;
		}

		int start = (page - 1) * pageSize;
		int end = Math.min(start + pageSize, abandoned.size());

		if (page < 1 || start >= abandoned.size()) {
			handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
					AdventureHelper.miniMessageToComponent(String.valueOf(page)),
					AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
			return;
		}

		List<String> pageEntries = abandoned.subList(start, end);

		handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ABANDONED_LIST,
				AdventureHelper.miniMessageToComponent(String.valueOf(page)),
				AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)),
				AdventureHelper.miniMessageToComponent(String.join(", ", pageEntries)));
	}

	@Override
	public String getFeatureID() {
		return "admin_abandoned";
	}
}