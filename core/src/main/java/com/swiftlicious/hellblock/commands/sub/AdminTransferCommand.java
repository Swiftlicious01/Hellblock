package com.swiftlicious.hellblock.commands.sub;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UUIDFetcher;
import com.swiftlicious.hellblock.player.UserData;

import net.kyori.adventure.text.Component;

public class AdminTransferCommand extends BukkitCommandFeature<CommandSender> {

	public AdminTransferCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).required("currentOwner", StringParser.stringComponent())
				.required("newOwner", StringParser.stringComponent()).handler(context -> {
					final String currentName = context.get("currentOwner");
					final String newName = context.get("newOwner");

					// Resolve UUIDs
					final UUID currentId = Bukkit.getPlayer(currentName) != null
							? Bukkit.getPlayer(currentName).getUniqueId()
							: UUIDFetcher.getUUID(currentName);

					final UUID newId = Bukkit.getPlayer(newName) != null ? Bukkit.getPlayer(newName).getUniqueId()
							: UUIDFetcher.getUUID(newName);

					if (currentId == null || newId == null) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE);
						return;
					}

					if (currentId.equals(newId)) {
						handleFeedback(context, MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND
								.arguments(Component.text(currentName)));
						return;
					}

					// Load current owner
					HellblockPlugin.getInstance().getStorageManager()
							.getOfflineUserData(currentId, HellblockPlugin.getInstance().getConfigManager().lockData())
							.thenAccept(currentOpt -> {
								if (currentOpt.isEmpty()) {
									handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
											.arguments(Component.text(currentName)));
									return;
								}

								// Load new owner
								HellblockPlugin.getInstance().getStorageManager()
										.getOfflineUserData(newId,
												HellblockPlugin.getInstance().getConfigManager().lockData())
										.thenAccept(newOpt -> {
											if (newOpt.isEmpty()) {
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
																.arguments(Component.text(newName)));
												return;
											}

											final UserData currentOwner = currentOpt.get();
											final UserData newOwner = newOpt.get();

											if (!currentOwner.getHellblockData().hasHellblock()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
												return;
											}

											if (newOwner.getHellblockData().isAbandoned()) {
												handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
												return;
											}

											try {
												// Perform transfer
												HellblockPlugin.getInstance().getCoopManager()
														.transferOwnershipOfHellblock(currentOwner, newOwner, true);

												// Feedback to executor
												handleFeedback(context,
														MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_SUCCESS.arguments(
																Component.text(currentName), Component.text(newName)));

												// Notify both players if online
												final Player currentOnline = Bukkit.getPlayer(currentId);
												final Player newOnline = Bukkit.getPlayer(newId);

												if (currentOnline != null) {
													handleFeedback(currentOnline,
															MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_LOST
																	.arguments(Component.text(newName)));
												}
												if (newOnline != null) {
													handleFeedback(newOnline,
															MessageConstants.MSG_HELLBLOCK_ADMIN_TRANSFER_GAINED
																	.arguments(Component.text(currentName)));
												}

											} catch (Exception ex) {
												HellblockPlugin.getInstance().getPluginLogger()
														.warn("Admin force transfer failed from " + currentName + " to "
																+ newName + ": " + ex.getMessage());
											}
										}).exceptionally(ex -> {
											HellblockPlugin.getInstance().getPluginLogger().warn(
													"Failed to load new owner " + newName + ": " + ex.getMessage());
											return null;
										});
							}).exceptionally(ex -> {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("Failed to load current owner " + currentName + ": " + ex.getMessage());
								return null;
							});
				});
	}

	@Override
	public String getFeatureID() {
		return "admin_transfer";
	}
}