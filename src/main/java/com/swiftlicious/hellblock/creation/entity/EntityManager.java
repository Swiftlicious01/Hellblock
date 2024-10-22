package com.swiftlicious.hellblock.creation.entity;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.loot.Loot;
import com.swiftlicious.hellblock.utils.LogUtils;

public class EntityManager implements EntityManagerInterface {

	private final HellblockPlugin instance;
	private final Map<String, EntityLibrary> entityLibraryMap;
	private final Map<String, EntityConfig> entityConfigMap;

	public EntityManager(HellblockPlugin plugin) {
		instance = plugin;
		this.entityLibraryMap = new HashMap<>();
		this.entityConfigMap = new HashMap<>();
		this.registerEntityLibrary(new VanillaEntity());
	}

	public void load() {
		this.loadConfig();
	}

	public void unload() {
		Map<String, EntityConfig> tempMap = new HashMap<>(this.entityConfigMap);
		this.entityConfigMap.clear();
		for (Map.Entry<String, EntityConfig> entry : tempMap.entrySet()) {
			if (entry.getValue().isPersist()) {
				tempMap.put(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Registers an entity library for use in the plugin.
	 *
	 * @param entityLibrary The entity library to register.
	 * @return {@code true} if the entity library was successfully registered,
	 *         {@code false} if it already exists.
	 */
	@Override
	public boolean registerEntityLibrary(EntityLibrary entityLibrary) {
		if (entityLibraryMap.containsKey(entityLibrary.identification()))
			return false;
		else
			entityLibraryMap.put(entityLibrary.identification(), entityLibrary);
		return true;
	}

	/**
	 * Unregisters an entity library by its identification key.
	 *
	 * @param identification The identification key of the entity library to
	 *                       unregister.
	 * @return {@code true} if the entity library was successfully unregistered,
	 *         {@code false} if it does not exist.
	 */
	@Override
	public boolean unregisterEntityLibrary(String identification) {
		return entityLibraryMap.remove(identification) != null;
	}

	/**
	 * Load configuration files for entity properties.
	 */
	private void loadConfig() {
		Deque<File> fileDeque = new ArrayDeque<>();
		for (String type : List.of("entity")) {
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

	/**
	 * Load a single entity configuration file.
	 *
	 * @param file The YAML file to load.
	 */
	private void loadSingleFile(File file) {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (Map.Entry<String, Object> entry : config.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection section) {
				String entityID = section.getString("entity");
				if (entityID == null) {
					LogUtils.warn(String.format("Entity can't be null. File: %s; Section: %s", file.getAbsolutePath(),
							section.getCurrentPath()));
					continue;
				}
				HashMap<String, Object> propertyMap = new HashMap<>();
				ConfigurationSection property = section.getConfigurationSection("properties");
				if (property != null) {
					propertyMap.putAll(property.getValues(false));
				}
				EntityConfig entityConfig = new EntityConfig.Builder().entityID(entityID).persist(false)
						.horizontalVector(section.getDouble("velocity.horizontal", 1.1))
						.verticalVector(section.getDouble("velocity.vertical", 1.2)).propertyMap(propertyMap).build();
				entityConfigMap.put(entry.getKey(), entityConfig);
			}
		}
	}

	public void disable() {
		unload();
		this.entityConfigMap.clear();
		this.entityLibraryMap.clear();
	}

	/**
	 * Summons an entity based on the given loot configuration to a specified
	 * location.
	 *
	 * @param hookLocation   The location where the entity will be summoned,
	 *                       typically where the fishing hook is.
	 * @param playerLocation The location of the player who triggered the entity
	 *                       summoning.
	 * @param loot           The loot configuration that defines the entity to be
	 *                       summoned.
	 */
	@Override
	public void summonEntity(Location hookLocation, Location playerLocation, Loot loot) {
		EntityConfig config = entityConfigMap.get(loot.getID());
		if (config == null) {
			LogUtils.warn(String.format("Entity: %s doesn't exist.", loot.getID()));
			return;
		}
		String entityID = config.getEntityID();
		Entity entity;
		if (entityID.contains(":")) {
			String[] split = entityID.split(":", 2);
			String identification = split[0];
			String id = split[1];
			EntityLibrary library = entityLibraryMap.get(identification);
			entity = library.spawn(hookLocation, id, config.getPropertyMap());
		} else {
			entity = entityLibraryMap.get("vanilla").spawn(hookLocation, entityID, config.getPropertyMap());
		}
		Vector vector = playerLocation.subtract(hookLocation).toVector().multiply((config.getHorizontalVector()) - 1);
		vector = vector.setY((vector.getY() + 0.2) * config.getVerticalVector());
		entity.setVelocity(vector);
	}
}