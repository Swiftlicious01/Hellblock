package com.swiftlicious.hellblock.commands.sub;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminCleanupCommand extends BukkitCommandFeature<CommandSender> {

	public AdminCleanupCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.handler(context -> {
			HellblockPlugin.getInstance().getStorageManager().getDataSource().getUniqueUsers()
					.forEach(id -> HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(id, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(result -> {
								if (result.isEmpty()) {
									// owner profile is missing entirely → orphan
									HellblockPlugin.getInstance().getPluginLogger()
											.warn("Orphaned hellblock found for UUID " + id);
									HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true, "Console")
											.thenRun(() -> handleFeedback(context,
													MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_ORPHAN
															.arguments(Component.text(id.toString()))));
									return;
								}

								final UserData data = result.get();
								if (data.getHellblockData().hasHellblock()
										&& data.getHellblockData().getOwnerUUID() == null) {
									// corrupt hellblock data → cleanup
									HellblockPlugin.getInstance().getPluginLogger()
											.warn("Hellblock with null owner detected for " + data.getName() + " (" + id
													+ ")");
									HellblockPlugin.getInstance().getHellblockHandler().resetHellblock(id, true, "Console")
											.thenRun(() -> handleFeedback(context,
													MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_CORRUPT
															.arguments(Component.text(data.getName()))));
								}
							}));

			handleFeedback(context, MessageConstants.MSG_HELLBLOCK_ADMIN_CLEANUP_STARTED);
		});
	}

	@Override
	public String getFeatureID() {
		return "admin_cleanup";
	}
}