package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

import net.kyori.adventure.text.Component;

public class HellblockUnbanCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockUnbanCommand(HellblockCommandManager<CommandSender> commandManager) {
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

						final HellblockData data = onlineUserOpt.get().getHellblockData();
						final List<String> suggestions = data.getBanned().stream()
								.map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).filter(Objects::nonNull).toList();

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
						HellblockPlugin.getInstance().getPluginLogger().severe("Hellblock owner UUID was null for player "
								+ player.getName() + " (" + player.getUniqueId() + "). This indicates corrupted data.");
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

					if (data.getParty().contains(targetId) || data.getTrusted().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_TO_PARTY);
						return;
					}

					if (!data.getBanned().contains(targetId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_BANNED);
						return;
					}

					// Perform unban
					data.unbanPlayer(targetId);
					handleFeedback(context,
							MessageConstants.MSG_HELLBLOCK_UNBANNED_PLAYER.arguments(Component.text(targetName)));
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_unban";
	}
}