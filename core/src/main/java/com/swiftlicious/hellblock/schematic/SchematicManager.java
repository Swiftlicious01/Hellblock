package com.swiftlicious.hellblock.schematic;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.player.UserData;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;

public class SchematicManager implements Reloadable {

	protected final HellblockPlugin instance;
	public SchematicPaster schematicPaster;
	public File schematicsDirectory;
	public final Map<String, File> schematicFiles = new HashMap<>();
	public final TreeMap<String, SchematicPaster> availablePasters = new TreeMap<>();

	protected final Map<File, Vector> schematicDimensions = new ConcurrentHashMap<>();

	private final boolean worldEdit = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
	private final boolean fawe = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")
			|| Bukkit.getPluginManager().isPluginEnabled("AsyncWorldEdit");

	public SchematicManager(HellblockPlugin plugin) {
		instance = plugin;
		this.schematicsDirectory = new File(instance.getDataFolder() + File.separator + "schematics");
		if (!this.schematicsDirectory.exists()) {
			this.schematicsDirectory.mkdirs();
		}
		availablePasters.put("internalAsync", new SchematicAsync(plugin));
		if ((worldEdit) && WorldEditHook.isWorking()) {
			availablePasters.put("worldedit", new WorldEditHook(plugin));
			instance.getIntegrationManager().isHooked("WorldEdit");
		}
		if ((fawe) && FastAsyncWorldEditHook.isWorking()) {
			availablePasters.put("fawe", new FastAsyncWorldEditHook(plugin));
			instance.getIntegrationManager().isHooked("FastAsyncWorldEdit");
		}
		if ((worldEdit) && !WorldEditHook.isWorking()) {
			instance.getPluginLogger()
					.warn("WorldEdit version doesn't support this minecraft version, disabling WorldEdit integration.");
		}
		if ((fawe) && !FastAsyncWorldEditHook.isWorking()) {
			instance.getPluginLogger().warn(
					"FAWE version does not implement API correctly, did you miss an update? Disabling FAWE integration.");
		}

		for (File file : getSchematics()) {
			schematicFiles.put(file.getName(), file);
		}
	}

	@Override
	public void load() {
		loadCache();
		setPasterFromConfig();
	}

	@Override
	public void unload() {
		if (schematicPaster != null) {
			SchematicPaster paster = schematicPaster;
			paster.clearCache();
			instance.getStorageManager().getOnlineUsers().stream().map(UserData::getUUID).filter(Objects::nonNull)
					.forEach(paster::cancelPaste);
		}
	}

	private void setPasterFromConfig() {
		final String paster = instance.getConfigManager().schematicPaster();
		if (availablePasters.containsKey(paster)) {
			this.schematicPaster = availablePasters.get(paster);
		} else {
			instance.getPluginLogger()
					.warn("Configuration error, selected paster [%s] is not available, available choices are %s"
							.formatted(paster, availablePasters.keySet()));
			instance.getPluginLogger()
					.warn("This is only a problem if you decide to use schematics as hellblock island options.");
		}
	}

	private @NotNull File[] getSchematics() {
		return this.schematicsDirectory.listFiles();
	}

	public void loadCache() {
		schematicFiles.clear();
		for (File file : getSchematics()) {
			schematicFiles.put(file.getName(), file);
		}
		schematicDimensions.clear();
	}

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

	public SchematicMetadata loadSchematicMetadata(String schematicName) {
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
			Vector chest = toVector(config.getDoubleList("chest"), null);
			float yaw = config.getFloat("yaw", 0f);
			Vector tree = toVector(config.getDoubleList("tree"), null);

			String author = config.getString("author", null);

			return new SchematicMetadata(home, chest, tree, yaw, author);

		} catch (IOException e) {
			instance.getPluginLogger().severe("Failed to load schematic metadata from " + schematicName + ".yml", e);
			return getDefaultMetadata();
		}
	}

	private @Nullable Vector toVector(List<Double> list, @Nullable Vector fallback) {
		if (list == null || list.size() != 3)
			return fallback;
		return new Vector(list.get(0), list.get(1), list.get(2));
	}

	private SchematicMetadata getDefaultMetadata() {
		return new SchematicMetadata(null, // home
				null, // chest
				null, // tree
				0f, // yaw
				null // author
		);
	}

	public Location findSafeSpawn(World world, Location origin, int width, int height, int length,
			SpawnSearchMode mode) {
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

	public enum SpawnSearchMode {
		CENTER, FIRST_FOUND;
	}
}