package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class CoopOwnerCommand extends BukkitCommandFeature<CommandSender> {

	public CoopOwnerCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", PlayerParser.playerComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							List<String> suggestions = HellblockPlugin.getInstance().getStorageManager()
									.getOnlineUsers().stream()
									.filter(onlineUser -> onlineUser.isOnline()
											&& onlineUser.getHellblockData().hasHellblock()
											&& onlineUser.getHellblockData().getOwnerUUID() != null
											&& onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())
											&& !onlineUser.getName().equalsIgnoreCase(player.getName()))
									.map(onlineUser -> onlineUser.getName()).collect(Collectors.toList());
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
					if (!HellblockPlugin.getInstance().getConfigManager().transferIslands()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED);
						return;
					}
					if (onlineUser.get().getHellblockData().hasHellblock()) {
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
						if (onlineUser.get().getHellblockData().getTransferCooldown() > 0) {
							handleFeedback(context,
									MessageConstants.MSG_HELLBLOCK_TRANSFER_ON_COOLDOWN.arguments(
											Component.text(HellblockPlugin.getInstance().getFormattedCooldown(
													onlineUser.get().getHellblockData().getTransferCooldown()))));
							return;
						}
						Player user = context.get("player");
						if (user == null || !user.isOnline()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						if (user.getUniqueId().equals(player.getUniqueId())) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_SELF);
							return;
						}
						Optional<UserData> transferPlayer = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUser(user.getUniqueId());
						if (transferPlayer.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
									.arguments(Component.text(user.getName())));
							return;
						}
						HellblockPlugin.getInstance().getCoopManager().transferOwnershipOfHellblock(onlineUser.get(),
								transferPlayer.get());
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}
				});
	}

	@Override
	public String getFeatureID() {
		return "coop_owner";
	}
}