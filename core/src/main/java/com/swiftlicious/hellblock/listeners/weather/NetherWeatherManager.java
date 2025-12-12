package com.swiftlicious.hellblock.listeners.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.context.ContextKeys;
import com.swiftlicious.hellblock.listeners.weather.events.AshStormWeather;
import com.swiftlicious.hellblock.listeners.weather.events.EmberFogWeather;
import com.swiftlicious.hellblock.listeners.weather.events.LavaRainWeather;
import com.swiftlicious.hellblock.listeners.weather.events.MagmaWindWeather;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;

import net.kyori.adventure.text.TranslatableComponent;

public class NetherWeatherManager implements Reloadable {

	private static final Set<Material> TRANSPARENT_MATERIALS = createTransparentMaterialSet();

	@NotNull
	private static Set<Material> createTransparentMaterialSet() {
		Set<Material> set = EnumSet.noneOf(Material.class);
		addIfExists(set, "AIR");
		addIfExists(set, "LAVA");
		addIfExists(set, "WATER");
		addIfExists(set, "COBWEB");
		addIfExists(set, "STRING");
		addIfExists(set, "FLOWER_POT");
		addIfExists(set, "BAMBOO");
		addIfExists(set, "NETHER_PORTAL");
		addIfExists(set, "END_PORTAL");
		addIfExists(set, "END_GATEWAY");
		addIfExists(set, "LADDER");
		addIfExists(set, "CHAIN");
		addIfExists(set, "IRON_CHAIN");
		addIfExists(set, "CANDLE");
		addIfExists(set, "SEA_PICKLE");
		addIfExists(set, "VINE");
		addIfExists(set, "TWISTING_VINES");
		addIfExists(set, "WEEPING_VINES");
		addIfExists(set, "END_ROD");
		addIfExists(set, "LIGHTNING_ROD");
		addIfExists(set, "LEVER");
		addIfExists(set, "SWEET_BERRY_BUSH");
		addIfExists(set, "SCAFFOLDING");
		addIfExists(set, "LANTERN");
		addIfExists(set, "SOUL_LANTERN");
		addIfExists(set, "TURTLE_EGG");
		addIfExists(set, "SMALL_DRIPLEAF");
		addIfExists(set, "BIG_DRIPLEAF");
		addIfExists(set, "IRON_BARS");
		addIfExists(set, "POWDER_SNOW");
		addIfExists(set, "TRIPWIRE");
		addIfExists(set, "TRIPWIRE_HOOK");
		return Collections.unmodifiableSet(set);
	}

	private static void addIfExists(@NotNull Set<Material> set, @NotNull String name) {
		try {
			Material mat = Material.matchMaterial(name);
			if (mat != null)
				set.add(mat);
		} catch (NoSuchFieldError | IllegalArgumentException ignored) {
		}
	}

	private final HellblockPlugin instance;
	private final Map<Integer, AbstractNetherWeatherTask> activeWeather = new ConcurrentHashMap<>();

	public NetherWeatherManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		startWeatherForAllIslands();
	}

	@Override
	public void unload() {
		stopAllWeather();
	}

	@Override
	public void disable() {
		unload();
	}

	public synchronized void registerWeather(int islandId, @NotNull AbstractNetherWeatherTask task) {
		AbstractNetherWeatherTask existing = activeWeather.get(islandId);
		if (existing != null && existing.isActive()) {
			// Don’t register new weather if one is already active
			instance.getPluginLogger().warn("Tried to start " + task.getType() + " on island " + islandId + " but "
					+ existing.getType() + " is still active.");
			return;
		}

		activeWeather.put(islandId, task);
	}

	public void stopWeather(int islandId) {
		AbstractNetherWeatherTask task = activeWeather.remove(islandId);
		if (task != null)
			task.stop();
	}

	public void stopAllWeather() {
		activeWeather.keySet().forEach(this::clearAllWeatherContext);
		activeWeather.values().forEach(AbstractNetherWeatherTask::cancel);
		activeWeather.clear();
	}

	/**
	 * Checks if weather is active on the given island.
	 *
	 * @param islandId the island ID
	 * @return true if weather is currently active
	 */
	public boolean isWeatherActive(int islandId) {
		AbstractNetherWeatherTask task = activeWeather.get(islandId);
		return task != null && task.isActive();
	}

	/**
	 * Checks if a specific weather type is active on the given island.
	 *
	 * @param islandId the island ID
	 * @param type     the weather type to check
	 * @return true if that specific weather type is currently active
	 */
	public boolean isWeatherActive(int islandId, @NotNull WeatherType type) {
		AbstractNetherWeatherTask task = activeWeather.get(islandId);
		return task != null && task.isActive() && task.getType() == type;
	}

	/**
	 * Returns the currently active weather type for the specified island, if any.
	 *
	 * @param islandId the island ID
	 * @return the current WeatherType, or null if none is active
	 */
	@Nullable
	public WeatherType getActiveWeatherType(int islandId) {
		AbstractNetherWeatherTask task = activeWeather.get(islandId);
		return (task != null && task.isActive()) ? task.getType() : null;
	}

	public Map<Integer, AbstractNetherWeatherTask> getActiveWeather() {
		return activeWeather;
	}

	/**
	 * @return all islands currently experiencing the given weather type.
	 */
	public Set<Integer> getIslandsWithWeather(@NotNull WeatherType type) {
		return activeWeather.entrySet().stream().filter(e -> e.getValue().isActive() && e.getValue().getType() == type)
				.map(Map.Entry::getKey).collect(Collectors.toSet());
	}

	private void startWeatherForAllIslands() {
		if (!instance.getConfigManager().weatherEnabled())
			return;

		instance.getIslandManager().getAllIslandIds()
				.thenAccept(ids -> ids.forEach(this::scheduleRandomWeatherForIsland));
	}

	public void scheduleRandomWeatherForIsland(int islandId) {
		if (!instance.getConfigManager().weatherEnabled())
			return;

		// Prevent starting if one is already running
		if (isWeatherActive(islandId))
			return;

		instance.getIslandManager().getWorldForIsland(islandId).thenAccept(optWorld -> {
			if (optWorld.isEmpty())
				return;

			HellblockWorld<?> world = optWorld.get();
			Set<WeatherType> supported = instance.getConfigManager().supportedWeatherTypes();

			if (supported.isEmpty())
				return;

			// Pick a random supported weather type
			int index = RandomUtils.generateRandomInt(0, supported.size() - 1);
			WeatherType chosen = supported.stream().skip(index).findFirst().orElse(null);

			if (chosen == null)
				return;

			AbstractNetherWeatherTask task = createWeatherTask(chosen, islandId, world);

			if (task != null && task.canRun(world)) {
				// Randomize start delay (10-15 min)
				long delayMinutes = RandomUtils.generateRandomLong(10, 16);
				instance.getScheduler().asyncLater(() -> registerWeather(islandId, task), delayMinutes,
						TimeUnit.MINUTES);
			}
		});
	}

	@Nullable
	private AbstractNetherWeatherTask createWeatherTask(@NotNull WeatherType type, int islandId,
			@NotNull HellblockWorld<?> world) {
		switch (type) {
		case LAVA_RAIN:
			return new LavaRainWeather(instance, islandId, world);
		case ASH_STORM:
			return new AshStormWeather(instance, islandId, world);
		case EMBER_FOG:
			return new EmberFogWeather(instance, islandId, world);
		case MAGMA_WIND:
			return new MagmaWindWeather(instance, islandId, world);
		default:
			return null;
		}
	}

	/**
	 * Asynchronously checks whether is active at a given location.
	 *
	 * @param location the location to check
	 * @return a CompletableFuture resolving to true if weather is active at that
	 *         location
	 */
	public CompletableFuture<Boolean> isWeatherActiveAt(@NotNull Location location) {
		if (location.getWorld() == null)
			return CompletableFuture.completedFuture(false);

		return instance.getIslandManager().resolveIslandId(location)
				.thenApply(optId -> optId.map(this::isWeatherActive).orElse(false));
	}

	/**
	 * Asynchronously checks whether a specific weather type is active at a given
	 * location.
	 *
	 * @param location the location to check
	 * @param type     the weather type
	 * @return a CompletableFuture resolving to true if that weather type is active
	 *         at that location
	 */
	public CompletableFuture<Boolean> isWeatherActiveAt(@NotNull Location location, @NotNull WeatherType type) {
		if (location.getWorld() == null)
			return CompletableFuture.completedFuture(false);

		return instance.getIslandManager().resolveIslandId(location)
				.thenApply(optId -> optId.map(id -> isWeatherActive(id, type)).orElse(false));
	}

	public void onWeatherEnd(int islandId, WeatherType endedType) {
		activeWeather.remove(islandId);

		long cooldownMinutes = RandomUtils.generateRandomInt(10, 25);
		long cooldownTicks = TimeUnit.MINUTES.toSeconds(cooldownMinutes) * 20;

		// Warn 5 seconds (100 ticks) before the next weather starts
		long warnDelayTicks = Math.max(cooldownTicks - 100, 0);

		instance.getScheduler().sync().runLater(() -> {
			WeatherType next = pickRandomWeatherType(endedType);

			// Send pre-weather warning message if enabled
			if (willSendWarning()) {
				instance.getIslandManager().getPlayersOnIsland(islandId).stream().map(Bukkit::getPlayer)
						.filter(Objects::nonNull).forEach(player -> {
							TranslatableComponent message = MessageConstants.forWeatherWarning(next);
							instance.getSenderFactory().wrap(player)
									.sendMessage(instance.getTranslationManager().render(message));
						});
			}
		}, warnDelayTicks, LocationUtils.getAnyLocationInstance());

		// Actually start the next weather after the cooldown
		instance.getScheduler().asyncLater(() -> {
			WeatherType next = pickRandomWeatherType(endedType);

			instance.getIslandManager().getWorldForIsland(islandId).thenAccept(optWorld -> {
				if (optWorld.isEmpty())
					return;

				HellblockWorld<?> world = optWorld.get();

				// Clear previous context flags
				synchronized (Context.island(islandId)) {
				    clearAllWeatherContext(islandId);
				}

				AbstractNetherWeatherTask nextTask = createWeatherTask(next, islandId, world);
				if (nextTask != null) {
					registerWeather(islandId, nextTask);
					nextTask.start();
				}
			});
		}, cooldownMinutes, TimeUnit.MINUTES);
	}

	@NotNull
	private WeatherType pickRandomWeatherType(@NotNull WeatherType exclude) {
		List<WeatherType> types = new ArrayList<>(instance.getConfigManager().supportedWeatherTypes());
		if (RandomUtils.generateRandomBoolean())
			types.remove(exclude);
		return types.isEmpty() ? exclude : types.get(RandomUtils.generateRandomInt(types.size() - 1));
	}

	private void clearAllWeatherContext(int islandId) {
		Context<Integer> ctx = Context.island(islandId);
		ctx.arg(ContextKeys.LAVA_RAIN, false).arg(ContextKeys.ASH_STORM, false).arg(ContextKeys.EMBER_FOG, false)
				.arg(ContextKeys.MAGMA_WIND, false);
	}

	public boolean canHurtLivingCreatures() {
		return instance.getConfigManager().canHurtCreatures();
	}

	public boolean willSendWarning() {
		return instance.getConfigManager().willWarnPlayers();
	}

	public boolean willTNTExplode() {
		return instance.getConfigManager().canExplodeTNT();
	}

	@Nullable
	public Block getHighestBlock(@Nullable Location location) {
		if (location == null || location.getWorld() == null)
			return null;

		Block highestBlock = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(),
				location.getBlockZ());
		for (int y = 0; y < 10; y++) {
			final Block block = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() + 2 + y,
					location.getBlockZ());
			if (isTransparent(block))
				continue;
			highestBlock = block;
			break;
		}
		return highestBlock;
	}

	private boolean isTransparent(@NotNull Block block) {
		Material type = block.getType();
		String name = type.name();
		return TRANSPARENT_MATERIALS.contains(type) || name.endsWith("_SIGN") || name.endsWith("_GLASS_PANE")
				|| name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE") || name.endsWith("_DOOR")
				|| name.endsWith("_BUTTON") || name.endsWith("_PRESSURE_PLATE");
	}

	public class NetherWeatherRegion {
		private final World world;
		private final BoundingBox box;

		public NetherWeatherRegion(@NotNull Location min, @NotNull Location max) {
			if (!Objects.equals(min.getWorld(), max.getWorld()))
				throw new IllegalArgumentException("NetherWeatherRegion requires both locations in the same world!");
			this.world = Objects.requireNonNull(min.getWorld());
			this.box = BoundingBox.of(min, max);
		}

		@NotNull
		public Iterator<Block> getBlocks() {
			List<Block> list = new ArrayList<>((int) box.getVolume());
			for (int x = (int) box.getMinX(); x <= box.getMaxX(); x++)
				for (int y = (int) box.getMinY(); y <= box.getMaxY(); y++)
					for (int z = (int) box.getMinZ(); z <= box.getMaxZ(); z++)
						list.add(world.getBlockAt(x, y, z));
			return list.iterator();
		}

		@NotNull
		public Location getCenter() {
			return box.getCenter().toLocation(world);
		}

		public double getDistance() {
			return getMinLocation().distance(getMaxLocation());
		}

		public double getDistanceSquared() {
			return getMinLocation().distanceSquared(getMaxLocation());
		}

		@NotNull
		public Location getMinLocation() {
			return new Location(world, box.getMinX(), box.getMinY(), box.getMinZ());
		}

		@NotNull
		public Location getMaxLocation() {
			return new Location(world, box.getMaxX(), box.getMaxY(), box.getMaxZ());
		}

		@NotNull
		public Location getRandomLocation() {
			double x = RandomUtils.generateRandomDouble(box.getMinX(), box.getMaxX());
			double y = RandomUtils.generateRandomDouble(box.getMinY(), box.getMaxY());
			double z = RandomUtils.generateRandomDouble(box.getMinZ(), box.getMaxZ());
			return new Location(world, x, y, z);
		}

		public boolean contains(@NotNull Location location) {
			return location.getWorld() != null && location.getWorld().getUID().equals(this.world.getUID())
					&& box.contains(location.toVector());
		}

		public boolean contains(@NotNull Player player) {
			return contains(player.getLocation());
		}
	}
}