package com.swiftlicious.hellblock.commands.sub;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

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

			final Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(playerUUID);

			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}

			final UserData user = onlineUser.get();
			final HellblockData hellblock = user.getHellblockData();

			if (!hellblock.hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			}

			if (hellblock.isAbandoned()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
				return;
			}

			final UUID ownerUUID = hellblock.getOwnerUUID();
			if (ownerUUID == null) {
				HellblockPlugin.getInstance().getPluginLogger().severe("Hellblock owner UUID was null for player "
						+ player.getName() + " (" + playerUUID + "). This indicates corrupted data or a serious bug.");
				throw new IllegalStateException("Owner UUID was null. This should never happen.");
			}

			HellblockPlugin.getInstance().getStorageManager()
					.getOfflineUserData(ownerUUID, HellblockPlugin.getInstance().getConfigManager().lockData())
					.thenAccept(ownerOpt -> {
						if (ownerOpt.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return;
						}

						final UserData ownerData = ownerOpt.get();
						final BoundingBox boundingBox = ownerData.getHellblockData().getBoundingBox();

						if (boundingBox == null) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
							return;
						}

						final Set<UUID> coopMembers = ownerData.getHellblockData().getPartyPlusOwner();
						final World world = player.getWorld();

						final List<String> visitorNames = HellblockPlugin.getInstance().getStorageManager()
								.getOnlineUsers().stream().filter(uuid -> !uuid.getUUID().equals(ownerUUID))
								.filter(uuid -> !coopMembers.contains(uuid.getUUID())).map(UserData::getPlayer)
								.filter(Objects::nonNull).filter(p -> p.getWorld().getName().equals(world.getName()))
								.filter(p -> boundingBox.contains(p.getLocation().toVector())).map(Player::getName)
								.sorted(String.CASE_INSENSITIVE_ORDER).toList();

						if (visitorNames.isEmpty()) {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NO_VISITORS);
						} else {
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_VISITOR_LIST
									.arguments(Component.text(String.join(", ", visitorNames))));
						}
					}).exceptionally(ex -> {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("Failed to load owner data for visitors check of " + player.getName(), ex);
						return null;
					});
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_visitors";
	}
}
