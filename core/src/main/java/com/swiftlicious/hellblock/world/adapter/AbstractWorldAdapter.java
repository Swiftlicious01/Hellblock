package com.swiftlicious.hellblock.world.adapter;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.StringTag;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.DelayedTickTask;
import com.swiftlicious.hellblock.world.HellblockBlockState;
import com.swiftlicious.hellblock.world.HellblockChunk;
import com.swiftlicious.hellblock.world.HellblockSection;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.SerializableSection;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public abstract class AbstractWorldAdapter<W> implements WorldAdapter<W> {

	public static final int CHUNK_VERSION = 2;
	public static final int REGION_VERSION = 1;

	private final Method decompressMethod;
	private final Method compressMethod;

	public AbstractWorldAdapter() {
		ClassLoader classLoader = HellblockPlugin.getInstance().getDependencyManager()
				.obtainClassLoaderWith(EnumSet.of(Dependency.ZSTD));
		try {
			Class<?> zstd = classLoader.loadClass("com.github.luben.zstd.Zstd");
			decompressMethod = zstd.getMethod("decompress", byte[].class, byte[].class);
			compressMethod = zstd.getMethod("compress", byte[].class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	protected void zstdDecompress(byte[] decompressedData, byte[] compressedData) {
		try {
			decompressMethod.invoke(null, decompressedData, compressedData);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		// Zstd.decompress(decompressedData, compressedData);
	}

	protected byte[] zstdCompress(byte[] data) {
		try {
			Object result = compressMethod.invoke(null, (Object) data);
			return (byte[]) result;
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		// return Zstd.compress(data);
	}

	@Override
	public int compareTo(@NotNull WorldAdapter<W> o) {
		return Integer.compare(o.priority(), this.priority());
	}

	protected SerializableChunk toSerializableChunk(HellblockChunk chunk) {
		ChunkPos chunkPos = chunk.chunkPos();
		return new SerializableChunk(chunkPos.x(), chunkPos.z(), chunk.loadedMilliSeconds(), chunk.lastLoadedTime(),
				chunk.sectionsToSave().map(this::toSerializableSection).toList(),
				queueToIntArray(chunk.tickTaskQueue()), tickedBlocksToArray(chunk.tickedBlocks()));
	}

	protected SerializableSection toSerializableSection(HellblockSection section) {
		return new SerializableSection(section.getSectionID(), toCompoundTags(section.blockMap()));
	}

	private List<CompoundTag> toCompoundTags(Map<BlockPos, HellblockBlockState> blocks) {
		List<CompoundTag> tags = new ArrayList<>(blocks.size());
		Map<HellblockBlockState, List<Integer>> blockToPosMap = new HashMap<>();
		for (Map.Entry<BlockPos, HellblockBlockState> entry : blocks.entrySet()) {
			BlockPos coordinate = entry.getKey();
			HellblockBlockState block = entry.getValue();
			List<Integer> coordinates = blockToPosMap.computeIfAbsent(block, k -> new ArrayList<>());
			coordinates.add(coordinate.position());
		}
		for (Map.Entry<HellblockBlockState, List<Integer>> entry : blockToPosMap.entrySet()) {
			tags.add(new CompoundTag("", toCompoundMap(entry.getKey(), entry.getValue())));
		}
		return tags;
	}

	private CompoundMap toCompoundMap(HellblockBlockState block, List<Integer> pos) {
		CompoundMap map = new CompoundMap();
		int[] result = new int[pos.size()];
		for (int i = 0; i < pos.size(); i++) {
			result[i] = pos.get(i);
		}
		map.put(new StringTag("type", block.type().type().asString()));
		map.put(new IntArrayTag("pos", result));
		map.put(new CompoundTag("data", block.compoundMap().originalMap()));
		return map;
	}

	private int[] tickedBlocksToArray(Set<BlockPos> set) {
		int[] ticked = new int[set.size()];
		int i = 0;
		for (BlockPos pos : set) {
			ticked[i] = pos.position();
			i++;
		}
		return ticked;
	}

	private int[] queueToIntArray(PriorityBlockingQueue<DelayedTickTask> queue) {
		int size = queue.size() * 2;
		int[] tasks = new int[size];
		int i = 0;
		for (DelayedTickTask task : queue) {
			tasks[i * 2] = task.getTime();
			tasks[i * 2 + 1] = task.blockPos().position();
			i++;
		}
		return tasks;
	}
}