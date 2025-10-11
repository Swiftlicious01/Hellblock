package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.swiftlicious.hellblock.player.mailbox.MailboxEntry;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;

import net.kyori.adventure.text.Component;

public class CoopTrustCommand extends BukkitCommandFeature<CommandSender> {

	public CoopTrustCommand(HellblockCommandManager<CommandSender> commandManager) {
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
						if (!(context.sender() instanceof Player player)) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(player.getUniqueId());

						if (onlineUserOpt.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}

						final UserData onlineUser = onlineUserOpt.get();

						// Suggest players who are online, not already trusted/party, and not self
						final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUsers().stream()
								.filter(user -> user != null && user.isOnline()
										&& !user.getHellblockData().getTrusted().contains(onlineUser.getUUID())
										&& !user.getHellblockData().getParty().contains(onlineUser.getUUID())
										&& !user.getName().equalsIgnoreCase(onlineUser.getName()))
								.map(UserData::getName).toList();

						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = onlineUserOpt.get();
					final HellblockData data = user.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						HellblockPlugin.getInstance().getPluginLogger()
								.severe("Hellblock owner UUID was null for player " + player.getName() + " ("
										+ player.getUniqueId() + "). This indicates corrupted data.");
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

					final String targetName = context.get("player");

					if (targetName.equalsIgnoreCase(player.getName())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
						return;
					}

					final UUID targetId = Bukkit.getPlayer(targetName) != null
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

					if (data.getParty().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_PARTY);
						return;
					}

					// Load target user data async
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetId, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
									return;
								}

								final UserData targetUser = result.get();

								if (targetUser.getHellblockData().getTrusted().contains(player.getUniqueId())) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_TRUSTED);
									return;
								}

								// Add trust relationship
								targetUser.getHellblockData().addTrustPermission(player.getUniqueId());

								HellblockPlugin.getInstance().getCoopManager()
										.addTrustAccess(user, targetName, targetId).thenAccept(trust -> {
											// Feedback for executor
											handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TRUST_GIVEN
													.arguments(Component.text(targetName)));

											// Feedback for target if online
											if (targetUser.isOnline()) {
												handleFeedback(Bukkit.getPlayer(targetUser.getUUID()),
														MessageConstants.MSG_HELLBLOCK_COOP_TRUST_GAINED
																.arguments(Component.text(player.getName())));
											} else {
												// else offline
												HellblockPlugin.getInstance().getMailboxManager().queue(
														targetUser.getUUID(),
														new MailboxEntry("message.hellblock.coop.trusted.offline",
																List.of(Component.text(player.getName())),
																Set.of(MailboxFlag.NOTIFY_TRUSTED)));
											}
										}).exceptionally(ex -> {
											HellblockPlugin.getInstance().getPluginLogger().warn(
													"addTrustAccess failed for " + targetName + ": " + ex.getMessage());
											return null;
										});
							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("getOfflineUserData failed for giving trust of " + targetName + ": "
												+ ex.getMessage());
								return null;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_trust";
	}
}
