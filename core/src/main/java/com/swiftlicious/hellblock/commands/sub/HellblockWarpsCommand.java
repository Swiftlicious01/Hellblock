package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
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
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager;
import com.swiftlicious.hellblock.gui.visit.VisitGUIManager.VisitSorter;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class HellblockWarpsCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockWarpsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.optional("sort", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// Fallback: show none
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					return CompletableFuture.completedFuture(Arrays.stream(VisitSorter.values())
							.map(value -> value.toString().toLowerCase()).map(Suggestion::suggestion).toList());
				})).handler(context -> {
					Player player = context.sender();
					String sortArg = context.getOrDefault("sort", VisitSorter.FEATURED.toString());

					VisitGUIManager.VisitSorter sorter;

					try {
						sorter = VisitGUIManager.VisitSorter.valueOf(sortArg.toUpperCase(Locale.ENGLISH));
					} catch (IllegalArgumentException e) {
						handleFeedback(context, MessageConstants.COMMAND_INVALID_SORT_TYPE,
								AdventureHelper.miniMessageToComponent(sortArg));
						return;
					}

					final Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					boolean isOwner = false;
					int islandId = -1;

					final UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();
					if (data.hasHellblock()) {
						final UUID ownerUUID = data.getOwnerUUID();
						islandId = data.getIslandId();
						isOwner = ownerUUID != null && data.isOwner(player.getUniqueId());
					}

					boolean success = plugin.getVisitGUIManager().openVisitGUI(player, islandId, sorter, isOwner,
							false);

					if (!success) {
						handleFeedback(context, MessageConstants.COMMAND_VISIT_GUI_FAILED);
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_warps";
	}
}