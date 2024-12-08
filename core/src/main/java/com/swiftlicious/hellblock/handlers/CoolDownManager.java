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
import com.swiftlicious.hellblock.player.UserData;

/**
 * Manages cooldowns for various actions or events. Keeps track of cooldown
 * times for different keys associated with player UUIDs.
 */
public class CoolDownManager implements Listener, Reloadable {

	private final ConcurrentMap<UUID, Data> dataMap;
	protected final HellblockPlugin instance;

	public CoolDownManager(HellblockPlugin plugin) {
		this.dataMap = new ConcurrentHashMap<>();
		this.instance = plugin;
		instance.getScheduler().asyncLater(() -> startCountdowns(), 5, TimeUnit.SECONDS);
	}

	public void startCountdowns() {
		instance.getScheduler().asyncRepeating(() -> {
			for (UUID playerData : instance.getStorageManager().getDataSource().getUniqueUsers()) {
				instance.getStorageManager().getOfflineUserData(playerData, instance.getConfigManager().lockData())
						.thenAccept((result) -> {
							if (result.isEmpty())
								return;
							UserData offlineUser = result.get();
							UUID ownerUUID = offlineUser.getHellblockData().getOwnerUUID();
							if (ownerUUID != null && playerData.equals(ownerUUID)) {
								if (offlineUser.getHellblockData().getResetCooldown() > 0) {
									offlineUser.getHellblockData()
											.setResetCooldown(offlineUser.getHellblockData().getResetCooldown() - 1);
								}
								if (offlineUser.getHellblockData().getBiomeCooldown() > 0) {
									offlineUser.getHellblockData()
											.setBiomeCooldown(offlineUser.getHellblockData().getBiomeCooldown() - 1);
								}
								if (offlineUser.getHellblockData().getTransferCooldown() > 0) {
									offlineUser.getHellblockData().setTransferCooldown(
											offlineUser.getHellblockData().getTransferCooldown() - 1);
								}
							}
							if (!offlineUser.getHellblockData().hasHellblock()
									&& offlineUser.getHellblockData().getInvitations() != null) {
								for (Map.Entry<UUID, Long> invites : offlineUser.getHellblockData().getInvitations()
										.entrySet()) {
									if (invites.getValue() > 0) {
										offlineUser.getHellblockData().getInvitations().replace(invites.getKey(),
												invites.getValue() - 1);
									} else {
										offlineUser.getHellblockData().getInvitations().remove(invites.getKey());
									}
								}
							}
						});
			}
		}, 0, 1, TimeUnit.SECONDS);
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
		Data data = this.dataMap.computeIfAbsent(uuid, k -> new Data());
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
			long time = System.currentTimeMillis();
			long last = coolDownMap.getOrDefault(key, time - delay);
			if (last + delay > time) {
				return true; // Player is in cooldown
			} else {
				coolDownMap.put(key, time);
				return false; // Player is not in cooldown
			}
		}
	}
}