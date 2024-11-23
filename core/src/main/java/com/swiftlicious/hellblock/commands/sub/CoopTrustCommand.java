package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import lombok.NonNull;

public class CoopTrustCommand extends BukkitCommandFeature<CommandSender> {

	public CoopTrustCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", PlayerParser.playerComponent().suggestionProvider(new SuggestionProvider<>() {
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
									.filter(user -> user.isOnline()
											&& !user.getHellblockData().getTrusted()
													.contains(onlineUser.get().getUUID())
											&& !user.getHellblockData().getParty().contains(onlineUser.get().getUUID())
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
						if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}
						if (onlineUser.get().getHellblockData().getOwnerUUID() != null
								&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
											MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
							return;
						}
						if (onlineUser.get().getHellblockData().isAbandoned()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
											MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
							return;
						}
						Player user = context.get("player");
						if (user == null || !user.isOnline()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>The player you entered is either not online or doesn't exist!");
							return;
						}
						UUID id = user.getUniqueId();
						if (id.equals(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>You can't do this to yourself!");
							return;
						}
						if (onlineUser.get().getHellblockData().getParty().contains(id)) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>The player you're trying to apply this to is already a member of your party!");
							return;
						}
						Optional<UserData> trustedPlayer = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(user.getUniqueId());
						if (trustedPlayer.isEmpty()) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<red>Still loading %s's data... please try again in a few seconds.",
											user.getName()));
							return;
						}
						if (trustedPlayer.get().getHellblockData().getTrusted().contains(player.getUniqueId())) {
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>This player is already trusted on your hellblock!");
							return;
						}
						trustedPlayer.get().getHellblockData().addTrustPermission(player.getUniqueId());
						HellblockPlugin.getInstance().getCoopManager().addTrustAccess(onlineUser.get(), user.getName(),
								id);
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								String.format("<red>You've given trust access to <dark_red>%s <red>on your hellblock!",
										user.getName()));
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(user,
								String.format("<red>You've been given trust access to <dark_red>%s<red>'s hellblock!",
										player.getName()));
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
		return "coop_trust";
	}
}
