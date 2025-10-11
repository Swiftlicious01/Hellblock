package com.swiftlicious.hellblock.commands.sub;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslatableComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Command implementation for "/hellblock top". Displays a paginated leaderboard
 * of Hellblock levels with clickable navigation.
 */
public class HellblockTopCommand extends BukkitCommandFeature<CommandSender> {

	// Number of entries to show per leaderboard page
	private final int pageSize = 10;

	public HellblockTopCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	// Locale-aware number formatter for displaying levels (2 decimal max, grouped)
	private static final NumberFormat LEVEL_FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());
	static {
		LEVEL_FORMAT.setMaximumFractionDigits(2);
		LEVEL_FORMAT.setMinimumFractionDigits(0);
		LEVEL_FORMAT.setGroupingUsed(true);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class) // only players can run this
				// optional "page" argument with suggestions
				.optional("page", IntegerParser.integerComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						// if sender isn't a player, return empty suggestions
						if (!(context.sender() instanceof Player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						// Use the same async top call we use in showHellblockTop.
						// We map the returned top map -> page numbers and transform those to Suggestion
						// objects.
						final CompletableFuture<LinkedHashMap<UUID, Float>> topFuture = HellblockPlugin.getInstance()
								.getIslandLevelManager().getTopHellblocks(1000);

						// Transform the future into a suggestions future
						return topFuture.thenApply(top -> {
							if (top == null || top.isEmpty()) {
								return Collections.emptyList();
							}

							// only count entries with level > 1
							final long totalValid = top.values().stream().filter(Objects::nonNull).filter(v -> v > 1.0f)
									.count();

							final int totalPages = (int) Math.max(1, Math.ceil((double) totalValid / pageSize));
							final List<Suggestion> suggestions = new ArrayList<>(totalPages);
							for (int i = 1; i <= totalPages; i++) {
								suggestions.add(Suggestion.suggestion(String.valueOf(i)));
							}
							return suggestions;
						});
					}
				}))
				// Handler executed when the command runs
				.handler(ctx -> {
					final Player player = ctx.sender();

					// Ensure user data is loaded
					final Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(ctx, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					// Parse page argument, default = 1
					final int page = ctx.getOrDefault("page", 1);

					// Run main display logic
					this.showHellblockTop(ctx, page);
				});
	}

	/**
	 * Displays the paginated Hellblock leaderboard to the player.
	 *
	 * @param context Command execution context
	 * @param page    Page number to display (1-based index)
	 */
	private void showHellblockTop(CommandContext<?> context, int page) {
		final HellblockPlugin inst = HellblockPlugin.getInstance();

		// Fetch leaderboard asynchronously (top N entries)
		final CompletableFuture<LinkedHashMap<UUID, Float>> topFuture = inst.getIslandLevelManager()
				.getTopHellblocks(1000);

		topFuture.thenAccept(top -> {
			if (top == null || top.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_FOUND);
				return;
			}

			// Build leaderboard entries (skip invalid or level <= 1.0f)
			final List<LeaderboardEntry> entries = new ArrayList<>(top.size());
			for (Map.Entry<UUID, Float> e : top.entrySet()) {
				final UUID id = e.getKey();
				final float level = e.getValue() != null ? e.getValue() : 0f;
				if (level <= 1.0f) {
					continue;
				}

				final OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
				if (offline == null || !offline.hasPlayedBefore() || offline.getName() == null) {
					continue;
				}

				entries.add(new LeaderboardEntry(id, offline.getName(), level));
			}

			if (entries.isEmpty()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_FOUND);
				return;
			}

			// Sort: level DESC, name ASC, UUID fallback
			entries.sort((a, b) -> {
				int cmp = Float.compare(b.level, a.level);
				if (cmp != 0) {
					return cmp;
				}
				cmp = a.name.compareToIgnoreCase(b.name);
				return cmp != 0 ? cmp : a.uuid.compareTo(b.uuid);
			});

			// Pagination check
			final int totalEntries = entries.size();
			final int totalPages = (int) Math.ceil((double) totalEntries / pageSize);
			if (page < 1 || page > Math.max(1, totalPages)) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_INVALID_PAGE
						.arguments(Component.text(String.valueOf(Math.max(1, totalPages)))));
				return;
			}

			final int startIndex = (page - 1) * pageSize;
			final int endIndex = Math.min(startIndex + pageSize, totalEntries);

			// Header (configurable, accepts {0}=current page, {1}=total pages)
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_HEADER
					.arguments(Component.text(String.valueOf(page)), Component.text(String.valueOf(totalPages))));

			// Determine sender UUID for self-highlighting
			final UUID senderUuid = (context.sender() instanceof Player p) ? p.getUniqueId() : null;

			// --- Output leaderboard rows ---
			for (int i = startIndex; i < endIndex; i++) {
				final LeaderboardEntry le = entries.get(i);
				final int rank = i + 1;
				final String formattedLevel = LEVEL_FORMAT.format(le.level);

				// Base components to inject into message templates
				Component rankComponent = Component.text(rank);
				Component nameComponent = Component.text(le.name);
				final Component levelComponent = Component.text(formattedLevel);

				final boolean isSelf = senderUuid != null && senderUuid.equals(le.uuid);
				if (isSelf) {
					rankComponent = rankComponent.decorate(TextDecoration.BOLD);
					nameComponent = nameComponent.decorate(TextDecoration.BOLD);
				}

				// Choose message key: top 3 ranks get their own, otherwise default format
				final TranslatableComponent.Builder messageKey;
				switch (rank) {
				case 1 -> messageKey = MessageConstants.MSG_HELLBLOCK_TOP_RANK_1;
				case 2 -> messageKey = MessageConstants.MSG_HELLBLOCK_TOP_RANK_2;
				case 3 -> messageKey = MessageConstants.MSG_HELLBLOCK_TOP_RANK_3;
				default -> messageKey = MessageConstants.MSG_HELLBLOCK_TOP_FORMAT;
				}

				// Send formatted row
				handleFeedback(context, messageKey.arguments(rankComponent, nameComponent, levelComponent));
			}

			// --- Extract the base command alias + subcommand the player actually typed ---
			// Example: input = "hb top 3" → baseCommand = "/hb top"
			final String fullInput = context.rawInput().input();
			final String[] parts = fullInput.split(" ");
			final String baseCommand = parts.length >= 2 ? "/" + parts[0] + " " + parts[1] : "/" + parts[0];

			// --- Show sender’s personal rank summary ---
			int selfIndex = -1;
			if (senderUuid != null) {
				for (int i = 0; i < entries.size(); i++) {
					if (entries.get(i).uuid.equals(senderUuid)) {
						selfIndex = i + 1;
						break;
					}
				}
			}

			if (selfIndex != -1) {
				final LeaderboardEntry selfEntry = entries.get(selfIndex - 1);
				final String formattedLevel = LEVEL_FORMAT.format(selfEntry.level);
				final int selfPage = (selfIndex - 1) / pageSize + 1;

				final Builder viewYourPageClickable = MessageConstants.MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE
						.clickEvent(ClickEvent.runCommand(baseCommand + " " + selfPage))
						.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE_HOVER));

				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_SELF_SUMMARY
						.arguments(Component.text(selfIndex), Component.text(formattedLevel), viewYourPageClickable));
			} else {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_RANKED);
			}

			// --- Navigation bar ---
			TextComponent nav = Component.empty();

			if (page > 1) {
				final Builder prevLabel = MessageConstants.MSG_HELLBLOCK_TOP_PREVIOUS
						.clickEvent(ClickEvent.runCommand(baseCommand + " " + (page - 1)))
						.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_PREVIOUS_HOVER));
				nav = nav.append(prevLabel);
			}

			if (endIndex < totalEntries) {
				if (!nav.equals(Component.empty())) {
					nav = nav.append(MessageConstants.MSG_HELLBLOCK_TOP_NAV_SEPARATOR);
				}
				final Builder nextLabel = MessageConstants.MSG_HELLBLOCK_TOP_NEXT
						.clickEvent(ClickEvent.runCommand(baseCommand + " " + (page + 1)))
						.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_NEXT_HOVER));
				nav = nav.append(nextLabel);
			}

			// --- Page info (click to jump to another page) ---
			final Builder pageInfo = MessageConstants.MSG_HELLBLOCK_TOP_PAGE_INFO
					.arguments(Component.text(String.valueOf(page)), Component.text(String.valueOf(totalPages)))
					.clickEvent(ClickEvent.suggestCommand(baseCommand + " ")) // clean suggestion with alias
					.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_PAGE_INFO_HOVER));

			// Wrap navigation bar (wrappers configurable in YAML)
			final Builder centered;
			if (!nav.equals(Component.empty())) {
				centered = MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_START.append(nav)
						.append(MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_END).append(pageInfo);
			} else {
				centered = MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_START.append(pageInfo)
						.append(MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_END);
			}
			handleFeedback(context, centered);
		});
	}

	/**
	 * Simple holder for leaderboard entries (UUID, name, level).
	 */
	private final class LeaderboardEntry {
		final UUID uuid;
		final String name;
		final float level;

		LeaderboardEntry(UUID uuid, String name, float level) {
			this.uuid = uuid;
			this.name = name;
			this.level = level;
		}
	}

	@Override
	public String getFeatureID() {
		return "hellblock_top";
	}
}