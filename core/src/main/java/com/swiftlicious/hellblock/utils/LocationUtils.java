package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

	private static final Set<Material> UNSAFE_GROUND_BLOCKS = EnumSet.of(Material.ANVIL, Material.CHIPPED_ANVIL,
			Material.DAMAGED_ANVIL, Material.BARRIER, Material.GRAVEL, Material.SAND, Material.SUSPICIOUS_GRAVEL,
			Material.SUSPICIOUS_SAND, Material.RED_SAND, Material.POINTED_DRIPSTONE, Material.STRUCTURE_VOID,
			Material.CACTUS, Material.END_PORTAL, Material.END_ROD, Material.FIRE, Material.FLOWER_POT, Material.LADDER,
			Material.LEVER, Material.TALL_GRASS, Material.PISTON_HEAD, Material.MOVING_PISTON, Material.TORCH,
			Material.SOUL_TORCH, Material.REDSTONE_TORCH, Material.WALL_TORCH, Material.TRIPWIRE, Material.WATER,
			Material.COBWEB, Material.LAVA, Material.SOUL_FIRE, Material.SOUL_CAMPFIRE, Material.CAMPFIRE,
			Material.BAMBOO, Material.POWDER_SNOW, Material.CANDLE, Material.VINE, Material.SWEET_BERRY_BUSH,
			Material.NETHER_PORTAL, Material.MAGMA_BLOCK, Material.TURTLE_EGG, Material.SEA_PICKLE,
			Material.LIGHTNING_ROD, Material.CHAIN, Material.SCULK_SENSOR, Material.LANTERN, Material.SOUL_LANTERN,
			Material.SCAFFOLDING, Material.TNT, Material.LAVA_CAULDRON, Material.DRAGON_EGG, Material.SMALL_DRIPLEAF,
			Material.BIG_DRIPLEAF, Material.SHULKER_BOX, Material.END_GATEWAY, Material.IRON_BARS, Material.STRING,
			Material.CALIBRATED_SCULK_SENSOR, Material.DROPPER, Material.OBSERVER, Material.END_CRYSTAL,
			Material.DISPENSER, Material.HOPPER, Material.LILY_PAD, Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD,
			Material.ZOMBIE_HEAD, Material.CREEPER_HEAD, Material.PIGLIN_HEAD, Material.DRAGON_HEAD,
			Material.ZOMBIE_WALL_HEAD, Material.CREEPER_WALL_HEAD, Material.DRAGON_WALL_HEAD, Material.PIGLIN_WALL_HEAD,
			Material.WITHER_SKELETON_SKULL, Material.SKELETON_SKULL, Material.SKELETON_WALL_SKULL,
			Material.WITHER_SKELETON_WALL_SKULL, Material.WITHER_ROSE, Material.WEEPING_VINES, Material.TWISTING_VINES,
			Material.TRIPWIRE_HOOK, Material.POWDER_SNOW_CAULDRON, Material.ORANGE_STAINED_GLASS_PANE,
			Material.BLACK_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
			Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE,
			Material.GRAY_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE, Material.YELLOW_STAINED_GLASS_PANE,
			Material.PINK_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
			Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE,
			Material.MAGENTA_STAINED_GLASS_PANE, Material.GLASS_PANE);

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
		return Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()),
				String.valueOf(location.getY()), String.valueOf(location.getZ()));
	}

	public static Location getAnyLocationInstance() {
		return new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
	}

	public static String toChunkPosString(Location location) {
		return (location.getBlockX() % 16) + "_" + location.getBlockY() + "_" + (location.getBlockZ() % 16);
	}

	/**
	 * Checks if a player is standing on a safe block right now.
	 */
	public static boolean isPlayerStandingSafe(@NotNull Player player) {
		return isSafeLocation(player.getLocation());
	}

	/**
	 * Checks if this location is safe for a player to teleport to and loads chunks
	 * async to check.
	 */
	/**
	 * Checks if this location is safe for teleporting, with async chunk loading.
	 */
	public static CompletableFuture<Boolean> isSafeLocationAsync(@NotNull Location location) {
		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		ChunkUtils.getChunkAtAsync(location).thenRun(() -> result.complete(isSafeLocation(location)));
		return result;
	}

	/**
	 * Checks if this location is safe for a player to teleport to. Safe means:
	 * player can stand without being harmed, with at least 2 blocks of vertical
	 * clearance and no hazards overhead.
	 */
	public static boolean isSafeLocation(@NotNull Location location) {
		final World world = location.getWorld();
		if (world == null) {
			return false;
		}

		// Full player bounding box (~0.6 wide, ~1.8 tall)
		final BoundingBox playerBox = BoundingBox.of(location, 0.6, 1.8, 0.6);

		// Ground check box extends down 1 block below feet
		final BoundingBox groundCheck = playerBox.clone().expand(0, -1, 0);

		final int minX = NumberConversions.floor(groundCheck.getMinX());
		final int maxX = NumberConversions.ceil(groundCheck.getMaxX());
		final int minY = NumberConversions.floor(groundCheck.getMinY());
		final int maxY = NumberConversions.ceil(groundCheck.getMaxY());
		final int minZ = NumberConversions.floor(groundCheck.getMinZ());
		final int maxZ = NumberConversions.ceil(groundCheck.getMaxZ());

		boolean standingOnSomething = false;

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					final Block block = world.getBlockAt(x, y, z);
					final Material type = block.getType();

					// --- Explicit check: head in water or lava = unsafe ---
					if ((type == Material.WATER || type == Material.LAVA)
							&& playerBox.overlaps(block.getBoundingBox())) {
						return false;
					}

					// Liquids/air/passable blocks are not valid support
					if (block.isPassable()) {
						continue;
					}

					Collection<BoundingBox> shapes = block.getCollisionShape().getBoundingBoxes();
					if (shapes.isEmpty()) {
						shapes = Collections.singletonList(block.getBoundingBox());
					}

					for (BoundingBox shape : shapes) {
						shape = shape.clone().shift(block.getX(), block.getY(), block.getZ());

						// If the full player box intersects with a solid block → unsafe
						if (playerBox.overlaps(shape)) {
							return false;
						}

						// Check if standing on this block
						if (groundCheck.overlaps(shape)) {
							final double blockTopY = shape.getMaxY();
							final double feetY = playerBox.getMinY();

							// Player is "standing" if feet are aligned within tolerance
							if (feetY >= blockTopY && feetY - blockTopY < 0.3) {
								// Carpet/snow require solid support beneath
								if (Tag.WOOL_CARPETS.isTagged(type) || type == Material.SNOW) {
									final Block support = block.getRelative(BlockFace.DOWN);
									if (support.isPassable()) {
										continue; // floating carpet/snow = unsafe
									}
								}

								standingOnSomething = true;

								// --- Unsafe ground checks ---
								if (UNSAFE_GROUND_BLOCKS.contains(type) || Tag.FIRE.isTagged(type)
										|| type == Material.LAVA || type == Material.CACTUS
										|| type == Material.MAGMA_BLOCK) {
									return false;
								}

								// Bubble column check: Magma/Soul Sand under water
								if ((type == Material.MAGMA_BLOCK || type == Material.SOUL_SAND)
										&& block.getRelative(BlockFace.UP).getType() == Material.WATER) {
									return false;
								}
							}
						}
					}
				}
			}
		}

		// If no ground to stand on → unsafe
		if (!standingOnSomething) {
			return false;
		}

		// --- Extra hazard check ABOVE the player’s head ---
		final Block hazardCheck = world.getBlockAt(location).getRelative(BlockFace.UP, 2);
		final Material hazardType = hazardCheck.getType();
		if (hazardType == Material.POINTED_DRIPSTONE || hazardType == Material.ANVIL
				|| hazardType == Material.CHIPPED_ANVIL || hazardType == Material.DAMAGED_ANVIL
				|| hazardType == Material.SAND || hazardType == Material.RED_SAND || hazardType == Material.GRAVEL
				|| hazardType == Material.SUSPICIOUS_GRAVEL || hazardType == Material.SUSPICIOUS_SAND) {
			return false; // unsafe falling hazard
		}

		return true;
	}

	public @Nullable Block getBlockSupporting(@NotNull Player player) {
		final World w = player.getWorld();
		final BoundingBox box = player.getBoundingBox();
		// check two blocks below the player as some blocks
		// use minY as our other value is always lower
		// have tall collision boxes
		final int blx = NumberConversions.floor(box.getMinX());
		final int bgx = NumberConversions.ceil(box.getMaxX());
		final int bly = NumberConversions.floor(box.getMinY()) - 2;
		final int bgy = NumberConversions.ceil(box.getMinY());
		final int blz = NumberConversions.floor(box.getMinZ());
		final int bgz = NumberConversions.ceil(box.getMaxZ());
		for (int x = blx; x < bgx; x++) {
			for (int y = bly; y < bgy; y++) {
				for (int z = blz; z < bgz; z++) {
					final Block block = w.getBlockAt(x, y, z);
					if (!block.isPassable()) {
						final Collection<BoundingBox> collisionShapes = block.getCollisionShape().getBoundingBoxes();
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
		if (s == null || "".equals(s.trim())) {
			return null;
		}
		final String[] parts = s.split(":");
		if (parts.length != 6) {
			return null;
		}
		final World w = Bukkit.getWorld(parts[0]);
		if (w == null) {
			return null;
		}
		// Parse string as double just in case
		final int x = (int) Double.parseDouble(parts[1]);
		final int y = (int) Double.parseDouble(parts[2]);
		final int z = (int) Double.parseDouble(parts[3]);
		final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
		final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
		return new Location(w, x + 0.5D, y, z + 0.5D, yaw, pitch);
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
		final double yaw = player.getLocation().getYaw();
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

		if (world.isEmpty()) {
			return;
		}
		section.set("world", world);
		section.set("x", round(x, 3));
		section.set("y", round(y, 3));
		section.set("z", round(z, 3));
		if (!includeExtras) {
			return;
		}
		section.set("yaw", round(yaw, 3));
		section.set("pitch", round(pitch, 3));
	}

	public static @Nullable Location deserializeLocation(@NotNull Section section) {
		final World world = Bukkit.getWorld(section.getString("world"));
		if (world == null) {
			return null;
		}
		final double x = section.getDouble("x");
		final double y = section.getDouble("y");
		final double z = section.getDouble("z");
		final float yaw = section.getFloat("yaw", 90F);
		final float pitch = section.getFloat("pitch", 0F);
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
		final double p = Math.pow(10, decimals);
		return Math.round(value * p) / p;
	}

	@NotNull
	public static Location toBlockLocation(Location location) {
		final Location blockLoc = location.clone();
		blockLoc.setX(location.getBlockX());
		blockLoc.setY(location.getBlockY());
		blockLoc.setZ(location.getBlockZ());
		return blockLoc;
	}

	@NotNull
	public static Location toBlockCenterLocation(Location location) {
		final Location centerLoc = location.clone();
		centerLoc.setX(location.getBlockX() + 0.5);
		centerLoc.setY(location.getBlockY() + 0.5);
		centerLoc.setZ(location.getBlockZ() + 0.5);
		return centerLoc;
	}

	@NotNull
	public static Location toSurfaceCenterLocation(Location location) {
		final Location centerLoc = location.clone();
		centerLoc.setX(location.getBlockX() + 0.5);
		centerLoc.setZ(location.getBlockZ() + 0.5);
		centerLoc.setY(location.getBlockY());
		return centerLoc;
	}
}