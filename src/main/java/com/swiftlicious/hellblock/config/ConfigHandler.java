package com.swiftlicious.hellblock.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.utils.LogUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;

public abstract class ConfigHandler implements Reloadable {

	protected final HellblockPlugin instance;

	public ConfigHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	protected Path resolveConfig(String filePath) {
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("ResourcePath cannot be null or empty");
		}
		filePath = filePath.replace('\\', '/');
		Path configFile = instance.getDataFolder().toPath().toAbsolutePath().resolve(filePath);
		// if the config doesn't exist, create it based on the template in the resources
		// dir
		if (!Files.exists(configFile)) {
			try {
				Files.createDirectories(configFile.getParent());
			} catch (IOException ignored) {
				// ignore
			}
			try (InputStream is = instance.getResource(filePath.replace("\\", "/"))) {
				if (is == null) {
					throw new IllegalArgumentException(
							String.format("The embedded resource '%s' cannot be found", filePath));
				}
				Files.copy(is, configFile);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		return configFile;
	}

	public YamlDocument loadConfig(String filePath) {
		return loadConfig(filePath, '.');
	}

	public YamlDocument loadConfig(String filePath, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(resolveConfig(filePath).toFile())) {
			return YamlDocument.create(inputStream, instance.getResource(filePath.replace("\\", "/")),
					GeneralSettings.builder().setRouteSeparator(routeSeparator).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build(), UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build());
		} catch (IOException ex) {
			LogUtils.severe(String.format("Failed to load config %s", filePath), ex);
			throw new RuntimeException(ex);
		}
	}

	public YamlDocument loadData(File file) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream);
		} catch (IOException ex) {
			LogUtils.severe(String.format("Failed to load config %s", file), ex);
			throw new RuntimeException(ex);
		}
	}

	public YamlDocument loadData(File file, char routeSeparator) {
		try (InputStream inputStream = new FileInputStream(file)) {
			return YamlDocument.create(inputStream,
					GeneralSettings.builder().setRouteSeparator(routeSeparator).build());
		} catch (IOException ex) {
			LogUtils.severe(String.format("Failed to load config %s", file), ex);
			throw new RuntimeException(ex);
		}
	}
}
