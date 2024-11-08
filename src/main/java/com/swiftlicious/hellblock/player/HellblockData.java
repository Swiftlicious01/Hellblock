package com.swiftlicious.hellblock.player;

import java.util.AbstractMap.SimpleEntry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeData;
import com.swiftlicious.hellblock.challenges.HellblockChallenge;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.CompletionStatus;
import com.swiftlicious.hellblock.challenges.ProgressBar;
import com.swiftlicious.hellblock.coop.HellblockParty;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

import lombok.NonNull;

public class HellblockData {

	@SerializedName("id")
	public int id;
	@SerializedName("level")
	public float level;
	@SerializedName("exists")
	public boolean hasHellblock;
	@SerializedName("owner")
	public UUID ownerUUID;
	@SerializedName("linked")
	public UUID linkedUUID;
	@SerializedName("bounds")
	public BoundingBox boundingBox;
	@SerializedName("party")
	public Set<UUID> party;
	@SerializedName("trusted")
	public Set<UUID> trusted;
	@SerializedName("banned")
	public Set<UUID> banned;
	@SerializedName("invitations")
	public Map<UUID, Long> invitations;
	@SerializedName("flags")
	public Map<FlagType, AccessType> flags;
	@SerializedName("challenges")
	public Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges;
	@SerializedName("location")
	public Location location;
	@SerializedName("home")
	public Location home;
	@SerializedName("creation")
	public long creationTime;
	@SerializedName("visitors")
	public int visitors;
	@SerializedName("biome")
	public HellBiome biome;
	@SerializedName("choice")
	public IslandOptions choice;
	@SerializedName("schematic")
	public String schematic;
	@SerializedName("locked")
	public boolean locked;
	@SerializedName("abandoned")
	public boolean abandoned;
	@SerializedName("resetcooldown")
	public long resetCooldown;
	@SerializedName("biomecooldown")
	public long biomeCooldown;
	@SerializedName("transfercooldown")
	public long transferCooldown;

	public final static float DEFAULT_LEVEL = 1.0F;

	public HellblockData(int id, float level, boolean hasHellblock, UUID ownerUUID, UUID linkedUUID,
			BoundingBox boundingBox, Set<UUID> party, Set<UUID> trusted, Set<UUID> banned, Map<UUID, Long> invitations,
			Map<FlagType, AccessType> flags, Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges,
			Location location, Location home, long creationTime, int visitors, HellBiome biome, IslandOptions choice,
			String schematic, boolean locked, boolean abandoned, long resetCooldown, long biomeCooldown,
			long transferCooldown) {
		this.id = id;
		this.level = level;
		this.hasHellblock = hasHellblock;
		this.ownerUUID = ownerUUID;
		this.linkedUUID = linkedUUID;
		this.boundingBox = boundingBox;
		this.party = party;
		this.trusted = trusted;
		this.banned = banned;
		this.invitations = invitations;
		this.flags = flags;
		this.challenges = challenges;
		this.location = location;
		this.home = home;
		this.creationTime = creationTime;
		this.visitors = visitors;
		this.biome = biome;
		this.choice = choice;
		this.schematic = schematic;
		this.locked = locked;
		this.abandoned = abandoned;
		this.resetCooldown = resetCooldown;
		this.biomeCooldown = biomeCooldown;
		this.transferCooldown = transferCooldown;
	}

	public boolean hasHellblock() {
		return this.hasHellblock;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public boolean isAbandoned() {
		return this.abandoned;
	}

	public int getID() {
		return this.id;
	}

	public float getLevel() {
		return this.level;
	}

	public int getTotalVisits() {
		return this.visitors;
	}

	public @Nullable HellBiome getBiome() {
		return this.biome;
	}

	public @Nullable IslandOptions getIslandChoice() {
		return this.choice;
	}

	public @Nullable String getUsedSchematic() {
		return this.schematic;
	}

	public long getCreation() {
		return this.creationTime;
	}

	public @NonNull String getCreationTime() {
		LocalDateTime localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.creationTime),
				ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("KK:mm:ss a", Locale.ENGLISH);
		String now = localDate.format(formatter);
		return String.format("%s %s %s %s", localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
				localDate.getDayOfMonth(), localDate.getYear(), now);
	}

	public long getResetCooldown() {
		return this.resetCooldown;
	}

	public long getBiomeCooldown() {
		return this.biomeCooldown;
	}

	public long getTransferCooldown() {
		return this.transferCooldown;
	}

	public @Nullable UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	public @Nullable UUID getLinkedUUID() {
		return this.linkedUUID;
	}

	public @Nullable BoundingBox getBoundingBox() {
		return this.boundingBox;
	}

	public @Nullable Location getHellblockLocation() {
		return this.location;
	}

	public @Nullable Location getHomeLocation() {
		return this.home;
	}

	public @Nullable Set<UUID> getParty() {
		return this.party;
	}

	public @Nullable Set<UUID> getTrusted() {
		return this.trusted;
	}

	public @Nullable Set<UUID> getBanned() {
		return this.banned;
	}

	public @Nullable HellblockParty getEntireParty() {
		return new HellblockParty(this);
	}

	public @Nullable Map<UUID, Long> getInvitations() {
		return this.invitations;
	}

	public boolean hasInvite(@NonNull UUID playerID) {
		boolean inviteExists = false;
		if (!this.invitations.isEmpty()) {
			for (Entry<UUID, Long> invites : this.invitations.entrySet()) {
				if (invites.getKey().equals(playerID)) {
					inviteExists = true;
					break;
				}
			}
		}
		return inviteExists;
	}

	public boolean hasInviteExpired(@NonNull UUID playerID) {
		boolean expired = false;
		if (!this.invitations.isEmpty()) {
			for (Entry<UUID, Long> invites : this.invitations.entrySet()) {
				if (invites.getKey().equals(playerID)) {
					if (invites.getValue().longValue() == 0) {
						expired = true;
						break;
					}
				}
			}
		}
		return expired;
	}

	public @Nullable Map<FlagType, AccessType> getProtectionFlags() {
		return this.flags;
	}

	public AccessType getProtectionValue(@NonNull FlagType flag) {
		AccessType returnValue = flag.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
		if (!this.flags.isEmpty()) {
			for (Entry<FlagType, AccessType> flags : this.flags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getName())) {
					returnValue = flags.getValue();
					break;
				}
			}
		}
		return returnValue;
	}

	public @Nullable Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> getChallenges() {
		return this.challenges;
	}

	public int getChallengeProgress(@NonNull ChallengeType challenge) {
		int progress = 0;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS) {
						progress = challenges.getValue().getValue().getProgress();
						break;
					}
				}
			}
		}
		return progress;
	}

	public boolean isChallengeActive(@NonNull ChallengeType challenge) {
		boolean active = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					active = challenges.getValue().getKey() == CompletionStatus.IN_PROGRESS;
					break;
				}
			}
		}
		return active;
	}

	public boolean isChallengeCompleted(@NonNull ChallengeType challenge) {
		boolean completed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					completed = challenges.getValue().getValue().getProgress() >= challenge.getNeededAmount();
					break;
				}
			}
		}
		return completed;
	}

	public boolean isChallengeRewardClaimed(@NonNull ChallengeType challenge) {
		boolean claimed = false;
		if (!this.challenges.isEmpty()) {
			for (Entry<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges : this.challenges.entrySet()) {
				if (challenges.getKey().getName().equalsIgnoreCase(challenge.getName())) {
					if (challenges.getValue().getKey() == CompletionStatus.COMPLETED) {
						if (challenges.getValue().getValue().getProgress() == challenge.getNeededAmount()) {
							claimed = challenges.getValue().getValue().isRewardClaimed();
							break;
						}
					}
				}
			}
		}
		return claimed;
	}

	public void setDefaultHellblockData(boolean hasHellblock, @Nullable Location hellblockLocation, int hellblockID) {
		this.hasHellblock = hasHellblock;
		this.location = hellblockLocation;
		this.id = hellblockID;
		this.level = HellblockData.DEFAULT_LEVEL;
		this.biome = HellBiome.NETHER_WASTES;
	}

	public void transferHellblockData(@NonNull OnlineUser transferee) {
		this.id = transferee.getHellblockData().id;
		this.hasHellblock = transferee.getHellblockData().hasHellblock;
		this.location = transferee.getHellblockData().location;
		this.home = transferee.getHellblockData().home;
		this.level = transferee.getHellblockData().level;
		this.party = transferee.getHellblockData().party;
		this.trusted = transferee.getHellblockData().trusted;
		this.banned = transferee.getHellblockData().banned;
		this.flags = transferee.getHellblockData().flags;
		this.biome = transferee.getHellblockData().biome;
		this.biomeCooldown = transferee.getHellblockData().biomeCooldown;
		this.resetCooldown = transferee.getHellblockData().resetCooldown;
		this.creationTime = transferee.getHellblockData().creationTime;
		this.boundingBox = transferee.getHellblockData().boundingBox;
		this.ownerUUID = transferee.getHellblockData().ownerUUID;
		this.choice = transferee.getHellblockData().choice;
		this.schematic = transferee.getHellblockData().schematic;
		this.locked = transferee.getHellblockData().locked;
		this.visitors = transferee.getHellblockData().visitors;
	}

	public void resetHellblockData() {
		this.hasHellblock = false;
		this.location = null;
		this.home = null;
		this.level = 0.0F;
		this.party = new HashSet<>();
		this.banned = new HashSet<>();
		this.flags = new HashMap<>();
		this.invitations = new HashMap<>();
		this.biome = null;
		this.biomeCooldown = 0L;
		this.creationTime = 0L;
		this.boundingBox = null;
		this.ownerUUID = null;
		this.choice = null;
		this.schematic = null;
		this.locked = false;
		this.visitors = 0;
	}

	public void setHasHellblock(boolean hasHellblock) {
		this.hasHellblock = hasHellblock;
	}

	public void setLockedStatus(boolean locked) {
		this.locked = locked;
	}

	public void setAsAbandoned(boolean abandoned) {
		this.abandoned = abandoned;
	}

	public void setID(int id) {
		this.id = id;
	}

	public void setLevel(float level) {
		this.level = level;
	}

	public void increaseIslandLevel() {
		this.level++;
	}

	public void decreaseIslandLevel() {
		this.level--;
	}

	public void addToLevel(float levels) {
		this.level = this.level + levels;
	}

	public void removeFromLevel(float levels) {
		this.level = this.level - levels;
	}

	public void setTotalVisits(int visitors) {
		this.visitors = visitors;
	}

	public void addTotalVisit() {
		this.visitors++;
	}

	public void removeTotalVisit() {
		this.visitors--;
	}

	public void addToTotalVisits(int visits) {
		this.visitors = this.visitors + visits;
	}

	public void removeFromTotalVisits(int visits) {
		this.visitors = this.visitors - visits;
	}

	public void setBiome(@Nullable HellBiome biome) {
		this.biome = biome;
	}

	public void setIslandChoice(@Nullable IslandOptions choice) {
		this.choice = choice;
	}

	public void setUsedSchematic(@Nullable String schematic) {
		this.schematic = schematic;
	}

	public void setCreation(long creationTime) {
		this.creationTime = creationTime;
	}

	public void setResetCooldown(long resetCooldown) {
		this.resetCooldown = resetCooldown;
	}

	public void setBiomeCooldown(long biomeCooldown) {
		this.biomeCooldown = biomeCooldown;
	}

	public void setTransferCooldown(long transferCooldown) {
		this.transferCooldown = transferCooldown;
	}

	public void setOwnerUUID(@Nullable UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	public void setLinkedUUID(@Nullable UUID linkedUUID) {
		this.linkedUUID = linkedUUID;
	}

	public void setBoundingBox(@Nullable BoundingBox boundingBox) {
		this.boundingBox = boundingBox;
	}

	public void setHellblockLocation(@Nullable Location location) {
		this.location = location;
	}

	public void setHomeLocation(@Nullable Location home) {
		this.home = home;
	}

	public void addToParty(@NonNull UUID newMember) {
		if (!this.party.contains(newMember))
			this.party.add(newMember);
	}

	public void kickFromParty(@NonNull UUID oldMember) {
		if (this.party.contains(oldMember))
			this.party.remove(oldMember);
	}

	public void setParty(@Nullable Set<UUID> partyMembers) {
		this.party = partyMembers;
	}

	public void addTrustPermission(@NonNull UUID newTrustee) {
		if (!this.trusted.contains(newTrustee))
			this.trusted.add(newTrustee);
	}

	public void removeTrustPermission(@NonNull UUID oldTrustee) {
		if (this.trusted.contains(oldTrustee))
			this.trusted.remove(oldTrustee);
	}

	public void setTrusted(@Nullable Set<UUID> trustedMembers) {
		this.trusted = trustedMembers;
	}

	public void banPlayer(@NonNull UUID bannedPlayer) {
		if (!this.banned.contains(bannedPlayer))
			this.banned.add(bannedPlayer);
	}

	public void unbanPlayer(@NonNull UUID unbannedPlayer) {
		if (this.banned.contains(unbannedPlayer))
			this.banned.remove(unbannedPlayer);
	}

	public void setBanned(@Nullable Set<UUID> bannedPlayers) {
		this.banned = bannedPlayers;
	}

	public void setInvitations(@Nullable Map<UUID, Long> invitations) {
		this.invitations = invitations;
	}

	public void sendInvitation(@NonNull UUID playerID) {
		this.invitations.putIfAbsent(playerID, 86400L);
	}

	public void removeInvitation(@NonNull UUID playerID) {
		if (this.invitations.containsKey(playerID)) {
			this.invitations.remove(playerID);
		}
	}

	public void clearInvitations() {
		this.invitations.clear();
	}

	public void setProtectionFlags(@Nullable Map<FlagType, AccessType> flags) {
		this.flags = flags;
	}

	public void setProtectionValue(@NonNull HellblockFlag flag) {
		if (!this.flags.isEmpty()) {
			for (Iterator<Entry<FlagType, AccessType>> iterator = this.flags.entrySet().iterator(); iterator
					.hasNext();) {
				Entry<FlagType, AccessType> flags = iterator.next();
				if (flags.getKey().getName().equalsIgnoreCase(flag.getFlag().getName())) {
					iterator.remove();
				}
			}
		}

		AccessType returnValue = flag.getFlag().getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
		if (flag.getStatus() != returnValue) {
			this.flags.put(flag.getFlag(), flag.getStatus());
		}
	}

	public void setChallenges(@Nullable Map<ChallengeType, Entry<CompletionStatus, ChallengeData>> challenges) {
		this.challenges = challenges;
	}

	public void setChallengeRewardAsClaimed(@NonNull ChallengeType challenge, boolean claimedReward) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.COMPLETED) {
			this.challenges.get(challenge).setValue(new ChallengeData(challenge.getNeededAmount(), true));
		}
	}

	public void beginChallengeProgression(@NonNull Player player, @NonNull ChallengeType challenge) {
		HellblockChallenge newChallenge = new HellblockChallenge(challenge, CompletionStatus.IN_PROGRESS, 1);
		this.challenges.putIfAbsent(newChallenge.getChallengeType(), new SimpleEntry<CompletionStatus, ChallengeData>(
				newChallenge.getCompletionStatus(), new ChallengeData(newChallenge.getProgress(), false)));
		HellblockPlugin.getInstance().getAdventureManager().sendActionbar(player,
				String.format("<yellow>Progress <gold>(%s/%s)<gray>: %s",
						this.challenges.get(challenge).getValue().getProgress(), challenge.getNeededAmount(),
						ProgressBar.getProgressBar(new ProgressBar(challenge.getNeededAmount(),
								this.challenges.get(challenge).getValue().getProgress()), 25)));
	}

	public void updateChallengeProgression(@NonNull Player player, @NonNull ChallengeType challenge,
			int progressToAdd) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.IN_PROGRESS) {
			this.challenges.get(challenge).setValue(new ChallengeData(
					(this.challenges.get(challenge).getValue().getProgress() + progressToAdd), false));
			HellblockPlugin.getInstance().getAdventureManager()
					.sendActionbar(
							player, String
									.format("<yellow>Progress <gold>(%s/%s)<gray>: %s",
											this.challenges.get(challenge).getValue().getProgress(),
											challenge.getNeededAmount(),
											ProgressBar.getProgressBar(
													new ProgressBar(challenge.getNeededAmount(),
															this.challenges.get(challenge).getValue().getProgress()),
													25)));
		}
	}

	public void completeChallenge(@NonNull Player player, @NonNull ChallengeType challenge) {
		if (this.challenges.containsKey(challenge)
				&& this.challenges.get(challenge).getKey() == CompletionStatus.IN_PROGRESS) {
			this.challenges.remove(challenge);
			HellblockChallenge completedChallenge = new HellblockChallenge(challenge, CompletionStatus.COMPLETED,
					challenge.getNeededAmount());
			this.challenges.putIfAbsent(completedChallenge.getChallengeType(),
					new SimpleEntry<CompletionStatus, ChallengeData>(completedChallenge.getCompletionStatus(),
							new ChallengeData(challenge.getNeededAmount(), false)));
			HellblockPlugin.getInstance().getChallengeRewardBuilder().performChallengeCompletionActions(player,
					challenge);
		}
	}

	public static @NonNull HellblockData empty() {
		return new HellblockData(0, 0.0F, false, null, null, null, new HashSet<>(), new HashSet<>(), new HashSet<>(),
				new HashMap<>(), new HashMap<>(), new HashMap<>(), null, null, 0L, 0, null, null, null, false, false,
				0L, 0L, 0L);
	}
}
