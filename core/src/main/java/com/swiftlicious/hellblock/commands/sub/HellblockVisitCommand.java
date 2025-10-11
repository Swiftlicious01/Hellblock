package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
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
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class HellblockVisitCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockVisitCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider(this::suggestPlayers))
				.handler(context -> {
					final Player visitor = context.sender();
					final Optional<UserData> visitorOpt = HellblockPlugin.getInstance().getStorageManager()
							.getOnlineUser(visitor.getUniqueId());

					if (visitorOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final String targetName = context.get("player");
					final UUID targetUUID = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetUUID == null || !Bukkit.getOfflinePlayer(targetUUID).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					// Step 1: Check if banned
					HellblockPlugin.getInstance().getCoopManager().trackBannedPlayer(targetUUID, visitor.getUniqueId())
							.thenAccept(banned -> {
								if (banned) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY);
									return;
								}

								// Step 2: Load target’s island
								loadTargetIsland(visitor, visitorOpt.get(), targetUUID, targetName, context);
							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("trackBannedPlayer failed for " + targetName + ": " + ex.getMessage());
								return null;
							});
				});
	}

	private void loadTargetIsland(Player visitor, UserData visitorData, UUID targetUUID, String targetName,
			CommandContext<Player> context) {
		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(targetUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept(targetOpt -> {
					if (targetOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
								.arguments(Component.text(targetName)));
						return;
					}

					final UserData targetUser = targetOpt.get();

					// Visiting own or party island
					if (targetUUID.equals(visitor.getUniqueId())
							|| targetUser.getHellblockData().getParty().contains(visitor.getUniqueId())) {
						visitOwnOrPartyIsland(visitor, visitorData, context);
						return;
					}

					// Visiting someone else’s island
					if (targetUser.getHellblockData().hasHellblock()) {
						visitIsland(visitor, targetUser, context);
					} else {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
					}
				}).exceptionally(ex -> {
					HellblockPlugin.getInstance().getPluginLogger()
							.warn("getOfflineUserData failed for visiting island of " + visitor.getName() + ": "
									+ ex.getMessage());
					return null;
				});
	}

	private void visitOwnOrPartyIsland(Player visitor, UserData visitorData, CommandContext<Player> context) {
		final HellblockData data = visitorData.getHellblockData();
		if (!data.hasHellblock()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
			return;
		}

		final UUID ownerUUID = data.getOwnerUUID();
		if (ownerUUID == null) {
			HellblockPlugin.getInstance().getPluginLogger().severe("Null owner UUID in visitOwnOrPartyIsland for "
					+ visitor.getName() + " (" + visitor.getUniqueId() + ")");
			throw new IllegalStateException("Owner reference was null in visitOwnOrPartyIsland.");
		}

		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept(ownerOpt -> {
					if (ownerOpt.isEmpty()) {
						final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
								.arguments(Component.text(username != null ? username : "???")));
						return;
					}

					final UserData ownerUser = ownerOpt.get();
					if (ownerUser.getHellblockData().isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					HellblockPlugin.getInstance().getVisitManager().handleVisit(visitor, ownerUUID);
				});
	}

	private void visitIsland(Player visitor, UserData targetUser, CommandContext<Player> context) {
		final HellblockData targetData = targetUser.getHellblockData();
		final UUID ownerUUID = targetData.getOwnerUUID();
		if (ownerUUID == null) {
			HellblockPlugin.getInstance().getPluginLogger().severe("Null owner UUID in external visit for "
					+ targetUser.getName() + " (" + targetUser.getUUID() + ")");
			throw new IllegalStateException("Owner reference was null in visitIsland.");
		}

		HellblockPlugin.getInstance().getStorageManager()
				.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
				.thenAccept(ownerOpt -> {
					if (ownerOpt.isEmpty()) {
						final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
								.arguments(Component.text(username != null ? username : "???")));
						return;
					}

					final UserData ownerUser = ownerOpt.get();
					final HellblockData ownerData = ownerUser.getHellblockData();

					if (ownerData.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISIT_ABANDONED);
						return;
					}

					HellblockPlugin.getInstance().getCoopManager()
							.checkIfVisitorsAreWelcome(visitor, ownerUser.getUUID()).thenAccept(status -> {
								if (ownerData.isLocked() || !status) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS
											.arguments(Component.text(targetUser.getName())));
									return;
								}

								HellblockPlugin.getInstance().getVisitManager().handleVisit(visitor, ownerUUID);
							});
				});
	}

	private @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestPlayers(
			@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
		if (!(context.sender() instanceof Player player)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		final Optional<UserData> visitorOpt = HellblockPlugin.getInstance().getStorageManager()
				.getOnlineUser(player.getUniqueId());

		if (visitorOpt.isEmpty()) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
				.filter(user -> user != null && user.isOnline() && user.getHellblockData().hasHellblock()
						&& user.getHellblockData().getHomeLocation() != null
						&& !visitorOpt.get().getHellblockData().getParty().contains(user.getUUID())
						&& !user.getName().equalsIgnoreCase(visitorOpt.get().getName()))
				.map(UserData::getName).toList();

		return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visit";
	}
}