package com.swiftlicious.hellblock.player.mailbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.ConfigManager.TitleScreenInfo;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.config.locale.TranslationManager;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.sender.Sender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

/**
 * Handles queuing and processing of mailbox entries for players.
 * <p>
 * Mailbox entries are temporary actions or messages (like resets or warnings)
 * that are executed when a player next logs in.
 */
public class MailboxManager {

	protected final HellblockPlugin instance;

	public MailboxManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	/**
	 * Queues a single mailbox entry for a player.
	 * <p>
	 * The entry will be stored and processed on the player's next login.
	 *
	 * @param playerId The UUID of the player.
	 * @param entry    The mailbox entry to queue.
	 */
	public void queue(@NotNull UUID playerId, @NotNull MailboxEntry entry) {
		instance.getStorageManager().getCachedUserDataWithFallback(playerId, instance.getConfigManager().lockData())
				.thenAccept(optionalUser -> optionalUser.ifPresent(userData -> userData.getMailbox().add(entry)));
	}

	/**
	 * Queues a mailbox entry containing only flags.
	 *
	 * @param uuid  The UUID of the player.
	 * @param flags The flags to include in the entry.
	 */
	public void queueMailbox(@NotNull UUID uuid, @NotNull MailboxFlag... flags) {
		queue(uuid, new MailboxEntry(null, null, Set.of(flags)));
	}

	/**
	 * Processes all mailbox entries for a player after login.
	 * <p>
	 * Applies any queued actions or sends stored messages, then clears the mailbox.
	 *
	 * @param userData The player's user data.
	 */
	public void handleLogin(@NotNull UserData userData) {
		List<MailboxEntry> entries = new ArrayList<>(userData.getMailbox());

		if (entries.isEmpty())
			return;

		Player player = userData.getPlayer();

		if (player == null || !player.isOnline())
			return;

		Sender sender = instance.getSenderFactory().wrap(player);
		TranslationManager tm = instance.getTranslationManager();

		entries.forEach(entry -> {
			// Send stored message
			if (entry.getMessageKey() != null) {
				TranslatableComponent.Builder builder = Component.translatable().key(entry.getMessageKey());
				entry.getArguments().forEach(builder::arguments);
				sender.sendMessage(tm.render(builder.build()));
			}

			// Apply actions based on flags
			entry.getFlags().forEach(flag -> {
				switch (flag) {
				case RESET_INVENTORY -> {
					if (instance.getConfigManager().resetInventory()) {
						sender.sendMessage(tm.render(MessageConstants.MSG_HELLBLOCK_CLEARED_INVENTORY.build()));
						player.getInventory().clear();
						player.getInventory().setArmorContents(null);
					}
				}
				case RESET_ENDERCHEST -> {
					if (instance.getConfigManager().resetEnderchest()) {
						sender.sendMessage(tm.render(MessageConstants.MSG_HELLBLOCK_CLEARED_ENDERCHEST.build()));
						player.getEnderChest().clear();
					}
				}
				case UNSAFE_LOCATION -> {
					instance.getScheduler()
							.executeSync(() -> instance.getHellblockHandler().teleportToSpawn(player, true));
					sender.sendMessage(tm.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_ENVIRONMENT.build()));
				}
				case QUEUE_TELEPORT_HOME -> {
					instance.getScheduler()
							.executeSync(() -> instance.getCoopManager().makeHomeLocationSafe(userData, userData));
				}
				case SHOW_TITLE -> {
					TitleScreenInfo titleScreen = instance.getConfigManager().creationTitleScreen();
					if (titleScreen != null && titleScreen.enabled()) {
						String titleRaw = titleScreen.title();
						String subtitleRaw = titleScreen.subtitle();

						String title = titleRaw.replace("{player}", userData.getName());
						String subtitle = subtitleRaw.replace("{player}", userData.getName());

						VersionHelper.getNMSManager().sendTitle(player,
								AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(title)),
								AdventureHelper.componentToJson(AdventureHelper.miniMessageToComponent(subtitle)),
								titleScreen.fadeIn(), titleScreen.stay(), titleScreen.fadeOut());
					}
				}
				case PLAY_SOUND -> {
					if (instance.getConfigManager().creatingHellblockSound() != null) {
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								instance.getConfigManager().creatingHellblockSound());
					}
				}
				case SHOW_RESET_GUI -> {
					instance.getScheduler()
							.executeSync(() -> instance.getIslandChoiceGUIManager().openIslandChoiceGUI(player, true));
					sender.sendMessage(tm.render(MessageConstants.MSG_HELLBLOCK_UNSAFE_CONDITIONS.build()));
				}
				case RESET_GAMEMODE -> {
					instance.getScheduler().executeSync(() -> player.setGameMode(GameMode.SURVIVAL));
				}
				default -> {
					// no-op
				}
				}
			});
		});

		// Clear processed entries
		userData.getMailbox().clear();
	}
}