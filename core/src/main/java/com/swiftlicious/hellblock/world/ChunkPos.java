package com.swiftlicious.hellblock.world;

import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the position of a chunk in a Minecraft world using chunk
 * coordinates (x, z).
 */
public record ChunkPos(int x, int z) {

	/**
	 * Creates a new ChunkPos instance with the specified chunk coordinates.
	 *
	 * @param x The x-coordinate of the chunk.
	 * @param z The z-coordinate of the chunk.
	 * @return A new ChunkPos instance.
	 */
	public static ChunkPos of(int x, int z) {
		return new ChunkPos(x, z);
	}

	/**
	 * Adds another {@link ChunkPos} to this one, component-wise.
	 *
	 * <p>
	 * This is used when applying a chunk-based offset to migrate positions between
	 * different world layouts (e.g., shifting per-player world chunks into a shared
	 * world).
	 * </p>
	 *
	 * @param other the chunk offset to apply
	 * @return a new {@link ChunkPos} representing the summed coordinates
	 */
	public ChunkPos add(@NotNull ChunkPos other) {
		return ChunkPos.of(this.x() + other.x(), this.z() + other.z());
	}

	/**
	 * Converts this chunk position to a block-space offset by multiplying the chunk
	 * coordinates by 16 (chunk size).
	 *
	 * <p>
	 * This is useful when determining the base block coordinate of a chunk, such as
	 * the origin of a region being copied.
	 * </p>
	 *
	 * @return a {@link BlockPos} representing the base (min) block coordinate of
	 *         this chunk
	 */
	public BlockPos toBlockOffset() {
		return new BlockPos(this.x() << 4, 0, this.z() << 4); // y defaults to 0
	}

	/**
	 * Parses a ChunkPos from a string representation in the format "x,z".
	 *
	 * @param coordinate The string representation of the chunk coordinates.
	 * @return A new ChunkPos instance parsed from the string.
	 * @throws RuntimeException If the string cannot be parsed into valid
	 *                          coordinates.
	 */
	public static ChunkPos fromString(String coordinate) {
		final String[] split = coordinate.split(",", 2);
		try {
			final int x = Integer.parseInt(split[0]);
			final int z = Integer.parseInt(split[1]);
			return new ChunkPos(x, z);
		} catch (NumberFormatException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a ChunkPos from a Pos3 object by calculating which chunk the position
	 * falls into.
	 *
	 * @param pos3 The Pos3 object representing a 3D position.
	 * @return A ChunkPos representing the chunk containing the provided position.
	 */
	public static ChunkPos fromPos3(Pos3 pos3) {
		final int chunkX = (int) Math.floor((double) pos3.x() / 16.0);
		final int chunkZ = (int) Math.floor((double) pos3.z() / 16.0);
		return ChunkPos.of(chunkX, chunkZ);
	}

	/**
	 * Computes a hash code for this ChunkPos.
	 *
	 * @return A hash code representing this ChunkPos.
	 */
	@Override
	public int hashCode() {
		final long combined = (long) x << 32 | z;
		return Long.hashCode(combined);
	}

	/**
	 * Checks if this ChunkPos is equal to another object.
	 *
	 * @param obj The object to compare.
	 * @return true if the object is a ChunkPos with the same x and z coordinates,
	 *         false otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ChunkPos other = (ChunkPos) obj;
		if (this.x != other.x) {
			return false;
		}
		return this.z != other.z ? false : true;
	}

	/**
	 * Converts this ChunkPos to a RegionPos, which represents a larger area
	 * containing multiple chunks.
	 *
	 * @return A RegionPos representing the region containing this chunk.
	 */
	@NotNull
	public RegionPos toRegionPos() {
		return RegionPos.getByChunkPos(this);
	}

	/**
	 * Creates a ChunkPos from a Bukkit Chunk object.
	 *
	 * @param chunk The Bukkit Chunk object.
	 * @return A ChunkPos representing the chunk's coordinates.
	 */
	@NotNull
	public static ChunkPos fromBukkitChunk(@NotNull Chunk chunk) {
		return new ChunkPos(chunk.getX(), chunk.getZ());
	}

	/**
	 * Converts this ChunkPos to a string representation in the format "x,z".
	 *
	 * @return A string representing this ChunkPos.
	 */
	public String asString() {
		return x + "," + z;
	}

	/**
	 * Returns a string representation of this ChunkPos for debugging and logging
	 * purposes.
	 *
	 * @return A string in the format "ChunkPos{x=..., z=...}".
	 */
	@Override
	public String toString() {
		return "ChunkPos{" + "x=" + x + ", z=" + z + '}';
	}
}