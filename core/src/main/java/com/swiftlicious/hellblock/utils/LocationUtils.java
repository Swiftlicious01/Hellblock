package com.swiftlicious.hellblock.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

	private static final Set<Material> UNSAFE_GROUND_BLOCKS = Collections.unmodifiableSet(initUnsafeBlocks());

	@NotNull
	private static Set<Material> initUnsafeBlocks() {
		Set<String> materialNames = Set.of("ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "BARRIER", "GRAVEL", "SAND",
				"SUSPICIOUS_GRAVEL", "SUSPICIOUS_SAND", "RED_SAND", "POINTED_DRIPSTONE", "STRUCTURE_VOID", "CACTUS",
				"END_PORTAL", "END_ROD", "FIRE", "FLOWER_POT", "LADDER", "LEVER", "TALL_GRASS", "PISTON_HEAD",
				"MOVING_PISTON", "TORCH", "SOUL_TORCH", "REDSTONE_TORCH", "WALL_TORCH", "TRIPWIRE", "WATER", "COBWEB",
				"LAVA", "SOUL_FIRE", "SOUL_CAMPFIRE", "CAMPFIRE", "BAMBOO", "POWDER_SNOW", "CANDLE", "VINE",
				"SWEET_BERRY_BUSH", "NETHER_PORTAL", "MAGMA_BLOCK", "TURTLE_EGG", "SEA_PICKLE", "LIGHTNING_ROD",
				"CHAIN", "SCULK_SENSOR", "LANTERN", "SOUL_LANTERN", "SCAFFOLDING", "TNT", "LAVA_CAULDRON", "DRAGON_EGG",
				"SMALL_DRIPLEAF", "BIG_DRIPLEAF", "SHULKER_BOX", "END_GATEWAY", "IRON_BARS", "STRING",
				"CALIBRATED_SCULK_SENSOR", "DROPPER", "OBSERVER", "END_CRYSTAL", "DISPENSER", "HOPPER", "LILY_PAD",
				"PLAYER_HEAD", "PLAYER_WALL_HEAD", "ZOMBIE_HEAD", "CREEPER_HEAD", "PIGLIN_HEAD", "DRAGON_HEAD",
				"ZOMBIE_WALL_HEAD", "CREEPER_WALL_HEAD", "DRAGON_WALL_HEAD", "PIGLIN_WALL_HEAD",
				"WITHER_SKELETON_SKULL", "SKELETON_SKULL", "SKELETON_WALL_SKULL", "WITHER_SKELETON_WALL_SKULL",
				"WITHER_ROSE", "WEEPING_VINES", "TWISTING_VINES", "TRIPWIRE_HOOK", "POWDER_SNOW_CAULDRON",
				"ORANGE_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE", "BLUE_STAINED_GLASS_PANE",
				"LIGHT_BLUE_STAINED_GLASS_PANE", "LIME_STAINED_GLASS_PANE", "GREEN_STAINED_GLASS_PANE",
				"RED_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS_PANE", "WHITE_STAINED_GLASS_PANE",
				"YELLOW_STAINED_GLASS_PANE", "PINK_STAINED_GLASS_PANE", "PURPLE_STAINED_GLASS_PANE",
				"CYAN_STAINED_GLASS_PANE", "LIGHT_GRAY_STAINED_GLASS_PANE", "BROWN_STAINED_GLASS_PANE",
				"MAGENTA_STAINED_GLASS_PANE", "GLASS_PANE");

		EnumSet<Material> safeSet = EnumSet.noneOf(Material.class);

		materialNames.forEach(name -> {
			Material mat = Material.matchMaterial(name);
			if (mat != null) {
				safeSet.add(mat);
			} else {
				// Optional: log skipped materials
				HellblockPlugin.getInstance().debug("Skipped missing Material for safety check: " + name);
			}
		});

		return safeSet;
	}

	private static final Set<Material> HAZARD_BLOCKS = initHazardBlocks();

	@NotNull
	private static Set<Material> initHazardBlocks() {
		Set<String> materialNames = Set.of("POINTED_DRIPSTONE", "SAND", "RED_SAND", "GRAVEL", "SUSPICIOUS_GRAVEL",
				"SUSPICIOUS_SAND");

		EnumSet<Material> safeSet = EnumSet.noneOf(Material.class);
		materialNames.stream().map(Material::matchMaterial).filter(Objects::nonNull).forEach(safeSet::add);
		return safeSet;
	}

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

	@NotNull
	public static List<String> readableLocation(@NotNull Location location) {
		return Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()),
				String.valueOf(location.getY()), String.valueOf(location.getZ()));
	}

	@Nullable
	public static Location getAnyLocationInstance() {
		return !Bukkit.getWorlds().isEmpty()
				? new Location(Bukkit.getWorlds().stream().filter(Objects::nonNull).findFirst().orElse(null), 0, 64, 0)
				: null;
	}

	@NotNull
	public static String toChunkPosString(@NotNull Location location) {
		return (location.getBlockX() % 16) + "_" + location.getBlockY() + "_" + (location.getBlockZ() % 16);
	}

	/**
	 * Checks if a player is standing on a safe block right now.
	 */
	public static boolean isPlayerStandingSafe(@NotNull Player player) {
		return isSafeLocation(player.getLocation());
	}

	@NotNull
	public static CompletableFuture<Boolean> isSafeLocationAsync(@NotNull Location location, @Nullable Player player) {
		// Flying is always safe
		if (player != null && (player.isFlying() || player.isGliding() || player.getGameMode() == GameMode.CREATIVE)) {
			return CompletableFuture.completedFuture(true);
		}

		final CompletableFuture<Boolean> result = new CompletableFuture<>();
		ChunkUtils.getChunkAtAsync(location).thenRun(() -> result.complete(isSafeLocation(location)));
		return result;
	}

	/**
	 * Checks if this location is safe for a player to teleport to and loads chunks
	 * async to check.
	 */
	/**
	 * Checks if this location is safe for teleporting, with async chunk loading.
	 */
	@NotNull
	public static CompletableFuture<Boolean> isSafeLocationAsync(@NotNull Location location) {
		return isSafeLocationAsync(location, null);
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

		final double feetY = location.getY();
		final double headY = feetY + 1.8;

		double width = 0.28; // experimental
		BoundingBox playerBox = new BoundingBox(location.getX() - width, feetY, location.getZ() - width,
				location.getX() + width, headY, location.getZ() + width);

		// Ground check box extends down 1 block below feet
		final BoundingBox groundCheck = playerBox.clone().shift(0, -1, 0);

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
						HellblockPlugin.getInstance()
								.debug("Unsafe: Head in liquid block: " + type + " at " + block.getLocation());
						return false;
					}

					// Liquids/air/passable blocks are not valid support
					if (block.isPassable() || Tag.LEAVES.isTagged(type)) {
						continue;
					}

					Collection<BoundingBox> shapes = block.getCollisionShape().getBoundingBoxes();
					if (shapes.isEmpty()) {
						shapes = Collections.singletonList(block.getBoundingBox());
					}

					for (BoundingBox shape : shapes) {
						shape = shape.clone().shift(block.getX(), block.getY(), block.getZ());

						if (playerBox.overlaps(shape)) {
							HellblockPlugin.getInstance().debug("Player box overlaps solid block: " + block.getType()
									+ " @ " + block.getLocation());
							return false;
						}

						if (groundCheck.overlaps(shape)) {
							final double blockTopY = shape.getMaxY();
							final double delta = feetY - blockTopY;

							if (delta >= 0 && delta < 0.5) {
								// Carpet/snow require solid support beneath
								if (HellblockPlugin.getInstance().getFarmingManager().isWoolCarpet(type)
										|| type == Material.SNOW) {
									final Block support = block.getRelative(BlockFace.DOWN);
									if (support.isPassable()) {
										continue;
									}
								}

								standingOnSomething = true;
								HellblockPlugin.getInstance()
										.debug("FeetY=" + feetY + ", BlockTopY=" + blockTopY + ", delta=" + delta);
								HellblockPlugin.getInstance().debug("StandingOn: " + type + " | shape: " + shape);

								if (UNSAFE_GROUND_BLOCKS.contains(type) || Tag.FIRE.isTagged(type)
										|| type == Material.LAVA || type == Material.CACTUS
										|| type == Material.MAGMA_BLOCK) {
									return false;
								}

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

		HellblockPlugin.getInstance().debug("Final playerBox: " + playerBox);
		HellblockPlugin.getInstance().debug("GroundCheck: " + groundCheck);
		HellblockPlugin.getInstance()
				.debug("Bounding: X(%d-%d), Y(%d-%d), Z(%d-%d)".formatted(minX, maxX, minY, maxY, minZ, maxZ));

		if (!standingOnSomething) {
			HellblockPlugin.getInstance().debug("No valid ground to stand on at " + getStringLocation(location));
			return false;
		}

		final Block hazardCheck = world.getBlockAt(location).getRelative(BlockFace.UP, 2);
		final Material hazardType = hazardCheck.getType();
		if (HAZARD_BLOCKS.contains(hazardType) || Tag.ANVIL.isTagged(hazardType) || HellblockPlugin.getInstance()
				.getFarmingManager().getConcreteConverter().getConcretePowderBlocks().contains(hazardType)) {
			return false;
		}

		HellblockPlugin.getInstance().debug("Safe location: " + getStringLocation(location));
		return true;
	}

	@Nullable
	public Block getBlockSupporting(@NotNull Player player) {
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
	@Nullable
	public static Location getLocationString(final @NotNull String s) {
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
	@NotNull
	public static String getStringLocation(final @NotNull Location l) {
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

	@NotNull
	public static String getFacing(@NotNull Player player) {
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

	@Nullable
	public static Location deserializeLocation(@NotNull Section section) {
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

	public static boolean canSeeLocation(@NotNull Player player, @NotNull Location target, double baseRange) {
		if (target.getWorld() == null)
			return false;
		
		if (!player.getWorld().getUID().equals(target.getWorld().getUID()))
			return false;

		// Ensure the chunk is loaded for the player
		if (!target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4))
			return false;

		// Player's client render distance (in chunks)
		int viewDistanceChunks = player.getClientViewDistance();
		double viewDistanceBlocks = viewDistanceChunks * 16.0;

		// Use the higher of plugin-configured range or player's view distance
		double maxDistance = Math.max(baseRange, viewDistanceBlocks);

		// Only consider horizontal distance (ignore height difference)
		double dx = player.getLocation().getX() - target.getX();
		double dz = player.getLocation().getZ() - target.getZ();
		double horizontalDistanceSquared = dx * dx + dz * dz;

		return horizontalDistanceSquared <= (maxDistance * maxDistance);
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