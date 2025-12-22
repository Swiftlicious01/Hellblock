package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.CoopChatSetting;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class CoopChatCommand extends BukkitCommandFeature<CommandSender> {

	public CoopChatCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.optional("setting", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// No data loaded — show nothing
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					UserData userData = userOpt.get();
					HellblockData data = userData.getHellblockData();

					CoopChatSetting currentSetting = data.getChatSetting();
					boolean hasHellblock = data.hasHellblock();

					List<Suggestion> suggestions = Arrays.stream(CoopChatSetting.values())
							.filter(setting -> setting != currentSetting
									&& (hasHellblock || setting != CoopChatSetting.PARTY))
							.map(value -> value.toString().toLowerCase()).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					Player player = context.sender();

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();

					CoopChatSetting current = data.getChatSetting();
					String inputChoice = context.getOrDefault("setting", null);

					if (inputChoice == null) {
						CoopChatSetting[] values = CoopChatSetting.values();
						int currentIndex = Arrays.asList(values).indexOf(current);

						for (int i = 1; i < values.length; i++) {
							int nextIndex = (currentIndex + i) % values.length;
							CoopChatSetting candidate = values[nextIndex];

							// Skip PARTY if user doesn't have Hellblock
							if (candidate == CoopChatSetting.PARTY && !data.hasHellblock()) {
								continue;
							}

							// Found the next valid setting
							data.setChatSetting(candidate);
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_TOGGLED,
									AdventureHelper.miniMessageToComponent(candidate.name()));
							switch (candidate) {
							case CoopChatSetting.GLOBAL ->
								handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_GLOBAL);
							case CoopChatSetting.LOCAL ->
								handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_LOCAL);
							case CoopChatSetting.PARTY ->
								handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_COOP);
							}
							return;
						}

						// Fallback, in case no valid setting is found (should not happen)
						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_UNCHANGED,
								AdventureHelper.miniMessageToComponent(current.name()));
					}

					// Input provided — try to match a ChatSetting
					try {
						CoopChatSetting requested = CoopChatSetting.valueOf(inputChoice.toUpperCase(Locale.ENGLISH));

						if (current == requested) {
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_UNCHANGED,
									AdventureHelper.miniMessageToComponent(current.name()));
							return;
						}

						if (!data.hasHellblock() && requested == CoopChatSetting.PARTY) {
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_NOT_IN_COOP);
							return;
						}

						data.setChatSetting(requested);
						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_SET,
								AdventureHelper.miniMessageToComponent(requested.name()));
						switch (requested) {
						case CoopChatSetting.GLOBAL ->
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_GLOBAL);
						case CoopChatSetting.LOCAL ->
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_LOCAL);
						case CoopChatSetting.PARTY ->
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_LISTEN_COOP);
						}

					} catch (IllegalArgumentException e) {
						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_CHAT_INVALID);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_chat";
	}
}