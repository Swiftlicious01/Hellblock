package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;

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
		List<String> readableLocation = Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()));
		return readableLocation;
	}
}