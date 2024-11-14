package com.swiftlicious.hellblock.config;

import java.io.File;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * Interface for loading and managing configuration files.
 */
public interface ConfigLoader {

	/**
	 * Loads a YAML configuration file from the specified file path.
	 *
	 * @param filePath the path to the configuration file
	 * @return the loaded {@link YamlDocument}
	 */
	YamlDocument loadConfig(String filePath);

	/**
	 * Loads a YAML configuration file from the specified file path with a custom
	 * route separator.
	 *
	 * @param filePath       the path to the configuration file
	 * @param routeSeparator the custom route separator character
	 * @return the loaded {@link YamlDocument}
	 */
	YamlDocument loadConfig(String filePath, char routeSeparator);

	/**
	 * Loads a YAML data file.
	 *
	 * @param file the {@link File} object representing the data file
	 * @return the loaded {@link YamlDocument}
	 */
	YamlDocument loadData(File file);

	/**
	 * Loads a YAML data file with a custom route separator.
	 *
	 * @param file           the {@link File} object representing the data file
	 * @param routeSeparator the custom route separator character
	 * @return the loaded {@link YamlDocument}
	 */
	YamlDocument loadData(File file, char routeSeparator);

	/**
	 * Saves a resource file from the plugin's jar to the specified file path.
	 *
	 * @param filePath the path where the resource file will be saved
	 */
	void saveResource(String filePath);
}