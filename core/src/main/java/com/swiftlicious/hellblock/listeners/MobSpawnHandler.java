package com.swiftlicious.hellblock.listeners;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

public class MobSpawnHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private final Map<Integer, Double> mobSpawnBonusCache = new ConcurrentHashMap<>();

	public MobSpawnHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		mobSpawnBonusCache.clear();
	}

	@EventHandler
	public void onMobSpawn(CreatureSpawnEvent event) {
		final LivingEntity entity = event.getEntity();
		final Pos3 pos = Pos3.from(entity.getLocation());
		final World world = entity.getWorld();

		final Optional<HellblockWorld<?>> hellWorldOpt = instance.getWorldManager().getWorld(world);
		if (hellWorldOpt.isEmpty() || hellWorldOpt.get().bukkitWorld() == null) {
			return;
		}

		final HellblockWorld<?> hellWorld = hellWorldOpt.get();

		instance.getIslandManager().resolveIslandId(hellWorld, pos).thenAccept(id -> {
			if (id.isEmpty() || id.get() <= 0) {
				instance.getScheduler().executeSync(() -> event.setCancelled(true));
			}
		});
	}

	public double getCachedMobSpawnBonus(@NotNull HellblockData data) {
		int islandId = data.getIslandId();
		return mobSpawnBonusCache.computeIfAbsent(islandId, id -> calculateMobSpawnBonus(data));
	}

	private double calculateMobSpawnBonus(@NotNull HellblockData data) {
		int currentLevel = data.getUpgradeLevel(IslandUpgradeType.MOB_SPAWN_RATE);
		double total = 0.0;

		for (int i = 0; i <= currentLevel; i++) {
			UpgradeTier tier = instance.getUpgradeManager().getTier(i);
			if (tier != null) {
				UpgradeData upgrade = tier.getUpgrade(IslandUpgradeType.MOB_SPAWN_RATE);
				if (upgrade != null && upgrade.getValue() != null) {
					total += upgrade.getValue().doubleValue();
				}
			}
		}

		return total;
	}

	public double updateMobSpawnBonusCache(@NotNull HellblockData data) {
		return mobSpawnBonusCache.put(data.getIslandId(), calculateMobSpawnBonus(data));
	}

	public double invalidateMobSpawnBonusCache(int islandId) {
		return mobSpawnBonusCache.remove(islandId);
	}
}