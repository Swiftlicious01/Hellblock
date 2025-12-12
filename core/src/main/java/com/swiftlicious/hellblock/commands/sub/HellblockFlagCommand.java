package com.swiftlicious.hellblock.commands.sub;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class HellblockFlagCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockFlagCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class)
				.required("flag", StringParser.stringComponent().suggestionProvider((context, input) -> {
					if (!(context.sender() instanceof Player player)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					Optional<UserData> userOpt = plugin.getStorageManager().getOnlineUser(player.getUniqueId());

					if (userOpt.isEmpty()) {
						// No data loaded — show nothing
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UserData userData = userOpt.get();
					final HellblockData data = userData.getHellblockData();

					if (!data.hasHellblock()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (!data.isOwner(ownerUUID)) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					if (data.isAbandoned()) {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}

					// Filter out disallowed flags from suggestions
					List<Suggestion> availableFlags = Arrays
							.stream(FlagType.values()).filter(flag -> flag != FlagType.GREET_MESSAGE
									&& flag != FlagType.FAREWELL_MESSAGE && flag != FlagType.ENTRY)
							.map(Enum::name).map(Suggestion::suggestion).toList();

					return CompletableFuture.completedFuture(availableFlags);
				})).handler(context -> {
					final Player player = context.sender();
					final UUID playerUUID = player.getUniqueId();

					final Optional<UserData> onlineUserOpt = plugin.getStorageManager().getOnlineUser(playerUUID);

					if (onlineUserOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
						return;
					}

					final UserData user = onlineUserOpt.get();
					final HellblockData data = user.getHellblockData();

					if (!data.hasHellblock()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
						return;
					}

					final UUID ownerUUID = data.getOwnerUUID();
					if (ownerUUID == null) {
						plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName()
								+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
						throw new IllegalStateException(
								"Owner reference was null. This should never happen — please report to the developer.");
					}

					if (!data.isOwner(playerUUID)) {
						handleFeedback(context, MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK);
						return;
					}

					if (data.isAbandoned()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
						return;
					}

					final String flagInput = context.getOrDefault("flag", HellblockFlag.FlagType.INTERACT.name())
							.toUpperCase();
					final Optional<HellblockFlag.FlagType> flagOpt = Arrays.stream(HellblockFlag.FlagType.values())
							.filter(up -> up.name().equalsIgnoreCase(flagInput)).findFirst();

					if (flagOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_FLAG);
						return;
					}

					final HellblockFlag.FlagType flagType = flagOpt.get();

					if (flagType == FlagType.GREET_MESSAGE || flagType == FlagType.FAREWELL_MESSAGE
							|| flagType == FlagType.ENTRY) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INVALID_FLAG,
								AdventureHelper.miniMessageToComponent(flagInput));
						return;
					}

					final int islandId = data.getIslandId();
					final String worldName = plugin.getWorldManager().getHellblockWorldFormat(islandId);

					final Optional<HellblockWorld<?>> worldOpt = plugin.getWorldManager().getWorld(worldName);
					if (worldOpt.isEmpty()) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
						plugin.getPluginLogger().warn("World not found for force flag: " + worldName + " (Island ID: "
								+ islandId + ", Owner UUID: " + ownerUUID + ")");
						return;
					}

					final HellblockWorld<?> world = worldOpt.get();
					if (world.bukkitWorld() == null) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
						plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName + " (Island ID: "
								+ islandId + ", Owner UUID: " + ownerUUID + ")");
						return;
					}

					AccessType current = data.getProtectionValue(flagType);
					AccessType reversed = (current == AccessType.ALLOW) ? AccessType.DENY : AccessType.ALLOW;

					// Get the reverse of the current access type for the new flag update
					final HellblockFlag flagUpdate = new HellblockFlag(flagType, reversed);

					plugin.getProtectionManager().changeProtectionFlag(world, ownerUUID, flagUpdate);

					final String flagDisplay = StringUtils.toCamelCase(flagUpdate.getFlag().getName());
					final boolean flagValue = flagUpdate.getStatus().getReturnValue();

					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_CHANGED_FLAG,
							AdventureHelper.miniMessageToComponent(flagDisplay),
							AdventureHelper.miniMessageToComponent(String.valueOf(flagValue)));
				});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_flag";
	}
}