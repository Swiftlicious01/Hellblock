package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.DamageUtil;
import com.swiftlicious.hellblock.utils.EntityTypeUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.Pos3;

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
		golemTargetTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
		golemTargetTasks.clear();
		golemAuraTasks.values().stream().filter(Objects::nonNull).filter(task -> !task.isCancelled())
				.forEach(SchedulerTask::cancel);
		golemAuraTasks.clear();
	}

	@NotNull
	private List<Block> checkHellGolemBuild(@NotNull Location location) {
		final World world = location.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return List.of();
		}

		final Block base = location.getBlock();
		final List<Block> result = new ArrayList<>();

		final BiConsumer<Block, Block> forceSoulFire = (fireBlock, jackOLantern) -> {
			if (fireBlock.getType() == Material.FIRE) {
				fireBlock.setType(Material.SOUL_FIRE);
			}

			final Location fxLoc = fireBlock.getLocation().clone().add(0.5, 0.5, 0.5);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, fxLoc, 12, 0.35, 0.35, 0.35, 0.01);
			world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, fxLoc, 6, 0.25, 0.4, 0.25, 0.01);

			if (jackOLantern != null && jackOLantern.getType() == Material.JACK_O_LANTERN) {
				final Location jackLoc = jackOLantern.getLocation().clone().add(0.5, 0.5, 0.5);
				world.spawnParticle(Particle.GLOW, jackLoc, 16, 0.4, 0.4, 0.4, 0.05);
				world.spawnParticle(Particle.END_ROD, jackLoc, 8, 0.25, 0.25, 0.25, 0.02);

				final Block jackBlock = jackOLantern.getLocation().getBlock();
				final Material original = jackBlock.getType();

				jackBlock.setBlockData(Bukkit.createBlockData("minecraft:light[level=15]"), false);
				instance.getScheduler().sync().runLater(() -> {
					if (jackBlock.getType() == Material.LIGHT) {
						jackBlock.setBlockData(Bukkit.createBlockData("minecraft:light[level=10]"), false);
					}
				}, 4L, jackLoc);

				instance.getScheduler().sync().runLater(() -> {
					if (jackBlock.getType() == Material.LIGHT) {
						jackBlock.setBlockData(Bukkit.createBlockData("minecraft:light[level=5]"), false);
					}
				}, 8L, jackLoc);

				instance.getScheduler().sync().runLater(() -> {
					if (jackBlock.getType() == Material.LIGHT) {
						jackBlock.setType(original, false); // restore original block
					}
				}, 12L, jackLoc);
			}

			final float pitch = 1.0f
					+ (RandomUtils.generateRandomFloat(0, 1) - RandomUtils.generateRandomFloat(0, 1)) * 0.4f;

			world.getNearbyEntities(fxLoc, 16, 16, 16, e -> e instanceof Player).stream().map(e -> (Player) e)
					.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
							net.kyori.adventure.sound.Sound.sound(
									net.kyori.adventure.key.Key.key("minecraft:entity.generic.extinguish_fire"),
									net.kyori.adventure.sound.Sound.Source.BLOCK, 0.6f, pitch)));
		};

		// Pattern 1:
		// [Fire]
		// [Jack O Lantern]
		// [Soul Soil]
		// [Soul Soil]
		if (matchesPattern1(base)) {
			Block top = base.getRelative(BlockFace.UP);
			forceSoulFire.accept(top, base);
			result.addAll(List.of(base, base.getRelative(BlockFace.DOWN), base.getRelative(BlockFace.DOWN, 2), top));
			return result;
		}

		// Pattern 2:
		// [Soul Fire]
		// [Jack O Lantern]
		// [Soul Soil]
		// [Soul Soil]
		if (matchesPattern2(base)) {
			Block jack = base.getRelative(BlockFace.DOWN);
			forceSoulFire.accept(base, jack);
			result.addAll(
					List.of(base, jack, base.getRelative(BlockFace.DOWN, 2), base.getRelative(BlockFace.DOWN, 3)));
			return result;
		}

		// Pattern 3:
		// [Soul Fire]
		// [Jack O Lantern]
		// [Soul Soil]
		// [Soul Soil]
		if (matchesPattern3(base)) {
			Block jack = base.getRelative(BlockFace.UP);
			Block top = base.getRelative(BlockFace.UP, 2);
			forceSoulFire.accept(top, jack);
			result.addAll(List.of(base, base.getRelative(BlockFace.DOWN), jack, top));
			return result;
		}

		// Pattern 4:
		// [Soul Fire]
		// [Jack O Lantern]
		// [Soul Soil]
		// [Soul Soil]
		if (matchesPattern4(base)) {
			Block jack = base.getRelative(BlockFace.UP, 2);
			Block top = base.getRelative(BlockFace.UP, 3);
			forceSoulFire.accept(top, jack);
			result.addAll(List.of(base, base.getRelative(BlockFace.UP), jack, top));
			return result;
		}

		return result;
	}

	private boolean isFire(@NotNull Material type) {
		return type == Material.FIRE || type == Material.SOUL_FIRE;
	}

	private boolean matchesPattern1(@NotNull Block base) {
		return base.getType() == Material.JACK_O_LANTERN
				&& base.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.DOWN, 2).getType() == Material.SOUL_SOIL
				&& isFire(base.getRelative(BlockFace.UP).getType());
	}

	private boolean matchesPattern2(@NotNull Block base) {
		return isFire(base.getType()) && base.getRelative(BlockFace.DOWN).getType() == Material.JACK_O_LANTERN
				&& base.getRelative(BlockFace.DOWN, 2).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.DOWN, 3).getType() == Material.SOUL_SOIL;
	}

	private boolean matchesPattern3(@NotNull Block base) {
		return base.getType() == Material.SOUL_SOIL && base.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.UP).getType() == Material.JACK_O_LANTERN
				&& isFire(base.getRelative(BlockFace.UP, 2).getType());
	}

	private boolean matchesPattern4(@NotNull Block base) {
		return base.getType() == Material.SOUL_SOIL && base.getRelative(BlockFace.UP).getType() == Material.SOUL_SOIL
				&& base.getRelative(BlockFace.UP, 2).getType() == Material.JACK_O_LANTERN
				&& isFire(base.getRelative(BlockFace.UP, 3).getType());
	}

	@SuppressWarnings("deprecation")
	@NotNull
	private CompletableFuture<Boolean> spawnHellGolem(@NotNull Player player, @NotNull UUID ownerId,
			@NotNull Location location) {
		final World world = location.getWorld();
		if (world == null || !instance.getHellblockHandler().isInCorrectWorld(world)) {
			return CompletableFuture.completedFuture(false);
		}

		final List<Block> structure = checkHellGolemBuild(location);
		if (structure.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}

		return instance.getStorageManager().getCachedUserDataWithFallback(ownerId, false).thenCompose(optData -> {
			if (optData.isEmpty()) {
				return CompletableFuture.completedFuture(false);
			}

			final UserData ownerData = optData.get();
			if (ownerData.getHellblockData()
					.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) != AccessType.ALLOW) {
				return CompletableFuture.completedFuture(false);
			}

			return instance.getScheduler().callSync(() -> {
				try {
					// clear structure
					structure.forEach(block -> block.setType(Material.AIR));

					// --- Summoning burst FX ---
					final Location fxLoc = location.clone().add(0.5, 0, 0.5);

					// Big soul flames + smoke
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, fxLoc, 40, 1.5, 1.5, 1.5, 0.05);
					world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, fxLoc, 25, 1.2, 1.5, 1.2, 0.05);

					// Sound burst (deep + fiery)
					world.getNearbyEntities(fxLoc, 32, 32, 32, e -> e instanceof Player).stream().map(e -> (Player) e)
							.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
									net.kyori.adventure.sound.Sound.sound(
											net.kyori.adventure.key.Key.key("minecraft:entity.blaze.shoot"),
											net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.5f, 0.6f)));

					world.getNearbyEntities(fxLoc, 32, 32, 32, e -> e instanceof Player).stream().map(e -> (Player) e)
							.forEach(p -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(p),
									net.kyori.adventure.sound.Sound.sound(
											net.kyori.adventure.key.Key.key("minecraft:block.fire.extinguish"),
											net.kyori.adventure.sound.Sound.Source.BLOCK, 0.8f, 0.9f)));

					// spawn hell golem
					final Snowman hellGolem = (Snowman) world.spawnEntity(location,
							EntityTypeUtils.getCompatibleEntityType("SNOW_GOLEM", "SNOWMAN"));
					hellGolem.setAware(true);
					hellGolem.setDerp(false);
					hellGolem.setVisualFire(true);
					hellGolem.getPersistentDataContainer().set(hellGolemKey, PersistentDataType.STRING, HELLGOLEM_KEY);

					// --- Permanent aura FX ---
					final SchedulerTask auraTask = instance.getScheduler().sync().runRepeating(() -> {
						if (hellGolem.isDead() || !hellGolem.isValid()) {
							stopHellGolemAura(hellGolem.getUniqueId());
							return;
						}
						final Location auraLoc = hellGolem.getLocation().clone().add(0, 1, 0);
						world.spawnParticle(Particle.SOUL_FIRE_FLAME, auraLoc, 2, 0.2, 0.3, 0.2, 0.01);
						world.spawnParticle(ParticleUtils.getParticle("SCULK_SOUL"), auraLoc, 1, 0.2, 0.3, 0.2, 0.01);
					}, 0L, 10L, hellGolem.getLocation()); // every 0.5s

					golemAuraTasks.put(hellGolem.getUniqueId(), auraTask);

					// --- Aura FX on spawn ---
					final Location golemLoc = hellGolem.getLocation().clone().add(0, 1, 0);

					// Initial flash
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, golemLoc, 30, 0.6, 1.0, 0.6, 0.05);
					world.spawnParticle(ParticleUtils.getParticle("SCULK_SOUL"), golemLoc, 12, 0.4, 0.8, 0.4, 0.05);
					world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), golemLoc, 15, 0.6, 0.8, 0.6, 0.02);

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
							final Location auraLoc = hellGolem.getLocation().clone().add(0, 1, 0);
							world.spawnParticle(Particle.SOUL_FIRE_FLAME, auraLoc, 6, 0.3, 0.5, 0.3, 0.01);
							ticks += 5;
						}
					}, 0L, 5L, hellGolem.getLocation());

					// start AI targeting loop and track the task for cancellation
					return CompletableFuture.completedFuture(startHellGolemTargeting(hellGolem) != null);
				} catch (Exception ex) {
					instance.getPluginLogger().warn("Failed to spawn Hell Golem at location: " + Pos3.from(location),
							ex);
					return CompletableFuture.completedFuture(false);
				}
			});
		}).exceptionally(ex -> {
			instance.getPluginLogger().warn("spawnHellGolem: Unexpected failure at location " + Pos3.from(location),
					ex);
			return false;
		});
	}

	@Nullable
	private SchedulerTask startHellGolemTargeting(@NotNull Snowman golem) {
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

					if (!newTarget.equals(currentTarget)) {
						golem.setTarget(newTarget);
					}
				} else {
					golem.setTarget(null);
				}
			}
		}, initialDelay, 20L, golem.getLocation()); // runs every second

		return golemTargetTasks.put(golem.getUniqueId(), task);
	}

	private void stopHellGolemTargeting(@NotNull UUID golemId) {
		final SchedulerTask task = golemTargetTasks.remove(golemId);
		if (task != null && !task.isCancelled()) {
			task.cancel();
		}
	}

	private void stopHellGolemAura(@NotNull UUID golemId) {
		final SchedulerTask task = golemAuraTasks.remove(golemId);
		if (task != null && !task.isCancelled()) {
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
				&& instance.getMinionHandler().isMinion(ws)) {
			return 1;
		}
		if (entity instanceof Mob m && instance.getMinionHandler().isMinion(m)) {
			return 2;
		}
		if (entity instanceof Mob m && instance.getInvasionHandler().isInvasionMob(m)) {
			return 3;
		}
		if (entity instanceof Wither w && instance.getWitherHandler().isEnhancedWither(w)) {
			return 4;
		}
		if (entity instanceof PiglinBrute pg && instance.getInvasionHandler().isBossMob(pg)) {
			return 5;
		}
		return entity instanceof Monster ? 6 : Integer.MAX_VALUE;
	}

	@Nullable
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

	public boolean isHellGolem(@NotNull Snowman golem) {
		return golem.getPersistentDataContainer().has(hellGolemKey, PersistentDataType.STRING) && HELLGOLEM_KEY
				.equals(golem.getPersistentDataContainer().get(hellGolemKey, PersistentDataType.STRING));
	}

	@EventHandler
	public void onCreationOfHellGolem(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		final Block block = event.getBlockPlaced();

		if (!checkHellGolemBuild(block.getLocation()).isEmpty()) {
			instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenApply(ownerUUID -> {
				if (ownerUUID == null) {
					return null;
				}

				return spawnHellGolem(player, ownerUUID, block.getLocation());
			});
		}
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

			if (!checkHellGolemBuild(block.getLocation()).isEmpty()) {
				final Collection<Entity> playersNearby = block.getWorld().getNearbyEntities(block.getLocation(), 25, 25,
						25, e -> e.getType() == EntityType.PLAYER);

				final Player closest = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
						playersNearby);
				if (closest != null) {
					instance.getCoopManager().getHellblockOwnerOfVisitingIsland(closest).thenApply(ownerUUID -> {
						if (ownerUUID == null) {
							return null;
						}

						return spawnHellGolem(closest, ownerUUID, block.getLocation());
					});
				}
			}
		}
	}

	@EventHandler
	public void onDispenserUse(BlockDispenseEvent event) {
		if (event.getItem().getType() != Material.FLINT_AND_STEEL)
			return;

		Block dispenser = event.getBlock();
		if (!instance.getHellblockHandler().isInCorrectWorld(dispenser.getWorld()))
			return;

		BlockFace facing = ((Dispenser) dispenser.getState().getBlockData()).getFacing();
		Block targetBlock = dispenser.getRelative(facing);

		if (!checkHellGolemBuild(targetBlock.getLocation()).isEmpty()) {
			// Find the nearest player to attribute the spawn
			Collection<Entity> nearby = targetBlock.getWorld().getNearbyEntities(targetBlock.getLocation(), 25, 25, 25,
					e -> e instanceof Player);
			Player closest = instance.getNetherrackGeneratorHandler().getClosestPlayer(targetBlock.getLocation(),
					nearby);
			if (closest != null) {
				instance.getCoopManager().getHellblockOwnerOfVisitingIsland(closest).thenApply(ownerUUID -> {
					if (ownerUUID == null) {
						return null;
					}

					return spawnHellGolem(closest, ownerUUID, targetBlock.getLocation());
				});
			}
		}
	}

	@EventHandler
	public void onPlayerIgnite(PlayerInteractEvent event) {
		if (event.getItem() == null || event.getItem().getType() != Material.FLINT_AND_STEEL)
			return;
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if (event.getClickedBlock() == null)
			return;

		Block clicked = event.getClickedBlock();
		Block above = clicked.getRelative(BlockFace.UP);

		if (!instance.getHellblockHandler().isInCorrectWorld(clicked.getWorld()))
			return;

		if (above.getType() == Material.FIRE) {
			if (!checkHellGolemBuild(clicked.getLocation()).isEmpty()) {
				Player player = event.getPlayer();
				instance.getCoopManager().getHellblockOwnerOfVisitingIsland(player).thenApply(ownerUUID -> {
					if (ownerUUID == null) {
						return null;
					}

					return spawnHellGolem(player, ownerUUID, clicked.getLocation());
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

		if (entity instanceof Snowman snowman && isHellGolem(snowman)) {
			if (DamageUtil.isFireDamage(event)) {
				event.setCancelled(true);
			}
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
		if (living instanceof Mob mob && instance.getMinionHandler().isMinion(mob)) {
			final double newDamage = baseDamage * 1.5;
			event.setDamage(newDamage);

			// FX
			living.getWorld().spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"),
					living.getLocation().clone().add(0, 0.5, 0), 8, 0.25, 0.25, 0.25, 0.02);
			living.getWorld().getNearbyEntities(living.getLocation(), 16, 16, 16, e -> e instanceof Player).stream()
					.map(e -> (Player) e)
					.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
							net.kyori.adventure.sound.Sound.sound(
									net.kyori.adventure.key.Key.key("minecraft:entity.blaze.hurt"),
									net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.8f, 1.1f)));

		} else if (living instanceof Mob mob && instance.getInvasionHandler().isInvasionMob(mob)) {
			final double newDamage = baseDamage * 1.15;
			event.setDamage(newDamage);

			World world = living.getWorld();
			Location center = living.getLocation().clone().add(0, 1, 0);

			// Particles: ash + angry villager + portal shimmer
			world.spawnParticle(Particle.ASH, center, 10, 0.3, 0.4, 0.3, 0.02);
			world.spawnParticle(ParticleUtils.getParticle("VILLAGER_ANGRY"), center, 2, 0.1, 0.2, 0.1, 0.0);
			world.spawnParticle(Particle.PORTAL, center, 6, 0.25, 0.25, 0.25, 0.05);

			// Sounds: piglin ambient + enderman teleport (chaotic vibe) + ghast moan
			// (quiet)
			world.getNearbyEntities(center, 24, 24, 24, e -> e instanceof Player).stream().map(e -> (Player) e)
					.map(player -> instance.getSenderFactory().getAudience(player)).forEach(audience -> {
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.piglin.ambient"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.0f, 1.2f));
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.enderman.teleport"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.5f, 1.4f));
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.ghast.moan"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.3f, 0.6f));
					});

		} else if (living instanceof PiglinBrute brute && instance.getInvasionHandler().isBossMob(brute)) {
			final double newDamage = baseDamage * 1.05;
			event.setDamage(newDamage);

			// Particles: lava + redstone + block crack for brute brutality
			World world = living.getWorld();
			Location center = living.getLocation().clone().add(0, 1, 0);

			world.spawnParticle(Particle.LAVA, center, 6, 0.25, 0.4, 0.25, 0.01);
			world.spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"), center, 10, 0.3, 0.5, 0.3, 0.02,
					Bukkit.createBlockData(Material.NETHERRACK));
			world.spawnParticle(ParticleUtils.getParticle("REDSTONE"), center, 12, 0.3, 0.3, 0.3, 1.0,
					new Particle.DustOptions(Color.fromRGB(255, 85, 0), 1.2f)); // fiery red-orange

			// Sound: piglin brute step + anvil land for weight + blaze shoot (layered)
			world.getNearbyEntities(center, 32, 32, 32, e -> e instanceof Player).stream().map(e -> (Player) e)
					.map(player -> instance.getSenderFactory().getAudience(player)).forEach(audience -> {
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.piglin_brute.angry"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 1.1f, 0.85f));
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:block.anvil.land"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.6f, 0.6f));
						AdventureHelper.playSound(audience,
								net.kyori.adventure.sound.Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.blaze.shoot"),
										net.kyori.adventure.sound.Sound.Source.HOSTILE, 0.8f, 1.6f));
					});

		} else if (living instanceof Wither wither && instance.getWitherHandler().isEnhancedWither(wither)) {
			final double newDamage = baseDamage * 1.25;
			event.setDamage(newDamage);

			// FX
			living.getWorld().spawnParticle(Particle.FLAME, living.getLocation().clone().add(0, 1, 0), 8, 0.3, 0.3, 0.3,
					0.05);

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
		if (event.getEntityType() != EntityTypeUtils.getCompatibleEntityType("SNOW_GOLEM", "SNOWMAN")) {
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

		instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(userData -> {
			if (instance.getCooldownManager().shouldUpdateActivity(killer.getUniqueId(), 5000)) {
				userData.getHellblockData().updateLastIslandActivity();
			}

			instance.getChallengeManager().handleChallengeProgression(userData, ActionType.SLAY, snowman);
		});
	}
}