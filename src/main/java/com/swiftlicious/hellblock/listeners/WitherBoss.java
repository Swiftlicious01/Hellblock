package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import com.swiftlicious.hellblock.config.HBConfig;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;

import lombok.Getter;
import lombok.NonNull;

public class WitherBoss implements Listener {

	protected final HellblockPlugin instance;
	@Getter
	private final WitherHandler witherHandler;

	public WitherBoss(HellblockPlugin plugin) {
		instance = plugin;
		this.witherHandler = new WitherHandler();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@EventHandler
	public void onWitherSpawn(CreatureSpawnEvent event) {
		if (event.getLocation().getWorld() == null)
			return;
		if (!event.getLocation().getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER)
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;

		WitherStats witherStats = getWitherStats();
		Wither wither = (Wither) event.getEntity();
		wither.getAttribute(Attribute.MAX_HEALTH).setBaseValue(witherStats.getHealth());
		wither.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(witherStats.getStrength());
		wither.getAttribute(Attribute.ARMOR).setBaseValue(witherStats.getStrength());
		wither.setHealth(witherStats.getHealth());
		wither.setAware(true);
		wither.setAggressive(true);
		wither.setCanTravelThroughPortals(false);
		int lowHealth = RandomUtils.generateRandomInt(15, 75);
		instance.getScheduler().sync().runRepeating(() -> {
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
		}, 15 * 20, 30 * 20, event.getLocation());

		getWitherHandler().addWither(wither, witherStats);
	}

	@EventHandler
	public void onWitherDestroyBlock(EntityChangeBlockEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;
		if (!(event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL))
			return;

		if (event.getTo().isAir()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWitherExplode(EntityExplodeEvent event) {
		if (!event.getLocation().getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;

		if (event.getExplosionResult() != ExplosionResult.KEEP) {
			if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onWitherDeath(EntityDeathEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;
		Wither wither = (Wither) event.getEntity();
		getWitherHandler().removeWither(wither);
		if (wither.getKiller() != null) {
			Player player = wither.getKiller();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null)
				return;
			if (!onlineUser.get().getChallengeData().isChallengeActive(ChallengeType.ENHANCED_WITHER_CHALLENGE)
					&& !onlineUser.get().getChallengeData()
							.isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
				onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
						ChallengeType.ENHANCED_WITHER_CHALLENGE);
			} else {
				onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
						ChallengeType.ENHANCED_WITHER_CHALLENGE, 1);
				if (onlineUser.get().getChallengeData().isChallengeCompleted(ChallengeType.ENHANCED_WITHER_CHALLENGE)) {
					onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
							ChallengeType.ENHANCED_WITHER_CHALLENGE);
				}
			}
		}
	}

	@EventHandler
	public void onWitherDamage(EntityDamageByEntityEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(HBConfig.worldName))
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
		if (!HBConfig.randomWither)
			return new WitherStats(getWitherHealth(), getWitherStrength());
		else {
			int randomHealth = RandomUtils.generateRandomInt(getWitherHealthRangeMin(), getWitherHealthRangeMax());
			double randomStrength = getWitherStrengthRangeMin()
					+ (getWitherStrengthRangeMax() - getWitherStrengthRangeMin()) * RandomUtils.generateRandomDouble();

			return new WitherStats(randomHealth, randomStrength);
		}
	}

	public double getWitherStrength() {
		return HBConfig.witherStrength;
	}

	public int getWitherHealth() {
		return HBConfig.witherHealth;
	}

	public int getWitherHealthRangeMin() {
		return HBConfig.witherHealthRangeMin;
	}

	public int getWitherHealthRangeMax() {
		return HBConfig.witherHealthRangeMax;
	}

	public double getWitherStrengthRangeMin() {
		return HBConfig.witherStrengthRangeMin;
	}

	public double getWitherStrengthRangeMax() {
		return HBConfig.witherStrengthRangeMax;
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