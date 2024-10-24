package com.swiftlicious.hellblock.coop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CoopManager {

	private final HellblockPlugin instance;
	private int partySizeLimit;
	private boolean canTransferIsland;

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
		this.partySizeLimit = instance.getConfig("config.yml").getInt("hellblock.party-size", 4);
		this.canTransferIsland = instance.getConfig("config.yml").getBoolean("hellblock.can-transfer-islands", true);
	}

	public void addMemberToHellblock(@NonNull HellblockPlayer hbPlayer, @NonNull HellblockPlayer playerToAdd) {
		Player owner = hbPlayer.getPlayer();
		Player player = playerToAdd.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (!hbPlayer.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You cannot invite yourself to your own island!");
					return;
				}
				if (playerToAdd.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player already has their own hellblock!");
					return;
				}

				if (instance.getHellblockHandler().isWorldguardProtect()) {
					RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform()
							.getRegionContainer();
					World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
					RegionManager regions = container.get(world);
					if (regions == null) {
						LogUtils.severe(String.format("Could not load WorldGuard regions for hellblock world: %s",
								world.getName()));
						return;
					}
					ProtectedRegion region = regions.getRegion(owner.getName() + "Hellblock");
					if (region == null) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>You don't have a hellblock island! Create one with /hellblock create");
						return;
					}
					Set<UUID> party = region.getMembers().getUniqueIds();
					if (party.size() >= this.partySizeLimit) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>You have reached the maximum party limit!");
						return;
					}
					if (party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>" + player.getName() + " is already part of your hellblock party!");
						return;
					}

					party.add(player.getUniqueId());
					playerToAdd.setHellblock(true, hbPlayer.getHellblockLocation(), hbPlayer.getID());
					playerToAdd.setHome(hbPlayer.getHomeLocation());
					playerToAdd.setHellblockParty(hbPlayer.getHellblockParty());
					playerToAdd.addToHellblockParty(player.getUniqueId());
					playerToAdd.setHellblockOwner(owner.getUniqueId());
					playerToAdd.setHellblockBiome(hbPlayer.getHellblockBiome());
					playerToAdd.setLockedStatus(hbPlayer.getLockedStatus());
					playerToAdd.setCreationTime(hbPlayer.getCreation());
					playerToAdd.setTotalVisits(hbPlayer.getTotalVisitors());
					playerToAdd.setBiomeCooldown(hbPlayer.getBiomeCooldown());
					playerToAdd.setResetCooldown(hbPlayer.getResetCooldown());
					playerToAdd.setIslandChoice(hbPlayer.getIslandChoice());
					playerToAdd.setUsedSchematic(hbPlayer.getUsedSchematic());
					if (playerToAdd.getWhoTrusted().contains(owner.getUniqueId())) {
						playerToAdd.removeTrustPermission(owner.getUniqueId());
					}
					player.teleportAsync(hbPlayer.getHomeLocation());
					// if raining give player a bit of protection
					if (instance.getLavaRain().getLavaRainTask() != null
							&& instance.getLavaRain().getLavaRainTask().isLavaRaining()
							&& instance.getLavaRain().getHighestBlock(player.getLocation()) != null
							&& !instance.getLavaRain().getHighestBlock(player.getLocation()).isEmpty()) {
						player.setNoDamageTicks(5 * 20);
					}
					hbPlayer.addToHellblockParty(player.getUniqueId());
					for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
						if (active == null || active.getPlayer() == null)
							continue;
						if (active.getHellblockOwner().equals(playerToAdd.getHellblockOwner())) {
							active.addToHellblockParty(player.getUniqueId());
						}
						active.saveHellblockPlayer();
					}
					for (File offline : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
						if (id != null && instance.getHellblockHandler().getActivePlayers().keySet().contains(id))
							continue;
						YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
						if (offlineFile.getString("player.owner").equals(playerToAdd.getHellblockOwner().toString())) {
							List<String> offlineParty = offlineFile.getStringList("player.party");
							offlineParty.add(player.getUniqueId().toString());
							offlineFile.set("player.party", offlineParty);
						}
						try {
							offlineFile.save(offline);
						} catch (IOException ex) {
							LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
							continue;
						}
					}
					playerToAdd.saveHellblockPlayer();
					hbPlayer.saveHellblockPlayer();
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>" + player.getName() + " has been added to your hellblock party!");
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You have joined " + owner.getName() + "'s hellblock!");
				} else {
					// TODO: using plugin protection
				}
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>The player you are trying to add isn't online!");
				return;
			}
		}
	}

	public void removeMemberFromHellblock(@NonNull HellblockPlayer hbPlayer, @NonNull String input, @NonNull UUID id) {
		Player owner = hbPlayer.getPlayer();
		if (owner != null) {
			if (!hbPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>You don't have a hellblock island! Create one with /hellblock create");
				return;
			}
			HellblockPlayer ti = null;
			if (HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().containsKey(id)) {
				ti = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayers().get(id);
			} else {
				ti = new HellblockPlayer(id);
			}

			if (owner.getUniqueId().equals(ti.getHellblockOwner())) {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>You cannot kick yourself from your own island!");
				return;
			}
			if (!ti.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>This player is not a part of your hellblock party!");
				return;
			}

			if (instance.getHellblockHandler().isWorldguardProtect()) {
				RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform()
						.getRegionContainer();
				World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
				RegionManager regions = container.get(world);
				if (regions == null) {
					LogUtils.severe(String.format("Could not load WorldGuard regions for hellblock world: %s",
							world.getName()));
					return;
				}
				ProtectedRegion region = regions.getRegion(owner.getName() + "Hellblock");
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				Set<UUID> party = region.getMembers().getUniqueIds();
				if (!party.contains(id)) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>" + input + " is not a part of your hellblock party!");
					return;
				}

				party.remove(id);
				ti.setHellblock(false, null, -1);
				ti.setHellblockParty(new ArrayList<>());
				ti.setHome(null);
				ti.setTotalVisits(0);
				ti.setCreationTime(0L);
				ti.setHellblockBiome(null);
				ti.setLockedStatus(false);
				ti.setBiomeCooldown(0L);
				ti.setBiomeCooldown(0L);
				ti.setIslandChoice(null);
				ti.setUsedSchematic(null);
				hbPlayer.kickFromHellblockParty(id);
				for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
					if (active == null || active.getPlayer() == null)
						continue;
					if (active.getHellblockOwner().equals(ti.getHellblockOwner())) {
						active.kickFromHellblockParty(id);
					}
					active.saveHellblockPlayer();
				}
				for (File offline : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
					if (offlineID != null
							&& instance.getHellblockHandler().getActivePlayers().keySet().contains(offlineID))
						continue;
					YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
					if (offlineFile.getString("player.owner").equals(ti.getHellblockOwner().toString())) {
						List<String> offlineParty = offlineFile.getStringList("player.party");
						offlineParty.remove(id.toString());
						offlineFile.set("player.party", offlineParty);
					}
					try {
						offlineFile.save(offline);
					} catch (IOException ex) {
						LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
						continue;
					}
				}
				ti.setHellblockOwner(null);
				ti.saveHellblockPlayer();
				hbPlayer.saveHellblockPlayer();
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>" + input + " has been kicked to your hellblock party!");
				Player player = Bukkit.getPlayer(id);
				if (player != null) {
					player.performCommand(instance.getHellblockHandler().getNetherCMD());
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You have been removed from " + owner.getName() + "'s hellblock!");
				}
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public void leaveHellblockParty(@NonNull HellblockPlayer leavingPlayer) {
		Player player = leavingPlayer.getPlayer();
		if (player != null) {
			if (!leavingPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock island! Create one with /hellblock create");
				return;
			}

			if (instance.getHellblockHandler().isWorldguardProtect()) {
				RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform()
						.getRegionContainer();
				World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
				RegionManager regions = container.get(world);
				if (regions == null) {
					LogUtils.severe(String.format("Could not load WorldGuard regions for hellblock world: %s",
							world.getName()));
					return;
				}
				if (leavingPlayer.getHellblockOwner() == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>An error has occurred. Please report this to the developer.");
					return;
				}
				if (leavingPlayer.getHellblockOwner() != null
						&& leavingPlayer.getHellblockOwner().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You cannot leave the hellblock you own!");
					return;
				}
				OfflinePlayer owner;
				if (Bukkit.getPlayer(leavingPlayer.getHellblockOwner()) != null) {
					owner = Bukkit.getPlayer(leavingPlayer.getHellblockOwner());
				} else {
					owner = Bukkit.getOfflinePlayer(leavingPlayer.getHellblockOwner());
				}
				ProtectedRegion region = owner.getName() != null ? regions.getRegion(owner.getName() + "Hellblock")
						: null;
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>An error has occurred. Please report this to the developer.");
					return;
				}
				Set<UUID> party = region.getMembers().getUniqueIds();
				if (!party.contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You aren't a part of this hellblock island!");
					return;
				}

				party.remove(player.getUniqueId());
				leavingPlayer.setHellblock(false, null, -1);
				leavingPlayer.setHome(null);
				leavingPlayer.setHellblockBiome(null);
				leavingPlayer.setLockedStatus(false);
				leavingPlayer.setTotalVisits(0);
				leavingPlayer.setCreationTime(0L);
				leavingPlayer.setBiomeCooldown(0L);
				leavingPlayer.setBiomeCooldown(0L);
				leavingPlayer.setIslandChoice(null);
				leavingPlayer.setUsedSchematic(null);
				leavingPlayer.setHellblockParty(new ArrayList<>());
				player.performCommand(instance.getHellblockHandler().getNetherCMD());
				if (owner != null && owner.isOnline()) {
					HellblockPlayer ownerPlayer = instance.getHellblockHandler()
							.getActivePlayer(leavingPlayer.getHellblockOwner());
					if (ownerPlayer.getHellblockParty().contains(player.getUniqueId())) {
						ownerPlayer.kickFromHellblockParty(player.getUniqueId());
					}
					ownerPlayer.saveHellblockPlayer();
				} else {
					File ownerFile = new File(HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory()
							+ File.separator + leavingPlayer.getHellblockOwner() + ".yml");
					YamlConfiguration ownerConfig = YamlConfiguration.loadConfiguration(ownerFile);
					List<String> partyList = ownerConfig.getStringList("player.party");
					if (partyList.contains(player.getUniqueId().toString())) {
						partyList.remove(player.getUniqueId().toString());
					}
					ownerConfig.set("player.party", partyList);
					List<String> trustedList = ownerConfig.getStringList("player.trusted");
					if (trustedList.contains(player.getUniqueId().toString())) {
						trustedList.remove(player.getUniqueId().toString());
					}
					ownerConfig.set("player.trusted", trustedList);
					try {
						ownerConfig.save(ownerFile);
					} catch (IOException ex) {
						LogUtils.severe(
								String.format("Unable to save owner file for %s!", leavingPlayer.getHellblockOwner()),
								ex);
					}
				}
				for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
					if (active == null || active.getPlayer() == null)
						continue;
					if (active.getHellblockOwner().equals(leavingPlayer.getHellblockOwner())) {
						active.kickFromHellblockParty(player.getUniqueId());
					}
					active.saveHellblockPlayer();
				}
				for (File offline : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
					if (id != null && instance.getHellblockHandler().getActivePlayers().keySet().contains(id))
						continue;
					YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
					if (offlineFile.getString("player.owner").equals(leavingPlayer.getHellblockOwner().toString())) {
						List<String> offlineParty = offlineFile.getStringList("player.party");
						offlineParty.remove(player.getUniqueId().toString());
						offlineFile.set("player.party", offlineParty);
					}
					try {
						offlineFile.save(offline);
					} catch (IOException ex) {
						LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
						continue;
					}
				}
				leavingPlayer.setHellblockOwner(null);
				leavingPlayer.saveHellblockPlayer();
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You have left your hellblock party with "
								+ (owner.getName() != null ? owner.getName() : "Unknown"));
				if (owner.isOnline())
					instance.getAdventureManager().sendMessageWithPrefix(owner.getPlayer(),
							"<red>" + player.getName() + " has just left your hellblock party!");
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public void transferOwnershipOfHellblock(@NonNull HellblockPlayer hbPlayer,
			@NonNull HellblockPlayer playerToTransfer) {
		Player owner = hbPlayer.getPlayer();
		Player player = playerToTransfer.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (!this.canTransferIsland) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>Transferring hellblock islands has been disabled!");
					return;
				}
				if (!hbPlayer.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You cannot transfer ownership to yourself for your own island!");
					return;
				}
				if (!playerToTransfer.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player isn't a member of your hellblock party!");
					return;
				}
				if (instance.getHellblockHandler().isWorldguardProtect()) {
					RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform()
							.getRegionContainer();
					World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
					RegionManager regions = container.get(world);
					if (regions == null) {
						LogUtils.severe(String.format("Could not load WorldGuard regions for hellblock world: %s",
								world.getName()));
						return;
					}
					ProtectedRegion region = regions.getRegion(owner.getName() + "Hellblock");
					if (region == null) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>You don't have a hellblock island! Create one with /hellblock create");
						return;
					}
					Set<UUID> party = region.getMembers().getUniqueIds();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>" + player.getName() + " is not a part of your hellblock party!");
						return;
					}

					Set<UUID> owners = region.getOwners().getUniqueIds();
					if (!owners.contains(owner.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>You don't own this hellblock island!");
						return;
					}
					if (owners.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>" + player.getName() + " is already the owner of this hellblock!");
						return;
					}

					owners.add(player.getUniqueId());
					owners.remove(owner.getUniqueId());
					party.add(owner.getUniqueId());
					party.remove(player.getUniqueId());
					hbPlayer.setHellblockOwner(player.getUniqueId());
					playerToTransfer.setHellblockOwner(player.getUniqueId());
					playerToTransfer.kickFromHellblockParty(player.getUniqueId());
					hbPlayer.addToHellblockParty(owner.getUniqueId());
					for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
						if (active == null || active.getPlayer() == null)
							continue;
						if (active.getHellblockOwner().equals(owner.getUniqueId())) {
							active.setHellblockOwner(playerToTransfer.getHellblockOwner());
							active.setHellblockParty(playerToTransfer.getHellblockParty());
						}
						active.saveHellblockPlayer();
					}
					for (File offline : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
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
						if (id != null && instance.getHellblockHandler().getActivePlayers().keySet().contains(id))
							continue;
						YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
						if (offlineFile.getString("player.owner").equals(owner.getUniqueId().toString())) {
							List<String> offlineParty = offlineFile.getStringList("player.party");
							offlineParty.remove(player.getUniqueId().toString());
							offlineParty.add(owner.getUniqueId().toString());
							offlineFile.set("player.party", offlineParty);
							offlineFile.set("player.owner", player.getUniqueId().toString());
						}
						try {
							offlineFile.save(offline);
						} catch (IOException ex) {
							LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
							continue;
						}
					}
					playerToTransfer.saveHellblockPlayer();
					hbPlayer.saveHellblockPlayer();
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>" + player.getName() + " is the new owner of your hellblock!");
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You have been made the new owner of " + owner.getName() + "'s hellblock!");
				} else {
					// TODO: using plugin protection
				}
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>The player you are trying to transfer with isn't online!");
				return;
			}
		}
	}

	public List<UUID> getVisitors(@NonNull UUID id) {
		List<UUID> visitors = new ArrayList<>();
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return new ArrayList<>();
			}
			ProtectedRegion region = regions.getRegion((Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
					: Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
							? Bukkit.getOfflinePlayer(id).getName()
							: "?")
					+ "Hellblock");
			if (region != null && !region.getId().equals("?Hellblock")) {
				instance.getHellblockHandler().getActivePlayers().values().forEach(player -> {
					Player onlinePlayer = player.getPlayer();
					if (onlinePlayer != null && onlinePlayer.isOnline()) {
						if (region.contains(onlinePlayer.getLocation().getBlockX(),
								onlinePlayer.getLocation().getBlockY(), onlinePlayer.getLocation().getBlockZ())) {
							visitors.add(onlinePlayer.getUniqueId());
						}
					}
				});
			}
		} else {
			// TODO: using plugin protection
		}
		return visitors;
	}

	public void changeLockStatus(@NonNull Player player) {
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return;
			}
			ProtectedRegion region = regions.getRegion(player.getName() + "Hellblock");
			if (region == null) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>An error has occurred. Please report this to the developer.");
				return;
			}
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			region.setFlag(Flags.ENTRY, StateFlag.State.valueOf(pi.getLockedStatus() ? "DENY" : "ALLOW"));
		} else {
			// TODO: using plugin protection
		}
	}

	public @Nullable UUID getHellblockOwnerOfVisitingIsland(@NonNull Player player) {
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return null;
			}
			ApplicableRegionSet region = regions.getApplicableRegions(new BlockVector3(player.getLocation().getBlockX(),
					player.getLocation().getBlockY(), player.getLocation().getBlockZ()));
			Set<UUID> owners = new HashSet<>();
			for (ProtectedRegion rg : region.getRegions()) {
				if (rg == null)
					continue;
				owners = rg.getOwners().getUniqueIds();
				break;
			}
			UUID ownerUUID = null;
			Iterator<UUID> uuids = owners.iterator();
			do {
				if (!uuids.hasNext()) {
					return null;
				}
				ownerUUID = uuids.next();
				break;
			} while (uuids.hasNext());
			return ownerUUID;
		} else {
			// TODO: using plugin protection
			return null;
		}
	}

	public boolean checkIfVisitorIsWelcome(@NonNull Player player, @NonNull UUID id) {
		HellblockPlayer pi = null;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion((Bukkit.getPlayer(id) != null ? Bukkit.getPlayer(id).getName()
					: Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
							? Bukkit.getOfflinePlayer(id).getName()
							: "?")
					+ "Hellblock");
			if (region == null || region.getId().equals("?Hellblock")) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>An error has occurred. Please report this to the developer.");
				return false;
			}
			Set<UUID> owners = region.getOwners().getUniqueIds();
			Set<UUID> members = region.getMembers().getUniqueIds();
			return (owners.contains(player.getUniqueId()) || members.contains(player.getUniqueId())
					|| pi.getWhoTrusted().contains(player.getUniqueId())
					|| player.hasPermission("hellblock.bypass.lock") || player.hasPermission("hellblock.admin")
					|| player.isOp());
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public void kickVisitorsIfLocked(@NonNull UUID id) {
		HellblockPlayer pi = null;
		if (instance.getHellblockHandler().getActivePlayers().get(id) != null) {
			pi = instance.getHellblockHandler().getActivePlayers().get(id);
		} else {
			pi = new HellblockPlayer(id);
		}
		if (pi.getLockedStatus()) {
			if (instance.getHellblockHandler().isWorldguardProtect()) {
				RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform()
						.getRegionContainer();
				World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
				RegionManager regions = container.get(world);
				if (regions == null) {
					LogUtils.severe(String.format("Could not load WorldGuard regions for hellblock world: %s",
							world.getName()));
					return;
				}
				ProtectedRegion region = regions.getRegion((Bukkit.getPlayer(id) != null
						? Bukkit.getPlayer(id).getName()
						: Bukkit.getOfflinePlayer(id).hasPlayedBefore() && Bukkit.getOfflinePlayer(id).getName() != null
								? Bukkit.getOfflinePlayer(id).getName()
								: "?")
						+ "Hellblock");
				if (region != null && !region.getId().equals("?Hellblock")) {
					List<UUID> visitors = getVisitors(id);
					for (UUID visitor : visitors) {
						HellblockPlayer vi = instance.getHellblockHandler().getActivePlayer(visitor);
						if (vi.getPlayer() != null) {
							if (!checkIfVisitorIsWelcome(vi.getPlayer(), id)) {
								if (vi.hasHellblock()) {
									vi.getPlayer().teleportAsync(vi.getHomeLocation());
								} else {
									vi.getPlayer().performCommand(instance.getHellblockHandler().getNetherCMD());
								}
								instance.getAdventureManager().sendMessageWithPrefix(vi.getPlayer(),
										"<red>The hellblock you are trying to enter has been locked from having visitors at the moment.");
							}
						}
					}
				}
			} else {
				// TODO: using plugin protection
			}
		}
	}

	public boolean addTrustAccess(@NonNull HellblockPlayer hbPlayer, @NonNull String input, @NonNull UUID id) {
		if (hbPlayer.getPlayer() == null || !hbPlayer.getPlayer().isOnline()) {
			LogUtils.severe("Player object returned null, please report this to the developer.");
			return false;
		}
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion(String.format("%sHellblock", hbPlayer.getPlayer().getName()));
			if (region == null) {
				return false;
			}
			Set<UUID> trusted = region.getMembers().getUniqueIds();
			if (trusted.contains(id)) {
				instance.getAdventureManager().sendMessageWithPrefix(hbPlayer.getPlayer(),
						"<red>" + input + " is already trusted on your hellblock!");
				return false;
			}

			return trusted.add(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public boolean removeTrustAccess(@NonNull HellblockPlayer hbPlayer, @NonNull String input, @NonNull UUID id) {
		if (hbPlayer.getPlayer() == null || !hbPlayer.getPlayer().isOnline()) {
			LogUtils.severe("Player object returned null, please report this to the developer.");
			return false;
		}
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			RegionContainer container = instance.getWorldGuardHandler().getWorldGuardPlatform().getRegionContainer();
			World world = BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld());
			RegionManager regions = container.get(world);
			if (regions == null) {
				LogUtils.severe(
						String.format("Could not load WorldGuard regions for hellblock world: %s", world.getName()));
				return false;
			}
			ProtectedRegion region = regions.getRegion(String.format("%sHellblock", hbPlayer.getPlayer().getName()));
			if (region == null) {
				return false;
			}
			Set<UUID> trusted = region.getMembers().getUniqueIds();
			if (!trusted.contains(id)) {
				instance.getAdventureManager().sendMessageWithPrefix(hbPlayer.getPlayer(),
						"<red>" + input + " isn't trusted on your hellblock!");
				return false;
			}

			return trusted.remove(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}
}