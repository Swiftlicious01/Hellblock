package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

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
					final Optional<UserData> visitorOpt = plugin.getStorageManager()
							.getOnlineUser(visitor.getUniqueId());

					if (visitorOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final String targetName = context.get("player");
					UUID targetId;

					Player onlinePlayer = Bukkit.getPlayer(targetName);
					if (onlinePlayer != null) {
						targetId = onlinePlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(targetName);
						if (fetchedId.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
							return;
						}
						targetId = fetchedId.get();
					}

					if (!Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					loadTargetIsland(visitor, visitorOpt.get(), targetId, targetName, context);
				});
	}

	private void loadTargetIsland(@NotNull Player visitor, @NotNull UserData visitorData, @NotNull UUID targetUUID,
			@NotNull String targetName, @NotNull CommandContext<Player> context) {
		plugin.getStorageManager().getCachedUserDataWithFallback(targetUUID, plugin.getConfigManager().lockData())
				.thenAccept(targetOpt -> {
					if (targetOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
								AdventureHelper.miniMessageToComponent(targetName));
						return;
					}

					final UserData targetUser = targetOpt.get();
					final HellblockData targetData = targetUser.getHellblockData();
					final UUID visitorUUID = visitor.getUniqueId();

					if (!targetData.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
						return;
					}

					// Step 1: Get actual island owner (always required)
					final UUID ownerUUID = targetData.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Null owner UUID in loadTargetIsland for "
								+ targetUser.getName() + " (" + targetUser.getUUID() + ")");
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
						return;
					}

					// Step 2: If the visitor is visiting their own island
					if (ownerUUID.equals(visitorUUID)) {
						visitOwnOrPartyIsland(visitor, visitorData, context);
						return;
					}

					// Step 3: Load the owner's island data
					plugin.getStorageManager()
							.getCachedUserDataWithFallback(ownerUUID, plugin.getConfigManager().lockData())
							.thenAccept(ownerOpt -> {
								if (ownerOpt.isEmpty()) {
									final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
											AdventureHelper.miniMessageToComponent(username != null ? username
													: plugin.getTranslationManager().miniMessageTranslation(
															MessageConstants.FORMAT_UNKNOWN.build().key())));
									return;
								}

								final UserData ownerUser = ownerOpt.get();
								final HellblockData ownerData = ownerUser.getHellblockData();
								if (!ownerData.hasHellblock()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
									return;
								}

								if (ownerData.getPartyMembers().contains(visitor.getUniqueId())) {
									visitOwnOrPartyIsland(visitor, ownerUser, context);
									return;
								}

								// Step 4: Check if island is abandoned
								if (ownerData.isAbandoned()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
									return;
								}

								// Step 5: Check ban list from the owner's island data
								final boolean isBypassing = visitor.isOp() || visitor.hasPermission("hellblock.admin")
										|| visitor.hasPermission("hellblock.bypass.interact");

								final Set<UUID> banned = ownerData.getBannedMembers();

								if (!isBypassing && banned.contains(visitorUUID)) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_BANNED_ENTRY);
									return;
								}

								// Step 6: Proceed to visit the island
								visitIsland(visitor, ownerUser, context);

							}).exceptionally(ex -> {
								plugin.getPluginLogger()
										.warn("Failed to load island owner data for visit: " + ex.getMessage());
								return null;
							});

				}).exceptionally(ex -> {
					plugin.getPluginLogger().warn("getCachedUserDataWithFallback failed for visiting island of "
							+ visitor.getName() + ": " + ex.getMessage());
					return null;
				});
	}

	private void visitOwnOrPartyIsland(@NotNull Player visitor, @NotNull UserData visitorData,
			@NotNull CommandContext<Player> context) {
		final HellblockData data = visitorData.getHellblockData();
		if (!data.hasHellblock()) {
			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
			return;
		}

		final UUID ownerUUID = data.getOwnerUUID();
		if (ownerUUID == null) {
			plugin.getPluginLogger().severe("Null owner UUID in visitOwnOrPartyIsland for " + visitor.getName() + " ("
					+ visitor.getUniqueId() + ")");
			throw new IllegalStateException("Owner reference was null in visitOwnOrPartyIsland.");
		}

		plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, plugin.getConfigManager().lockData())
				.thenAccept(ownerOpt -> {
					if (ownerOpt.isEmpty()) {
						final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
								AdventureHelper.miniMessageToComponent(username != null ? username
										: plugin.getTranslationManager().miniMessageTranslation(
												MessageConstants.FORMAT_UNKNOWN.build().key())));
						return;
					}

					final UserData ownerUser = ownerOpt.get();
					if (ownerUser.getHellblockData().isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					plugin.getVisitManager().handleVisit(visitor, ownerUUID);
				});
	}

	private void visitIsland(@NotNull Player visitor, @NotNull UserData targetUser,
			@NotNull CommandContext<Player> context) {
		plugin.getCoopManager().checkIfVisitorsAreWelcome(visitor, targetUser.getUUID()).thenAccept(status -> {
			if (targetUser.isLocked() || !status) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_LOCKED_FROM_VISITORS,
						AdventureHelper.miniMessageToComponent(targetUser.getName()));
				return;
			}

			plugin.getVisitManager().handleVisit(visitor, targetUser.getUUID());
		});
	}

	@NotNull
	private CompletableFuture<? extends Iterable<? extends Suggestion>> suggestPlayers(
			@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
		if (!(context.sender() instanceof Player player)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		final Optional<UserData> visitorOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

		if (visitorOpt.isEmpty()) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		final UUID visitorId = visitorOpt.get().getUUID();
		final Set<UUID> visitorParty = visitorOpt.get().getHellblockData().getPartyMembers();

		final List<String> suggestions = plugin.getStorageManager().getDataSource().getUniqueUsers().stream()
				.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
				.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
				.filter(user -> user.getHellblockData().getHomeLocation() != null)
				.filter(user -> !visitorParty.contains(user.getUUID()))
				.filter(user -> !user.getUUID().equals(visitorId)).sorted(Comparator.comparingLong((UserData u) -> {
					long activity = u.getHellblockData().getLastIslandActivity();
					return activity > 0 ? activity : Long.MIN_VALUE; // push unknowns to end
				}).reversed()).map(UserData::getName).filter(Objects::nonNull).distinct().toList();

		return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visit";
	}
}