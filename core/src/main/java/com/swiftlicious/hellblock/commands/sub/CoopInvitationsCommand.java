package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

public class CoopInvitationsCommand extends BukkitCommandFeature<CommandSender> {

	public CoopInvitationsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {

		return builder.senderType(Player.class)
				.optional("page", IntegerParser.integerComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Map<UUID, Long> invites = userOpt.get().getHellblockData().getInvitations();
					long activeCount = invites.values().stream().filter(exp -> exp > System.currentTimeMillis())
							.count();

					int totalPages = (int) Math.max(1, Math.ceil(activeCount / 10.0));
					return CompletableFuture.completedFuture(IntStream.rangeClosed(1, totalPages)
							.mapToObj(i -> Suggestion.suggestion(String.valueOf(i))).toList());
				})).handler(context -> {
					final Player player = context.sender();
					final int page = (int) context.optional("page").orElse(1);

					final Optional<UserData> userOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = userOpt.get();
					final HellblockData data = user.getHellblockData();

					if (data.getInvitations().isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES);
						return;
					}

					HellblockPlugin.getInstance().getCoopManager().listInvitations(user, page);
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_invites";
	}
}