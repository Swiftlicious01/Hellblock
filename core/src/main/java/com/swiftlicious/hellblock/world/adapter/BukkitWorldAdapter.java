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
import java.io.FileWriter;
import java.io.IOException;
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
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.VoidGenerator;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
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
import com.swiftlicious.hellblock.world.RegionPos;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.SerializableSection;
import com.swiftlicious.hellblock.world.WorldExtraData;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

/**
 * An implementation of AbstractWorldAdapter for Bukkit/Spigot/Paper servers.
 */
public class BukkitWorldAdapter extends AbstractWorldAdapter<World> {

	private static BiFunction<World, RegionPos, File> regionFileProvider;
	private static Function<World, File> worldFolderProvider;
	private static final NamespacedKey WORLD_DATA = new NamespacedKey(HellblockPlugin.getInstance(), "data");
	private static final String DATA_FILE = "hellblock.dat";

	public BukkitWorldAdapter() {
		worldFolderProvider = (world -> {
			if (HellblockPlugin.getInstance().getConfigManager().absoluteWorldPath().isEmpty()) {
				return world.getWorldFolder();
			} else {
				return new File(HellblockPlugin.getInstance().getConfigManager().absoluteWorldPath(), world.getName());
			}
		});
		regionFileProvider = (world, pos) -> new File(worldFolderProvider.apply(world),
				"hellblock" + File.separator + getRegionDataFile(pos));
	}

	public static void regionFileProvider(BiFunction<World, RegionPos, File> regionFileProvider) {
		BukkitWorldAdapter.regionFileProvider = regionFileProvider;
	}

	public static void worldFolderProvider(Function<World, File> worldFolderProvider) {
		BukkitWorldAdapter.worldFolderProvider = worldFolderProvider;
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
			HellblockPlugin.getInstance().getWorldManager().markWorldAccess(worldName);
			return adapt(bukkitOpt.get());
		}

		return null;
	}

	@Override
	public HellblockWorld<World> adapt(Object world) {
		if (!(world instanceof World)) {
			throw new IllegalArgumentException("Expected Bukkit World, but got: " + world.getClass().getName());
		}
		return CustomWorldInterface.create((World) world, this);
	}

	@Override
	public CompletableFuture<HellblockWorld<World>> createWorld(String worldName) {
		CompletableFuture<HellblockWorld<World>> future = new CompletableFuture<>();

		// Ensure the world creation and loading is performed on the main thread
		HellblockPlugin.getInstance().getScheduler().executeSync(() -> {
			World hellblockWorld = getWorld(worldName).orElse(null);
			if (hellblockWorld == null) {
				final VoidGenerator voidGen = new VoidGenerator();
				hellblockWorld = WorldCreator.name(worldName).type(WorldType.FLAT).generateStructures(false)
						.environment(Environment.NETHER).generator(voidGen)
						.biomeProvider(voidGen.new VoidBiomeProvider()).createWorld();
				HellblockPlugin.getInstance().debug("Created a new Hellblock World: %s".formatted(worldName));
			}
			final HellblockWorld<World> adaptedWorld = adapt(hellblockWorld);
			HellblockPlugin.getInstance().getLavaRainHandler().startLavaRainProcess(adaptedWorld);
			future.complete(adaptedWorld);
		});

		return future;
	}

	@Override
	public CompletableFuture<HellblockWorld<World>> getOrLoadIslandWorld(int islandId) {
		String worldName = HellblockPlugin.getInstance().getWorldManager().getHellblockWorldFormat(islandId);

		// Attempt to get already-loaded world
		HellblockWorld<World> existing = getLoadedHellblockWorld(worldName);
		if (existing != null) {
			return CompletableFuture.completedFuture(existing);
		}

		// Otherwise, load or create it
		return createWorld(worldName);
	}

	@Override
	public void deleteWorld(String world) {
		World bukkitWorld = getWorld(world).orElse(null);
		if (bukkitWorld == null) {
			return;
		}
		HellblockPlugin.getInstance().getLavaRainHandler().stopLavaRainProcess(bukkitWorld.getName());
		Bukkit.unloadWorld(bukkitWorld, false);
		final File worldFolder = worldFolderProvider.apply(bukkitWorld);
		if (worldFolder.exists() && worldFolder.isDirectory()) {
			try {
				FileUtils.deleteDirectory(worldFolder.toPath());
				HellblockPlugin.getInstance().debug("Deleted Hellblock World: %s".formatted(world));
			} catch (IOException e) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("Failed to delete Hellblock world folder: " + worldFolder.getAbsolutePath(), e);
			}
		}
	}

	@Override
	public WorldExtraData loadExtraData(World world) {
		if (VersionHelper.isVersionNewerThan1_18()) {
			// init world basic info
			final String json = world.getPersistentDataContainer().get(WORLD_DATA, PersistentDataType.STRING);
			WorldExtraData data = (json == null || "null".equals(json)) ? WorldExtraData.empty()
					: AdventureHelper.getGson().serializer().fromJson(json, WorldExtraData.class);
			if (data == null) {
				data = WorldExtraData.empty();
			}
			return data;
		} else {
			final File data = new File(getWorldFolder(world), DATA_FILE);
			if (data.exists()) {
				final byte[] fileBytes = new byte[(int) data.length()];
				try (FileInputStream fis = new FileInputStream(data)) {
					fis.read(fileBytes);
				} catch (IOException e) {
					HellblockPlugin.getInstance().getPluginLogger().severe(
							"[" + world.getName() + "] Failed to load extra data from " + data.getAbsolutePath(), e);
				}
				final String jsonContent = new String(fileBytes, StandardCharsets.UTF_8);
				return AdventureHelper.getGson().serializer().fromJson(jsonContent, WorldExtraData.class);
			} else {
				return WorldExtraData.empty();
			}
		}
	}

	@Override
	public void saveExtraData(HellblockWorld<World> world) {
		if (VersionHelper.isVersionNewerThan1_18()) {
			world.world().getPersistentDataContainer().set(WORLD_DATA, PersistentDataType.STRING,
					AdventureHelper.getGson().serializer().toJson(world.extraData()));
		} else {
			final File data = new File(getWorldFolder(world.world()), DATA_FILE);
			final File parentDir = data.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}
			try (FileWriter file = new FileWriter(data)) {
				AdventureHelper.getGson().serializer().toJson(world.extraData(), file);
			} catch (IOException e) {
				HellblockPlugin.getInstance().getPluginLogger().severe(
						"[" + world.worldName() + "] Failed to save extra data to " + data.getAbsolutePath(), e);
			}
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
				HellblockPlugin.getInstance().getPluginLogger().severe("[" + world.worldName()
						+ "] Failed to load Hellblock region data at " + pos + ". Deleting the corrupted region.", e);
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
		if (bytes == null) {
			return createIfNotExist ? world.createChunk(pos) : null;
		} else {
			try {
				final long time1 = System.currentTimeMillis();
				final DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(bytes));
				final CustomChunk chunk = deserializeChunk(world, dataStream);
				dataStream.close();
				final long time2 = System.currentTimeMillis();
				HellblockPlugin.getInstance().debug(() -> "[" + world.worldName() + "] Took " + (time2 - time1)
						+ "ms to load chunk " + pos + " from cached region");
				return chunk;
			} catch (IOException e) {
				HellblockPlugin.getInstance().getPluginLogger()
						.severe("[" + world.worldName() + "] Failed to load Hellblock data at " + pos, e);
				region.removeCachedChunk(pos);
				return createIfNotExist ? world.createChunk(pos) : null;
			}
		}
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
			HellblockPlugin.getInstance().debug(() -> "[" + world.worldName() + "] Took " + (time2 - time1)
					+ "ms to save region " + region.regionPos());
		} catch (IOException e) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"[" + world.worldName() + "] Failed to save Hellblock region data." + region.regionPos(), e);
		}
	}

	@Override
	public void saveChunk(HellblockWorld<World> world, CustomChunk chunk) {
		final RegionPos pos = chunk.chunkPos().toRegionPos();
		final Optional<CustomRegion> region = world.getLoadedRegion(pos);
		if (region.isEmpty()) {
			HellblockPlugin.getInstance().getPluginLogger().severe("[" + world.worldName() + "] Region " + pos
					+ " unloaded before chunk " + chunk.chunkPos() + " saving.");
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
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("Failed to serialize Hellblock region data." + region.regionPos(), e);
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
			HellblockPlugin.getInstance().getPluginLogger().severe("Failed to serialize chunk "
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
				? s -> net.kyori.adventure.key.Key.key("hellblock", StringUtils.toLowerCase(s))
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