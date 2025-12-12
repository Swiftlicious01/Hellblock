package com.swiftlicious.hellblock.commands.sub;

import java.util.Collections;
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
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.ProtectionManager;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.WorldManager;

public class AdminUnlockCommand extends BukkitCommandFeature<CommandSender> {

	public AdminUnlockCommand(HellblockCommandManager<CommandSender> commandManager) {
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
						final HellblockData data = targetUser.getHellblockData();

						if (!data.hasHellblock()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return;
						}

						final UUID ownerUUID = data.getOwnerUUID();
						if (ownerUUID == null) {
							plugin.getPluginLogger()
									.severe("Hellblock owner UUID was null for player " + targetUser.getName() + " ("
											+ targetUser.getUUID() + "). This indicates corrupted data.");
							throw new IllegalStateException(
									"Owner reference was null. This should never happen — please report to the developer.");
						}

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

									final UserData ownerData = ownerOpt.get();

									if (ownerData.getHellblockData().isAbandoned()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
										return;
									}

									final WorldManager worldManager = plugin.getWorldManager();
									final ProtectionManager protectionManager = plugin.getProtectionManager();

									final HellblockData ownerHellblockData = ownerData.getHellblockData();
									final int islandId = ownerHellblockData.getIslandId();
									final String worldName = worldManager.getHellblockWorldFormat(islandId);

									final Optional<HellblockWorld<?>> worldOpt = worldManager.getWorld(worldName);
									if (worldOpt.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
										plugin.getPluginLogger().warn("World not found for force flag: " + worldName
												+ " (Island ID: " + islandId + ", Owner UUID: " + ownerUUID + ")");
										return;
									}

									final HellblockWorld<?> world = worldOpt.get();
									if (world.bukkitWorld() == null) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
										plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName
												+ " (Island ID: " + islandId + ", Owner UUID: " + ownerUUID + ")");
										return;
									}

									// Toggle lock
									ownerHellblockData.setLockedStatus(!ownerHellblockData.isLocked());
									if (ownerHellblockData.isLocked()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_LOCKED,
												AdventureHelper.miniMessageToComponent(targetName));
									} else {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_UNLOCKED,
												AdventureHelper.miniMessageToComponent(targetName));
									}

									// Update protections
									protectionManager.changeLockStatus(world, ownerUUID);

									plugin.getStorageManager().saveUserData(ownerData,
											plugin.getConfigManager().lockData());

									// Kick out visitors if locked
									if (ownerHellblockData.isLocked()) {
										plugin.getCoopManager().kickVisitorsIfLocked(ownerUUID);
									}
								});
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_unlock";
	}
}