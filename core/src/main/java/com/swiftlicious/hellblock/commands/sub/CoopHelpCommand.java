package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class CoopHelpCommand extends BukkitCommandFeature<CommandSender> {

	private static final int ENTRIES_PER_PAGE = 7;

	public CoopHelpCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.optional("page", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final Optional<UserData> onlineUserOpt = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UserData userData = onlineUserOpt.get();
					final HellblockData data = userData.getHellblockData();

					if (!data.hasHellblock()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					List<String> lines = getCoopHelpLines();
					int totalPages = Math.max(1, (int) Math.ceil(lines.size() / (double) ENTRIES_PER_PAGE));

					// Suggest all pages, or maybe filter by input
					String userInput = input.peekString();

					List<Suggestion> suggestions = IntStream.rangeClosed(1, totalPages).mapToObj(String::valueOf)
							.filter(s -> userInput.isEmpty() || s.startsWith(userInput)).map(Suggestion::suggestion)
							.toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final Player player = context.sender();

					final Optional<UserData> onlineUserOpt = plugin.getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData onlineUser = onlineUserOpt.get();
					final HellblockData data = onlineUser.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TUTORIAL);
						return;
					}

					int page;
					// Extract the page argument as a string (if present), or default to "1"
					String pageInput = context.<String>optional("page").orElse("1");
					try {
						page = Integer.parseInt(pageInput);
					} catch (NumberFormatException e) {
						int totalPages = Math.max(1,
								(int) Math.ceil(getCoopHelpLines().size() / (double) ENTRIES_PER_PAGE));
						handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
								AdventureHelper.miniMessageToComponent(pageInput),
								AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
						return;
					}

					List<String> lines = getCoopHelpLines();
					int totalPages = Math.max(1, (int) Math.ceil(lines.size() / (double) ENTRIES_PER_PAGE));

					if (page < 1 || page > totalPages) {
						handleFeedback(context, MessageConstants.COMMAND_INVALID_PAGE_ARGUMENT,
								AdventureHelper.miniMessageToComponent(String.valueOf(page)),
								AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));
						return;
					}

					int start = (page - 1) * ENTRIES_PER_PAGE;
					int end = Math.min(start + ENTRIES_PER_PAGE, lines.size());

					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_HELP_HEADER,
							AdventureHelper.miniMessageToComponent(String.valueOf(page)),
							AdventureHelper.miniMessageToComponent(String.valueOf(totalPages)));

					for (int i = start; i < end; i++) {
						handleFeedbackRaw(context, AdventureHelper.miniMessageToComponent(lines.get(i)));
					}
				});
	}

	@NotNull
	private List<String> getCoopHelpLines() {
		return plugin.getTranslationManager().getRawStringList(MessageConstants.MSG_HELLBLOCK_COOP_HELP.build().key());
	}

	@Override
	public String getFeatureID() {
		return "coop_help";
	}
}