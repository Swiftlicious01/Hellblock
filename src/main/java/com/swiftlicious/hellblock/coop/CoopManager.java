package com.swiftlicious.hellblock.coop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.domains.DefaultDomain;
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
					DefaultDomain party = region.getMembers();
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

					party.addPlayer(player.getUniqueId());
					playerToAdd.setHellblock(true, hbPlayer.getHellblockLocation());
					playerToAdd.setHome(hbPlayer.getHomeLocation());
					playerToAdd.setHellblockParty(hbPlayer.getHellblockParty());
					playerToAdd.addToHellblockParty(player.getUniqueId());
					playerToAdd.setHellblockOwner(owner.getUniqueId());
					playerToAdd.setHellblockBiome(hbPlayer.getHellblockBiome());
					playerToAdd.setBiomeCooldown(hbPlayer.getBiomeCooldown());
					playerToAdd.setResetCooldown(hbPlayer.getResetCooldown());
					playerToAdd.setIslandChoice(hbPlayer.getIslandChoice());
					playerToAdd.setUsedSchematic(hbPlayer.getUsedSchematic());
					player.teleportAsync(hbPlayer.getHomeLocation());
					// if raining give player a bit of protection
					if (instance.getLavaRain().getLavaRainTask() != null
							&& instance.getLavaRain().getLavaRainTask().isLavaRaining()
							&& instance.getLavaRain().getHighestBlock(player.getLocation()) != null
							&& !instance.getLavaRain().getHighestBlock(player.getLocation()).isEmpty()) {
						player.setNoDamageTicks(5 * 20);
					}
					hbPlayer.addToHellblockParty(player.getUniqueId());
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

	public void removeMemberFromHellblock(@NonNull HellblockPlayer hbPlayer, @NonNull HellblockPlayer playerToRemove) {
		Player owner = hbPlayer.getPlayer();
		Player player = playerToRemove.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (!hbPlayer.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You cannot kick yourself from your own island!");
					return;
				}
				if (!playerToRemove.hasHellblock()) {
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
					DefaultDomain party = region.getMembers();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>" + player.getName() + " is not a part of your hellblock party!");
						return;
					}

					party.removePlayer(player.getUniqueId());
					playerToRemove.setHellblock(false, null);
					playerToRemove.setHellblockParty(new ArrayList<>());
					playerToRemove.setHome(null);
					playerToRemove.setHellblockBiome(null);
					playerToRemove.setBiomeCooldown(0L);
					playerToRemove.setBiomeCooldown(0L);
					playerToRemove.setHellblockOwner(null);
					playerToRemove.setIslandChoice(null);
					playerToRemove.setUsedSchematic(null);
					hbPlayer.kickFromHellblockParty(player.getUniqueId());
					player.performCommand(instance.getHellblockHandler().getNetherCMD());
					playerToRemove.saveHellblockPlayer();
					hbPlayer.saveHellblockPlayer();
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>" + player.getName() + " has been kicked to your hellblock party!");
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You have been removed from " + owner.getName() + "'s hellblock!");
				} else {
					// TODO: using plugin protection
				}
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>The player you are trying to kick isn't online!");
				return;
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
				DefaultDomain party = region.getMembers();

				party.removePlayer(player.getUniqueId());
				leavingPlayer.setHellblock(false, null);
				leavingPlayer.setHome(null);
				leavingPlayer.setHellblockOwner(null);
				leavingPlayer.setHellblockBiome(null);
				leavingPlayer.setBiomeCooldown(0L);
				leavingPlayer.setBiomeCooldown(0L);
				leavingPlayer.setIslandChoice(null);
				leavingPlayer.setUsedSchematic(null);
				leavingPlayer.setHellblockParty(new ArrayList<>());
				player.performCommand(instance.getHellblockHandler().getNetherCMD());
				leavingPlayer.saveHellblockPlayer();
				if (owner != null && owner.isOnline()) {
					HellblockPlayer ownerPlayer = instance.getHellblockHandler()
							.getActivePlayer(leavingPlayer.getHellblockOwner());
					ownerPlayer.kickFromHellblockParty(player.getUniqueId());
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
					try {
						ownerConfig.save(ownerFile);
					} catch (IOException ex) {
						LogUtils.severe(
								String.format("Unable to save owner file for %s!", leavingPlayer.getHellblockOwner()),
								ex);
					}
				}
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
					DefaultDomain party = region.getMembers();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(owner,
								"<red>" + player.getName() + " is not a part of your hellblock party!");
						return;
					}

					DefaultDomain owners = region.getOwners();
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

					owners.addPlayer(player.getUniqueId());
					owners.removePlayer(owner.getUniqueId());
					party.addPlayer(owner.getUniqueId());
					party.removePlayer(player.getUniqueId());
					hbPlayer.setHellblockOwner(player.getUniqueId());
					playerToTransfer.setHellblockOwner(player.getUniqueId());
					playerToTransfer.kickFromHellblockParty(player.getUniqueId());
					hbPlayer.addToHellblockParty(owner.getUniqueId());
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
}