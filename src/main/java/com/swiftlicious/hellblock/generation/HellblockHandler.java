package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import com.google.common.io.Files;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
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
	public String paster;
	public int height;
	public int distance;
	public List<String> islandOptions;
	public boolean worldguardProtect;
	public boolean disableBedExplosions;
	public int protectionRange;
	public @NotNull List<String> chestItems;
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
		this.paster = instance.getConfig("config.yml").getString("hellblock.schematic-paster", "worldedit");
		this.chestItems = instance.getConfig("config.yml").getStringList("hellblock.starter-chest");
		this.height = instance.getConfig("config.yml").getInt("hellblock.height", 150);
		this.islandOptions = instance.getConfig("config.yml").getStringList("hellblock.island-options");
		this.distance = instance.getConfig("config.yml").getInt("hellblock.distance", 110);
		this.worldguardProtect = instance.getConfig("config.yml").getBoolean("hellblock.worldguardProtect");
		this.worldguardProtect = instance.getConfig("config.yml").getBoolean("hellblock.disable-bed-explosions", true);
		this.protectionRange = instance.getConfig("config.yml").getInt("hellblock.protectionRange", 105);
		this.activePlayers = new HashMap<>();
		this.playersDirectory = new File(instance.getDataFolder() + File.separator + "players");
		if (!this.playersDirectory.exists())
			this.playersDirectory.mkdirs();
		this.schematicsDirectory = new File(instance.getDataFolder() + File.separator + "schematics");
		if (!this.schematicsDirectory.exists())
			this.schematicsDirectory.mkdirs();
		startCountdowns();
	}

	public void startCountdowns() {
		HellblockPlugin.getInstance().getScheduler().runTaskAsyncTimer(() -> {
			for (HellblockPlayer active : getActivePlayers().values()) {
				if (active == null || active.getPlayer() == null)
					continue;
				if (active.getResetCooldown() > 0) {
					active.setResetCooldown(active.getResetCooldown() - 1);
				}
				if (active.getBiomeCooldown() > 0) {
					active.setBiomeCooldown(active.getBiomeCooldown() - 1);
				}
				active.saveHellblockPlayer();
			}
			for (File offline : this.getPlayersDirectory().listFiles()) {
				if (!offline.isFile() || !offline.getName().endsWith(".yml"))
					continue;
				String uuid = Files.getNameWithoutExtension(offline.getName());
				UUID id = null;
				try {
					id = UUID.fromString(uuid);
				} catch (IllegalArgumentException ignored) {
					// ignored
					continue;
				}
				if (id != null && getActivePlayers().keySet().contains(id))
					continue;
				YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
				if (offlineFile.getLong("player.reset-cooldown") > 0) {
					offlineFile.set("player.reset-cooldown", offlineFile.getLong("player.reset-cooldown") - 1);
				}
				if (offlineFile.getLong("player.biome-cooldown") > 0) {
					offlineFile.set("player.biome-cooldown", offlineFile.getLong("player.biome-cooldown") - 1);
				}
				try {
					offlineFile.save(offline);
				} catch (IOException ex) {
					LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
					continue;
				}
			}
		}, 0, 1, TimeUnit.HOURS);
	}

	public void createHellblock(Player player, IslandOptions islandChoice, String schematic) {
		HellblockPlayer pi = this.getActivePlayer(player);

		if (pi.getResetCooldown() > 0) {
			instance.getAdventureManager().sendMessageWithPrefix(player,
					String.format("<red>You have recently reset your hellblock already, you must wait for %s!",
							instance.getFormattedCooldown(pi.getResetCooldown())));
			return;
		}

		Location last = this.getLastHellblock();

		Location next = this.nextHellblockLocation(last);
		do {
			next = this.nextHellblockLocation(next);
		} while (this.hellblockInSpawn(next));
		this.setLastHellblock(next);

		pi.setHellblock(true, next, nextHellblockID());
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
		pi.setLockedStatus(false);
		pi.setHellblockOwner(player.getUniqueId());
		pi.setHellblockParty(new ArrayList<>());
		pi.setProtectionFlags(new HashSet<>());

		player.teleportAsync(pi.getHomeLocation());
		instance.debug(String.format("Creating new hellblock for %s", player.getName()));
		instance.getAdventureManager().sendMessageWithPrefix(player, "<red>Creating new hellblock!");
		instance.getScheduler().runTaskSyncLater(
				() -> instance.getBiomeHandler().changeHellblockBiome(pi, pi.getHellblockBiome(), true),
				pi.getHomeLocation(), 5 * 20);
		pi.setCreationTime(System.currentTimeMillis());
		pi.saveHellblockPlayer();
		
		instance.getAdventureManager().sendTitle(player, String.format("<red>Welcome <dark_red>%s", player.getName()), "<red>To Your Hellblock!", 5, 20, 5);

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
		Location loc = pi.getHellblockLocation();
		double y = loc.getY();
		int z_operate = (int) loc.getX() - getDistance();

		while (true) {
			if (z_operate > (int) loc.getX() + getDistance()) {
				instance.getWorldGuardHandler().unprotectHellblock(id, forceReset);
				pi.setHellblock(false, null, 0);
				pi.setHellblockOwner(null);
				pi.setTotalVisits(0);
				pi.setCreationTime(0L);
				pi.setHellblockBiome(null);
				pi.setBiomeCooldown(0L);
				pi.setUsedSchematic(null);
				pi.setLockedStatus(false);
				pi.setIslandChoice(null);
				pi.setBannedPlayers(new ArrayList<>());
				pi.setProtectionFlags(new HashSet<>());
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
				for (HellblockPlayer active : getActivePlayers().values()) {
					if (active == null || active.getPlayer() == null)
						continue;
					if (active.getWhoTrusted().contains(id)) {
						active.removeTrustPermission(id);
					}
					active.saveHellblockPlayer();
				}
				for (File offline : this.getPlayersDirectory().listFiles()) {
					if (!offline.isFile() || !offline.getName().endsWith(".yml"))
						continue;
					String uuid = Files.getNameWithoutExtension(offline.getName());
					UUID offlineID = null;
					try {
						offlineID = UUID.fromString(uuid);
					} catch (IllegalArgumentException ignored) {
						// ignored
						continue;
					}
					if (offlineID != null && getActivePlayers().keySet().contains(offlineID))
						continue;
					YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
					if (offlineFile.getStringList("player.trusted-on-islands").contains(id.toString())) {
						List<String> trusted = offlineFile.getStringList("player.trusted-on-islands");
						trusted.remove(id.toString());
						offlineFile.set("player.trusted-on-islands", trusted);
					}
					try {
						offlineFile.save(offline);
					} catch (IOException ex) {
						LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
						continue;
					}
				}
				List<UUID> party = pi.getHellblockParty();
				if (party != null && !party.isEmpty()) {
					for (UUID uuid : party) {
						Player member = Bukkit.getPlayer(uuid);
						if (member != null && member.isOnline()) {
							HellblockPlayer hbMember = getActivePlayer(member);
							hbMember.setHellblock(false, null, 0);
							hbMember.setHellblockOwner(null);
							hbMember.setHellblockBiome(null);
							hbMember.setTotalVisits(0);
							hbMember.setCreationTime(0L);
							hbMember.setBiomeCooldown(0L);
							hbMember.setResetCooldown(0L);
							hbMember.setIslandChoice(null);
							hbMember.setLockedStatus(false);
							hbMember.setUsedSchematic(null);
							hbMember.setProtectionFlags(new HashSet<>());
							hbMember.setHellblockParty(new ArrayList<>());
							hbMember.setBannedPlayers(new ArrayList<>());
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
							memberConfig.set("player.hellblock-id", null);
							memberConfig.set("player.home", null);
							memberConfig.set("player.owner", null);
							memberConfig.set("player.biome", null);
							memberConfig.set("player.party", null);
							memberConfig.set("player.creation-time", null);
							memberConfig.set("player.total-visits", null);
							memberConfig.set("player.locked-island", null);
							memberConfig.set("player.reset-cooldown", null);
							memberConfig.set("player.biome-cooldown", null);
							memberConfig.set("player.island-choice", null);
							memberConfig.set("player.protection-flags", null);
							memberConfig.set("player.banned-from-island", null);
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
				pi.saveHellblockPlayer();
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
					block.setBiome(Biome.NETHER_WASTES);
					block.getWorld().refreshChunk(block.getX(), block.getZ());
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

	public int nextHellblockID() {
		int nextID = 1;
		if (this.getPlayersDirectory().listFiles().length == 0)
			return nextID;
		for (File playerFile : this.getPlayersDirectory().listFiles()) {
			if (!(playerFile.isFile() || playerFile.getName().endsWith(".yml")))
				continue;
			YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
			if (!(playerConfig.contains("player.hellblock-id")))
				continue;
			int id = playerConfig.getInt("player.hellblock-id");
			do {
				nextID++;
			} while (id >= nextID);
		}
		return nextID;
	}

	public Location locateBedrock(UUID id) {
		if (isWorldguardProtect()) {
			List<Location> blocks = instance.getWorldGuardHandler().getRegionBlocks(id);
			Location bedrock = new Location(getHellblockWorld(), 0.0D, (getHeight() + 1), 0.0D);
			for (Location location : blocks) {
				if (location.getBlock().getType() != Material.BEDROCK)
					continue;

				bedrock = location;
				break;
			}
			bedrock = new Location(bedrock.getWorld(), bedrock.getX(), bedrock.getWorld().getHighestBlockYAt(bedrock),
					bedrock.getZ());
			return bedrock;
		} else {
			return new Location(getHellblockWorld(), 0.0D, (getHeight() + 1), 0.0D);
			// TODO: using plugin protection
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
		this.activePlayers.put(id, new HellblockPlayer(id));
		return this.activePlayers.get(id);
	}

	public @NonNull HellblockPlayer getActivePlayer(UUID playerUUID) {
		if (this.activePlayers.containsKey(playerUUID) && this.activePlayers.get(playerUUID) != null) {
			return this.activePlayers.get(playerUUID);
		}

		// in case somehow the id becomes null?
		this.activePlayers.put(playerUUID, new HellblockPlayer(playerUUID));
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

	public boolean checkIfInSpawn(Location location) {
		if (HellblockPlugin.getInstance().getHellblockHandler().isWorldguardProtect()) {
			if (instance.getWorldGuardHandler().getWorldGuardPlatform() == null) {
				LogUtils.severe("Could not retrieve WorldGuard platform.");
				return false;
			}
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			com.sk89q.worldedit.world.World world = BukkitAdapter
					.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion("Spawn");
			if (region == null) {
				return false;
			}

			return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		} else {
			// TODO: using plugin protection
			return false;
		}
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
			// TODO: using plugin protection
		}

		return spawn;
	}
}
