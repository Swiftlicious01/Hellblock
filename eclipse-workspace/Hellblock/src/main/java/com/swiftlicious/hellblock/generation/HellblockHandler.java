package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import com.onarandombox.MultiverseCore.api.MVWorldManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

import lombok.Getter;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;

@Getter
public class HellblockHandler {

	private final HellblockPlugin instance;
	public String worldName;
	public int spawnSize;
	public String netherCMD;
	public String schematic;
	public boolean classicShape;
	public int height;
	public int distance;
	public boolean worldguardProtect;
	public int protectionRange;
	public @NotNull List<String> chestItems;
	public String allowPvP;
	public Map<UUID, HellblockPlayer> activePlayers;
	public File playersDirectory;
	public File schematicsDirectory;
	public File lastHellblockFile;
	public YamlConfiguration lastHellblock;

	@Setter
	private MVWorldManager mvWorldManager;

	public HellblockHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.worldName = instance.getConfig("config.yml").getString("general.world", "hellworld");
		this.spawnSize = instance.getConfig("config.yml").getInt("general.spawnSize", 50);
		this.schematic = instance.getConfig("config.yml").getString("hellblock.schematic", "yourschematichere");
		this.netherCMD = instance.getConfig("config.yml").getString("general.netherCMD", "/spawn");
		this.classicShape = instance.getConfig("config.yml").getBoolean("hellblock.useClassicShape", false);
		this.chestItems = instance.getConfig("config.yml").getStringList("hellblock.starter-chest");
		this.height = instance.getConfig("config.yml").getInt("hellblock.height", 150);
		this.distance = instance.getConfig("config.yml").getInt("hellblock.distance", 110);
		this.worldguardProtect = instance.getConfig("config.yml").getBoolean("hellblock.worldguardProtect");
		this.protectionRange = instance.getConfig("config.yml").getInt("hellblock.protectionRange", 105);
		this.allowPvP = instance.getConfig("config.yml").getString("hellblock.allowPvP", "deny");
		this.activePlayers = new HashMap<>();
		this.playersDirectory = new File(instance.getDataFolder() + File.separator + "players");
		if (!this.playersDirectory.exists())
			this.playersDirectory.mkdirs();
		this.schematicsDirectory = new File(instance.getDataFolder() + File.separator + "schematics");
		if (!this.schematicsDirectory.exists())
			this.schematicsDirectory.mkdirs();
	}

	public void createHellblock(Player player) {
		HellblockPlayer pi = (HellblockPlayer) this.getActivePlayers().get(player.getUniqueId());
		Location last = this.getLastHellblock();

		Location next;
		for (next = this.nextHellblockLocation(last); this
				.hellblockInSpawn(next); next = this.nextHellblockLocation(next)) {
		}
		this.setLastHellblock(next);

		pi.setHellblock(true, next);
		HellblockPlugin.getInstance().getIslandGenerator().generateHellblock(next, player);
		if (this.worldguardProtect) {
			HellblockPlugin.getInstance().getWorldGuardHandler().protectHellblock(player);
		} else {
			// TODO: island protection
		}
		pi.setHellblockOwner(player.getUniqueId());
		pi.setHellblockBiome(HellBiome.NETHER_WASTES);
		pi.setBiomeCooldown(0L);
		pi.setHellblockParty(new ArrayList<>());

		player.teleport(pi.getHomeLocation());
		instance.debug("Creating new hellblock for " + player.getName());
		instance.getAdventureManager().sendMessageWithPrefix(player, "<red>Creating new hellblock!");
	}

	public boolean hellblockInSpawn(Location location) {
		if (location == null) {
			return true;
		} else {
			return location.getX() > (double) (0 - this.spawnSize) && location.getX() < (double) this.spawnSize
					&& location.getZ() > (double) (0 - this.spawnSize) && location.getZ() < (double) this.spawnSize;
		}
	}

	public void reloadLastHellblock() {
		if (this.lastHellblockFile == null) {
			this.lastHellblockFile = new File(instance.getDataFolder(), "lastHellblock.yml");
		}

		if (!this.lastHellblockFile.exists()) {
			instance.saveResource("lastHellblock.yml", false);
		}

		this.lastHellblock = YamlConfiguration.loadConfiguration(this.lastHellblockFile);
	}

	public Location nextHellblockLocation(Location last) {
		int x = (int) last.getX();
		int z = (int) last.getZ();
		if (x < z) {
			if (-1 * x < z) {
				last.setX(last.getX() + (double) this.distance);
				return last;
			} else {
				last.setZ(last.getZ() + (double) this.distance);
				return last;
			}
		} else if (x > z) {
			if (-1 * x >= z) {
				last.setX(last.getX() - (double) this.distance);
				return last;
			} else {
				last.setZ(last.getZ() - (double) this.distance);
				return last;
			}
		} else if (x <= 0) {
			last.setZ(last.getZ() + (double) this.distance);
			return last;
		} else {
			last.setZ(last.getZ() - (double) this.distance);
			return last;
		}
	}

	public YamlConfiguration getLastHellblockConfig() {
		if (this.lastHellblock == null) {
			this.reloadLastHellblock();
		}

		return this.lastHellblock;
	}

	public Location getLastHellblock() {
		this.reloadLastHellblock();
		World world = getHellblockWorld();
		int x = this.getLastHellblockConfig().getInt("last.x");
		int y = this.height;
		int z = this.getLastHellblockConfig().getInt("last.z");
		return new Location(world, (double) x, (double) y, (double) z);
	}

	public void setLastHellblock(Location location) {
		this.getLastHellblockConfig().set("last.x", location.getBlockX());
		this.getLastHellblockConfig().set("last.z", location.getBlockZ());

		try {
			this.getLastHellblockConfig().save(this.lastHellblockFile);
		} catch (IOException var3) {
			var3.printStackTrace();
		}
	}

	public File[] getSchematics() {
		return this.schematicsDirectory.listFiles();
	}

	public Map<UUID, HellblockPlayer> getActivePlayers() {
		return this.activePlayers;
	}

	public void addActivePlayer(Player player, HellblockPlayer pi) {
		UUID id = player.getUniqueId();
		this.activePlayers.put(id, pi);
	}

	public void removeActivePlayer(Player player) {
		UUID id = player.getUniqueId();
		if (this.activePlayers.containsKey(id)) {
			((HellblockPlayer) this.activePlayers.get(id)).saveHellblockPlayer();
			this.activePlayers.remove(id);
		}
	}

	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return new VoidGenerator();
	}

	public World getHellblockWorld() {
		World hellblockWorld = Bukkit.getWorld(this.worldName);
		if (hellblockWorld == null) {
			hellblockWorld = WorldCreator.name(this.worldName).type(WorldType.FLAT).generateStructures(false)
					.environment(Environment.NETHER).generator(new VoidGenerator()).createWorld();
			instance.debug("Created the new world to be used for Hellblock islands: " + this.worldName);
			if (getMvWorldManager() != null) {
				getMvWorldManager().addWorld(hellblockWorld.getName(), Environment.NETHER,
						String.valueOf(hellblockWorld.getSeed()), WorldType.FLAT, false,
						instance.getPluginMeta().getName(), true);
			}
		}
		return hellblockWorld;
	}
}
