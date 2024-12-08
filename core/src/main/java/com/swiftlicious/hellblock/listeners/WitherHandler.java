package com.swiftlicious.hellblock.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class WitherHandler implements Listener {

	protected final HellblockPlugin instance;
	private final CustomWither customWither;

	public WitherHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.customWither = new CustomWither();
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public CustomWither getWitherHandler() {
		return this.customWither;
	}

	@SuppressWarnings("removal")
	@EventHandler
	public void onWitherSpawn(CreatureSpawnEvent event) {
		if (event.getLocation().getWorld() == null)
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getLocation().getWorld()))
			return;
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER)
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;

		WitherStats witherStats = getWitherStats();
		Wither wither = (Wither) event.getEntity();
		RtagEntity witherTag = new RtagEntity(wither);
		witherTag.setAttributeBase("generic.max_health", witherStats.getHealth());
		witherTag.setAttributeBase("generic.attack_damage", witherStats.getStrength());
		witherTag.setAttributeBase("generic.armor", witherStats.getStrength());
		witherTag.update();
		witherTag.load();
		wither.getPersistentDataContainer().set(new NamespacedKey(instance, "hellblock-wither"),
				PersistentDataType.STRING, "customwither");
		wither.setHealth(witherStats.getHealth());
		wither.setAware(true);
		int lowHealth = RandomUtils.generateRandomInt(15, 75);
		instance.getScheduler().sync().runRepeating(() -> {
			if (wither.isDead() || !wither.isValid()) {
				return;
			}
			if (wither.getHealth() < lowHealth) {
				wither.setInvulnerabilityTicks(RandomUtils.generateRandomInt(100, 300));
			}
			Player closestPlayer = instance.getNetherrackGeneratorHandler().getClosestPlayer(event.getLocation(),
					event.getLocation().getWorld().getNearbyEntities(event.getLocation(), 15.0D, 5.0D, 15.0D).stream()
							.filter(e -> e.getType() == EntityType.PLAYER).toList());
			if (closestPlayer != null) {
				wither.setTarget(closestPlayer);
			}
		}, 15 * 20, 30 * 20, event.getLocation());

		getWitherHandler().addWither(wither, witherStats);
	}

	@EventHandler
	public void onWitherDestroyBlock(EntityChangeBlockEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld()))
			return;
		if (!(event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL))
			return;
		if (!(event.getEntity().getPersistentDataContainer().has(new NamespacedKey(instance, "hellblock-wither"))))
			return;

		if (event.getTo().isAir()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWitherExplode(EntityExplodeEvent event) {
		if (event.getLocation().getWorld() == null)
			return;
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getLocation().getWorld()))
			return;
		if (!(event.getEntity().getPersistentDataContainer().has(new NamespacedKey(instance, "hellblock-wither"))))
			return;

		if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL)
			event.setCancelled(true);

	}

	@EventHandler
	public void onWitherDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld()))
			return;
		if (event.getEntityType() != EntityType.WITHER)
			return;
		if (!(event.getEntity().getPersistentDataContainer().has(new NamespacedKey(instance, "hellblock-wither"))))
			return;
		Wither wither = (Wither) event.getEntity();
		getWitherHandler().removeWither(wither);
		if (wither.getKiller() != null) {
			Player player = wither.getKiller();
			Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(player.getUniqueId());
			if (onlineUser.isEmpty() || onlineUser.get().getPlayer() == null
					|| !onlineUser.get().getHellblockData().hasHellblock())
				return;
			if (!onlineUser.get().getChallengeData()
					.isChallengeActive(instance.getChallengeManager().getByActionType(ActionType.KILL))
					&& !onlineUser.get().getChallengeData()
							.isChallengeCompleted(instance.getChallengeManager().getByActionType(ActionType.KILL))) {
				onlineUser.get().getChallengeData().beginChallengeProgression(onlineUser.get().getPlayer(),
						instance.getChallengeManager().getByActionType(ActionType.KILL));
			} else {
				onlineUser.get().getChallengeData().updateChallengeProgression(onlineUser.get().getPlayer(),
						instance.getChallengeManager().getByActionType(ActionType.KILL), 1);
				if (onlineUser.get().getChallengeData()
						.isChallengeCompleted(instance.getChallengeManager().getByActionType(ActionType.KILL))) {
					onlineUser.get().getChallengeData().completeChallenge(onlineUser.get().getPlayer(),
							instance.getChallengeManager().getByActionType(ActionType.KILL));
				}
			}
		}
	}

	@EventHandler
	public void onWitherDamage(EntityDamageByEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld()))
			return;
		Wither wither;
		if (event.getDamager().getType() == EntityType.WITHER_SKULL) {
			WitherSkull witherSkull = (WitherSkull) event.getDamager();
			if (witherSkull.getShooter() == null)
				return;
			if (!(witherSkull.getShooter() instanceof Wither))
				return;
			if (!(((Wither) witherSkull.getShooter()).getPersistentDataContainer()
					.has(new NamespacedKey(instance, "hellblock-wither"))))
				return;

			wither = (Wither) witherSkull.getShooter();
		} else if (event.getDamager().getType() == EntityType.WITHER) {
			if (!(event.getDamager().getPersistentDataContainer().has(new NamespacedKey(instance, "hellblock-wither"))))
				return;
			wither = (Wither) event.getDamager();
		} else {
			return;
		}

		WitherStats witherStats = getWitherHandler().getWitherStats(wither);

		event.setDamage(event.getDamage() * witherStats.getStrength());
	}

	private @NotNull WitherStats getWitherStats() {
		if (!instance.getConfigManager().randomStats())
			return new WitherStats(getWitherHealth(), getWitherStrength());
		else {
			int randomHealth = RandomUtils.generateRandomInt(getWitherHealthRangeMin(), getWitherHealthRangeMax());
			double randomStrength = getWitherStrengthRangeMin()
					+ (getWitherStrengthRangeMax() - getWitherStrengthRangeMin()) * RandomUtils.generateRandomDouble();

			return new WitherStats(randomHealth, randomStrength);
		}
	}

	public double getWitherStrength() {
		return instance.getConfigManager().defaultStrength();
	}

	public int getWitherHealth() {
		return instance.getConfigManager().defaultHealth();
	}

	public int getWitherHealthRangeMin() {
		return instance.getConfigManager().randomMinHealth();
	}

	public int getWitherHealthRangeMax() {
		return instance.getConfigManager().randomMaxHealth();
	}

	public double getWitherStrengthRangeMin() {
		return instance.getConfigManager().randomMinStrength();
	}

	public double getWitherStrengthRangeMax() {
		return instance.getConfigManager().randomMaxStrength();
	}

	public class CustomWither {

		private final Map<Entity, WitherStats> witherStats = new HashMap<>();

		public void addWither(@NotNull Wither wither, @NotNull WitherStats stats) {
			witherStats.putIfAbsent(wither, stats);
		}

		public void removeWither(@NotNull Wither wither) {
			if (witherStats.containsKey(wither))
				witherStats.remove(wither);
		}

		public @NotNull WitherStats getWitherStats(@NotNull Wither wither) {
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