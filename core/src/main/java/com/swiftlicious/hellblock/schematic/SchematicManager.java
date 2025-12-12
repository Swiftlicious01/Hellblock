package com.swiftlicious.hellblock.schematic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.WorldEdit;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;

/**
 * Manages the loading, caching, and pasting of schematic files for island
 * generation.
 *
 * <p>
 * This class is responsible for handling all schematic-related operations
 * within the plugin, including:
 * <ul>
 * <li>Detecting and selecting the appropriate schematic paster implementation
 * (FAWE, WorldEdit, or internal async)</li>
 * <li>Copying default schematics on first load</li>
 * <li>Loading and caching schematic files and their associated metadata</li>
 * <li>Handling schematic pasting with support for asynchronous operations and
 * spawn safety checks</li>
 * </ul>
 *
 * <p>
 * It supports dynamic reloading through the {@link Reloadable} interface and
 * integrates with multiple third-party tools via hook detection.
 *
 * <p>
 * All schematic metadata is parsed from accompanying YAML files, which define
 * important details like spawn points, containers, trees, and biome data.
 *
 * <p>
 * This class is thread-safe for core operations and ensures fallbacks and
 * logging are in place in case of failure or misconfiguration.
 */
public class SchematicManager implements Reloadable {

	protected final HellblockPlugin instance;
	public SchematicPaster schematicPaster;
	public File schematicsDirectory;
	public final Map<String, File> schematicFiles = new HashMap<>();
	public final TreeMap<String, SchematicPaster> availablePasters = new TreeMap<>();

	protected final Map<File, Vector> schematicDimensions = new ConcurrentHashMap<>();

	private boolean worldEdit;
	private boolean fawe;

	public SchematicManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		this.worldEdit = instance.isHookedPluginEnabled("WorldEdit");
		this.fawe = instance.isHookedPluginEnabled("FastAsyncWorldEdit")
				|| instance.isHookedPluginEnabled("AsyncWorldEdit");
		this.schematicsDirectory = new File(instance.getDataFolder() + File.separator + "schematics");
		if (!this.schematicsDirectory.exists()) {
			this.schematicsDirectory.mkdirs();
		}
		copyDefaultSchematicsIfMissing();
		loadCache();
		setPasterFromConfig();
	}

	@Override
	public void unload() {
		this.worldEdit = false;
		this.fawe = false;
		if (schematicPaster != null) {
			SchematicPaster paster = schematicPaster;
			paster.clearCache();
			instance.getStorageManager().getOnlineUsers().stream().map(UserData::getUUID).filter(Objects::nonNull)
					.forEach(paster::cancelPaste);
		}
	}

	/**
	 * Initializes and sets the schematic paster based on the configuration setting.
	 * <p>
	 * This method determines which schematic paster to use (FAWE, WorldEdit, or
	 * InternalAsync) by checking compatibility, integration hooks, and fallback
	 * options. If the selected paster is unavailable or incompatible, it falls back
	 * to using the internal asynchronous paster.
	 */
	private void setPasterFromConfig() {
		final String selected = instance.getConfigManager().schematicPaster();
		SchematicPaster paster = null;

		instance.debug("Loading schematic pasters...");

		availablePasters.clear();

		switch (selected.toLowerCase(Locale.ENGLISH)) {
		case "fawe" -> {
			if (fawe) {
				if (FastAsyncWorldEditHook.isWorking()) {
					if (instance.getIntegrationManager().isHooked("FastAsyncWorldEdit")) {
						paster = new FastAsyncWorldEditHook(instance);
						instance.debug("Registering FAWE as island schematic paster.");
					} else {
						instance.getPluginLogger()
								.warn("FAWE was not able to be hooked up correctly, are you using the latest version?");
					}
				} else {
					instance.getPluginLogger().warn("FAWE is incompatible with this Minecraft version.");
				}
			}
		}
		case "worldedit" -> {
			if (worldEdit) {
				if (WorldEditHook.isWorking()) {
					if (instance.getIntegrationManager().isHooked("WorldEdit", "7")) {
						paster = new WorldEditHook(instance);
						instance.debug("Registering WorldEdit as island schematic paster.");
					} else {
						final String version = WorldEdit.getVersion();
						if (!version.startsWith("7.")) {
							instance.getPluginLogger().warn("WorldEdit version must be 7.0 or higher to be usable.");
						}
					}
				} else {
					instance.getPluginLogger().warn("WorldEdit is incompatible with this Minecraft version.");
				}
			}
		}
		case "internalasync" -> {
			paster = new SchematicAsync(instance); // Always safe fallback
			instance.debug("Registering internalAsync as island schematic paster.");
		}
		default -> {
			instance.getPluginLogger().warn("Unknown schematic paster selected in config: " + selected);
		}
		}

		if (paster == null) {
			instance.getPluginLogger().warn("Falling back to internal async paster.");
			paster = new SchematicAsync(instance);
		}

		this.schematicPaster = paster;
		instance.debug("Active island schematic paster set to: " + schematicPaster.getClass().getSimpleName());
		availablePasters.put(selected, paster);
	}

	/**
	 * Retrieves all files located in the schematics directory.
	 *
	 * @return an array of schematic {@link File} objects, or {@code null} if the
	 *         directory does not exist.
	 */
	@Nullable
	private File[] getSchematics() {
		return this.schematicsDirectory.listFiles();
	}

	/**
	 * Copies default schematic and metadata files from the plugin JAR into the
	 * schematics folder if they are not already present. This is used to bootstrap
	 * the schematic system with defaults.
	 */
	private void copyDefaultSchematicsIfMissing() {
		File folder = this.schematicsDirectory;

		// Add the list of schematic files and their metadata you expect to ship
		String[] defaultSchematics = { "custom.schem", "custom.yml" };

		for (String name : defaultSchematics) {
			File targetFile = new File(folder, name);
			if (!targetFile.exists()) {
				try (InputStream in = instance.getConfigManager()
						.getResourceMaybeGz("schematics" + File.separator + name)) {
					if (in == null) {
						instance.getPluginLogger().warn("Missing default schematic resource: schematics/" + name);
						continue;
					}
					Files.copy(in, targetFile.toPath());
					instance.getPluginLogger().info("Copied default schematic file: " + name);
				} catch (IOException e) {
					instance.getPluginLogger().severe("Failed to copy default schematic: " + name, e);
				}
			}
		}
	}

	/**
	 * Checks whether a schematic with the given name exists in the loaded schematic
	 * cache.
	 *
	 * @param schematicName the name of the schematic file (case-sensitive).
	 * @return {@code true} if the schematic is cached, otherwise {@code false}.
	 */
	public boolean schematicExists(@NotNull String schematicName) {
		return schematicFiles.containsKey(schematicName);
	}

	/**
	 * Loads and caches all valid schematic files from the schematics directory.
	 * Clears previous schematic and dimension caches. Only files with a .schem or
	 * .schematic extension are considered.
	 */
	public void loadCache() {
		schematicFiles.clear();
		for (File file : getSchematics()) {
			String name = file.getName().toLowerCase();
			if (name.endsWith(".schem") || name.endsWith(".schematic")) {
				schematicFiles.put(file.getName(), file);
				instance.debug("Found island schematic file: " + file.getName());
			}
		}
		schematicDimensions.clear();
	}

	/**
	 * Pastes a schematic file into the specified world at the given origin
	 * location.
	 *
	 * @param playerId       the UUID of the player for whom the schematic is being
	 *                       pasted (used for logging or events).
	 * @param world          the target {@link World} to paste the schematic into.
	 * @param schematic      the name of the schematic file.
	 * @param pasteOrigin    the location where the schematic will be pasted.
	 * @param metadata       metadata containing home, container, tree positions,
	 *                       and biome settings.
	 * @param ignoreAirBlock if {@code true}, air blocks in the schematic will not
	 *                       overwrite existing blocks.
	 * @param animated       whether to use animation during the paste process.
	 * @return a {@link CompletableFuture} containing the computed final spawn
	 *         location after the paste.
	 */
	public CompletableFuture<Location> pasteSchematic(@NotNull UUID playerId, @NotNull World world,
			@NotNull String schematic, @NotNull Location pasteOrigin, @NotNull SchematicMetadata metadata,
			boolean ignoreAirBlock, boolean animated) {
		// Resolve schematic file or fallback
		final File file = schematicFiles.getOrDefault(schematic,
				schematicFiles.values().stream().findFirst().orElse(null));

		CompletableFuture<Location> resultFuture = new CompletableFuture<>();

		instance.getScheduler().executeSync(() -> {
			if (file == null) {
				pasteOrigin.getBlock().setType(Material.BEDROCK);
				instance.getPluginLogger().warn("Schematic '%s' not found. Fallback block set.".formatted(schematic));
				resultFuture.complete(pasteOrigin);
				return;
			}

			// === Paste using metadata and animation ===
			schematicPaster.pasteHellblock(playerId, file, pasteOrigin, ignoreAirBlock, metadata, animated)
					.whenComplete((finalSpawn, ex) -> {
						if (ex != null) {
							instance.getPluginLogger().severe(
									"Error pasting schematic '%s': %s".formatted(schematic, ex.getMessage()), ex);
							resultFuture.completeExceptionally(ex);
						} else {
							resultFuture.complete(finalSpawn); // Already computed inside pasteHellblock
						}
					});
		}, pasteOrigin);

		return resultFuture;
	}

	/**
	 * Loads associated metadata for a given schematic name from a YAML file.
	 *
	 * @param schematicName the base name (without extension) of the schematic.
	 * @return a populated {@link SchematicMetadata} object; falls back to defaults
	 *         if file is missing or invalid.
	 */
	@NotNull
	public SchematicMetadata loadSchematicMetadata(@NotNull String schematicName) {
		File metaFile = new File(schematicsDirectory, schematicName + ".yml");

		if (!metaFile.exists()) {
			instance.getPluginLogger().warn("Missing metadata for schematic: " + schematicName + ". Using defaults.");
			return getDefaultMetadata();
		}

		try {
			YamlDocument config = YamlDocument.create(metaFile,
					new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), // Empty internal defaults
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(false).build(), DumperSettings.builder().build());

			Vector home = toVector(config.getDoubleList("home"), null);
			Vector container = toVector(config.getDoubleList("container"), null);
			float yaw = config.getFloat("yaw", 0f);
			Vector tree = toVector(config.getDoubleList("tree"), null);

			String author = config.getString("author", null);

			String biomeStr = config.getString("biome", HellBiome.NETHER_WASTES.name().toUpperCase(Locale.ENGLISH));
			HellBiome biome;

			try {
				biome = HellBiome.valueOf(biomeStr.toUpperCase(Locale.ENGLISH));
			} catch (IllegalArgumentException ex) {
				biome = HellBiome.NETHER_WASTES;
				instance.getPluginLogger().warn("Invalid biome '" + biomeStr + "' in schematic metadata for "
						+ schematicName + ".yml. Using default Nether Wastes biome.");
			}

			return new SchematicMetadata(home, container, tree, yaw, author, biome);

		} catch (IOException e) {
			instance.getPluginLogger().severe("Failed to load schematic metadata from " + schematicName + ".yml", e);
			return getDefaultMetadata();
		}
	}

	/**
	 * Converts a list of three double values into a {@link Vector}, or returns a
	 * fallback if invalid.
	 *
	 * @param list     the list of coordinates (must be of size 3).
	 * @param fallback a fallback {@link Vector} to use if the input list is invalid
	 *                 or null.
	 * @return the resulting {@link Vector}, or the fallback.
	 */
	@Nullable
	private Vector toVector(@Nullable List<Double> list, @Nullable Vector fallback) {
		if (list == null || list.size() != 3)
			return fallback;
		return new Vector(list.get(0), list.get(1), list.get(2));
	}

	/**
	 * Constructs a default {@link SchematicMetadata} object with predefined values,
	 * such as Nether Wastes biome and null positions.
	 *
	 * @return a default metadata instance.
	 */
	@NotNull
	private SchematicMetadata getDefaultMetadata() {
		return new SchematicMetadata(null, // home
				null, // container
				null, // tree
				0f, // yaw
				null, // author
				HellBiome.NETHER_WASTES // biome
		);
	}

	/**
	 * Attempts to find a safe spawn location within a cuboid defined by the origin,
	 * width, height, and length.
	 *
	 * A location is considered safe if the block below is solid, and both the feet
	 * and head blocks are empty.
	 *
	 * @param world  the {@link World} to search in.
	 * @param origin the base origin location of the schematic.
	 * @param width  the X-dimension of the schematic.
	 * @param height the Y-dimension of the schematic.
	 * @param length the Z-dimension of the schematic.
	 * @param mode   spawn search mode - either closest to center or first valid
	 *               found.
	 * @return a safe spawn {@link Location}, or a fallback if none is found.
	 */
	@NotNull
	public Location findSafeSpawn(@NotNull World world, @NotNull Location origin, int width, int height, int length,
			@NotNull SpawnSearchMode mode) {
		int startX = origin.getBlockX();
		int startY = origin.getBlockY();
		int startZ = origin.getBlockZ();

		List<Location> possibleSpawns = new ArrayList<>();

		for (int y = startY + height; y >= startY; y--) {
			for (int x = startX; x < startX + width; x++) {
				for (int z = startZ; z < startZ + length; z++) {
					Block base = world.getBlockAt(x, y, z);
					Block feet = world.getBlockAt(x, y + 1, z);
					Block head = world.getBlockAt(x, y + 2, z);

					if (base.getType().isSolid() && feet.isEmpty() && head.isEmpty()) {
						Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
						if (mode == SpawnSearchMode.FIRST_FOUND) {
							return loc;
						}
						possibleSpawns.add(loc);
					}
				}
			}
		}

		// If center priority, find the closest to schematic center
		if (mode == SpawnSearchMode.CENTER && !possibleSpawns.isEmpty()) {
			Location center = origin.clone().add(width / 2.0, height / 2.0, length / 2.0);
			return possibleSpawns.stream().min(Comparator.comparingDouble(loc -> loc.distanceSquared(center)))
					.orElse(possibleSpawns.get(0));
		}

		// Fallback
		return origin.clone().add(width / 2.0, height + 3, length / 2.0);
	}

	/**
	 * Defines the strategy for finding a safe spawn point when pasting a schematic.
	 */
	public enum SpawnSearchMode {

		/**
		 * Chooses the spawn location closest to the geometric center of the schematic
		 * area. Useful when centralized placement is desired.
		 */
		CENTER,

		/**
		 * Returns the first valid spawn location found during the search. Offers
		 * slightly better performance and ensures quick results.
		 */
		FIRST_FOUND;
	}
}