package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
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
import com.swiftlicious.hellblock.utils.ChunkUtils;

public class AdminTeleportCommand extends BukkitCommandFeature<CommandSender> {

	public AdminTeleportCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final String lowerInput = input.input().toLowerCase(Locale.ROOT);
					final Set<UUID> allKnownUUIDs = new HashSet<>(
							plugin.getStorageManager().getDataSource().getUniqueUsers());

					final List<Suggestion> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull)
							.filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerInput))
							.sorted(String.CASE_INSENSITIVE_ORDER).limit(64).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final Player executor = context.sender();
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

					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenCompose(targetOpt -> {
						if (targetOpt.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return CompletableFuture.completedFuture(false);
						}

						final UserData targetUserData = targetOpt.get();
						final HellblockData data = targetUserData.getHellblockData();

						if (!data.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return CompletableFuture.completedFuture(false);
						}

						final UUID ownerUUID = data.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUserData.getName()
											+ " (" + targetUserData.getUUID() + "). This indicates corrupted data.");
							return CompletableFuture.failedFuture(new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer."));
						}

						// Load island owner
						return plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false)
								.thenCompose(ownerOpt -> {
									if (ownerOpt.isEmpty()) {
										final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(username != null ? username
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())));
										return CompletableFuture.completedFuture(false);
									}

									final UserData ownerData = ownerOpt.get();
									return teleportToIslandTop(executor, targetUserData, ownerData, context)
											.thenApply(v -> true);
								}).exceptionally(ex -> {
									plugin.getPluginLogger().warn("Admin teleport command failed (Could not read owner "
											+ ownerUUID + "'s data): " + ex.getMessage());
									return false;
								});
					}).exceptionally(ex -> {
						plugin.getPluginLogger().warn("Admin teleport command failed (Could not read target "
								+ targetName + "'s data): " + ex.getMessage());
						return false;
					});
				});
	}

	private CompletableFuture<Boolean> teleportToIslandTop(@NotNull Player executor, @NotNull UserData targetData,
			@NotNull UserData ownerData, @NotNull CommandContext<Player> context) {
		final HellblockData hellblockData = ownerData.getHellblockData();

		// Ensure world is loaded (sync or async)
		return plugin.getWorldManager().ensureHellblockWorldLoaded(hellblockData.getIslandId())
				.thenCompose(loadedWorld -> {
					World bukkitWorld = loadedWorld.bukkitWorld();
					if (bukkitWorld == null) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
						return CompletableFuture.completedFuture(false);
					}

					final Location hellblockLoc = hellblockData.getHellblockLocation();
					if (hellblockLoc == null) {
						plugin.getPluginLogger()
								.severe("Hellblock location returned null for owner " + ownerData.getName() + " ("
										+ ownerData.getUUID() + "). This indicates corrupted data or a serious bug.");
						return CompletableFuture.failedFuture(new IllegalStateException(
								"Hellblock location reference was null. This should never happen — please report to the developer."));
					}

					int x = hellblockLoc.getBlockX();
					int z = hellblockLoc.getBlockZ();
					Location highest = bukkitWorld.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

					// Async teleport
					return ChunkUtils.teleportAsync(executor, highest, TeleportCause.PLUGIN).thenApply(v -> {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT,
								AdventureHelper.miniMessageToComponent(targetData.getName()));
						return true;
					});
				}).exceptionally(ex -> {
					plugin.getPluginLogger()
							.warn("Failed to ensure world is loaded for islandID=" + hellblockData.getIslandId(), ex);
					return false;
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_teleport";
	}
}