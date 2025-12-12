package com.swiftlicious.hellblock.world.adapter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.VoidGeneratorFactory;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.FileUtils;
import com.swiftlicious.hellblock.utils.StringUtils;
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
import com.swiftlicious.hellblock.world.SerializableSection;
import com.swiftlicious.hellblock.world.WorldContainerUtil;
import com.swiftlicious.hellblock.world.WorldExtraData;
import com.swiftlicious.hellblock.world.WorldManager;
import com.swiftlicious.hellblock.world.WorldSetting;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

/**
 * An implementation of AbstractWorldAdapter for Bukkit/Spigot/Paper servers.
 */
public class BukkitWorldAdapter extends AbstractWorldAdapter<World> {

	private final HellblockPlugin instance;

	private BiFunction<World, RegionPos, File> regionFileProvider;
	private Function<World, File> worldFolderProvider;

	private static final NamespacedKey WORLD_DATA = new NamespacedKey(HellblockPlugin.getInstance(), "data");
	private static final String DATA_FILE = "hellblock.dat";

	private static final String TAG_HELLBLOCK_DATA = "hellblock";

	public BukkitWorldAdapter(HellblockPlugin plugin) {
		instance = plugin;
		this.worldFolderProvider = (world -> {
			if (instance.getConfigManager().absoluteWorldPath().isEmpty()) {
				return world.getWorldFolder();
			} else {
				return new File(instance.getConfigManager().absoluteWorldPath(), world.getName());
			}
		});

		this.regionFileProvider = (world, pos) -> new File(this.worldFolderProvider.apply(world),
				TAG_HELLBLOCK_DATA + File.separator + getRegionDataFile(pos));
	}

	public void setRegionFileProvider(BiFunction<World, RegionPos, File> regionFileProvider) {
		this.regionFileProvider = regionFileProvider;
	}

	public void setWorldFolderProvider(Function<World, File> worldFolderProvider) {
		this.worldFolderProvider = worldFolderProvider;
	}

	@Override
	public Optional<World> getWorld(String worldName) {
		World world = Bukkit.getWorld(worldName);
		return Optional.ofNullable(world);
	}

	@Override
	@Nullable
	public HellblockWorld<World> getLoadedHellblockWorld(String worldName) {
		Optional<World> bukkitOpt = getWorld(worldName);

		if (bukkitOpt.isPresent()) {
			instance.getWorldManager().markWorldAccess(worldName);
			return adapt(bukkitOpt.get());
		}

		return null;
	}

	@Override
	public HellblockWorld<World> adapt(Object world) {
		if (!(world instanceof World bukkitWorld)) {
			throw new HellblockWorldException("Expected Bukkit World, but got: " + world.getClass().getName());
		}
		HellblockWorld<World> adaptedWorld = CustomWorldInterface.create(bukkitWorld, this);

		// Ensure setting is applied immediately
		WorldSetting setting = Optional.ofNullable(instance.getWorldManager().getWorldSetting(bukkitWorld.getName()))
				.orElse(instance.getWorldManager().getDefaultWorldSetting());

		adaptedWorld.setting(setting);
		return adaptedWorld;
	}

	@Override
	public CompletableFuture<HellblockWorld<World>> createWorld(String worldName) {
		CompletableFuture<HellblockWorld<World>> future = new CompletableFuture<>();

		instance.getScheduler().executeSync(() -> {
			World hellblockWorld = getWorld(worldName).orElse(null);

			// Check if world folder exists before creation
			File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
			boolean worldExists = worldFolder.exists() && worldFolder.isDirectory();

			if (hellblockWorld == null) {
				final ChunkGenerator voidGen = VoidGeneratorFactory.create();

				// Create WorldCreator with generator
				WorldCreator creator = WorldCreator.name(worldName).type(WorldType.NORMAL).generateStructures(false)
						.environment(Environment.NETHER).generator(voidGen);

				// biomeProvider(...) only exists in 1.17.1+
				// Use reflection to avoid compile errors on 1.17.0
				try {
					Class<?> biomeProviderClass = Class.forName("org.bukkit.generator.BiomeProvider");
					Method biomeProviderSetter = WorldCreator.class.getMethod("biomeProvider", biomeProviderClass);

					// Get biome provider from generator directly (will be the fixed value from
					// proxy)
					Object provider = voidGen.getClass()
							.getMethod("getDefaultBiomeProvider", Class.forName("org.bukkit.generator.WorldInfo"))
							.invoke(voidGen, (Object) null);
					if (provider != null) {
						biomeProviderSetter.invoke(creator, provider);
						instance.debug("BiomeProvider dynamically applied to world: " + worldName);
					}
				} catch (Throwable t) {
					instance.getPluginLogger().warn(
							"Failed to assign biome provider via reflection, if you're running on 1.17 ignore this.",
							t);
				}

				// Create world
				hellblockWorld = creator.createWorld();

				instance.debug("Created a new Hellblock Bukkit world: %s".formatted(worldName));
				importWorldIntoMultiverse(worldName);
			}

			if (worldExists) {
				instance.debug("Loaded existing Hellblock Bukkit world: %s".formatted(worldName));
			}
			final HellblockWorld<World> adaptedWorld = adapt(hellblockWorld);
			future.complete(adaptedWorld);
		});

		return future;
	}

	@SuppressWarnings({ "deprecation" })
	private boolean importWorldIntoMultiverse(@NotNull String worldName) {
		if (!org.mvplugins.multiverse.core.MultiverseCoreApi.isLoaded()) {
			return false;
		}

		String pluginName = instance.getDescription().getName();

		try {
			org.mvplugins.multiverse.core.MultiverseCoreApi mvApi = org.mvplugins.multiverse.core.MultiverseCoreApi
					.get();
			org.mvplugins.multiverse.core.world.WorldManager worldManager = mvApi.getWorldManager();

			org.mvplugins.multiverse.core.world.options.ImportWorldOptions worldOptions = org.mvplugins.multiverse.core.world.options.ImportWorldOptions
					.worldName(worldName);
			worldOptions.generator(pluginName).biome(pluginName).environment(Environment.NETHER).useSpawnAdjust(false);

			org.mvplugins.multiverse.core.utils.result.Attempt<org.mvplugins.multiverse.core.world.LoadedMultiverseWorld, org.mvplugins.multiverse.core.world.reasons.ImportFailureReason> importedWorldResult = worldManager
					.importWorld(worldOptions);

			if (importedWorldResult.isSuccess()) {
				// Success case: call get() to get LoadedMultiverseWorld
				// org.mvplugins.multiverse.core.world.LoadedMultiverseWorld mvWorld =
				// importedWorldResult.get();
				instance.debug("Successfully imported world '%s' into Multiverse-Core.".formatted(worldName));
				return true;
			} else if (importedWorldResult.isFailure()) {
				// Failure case: call getFailureReason() to get ImportFailureReason
				org.mvplugins.multiverse.core.world.reasons.ImportFailureReason reason = importedWorldResult
						.getFailureReason();
				instance.getPluginLogger().warn("Failed to import world '%s' into Multiverse-Core. Reason: %s"
						.formatted(worldName, reason.getMessageKey()));
				return false;
			} else {
				instance.getPluginLogger().warn("Unknown result from Multiverse-Core importWorld for world '%s': %s"
						.formatted(worldName, importedWorldResult.getFailureMessage()));
				return false;
			}
		} catch (IllegalArgumentException ex) {
			instance.getPluginLogger().severe("Failed to import world '%s' into Multiverse-Core.".formatted(worldName),
					ex);
			return false;
		}
	}

	@Override
	public CompletableFuture<HellblockWorld<World>> getOrLoadIslandWorld(String worldName) {
		// Attempt to get already-loaded world
		HellblockWorld<World> existing = getLoadedHellblockWorld(worldName);
		if (existing != null && existing.bukkitWorld() != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public CompletableFuture<HellblockWorld<World>> getOrLoadIslandWorld(int islandId) {
		String worldName = instance.getWorldManager().getHellblockWorldFormat(islandId);

		// Attempt to get already-loaded world
		HellblockWorld<World> existing = getLoadedHellblockWorld(worldName);
		if (existing != null && existing.bukkitWorld() != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public void deleteWorld(String world) {
		if (instance.getConfigManager().perPlayerWorlds())
			return;

		World bukkitWorld = getWorld(world).orElse(null);
		if (bukkitWorld == null) {
			return;
		}
		Bukkit.unloadWorld(bukkitWorld, false);
		final File worldFolder = worldFolderProvider.apply(bukkitWorld);
		if (worldFolder.exists() && worldFolder.isDirectory()) {
			try {
				FileUtils.deleteDirectory(worldFolder.toPath());
				instance.debug("Deleted Hellblock Bukkit World: %s".formatted(world));
			} catch (IOException e) {
				instance.getPluginLogger()
						.severe("Failed to delete Hellblock world folder: " + worldFolder.getAbsolutePath(), e);
			}
		}
	}

	@Override
	public WorldExtraData loadExtraData(World world) {
		if (WorldContainerUtil.isPersistentDataContainerAvailable()) {
			String json = WorldContainerUtil.getString(world, WORLD_DATA);
			WorldContainerUtil.setVersion(world, WorldManager.CURRENT_WORLD_VERSION);
			WorldExtraData data = (json == null || "null".equals(json)) ? WorldExtraData.empty()
					: AdventureHelper.getGsonComponentSerializer().serializer().fromJson(json, WorldExtraData.class);
			return data != null ? data : WorldExtraData.empty();
		}

		// Fallback for < 1.18.1
		final File data = new File(getWorldFolder(world), DATA_FILE);
		if (data.exists()) {
			final byte[] fileBytes = new byte[(int) data.length()];
			try (FileInputStream fis = new FileInputStream(data)) {
				fis.read(fileBytes);
			} catch (IOException e) {
				instance.getPluginLogger().severe(
						"[" + world.getName() + "] Failed to load extra data from " + data.getAbsolutePath(), e);
			}
			final String jsonContent = new String(fileBytes, StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();

			int version = root.has("version") ? root.get("version").getAsInt() : 0;
			if (version < WorldManager.CURRENT_WORLD_VERSION) {
				// Trigger migration
				migrateWorld(world, version);
			}

			JsonElement dataElement = root.get("extraData");
			if (dataElement == null || dataElement.isJsonNull()) {
				return WorldExtraData.empty();
			}

			return AdventureHelper.getGsonComponentSerializer().serializer().fromJson(dataElement,
					WorldExtraData.class);
		} else {
			return WorldExtraData.empty();
		}
	}

	@SuppressWarnings("unused")
	@Override
	public void migrateWorld(World world, int oldVersion) {
		int newVersion = WorldManager.CURRENT_WORLD_VERSION;

		instance.getPluginLogger()
				.info("Migrating world '" + world.getName() + "' from version " + oldVersion + " to " + newVersion);

		try {
			if (oldVersion < 1) {
				// Example migration for legacy worlds
				instance.getPluginLogger().info("Applying migration patch for legacy world format (< v1)");

				Optional<HellblockWorld<?>> wrapperOpt = instance.getWorldManager().getWorld(world);
				wrapperOpt.ifPresent(wrapper -> {
					WorldSetting setting = wrapper.setting();

					// Example: ensure new fields have defaults if missing
					// setting.setNewFlagIfNull(true);
					// setting.updateTickParameters();

					// Save after patch if needed
					wrapper.save(false, false);
				});
			}

			// After successful migration, persist the new version marker

			// If PDC is available (modern worlds)
			if (WorldContainerUtil.isPersistentDataContainerAvailable()) {
				WorldContainerUtil.setVersion(world, newVersion);
			}

			// If using file‑based fallback (older Minecraft versions)
			File data = new File(world.getWorldFolder(), "hellblock.dat");
			if (data.exists()) {
				try (FileReader reader = new FileReader(data)) {
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					root.addProperty("version", newVersion);

					try (FileWriter writer = new FileWriter(data)) {
						AdventureHelper.getGsonComponentSerializer().serializer().toJson(root, writer);
					}
				} catch (Exception e) {
					instance.getPluginLogger().warn("Failed to update version for " + world.getName(), e);
				}
			}

			instance.getPluginLogger()
					.info("Migration complete for world '" + world.getName() + "' (now at v" + newVersion + ")");
		} catch (Exception e) {
			instance.getPluginLogger().severe("Migration failed for world '" + world.getName() + "'", e);
		}
	}

	@Override
	public void saveExtraData(HellblockWorld<World> world) {
		if (WorldContainerUtil.isPersistentDataContainerAvailable()) {
			// Serialize the extra data to JSON
			String json = AdventureHelper.getGsonComponentSerializer().serializer().toJson(world.extraData());

			// Write both data and version
			boolean dataSuccess = WorldContainerUtil.setString(world.world(), WORLD_DATA, json);
			boolean versionSuccess = WorldContainerUtil.setVersion(world.world(), WorldManager.CURRENT_WORLD_VERSION);

			if (dataSuccess && versionSuccess) {
				return; // Successfully saved via PDC
			}

			// Optionally log a warning if partial failure
			if (!versionSuccess) {
				instance.getPluginLogger().warn("Failed to save version to PDC for world: " + world.worldName());
			}
		}

		// Fallback for < 1.18.1
		final File data = new File(getWorldFolder(world.world()), DATA_FILE);
		final File parentDir = data.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		try (FileWriter file = new FileWriter(data)) {
			// wrap your data in a JSON object with a version
			JsonObject root = new JsonObject();
			root.addProperty("version", WorldManager.CURRENT_WORLD_VERSION);
			root.add("extraData",
					AdventureHelper.getGsonComponentSerializer().serializer().toJsonTree(world.extraData()));
			AdventureHelper.getGsonComponentSerializer().serializer().toJson(root, file);
		} catch (IOException e) {
			instance.getPluginLogger()
					.severe("[" + world.worldName() + "] Failed to save extra data to " + data.getAbsolutePath(), e);
		}
	}

	@Nullable
	@Override
	public CustomRegion loadRegion(HellblockWorld<World> world, RegionPos pos, boolean createIfNotExist) {
		final File data = getRegionDataFile(world.world(), pos);
		// if the data file doesn't exist
		if (!data.exists()) {
			return createIfNotExist ? world.createRegion(pos) : null;
		} else {
			// load region from local files
			try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(data));
					DataInputStream dataStream = new DataInputStream(bis)) {
				return deserializeRegion(world, dataStream, pos);
			} catch (Exception e) {
				instance.getPluginLogger().severe("[" + world.worldName() + "] Failed to load Hellblock region data at "
						+ pos + ". Deleting the corrupted region.", e);
				final boolean success = data.delete();
				if (success) {
					return createIfNotExist ? world.createRegion(pos) : null;
				} else {
					throw new RuntimeException(
							"[" + world.worldName() + "] Failed to delete corrupted Hellblock region data at " + pos);
				}
			}
		}
	}

	@Nullable
	@Override
	public CustomChunk loadChunk(HellblockWorld<World> world, ChunkPos pos, boolean createIfNotExist) {
		final CustomRegion region = world.getOrCreateRegion(pos.toRegionPos());

		// In order to reduce frequent disk reads to determine whether a region exists,
		// we read the region into the cache
		if (!region.isLoaded()) {
			region.load();
		}

		final byte[] bytes = region.getCachedChunkBytes(pos);

		if (bytes != null) {
			instance.debug(() -> "[" + world.worldName() + "] Deserializing chunk at " + pos);
			long time1 = System.currentTimeMillis();
			try (DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
				CustomChunk chunk = deserializeChunk(world, dataStream);
				long time2 = System.currentTimeMillis();
				instance.debug(
						() -> "[" + world.worldName() + "] Loaded chunk " + pos + " in " + (time2 - time1) + "ms");
				return chunk;
			} catch (IOException e) {
				instance.getPluginLogger().severe("Failed to load chunk " + pos, e);
				region.removeCachedChunk(pos);
			}
		}

		if (!createIfNotExist) {
			return null;
		}

		// Safe chunk creation
		instance.debug(() -> "[" + world.worldName() + "] Creating new chunk at: " + pos);
		return world.createChunk(pos);
	}

	@Override
	public void saveRegion(HellblockWorld<World> world, CustomRegion region) {
		final File file = getRegionDataFile(world.world(), region.regionPos());
		if (region.canPrune()) {
			if (file.exists()) {
				file.delete();
			}
			return;
		}
		final long time1 = System.currentTimeMillis();
		final File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		try (FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			bos.write(serializeRegion(region));
			final long time2 = System.currentTimeMillis();
			instance.debug(() -> "[" + world.worldName() + "] Took " + (time2 - time1) + "ms to save region "
					+ region.regionPos());
		} catch (IOException e) {
			instance.getPluginLogger().severe(
					"[" + world.worldName() + "] Failed to save Hellblock region data." + region.regionPos(), e);
		}
	}

	@Override
	public void saveChunk(HellblockWorld<World> world, CustomChunk chunk) {
		final RegionPos pos = chunk.chunkPos().toRegionPos();
		final Optional<CustomRegion> region = world.getLoadedRegion(pos);
		if (region.isEmpty()) {
			instance.getPluginLogger().severe("[" + world.worldName() + "] Region " + pos + " unloaded before chunk "
					+ chunk.chunkPos() + " saving.");
		} else {
			final CustomRegion hellblockRegion = region.get();
			final SerializableChunk serializableChunk = toSerializableChunk(chunk);
			if (serializableChunk.canPrune()) {
				hellblockRegion.removeCachedChunk(chunk.chunkPos());
			} else {
				hellblockRegion.setCachedChunk(chunk.chunkPos(), serializeChunk(serializableChunk));
			}
		}
	}

	@Override
	public String getName(World world) {
		return world.getName();
	}

	@Override
	public int priority() {
		return BUKKIT_WORLD_PRIORITY;
	}

	private CustomRegion deserializeRegion(HellblockWorld<World> world, DataInputStream dataStream, RegionPos pos)
			throws IOException {
		final ConcurrentMap<ChunkPos, byte[]> map = new ConcurrentHashMap<>();
		final int chunkAmount = dataStream.readInt();
		for (int i = 0; i < chunkAmount; i++) {
			final int chunkX = dataStream.readInt();
			final int chunkZ = dataStream.readInt();
			final ChunkPos chunkPos = ChunkPos.of(chunkX, chunkZ);
			final byte[] chunkData = new byte[dataStream.readInt()];
			dataStream.read(chunkData);
			map.put(chunkPos, chunkData);
		}
		return world.restoreRegion(pos, map);
	}

	private byte[] serializeRegion(CustomRegion region) {
		final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		final DataOutputStream outStream = new DataOutputStream(outByteStream);
		try {
			outStream.writeByte(REGION_VERSION);
			outStream.writeInt(region.regionPos().x());
			outStream.writeInt(region.regionPos().z());
			final Map<ChunkPos, byte[]> map = region.dataToSave();
			outStream.writeInt(map.size());
			for (Map.Entry<ChunkPos, byte[]> entry : map.entrySet()) {
				outStream.writeInt(entry.getKey().x());
				outStream.writeInt(entry.getKey().z());
				final byte[] dataArray = entry.getValue();
				outStream.writeInt(dataArray.length);
				outStream.write(dataArray);
			}
		} catch (IOException e) {
			instance.getPluginLogger().severe("Failed to serialize Hellblock region data." + region.regionPos(), e);
		}
		return outByteStream.toByteArray();
	}

	private CustomChunk deserializeChunk(HellblockWorld<World> world, DataInputStream dataStream) throws IOException {
		final int chunkVersion = dataStream.readByte();
		final byte[] blockData = readCompressedBytes(dataStream);
		return deserializeChunk(world, blockData, chunkVersion);
	}

	private byte[] readCompressedBytes(DataInputStream dataStream) throws IOException {
		final int compressedLength = dataStream.readInt();
		final int decompressedLength = dataStream.readInt();
		final byte[] compressedData = new byte[compressedLength];
		final byte[] decompressedData = new byte[decompressedLength];

		dataStream.read(compressedData);
		zstdDecompress(decompressedData, compressedData);
		return decompressedData;
	}

	private File getWorldFolder(World world) {
		return worldFolderProvider.apply(world);
	}

	private File getRegionDataFile(World world, RegionPos regionPos) {
		return regionFileProvider.apply(world, regionPos);
	}

	private String getRegionDataFile(RegionPos regionPos) {
		return "r." + regionPos.x() + "." + regionPos.z() + ".mcc";
	}

	private byte[] serializeChunk(SerializableChunk serializableChunk) {
		final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		final DataOutputStream outStream = new DataOutputStream(outByteStream);

		try {
			// Write the chunk format version
			outStream.writeByte(CHUNK_VERSION);

			// Convert sections to CompoundBinaryTag list
			final Collection<SerializableSection> sections = serializableChunk.sections();

			final List<CompoundBinaryTag> sectionTags = sections.stream().map(this::toCompoundTag).toList();

			// Serialize and compress
			final byte[] serializedSections = toBytes(sectionTags);
			final byte[] compressed = zstdCompress(serializedSections);

			// Write metadata and data
			outStream.writeInt(compressed.length);
			outStream.writeInt(serializedSections.length);
			outStream.write(compressed);

		} catch (IOException e) {
			instance.getPluginLogger().severe("Failed to serialize chunk "
					+ ChunkPos.of(serializableChunk.x(), serializableChunk.z()) + ": " + e.getMessage());
		}

		return outByteStream.toByteArray();
	}

	private byte[] toBytes(Collection<CompoundBinaryTag> blocks) throws IOException {
		final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
		final DataOutputStream outStream = new DataOutputStream(outByteStream);

		outStream.writeInt(blocks.size());

		for (CompoundBinaryTag block : blocks) {
			final byte[] blockData = toBytes(block);
			outStream.writeInt(blockData.length);
			outStream.write(blockData);
		}

		return outByteStream.toByteArray();
	}

	private byte[] toBytes(CompoundBinaryTag tag) throws IOException {
		if (tag == null || tag.isEmpty()) {
			return new byte[0];
		}
		final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		BinaryTagIO.writer().write(tag, outByteStream);
		return outByteStream.toByteArray();
	}

	private CustomChunk deserializeChunk(HellblockWorld<World> world, byte[] bytes, int chunkVersion)
			throws IOException {
		final Function<String, net.kyori.adventure.key.Key> keyFunction = chunkVersion < 2
				? s -> net.kyori.adventure.key.Key.key(TAG_HELLBLOCK_DATA, StringUtils.toLowerCase(s))
				: net.kyori.adventure.key.Key::key;

		final DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(bytes));

		// Read chunk coordinate
		final int x = chunkData.readInt();
		final int z = chunkData.readInt();
		final ChunkPos coordinate = new ChunkPos(x, z);

		// Read loading info
		final int loadedSeconds = chunkData.readInt();
		final long lastLoadedTime = chunkData.readLong();

		// Read task queue
		final int tasksSize = chunkData.readInt();
		final PriorityBlockingQueue<DelayedTickTask> queue = new PriorityBlockingQueue<>(Math.max(11, tasksSize));
		for (int i = 0; i < tasksSize; i++) {
			final int time = chunkData.readInt();
			final BlockPos pos = new BlockPos(chunkData.readInt());
			queue.add(new DelayedTickTask(time, pos));
		}

		// Read ticked blocks
		final int tickedSize = chunkData.readInt();
		final Set<BlockPos> tickedSet = new HashSet<>(Math.max(11, tickedSize));
		for (int i = 0; i < tickedSize; i++) {
			tickedSet.add(new BlockPos(chunkData.readInt()));
		}

		// Read block sections
		final int sections = chunkData.readInt();
		final ConcurrentMap<Integer, CustomSection> sectionMap = new ConcurrentHashMap<>();

		for (int i = 0; i < sections; i++) {
			final int sectionID = chunkData.readInt();
			final byte[] sectionBytes = new byte[chunkData.readInt()];
			chunkData.readFully(sectionBytes);

			final DataInputStream sectionData = new DataInputStream(new ByteArrayInputStream(sectionBytes));
			final int blockAmount = sectionData.readInt();
			final ConcurrentMap<BlockPos, CustomBlockState> blockMap = new ConcurrentHashMap<>();

			for (int j = 0; j < blockAmount; j++) {
				final byte[] blockData = new byte[sectionData.readInt()];
				sectionData.readFully(blockData);
				final CompoundBinaryTag tag = readCompound(blockData);
				if (tag == null || tag.isEmpty()) {
					continue;
				}

				final String typeString = tag.getString("type");
				final net.kyori.adventure.key.Key key = keyFunction.apply(typeString);

				final BinaryTag posTag = tag.get("pos");
				if (!(posTag instanceof IntArrayBinaryTag posArray)) {
					continue;
				}

				final BinaryTag dataTag = tag.get("data");
				if (!(dataTag instanceof CompoundBinaryTag dataCompound)) {
					continue;
				}

				final CustomBlock customBlock = new CustomBlock(key);
				for (int pos : posArray.value()) {
					final BlockPos blockPos = new BlockPos(pos);
					blockMap.put(blockPos, CustomBlockStateInterface.create(customBlock, dataCompound));
				}
			}

			sectionMap.put(sectionID, CustomSectionInterface.restore(sectionID, blockMap));
		}

		return world.restoreChunk(coordinate, loadedSeconds, lastLoadedTime, sectionMap, queue, tickedSet);
	}

	private CompoundBinaryTag readCompound(byte[] bytes) throws IOException {
		if (bytes.length == 0) {
			return null;
		}

		try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
			final BinaryTag tag = BinaryTagIO.reader().read(input);
			if (tag instanceof CompoundBinaryTag compoundTag) {
				return compoundTag;
			} else {
				throw new IOException("Expected CompoundBinaryTag, got: " + tag.getClass().getSimpleName());
			}
		}
	}

	private CompoundBinaryTag toCompoundTag(SerializableSection section) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

		builder.putInt("sectionID", section.sectionID());

		final List<CompoundBinaryTag> blockTags = section.blocks();
		final ListBinaryTag blockList = ListBinaryTag.from(blockTags);

		builder.put("blocks", blockList);

		return builder.build();
	}
}