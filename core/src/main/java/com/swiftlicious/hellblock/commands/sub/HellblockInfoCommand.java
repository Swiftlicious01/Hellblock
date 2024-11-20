package com.swiftlicious.hellblock.commands.sub;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
				HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
						"<red>Still loading your player data... please try again in a few seconds.");
				return;
			}
			if (!onlineUser.get().getHellblockData().hasHellblock()) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
						HellblockPlugin.getInstance().getTranslationManager()
								.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
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
							UserData offlineUser = result.orElseThrow();
							if (offlineUser.getHellblockData().isAbandoned()) {
								HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(player,
										HellblockPlugin.getInstance().getTranslationManager().miniMessageTranslation(
												MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
								return;
							}
							String partyString = "", trustedString = "", bannedString = "";
							for (UUID id : offlineUser.getHellblockData().getParty()) {
								if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(id).getName() != null) {
									partyString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
								}
							}
							for (UUID id : offlineUser.getHellblockData().getTrusted()) {
								if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(id).getName() != null) {
									trustedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
								}
							}
							for (UUID id : offlineUser.getHellblockData().getBanned()) {
								if (Bukkit.getOfflinePlayer(id).hasPlayedBefore()
										&& Bukkit.getOfflinePlayer(id).getName() != null) {
									bannedString = "<dark_red>" + Bukkit.getOfflinePlayer(id).getName() + "<red>, ";
								}
							}
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									String.format("<dark_red>Hellblock Information (ID: <red>%s<dark_red>):",
											offlineUser.getHellblockData().getID()));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Owner: <dark_red>" + (offlineUser.getName() != null
											&& Bukkit.getOfflinePlayer(offlineUser.getUUID()).hasPlayedBefore()
													? offlineUser.getName()
													: "Unknown"));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Level: <dark_red>" + offlineUser.getHellblockData().getLevel());
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Creation Date: <dark_red>"
											+ offlineUser.getHellblockData().getCreationTime());
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Visitor Status: <dark_red>"
											+ (offlineUser.getHellblockData().isLocked() ? "Closed" : "Open"));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Total Visits: <dark_red>" + offlineUser.getHellblockData().getTotalVisits());
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Island Type: <dark_red>" + StringUtils
											.capitalize(offlineUser.getHellblockData().getIslandChoice().getName()));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Biome: <dark_red>" + offlineUser.getHellblockData().getBiome().getName());
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Party Size: <dark_red>" + offlineUser.getHellblockData().getParty().size()
											+ " <red>/<dark_red> "
											+ HellblockPlugin.getInstance().getConfigManager().partySize());
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Party Members: <dark_red>" + (!partyString.isEmpty()
											? partyString.substring(0, partyString.length() - 2)
											: "None"));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Trusted Members: <dark_red>" + (!trustedString.isEmpty()
											? trustedString.substring(0, trustedString.length() - 2)
											: "None"));
							HellblockPlugin.getInstance().getAdventureManager().sendMessage(player,
									"<red>Banned Players: <dark_red>" + (!bannedString.isEmpty()
											? bannedString.substring(0, bannedString.length() - 2)
											: "None"));
						});

			}
		});
	}

	@Override
	public String getFeatureID() {
		return "hellblock_info";
	}
}