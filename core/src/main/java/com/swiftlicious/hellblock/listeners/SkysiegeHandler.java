package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.events.skysiege.SkysiegeEndEvent;
import com.swiftlicious.hellblock.events.skysiege.SkysiegeQueenSpawnEvent;
import com.swiftlicious.hellblock.events.skysiege.SkysiegeStartEvent;
import com.swiftlicious.hellblock.events.skysiege.SkysiegeWaveEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.bossbar.BossBarColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarOverlay;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.SkysiegeData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.AdventureMetadata;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

public class SkysiegeHandler implements Listener, Reloadable {

	private final HellblockPlugin instance;
	private final Map<Integer, CustomSkysiege> activeSieges = new ConcurrentHashMap<>();

	private SchedulerTask siegeTask;

	private static final String SKYSIEGE_QUEEN_KEY = "skysiege_queen";
	private static final String SKYSIEGE_GHAST_KEY = "skysiege_ghast";

	private final NamespacedKey skysiegeQueenKey;
	private final NamespacedKey skysiegeGhastKey;

	public SkysiegeHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.skysiegeQueenKey = new NamespacedKey(plugin, SKYSIEGE_QUEEN_KEY);
		this.skysiegeGhastKey = new NamespacedKey(plugin, SKYSIEGE_GHAST_KEY);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		startSkysiegeTask();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		activeSieges.values().stream().filter(Objects::nonNull).forEach(siege -> siege.end(false));
		activeSieges.clear();
		if (siegeTask != null && !siegeTask.isCancelled()) {
			siegeTask.cancel();
			siegeTask = null;
		}
	}

	public int getIslandIdByLocation(@NotNull Location loc) {
		for (CustomSkysiege siege : activeSieges.values()) {
			Optional<UserData> dataOpt = instance.getStorageManager().getCachedUserData(siege.ownerUUID);
			if (dataOpt.isEmpty())
				continue;
			HellblockData data = dataOpt.get().getHellblockData();

			if (data.getBoundingBox().contains(loc.toVector()))
				return data.getIslandId();
		}
		return -1;
	}

	public void startSkysiegeTask() {
		if (!instance.getConfigManager().skysiegeEventSettings().enabled())
			return;

		siegeTask = instance.getScheduler()
				.asyncRepeating(() -> instance.getCoopManager().getCachedIslandOwnerData().thenAccept(allOwners -> {
					for (UserData ownerData : allOwners) {
						HellblockData hellblockData = ownerData.getHellblockData();
						int islandId = hellblockData.getIslandId();
						Optional<HellblockWorld<?>> hbWorld = instance.getWorldManager()
								.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
						if (hbWorld.isEmpty() || hbWorld.get().bukkitWorld() == null)
							continue;
						World world = hbWorld.get().bukkitWorld();
						BoundingBox bounds = hellblockData.getBoundingBox();
						if (bounds == null)
							continue;
						tryStartSkysiege(islandId, ownerData.getUUID(), world, bounds);
					}
				}), 3, 3, TimeUnit.MINUTES);
	}

	public boolean isSkysiegeRunning(int islandId) {
		return activeSieges.containsKey(islandId);
	}

	public CustomSkysiege getSkysiege(int islandId) {
		return activeSieges.get(islandId);
	}

	public boolean tryStartSkysiege(int islandId, UUID ownerUUID, World world, BoundingBox bounds) {
		if (isSkysiegeRunning(islandId))
			return false;

		Optional<UserData> dataOpt = instance.getStorageManager().getCachedUserData(ownerUUID);
		if (dataOpt.isEmpty())
			return false;

		if (instance.getInvasionHandler().isInvasionRunning(islandId))
			return false;

		HellblockData hellblockData = dataOpt.get().getHellblockData();
		SkysiegeData data = hellblockData.getSkysiegeData();

		if (!hellblockData.hasHellblock()) {
			return false;
		}

		if (hellblockData.isAbandoned()) {
			return false;
		}

		boolean hasWeatherEvent = instance.getNetherWeatherManager().isWeatherActive(islandId);

		if (hasWeatherEvent || instance.getWitherHandler().getCustomWither().hasActiveWither(islandId))
			return false;

		long lastTime = data.getLastSkysiegeTime();
		long cooldown = instance.getConfigManager().skysiegeEventSettings().cooldown();
		if (System.currentTimeMillis() - lastTime < cooldown)
			return false;

		float requiredLevel = instance.getConfigManager().skysiegeEventSettings().levelRequired();
		if (hellblockData.getIslandLevel() < requiredLevel)
			return false;

		double chance = 0.15; // 15% chance
		if (Math.random() >= chance)
			return false;

		Location center = bounds.getCenter().toLocation(world).clone().add(0, 15, 0);
		world.getPlayers().stream().filter(player -> bounds.contains(player.getLocation().toVector()))
				.forEach(player -> {
					instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_STARTED.build()));

					// Nether-themed intro animation
					world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), player.getLocation(), 20, 0.8, 0.6,
							0.8, 0.02);
					world.spawnParticle(Particle.FLAME, player.getLocation(), 10, 0.4, 0.3, 0.4, 0.01);
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 8, 0.3, 0.2, 0.3, 0.01);
					AdventureHelper.playPositionalSound(world, player.getLocation(),
							Sound.sound(Key.key("minecraft:entity.ghast.scream"), Source.BLOCK, 2.5f, 0.7f));
					AdventureHelper.playPositionalSound(world, player.getLocation(),
							Sound.sound(Key.key("minecraft:ambient.nether_wastes.loop"), Source.BLOCK, 1.0f, 0.8f));
				});

		CustomSkysiege siege = new CustomSkysiege(islandId, ownerUUID, world, bounds, center);
		activeSieges.put(islandId, siege);
		siege.start();
		data.recordStart();
		EventUtils.fireAndForget(new SkysiegeStartEvent(islandId, ownerUUID, world));

		return true;
	}

	public void tagAsSkysiegeGhast(@NotNull Ghast ghast) {
		ghast.getPersistentDataContainer().set(skysiegeGhastKey, PersistentDataType.STRING, SKYSIEGE_GHAST_KEY);
	}

	public boolean isSkysiegeGhast(@NotNull Ghast ghast) {
		return ghast.getPersistentDataContainer().has(skysiegeGhastKey, PersistentDataType.STRING) && SKYSIEGE_GHAST_KEY
				.equals(ghast.getPersistentDataContainer().get(skysiegeGhastKey, PersistentDataType.STRING));
	}

	public void tagAsQueenGhast(@NotNull Ghast ghast) {
		ghast.getPersistentDataContainer().set(skysiegeQueenKey, PersistentDataType.STRING, SKYSIEGE_QUEEN_KEY);
	}

	public boolean isQueenGhast(@NotNull Ghast ghast) {
		return ghast.getPersistentDataContainer().has(skysiegeQueenKey, PersistentDataType.STRING) && SKYSIEGE_QUEEN_KEY
				.equals(ghast.getPersistentDataContainer().get(skysiegeQueenKey, PersistentDataType.STRING));
	}

	@EventHandler
	public void onQueenDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Ghast ghast && isQueenGhast(ghast)) {
			CustomSkysiege siege = getSkysiege(getIslandIdByLocation(ghast.getLocation()));
			if (siege != null && siege.shieldedQueenVortexCount.get() > 0) {
				event.setCancelled(true); // Queen is shielded during vortex
				ghast.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_WITCH"), ghast.getLocation(), 10, 0.5,
						0.5, 0.5, 0.01);
			}
		}
	}

	public class CustomSkysiege {

		private final int islandId;
		private final UUID ownerUUID;
		private final World world;
		private final BoundingBox bounds;
		private final Location center;

		private final Set<UUID> ghastIds = ConcurrentHashMap.newKeySet();
		private final AtomicInteger currentWave = new AtomicInteger(0);
		private SchedulerTask mainTask;

		private UUID queenId;
		private UUID bossBarId;
		private UUID bossHealthBarId;

		private final Set<UUID> bossBarViewers = ConcurrentHashMap.newKeySet();
		private final Set<UUID> bossHealthViewers = ConcurrentHashMap.newKeySet();

		private final AtomicInteger shieldedQueenVortexCount = new AtomicInteger(0);
		private final Set<Location> activeVortexes = ConcurrentHashMap.newKeySet();

		private boolean queenSpawned = false;
		private long startTime;

		public CustomSkysiege(int islandId, UUID ownerUUID, World world, BoundingBox bounds, Location center) {
			this.islandId = islandId;
			this.ownerUUID = ownerUUID;
			this.world = world;
			this.bounds = bounds;
			this.center = center;
			this.startTime = System.currentTimeMillis();
		}

		public void start() {
			// Add initial atmosphere burst for immersion
			world.spawnParticle(Particle.ASH, center, 100, 8, 6, 8, 0.05);
			world.spawnParticle(Particle.SOUL, center, 40, 6, 3, 6, 0.02);
			AdventureHelper.playPositionalSound(world, center,
					Sound.sound(Key.key("minecraft:ambient.basalt_deltas.additions"), Source.BLOCK, 1.6f, 0.8f));

			mainTask = instance.getScheduler().asyncRepeating(() -> {
				long elapsed = System.currentTimeMillis() - startTime;
				if (elapsed > instance.getConfigManager().skysiegeEventSettings().maxDuration()) {
					end(false);
					return;
				}

				if (!anyPlayersInBounds()) {
					end(false);
					return;
				}

				ghastIds.forEach(id -> {
					Entity entity = Bukkit.getEntity(id);
					if (entity instanceof Ghast ghast && ghast.isValid() && !ghast.isDead()) {
						Location loc = ghast.getLocation();
						if (!bounds.contains(loc.toVector())) {
							Vector centerDir = bounds.getCenter().toBlockVector().subtract(loc.toVector()).normalize();
							ghast.setVelocity(centerDir.multiply(0.5));
						}
					}
				});

				cleanupDeadGhasts();

				if (ghastIds.isEmpty() && !queenSpawned) {
					int nextWave = currentWave.incrementAndGet();
					if (nextWave <= 5) {
						spawnWave(nextWave);
					} else {
						spawnQueen();
					}
				}

				updateBossHealthBar();

				// Subtle ambient activity
				if (Math.random() < 0.05) {
					world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), center, 8, 6, 3, 6, 0.02);
					AdventureHelper.playPositionalSound(world, center,
							Sound.sound(Key.key("minecraft:block.fire.ambient"), Source.BLOCK, 0.7f, 0.9f));
				}

				if (Math.random() < 0.03) {
					world.spawnParticle(Particle.REVERSE_PORTAL, center, 3, 6, 3, 6, 0.01);
				}
			}, 2, 2, TimeUnit.SECONDS);

			// Schedule Queen ability if spawned
			instance.getScheduler().sync().runRepeating(() -> {
				if (queenSpawned && queenId != null) {
					Entity entity = Bukkit.getEntity(queenId);
					if (entity instanceof Ghast queen && queen.isValid() && !queen.isDead()) {
						triggerQueenSoulBombardment(queen);
					}
				}
			}, 100, 200, bounds.getCenter().toLocation(world)); // every ~10 seconds

			// Schedule soul vortex attack
			instance.getScheduler().sync().runRepeating(() -> {
				if (queenSpawned && queenId != null) {
					Entity entity = Bukkit.getEntity(queenId);
					if (entity instanceof Ghast queen && queen.isValid() && !queen.isDead()) {
						triggerSoulVortexes(queen);
					}
				}
			}, 140, 260, bounds.getCenter().toLocation(world)); // Every ~13s
		}

		private void spawnWave(int waveNumber) {
			int ghastCount = 3 + waveNumber;

			// Portal cinematic pre-warning
			world.spawnParticle(Particle.REVERSE_PORTAL, center, 80, 8, 6, 8, 0.02);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 50, 6, 5, 6, 0.01);
			world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), center, 30, 5, 4, 5, 0.02);

			AdventureHelper.playPositionalSound(world, center,
					Sound.sound(Key.key("minecraft:block.beacon.activate"), Source.BLOCK, 1.5f, 0.7f));

			// Delayed portal spawning
			instance.getScheduler().sync().runLater(() -> {
				for (int i = 0; i < ghastCount; i++) {
					Location spawnLoc = randomSkyLocation(center, 15, 25);
					portalSummonGhast(spawnLoc, waveNumber);
				}
			}, 40L, center); // 2-second delay before spawn

			EventUtils.fireAndForget(new SkysiegeWaveEvent(islandId, ownerUUID, world, waveNumber));

			// Notify players
			world.getPlayers().stream().filter(p -> bounds.contains(p.getLocation().toVector())).forEach(player -> {
				instance.getSenderFactory().wrap(player)
						.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_WAVE
										.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(waveNumber)))
										.build()));

				world.spawnParticle(Particle.ASH, player.getLocation(), 25, 1.5, 1.2, 1.5, 0.02);
				AdventureHelper.playPositionalSound(world, player.getLocation(),
						Sound.sound(Key.key("minecraft:block.fire.extinguish"), Source.BLOCK, 0.7f, 1.1f));
			});
		}

		private void portalSummonGhast(Location spawnLoc, int waveNumber) {
			World w = spawnLoc.getWorld();
			if (w == null)
				return;

			// Portal buildup animation
			for (int t = 0; t <= 20; t += 2) {
				int delay = t;
				instance.getScheduler().sync().runLater(() -> {
					double size = 1 + (delay / 5.0);
					w.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 20, size, size / 2, size, 0.02);
					w.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), spawnLoc, 10, 0.6, 0.6, 0.6, 0.01);
					if (delay % 10 == 0) {
						AdventureHelper.playPositionalSound(w, spawnLoc,
								Sound.sound(Key.key("minecraft:block.portal.trigger"), Source.BLOCK, 0.9f, 1.2f));
					}
				}, delay, spawnLoc);
			}

			// Final burst and spawn
			instance.getScheduler().sync().runLater(() -> {
				w.spawnParticle(Particle.FLASH, spawnLoc, 1);
				w.spawnParticle(ParticleUtils.getParticle("EXPLOSION_HUGE"), spawnLoc, 1);
				AdventureHelper.playPositionalSound(w, spawnLoc,
						Sound.sound(Key.key("minecraft:entity.ghast.scream"), Source.BLOCK, 1.8f, 0.8f));

				Ghast ghast = w.spawn(spawnLoc, Ghast.class, g -> {
					g.setSilent(false);
					g.setGlowing(true);
					g.setRemoveWhenFarAway(false);
					tagAsSkysiegeGhast(g);
				});

				ghastIds.add(ghast.getUniqueId());
			}, 40L, spawnLoc);
		}

		private void spawnQueen() {
			Location spawnLoc = center.clone().add(0, 12, 0);
			World w = spawnLoc.getWorld();

			// Portal charge-up
			for (int i = 0; i < 60; i += 5) {
				int delay = i;
				instance.getScheduler().sync().runLater(() -> {
					double radius = 2 + (delay / 10.0);
					w.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 40, radius, radius, radius, 0.02);
					w.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 25, radius * 0.8, 1.0, radius * 0.8, 0.01);
					w.spawnParticle(Particle.ASH, spawnLoc, 15, radius, 1.0, radius, 0.01);
					if (delay % 10 == 0) {
						AdventureHelper.playPositionalSound(w, spawnLoc, Sound
								.sound(Key.key("minecraft:block.respawn_anchor.charge"), Source.BLOCK, 1.5f, 0.6f));
					}
				}, delay, spawnLoc);
			}

			instance.getScheduler().sync().runLater(() -> {
				w.strikeLightningEffect(spawnLoc);
				AdventureHelper.playPositionalSound(w, spawnLoc,
						Sound.sound(Key.key("minecraft:entity.ghast.shoot"), Source.BLOCK, 2.0f, 0.5f));
				w.spawnParticle(ParticleUtils.getParticle("EXPLOSION_HUGE"), spawnLoc, 1);

				Ghast queen = w.spawn(spawnLoc, Ghast.class, g -> {
					AdventureMetadata.setEntityCustomName(g, instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_BOSS_NAME.build()));
					g.setCustomNameVisible(true);
					g.setSilent(false);
					g.setGlowing(true);
					g.setRemoveWhenFarAway(false);
					tagAsQueenGhast(g);

					RtagEntity tagged = (RtagEntity) g;
					tagged.setAttributeBase("generic.max_health", 300D);
					g = (Ghast) tagged.load();
					g.setHealth(300D);
				});

				queenId = queen.getUniqueId();
				queenSpawned = true;

				EventUtils.fireAndForget(new SkysiegeQueenSpawnEvent(islandId, ownerUUID, w));

				showBossBars(queen);
			}, 80L, spawnLoc);
		}

		private void triggerQueenSoulBombardment(Ghast queen) {
			Location loc = queen.getLocation();
			World w = queen.getWorld();

			// Charge up effect
			w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 40, 1.5, 1.0, 1.5, 0.02);
			w.spawnParticle(ParticleUtils.getParticle("SCULK_SOUL"), loc, 30, 1.5, 1.0, 1.5, 0.01);
			AdventureHelper.playPositionalSound(w, loc,
					Sound.sound(Key.key("minecraft:block.enchantment_table.use"), Source.BLOCK, 1.5f, 0.5f));

			// Delay actual launch
			instance.getScheduler().sync().runLater(() -> {
				List<Player> targets = w.getPlayers().stream().filter(p -> bounds.contains(p.getLocation().toVector()))
						.toList();

				targets.forEach(player -> {
					Location from = queen.getLocation().clone().add(0, 1.2, 0);
					Location to = player.getLocation().clone().add(0, 1.2, 0);
					Vector direction = to.toVector().subtract(from.toVector()).normalize();

					// Fireball launch
					w.spawn(from, Fireball.class, fb -> {
						fb.setDirection(direction);
						fb.setYield(1.2f);
						fb.setIsIncendiary(false);
						fb.setShooter(queen);
					});

					// Visual trail
					w.spawnParticle(Particle.SOUL, from, 15, 0.3, 0.3, 0.3, 0.01);
					AdventureHelper.playPositionalSound(w, from,
							Sound.sound(Key.key("minecraft:entity.ghast.shoot"), Source.BLOCK, 1.4f, 0.8f));
				});

			}, 40L, loc); // 2 seconds charge-up
		}

		private void triggerSoulVortexes(Ghast queen) {
			final World w = queen.getWorld();

			int vortexCount = RandomUtils.generateRandomInt(1, 3);
			List<Location> vortexSpots = new ArrayList<>();

			for (int i = 0; i < vortexCount; i++) {
				Location origin = queen.getLocation().clone().add(RandomUtils.generateRandomInt(-8, 8), -1,
						RandomUtils.generateRandomInt(-8, 8));

				// Snap Y to ground
				Block blockBelow = w.getBlockAt(origin);
				while (blockBelow.getType().isAir() && blockBelow.getY() > w.getMinHeight()) {
					blockBelow = blockBelow.getRelative(BlockFace.DOWN);
				}
				Location finalLoc = blockBelow.getLocation().clone().add(0.5, 1, 0.5);
				vortexSpots.add(finalLoc);

				// Start vortex animation
				playSoulVortexEffect(finalLoc);
			}
		}

		private void playSoulVortexEffect(Location center) {
			final World w = center.getWorld();
			final int durationTicks = 8 * 20;
			final int radius = 3;

			// Add vortex location to tracking list
			activeVortexes.add(center.clone());

			// Enable Queen shield during this vortex's life
			shieldedQueenVortexCount.incrementAndGet();

			for (int t = 0; t < durationTicks; t += 5) {
				int delay = t;

				instance.getScheduler().sync().runLater(() -> {
					// Swirling particles
					w.spawnParticle(Particle.SOUL, center, 10, 0.8, 0.5, 0.8, 0.01);
					w.spawnParticle(ParticleUtils.getParticle("SCULK_SOUL"), center, 6, 0.6, 0.4, 0.6, 0.01);
					w.spawnParticle(Particle.ASH, center, 4, 0.5, 0.3, 0.5, 0.01);

					if (Math.random() < 0.1) {
						AdventureHelper.playPositionalSound(w, center, Sound
								.sound(Key.key("minecraft:block.respawn_anchor.ambient"), Source.BLOCK, 0.8f, 0.5f));
					}

					w.getNearbyEntities(center, 4, 4, 4, e -> e instanceof Ghast).forEach(e -> {
						if (e instanceof Ghast ghast && isSkysiegeGhast(ghast) && ghast.isValid() && !ghast.isDead()) {
							ghast.remove(); // sacrifice the ghast
							w.spawnParticle(Particle.SOUL, center, 30, 0.5, 0.5, 0.5, 0.02);
							AdventureHelper.playPositionalSound(w, center, Sound
									.sound(Key.key("minecraft:entity.evoker.cast_spell"), Source.BLOCK, 1.3f, 0.5f));

							// Heal Queen if valid
							if (queenId != null) {
								Entity q = Bukkit.getEntity(queenId);
								if (q instanceof Ghast queen && queen.isValid() && !queen.isDead()) {
									double newHealth = Math.min(queen.getHealth() + 10.0,
											new RtagEntity(queen).getAttributeBase("generic.max_health"));
									queen.setHealth(newHealth);

									// Heal visual
									w.spawnParticle(Particle.HEART, queen.getLocation().clone().add(0, 1, 0), 5, 0.3,
											0.5, 0.3, 0.01);
								}
							}
						}
					});

					// Apply weakness based on overlap
					for (Player player : w.getPlayers()) {
						if (!bounds.contains(player.getLocation().toVector()))
							continue;

						long overlap = activeVortexes.stream()
								.filter(loc -> loc.getWorld().getUID().equals(player.getWorld().getUID()))
								.filter(loc -> loc.distanceSquared(player.getLocation()) <= radius * radius).count();

						if (overlap >= 2) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, true, false, false)); // Weakness
																												// II
						} else if (overlap == 1) {
							player.addPotionEffect(
									new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false, false)); // Weakness
																												// I
						}
					}

					// Pull players toward vortex center
					for (Player player : w.getPlayers()) {
						if (!bounds.contains(player.getLocation().toVector()))
							continue;

						double distance = player.getLocation().distance(center);
						if (distance > radius)
							continue;

						Vector direction = center.toVector().clone().subtract(player.getLocation().toVector())
								.normalize();
						player.setVelocity(player.getVelocity().clone().add(direction.multiply(0.08))); // Light pull
					}

				}, delay, center);
			}

			// On vortex end: check for explosion
			instance.getScheduler().sync().runLater(() -> {
				// Remove from tracking
				activeVortexes.removeIf(loc -> loc.distanceSquared(center) < 1);

				// Final burst
				w.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), center, 30, 1, 1, 1, 0.02);
				w.spawnParticle(Particle.SOUL, center, 20, 1, 1, 1, 0.02);
				AdventureHelper.playPositionalSound(w, center,
						Sound.sound(Key.key("minecraft:block.candle.extinguish"), Source.BLOCK, 1.0f, 0.6f));

				// Explosion effect (no block damage)
				for (Player player : w.getPlayers()) {
					if (!bounds.contains(player.getLocation().toVector()))
						continue;

					if (player.getLocation().distanceSquared(center) <= (radius * radius)) {
						w.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), player.getLocation(), 1);
						AdventureHelper.playPositionalSound(w, player.getLocation(),
								Sound.sound(Key.key("minecraft:entity.generic.explode"), Source.BLOCK, 1.4f, 0.6f));
						player.setVelocity(
								player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.6));
						player.damage(2.0); // Optional: light damage
					}
				}
				// Disable shield if last vortex ends
				if (shieldedQueenVortexCount.decrementAndGet() <= 0) {
					shieldedQueenVortexCount.set(0);
				}
			}, durationTicks, center);
		}

		private void showBossBars(Ghast queen) {
			bossBarId = UUID.randomUUID();
			bossHealthBarId = UUID.randomUUID();

			Component title = instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_BOSS_BAR
					.arguments(AdventureHelper.miniMessageToComponent("1")).build());

			List<Player> players = world.getPlayers().stream().filter(p -> bounds.contains(p.getLocation().toVector()))
					.toList();

			players.forEach(player -> {
				UUID uuid = player.getUniqueId();

				bossBarViewers.add(uuid);
				bossHealthViewers.add(uuid);

				VersionHelper.getNMSManager().createBossBar(player, bossBarId, title, BossBarColor.PURPLE,
						BossBarOverlay.NOTCHED_10, 1.0f, false, false, false);

				VersionHelper.getNMSManager().createBossBar(player, bossHealthBarId, title, BossBarColor.PURPLE,
						BossBarOverlay.PROGRESS, 1.0f, true, true, true);

				instance.getSenderFactory().wrap(player).sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_BOSS_SPAWNED.build()));

				world.spawnParticle(Particle.END_ROD, player.getLocation(), 40, 1.5, 1.0, 1.5, 0.02);
				AdventureHelper.playPositionalSound(world, player.getLocation(),
						Sound.sound(Key.key("minecraft:entity.wither.spawn"), Source.BLOCK, 2.2f, 0.6f));
			});
		}

		private void updateBossHealthBar() {
			if (queenId == null || bossHealthBarId == null)
				return;

			Entity entity = Bukkit.getEntity(queenId);
			if (!(entity instanceof Ghast queen) || !queen.isValid() || queen.isDead()) {
				removeBossHealthBar();
				end(false);
				return;
			}

			// Prevent damage if shielded
			if (shieldedQueenVortexCount.get() > 0) {
				// Optional: show boss bar effect or send particles
				return; // Skip update if immune (custom damage handler will respect this)
			}

			RtagEntity tagged = new RtagEntity(queen);
			double max = tagged.getAttributeBase("generic.max_health");
			float progress = (float) (queen.getHealth() / max);

			Component title = instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_BOSS_HEALTH_BAR
							.arguments(AdventureHelper.miniMessageToComponent(String.valueOf((int) queen.getHealth())))
							.build());

			List<Player> nearby = world.getNearbyEntities(queen.getLocation(), 30, 10, 30).stream()
					.filter(e -> e instanceof Player p && p.isOnline()).map(e -> (Player) e).toList();

			Set<UUID> updated = new HashSet<>();
			nearby.forEach(player -> {
				UUID uuid = player.getUniqueId();
				updated.add(uuid);

				if (!bossHealthViewers.contains(uuid)) {
					VersionHelper.getNMSManager().createBossBar(player, bossHealthBarId, title, BossBarColor.PURPLE,
							BossBarOverlay.PROGRESS, progress, true, true, true);
				} else {
					VersionHelper.getNMSManager().updateBossBarName(player, bossHealthBarId, title);
					VersionHelper.getNMSManager().updateBossBarProgress(player, bossHealthBarId, progress);
				}

				VersionHelper.getNMSManager().sendActionBar(player, AdventureHelper.componentToJson(title));

				// Small visual feedback
				if (Math.random() < 0.15) {
					world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), queen.getLocation(), 5, 0.6, 0.6, 0.6,
							0.01);
					world.spawnParticle(Particle.FLAME, queen.getLocation(), 3, 0.3, 0.3, 0.3, 0.01);
				}
			});

			bossHealthViewers.stream().filter(uuid -> !updated.contains(uuid)).map(Bukkit::getPlayer)
					.filter(Objects::nonNull)
					.forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, bossHealthBarId));

			bossHealthViewers.clear();
			bossHealthViewers.addAll(updated);

			if (queen.getHealth() <= 0.0) {
				end(true);
			}
		}

		public void end(boolean success) {
			if (mainTask != null && !mainTask.isCancelled()) {
				mainTask.cancel();
			}

			removeBossBar();
			removeBossHealthBar();

			int wavesCompleted = currentWave.get();
			int ghastsKilled = wavesCompleted * (3 + wavesCompleted) / 2 + (queenSpawned ? 1 : 0);
			long duration = System.currentTimeMillis() - startTime;

			Optional<UserData> dataOpt = instance.getStorageManager().getCachedUserData(ownerUUID);
			if (dataOpt.isEmpty())
				return;

			HellblockData hellblockData = dataOpt.get().getHellblockData();
			SkysiegeData data = hellblockData.getSkysiegeData();

			if (success) {
				data.recordSuccess(true, wavesCompleted, ghastsKilled, duration);
				world.spawnParticle(ParticleUtils.getParticle("FIREWORKS_SPARK"), center, 100, 4, 4, 4, 0.05);
				world.spawnParticle(Particle.SOUL, center, 40, 3, 3, 3, 0.02);
				AdventureHelper.playPositionalSound(world, center,
						Sound.sound(Key.key("minecraft:ui.toast.challenge_complete"), Source.BLOCK, 2.0f, 1.2f));
			} else {
				data.recordFailure();
				world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), center, 60, 4, 3, 4, 0.02);
				world.spawnParticle(Particle.LAVA, center, 20, 2, 2, 2, 0.03);
				world.strikeLightningEffect(center);
				AdventureHelper.playPositionalSound(world, center,
						Sound.sound(Key.key("minecraft:entity.blaze.hurt"), Source.BLOCK, 1.5f, 0.7f));
			}

			if (!success) {
				ghastIds.stream().map(Bukkit::getEntity)
						.filter(e -> e instanceof Ghast ghast && ghast.isValid() && !ghast.isDead()).map(e -> (Ghast) e)
						.forEach(ghast -> {
							Location loc = ghast.getLocation();
							World w = loc.getWorld();

							// Retreat animation
							for (int t = 0; t < 20; t += 3) {
								int delay = t;
								instance.getScheduler().sync().runLater(() -> {
									w.spawnParticle(Particle.SOUL, loc, 15, 0.6, 0.6, 0.6, 0.01);
									w.spawnParticle(Particle.REVERSE_PORTAL, loc, 10, 0.4, 0.4, 0.4, 0.01);
									w.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), loc, 5, 0.3, 0.3, 0.3,
											0.02);
									if (delay % 6 == 0) {
										AdventureHelper.playPositionalSound(w, loc, Sound.sound(
												Key.key("minecraft:block.portal.trigger"), Source.BLOCK, 0.7f, 1.3f));
									}
								}, delay, loc);
							}

							instance.getScheduler().sync().runLater(() -> {
								ghast.remove();
								w.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), loc, 30, 1, 1, 1, 0.02);
								AdventureHelper.playPositionalSound(w, loc, Sound.sound(
										Key.key("minecraft:entity.enderman.teleport"), Source.BLOCK, 1.5f, 0.6f));
							}, 20L, loc);
						});
			}

			world.getPlayers().stream().filter(player -> bounds.contains(player.getLocation().toVector()))
					.forEach(player -> {
						instance.getSenderFactory().wrap(player)
								.sendMessage(instance.getTranslationManager()
										.render(success ? MessageConstants.MSG_HELLBLOCK_SKYSIEGE_VICTORY.build()
												: MessageConstants.MSG_HELLBLOCK_SKYSIEGE_FAILURE.build()));

						Component summary = instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_SKYSIEGE_SUMMARY.arguments(
										AdventureHelper.miniMessageToComponent(String.valueOf(wavesCompleted)),
										AdventureHelper.miniMessageToComponent(String.valueOf(ghastsKilled)),
										AdventureHelper
												.miniMessageToComponent(String.format("%.1f%%", data.getSuccessRate())))
										.build());

						instance.getSenderFactory().wrap(player).sendMessage(summary);
					});

			activeSieges.remove(islandId);

			EventUtils.fireAndForget(new SkysiegeEndEvent(islandId, ownerUUID, world, success));
		}

		private void removeBossBar() {
			if (bossBarId == null)
				return;

			bossBarViewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
					.forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, bossBarId));

			bossBarViewers.clear();
			bossBarId = null;
		}

		private void removeBossHealthBar() {
			if (bossHealthBarId == null)
				return;

			bossHealthViewers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
					.forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, bossHealthBarId));

			bossHealthViewers.clear();
			bossHealthBarId = null;
		}

		private void cleanupDeadGhasts() {
			ghastIds.removeIf(id -> {
				Entity entity = Bukkit.getEntity(id);
				return entity == null || !entity.isValid() || entity.isDead();
			});
		}

		private boolean anyPlayersInBounds() {
			return world.getPlayers().stream().anyMatch(p -> bounds.contains(p.getLocation().toVector()));
		}

		private Location randomSkyLocation(Location base, int radiusXZ, int yRange) {
			double x = base.getX() + (Math.random() * radiusXZ * 2 - radiusXZ);
			double y = base.getY() + (Math.random() * yRange);
			double z = base.getZ() + (Math.random() * radiusXZ * 2 - radiusXZ);
			return new Location(base.getWorld(), x, y, z);
		}
	}
}