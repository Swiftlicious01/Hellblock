package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class CoopInviteCommand extends BukkitCommandFeature<CommandSender> {

	public CoopInviteCommand(HellblockCommandManager<CommandSender> commandManager) {
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
					// Suggest all *known* players, not just online ones
					List<Suggestion> suggestions = Stream.concat(
							HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
									.map(UserData::getName),
							Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName)
									.filter(Objects::nonNull))
							.distinct().map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
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

					// Must own a Hellblock
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

					// Target
					final String targetName = context.get("player");

					// Offline UUID resolution
					final UUID targetId = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetId == null || !Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					// Prevent self-invite
					if (targetId.equals(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					// Check party
					if (senderData.getParty().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY,
								Component.text(targetName));
						return;
					}

					if (senderData.getParty().size() >= HellblockPlugin.getInstance().getCoopManager()
							.getMaxPartySize(sender)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL);
						return;
					}

					// Async load for offline target
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetId, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(targetOpt -> {
								if (targetOpt.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
									return;
								}

								final UserData targetUser = targetOpt.get();

								if (targetUser.getHellblockData().hasInvite(player.getUniqueId())) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS);
									return;
								}

								// Send invite (now handles offline)
								HellblockPlugin.getInstance().getCoopManager().sendInvite(sender, targetUser);
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_invite";
	}
}