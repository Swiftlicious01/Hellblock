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

import lombok.NonNull;

public class CoopCancelCommand extends BukkitCommandFeature<CommandSender> {

	public CoopCancelCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
							@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(onlineUser -> onlineUser.isOnline()
											&& onlineUser.getHellblockData().getInvitations() != null
											&& onlineUser.getHellblockData().getInvitations().keySet()
													.contains(player.getUniqueId())
											&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
									.map(hbPlayer -> hbPlayer.getPlayer().getName()).collect(Collectors.toList());
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						String user = context.get("player");
						if (user.equalsIgnoreCase(player.getName())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>You can't do this to yourself!");
							return;
						}
						UUID id = Bukkit.getPlayer(user) != null ? Bukkit.getPlayer(user).getUniqueId()
								: UUIDFetcher.getUUID(user);
						if (id == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>The player you're trying to cancel an invite from doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>The player you're trying to cancel an invite from doesn't exist!");
							return;
						}
						HellblockPlugin.getInstance().getStorageManager()
								.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
								.thenAccept((result) -> {
									UserData offlineUser = result.orElseThrow();
									if (offlineUser.getHellblockData().getInvitations() == null) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessage(
												player, "<red>This player doesn't have an invite from you!");
										return;
									}
									if (!offlineUser.getHellblockData().hasInvite(player.getUniqueId())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessage(
												player, "<red>This player doesn't have an invite from you!");
										return;
									}
									if (offlineUser.getHellblockData().hasInviteExpired(player.getUniqueId())) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessage(
												player, "<red>Your invitation already expired for this player!");
										return;
									}
									offlineUser.getHellblockData().removeInvitation(player.getUniqueId());
									HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
											"<red>You've cancelled your invitation to <dark_red>" + user + "<red>!");
									if (Bukkit.getPlayer(id) != null) {
										HellblockPlugin.getInstance().getAdventureManager().sendMessage(
												Bukkit.getPlayer(id), "<dark_red>" + player.getName()
														+ " <red> has revoked their invitation to you!");
									}
								});
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
										MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_cancel";
	}
}