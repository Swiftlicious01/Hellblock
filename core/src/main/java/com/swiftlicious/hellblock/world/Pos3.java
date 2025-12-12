package com.swiftlicious.hellblock.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a 3-dimensional position (x, y, z) in a Minecraft world.
 */
public record Pos3(int x, int y, int z) {

	/**
	 * Checks if this position is equal to another object.
	 *
	 * @param obj The object to compare with.
	 * @return true if the object is a Pos3 with the same coordinates, false
	 *         otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Pos3 other = (Pos3) obj;
		if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
			return false;
		}
		if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
			return false;
		}
		if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
			return false;
		}
		return true;
	}

	/**
	 * Computes a hash code for this position.
	 *
	 * @return A hash code representing this Pos3.
	 */
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.x));
		hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.y));
		hash = 19 * hash + Long.hashCode(Double.doubleToLongBits(this.z));
		return hash;
	}

	/**
	 * Converts a Bukkit {@link Location} to a Pos3 instance.
	 *
	 * @param location The Bukkit location to convert.
	 * @return A new Pos3 instance representing the block coordinates of the
	 *         location.
	 */
	public static Pos3 from(Location location) {
		return new Pos3(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	/**
	 * Converts the minimum X and Z coordinates of a BoundingBox to a Pos3, using a
	 * specified minimum Y-coordinate.
	 *
	 * @param box  the BoundingBox to convert
	 * @param yMin the Y-coordinate to assign (typically world min height)
	 * @return a Pos3 representing the lower corner (min X, Y, min Z) of the
	 *         bounding box
	 */
	public static Pos3 toMinPos3(BoundingBox box, int yMin) {
		return new Pos3((int) Math.floor(box.getMinX()), yMin, (int) Math.floor(box.getMinZ()));
	}

	/**
	 * Converts the maximum X and Z coordinates of a BoundingBox to a Pos3, using a
	 * specified maximum Y-coordinate.
	 *
	 * @param box  the BoundingBox to convert
	 * @param yMax the Y-coordinate to assign (typically world max height)
	 * @return a Pos3 representing the upper corner (max X, Y, max Z) of the
	 *         bounding box
	 */
	public static Pos3 toMaxPos3(BoundingBox box, int yMax) {
		return new Pos3((int) Math.ceil(box.getMaxX()), yMax, (int) Math.ceil(box.getMaxZ()));
	}

	/**
	 * Converts a {@link BlockPos} into a {@link Pos3} instance.
	 *
	 * <p>
	 * This is typically used when converting from chunk/block-based coordinates
	 * (like those used in storage or logic) into the generic 3D coordinate wrapper
	 * {@code Pos3} used by the custom world system.
	 * </p>
	 *
	 * @param pos the {@link BlockPos} to convert (must not be null)
	 * @return a new {@link Pos3} representing the same x, y, z coordinates
	 */
	public static Pos3 fromBlockPos(@NotNull BlockPos pos) {
		return new Pos3(pos.x(), pos.y(), pos.z());
	}

	/**
	 * Returns a new {@code Pos3} that is one block higher on the Y-axis.
	 * <p>
	 * This is useful for checking the block above a given position, such as when
	 * determining valid spawn locations or placing blocks vertically.
	 *
	 * @return a new {@code Pos3} instance shifted up by 1 block.
	 */
	public Pos3 up() {
		return new Pos3(this.x, this.y + 1, this.z);
	}

	/**
	 * Returns a new {@code Pos3} that is x amount of blocks higher on the Y-axis.
	 * <p>
	 * This is useful for checking the blocks above a given position, such as when
	 * determining valid spawn locations or placing blocks vertically.
	 *
	 * @return a new {@code Pos3} instance shifted up by x blocks.
	 */
	public Pos3 up(int upValue) {
		return new Pos3(this.x, this.y + upValue, this.z);
	}

	/**
	 * Returns a new {@code Pos3} that is one block lower on the Y-axis.
	 * <p>
	 * This is useful for checking the block below a given position, such as when
	 * determining valid spawn locations or placing blocks vertically.
	 *
	 * @return a new {@code Pos3} instance shifted down by 1 block.
	 */
	public Pos3 down() {
		return new Pos3(this.x, this.y - 1, this.z);
	}

	/**
	 * Returns a new {@code Pos3} that is x amount of blocks lower on the Y-axis.
	 * <p>
	 * This is useful for checking the blocks below a given position, such as when
	 * determining valid spawn locations or placing blocks vertically.
	 *
	 * @return a new {@code Pos3} instance shifted down by x blocks.
	 */
	public Pos3 down(int downValue) {
		return new Pos3(this.x, this.y - downValue, this.z);
	}

	/**
	 * Returns a new {@link Pos3} offset by one unit in the direction of the given
	 * {@link BlockFace}.
	 * <p>
	 * Only supports cardinal directions (NORTH, SOUTH, EAST, WEST) and vertical
	 * directions (UP, DOWN). For unsupported or diagonal faces, the current
	 * position is returned unchanged.
	 *
	 * @param face the direction to offset in
	 * @return a new Pos3 one block away in the specified direction
	 */
	public Pos3 offset(@NotNull BlockFace face) {
		return switch (face) {
		case NORTH -> new Pos3(x, y, z - 1);
		case SOUTH -> new Pos3(x, y, z + 1);
		case EAST -> new Pos3(x + 1, y, z);
		case WEST -> new Pos3(x - 1, y, z);
		case UP -> new Pos3(x, y + 1, z);
		case DOWN -> new Pos3(x, y - 1, z);
		default -> this;
		};
	}

	/**
	 * Converts this Pos3 instance to a Bukkit {@link Location}.
	 *
	 * @param world The Bukkit world to associate with the location.
	 * @return A new Location instance with this Pos3's coordinates in the specified
	 *         world.
	 */
	public Location toLocation(World world) {
		return new Location(world, x, y, z);
	}

	/**
	 * Adds the specified values to this position's coordinates and returns a new
	 * Pos3 instance.
	 *
	 * @param x The amount to add to the x-coordinate.
	 * @param y The amount to add to the y-coordinate.
	 * @param z The amount to add to the z-coordinate.
	 * @return A new Pos3 instance with updated coordinates.
	 */
	public Pos3 add(int x, int y, int z) {
		return new Pos3(this.x + x, this.y + y, this.z + z);
	}

	/**
	 * Converts this Pos3 instance to a {@link ChunkPos}, representing the chunk
	 * coordinates of this position.
	 *
	 * @return The {@link ChunkPos} containing this Pos3.
	 */
	public ChunkPos toChunkPos() {
		return ChunkPos.fromPos3(this);
	}

	/**
	 * Calculates the chunk x-coordinate of this position.
	 *
	 * @return The chunk x-coordinate.
	 */
	public int chunkX() {
		return (int) Math.floor((double) this.x() / 16.0);
	}

	/**
	 * Calculates the chunk z-coordinate of this position.
	 *
	 * @return The chunk z-coordinate.
	 */
	public int chunkZ() {
		return (int) Math.floor((double) this.z() / 16.0);
	}

	/**
	 * Returns a string representation of this Pos3 instance for debugging and
	 * logging.
	 *
	 * @return A string in the format "Pos3{x=..., y=..., z=...}".
	 */
	@Override
	public String toString() {
		return "Pos3{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
	}
}