package com.swiftlicious.hellblock.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

/**
 * Manages cooldowns for various actions or events. Keeps track of cooldown
 * times for different keys associated with player UUIDs.
 */
public class CoolDownManager implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final ConcurrentMap<UUID, CooldownData> dataMap = new ConcurrentHashMap<>();
	private SchedulerTask cooldownTask;
	private SchedulerTask cleanupTask;

	private final Map<UUID, Long> lastActivityUpdate = new HashMap<>();

	public CoolDownManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public boolean shouldUpdateActivity(UUID uuid, long cooldownMillis) {
		long now = System.currentTimeMillis();
		Long lastUpdate = lastActivityUpdate.get(uuid);

		if (lastUpdate == null || (now - lastUpdate) >= cooldownMillis) {
			lastActivityUpdate.put(uuid, now);
			return true; // Allow update
		}

		return false; // Cooldown not passed yet
	}

	public void cleanupCooldowns() {
		long now = System.currentTimeMillis();
		lastActivityUpdate.entrySet().removeIf(entry -> now - entry.getValue() > 60_000); // 1 minute old
	}

	public void startCountdowns() {
		final Set<UUID> allUsers = instance.getStorageManager().getDataSource().getUniqueUsers();
		if (allUsers.isEmpty()) {
			return;
		}
		for (UUID playerId : allUsers) {
			instance.getStorageManager().getCachedUserDataWithFallback(playerId, false).thenAccept(userOpt -> {
				if (userOpt.isEmpty())
					return;

				final UserData user = userOpt.get();
				final HellblockData data = user.getHellblockData();

				// Skip if there's nothing to update
				boolean shouldUpdate = false;

				if (data.getOwnerUUID() != null && data.getOwnerUUID().equals(playerId)) {
					if (data.getResetCooldown() > 0 || data.getBiomeCooldown() > 0 || data.getTransferCooldown() > 0) {
						shouldUpdate = true;
					}
				}

				Map<UUID, Long> invites = data.getInvitations();
				if (invites != null && !invites.isEmpty()) {
					shouldUpdate = true;
				}

				if (!shouldUpdate)
					return;

				// Run cooldown decrement on main thread
				instance.getScheduler().executeSync(() -> {
					if (data.getOwnerUUID() != null && data.getOwnerUUID().equals(playerId)) {
						if (data.getResetCooldown() > 0)
							data.setResetCooldown(data.getResetCooldown() - 1);
						if (data.getBiomeCooldown() > 0)
							data.setBiomeCooldown(data.getBiomeCooldown() - 1);
						if (data.getTransferCooldown() > 0)
							data.setTransferCooldown(data.getTransferCooldown() - 1);
					}

					if (invites != null && !invites.isEmpty()) {
						invites.replaceAll((key, value) -> value - 1);
						invites.entrySet().removeIf(e -> e.getValue() <= 0);
					}
				});
			});
		}
	}

	public void stopCooldowns() {
		if (this.cooldownTask != null && !this.cooldownTask.isCancelled()) {
			this.cooldownTask.cancel();
			this.cooldownTask = null;
		}
		if (this.cleanupTask != null && !this.cleanupTask.isCancelled()) {
			this.cleanupTask.cancel();
			this.cleanupTask = null;
		}
	}

	/**
	 * Checks if a player is currently in cooldown for a specific key.
	 *
	 * @param uuid The UUID of the player.
	 * @param key  The key associated with the cooldown.
	 * @param time The cooldown time in milliseconds.
	 * @return True if the player is in cooldown, false otherwise.
	 */
	public boolean isCoolDown(UUID uuid, String key, long time) {
		final CooldownData data = this.dataMap.computeIfAbsent(uuid, k -> new CooldownData());
		return data.isCoolDown(key, time);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		cooldownTask = instance.getScheduler().asyncRepeating(this::startCountdowns, 0, 1, TimeUnit.SECONDS);
		cleanupTask = instance.getScheduler().asyncRepeating(this::cleanupCooldowns, 0, 3, TimeUnit.MINUTES);
	}

	@Override
	public void unload() {
		stopCooldowns();
		HandlerList.unregisterAll(this);
	}

	@Override
	public void disable() {
		unload();
		this.dataMap.clear();
		this.lastActivityUpdate.clear();
	}

	/**
	 * Formats a cooldown time in seconds into a human-readable string.
	 *
	 * @param seconds the cooldown time in seconds
	 * @return a formatted string representing the cooldown time
	 */
	@NotNull
	public String getFormattedCooldown(long seconds) {
		final long days = seconds / 86400;
		final long hours = (seconds % 86400) / 3600;
		final long minutes = (seconds % 3600) / 60;
		final long remainingSeconds = seconds % 60;

		final String dayFormat = instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_DAY.build().key());
		final String hourFormat = instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_HOUR.build().key());
		final String minuteFormat = instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_MINUTE.build().key());
		final String secondFormat = instance.getTranslationManager()
				.miniMessageTranslation(MessageConstants.FORMAT_SECOND.build().key());

		final StringBuilder formattedTime = new StringBuilder();
		if (days > 0) {
			formattedTime.append(days).append(dayFormat);
		}
		if (hours > 0) {
			formattedTime.append(hours).append(hourFormat);
		}
		if (minutes > 0) {
			formattedTime.append(minutes).append(minuteFormat);
		}
		if (remainingSeconds > 0) {
			formattedTime.append(remainingSeconds).append(secondFormat);
		}

		return formattedTime.toString();
	}

	/**
	 * Event handler for when a player quits. Removes their cooldown data.
	 *
	 * @param event The PlayerQuitEvent triggered when a player quits.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		dataMap.remove(event.getPlayer().getUniqueId());
		lastActivityUpdate.remove(event.getPlayer().getUniqueId());
	}

	public static class CooldownData {

		private final Map<String, Long> coolDownMap;

		public CooldownData() {
			this.coolDownMap = Collections.synchronizedMap(new HashMap<>());
		}

		/**
		 * Checks if the player is in cooldown for a specific key.
		 *
		 * @param key   The key associated with the cooldown.
		 * @param delay The cooldown delay in milliseconds.
		 * @return True if the player is in cooldown, false otherwise.
		 */
		public boolean isCoolDown(String key, long delay) {
			final long time = System.currentTimeMillis();
			final long last = coolDownMap.getOrDefault(key, time - delay);
			if (last + delay > time) {
				return true; // Player is in cooldown
			} else {
				coolDownMap.put(key, time);
				return false; // Player is not in cooldown
			}
		}
	}
}