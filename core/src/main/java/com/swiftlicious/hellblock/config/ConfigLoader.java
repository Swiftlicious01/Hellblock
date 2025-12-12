package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import dev.dejvokep.boostedyaml.YamlDocument;

/**
 * Interface for loading and managing configuration files.
 */
public interface ConfigLoader {

	/**
	 * Resolves the config path for the specified file path.
	 * 
	 * @param filePath the path to the configuration file
	 * @return the {@link Path} to the file
	 */
	Path resolveConfig(String filePath);

	/**
	 * Gets the resource in the case if it's gzip.
	 * 
	 * @param filePath the path to the configuration file
	 * @return the {@link InputStream} for the gzip file
	 */
	InputStream getResourceMaybeGz(String filePath);

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
	 * @return the loaded {@link File}
	 */
	File saveResource(String filePath);
}