package com.swiftlicious.hellblock.commands.sub;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class AdminRollbackCommand extends BukkitCommandFeature<CommandSender> {

	// Cache mapping formatted string -> timestamp for suggestion resolution
	private final Map<String, Long> snapshotLookup = new HashMap<>();
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public AdminRollbackCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final Set<UUID> allKnownUUIDs = plugin.getStorageManager().getDataSource().getUniqueUsers();

					final List<String> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull).toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
				})).optional("minutes", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					return CompletableFuture.completedFuture(
							List.of("5", "10", "30", "60", "120").stream().map(Suggestion::suggestion).toList());
				}))
				// timestamp suggestion provider
				.optional("timestamp", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final String playerName = context.<String>optional("player").orElse(null);
					if (playerName == null) {
						return CompletableFuture.completedFuture(List.of());
					}

					return CompletableFuture.supplyAsync(() -> {
						final List<String> snapshotStrings = getSnapshotSuggestions(playerName);
						return snapshotStrings.stream().map(Suggestion::suggestion).toList();
					});
				})).handler(this::executeRollback);
	}

	@Override
	public String getFeatureID() {
		return "admin_rollback";
	}

	private void executeRollback(@NotNull CommandContext<Player> context) {
		final String playerName = context.get("player");
		final Integer minutes = context.getOrDefault("minutes", null);
		final String timestampStr = context.getOrDefault("timestamp", null);

		final Optional<UUID> optionalUuid = UUIDFetcher.getUUID(playerName);

		if (optionalUuid.isEmpty()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE,
					AdventureHelper.miniMessageToComponent(playerName));
			return;
		}

		final UUID targetId = optionalUuid.get();
		if (!Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE,
					AdventureHelper.miniMessageToComponent(playerName));
			return;
		}

		if (timestampStr != null) {
			// --- Priority: timestamp rollback ---
			Long ts = snapshotLookup.get(timestampStr);

			// Fallback: rebuild cache
			if (ts == null) {
				getSnapshotSuggestions(playerName);
				ts = snapshotLookup.get(timestampStr);
			}

			// Final fallback: parse formatted date string
			if (ts == null) {
				try {
					ts = sdf.parse(timestampStr).getTime();
				} catch (ParseException e) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_INVALID,
							AdventureHelper.miniMessageToComponent(timestampStr));
					return;
				}
			}

			final long chosenTs = ts;
			plugin.getHellblockHandler().rollbackIsland(targetId, chosenTs)
					.thenRun(() -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_TIMESTAMP,
							AdventureHelper.miniMessageToComponent(playerName), AdventureHelper.miniMessageToComponent(timestampStr)));

		} else if (minutes != null) {
			// --- Secondary: rollback by "last X minutes" ---
			plugin.getHellblockHandler().rollbackLastMinutes(targetId, minutes)
					.thenRun(() -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_MINUTES,
							AdventureHelper.miniMessageToComponent(playerName), AdventureHelper.miniMessageToComponent(minutes.toString())));

		} else {
			// --- Fallback: rollback to latest snapshot ---
			plugin.getIslandBackupManager().listSnapshots(targetId).stream().reduce((first, second) -> second) // newest
					.ifPresentOrElse(ts -> {
						final String formatted = sdf.format(new Date(ts));
						plugin.getHellblockHandler().rollbackIsland(targetId, ts).thenRun(() -> handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LATEST,
								AdventureHelper.miniMessageToComponent(playerName), AdventureHelper.miniMessageToComponent(formatted)));
					}, () -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_NONE,
							AdventureHelper.miniMessageToComponent(playerName)));
		}
	}

	@NotNull
	private List<String> getSnapshotSuggestions(@NotNull String playerName) {
		final OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (!player.hasPlayedBefore() || player.getName() == null) {
			return List.of();
		}

		final UUID playerId = player.getUniqueId();
		final List<Long> snapshots = plugin.getIslandBackupManager().listSnapshots(playerId);

		// Rebuild the lookup map fresh per-request (avoids stale cache)
		snapshotLookup.clear();
		return snapshots.stream().map(ts -> {
			final String formatted = sdf.format(new Date(ts));
			snapshotLookup.put(formatted, ts);
			return formatted;
		}).toList();
	}

	public class AdminRollbackListCommand extends BukkitCommandFeature<CommandSender> {

		private static final int SNAPSHOT_LIST_LIMIT = 10;

		public AdminRollbackListCommand(HellblockCommandManager<CommandSender> commandManager) {
			super(commandManager);
		}

		@Override
		public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
				Command.Builder<CommandSender> builder) {
			return builder.senderType(Player.class).literal("list")
					.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
						if (!(context.sender() instanceof Player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final Set<UUID> allKnownUUIDs = plugin.getStorageManager().getDataSource().getUniqueUsers();

						final List<String> suggestions = allKnownUUIDs.stream()
								.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid))
								.filter(Optional::isPresent).map(Optional::get)
								.filter(user -> user.getHellblockData().hasHellblock()).map(UserData::getName)
								.filter(Objects::nonNull).toList();

						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}))
					// page suggestion provider
					.optional("page", StringParser.stringComponent().suggestionProvider((context, input) -> {
						if (!(context.sender() instanceof Player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final String playerName = context.<String>optional("player").orElse(null);
						if (playerName == null) {
							return CompletableFuture.completedFuture(List.of());
						}

						// Suggest all pages, or maybe filter by input
						String userInput = input.peekString();

						return CompletableFuture.supplyAsync(() -> {
							final List<String> snapshotStrings = getSnapshotSuggestions(playerName);
							final int pages = Math.max(1,
									(snapshotStrings.size() + SNAPSHOT_LIST_LIMIT - 1) / SNAPSHOT_LIST_LIMIT);

							return IntStream.rangeClosed(1, pages).mapToObj(Integer::toString)
									.filter(s -> userInput.isEmpty() || s.startsWith(userInput))
									.map(Suggestion::suggestion).toList();
						});
					})).handler(this::executeRollbackList);
		}

		@Override
		public String getFeatureID() {
			return "admin_rollback_list";
		}

		private void executeRollbackList(@NotNull CommandContext<Player> context) {
			final String playerName = context.get("player");
			final Optional<UUID> optionalUuid = UUIDFetcher.getUUID(playerName);
			if (optionalUuid.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE,
						AdventureHelper.miniMessageToComponent(playerName));
				return;
			}

			final UUID targetId = optionalUuid.get();
			if (!Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE,
						AdventureHelper.miniMessageToComponent(playerName));
				return;
			}

			final List<Long> snapshots = plugin.getIslandBackupManager().listSnapshots(targetId);

			if (snapshots.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_EMPTY,
						AdventureHelper.miniMessageToComponent(playerName));
				return;
			}

			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_HEADER,
					AdventureHelper.miniMessageToComponent(playerName));

			final long now = System.currentTimeMillis();

			// Sort newest → oldest
			final List<Long> sorted = snapshots.stream().sorted(Comparator.reverseOrder()).toList();

			// Reconstruct base command they typed (strip "list <player>")
			final String rawInput = context.rawInput().input(); // full typed input
			// Example: "hellblock rollback list Notch"
			// Base = "hellblock rollback"
			final String[] parts = rawInput.split(" ");
			final String baseCommand = parts.length >= 2 ? parts[0] + " " + parts[1] : "rollback";

			int page;
			// Extract the page argument as a string (if present), or default to "1"
			String pageInput = context.<String>optional("page").orElse("1");
			try {
				page = Math.max(1, Integer.parseInt(pageInput));
			} catch (NumberFormatException e) {
				final List<String> snapshotStrings = getSnapshotSuggestions(playerName);
				final int totalPages = Math.max(1,
						(snapshotStrings.size() + SNAPSHOT_LIST_LIMIT - 1) / SNAPSHOT_LIST_LIMIT);
				handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
						AdventureHelper.miniMessageToComponent(pageInput),
						AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
				return;
			}

			final int start = (page - 1) * SNAPSHOT_LIST_LIMIT;
			final int end = Math.min(start + SNAPSHOT_LIST_LIMIT, sorted.size());

			for (int i = start; i < end; i++) {
				final Long ts = sorted.get(i);

				final String formatted = sdf.format(new Date(ts));
				final String ago = formatTimeAgo(now - ts);

				final String clickCmd = "/" + baseCommand + " " + playerName + " 0 " + formatted;

				final Component entry = MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_ENTRY
						.arguments(AdventureHelper.miniMessageToComponent(formatted), AdventureHelper.miniMessageToComponent(ago)).build()
						.clickEvent(ClickEvent.runCommand(clickCmd))
						.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_HOVER
								.arguments(AdventureHelper.miniMessageToComponent(playerName),
										AdventureHelper.miniMessageToComponent(formatted))
								.build()));

				handleFeedbackRaw(context.sender(), entry);
			}

			final int remaining = sorted.size() - end;
			if (remaining > 0) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_ROLLBACK_LIST_MORE,
						AdventureHelper.miniMessageToComponent(Integer.toString(remaining)));
			}
		}

		@NotNull
		private String formatTimeAgo(long millis) {
			final long seconds = millis / 1000;
			final long minutes = seconds / 60;
			final long hours = minutes / 60;
			final long days = hours / 24;

			if (days > 0) {
				return days + plugin.getTranslationManager()
						.miniMessageTranslation(MessageConstants.FORMAT_DAY.build().key());
			}
			if (hours > 0) {
				return hours + plugin.getTranslationManager()
						.miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
			}
			return minutes > 0
					? minutes + plugin.getTranslationManager()
							.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key())
					: seconds + plugin.getTranslationManager()
							.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());
		}
	}
}