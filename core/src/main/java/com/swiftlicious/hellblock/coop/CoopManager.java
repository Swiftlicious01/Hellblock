package com.swiftlicious.hellblock.coop;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class CoopManager {

	protected final HellblockPlugin instance;

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void sendInvite(@NonNull UserData onlineUser, @NonNull UserData playerToInvite) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToInvite.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (onlineUser.getHellblockData().isAbandoned()) {
					instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
					return;
				}
				if (playerToInvite.getHellblockData().hasHellblock()) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>This player already has their own hellblock!");
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>You can't invite yourself to your own island!");
					return;
				}
				if (onlineUser.getHellblockData().getParty().contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>This player is already a part of your party!");
					return;
				}
				if (onlineUser.getHellblockData().getBanned().contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>This player is banned from your island!");
					return;
				}
				if (playerToInvite.getHellblockData().hasInvite(owner.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>This player already has an invite from you, wait for them to accept or decline!");
					return;
				}

				playerToInvite.getHellblockData().sendInvitation(owner.getUniqueId());
				instance.getAdventureManager().sendMessage(owner,
						"<red>You've invited <dark_red>" + player.getName() + " <red>to your hellblock!");
				instance.getAdventureManager().sendMessage(player, String.format(
						"<dark_red>%s <red>invited you to their hellblock! <green><bold><click:run_command:/hellcoop accept %s><hover:show_text:'<yellow>Click here to accept!'>[ACCEPT]</click> <red><bold><click:run_command:/hellcoop decline %s><hover:show_text:'<yellow>Click here to decline!'>[DECLINE]</click> <reset><gray>It will expire in %s.",
						owner.getName(), owner.getName(), owner.getName(), instance.getFormattedCooldown(
								playerToInvite.getHellblockData().getInvitations().get(owner.getUniqueId()))));
			} else {
				instance.getAdventureManager().sendMessage(owner,
						"<red>The player you are trying to invite isn't online!");
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void rejectInvite(@NonNull UUID ownerID, @NonNull UserData rejectingPlayer) {
		Player player = rejectingPlayer.getPlayer();
		if (player != null) {
			if (rejectingPlayer.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (!rejectingPlayer.getHellblockData().hasInvite(ownerID)) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You don't have an invite from this player!");
				return;
			}

			rejectingPlayer.getHellblockData().removeInvitation(ownerID);
			if (Bukkit.getPlayer(ownerID) != null) {
				instance.getAdventureManager().sendMessage(Bukkit.getPlayer(ownerID),
						"<dark_red>" + player.getName() + " <red>has rejected your invitation to join!");
			}
			OfflinePlayer owner;
			if (Bukkit.getPlayer(ownerID) != null) {
				owner = Bukkit.getPlayer(ownerID);
			} else {
				owner = Bukkit.getOfflinePlayer(ownerID);
			}
			instance.getAdventureManager().sendMessage(player,
					"<red>You've rejected to join <dark_red>"
							+ (owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???")
							+ "<red>'s hellblock!");
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void listInvitations(@NonNull UserData onlineUser) {
		Player player = onlineUser.getPlayer();
		if (player != null) {
			if (onlineUser.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (onlineUser.getHellblockData().getInvitations() != null
					&& onlineUser.getHellblockData().getInvitations().isEmpty()) {
				instance.getAdventureManager().sendMessage(player, "<red>You don't have any invitations!");
				return;
			}

			for (Entry<UUID, Long> invites : onlineUser.getHellblockData().getInvitations().entrySet()) {
				UUID invitee = invites.getKey();
				OfflinePlayer owner;
				if (Bukkit.getPlayer(invitee) != null) {
					owner = Bukkit.getPlayer(invitee);
				} else {
					owner = Bukkit.getOfflinePlayer(invitee);
				}
				long expirationLeft = invites.getValue().longValue();
				instance.getAdventureManager().sendMessage(player, String.format(
						"<red>Hellblock Invitation: <dark_red>%s <green><bold><click:run_command:/hellcoop accept %s><hover:show_text:'<yellow>Click here to accept!'>[ACCEPT]</click> <red><bold><click:run_command:/hellcoop decline %s><hover:show_text:'<yellow>Click here to decline!'>[DECLINE]</click> <reset><gray>It will expire in %s.",
						owner.getName(), owner.getName(), owner.getName(),
						instance.getFormattedCooldown(expirationLeft)));
			}

		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void addMemberToHellblock(@NonNull UUID ownerID, @NonNull UserData playerToAdd) {
		Player player = playerToAdd.getPlayer();
		if (player != null) {
			if (playerToAdd.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You already have your own hellblock!");
				return;
			}
			if (!playerToAdd.getHellblockData().hasInvite(ownerID)) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You don't have an invite from this player!");
				return;
			}
			instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
						if (offlineUser.getHellblockData().isAbandoned()) {
							instance.getAdventureManager().sendMessage(player,
									"<red>This hellblock is abandoned, you can't join it!");
							return;
						}

						if (instance.getConfigManager().worldguardProtect()) {
							ProtectedRegion region = instance.getWorldGuardHandler().getRegion(offlineUser.getUUID(),
									offlineUser.getHellblockData().getID());
							if (region == null) {
								throw new NullPointerException(
										"Region returned null, please report this to the developer.");
							}
							Set<UUID> party = offlineUser.getHellblockData().getParty();
							if (party.size() >= instance.getConfigManager().partySize()) {
								instance.getAdventureManager().sendMessage(player,
										"<red>The party size is already full for this hellblock!");
								return;
							}
							if (party.contains(player.getUniqueId())) {
								instance.getAdventureManager().sendMessage(player,
										"<red>You're already part of this hellblock party!");
								return;
							}

							if (!region.getMembers().getUniqueIds().contains(player.getUniqueId()))
								region.getMembers().getUniqueIds().add(player.getUniqueId());
							playerToAdd.getHellblockData().clearInvitations();
							playerToAdd.getHellblockData().setHasHellblock(true);
							playerToAdd.getHellblockData().setOwnerUUID(ownerID);
							offlineUser.getHellblockData().addToParty(player.getUniqueId());
							if (playerToAdd.getHellblockData().getTrusted().contains(ownerID)) {
								playerToAdd.getHellblockData().removeTrustPermission(ownerID);
							}
							makeHomeLocationSafe(offlineUser, playerToAdd);
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(playerToAdd.getUUID(),
									playerToAdd.toPlayerData(), instance.getConfigManager().lockData());
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(offlineUser.getUUID(),
									offlineUser.toPlayerData(), instance.getConfigManager().lockData());
							if (offlineUser.isOnline()) {
								instance.getAdventureManager().sendMessage(
										Bukkit.getPlayer(offlineUser.getUUID()),
										"<red>" + player.getName() + " has been added to your hellblock party!");
							}
							instance.getAdventureManager().sendMessage(player,
									"<red>You've joined " + offlineUser.getName() + "'s hellblock!");
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void removeMemberFromHellblock(@NonNull UserData onlineUser, @NonNull String input, @NonNull UUID id) {
		Player owner = onlineUser.getPlayer();
		if (owner != null) {
			if (!onlineUser.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(owner, instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
						if (offlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}

						if (owner.getUniqueId().equals(offlineUser.getHellblockData().getOwnerUUID())) {
							instance.getAdventureManager().sendMessage(owner,
									"<red>You can't kick yourself from your own island!");
							return;
						}
						if (!offlineUser.getHellblockData().hasHellblock()) {
							instance.getAdventureManager().sendMessage(owner,
									"<red>This player is not a part of your hellblock party!");
							return;
						}

						if (instance.getConfigManager().worldguardProtect()) {
							ProtectedRegion region = instance.getWorldGuardHandler().getRegion(owner.getUniqueId(),
									onlineUser.getHellblockData().getID());
							if (region == null) {
								throw new NullPointerException(
										"Region returned null, please report this to the developer.");
							}
							Set<UUID> party = onlineUser.getHellblockData().getParty();
							if (!party.contains(id)) {
								instance.getAdventureManager().sendMessage(owner,
										"<red>" + input + " is not a part of your hellblock party!");
								return;
							}

							if (region.getMembers().getUniqueIds().contains(id))
								region.getMembers().getUniqueIds().remove(id);
							offlineUser.getHellblockData().setHasHellblock(false);
							offlineUser.getHellblockData().setOwnerUUID(null);
							onlineUser.getHellblockData().kickFromParty(id);
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(onlineUser.getUUID(),
									onlineUser.toPlayerData(), instance.getConfigManager().lockData());
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(offlineUser.getUUID(),
									offlineUser.toPlayerData(), instance.getConfigManager().lockData());
							instance.getAdventureManager().sendMessage(owner,
									"<red>" + input + " has been kicked to your hellblock party!");
							if (offlineUser.isOnline()) {
								instance.getHellblockHandler().teleportToSpawn(Bukkit.getPlayer(offlineUser.getUUID()),
										true);
								instance.getAdventureManager().sendMessage(
										Bukkit.getPlayer(offlineUser.getUUID()),
										"<red>You've been removed from " + owner.getName() + "'s hellblock!");
							}
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void leaveHellblockParty(@NonNull UserData leavingPlayer) {
		Player player = leavingPlayer.getPlayer();
		if (player != null) {
			if (!leavingPlayer.getHellblockData().hasHellblock()) {
				instance.getAdventureManager().sendMessage(player, instance.getTranslationManager()
						.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
				return;
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() != null
					&& leavingPlayer.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				instance.getAdventureManager().sendMessage(player,
						"<red>You can't leave the hellblock you own!");
				return;
			}
			instance.getStorageManager().getOfflineUserData(leavingPlayer.getHellblockData().getOwnerUUID(),
					instance.getConfigManager().lockData()).thenAccept((result) -> {
						UserData offlineUser = result.orElseThrow();
						if (instance.getConfigManager().worldguardProtect()) {
							ProtectedRegion region = instance.getWorldGuardHandler().getRegion(offlineUser.getUUID(),
									offlineUser.getHellblockData().getID());
							if (region == null) {
								throw new NullPointerException(
										"Region returned null, please report this to the developer.");
							}
							Set<UUID> party = offlineUser.getHellblockData().getParty();

							party = offlineUser.getHellblockData().getParty();
							if (!party.contains(player.getUniqueId())) {
								instance.getAdventureManager().sendMessage(player,
										"<red>You aren't a part of this hellblock island!");
								return;
							}

							if (region.getMembers().getUniqueIds().contains(leavingPlayer.getUUID()))
								region.getMembers().getUniqueIds().remove(leavingPlayer.getUUID());
							leavingPlayer.getHellblockData().setHasHellblock(false);
							leavingPlayer.getHellblockData().setOwnerUUID(null);
							offlineUser.getHellblockData().kickFromParty(player.getUniqueId());
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(
									leavingPlayer.getUUID(), leavingPlayer.toPlayerData(),
									instance.getConfigManager().lockData());
							instance.getStorageManager().getDataSource().updateOrInsertPlayerData(offlineUser.getUUID(),
									offlineUser.toPlayerData(), instance.getConfigManager().lockData());
							instance.getHellblockHandler().teleportToSpawn(player, true);
							instance.getAdventureManager().sendMessage(player,
									"<red>You've left your hellblock party with " + offlineUser.getName());
							if (offlineUser.isOnline())
								instance.getAdventureManager().sendMessage(
										Bukkit.getPlayer(offlineUser.getUUID()),
										"<red>" + player.getName() + " has just left your hellblock party!");
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void transferOwnershipOfHellblock(@NonNull UserData onlineUser, @NonNull UserData playerToTransfer) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToTransfer.getPlayer();
		if (owner != null) {
			if (player != null) {
				if (!instance.getConfigManager().transferIslands()) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>Transferring hellblock islands has been disabled!");
					return;
				}
				if (!onlineUser.getHellblockData().hasHellblock()) {
					instance.getAdventureManager().sendMessage(owner, instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
					return;
				}
				if (onlineUser.getHellblockData().isAbandoned()) {
					instance.getAdventureManager().sendMessage(owner, instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
					return;
				}
				if (onlineUser.getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.getHellblockData().getOwnerUUID() != null
						&& !onlineUser.getHellblockData().getOwnerUUID().equals(owner.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner, instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>You can't transfer ownership to yourself for your own island!");
					return;
				}
				if (!playerToTransfer.getHellblockData().hasHellblock()) {
					instance.getAdventureManager().sendMessage(owner,
							"<red>This player isn't a member of your hellblock party!");
					return;
				}
				if (instance.getConfigManager().worldguardProtect()) {
					ProtectedRegion region = instance.getWorldGuardHandler().getRegion(owner.getUniqueId(),
							onlineUser.getHellblockData().getID());
					if (region == null) {
						throw new NullPointerException("Region returned null, please report this to the developer.");
					}
					Set<UUID> party = onlineUser.getHellblockData().getParty();
					if (!party.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessage(owner,
								"<red>" + player.getName() + " is not a part of your hellblock party!");
						return;
					}

					Set<UUID> owners = region.getOwners().getUniqueIds();
					if (!owners.contains(owner.getUniqueId())) {
						instance.getAdventureManager().sendMessage(owner, instance.getTranslationManager()
								.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
						return;
					}
					if (owners.contains(player.getUniqueId())) {
						instance.getAdventureManager().sendMessage(owner,
								"<red>" + player.getName() + " is already the owner of this hellblock!");
						return;
					}

					owners.add(player.getUniqueId());
					owners.remove(owner.getUniqueId());
					if (!region.getMembers().getUniqueIds().contains(owner.getUniqueId()))
						region.getMembers().getUniqueIds().add(owner.getUniqueId());
					if (region.getMembers().getUniqueIds().contains(player.getUniqueId()))
						region.getMembers().getUniqueIds().remove(player.getUniqueId());
					playerToTransfer.getHellblockData().transferHellblockData(onlineUser);
					playerToTransfer.getHellblockData().setTransferCooldown(86400L);
					playerToTransfer.getHellblockData().setOwnerUUID(player.getUniqueId());
					playerToTransfer.getHellblockData().kickFromParty(player.getUniqueId());
					for (UUID partyData : playerToTransfer.getHellblockData().getParty()) {
						instance.getStorageManager()
								.getOfflineUserData(partyData, instance.getConfigManager().lockData())
								.thenAccept((result) -> {
									UserData offlineParty = result.orElseThrow();
									offlineParty.getHellblockData().setOwnerUUID(player.getUniqueId());
									instance.getStorageManager().getDataSource().updateOrInsertPlayerData(
											offlineParty.getUUID(), offlineParty.toPlayerData(),
											instance.getConfigManager().lockData());
								});
					}
					playerToTransfer.getHellblockData().addToParty(owner.getUniqueId());
					onlineUser.getHellblockData().resetHellblockData();
					onlineUser.getHellblockData().setHasHellblock(true);
					onlineUser.getHellblockData().setID(0);
					onlineUser.getHellblockData().setTrusted(new HashSet<>());
					onlineUser.getHellblockData().setResetCooldown(0L);
					onlineUser.getHellblockData().setOwnerUUID(player.getUniqueId());
					instance.getStorageManager().getDataSource().updateOrInsertPlayerData(onlineUser.getUUID(),
							onlineUser.toPlayerData(), instance.getConfigManager().lockData());
					instance.getStorageManager().getDataSource().updateOrInsertPlayerData(playerToTransfer.getUUID(),
							playerToTransfer.toPlayerData(), instance.getConfigManager().lockData());

					if (instance.getWorldGuardHandler().getWorldGuardPlatform() == null) {
						throw new NullPointerException("Could not retrieve WorldGuard platform.");
					}
					RegionContainer regionContainer = instance.getWorldGuardHandler().getWorldGuardPlatform()
							.getRegionContainer();
					RegionManager regionManager = regionContainer
							.get(BukkitAdapter.adapt(instance.getHellblockHandler().getHellblockWorld()));
					if (regionManager == null) {
						instance.getPluginLogger()
								.severe(String.format("Could not get the WorldGuard region manager for the world: %s",
										instance.getConfigManager().worldName()));
						return;
					}

					ProtectedRegion renamedRegion = new ProtectedCuboidRegion(
							String.format("%s_%s", playerToTransfer.getUUID().toString(),
									playerToTransfer.getHellblockData().getID()),
							instance.getWorldGuardHandler().getProtectionVectorLeft(
									playerToTransfer.getHellblockData().getHellblockLocation()),
							instance.getWorldGuardHandler().getProtectionVectorRight(
									playerToTransfer.getHellblockData().getHellblockLocation()));

					try {
						renamedRegion.setOwners(region.getOwners());
						renamedRegion.setMembers(region.getMembers());
						renamedRegion.setFlags(region.getFlags());
						renamedRegion.setPriority(region.getPriority());
						renamedRegion.setParent(region.getParent());
						regionManager.addRegion(renamedRegion);
						regionManager.removeRegion(region.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);
						regionManager.save();
					} catch (Exception ex) {
						instance.getPluginLogger().severe(
								String.format("Unable to reprotect %s's hellblock!", playerToTransfer.getName()), ex);
						return;
					}
					instance.getWorldGuardHandler().updateHellblockMessages(playerToTransfer.getUUID(), renamedRegion);
					changeLockStatus(playerToTransfer);

					instance.getAdventureManager().sendMessage(owner,
							"<red>" + player.getName() + " is the new owner of your hellblock!");
					instance.getAdventureManager().sendMessage(player,
							"<red>You've been made the new owner of " + owner.getName() + "'s hellblock!");
				} else {
					// TODO: using plugin protection
				}
			} else {
				instance.getAdventureManager().sendMessage(owner,
						"<red>The player you are trying to transfer with isn't online!");
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public Set<UUID> getVisitors(@NonNull UUID id) {
		Set<UUID> visitors = new HashSet<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					if (instance.getConfigManager().worldguardProtect()) {
						ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id,
								offlineUser.getHellblockData().getID());
						if (region != null) {
							for (UserData user : instance.getStorageManager().getOnlineUsers()) {
								if (user == null)
									continue;
								if (user.isOnline()) {
									Player onlinePlayer = user.getPlayer();
									if (onlinePlayer.getLocation() == null)
										continue;
									if (region.contains(onlinePlayer.getLocation().getBlockX(),
											onlinePlayer.getLocation().getBlockY(),
											onlinePlayer.getLocation().getBlockZ())) {
										visitors.add(onlinePlayer.getUniqueId());
									}
								}
							}
						}
					} else {
						// TODO: using plugin protection
					}
				}).join();
		return visitors;
	}

	public boolean changeLockStatus(@NonNull UserData onlineUser) {
		if (instance.getConfigManager().worldguardProtect()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(onlineUser.getUUID(),
					onlineUser.getHellblockData().getID());
			if (region == null) {
				throw new NullPointerException("Region returned null, please report this to the developer.");
			}
			region.setFlag(instance.getIslandProtectionManager().convertToWorldGuardFlag(HellblockFlag.FlagType.ENTRY),
					(!onlineUser.getHellblockData().isLocked() ? null : StateFlag.State.DENY));
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public @Nullable UUID getHellblockOwnerOfVisitingIsland(@NonNull Player player) {
		if (instance.getConfigManager().worldguardProtect()) {
			if (player.getLocation() == null)
				return null;
			if (instance.getHellblockHandler().checkIfInSpawn(player.getLocation()))
				return null;
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
		VisitorTracker welcomedOnIsland = new VisitorTracker();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					if (instance.getConfigManager().worldguardProtect()) {
						ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id,
								offlineUser.getHellblockData().getID());
						if (region == null) {
							throw new NullPointerException(
									"Region returned null, please report this to the developer.");
						}
						Set<UUID> owners = region.getOwners().getUniqueIds();
						Set<UUID> members = region.getMembers().getUniqueIds();
						welcomedOnIsland.setAsWelcomed(
								(owners.contains(player.getUniqueId()) || members.contains(player.getUniqueId())
										|| offlineUser.getHellblockData().getTrusted().contains(player.getUniqueId())
										|| player.hasPermission("hellblock.bypass.lock")
										|| player.hasPermission("hellblock.admin") || player.isOp()));
					} else {
						// TODO: using plugin protection
						welcomedOnIsland.setAsWelcomed(false);
					}
				}).join();
		return welcomedOnIsland.isWelcomed();
	}

	public void kickVisitorsIfLocked(@NonNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					if (offlineUser.getHellblockData().isLocked()) {
						if (instance.getConfigManager().worldguardProtect()) {
							ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id,
									offlineUser.getHellblockData().getID());
							if (region != null) {
								Set<UUID> visitors = getVisitors(id);
								for (UUID visitor : visitors) {
									Optional<UserData> onlineUser = instance.getStorageManager().getOnlineUser(visitor);
									if (onlineUser.isEmpty())
										continue;
									if (onlineUser.get().isOnline() && onlineUser.get().getPlayer() != null) {
										if (!checkIfVisitorIsWelcome(onlineUser.get().getPlayer(), id)) {
											if (onlineUser.get().getHellblockData().hasHellblock()) {
												if (onlineUser.get().getHellblockData().getOwnerUUID() == null) {
													throw new NullPointerException(
															"Owner reference returned null, please report this to the developer.");
												}
												instance.getStorageManager()
														.getOfflineUserData(
																onlineUser.get().getHellblockData().getOwnerUUID(),
																instance.getConfigManager().lockData())
														.thenAccept((owner) -> {
															UserData visitorOwner = owner.orElseThrow();
															makeHomeLocationSafe(visitorOwner, onlineUser.get());
														});
											} else {
												instance.getHellblockHandler()
														.teleportToSpawn(onlineUser.get().getPlayer(), true);
											}
											instance.getAdventureManager().sendMessage(
													onlineUser.get().getPlayer(),
													"<red>The hellblock you are trying to enter has been locked from having visitors at the moment.");
										}
									}
								}
							}
						} else {
							// TODO: using plugin protection
						}
					}
				});
	}

	public boolean addTrustAccess(@NonNull UserData onlineUser, @NonNull String input, @NonNull UUID id) {
		if (!onlineUser.isOnline()) {
			throw new NullPointerException("Player object returned null, please report this to the developer.");
		}
		if (instance.getConfigManager().worldguardProtect()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(onlineUser.getUUID(),
					onlineUser.getHellblockData().getID());
			if (region == null) {
				throw new NullPointerException("Region returned null, please report this to the developer.");
			}
			Set<UUID> trusted = region.getMembers().getUniqueIds();
			if (trusted.contains(id)) {
				instance.getAdventureManager().sendMessage(onlineUser.getPlayer(),
						"<red>" + input + " is already trusted on your hellblock!");
				return false;
			}

			return trusted.add(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public boolean removeTrustAccess(@NonNull UserData onlineUser, @NonNull String input, @NonNull UUID id) {
		if (!onlineUser.isOnline()) {
			throw new NullPointerException("Player object returned null, please report this to the developer.");
		}
		if (instance.getConfigManager().worldguardProtect()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(onlineUser.getUUID(),
					onlineUser.getHellblockData().getID());
			if (region == null) {
				throw new NullPointerException("Region returned null, please report this to the developer.");
			}
			Set<UUID> trusted = region.getMembers().getUniqueIds();
			if (!trusted.contains(id)) {
				instance.getAdventureManager().sendMessage(onlineUser.getPlayer(),
						"<red>" + input + " isn't trusted on your hellblock!");
				return false;
			}

			return trusted.remove(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public boolean trackBannedPlayer(@NonNull UUID bannedFromUUID, @NonNull UUID playerUUID) {
		BanTracker onBannedIsland = new BanTracker();
		instance.getStorageManager().getOfflineUserData(bannedFromUUID, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					UserData offlineUser = result.orElseThrow();
					if (instance.getConfigManager().worldguardProtect()) {
						int hellblockID = offlineUser.getHellblockData().getID();
						ProtectedRegion region = instance.getWorldGuardHandler().getRegion(bannedFromUUID, hellblockID);
						if (region == null) {
							return;
						}
						onBannedIsland.setAsBanned(
								instance.getWorldGuardHandler().isPlayerInAnyRegion(playerUUID, region.getId())
										&& offlineUser.getHellblockData().getBanned().contains(playerUUID));
					} else {
						// TODO: using plugin protection
					}
				}).join();

		return onBannedIsland.isBanned();
	}

	public CompletableFuture<Void> makeHomeLocationSafe(@NonNull UserData offlineUser, @NonNull UserData onlineUser) {
		return CompletableFuture.runAsync(() -> {
			if (!onlineUser.isOnline()) {
				throw new NullPointerException("Player object returned null, please report this to the developer.");
			}
			LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation()).thenAccept((safe) -> {
				if (!safe.booleanValue()) {
					instance.getAdventureManager().sendMessage(onlineUser.getPlayer(),
							"<red>This hellblock home location was deemed not safe, resetting to bedrock location!");
					instance.getHellblockHandler().locateBedrock(offlineUser.getUUID()).thenAccept((bedrock) -> {
						offlineUser.getHellblockData().setHomeLocation(bedrock.getBedrockLocation());
					}).thenRunAsync(() -> {
						ChunkUtils.teleportAsync(onlineUser.getPlayer(),
								offlineUser.getHellblockData().getHomeLocation(), TeleportCause.PLUGIN);
					});
				}
			});
		});
	}

	public class BanTracker {

		private boolean banTracking;

		public boolean isBanned() {
			return this.banTracking;
		}

		public void setAsBanned(boolean banTracking) {
			this.banTracking = banTracking;
		}
	}

	public class VisitorTracker {

		private boolean visitorTracking;

		public boolean isWelcomed() {
			return this.visitorTracking;
		}

		public void setAsWelcomed(boolean visitorTracking) {
			this.visitorTracking = visitorTracking;
		}
	}
}