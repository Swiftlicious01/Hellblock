package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

public class CoopKickCommand extends BukkitCommandFeature<CommandSender> {

	public CoopKickCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							return CompletableFuture.completedFuture(HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(onlineUser -> onlineUser.isOnline()
											&& onlineUser.getHellblockData().hasHellblock()
											&& onlineUser.getHellblockData().getOwnerUUID() != null
											&& onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())
											&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
									.map(UserData::getName).map(Suggestion::suggestion).toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> senderOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (senderOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData sender = senderOpt.get();
					final HellblockData senderData = sender.getHellblockData();

					// Must have a Hellblock
					if (!senderData.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					// Must be the owner
					final UUID ownerUUID = senderData.getOwnerUUID();
					if (ownerUUID == null) {
						HellblockPlugin.getInstance().getPluginLogger()
								.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
										+ player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!senderData.isOwner(ownerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (senderData.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					// Resolve target
					final String targetName = context.get("player");
					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					final UUID targetId = (Bukkit.getPlayer(targetName) != null)
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					// Must be in the party
					if (!senderData.getParty().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY);
						return;
					}

					// Remove from party
					HellblockPlugin.getInstance().getCoopManager().removeMemberFromHellblock(sender, targetName,
							targetId);
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_kick";
	}
}