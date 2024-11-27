package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
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

public class AdminDeleteCommand extends BukkitCommandFeature<CommandSender> {

	public AdminDeleteCommand(HellblockCommandManager<CommandSender> commandManager) {
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
						List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers()
								.stream()
								.filter(onlineUser -> onlineUser.isOnline()
										&& onlineUser.getHellblockData().hasHellblock())
								.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
						return CompletableFuture
								.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
					}
				})).handler(context -> {
					final Player player = context.sender();
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
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept((result) -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(user)));
									return;
								}
								UserData offlineUser = result.get();
								if (offlineUser.getHellblockData().hasHellblock()) {
									HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true)
											.thenRun(() -> {
												HellblockPlugin.getInstance()
														.debug(String.format(
																"%s's hellblock has been forcefully deleted by %s.",
																user, player.getName()));
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_ADMIN_ISLAND_DELETED
																.arguments(Component.text(user)));
											});
									return;
								}

								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
								return;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_delete";
	}
}