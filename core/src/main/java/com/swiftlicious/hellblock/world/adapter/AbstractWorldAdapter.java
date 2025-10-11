package com.swiftlicious.hellblock.world.adapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.Dependency;
import com.swiftlicious.hellblock.world.BlockPos;
import com.swiftlicious.hellblock.world.ChunkPos;
import com.swiftlicious.hellblock.world.CustomBlockState;
import com.swiftlicious.hellblock.world.CustomChunk;
import com.swiftlicious.hellblock.world.CustomSection;
import com.swiftlicious.hellblock.world.DelayedTickTask;
import com.swiftlicious.hellblock.world.SerializableChunk;
import com.swiftlicious.hellblock.world.SerializableSection;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;

/**
 * An abstract implementation of WorldAdapter that provides common functionality
 * for handling world data serialization and compression.
 *
 * @param <W> the type of world this adapter handles
 */
public abstract class AbstractWorldAdapter<W> implements WorldAdapter<W> {

	public static final int CHUNK_VERSION = 2;
	public static final int REGION_VERSION = 1;

	private final Method decompressMethod;
	private final Method compressMethod;

	public AbstractWorldAdapter() {
		final ClassLoader classLoader = HellblockPlugin.getInstance().getDependencyManager()
				.obtainClassLoaderWith(EnumSet.of(Dependency.ZSTD));
		try {
			final Class<?> zstd = classLoader.loadClass("com.github.luben.zstd.Zstd");
			decompressMethod = zstd.getMethod("decompress", byte[].class, byte[].class);
			compressMethod = zstd.getMethod("compress", byte[].class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decompress data using Zstandard compression.
	 * 
	 * @param decompressedData the buffer to hold the decompressed data
	 * @param compressedData   the compressed data
	 */
	protected void zstdDecompress(byte[] decompressedData, byte[] compressedData) {
		try {
			decompressMethod.invoke(null, decompressedData, compressedData);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		// Zstd.decompress(decompressedData, compressedData);
	}

	/**
	 * Compress data using Zstandard compression.
	 * 
	 * @param data the data to compress
	 * @return the compressed data
	 */
	protected byte[] zstdCompress(byte[] data) {
		try {
			final Object result = compressMethod.invoke(null, (Object) data);
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

	/**
	 * Convert a CustomChunk to a SerializableChunk.
	 * 
	 * @param chunk the CustomChunk
	 * @return a SerializableChunk representing the chunk
	 */
	protected SerializableChunk toSerializableChunk(CustomChunk chunk) {
		final ChunkPos chunkPos = chunk.chunkPos();
		return new SerializableChunk(chunkPos.x(), chunkPos.z(), chunk.loadedMilliSeconds(), chunk.lastLoadedTime(),
				chunk.sectionsToSave().map(this::toSerializableSection).toList(),
				queueToIntArray(chunk.tickTaskQueue()), tickedBlocksToArray(chunk.tickedBlocks()));
	}

	/**
	 * Convert a CustomSection to a SerializableSection.
	 * 
	 * @param section the CustomSection
	 * @return a SerializableSection representing the section
	 */
	protected SerializableSection toSerializableSection(CustomSection section) {
		return new SerializableSection(section.getSectionID(), toCompoundTags(section.blockMap()));
	}

	/**
	 * Convert a map of BlockPos to CustomBlockState to a list of CompoundBinaryTag.
	 * 
	 * @param blocks the map of BlockPos to CustomBlockState
	 * @return a list of CompoundBinaryTag representing the block states and
	 *         positions
	 */
	private List<CompoundBinaryTag> toCompoundTags(Map<BlockPos, CustomBlockState> blocks) {
		final List<CompoundBinaryTag> tags = new ArrayList<>(blocks.size());
		final Map<CustomBlockState, List<Integer>> blockToPosMap = new HashMap<>();

		// Group by unique block state
		blocks.entrySet().forEach(entry -> {
			final BlockPos coordinate = entry.getKey();
			final CustomBlockState block = entry.getValue();
			blockToPosMap.computeIfAbsent(block, k -> new ArrayList<>()).add(coordinate.position());
		});

		// Serialize each unique block state with its position list
		blockToPosMap.entrySet().forEach(entry -> {
			final CustomBlockState block = entry.getKey();
			final List<Integer> positions = entry.getValue();
			final CompoundBinaryTag tag = toCompoundTag(block, positions);
			tags.add(tag);
		});

		return tags;
	}

	/**
	 * Convert a CustomBlockState and a list of positions to a CompoundBinaryTag.
	 * 
	 * @param block     the CustomBlockState
	 * @param positions the list of block positions
	 * @return a CompoundBinaryTag representing the block state and positions
	 */
	private CompoundBinaryTag toCompoundTag(CustomBlockState block, List<Integer> positions) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

		// Convert position list to int[]
		final int[] posArray = positions.stream().mapToInt(Integer::intValue).toArray();

		// Add type, pos[], and data
		builder.putString("type", block.type().type().asString());
		builder.put("pos", IntArrayBinaryTag.intArrayBinaryTag(posArray));
		builder.put("data", block.compound().original());

		return builder.build();
	}

	/**
	 * Convert a set of BlockPos to an int array of their positions.
	 * 
	 * @param set the set of BlockPos
	 * @return an int array of block positions
	 */
	private int[] tickedBlocksToArray(Set<BlockPos> set) {
		final int[] ticked = new int[set.size()];
		int i = 0;
		for (BlockPos pos : set) {
			ticked[i] = pos.position();
			i++;
		}
		return ticked;
	}

	/**
	 * Convert a PriorityBlockingQueue of DelayedTickTask to an int array of their
	 * times and positions.
	 * 
	 * @param queue the PriorityBlockingQueue of DelayedTickTask
	 * @return an int array of times and block positions
	 */
	private int[] queueToIntArray(PriorityBlockingQueue<DelayedTickTask> queue) {
		final int size = queue.size() * 2;
		final int[] tasks = new int[size];
		int i = 0;
		for (DelayedTickTask task : queue) {
			tasks[i * 2] = task.getTime();
			tasks[i * 2 + 1] = task.blockPos().position();
			i++;
		}
		return tasks;
	}
}