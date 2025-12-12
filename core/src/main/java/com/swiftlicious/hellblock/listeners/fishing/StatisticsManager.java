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
		categoryMap.entrySet()
				.forEach(entry -> instance.debug("Category: {" + entry.getKey() + "} Members: " + entry.getValue()));
	}

	@Override
	public void unload() {
		this.categoryMap.clear();
	}

	public void loadCategoriesFromPluginFolder() {
		final File rootFolder = new File(instance.getDataFolder(), "contents" + File.separator + "category");
		if (!rootFolder.exists() && !rootFolder.mkdirs()) {
			instance.getPluginLogger().severe("Failed to create contents/category directory.");
			return;
		}

		// Ensure at least one default file exists
		instance.getConfigManager()
				.saveResource("contents" + File.separator + "category" + File.separator + "default.yml");

		final Deque<File> stack = new ArrayDeque<>();
		stack.push(rootFolder);

		while (!stack.isEmpty()) {
			final File dir = stack.pop();
			final File[] files = dir.listFiles();
			if (files == null)
				continue;

			for (File subFile : files) {
				if (subFile.isDirectory()) {
					stack.push(subFile);
				} else if (subFile.isFile() && subFile.getName().endsWith(".yml")) {
					try {
						this.loadSingleFile(subFile);
					} catch (Exception e) {
						instance.getPluginLogger().warn("Failed to load category file: " + subFile.getPath(), e);
					}
				}
			}
		}
	}

	private void loadSingleFile(File file) {
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		config.getKeys(false).forEach(key -> categoryMap.put(key, config.getStringList(key)));
	}

	@NotNull
	@Override
	public List<String> getCategoryMembers(String key) {
		return categoryMap.getOrDefault(key, List.of());
	}
}