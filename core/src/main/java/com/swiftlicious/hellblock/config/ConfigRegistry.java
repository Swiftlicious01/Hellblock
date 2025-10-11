package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.swiftlicious.hellblock.HellblockPlugin;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

public class ConfigRegistry {
	private final Map<String, Section> registry = new HashMap<>();
	private final HellblockPlugin plugin;

	public ConfigRegistry(HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Load main config.yml and merge all contents/item + contents/block + contents/entity folders.
	 */
	public void load() {
		registry.clear();

		// 1. Register top-level config.yml sections (tools, armor, barter, etc.)
		final YamlDocument config = plugin.getConfigManager().getMainConfig();
		config.getRoutesAsStrings(false).forEach(key -> {
			final Section sub = config.getSection(key);
			if (sub != null) {
				registry.put(key, sub);
			}
		});

		// 2. Register contents/items/*.yml
		loadFolder("contents/item", "contents.item");

		// 3. Register contents/entity/*.yml
		loadFolder("contents/entity", "contents.entity");

		// 3. Register contents/block/*.yml
		loadFolder("contents/block", "contents.block");
	}

	private void loadFolder(String folderPath, String namespace) {
		final File folder = new File(plugin.getDataFolder(), folderPath);
		if (!folder.exists() || !folder.isDirectory()) {
			return;
		}

		final File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
		if (files == null) {
			return;
		}

		for (File file : files) {
			try {
				// Load the file as a YamlDocument
				final YamlDocument doc = YamlDocument.create(file);

				final String fileName = file.getName().replace(".yml", "");
				final Section section = doc.getSection("");

				if (section != null) {
					// e.g. contents/item/flaming_rod.yml → contents.item.flaming_rod
					final String id = namespace + "." + fileName;
					registry.put(id, section);

					// flatten inner subsections for convenience
					doc.getRoutesAsStrings(false).forEach(route -> {
						final Section inner = doc.getSection(route);
						if (inner != null) {
							registry.put(id + "." + route, inner);
						}
					});
				}
			} catch (IOException ex) {
				plugin.getPluginLogger().warn("Failed to load config file: " + file.getName());
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Get a Section by path (dot notation supported).
	 */
	public Section getSection(String path) {
		return registry.get(path);
	}

	/**
	 * Check if a section exists for the given path.
	 */
	public boolean hasSection(String path) {
		return registry.containsKey(path);
	}
}