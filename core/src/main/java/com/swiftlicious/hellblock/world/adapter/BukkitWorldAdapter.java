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
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.VoidGenerator;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.StringUtils;
import com.swiftlicious.hellblock.utils.TagUtils;
import com.swiftlicious.hellblock.utils.extras.Key;
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
	public World getWorld(String worldName) {
		return Bukkit.getWorld(worldName);
	}

	@Override
	public HellblockWorld<World> adapt(Object world) {
		return HellblockWorldInterface.create((World) world, this);
	}

	@Override
	public HellblockWorld<World> createWorld(String world) {
		World hellblockWorld = getWorld(world);
		if (hellblockWorld == null) {
			VoidGenerator voidGen = new VoidGenerator();
			hellblockWorld = WorldCreator.name(world).type(WorldType.FLAT).generateStructures(false)
					.environment(Environment.NETHER).generator(voidGen).biomeProvider(voidGen.new VoidBiomeProvider())
					.createWorld();
			HellblockPlugin.getInstance().debug(String.format("Created a new Hellblock World: %s", world));
		}
		HellblockWorld<World> adaptedWorld = adapt(hellblockWorld);
		HellblockPlugin.getInstance().getLavaRainHandler().startLavaRainProcess(adaptedWorld);
		return adaptedWorld;
	}

	@Override
	public void deleteWorld(String world) {
	}

	@Override
	public WorldExtraData loadExtraData(World world) {
		if (VersionHelper.isVersionNewerThan1_18()) {
			// init world basic info
			String json = world.getPersistentDataContainer().get(WORLD_DATA, PersistentDataType.STRING);
			WorldExtraData data = (json == null || json.equals("null")) ? WorldExtraData.empty()
					: AdventureHelper.getGson().serializer().fromJson(json, WorldExtraData.class);
			if (data == null)
				data = WorldExtraData.empty();
			return data;
		} else {
			File data = new File(getWorldFolder(world), DATA_FILE);
			if (data.exists()) {
				byte[] fileBytes = new byte[(int) data.length()];
				try (FileInputStream fis = new FileInputStream(data)) {
					fis.read(fileBytes);
				} catch (IOException e) {
					HellblockPlugin.getInstance().getPluginLogger().severe(
							"[" + world.getName() + "] Failed to load extra data from " + data.getAbsolutePath(), e);
				}
				String jsonContent = new String(fileBytes, StandardCharsets.UTF_8);
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
			File data = new File(getWorldFolder(world.world()), DATA_FILE);
			File parentDir = data.getParentFile();
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
	public HellblockRegion loadRegion(HellblockWorld<World> world, RegionPos pos, boolean createIfNotExist) {
		File data = getRegionDataFile(world.world(), pos);
		// if the data file not exists
		if (!data.exists()) {
			return createIfNotExist ? world.createRegion(pos) : null;
		} else {
			// load region from local files
			try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(data))) {
				DataInputStream dataStream = new DataInputStream(bis);
				HellblockRegion region = deserializeRegion(world, dataStream, pos);
				dataStream.close();
				return region;
			} catch (Exception e) {
				HellblockPlugin.getInstance().getPluginLogger().severe("[" + world.worldName()
						+ "] Failed to load Hellblock region data at " + pos + ". Deleting the corrupted region.", e);
				boolean success = data.delete();
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
	public HellblockChunk loadChunk(HellblockWorld<World> world, ChunkPos pos, boolean createIfNotExist) {
		HellblockRegion region = world.getOrCreateRegion(pos.toRegionPos());
		// In order to reduce frequent disk reads to determine whether a region exists,
		// we read the region into the cache
		if (!region.isLoaded()) {
			region.load();
		}
		byte[] bytes = region.getCachedChunkBytes(pos);
		if (bytes == null) {
			return createIfNotExist ? world.createChunk(pos) : null;
		} else {
			try {
				long time1 = System.currentTimeMillis();
				DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(bytes));
				HellblockChunk chunk = deserializeChunk(world, dataStream);
				dataStream.close();
				long time2 = System.currentTimeMillis();
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
	public void saveRegion(HellblockWorld<World> world, HellblockRegion region) {
		File file = getRegionDataFile(world.world(), region.regionPos());
		if (region.canPrune()) {
			if (file.exists()) {
				file.delete();
			}
			return;
		}
		long time1 = System.currentTimeMillis();
		File parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		try (FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos)) {
			bos.write(serializeRegion(region));
			long time2 = System.currentTimeMillis();
			HellblockPlugin.getInstance().debug(() -> "[" + world.worldName() + "] Took " + (time2 - time1)
					+ "ms to save region " + region.regionPos());
		} catch (IOException e) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"[" + world.worldName() + "] Failed to save Hellblock region data." + region.regionPos(), e);
		}
	}

	@Override
	public void saveChunk(HellblockWorld<World> world, HellblockChunk chunk) {
		RegionPos pos = chunk.chunkPos().toRegionPos();
		Optional<HellblockRegion> region = world.getLoadedRegion(pos);
		if (region.isEmpty()) {
			HellblockPlugin.getInstance().getPluginLogger().severe("[" + world.worldName() + "] Region " + pos
					+ " unloaded before chunk " + chunk.chunkPos() + " saving.");
		} else {
			HellblockRegion hellblockRegion = region.get();
			SerializableChunk serializableChunk = toSerializableChunk(chunk);
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

	private HellblockRegion deserializeRegion(HellblockWorld<World> world, DataInputStream dataStream, RegionPos pos)
			throws IOException {
		ConcurrentHashMap<ChunkPos, byte[]> map = new ConcurrentHashMap<>();
		int chunkAmount = dataStream.readInt();
		for (int i = 0; i < chunkAmount; i++) {
			int chunkX = dataStream.readInt();
			int chunkZ = dataStream.readInt();
			ChunkPos chunkPos = ChunkPos.of(chunkX, chunkZ);
			byte[] chunkData = new byte[dataStream.readInt()];
			dataStream.read(chunkData);
			map.put(chunkPos, chunkData);
		}
		return world.restoreRegion(pos, map);
	}

	private byte[] serializeRegion(HellblockRegion region) {
		ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		DataOutputStream outStream = new DataOutputStream(outByteStream);
		try {
			outStream.writeByte(REGION_VERSION);
			outStream.writeInt(region.regionPos().x());
			outStream.writeInt(region.regionPos().z());
			Map<ChunkPos, byte[]> map = region.dataToSave();
			outStream.writeInt(map.size());
			for (Map.Entry<ChunkPos, byte[]> entry : map.entrySet()) {
				outStream.writeInt(entry.getKey().x());
				outStream.writeInt(entry.getKey().z());
				byte[] dataArray = entry.getValue();
				outStream.writeInt(dataArray.length);
				outStream.write(dataArray);
			}
		} catch (IOException e) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("Failed to serialize Hellblock region data." + region.regionPos(), e);
		}
		return outByteStream.toByteArray();
	}

	private HellblockChunk deserializeChunk(HellblockWorld<World> world, DataInputStream dataStream)
			throws IOException {
		int chunkVersion = dataStream.readByte();
		byte[] blockData = readCompressedBytes(dataStream);
		return deserializeChunk(world, blockData, chunkVersion);
	}

	private byte[] readCompressedBytes(DataInputStream dataStream) throws IOException {
		int compressedLength = dataStream.readInt();
		int decompressedLength = dataStream.readInt();
		byte[] compressedData = new byte[compressedLength];
		byte[] decompressedData = new byte[decompressedLength];

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
		ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		DataOutputStream outStream = new DataOutputStream(outByteStream);
		try {
			outStream.writeByte(CHUNK_VERSION);
			byte[] serializedSections = toBytes(serializableChunk);
			byte[] compressed = zstdCompress(serializedSections);
			outStream.writeInt(compressed.length);
			outStream.writeInt(serializedSections.length);
			outStream.write(compressed);
		} catch (IOException e) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("Failed to serialize chunk " + ChunkPos.of(serializableChunk.x(), serializableChunk.z()));
		}
		return outByteStream.toByteArray();
	}

	private byte[] toBytes(SerializableChunk chunk) throws IOException {
		ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
		DataOutputStream outStream = new DataOutputStream(outByteStream);
		outStream.writeInt(chunk.x());
		outStream.writeInt(chunk.z());
		outStream.writeInt(chunk.loadedSeconds());
		outStream.writeLong(chunk.lastLoadedTime());
		// write queue
		int[] queue = chunk.queuedTasks();
		outStream.writeInt(queue.length / 2);
		for (int i : queue) {
			outStream.writeInt(i);
		}
		// write ticked blocks
		int[] tickedSet = chunk.ticked();
		outStream.writeInt(tickedSet.length);
		for (int i : tickedSet) {
			outStream.writeInt(i);
		}
		// write block data
		List<SerializableSection> sectionsToSave = chunk.sections();
		outStream.writeInt(sectionsToSave.size());
		for (SerializableSection section : sectionsToSave) {
			outStream.writeInt(section.sectionID());
			byte[] blockData = toBytes(section.blocks());
			outStream.writeInt(blockData.length);
			outStream.write(blockData);
		}
		return outByteStream.toByteArray();
	}

	private byte[] toBytes(Collection<CompoundTag> blocks) throws IOException {
		ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
		DataOutputStream outStream = new DataOutputStream(outByteStream);
		outStream.writeInt(blocks.size());
		for (CompoundTag block : blocks) {
			byte[] blockData = toBytes(block);
			outStream.writeInt(blockData.length);
			outStream.write(blockData);
		}
		return outByteStream.toByteArray();
	}

	private byte[] toBytes(CompoundTag tag) throws IOException {
		if (tag == null || tag.getValue().isEmpty())
			return new byte[0];
		ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
		try (NBTOutputStream outStream = new NBTOutputStream(outByteStream, NBTInputStream.NO_COMPRESSION,
				ByteOrder.BIG_ENDIAN)) {
			outStream.writeTag(tag);
			return outByteStream.toByteArray();
		}
	}

	@SuppressWarnings("all")
	private HellblockChunk deserializeChunk(HellblockWorld world, byte[] bytes, int chunkVersion) throws IOException {
		Function<String, Key> keyFunction = chunkVersion < 2 ? (s) -> {
			return Key.of("hellblock", StringUtils.toLowerCase(s));
		} : s -> {
			return Key.of(s);
		};
		DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(bytes));
		// read coordinate
		int x = chunkData.readInt();
		int z = chunkData.readInt();
		ChunkPos coordinate = new ChunkPos(x, z);
		// read loading info
		int loadedSeconds = chunkData.readInt();
		long lastLoadedTime = chunkData.readLong();
		// read task queue
		int tasksSize = chunkData.readInt();
		PriorityBlockingQueue<DelayedTickTask> queue = new PriorityBlockingQueue<>(Math.max(11, tasksSize));
		for (int i = 0; i < tasksSize; i++) {
			int time = chunkData.readInt();
			BlockPos pos = new BlockPos(chunkData.readInt());
			queue.add(new DelayedTickTask(time, pos));
		}
		// read ticked blocks
		int tickedSize = chunkData.readInt();
		Set<BlockPos> tickedSet = new HashSet<>(Math.max(11, tickedSize));
		for (int i = 0; i < tickedSize; i++) {
			tickedSet.add(new BlockPos(chunkData.readInt()));
		}
		// read block data
		ConcurrentMap<Integer, HellblockSection> sectionMap = new ConcurrentHashMap<>();
		int sections = chunkData.readInt();
		// read sections
		for (int i = 0; i < sections; i++) {
			ConcurrentMap<BlockPos, HellblockBlockState> blockMap = new ConcurrentHashMap<>();
			int sectionID = chunkData.readInt();
			byte[] sectionBytes = new byte[chunkData.readInt()];
			chunkData.read(sectionBytes);
			DataInputStream sectionData = new DataInputStream(new ByteArrayInputStream(sectionBytes));
			int blockAmount = sectionData.readInt();
			// read blocks
			for (int j = 0; j < blockAmount; j++) {
				byte[] blockData = new byte[sectionData.readInt()];
				sectionData.read(blockData);
				CompoundTag tag = readCompound(blockData);
				CompoundMap block = tag.getValue();
				Key key = keyFunction.apply((String) block.get("type").getValue());
				CompoundMap data = (CompoundMap) block.get("data").getValue();
				HellblockBlock customBlock = new HellblockBlock(key);
				if (customBlock == null) {
					HellblockPlugin.getInstance().getInstance().getPluginLogger().warn("[" + world.worldName()
							+ "] Unrecognized block " + key + " has been removed from chunk " + ChunkPos.of(x, z));
					continue;
				}
				for (int pos : (int[]) block.get("pos").getValue()) {
					BlockPos blockPos = new BlockPos(pos);
					blockMap.put(blockPos, HellblockBlockStateInterface.create(customBlock, TagUtils.deepClone(data)));
				}
			}
			sectionMap.put(sectionID, HellblockSectionInterface.restore(sectionID, blockMap));
		}
		return world.restoreChunk(coordinate, loadedSeconds, lastLoadedTime, sectionMap, queue, tickedSet);
	}

	private CompoundTag readCompound(byte[] bytes) throws IOException {
		if (bytes.length == 0)
			return null;
		try (NBTInputStream nbtInputStream = new NBTInputStream(new ByteArrayInputStream(bytes),
				NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN)) {
			return (CompoundTag) nbtInputStream.readTag();
		}
	}
}