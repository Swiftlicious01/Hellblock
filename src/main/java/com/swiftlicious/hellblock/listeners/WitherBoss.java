package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ExplosionResult;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.Getter;
import lombok.NonNull;

public class WitherBoss implements Listener {

	private final HellblockPlugin instance;
	@Getter
	private final WitherHandler witherHandler;

	private boolean randomWither;
	private int witherHealthRangeMin;
	private int witherHealthRangeMax;
	private double witherStrengthRangeMin;
	private double witherStrengthRangeMax;

	private double witherStrength;
	private int witherHealth;

	public WitherBoss(HellblockPlugin plugin) {
		instance = plugin;
		this.witherHandler = new WitherHandler();
		this.randomWither = instance.getConfig("config.yml").getBoolean("wither-stats.random-stats", true);
		this.witherHealthRangeMin = instance.getConfig("config.yml").getInt("wither-stats.random-min-health", 200);
		this.witherHealthRangeMax = instance.getConfig("config.yml").getInt("wither-stats.random-max-health", 500);
		if (this.witherHealthRangeMin <= 0)
			this.witherHealthRangeMin = 200;
		if (this.witherHealthRangeMax <= 0)
			this.witherHealthRangeMax = 500;
		this.witherStrengthRangeMin = instance.getConfig("config.yml").getDouble("wither-stats.random-min-strength",
				0.5);
		this.witherStrengthRangeMax = instance.getConfig("config.yml").getDouble("wither-stats.random-max-strength",
				2.5);
		if (this.witherStrengthRangeMin <= 0)
			this.witherStrengthRangeMin = 0.5;
		if (this.witherStrengthRangeMax <= 0)
			this.witherStrengthRangeMax = 2.5;

		this.witherStrength = instance.getConfig("config.yml").getDouble("wither-stats.default-strength", 1.25);
		this.witherHealth = instance.getConfig("config.yml").getInt("wither-stats.default-health", 300);
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onWitherSpawn(CreatureSpawnEvent event) {
		if (event.getLocation().getWorld() == null)
			return;
		if (!event.getLocation().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER)
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;

		WitherStats witherStats = getWitherStats();
		Wither wither = (Wither) event.getEntity();
		wither.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(witherStats.getHealth());
		wither.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(witherStats.getStrength());
		wither.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(witherStats.getStrength());
		wither.setHealth(witherStats.getHealth());
		wither.setAware(true);
		wither.setAggressive(true);
		wither.setCanTravelThroughPortals(false);
		int lowHealth = RandomUtils.generateRandomInt(15, 75);
		instance.getScheduler().runTaskSyncTimer(() -> {
			if (wither.isDead() || !wither.isValid()) {
				return;
			}
			if (wither.getHealth() < lowHealth) {
				wither.setInvulnerableTicks(RandomUtils.generateRandomInt(100, 300));
			}
			Player closestPlayer = instance.getNetherrackGeneratorHandler().getClosestPlayer(event.getLocation(),
					event.getLocation().getWorld().getNearbyPlayers(event.getLocation(), 15.0D));
			if (closestPlayer != null) {
				wither.setTarget(closestPlayer);
			}
		}, event.getLocation(), 15 * 20, 30 * 20);

		getWitherHandler().addWither(wither, witherStats);
	}

	@EventHandler
	public void onWitherDestroyBlock(EntityChangeBlockEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (!(event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL))
			return;

		if (event.getTo().isAir()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWitherExplode(EntityExplodeEvent event) {
		if (!event.getLocation().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		if (event.getExplosionResult() != ExplosionResult.KEEP) {
			if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onWitherDeath(EntityDeathEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;
		Wither wither = (Wither) event.getEntity();
		getWitherHandler().removeWither(wither);
		if (wither.getKiller() != null) {
			Player player = wither.getKiller();
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			if (!pi.isChallengeActive(ChallengeType.ENHANCED_WITHER_CHALLENGE)
					&& !pi.isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
				pi.beginChallengeProgression(ChallengeType.ENHANCED_WITHER_CHALLENGE);
			} else {
				pi.updateChallengeProgression(ChallengeType.ENHANCED_WITHER_CHALLENGE, 1);
				if (pi.isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
					pi.completeChallenge(ChallengeType.ENHANCED_WITHER_CHALLENGE);
				}
			}
		}
	}

	@EventHandler
	public void onWitherDamage(EntityDamageByEntityEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		Wither wither;
		if (event.getDamager().getType() == EntityType.WITHER_SKULL) {
			WitherSkull witherSkull = (WitherSkull) event.getDamager();
			if (witherSkull.getShooter() == null)
				return;
			if (!(witherSkull.getShooter() instanceof Wither))
				return;

			wither = (Wither) witherSkull.getShooter();
		} else if (event.getDamager().getType() == EntityType.WITHER) {
			wither = (Wither) event.getDamager();
		} else {
			return;
		}

		WitherStats witherStats = getWitherHandler().getWitherStats(wither);

		event.setDamage(event.getDamage() * witherStats.getStrength());
	}

	private @NonNull WitherStats getWitherStats() {
		if (!this.randomWither)
			return new WitherStats(getWitherHealth(), getWitherStrength());
		else {
			int randomHealth = RandomUtils.generateRandomInt(getWitherHealthRangeMin(), getWitherHealthRangeMax());
			double randomStrength = getWitherStrengthRangeMin()
					+ (getWitherStrengthRangeMax() - getWitherStrengthRangeMin()) * RandomUtils.generateRandomDouble();

			return new WitherStats(randomHealth, randomStrength);
		}
	}

	public double getWitherStrength() {
		return witherStrength;
	}

	public int getWitherHealth() {
		return witherHealth;
	}

	public int getWitherHealthRangeMin() {
		return witherHealthRangeMin;
	}

	public int getWitherHealthRangeMax() {
		return witherHealthRangeMax;
	}

	public double getWitherStrengthRangeMin() {
		return witherStrengthRangeMin;
	}

	public double getWitherStrengthRangeMax() {
		return witherStrengthRangeMax;
	}

	public class WitherHandler {

		private final Map<Entity, WitherStats> witherStats = new HashMap<>();

		public void addWither(@NonNull Wither wither, @NonNull WitherStats stats) {
			witherStats.putIfAbsent(wither, stats);
		}

		public void removeWither(@NonNull Wither wither) {
			if (witherStats.containsKey(wither))
				witherStats.remove(wither);
		}

		public @NonNull WitherStats getWitherStats(@NonNull Wither wither) {
			return witherStats.get(wither);
		}
	}

	public class WitherStats {

		private int health;
		private double strength;

		public WitherStats(int health, double strength) {
			this.health = health;
			this.strength = strength;
		}

		public int getHealth() {
			return health;
		}

		public void setHealth(int health) {
			this.health = health;
		}

		public double getStrength() {
			return strength;
		}

		public void setStrength(double strength) {
			this.strength = strength;
		}
	}
}