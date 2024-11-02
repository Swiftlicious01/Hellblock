package com.swiftlicious.hellblock.coop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.Files;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;
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

	public void sendInvite(@NonNull HellblockPlayer hbPlayer, @NonNull HellblockPlayer playerToInvite) {
		Player owner = hbPlayer.getPlayer();
		Player player = playerToInvite.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (hbPlayer.isAbandoned()) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>This hellblock is abandoned, you can't do anything until it's recovered!");
					return;
				}
				if (playerToInvite.hasHellblock()) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player already has their own hellblock!");
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>You cannot invite yourself to your own island!");
					return;
				}
				if (hbPlayer.getHellblockParty().contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player is already a part of your party!");
					return;
				}
				if (hbPlayer.getBannedPlayers().contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player is banned from your island!");
					return;
				}
				if (playerToInvite.hasInvite(owner.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>This player already has an invite from you, wait for them to accept or decline!");
					return;
				}

				playerToInvite.sendInvitation(owner.getUniqueId());
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>You've invited <dark_red>" + player.getName() + " <red>to your hellblock!");
				instance.getAdventureManager().sendMessageWithPrefix(player, String.format(
						"<dark_red>%s <red>invited you to their hellblock! <green><bold><click:run_command:/hellblock coop accept %s>[ACCEPT]</click> <red><bold><click:run_command:/hellblock coop decline %s>[DECLINE]</click> <reset><gray>It will expire in 24 hours.",
						owner.getName(), owner.getName(), owner.getName()));
			} else {
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>The player you are trying to invite isn't online!");
				return;
			}
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
		}
	}

	public void rejectInvite(@NonNull UUID ownerID, @NonNull HellblockPlayer rejectingPlayer) {
		Player player = rejectingPlayer.getPlayer();
		if (player != null) {
			if (rejectingPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (!rejectingPlayer.hasInvite(ownerID)) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have an invite from this player!");
				return;
			}

			rejectingPlayer.removeInvitation(ownerID);
			if (Bukkit.getPlayer(ownerID) != null) {
				instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getPlayer(ownerID),
						"<dark_red>" + player.getName() + " <red>has rejected your invitation to join!");
			}
			OfflinePlayer owner;
			if (Bukkit.getPlayer(ownerID) != null) {
				owner = Bukkit.getPlayer(ownerID);
			} else {
				owner = Bukkit.getOfflinePlayer(ownerID);
			}
			instance.getAdventureManager().sendMessageWithPrefix(player,
					"<red>You've rejected to join <dark_red>"
							+ (owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???")
							+ "<red>'s hellblock!");
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
		}
	}

	public void listInvitations(@NonNull HellblockPlayer hbPlayer) {
		Player player = hbPlayer.getPlayer();
		if (player != null) {
			if (hbPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (hbPlayer.getInvitations() != null && hbPlayer.getInvitations().isEmpty()) {
				instance.getAdventureManager().sendMessageWithPrefix(player, "<red>You don't have any invitations!");
				return;
			}

			for (Entry<UUID, Long> invites : hbPlayer.getInvitations().entrySet()) {
				UUID invitee = invites.getKey();
				OfflinePlayer owner;
				if (Bukkit.getPlayer(invitee) != null) {
					owner = Bukkit.getPlayer(invitee);
				} else {
					owner = Bukkit.getOfflinePlayer(invitee);
				}
				long expirationLeft = invites.getValue().longValue();
				instance.getAdventureManager().sendMessage(player, String.format(
						"<red>Hellblock Invitation: <dark_red>%s <green><bold><click:run_command:/hellblock coop accept %s>[ACCEPT]</click> <red><bold><click:run_command:/hellblock coop decline %s>[DECLINE]</click> <reset><gray>It will expire in %s %s.",
						owner.getName(), owner.getName(), owner.getName(), expirationLeft,
						(expirationLeft > 1 ? "hours" : "hour")));
			}

		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
		}
	}

	public void addMemberToHellblock(@NonNull UUID ownerID, @NonNull HellblockPlayer playerToAdd) {
		Player player = playerToAdd.getPlayer();
		if (player != null) {
			if (playerToAdd.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (!playerToAdd.hasInvite(ownerID)) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have an invite from this player!");
				return;
			}
			HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(ownerID);
			if (ti.isAbandoned()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>This hellblock is abandoned, you can't join it!");
				return;
			}

			if (instance.getHellblockHandler().isWorldguardProtected()) {
				OfflinePlayer owner;
				if (Bukkit.getPlayer(ownerID) != null) {
					owner = Bukkit.getPlayer(ownerID);
				} else {
					owner = Bukkit.getOfflinePlayer(ownerID);
				}
				ProtectedRegion region = owner.hasPlayedBefore()
						? instance.getWorldGuardHandler().getRegion(owner.getUniqueId(), ti.getID())
						: null;
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>An error has occurred, please report this to the developer.");
					throw new NullPointerException();
				}
				Set<UUID> party = region.getMembers().getUniqueIds();
				if (party.size() >= this.partySizeLimit) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>The party size is already full for this hellblock!");
					return;
				}
				if (party.contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You're already part of this hellblock party!");
					return;
				}

				party.add(player.getUniqueId());
				playerToAdd.clearInvites();
				playerToAdd.setHellblock(true, ti.getHellblockLocation(), ti.getID());
				playerToAdd.setHome(ti.getHomeLocation());
				playerToAdd.setHellblockParty(ti.getHellblockParty());
				playerToAdd.addToHellblockParty(player.getUniqueId());
				playerToAdd.setHellblockOwner(ownerID);
				playerToAdd.setBannedPlayers(ti.getBannedPlayers());
				playerToAdd.setLevel(ti.getLevel());
				playerToAdd.setHellblockBiome(ti.getHellblockBiome());
				playerToAdd.setProtectionFlags(ti.getProtectionFlags());
				playerToAdd.setLockedStatus(ti.getLockedStatus());
				playerToAdd.setCreationTime(ti.getCreation());
				playerToAdd.setTotalVisits(ti.getTotalVisitors());
				playerToAdd.setBiomeCooldown(ti.getBiomeCooldown());
				playerToAdd.setResetCooldown(ti.getResetCooldown());
				playerToAdd.setIslandChoice(ti.getIslandChoice());
				playerToAdd.setUsedSchematic(ti.getUsedSchematic());
				if (playerToAdd.getWhoTrusted().contains(ownerID)) {
					playerToAdd.removeTrustPermission(ownerID);
				}
				LocationUtils.isSafeLocationAsync(playerToAdd.getHomeLocation()).thenAccept((result) -> {
					if (!result.booleanValue()) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
						instance.getHellblockHandler().locateBedrock(player.getUniqueId()).thenAccept((bedrock) -> {
							playerToAdd.setHome(bedrock.getBedrockLocation());
							instance.getCoopManager().updateParty(player.getUniqueId(), HellblockData.HOME,
									playerToAdd.getHomeLocation());
						});
					}
					ChunkUtils.teleportAsync(player, playerToAdd.getHomeLocation(), TeleportCause.PLUGIN);
				});
				ti.addToHellblockParty(player.getUniqueId());
				for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
					if (active == null || active.getPlayer() == null || active.getUUID().equals(ownerID))
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
					if (offlineFile.getString("player.owner").equals(id.toString()))
						continue;
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
				ti.saveHellblockPlayer();
				if (Bukkit.getPlayer(ownerID) != null) {
					instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getPlayer(ownerID),
							"<red>" + player.getName() + " has been added to your hellblock party!");
				}
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You've joined " + owner.getName() + "'s hellblock!");
			} else {
				// TODO: using plugin protection
			}
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
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
			HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(id);

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

			if (instance.getHellblockHandler().isWorldguardProtected()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegion(owner.getUniqueId(), ti.getID());
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>An error has occurred, please report this to the developer.");
					throw new NullPointerException();
				}
				Set<UUID> party = region.getMembers().getUniqueIds();
				if (!party.contains(id)) {
					instance.getAdventureManager().sendMessageWithPrefix(owner,
							"<red>" + input + " is not a part of your hellblock party!");
					return;
				}

				party.remove(id);
				ti.setHellblock(false, null, 0);
				ti.setHellblockParty(new HashSet<>());
				ti.setBannedPlayers(new HashSet<>());
				ti.setProtectionFlags(new HashMap<>());
				ti.setHome(null);
				ti.setTotalVisits(0);
				ti.setLevel(0);
				ti.setCreationTime(0L);
				ti.setHellblockBiome(null);
				ti.setLockedStatus(false);
				ti.setResetCooldown(0L);
				ti.setBiomeCooldown(0L);
				ti.setIslandChoice(null);
				ti.setUsedSchematic(null);
				hbPlayer.kickFromHellblockParty(id);
				for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
					if (active == null || active.getPlayer() == null || active.getUUID().equals(owner.getUniqueId()))
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
					if (offlineFile.getString("player.owner").equals(offlineID.toString()))
						continue;
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
				ti.resetHellblockData();
				hbPlayer.saveHellblockPlayer();
				instance.getAdventureManager().sendMessageWithPrefix(owner,
						"<red>" + input + " has been kicked to your hellblock party!");
				Player player = Bukkit.getPlayer(id);
				if (player != null) {
					instance.getHellblockHandler().teleportToSpawn(player);
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You have been removed from " + owner.getName() + "'s hellblock!");
				}
			} else {
				// TODO: using plugin protection
			}
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
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

			if (instance.getHellblockHandler().isWorldguardProtected()) {
				if (leavingPlayer.getHellblockOwner() == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>An error has occurred. Please report this to the developer.");
					return;
				}
				if (leavingPlayer.getHellblockOwner() != null
						&& leavingPlayer.getHellblockOwner().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You can't leave the hellblock you own!");
					return;
				}
				OfflinePlayer owner;
				if (Bukkit.getPlayer(leavingPlayer.getHellblockOwner()) != null) {
					owner = Bukkit.getPlayer(leavingPlayer.getHellblockOwner());
				} else {
					owner = Bukkit.getOfflinePlayer(leavingPlayer.getHellblockOwner());
				}

				HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(owner.getUniqueId());

				ProtectedRegion region = owner.hasPlayedBefore()
						? instance.getWorldGuardHandler().getRegion(owner.getUniqueId(), ti.getID())
						: null;
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>An error has occurred, please report this to the developer.");
					throw new NullPointerException();
				}

				Set<UUID> party = new HashSet<>();

				if (!ti.isAbandoned()) {
					party = region.getMembers().getUniqueIds();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>You aren't a part of this hellblock island!");
						return;
					}
				} else {
					party = ti.getHellblockParty();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessageWithPrefix(player,
								"<red>You aren't a part of this hellblock island!");
						return;
					}
				}

				if (!ti.isAbandoned()) {
					party.remove(player.getUniqueId());
				} else {
					ti.kickFromHellblockParty(player.getUniqueId());
				}
				leavingPlayer.setHellblock(false, null, 0);
				leavingPlayer.setHome(null);
				leavingPlayer.setHellblockBiome(null);
				leavingPlayer.setLockedStatus(false);
				leavingPlayer.setTotalVisits(0);
				leavingPlayer.setLevel(0);
				leavingPlayer.setCreationTime(0L);
				leavingPlayer.setResetCooldown(0L);
				leavingPlayer.setBiomeCooldown(0L);
				leavingPlayer.setIslandChoice(null);
				leavingPlayer.setUsedSchematic(null);
				leavingPlayer.setBannedPlayers(new HashSet<>());
				leavingPlayer.setHellblockParty(new HashSet<>());
				leavingPlayer.setProtectionFlags(new HashMap<>());
				instance.getHellblockHandler().teleportToSpawn(player);
				if (owner != null && owner.isOnline()) {
					HellblockPlayer ownerPlayer = instance.getHellblockHandler()
							.getActivePlayer(leavingPlayer.getHellblockOwner());
					if (ownerPlayer.getHellblockParty().contains(player.getUniqueId())) {
						ownerPlayer.kickFromHellblockParty(player.getUniqueId());
					}
					ownerPlayer.saveHellblockPlayer();
				} else {
					File ownerFile = new File(instance.getHellblockHandler().getPlayersDirectory() + File.separator
							+ leavingPlayer.getHellblockOwner() + ".yml");
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
					if (active == null || active.getPlayer() == null || active.getUUID().equals(owner.getUniqueId()))
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
					if (offlineFile.getString("player.owner").equals(id.toString()))
						continue;
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
				leavingPlayer.resetHellblockData();
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You have left your hellblock party with "
								+ (owner.getName() != null ? owner.getName() : "Unknown"));
				if (owner.isOnline())
					instance.getAdventureManager().sendMessageWithPrefix(owner.getPlayer(),
							"<red>" + player.getName() + " has just left your hellblock party!");
			} else {
				// TODO: using plugin protection
			}
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
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
				if (instance.getHellblockHandler().isWorldguardProtected()) {
					ProtectedRegion region = instance.getWorldGuardHandler().getRegion(owner.getUniqueId(),
							hbPlayer.getID());
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
						if (active == null || active.getPlayer() == null
								|| active.getUUID().equals(owner.getUniqueId()))
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
						if (offlineFile.getString("player.owner").equals(id.toString()))
							continue;
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
		} else {
			instance.getAdventureManager().sendMessageWithPrefix(Bukkit.getConsoleSender(),
					"<red>An error has occurred, please report this to the developer!");
			throw new NullPointerException();
		}
	}

	public Set<UUID> getVisitors(@NonNull UUID id) {
		Set<UUID> visitors = new HashSet<>();
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
			if (region != null) {
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

	public boolean changeLockStatus(@NonNull HellblockPlayer player) {
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(player.getUUID(), player.getID());
			if (region == null) {
				instance.getAdventureManager().sendMessageWithPrefix(player.getPlayer(),
						"<red>An error has occurred. Please report this to the developer.");
				return false;
			}
			region.setFlag(instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY),
					(!player.getLockedStatus() ? null : StateFlag.State.DENY));
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public @Nullable UUID getHellblockOwnerOfVisitingIsland(@NonNull Player player) {
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegions(player.getUniqueId()).stream().findAny()
					.orElse(null);
			if (region == null) {
				return null;
			}
			UUID ownerUUID = region.getOwners().getUniqueIds().stream().findFirst().orElse(null);
			if (ownerUUID == null) {
				return null;
			}
			return ownerUUID;
		} else {
			// TODO: using plugin protection
			return null;
		}
	}

	public boolean checkIfVisitorIsWelcome(@NonNull Player player, @NonNull UUID id) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
			if (region == null) {
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
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
		if (pi.getLockedStatus()) {
			if (instance.getHellblockHandler().isWorldguardProtected()) {
				ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
				if (region != null) {
					Set<UUID> visitors = getVisitors(id);
					for (UUID visitor : visitors) {
						HellblockPlayer vi = instance.getHellblockHandler().getActivePlayer(visitor);
						if (vi.getPlayer() != null) {
							if (!checkIfVisitorIsWelcome(vi.getPlayer(), id)) {
								if (vi.hasHellblock()) {
									LocationUtils.isSafeLocationAsync(vi.getHomeLocation()).thenAccept((result) -> {
										if (!result.booleanValue()) {
											instance.getAdventureManager().sendMessageWithPrefix(vi.getPlayer(),
													"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
											instance.getHellblockHandler().locateBedrock(visitor)
													.thenAccept((bedrock) -> {
														vi.setHome(bedrock.getBedrockLocation());
														instance.getCoopManager().updateParty(visitor,
																HellblockData.HOME, vi.getHomeLocation());
													});
										}
										ChunkUtils.teleportAsync(vi.getPlayer(), vi.getHomeLocation(),
												TeleportCause.PLUGIN);
									});
								} else {
									instance.getHellblockHandler().teleportToSpawn(vi.getPlayer());
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
		if (hbPlayer.getPlayer() == null) {
			LogUtils.severe("Player object returned null, please report this to the developer.");
			throw new NullPointerException();
		}
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(hbPlayer.getUUID(), hbPlayer.getID());
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
		if (hbPlayer.getPlayer() == null) {
			LogUtils.severe("Player object returned null, please report this to the developer.");
			throw new NullPointerException();
		}
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(hbPlayer.getUUID(), hbPlayer.getID());
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

	public <T> void updateParty(@NonNull UUID id, @NonNull HellblockData type, @Nullable T value) {
		for (HellblockPlayer active : instance.getHellblockHandler().getActivePlayers().values()) {
			if (active == null || active.getPlayer() == null || active.getUUID().equals(id))
				continue;
			if (active.getHellblockOwner().equals(id)) {
				switch (type) {
				case LOCK:
					active.setLockedStatus((boolean) value);
					break;
				case HOME:
					active.setHome((Location) value);
					break;
				case BAN:
					active.banPlayer((UUID) value);
					break;
				case UNBAN:
					active.unbanPlayer((UUID) value);
					break;
				case BIOME:
					active.setHellblockBiome((HellBiome) value);
					break;
				case VISIT:
					active.setTotalVisits(active.getTotalVisitors() + (int) value);
					break;
				case PROTECTION_FLAG:
					active.setProtectionValue((HellblockFlag) value);
					break;
				case LEVEL_ADDITION:
					active.setLevel(active.getLevel() + (float) value);
					break;
				case LEVEL_REMOVAL:
					active.setLevel(active.getLevel() - (float) value);
					break;
				case BIOME_COOLDOWN:
					active.setBiomeCooldown((long) value);
					break;
				case OWNER:
					active.setHellblockOwner((UUID) value);
					break;
				case PARTY_ADDITION:
					active.addToHellblockParty((UUID) value);
					break;
				case PARTY_REMOVAL:
					active.kickFromHellblockParty((UUID) value);
					break;
				case RESET_COOLDOWN:
					active.setResetCooldown((long) value);
					break;
				default:
					break;
				}
			}
			active.saveHellblockPlayer();
		}
		for (File offline : instance.getHellblockHandler().getPlayersDirectory().listFiles()) {
			if (!offline.isFile() || !offline.getName().endsWith(".yml"))
				continue;
			String fileName = Files.getNameWithoutExtension(offline.getName());
			UUID uuid = null;
			try {
				uuid = UUID.fromString(fileName);
			} catch (IllegalArgumentException ignored) {
				// ignored
				continue;
			}
			if (uuid != null && instance.getHellblockHandler().getActivePlayers().keySet().contains(uuid))
				continue;
			YamlConfiguration offlineFile = YamlConfiguration.loadConfiguration(offline);
			if (offlineFile.getString("player.owner").equals(uuid.toString()))
				continue;
			if (offlineFile.getString("player.owner").equals(id.toString())) {
				switch (type) {
				case LOCK:
					offlineFile.set("player.locked-island", (boolean) value);
					break;
				case HOME:
					Location location = (Location) value;
					String world = location.getWorld().getName();
					double x = location.getX();
					double y = location.getY();
					double z = location.getZ();
					float yaw = location.getYaw();
					float pitch = location.getPitch();

					if (!(offlineFile.getString("player.home.world").equalsIgnoreCase(world)
							|| offlineFile.getDouble("player.home.x") == x
							|| offlineFile.getDouble("player.home.y") == y
							|| offlineFile.getDouble("player.home.z") == z
							|| (float) offlineFile.getDouble("player.home.yaw") == yaw
							|| (float) offlineFile.getDouble("player.home.pitch") == pitch)) {
						offlineFile.set("player.home.world", world);
						offlineFile.set("player.home.x", x);
						offlineFile.set("player.home.y", y);
						offlineFile.set("player.home.z", z);
						offlineFile.set("player.home.yaw", yaw);
						offlineFile.set("player.home.pitch", pitch);
					}
					break;
				case BAN:
					List<String> ban = offlineFile.getStringList("player.banned-from-island");
					if (!ban.contains(((UUID) value).toString())) {
						ban.add(((UUID) value).toString());
					}
					offlineFile.set("player.banned-from-island", ban);
					break;
				case UNBAN:
					List<String> unban = offlineFile.getStringList("player.banned-from-island");
					if (unban.contains(((UUID) value).toString())) {
						unban.remove(((UUID) value).toString());
					}
					offlineFile.set("player.banned-from-island", unban);
					break;
				case BIOME:
					offlineFile.set("player.biome", ((HellBiome) value).toString());
					break;
				case VISIT:
					offlineFile.set("player.total-visits", offlineFile.getInt("player.total-visits") + (int) value);
					break;
				case PROTECTION_FLAG:
					Map<FlagType, AccessType> flags = new HashMap<>();
					offlineFile.getConfigurationSection("player.protection-flags").getKeys(false).forEach(key -> {
						FlagType flag = FlagType.valueOf(key);
						AccessType status = AccessType.valueOf(offlineFile.getString("player.protection-flags." + key));
						flags.put(flag, status);
					});
					HellblockFlag flag = (HellblockFlag) value;
					flags.put(flag.getFlag(), flag.getStatus());
					AccessType returnValue = flag.getFlag().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
					for (Map.Entry<FlagType, AccessType> entry : flags.entrySet()) {
						if (entry.getValue() == returnValue)
							continue;
						offlineFile.set("player.protection-flags." + entry.getKey().toString(),
								entry.getValue().toString());
					}
					break;
				case LEVEL_ADDITION:
					offlineFile.set("player.hellblock-level",
							(float) offlineFile.getDouble("player.hellblock-level") + (float) value);
				case LEVEL_REMOVAL:
					offlineFile.set("player.hellblock-level",
							(float) offlineFile.getDouble("player.hellblock-level") - (float) value);
				case BIOME_COOLDOWN:
					offlineFile.set("player.biome-cooldown", (long) value);
					break;
				case OWNER:
					offlineFile.set("player.owner", ((UUID) value).toString());
					break;
				case PARTY_ADDITION:
					List<String> partyAdd = offlineFile.getStringList("player.party");
					if (!partyAdd.contains(((UUID) value).toString())) {
						partyAdd.add(((UUID) value).toString());
					}
					offlineFile.set("player.party", partyAdd);
					break;
				case PARTY_REMOVAL:
					List<String> partyRemove = offlineFile.getStringList("player.party");
					if (partyRemove.contains(((UUID) value).toString())) {
						partyRemove.remove(((UUID) value).toString());
					}
					offlineFile.set("player.party", partyRemove);
					break;
				case RESET_COOLDOWN:
					offlineFile.set("player.reset-cooldown", (long) value);
					break;
				default:
					break;
				}
			}
			try {
				offlineFile.save(offline);
			} catch (IOException ex) {
				LogUtils.warn(String.format("Could not save the offline data file for %s", uuid), ex);
				continue;
			}
		}
	}

	public boolean trackBannedPlayer(@NonNull UUID bannedFromUUID, @NonNull UUID playerUUID) {
		boolean onBannedIsland = false;
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			HellblockPlayer ti = instance.getHellblockHandler().getActivePlayer(bannedFromUUID);
			int hellblockID = ti.getID();
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(bannedFromUUID, hellblockID);
			if (region == null) {
				return false;
			}
			onBannedIsland = instance.getWorldGuardHandler().isPlayerInAnyRegion(playerUUID, region.getId())
					&& ti.getBannedPlayers().contains(playerUUID);
		} else {
			// TODO: using plugin protection
		}

		return onBannedIsland;
	}
}