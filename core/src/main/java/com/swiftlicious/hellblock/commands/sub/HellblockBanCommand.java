package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class HellblockBanCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockBanCommand(HellblockCommandManager<CommandSender> commandManager) {
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
							Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty())
								return CompletableFuture.completedFuture(Collections.emptyList());
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(user -> user != null && user.isOnline()
											&& !onlineUser.get().getHellblockData().getTrusted()
													.contains(user.getUUID())
											&& !onlineUser.get().getHellblockData().getParty().contains(user.getUUID())
											&& !user.getName().equalsIgnoreCase(onlineUser.get().getName()))
									.map(user -> user.getName()).collect(Collectors.toList());
							return CompletableFuture
									.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
						}
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})).handler(context -> {
					final Player player = context.sender();
					Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(player.getUniqueId());
					if (onlineUser.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					} else {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
							return;
						}
						String user = context.get("player");
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						if (id.equals(player.getUniqueId())) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
							return;
						}
						if (onlineUser.get().getHellblockData().getParty().contains(id)
								|| onlineUser.get().getHellblockData().getTrusted().contains(id)) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_PARTY);
							return;
						}
						if (onlineUser.get().getHellblockData().getBanned().contains(id)) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ALREADY_BANNED);
							return;
						}

						onlineUser.get().getHellblockData().banPlayer(id);
						if (Bukkit.getPlayer(user) != null) {
							if (HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(player.getUniqueId(),
									id)) {
								Optional<UserData> bannedPlayer = HellblockPlugin.getInstance().getStorageManager()
										.getOnlineUser(id);
								if (bannedPlayer.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(user)));
									return;
								}
								if (bannedPlayer.get().getHellblockData().hasHellblock()) {
									if (bannedPlayer.get().getHellblockData().getOwnerUUID() == null) {
										throw new NullPointerException(
												"Owner reference returned null, please report this to the developer.");
									}
									HellblockPlugin.getInstance().getStorageManager()
											.getOfflineUserData(bannedPlayer.get().getHellblockData().getOwnerUUID(),
													HellblockPlugin.getInstance().getConfigManager().lockData())
											.thenAccept((owner) -> {
												if (owner.isEmpty()) {
													String username = Bukkit.getOfflinePlayer(
															bannedPlayer.get().getHellblockData().getOwnerUUID())
															.getName() != null
																	? Bukkit.getOfflinePlayer(bannedPlayer.get()
																			.getHellblockData().getOwnerUUID())
																			.getName()
																	: "???";
													handleFeedback(context,
															MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																	.arguments(Component.text(username)));
													return;
												}
												UserData bannedOwner = owner.get();
												HellblockPlugin.getInstance().getCoopManager()
														.makeHomeLocationSafe(bannedOwner, bannedPlayer.get());
											}).thenRun(() -> {
												handleFeedback(Bukkit.getPlayer(user), MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY);
											});
								} else {
									HellblockPlugin.getInstance().getHellblockHandler()
											.teleportToSpawn(Bukkit.getPlayer(user), true);

								}
							}
						}
						handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_BANNED_PLAYER.arguments(Component.text(user)));
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_ban";
	}
}