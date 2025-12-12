package com.swiftlicious.hellblock.commands.sub;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
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

	// Locale-aware number formatter for displaying levels (2 decimal max, grouped)
	private final NumberFormat levelFormat;

	public HellblockTopCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);

		this.levelFormat = NumberFormat.getNumberInstance(plugin.getTranslationManager().getForcedLocale());
		this.levelFormat.setMaximumFractionDigits(2);
		this.levelFormat.setMinimumFractionDigits(0);
		this.levelFormat.setGroupingUsed(true);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class) // only players can run this
				// optional "page" argument with suggestions
				.optional("page", StringParser.stringComponent().suggestionProvider((context, input) -> {
					// if sender isn't a player, return empty suggestions
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					// Use the same async top call we use in showHellblockTop.
					// We map the returned top map -> page numbers and transform those to Suggestion
					// objects.
					final CompletableFuture<LinkedHashMap<Integer, Float>> topFuture = plugin.getIslandLevelManager()
							.getTopHellblocks(1000);

					// Transform the future into a suggestions future
					return topFuture.thenApply(top -> {
						if (top == null || top.isEmpty()) {
							return Collections.emptyList();
						}

						// only count entries with level > 1
						final long totalValid = top.values().stream().filter(Objects::nonNull)
								.filter(v -> v > HellblockData.DEFAULT_LEVEL).count();

						final int totalPages = (int) Math.max(1, Math.ceil((double) totalValid / pageSize));
						final List<Suggestion> suggestions = new ArrayList<>(totalPages);
						for (int i = 1; i <= totalPages; i++) {
							suggestions.add(Suggestion.suggestion(String.valueOf(i)));
						}
						return suggestions;
					});
				}))
				// Handler executed when the command runs
				.handler(context -> {
					final Player player = context.sender();

					// Ensure user data is loaded
					final Optional<UserData> onlineUser = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					// Fetch leaderboard async first
					plugin.getIslandLevelManager().getTopHellblocks(1000).thenCompose(topBoard -> {
						if (topBoard == null || topBoard.isEmpty()) {
							return CompletableFuture.completedFuture(List.of());
						}

						// Build async futures to resolve names for each island
						List<CompletableFuture<LeaderboardEntry>> futures = topBoard.entrySet().stream()
								.filter(e -> e.getValue() > HellblockData.DEFAULT_LEVEL).map(e -> {
									int islandId = e.getKey();
									float level = e.getValue();

									return plugin.getCoopManager().getOwnerUserDataByIslandId(islandId)
											.thenApply(userOpt -> {
												if (userOpt.isEmpty())
													return null;

												UserData ownerData = userOpt.get();
												OfflinePlayer offlineOwner = Bukkit
														.getOfflinePlayer(ownerData.getUUID());
												String name = offlineOwner.getName();

												if (!offlineOwner.hasPlayedBefore() || name == null)
													return null;
												return new LeaderboardEntry(islandId, name, level);
											});
								}).toList();

						// Combine all futures into one
						return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(
								v -> futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList());

					}).thenAcceptAsync(entries -> {
						if (entries.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_FOUND);
							return;
						}

						// Parse the page argument and show the leaderboard
						int page;
						String pageInput = context.<String>optional("page").orElse("1");
						try {
							page = Integer.parseInt(pageInput);
						} catch (NumberFormatException e) {
							int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / pageSize));
							handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
									AdventureHelper.miniMessageToComponent(pageInput),
									AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
							return;
						}

						List<LeaderboardEntry> typedEntries = entries.stream()
								.filter(LeaderboardEntry.class::isInstance).map(LeaderboardEntry.class::cast).toList();

						showHellblockTop(context, page, typedEntries);
					});
				});
	}

	/**
	 * Displays the paginated Hellblock leaderboard to the player.
	 *
	 * @param context Command execution context
	 * @param page    Page number to display (1-based index)
	 * @param entries the entries to sort through
	 */
	private void showHellblockTop(@NotNull CommandContext<?> context, int page,
			@Nullable List<LeaderboardEntry> entries) {
		if (entries == null || entries.isEmpty()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_NOT_FOUND);
			return;
		}

		// Sort: level DESC, name ASC, island id fallback
		entries.sort((a, b) -> {
			int cmp = Float.compare(b.level, a.level);
			if (cmp != 0) {
				return cmp;
			}
			cmp = a.name.compareToIgnoreCase(b.name);
			return cmp != 0 ? cmp : Integer.compare(a.islandId, b.islandId);
		});

		final int totalEntries = entries.size();
		final int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / pageSize));
		if (page < 1 || page > totalPages) {
			handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
					AdventureHelper.miniMessageToComponent(String.valueOf(page)),
					AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
			return;
		}

		final int startIndex = (page - 1) * pageSize;
		final int endIndex = Math.min(startIndex + pageSize, totalEntries);

		handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_HEADER,
				AdventureHelper.miniMessageToComponent(String.valueOf(page)),
				AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));

		final UUID senderUuid = (context.sender() instanceof Player p) ? p.getUniqueId() : null;

		int senderIslandId = -1;
		if (senderUuid != null) {
			// Find the owner of the island the sender belongs to
			// safe here because it's already async
			Optional<UserData> ownerData = plugin.getCoopManager().getCachedIslandOwnerData().join().stream()
					.filter(Objects::nonNull).filter(userData -> {
						if (!userData.getHellblockData().hasHellblock() && !userData.getHellblockData().isAbandoned())
							return false;
						int id = userData.getHellblockData().getIslandId();
						return id != -1 && userData.getHellblockData().getPartyPlusOwner().contains(senderUuid);
					}).findFirst();

			if (ownerData.isPresent()) {
				senderIslandId = ownerData.get().getHellblockData().getIslandId();
			}
		}

		for (int i = startIndex; i < endIndex; i++) {
			final LeaderboardEntry le = entries.get(i);
			final int rank = i + 1;
			final String formattedLevel = levelFormat.format(le.level);

			Component rankComponent = AdventureHelper.miniMessageToComponent(String.valueOf(rank));
			Component nameComponent = AdventureHelper.miniMessageToComponent(le.name);
			Component levelComponent = AdventureHelper.miniMessageToComponent(formattedLevel);

			final boolean isSelf = senderIslandId != -1 && senderIslandId == le.islandId;
			if (isSelf) {
				rankComponent = rankComponent.decorate(TextDecoration.BOLD);
				nameComponent = nameComponent.decorate(TextDecoration.BOLD);
			}

			final TranslatableComponent.Builder messageKey = switch (rank) {
			case 1 -> MessageConstants.MSG_HELLBLOCK_TOP_RANK_1;
			case 2 -> MessageConstants.MSG_HELLBLOCK_TOP_RANK_2;
			case 3 -> MessageConstants.MSG_HELLBLOCK_TOP_RANK_3;
			default -> MessageConstants.MSG_HELLBLOCK_TOP_FORMAT;
			};

			handleFeedback(context, messageKey, rankComponent, nameComponent, levelComponent);
		}

		// --- Extract base command
		final String fullInput = context.rawInput().input();
		final String[] parts = fullInput.split(" ");
		final String baseCommand = parts.length >= 2 ? "/" + parts[0] + " " + parts[1] : "/" + parts[0];

		// --- Sender rank summary
		int selfIndex = -1;
		if (senderUuid != null && senderIslandId != -1) {
			for (int i = 0; i < entries.size(); i++) {
				if (entries.get(i).islandId == senderIslandId) {
					selfIndex = i + 1;
					break;
				}
			}
		}

		if (selfIndex != -1) {
			final LeaderboardEntry selfEntry = entries.get(selfIndex - 1);
			final String formattedLevel = levelFormat.format(selfEntry.level);
			final int selfPage = (selfIndex - 1) / pageSize + 1;

			final Builder viewYourPageClickable = MessageConstants.MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE
					.clickEvent(ClickEvent.runCommand(baseCommand + " " + selfPage))
					.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_VIEW_YOUR_PAGE_HOVER));

			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_TOP_SELF_SUMMARY,
					AdventureHelper.miniMessageToComponent(String.valueOf(selfIndex)), AdventureHelper.miniMessageToComponent(formattedLevel),
					viewYourPageClickable.build());
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

		final Builder pageInfo = MessageConstants.MSG_HELLBLOCK_TOP_PAGE_INFO
				.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(page)),
						AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)))
				.clickEvent(ClickEvent.suggestCommand(baseCommand + " "))
				.hoverEvent(HoverEvent.showText(MessageConstants.MSG_HELLBLOCK_TOP_PAGE_INFO_HOVER));

		final Builder centered = nav.equals(Component.empty())
				? MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_START.append(pageInfo)
						.append(MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_END)
				: MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_START.append(nav)
						.append(MessageConstants.MSG_HELLBLOCK_TOP_NAV_WRAPPER_END).append(pageInfo);

		handleFeedback(context, centered);
	}

	/**
	 * Simple holder for leaderboard entries (island id, name, level).
	 */
	private final class LeaderboardEntry {
		final int islandId;
		final String name;
		final float level;

		LeaderboardEntry(int islandId, String name, float level) {
			this.islandId = islandId;
			this.name = name;
			this.level = level;
		}
	}

	@Override
	public String getFeatureID() {
		return "hellblock_top";
	}
}