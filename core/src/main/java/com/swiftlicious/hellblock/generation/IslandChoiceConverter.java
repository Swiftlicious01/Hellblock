package com.swiftlicious.hellblock.generation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.schematic.SchematicMetadata;

public class IslandChoiceConverter {

	protected final HellblockPlugin instance;

	public IslandChoiceConverter(HellblockPlugin plugin) {
		instance = plugin;
	}

	/**
	 * Converts a player's island choice into an actual island in the specified
	 * world at the given location.
	 *
	 * @param world    The world where the island will be generated.
	 * @param player   The player for whom the island is being generated.
	 * @param location The location where the island will be placed.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	public CompletableFuture<Void> convertIslandChoice(@NotNull World world, @NotNull Player player,
			@NotNull Location location) {
		return convertIslandChoice(world, player, location, null);
	}

	/**
	 * Converts a player's island choice into an actual island in the specified
	 * world at the given location.
	 *
	 * @param world     The world where the island will be generated.
	 * @param player    The player for whom the island is being generated.
	 * @param location  The location where the island will be placed.
	 * @param schematic The name of the schematic to use if the player chose a
	 *                  schematic island (can be null).
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	public CompletableFuture<Void> convertIslandChoice(@NotNull World world, @NotNull Player player,
			@NotNull Location location, @Nullable String schematic) {

		Optional<UserData> userOpt = instance.getStorageManager().getOnlineUser(player.getUniqueId());
		if (userOpt.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		UserData user = userOpt.get();
		IslandOptions choice = user.getHellblockData().getIslandChoice();

		// === Build IslandGenerationRequest based on choice ===
		IslandGenerationRequest request = switch (choice) {
		case DEFAULT -> IslandGenerationRequest.fromVariant(IslandVariant.DEFAULT);
		case CLASSIC -> IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		case SCHEMATIC -> {
			// Check for schematic validity
			if (!instance.getSchematicGUIManager().checkForSchematics() || schematic == null || schematic.isEmpty()) {
				instance.getPluginLogger().warn("Schematic not found or unavailable, defaulting to CLASSIC.");
				yield IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
			}

			// Load metadata
			SchematicMetadata metadata = instance.getSchematicManager().loadSchematicMetadata(schematic);
			if (metadata.getChest() == null)
				instance.getPluginLogger().warn("Missing chest location in metadata for schematic: " + schematic);
			if (metadata.getHome() == null)
				instance.getPluginLogger().warn("Missing home location in metadata for schematic: " + schematic);

			// Build request using new metadata structure
			yield new IslandGenerationRequest(IslandOptions.SCHEMATIC, metadata.getChest(), // chest vector
					metadata.getTree(), // tree vector
					metadata.getHome(), // home vector
					metadata.getYaw(), // yaw facing
					schematic, // schematic name
					metadata.getAuthor() // optional author
			);
		}
		default -> IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		};

		// Validate config has this island type enabled
		if (!instance.getConfigManager().islandOptions().contains(request.options())) {
			instance.getPluginLogger().warn("Island variant (" + request.options().name()
					+ ") not found within the island options via config.yml, defaulting to CLASSIC.");
			request = IslandGenerationRequest.fromVariant(IslandVariant.CLASSIC);
		}

		// Calculate actual home location
		// relative vector from origin
		Location home = (request.home() != null) ? location.clone().add(request.home()) : location.clone();
		home.setYaw(request.homeYaw()); // uses yaw for default/classic, 0 for schematics
		home.setPitch(0f);

		if (request.author() != null) {
			instance.getPluginLogger()
					.info("Generating island from schematic '%s' by %s".formatted(schematic, request.author()));
		}

		// === Generate the chosen variant island ===
		return generateVariantIsland(world, location, player, user, request, home);
	}

	/**
	 * Generates a defined island at the specified location.
	 *
	 * @param world    The world where the island will be generated.
	 * @param location The location where the island will be placed.
	 * @param player   The player for whom the island is being generated.
	 * @param user     The user data associated with the player.
	 * @param request  The variant to create the island as.
	 * @param home     The home location to set after generation.
	 * @return A CompletableFuture that completes when the island generation is
	 *         done.
	 */
	private CompletableFuture<Void> generateVariantIsland(@NotNull World world, @NotNull Location location,
			@NotNull Player player, @NotNull UserData user, @NotNull IslandGenerationRequest request,
			@NotNull Location home) {

		Runnable postGen = () -> {
			instance.getHellblockHandler().showCreationTitleAndSound(player);
			user.getHellblockData().setHomeLocation(home);
		};

		boolean animate = !instance.getConfigManager().disableGenerationAnimation();

		// Handle schematic islands
		if (request.isSchematic()) {
			return instance.getIslandGenerator()
					.generateHellblockSchematic(world, location, player, request.schematicName(), true, animate)
					.thenAccept(safeSpawn -> {
						if (safeSpawn != null) {
							user.getHellblockData().setHomeLocation(safeSpawn);
						} else {
							user.getHellblockData().setHomeLocation(home);
							instance.getPluginLogger()
									.warn("Safe spawn was null — fallback home used for " + player.getName());
						}

						instance.getHellblockHandler().showCreationTitleAndSound(player);
					});
		}

		// Handle classic/default generation via IslandVariant
		if (animate) {
			postGen.run(); // Show feedback immediately
			return instance.getIslandGenerator().generateAnimatedHellblockIsland(
					IslandVariant.valueOf(request.options().name()), world, location, player).thenRun(postGen);
		}

		// Fallback: instant generation
		return CompletableFuture.runAsync(() -> {
			instance.getIslandGenerator().generateInstantHellblockIsland(
					IslandVariant.valueOf(request.options().name()), world, location, player);
			postGen.run();
		});
	}
}