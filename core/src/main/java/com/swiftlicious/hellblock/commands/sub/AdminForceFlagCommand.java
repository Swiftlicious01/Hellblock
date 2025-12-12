package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.protection.ProtectionManager;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.WorldManager;

public class AdminForceFlagCommand extends BukkitCommandFeature<CommandSender> {

	public AdminForceFlagCommand(HellblockCommandManager<CommandSender> commandManager) {
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
				})).required("flag", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					String targetName = context.getOrDefault("player", null);
					if (targetName == null) {
						// No player specified yet, show all valid flags (excluding disallowed)
						return CompletableFuture.completedFuture(Arrays.stream(FlagType.values())
								.filter(flag -> flag != FlagType.GREET_MESSAGE && flag != FlagType.FAREWELL_MESSAGE
										&& flag != FlagType.ENTRY)
								.map(Enum::name).map(Suggestion::suggestion).toList());
					}

					UUID targetId;

					Player onlinePlayer = Bukkit.getPlayer(targetName);
					if (onlinePlayer != null) {
						targetId = onlinePlayer.getUniqueId();
					} else {
						Optional<UUID> fetchedId = UUIDFetcher.getUUID(targetName);
						if (fetchedId.isEmpty()) {
							return CompletableFuture.completedFuture(Collections.emptyList());
						}
						targetId = fetchedId.get();
					}

					if (!Bukkit.getOfflinePlayer(targetId).hasPlayedBefore()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userDataOpt = plugin.getStorageManager().getCachedUserData(targetId);
					if (userDataOpt.isEmpty()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					// Filter out disallowed flags from suggestions
					List<Suggestion> suggestions = Arrays
							.stream(FlagType.values()).filter(flag -> flag != FlagType.GREET_MESSAGE
									&& flag != FlagType.FAREWELL_MESSAGE && flag != FlagType.ENTRY)
							.map(Enum::name).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(suggestions);
				})).handler(context -> {
					final String targetName = context.get("player");
					final String flagInput = context.getOrDefault("flag", FlagType.INTERACT.name())
							.toUpperCase(Locale.ROOT);

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
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_ISLAND_FOUND);
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

									final Optional<FlagType> flagOpt = Arrays.stream(FlagType.values())
											.filter(f -> f.name().equalsIgnoreCase(flagInput)).findFirst();

									if (flagOpt.isEmpty()) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_FLAG,
												AdventureHelper.miniMessageToComponent(flagInput));
										return;
									}

									final FlagType flagType = flagOpt.get();

									if (flagType == FlagType.GREET_MESSAGE || flagType == FlagType.FAREWELL_MESSAGE
											|| flagType == FlagType.ENTRY) {
										handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_FLAG,
												AdventureHelper.miniMessageToComponent(flagInput));
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

									AccessType current = ownerHellblockData.getProtectionValue(flagType);
									AccessType reversed = (current == AccessType.ALLOW) ? AccessType.DENY
											: AccessType.ALLOW;

									// Get the reverse of the current access type for the new flag update
									final HellblockFlag flagUpdate = new HellblockFlag(flagType, reversed);

									// Apply flag change
									protectionManager.changeProtectionFlag(world, ownerUUID, flagUpdate);

									// Save changes
									plugin.getStorageManager().saveUserData(ownerData,
											plugin.getConfigManager().lockData());

									// Send confirmation message
									final String flagDisplay = StringUtils.toCamelCase(flagUpdate.getFlag().getName());
									final boolean flagValue = flagUpdate.getStatus().getReturnValue();

									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_FORCE_FLAG,
											AdventureHelper.miniMessageToComponent(targetName),
											AdventureHelper.miniMessageToComponent(flagDisplay),
											AdventureHelper.miniMessageToComponent(String.valueOf(flagValue)));
								});
					});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_forceflag";
	}
}