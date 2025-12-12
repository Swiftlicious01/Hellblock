package com.swiftlicious.hellblock.world.adapter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import com.infernalsuite.asp.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.TagUtils;
import com.swiftlicious.hellblock.utils.extras.Either;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlock;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomBlockStateInterface;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.CustomRegion;
import com.swiftlicious.hellblock.world.CustomSection;
import com.swiftlicious.hellblock.world.CustomSectionInterface;
import com.swiftlicious.hellblock.world.CustomWorldInterface;
import com.swiftlicious.hellblock.world.DelayedTickTask;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.HellblockWorldException;
import com.swiftlicious.hellblock.world.RegionPos;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.WorldExtraData;
import com.swiftlicious.hellblock.world.WorldManager;
import com.swiftlicious.hellblock.world.WorldSetting;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class SlimeWorldAdapter extends AbstractWorldAdapter<SlimeWorld> implements Listener {

	private final HellblockPlugin instance;

	private final GetSlimeWorldFunction getSlimeWorldFunction;
	private final GetSlimeLoaderFunction getSlimeLoaderFunction;
	private final DeleteSlimeWorldFunction deleteSlimeWorldFunction;
	private final WorldExistsFunction worldExistsFunction;
	private final GenerateSlimeWorldFunction generateSlimeWorldFunction;
	private final CreateSlimeWorldFunction createSlimeWorldFunction;

	private final Method getWorldMethod;
	private final Method getLoaderMethod;
	private final Method createEmptyWorldMethod;
	private final Method generateWorldMethod;
	private final Method worldExistsMethod;
	private final Method deleteWorldMethod;

	private SlimeLoader cachedLoader;

	private static final String TAG_HELLBLOCK_DATA = "hellblock";
	private static final String TAG_WORLD_INFO = "world-info";
	private static final String TAG_WORLD_VERSION = "hellblock-version";

	private static final String[] LOADERS = new String[] { "api-loader", "mysql-loader", "mongo-loader", "file-loader",
			"redis-loader" };

	public SlimeWorldAdapter(HellblockPlugin plugin, int version) {
		instance = plugin;
		try {
			if (version == 1) {
				final Plugin slimePlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
				if (slimePlugin == null) {
					throw new IllegalStateException("SlimeWorldManager plugin not found");
				}
				final Class<?> slimeClass = Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
				this.getWorldMethod = slimeClass.getMethod("getWorld", String.class);
				this.getLoaderMethod = slimeClass.getMethod("getLoader", String.class);
				this.createEmptyWorldMethod = slimeClass.getMethod("createEmptyWorld", SlimeLoader.class, String.class,
						Boolean.class, SlimePropertyMap.class);
				this.generateWorldMethod = slimeClass.getMethod("generateWorld", SlimeWorld.class);
				final Class<?> loaderClass = Class.forName("com.infernalsuite.aswm.api.loaders.SlimeLoader");
				this.worldExistsMethod = loaderClass.getMethod("worldExists", String.class);
				this.deleteWorldMethod = loaderClass.getMethod("deleteWorld", String.class);
				this.getSlimeWorldFunction = (worldName) -> {
					try {
						return (SlimeWorld) this.getWorldMethod.invoke(slimePlugin, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) this.getLoaderMethod.invoke(slimePlugin, data);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.createSlimeWorldFunction = (worldName, readOnly, properties, loader) -> {
					try {
						return (SlimeWorld) this.createEmptyWorldMethod.invoke(slimePlugin, loader, worldName, readOnly,
								properties);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofFallback((Void) this.generateWorldMethod.invoke(slimePlugin, slime));
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.worldExistsFunction = (worldName) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return false;
						return (Boolean) this.worldExistsMethod.invoke(loader, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.deleteSlimeWorldFunction = (worldName) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return;
						this.deleteWorldMethod.invoke(loader, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
			} else if (version == 2 && VersionHelper.isPaperFork()) {
				final Class<?> apiClass = Class.forName("com.infernalsuite.aswm.api.AdvancedSlimePaperAPI");
				final Object apiInstance = apiClass.getMethod("instance").invoke(null);
				this.getWorldMethod = apiClass.getMethod("getLoadedWorld", String.class);
				final Class<?> loaderManagerClass = Class.forName("com.infernalsuite.aswm.plugin.loader.LoaderManager");
				this.getLoaderMethod = loaderManagerClass.getMethod("getLoader", String.class);
				this.createEmptyWorldMethod = apiClass.getMethod("createEmptyWorld", String.class, Boolean.class,
						SlimePropertyMap.class, SlimeLoader.class);
				this.generateWorldMethod = apiClass.getMethod("loadWorld", SlimeWorld.class, Boolean.class);
				final Class<?> loaderClass = Class.forName("com.infernalsuite.aswm.api.loaders.SlimeLoader");
				this.worldExistsMethod = loaderClass.getMethod("worldExists", String.class);
				this.deleteWorldMethod = loaderClass.getMethod("deleteWorld", String.class);
				this.getSlimeWorldFunction = (worldName) -> {
					try {
						return (SlimeWorld) this.getWorldMethod.invoke(apiInstance, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) this.getLoaderMethod.invoke(apiInstance, data);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.createSlimeWorldFunction = (worldName, readOnly, properties, loader) -> {
					try {
						return (SlimeWorld) this.createEmptyWorldMethod.invoke(apiInstance, worldName, readOnly,
								properties, loader);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofPrimary((SlimeWorld) this.generateWorldMethod.invoke(apiInstance, slime, true));
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
				this.worldExistsFunction = (worldName) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return false;
						return (Boolean) this.worldExistsMethod.invoke(loader, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};

				this.deleteSlimeWorldFunction = (worldName) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return;
						this.deleteWorldMethod.invoke(loader, worldName);
					} catch (ReflectiveOperationException e) {
						throw new HellblockWorldException(e);
					}
				};
			} else {
				throw new IllegalArgumentException("Unsupported version: " + version);
			}
		} catch (ReflectiveOperationException e) {
			throw new HellblockWorldException(e);
		}
	}

	@EventHandler
	public void onWorldLoad(LoadSlimeWorldEvent event) {
		final String worldName = event.getSlimeWorld().getName();
		instance.getWorldManager().markWorldAccess(worldName);
		final World world = Bukkit.getWorld(worldName);

		if (world == null) {
			instance.getPluginLogger().warn(
					"LoadSlimeWorldEvent triggered, but Bukkit world '%s' is not yet loaded.".formatted(worldName));
			return;
		}

		if (!instance.getWorldManager().isMechanicEnabled(world)) {
			return;
		}

		instance.getWorldManager().loadWorld(world);
	}

	@Override
	public HellblockWorld<SlimeWorld> adapt(Object world) {
		if (!(world instanceof SlimeWorld slimeWorld)) {
			throw new HellblockWorldException("Expected Slime World, but got: " + world.getClass().getName());
		}
		HellblockWorld<SlimeWorld> adaptedWorld = CustomWorldInterface.create(slimeWorld, this);

		// Ensure setting is applied immediately
		WorldSetting setting = Optional.ofNullable(instance.getWorldManager().getWorldSetting(slimeWorld.getName()))
				.orElse(instance.getWorldManager().getDefaultWorldSetting());

		adaptedWorld.setting(setting);
		return adaptedWorld;
	}

	@Override
	public Optional<SlimeWorld> getWorld(String worldName) {
		SlimeWorld world = this.getSlimeWorldFunction.getWorld(worldName);
		return Optional.ofNullable(world);
	}

	@Override
	@Nullable
	public HellblockWorld<SlimeWorld> getLoadedHellblockWorld(String worldName) {
		Optional<SlimeWorld> slimeOpt = getWorld(worldName);

		if (slimeOpt.isPresent()) {
			instance.getWorldManager().markWorldAccess(worldName);
			return adapt(slimeOpt.get());
		}

		return null;
	}

	private SlimeLoader getSlimeLoader() {
		if (cachedLoader != null)
			return cachedLoader;

		for (String loaderName : LOADERS) {
			try {
				final SlimeLoader worldLoader = this.getSlimeLoaderFunction.getLoader(loaderName);
				if (worldLoader != null) {
					cachedLoader = worldLoader;
					instance.debug("Using SlimeLoader: " + loaderName);
					break;
				}
			} catch (HellblockWorldException e) {
				instance.getPluginLogger().warn("Failed to load SlimeLoader: " + loaderName + " - " + e.getMessage());
			}
		}

		if (cachedLoader == null) {
			instance.getPluginLogger().severe("No working SlimeLoader could be found. SlimeWorlds cannot be created.");
		}

		return cachedLoader;
	}

	@Override
	public CompletableFuture<HellblockWorld<SlimeWorld>> createWorld(String worldName) {
		CompletableFuture<HellblockWorld<SlimeWorld>> resultFuture = new CompletableFuture<>();
		instance.getWorldManager().markWorldAccess(worldName);

		CompletableFuture.runAsync(() -> {
			SlimeWorld slimeWorld = getWorld(worldName).orElse(null);
			boolean exists = this.worldExistsFunction.exists(worldName);

			// === Build world properties ===
			SlimePropertyMap properties = new SlimePropertyMap();
			properties.setValue(SlimeProperties.SPAWN_X, 0);
			properties.setValue(SlimeProperties.SPAWN_Y, instance.getConfigManager().height());
			properties.setValue(SlimeProperties.SPAWN_Z, 0);
			properties.setValue(SlimeProperties.DEFAULT_BIOME,
					HellBiome.NETHER_WASTES.getConvertedBiome().getKey().getKey().toLowerCase(Locale.ROOT));
			properties.setValue(SlimeProperties.ALLOW_ANIMALS, HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
			properties.setValue(SlimeProperties.ALLOW_MONSTERS, HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
			properties.setValue(SlimeProperties.PVP, HellblockFlag.FlagType.PVP.getDefaultValue());
			properties.setValue(SlimeProperties.DIFFICULTY, Difficulty.NORMAL.toString().toLowerCase(Locale.ROOT));
			properties.setValue(SlimeProperties.ENVIRONMENT, Environment.NETHER.toString().toLowerCase(Locale.ROOT));

			// === Load or Create Behavior ===
			final SlimeLoader loader = getSlimeLoader();
			if (loader == null) {
				instance.getPluginLogger()
						.severe("No valid SlimeLoader available — cannot create or load world: " + worldName);
				resultFuture.completeExceptionally(new HellblockWorldException("SlimeLoader unavailable"));
				return;
			}

			try {
				if (exists) {
					// load existing world (readOnly=true)
					slimeWorld = this.createSlimeWorldFunction.create(worldName, true, properties, loader);
					instance.debug("Loaded existing Hellblock Slime World: %s".formatted(worldName));
				} else {
					// create new world (readOnly=false + generate)
					slimeWorld = this.createSlimeWorldFunction.create(worldName, false, properties, loader);
					this.generateSlimeWorldFunction.generate(slimeWorld);
					instance.debug("Created new Hellblock Slime World: %s".formatted(worldName));
				}

				final SlimeWorld finalWorld = slimeWorld;
				instance.getScheduler().executeSync(() -> {
					HellblockWorld<SlimeWorld> adapted = adapt(finalWorld);
					resultFuture.complete(adapted);
				});

			} catch (HellblockWorldException e) {
				instance.getPluginLogger()
						.severe("Failed to create/load SlimeWorld: " + worldName + " (" + e.getMessage() + ")");
				resultFuture.completeExceptionally(e);
			}
		});

		return resultFuture;
	}

	@Override
	public CompletableFuture<HellblockWorld<SlimeWorld>> getOrLoadIslandWorld(String worldName) {
		// Attempt to get already-loaded world
		HellblockWorld<SlimeWorld> existing = getLoadedHellblockWorld(worldName);
		if (existing != null && existing.bukkitWorld() != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public CompletableFuture<HellblockWorld<SlimeWorld>> getOrLoadIslandWorld(int islandId) {
		String worldName = instance.getWorldManager().getHellblockWorldFormat(islandId);

		// Attempt to get already-loaded world
		HellblockWorld<SlimeWorld> existing = getLoadedHellblockWorld(worldName);
		if (existing != null && existing.bukkitWorld() != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public void deleteWorld(String worldName) {
		this.deleteSlimeWorldFunction.delete(worldName);
		instance.debug("Deleted Hellblock Slime World: %s".formatted(worldName));
	}

	private CompoundBinaryTag createOrGetDataTag(SlimeWorld world) {
		final ConcurrentMap<String, BinaryTag> extraData = world.getExtraData();

		final BinaryTag tag = extraData.get(TAG_HELLBLOCK_DATA);

		if (tag instanceof CompoundBinaryTag compound) {
			return compound;
		}

		final CompoundBinaryTag newTag = CompoundBinaryTag.empty();
		extraData.put(TAG_HELLBLOCK_DATA, newTag);
		return newTag;
	}

	@Override
	public WorldExtraData loadExtraData(SlimeWorld world) {
		CompoundBinaryTag tag = createOrGetDataTag(world);

		// Read version if present (default to 0 if missing)
		int version = tag.getInt(TAG_WORLD_VERSION);
		if (version < WorldManager.CURRENT_WORLD_VERSION) {
			migrateWorld(world, version);
		}

		tag.putInt(TAG_WORLD_VERSION, WorldManager.CURRENT_WORLD_VERSION);

		// Load extraData JSON
		final String json = Optional.ofNullable(tag.get(TAG_WORLD_INFO))
				.filter(tagData -> tagData instanceof StringBinaryTag)
				.map(tagData -> ((StringBinaryTag) tagData).value()).orElse(null);

		return (json == null || "null".equals(json)) ? WorldExtraData.empty()
				: AdventureHelper.getGsonComponentSerializer().serializer().fromJson(json, WorldExtraData.class);
	}

	@SuppressWarnings("unused")
	@Override
	public void migrateWorld(SlimeWorld world, int oldVersion) {
		int newVersion = WorldManager.CURRENT_WORLD_VERSION;

		instance.getPluginLogger().info(
				"Migrating SlimeWorld '" + world.getName() + "' from version " + oldVersion + " to " + newVersion);

		try {
			HellblockWorld<SlimeWorld> wrapper = getLoadedHellblockWorld(world.getName());

			if (wrapper != null) {
				WorldSetting setting = wrapper.setting();

				// TODO: Add any migration-specific logic here
				// e.g. patch default values, enable new flags, adjust tick settings

				// Save patched world if needed
				wrapper.save(false, false);
			}

			// Update version in tag
			CompoundBinaryTag tag = createOrGetDataTag(world);
			tag.putInt(TAG_WORLD_VERSION, newVersion);

			instance.getPluginLogger()
					.info("Migration complete for SlimeWorld '" + world.getName() + "' (now at v" + newVersion + ")");
		} catch (Exception e) {
			instance.getPluginLogger().severe("Migration failed for SlimeWorld '" + world.getName() + "'", e);
		}
	}

	@Override
	public void saveExtraData(HellblockWorld<SlimeWorld> world) {
		CompoundBinaryTag tag = createOrGetDataTag(world.world());

		// Save extra data JSON
		String json = AdventureHelper.getGsonComponentSerializer().serializer().toJson(world.extraData());
		tag.put(TAG_WORLD_INFO, StringBinaryTag.stringBinaryTag(json));

		// Save version number as a separate NBT tag
		tag.putInt(TAG_WORLD_VERSION, WorldManager.CURRENT_WORLD_VERSION);
	}

	@Nullable
	@Override
	public CustomRegion loadRegion(HellblockWorld<SlimeWorld> world, RegionPos pos, boolean createIfNotExists) {
		// Return a mock region with empty cache, if createIfNotExists is true
		return createIfNotExists ? world.createRegion(pos) : null;
	}

	@Nullable
	@Override
	public CustomChunk loadChunk(HellblockWorld<SlimeWorld> world, ChunkPos pos, boolean createIfNotExists) {
		final long time1 = System.currentTimeMillis();
		instance.getWorldManager().markWorldAccess(world.worldName());
		CompoundBinaryTag tag = createOrGetDataTag(world.world());
		final BinaryTag binaryTag = tag.get(pos.asString());

		if (!(binaryTag instanceof CompoundBinaryTag compoundTag)) {
			return createIfNotExists ? world.createChunk(pos) : null;
		}

		final CustomChunk chunk = tagToChunk(world, compoundTag);
		final long time2 = System.currentTimeMillis();
		instance.debug(() -> "[" + world.worldName() + "] Loaded chunk " + pos + " in " + (time2 - time1) + "ms");
		return chunk;
	}

	@Override
	public void saveRegion(HellblockWorld<SlimeWorld> world, CustomRegion region) {
		// Regions are not stored in SlimeWorldManager
	}

	@Override
	public void saveChunk(HellblockWorld<SlimeWorld> world, CustomChunk chunk) {
		CompoundBinaryTag tag = createOrGetDataTag(world.world());
		final SerializableChunk serializableChunk = toSerializableChunk(chunk);

		final Runnable runnable = () -> {
			final String key = chunk.chunkPos().asString();

			if (serializableChunk.canPrune()) {
				tag.remove(key);
			} else {
				final CompoundBinaryTag binaryTag = chunkToTag(serializableChunk);
				tag.put(key, binaryTag);
			}
		};

		if (Bukkit.isPrimaryThread()) {
			runnable.run();
		} else {
			instance.getScheduler().sync().run(runnable, null);
		}
	}

	public void saveAllChunks(HellblockWorld<SlimeWorld> world) {
		Arrays.asList(world.loadedChunks()).stream().forEach(chunk -> saveChunk(world, chunk));
	}

	@Override
	public String getName(SlimeWorld world) {
		return world.getName();
	}

	@Override
	public int priority() {
		return SLIME_WORLD_PRIORITY;
	}

	private CompoundBinaryTag chunkToTag(SerializableChunk serializableChunk) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

		builder.putInt("x", serializableChunk.x());
		builder.putInt("z", serializableChunk.z());
		builder.putInt("version", CHUNK_VERSION);
		builder.putInt("loadedSeconds", serializableChunk.loadedSeconds());
		builder.putLong("lastLoadedTime", serializableChunk.lastLoadedTime());

		builder.putIntArray("queued", serializableChunk.queuedTasks());
		builder.putIntArray("ticked", serializableChunk.ticked());

		// Sections
		final CompoundBinaryTag.Builder sectionBuilder = CompoundBinaryTag.builder();
		serializableChunk.sections().forEach(section -> sectionBuilder.put(String.valueOf(section.sectionID()),
				ListBinaryTag.from(section.blocks())));

		builder.put("sections", sectionBuilder.build());

		return builder.build();
	}

	private CustomChunk tagToChunk(HellblockWorld<SlimeWorld> world, CompoundBinaryTag tag) {
		final int versionNumber = tag.getInt("version", 1); // default to 1
		final Function<String, net.kyori.adventure.key.Key> keyFunction = versionNumber < 2
				? s -> net.kyori.adventure.key.Key.key(TAG_HELLBLOCK_DATA, s.toLowerCase(Locale.ROOT))
				: net.kyori.adventure.key.Key::key;

		final int x = tag.getInt("x");
		final int z = tag.getInt("z");

		final ChunkPos coordinate = new ChunkPos(x, z);
		final int loadedSeconds = tag.getInt("loadedSeconds");
		final long lastLoadedTime = tag.getLong("lastLoadedTime");

		final int[] queued = tag.getIntArray("queued");
		final int[] ticked = tag.getIntArray("ticked");

		// queued tasks
		final PriorityBlockingQueue<DelayedTickTask> queue = new PriorityBlockingQueue<>(
				Math.max(11, queued.length / 2));
		for (int i = 0, size = queued.length / 2; i < size; i++) {
			final BlockPos pos = new BlockPos(queued[2 * i + 1]);
			queue.add(new DelayedTickTask(queued[2 * i], pos));
		}

		// ticked set
		final Set<BlockPos> tickedSet = new HashSet<>(Math.max(11, ticked.length));
		for (int tick : ticked) {
			tickedSet.add(new BlockPos(tick));
		}

		// Sections
		final ConcurrentMap<Integer, CustomSection> sectionMap = new ConcurrentHashMap<>();
		final CompoundBinaryTag sectionsTag = tag.getCompound("sections");

		for (String sectionId : sectionsTag.keySet()) {
			final BinaryTag sectionValue = sectionsTag.get(sectionId);
			if (sectionValue instanceof ListBinaryTag listTag) {
				final int id = Integer.parseInt(sectionId);
				final ConcurrentMap<BlockPos, CustomBlockState> blockMap = new ConcurrentHashMap<>();

				// list of compound tags
				for (BinaryTag blockTag : listTag) {
					if (!(blockTag instanceof CompoundBinaryTag blockCompound)) {
						continue;
					}

					// read block data
					final int[] posArray = blockCompound.getIntArray("pos");
					final CompoundBinaryTag dataTag = blockCompound.getCompound("data");
					final String typeString = blockCompound.getString("type");
					final net.kyori.adventure.key.Key key = keyFunction.apply(typeString);
					final CustomBlock customBlock = new CustomBlock(key);

					for (int pos : posArray) {
						final BlockPos blockPos = new BlockPos(pos);
						blockMap.put(blockPos,
								CustomBlockStateInterface.create(customBlock, TagUtils.deepClone(dataTag)));
					}
				}

				sectionMap.put(id, CustomSectionInterface.restore(id, blockMap));
			}
		}

		return world.restoreChunk(coordinate, loadedSeconds, lastLoadedTime, sectionMap, queue, tickedSet);
	}

	@FunctionalInterface
	public interface CreateSlimeWorldFunction {
		SlimeWorld create(String worldName, boolean readOnly, SlimePropertyMap properties, SlimeLoader loader);
	}

	@FunctionalInterface
	public interface GetSlimeWorldFunction {
		SlimeWorld getWorld(String worldName);
	}

	@FunctionalInterface
	public interface GetSlimeLoaderFunction {
		SlimeLoader getLoader(String loaderName);
	}

	@FunctionalInterface
	public interface DeleteSlimeWorldFunction {
		void delete(String worldName);
	}

	@FunctionalInterface
	public interface WorldExistsFunction {
		boolean exists(String worldName);
	}

	@FunctionalInterface
	public interface GenerateSlimeWorldFunction {
		Either<SlimeWorld, Void> generate(SlimeWorld world);
	}
}