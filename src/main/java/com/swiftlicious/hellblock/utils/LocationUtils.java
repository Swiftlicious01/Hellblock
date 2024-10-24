package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

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
	 * Checks if a location is safe (solid ground with 2 breathable blocks)
	 *
	 * @param location Location to check
	 * @return True if location is safe
	 */
	@SuppressWarnings("deprecation")
	public static boolean isSafeLocation(Location location) {
		Block feet = location.getBlock();
		if (!feet.getType().isTransparent() && !feet.getLocation().add(0, 1, 0).getBlock().getType().isTransparent()) {
			return false; // not transparent (will suffocate)
		}
		Block head = feet.getRelative(BlockFace.UP);
		if (!head.getType().isTransparent()) {
			return false; // not transparent (will suffocate)
		}
		Block ground = feet.getRelative(BlockFace.DOWN);
		if (!ground.getType().isSolid()) {
			return false; // not solid
		}
		return true;
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