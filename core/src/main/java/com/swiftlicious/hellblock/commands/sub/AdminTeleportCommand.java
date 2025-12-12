package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
import java.util.List;
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

					final Set<UUID> allKnownUUIDs = plugin.getStorageManager().getDataSource().getUniqueUsers();

					final List<String> suggestions = allKnownUUIDs.stream()
							.map(uuid -> plugin.getStorageManager().getCachedUserData(uuid)).filter(Optional::isPresent)
							.map(Optional::get).filter(user -> user.getHellblockData().hasHellblock())
							.map(UserData::getName).filter(Objects::nonNull).toList();

					return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
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

					plugin.getStorageManager().getCachedUserDataWithFallback(targetId, false).thenAccept(result -> {
						if (result.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
									AdventureHelper.miniMessageToComponent(targetName));
							return;
						}

						final UserData targetUser = result.get();
						final HellblockData targetData = targetUser.getHellblockData();

						if (!targetData.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return;
						}

						final UUID ownerUUID = targetData.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUser.getName() + " ("
											+ targetUser.getUUID() + "). This indicates corrupted data.");
							throw new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer.");
						}

						// Load island owner
						plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false)
								.thenAccept(ownerOpt -> {
									if (ownerOpt.isEmpty()) {
										final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD,
												AdventureHelper.miniMessageToComponent(username != null ? username
														: plugin.getTranslationManager().miniMessageTranslation(
																MessageConstants.FORMAT_UNKNOWN.build().key())));
										return;
									}

									final UserData ownerUser = ownerOpt.get();
									teleportToIslandTop(executor, targetUser, ownerUser, context);
								});
					});
				});
	}

	private void teleportToIslandTop(@NotNull Player executor, @NotNull UserData targetUser,
			@NotNull UserData ownerUser, @NotNull CommandContext<Player> context) {
		final HellblockData ownerData = ownerUser.getHellblockData();

		// Ensure world is loaded (sync or async)
		plugin.getWorldManager().ensureHellblockWorldLoaded(ownerData.getIslandId()).thenCompose(loadedWorld -> {
			World bukkitWorld = loadedWorld.bukkitWorld();
			if (bukkitWorld == null) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
				return CompletableFuture.completedFuture(null);
			}

			int x = ownerData.getHellblockLocation().getBlockX();
			int z = ownerData.getHellblockLocation().getBlockZ();
			Location highest = bukkitWorld.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

			// Async teleport
			return ChunkUtils.teleportAsync(executor, highest, TeleportCause.PLUGIN)
					.thenRun(() -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT,
							AdventureHelper.miniMessageToComponent(targetUser.getName())));
		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_teleport";
	}
}