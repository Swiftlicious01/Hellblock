package com.swiftlicious.hellblock.world.adapter;

import java.lang.reflect.Method;
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
import com.swiftlicious.hellblock.utils.extras.QuadFunction;
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
import com.swiftlicious.hellblock.world.RegionPos;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.WorldExtraData;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

public class SlimeWorldAdapter extends AbstractWorldAdapter<SlimeWorld> implements Listener {

	private final Function<String, SlimeWorld> getSlimeWorldFunction;
	private final Function<String, SlimeLoader> getSlimeLoaderFunction;
	private final Function<String, Void> deleteSlimeWorldFunction;
	private final Function<String, Boolean> worldExistsFunction;
	private final Function<SlimeWorld, Either<SlimeWorld, Void>> generateSlimeWorldFunction;
	private final QuadFunction<String, Boolean, SlimePropertyMap, SlimeLoader, SlimeWorld> createSlimeWorldFunction;

	private final Method getWorldMethod;
	private final Method getLoaderMethod;
	private final Method createEmptyWorldMethod;
	private final Method generateWorldMethod;
	private final Method worldExistsMethod;
	private final Method deleteWorldMethod;

	private SlimeLoader cachedLoader;

	private static final String[] LOADERS = new String[] { "api-loader", "mysql-loader", "mongo-loader", "file-loader",
			"redis-loader" };

	public SlimeWorldAdapter(int version) {
		try {
			if (version == 1) {
				final Plugin plugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
				if (plugin == null) {
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
				this.getSlimeWorldFunction = (name) -> {
					try {
						return (SlimeWorld) this.getWorldMethod.invoke(plugin, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) this.getLoaderMethod.invoke(plugin, data);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.createSlimeWorldFunction = (name, read, properties, loader) -> {
					try {
						return (SlimeWorld) this.createEmptyWorldMethod.invoke(plugin, loader, name, read, properties);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofFallback((Void) this.generateWorldMethod.invoke(plugin, slime));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.worldExistsFunction = (name) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return false;
						return (Boolean) this.worldExistsMethod.invoke(loader, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.deleteSlimeWorldFunction = (name) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return null;
						return (Void) this.deleteWorldMethod.invoke(loader, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			} else if (version == 2 && VersionHelper.isPaper()) {
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
				this.getSlimeWorldFunction = (name) -> {
					try {
						return (SlimeWorld) this.getWorldMethod.invoke(apiInstance, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) this.getLoaderMethod.invoke(apiInstance, data);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.createSlimeWorldFunction = (name, read, properties, loader) -> {
					try {
						return (SlimeWorld) this.createEmptyWorldMethod.invoke(apiInstance, name, read, properties,
								loader);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofPrimary((SlimeWorld) this.generateWorldMethod.invoke(apiInstance, slime, true));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.worldExistsFunction = (name) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return false;
						return (Boolean) this.worldExistsMethod.invoke(loader, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.deleteSlimeWorldFunction = (name) -> {
					try {
						SlimeLoader loader = this.getSlimeLoader();
						if (loader == null)
							return null;
						return (Void) this.deleteWorldMethod.invoke(loader, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			} else {
				throw new IllegalArgumentException("Unsupported version: " + version);
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@EventHandler
	public void onWorldLoad(LoadSlimeWorldEvent event) {
		final String worldName = event.getSlimeWorld().getName();
		HellblockPlugin.getInstance().getWorldManager().markWorldAccess(worldName);
		final World world = Bukkit.getWorld(worldName);

		if (world == null) {
			HellblockPlugin.getInstance().getPluginLogger().warn(
					"LoadSlimeWorldEvent triggered, but Bukkit world '%s' is not yet loaded.".formatted(worldName));
			return;
		}

		if (!HellblockPlugin.getInstance().getWorldManager().isMechanicEnabled(world)) {
			return;
		}

		HellblockPlugin.getInstance().getWorldManager().loadWorld(world);
	}

	@Override
	public HellblockWorld<SlimeWorld> adapt(Object world) {
		if (!(world instanceof SlimeWorld)) {
			throw new IllegalArgumentException("Expected Slime World, but got: " + world.getClass().getName());
		}
		return CustomWorldInterface.create((SlimeWorld) world, this);
	}

	@Override
	public Optional<SlimeWorld> getWorld(String worldName) {
		SlimeWorld world = this.getSlimeWorldFunction.apply(worldName);
		return Optional.ofNullable(world);
	}

	@Override
	@Nullable
	public HellblockWorld<SlimeWorld> getLoadedHellblockWorld(String worldName) {
		Optional<SlimeWorld> slimeOpt = getWorld(worldName);

		if (slimeOpt.isPresent()) {
			HellblockPlugin.getInstance().getWorldManager().markWorldAccess(worldName);
			return adapt(slimeOpt.get());
		}

		return null;
	}

	@SuppressWarnings("unused")
	private CompoundBinaryTag createOrGetDataTag(SlimeWorld world) {
		final ConcurrentMap<String, BinaryTag> extraData = world.getExtraData();

		final BinaryTag tag = extraData.get("hellblock");

		if (tag instanceof CompoundBinaryTag compound) {
			return compound;
		}

		final CompoundBinaryTag newTag = CompoundBinaryTag.empty();
		extraData.put("hellblock", newTag);
		return newTag;
	}

	private SlimeLoader getSlimeLoader() {
		if (cachedLoader != null)
			return cachedLoader;

		for (String loaderName : LOADERS) {
			try {
				final SlimeLoader worldLoader = this.getSlimeLoaderFunction.apply(loaderName);
				if (worldLoader != null) {
					cachedLoader = worldLoader;
					HellblockPlugin.getInstance().debug("Using SlimeLoader: " + loaderName);
					break;
				}
			} catch (Exception e) {
				HellblockPlugin.getInstance().getPluginLogger()
						.warn("Failed to load SlimeLoader: " + loaderName + " - " + e.getMessage());
			}
		}

		if (cachedLoader == null) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("No working SlimeLoader could be found. SlimeWorlds cannot be created.");
		}

		return cachedLoader;
	}

	@Override
	public CompletableFuture<HellblockWorld<SlimeWorld>> createWorld(String worldName) {
		CompletableFuture<HellblockWorld<SlimeWorld>> resultFuture = new CompletableFuture<>();
		HellblockPlugin.getInstance().getWorldManager().markWorldAccess(worldName);

		CompletableFuture.runAsync(() -> {
			SlimeWorld slimeWorld = getWorld(worldName).orElse(null);
			boolean exists = this.worldExistsFunction.apply(worldName);

			if (!exists || slimeWorld == null) {
				SlimePropertyMap properties = new SlimePropertyMap();
				properties.setValue(SlimeProperties.SPAWN_X, 0);
				properties.setValue(SlimeProperties.SPAWN_Y, HellblockPlugin.getInstance().getConfigManager().height());
				properties.setValue(SlimeProperties.SPAWN_Z, 0);
				properties.setValue(SlimeProperties.DEFAULT_BIOME,
						HellBiome.NETHER_WASTES.getConvertedBiome().getKey().getKey().toLowerCase(Locale.ROOT));
				properties.setValue(SlimeProperties.ALLOW_ANIMALS,
						HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
				properties.setValue(SlimeProperties.ALLOW_MONSTERS,
						HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
				properties.setValue(SlimeProperties.PVP, HellblockFlag.FlagType.PVP.getDefaultValue());
				properties.setValue(SlimeProperties.DIFFICULTY, Difficulty.NORMAL.toString().toLowerCase(Locale.ROOT));
				properties.setValue(SlimeProperties.ENVIRONMENT,
						Environment.NETHER.toString().toLowerCase(Locale.ROOT));

				slimeWorld = this.createSlimeWorldFunction.apply(worldName, false, properties, getSlimeLoader());
				this.generateSlimeWorldFunction.apply(slimeWorld);
			}

			final SlimeWorld finalWorld = slimeWorld;

			HellblockPlugin.getInstance().getScheduler().executeSync(() -> {
				HellblockWorld<SlimeWorld> adapted = adapt(finalWorld);
				HellblockPlugin.getInstance().getLavaRainHandler().startLavaRainProcess(adapted);
				resultFuture.complete(adapted);
			});
		});

		return resultFuture;
	}

	@Override
	public CompletableFuture<HellblockWorld<SlimeWorld>> getOrLoadIslandWorld(int islandId) {
		String worldName = HellblockPlugin.getInstance().getWorldManager().getHellblockWorldFormat(islandId);

		// Attempt to get already-loaded world
		HellblockWorld<SlimeWorld> existing = getLoadedHellblockWorld(worldName);
		if (existing != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public void deleteWorld(String world) {
		HellblockPlugin.getInstance().getLavaRainHandler().stopLavaRainProcess(world);
		this.deleteSlimeWorldFunction.apply(world);
		HellblockPlugin.getInstance().debug("Deleted Hellblock World: %s".formatted(world));
	}

	@Override
	public WorldExtraData loadExtraData(SlimeWorld world) {
		final ConcurrentMap<String, BinaryTag> extraData = world.getExtraData();
		final String json = Optional.ofNullable(extraData.get("world-info"))
				.filter(tag -> tag instanceof StringBinaryTag).map(tag -> ((StringBinaryTag) tag).value()).orElse(null);
		return (json == null || "null".equals(json)) ? WorldExtraData.empty()
				: AdventureHelper.getGson().serializer().fromJson(json, WorldExtraData.class);
	}

	@Override
	public void saveExtraData(HellblockWorld<SlimeWorld> world) {
		world.world().getExtraData().put("world-info",
				StringBinaryTag.stringBinaryTag(AdventureHelper.getGson().serializer().toJson(world.extraData())));
	}

	@Nullable
	@Override
	public CustomRegion loadRegion(HellblockWorld<SlimeWorld> world, RegionPos pos, boolean createIfNotExists) {
		// Regions are not stored in SlimeWorldManager
		return null;
	}

	@Nullable
	@Override
	public CustomChunk loadChunk(HellblockWorld<SlimeWorld> world, ChunkPos pos, boolean createIfNotExists) {
		final long time1 = System.currentTimeMillis();
		HellblockPlugin.getInstance().getWorldManager().markWorldAccess(world.worldName());
		final BinaryTag tag = world.world().getExtraData().get(pos.asString());

		if (!(tag instanceof CompoundBinaryTag compoundTag)) {
			return createIfNotExists ? world.createChunk(pos) : null;
		}

		final CustomChunk chunk = tagToChunk(world, compoundTag);
		final long time2 = System.currentTimeMillis();
		HellblockPlugin.getInstance().debug(() -> "Took " + (time2 - time1) + "ms to load chunk " + pos);
		return chunk;
	}

	@Override
	public void saveRegion(HellblockWorld<SlimeWorld> world, CustomRegion region) {
		// Regions are not stored in SlimeWorldManager
	}

	@Override
	public void saveChunk(HellblockWorld<SlimeWorld> world, CustomChunk chunk) {
		final ConcurrentMap<String, BinaryTag> extraData = world.world().getExtraData();
		final SerializableChunk serializableChunk = toSerializableChunk(chunk);

		final Runnable runnable = () -> {
			final String key = chunk.chunkPos().asString();

			if (serializableChunk.canPrune()) {
				extraData.remove(key);
			} else {
				final CompoundBinaryTag tag = chunkToTag(serializableChunk);
				extraData.put(key, tag);
			}
		};

		if (Bukkit.isPrimaryThread()) {
			runnable.run();
		} else {
			HellblockPlugin.getInstance().getScheduler().sync().run(runnable, null);
		}
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
				? s -> net.kyori.adventure.key.Key.key("hellblock", s.toLowerCase(Locale.ROOT))
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
}