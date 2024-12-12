package com.swiftlicious.hellblock.world.adapter;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.LongTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.TagType;
import com.infernalsuite.aswm.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.TagUtils;
import com.swiftlicious.hellblock.utils.extras.Key;
import com.swiftlicious.hellblock.utils.extras.Either;
import com.swiftlicious.hellblock.utils.extras.QuadFunction;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.DelayedTickTask;
import com.swiftlicious.hellblock.world.HellblockBlock;
import com.swiftlicious.hellblock.world.HellblockBlockState;
import com.swiftlicious.hellblock.world.HellblockBlockStateInterface;
import com.swiftlicious.hellblock.world.HellblockChunk;
import com.swiftlicious.hellblock.world.HellblockRegion;
import com.swiftlicious.hellblock.world.HellblockSection;
import com.swiftlicious.hellblock.world.HellblockSectionInterface;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.HellblockWorldInterface;
import com.swiftlicious.hellblock.world.RegionPos;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.SerializableSection;
import com.swiftlicious.hellblock.world.WorldExtraData;

public class SlimeWorldAdapter extends AbstractWorldAdapter<SlimeWorld> implements Listener {

	private final Function<String, SlimeWorld> getSlimeWorldFunction;
	private final Function<String, SlimeLoader> getSlimeLoaderFunction;
	private final Function<String, Void> deleteSlimeWorldFunction;
	private final Function<String, Boolean> worldExistsFunction;
	private final Function<SlimeWorld, Either<SlimeWorld, Void>> generateSlimeWorldFunction;
	private final QuadFunction<String, Boolean, SlimePropertyMap, SlimeLoader, SlimeWorld> createSlimeWorldFunction;

	private static final String[] LOADERS = new String[] { "api", "mysql", "mongodb", "file" };

	public SlimeWorldAdapter(int version) {
		try {
			if (version == 1) {
				Plugin plugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
				Class<?> slimeClass = Class.forName("com.infernalsuite.aswm.api.SlimePlugin");
				Method getWorldMethod = slimeClass.getMethod("getWorld", String.class);
				Method getLoaderMethod = slimeClass.getMethod("getLoader", String.class);
				Method createEmptyWorldMethod = slimeClass.getMethod("createEmptyWorld", SlimeLoader.class,
						String.class, Boolean.class, SlimePropertyMap.class);
				Method generateWorldMethod = slimeClass.getMethod("generateWorld", SlimeWorld.class);
				Class<?> loaderClass = Class.forName("com.infernalsuite.aswm.api.loaders.SlimeLoader");
				Method worldExistsMethod = loaderClass.getMethod("worldExists", String.class);
				Method deleteWorldMethod = loaderClass.getMethod("deleteWorld", String.class);
				this.getSlimeWorldFunction = (name) -> {
					try {
						return (SlimeWorld) getWorldMethod.invoke(plugin, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) getLoaderMethod.invoke(plugin, data);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.createSlimeWorldFunction = (name, read, properties, loader) -> {
					try {
						return (SlimeWorld) createEmptyWorldMethod.invoke(plugin, loader, name, read, properties);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofFallback((Void) generateWorldMethod.invoke(plugin, slime));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.worldExistsFunction = (name) -> {
					try {
						return (Boolean) worldExistsMethod.invoke(loaderClass, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.deleteSlimeWorldFunction = (name) -> {
					try {
						return (Void) deleteWorldMethod.invoke(loaderClass, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
			} else if (version == 2 && VersionHelper.isPaper()) {
				Class<?> apiClass = Class.forName("com.infernalsuite.aswm.api.AdvancedSlimePaperAPI");
				Object apiInstance = apiClass.getMethod("instance").invoke(null);
				Method getWorldMethod = apiClass.getMethod("getLoadedWorld", String.class);
				Class<?> loaderManagerClass = Class.forName("com.infernalsuite.aswm.plugin.loader.LoaderManager");
				Method getLoaderMethod = loaderManagerClass.getMethod("getLoader", String.class);
				Method createEmptyWorldMethod = apiClass.getMethod("createEmptyWorld", String.class, Boolean.class,
						SlimePropertyMap.class, SlimeLoader.class);
				Method generateWorldMethod = apiClass.getMethod("loadWorld", SlimeWorld.class, Boolean.class);
				Class<?> loaderClass = Class.forName("com.infernalsuite.aswm.api.loaders.SlimeLoader");
				Method worldExistsMethod = loaderClass.getMethod("worldExists", String.class);
				Method deleteWorldMethod = loaderClass.getMethod("deleteWorld", String.class);
				this.getSlimeWorldFunction = (name) -> {
					try {
						return (SlimeWorld) getWorldMethod.invoke(apiInstance, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.getSlimeLoaderFunction = (data) -> {
					try {
						return (SlimeLoader) getLoaderMethod.invoke(apiInstance, data);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.createSlimeWorldFunction = (name, read, properties, loader) -> {
					try {
						return (SlimeWorld) createEmptyWorldMethod.invoke(apiInstance, name, read, properties, loader);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.generateSlimeWorldFunction = (slime) -> {
					try {
						return Either.ofPrimary((SlimeWorld) generateWorldMethod.invoke(apiInstance, slime, true));
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.worldExistsFunction = (name) -> {
					try {
						return (Boolean) worldExistsMethod.invoke(loaderClass, name);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException(e);
					}
				};
				this.deleteSlimeWorldFunction = (name) -> {
					try {
						return (Void) deleteWorldMethod.invoke(loaderClass, name);
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
		World world = Bukkit.getWorld(event.getSlimeWorld().getName());
		if (!HellblockPlugin.getInstance().getWorldManager().isMechanicEnabled(world))
			return;
		HellblockPlugin.getInstance().getWorldManager().loadWorld(world);
	}

	@Override
	public HellblockWorld<SlimeWorld> adapt(Object world) {
		return HellblockWorldInterface.create((SlimeWorld) world, this);
	}

	@Override
	public SlimeWorld getWorld(String worldName) {
		return getSlimeWorldFunction.apply(worldName);
	}

	private CompoundMap createOrGetDataMap(SlimeWorld world) {
		Optional<CompoundTag> optionalCompoundTag = world.getExtraData().getAsCompoundTag("hellblock");
		CompoundMap ccDataMap;
		if (optionalCompoundTag.isEmpty()) {
			ccDataMap = new CompoundMap();
			world.getExtraData().getValue().put(new CompoundTag("hellblock", ccDataMap));
		} else {
			ccDataMap = optionalCompoundTag.get().getValue();
		}
		return ccDataMap;
	}

	private SlimeLoader getSlimeLoader() {
		SlimeLoader loader = null;
		for (String loaderName : LOADERS) {
			SlimeLoader worldLoader = getSlimeLoaderFunction.apply(loaderName);
			if (worldLoader != null) {
				loader = worldLoader;
				break;
			}
		}
		return loader;
	}

	@Override
	public HellblockWorld<SlimeWorld> createWorld(String world) {
		SlimeWorld hellblockWorld = getWorld(world);
		if (!worldExistsFunction.apply(world).booleanValue() || hellblockWorld == null) {
			SlimePropertyMap properties = new SlimePropertyMap();
			properties.setValue(SlimeProperties.SPAWN_X, 0);
			properties.setValue(SlimeProperties.SPAWN_Y, HellblockPlugin.getInstance().getConfigManager().height());
			properties.setValue(SlimeProperties.SPAWN_Z, 0);
			properties.setValue(SlimeProperties.ALLOW_ANIMALS, HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
			properties.setValue(SlimeProperties.ALLOW_MONSTERS, HellblockFlag.FlagType.MOB_SPAWNING.getDefaultValue());
			properties.setValue(SlimeProperties.PVP, HellblockFlag.FlagType.PVP.getDefaultValue());
			properties.setValue(SlimeProperties.DIFFICULTY, "normal");
			properties.setValue(SlimeProperties.ENVIRONMENT, "nether");
			hellblockWorld = createSlimeWorldFunction.apply(world, false, properties,
					getSlimeLoader() != null ? getSlimeLoader() : null);
			generateSlimeWorldFunction.apply(hellblockWorld);
			HellblockPlugin.getInstance().debug(String.format("Created a new Hellblock World: %s", world));
		}
		HellblockWorld<SlimeWorld> adaptedWorld = adapt(hellblockWorld);
		HellblockPlugin.getInstance().getLavaRainHandler().startLavaRainProcess(adaptedWorld);
		return adaptedWorld;
	}

	@Override
	public void deleteWorld(String world) {
		HellblockPlugin.getInstance().getLavaRainHandler().stopLavaRainProcess(world);
		deleteSlimeWorldFunction.apply(world);
	}

	@Override
	public WorldExtraData loadExtraData(SlimeWorld world) {
		CompoundMap ccDataMap = createOrGetDataMap(world);
		String json = Optional.ofNullable(ccDataMap.get("world-info")).map(tag -> tag.getAsStringTag().get().getValue())
				.orElse(null);
		return (json == null || json.equals("null")) ? WorldExtraData.empty()
				: AdventureHelper.getGson().serializer().fromJson(json, WorldExtraData.class);
	}

	@Override
	public void saveExtraData(HellblockWorld<SlimeWorld> world) {
		CompoundMap ccDataMap = createOrGetDataMap(world.world());
		ccDataMap.put(new StringTag("world-info", AdventureHelper.getGson().serializer().toJson(world.extraData())));
	}

	@Nullable
	@Override
	public HellblockRegion loadRegion(HellblockWorld<SlimeWorld> world, RegionPos pos, boolean createIfNotExists) {
		return null;
	}

	@Nullable
	@Override
	public HellblockChunk loadChunk(HellblockWorld<SlimeWorld> world, ChunkPos pos, boolean createIfNotExists) {
		long time1 = System.currentTimeMillis();
		CompoundMap ccDataMap = createOrGetDataMap(world.world());
		Tag<?> chunkTag = ccDataMap.get(pos.asString());
		if (chunkTag == null) {
			return createIfNotExists ? world.createChunk(pos) : null;
		}
		Optional<CompoundTag> chunkCompoundTag = chunkTag.getAsCompoundTag();
		if (chunkCompoundTag.isEmpty()) {
			return createIfNotExists ? world.createChunk(pos) : null;
		}
		HellblockChunk chunk = tagToChunk(world, chunkCompoundTag.get());
		long time2 = System.currentTimeMillis();
		HellblockPlugin.getInstance().debug(() -> "Took " + (time2 - time1) + "ms to load chunk " + pos);
		return chunk;
	}

	@Override
	public void saveRegion(HellblockWorld<SlimeWorld> world, HellblockRegion region) {
	}

	@Override
	public void saveChunk(HellblockWorld<SlimeWorld> world, HellblockChunk chunk) {
		CompoundMap ccDataMap = createOrGetDataMap(world.world());
		SerializableChunk serializableChunk = toSerializableChunk(chunk);
		Runnable runnable = () -> {
			if (serializableChunk.canPrune()) {
				ccDataMap.remove(chunk.chunkPos().asString());
			} else {
				ccDataMap.put(chunkToTag(serializableChunk));
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

	private CompoundTag chunkToTag(SerializableChunk serializableChunk) {
		CompoundMap map = new CompoundMap();
		map.put(new IntTag("x", serializableChunk.x()));
		map.put(new IntTag("z", serializableChunk.z()));
		map.put(new IntTag("version", CHUNK_VERSION));
		map.put(new IntTag("loadedSeconds", serializableChunk.loadedSeconds()));
		map.put(new LongTag("lastLoadedTime", serializableChunk.lastLoadedTime()));
		map.put(new IntArrayTag("queued", serializableChunk.queuedTasks()));
		map.put(new IntArrayTag("ticked", serializableChunk.ticked()));
		CompoundMap sectionMap = new CompoundMap();
		for (SerializableSection section : serializableChunk.sections()) {
			sectionMap.put(new ListTag<>(String.valueOf(section.sectionID()), TagType.TAG_COMPOUND, section.blocks()));
		}
		map.put(new CompoundTag("sections", sectionMap));
		return new CompoundTag(serializableChunk.x() + "," + serializableChunk.z(), map);
	}

	@SuppressWarnings("all")
	private HellblockChunk tagToChunk(HellblockWorld<SlimeWorld> world, CompoundTag tag) {
		CompoundMap map = tag.getValue();
		IntTag version = (IntTag) map.getOrDefault("version", new IntTag("version", 1));
		int versionNumber = version.getValue();
		Function<String, Key> keyFunction = versionNumber < 2 ? (s) -> {
			return Key.of("hellblock", StringUtils.toLowerCase(s));
		} : s -> {
			return Key.of(s);
		};
		int x = (int) map.get("x").getValue();
		int z = (int) map.get("z").getValue();

		ChunkPos coordinate = new ChunkPos(x, z);
		int loadedSeconds = (int) map.get("loadedSeconds").getValue();
		long lastLoadedTime = (long) map.get("lastLoadedTime").getValue();
		int[] queued = (int[]) map.get("queued").getValue();
		int[] ticked = (int[]) map.get("ticked").getValue();

		PriorityBlockingQueue<DelayedTickTask> queue = new PriorityBlockingQueue<>(Math.max(11, queued.length / 2));
		for (int i = 0, size = queued.length / 2; i < size; i++) {
			BlockPos pos = new BlockPos(queued[2 * i + 1]);
			queue.add(new DelayedTickTask(queued[2 * i], pos));
		}

		Set<BlockPos> tickedSet = new HashSet<>(Math.max(11, ticked.length));
		for (int tick : ticked) {
			tickedSet.add(new BlockPos(tick));
		}

		ConcurrentMap<Integer, HellblockSection> sectionMap = new ConcurrentHashMap<>();
		CompoundMap sectionCompoundMap = (CompoundMap) map.get("sections").getValue();
		for (Map.Entry<String, Tag<?>> entry : sectionCompoundMap.entrySet()) {
			if (entry.getValue() instanceof ListTag<?> listTag) {
				int id = Integer.parseInt(entry.getKey());
				ConcurrentMap<BlockPos, HellblockBlockState> blockMap = new ConcurrentHashMap<>();
				ListTag<CompoundTag> blocks = (ListTag<CompoundTag>) listTag;
				for (CompoundTag blockTag : blocks.getValue()) {
					CompoundMap block = blockTag.getValue();
					CompoundMap data = (CompoundMap) block.get("data").getValue();
					Key key = keyFunction.apply((String) block.get("type").getValue());
					HellblockBlock customBlock = new HellblockBlock(key);
					if (customBlock == null) {
						HellblockPlugin.getInstance().getInstance().getPluginLogger().warn("[" + world.worldName()
								+ "] Unrecognized block " + key + " has been removed from chunk " + ChunkPos.of(x, z));
						continue;
					}
					for (int pos : (int[]) block.get("pos").getValue()) {
						BlockPos blockPos = new BlockPos(pos);
						blockMap.put(blockPos,
								HellblockBlockStateInterface.create(customBlock, TagUtils.deepClone(data)));
					}
				}
				sectionMap.put(id, HellblockSectionInterface.restore(id, blockMap));
			}
		}
		return world.restoreChunk(coordinate, loadedSeconds, lastLoadedTime, sectionMap, queue, tickedSet);
	}
}