package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import com.onarandombox.MultiverseCore.api.MVWorldManager;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;

@Getter
public class HellblockHandler {

	private final HellblockPlugin instance;
	public String worldName;
	public int spawnSize;
	public String netherCMD;
	public int height;
	public int distance;
	public List<String> islandOptions;
	public boolean worldguardProtect;
	public boolean disableBedExplosions;
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
		this.netherCMD = instance.getConfig("config.yml").getString("general.netherCMD", "/spawn");
		this.chestItems = instance.getConfig("config.yml").getStringList("hellblock.starter-chest");
		this.height = instance.getConfig("config.yml").getInt("hellblock.height", 150);
		this.islandOptions = instance.getConfig("config.yml").getStringList("hellblock.island-options");
		this.distance = instance.getConfig("config.yml").getInt("hellblock.distance", 110);
		this.worldguardProtect = instance.getConfig("config.yml").getBoolean("hellblock.worldguardProtect");
		this.worldguardProtect = instance.getConfig("config.yml").getBoolean("hellblock.disable-bed-explosions", true);
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

	public void createHellblock(Player player, IslandOptions islandChoice, String schematic) {
		HellblockPlayer pi = this.getActivePlayer(player);
		Location last = this.getLastHellblock();

		Location next;
		for (next = this.nextHellblockLocation(last); this
				.hellblockInSpawn(next); next = this.nextHellblockLocation(next)) {
		}
		this.setLastHellblock(next);

		pi.setHellblock(true, next);
		pi.setIslandChoice(islandChoice);
		if (islandChoice == IslandOptions.SCHEMATIC && schematic != null && !schematic.isEmpty()) {
			pi.setUsedSchematic(schematic);
		}
		instance.getIslandChoiceConverter().convertIslandChoice(player, next);
		if (this.worldguardProtect) {
			instance.getWorldGuardHandler().protectHellblock(player);
		} else {
			// TODO: island protection
		}
		pi.setHellblockBiome(HellBiome.NETHER_WASTES);
		pi.setBiomeCooldown(0L);
		pi.setHellblockOwner(player.getUniqueId());
		pi.setHellblockParty(new ArrayList<>());

		player.teleportAsync(pi.getHomeLocation());
		instance.debug(String.format("Creating new hellblock for %s", player.getName()));
		instance.getAdventureManager().sendMessageWithPrefix(player, "<red>Creating new hellblock!");
		pi.saveHellblockPlayer();

		// if raining give player a bit of protection
		if (instance.getLavaRain().getLavaRainTask() != null && instance.getLavaRain().getLavaRainTask().isLavaRaining()
				&& instance.getLavaRain().getHighestBlock(player.getLocation()) != null
				&& !instance.getLavaRain().getHighestBlock(player.getLocation()).isEmpty()) {
			player.setNoDamageTicks(5 * 20);
		}
	}

	public void createHellblock(Player player, IslandOptions islandChoice) {
		createHellblock(player, islandChoice, null);
	}

	public void resetHellblock(UUID id, boolean forceReset) {
		HellblockPlayer pi;
		if (getActivePlayers().get(id) != null) {
			pi = getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		int z_operate;
		Location loc = pi.getHellblockLocation();
		double y = loc.getY();
		z_operate = (int) loc.getX() - getDistance();

		while (true) {
			if (z_operate > (int) loc.getX() + getDistance()) {
				instance.getWorldGuardHandler().unprotectHellblock(id, forceReset);
				pi.setHellblock(false, null);
				pi.setHellblockOwner(null);
				pi.setHellblockBiome(null);
				pi.setBiomeCooldown(0L);
				pi.setUsedSchematic(null);
				pi.setIslandChoice(null);
				pi.setResetCooldown(!forceReset ? Duration.ofDays(1).toHours() : 0L);
				List<UUID> visitors = instance.getCoopManager().getVisitors(id);
				for (UUID uuid : visitors) {
					Player visitor = Bukkit.getPlayer(uuid);
					if (visitor != null && visitor.isOnline()) {
						visitor.performCommand(getNetherCMD());
					} else {
						File visitorFile = new File(getPlayersDirectory() + File.separator + uuid + ".yml");
						YamlConfiguration visitorConfig = YamlConfiguration.loadConfiguration(visitorFile);
						visitorConfig.set("player.in-unsafe-island", true);
						try {
							visitorConfig.save(visitorFile);
						} catch (IOException ex) {
							LogUtils.severe(String.format("Unable to save visitor file for %s!", uuid), ex);
						}
					}
				}
				List<UUID> party = pi.getHellblockParty();
				if (party != null && !party.isEmpty()) {
					for (UUID uuid : party) {
						Player member = Bukkit.getPlayer(uuid);
						if (member != null && member.isOnline()) {
							HellblockPlayer hbMember = getActivePlayer(member);
							hbMember.setHellblock(false, null);
							hbMember.setHellblockOwner(null);
							hbMember.setHellblockBiome(null);
							hbMember.setBiomeCooldown(0L);
							hbMember.setResetCooldown(0L);
							hbMember.setIslandChoice(null);
							hbMember.setUsedSchematic(null);
							hbMember.setHellblockParty(new ArrayList<>());
							hbMember.saveHellblockPlayer();
							member.performCommand(getNetherCMD());
							if (!forceReset) {
								Player player = Bukkit.getPlayer(id);
								if (player != null) {
									instance.getAdventureManager().sendMessageWithPrefix(member, String.format(
											"<red>Your hellblock owner <dark_red>%s <red>has reset the island, so you have been removed.",
											player.getName()));
								}
							} else {
								instance.getAdventureManager().sendMessageWithPrefix(member,
										"<red>The hellblock party you were a part of has been forcefully deleted.");
							}
						} else {
							File memberFile = new File(getPlayersDirectory() + File.separator + uuid + ".yml");
							YamlConfiguration memberConfig = YamlConfiguration.loadConfiguration(memberFile);
							memberConfig.set("player.hasHellblock", false);
							memberConfig.set("player.hellblock", null);
							memberConfig.set("player.home", null);
							memberConfig.set("player.owner", null);
							memberConfig.set("player.biome", null);
							memberConfig.set("player.party", null);
							memberConfig.set("player.reset-cooldown", null);
							memberConfig.set("player.biome-cooldown", null);
							memberConfig.set("player.island-choice", null);
							memberConfig.set("player.island-choice.schematic", null);
							memberConfig.set("player.in-unsafe-island", true);
							try {
								memberConfig.save(memberFile);
							} catch (IOException ex) {
								LogUtils.severe(String.format("Unable to save member file for %s!", uuid), ex);
							}
						}
					}
				}
				pi.setHellblockParty(new ArrayList<>());
				break;
			}

			for (int x_operate = (int) loc.getZ() - getDistance(); x_operate <= (int) loc.getZ()
					+ getDistance(); ++x_operate) {
				if (loc.getWorld() == null)
					continue;
				Block block = loc.getWorld().getBlockAt(x_operate, (int) y, z_operate);
				if (block.getType() != Material.AIR) {
					block.setType(Material.AIR);
					block.getState().update();
				}
			}

			++z_operate;
		}
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

	public @NonNull HellblockPlayer getActivePlayer(Player player) {
		UUID id = player.getUniqueId();
		if (this.activePlayers.containsKey(id) && this.activePlayers.get(id) != null) {
			return this.activePlayers.get(id);
		}

		// in case somehow the id becomes null?
		this.activePlayers.replace(id, new HellblockPlayer(id));
		return this.activePlayers.get(id);
	}

	public @NonNull HellblockPlayer getActivePlayer(UUID playerUUID) {
		if (this.activePlayers.containsKey(playerUUID) && this.activePlayers.get(playerUUID) != null) {
			return this.activePlayers.get(playerUUID);
		}

		// in case somehow the id becomes null?
		this.activePlayers.replace(playerUUID, new HellblockPlayer(playerUUID));
		return this.activePlayers.get(playerUUID);
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
			VoidGenerator voidGen = new VoidGenerator();
			hellblockWorld = WorldCreator.name(this.worldName).type(WorldType.FLAT).generateStructures(false)
					.environment(Environment.NETHER).generator(voidGen).biomeProvider(voidGen.new VoidBiomeProvider())
					.createWorld();
			instance.debug(String.format("Created the new world to be used for Hellblock islands: %s", this.worldName));
			Location spawn = generateSpawn();
			hellblockWorld.setSpawnLocation(spawn);
			instance.debug(String.format("Generated the spawn for the new Hellblock world %s at x:%s, y:%s, z:%s",
					this.worldName, spawn.x(), spawn.y(), spawn.z()));
			if (getMvWorldManager() != null) {
				getMvWorldManager().addWorld(hellblockWorld.getName(), Environment.NETHER,
						String.valueOf(hellblockWorld.getSeed()), WorldType.FLAT, false,
						instance.getPluginMeta().getName(), true);
				instance.debug(String.format("Imported the world to Multiverse-Core: %s", this.worldName));
			}
		}
		return hellblockWorld;
	}

	public Location generateSpawn() {
		World world = HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld();
		int y = HellblockPlugin.getInstance().getHellblockHandler().getHeight();
		int spawnSize = HellblockPlugin.getInstance().getHellblockHandler().getSpawnSize();

		Location spawn = new Location(world, 0, y, 0);
		int i = 0;
		for (int x_operate = 0 - spawnSize; x_operate <= spawnSize; ++x_operate) {
			for (int z_operate = 0 - spawnSize; z_operate <= spawnSize; ++z_operate) {
				Block block = world.getBlockAt(x_operate, y, z_operate);
				block.setType(Material.RED_NETHER_BRICKS);
				block.getState().update();
				if (i == 0) {
					spawn = block
							.getLocation(new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()));
					i++;
				}
			}
		}

		HellblockPlugin.getInstance().debug("Spawn area has been generated!");
		if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
			HellblockPlugin.getInstance().getWorldGuardHandler().protectSpawn();
			HellblockPlugin.getInstance().debug("Spawn area protected with WorldGuard!");
		} else {
			// TODO: island protection
		}

		return spawn;
	}
}
