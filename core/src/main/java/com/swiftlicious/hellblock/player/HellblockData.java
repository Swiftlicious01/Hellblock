package com.swiftlicious.hellblock.player;

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

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

public class HellblockData {

	@Expose
	@SerializedName("id")
	protected int id;
	@Expose
	@SerializedName("level")
	protected float level;
	@Expose
	@SerializedName("exists")
	protected boolean hasHellblock;
	@Expose
	@SerializedName("owner")
	protected UUID ownerUUID;
	@Expose
	@SerializedName("linked")
	protected UUID linkedUUID;
	@Expose
	@SerializedName("bounds")
	protected BoundingBox boundingBox;
	@Expose
	@SerializedName("party")
	protected Set<UUID> party;
	@Expose
	@SerializedName("trusted")
	protected Set<UUID> trusted;
	@Expose
	@SerializedName("banned")
	protected Set<UUID> banned;
	@Expose
	@SerializedName("invitations")
	protected Map<UUID, Long> invitations;
	@Expose
	@SerializedName("flags")
	protected Map<FlagType, AccessType> flags;
	@Expose
	@SerializedName("location")
	protected Location location;
	@Expose
	@SerializedName("home")
	protected Location home;
	@Expose
	@SerializedName("creation")
	protected long creationTime;
	@Expose
	@SerializedName("visitors")
	protected int visitors;
	@Expose
	@SerializedName("biome")
	protected HellBiome biome;
	@Expose
	@SerializedName("choice")
	protected IslandOptions choice;
	@Expose
	@SerializedName("schematic")
	protected String schematic;
	@Expose
	@SerializedName("locked")
	protected boolean locked;
	@Expose
	@SerializedName("abandoned")
	protected boolean abandoned;
	@Expose
	@SerializedName("resetcooldown")
	protected long resetCooldown;
	@Expose
	@SerializedName("biomecooldown")
	protected long biomeCooldown;
	@Expose
	@SerializedName("transfercooldown")
	protected long transferCooldown;

	public final static float DEFAULT_LEVEL = 1.0F;

	public HellblockData(int id, float level, boolean hasHellblock, UUID ownerUUID, UUID linkedUUID,
			BoundingBox boundingBox, Set<UUID> party, Set<UUID> trusted, Set<UUID> banned, Map<UUID, Long> invitations,
			Map<FlagType, AccessType> flags, Location location, Location home, long creationTime, int visitors,
			HellBiome biome, IslandOptions choice, String schematic, boolean locked, boolean abandoned,
			long resetCooldown, long biomeCooldown, long transferCooldown) {
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

	public @NotNull String getCreationTime() {
		LocalDateTime localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.creationTime),
				ZoneId.systemDefault());
		Locale locale = HellblockPlugin.getInstance().getTranslationManager().parseLocale(
				HellblockPlugin.getInstance().getConfigManager().getMainConfig().getString("force-locale", ""));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("KK:mm:ss a", locale);
		String now = localDate.format(formatter);
		return String.format("%s %s %s %s", localDate.getMonth().getDisplayName(TextStyle.FULL, locale),
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

	public @NotNull Set<UUID> getParty() {
		return this.party;
	}

	public @NotNull Set<UUID> getTrusted() {
		return this.trusted;
	}

	public @NotNull Set<UUID> getBanned() {
		return this.banned;
	}

	public @NotNull Set<UUID> getIslandMembers() {
		Set<UUID> members = new HashSet<>();
		if (this.ownerUUID != null)
			members.add(this.ownerUUID);
		if (this.party != null && !this.party.isEmpty())
			members.addAll(this.party);
		if (this.trusted != null && !this.trusted.isEmpty())
			members.addAll(this.trusted);
		return members;
	}

	public @NotNull Map<UUID, Long> getInvitations() {
		return this.invitations;
	}

	public boolean hasInvite(@NotNull UUID playerID) {
		boolean inviteExists = false;
		if (!this.invitations.isEmpty()) {
			for (Map.Entry<UUID, Long> invites : this.invitations.entrySet()) {
				if (invites.getKey().equals(playerID)) {
					inviteExists = true;
					break;
				}
			}
		}
		return inviteExists;
	}

	public boolean hasInviteExpired(@NotNull UUID playerID) {
		boolean expired = false;
		if (!this.invitations.isEmpty()) {
			for (Map.Entry<UUID, Long> invites : this.invitations.entrySet()) {
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

	public @NotNull Map<FlagType, AccessType> getProtectionFlags() {
		return this.flags;
	}

	public @NotNull AccessType getProtectionValue(@NotNull FlagType flag) {
		AccessType returnValue = flag.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY;
		if (!this.flags.isEmpty()) {
			for (Map.Entry<FlagType, AccessType> flags : this.flags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getName())) {
					returnValue = flags.getValue();
					break;
				}
			}
		}
		return returnValue;
	}

	public @Nullable String getProtectionData(@NotNull FlagType flag) {
		if (!(flag == FlagType.GREET_MESSAGE || flag == FlagType.FAREWELL_MESSAGE))
			return null;
		String data = flag.getData() != null ? flag.getData() : null;
		if (!this.flags.isEmpty()) {
			for (Map.Entry<FlagType, AccessType> flags : this.flags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getName())) {
					data = flags.getKey().getData();
					break;
				}
			}
		}
		return data;
	}

	public void setDefaultHellblockData(boolean hasHellblock, @Nullable Location hellblockLocation, int hellblockID) {
		this.hasHellblock = hasHellblock;
		this.location = hellblockLocation;
		this.id = hellblockID;
		this.level = HellblockData.DEFAULT_LEVEL;
		this.biome = HellBiome.NETHER_WASTES;
	}

	public void transferHellblockData(@NotNull UserData transferee) {
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
		this.trusted = new HashSet<>();
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

	public void addToParty(@NotNull UUID newMember) {
		if (!this.party.contains(newMember))
			this.party.add(newMember);
	}

	public void kickFromParty(@NotNull UUID oldMember) {
		if (this.party.contains(oldMember))
			this.party.remove(oldMember);
	}

	public void setParty(@Nullable Set<UUID> partyMembers) {
		this.party = partyMembers;
	}

	public void addTrustPermission(@NotNull UUID newTrustee) {
		if (!this.trusted.contains(newTrustee))
			this.trusted.add(newTrustee);
	}

	public void removeTrustPermission(@NotNull UUID oldTrustee) {
		if (this.trusted.contains(oldTrustee))
			this.trusted.remove(oldTrustee);
	}

	public void setTrusted(@Nullable Set<UUID> trustedMembers) {
		this.trusted = trustedMembers;
	}

	public void banPlayer(@NotNull UUID bannedPlayer) {
		if (!this.banned.contains(bannedPlayer))
			this.banned.add(bannedPlayer);
	}

	public void unbanPlayer(@NotNull UUID unbannedPlayer) {
		if (this.banned.contains(unbannedPlayer))
			this.banned.remove(unbannedPlayer);
	}

	public void setBanned(@Nullable Set<UUID> bannedPlayers) {
		this.banned = bannedPlayers;
	}

	public void setInvitations(@Nullable Map<UUID, Long> invitations) {
		this.invitations = invitations;
	}

	public void sendInvitation(@NotNull UUID playerID) {
		this.invitations.putIfAbsent(playerID, 86400L);
	}

	public void removeInvitation(@NotNull UUID playerID) {
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

	public void setProtectionValue(@NotNull HellblockFlag flag) {
		if (!this.flags.isEmpty()) {
			for (Iterator<Map.Entry<FlagType, AccessType>> iterator = this.flags.entrySet().iterator(); iterator
					.hasNext();) {
				Map.Entry<FlagType, AccessType> flags = iterator.next();
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

	/**
	 * Creates an instance of HellblockData with default values (empty values).
	 *
	 * @return a new instance of HellblockData with default values.
	 */
	public static @NotNull HellblockData empty() {
		return new HellblockData(0, 0.0F, false, null, null, null, new HashSet<>(), new HashSet<>(), new HashSet<>(),
				new HashMap<>(), new HashMap<>(), null, null, 0L, 0, null, null, null, false, false, 0L, 0L, 0L);
	}

	public @NotNull HellblockData copy() {
		return new HellblockData(id, level, hasHellblock, ownerUUID, linkedUUID, boundingBox, party, trusted, banned,
				invitations, flags, location, home, creationTime, visitors, biome, choice, schematic, locked, abandoned,
				resetCooldown, biomeCooldown, transferCooldown);
	}
}