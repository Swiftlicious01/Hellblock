package com.swiftlicious.hellblock.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.upgrades.IslandUpgradeType;
import com.swiftlicious.hellblock.upgrades.UpgradeData;
import com.swiftlicious.hellblock.upgrades.UpgradeTier;

public class MobSpawnHandler implements Reloadable {

	protected final HellblockPlugin instance;

	private final Map<UUID, Double> mobSpawnBonusCache = new ConcurrentHashMap<>();

	public MobSpawnHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void unload() {
		mobSpawnBonusCache.clear();
	}

	public double getCachedMobSpawnBonus(@NotNull HellblockData data) {
		UUID ownerUUID = data.getOwnerUUID();
		return mobSpawnBonusCache.computeIfAbsent(ownerUUID, id -> calculateMobSpawnBonus(data));
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

	public void updateMobSpawnBonusCache(@NotNull HellblockData data) {
		mobSpawnBonusCache.put(data.getOwnerUUID(), calculateMobSpawnBonus(data));
	}

	public void invalidateMobSpawnBonusCache(@NotNull UUID ownerUUID) {
		mobSpawnBonusCache.remove(ownerUUID);
	}
}