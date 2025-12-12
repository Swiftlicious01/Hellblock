package com.swiftlicious.hellblock.listeners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.ConfigManager.IslandEventData;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.events.invasion.InvasionBossKillEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionBossSpawnEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionChangeFormationEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionFailureEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionLootGenerateEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionRetreatEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionSpawnWaveEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionSynergyActivateEvent;
import com.swiftlicious.hellblock.events.invasion.InvasionVictoryEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.listeners.invasion.CustomInvasion;
import com.swiftlicious.hellblock.listeners.invasion.InvasionDifficulty;
import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation;
import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation.FormationType;
import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation.InvasionBehavior;
import com.swiftlicious.hellblock.listeners.invasion.InvasionMobFactory;
import com.swiftlicious.hellblock.listeners.invasion.InvasionMobFactory.InvasionSpawnContext;
import com.swiftlicious.hellblock.listeners.invasion.InvasionMobFactory.MountSpawnResult;
import com.swiftlicious.hellblock.listeners.invasion.InvasionProfile;
import com.swiftlicious.hellblock.listeners.invasion.InvasionSynergy;
import com.swiftlicious.hellblock.listeners.invasion.InvasionSynergy.SynergyPattern;
import com.swiftlicious.hellblock.nms.bossbar.BossBarColor;
import com.swiftlicious.hellblock.nms.bossbar.BossBarOverlay;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.player.InvasionData;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.AdventureMetadata;
import com.swiftlicious.hellblock.utils.EntityTypeUtils;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.PotionUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

public class PiglinInvasionHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	// Global active invasions, keyed by island ID
	private final Map<Integer, CustomInvasion> activeInvasions = new ConcurrentHashMap<>();
	// Main invasion scheduler
	private SchedulerTask invasionTask;
	private SchedulerTask bossBarUpdateTask;

	protected final InvasionMobFactory invasionMobFactory;
	protected final InvasionSynergy synergyHandler;
	protected final InvasionFormation formationHandler;

	private static final String INVASION_MOB_KEY = "piglin_invader";
	private static final String INVASION_BOSS_KEY = "piglin_commander";

	private final NamespacedKey invasionMobKey;
	private final NamespacedKey invasionBossKey;

	private static final List<LootEntry> BASTION_LOOT = createBastionLoot();

	@NotNull
	private static List<LootEntry> createBastionLoot() {
		List<LootEntry> loot = new ArrayList<>();

		addLoot(loot, "GOLD_BLOCK", 1, 3);
		addLoot(loot, "GOLD_INGOT", 8, 17);
		addLoot(loot, "GILDED_BLACKSTONE", 6, 12);
		addLoot(loot, "CRYING_OBSIDIAN", 4, 8);
		addLoot(loot, "BLACKSTONE", 6, 12);
		addLoot(loot, "POLISHED_BLACKSTONE_BRICKS", 4, 10);
		addLoot(loot, "BASALT", 4, 10);
		addLoot(loot, "NETHER_BRICKS", 4, 10);
		addLoot(loot, "STRING", 4, 10);
		addLoot(loot, "ARROW", 8, 20);
		addLoot(loot, "GRAVEL", 6, 12);
		addLoot(loot, "QUARTZ", 6, 12);
		addLoot(loot, "GLOWSTONE_DUST", 6, 12);
		addLoot(loot, "SOUL_SAND", 4, 8);
		addLoot(loot, "SOUL_SOIL", 4, 8);

		// Support both old and new names for CHAIN
		addLoot(loot, "CHAIN", 4, 8);
		addLoot(loot, "IRON_CHAIN", 4, 8);

		addLoot(loot, "ENDER_PEARL", 2, 6);
		addLoot(loot, "MAGMA_CREAM", 6, 10);
		addLoot(loot, "BLAZE_POWDER", 2, 5);
		addLoot(loot, "NETHER_WART", 2, 5);
		addLoot(loot, "GHAST_TEAR", 1, 1);
		addLoot(loot, "CHISELED_POLISHED_BLACKSTONE", 2, 6);
		addLoot(loot, "POLISHED_BASALT", 4, 8);
		addLoot(loot, "CRACKED_POLISHED_BLACKSTONE_BRICKS", 4, 8);
		addLoot(loot, "RED_NETHER_BRICKS", 4, 8);
		addLoot(loot, "SHROOMLIGHT", 1, 3);
		addLoot(loot, "WARPED_FUNGUS", 2, 5);
		addLoot(loot, "CRIMSON_FUNGUS", 2, 5);
		addLoot(loot, "TWISTING_VINES", 2, 6);
		addLoot(loot, "WEEPING_VINES", 2, 6);

		// Rare loot with drop chances
		addLoot(loot, "MUSIC_DISC_PIGSTEP", 1, 1, 0.05);
		addLoot(loot, "NETHERITE_SCRAP", 1, 3, 0.05);
		addLoot(loot, "NETHERITE_INGOT", 1, 1, 0.05);
		addLoot(loot, "ANCIENT_DEBRIS", 1, 1, 0.07);
		addLoot(loot, "LODESTONE", 1, 1, 0.03);

		return List.copyOf(loot); // Make unmodifiable
	}

	private static void addLoot(@NotNull List<LootEntry> list, @NotNull String materialName, int min, int max) {
		try {
			Material mat = Material.matchMaterial(materialName);
			if (mat != null) {
				list.add(new LootEntry(mat, min, max));
			}
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Material doesn't exist in this server version
		}
	}

	private static void addLoot(@NotNull List<LootEntry> list, @NotNull String materialName, int min, int max,
			double chance) {
		try {
			Material mat = Material.matchMaterial(materialName);
			if (mat != null) {
				list.add(new LootEntry(mat, min, max, chance));
			}
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			// Material doesn't exist in this server version
		}
	}

	private Random random = new SecureRandom();

	public PiglinInvasionHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.invasionMobKey = new NamespacedKey(plugin, INVASION_MOB_KEY);
		this.invasionBossKey = new NamespacedKey(plugin, INVASION_BOSS_KEY);
		this.invasionMobFactory = new InvasionMobFactory(plugin);
		this.synergyHandler = new InvasionSynergy(plugin);
		this.formationHandler = new InvasionFormation(plugin);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		startInvasionTask();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		synergyHandler.reset();
		formationHandler.reset();

		// Clean up all active invasions
		activeInvasions.values().forEach(invasion -> {
			invasion.cancelAllMobTasks();
			invasion.cancelSynergyTask();

			UUID bossBarId = invasion.getBossBarId();
			if (bossBarId != null) {
				invasion.getBossBarViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
						.filter(Player::isOnline)
						.forEach(viewerId -> VersionHelper.getNMSManager().removeBossBar(viewerId, bossBarId));
			}

			UUID bossHealthBarId = invasion.getBossHealthBarId();
			if (bossHealthBarId != null) {
				invasion.getBossHealthViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
						.filter(Player::isOnline)
						.forEach(viewerId -> VersionHelper.getNMSManager().removeBossBar(viewerId, bossHealthBarId));
			}
		});

		activeInvasions.clear();

		if (invasionTask != null && !invasionTask.isCancelled()) {
			invasionTask.cancel();
			invasionTask = null;
		}

		if (bossBarUpdateTask != null && !bossBarUpdateTask.isCancelled()) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}
	}

	@NotNull
	public InvasionMobFactory getInvaderFactory() {
		return this.invasionMobFactory;
	}

	@NotNull
	public InvasionSynergy getSynergyHandler() {
		return this.synergyHandler;
	}

	@NotNull
	public InvasionFormation getFormationHandler() {
		return this.formationHandler;
	}

	public int getIslandIdByLocation(@NotNull Location loc) {
		// Loop through all known loaded islands
		for (CustomInvasion invasion : activeInvasions.values()) {
			Optional<UserData> dataOpt = instance.getStorageManager().getCachedUserData(invasion.getOwnerUUID());
			if (dataOpt.isEmpty())
				continue;
			UserData data = dataOpt.get();
			HellblockData hellblockData = data.getHellblockData();
			if (hellblockData.getBoundingBox() == null)
				continue;

			if (hellblockData.getBoundingBox().contains(loc.toVector()))
				return hellblockData.getIslandId();
		}
		return -1; // Not found
	}

	private int calculateDifficultyTier(@NotNull HellblockData data) {
		InvasionData invasionData = data.getInvasionData();
		if (invasionData == null)
			return 1;

		float islandLevel = data.getIslandLevel();
		int streak = invasionData.getCurrentStreak();
		int bossKills = invasionData.getBossKills();

		// Base difficulty from island level
		int levelTier = (int) Math.floor(islandLevel / 100); // e.g., 0–10 for levels 0–1000

		// Streak bonus: 1 extra tier every 3 wins
		int streakBonus = streak / 3;

		// Boss kill bonus: every 5 boss kills = +1 tier
		int bossBonus = bossKills / 5;

		// Final tier calculation
		int tier = 1 + levelTier + streakBonus + bossBonus;

		// Clamp to max
		return Math.min(tier, InvasionDifficulty.getMaxTier());
	}

	private void ensureBossBarUpdateTaskRunning() {
		if (bossBarUpdateTask != null && !bossBarUpdateTask.isCancelled())
			return;

		bossBarUpdateTask = instance.getScheduler().asyncRepeating(() -> activeInvasions.values().forEach(invasion -> {
			int islandId = invasion.getIslandId();

			// End invasion if no one online inside bounds
			BoundingBox bounds = invasion.getBounds();
			boolean onlineInside = invasion.getOwnerUUID() != null
					&& instance.getStorageManager().getCachedUserData(invasion.getOwnerUUID())
							.map(user -> user.getHellblockData().getPartyPlusOwner().stream().map(Bukkit::getPlayer)
									.filter(Objects::nonNull).filter(Player::isOnline)
									.anyMatch(p -> bounds.contains(p.getLocation().toVector())))
							.orElse(false);

			if (!onlineInside) {
				endInvasion(islandId);
				return;
			}

			updateBossBarProgress(islandId);
			updateBossHealthBar(invasion);
		}), 20L, 20L, TimeUnit.SECONDS);
	}

	private void tryStopBossBarUpdateTask() {
		if (activeInvasions.isEmpty() && bossBarUpdateTask != null && !bossBarUpdateTask.isCancelled()) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}
	}

	private void startInvasionTask() {
		if (!instance.getConfigManager().invasionEventSettings().enabled())
			return;
		invasionTask = instance.getScheduler().asyncRepeating(() -> {
			// Cleanup stale invasions first
			cleanupStaleInvasions();

			// Then try starting new ones
			instance.getCoopManager().getCachedIslandOwnerData().thenAccept(
					allOwners -> allOwners.stream().filter(Objects::nonNull).forEach(this::tryStartInvasion));
		}, 3, 3, TimeUnit.MINUTES);

		instance.getScheduler().sync().runRepeating(() -> activeInvasions.values().forEach(this::updateBossHealthBar),
				20, 20, LocationUtils.getAnyLocationInstance()); // Every 1 second
	}

	private void cleanupStaleInvasions() {
		long now = System.currentTimeMillis();
		activeInvasions.entrySet().stream().filter(entry -> now - entry.getValue().getStartTime() > instance
				.getConfigManager().invasionEventSettings().maxDuration())
				.forEach(entry -> endInvasion(entry.getKey()));
	}

	@Nullable
	public CustomInvasion getInvasion(int islandID) {
		return activeInvasions.get(islandID);
	}

	public boolean isInvasionRunning(int islandID) {
		return activeInvasions.containsKey(islandID);
	}

	private void tryStartInvasion(@NotNull UserData user) {
		HellblockData data = user.getHellblockData();
		float level = data.getIslandLevel();
		IslandEventData eventData = instance.getConfigManager().invasionEventSettings();

		if (!data.hasHellblock()) {
			return;
		}

		if (data.isAbandoned()) {
			return;
		}

		if (level < eventData.levelRequired() || data.getBoundingBox() == null)
			return;

		int islandId = data.getIslandId();
		if (isInvasionRunning(islandId))
			return;

		if (instance.getSkysiegeHandler().isSkysiegeRunning(islandId)) {
			return;
		}

		boolean hasWeatherEvent = instance.getNetherWeatherManager().isWeatherActive(islandId);

		if (hasWeatherEvent || instance.getWitherHandler().getCustomWither().hasActiveWither(islandId))
			return;

		long lastTime = data.getInvasionData().getLastInvasionTime();
		if (System.currentTimeMillis() - lastTime < TimeUnit.MINUTES.toMillis(eventData.cooldown()))
			return;

		// At least one online player in bounds
		BoundingBox bounds = data.getBoundingBox();
		boolean activePlayerPresent = data.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
				.filter(Player::isOnline).anyMatch(p -> bounds.contains(p.getLocation().toVector()));

		if (!activePlayerPresent || bounds.getVolume() < 3000)
			return;

		float baseChance = 10f;
		float bonus = Math.min(15f, level * 0.1f); // up to +15%
		float finalChance = baseChance + bonus + 5f;
		if (RandomUtils.generateRandomFloat(0, 100) > finalChance)
			return;

		Optional<HellblockWorld<?>> optWorld = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));
		if (optWorld.isEmpty() || optWorld.get().bukkitWorld() == null)
			return;

		World world = optWorld.get().bukkitWorld();
		instance.debug("Invasion start attempted by " + user.getName() + " (UUID: " + user.getUUID() + ")");

		UUID owner = user.getUUID();
		int tier = calculateDifficultyTier(data);
		InvasionProfile profile = new InvasionProfile(InvasionDifficulty.getByTier(tier));
		CustomInvasion invasion = new CustomInvasion(islandId, owner, world, bounds, profile);

		activeInvasions.put(islandId, invasion);

		// Save invasion info
		InvasionData invasionData = data.getInvasionData();
		invasionData.setLastInvasionTime(System.currentTimeMillis());
		if (tier > invasionData.getHighestDifficultyTierReached())
			invasionData.setHighestDifficultyTierReached(tier);

		ensureBossBarUpdateTaskRunning();
		instance.getScheduler().executeSync(() -> startInvasion(data, world, bounds));
	}

	private void startInvasion(@NotNull HellblockData island, @NotNull World world, @NotNull BoundingBox bounds) {
		int islandId = island.getIslandId();
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		Location center = bounds.getCenter().toLocation(world);
		List<Player> onlinePlayers = island.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
				.filter(Player::isOnline).filter(p -> bounds.contains(p.getLocation().toVector())).toList();

		if (onlinePlayers.isEmpty()) {
			endInvasion(islandId);
			return;
		}

		onlinePlayers.forEach(player -> instance.getSenderFactory().wrap(player).sendMessage(
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_START.build())));

		// Effects
		AdventureHelper.playPositionalSound(world, center,
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.piglin.angry"), Source.BLOCK, 1f, 0.6f));
		playBeam(center, Particle.DUST_COLOR_TRANSITION, 10, 30, 1.5);
		AdventureHelper.playPositionalSound(world, center, Sound
				.sound(net.kyori.adventure.key.Key.key("minecraft:block.beacon.activate"), Source.BLOCK, 1.2f, 1.1f));

		// Begin synergy logic
		startSynergyLoop(invasion, center);

		// Wave scheduler
		int waveCount = invasion.getProfile().getWaveCount();
		long spacing = 160L - invasion.getProfile().getDifficulty().getTier() * 10;

		for (int i = 0; i < waveCount; i++) {
			int wave = i + 1;
			long delay = 60L + i * spacing;
			instance.getScheduler().sync().runLater(() -> spawnWave(center, world, island, wave), delay, center);
		}
	}

	private void startSynergyLoop(@NotNull CustomInvasion invasion, @NotNull Location center) {
		invasion.startSynergyTask(instance.getScheduler().sync().runRepeating(() -> {
			Mob leader = invasion.getLeader();
			if (leader == null || leader.isDead() || !leader.isValid()) {
				leader = formationHandler.promoteNewLeader(invasion);
				invasion.setLeader(leader);
			}

			Map<UUID, List<Mob>> nearby = new HashMap<>();

			for (UUID uuid : invasion.getMobIds()) {
				Entity entity = Bukkit.getEntity(uuid);
				if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead())
					continue;

				SynergyPattern pattern = synergyHandler.trySynergyPattern(mob, invasion);
				if (pattern != null) {
					EventUtils.fireAndForget(new InvasionSynergyActivateEvent(mob.getUniqueId(), pattern));
					instance.debug("Synergy pattern '" + pattern.name() + "' activated by " + mob.getType());
				}

				formationHandler.applyBehavior(mob, invasion);
				formationHandler.applyCommand(mob, invasion);

				if (leader != null && mob.getLocation().distanceSquared(leader.getLocation()) <= 100) {
					nearby.computeIfAbsent(leader.getUniqueId(), k -> new ArrayList<>()).add(mob);
				}
			}

			synergyHandler.tickLeaderAuras(invasion);
			synergyHandler.checkForRetreat(invasion);
			formationHandler.tickFormationClusters(invasion, synergyHandler);

			FormationType type = Optional.ofNullable(invasion.getCurrentFormation()).orElseGet(() -> {
				FormationType newType = FormationType.random();
				invasion.setCurrentFormation(newType);
				return newType;
			});

			if (leader != null) {
				List<Mob> allies = nearby.getOrDefault(leader.getUniqueId(), List.of());
				Player target = instance.getNetherrackGeneratorHandler().getClosestPlayer(leader.getLocation(),
						leader.getNearbyEntities(30, 10, 30));

				double angle = (target != null)
						? formationHandler.calculateYawTo(leader.getLocation(), target.getLocation())
						: 0;

				formationHandler.arrangeFormation(leader, allies, type, 4.5, angle);
				EventUtils.fireAndForget(new InvasionChangeFormationEvent(invasion.getOwnerUUID(), type));
				instance.debug("Formation " + type.name() + " applied to " + leader.getType() + " with " + allies.size()
						+ " allies.");
			}
		}, 60L, 100L, center));
	}

	public void endInvasion(int islandId) {
		CustomInvasion invasion = activeInvasions.remove(islandId);
		if (invasion == null)
			return;

		boolean failed = invasion.isBossAlive() || invasion.getRemainingWaveCount() > 0;
		UUID ownerUUID = invasion.getOwnerUUID();

		if (ownerUUID != null) {
			instance.getStorageManager().getCachedUserData(ownerUUID).ifPresent(user -> {
				HellblockData data = user.getHellblockData();

				if (failed) {
					data.getInvasionData().recordFailure();
					instance.debug("Invasion failure recorded for hellblock island ID: " + islandId);
					EventUtils.fireAndForget(new InvasionFailureEvent(ownerUUID, invasion.getProfile()));
				}

				BoundingBox bounds = data.getBoundingBox();
				if (bounds != null) {
					List<Player> players = data.getPartyPlusOwner().stream().map(Bukkit::getPlayer)
							.filter(Objects::nonNull).filter(Player::isOnline)
							.filter(p -> bounds.contains(p.getLocation().toVector())).toList();

					players.forEach(player -> {
						sendInvasionSummary(player, data.getInvasionData());
						AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.player.levelup"),
										Source.PLAYER, 1f, 1f));
					});
				}
			});
		}

		removeBossBar(islandId);
		fallbackDespawnMobs(invasion, true);
		fallbackDespawnBoss(invasion);
		cleanupRemainingMounts(invasion);
		invasion.cleanup();
		instance.debug("Ended invasion for island ID " + islandId);
		tryStopBossBarUpdateTask();
	}

	private void spawnWave(@NotNull Location base, @NotNull World world, @NotNull HellblockData island, int wave) {
		int islandId = island.getIslandId();
		BoundingBox box = island.getBoundingBox();
		if (box == null)
			return;

		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		List<Player> players = getOnlinePlayersWithinBox(island, box);
		if (players.isEmpty())
			return;

		AtomicInteger specialMobCounter = new AtomicInteger(0);
		int mobCount = invasion.getProfile().getMobsPerWave() + RandomUtils.generateRandomInt(0, 2);

		List<UUID> mobIds = Collections.synchronizedList(new ArrayList<>());
		List<Mob> spawnedMobs = Collections.synchronizedList(new ArrayList<>());

		spawnWaveMobs(base, world, box, mobCount, invasion, specialMobCounter, wave, mobIds, spawnedMobs);

		assignWaveBanner(mobIds, wave, invasion.getProfile().getWaveCount());
		arrangeFormationAndLeader(invasion, base, spawnedMobs);
		maybeSpawnBoss(wave, invasion, world, box, base, mobIds);

		instance.debug("Spawning wave #" + wave + " for invasion with island ID " + invasion.getIslandId());
		EventUtils
				.fireAndForget(new InvasionSpawnWaveEvent(invasion.getOwnerUUID(), new ArrayList<>(spawnedMobs), wave));

		formationHandler.clearFormationHosts();

		instance.getScheduler().sync().runLater(() -> {
			notifyPlayersOfWave(players, wave);
			invasion.addWaveMobs(mobIds);
			showBossBar(islandId, players, invasion.getTotalSpawned());

			instance.debug("Wave #" + wave + " spawned with " + mobIds.size() + " mobs.");
		}, 60L, base);
	}

	@NotNull
	private List<Player> getOnlinePlayersWithinBox(@NotNull HellblockData island, @NotNull BoundingBox box) {
		return island.getPartyPlusOwner().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
				.filter(Player::isOnline).filter(p -> box.contains(p.getLocation().toVector())).toList();
	}

	private void spawnWaveMobs(@NotNull Location base, @NotNull World world, @NotNull BoundingBox box, int mobCount,
			@NotNull CustomInvasion invasion, @NotNull AtomicInteger specialMobCounter, int wave,
			@NotNull List<UUID> mobIds, @NotNull List<Mob> spawnedMobs) {
		for (int i = 0; i < mobCount; i++) {
			Location loc = findValidSpawnLocation(world, box, base);
			if (loc == null)
				continue;

			Consumer<UUID> onSpawned = uuid -> {
				if (uuid != null) {
					mobIds.add(uuid);
					Entity entity = Bukkit.getEntity(uuid);
					if (entity instanceof Mob mob) {
						spawnedMobs.add(mob);
						startInvasionTargeting(uuid);
						InvasionBehavior behavior = formationHandler.decideBehaviorFor(mob, invasion);
						formationHandler.setMobBehavior(uuid, behavior);
					}
				}
			};

			InvasionSpawnContext context = new InvasionSpawnContext(invasion.getProfile(), specialMobCounter, random);

			EntityType peekedType = invasionMobFactory.peekNextMobType(context);

			if (peekedType == EntityType.HOGLIN || peekedType == EntityType.ZOGLIN) {
				spawnStompIn(loc, invasion, context, onSpawned);
			} else {
				InvasionSpawnStyle style = InvasionSpawnStyle.random();
				if (style == InvasionSpawnStyle.PORTAL) {
					spawnFromNetherPortal(loc, invasion, context, wave, onSpawned);
				} else {
					spawnFromGround(loc, invasion, context, wave, onSpawned);
				}
			}
		}
	}

	private void assignWaveBanner(@NotNull List<UUID> mobIds, int wave, int totalWaves) {
		UUID chosen = RandomUtils.getRandomElement(mobIds);
		Entity entity = Bukkit.getEntity(chosen);
		if (entity instanceof Piglin piglin) {
			Item<ItemStack> banner = getPiglinWarBanner(wave, totalWaves);
			piglin.getEquipment().setHelmet(banner.loadCopy());
		}
	}

	private void arrangeFormationAndLeader(@NotNull CustomInvasion invasion, @NotNull Location base,
			@NotNull List<Mob> spawnedMobs) {
		invasion.setCurrentFormation(FormationType.fromDifficulty(invasion.getProfile().getDifficulty()));
		formationHandler.arrangeWaveFormation(spawnedMobs, base);

		if (!spawnedMobs.isEmpty()) {
			Mob leader = RandomUtils.getRandomElement(spawnedMobs);
			synergyHandler.tagAsSynergyGroupLeader(leader);
		}
	}

	private void maybeSpawnBoss(int wave, @NotNull CustomInvasion invasion, @NotNull World world,
			@NotNull BoundingBox box, @NotNull Location base, @NotNull List<UUID> mobIds) {
		int totalWaves = invasion.getProfile().getWaveCount();
		if (wave != totalWaves)
			return;

		Location bossLoc = findValidSpawnLocation(world, box, base);
		if (bossLoc == null)
			return;

		spawnBossWithAnimation(bossLoc, uuid -> {
			if (uuid != null) {
				mobIds.add(uuid);
				startInvasionTargeting(uuid);
			}
		});
	}

	private void notifyPlayersOfWave(@NotNull List<Player> players, int wave) {
		Component message = instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_WAVE
				.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(wave))).build());
		players.forEach(p -> instance.getSenderFactory().wrap(p).sendMessage(message));
	}

	@NotNull
	private Item<ItemStack> getPiglinWarBanner(int wave, int totalWaves) {
		ItemStack banner = new ItemStack(Material.ORANGE_BANNER);
		Item<ItemStack> item = instance.getItemManager().wrap(banner);

		float progress = (float) wave / totalWaves;

		List<Map<String, Object>> pattern = switch ((int) (progress * 3)) {
		case 0 -> List.of(Map.of("pattern", "base", "color", "orange"), Map.of("pattern", "circle", "color", "black"),
				Map.of("pattern", "border", "color", "black"));
		case 1 -> List.of(Map.of("pattern", "base", "color", "red"),
				Map.of("pattern", "stripe_downleft", "color", "gold"),
				Map.of("pattern", "half_horizontal", "color", "black"), Map.of("pattern", "border", "color", "gold"));
		case 2, 3 -> List.of( // both 0.67–0.99 and 1.0 fall here
				Map.of("pattern", "base_gradient", "color", "black"), Map.of("pattern", "skull", "color", "orange"),
				Map.of("pattern", "bordure_indented", "color", "red"));
		default -> List.of();
		};

		item.setTag(Key.of("minecraft:banner_patterns"), pattern);
		item.setTag(Key.of("minecraft:custom_name"), instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_INVASION_BANNER_ITEM_NAME.build()));
		item.unbreakable(true);

		return item;
	}

	private void spawnBossWithAnimation(@NotNull Location loc, @NotNull Consumer<UUID> onSpawned) {
		World world = loc.getWorld();
		if (world == null)
			return;

		List<Location> portalBlocks = buildTemporaryPortal(loc);
		playPortalEffects(loc);

		instance.getScheduler().sync().runLater(() -> {
			UUID bossId = spawnBossPiglin(loc);
			if (bossId == null)
				return;

			onSpawned.accept(bossId);
			assignBossToInvasion(loc, bossId);
			cleanupPortal(portalBlocks);

			world.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), loc, 3);
			AdventureHelper.playPositionalSound(world, loc, Sound
					.sound(net.kyori.adventure.key.Key.key("minecraft:entity.wither.spawn"), Source.BLOCK, 1.3f, 0.5f));
		}, 60L, loc);
	}

	@NotNull
	private List<Location> buildTemporaryPortal(@NotNull Location loc) {
		List<Location> portalBlocks = new ArrayList<>();
		for (int y = 0; y < 4; y++) {
			for (int x = -1; x <= 2; x++) {
				Location blockLoc = loc.clone().add(x, y, 0);
				if (blockLoc.getBlock().getType().isAir()) {
					blockLoc.getBlock().setType(Material.NETHER_PORTAL);
					blockLoc.getBlock().setMetadata("invasion_portal", new FixedMetadataValue(instance, true));
					portalBlocks.add(blockLoc);
				}
			}
		}
		return portalBlocks;
	}

	private void cleanupPortal(@NotNull List<Location> portalBlocks) {
		portalBlocks.forEach(p -> {
			p.getBlock().removeMetadata("invasion_portal", instance);
			p.getBlock().setType(Material.AIR);
		});
	}

	private void playPortalEffects(@NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return;

		world.spawnParticle(Particle.PORTAL, loc, 100, 1.5, 2, 1.5, 0.3);
		world.spawnParticle(Particle.FLAME, loc, 60, 1.2, 1.5, 1.2, 0.1);
		AdventureHelper.playPositionalSound(world, loc, Sound.sound(
				net.kyori.adventure.key.Key.key("minecraft:block.beacon.power_select"), Source.BLOCK, 1.5f, 0.6f));
	}

	private void assignBossToInvasion(@NotNull Location loc, @NotNull UUID bossId) {
		int islandId = getIslandIdByLocation(loc);
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		invasion.setBossId(bossId);
		UUID healthBarId = UUID.randomUUID();
		invasion.setBossHealthBarId(healthBarId);

		PiglinBrute boss = (PiglinBrute) Bukkit.getEntity(bossId);
		if (boss == null)
			return;

		instance.debug("Boss mob " + boss.getType() + " spawned at " + boss.getLocation());
		EventUtils.fireAndForget(new InvasionBossSpawnEvent(invasion.getOwnerUUID(), boss));

		if (invasion.getProfile().doesBossHaveBuffs()) {
			boss.addPotionEffect(new PotionEffect(
					PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"), 20 * 60, 1));
			boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1));
			boss.addPotionEffect(new PotionEffect(
					PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"), 20 * 60, 1));
		}

		double max = new RtagEntity(boss).getAttributeBase("generic.max_health");
		Component healthTitle = instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_INVASION_BOSS_HEALTH_BAR
						.arguments(AdventureHelper.miniMessageToComponent(String.valueOf((int) max))).build());

		invasion.getBossBarViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(Player::isOnline)
				.forEach(player -> VersionHelper.getNMSManager().createBossBar(player, healthBarId, healthTitle,
						BossBarColor.PURPLE, BossBarOverlay.PROGRESS, 1.0f, true, true, true));
	}

	@Nullable
	private UUID spawnBossPiglin(@NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		PiglinBrute boss = (PiglinBrute) world.spawnEntity(loc, EntityType.PIGLIN_BRUTE);
		RtagEntity tag = new RtagEntity(boss);
		tag.setAttributeBase("generic.max_health", 100.0);
		boss = (PiglinBrute) tag.load();

		setupBossAttributes(boss);
		setupBossGear(boss);
		setupBossMetadata(boss);

		startSpiralParticles(boss);
		return boss.getUniqueId();
	}

	private void setupBossAttributes(@NotNull PiglinBrute boss) {
		boss.setHealth(100.0);
		boss.addPotionEffect(new PotionEffect(
				PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"), 9999, 1));
		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(boss);
	}

	private void setupBossGear(@NotNull PiglinBrute boss) {
		boss.getEquipment()
				.setItemInMainHand(instance.getItemManager().wrap(new ItemStack(Material.NETHERITE_AXE)).loadCopy());

		Material[] armorOptions = { Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS,
				Material.DIAMOND_BOOTS };

		Material chosen = RandomUtils.getRandomElement(Arrays.asList(armorOptions));
		Item<ItemStack> armor = instance.getItemManager().wrap(new ItemStack(chosen));

		if (RandomUtils.generateRandomBoolean()) {
			NamespacedKey thorns = Enchantment.THORNS.getKey();
			armor.addEnchantment(Key.of(thorns.getNamespace(), thorns.getKey()), 1);
		}

		switch (chosen) {
		case DIAMOND_HELMET -> boss.getEquipment().setHelmet(armor.loadCopy());
		case DIAMOND_CHESTPLATE -> boss.getEquipment().setChestplate(armor.loadCopy());
		case DIAMOND_LEGGINGS -> boss.getEquipment().setLeggings(armor.loadCopy());
		case DIAMOND_BOOTS -> boss.getEquipment().setBoots(armor.loadCopy());
		default -> throw new IllegalArgumentException("Unexpected value: " + chosen);
		}
	}

	private void setupBossMetadata(@NotNull PiglinBrute boss) {
		AdventureMetadata.setEntityCustomName(boss,
				instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_BOSS_NAME.build()));
		boss.setCustomNameVisible(true);
		boss.setGlowing(true);
		boss.getPersistentDataContainer().set(invasionBossKey, PersistentDataType.STRING, INVASION_BOSS_KEY);
	}

	private void startSpiralParticles(@Nullable Entity entity) {
		if (entity == null || !entity.isValid() || entity.isDead())
			return;

		SchedulerTask[] spiralTask = new SchedulerTask[1];
		spiralTask[0] = instance.getScheduler().sync().runRepeating(new SpiralEffectRunnable(entity, spiralTask), 0L,
				2L, entity.getLocation());
	}

	private void showBossBar(int islandId, List<@NotNull Player> players, int total) {
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		UUID barId = UUID.randomUUID();
		invasion.setBossBarId(barId);

		Component title = instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_BOSS_BAR
				.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(total))).build());

		players.forEach(player -> {
			invasion.getBossBarViewers().add(player.getUniqueId());
			VersionHelper.getNMSManager().createBossBar(player, barId, title, BossBarColor.RED,
					BossBarOverlay.NOTCHED_10, 1.0f, false, false, false);
		});
	}

	private void updateBossHealthBar(@NotNull CustomInvasion invasion) {
		UUID bossId = invasion.getBossId();
		UUID barId = invasion.getBossHealthBarId();

		if (bossId == null || barId == null)
			return;

		Entity entity = Bukkit.getEntity(bossId);
		if (!(entity instanceof PiglinBrute boss) || !boss.isValid() || boss.isDead()) {
			removeBossHealthBar(invasion);
			return;
		}

		List<Player> nearby = boss.getWorld().getNearbyEntities(boss.getLocation(), 30, 10, 30).stream()
				.filter(e -> e instanceof Player p && p.isOnline()).map(e -> (Player) e).toList();

		if (nearby.isEmpty()) {
			removeBossHealthBar(invasion);
			return;
		}

		float progress = calculateBossHealthProgress(boss);
		Component updatedTitle = instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_INVASION_BOSS_HEALTH_BAR
						.arguments(AdventureHelper.miniMessageToComponent(String.valueOf((int) boss.getHealth())))
						.build());

		Set<UUID> visiblePlayers = new HashSet<>();
		nearby.forEach(player -> {
			UUID uuid = player.getUniqueId();
			visiblePlayers.add(uuid);

			if (!invasion.getBossHealthViewers().contains(uuid)) {
				VersionHelper.getNMSManager().createBossBar(player, barId, updatedTitle, BossBarColor.PURPLE,
						BossBarOverlay.PROGRESS, progress, true, true, true);
			} else {
				VersionHelper.getNMSManager().updateBossBarProgress(player, barId, progress);
				VersionHelper.getNMSManager().updateBossBarName(player, barId, updatedTitle);
			}
		});

		// Cleanup old viewers
		invasion.getBossHealthViewers().stream().filter(uuid -> !visiblePlayers.contains(uuid)).map(Bukkit::getPlayer)
				.filter(Objects::nonNull).filter(Player::isOnline)
				.forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, barId));

		invasion.setBossHealthViewers(visiblePlayers);
	}

	private float calculateBossHealthProgress(@NotNull PiglinBrute boss) {
		RtagEntity taggedBoss = new RtagEntity(boss);
		double max = taggedBoss.getAttributeBase("generic.max_health");
		return (float) (boss.getHealth() / max);
	}

	private void updateBossBarProgress(int islandId) {
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		UUID barId = invasion.getBossBarId();
		if (barId == null)
			return;

		int remaining = invasion.getRemainingWaveCount();
		int total = invasion.getTotalSpawned();
		if (total <= 0)
			return;

		float progress = Math.max(0f, Math.min(1f, (float) remaining / total));
		Component updatedTitle = instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_INVASION_BOSS_BAR
						.arguments(AdventureHelper.miniMessageToComponent(String.valueOf(remaining))).build());

		Set<UUID> updatedViewers = new HashSet<>();
		invasion.getBossBarViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(Player::isOnline)
				.forEach(viewer -> {
					boolean inside = invasion.getBounds().contains(viewer.getLocation().toVector());
					if (inside) {
						VersionHelper.getNMSManager().updateBossBarProgress(viewer, barId, progress);
						VersionHelper.getNMSManager().updateBossBarName(viewer, barId, updatedTitle);
						updatedViewers.add(viewer.getUniqueId());
					} else {
						VersionHelper.getNMSManager().removeBossBar(viewer, barId);
					}
				});
		invasion.setBossBarViewers(updatedViewers);
	}

	private void removeBossBar(int islandId) {
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		removeBossHealthBar(invasion);

		UUID barId = invasion.getBossBarId();
		if (barId == null)
			return;

		invasion.getBossBarViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(Player::isOnline)
				.forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, barId));

		invasion.getBossBarViewers().clear();
		invasion.setBossBarId(null);
	}

	private void removeBossHealthBar(@NotNull CustomInvasion invasion) {
		UUID barId = invasion.getBossHealthBarId();
		if (barId == null)
			return;

		invasion.getBossHealthViewers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
				.filter(Player::isOnline).forEach(p -> VersionHelper.getNMSManager().removeBossBar(p, barId));

		invasion.getBossHealthViewers().clear();
		invasion.setBossHealthBarId(null);
	}

	private void startInvasionTargeting(@NotNull UUID uuid) {
		Entity entity = Bukkit.getEntity(uuid);
		if (!(entity instanceof Mob mob))
			return;

		int islandId = getIslandIdByLocation(mob.getLocation());
		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		prepareMobForInvasion(mob);

		SchedulerTask boundsCheckTask = startBoundsEnforcer(mob, invasion, uuid);
		SchedulerTask targetingTask = startAggressionTask(mob, invasion, uuid);

		invasion.addMobTask(uuid, targetingTask, boundsCheckTask);
	}

	private void prepareMobForInvasion(@NotNull Mob mob) {
		if (mob instanceof Piglin piglin) {
			piglin.setImmuneToZombification(true);
			piglin.setCanPickupItems(false);
		}

		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(mob);
		// Optional: Apply short invulnerability during targeting setup
		markMobTemporarilyInvulnerable(mob, 40L); // 2 seconds safety buffer

		instance.debug("Prepared " + mob.getType() + " (" + mob.getUniqueId() + ") for invasion behavior.");
	}

	@NotNull
	private SchedulerTask startBoundsEnforcer(@NotNull Mob mob, @NotNull CustomInvasion invasion, @NotNull UUID mobId) {
		return instance.getScheduler().sync().runRepeating(() -> {
			if (!mob.isValid() || mob.isDead()) {
				cancelInvasionMobTask(invasion.getMobGoalTask(mobId));
				return;
			}

			if (!invasion.getBounds().contains(mob.getLocation().toVector())) {
				Location center = invasion.getBounds().getCenter().toLocation(mob.getWorld());
				Vector toCenter = center.toVector().subtract(mob.getLocation().toVector()).normalize().multiply(0.6);
				mob.setVelocity(toCenter);

				mob.getWorld().spawnParticle(ParticleUtils.getParticle("SMOKE_NORMAL"), mob.getLocation(), 4, 0.2, 0.2,
						0.2, 0.01);
				AdventureHelper.playPositionalSound(mob.getWorld(), mob.getLocation(), Sound.sound(
						net.kyori.adventure.key.Key.key("minecraft:entity.arrow.hit"), Source.BLOCK, 0.6f, 1.2f));

				instance.debug("Re-centered invasion mob " + mob.getType() + " back into invasion bounds.");
			}
		}, 20L, 20L, mob.getLocation());
	}

	@NotNull
	private SchedulerTask startAggressionTask(@NotNull Mob mob, @NotNull CustomInvasion invasion, @NotNull UUID mobId) {
		long retargetRate = Math.max(20L, 100L - (invasion.getProfile().getDifficulty().getTier() * 5L));
		return instance.getScheduler().sync().runRepeating(new Runnable() {
			private int ticks = 0;

			@Override
			public void run() {
				if (!mob.isValid() || mob.isDead()) {
					cancelInvasionMobTask(invasion.getMobTargetingTask(mobId));
					return;
				}

				ticks++;
				// Refresh target only every 5 seconds or if none
				if (ticks % 100 != 0 && mob.getTarget() != null && mob.getTarget().isValid()
						&& !mob.getTarget().isDead())
					return;

				List<LivingEntity> potentialTargets = mob.getWorld().getNearbyEntities(mob.getLocation(), 30, 10, 30)
						.stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).filter(target -> {
							if (target instanceof Player player)
								return player.isOnline();
							if (target instanceof Snowman snowman)
								return instance.getGolemHandler().isHellGolem(snowman);
							return false;
						}).toList();

				if (!potentialTargets.isEmpty()) {
					LivingEntity chosen = RandomUtils.getRandomElement(potentialTargets);
					mob.setTarget(chosen);

					if (mob instanceof Piglin pig) {
						pig.setTarget(chosen);
						if (VersionHelper.isPaperFork()) {
							try {
								Method setAggressive = pig.getClass().getMethod("setAggressive", boolean.class);
								setAggressive.invoke(pig, true);
							} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
								instance.getPluginLogger().warn("Could not set pig aggressive: " + e.getMessage());
							}
						}
					}

					instance.debug("Invasion mob " + mob.getType() + " targeting " + chosen.getType());
				}
			}
		}, RandomUtils.generateRandomInt(0, 20), retargetRate, mob.getLocation());
	}

	private void cancelInvasionMobTask(@Nullable SchedulerTask task) {
		if (task != null && !task.isCancelled()) {
			task.cancel();
		}
	}

	private void stopInvasionTargeting(@NotNull UUID uuid) {
		for (CustomInvasion invasion : activeInvasions.values()) {
			SchedulerTask task = invasion.getMobTargetingTask(uuid);
			if (task != null && !task.isCancelled()) {
				task.cancel();
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteractPiglin(PlayerInteractEntityEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getRightClicked().getWorld())) {
			return;
		}
		if (!(event.getRightClicked() instanceof Piglin piglin))
			return;

		// Prevent bartering if it's an invasion mob
		if (isInvasionMob(piglin)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getPlayer().getWorld())) {
			return;
		}
		Block portalBlock = event.getFrom().getBlock();

		if (portalBlock.getType() == Material.NETHER_PORTAL && portalBlock.hasMetadata("invasion_portal")) {
			event.setCancelled(true);
			AdventureHelper.playPositionalSound(event.getPlayer().getWorld(), event.getPlayer().getLocation(),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.portal.trigger"), Source.BLOCK, 0.8f,
							0.6f));
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInvasionMobDamage(EntityDamageEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		Entity entity = event.getEntity();
		if (!(entity instanceof Mob mob))
			return;
		if (!isInvasionMob(mob))
			return;

		if (mob.hasMetadata("invulnerable_phase")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		Entity entity = event.getEntity();
		UUID entityId = entity.getUniqueId();

		stopInvasionTargeting(entityId);

		int islandId = resolveInvasionRemoval(entityId);
		if (islandId == -1)
			return;

		CustomInvasion invasion = getInvasion(islandId);
		if (invasion == null)
			return;

		updateBossBarProgress(islandId);

		handleMountDeath(entity, invasion);
		handlePiglinOnMountDeath(entity, invasion);
		handleSynergyLeaderDeath(entity, invasion);
		handleBossDeath(entity, invasion);
		checkInvasionEndCondition(invasion, islandId);
	}

	private int resolveInvasionRemoval(@NotNull UUID entityId) {
		for (Map.Entry<Integer, CustomInvasion> entry : activeInvasions.entrySet()) {
			CustomInvasion invasion = entry.getValue();

			if (invasion.isBoss(entityId)) {
				invasion.setBossId(null); // Mark boss as dead
				return entry.getKey();
			}

			if (invasion.getMobIds().remove(entityId)) {
				return entry.getKey();
			}
		}
		return -1;
	}

	private void handleMountDeath(@NotNull Entity entity, @NotNull CustomInvasion invasion) {
		if (!(entity instanceof Piglin piglin))
			return;
		if (!isInvasionMob(piglin))
			return;
		UUID piglinId = piglin.getUniqueId();
		UUID mountId = invasion.getMountId(piglinId);
		if (mountId != null) {
			invasion.cancelMobGoalTask(mountId);
			invasion.removeRetreatMountMapping(piglinId);
			formationHandler.clearFormationHost(piglinId);
		}
	}

	private void handlePiglinOnMountDeath(@NotNull Entity mount, @NotNull CustomInvasion invasion) {
		UUID mountId = mount.getUniqueId();

		// Find all piglins that used this mount
		for (Map.Entry<UUID, UUID> entry : invasion.getMountMappings().entrySet()) {
			if (entry.getValue().equals(mountId)) {
				UUID piglinId = entry.getKey();
				invasion.cancelMobTargetingTask(piglinId);
				invasion.cancelMobGoalTask(piglinId);
				invasion.removeRetreatMountMapping(piglinId);
				formationHandler.clearFormationHost(piglinId);
				break;
			}
		}
	}

	private void handleSynergyLeaderDeath(@NotNull Entity entity, @NotNull CustomInvasion invasion) {
		if (!(entity instanceof Mob mob))
			return;
		if (!synergyHandler.isSynergyGroupLeader(mob))
			return;

		invasion.getMobIds().stream().map(Bukkit::getEntity)
				.filter(e -> e instanceof Mob ally && ally.isValid() && !ally.isDead() && !ally.equals(mob))
				.map(e -> (Mob) e).forEach(ally -> {
					Vector scatter = new Vector(RandomUtils.generateRandomFloat(-1.5f, 1.5f), 0.3,
							RandomUtils.generateRandomFloat(-1.5f, 1.5f)).normalize().multiply(0.7);

					ally.setVelocity(scatter);
					ally.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
					ally.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, ally.getLocation(), 6, 0.3, 0.4, 0.3,
							0.01);
				});

		entity.getWorld().spawnParticle(Particle.SOUL, entity.getLocation(), 20, 0.5, 0.8, 0.5, 0.03);
		AdventureHelper.playPositionalSound(entity.getWorld(), entity.getLocation(), Sound.sound(
				net.kyori.adventure.key.Key.key("minecraft:block.respawn_anchor.deplete"), Source.BLOCK, 1.4f, 0.6f));

		promoteNewSynergyLeader(invasion, entity);
	}

	private void promoteNewSynergyLeader(@NotNull CustomInvasion invasion, @NotNull Entity oldLeader) {
		List<Mob> candidates = invasion.getMobIds().stream().map(Bukkit::getEntity)
				.filter(e -> e instanceof Mob m && m.isValid() && !m.isDead() && !m.equals(oldLeader)).map(e -> (Mob) e)
				.toList();

		if (candidates.isEmpty())
			return;

		Mob promoted = candidates.stream()
				.max(Comparator.comparingDouble(m -> new RtagEntity(m).getAttributeBase("generic.max_health")))
				.orElse(null);

		if (promoted != null) {
			synergyHandler.tagAsSynergyGroupLeader(promoted);
			promoted.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, promoted.getLocation(), 16, 0.5, 1, 0.5, 0.03);
			AdventureHelper.playPositionalSound(promoted.getWorld(), promoted.getLocation(),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.evoker.prepare_summon"), Source.BLOCK,
							1.2f, 1.0f));
		}
	}

	private void handleBossDeath(@NotNull Entity entity, @NotNull CustomInvasion invasion) {
		if (!(entity instanceof PiglinBrute brute))
			return;
		if (invasion.getBossId() != null)
			return;

		instance.debug("Boss mob " + brute.getType() + " killed at " + brute.getLocation());
		EventUtils.fireAndForget(new InvasionBossKillEvent(invasion.getOwnerUUID(), brute));

		if (!invasion.getMobIds().isEmpty()) {
			Location portalBase = findValidPortalSpawn(invasion.getBounds(), brute.getWorld());
			if (portalBase != null) {
				boolean faceX = RandomUtils.generateRandomBoolean();
				spawnRetreatPortal(portalBase, new ArrayList<>(invasion.getMobIds()), invasion, faceX);
				Bukkit.getPluginManager().callEvent(new InvasionRetreatEvent(invasion.getOwnerUUID(), portalBase));
			} else {
				fallbackDespawnMobs(invasion, false);
				cleanupRemainingMounts(invasion);
			}
		} else {
			finishInvasionWithVictory(invasion, brute);
		}
	}

	private void fallbackDespawnMobs(@NotNull CustomInvasion invasion, boolean staleInvasion) {
		if (invasion.getMobIds().isEmpty())
			return;
		instance.getScheduler().sync().runLater(() -> {
			invasion.getMobIds().forEach(uuid -> {
				Entity e = Bukkit.getEntity(uuid);
				if (e != null && e.isValid() && !e.isDead()) {
					e.getWorld().spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), e.getLocation(), 8, 0.25, 0.25,
							0.25, 0.02);
					AdventureHelper.playPositionalSound(e.getWorld(), e.getLocation(),
							Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.enderman.teleport"),
									Source.BLOCK, 0.9f, 1.1f));
					e.remove();
				}
			});
			if (!staleInvasion)
				instance.debug("Retreat portal failed to spawn. Despawning all invasion mobs.");
			invasion.getMobIds().clear();
		}, 40L, invasion.getBounds().getCenter().toLocation(invasion.getWorld()));
	}

	private void fallbackDespawnBoss(@NotNull CustomInvasion invasion) {
		if (invasion.getBossId() == null)
			return;
		instance.getScheduler().sync().runLater(() -> {
			Entity e = Bukkit.getEntity(invasion.getBossId());
			if (e != null && e.isValid() && !e.isDead()) {
				e.getWorld().spawnParticle(Particle.DRAGON_BREATH, e.getLocation(), 20, 0.5, 0.5, 0.5, 0.03);
				AdventureHelper.playPositionalSound(e.getWorld(), e.getLocation(), Sound.sound(
						net.kyori.adventure.key.Key.key("minecraft:entity.wither.death"), Source.BLOCK, 1.0f, 0.9f));
				e.remove();
			}
			invasion.setBossId(null);
		}, 40L, invasion.getBounds().getCenter().toLocation(invasion.getWorld()));
	}

	private void finishInvasionWithVictory(@NotNull CustomInvasion invasion, @NotNull PiglinBrute brute) {
		instance.getHellblockHandler().getHellblockByWorld(brute.getWorld(), brute.getLocation()).thenAccept(island -> {
			if (island == null || island.getBoundingBox() == null)
				return;

			Location center = island.getBoundingBox().getCenter().toLocation(brute.getWorld());

			instance.getScheduler().sync().runLater(() -> {
				island.getInvasionData().recordVictory(true);
				instance.debug("Invasion victory recorded for hellblock island ID: " + invasion.getIslandId());
				EventUtils.fireAndForget(new InvasionVictoryEvent(invasion.getOwnerUUID(), invasion.getProfile()));
				spawnLootChest(center);
				cleanupRemainingMounts(invasion);
				endInvasion(island.getIslandId());
			}, 60L, center);
		});
	}

	private void checkInvasionEndCondition(@NotNull CustomInvasion invasion, int islandId) {
		if (invasion.getRemainingWaveCount() <= 0) {
			endInvasion(islandId);
		}
	}

	private void spawnLootChest(@NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return;

		Location spawnBelow = loc.clone().subtract(0, 3, 0);
		FallingBlock fakeChest = spawnFallingChest(world, spawnBelow);

		SchedulerTask[] task = new SchedulerTask[1];
		task[0] = instance.getScheduler().sync().runRepeating(new Runnable() {
			int ticks = 0;

			@Override
			public void run() {
				if (ticks >= 60) {
					fakeChest.remove();
					loc.getBlock().setType(Material.CHEST);

					if (!(loc.getBlock().getState() instanceof Chest chest))
						return;

					int islandId = getIslandIdByLocation(loc);
					CustomInvasion invasion = getInvasion(islandId);
					if (invasion == null)
						return;

					UUID ownerId = invasion.getOwnerUUID();
					if (ownerId == null)
						return;

					// Generate and scale loot
					List<ItemStack> finalLootItems = generateScaledLoot(invasion, ownerId, loc, chest);
					instance.debug("Placed invasion chest including the following loot: " + finalLootItems.stream()
							.map(item -> item.getType().toString()).collect(Collectors.joining(", ")));

					// Determine players and apply buffs
					instance.getStorageManager().getCachedUserData(ownerId).ifPresent(ownerData -> {
						float level = ownerData.getHellblockData().getIslandLevel();
						int difficulty = invasion.getProfile().getDifficulty().getTier();
						double scalingFactor = computeScalingFactor(level, difficulty);

						Set<UUID> allPlayerIds = ownerData.getHellblockData().getPartyPlusOwner();
						List<Player> players = allPlayerIds.stream().map(Bukkit::getPlayer).filter(Objects::nonNull)
								.filter(Player::isOnline)
								.filter(p -> invasion.getBounds().contains(p.getLocation().toVector())).toList();

						applyVictoryBuffs(players, ownerId, scalingFactor);

						// Fireworks
						if (!players.isEmpty()) {
							Player target = RandomUtils.getRandomElement(players);
							launchFireworkAtTarget(loc.clone().add(0.5, 1, 0.5), target,
									invasion.getProfile().getDifficulty().getTier());
						}
					});

					// Visual + sound FX
					triggerLootChestParticles(world, loc);

					if (task[0] != null && !task[0].isCancelled())
						task[0].cancel();
					return;
				}

				// Animate floating chest
				Location newLoc = spawnBelow.clone().add(0, ticks * 0.05, 0);
				fakeChest.teleport(newLoc);
				world.spawnParticle(Particle.CLOUD, newLoc, 4, 0.1, 0.1, 0.1, 0.01);
				ticks++;
			}
		}, 0L, 1L, spawnBelow);
	}

	@SuppressWarnings("deprecation")
	@NotNull
	private FallingBlock spawnFallingChest(@NotNull World world, @NotNull Location spawnBelow) {
		FallingBlock fakeChest = world.spawnFallingBlock(spawnBelow, Material.CHEST.createBlockData());
		fakeChest.setGravity(false);
		fakeChest.setDropItem(false);
		return fakeChest;
	}

	@NotNull
	private List<ItemStack> generateScaledLoot(@NotNull CustomInvasion invasion, @NotNull UUID ownerId,
			@NotNull Location loc, @NotNull Chest chest) {
		List<ItemStack> lootItems = generateBastionLoot(invasion.getProfile()).stream().map(Item::loadCopy).toList();

		InvasionLootGenerateEvent lootEvent = new InvasionLootGenerateEvent(ownerId, invasion.getProfile(), loc,
				lootItems);
		EventUtils.fireAndForget(lootEvent);
		List<ItemStack> finalLootItems = lootEvent.getLoot();

		instance.debug(
				"Loot generated for invasion " + invasion.getIslandId() + " | Loot count: " + finalLootItems.size());

		Inventory chestInv = chest.getBlockInventory();

		instance.getStorageManager().getCachedUserData(ownerId).ifPresent(ownerData -> {
			float level = ownerData.getHellblockData().getIslandLevel();
			int difficulty = invasion.getProfile().getDifficulty().getTier();

			double scalingFactor = computeScalingFactor(level, difficulty);
			double duplicationChance = computeDuplicationChance(scalingFactor);
			double skipChance = computeSkipChance(scalingFactor);

			// Duplicate and shuffle loot
			List<ItemStack> finalLoot = new ArrayList<>();
			finalLootItems.forEach(item -> {
				finalLoot.add(item);
				if (RandomUtils.generateRandomDouble() < duplicationChance) {
					finalLoot.add(item.clone());
				}
			});

			Collections.shuffle(finalLoot);
			List<Integer> slots = IntStream.range(0, chestInv.getSize()).boxed().collect(Collectors.toList());
			Collections.shuffle(slots);

			int slotIndex = 0;
			for (ItemStack item : finalLoot) {
				while (slotIndex < slots.size()) {
					if (RandomUtils.generateRandomDouble() > skipChance) {
						chestInv.setItem(slots.get(slotIndex), item);
						slotIndex++;
						break;
					}
					slotIndex++;
				}
				if (slotIndex >= slots.size())
					break;
			}
		});

		return finalLootItems;
	}

	private double computeScalingFactor(float level, int difficulty) {
		double levelFactor = Math.min(level / 1000.0, 1.0);
		double difficultyFactor = Math.min(difficulty / 10.0, 1.0);
		return (levelFactor * 0.7) + (difficultyFactor * 0.3);
	}

	private double computeDuplicationChance(double scalingFactor) {
		double base = 0.05;
		double max = 0.25;
		return base + (max - base) * scalingFactor;
	}

	private double computeSkipChance(double scalingFactor) {
		double base = 0.30;
		double min = 0.10;
		return base - (base - min) * scalingFactor;
	}

	private void applyVictoryBuffs(@NotNull List<Player> players, @NotNull UUID ownerId, double scalingFactor) {
		players.forEach(player -> {
			boolean isOwner = player.getUniqueId().equals(ownerId);
			int baseDurationSeconds = (int) (300 + (300 * scalingFactor));
			int duration = 20 * baseDurationSeconds + (isOwner ? 20 * 120 : 0);

			int absorptionAmp = scalingFactor >= 0.8 ? 2 : (scalingFactor >= 0.5 ? 1 : 0);
			int resistanceAmp = scalingFactor >= 0.7 ? 1 : 0;
			int regenAmp = scalingFactor >= 0.9 ? 1 : 0;

			applyPotionIfBetter(player, PotionEffectType.ABSORPTION, duration, absorptionAmp);
			applyPotionIfBetter(player, PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"),
					duration, resistanceAmp);
			applyPotionIfBetter(player, PotionEffectType.REGENERATION, duration, regenAmp);

			player.getWorld().spawnParticle(Particle.HEART, player.getLocation().clone().add(0, 1.5, 0), 3, 0.3, 0.4,
					0.3, 0.01);
			AdventureHelper.playPositionalSound(player.getWorld(), player.getLocation(),
					Sound.sound(net.kyori.adventure.key.Key.key("minecraft:item.totem.use"), Source.BLOCK, 1.1f, 1.2f));

			instance.debug("Buffs applied to " + player.getName() + " | Absorption: " + absorptionAmp
					+ " | Resistance: " + resistanceAmp + " | Regen: " + regenAmp);
		});
	}

	private void applyPotionIfBetter(@NotNull Player player, @NotNull PotionEffectType type, int duration,
			int amplifier) {
		PotionEffect current = player.getPotionEffect(type);
		if (current == null || current.getAmplifier() < amplifier || current.getDuration() < duration) {
			player.addPotionEffect(new PotionEffect(type, duration, amplifier));
		}
	}

	private void launchFireworkAtTarget(@NotNull Location loc, @NotNull Player target, int tier) {
		World world = loc.getWorld();
		Firework firework = (Firework) world.spawnEntity(loc,
				EntityTypeUtils.getCompatibleEntityType("FIREWORK_ROCKET", "FIREWORK"));
		FireworkMeta meta = firework.getFireworkMeta();

		int power = tier >= 9 ? 3 : tier >= 7 ? 2 : 1;
		meta.setPower(power);
		meta.addEffect(FireworkEffect.builder().withColor(org.bukkit.Color.RED).withFade(org.bukkit.Color.ORANGE)
				.with(FireworkEffect.Type.BURST).flicker(true).trail(tier >= 8).build());
		firework.setFireworkMeta(meta);

		instance.getScheduler().sync().runRepeating(new Runnable() {
			int fwTicks = 0;

			@Override
			public void run() {
				if (fwTicks++ > 50 || !target.isOnline() || !firework.isValid() || firework.isDead())
					return;

				Location targetLoc = target.getLocation().clone().add(0, 1.5, 0);
				Vector direction = targetLoc.toVector().clone().subtract(firework.getLocation().toVector()).normalize()
						.multiply(0.4);
				firework.setVelocity(direction);
			}
		}, 0L, 1L, loc);
	}

	private void triggerLootChestParticles(@NotNull World world, @NotNull Location loc) {
		world.spawnParticle(Particle.PORTAL, loc, 80, 0.6, 0.6, 0.6, 0.1);
		world.spawnParticle(ParticleUtils.getParticle("FIREWORKS_SPARK"), loc.clone().add(0, 1.2, 0), 50, 0.2, 0.4, 0.2,
				0.01);
		AdventureHelper.playPositionalSound(world, loc, Sound.sound(
				net.kyori.adventure.key.Key.key("minecraft:block.respawn_anchor.charge"), Source.BLOCK, 1.4f, 1.2f));
	}

	@NotNull
	private List<Item<ItemStack>> generateBastionLoot(@NotNull InvasionProfile profile) {
		List<Item<ItemStack>> loot = new ArrayList<>();
		float multiplier = profile.getLootMultiplier();

		// Create scaled loot
		for (LootEntry entry : BASTION_LOOT) {
			if (Math.random() > entry.chance)
				continue;

			int baseAmount = RandomUtils.generateRandomInt(entry.min, entry.max);
			int scaledAmount = Math.max(1, Math.round(baseAmount * multiplier));

			ItemStack stack = new ItemStack(entry.material, scaledAmount);
			loot.add(instance.getItemManager().wrap(stack));
		}

		// 30% chance for soul speed enchanted book
		if (RandomUtils.generateRandomInt(100) < 30) {
			Item<ItemStack> book = instance.getItemManager().wrap(new ItemStack(Material.ENCHANTED_BOOK));
			final NamespacedKey soulSpeedKey = Enchantment.SOUL_SPEED.getKey();
			book.addStoredEnchantment(Key.of(soulSpeedKey.getNamespace(), soulSpeedKey.getKey()), 3);
			loot.add(book);
		}

		// 10% chance for custom enchanted book
		if (RandomUtils.generateRandomInt(100) < 10) {
			int type = RandomUtils.generateRandomInt(0, 3);
			Item<ItemStack> book;

			switch (type) {
			case 0 ->
				book = instance.getMagmaWalkerHandler().createMagmaWalkerBook(RandomUtils.generateRandomInt(1, 3));
			case 1 -> book = instance.getLavaVisionHandler().createLavaVisionBook(RandomUtils.generateRandomInt(1, 3));
			case 2 -> book = instance.getMoltenCoreHandler().createMoltenCoreBook(RandomUtils.generateRandomInt(1, 3));
			case 3 ->
				book = instance.getCrimsonThornsHandler().createCrimsonThornsBook(RandomUtils.generateRandomInt(1, 3));
			default -> throw new IllegalStateException("Unexpected value: " + type);
			}

			loot.add(book);
		}

		// Shuffle and return up to 7 items
		Collections.shuffle(loot);
		return loot.subList(0, Math.min(7, loot.size()));
	}

	private void spawnFromNetherPortal(@NotNull Location spawnCenter, @NotNull CustomInvasion invasion,
			@NotNull InvasionSpawnContext context, int wave, @NotNull Consumer<UUID> onSpawned) {
		World world = spawnCenter.getWorld();
		if (world == null)
			return;

		List<Location> portalBlocks = new ArrayList<>();

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 2; x++) {
				Location blockLoc = spawnCenter.clone().add(x - 0.5, y, 0);
				if (blockLoc.getBlock().getType() == Material.AIR) {
					blockLoc.getBlock().setType(Material.NETHER_PORTAL);
					blockLoc.getBlock().setMetadata("invasion_portal", new FixedMetadataValue(instance, true));
					portalBlocks.add(blockLoc);
				}
			}
		}

		List<Location> obsidianFrame = List.of(spawnCenter.clone().add(-1, 0, 0), spawnCenter.clone().add(2, 0, 0),
				spawnCenter.clone().add(-1, 1, 0), spawnCenter.clone().add(2, 1, 0), spawnCenter.clone().add(-1, 2, 0),
				spawnCenter.clone().add(2, 2, 0), spawnCenter.clone().add(0, -1, 0), spawnCenter.clone().add(1, -1, 0),
				spawnCenter.clone().add(0, 3, 0), spawnCenter.clone().add(1, 3, 0));

		obsidianFrame.stream().filter(frame -> frame.getBlock().getType() == Material.AIR).forEach(frame -> {
			frame.getBlock().setType(Material.OBSIDIAN);
			portalBlocks.add(frame);
		});

		world.spawnParticle(Particle.PORTAL, spawnCenter, 60, 1, 1, 1, 0.2);
		AdventureHelper.playPositionalSound(world, spawnCenter,
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.portal.ambient"), Source.BLOCK, 1f, 1f));

		// Spawn after delay
		instance.getScheduler().sync().runLater(() -> {
			invasionMobFactory.spawnInvaderAt(spawnCenter, invasion, context, mob -> {
				if (onSpawned != null)
					onSpawned.accept(mob.getUniqueId());

				// Make mob temporarily invulnerable after spawning
				markMobTemporarilyInvulnerable(mob, 60L); // 3 seconds (60 ticks)

				if (mob instanceof Piglin piglin) {
					MountSpawnResult result = invasionMobFactory.maybeMountPiglin(piglin, spawnCenter, invasion,
							context, wave);
					if (result != null) {
						invasion.addRetreatMountMapping(piglin.getUniqueId(), result.mount().getUniqueId());
						formationHandler.setFormationHost(piglin.getUniqueId(), result.mount().getUniqueId());
					}
				}
			});

			portalBlocks.forEach(loc -> {
				loc.getBlock().removeMetadata("invasion_portal", instance);
				loc.getBlock().setType(Material.AIR);
			});
			world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), spawnCenter, 20, 0.5, 1, 0.5, 0.01);
			AdventureHelper.playPositionalSound(world, spawnCenter, Sound
					.sound(net.kyori.adventure.key.Key.key("minecraft:block.portal.travel"), Source.BLOCK, 1.2f, 0.6f));
		}, 40L, spawnCenter);
	}

	private void spawnFromGround(@NotNull Location loc, @NotNull CustomInvasion invasion,
			@NotNull InvasionSpawnContext context, int wave, @NotNull Consumer<UUID> onSpawned) {
		World world = loc.getWorld();
		if (world == null)
			return;

		Location below = loc.clone().subtract(0, 1, 0);
		BlockData baseBlock = below.getBlock().getBlockData();

		for (int i = 0; i < 20; i++) {
			int delay = i * 2;
			instance.getScheduler().sync().runLater(() -> {
				world.spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"), below, 8, 0.4, 0.1, 0.4, 0.05, baseBlock);
				AdventureHelper.playPositionalSound(world, below,
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.gravel.break"), Source.BLOCK, 0.7f,
								0.9f + RandomUtils.generateRandomFloat() * 0.2f));
			}, delay, below);
		}

		instance.getScheduler().sync().runLater(() -> {
			invasionMobFactory.spawnInvaderAt(loc, invasion, context, mob -> {
				if (onSpawned != null)
					onSpawned.accept(mob.getUniqueId());

				// Make mob temporarily invulnerable during spawn rise
				markMobTemporarilyInvulnerable(mob, 60L); // 3 seconds

				if (mob instanceof Piglin piglin) {
					MountSpawnResult result = invasionMobFactory.maybeMountPiglin(piglin, loc, invasion, context, wave);
					if (result != null) {
						invasion.addRetreatMountMapping(piglin.getUniqueId(), result.mount().getUniqueId());
						formationHandler.setFormationHost(piglin.getUniqueId(), result.mount().getUniqueId());
					}
				}
			});

			world.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), loc, 10, 0.3, 0.2, 0.3, 0.05);
			AdventureHelper.playPositionalSound(world, loc, Sound
					.sound(net.kyori.adventure.key.Key.key("minecraft:entity.piglin.angry"), Source.BLOCK, 1.2f, 0.7f));
		}, 40L, loc);
	}

	private void spawnStompIn(@NotNull Location spawnLoc, @NotNull CustomInvasion invasion,
			@NotNull InvasionSpawnContext context, @NotNull Consumer<UUID> onSpawned) {
		World world = spawnLoc.getWorld();
		if (world == null)
			return;

		for (int i = 0; i < 10; i++) {
			int delay = i * 2;
			instance.getScheduler().sync().runLater(() -> {
				world.spawnParticle(Particle.CLOUD, spawnLoc, 12, 0.6, 0.1, 0.6, 0.02);
				world.spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"), spawnLoc, 8, 0.4, 0.1, 0.4, 0.05,
						spawnLoc.getBlock().getBlockData());
				AdventureHelper.playPositionalSound(world, spawnLoc, Sound.sound(
						net.kyori.adventure.key.Key.key("minecraft:entity.hoglin.step"), Source.BLOCK, 1.0f, 0.9f));
			}, delay, spawnLoc);
		}

		instance.getScheduler().sync().runLater(() -> {
			invasionMobFactory.spawnInvaderAt(spawnLoc, invasion, context, mob -> {
				if (onSpawned != null)
					onSpawned.accept(mob.getUniqueId());

				markMobTemporarilyInvulnerable(mob, 60L);
			});

			world.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), spawnLoc, 12, 0.3, 0.2, 0.3, 0.05);
			AdventureHelper.playPositionalSound(world, spawnLoc, Sound
					.sound(net.kyori.adventure.key.Key.key("minecraft:entity.hoglin.angry"), Source.BLOCK, 1.4f, 0.6f));
		}, 40L, spawnLoc);
	}

	private void spawnRetreatPortal(@NotNull Location base, @Nullable List<UUID> mobIds,
			@Nullable CustomInvasion invasion, boolean faceX) {
		World world = base.getWorld();
		if (world == null || invasion == null || mobIds == null || mobIds.isEmpty())
			return;

		List<Location> placedBlocks = Collections.synchronizedList(new ArrayList<>());
		createRetreatPortal(base, faceX, placedBlocks);
		playInitialPortalEffects(world, base);

		Location portalCenter = base.clone().add(0.5, 1.0, 0.0); // center of interior
		instance.debug("Retreat portal spawned at " + portalCenter + " for island ID: " + invasion.getIslandId());

		shepherdMobsToPortal(mobIds, invasion, portalCenter, placedBlocks);
	}

	private void createRetreatPortal(@NotNull Location base, boolean faceX, @NotNull List<Location> placedBlocks) {
		int dx = faceX ? 1 : 0;
		int dz = faceX ? 0 : 1;

		// Interior (2x3)
		for (int px = 0; px <= 1; px++) {
			for (int py = 0; py <= 2; py++) {
				Location loc = base.clone().add(dx * px, py, dz * px);
				if (loc.getBlock().getType().isAir()) {
					loc.getBlock().setType(Material.NETHER_PORTAL);
					loc.getBlock().setMetadata("invasion_portal", new FixedMetadataValue(instance, true));
					placedBlocks.add(loc);
				}
			}
		}

		// Frame
		List<Location> framePositions = new ArrayList<>();
		for (int py = 0; py <= 2; py++) {
			framePositions.add(base.clone().add(-1 * dx, py, -1 * dz));
			framePositions.add(base.clone().add(2 * dx, py, 2 * dz));
		}
		framePositions.add(base.clone().add(-1 * dx, -1, -1 * dz));
		framePositions.add(base.clone().add(0 * dx, -1, 0 * dz));
		framePositions.add(base.clone().add(1 * dx, -1, 1 * dz));
		framePositions.add(base.clone().add(2 * dx, -1, 2 * dz));
		framePositions.add(base.clone().add(-1 * dx, 3, -1 * dz));
		framePositions.add(base.clone().add(0 * dx, 3, 0 * dz));
		framePositions.add(base.clone().add(1 * dx, 3, 1 * dz));
		framePositions.add(base.clone().add(2 * dx, 3, 2 * dz));

		framePositions.stream().filter(f -> f.getBlock().getType().isAir()).forEach(f -> {
			f.getBlock().setType(Material.OBSIDIAN);
			placedBlocks.add(f);
		});
	}

	private void playInitialPortalEffects(@NotNull World world, @NotNull Location base) {
		world.spawnParticle(Particle.PORTAL, base, 120, 1.2, 1.2, 1.2, 0.25);
		world.spawnParticle(Particle.FLAME, base, 40, 0.8, 1.2, 0.8, 0.1);
		AdventureHelper.playPositionalSound(world, base, Sound
				.sound(net.kyori.adventure.key.Key.key("minecraft:block.portal.ambient"), Source.BLOCK, 1.0f, 1.0f));
	}

	private void shepherdMobsToPortal(@NotNull List<UUID> mobIds, @NotNull CustomInvasion invasion,
			@NotNull Location portalCenter, @NotNull List<Location> placedBlocks) {
		Map<UUID, SchedulerTask> shepherdTasks = new ConcurrentHashMap<>();

		for (UUID mobId : new ArrayList<>(mobIds)) {
			Entity e = Bukkit.getEntity(mobId);
			if (!(e instanceof Mob mob) || !mob.isValid() || mob.isDead())
				continue;

			// Make retreating mobs invulnerable until they despawn in the portal
			markMobTemporarilyInvulnerable(mob, 200L); // 10 seconds (enough to reach portal)

			SchedulerTask prevTarget = invasion.getMobGoalTask(mobId);
			if (prevTarget != null && !prevTarget.isCancelled())
				prevTarget.cancel();

			if (mob instanceof Piglin piglin) {
				UUID retreatMount = invasion.getMountId(piglin.getUniqueId());
				if (retreatMount != null) {
					Entity mountEntity = Bukkit.getEntity(retreatMount);
					if (mountEntity instanceof Mob mount && mount.isValid() && !mount.isDead()) {
						// Shepherd the mount as well
						shepherdTasks.put(mount.getUniqueId(),
								runShepherdLogicFor(mount, portalCenter, invasion, placedBlocks));
					}
				}
			}

			SchedulerTask shepherd = instance.getScheduler().sync().runRepeating(new Runnable() {
				int stuckTicks = 0;
				Location lastLoc = mob.getLocation();

				@Override
				public void run() {
					if (!mob.isValid() || mob.isDead()) {
						cancelTask(mobId, invasion, shepherdTasks);
						return;
					}

					double dist = mob.getLocation().distance(portalCenter);
					if (dist <= 1.6) {
						handleMobArrived(mob, mobId, invasion, shepherdTasks, placedBlocks, portalCenter);
						return;
					}

					// Move toward portal
					Vector dir = portalCenter.toVector().clone().subtract(mob.getLocation().toVector()).normalize();
					mob.setVelocity(dir.clone().multiply(0.45));

					if (mob.getLocation().distance(lastLoc) < 0.02)
						stuckTicks++;
					else
						stuckTicks = 0;
					lastLoc = mob.getLocation().clone();

					if (stuckTicks > 60) {
						Location teleportTo = mob.getLocation().clone().add(dir.multiply(2.0));
						if (invasion.getBounds().contains(teleportTo.toVector())) {
							mob.teleport(teleportTo);
							stuckTicks = 0;
						}
					}
				}
			}, 0L, 1L, mob.getLocation());

			invasion.addMobTask(mobId, null, shepherd);
			shepherdTasks.put(mobId, shepherd);
		}
	}

	@NotNull
	public SchedulerTask runShepherdLogicFor(@NotNull Mob mob, @NotNull Location portalCenter,
			@NotNull CustomInvasion invasion, @NotNull List<Location> placedBlocks) {

		UUID mobId = mob.getUniqueId();

		return instance.getScheduler().sync().runRepeating(new Runnable() {
			int stuckTicks = 0;
			Location lastLoc = mob.getLocation();

			@Override
			public void run() {
				// --- Handle invalid mobs ---
				if (!mob.isValid() || mob.isDead()) {
					invasion.cancelMobGoalTask(mobId);

					if (invasionMobFactory.isMountEntity(mob)) {
						UUID riderId = invasion.getRiderForMount(mobId);
						if (riderId != null) {
							invasion.removeRetreatMountMapping(riderId);
							formationHandler.clearFormationHost(riderId);
						}
					} else {
						// regular invasion mob
						invasion.removeWaveMob(mobId);
						invasion.removeRetreatMountMapping(mobId);
					}

					return;
				}

				// --- Mount check: ensure Piglin rider still exists ---
				if (invasionMobFactory.isMountEntity(mob)) {
					UUID riderId = invasion.getRiderForMount(mobId);
					Entity rider = riderId != null ? Bukkit.getEntity(riderId) : null;

					if (!(rider instanceof Piglin piglin) || piglin.isDead() || !piglin.isValid()) {
						// Rider is gone — remove mapping and despawn this mount with effects
						invasion.cancelMobGoalTask(mobId);
						invasion.removeRiderForMount(mobId);
						formationHandler.clearFormationHost(riderId);
						despawnMountWithEffects(mob);
						return;
					}
				}

				// --- Movement logic ---
				double dist = mob.getLocation().distance(portalCenter);
				if (dist <= 1.6) {
					handleMobArrived(mob, mobId, invasion, null, placedBlocks, portalCenter);
					return;
				}

				Vector dir = portalCenter.toVector().clone().subtract(mob.getLocation().toVector()).normalize();
				mob.setVelocity(dir.clone().multiply(0.45));

				if (mob.getLocation().distance(lastLoc) < 0.02)
					stuckTicks++;
				else
					stuckTicks = 0;

				lastLoc = mob.getLocation().clone();

				if (stuckTicks > 60) {
					Location teleportTo = mob.getLocation().clone().add(dir.multiply(2.0));
					if (invasion.getBounds().contains(teleportTo.toVector())) {
						mob.teleport(teleportTo);
						stuckTicks = 0;
					}
				}
			}
		}, 0L, 1L, mob.getLocation());
	}

	private void handleMobArrived(@NotNull Mob mob, @NotNull UUID mobId, @NotNull CustomInvasion invasion,
			@Nullable Map<UUID, SchedulerTask> shepherdTasks, @NotNull List<Location> placedBlocks,
			@NotNull Location portalCenter) {
		World world = mob.getWorld();
		world.spawnParticle(Particle.PORTAL, mob.getLocation(), 18, 0.3, 0.6, 0.3, 0.05);
		world.strikeLightningEffect(mob.getLocation());
		AdventureHelper.playPositionalSound(world, mob.getLocation(),
				Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.zombie_villager.converted"), Source.BLOCK,
						1.1f, 0.9f));

		try {
			mob.remove();
		} catch (Throwable ignored) {
			world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), mob.getLocation(), 6, 0.2, 0.3, 0.2, 0.02);
			mob.remove();
		}

		if (shepherdTasks != null && !shepherdTasks.isEmpty()) {
			cancelTask(mobId, invasion, shepherdTasks);
		}
		invasion.getMobIds().remove(mobId);

		if (invasion.getMobIds().isEmpty()) {
			instance.getScheduler().sync().runLater(() -> {
				cleanupPortalBlocks(placedBlocks, portalCenter.getWorld());
				recordVictoryAndEndInvasion(invasion, portalCenter);
			}, 120, portalCenter);
		}
	}

	private void cancelTask(@NotNull UUID mobId, @NotNull CustomInvasion invasion,
			@NotNull Map<UUID, SchedulerTask> taskMap) {
		SchedulerTask removed = invasion.getMobGoalTask(mobId);
		if (removed != null && !removed.isCancelled())
			removed.cancel();
		taskMap.remove(mobId);
	}

	private void cleanupPortalBlocks(@NotNull List<Location> placedBlocks, @NotNull World world) {
		world.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), placedBlocks.get(0), 6);
		AdventureHelper.playPositionalSound(world, placedBlocks.get(0), Sound
				.sound(net.kyori.adventure.key.Key.key("minecraft:block.portal.travel"), Source.BLOCK, 1.2f, 0.6f));

		placedBlocks.forEach(loc -> {
			loc.getBlock().removeMetadata("invasion_portal", instance);
			loc.getBlock().setType(Material.AIR);
		});
	}

	private void recordVictoryAndEndInvasion(@NotNull CustomInvasion invasion, @NotNull Location portalCenter) {
		UUID ownerId = invasion.getOwnerUUID();
		instance.getStorageManager().getCachedUserData(ownerId).ifPresent(ownerData -> {
			HellblockData hellblockData = ownerData.getHellblockData();
			hellblockData.getInvasionData().recordVictory(true);
			instance.debug("Invasion victory recorded for hellblock island ID: " + invasion.getIslandId());

			EventUtils.fireAndForget(new InvasionVictoryEvent(ownerId, invasion.getProfile()));
		});

		cleanupRemainingMounts(invasion);
		endInvasion(invasion.getIslandId());
	}

	public void cleanupRemainingMounts(@NotNull CustomInvasion invasion) {
		if (invasion.getMountMappings().isEmpty())
			return;
		invasion.getMountMappings().values().stream().map(Bukkit::getEntity)
				.filter(mount -> mount instanceof Mob mob && mob.isValid() && !mob.isDead())
				.forEach(this::despawnMountWithEffects);

		invasion.getMountMappings().clear();
	}

	public void despawnMountWithEffects(@NotNull Entity mount) {
		Location loc = mount.getLocation();
		World world = loc.getWorld();

		if (world == null)
			return;

		// Visuals
		world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), loc, 12, 0.3, 0.5, 0.3, 0.02);
		world.spawnParticle(Particle.LAVA, loc, 8, 0.2, 0.4, 0.2, 0.01);

		// Sound
		AdventureHelper.playPositionalSound(world, loc, Sound
				.sound(net.kyori.adventure.key.Key.key("minecraft:entity.strider.death"), Source.BLOCK, 1.1f, 0.8f));

		// Finally remove
		mount.remove();
	}

	@Nullable
	private Location findValidPortalSpawn(@Nullable BoundingBox bounds, @Nullable World world) {
		if (bounds == null || world == null)
			return null;

		final int attempts = 80;
		int minX = (int) Math.ceil(bounds.getMinX()) + 2;
		int maxX = (int) Math.floor(bounds.getMaxX()) - 2;
		int minZ = (int) Math.ceil(bounds.getMinZ()) + 2;
		int maxZ = (int) Math.floor(bounds.getMaxZ()) - 2;
		int minY = (int) Math.max(1, Math.ceil(bounds.getMinY()) + 1);
		int maxY = (int) Math.floor(bounds.getMaxY()) - 4; // Needs vertical clearance for portal

		if (maxX <= minX || maxZ <= minZ || maxY <= minY) {
			instance.debug("Invalid bounds: Portal spawn region too small");
			return null;
		}

		for (int i = 0; i < attempts; i++) {
			int x = RandomUtils.generateRandomInt(minX, maxX);
			int z = RandomUtils.generateRandomInt(minZ, maxZ);
			int y = RandomUtils.generateRandomInt(minY, maxY);

			Location base = new Location(world, x + 0.5, y, z + 0.5);

			if (isValidPortalLocation(base, bounds)) {
				instance.debug("Found valid retreat portal base at attempt " + (i + 1) + " | " + base);
				return base;
			}
		}

		instance.debug("Failed to find valid retreat portal location after " + attempts + " attempts");
		return null;
	}

	private boolean isValidPortalLocation(@NotNull Location base, @NotNull BoundingBox bounds) {
		List<Location> required = new ArrayList<>();

		// Interior blocks (2x3 portal space)
		for (int px = 0; px <= 1; px++) {
			for (int py = 0; py <= 2; py++) {
				required.add(base.clone().add(px, py, 0));
			}
		}

		// Frame sides
		for (int py = 0; py <= 2; py++) {
			required.add(base.clone().add(-1, py, 0));
			required.add(base.clone().add(2, py, 0));
		}

		// Bottom & top frame
		required.add(base.clone().add(-1, -1, 0));
		required.add(base.clone().add(0, -1, 0));
		required.add(base.clone().add(1, -1, 0));
		required.add(base.clone().add(2, -1, 0));
		required.add(base.clone().add(-1, 3, 0));
		required.add(base.clone().add(0, 3, 0));
		required.add(base.clone().add(1, 3, 0));
		required.add(base.clone().add(2, 3, 0));

		for (Location loc : required) {
			if (!bounds.contains(loc.toVector()))
				return false;

			if (!loc.getBlock().getType().isAir())
				return false;
		}

		return true;
	}

	private void playBeam(@NotNull Location center, @NotNull Particle particle, int height, int density,
			double radius) {
		World world = center.getWorld();
		if (world == null)
			return;

		for (int y = 0; y < height; y++) {
			Location ringCenter = center.clone().add(0, y, 0);
			for (int i = 0; i < density; i++) {
				double angle = 2 * Math.PI * i / density;
				double x = radius * Math.cos(angle);
				double z = radius * Math.sin(angle);
				Location loc = ringCenter.clone().add(x, 0, z);
				world.spawnParticle(particle, loc, 0, new Particle.DustTransition(Color.RED, Color.ORANGE, 1.5f));
			}
			AdventureHelper.playPositionalSound(world, center, Sound.sound(
					net.kyori.adventure.key.Key.key("minecraft:block.beacon.power_select"), Source.BLOCK, 0.6f, 1.0f));
		}
	}

	@Nullable
	private Location findValidSpawnLocation(@NotNull World world, @NotNull BoundingBox box, @NotNull Location base) {
		int attempts = 12;
		while (attempts-- > 0) {
			int x = RandomUtils.generateRandomInt((int) box.getMinX() + 5, (int) box.getMaxX() - 5);
			int z = RandomUtils.generateRandomInt((int) box.getMinZ() + 5, (int) box.getMaxZ() - 5);
			int y = (int) box.getMaxY();

			Location loc = new Location(world, x + 0.5, y, z + 0.5);
			for (int dy = 0; dy < 20; dy++) {
				Location check = loc.clone().subtract(0, dy, 0);
				if (isAirPocket(check))
					return check;
			}
		}
		return null;
	}

	private boolean isAirPocket(@NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return false;

		// Check 3 blocks of air for the mob to fit
		for (int y = 0; y < 3; y++) {
			if (!world.getBlockAt(loc.getBlockX(), loc.getBlockY() + y, loc.getBlockZ()).getType().isAir())
				return false;
		}

		// Ensure block *below* is solid and walkable
		Block below = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
		Set<Material> invalid = Set.of(Material.LAVA, Material.CACTUS, Material.MAGMA_BLOCK);
		if (invalid.contains(below.getType()))
			return false;
		return below.getType().isSolid() && below.getType().isOccluding(); // e.g., no glass or fences
	}

	private void markMobTemporarilyInvulnerable(@NotNull Mob mob, long durationTicks) {
		mob.setMetadata("invulnerable_phase", new FixedMetadataValue(instance, true));
		long removeDelay = Math.max(20L, durationTicks); // at least 1s

		instance.getScheduler().sync().runLater(() -> {
			if (mob.isValid() && !mob.isDead()) {
				mob.removeMetadata("invulnerable_phase", instance);
			}
		}, removeDelay, mob.getLocation());
	}

	private void sendInvasionSummary(@NotNull Player player, @NotNull InvasionData data) {
		String successRate = String.format("%.1f%%", data.getSuccessRate());
		String bossKills = String.valueOf(data.getBossKills());
		String successful = String.valueOf(data.getSuccessfulInvasions());
		String streak = String.valueOf(data.getCurrentStreak());

		Component summary = instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_INVASION_SUMMARY
						.arguments(AdventureHelper.miniMessageToComponent(successRate),
								AdventureHelper.miniMessageToComponent(bossKills),
								AdventureHelper.miniMessageToComponent(successful),
								AdventureHelper.miniMessageToComponent(streak))
						.build());

		instance.getSenderFactory().wrap(player).sendMessage(summary);
	}

	public void tagAsInvasionMob(@NotNull Mob mob) {
		mob.getPersistentDataContainer().set(invasionMobKey, PersistentDataType.STRING, INVASION_MOB_KEY);
	}

	public boolean isInvasionMob(@NotNull Mob mob) {
		return mob.getPersistentDataContainer().has(invasionMobKey, PersistentDataType.STRING) && INVASION_MOB_KEY
				.equals(mob.getPersistentDataContainer().get(invasionMobKey, PersistentDataType.STRING));
	}

	public boolean isBossMob(@NotNull PiglinBrute brute) {
		return brute.getPersistentDataContainer().has(invasionBossKey, PersistentDataType.STRING) && INVASION_BOSS_KEY
				.equals(brute.getPersistentDataContainer().get(invasionBossKey, PersistentDataType.STRING));
	}

	public enum InvasionSpawnStyle {
		PORTAL, DIG_UP;

		private static final InvasionSpawnStyle[] VALUES = values();

		public static InvasionSpawnStyle random() {
			return VALUES[RandomUtils.generateRandomInt(VALUES.length)];
		}
	}

	private static class LootEntry {
		final Material material;
		final int min;
		final int max;
		final double chance; // 1.0 = 100%

		LootEntry(@NotNull Material material, int min, int max) {
			this(material, min, max, 1.0); // default: always include
		}

		LootEntry(@NotNull Material material, int min, int max, double chance) {
			this.material = material;
			this.min = min;
			this.max = max;
			this.chance = chance;
		}
	}

	private static class SpiralEffectRunnable implements Runnable {
		private final Entity entity;
		private final SchedulerTask[] task;
		private double angle = 0;
		private int ticks = 0;

		public SpiralEffectRunnable(@NotNull Entity entity, @NotNull SchedulerTask[] task) {
			this.entity = entity;
			this.task = task;
		}

		@Override
		public void run() {
			if (!entity.isValid() || entity.isDead() || ticks++ > 200) {
				if (task[0] != null && !task[0].isCancelled())
					task[0].cancel();
				return;
			}

			Location loc = entity.getLocation().clone().add(0, 1.2, 0);
			for (int i = 0; i < 2; i++) {
				double currentAngle = angle + (Math.PI * i);
				double x = 0.6 * Math.cos(currentAngle);
				double z = 0.6 * Math.sin(currentAngle);
				loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, loc.clone().add(x, angle * 0.1, z), 0,
						new Particle.DustTransition(Color.ORANGE, Color.RED, 1.2f));
			}
			angle += Math.PI / 12;
		}
	}
}