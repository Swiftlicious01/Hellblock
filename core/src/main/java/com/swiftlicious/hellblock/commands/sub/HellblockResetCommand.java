package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

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
				.optional("confirm", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (context.sender() instanceof Player player
							&& Boolean.TRUE.equals(confirmCache.getIfPresent(player.getUniqueId()))) {
						// Only show “confirm” as a tab-completion if pending confirmation
						return CompletableFuture.completedFuture(List.of(Suggestion.suggestion("confirm")));
					}
					return CompletableFuture.completedFuture(Collections.emptyList());
				})).handler(context -> {
					final Player player = context.sender();

					if (!canReset(context)) {
						return;
					}

					final Optional<String> confirmArg = context.optional("confirm");
					final boolean isConfirming = confirmArg.map(arg -> "confirm".equalsIgnoreCase(arg)).orElse(false);

					// if they typed “confirm”
					if (isConfirming) {
						if (!Boolean.TRUE.equals(confirmCache.getIfPresent(player.getUniqueId()))) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_NOT_IN_PROCESS);
							return;
						}

						HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(player.getUniqueId(), false,
								null);
						confirmCache.invalidate(player.getUniqueId());
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_IN_PROCESS);
						return;
					}

					// if no “confirm” argument
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_CONFIRMATION);
					confirmCache.put(player.getUniqueId(), true);
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_reset";
	}

	private boolean canReset(CommandContext<Player> context) {
		final Player player = context.sender();
		final Optional<UserData> onlineUserOpt = HellblockPlugin.getInstance().getStorageManager()
				.getOnlineUser(player.getUniqueId());

		if (onlineUserOpt.isEmpty()) {
			handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
			return false;
		}

		final UserData user = onlineUserOpt.get();
		final HellblockData data = user.getHellblockData();

		if (!data.hasHellblock()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
			return false;
		}

		final UUID ownerUUID = data.getOwnerUUID();
		if (ownerUUID == null) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("Hellblock owner UUID was null for player " + player.getName() + " (" + player.getUniqueId()
							+ "). This indicates corrupted data or a serious bug.");
			throw new IllegalStateException("Owner reference was null — please report to the developer.");
		}

		if (!data.isOwner(ownerUUID)) {
			handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
			return false;
		}

		if (data.isAbandoned()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
			return false;
		}

		if (data.getResetCooldown() > 0) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_RESET_ON_COOLDOWN.arguments(
					Component.text(HellblockPlugin.getInstance().getFormattedCooldown(data.getResetCooldown()))));
			return false;
		}

		return data.getResetCooldown() <= 0;
	}
}