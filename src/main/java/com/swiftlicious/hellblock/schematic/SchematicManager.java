package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import com.google.common.io.Files;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.FastAsyncWorldEditHook;
import com.swiftlicious.hellblock.api.compatibility.WorldEditHook;
import com.swiftlicious.hellblock.utils.LogUtils;

public class SchematicManager {

	private final HellblockPlugin instance;
	public SchematicPaster schematicPaster;
	public final Map<String, File> schematicFiles;
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
			LogUtils.warn("WorldEdit version doesn't support minecraft version, disabling WorldEdit integration.");
		}
		if ((fawe) && !FastAsyncWorldEditHook.isWorking()) {
			LogUtils.warn(
					"FAWE version does not implement API correctly, did you miss an update? Disabling FAWE integration.");
		}

		setPasterFromConfig();

		this.schematicFiles = new HashMap<>();
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(Files.getNameWithoutExtension(file.getName()), file);
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
		File parent = instance.getHellblockHandler().getSchematicsDirectory();
		for (File file : parent.listFiles()) {
			schematicFiles.put(Files.getNameWithoutExtension(file.getName()), file);
		}
	}

	public CompletableFuture<Void> pasteSchematic(String schematic, Location location) {
		List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

		completableFutures.add(pasteSchematic(schematic, location, instance.getHellblockHandler().getHellblockWorld()));

		return CompletableFuture.runAsync(() -> completableFutures.forEach(CompletableFuture::join));
	}

	private CompletableFuture<Void> pasteSchematic(String schematic, Location location, World world) {
		CompletableFuture<Void> completableFuture = new CompletableFuture<>();
		location.add(0, instance.getHellblockHandler().getHeight(), 0);
		File file = schematicFiles.getOrDefault(schematic, schematicFiles.values().stream().findFirst().orElse(null));
		instance.getScheduler().runTaskSync(() -> {
			if (file == null) {
				location.getBlock().setType(Material.BEDROCK);
				LogUtils.warn(String.format("Could not find schematic %s.", schematic));
			} else {
				if (fawe) {
					instance.getScheduler().runTaskAsync(
							() -> schematicPaster.pasteHellIsland(file, location, true, completableFuture));
				} else {
					schematicPaster.pasteHellIsland(file, location, true, completableFuture);
				}
			}
		}, location);
		return completableFuture;
	}
}
