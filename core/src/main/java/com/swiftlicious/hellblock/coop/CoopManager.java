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
import org.jetbrains.annotations.NotNull;
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
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.utils.ChunkUtils;
import com.swiftlicious.hellblock.utils.LocationUtils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class CoopManager {

	protected final HellblockPlugin instance;

	public CoopManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	public void sendInvite(@NotNull UserData onlineUser, @NotNull UserData playerToInvite) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToInvite.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (player != null) {
				Audience audience = instance.getSenderFactory().getAudience(player);
				if (onlineUser.getHellblockData().isAbandoned()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
					return;
				}
				if (playerToInvite.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_HAS_HELLBLOCK.build()));
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_SELF.build()));
					return;
				}
				if (onlineUser.getHellblockData().getParty().contains(player.getUniqueId())) {
					ownerAudience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_IN_PARTY
									.arguments(Component.text(player.getName())).build()));
					return;
				}
				if (onlineUser.getHellblockData().getBanned().contains(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_BANNED_FROM_INVITE.build()));
					return;
				}
				if (playerToInvite.getHellblockData().hasInvite(owner.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_EXISTS.build()));
					return;
				}

				playerToInvite.getHellblockData().sendInvitation(owner.getUniqueId());
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_SENT
								.arguments(Component.text(player.getName())).build()));
				audience.sendMessage(
						instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_RECEIVED
										.arguments(Component.text(owner.getName()),
												Component.text(instance.getFormattedCooldown(playerToInvite
														.getHellblockData().getInvitations().get(owner.getUniqueId()))))
										.build()));
			} else {
				ownerAudience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE.build()));
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void rejectInvite(@NotNull UUID ownerID, @NotNull UserData rejectingPlayer) {
		Player player = rejectingPlayer.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (rejectingPlayer.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (!rejectingPlayer.getHellblockData().hasInvite(ownerID)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND.build()));
				return;
			}

			rejectingPlayer.getHellblockData().removeInvitation(ownerID);
			if (Bukkit.getPlayer(ownerID) != null) {
				Audience ownerAudience = instance.getSenderFactory().getAudience(Bukkit.getPlayer(ownerID));
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_REJECTED_TO_OWNER
								.arguments(Component.text(player.getName())).build()));
			}
			OfflinePlayer owner;
			if (Bukkit.getPlayer(ownerID) != null) {
				owner = Bukkit.getPlayer(ownerID);
			} else {
				owner = Bukkit.getOfflinePlayer(ownerID);
			}
			String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";
			audience.sendMessage(instance.getTranslationManager().render(
					MessageConstants.MSG_HELLBLOCK_COOP_INVITE_REJECTED.arguments(Component.text(username)).build()));
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void listInvitations(@NotNull UserData onlineUser) {
		Player player = onlineUser.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (onlineUser.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (onlineUser.getHellblockData().getInvitations() != null
					&& onlineUser.getHellblockData().getInvitations().isEmpty()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITES.build()));
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
				String username = owner.hasPlayedBefore() && owner.getName() != null ? owner.getName() : "???";
				long expirationLeft = invites.getValue().longValue();
				audience.sendMessage(
						instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITATION_LIST
										.arguments(Component.text(username),
												Component.text(instance.getFormattedCooldown(expirationLeft)))
										.build()));
			}

		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void addMemberToHellblock(@NotNull UUID ownerID, @NotNull UserData playerToAdd) {
		Player player = playerToAdd.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (playerToAdd.getHellblockData().hasHellblock()) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_HELLBLOCK_EXISTS.build()));
				return;
			}
			if (!playerToAdd.getHellblockData().hasInvite(ownerID)) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_INVITE_FOUND.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(ownerID, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().isAbandoned()) {
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_INVITE_ABANDONED.build()));
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
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_FULL.build()));
								return;
							}
							if (party.contains(player.getUniqueId())) {
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_JOINED_PARTY.build()));
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
								Audience ownerAudience = instance.getSenderFactory()
										.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
								ownerAudience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_ADDED_TO_PARTY
												.arguments(Component.text(player.getName())).build()));
							}
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_JOINED_PARTY
											.arguments(Component.text(offlineUser.getName())).build()));
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void removeMemberFromHellblock(@NotNull UserData onlineUser, @NotNull String input, @NotNull UUID id) {
		Player owner = onlineUser.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (!onlineUser.getHellblockData().hasHellblock()) {
				ownerAudience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
					.thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
						if (offlineUser.getHellblockData().getOwnerUUID() == null) {
							throw new NullPointerException(
									"Owner reference returned null, please report this to the developer.");
						}

						if (owner.getUniqueId().equals(offlineUser.getHellblockData().getOwnerUUID())) {
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_KICK_SELF.build()));
							return;
						}
						if (!offlineUser.getHellblockData().hasHellblock()) {
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
											.arguments(Component.text(input)).build()));
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
								ownerAudience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
												.arguments(Component.text(input)).build()));
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
							ownerAudience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_KICKED
											.arguments(Component.text(input)).build()));
							if (offlineUser.isOnline()) {
								instance.getHellblockHandler().teleportToSpawn(Bukkit.getPlayer(offlineUser.getUUID()),
										true);
								Audience audience = instance.getSenderFactory()
										.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_REMOVED_FROM_PARTY
												.arguments(Component.text(owner.getName())).build()));
							}
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void leaveHellblockParty(@NotNull UserData leavingPlayer) {
		Player player = leavingPlayer.getPlayer();
		if (player != null) {
			Audience audience = instance.getSenderFactory().getAudience(player);
			if (!leavingPlayer.getHellblockData().hasHellblock()) {
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
				return;
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() == null) {
				throw new NullPointerException("Owner reference returned null, please report this to the developer.");
			}
			if (leavingPlayer.getHellblockData().getOwnerUUID() != null
					&& leavingPlayer.getHellblockData().getOwnerUUID().equals(player.getUniqueId())) {
				audience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_NO_LEAVE.build()));
				return;
			}
			instance.getStorageManager().getOfflineUserData(leavingPlayer.getHellblockData().getOwnerUUID(),
					instance.getConfigManager().lockData()).thenAccept((result) -> {
						if (result.isEmpty())
							return;
						UserData offlineUser = result.get();
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
								audience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_IN_PARTY.build()));
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
							audience.sendMessage(instance.getTranslationManager()
									.render(MessageConstants.MSG_HELLBLOCK_COOP_PARTY_LEFT
											.arguments(Component.text(offlineUser.getName())).build()));
							if (offlineUser.isOnline()) {
								Audience ownerAudience = instance.getSenderFactory()
										.getAudience(Bukkit.getPlayer(offlineUser.getUUID()));
								ownerAudience.sendMessage(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_COOP_LEFT_PARTY
												.arguments(Component.text(player.getName())).build()));
							}
						} else {
							// TODO: using plugin protection
						}
					});
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public void transferOwnershipOfHellblock(@NotNull UserData onlineUser, @NotNull UserData playerToTransfer) {
		Player owner = onlineUser.getPlayer();
		Player player = playerToTransfer.getPlayer();
		if (owner != null) {
			Audience ownerAudience = instance.getSenderFactory().getAudience(owner);
			if (player != null) {
				Audience audience = instance.getSenderFactory().getAudience(player);
				if (!instance.getConfigManager().transferIslands()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_TRANSFER_OWNERSHIP_DISABLED.build()));
					return;
				}
				if (!onlineUser.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
					return;
				}
				if (onlineUser.getHellblockData().isAbandoned()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build()));
					return;
				}
				if (onlineUser.getHellblockData().getOwnerUUID() == null) {
					throw new NullPointerException(
							"Owner reference returned null, please report this to the developer.");
				}
				if (onlineUser.getHellblockData().getOwnerUUID() != null
						&& !onlineUser.getHellblockData().getOwnerUUID().equals(owner.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
					return;
				}
				if (owner.getUniqueId().equals(player.getUniqueId())) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NO_TRANSFER_SELF.build()));
					return;
				}
				if (!playerToTransfer.getHellblockData().hasHellblock()) {
					ownerAudience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
									.arguments(Component.text(player.getName())).build()));
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
						ownerAudience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_PART_OF_PARTY
										.arguments(Component.text(player.getName())).build()));
						return;
					}

					Set<UUID> owners = region.getOwners().getUniqueIds();
					if (!owners.contains(owner.getUniqueId())) {
						ownerAudience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
						return;
					}
					if (onlineUser.getHellblockData().getOwnerUUID().equals(player.getUniqueId())
							|| owners.contains(player.getUniqueId())) {
						ownerAudience.sendMessage(instance.getTranslationManager()
								.render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_OWNER_OF_ISLAND
										.arguments(Component.text(player.getName())).build()));
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
									if (result.isEmpty())
										return;
									UserData offlineParty = result.get();
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

					ownerAudience.sendMessage(
							instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NEW_OWNER_SET
									.arguments(Component.text(player.getName())).build()));
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_COOP_OWNER_TRANSFER_SUCCESS
									.arguments(Component.text(owner.getName())).build()));
				} else {
					// TODO: using plugin protection
				}
			} else {
				ownerAudience.sendMessage(instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_PLAYER_OFFLINE.build()));
				return;
			}
		} else {
			throw new NullPointerException("Player returned null, please report this to the developer.");
		}
	}

	public Set<UUID> getVisitors(@NotNull UUID id) {
		Set<UUID> visitors = new HashSet<>();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
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

	public boolean changeLockStatus(@NotNull UserData onlineUser) {
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

	public @Nullable UUID getHellblockOwnerOfVisitingIsland(@NotNull Player player) {
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

	public boolean checkIfVisitorIsWelcome(@NotNull Player player, @NotNull UUID id) {
		VisitorTracker welcomedOnIsland = new VisitorTracker();
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
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

	public void kickVisitorsIfLocked(@NotNull UUID id) {
		instance.getStorageManager().getOfflineUserData(id, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
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
															if (owner.isEmpty())
																return;
															UserData visitorOwner = owner.get();
															makeHomeLocationSafe(visitorOwner, onlineUser.get());
														});
											} else {
												instance.getHellblockHandler()
														.teleportToSpawn(onlineUser.get().getPlayer(), true);
											}
											Audience audience = instance.getSenderFactory()
													.getAudience(onlineUser.get().getPlayer());
											audience.sendMessage(instance.getTranslationManager()
													.render(MessageConstants.MSG_HELLBLOCK_LOCKED_ENTRY.build()));
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

	public boolean addTrustAccess(@NotNull UserData onlineUser, @NotNull String input, @NotNull UUID id) {
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
				Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_ALREADY_TRUSTED
								.arguments(AdventureHelper.miniMessage(input)).build()));
				return false;
			}

			return trusted.add(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public boolean removeTrustAccess(@NotNull UserData onlineUser, @NotNull String input, @NotNull UUID id) {
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
				Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
				audience.sendMessage(
						instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_COOP_NOT_TRUSTED
								.arguments(AdventureHelper.miniMessage(input)).build()));
				return false;
			}

			return trusted.remove(id);
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public boolean trackBannedPlayer(@NotNull UUID bannedFromUUID, @NotNull UUID playerUUID) {
		BanTracker onBannedIsland = new BanTracker();
		instance.getStorageManager().getOfflineUserData(bannedFromUUID, instance.getConfigManager().lockData())
				.thenAccept((result) -> {
					if (result.isEmpty())
						return;
					UserData offlineUser = result.get();
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

	public CompletableFuture<Void> makeHomeLocationSafe(@NotNull UserData offlineUser, @NotNull UserData onlineUser) {
		return CompletableFuture.runAsync(() -> {
			if (!onlineUser.isOnline()) {
				throw new NullPointerException("Player object returned null, please report this to the developer.");
			}
			LocationUtils.isSafeLocationAsync(offlineUser.getHellblockData().getHomeLocation()).thenAccept((safe) -> {
				if (!safe.booleanValue()) {
					Audience audience = instance.getSenderFactory().getAudience(onlineUser.getPlayer());
					audience.sendMessage(instance.getTranslationManager()
							.render(MessageConstants.MSG_HELLBLOCK_RESET_HOME_TO_BEDROCK.build()));
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

	protected class BanTracker {

		private boolean banTracking;

		public boolean isBanned() {
			return this.banTracking;
		}

		public void setAsBanned(boolean banTracking) {
			this.banTracking = banTracking;
		}
	}

	protected class VisitorTracker {

		private boolean visitorTracking;

		public boolean isWelcomed() {
			return this.visitorTracking;
		}

		public void setAsWelcomed(boolean visitorTracking) {
			this.visitorTracking = visitorTracking;
		}
	}
}