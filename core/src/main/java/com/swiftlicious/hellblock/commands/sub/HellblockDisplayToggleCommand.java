package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
import com.swiftlicious.hellblock.player.DisplaySettings;
import com.swiftlicious.hellblock.player.DisplaySettings.DisplayChoice;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockDisplayToggleCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockDisplayToggleCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.optional("choice", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// No data loaded — show nothing
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();

					if (!data.hasHellblock()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (!data.isOwner(ownerUUID)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.isAbandoned()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					DisplayChoice current = data.getDisplaySettings().getDisplayChoice();

					List<Suggestion> suggestions = Arrays.stream(DisplayChoice.values())
							.filter(choice -> choice != current).map(Enum::name).map(Suggestion::suggestion).toList();

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

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					// Must be the owner
					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!data.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					DisplaySettings displaySettings = data.getDisplaySettings();
					DisplayChoice current = displaySettings.getDisplayChoice();
					String inputChoice = context.getOrDefault("choice", null);

					if (inputChoice == null) {
						// Toggle between CHAT and TITLE
						DisplayChoice newChoice = (current == DisplayChoice.CHAT) ? DisplayChoice.TITLE
								: DisplayChoice.CHAT;

						displaySettings.setDisplayChoice(newChoice);

						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_DISPLAY_TOGGLED,
								AdventureHelper.miniMessageToComponent(newChoice.name()));
						return;
					}

					// Input provided — try to match a DisplayChoice
					try {
						DisplayChoice requested = DisplayChoice.valueOf(inputChoice.toUpperCase(Locale.ENGLISH));

						if (current == requested) {
							handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_DISPLAY_UNCHANGED,
									AdventureHelper.miniMessageToComponent(current.name()));
							return;
						}

						displaySettings.setDisplayChoice(requested);
						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_DISPLAY_SET,
								AdventureHelper.miniMessageToComponent(requested.name()));

					} catch (IllegalArgumentException e) {
						handleFeedback(context, MessageConstants.COMMAND_HELLBLOCK_DISPLAY_INVALID);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_display_toggle";
	}
}