package com.swiftlicious.hellblock.commands.sub;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.text.Component;

public class AdminAbandonedCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, String> abandonedListCache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS)
			.build();

	public AdminAbandonedCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).optional("page", IntegerParser.integerParser(1)).handler(context -> {
			final int page = context.getOrDefault("page", 1);
			final int pageSize = 10;

			HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers()
					.forEach(id -> HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
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

			HellblockPlugin.getInstance().getScheduler().sync().runLater(() -> {
				final List<String> abandoned = new ArrayList<>(abandonedListCache.asMap().values());
				if (abandoned.isEmpty()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_NO_ABANDONED_FOUND);
					return;
				}

				final int totalPages = (int) Math.ceil((double) abandoned.size() / pageSize);
				final int start = (page - 1) * pageSize;
				final int end = Math.min(start + pageSize, abandoned.size());

				if (start >= abandoned.size()) {
					handleFeedback(context,
							MessageConstants.MSG_HELLBLOCK_ADMIN_INVALID_PAGE.arguments(Component.text(page)));
					return;
				}

				final List<String> pageEntries = abandoned.subList(start, end);

				handleFeedback(context,
						MessageConstants.MSG_HELLBLOCK_ADMIN_ABANDONED_LIST.arguments(Component.text(page),
								Component.text(totalPages), Component.text(String.join(", ", pageEntries))));
			}, 20L, LocationUtils.getAnyLocationInstance()); // small delay to collect futures
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_abandoned";
	}
}