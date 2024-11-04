package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.compatibility.WorldGuardHook;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound.Source;

@Getter
public class HellblockHandler {

	private final HellblockPlugin instance;
	public String worldName;
	public int spawnSize;
	public String paster;
	public int height;
	public int distance;
	public List<String> islandOptions;
	public boolean worldguardProtected;
	public boolean disableBedExplosions;
	public boolean voidTeleport;
	public boolean entryMessageEnabled, farewellMessageEnabled;
	public int protectionRange;
	public List<String> chestItems;
	public List<String> blockLevelSystem;
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
		this.paster = instance.getConfig("config.yml").getString("hellblock.schematic-paster", "worldedit");
		this.chestItems = instance.getConfig("config.yml").getStringList("hellblock.starter-chest");
		this.blockLevelSystem = instance.getConfig("config.yml").getStringList("level-system.blocks");
		this.height = instance.getConfig("config.yml").getInt("hellblock.height", 150);
		this.islandOptions = instance.getConfig("config.yml").getStringList("hellblock.island-options");
		this.distance = instance.getConfig("config.yml").getInt("hellblock.distance", 110);
		this.worldguardProtected = instance.getConfig("config.yml").getBoolean("hellblock.worldguardProtect");
		this.entryMessageEnabled = instance.getConfig("config.yml").getBoolean("hellblock.entry-message-enabled", true);
		this.farewellMessageEnabled = instance.getConfig("config.yml").getBoolean("hellblock.farewell-message-enabled",
				true);
		this.disableBedExplosions = instance.getConfig("config.yml").getBoolean("hellblock.disable-bed-explosions",
				true);
		this.voidTeleport = instance.getConfig("config.yml").getBoolean("hellblock.void-teleport", true);
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
		instance.getScheduler().runTaskAsyncTimer(() -> {
			for (HellblockPlayer active : getActivePlayers().values()) {
				if (active == null || active.getPlayer() == null)
					continue;
				if (active.getResetCooldown() > 0) {
					active.setResetCooldown(active.getResetCooldown() - 1);
				} else {
					active.getHellblockPlayer().set("player.reset-cooldown", null);
				}
				if (active.getBiomeCooldown() > 0) {
					active.setBiomeCooldown(active.getBiomeCooldown() - 1);
				} else {
					active.getHellblockPlayer().set("player.biome-cooldown", null);
				}
				if (active.getInvitations() != null && !active.getInvitations().isEmpty()) {
					for (Map.Entry<UUID, Long> invites : active.getInvitations().entrySet()) {
						if (invites.getValue() > 0) {
							active.getInvitations().replace(invites.getKey(), invites.getValue() - 1);
						} else {
							active.removeInvitation(invites.getKey());
							active.getHellblockPlayer().set("player.invitations." + invites.getKey().toString(), null);
						}
					}
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
				} else {
					offlineFile.set("player.reset-cooldown", null);
				}
				if (offlineFile.getLong("player.biome-cooldown") > 0) {
					offlineFile.set("player.biome-cooldown", offlineFile.getLong("player.biome-cooldown") - 1);
				} else {
					offlineFile.set("player.biome-cooldown", null);
				}
				if (offlineFile.contains("player.invitations")) {
					offlineFile.getConfigurationSection("player.invitations").getKeys(false).forEach(key -> {
						UUID invitee = UUID.fromString(key);
						long expirationTime = offlineFile.getLong("player.invitations." + invitee.toString());
						if (expirationTime > 0) {
							offlineFile.set("player.invitations." + invitee.toString(), expirationTime - 1);
						} else {
							offlineFile.set("player.invitations." + invitee.toString(), null);
						}
					});
				}
				try {
					offlineFile.save(offline);
				} catch (IOException ex) {
					LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
					continue;
				}
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	public CompletableFuture<Void> createHellblock(@NonNull Player player, @NonNull IslandOptions islandChoice,
			@Nullable String schematic) {
		HellblockPlayer pi = this.getActivePlayer(player);
		Location last = this.getLastHellblock();
		Location next = this.nextHellblockLocation(last);
		do {
			next = this.nextHellblockLocation(next);
		} while (this.hellblockInSpawn(next));
		this.setLastHellblock(next);
		pi.setHellblock(true, next, nextHellblockID());
		pi.setIslandChoice(islandChoice);
		pi.setHellblockBiome(HellBiome.NETHER_WASTES);
		pi.setBiomeCooldown(0L);
		pi.setLockedStatus(false);
		pi.setHellblockOwner(player.getUniqueId());
		pi.setHellblockParty(new HashSet<>());
		pi.setProtectionFlags(new HashMap<>());
		pi.setInvitations(new HashMap<>());
		pi.setWhoTrusted(new HashSet<>());
		pi.setBannedPlayers(new HashSet<>());
		CompletableFuture<Void> protection = new CompletableFuture<Void>();
		if (this.worldguardProtected) {
			protection = instance.getWorldGuardHandler().protectHellblock(pi);
		} else {
			// TODO: island protection
		}

		final Location nextHellblock = next;
		return protection.thenRun(() -> {
			CompletableFuture<Void> choice = new CompletableFuture<Void>();
			if (islandChoice == IslandOptions.SCHEMATIC && schematic != null && !schematic.isEmpty()
					&& instance.getSchematicManager().availableSchematics.stream()
							.filter(sch -> sch.equalsIgnoreCase(Files.getNameWithoutExtension(schematic))).findAny()
							.isPresent()) {
				pi.setUsedSchematic(Files.getNameWithoutExtension(schematic));
				choice = instance.getIslandChoiceConverter().convertIslandChoice(player, nextHellblock, schematic);
			} else {
				choice = instance.getIslandChoiceConverter().convertIslandChoice(player, nextHellblock);
			}

			instance.getAdventureManager().sendMessageWithPrefix(player, "<red>Creating new hellblock! Please wait...");
			choice.thenRun(() -> {
				if (pi.getHomeLocation() == null)
					throw new NullPointerException("Hellblock home location returned null");

				LocationUtils.isSafeLocationAsync(pi.getHomeLocation()).thenAccept((result) -> {
					ChunkUtils.teleportAsync(player, pi.getHomeLocation(), TeleportCause.PLUGIN).thenRun(() -> {
						instance.getAdventureManager().sendTitle(player,
								String.format("<red>Welcome <dark_red>%s", player.getName()), "<red>To Your Hellblock!",
								2 * 20, 3 * 20, 2 * 20);
						instance.getAdventureManager().sendSound(player, Source.PLAYER,
								Key.key("minecraft:item.totem.use"), 1.0F, 1.0F);
						pi.setCreationTime(System.currentTimeMillis());
						if (this.worldguardProtected) {
							ProtectedRegion region = instance.getWorldGuardHandler().getRegion(pi.getUUID(),
									pi.getID());
							if (region != null)
								instance.getBiomeHandler().setHellblockBiome(region, pi.getHellblockBiome());
						} else {
							// TODO: using plugin protection
						}
						pi.saveHellblockPlayer();
						instance.debug(String.format("Creating new hellblock for %s", player.getName()));
					});
				});
			}).join();
		});
	}

	public CompletableFuture<Void> createHellblock(@NonNull Player player, @NonNull IslandOptions islandChoice) {
		return createHellblock(player, islandChoice, null);
	}

	public CompletableFuture<Void> resetHellblock(@NonNull UUID id, boolean forceReset) {
		return CompletableFuture.runAsync(() -> {
			HellblockPlayer pi = getActivePlayer(id);

			Map<Block, Material> blockChanges = new LinkedHashMap<>();

			if (Bukkit.getPlayer(id) != null)
				instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getPlayer(id),
						"<red>Clearing your hellblock for a replacement space! Please wait...");
			instance.getScheduler().runTaskSync(() -> {
				ProtectionTask unprotection = new ProtectionTask();
				CompletableFuture<List<Location>> clearBlocks = new CompletableFuture<List<Location>>();
				if (this.worldguardProtected) {
					unprotection.setProtectionTask(instance.getWorldGuardHandler().unprotectHellblock(id, forceReset));
					ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
					instance.getBiomeHandler().setHellblockBiome(region, HellBiome.NETHER_WASTES);
					if (instance.getWorldGuardHandler().getCachedRegion().containsKey(id)) {
						instance.getWorldGuardHandler().getCachedRegion().remove(id);
					}
					clearBlocks = instance.getWorldGuardHandler().getRegionBlocks(id);
				} else {
					// TODO: using plugin protection
				}
				clearBlocks.thenAccept((blocks) -> {
					for (Location blockChange : blocks) {
						Block block = blockChange.getBlock();
						if (!block.getType().isAir()) {
							blockChanges.put(block, Material.AIR);
						}
					}
					unprotection.getProtectionTask().thenRun(() -> {
						instance.getScheduler().runTaskAsync(() -> {
							pi.setHellblock(false, null, 0);
							pi.setHellblockOwner(null);
							pi.setTotalVisits(0);
							pi.setLevel(0);
							pi.setCreationTime(0L);
							pi.setHellblockBiome(null);
							pi.setBiomeCooldown(0L);
							pi.setUsedSchematic(null);
							pi.setLockedStatus(false);
							pi.setIslandChoice(null);
							pi.setBannedPlayers(new HashSet<>());
							pi.setProtectionFlags(new HashMap<>());
							Set<UUID> visitors = instance.getCoopManager().getVisitors(id);
							if (!visitors.isEmpty()) {
								for (UUID uuid : visitors) {
									Player visitor = Bukkit.getPlayer(uuid);
									if (visitor != null && visitor.isOnline()) {
										HellblockPlayer vi = getActivePlayer(visitor);
										if (vi.hasHellblock()) {
											LocationUtils.isSafeLocationAsync(vi.getHomeLocation())
													.thenAccept((result) -> {
														if (!result.booleanValue()) {
															instance.getAdventureManager().sendMessageWithPrefix(
																	vi.getPlayer(),
																	"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
															locateBedrock(uuid).thenAccept((bedrock) -> {
																vi.setHome(bedrock.getBedrockLocation());
																instance.getCoopManager().updateParty(uuid,
																		HellblockData.HOME, vi.getHomeLocation());
															});
														}
														ChunkUtils.teleportAsync(visitor, vi.getHomeLocation(),
																TeleportCause.PLUGIN);
													});
										} else {
											instance.getHellblockHandler().teleportToSpawn(visitor, true);
										}
									} else {
										File visitorFile = new File(
												getPlayersDirectory() + File.separator + uuid + ".yml");
										YamlConfiguration visitorConfig = YamlConfiguration
												.loadConfiguration(visitorFile);
										visitorConfig.set("player.in-unsafe-island", true);
										try {
											visitorConfig.save(visitorFile);
										} catch (IOException ex) {
											LogUtils.severe(String.format("Unable to save visitor file for %s!", uuid),
													ex);
										}
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
									LogUtils.warn(String.format("Could not save the offline data file for %s", uuid),
											ex);
									continue;
								}
							}
							Set<UUID> party = pi.getHellblockParty();
							if (party != null && !party.isEmpty()) {
								for (UUID uuid : party) {
									Player member = Bukkit.getPlayer(uuid);
									if (member != null && member.isOnline()) {
										HellblockPlayer hbMember = getActivePlayer(member);
										hbMember.setHellblock(false, null, 0);
										hbMember.setHellblockOwner(null);
										hbMember.setHellblockBiome(null);
										hbMember.setTotalVisits(0);
										hbMember.setLevel(0);
										hbMember.setCreationTime(0L);
										hbMember.setBiomeCooldown(0L);
										hbMember.setResetCooldown(0L);
										hbMember.setIslandChoice(null);
										hbMember.setLockedStatus(false);
										hbMember.setUsedSchematic(null);
										hbMember.setProtectionFlags(new HashMap<>());
										hbMember.setHellblockParty(new HashSet<>());
										hbMember.setBannedPlayers(new HashSet<>());
										hbMember.saveHellblockPlayer();
										hbMember.resetHellblockData();
										teleportToSpawn(member, true);
										if (!forceReset) {
											Player player = Bukkit.getPlayer(id);
											if (player != null) {
												instance.getAdventureManager().sendMessageWithPrefix(member,
														String.format(
																"<red>Your hellblock owner <dark_red>%s <red>has reset the island, so you've been removed.",
																player.getName()));
											}
										} else {
											instance.getAdventureManager().sendMessageWithPrefix(member,
													"<red>The hellblock party you were a part of has been forcefully deleted.");
										}
									} else {
										File memberFile = new File(
												getPlayersDirectory() + File.separator + uuid + ".yml");
										YamlConfiguration memberConfig = YamlConfiguration
												.loadConfiguration(memberFile);
										memberConfig.set("player.hasHellblock", false);
										memberConfig.set("player.hellblock", null);
										memberConfig.set("player.hellblock-id", null);
										memberConfig.set("player.hellblock-level", null);
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
											LogUtils.severe(String.format("Unable to save member file for %s!", uuid),
													ex);
										}
									}
								}
							}
							pi.setHellblockParty(new HashSet<>());
							pi.saveHellblockPlayer();
							pi.resetHellblockData();
							if (Bukkit.getPlayer(id) != null) {
								if (!forceReset)
									instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getPlayer(id),
											"<red>Resetting your current hellblock. Please choose a new hellblock type to create!");
								teleportToSpawn(Bukkit.getPlayer(id), true);
							}
							instance.getScheduler().runTaskSync(() -> {
								blockChanges.forEach((change, type) -> {
									if (change.getState() instanceof BlockInventoryHolder holder) {
										holder.getInventory().clear();
									}
									change.setType(type);
								});
							}, new Location(getHellblockWorld(), 0.0D, getHeight(), 0.0D));
						});
					});
				}).join();
			}, new Location(getHellblockWorld(), 0.0D, getHeight(), 0.0D));
		});
	}

	public boolean hellblockInSpawn(@Nullable Location location) {
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

	public CompletableFuture<BedrockLocator> locateBedrock(@NonNull UUID id) {
		CompletableFuture<BedrockLocator> location = CompletableFuture.supplyAsync(() -> {
			if (this.worldguardProtected) {
				BedrockLocator bedrockLocator = new BedrockLocator(
						new Location(getHellblockWorld(), 0.0D, (getHeight() + 1), 0.0D));
				CompletableFuture<Void> regionBlockIterator = instance.getWorldGuardHandler().getRegionBlocks(id)
						.thenAccept((bedrockLocation) -> {
							Location bedrock = bedrockLocation.stream()
									.filter(loc -> loc.getBlock().getType() == Material.BEDROCK).findFirst()
									.orElse(null);
							if (bedrock != null) {
								bedrockLocator.setBedrockLocation(bedrock);
								Block highestBlock = getHellblockWorld().getHighestBlockAt(bedrock);
								CompletableFuture<Void> safe = LocationUtils
										.isSafeLocationAsync(highestBlock.getLocation()).thenAccept((result) -> {
											if (result.booleanValue()) {
												bedrockLocator.setBedrockLocation(new Location(bedrock.getWorld(),
														bedrock.getX(), highestBlock.getY() + 1.0D, bedrock.getZ())
														.add(new Vector(0.5D, 0D, 0.5D)));
											} else {
												highestBlock.getRelative(BlockFace.DOWN).setType(Material.NETHERRACK);
												highestBlock.setType(Material.AIR);
												highestBlock.getRelative(BlockFace.UP).setType(Material.AIR);
												bedrockLocator.setBedrockLocation(
														highestBlock.getLocation().add(new Vector(0.5D, 0D, 0.5D)));
											}
										});
								safe.join(); // Wait for the asynchronous operation to complete
							}
						});
				regionBlockIterator.join(); // Wait for the asynchronous operation to complete
				return bedrockLocator;
			} else {
				return new BedrockLocator(new Location(getHellblockWorld(), 0.0D, (getHeight() + 1), 0.0D));
				// TODO: using plugin protection
			}
		});
		return location;
	}

	public boolean isHellblockOwner(@NonNull UUID fileID, @NonNull UUID configID) {
		return fileID.equals(configID);
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

	public @NonNull Location nextHellblockLocation(@NonNull Location last) {
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

	public @NonNull YamlConfiguration getLastHellblockConfig() {
		if (this.lastHellblock == null) {
			this.reloadLastHellblock();
		}

		return this.lastHellblock;
	}

	public @NonNull Location getLastHellblock() {
		this.reloadLastHellblock();
		World world = getHellblockWorld();
		int x = this.getLastHellblockConfig().getInt("last.x");
		int y = this.height;
		int z = this.getLastHellblockConfig().getInt("last.z");
		return new Location(world, (double) x, (double) y, (double) z);
	}

	public void setLastHellblock(@NonNull Location location) {
		this.getLastHellblockConfig().set("last.x", location.getBlockX());
		this.getLastHellblockConfig().set("last.z", location.getBlockZ());

		try {
			this.getLastHellblockConfig().save(this.lastHellblockFile);
		} catch (IOException ex) {
			LogUtils.warn("Could not save the last known hellblock data to file.", ex);
		}
	}

	public @NonNull File[] getSchematics() {
		return this.schematicsDirectory.listFiles();
	}

	public @NonNull Map<UUID, HellblockPlayer> getActivePlayers() {
		return this.activePlayers;
	}

	public @NonNull HellblockPlayer getActivePlayer(@NonNull Player player) {
		final UUID id = player.getUniqueId();
		if (this.activePlayers.containsKey(id) && this.activePlayers.get(id) != null) {
			return this.activePlayers.get(id);
		}

		// in case somehow the id becomes null?
		this.activePlayers.put(id, new HellblockPlayer(id));
		return this.activePlayers.get(id);
	}

	public @NonNull HellblockPlayer getActivePlayer(@NonNull UUID playerUUID) {
		if (this.activePlayers.containsKey(playerUUID) && this.activePlayers.get(playerUUID) != null) {
			return this.activePlayers.get(playerUUID);
		}

		// in case somehow the id becomes null?
		this.activePlayers.put(playerUUID, new HellblockPlayer(playerUUID));
		return this.activePlayers.get(playerUUID);
	}

	public void addActivePlayer(@NonNull Player player, @NonNull HellblockPlayer pi) {
		final UUID id = player.getUniqueId();
		this.activePlayers.put(id, pi);
	}

	public void removeActivePlayer(@NonNull Player player) {
		final UUID id = player.getUniqueId();
		if (this.activePlayers.containsKey(id)) {
			this.activePlayers.get(id).saveHellblockPlayer();
			this.activePlayers.remove(id);
		}
	}

	public @NonNull ChunkGenerator getDefaultWorldGenerator(@NonNull String worldName, @NonNull String id) {
		return new VoidGenerator();
	}

	public @NonNull World getHellblockWorld() {
		World hellblockWorld = Bukkit.getWorld(this.worldName);
		if (hellblockWorld == null) {
			VoidGenerator voidGen = new VoidGenerator();
			hellblockWorld = WorldCreator.name(this.worldName).type(WorldType.FLAT).generateStructures(false)
					.environment(Environment.NETHER).generator(voidGen).biomeProvider(voidGen.new VoidBiomeProvider())
					.createWorld();
			instance.debug(String.format("Created the new world to be used for Hellblock islands: %s", this.worldName));
			final World spawnWorld = hellblockWorld;
			final HellblockSpawn spawnLocation = new HellblockSpawn();
			generateSpawn().thenAccept(spawn -> {
				CompletableFuture<Void> spawnProtection = new CompletableFuture<Void>();
				spawnLocation.setSpawnLocation(spawn);
				if (this.worldguardProtected) {
					spawnProtection = instance.getWorldGuardHandler().protectSpawn();
				} else {
					// TODO: using plugin protection
				}
				spawnProtection.thenRun(() -> {
					if (instance.getWorldGuardHandler().getSpawnRegion() != null)
						instance.getBiomeHandler().setHellblockBiome(instance.getWorldGuardHandler().getSpawnRegion(),
								HellBiome.SOUL_SAND_VALLEY);
					instance.debug("Spawn area protected with WorldGuard!");
					spawnLocation
							.setSpawnLocation(instance.getWorldGuardHandler().getSpawnCenter().toLocation(spawnWorld));
					spawnLocation.getSpawnLocation().setY(getHeight() + 1);
					spawnWorld.setSpawnLocation(spawnLocation.getSpawnLocation());
					instance.debug(
							String.format("Generated the spawn for the new Hellblock world %s at x:%s, y:%s, z:%s",
									this.worldName, spawnLocation.getSpawnLocation().x(),
									spawnLocation.getSpawnLocation().y(), spawnLocation.getSpawnLocation().z()));
				}).thenRun(() -> {
					if (getMvWorldManager() != null) {
						instance.getScheduler().runTaskSync(() -> {
							getMvWorldManager().addWorld(spawnWorld.getName(), Environment.NETHER,
									String.valueOf(spawnWorld.getSeed()), WorldType.FLAT, false,
									instance.getPluginMeta().getName(), true);
							getMvWorldManager().getMVWorld(spawnWorld).setSpawnLocation(spawnWorld.getSpawnLocation());
							instance.debug(String.format("Imported the world to Multiverse-Core: %s", this.worldName));
						}, new Location(spawnWorld, 0.0D, getHeight() + 1, 0.0D));
					}
				});
			});
		}
		return hellblockWorld;
	}

	public void teleportToSpawn(@NonNull Player player, boolean forced) {
		if (this.worldguardProtected) {
			Location spawn = instance.getWorldGuardHandler().getSpawnCenter().toLocation(getHellblockWorld());
			spawn.setY(getHellblockWorld().getHighestBlockYAt(spawn) + 1);
			ChunkUtils.teleportAsync(player, spawn).thenRun(() -> {
				if (!forced)
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>Sent to spawn due to unsafe conditions!");
			});
		} else {
			// TODO: using plugin protection
		}
	}

	public boolean checkIfInSpawn(@NonNull Location location) {
		if (this.worldguardProtected) {
			if (instance.getWorldGuardHandler().getWorldGuardPlatform() == null) {
				LogUtils.severe("Could not retrieve WorldGuard platform.");
				return false;
			}
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion(WorldGuardHook.SPAWN_REGION);
			if (region == null) {
				return false;
			}

			return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public CompletableFuture<Location> generateSpawn() {
		CompletableFuture<Location> location = CompletableFuture.supplyAsync(() -> {
			Map<Block, Material> blockChanges = new LinkedHashMap<>();
			World world = getHellblockWorld();
			int y = getHeight();
			int spawnSize = getSpawnSize();

			Location spawn = new Location(world, 0, y, 0);
			int i = 0;
			for (int operateX = 0 - spawnSize; operateX <= spawnSize; ++operateX) {
				for (int operateZ = 0 - spawnSize; operateZ <= spawnSize; ++operateZ) {
					Block block = world.getBlockAt(operateX, y, operateZ);
					blockChanges.put(block, Material.RED_NETHER_BRICKS);
					if (i == 0) {
						spawn = block.getLocation(
								new Location(block.getWorld(), block.getX(), block.getY() + 1, block.getZ()));
						i++;
					}
				}
			}

			instance.getScheduler().runTaskSync(() -> blockChanges.forEach((change, type) -> change.setType(type)),
					spawn);

			return spawn;
		});

		return location;
	}

	public class ProtectionTask {
		private CompletableFuture<Void> protectionTask;

		public @NonNull CompletableFuture<Void> getProtectionTask() {
			return this.protectionTask;
		}

		public void setProtectionTask(@NonNull CompletableFuture<Void> protectionTask) {
			this.protectionTask = protectionTask;
		}
	}

	public class HellblockSpawn {

		private Location spawnLocation;

		public @NonNull Location getSpawnLocation() {
			return this.spawnLocation;
		}

		public void setSpawnLocation(@NonNull Location spawnLocation) {
			this.spawnLocation = spawnLocation;
		}
	}

	public class BedrockLocator {

		private Location bedrockLocation;

		public BedrockLocator(Location bedrockLocation) {
			this.bedrockLocation = bedrockLocation;
		}

		public @NonNull Location getBedrockLocation() {
			return this.bedrockLocation;
		}

		public void setBedrockLocation(@NonNull Location bedrockLocation) {
			this.bedrockLocation = bedrockLocation;
		}
	}
}
