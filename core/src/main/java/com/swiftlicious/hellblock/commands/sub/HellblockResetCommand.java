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

import lombok.NonNull;

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
					public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(
							@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
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
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>Still loading your player data... please try again in a few seconds.");
						return;
					}
					if (!onlineUser.get().getHellblockData().hasHellblock()) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
										MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
						return;
					}
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
					if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								String.format(
										"<red>You've recently reset your hellblock already, you must wait for %s!",
										HellblockPlugin.getInstance().getFormattedCooldown(
												onlineUser.get().getHellblockData().getResetCooldown())));
						return;
					}
					if (confirmCache.getIfPresent(player.getUniqueId()) != null
							&& confirmCache.getIfPresent(player.getUniqueId())) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
								"<red>You're already in the process of restarting your hellblock, click confirm!");
						return;
					}

					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>Please click confirm to successfully restart your hellblock. <green><bold><click:run_command:/hellblock reset confirm><hover:show_text:'<yellow>Click here to confirm!'>[CONFIRM]</click>");
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
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>Still loading your player data... please try again in a few seconds.");
					return;
				}
				if (!onlineUser.get().getHellblockData().hasHellblock()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
					return;
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.get().getHellblockData().getOwnerUUID() != null
						&& !onlineUser.get().getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
					return;
				}
				if (onlineUser.get().getHellblockData().isAbandoned()) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							HellblockPlugin.getInstance().getTranslationManager()
									.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
					return;
				}
				if (onlineUser.get().getHellblockData().getResetCooldown() > 0) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							String.format("<red>You've recently reset your hellblock already, you must wait for %s!",
									HellblockPlugin.getInstance().getFormattedCooldown(
											onlineUser.get().getHellblockData().getResetCooldown())));
					return;
				}
				if (!confirmCache.getIfPresent(player.getUniqueId())) {
					HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
							"<red>You're not in the process of restarting your hellblock!");
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