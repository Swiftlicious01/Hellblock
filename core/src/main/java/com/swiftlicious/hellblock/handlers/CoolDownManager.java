package com.swiftlicious.hellblock.handlers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

/**
 * Manages cooldowns for various actions or events. Keeps track of cooldown
 * times for different keys associated with player UUIDs.
 */
public class CoolDownManager implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final ConcurrentMap<UUID, Data> dataMap = new ConcurrentHashMap<>();
	private SchedulerTask cooldownTask;

	public CoolDownManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public void init() {
		cooldownTask = instance.getScheduler().asyncRepeating(this::startCountdowns, 0, 1, TimeUnit.SECONDS);
	}

	public void startCountdowns() {
		for (UUID playerId : instance.getStorageManager().getDataSource().getUniqueUsers()) {
			instance.getStorageManager().getOfflineUserData(playerId, instance.getConfigManager().lockData())
					.thenAccept(result -> {
						if (result.isEmpty())
							return;
						final UserData user = result.get();

						// Schedule safely on main thread
						instance.getScheduler().executeSync(() -> {
							HellblockData data = user.getHellblockData();

							if (data.getOwnerUUID() != null && data.getOwnerUUID().equals(playerId)) {
								if (data.getResetCooldown() > 0)
									data.setResetCooldown(data.getResetCooldown() - 1);
								if (data.getBiomeCooldown() > 0)
									data.setBiomeCooldown(data.getBiomeCooldown() - 1);
								if (data.getTransferCooldown() > 0)
									data.setTransferCooldown(data.getTransferCooldown() - 1);
							}

							if (!data.getInvitations().isEmpty()) {
								Map<UUID, Long> invites = data.getInvitations();
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
		final Data data = this.dataMap.computeIfAbsent(uuid, k -> new Data());
		return data.isCoolDown(key, time);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public void disable() {
		unload();
		stopCooldowns();
		this.dataMap.clear();
	}

	/**
	 * Event handler for when a player quits. Removes their cooldown data.
	 *
	 * @param event The PlayerQuitEvent triggered when a player quits.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		dataMap.remove(event.getPlayer().getUniqueId());
	}

	public static class Data {

		private final Map<String, Long> coolDownMap;

		public Data() {
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