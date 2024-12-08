package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.commands.BukkitCommandFeature;
import com.swiftlicious.hellblock.commands.HellblockCommandManager;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.StringUtils;

import net.kyori.adventure.text.Component;

public class HellblockInfoCommand extends BukkitCommandFeature<CommandSender> {

	public HellblockInfoCommand(HellblockCommandManager<CommandSender> commandManager) {
		super(commandManager);
	}

	@Override
	public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager,
			Command.Builder<CommandSender> builder) {
		return builder.senderType(Player.class).handler(context -> {
			final Player player = context.sender();
			Optional<UserData> onlineUser = HellblockPlugin.getInstance().getStorageManager()
					.getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty()) {
				handleFeedback(context, MessageConstants.COMMAND_DATA_FAILURE_NOT_LOADED);
				return;
			}
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				handleFeedback(context, MessageConstants.MSG_HELLBLOCK_NOT_FOUND);
				return;
			} else {
				if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				HellblockPlugin.getInstance().getStorageManager()
						.getOfflineUserData(onlineUser.get().getHellblockData().getOwnerUUID(),
								HellblockPlugin.getInstance().getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty()) {
								String username = Bukkit
										.getOfflinePlayer(onlineUser.get().getHellblockData().getOwnerUUID())
										.getName() != null ? Bukkit
												.getOfflinePlayer(onlineUser.get().getHellblockData().getOwnerUUID())
												.getName() : "???";
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_PLAYER_DATA_FAILURE_LOAD
										.arguments(Component.text(username)));
								return;
							}
							UserData offlineUser = result.get();
							if (offlineUser.getHellblockData().isAbandoned()) {
								handleFeedback(context, MessageConstants.MSG_HELLBLOCK_IS_ABANDONED);
								return;
							}
							StringBuilder partyString = new StringBuilder(), trustedString = new StringBuilder(),
									bannedString = new StringBuilder();
							if (offlineUser.getHellblockData().getParty() != null) {
								for (UUID id : offlineUser.getHellblockData().getParty()) {
									if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
											&& Bukkit.getOfflinePlayer(id).getName() != null) {
										partyString.append(HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_STYLE
														.arguments(
																Component.text(Bukkit.getOfflinePlayer(id).getName()))
														.build().key()));
									}
								}
							}
							if (offlineUser.getHellblockData().getTrusted() != null) {
								for (UUID id : offlineUser.getHellblockData().getTrusted()) {
									if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
											&& Bukkit.getOfflinePlayer(id).getName() != null) {
										trustedString.append(HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_STYLE
														.arguments(
																Component.text(Bukkit.getOfflinePlayer(id).getName()))
														.build().key()));
									}
								}
							}
							if (offlineUser.getHellblockData().getBanned() != null) {
								for (UUID id : offlineUser.getHellblockData().getBanned()) {
									if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
											&& Bukkit.getOfflinePlayer(id).getName() != null) {
										bannedString.append(HellblockPlugin.getInstance().getTranslationManager()
												.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_INFO_LIST_STYLE
														.arguments(
																Component.text(Bukkit.getOfflinePlayer(id).getName()))
														.build().key()));
									}
								}
							}
							handleFeedback(context, MessageConstants.MSG_HELLBLOCK_INFORMATION.arguments(
									Component.text(offlineUser.getHellblockData().getID()),
									Component.text(offlineUser.getName() != null
											&& Bukkit.getOfflinePlayer(offlineUser.getUUID()).hasPlayedBefore()
													? offlineUser.getName()
													: HellblockPlugin.getInstance().getTranslationManager()
															.miniMessageTranslation(
																	MessageConstants.FORMAT_UNKNOWN.build().key()))),
									Component.text(offlineUser.getHellblockData().getLevel()),
									Component.text(offlineUser.getHellblockData().getCreationTime()),
									Component.text(offlineUser.getHellblockData().isLocked()
											? HellblockPlugin.getInstance().getTranslationManager()
													.miniMessageTranslation(
															MessageConstants.FORMAT_CLOSED.build().key())
											: HellblockPlugin.getInstance().getTranslationManager()
													.miniMessageTranslation(
															MessageConstants.FORMAT_OPEN.build().key())),
									Component.text(offlineUser.getHellblockData().getTotalVisits()),
									Component.text(StringUtils
											.toProperCase(offlineUser.getHellblockData().getIslandChoice().toString())),
									Component.text(offlineUser.getHellblockData().getBiome().getName()),
									Component.text(offlineUser.getHellblockData().getParty().size()),
									Component.text(HellblockPlugin.getInstance().getCoopManager()
											.getMaxPartySize(offlineUser)),
									Component.text((!partyString.isEmpty()
											? partyString.toString().substring(0, partyString.toString().length() - 2)
											: HellblockPlugin.getInstance().getTranslationManager()
													.miniMessageTranslation(
															MessageConstants.FORMAT_NONE.build().key()))),
									Component.text((!trustedString.isEmpty()
											? trustedString.toString().substring(0, partyString.toString().length() - 2)
											: HellblockPlugin.getInstance().getTranslationManager()
													.miniMessageTranslation(
															MessageConstants.FORMAT_NONE.build().key()))),
									Component.text((!bannedString.isEmpty()
											? bannedString.toString().substring(0, partyString.toString().length() - 2)
											: HellblockPlugin.getInstance().getTranslationManager()
													.miniMessageTranslation(
															MessageConstants.FORMAT_NONE.build().key()))));
						});

			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_info";
	}
}