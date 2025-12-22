package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.world.HellblockWorld;

public class HellblockVisitorsCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockVisitorsCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			final UUID playerUUID = player.getUniqueId();

			final Optional<UserData> onlineUser = plugin.getStorageManager().getOnlineUser(playerUUID);

			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData userData = onlineUser.get();
			final HellblockData data = userData.getHellblockData();

			if (!data.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			final UUID ownerUUID = data.getOwnerUUID();
			if (ownerUUID == null) {
				plugin.getPluginLogger().severe("Hellblock owner UUID was null for player " + player.getName() + " ("
						+ playerUUID + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException("Owner UUID was null. This should never happen.");
			}

			plugin.getStorageManager().getCachedUserDataWithFallback(ownerUUID, false).thenAccept(optOwnerData -> {
				if (optOwnerData.isEmpty()) {
					String username = Optional.ofNullable(Bukkit.getOfflinePlayer(ownerUUID).getName())
							.orElse(plugin.getTranslationManager()
									.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key()));
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_OWNER_DATA_NOT_LOADED,
							AdventureHelper.miniMessageToComponent(username));
					return;
				}

				final UserData ownerData = optOwnerData.get();

				if (ownerData.getHellblockData().isAbandoned()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
					return;
				}

				final BoundingBox boundingBox = ownerData.getHellblockData().getBoundingBox();
				if (boundingBox == null) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
					plugin.getPluginLogger().severe("Hellblock bounds returned null for player " + player.getName()
							+ " (" + player.getUniqueId() + "). This indicates corrupted data or a serious bug.");
					throw new IllegalStateException(
							"Hellblock bounds reference was null. This should never happen — please report to the developer.");
				}

				final int islandId = ownerData.getHellblockData().getIslandId();
				final String worldName = plugin.getWorldManager().getHellblockWorldFormat(islandId);

				final Optional<HellblockWorld<?>> worldOpt = plugin.getWorldManager().getWorld(worldName);
				if (worldOpt.isEmpty()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
					plugin.getPluginLogger().warn("World not found for visitors command: " + worldName + " (Island ID: "
							+ islandId + ", Owner UUID: " + ownerUUID + ")");
					return;
				}

				final World bukkitWorld = worldOpt.get().bukkitWorld();
				if (bukkitWorld == null) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_WORLD_ERROR);
					plugin.getPluginLogger().warn("Bukkit world is null for: " + worldName + " (Island ID: " + islandId
							+ ", Owner UUID: " + ownerUUID + ")");
					return;
				}

				final Set<UUID> coopMembers = ownerData.getHellblockData().getPartyPlusOwner();

				final List<String> visitorNames = plugin.getStorageManager().getOnlineUsers().stream()
						.filter(uuid -> !uuid.getUUID().equals(ownerUUID))
						.filter(uuid -> !coopMembers.contains(uuid.getUUID())).map(UserData::getPlayer)
						.filter(Objects::nonNull).filter(p -> p.getWorld().getUID().equals(bukkitWorld.getUID()))
						.filter(p -> boundingBox.contains(p.getLocation().toVector())).map(Player::getName)
						.sorted(String.CASE_INSENSITIVE_ORDER).toList();

				if (visitorNames.isEmpty()) {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_VISITORS);
				} else {
					handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISITOR_LIST,
							AdventureHelper.miniMessageToComponent(String.join(", ", visitorNames)));
				}
			}).exceptionally(ex -> {
				plugin.getPluginLogger().warn("Failed to load owner data for visitors check of " + player.getName(),
						ex);
				return null;
			});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visitors";
	}
}