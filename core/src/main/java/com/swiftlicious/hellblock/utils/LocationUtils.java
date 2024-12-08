package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.Collection;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;

import dev.dejvokep.boostedyaml.block.implementation.Section;

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
	public static double getDistance(@NotNull Location location1, @NotNull Location location2) {
		return Math.sqrt(Math.pow(location2.getX() - location1.getX(), 2)
				+ Math.pow(location2.getY() - location1.getY(), 2) + Math.pow(location2.getZ() - location1.getZ(), 2));
	}

	public static @NotNull List<String> readableLocation(@NotNull Location location) {
		List<String> readableLocation = Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()),
				String.valueOf(location.getY()), String.valueOf(location.getZ()));
		return readableLocation;
	}

	public static Location getAnyLocationInstance() {
		return new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
	}

	public static String toChunkPosString(Location location) {
		return (location.getBlockX() % 16) + "_" + location.getBlockY() + "_" + (location.getBlockZ() % 16);
	}

	/**
	 * Checks if this location is safe for a player to teleport to. Used by visits
	 * and home teleports Unsafe is any liquid or air and also if there's no space
	 */
	public static boolean isSafeLocation(@NotNull Location location) {
		Block feet = location.getBlock();
		Block head = feet.getRelative(BlockFace.UP);
		Block ground = feet.getRelative(BlockFace.DOWN);
		return checkIfSafe(ground, feet, head);
	}

	/**
	 * Checks if this location is safe for a player to teleport to and loads chunks
	 * async to check.
	 */
	public static CompletableFuture<Boolean> isSafeLocationAsync(@NotNull Location location) {
		CompletableFuture<Boolean> result = new CompletableFuture<>();
		ChunkUtils.getChunkAtAsync(location).thenRun(() -> {
			Block feet = location.getBlock();
			Block head = feet.getRelative(BlockFace.UP);
			Block ground = feet.getRelative(BlockFace.DOWN);
			result.complete(checkIfSafe(ground, feet, head));
		});
		return result;
	}

	/**
	 * Check if a location is safe for teleporting
	 * 
	 * @return
	 */
	private static boolean checkIfSafe(@NotNull Block ground, @NotNull Block feet, @NotNull Block head) {

		// Ground must be solid and head must not be solid
		if (ground.getCollisionShape().getBoundingBoxes().isEmpty()
				|| (head.getType().isSolid() && !head.isPassable())) {
			return false;
		}

		// Cannot be submerged
		if (feet.getType() == Material.WATER && head.getType() == Material.WATER) {
			return false;
		}

		// Unsafe
		if (ground.getType() == Material.LAVA || feet.getType() == Material.LAVA || head.getType() == Material.LAVA
				|| head.getRelative(BlockFace.UP).getType() == Material.LAVA || Tag.FIRE.isTagged(head.getType())
				|| head.getRelative(BlockFace.UP).getType() == Material.POINTED_DRIPSTONE
				|| Tag.ALL_SIGNS.isTagged(ground.getType()) || Tag.TRAPDOORS.isTagged(ground.getType())
				|| Tag.BANNERS.isTagged(ground.getType()) || Tag.PRESSURE_PLATES.isTagged(ground.getType())
				|| Tag.FENCE_GATES.isTagged(ground.getType()) || Tag.DOORS.isTagged(ground.getType())
				|| Tag.CONCRETE_POWDER.isTagged(ground.getType()) || Tag.FENCES.isTagged(ground.getType())
				|| Tag.BUTTONS.isTagged(ground.getType()) || Tag.BEDS.isTagged(ground.getType())
				|| Tag.CAMPFIRES.isTagged(ground.getType()) || Tag.FIRE.isTagged(ground.getType())
				|| Tag.FIRE.isTagged(feet.getType()) || feet.getType() == Material.POWDER_SNOW_CAULDRON
				|| feet.getType() == Material.POWDER_SNOW || feet.getType() == Material.COBWEB
				|| feet.getType() == Material.SCAFFOLDING || feet.getType() == Material.BAMBOO
				|| feet.getType() == Material.LAVA_CAULDRON || feet.getType() == Material.STRING
				|| feet.getType() == Material.NETHER_PORTAL || feet.getType() == Material.END_PORTAL
				|| feet.getType() == Material.END_GATEWAY || feet.getType() == Material.END_CRYSTAL
				|| feet.getType() == Material.WITHER_ROSE || feet.getType() == Material.TRIPWIRE) {
			return false;
		}
		// Known unsafe blocks
		return switch (ground.getType()) {
		// Unsafe
		case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL, BARRIER, GRAVEL, SAND, SUSPICIOUS_GRAVEL, SUSPICIOUS_SAND, RED_SAND,
				POINTED_DRIPSTONE, STRUCTURE_VOID, CACTUS, END_PORTAL, END_ROD, FIRE, FLOWER_POT, LADDER, LEVER,
				TALL_GRASS, PISTON_HEAD, MOVING_PISTON, TORCH, SOUL_TORCH, REDSTONE_TORCH, WALL_TORCH, TRIPWIRE, WATER,
				COBWEB, LAVA, SOUL_FIRE, SOUL_CAMPFIRE, CAMPFIRE, BAMBOO, POWDER_SNOW, CANDLE, VINE, SWEET_BERRY_BUSH,
				NETHER_PORTAL, MAGMA_BLOCK, TURTLE_EGG, SEA_PICKLE, LIGHTNING_ROD, CHAIN, SCULK_SENSOR, LANTERN,
				SOUL_LANTERN, SCAFFOLDING, TNT, LAVA_CAULDRON, DRAGON_EGG, SMALL_DRIPLEAF, BIG_DRIPLEAF, SHULKER_BOX,
				END_GATEWAY, IRON_BARS, STRING, CALIBRATED_SCULK_SENSOR, DROPPER, OBSERVER, END_CRYSTAL, DISPENSER,
				HOPPER, LILY_PAD, PLAYER_HEAD, PLAYER_WALL_HEAD, ZOMBIE_HEAD, CREEPER_HEAD, PIGLIN_HEAD, DRAGON_HEAD,
				ZOMBIE_WALL_HEAD, CREEPER_WALL_HEAD, DRAGON_WALL_HEAD, PIGLIN_WALL_HEAD, WITHER_SKELETON_SKULL,
				SKELETON_SKULL, SKELETON_WALL_SKULL, WITHER_SKELETON_WALL_SKULL, WITHER_ROSE, WEEPING_VINES,
				TWISTING_VINES, TRIPWIRE_HOOK, POWDER_SNOW_CAULDRON, ORANGE_STAINED_GLASS_PANE,
				BLACK_STAINED_GLASS_PANE, BLUE_STAINED_GLASS_PANE, LIGHT_BLUE_STAINED_GLASS_PANE,
				LIME_STAINED_GLASS_PANE, GREEN_STAINED_GLASS_PANE, RED_STAINED_GLASS_PANE, GRAY_STAINED_GLASS_PANE,
				WHITE_STAINED_GLASS_PANE, YELLOW_STAINED_GLASS_PANE, PINK_STAINED_GLASS_PANE, PURPLE_STAINED_GLASS_PANE,
				CYAN_STAINED_GLASS_PANE, LIGHT_GRAY_STAINED_GLASS_PANE, BROWN_STAINED_GLASS_PANE,
				MAGENTA_STAINED_GLASS_PANE, GLASS_PANE ->
			false;
		default -> true;
		};
	}

	public @Nullable Block getBlockSupporting(@NotNull Player player) {
		World w = player.getWorld();
		BoundingBox box = player.getBoundingBox();
		int blx = NumberConversions.floor(box.getMinX()), bgx = NumberConversions.ceil(box.getMaxX()),
				bly = NumberConversions.floor(box.getMinY()) - 2, // check two blocks below the player as some blocks
																	// have tall collision boxes
				bgy = NumberConversions.ceil(box.getMinY()), // use minY as our other value is always lower
				blz = NumberConversions.floor(box.getMinZ()), bgz = NumberConversions.ceil(box.getMaxZ());
		for (int x = blx; x < bgx; x++) {
			for (int y = bly; y < bgy; y++) {
				for (int z = blz; z < bgz; z++) {
					Block block = w.getBlockAt(x, y, z);
					if (!block.isPassable()) {
						Collection<BoundingBox> collisionShapes = block.getCollisionShape().getBoundingBoxes();
						// for some reason collision shapes can be empty if the block has a "primitive
						// shape" (such as slabs)
						if (collisionShapes.isEmpty()) {
							if (isStandingOn(block.getBoundingBox(), box)) {
								return block;
							}
						} else {
							for (BoundingBox cs : collisionShapes) {
								// we need to shift the box as for some reason bukkit loves to have methods
								// returning relative and non-relative BoundingBoxes because fuck everyone,
								// apparently.
								cs.shift(block.getX(), block.getY(), block.getZ());
								if (isStandingOn(cs, box)) {
									return block;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	public boolean isStandingOn(@NotNull BoundingBox blockBox, @NotNull BoundingBox playerBox) {
		// if the maxY of the block bounding box == the minY of the players box, if they
		// intersect 2d they are standing on it
		// unfortunately bukkit's BoundingBox doesn't have an intersect2d method, so we
		// shift it up.
		return blockBox.getMaxY() == playerBox.getMinY() && playerBox.contains(blockBox.shift(0, 0.1, 0));
	}

	/**
	 * Converts a serialized location to a Location. Returns null if string is empty
	 *
	 * @param s - serialized location in format "world:x:y:z:y:p"
	 * @return Location
	 */
	public static @NotNull Location getLocationString(final @NotNull String s) {
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
	public static @NotNull String getStringLocation(final @NotNull Location l) {
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
	public static float blockFaceToFloat(@NotNull BlockFace face) {
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

	public static @NotNull String getFacing(@NotNull Player player) {
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

	public static void serializeLocation(@NotNull Section section, @Nullable Location location, boolean includeExtras) {
		String world = "";
		double x = 0.0D;
		double y = (double) HellblockPlugin.getInstance().getConfigManager().height();
		double z = 0.0D;
		float yaw = 90.0F;
		float pitch = 0.0F;
		if (location != null && location.getWorld() != null) {
			world = location.getWorld().getName();
			x = location.getX();
			y = location.getY();
			z = location.getZ();
			yaw = location.getYaw();
			pitch = location.getPitch();
		}

		if (!world.isEmpty()) {
			section.set("world", world);
			section.set("x", round(x, 3));
			section.set("y", round(y, 3));
			section.set("z", round(z, 3));
			if (includeExtras) {
				section.set("yaw", round(yaw, 3));
				section.set("pitch", round(pitch, 3));
			}
		}
	}

	public static @Nullable Location deserializeLocation(@NotNull Section section) {
		World world = Bukkit.getWorld(section.getString("world"));
		if (world == null)
			return null;
		double x = section.getDouble("x");
		double y = section.getDouble("y");
		double z = section.getDouble("z");
		float yaw = section.getFloat("yaw", 90F);
		float pitch = section.getFloat("pitch", 0F);
		return new Location(world, x, y, z, yaw, pitch);
	}

	/**
	 * Rounds the specified value to the amount of decimals specified
	 *
	 * @param value    to round
	 * @param decimals count
	 * @return value round to the decimal count specified
	 */
	public static double round(double value, int decimals) {
		double p = Math.pow(10, decimals);
		return Math.round(value * p) / p;
	}

	@NotNull
	public static Location toBlockLocation(Location location) {
		Location blockLoc = location.clone();
		blockLoc.setX(location.getBlockX());
		blockLoc.setY(location.getBlockY());
		blockLoc.setZ(location.getBlockZ());
		return blockLoc;
	}

	@NotNull
	public static Location toBlockCenterLocation(Location location) {
		Location centerLoc = location.clone();
		centerLoc.setX(location.getBlockX() + 0.5);
		centerLoc.setY(location.getBlockY() + 0.5);
		centerLoc.setZ(location.getBlockZ() + 0.5);
		return centerLoc;
	}

	@NotNull
	public static Location toSurfaceCenterLocation(Location location) {
		Location centerLoc = location.clone();
		centerLoc.setX(location.getBlockX() + 0.5);
		centerLoc.setZ(location.getBlockZ() + 0.5);
		centerLoc.setY(location.getBlockY());
		return centerLoc;
	}
}