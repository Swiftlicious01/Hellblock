package com.swiftlicious.hellblock.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.entity.firework.FakeFirework;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

public class WitherHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final String WITHER_KEY = "enhanced_wither";
	private final NamespacedKey witherKey;
	private final CustomWither customWither;

	private SchedulerTask witherSpawnTask;

	private final Set<UUID> healedWithers = new HashSet<>(); // track one-time heal
	private final Map<UUID, Integer> mobSummonCount = new HashMap<>();
	private final Map<UUID, SchedulerTask> witherTargetTasks = new ConcurrentHashMap<>();
	// Track how many times a Wither has bounced outside the island
	private final Map<UUID, Integer> witherBounceCounts = new HashMap<>();
	private final Map<UUID, SchedulerTask> witherBounceResetTasks = new ConcurrentHashMap<>();

	public WitherHandler(HellblockPlugin plugin) {
		instance = plugin;
		this.witherKey = new NamespacedKey(instance, WITHER_KEY);
		this.customWither = new CustomWither();
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		startSpawnTask();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		if (witherSpawnTask != null && !witherSpawnTask.isCancelled()) {
			witherSpawnTask.cancel();
			witherSpawnTask = null;
		}
		// cancel all wither target tasks
		witherTargetTasks.values().stream().filter(task -> task != null).forEach(SchedulerTask::cancel);
		witherTargetTasks.clear();
		mobSummonCount.clear();
		healedWithers.clear();
		witherBounceCounts.clear();
		witherBounceResetTasks.values().stream().filter(task -> task != null).forEach(SchedulerTask::cancel);
		witherBounceResetTasks.clear();
		getCustomWither().activeIslandWithers.values().stream().filter(wither -> wither.isValid() && !wither.isDead())
				.forEach(Entity::remove);
		getCustomWither().activeIslandWithers.clear();
		getCustomWither().witherStats.clear();
		getCustomWither().spawnCooldowns.clear();
	}

	public CustomWither getCustomWither() {
		return this.customWither;
	}

	private void startSpawnTask() {
		this.witherSpawnTask = instance.getScheduler().asyncRepeating(
				() -> instance.getStorageManager().getDataSource().getUniqueUsers()
						.forEach(uuid -> instance.getStorageManager()
								.getOfflineUserData(uuid, instance.getConfigManager().lockData())
								.thenAccept(optUser -> optUser.ifPresent(this::trySpawnEnhancedWither))),
				5, 5, TimeUnit.MINUTES);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onWitherNaturalSpawn(CreatureSpawnEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.WITHER) {
			return;
		}

		// Block normal built-Withers
		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWitherDestroyBlock(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(wither.getWorld())) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		// Prevent block destruction
		event.setCancelled(true);
	}

	@EventHandler
	public void onWitherExplode(EntityExplodeEvent event) {
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(wither.getWorld())) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		// Prevent explosions entirely
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getBlock().getWorld())) {
			return;
		}

		// Cancel if near an enhanced Wither
		final boolean nearbyEnhanced = event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 15,
				15, 15, e -> e instanceof Wither && isEnhancedWither((Wither) e)).size() > 0;

		if (nearbyEnhanced) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onWitherTarget(EntityTargetEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		final Entity target = event.getTarget();
		if (target == null) {
			event.setCancelled(true);
			return;
		}

		// Hell Golems → always allowed
		if (target instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm)) {
			return;
		}

		// Players → allowed (coop check handled in scheduler)
		if (target instanceof Player) {
			return;
		}

		// Anything else (mobs, animals, etc.) → cancel
		event.setCancelled(true);
	}

	@EventHandler
	public void onWitherDamage(EntityDamageByEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		final Entity damager = event.getDamager();
		Player attacker = null;

		if (damager instanceof Player p) {
			attacker = p;
		} else if (damager instanceof Projectile proj && proj.getShooter() != null
				&& proj.getShooter() instanceof Player p) {
			attacker = p;
		}

		if (attacker == null) {
			event.setCancelled(true); // only players or their projectiles can damage
			return;
		}

		final Player attackerFinal = attacker;

		// Coop attacker → apply scaling
		instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
				.thenAccept(islandData -> instance.getScheduler().executeSync(() -> {
					if (islandData == null) {
						event.setCancelled(true);
						return;
					}
					final boolean isCoop = islandData.getPartyPlusOwner().contains(attackerFinal.getUniqueId());
					if (!isCoop) {
						event.setCancelled(true);
						return;
					}
					final WitherStats stats = getCustomWither().getWitherStats(wither);
					if (stats != null) {
						event.setDamage(event.getDamage() * stats.strength());
					}
				}));
	}

	@EventHandler
	public void onWitherSpawnMinion(EntityDamageByEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		final WitherStats stats = getCustomWither().getWitherStats(wither);
		if (stats == null) {
			return;
		}

		event.setDamage(event.getDamage() * stats.strength());

		final double newHealth = Math.max(0, wither.getHealth() - event.getDamage());
		final double maxHealth = stats.health();

		instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
				.thenAccept(islandData -> {
					if (islandData == null) {
						return;
					}
					instance.getScheduler().executeSync(() -> {
						if (newHealth <= maxHealth * 0.75 && !mobSummonCount.containsKey(wither.getUniqueId())) {
							trySummonMinions(wither, islandData);
						}
						if (newHealth <= maxHealth * 0.35
								&& mobSummonCount.getOrDefault(wither.getUniqueId(), 0) == 1) {
							trySummonMinions(wither, islandData);
						}
					});
				});
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getHitEntity() instanceof Wither wither)) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		final Projectile projectile = event.getEntity();
		if (projectile.getShooter() == null) {
			return;
		}

		if (projectile.getShooter() instanceof Player shooter) {
			instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
					.thenAccept(islandData -> {
						if (islandData == null || !islandData.getPartyPlusOwner().contains(shooter.getUniqueId())) {
							instance.getScheduler().executeSync(() -> bounceAndFizzle(projectile, shooter));
						}
					});
		} else {
			bounceAndFizzle(projectile, null);
		}
	}

	private void bounceAndFizzle(@NotNull Projectile projectile, @Nullable Player shooter) {
		// Cancel damage completely
		projectile.setMetadata("fizzled", new FixedMetadataValue(instance, true));

		if (projectile instanceof Fireball || projectile instanceof WitherSkull
				|| projectile instanceof DragonFireball) {
			// Explosive projectiles: cancel immediately
			projectile.getWorld().spawnParticle(Particle.LARGE_SMOKE, projectile.getLocation(), 16, 0.5, 0.5, 0.5,
					0.02);
			projectile.getWorld().spawnParticle(Particle.FLAME, projectile.getLocation(), 12, 0.3, 0.3, 0.3, 0.02);

			projectile.getWorld().getNearbyEntities(projectile.getLocation(), 16, 16, 16, e -> e instanceof Player)
					.stream().map(e -> (Player) e)
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.firework.rocket.twinkle"),
									net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.8f, 0.8f)));

			projectile.remove(); // prevent natural explosion
			return;
		}

		if (projectile instanceof Firework) {
			// Cancel its normal explosion
			projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 20, 0.6, 0.6, 0.6, 0.25);
			projectile.getWorld().spawnParticle(Particle.LARGE_SMOKE, projectile.getLocation(), 12, 0.4, 0.4, 0.4,
					0.05);

			projectile.getWorld().getNearbyEntities(projectile.getLocation(), 16, 16, 16, e -> e instanceof Player)
					.stream().map(e -> (Player) e)
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.firework.rocket.blast"),
									net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.0f, 0.9f)));

			projectile.remove();
			return;
		}

		// Default case: arrow-like projectiles
		final Vector velocity = projectile.getVelocity().multiply(-0.4).add(new Vector(0, 0.3, 0));
		projectile.setVelocity(velocity);

		projectile.getWorld().spawnParticle(Particle.CRIT, projectile.getLocation(), 12, 0.3, 0.3, 0.3, 0.15);

		if (shooter != null) {
			AdventureHelper.playSound(instance.getSenderFactory().getAudience(shooter),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.shield.block"),
							net.kyori.adventure.sound.Sound.Source.PLAYER, 1f, 1.1f));
		} else {
			projectile.getWorld().getNearbyEntities(projectile.getLocation(), 16, 16, 16, e -> e instanceof Player)
					.stream().map(e -> (Player) e)
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.shield.block"),
									net.kyori.adventure.sound.Sound.Source.PLAYER, 1f, 1.1f)));
		}

		// Remove after 10s
		instance.getScheduler().sync().runLater(() -> {
			if (!projectile.isDead() && projectile.isValid()) {
				projectile.getWorld().spawnParticle(Particle.SMOKE, projectile.getLocation(), 8, 0.2, 0.2, 0.2, 0.01);
				projectile.remove();
			}
		}, 20L * 10, projectile.getLocation());
	}

	@EventHandler
	public void onFizzleDamage(EntityDamageByEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getDamager() instanceof Projectile proj && proj.hasMetadata("fizzled")) {
			event.setCancelled(true); // no damage ever again
		}
	}

	@EventHandler
	public void onWitherLowHealth(EntityDamageEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof Wither wither)) {
			return;
		}
		if (!isEnhancedWither(wither)) {
			return;
		}

		final double newHealth = wither.getHealth() - event.getFinalDamage();
		final RtagEntity taggedWither = new RtagEntity(wither);
		final double maxHealth = taggedWither.getAttributeBase("generic.max_health");

		if (newHealth <= maxHealth * 0.25 && !healedWithers.contains(wither.getUniqueId())) {
			if (RandomUtils.generateRandomInt(1, 100) <= 30) { // 30% chance
				healedWithers.add(wither.getUniqueId());

				final double healAmount = maxHealth * 0.35; // heal ~35%
				wither.setHealth(Math.min(maxHealth, wither.getHealth() + healAmount));

				// FX
				wither.getWorld().spawnParticle(Particle.HEART, wither.getLocation(), 40, 2, 2, 2, 0.1);
				wither.getWorld().spawnParticle(Particle.WITCH, wither.getLocation(), 100, 2, 2, 2, 0.2);

				final Sound ambientSound = Sound.sound(Key.key("minecraft:entity.wither.ambient"), Sound.Source.HOSTILE,
						2f, 0.5f);

				instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
						.thenAccept(islandData -> {
							if (islandData == null) {
								return;
							}

							instance.getScheduler().executeSync(() -> islandData.getPartyPlusOwner().stream()
									.map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
									.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
											ambientSound)));
						});
			}
		}
	}

	@EventHandler
	public void onWitherDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.WITHER) {
			return;
		}

		final Wither wither = (Wither) event.getEntity();

		if (!isEnhancedWither(wither)) {
			return;
		}

		final Location deathLoc = wither.getLocation();

		getCustomWither().removeWither(wither);
		stopWitherTargeting(wither.getUniqueId());
		stopWitherTasks(wither.getUniqueId());

		instance.getHellblockHandler().getHellblockByWorld(deathLoc.getWorld(), deathLoc).thenAccept(islandData -> {
			if (islandData == null) {
				return;
			}

			// schedule sync to safely modify world/entities
			instance.getScheduler().executeSync(() -> {
				// Notify island members
				islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
						.forEach(member -> {
							instance.getSenderFactory().wrap(member).sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_WITHER_DEFEATED.build()));

							if (islandData.getBoundingBox() != null
									&& islandData.getBoundingBox().contains(member.getLocation().toVector())) {
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(member), Sound
										.sound(Key.key("minecraft:entity.wither.death"), Sound.Source.PLAYER, 1f, 1f));

								final FakeFirework firework = VersionHelper.getNMSManager().createFakeFirework(deathLoc,
										Color.BLACK);
								firework.flightTime(0);
								firework.invisible(true);
								firework.spawn(member);
							}
						});

				// Freeze and spin upwards
				wither.setAI(false);
				wither.setGravity(false);

				final AtomicReference<SchedulerTask> spinTaskRef = new AtomicReference<>();
				spinTaskRef.set(instance.getScheduler().sync().runRepeating(new Runnable() {
					int ticks = 0;

					@Override
					public void run() {
						if (ticks++ > 60 || wither.isDead() || !wither.isValid()) {
							wither.remove();
							final SchedulerTask task = spinTaskRef.get();
							if (task != null) {
								task.cancel();
							}
							return;
						}
						wither.teleport(wither.getLocation().add(0, 0.05, 0));
						wither.setRotation(wither.getLocation().getYaw() + 15, wither.getLocation().getPitch());
					}
				}, 0L, 2L, deathLoc));

				// Track item drops
				final List<Item> drops = new ArrayList<>();
				event.getDrops().forEach(stack -> {
					final Item item = deathLoc.getWorld().dropItemNaturally(deathLoc, stack);
					drops.add(item);
				});
				event.getDrops().clear();

				final long startTime = System.currentTimeMillis();
				final AtomicReference<SchedulerTask> beamTaskRef = new AtomicReference<>();
				beamTaskRef.set(instance.getScheduler().sync().runRepeating(() -> {
					drops.removeIf(i -> i == null || !i.isValid() || i.isDead() || i.getItemStack().getAmount() <= 0);

					final long elapsed = System.currentTimeMillis() - startTime;
					if (drops.isEmpty() || elapsed > TimeUnit.MINUTES.toMillis(5)) {
						final SchedulerTask task = beamTaskRef.get();
						if (task != null) {
							task.cancel();
						}
						return;
					}

					final List<Player> coopPlayers = islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer)
							.filter(p -> p != null && p.isOnline()).toList();

					if (!coopPlayers.isEmpty()) {
						sendBlackBeaconBeam(coopPlayers, deathLoc, 20);
					}
				}, 0L, 20L, deathLoc));

				// Challenge progression
				final Player killer = wither.getKiller();
				if (killer != null) {
					instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(user -> instance
							.getChallengeManager().handleChallengeProgression(killer, ActionType.SLAY, wither));
				}
			});
		});
	}

	public boolean isEnhancedWither(@NotNull Wither wither) {
		return wither.getPersistentDataContainer().has(witherKey, PersistentDataType.STRING)
				&& WITHER_KEY.equals(wither.getPersistentDataContainer().get(witherKey, PersistentDataType.STRING));
	}

	private @NotNull WitherStats getWitherStats() {
		if (!instance.getConfigManager().randomStats()) {
			return new WitherStats(instance.getConfigManager().defaultHealth(),
					instance.getConfigManager().defaultStrength());
		}

		final int randomHealth = RandomUtils.generateRandomInt(instance.getConfigManager().randomMinHealth(),
				instance.getConfigManager().randomMaxHealth());
		final double randomStrength = instance.getConfigManager().randomMinStrength()
				+ (instance.getConfigManager().randomMaxStrength() - instance.getConfigManager().randomMinStrength())
						* RandomUtils.generateRandomDouble();

		return new WitherStats(randomHealth, randomStrength);
	}

	public void trySpawnEnhancedWither(@NotNull UserData user) {
		final HellblockData data = user.getHellblockData();

		// must be level 50+
		if (data.getLevel() < 50) {
			return;
		}

		final int islandId = data.getID();
		if (getCustomWither().hasActiveWither(islandId)) {
			return;
		}

		// enforce cooldown (e.g., 30 minutes)
		if (!getCustomWither().canSpawn(islandId, TimeUnit.MINUTES.toMillis(30))) {
			return;
		}

		// require at least one member online & inside island bounding box
		final BoundingBox box = data.getBoundingBox();
		if (box == null) {
			return;
		}

		final boolean hasOnlineInside = data.getPartyPlusOwner().stream().map(Bukkit::getPlayer)
				.filter(p -> p != null && p.isOnline()).anyMatch(p -> box.contains(p.getLocation().toVector()));

		if (!hasOnlineInside) {
			return;
		}

		// 5% random chance
		if (RandomUtils.generateRandomInt(1, 100) > 5) {
			return;
		}

		// get world
		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
		if (worldOpt.isEmpty()) {
			return;
		}

		final World world = worldOpt.get().bukkitWorld();

		// spawn safely at center of island
		final Location center = findSafeSpawn(world, box);

		final WitherStats stats = getWitherStats();
		Wither rawWither = (Wither) world.spawnEntity(center, EntityType.WITHER, false);

		CreatureSpawnEvent event = new CreatureSpawnEvent(rawWither, CreatureSpawnEvent.SpawnReason.CUSTOM);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			rawWither.remove(); // Respect event cancellation
		}

		// enhance & mark
		final RtagEntity taggedWither = new RtagEntity(rawWither);
		taggedWither.setAttributeBase("generic.max_health", stats.health());
		taggedWither.setAttributeBase("generic.attack_damage", stats.strength());
		taggedWither.setAttributeBase("generic.armor", stats.strength());
		taggedWither.setHealth(stats.health());
		final Wither enhancedWither = (Wither) taggedWither.load(); // effectively final

		enhancedWither.getPersistentDataContainer().set(witherKey, PersistentDataType.STRING, WITHER_KEY);
		enhancedWither.setAware(true);

		// Strip block-targeting AI
		enhancedWither.setInvulnerable(true); // prevent spawn explosion
		enhancedWither.setCanPickupItems(false);
		enhancedWither.setRemoveWhenFarAway(false);

		// Only target players (no block targeting)
		enhancedWither.setTarget(null);

		restrictWitherAI(enhancedWither);

		// After a short delay (5 seconds), re-enable normal behavior
		instance.getScheduler().sync().runLater(() -> {
			if (!enhancedWither.isDead() && enhancedWither.isValid()) {
				enhancedWither.setInvulnerable(false);
			}
		}, 20L * 5, center); // 5 seconds after spawn

		getCustomWither().addWither(islandId, enhancedWither, stats);

		// notify island members (spawned)
		data.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(member -> member != null && member.isOnline())
				.forEach(member -> {
					instance.getSenderFactory().wrap(member).sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_WITHER_SPAWNED.build()));

					// play wither spawn sound if member is inside island bounds
					if (box.contains(member.getLocation().toVector())) {
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(member),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.wither.spawn"),
										net.kyori.adventure.sound.Sound.Source.PLAYER, 1f, 1f));
					}
				});

		// Safeguard: despawn if no members remain online in island bounds
		final BoundingBox islandBox = box.clone();
		final long spawnTime = System.currentTimeMillis();

		// Run every 30s to reset bounce counts
		final SchedulerTask resetTask = instance.getScheduler().sync().runRepeating(
				() -> witherBounceCounts.put(enhancedWither.getUniqueId(), 0), 20L * 30, 20L * 30,
				enhancedWither.getLocation());

		// Store this task so it can be cleaned up on Wither death
		witherBounceResetTasks.put(enhancedWither.getUniqueId(), resetTask);

		instance.getScheduler().sync().runRepeating(new Runnable() {
			boolean finished = false; // self-stop flag

			@Override
			public void run() {
				if (finished) {
					return;
				}

				// stop after 5 minutes regardless
				if (System.currentTimeMillis() - spawnTime >= TimeUnit.MINUTES.toMillis(5)) {
					finished = true;
					return;
				}

				if (enhancedWither.isDead() || !enhancedWither.isValid()) {
					finished = true;
					return;
				}
				// (1) Ensure Wither isn’t stuck inside blocks
				ensureNotStuck(enhancedWither, islandBox, data);

				// (2) Keep Wither inside island bounds
				enforceIslandBounds(enhancedWither, islandBox, data);

				// (3) attempt summon minions
				trySummonMinions(enhancedWither, data);

				// (4) set up targeting priority
				startWitherTargeting(enhancedWither);

				final boolean stillOnline = data.getPartyPlusOwner().stream().map(Bukkit::getPlayer)
						.filter(p -> p != null && p.isOnline())
						.anyMatch(p -> islandBox.contains(p.getLocation().toVector()));

				if (!stillOnline) {
					// remove wither silently
					final Location despawnLoc = enhancedWither.getLocation();
					enhancedWither.remove();
					getCustomWither().removeWither(enhancedWither);
					stopWitherTargeting(enhancedWither.getUniqueId());
					stopWitherTasks(enhancedWither.getUniqueId());

					// Notify island members + play vanish sound + particle effect
					data.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
							.forEach(p -> {
								instance.getSenderFactory().wrap(p).sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_WITHER_DESPAWNED.build()));

								// play vanish sound (enderman teleport)
								AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
										Sound.sound(
												net.kyori.adventure.key.Key.key("minecraft:entity.enderman.teleport"),
												net.kyori.adventure.sound.Sound.Source.PLAYER, 1f, 1f));
							});

					// black dust puff at despawn location
					despawnLoc.getWorld().spawnParticle(ParticleUtils.getParticle(Particle.DUST.name()),
							despawnLoc.add(0, 1, 0), 80, // particle count
							0.6, 1.0, 0.6, // spread
							new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.0f));

					finished = true;
				}
			}
		}, 20L, 20L, center); // check every second
	}

	private Location findSafeSpawn(@NotNull World world, @NotNull BoundingBox box) {
		// Start 10 blocks above island top
		final Location center = new Location(world, (box.getMinX() + box.getMaxX()) / 2.0, box.getMaxY() + 10,
				(box.getMinZ() + box.getMaxZ()) / 2.0);

		// Safety scan: move upward until 5 consecutive air blocks are found
		int consecutiveAir = 0;
		for (int y = center.getBlockY(); y < world.getMaxHeight(); y++) {
			final Location checkLoc = new Location(world, center.getX(), y, center.getZ());
			if (checkLoc.getBlock().getType().isAir()) {
				consecutiveAir++;
				if (consecutiveAir >= 5) {
					// Found at least 5 air blocks → safe for Wither bounding box
					return checkLoc.subtract(0, 4, 0); // move back down to start of air column
				}
			} else {
				consecutiveAir = 0; // reset if solid
			}
		}

		// Fallback: just return original center if no better found
		return center;
	}

	private void trySummonMinions(@NotNull Wither wither, @NotNull HellblockData islandData) {
		final int used = mobSummonCount.getOrDefault(wither.getUniqueId(), 0);
		if (used >= 2) {
			return; // max 2 waves
		}

		// 20% chance when checked
		if (RandomUtils.generateRandomInt(1, 100) > 20) {
			return;
		}

		mobSummonCount.put(wither.getUniqueId(), used + 1);

		final Location spawnBase = wither.getLocation();

		// At least 4 mobs in this wave
		final int mobCount = 4 + RandomUtils.generateRandomInt(0, 2); // 4–6 for variation
		for (int i = 0; i < mobCount; i++) {
			final Location spawnLoc = spawnBase.clone().add(RandomUtils.generateRandomInt(-6, 6), 0,
					RandomUtils.generateRandomInt(-6, 6));

			final Entity mob = switch (RandomUtils.generateRandomInt(1, 4)) {
			case 1 -> spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
			case 2 -> spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIFIED_PIGLIN);
			case 3 -> spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
			case 4 -> instance.getWraithHandler().summonWraith(spawnLoc);
			default -> null;
			};

			if (mob instanceof Mob minion) {
				// Mark and start targeting
				instance.getMinionHandler().tagAsMinion(minion);
				instance.getMinionHandler().startMinionTargeting(minion);
			}
		}

		// FX: smoke + summon sound
		spawnBase.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnBase, 50, 1.5, 1.5, 1.5, 0.05);

		final Sound summonSound = Sound.sound(Key.key("minecraft:entity.zombie_villager.converted"),
				Sound.Source.HOSTILE, 1.5f, 0.6f);

		islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
				.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p), summonSound));
	}

	private void startWitherTargeting(@NotNull Wither wither) {
		stopWitherTargeting(wither.getUniqueId());

		final long initialDelay = RandomUtils.generateRandomInt(0, 20);

		final SchedulerTask task = instance.getScheduler().sync().runRepeating(new Runnable() {
			private int ticksSinceLastCheck = 0;

			@Override
			public void run() {
				if (wither.isDead() || !wither.isValid()) {
					stopWitherTargeting(wither.getUniqueId());
					return;
				}

				final List<LivingEntity> nearby = wither.getWorld().getNearbyEntities(wither.getLocation(), 50, 25, 50)
						.stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).toList();

				final LivingEntity currentTarget = wither.getTarget();

				// --- Failsafe validation ---
				if (currentTarget != null) {
					if (!currentTarget.isValid() || currentTarget.isDead() || !nearby.contains(currentTarget)) {
						wither.setTarget(null);
					} else if (currentTarget instanceof Player player) {
						// async island lookup
						instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
								.thenAccept(islandData -> {
									if (islandData == null
											|| !islandData.getPartyPlusOwner().contains(player.getUniqueId())) {
										// outsider → clear immediately on main thread
										instance.getScheduler().executeSync(() -> wither.setTarget(null));
									}
								});
						return; // keep checking in next cycle
					} else if (currentTarget instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm)) {
						return; // still valid Hell Golem
					} else {
						wither.setTarget(null); // invalid entity type
					}
				}

				ticksSinceLastCheck++;

				// Only re-check every 5s if current target is still valid
				if (ticksSinceLastCheck < 100 && currentTarget != null && currentTarget.isValid()
						&& !currentTarget.isDead()) {
					return;
				}
				ticksSinceLastCheck = 0;

				final LivingEntity targetNow = currentTarget; // effectively final

				// --- Select new target ---
				instance.getHellblockHandler().getHellblockByWorld(wither.getWorld(), wither.getLocation())
						.thenAccept(islandData -> {
							if (islandData == null) {
								instance.getScheduler().executeSync(() -> wither.setTarget(null));
								return;
							}

							final Set<UUID> coopMembers = islandData.getPartyPlusOwner();
							LivingEntity newTarget = null;

							// 1) Coop players
							final List<Player> coopNearby = nearby.stream().filter(
									e -> e instanceof Player player && coopMembers.contains(player.getUniqueId()))
									.map(e -> (Player) e).toList();
							if (!coopNearby.isEmpty()) {
								newTarget = RandomUtils.getRandomElement(coopNearby);
							}

							// 2) Hell Golems (if no players)
							if (newTarget == null) {
								final List<Snowman> golemsNearby = nearby.stream().filter(
										e -> e instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm))
										.map(e -> (Snowman) e).toList();
								if (!golemsNearby.isEmpty()) {
									newTarget = RandomUtils.getRandomElement(golemsNearby);
								}
							}

							// --- Apply new target with stickiness ---
							if (newTarget != null) {
								if (targetNow != null && RandomUtils.generateRandomInt(1, 100) <= 80) {
									return; // stick with current target
								}
								final LivingEntity finalNewTarget = newTarget;
								instance.getScheduler().executeSync(() -> {
									if (finalNewTarget != wither.getTarget()) {
										wither.setTarget(finalNewTarget);
									}
								});
							} else {
								instance.getScheduler().executeSync(() -> wither.setTarget(null));
							}
						});
			}
		}, initialDelay, 20L, wither.getLocation());

		witherTargetTasks.put(wither.getUniqueId(), task);
	}

	private void stopWitherTargeting(@NotNull UUID witherId) {
		final SchedulerTask t = witherTargetTasks.remove(witherId);
		if (t != null) {
			t.cancel();
		}
	}

	private void enforceIslandBounds(@NotNull Wither wither, @NotNull BoundingBox islandBox,
			@NotNull HellblockData islandData) {
		final Location loc = wither.getLocation();
		final World world = loc.getWorld();
		if (world == null) {
			return;
		}

		if (!islandBox.contains(loc.toVector())) {
			final Location center = islandBox.getCenter().toLocation(world);

			if (loc.distance(center) > 50) {
				wither.remove();
				getCustomWither().removeWither(wither);
				witherBounceCounts.remove(wither.getUniqueId());
				return;
			}

			final int bounces = witherBounceCounts.getOrDefault(wither.getUniqueId(), 0) + 1;
			witherBounceCounts.put(wither.getUniqueId(), bounces);

			if (bounces <= 5) {
				// bounce back
				final Vector toCenter = center.toVector().subtract(loc.toVector()).normalize();
				final Vector bounce = toCenter.multiply(2.0).add(new Vector(0, 1.0, 0));
				wither.setVelocity(bounce);

				world.spawnParticle(Particle.LARGE_SMOKE, loc, 25, 1.0, 1.0, 1.0, 0.1);
				world.spawnParticle(Particle.CLOUD, loc, 15, 0.8, 0.8, 0.8, 0.05);

				islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
						.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
								Sound.sound(Key.key("minecraft:block.anvil.land"), Sound.Source.HOSTILE, 1f, 0.8f)));
			} else {
				// teleport safely
				final Location safeLoc = findSafeLocation(world, center.add(0, 10, 0), 20);
				wither.teleport(safeLoc);

				world.spawnParticle(Particle.PORTAL, safeLoc, 40, 0.8, 1.2, 0.8, 0.2);
				world.spawnParticle(Particle.LARGE_SMOKE, safeLoc, 20, 0.6, 1.0, 0.6, 0.05);

				islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
						.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p), Sound
								.sound(Key.key("minecraft:entity.enderman.teleport"), Sound.Source.HOSTILE, 1f, 1f)));

				witherBounceCounts.put(wither.getUniqueId(), 0);
			}
		}
	}

	private void ensureNotStuck(@NotNull Wither wither, @NotNull BoundingBox islandBox,
			@NotNull HellblockData islandData) {
		final Location loc = wither.getLocation();
		final World world = loc.getWorld();
		if (world == null) {
			return;
		}

		final org.bukkit.util.BoundingBox bb = wither.getBoundingBox();

		int solidBlocks = 0;
		int totalBlocks = 0;

		for (int x = (int) Math.floor(bb.getMinX()); x <= Math.floor(bb.getMaxX()); x++) {
			for (int y = (int) Math.floor(bb.getMinY()); y <= Math.floor(bb.getMaxY()); y++) {
				for (int z = (int) Math.floor(bb.getMinZ()); z <= Math.floor(bb.getMaxZ()); z++) {
					final Block block = world.getBlockAt(x, y, z);
					totalBlocks++;
					if (block.getType().isSolid()) {
						solidBlocks++;
					}
				}
			}
		}

		final boolean trapped = totalBlocks > 0 && solidBlocks > totalBlocks * 0.6;

		if (loc.getBlock().getType().isSolid() || trapped) {
			final Location safeLoc = findSafeLocation(world, islandBox.getCenter().toLocation(world).add(0, 8, 0), 20);
			wither.teleport(safeLoc);

			world.spawnParticle(Particle.PORTAL, safeLoc, 40, 0.8, 1.2, 0.8, 0.2);
			world.spawnParticle(Particle.LARGE_SMOKE, safeLoc, 20, 0.6, 1.0, 0.6, 0.05);

			islandData.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(p -> p != null && p.isOnline())
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							Sound.sound(Key.key("minecraft:entity.enderman.teleport"), Sound.Source.HOSTILE, 1f, 1f)));
		}
	}

	private void stopWitherTasks(@NotNull UUID witherId) {
		stopWitherTargeting(witherId);

		final SchedulerTask reset = witherBounceResetTasks.remove(witherId);
		if (reset != null) {
			reset.cancel();
		}

		witherBounceCounts.remove(witherId);
		healedWithers.remove(witherId);
		mobSummonCount.remove(witherId);
	}

	/**
	 * Find a safe teleport spot inside the island box near the target location.
	 * Ensures the Wither isn't teleported into solid blocks or traps.
	 */
	private Location findSafeLocation(@NotNull World world, @NotNull Location base, int maxSearch) {
		final Location check = base.clone();

		for (int dy = 0; dy <= maxSearch; dy++) {
			final Location up = check.clone().add(0, dy, 0);
			if (isLocationSafeForWither(up)) {
				return up;
			}

			final Location down = check.clone().subtract(0, dy, 0);
			if (dy > 0 && isLocationSafeForWither(down)) {
				return down;
			}
		}

		// Fallback: just return the base (might still be unsafe)
		return base;
	}

	/**
	 * Check whether the Wither can safely occupy this location.
	 */
	private boolean isLocationSafeForWither(@NotNull Location loc) {
		final World world = loc.getWorld();
		if (world == null) {
			return false;
		}

		// Withers are ~3x3x3
		final org.bukkit.util.BoundingBox bb = org.bukkit.util.BoundingBox.of(loc, 3.0, 3.0, 3.0);

		int solidBlocks = 0;
		int totalBlocks = 0;

		for (int x = (int) Math.floor(bb.getMinX()); x <= Math.floor(bb.getMaxX()); x++) {
			for (int y = (int) Math.floor(bb.getMinY()); y <= Math.floor(bb.getMaxY()); y++) {
				for (int z = (int) Math.floor(bb.getMinZ()); z <= Math.floor(bb.getMaxZ()); z++) {
					final Block block = world.getBlockAt(x, y, z);
					totalBlocks++;
					if (block.getType().isSolid()) {
						solidBlocks++;
					}
				}
			}
		}

		// Consider safe if <30% of bounding box is solid
		return totalBlocks > 0 && solidBlocks < totalBlocks * 0.3;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void sendBlackBeaconBeam(@NotNull Collection<Player> viewers, @NotNull Location location,
			int durationTicks) {
		try {
			// --- NMS Setup ---
			final Class<?> packetPlayOutSpawnEntity = getNMSClass("network.protocol.game.PacketPlayOutSpawnEntity");
			final Class<?> packetPlayOutEntityMetadata = getNMSClass(
					"network.protocol.game.PacketPlayOutEntityMetadata");
			final Class<?> packetPlayOutEntityDestroy = getNMSClass("network.protocol.game.PacketPlayOutEntityDestroy");
			final Class<?> dataWatcherClass = getNMSClass("network.syncher.DataWatcher");
			final Class<?> entityTypeClass = getNMSClass("world.entity.EntityType");
			final Class<?> vec3DClass = getNMSClass("world.phys.Vec3D");

			final int entityId = RandomUtils.generateRandomInt(Integer.MAX_VALUE);
			final double x = location.getX(), y = location.getY(), z = location.getZ();
			final Object vec3D = vec3DClass.getConstructor(double.class, double.class, double.class).newInstance(0d, 0d,
					0d);

			// --- Create Entity Spawn ---
			final Object spawnPacket = packetPlayOutSpawnEntity
					.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class,
							float.class, entityTypeClass, int.class, vec3DClass, double.class)
					.newInstance(entityId, UUID.randomUUID(), x, y, z, 0f, 0f,
							Enum.valueOf((Class<Enum>) entityTypeClass, "ENDER_CRYSTAL"), 0, vec3D, 0d);

			final Object dataWatcher = dataWatcherClass.getConstructor().newInstance();
			final Object metaPacket = packetPlayOutEntityMetadata
					.getConstructor(int.class, dataWatcherClass, boolean.class)
					.newInstance(entityId, dataWatcher, true);

			viewers.forEach(viewer -> {
				sendPacket(viewer, spawnPacket);
				sendPacket(viewer, metaPacket);
			});

			final int worldMax = location.getWorld().getMaxHeight();
			final AtomicReference<SchedulerTask> taskRef = new AtomicReference<>();

			// --- Scheduler Animation Task ---
			final SchedulerTask task = instance.getScheduler().sync().runRepeating(new Runnable() {
				int ticks = 0;

				@Override
				public void run() {
					ticks++;

					// Stop condition
					if (ticks > durationTicks) {
						try {
							final Object destroyPacket = createDestroyPacket(packetPlayOutEntityDestroy, entityId);

							viewers.forEach(viewer -> sendPacket(viewer, destroyPacket));

						} catch (Exception ex) {
							ex.printStackTrace();
						} finally {
							SchedulerTask t = taskRef.get();
							if (t != null)
								t.cancel();
						}
						return;
					}

					// --- Beam Animation ---
					double pulseStrength = (Math.sin(ticks / 6.0) + 1) / 2.0;
					float size = 1.5f + (float) pulseStrength * 1.2f;

					for (int yOffset = 0; y + yOffset <= worldMax; yOffset += 3) {
						Location loc = location.clone().add(0, yOffset, 0);
						location.getWorld().spawnParticle(Particle.DUST, loc,
								Math.max(1, (int) (4 + pulseStrength * 8)), 0.15, 0.4, 0.15,
								new Particle.DustOptions(Color.fromRGB(0, 0, 0), size));
					}

					location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location.clone().add(0, 0.2, 0),
							10 + (int) (pulseStrength * 20), 1.0, 0.1, 1.0, 0.02);

					location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, 0.5, 0),
							8 + (int) (pulseStrength * 20), 0.7, 0.2, 0.7,
							new Particle.DustOptions(Color.fromRGB(30, 30, 30), size));
				}
			}, 0L, 10L, location);

			taskRef.set(task);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private Object createDestroyPacket(Class<?> packetDestroyClass, int entityId) throws Exception {
		try {
			Constructor<?> oldCtor = packetDestroyClass.getConstructor(int[].class);
			return oldCtor.newInstance((Object) new int[] { entityId });
		} catch (NoSuchMethodException ex) {
			// 1.20.2+ switched to int constructor
			Constructor<?> newCtor = packetDestroyClass.getConstructor(int.class);
			return newCtor.newInstance(entityId);
		}
	}

	// --- Shared Utilities ---
	private static Class<?> getNMSClass(String path) throws ClassNotFoundException {
		return Class.forName("net.minecraft." + path);
	}

	private static Object findFieldByType(Object instance, String simpleTypeName) throws Exception {
		for (Field f : instance.getClass().getFields()) {
			if (f.getType().getSimpleName().equalsIgnoreCase(simpleTypeName)) {
				return f.get(instance);
			}
		}
		return null;
	}

	private static void sendPacket(Player player, Object packet) {
		try {
			Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);

			// Dynamically locate the connection field (changes between versions)
			Object connection = null;
			for (Field f : craftPlayer.getClass().getFields()) {
				if (f.getType().getSimpleName().toLowerCase().contains("servergamepacketlistener")) {
					connection = f.get(craftPlayer);
					break;
				}
			}

			if (connection == null) {
				throw new IllegalStateException(
						"Could not find PlayerConnection / ServerGamePacketListenerImpl field!");
			}

			Method sendPacket = connection.getClass().getMethod("sendPacket", getNMSClass("network.protocol.Packet"));
			sendPacket.invoke(connection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void restrictWitherAI(@NotNull Wither wither) {
		try {
			Object craftWither = wither; // CraftWither
			Method getHandle = craftWither.getClass().getMethod("getHandle");
			Object nmsWither = getHandle.invoke(craftWither);

			// Reflectively locate goal + target selectors
			Object goalSelector = findFieldByType(nmsWither, "GoalSelector");
			Object targetSelector = findFieldByType(nmsWither, "GoalSelector");

			if (goalSelector == null || targetSelector == null)
				throw new IllegalStateException("Could not find goal/target selector fields");

			// Get available goals set
			Set<?> goals;
			try {
				Method getAvailableGoals = goalSelector.getClass().getMethod("getAvailableGoals");
				goals = (Set<?>) getAvailableGoals.invoke(goalSelector);
			} catch (NoSuchMethodException e) {
				Field goalsField = goalSelector.getClass().getDeclaredField("availableGoals");
				goalsField.setAccessible(true);
				goals = (Set<?>) goalsField.get(goalSelector);
			}
			goals.clear();

			Set<?> targets;
			try {
				Method getAvailableGoals = targetSelector.getClass().getMethod("getAvailableGoals");
				targets = (Set<?>) getAvailableGoals.invoke(targetSelector);
			} catch (NoSuchMethodException e) {
				Field goalsField = targetSelector.getClass().getDeclaredField("availableGoals");
				goalsField.setAccessible(true);
				targets = (Set<?>) goalsField.get(targetSelector);
			}
			targets.clear();

			// Player class changes depending on version
			Class<?> playerClass;
			try {
				playerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
			} catch (ClassNotFoundException e) {
				playerClass = Class.forName("net.minecraft.world.entity.player.EntityHuman");
			}

			Class<?> pathfinderGoalClass = Class
					.forName("net.minecraft.world.entity.ai.goal.PathfinderGoalNearestAttackableTarget");
			Class<?> mobClass = Class.forName("net.minecraft.world.entity.Mob");

			Constructor<?> ctor = null;
			for (Constructor<?> c : pathfinderGoalClass.getConstructors()) {
				Class<?>[] params = c.getParameterTypes();
				if (params.length == 3 && params[0].isAssignableFrom(mobClass) && params[1].equals(Class.class)
						&& params[2] == boolean.class) {
					ctor = c;
					break;
				}
			}

			if (ctor == null)
				throw new RuntimeException("Could not find PathfinderGoalNearestAttackableTarget constructor!");

			Object goal = ctor.newInstance(nmsWither, playerClass, true);

			Method addGoal = targetSelector.getClass().getMethod("addGoal", int.class,
					Class.forName("net.minecraft.world.entity.ai.goal.PathfinderGoal"));
			addGoal.invoke(targetSelector, 2, goal);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class CustomWither {
		private final Map<Entity, WitherStats> witherStats = new HashMap<>();
		private final Map<Integer, Wither> activeIslandWithers = new HashMap<>(); // islandId → Wither
		private final Map<Integer, Long> spawnCooldowns = new HashMap<>(); // islandId → timestamp

		public void addWither(@NotNull int islandId, @NotNull Wither wither, @NotNull WitherStats stats) {
			witherStats.put(wither, stats);
			activeIslandWithers.put(islandId, wither);
			spawnCooldowns.put(islandId, System.currentTimeMillis());
		}

		public void removeWither(@NotNull Wither wither) {
			witherStats.remove(wither);
			activeIslandWithers.values().remove(wither);
		}

		public boolean hasActiveWither(@NotNull int islandId) {
			return activeIslandWithers.containsKey(islandId);
		}

		public boolean canSpawn(@NotNull int islandId, long cooldownMillis) {
			final long last = spawnCooldowns.getOrDefault(islandId, 0L);
			return System.currentTimeMillis() - last >= cooldownMillis;
		}

		public @Nullable WitherStats getWitherStats(@NotNull Wither wither) {
			return witherStats.get(wither);
		}
	}

	private record WitherStats(int health, double strength) {
	}
}