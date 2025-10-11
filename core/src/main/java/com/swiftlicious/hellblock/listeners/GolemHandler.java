package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;

/**
 * GolemHandler — spawns Hell Golems and runs their targeting + damage logic.
 */
public class GolemHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final String HELLGOLEM_KEY = "hell_golem";

	private NamespacedKey hellGolemKey;

	// Track per-golem targeting tasks so we can cancel them cleanly on death /
	// unload
	private final Map<UUID, SchedulerTask> golemTargetTasks = new ConcurrentHashMap<>();
	private final Map<UUID, SchedulerTask> golemAuraTasks = new ConcurrentHashMap<>();

	public GolemHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		hellGolemKey = new NamespacedKey(instance, HELLGOLEM_KEY);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		// cancel all golem tasks
		golemTargetTasks.values().stream().filter(task -> task != null).forEach(SchedulerTask::cancel);
		golemTargetTasks.clear();
		golemAuraTasks.values().stream().filter(task -> task != null).forEach(SchedulerTask::cancel);
		golemAuraTasks.clear();
	}

	private @NotNull List<Block> checkHellGolemBuild(@NotNull Location location) {
		final World world = location.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return List.of();
		}

		final Block base = location.getBlock();
		final List<Block> result = new ArrayList<>();

		// Helper: force normal fire into soul fire with FX
		final BiConsumer<Block, Block> forceSoulFire = (fireBlock, jackOLantern) -> {
			if (fireBlock.getType() == Material.FIRE) {
				fireBlock.setType(Material.SOUL_FIRE);
			}

			final Location fxLoc = fireBlock.getLocation().add(0.5, 0.5, 0.5);

			// Particles: soul flames + campfire smoke
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, fxLoc, 12, 0.35, 0.35, 0.35, 0.01);
			world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, fxLoc, 6, 0.25, 0.4, 0.25, 0.01);

			// Jack o’ Lantern flash (glow + light pulse)
			// Jack o’ Lantern flash (glow + light pulse with fade)
			if (jackOLantern != null && jackOLantern.getType() == Material.JACK_O_LANTERN) {
				final Location jackLoc = jackOLantern.getLocation().add(0.5, 0.5, 0.5);

				// Glow particles
				world.spawnParticle(Particle.GLOW, jackLoc, 16, 0.4, 0.4, 0.4, 0.05);
				world.spawnParticle(Particle.END_ROD, jackLoc, 8, 0.25, 0.25, 0.25, 0.02);

				// Temporary fading light pulse (requires LIGHT block support, 1.17+)
				final Block block = jackOLantern.getLocation().getBlock();
				final Material original = block.getType();

				// Replace with LIGHT at max brightness
				block.setBlockData(Bukkit.createBlockData("minecraft:light[level=15]"), false);

				// Fade steps: 15 → 10 → 5 → original
				instance.getScheduler().sync().runLater(() -> {
					if (block.getType() == Material.LIGHT) {
						block.setBlockData(Bukkit.createBlockData("minecraft:light[level=10]"), false);
					}
				}, 4L, jackLoc);

				instance.getScheduler().sync().runLater(() -> {
					if (block.getType() == Material.LIGHT) {
						block.setBlockData(Bukkit.createBlockData("minecraft:light[level=5]"), false);
					}
				}, 8L, jackLoc);

				instance.getScheduler().sync().runLater(() -> {
					if (block.getType() == Material.LIGHT) {
						block.setType(original, false); // restore Jack O Lantern
					}
				}, 12L, jackLoc);
			}

			// Sound with randomized pitch
			final float pitch = 1.0f
					+ (RandomUtils.generateRandomFloat(0, 1) - RandomUtils.generateRandomFloat(0, 1)) * 0.4f;

			world.getNearbyEntities(fxLoc, 16, 16, 16, e -> e instanceof Player).stream().map(e -> (Player) e)
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							net.kyori.adventure.sound.Sound.sound(
									net.kyori.adventure.key.Key.key("minecraft:entity.generic.extinguish_fire"),
									net.kyori.adventure.sound.Sound.Source.BLOCK, 0.6f, pitch)));
		};

		// Pattern 1: Jack O Lantern + 2x Soul Soil + Soul Fire
		if (base.getType() == Material.JACK_O_LANTERN
				&& base.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.DOWN, 2).getType() == Material.SOUL_SOIL
				&& (base.getRelative(BlockFace.UP).getType() == Material.FIRE
						|| base.getRelative(BlockFace.UP).getType() == Material.SOUL_FIRE)) {
			final Block jack = base;
			final Block top = base.getRelative(BlockFace.UP);
			forceSoulFire.accept(top, jack);
			result.addAll(List.of(jack, base.getRelative(BlockFace.DOWN), base.getRelative(BlockFace.DOWN, 2), top));
		}

		// Pattern 2: Soul Fire + Jack O Lantern + 2x Soul Soil
		if ((base.getType() == Material.FIRE || base.getType() == Material.SOUL_FIRE)
				&& base.getRelative(BlockFace.DOWN).getType() == Material.JACK_O_LANTERN
				&& base.getRelative(BlockFace.DOWN, 2).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.DOWN, 3).getType() == Material.SOUL_SOIL) {
			final Block jack = base.getRelative(BlockFace.DOWN);
			forceSoulFire.accept(base, jack);
			result.addAll(
					List.of(base, jack, base.getRelative(BlockFace.DOWN, 2), base.getRelative(BlockFace.DOWN, 3)));
		}

		// Pattern 3: 2 Soul Soil + Jack O Lantern + Soul Fire
		if (base.getType() == Material.SOUL_SOIL && base.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.UP).getType() == Material.JACK_O_LANTERN
				&& (base.getRelative(BlockFace.UP, 2).getType() == Material.FIRE
						|| base.getRelative(BlockFace.UP, 2).getType() == Material.SOUL_FIRE)) {
			final Block jack = base.getRelative(BlockFace.UP);
			final Block top = base.getRelative(BlockFace.UP, 2);
			forceSoulFire.accept(top, jack);
			result.addAll(List.of(base, base.getRelative(BlockFace.DOWN), jack, top));
		}

		// Pattern 4: 2 Soul Soil + Jack O Lantern + Soul Fire on top
		if (base.getType() == Material.SOUL_SOIL && base.getRelative(BlockFace.UP).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.UP, 2).getType() == Material.JACK_O_LANTERN
				&& (base.getRelative(BlockFace.UP, 3).getType() == Material.FIRE
						|| base.getRelative(BlockFace.UP, 3).getType() == Material.SOUL_FIRE)) {
			final Block jack = base.getRelative(BlockFace.UP, 2);
			final Block top = base.getRelative(BlockFace.UP, 3);
			forceSoulFire.accept(top, jack);
			result.addAll(List.of(base, base.getRelative(BlockFace.UP), jack, top));
		}

		return result;
	}

	@SuppressWarnings("deprecation")
	private void spawnHellGolem(@NotNull Player player, @NotNull UUID islandOwner, @NotNull Location location) {
		final World world = location.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return;
		}

		final List<Block> structure = checkHellGolemBuild(location);
		if (structure.isEmpty()) {
			return;
		}

		instance.getStorageManager().getOfflineUserData(islandOwner, instance.getConfigManager().lockData())
				.thenAccept(optionalUser -> {
					if (optionalUser.isEmpty()) {
						return;
					}

					final UserData offlineUser = optionalUser.get();
					if (offlineUser.getHellblockData()
							.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW) {
						return;
					}

					instance.getScheduler().executeSync(() -> {
						// clear structure
						structure.forEach(block -> block.setType(Material.AIR));

						// --- Summoning burst FX ---
						final Location fxLoc = location.clone().add(0.5, 0, 0.5);

						// Big soul flames + smoke
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, fxLoc, 40, 1.5, 1.5, 1.5, 0.05);
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, fxLoc, 25, 1.2, 1.5, 1.2, 0.05);

						// Sound burst (deep + fiery)
						world.getNearbyEntities(fxLoc, 32, 32, 32, e -> e instanceof Player).stream()
								.map(e -> (Player) e)
								.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
										net.kyori.adventure.sound.Sound.sound(
												net.kyori.adventure.key.Key.key("minecraft:entity.blaze.shoot"),
												net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.5f, 0.6f)));

						world.getNearbyEntities(fxLoc, 32, 32, 32, e -> e instanceof Player).stream()
								.map(e -> (Player) e)
								.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
										net.kyori.adventure.sound.Sound.sound(
												net.kyori.adventure.key.Key.key("minecraft:block.fire.extinguish"),
												net.kyori.adventure.sound.Sound.Source.BLOCK, 0.8f, 0.9f)));

						// spawn hell golem
						final Snowman hellGolem = (Snowman) world.spawnEntity(location, EntityType.SNOW_GOLEM);
						hellGolem.setAware(true);
						hellGolem.setDerp(false);
						hellGolem.setVisualFire(true);
						hellGolem.getPersistentDataContainer().set(hellGolemKey, PersistentDataType.STRING,
								HELLGOLEM_KEY);

						// --- Permanent aura FX ---
						final SchedulerTask auraTask = instance.getScheduler().sync().runRepeating(() -> {
							if (hellGolem.isDead() || !hellGolem.isValid()) {
								stopHellGolemAura(hellGolem.getUniqueId());
								return;
							}
							final Location auraLoc = hellGolem.getLocation().add(0, 1, 0);
							world.spawnParticle(Particle.SOUL_FIRE_FLAME, auraLoc, 2, 0.2, 0.3, 0.2, 0.01);
							world.spawnParticle(Particle.SCULK_SOUL, auraLoc, 1, 0.2, 0.3, 0.2, 0.01);
						}, 0L, 10L, hellGolem.getLocation()); // every 0.5s

						golemAuraTasks.put(hellGolem.getUniqueId(), auraTask);

						// --- Aura FX on spawn ---
						final Location golemLoc = hellGolem.getLocation().add(0, 1, 0);

						// Initial flash
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, golemLoc, 30, 0.6, 1.0, 0.6, 0.05);
						world.spawnParticle(Particle.SCULK_SOUL, golemLoc, 12, 0.4, 0.8, 0.4, 0.05);
						world.spawnParticle(Particle.LARGE_SMOKE, golemLoc, 15, 0.6, 0.8, 0.6, 0.02);

						// Summoning hum sound
						world.getNearbyEntities(golemLoc, 32, 32, 32, e -> e instanceof Player).stream()
								.map(e -> (Player) e)
								.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
										net.kyori.adventure.sound.Sound.sound(
												net.kyori.adventure.key.Key.key("minecraft:block.beacon.activate"),
												net.kyori.adventure.sound.Sound.Source.BLOCK, 1.2f, 0.7f)));

						// Lingering aura for ~2 seconds
						instance.getScheduler().sync().runRepeating(new Runnable() {
							int ticks = 0;

							@Override
							public void run() {
								if (hellGolem.isDead() || !hellGolem.isValid() || ticks > 40) {
									return; // stop aura after 2s
								}
								final Location auraLoc = hellGolem.getLocation().add(0, 1, 0);
								world.spawnParticle(Particle.SOUL_FIRE_FLAME, auraLoc, 6, 0.3, 0.5, 0.3, 0.01);
								ticks += 5;
							}
						}, 0L, 5L, hellGolem.getLocation());

						// start AI targeting loop and track the task for cancellation
						startHellGolemTargeting(hellGolem);
					});
				});
	}

	private void startHellGolemTargeting(@NotNull Snowman golem) {
		stopHellGolemTargeting(golem.getUniqueId());

		final long initialDelay = RandomUtils.generateRandomInt(0, 20);

		final SchedulerTask task = instance.getScheduler().sync().runRepeating(new Runnable() {
			// Track how long we’ve stuck to current target
			private int ticksSinceLastCheck = 0;

			@Override
			public void run() {
				if (golem.isDead() || !golem.isValid()) {
					stopHellGolemTargeting(golem.getUniqueId());
					return;
				}

				final List<LivingEntity> nearby = golem.getWorld().getNearbyEntities(golem.getLocation(), 25, 15, 25)
						.stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).toList();

				// Validate current target
				LivingEntity currentTarget = golem.getTarget();
				if (currentTarget != null) {
					if (!currentTarget.isValid() || currentTarget.isDead() || !nearby.contains(currentTarget)) {
						golem.setTarget(null);
						currentTarget = null;
					}
				}

				ticksSinceLastCheck++;

				// Only re-evaluate every 5 seconds (100 ticks)
				if (ticksSinceLastCheck < 100 && currentTarget != null) {
					return;
				}
				ticksSinceLastCheck = 0;

				final LivingEntity newTarget = pickRandomBestTarget(nearby);

				if (newTarget != null) {
					// 80% chance to stick with current target if still valid
					if (currentTarget != null && RandomUtils.generateRandomInt(1, 100) <= 80) {
						return; // stick with old target
					}

					if (newTarget != currentTarget) {
						golem.setTarget(newTarget);
					}
				} else {
					golem.setTarget(null);
				}
			}
		}, initialDelay, 20L, golem.getLocation()); // runs every second

		golemTargetTasks.put(golem.getUniqueId(), task);
	}

	private void stopHellGolemTargeting(@NotNull UUID golemId) {
		final SchedulerTask task = golemTargetTasks.remove(golemId);
		if (task != null) {
			task.cancel();
		}
	}

	private void stopHellGolemAura(@NotNull UUID golemId) {
		final SchedulerTask task = golemAuraTasks.remove(golemId);
		if (task != null) {
			task.cancel();
		}
	}

	/*
	 * Priority: lower number = higher priority 1 -> Wraith (tagged WitherSkeleton)
	 * 2 -> normal Wither minions (WitherSkeleton, Blaze, PigZombie) 3 -> Enhanced
	 * Wither 4 -> other hostiles (but never other Hell Golems)
	 */
	private int getTargetPriority(@NotNull LivingEntity entity) {
		if (entity instanceof Snowman sm && isHellGolem(sm)) {
			return Integer.MAX_VALUE; // never target Hell Golems
		}
		if (entity instanceof WitherSkeleton ws && instance.getWraithHandler().isWraith(ws)
				&& instance.getMinionHandler().isMinion(entity)) {
			return 1;
		}
		if (instance.getMinionHandler().isMinion(entity)) {
			return 2;
		}
		if (entity instanceof Wither w && instance.getWitherHandler().isEnhancedWither(w)) {
			return 3;
		}
		return entity instanceof Monster ? 4 : Integer.MAX_VALUE;
	}

	private LivingEntity pickRandomBestTarget(@NotNull List<LivingEntity> nearby) {
		// Group entities by priority
		final Map<Integer, List<LivingEntity>> grouped = new HashMap<>();
		int bestPriority = Integer.MAX_VALUE;

		for (LivingEntity entity : nearby) {
			final int priority = getTargetPriority(entity);
			if (priority == Integer.MAX_VALUE) {
				continue;
			}

			grouped.computeIfAbsent(priority, k -> new ArrayList<>()).add(entity);
			bestPriority = Math.min(bestPriority, priority);
		}

		// Nothing valid
		if (bestPriority == Integer.MAX_VALUE) {
			return null;
		}

		// Pick random from best priority group
		final List<LivingEntity> bestGroup = grouped.get(bestPriority);
		return RandomUtils.getRandomElement(bestGroup);
	}

	public boolean isHellGolem(@NotNull Snowman sm) {
		return sm.getPersistentDataContainer().has(hellGolemKey, PersistentDataType.STRING)
				&& HELLGOLEM_KEY.equals(sm.getPersistentDataContainer().get(hellGolemKey, PersistentDataType.STRING));
	}

	@EventHandler
	public void onCreationOfHellGolem(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlockPlaced();
		instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenAccept(ownerUUID -> {
			if (ownerUUID != null) {
				spawnHellGolem(player, ownerUUID, block.getLocation());
			}
		});
	}

	@EventHandler
	public void onPistonPushCreationOfHellGolem(BlockPistonExtendEvent event) {
		if (event.getBlocks().isEmpty()) {
			return;
		}

		for (Block block : event.getBlocks()) {
			if (!instance.getHellblockHandler().isInCorrectWorld(block.getWorld())) {
				continue;
			}

			final Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25, 25,
					e -> e.getType() == EntityType.PLAYER);

			final Player closest = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (closest != null) {
				instance.getCoopManager().getHellblockOwnerOfVisitingIsland(closest).thenAccept(ownerUUID -> {
					if (ownerUUID != null) {
						spawnHellGolem(closest, ownerUUID, block.getLocation());
					}
				});
			}
		}
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		final Entity entity = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld())) {
			return;
		}
		if (entity instanceof Snowman snowman && isHellGolem(snowman)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		final Entity entity = event.getEntity();
		if (!instance.getHellblockHandler().isInCorrectWorld(entity.getWorld())) {
			return;
		}
		if (entity instanceof Snowman snowman && isHellGolem(snowman)
				&& event.getDamageSource().getDamageType() == DamageType.ON_FIRE) {
			event.setCancelled(true);
		}
	}

	/**
	 * Modified snowball handler: - sets fire visual & ticks (existing) - applies
	 * bonus damage vs Enhanced Wither and Wither-minions (including tagged Wraith)
	 * - plays particles/sound on bonus hit
	 */
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSnowball(EntityDamageByEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}

		if (!(event.getEntity() instanceof LivingEntity living && event.getDamager() instanceof Snowball snowball
				&& snowball.getShooter() != null && snowball.getShooter() instanceof Snowman shooter
				&& isHellGolem(shooter))) {
			return;
		}

		// visual
		snowball.setVisualFire(true);
		living.setFireTicks(40);

		// determine base damage — fallback if event damage is 0
		double baseDamage = event.getDamage();
		if (baseDamage <= 0.0) {
			baseDamage = 2.0; // fallback base
		}

		// enhanced damage and FX
		if (instance.getMinionHandler().isMinion(living)) {
			final double newDamage = baseDamage * 1.5;
			event.setDamage(newDamage);

			// FX
			living.getWorld().spawnParticle(Particle.LARGE_SMOKE, living.getLocation().add(0, 0.5, 0), 8, 0.25, 0.25,
					0.25, 0.02);
			living.getWorld().getNearbyEntities(living.getLocation(), 16, 16, 16, e -> e instanceof Player).stream()
					.map(e -> (Player) e)
					.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							net.kyori.adventure.sound.Sound.sound(
									net.kyori.adventure.key.Key.key("minecraft:entity.blaze.hurt"),
									net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.8f, 1.1f)));

		} else if (living instanceof Wither wither && instance.getWitherHandler().isEnhancedWither(wither)) {
			final double newDamage = baseDamage * 1.25;
			event.setDamage(newDamage);

			// FX
			living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);

			living.getWorld().getNearbyEntities(living.getLocation(), 32, 32, 32, e -> e instanceof Player).stream()
					.map(e -> (Player) e)
					.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							net.kyori.adventure.sound.Sound.sound(
									net.kyori.adventure.key.Key.key("minecraft:entity.wither.hurt"),
									net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.0f, 1.0f)));

		} else {
			// default damage - keep baseDamage
			event.setDamage(baseDamage);
		}
	}

	@EventHandler
	public void onSnowmanDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.SNOW_GOLEM) {
			return;
		}

		final Snowman snowman = (Snowman) event.getEntity();
		if (!isHellGolem(snowman)) {
			return;
		}

		// cancel the golem's targeting task
		stopHellGolemTargeting(snowman.getUniqueId());
		stopHellGolemAura(snowman.getUniqueId());

		final Player killer = snowman.getKiller();
		if (killer == null) {
			return;
		}

		instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(
				user -> instance.getChallengeManager().handleChallengeProgression(killer, ActionType.SLAY, snowman));
	}
}