package com.swiftlicious.hellblock.listeners.fishing;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

public class StatisticsManager implements StatisticsManagerInterface {

	protected final HellblockPlugin instance;
	private final Map<String, List<String>> categoryMap = new HashMap<>();

	public StatisticsManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		this.loadCategoriesFromPluginFolder();
		for (Map.Entry<String, List<String>> entry : categoryMap.entrySet()) {
			instance.debug("Category: {" + entry.getKey() + "} Members: " + entry.getValue());
		}
	}

	@Override
	public void unload() {
		this.categoryMap.clear();
	}

	public void loadCategoriesFromPluginFolder() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (String type : List.of("category")) {
			File typeFolder = new File(instance.getDataFolder() + File.separator + "contents" + File.separator + type);
			if (!typeFolder.exists()) {
				if (!typeFolder.mkdirs())
					return;
				instance.saveResource("contents" + File.separator + type + File.separator + "default.yml", false);
			}
			fileDeque.push(typeFolder);
			while (!fileDeque.isEmpty()) {
				File file = fileDeque.pop();
				File[] files = file.listFiles();
				if (files == null)
					continue;
				for (File subFile : files) {
					if (subFile.isDirectory()) {
						fileDeque.push(subFile);
					} else if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
						this.loadSingleFile(subFile);
					}
				}
			}
		}
	}

	private void loadSingleFile(File file) {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (String key : config.getKeys(false)) {
			categoryMap.put(key, config.getStringList(key));
		}
	}

	@NotNull
	@Override
	public List<String> getCategoryMembers(String key) {
		return categoryMap.getOrDefault(key, List.of());
	}
}