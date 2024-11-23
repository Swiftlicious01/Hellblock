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
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import lombok.NonNull;

public class CoopRejectCommand extends BukkitCommandFeature<CommandSender> {

	public CoopRejectCommand(HellblockCommandManager<CommandSender> commandManager) {
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
							Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUser(player.getUniqueId());
							if (onlineUser.isEmpty())
								return CompletableFuture.completedFuture(Collections.emptyList());
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(user -> user != null && user.isOnline()
											&& !user.getHellblockData().hasHellblock()
											&& user.getHellblockData().getInvitations() != null
											&& user.getName().equalsIgnoreCase(player.getName()))
									.findFirst().orElse(onlineUser.get()).getHellblockData().getInvitations().keySet()
									.stream().map(id -> (Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
											: Bukkit.getOfflinePlayer(id).getName()))
									.collect(Collectors.toList());
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
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
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
									"<red>The player you're trying to decline an invite from doesn't exist!");
							return;
						}
						if (!Bukkit.getOfflinePlayer(id).hasPlayedBefore()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>The player you're trying to decline an invite from doesn't exist!");
							return;
						}
						if (onlineUser.get().getHellblockData().getInvitations() == null) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (!onlineUser.get().getHellblockData().hasInvite(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>You don't have an invite from this player!");
							return;
						}
						if (onlineUser.get().getHellblockData().hasInviteExpired(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Your invitation from this player has expired!");
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().rejectInvite(id, onlineUser.get());
					} else {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>You already have a hellblock!");
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_reject";
	}
}