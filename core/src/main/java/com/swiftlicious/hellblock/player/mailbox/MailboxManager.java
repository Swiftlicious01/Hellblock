package com.swiftlicious.hellblock.player.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.locale.TranslationManager;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public class MailboxManager {

	protected final HellblockPlugin instance;

	public MailboxManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void queue(UUID playerId, MailboxEntry entry) {
		instance.getStorageManager().getOfflineUserData(playerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUser -> optionalUser.ifPresent(user -> {
					PlayerData data = user.toPlayerData();
					data.getMailbox().add(entry);
				}));
	}

	public void queueMailbox(UUID uuid, MailboxFlag... flags) {
		queue(uuid, new MailboxEntry(null, null, Set.of(flags)));
	}

	public void handleLogin(Player player) {
		UUID uuid = player.getUniqueId();

		instance.getStorageManager().getOfflineUserData(uuid, instance.getConfigManager().lockData())
				.thenAccept(optionalUser -> optionalUser.ifPresent(user -> {
					PlayerData data = user.toPlayerData();
					List<MailboxEntry> entries = new ArrayList<>(data.getMailbox());

					if (entries.isEmpty())
						return;

					Sender sender = instance.getSenderFactory().wrap(player);
					TranslationManager tm = instance.getTranslationManager();

					entries.forEach(entry -> {
						// Handle message
						if (entry.getMessageKey() != null) {
							TranslatableComponent.Builder builder = Component.translatable().key(entry.getMessageKey());
							entry.getArguments().forEach(builder::arguments);
							sender.sendMessage(tm.render(builder.build()));
						}

						// Handle flags
						entry.getFlags().forEach(flag -> {
							switch (flag) {
							case RESET_INVENTORY -> {
								if (instance.getConfigManager().resetInventory()) {
									sender.sendMessage(
											tm.render(MessageConstants.MSG_HELLBLOCK_CLEARED_INVENTORY.build()));
									player.getInventory().clear();
									player.getInventory().setArmorContents(null);
								}
							}
							case RESET_ENDERCHEST -> {
								if (instance.getConfigManager().resetEnderchest()) {
									sender.sendMessage(
											tm.render(MessageConstants.MSG_HELLBLOCK_CLEARED_ENDERCHEST.build()));
									player.getEnderChest().clear();
								}
							}
							case UNSAFE_LOCATION, RESET_ANIMATION -> {
								instance.getScheduler().executeSync(
										() -> instance.getHellblockHandler().teleportToSpawn(player, true));
								sender.sendMessage(
										tm.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_ENVIRONMENT.build()));
								if (flag == MailboxFlag.RESET_ANIMATION) {
									player.setSpectatorTarget(null);
									if (player.getGameMode() == GameMode.SPECTATOR) {
										player.setGameMode(GameMode.SURVIVAL);
									}
								}
							}
							case SHOW_RESET_GUI -> {
								instance.getScheduler().executeSync(
										() -> instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, true));
								sender.sendMessage(tm.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
							}
							default -> {
								/* no action */
							}
							}
						});
					});

					// Clear mailbox and persist the updated state
					data.getMailbox().clear();
					instance.getStorageManager().saveUserData(user, instance.getConfigManager().lockData());
				}));
	}
}