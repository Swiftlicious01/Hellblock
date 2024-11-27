package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.Command.Builder;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

import org.jetbrains.annotations.NotNull;

public class HellblockResetCommand extends BukkitCommandFeature<CommandSender> {

	private final Cache<UUID, Boolean> confirmCache = Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS)
			.build();

	public HellblockResetCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.optional("confirm", StringParser.stringComponent().suggestionProvider(new SuggestionProvider<>() {
					@Override
					public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(
							@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
						if (context.sender() instanceof Player player) {
							if (confirmCache.getIfPresent(player.getUniqueId()) != null
									&& confirmCache.getIfPresent(player.getUniqueId())) {
								return CompletableFuture.completedFuture(
										Arrays.asList("confirm").stream().map(Suggestion::suggestion).toList());
							}
							return CompletableFuture.completedFuture(Collections.emptyList());
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
					}
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
					if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
						handleFeedback(context,
								MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN
										.arguments(Component.text(HellblockPlugin.getInstance().getFormattedCooldown(
												onlineUser.get().getHellblockData().getResetCooldown()))));
						return;
					}
					if (confirmCache.getIfPresent(player.getUniqueId()) != null
							&& confirmCache.getIfPresent(player.getUniqueId())) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_IN_PROCESS);
						return;
					}

					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_CONFIRMATION);
					confirmCache.put(player.getUniqueId(), true);
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_reset";
	}

	public class HellblockResetConfirmCommand extends BukkitCommandFeature<CommandSender> {

		public HellblockResetConfirmCommand(HellblockCommandManager<CommandSender> commandManager) {
			super(commandManager);
		}

		@Override
		public Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
				Builder<CommandSender> builder) {
			return builder.senderType(Player.class).handler(context -> {
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
				}
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
				if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN
							.arguments(Component.text(HellblockPlugin.getInstance()
									.getFormattedCooldown(onlineUser.get().getHellblockData().getResetCooldown()))));
					return;
				}
				if (!confirmCache.getIfPresent(player.getUniqueId())) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_NOT_IN_PROCESS);
					return;
				}

				HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false);
			});
		}

		@Override
		public String getFeatureID() {
			return "hellblock_reset_confirm";
		}
	}
}