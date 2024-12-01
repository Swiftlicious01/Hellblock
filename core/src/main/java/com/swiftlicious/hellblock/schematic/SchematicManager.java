package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.api.compatibility.FastAsyncWorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;

public class SchematicManager implements Reloadable {

	protected final HellblockPlugin instance;
	public SchematicPaster schematicPaster;
	public final Map<String, File> schematicFiles;
	public final TreeMap<String, SchematicPaster> availablePasters;

	private final boolean worldEdit = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
	private final boolean fawe = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")
			|| Bukkit.getPluginManager().isPluginEnabled("AsyncWorldEdit");

	public SchematicManager(HellblockPlugin plugin) {
		instance = plugin;
		availablePasters = new TreeMap<>();
		availablePasters.put("internalAsync", new SchematicAsync());
		if ((worldEdit) && WorldEditHook.isWorking()) {
			availablePasters.put("worldedit", new WorldEditHook());
			instance.getIntegrationManager().isHooked("WorldEdit");
		}
		if ((fawe) && FastAsyncWorldEditHook.isWorking()) {
			availablePasters.put("fawe", new FastAsyncWorldEditHook());
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

		setPasterFromConfig();

		this.schematicFiles = new HashMap<>();
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(file.getName(), file);
		}
	}

	@Override
	public void reload() {
		loadCache();
		setPasterFromConfig();
		schematicPaster.clearCache();
	}

	private void setPasterFromConfig() {
		String paster = instance.getConfigManager().schematicPaster();
		if (availablePasters.containsKey(paster))
			this.schematicPaster = availablePasters.get(paster);
		else {
			instance.getPluginLogger()
					.warn(String.format(
							"Configuration error, selected paster [%s] is not available, available choices are %s",
							paster, availablePasters.keySet()));
			instance.getPluginLogger()
					.warn("This is only a problem if you decide to use schematics as hellblock island options.");
		}
	}

	public void loadCache() {
		schematicFiles.clear();
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(file.getName(), file);
		}
	}

	public @NotNull CompletableFuture<Void> pasteSchematic(@NotNull String schematic, @NotNull ProtectedRegion region) {
		CompletableFuture<Void> completableFuture = new CompletableFuture<>();
		Location location = instance.getWorldGuardHandler().getCenter(region)
				.toLocation(instance.getHellblockHandler().getHellblockWorld());
		location.add(0, instance.getConfigManager().height(), 0);
		File file = schematicFiles.getOrDefault(schematic, schematicFiles.values().stream().findFirst().orElse(null));
		instance.getScheduler().sync().run(() -> {
			if (file == null) {
				location.getBlock().setType(Material.BEDROCK);
				instance.getPluginLogger().warn(String.format("Could not find schematic %s.", schematic));
			} else {
				if (fawe) {
					instance.getScheduler().async()
							.execute(() -> schematicPaster.pasteHellblock(file, location, true, completableFuture));
				} else {
					schematicPaster.pasteHellblock(file, location, true, completableFuture);
				}
			}
		}, location);
		return completableFuture;
	}
}