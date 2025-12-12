package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;

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

			plugin.getStorageManager().getDataSource().getUniqueUsers().forEach(
					id -> plugin.getStorageManager().getCachedUserDataWithFallback(id, false).thenAccept(result -> {
						if (result.isEmpty()) {
							return;
						}
						final UserData user = result.get();
						if (user.getHellblockData().hasHellblock() && user.getHellblockData().isAbandoned()
								&& user.getHellblockData().getOwnerUUID() != null
								&& id.equals(user.getHellblockData().getOwnerUUID())) {
							abandonedListCache.put(id, user.getName());
						}
					}));

			plugin.getScheduler().sync().runLater(() -> {
				final List<String> abandoned = new ArrayList<>(abandonedListCache.asMap().values());
				if (abandoned.isEmpty()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_NO_ABANDONED_FOUND);
					return;
				}

				int page;
				// Extract the page argument as a string (if present), or default to "1"
				String pageInput = context.<String>optional("page").orElse("1");
				final int totalPages = (int) Math.ceil((double) abandoned.size() / pageSize);
				try {
					page = Integer.parseInt(pageInput);
				} catch (NumberFormatException e) {
					handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
							AdventureHelper.miniMessageToComponent(pageInput),
							AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
					return;
				}

				final int start = (page - 1) * pageSize;
				final int end = Math.min(start + pageSize, abandoned.size());

				if (page < 1 || start >= abandoned.size()) {
					handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
							AdventureHelper.miniMessageToComponent(String.valueOf(page)),
							AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
					return;
				}

				final List<String> pageEntries = abandoned.subList(start, end);

				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ABANDONED_LIST,
						AdventureHelper.miniMessageToComponent(String.valueOf(page)),
						AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)),
						AdventureHelper.miniMessageToComponent(String.join(", ", pageEntries)));
			}, 20L, context.sender().getLocation()); // small delay to collect futures
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_abandoned";
	}
}