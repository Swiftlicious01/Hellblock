package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public class LocationUtils {

	private LocationUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Calculates the Euclidean distance between two locations in 3D space.
	 *
	 * @param location1 The first location
	 * @param location2 The second location
	 * @return The Euclidean distance between the two locations
	 */
	public static double getDistance(Location location1, Location location2) {
		return Math.sqrt(Math.pow(location2.getX() - location1.getX(), 2)
				+ Math.pow(location2.getY() - location1.getY(), 2) + Math.pow(location2.getZ() - location1.getZ(), 2));
	}

	public static Location getAnyLocationInstance() {
		return new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
	}

	public static List<String> readableLocation(Location location) {
		List<String> readableLocation = Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()),
				String.valueOf(location.getY()), String.valueOf(location.getZ()));
		return readableLocation;
	}

	/**
	 * Checks if this location is safe for a player to teleport to. Used by visits
	 * and home teleports Unsafe is any liquid or air and also if there's no space
	 *
	 * @param l Location to be checked, not null.
	 * @return true if safe, otherwise false
	 */
	public static boolean isSafeLocation(@NonNull Location l) {
		Block ground = l.getBlock().getRelative(BlockFace.DOWN);
		Block space1 = l.getBlock();
		Block space2 = l.getBlock().getRelative(BlockFace.UP);
		return checkIfSafe(l.getWorld(), ground.getType(), space1.getType(), space2.getType());
	}

	/**
	 * Checks if this location is safe for a player to teleport to and loads chunks
	 * async to check.
	 *
	 * @param l Location to be checked, not null.
	 * @return a completable future that will be true if safe, otherwise false
	 */
	public static CompletableFuture<Boolean> isSafeLocationAsync(@NonNull Location l) {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		ChunkUtils.getChunkAtAsync(l).thenRun(() -> {
			Block ground = l.getBlock().getRelative(BlockFace.DOWN);
			Block space1 = l.getBlock();
			Block space2 = l.getBlock().getRelative(BlockFace.UP);
			result.complete(checkIfSafe(l.getWorld(), ground.getType(), space1.getType(), space2.getType()));
		});
		return result;
	}

	/**
	 * Check if a location is safe for teleporting
	 * 
	 * @param world  - world
	 * @param ground Material of the block that is going to be the ground
	 * @param space1 Material of the block above the ground
	 * @param space2 Material of the block that is two blocks above the ground
	 * @return {@code true} if the location is considered safe, {@code false}
	 *         otherwise.
	 */
	private static boolean checkIfSafe(@Nullable World world, @NonNull Material ground, @NonNull Material space1,
			@NonNull Material space2) {
		// Ground must be solid, space 1 and 2 must not be solid
		if (world == null || !ground.isSolid() || (space1.isSolid() && !Tag.SIGNS.isTagged(space1))
				|| (space2.isSolid() && !Tag.SIGNS.isTagged(space2))) {
			return false;
		}
		// Cannot be submerged
		if (space1.equals(Material.WATER) && space2.equals(Material.WATER)) {
			return false;
		}
		// Unsafe
		if (ground.equals(Material.LAVA) || space1.equals(Material.LAVA) || space2.equals(Material.LAVA)
				|| Tag.SIGNS.isTagged(ground) || Tag.TRAPDOORS.isTagged(ground) || Tag.BANNERS.isTagged(ground)
				|| Tag.PRESSURE_PLATES.isTagged(ground) || Tag.FENCE_GATES.isTagged(ground)
				|| Tag.DOORS.isTagged(ground) || Tag.FENCES.isTagged(ground) || Tag.BUTTONS.isTagged(ground)
				|| Tag.ITEMS_BOATS.isTagged(ground) || Tag.ITEMS_CHEST_BOATS.isTagged(ground)
				|| Tag.CAMPFIRES.isTagged(ground) || Tag.FIRE.isTagged(ground) || Tag.FIRE.isTagged(space1)
				|| space1.equals(Material.END_PORTAL) || space2.equals(Material.END_PORTAL)
				|| space1.equals(Material.END_GATEWAY) || space2.equals(Material.END_GATEWAY)) {
			return false;
		}
		// Known unsafe blocks
		return switch (ground) {
		// Unsafe
		case ANVIL, BARRIER, CACTUS, END_PORTAL, END_ROD, FIRE, FLOWER_POT, LADDER, LEVER, TALL_GRASS, PISTON_HEAD,
				MOVING_PISTON, TORCH, WALL_TORCH, TRIPWIRE, WATER, COBWEB, NETHER_PORTAL, MAGMA_BLOCK ->
			false;
		default -> true;
		};
	}

	/**
	 * Converts a serialized location to a Location. Returns null if string is empty
	 *
	 * @param s - serialized location in format "world:x:y:z:y:p"
	 * @return Location
	 */
	public static Location getLocationString(final String s) {
		if (s == null || s.trim().equals("")) {
			return null;
		}
		final String[] parts = s.split(":");
		if (parts.length == 6) {
			final World w = Bukkit.getWorld(parts[0]);
			if (w == null) {
				return null;
			}
			// Parse string as double just in case
			int x = (int) Double.parseDouble(parts[1]);
			int y = (int) Double.parseDouble(parts[2]);
			int z = (int) Double.parseDouble(parts[3]);
			final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
			final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
			return new Location(w, x + 0.5D, y, z + 0.5D, yaw, pitch);
		}
		return null;
	}

	/**
	 * Converts a location to a simple string representation If location is null,
	 * returns empty string Only stores block ints. Inverse function returns block
	 * centers
	 *
	 * @param l - the location
	 * @return String of location in format "world:x:y:z:y:p"
	 */
	public static String getStringLocation(final Location l) {
		if (l == null || l.getWorld() == null) {
			return "";
		}
		return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":"
				+ Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
	}

	/**
	 * Converts block face direction to radial degrees. Returns 0 if block face is
	 * not radial.
	 *
	 * @param face - blockface
	 * @return degrees
	 */
	public static float blockFaceToFloat(BlockFace face) {
		return switch (face) {
		case EAST -> 90F;
		case EAST_NORTH_EAST -> 67.5F;
		case NORTH_EAST -> 45F;
		case NORTH_NORTH_EAST -> 22.5F;
		case NORTH_NORTH_WEST -> 337.5F;
		case NORTH_WEST -> 315F;
		case SOUTH -> 180F;
		case SOUTH_EAST -> 135F;
		case SOUTH_SOUTH_EAST -> 157.5F;
		case SOUTH_SOUTH_WEST -> 202.5F;
		case SOUTH_WEST -> 225F;
		case WEST -> 270F;
		case WEST_NORTH_WEST -> 292.5F;
		case WEST_SOUTH_WEST -> 247.5F;
		default -> 0F;
		};
	}

	public static String getFacing(Player player) {
		double yaw = player.getLocation().getYaw();
		if (yaw >= 337.5 || (yaw <= 22.5 && yaw >= 0.0) || (yaw >= -22.5 && yaw <= 0.0)
				|| (yaw <= -337.5 && yaw <= 0.0)) {
			return "South";
		}
		if ((yaw >= 22.5 && yaw <= 67.5) || (yaw <= -292.5 && yaw >= -337.5)) {
			return "South West";
		}
		if ((yaw >= 67.5 && yaw <= 112.5) || (yaw <= -247.5 && yaw >= -292.5)) {
			return "West";
		}
		if ((yaw >= 112.5 && yaw <= 157.5) || (yaw <= -202.5 && yaw >= -247.5)) {
			return "North West";
		}
		if ((yaw >= 157.5 && yaw <= 202.5) || (yaw <= -157.5 && yaw >= -202.5)) {
			return "North";
		}
		if ((yaw >= 202.5 && yaw <= 247.5) || (yaw <= -112.5 && yaw >= -157.5)) {
			return "North East";
		}
		if ((yaw >= 247.5 && yaw <= 292.5) || (yaw <= -67.5 && yaw >= -112.5)) {
			return "East";
		}
		if ((yaw >= 292.5 && yaw <= 337.5) || (yaw <= -22.5 && yaw >= -67.5)) {
			return "South East";
		}
		return "Error!";
	}
}