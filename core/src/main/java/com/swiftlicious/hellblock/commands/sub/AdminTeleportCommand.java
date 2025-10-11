package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
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
import com.swiftlicious.hellblock.utils.ChunkUtils;

import net.kyori.adventure.text.Component;

public class AdminTeleportCommand extends BukkitCommandFeature<CommandSender> {

	public AdminTeleportCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("player",
						StringParser.stringComponent().suggestionProvider(this::suggestOnlineHellblockPlayers))
				.handler(context -> {
					final Player executor = context.sender();
					final String targetName = context.get("player");

					final UUID targetUUID = Bukkit.getPlayer(targetName) != null
							? Bukkit.getPlayer(targetName).getUniqueId()
							: UUIDFetcher.getUUID(targetName);

					if (targetUUID == null || !Bukkit.getOfflinePlayer(targetUUID).hasPlayedBefore()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(targetUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(targetName)));
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
									HellblockPlugin.getInstance().getPluginLogger()
											.severe("Hellblock owner UUID was null for player " + targetUser.getName()
													+ " (" + targetUser.getUUID()
													+ "). This indicates corrupted data.");
									throw new IllegalStateException(
											"Owner reference was null. This should never happen — please report to the developer.");
								}

								// Load island owner
								HellblockPlugin.getInstance().getStorageManager()
										.getOfflineUserData(ownerUUID,
												HellblockPlugin.getInstance().getConfigManager().lockData())
										.thenAccept(ownerOpt -> {
											if (ownerOpt.isEmpty()) {
												final String username = Bukkit.getOfflinePlayer(ownerUUID).getName();
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																.arguments(Component
																		.text(username != null ? username : "???")));
												return;
											}

											final UserData ownerUser = ownerOpt.get();
											teleportToIslandTop(executor, targetUser, ownerUser, context);
										});
							});
				});
	}

	private void teleportToIslandTop(Player executor, UserData targetUser, UserData ownerUser,
			CommandContext<Player> context) {
		final HellblockData ownerData = ownerUser.getHellblockData();

		// Ensure world is loaded (sync or async)
		HellblockPlugin.getInstance().getWorldManager().ensureHellblockWorldLoaded(ownerData.getID())
				.thenCompose(loadedWorld -> {
					World bukkitWorld = loadedWorld.bukkitWorld();
					if (bukkitWorld == null) {
						return CompletableFuture.completedFuture(null);
					}

					int x = ownerData.getHellblockLocation().getBlockX();
					int z = ownerData.getHellblockLocation().getBlockZ();
					Location highest = bukkitWorld.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

					// Async teleport
					return ChunkUtils.teleportAsync(executor, highest, TeleportCause.PLUGIN)
							.thenRun(() -> handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_TELEPORT
									.arguments(Component.text(targetUser.getName()))));
				}).exceptionally(ex -> {
					ex.printStackTrace();
					return null;
				});
	}

	private @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestOnlineHellblockPlayers(
			@NotNull CommandContext<Object> context, @NotNull CommandInput input) {
		final List<String> suggestions = HellblockPlugin.getInstance().getStorageManager().getOnlineUsers().stream()
				.filter(user -> user.isOnline() && user.getHellblockData().hasHellblock()).map(UserData::getName)
				.toList();

		return CompletableFuture.completedFuture(suggestions.stream().map(Suggestion::suggestion).toList());
	}

	@Override
	public String getFeatureID() {
		return "admin_teleport";
	}
}