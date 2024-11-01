package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import com.google.common.io.Files;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.FastAsyncWorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class SchematicManager {

	private final HellblockPlugin instance;
	public SchematicPaster schematicPaster;
	public final Map<String, File> schematicFiles;
	public final Set<String> availableSchematics;
	public final TreeMap<String, SchematicPaster> availablePasters;

	private final boolean worldEdit = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
	private final boolean fawe = Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")
			|| Bukkit.getPluginManager().isPluginEnabled("AsyncWorldEdit");

	public SchematicManager(HellblockPlugin plugin) {
		instance = plugin;
		availablePasters = new TreeMap<>();
		if ((worldEdit) && WorldEditHook.isWorking()) {
			availablePasters.put("worldedit", new WorldEditHook());
			instance.getIntegrationManager().hookMessage("WorldEdit");
		}
		if ((fawe) && FastAsyncWorldEditHook.isWorking()) {
			availablePasters.put("fawe", new FastAsyncWorldEditHook());
			instance.getIntegrationManager().hookMessage("FastAsyncWorldEdit");
		}

		if ((worldEdit) && !WorldEditHook.isWorking()) {
			LogUtils.warn("WorldEdit version doesn't support this minecraft version, disabling WorldEdit integration.");
		}
		if ((fawe) && !FastAsyncWorldEditHook.isWorking()) {
			LogUtils.warn(
					"FAWE version does not implement API correctly, did you miss an update? Disabling FAWE integration.");
		}

		setPasterFromConfig();

		this.schematicFiles = new HashMap<>();
		this.availableSchematics = new HashSet<>();
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(file.getName(), file);
			availableSchematics.add(Files.getNameWithoutExtension(file.getName()));
		}
	}

	public void reload() {
		loadCache();
		setPasterFromConfig();
		schematicPaster.clearCache();
	}

	private void setPasterFromConfig() {
		String paster = instance.getHellblockHandler().getPaster();
		if (availablePasters.containsKey(paster))
			this.schematicPaster = availablePasters.get(paster);
		else {
			LogUtils.warn(String.format(
					"Configuration error, selected paster [%s] is not available, available choices are %s", paster,
					availablePasters.keySet()));
			LogUtils.warn("This is only a problem if you decide to use schematics as hellblock island options.");
		}
	}

	public void loadCache() {
		schematicFiles.clear();
		availableSchematics.clear();
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(file.getName(), file);
			availableSchematics.add(Files.getNameWithoutExtension(file.getName()));
		}
	}

	public @NonNull CompletableFuture<Void> pasteSchematic(@NonNull String schematic, @NonNull ProtectedRegion region) {
		CompletableFuture<Void> completableFuture = new CompletableFuture<>();
		Location location = instance.getWorldGuardHandler().getCenter(region)
				.toLocation(instance.getHellblockHandler().getHellblockWorld());
		location.add(0, instance.getHellblockHandler().getHeight(), 0);
		File file = schematicFiles.getOrDefault(schematic, schematicFiles.values().stream().findFirst().orElse(null));
		instance.getScheduler().runTaskSync(() -> {
			if (file == null) {
				location.getBlock().setType(Material.BEDROCK);
				LogUtils.warn(String.format("Could not find schematic %s.", schematic));
			} else {
				if (fawe) {
					instance.getScheduler()
							.runTaskAsync(() -> schematicPaster.pasteHellblock(file, location, completableFuture));
				} else {
					schematicPaster.pasteHellblock(file, location, completableFuture);
				}
			}
		}, location);
		return completableFuture;
	}
}
