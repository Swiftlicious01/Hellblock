package com.swiftlicious.hellblock.generation;

import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.schematic.SchematicMetadata;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * Responsible for converting a player's island selection into an actual island
 * instance within a specified world and at a given location.
 * <p>
 * Supports default, classic, and schematic-based island types based on player
 * selection and configuration settings.
 */
public class IslandChoiceConverter {

	protected final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	/**
	 * Converts a player's island choice into an island using the default conversion
	 * method, without specifying a schematic.
	 *
	 * @param world     the world to generate the island in
	 * @param ownerData the player for whom the island is being created
	 * @param location  the location at which the island will be placed
	 * @return a {@link CompletableFuture} that completes when the generation is
	 *         finished
	 */
	public CompletableFuture<Void> convertIslandChoice(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull Location location) {
		return convertIslandChoice(world, ownerData, location, null);
	}

	/**
	 * Converts a player's island choice into an island at the specified location,
	 * optionally using a schematic if the player has chosen one.
	 *
	 * @param world     the world to generate the island in
	 * @param ownerData the player for whom the island is being created
	 * @param location  the base location of the island
	 * @param schematic the name of the schematic to use (nullable)
	 * @return a {@link CompletableFuture} that completes when the generation is
	 *         finished
	 */
	public CompletableFuture<Void> convertIslandChoice(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull Location location, @Nullable String schematic) {
		IslandOptions choice = ownerData.getHellblockData().getIslandChoice();

		instance.debug("convertIslandChoice: Starting island generation for " + ownerData.getName() + " (choice="
				+ choice.name() + ", schematic=" + (schematic != null ? schematic : "none") + ")");

		// === Build IslandGenerationRequest based on choice ===
		IslandGenerationRequest request = switch (choice) {
		case DEFAULT -> IslandGenerationRequest.fromVariant(IslandVariant.DEFAULT);
		case CLASSIC -> IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		case SCHEMATIC -> {
			if (!instance.getSchematicGUIManager().checkForSchematics() || schematic == null || schematic.isEmpty()) {
				instance.getPluginLogger()
						.warn("convertIslandChoice: Schematic not found or unavailable. Defaulting to CLASSIC for "
								+ ownerData.getName());
				yield IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
			}

			SchematicMetadata metadata = instance.getSchematicManager().loadSchematicMetadata(schematic);

			if (metadata.getContainer() == null) {
				instance.getPluginLogger()
						.warn("convertIslandChoice: Missing container location in schematic metadata for " + schematic);
			}
			if (metadata.getHome() == null) {
				instance.getPluginLogger()
						.warn("convertIslandChoice: Missing home location in schematic metadata for " + schematic);
			}

			instance.debug(
					"convertIslandChoice: Loaded schematic '" + schematic + "' for player " + ownerData.getName());

			yield new IslandGenerationRequest(IslandOptions.SCHEMATIC, metadata.getContainer(), null,
					metadata.getTree(), metadata.getHome(), metadata.getYaw(), metadata.getBiome(), schematic,
					metadata.getAuthor());
		}
		default -> IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		};

		// Fallback if not allowed in config
		if (!instance.getConfigManager().islandOptions().contains(request.options())) {
			instance.getPluginLogger().warn("convertIslandChoice: Island option " + request.options().name()
					+ " not allowed by config. Falling back to CLASSIC.");
			request = IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		}

		Location home = (request.home() != null) ? location.clone().add(request.home()) : location.clone();
		home.setYaw(request.homeYaw());
		home.setPitch(0f);

		if (request.author() != null) {
			instance.getPluginLogger().info(
					"convertIslandChoice: Generating schematic '" + schematic + "' authored by " + request.author());
		}

		instance.debug("convertIslandChoice: Final home location for " + ownerData.getName() + " set to [world="
				+ home.getWorld().getName() + ", x=" + home.getX() + ", y=" + home.getY() + ", z=" + home.getZ() + "]");

		return generateVariantIsland(world, location, ownerData, request, home);
	}

	/**
	 * Generates the player's island based on the selected request variant and sets
	 * the home location.
	 *
	 * @param world     the world where the island should be placed
	 * @param location  the base location of the island
	 * @param ownerData the user's stored data
	 * @param request   the island generation request
	 * @param home      the final computed home location for the player
	 * @return a {@link CompletableFuture} that completes when the island is fully
	 *         generated
	 */
	private CompletableFuture<Void> generateVariantIsland(@NotNull HellblockWorld<?> world, @NotNull Location location,
			@NotNull UserData ownerData, @NotNull IslandGenerationRequest request, @NotNull Location home) {
		boolean animate = !instance.getConfigManager().disableGenerationAnimation();
		String playerName = ownerData.getName();

		Runnable postGen = () -> {
			ownerData.getHellblockData().setHomeLocation(home);
			if (animate) {
				instance.debug("generateVariantIsland: Cleaning up camera for " + playerName);
				instance.getIslandGenerator().cleanupAnimation(ownerData);
			}
			instance.getIslandGenerator().endGeneration(ownerData.getUUID());
			ownerData.getHellblockData().setBiome(request.biome());
			instance.getHellblockHandler().showCreationTitleAndSound(ownerData);
		};

		instance.getIslandGenerator().startGeneration(ownerData.getUUID());

		if (request.isSchematic()) {
			instance.debug("generateVariantIsland: Starting schematic generation for " + playerName
					+ " with schematic '" + request.schematicName() + "' at [world=" + location.getWorld().getName()
					+ ", x=" + location.getBlockX() + ", y=" + location.getBlockY() + ", z=" + location.getBlockZ()
					+ "]");

			return instance.getIslandGenerator().generateHellblockSchematic(request, world, location, ownerData, home,
					request.schematicName(), true, animate).thenAccept(safeSpawn -> {
						if (safeSpawn != null) {
							ownerData.getHellblockData().setHomeLocation(safeSpawn);
							instance.debug("generateVariantIsland: Schematic island generated for " + playerName
									+ " with safe spawn at [x=" + safeSpawn.getBlockX() + ", y=" + safeSpawn.getBlockY()
									+ ", z=" + safeSpawn.getBlockZ() + "]");
						} else {
							ownerData.getHellblockData().setHomeLocation(home);
							instance.getPluginLogger().warn(
									"generateVariantIsland: Safe spawn was null, fallback used for " + playerName);
						}
						if (animate) {
							instance.debug("generateVariantIsland: Cleaning up camera for " + playerName);
							instance.getIslandGenerator().cleanupAnimation(ownerData);
						}
						instance.getIslandGenerator().endGeneration(ownerData.getUUID());
						ownerData.getHellblockData().setBiome(request.biome());
						instance.getHellblockHandler().showCreationTitleAndSound(ownerData);
					});
		}

		// Animated island generation
		if (animate) {
			instance.debug("generateVariantIsland: Starting animated island generation for " + playerName + " (variant="
					+ request.options().name() + ")");
			return instance.getIslandGenerator()
					.generateAnimatedHellblockIsland(request, world, location, ownerData, home).thenRun(postGen);
		}

		// Instant generation (fallback)
		instance.debug("generateVariantIsland: Running instant island generation for " + playerName + " (variant="
				+ request.options().name() + ")");
		return instance.getIslandGenerator().generateInstantHellblockIsland(request, world, location, ownerData, home)
				.thenRun(postGen);
	}
}