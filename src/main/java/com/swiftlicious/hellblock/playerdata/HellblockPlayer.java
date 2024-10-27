package com.swiftlicious.hellblock.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.generation.HellBiome;
import com.swiftlicious.hellblock.generation.IslandOptions;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.LogUtils;

public class HellblockPlayer {

	private UUID id;
	private int hellblockID;
	private float hellblockLevel;
	private boolean hasHellblock;
	private UUID hellblockOwner;
	private Set<UUID> hellblockParty;
	private Set<UUID> whoHasTrusted;
	private Set<UUID> bannedPlayers;
	private Map<FlagType, AccessType> protectionFlags;
	private Location hellblockLocation;
	private Location homeLocation;
	private long creationTime;
	private int totalVisitors;
	private HellBiome hellblockBiome;
	private IslandOptions islandChoice;
	private String schematic;
	private boolean lockedStatus;
	private boolean isAbandoned;
	private boolean wearingGlowstoneArmor, holdingGlowstoneTool;
	private long resetCooldown, biomeCooldown;
	private File file;
	private YamlConfiguration pi;

	public final static float DEFAULT_LEVEL = 1.0F;

	public HellblockPlayer(UUID id) {
		this.id = id;
		this.loadHellblockPlayer();
	}

	public void loadHellblockPlayer() {
		this.file = new File(HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory() + File.separator
				+ this.id + ".yml");
		if (!this.file.exists()) {
			try {
				this.file.createNewFile();
			} catch (IOException var2) {
				LogUtils.severe(
						String.format("Could not create hellblock player file for %s!", this.getPlayer().getName()),
						var2);
				return;
			}
		}

		this.pi = YamlConfiguration.loadConfiguration(this.file);
		this.hasHellblock = this.getHellblockPlayer().getBoolean("player.hasHellblock");
		this.isAbandoned = this.getHellblockPlayer().getBoolean("player.abandoned", false);
		if (this.getHellblockPlayer().contains("player.trusted-on-islands")
				&& !this.getHellblockPlayer().getStringList("player.trusted-on-islands").isEmpty()) {
			for (String trusted : this.getHellblockPlayer().getStringList("player.trusted-on-islands")) {
				this.whoHasTrusted = new HashSet<>();
				UUID uuid = null;
				try {
					uuid = UUID.fromString(trusted);
				} catch (IllegalArgumentException ignored) {
					// ignored
				}
				if (uuid != null)
					this.whoHasTrusted.add(uuid);
			}
		} else {
			this.whoHasTrusted = new HashSet<>();
		}
		if (this.hasHellblock) {
			this.islandChoice = IslandOptions
					.valueOf(this.getHellblockPlayer().get("player.island-choice.type").toString().toUpperCase());
			this.hellblockID = this.getHellblockPlayer().getInt("player.hellblock-id");
			this.hellblockLevel = (float) this.getHellblockPlayer().getDouble("player.hellblock-level", DEFAULT_LEVEL);
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.schematic = this.getHellblockPlayer().getString("player.island-choice.used-schematic");
			}
			this.lockedStatus = this.getHellblockPlayer().getBoolean("player.locked-island", false);
			this.hellblockLocation = this.deserializeLocation("player.hellblock");
			this.homeLocation = this.deserializeLocation("player.home");
			this.creationTime = this.getHellblockPlayer().getLong("player.creation-time");
			this.resetCooldown = this.getHellblockPlayer().getLong("player.reset-cooldown", 0L);
			this.biomeCooldown = this.getHellblockPlayer().getLong("player.biome-cooldown", 0L);
			this.totalVisitors = this.getHellblockPlayer().getInt("player.total-visits", 0);
			this.hellblockBiome = HellblockPlugin.getInstance().getBiomeHandler().convertBiomeToHellBiome(
					Biome.valueOf(this.getHellblockPlayer().getString("player.biome", "NETHER_WASTES")));
			try {
				this.hellblockOwner = UUID.fromString(this.getHellblockPlayer().getString("player.owner"));
			} catch (IllegalArgumentException ex) {
				this.hellblockOwner = this.id;
				LogUtils.severe(String.format("Could not find the UUID from the hellblock owner: %s",
						this.getHellblockPlayer().getString("player.owner")), ex);
			}
			if (this.getHellblockPlayer().contains("player.party")
					&& !this.getHellblockPlayer().getStringList("player.party").isEmpty()) {
				for (String member : this.getHellblockPlayer().getStringList("player.party")) {
					this.hellblockParty = new HashSet<>();
					UUID uuid = null;
					try {
						uuid = UUID.fromString(member);
					} catch (IllegalArgumentException ignored) {
						// ignored
					}
					if (uuid != null)
						this.hellblockParty.add(uuid);
				}
			} else {
				this.hellblockParty = new HashSet<>();
			}
			if (this.getHellblockPlayer().contains("player.banned-from-island")
					&& !this.getHellblockPlayer().getStringList("player.banned-from-island").isEmpty()) {
				for (String banned : this.getHellblockPlayer().getStringList("player.banned-from-island")) {
					this.bannedPlayers = new HashSet<>();
					UUID uuid = null;
					try {
						uuid = UUID.fromString(banned);
					} catch (IllegalArgumentException ignored) {
						// ignored
					}
					if (uuid != null)
						this.bannedPlayers.add(uuid);
				}
			} else {
				this.bannedPlayers = new HashSet<>();
			}
			if (this.getHellblockPlayer().contains("player.protection-flags")) {
				this.protectionFlags = new HashMap<>();
				this.getHellblockPlayer().getConfigurationSection("player.protection-flags").getKeys(false)
						.forEach(key -> {
							FlagType flag = FlagType.valueOf(key);
							AccessType status = AccessType
									.valueOf(this.getHellblockPlayer().getString("player.protection-flags." + key));
							this.protectionFlags.put(flag, status);
						});
			} else {
				this.protectionFlags = new HashMap<>();
			}
		} else {
			this.hellblockID = 0;
			this.hellblockLevel = 0.0F;
			this.hellblockBiome = null;
			this.hellblockLocation = null;
			this.resetCooldown = 0L;
			this.biomeCooldown = 0L;
			this.creationTime = 0L;
			this.totalVisitors = 0;
			this.homeLocation = null;
			this.hellblockOwner = null;
			this.protectionFlags = new HashMap<>();
			this.bannedPlayers = new HashSet<>();
			this.hellblockParty = new HashSet<>();
		}
	}

	public @Nullable Player getPlayer() {
		return Bukkit.getPlayer(this.id);
	}

	public boolean hasHellblock() {
		return this.hasHellblock;
	}

	public int getID() {
		return this.hellblockID;
	}

	public Location getHellblockLocation() {
		return this.hellblockLocation;
	}

	public UUID getHellblockOwner() {
		return this.hellblockOwner;
	}

	public Set<UUID> getHellblockParty() {
		return this.hellblockParty;
	}

	public Set<UUID> getWhoTrusted() {
		return this.whoHasTrusted;
	}

	public Set<UUID> getBannedPlayers() {
		return this.bannedPlayers;
	}

	public HellBiome getHellblockBiome() {
		return this.hellblockBiome;
	}

	public long getResetCooldown() {
		return this.resetCooldown;
	}

	public long getBiomeCooldown() {
		return this.biomeCooldown;
	}

	public IslandOptions getIslandChoice() {
		return this.islandChoice;
	}

	public String getUsedSchematic() {
		return this.schematic;
	}

	public boolean getLockedStatus() {
		return this.lockedStatus;
	}

	public int getTotalVisitors() {
		return this.totalVisitors;
	}

	public float getLevel() {
		return this.hellblockLevel;
	}

	public long getCreation() {
		return this.creationTime;
	}

	public String getCreationTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("PST"));
		cal.setTimeInMillis(this.creationTime);
		return (cal.get(Calendar.MONTH) + "/" + (cal.get(Calendar.DAY_OF_MONTH) + 1) + "/" + cal.get(Calendar.YEAR)
				+ " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND)
				+ cal.get(Calendar.AM_PM));
	}

	public boolean isAbandoned() {
		return this.isAbandoned;
	}

	public Map<FlagType, AccessType> getProtectionFlags() {
		return this.protectionFlags;
	}

	public AccessType getProtectionValue(FlagType flag) {
		AccessType returnValue = AccessType.DENY;
		if (!this.protectionFlags.isEmpty()) {
			for (Entry<FlagType, AccessType> flags : this.protectionFlags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getName())) {
					returnValue = flags.getValue();
					break;
				}
			}
		}
		return returnValue;
	}

	public void setHellblock(boolean hasHellblock, Location hellblockLocation, int id) {
		this.hasHellblock = hasHellblock;
		this.hellblockLocation = hellblockLocation;
		this.hellblockID = id;
		this.hellblockLevel = DEFAULT_LEVEL;
	}

	public Location getHomeLocation() {
		return this.homeLocation;
	}

	public void setHome(Location homeLocation) {
		this.homeLocation = homeLocation;
	}

	public void setHellblockOwner(UUID newOwner) {
		this.hellblockOwner = newOwner;
	}

	public void addToHellblockParty(UUID newMember) {
		if (!this.hellblockParty.contains(newMember))
			this.hellblockParty.add(newMember);
	}

	public void kickFromHellblockParty(UUID oldMember) {
		if (this.hellblockParty.contains(oldMember))
			this.hellblockParty.remove(oldMember);
	}

	public void setHellblockParty(Set<UUID> partyMembers) {
		this.hellblockParty = partyMembers;
	}

	public void addTrustPermission(UUID newMember) {
		if (!this.whoHasTrusted.contains(newMember))
			this.whoHasTrusted.add(newMember);
	}

	public void removeTrustPermission(UUID oldMember) {
		if (this.whoHasTrusted.contains(oldMember))
			this.whoHasTrusted.remove(oldMember);
	}

	public void setWhoTrusted(Set<UUID> trustedMembers) {
		this.whoHasTrusted = trustedMembers;
	}

	public void banPlayer(UUID newMember) {
		if (!this.bannedPlayers.contains(newMember))
			this.bannedPlayers.add(newMember);
	}

	public void unbanPlayer(UUID oldMember) {
		if (this.bannedPlayers.contains(oldMember))
			this.bannedPlayers.remove(oldMember);
	}

	public void setBannedPlayers(Set<UUID> trustedMembers) {
		this.bannedPlayers = trustedMembers;
	}

	public void setHellblockBiome(HellBiome biome) {
		this.hellblockBiome = biome;
	}

	public void setResetCooldown(long cooldown) {
		this.resetCooldown = cooldown;
	}

	public void setBiomeCooldown(long cooldown) {
		this.biomeCooldown = cooldown;
	}

	public void setIslandChoice(IslandOptions choice) {
		this.islandChoice = choice;
	}

	public void setUsedSchematic(String schematic) {
		this.schematic = schematic;
	}

	public void setLockedStatus(boolean locked) {
		this.lockedStatus = locked;
	}

	public void setCreationTime(long creation) {
		this.creationTime = creation;
	}

	public void addTotalVisit() {
		this.totalVisitors++;
	}

	public void setTotalVisits(int visits) {
		this.totalVisitors = visits;
	}

	public void increaseIslandLevel() {
		this.hellblockLevel++;
	}

	public void decreaseIslandLevel() {
		this.hellblockLevel--;
	}

	public void addToLevel(float levels) {
		this.hellblockLevel = this.hellblockLevel + levels;
	}

	public void removeFromLevel(float levels) {
		this.hellblockLevel = this.hellblockLevel - levels;
	}

	public void setLevel(float level) {
		this.hellblockLevel = level;
	}

	public void setProtectionFlags(Map<FlagType, AccessType> flags) {
		this.protectionFlags = flags;
	}

	public void setProtectionValue(HellblockFlag flag) {
		if (!this.protectionFlags.isEmpty()) {
			for (Entry<FlagType, AccessType> flags : this.protectionFlags.entrySet()) {
				if (flags.getKey().getName().equalsIgnoreCase(flag.getFlag().getName())) {
					this.protectionFlags.remove(flag.getFlag());
				}
			}
		}

		if (flag.getStatus() == AccessType.ALLOW) {
			this.protectionFlags.put(flag.getFlag(), flag.getStatus());
		}
	}

	public boolean hasGlowstoneArmorEffect() {
		return this.wearingGlowstoneArmor;
	}

	public boolean hasGlowstoneToolEffect() {
		return this.holdingGlowstoneTool;
	}

	public void isWearingGlowstoneArmor(boolean wearingGlowstoneArmor) {
		this.wearingGlowstoneArmor = wearingGlowstoneArmor;
	}

	public void isHoldingGlowstoneTool(boolean holdingGlowstoneTool) {
		this.holdingGlowstoneTool = holdingGlowstoneTool;
	}

	public void reloadHellblockPlayer() {
		this.file = new File(HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory() + File.separator
				+ this.id + ".yml");
		this.pi = YamlConfiguration.loadConfiguration(this.file);
	}

	public void saveHellblockPlayer() {
		this.getHellblockPlayer().set("player.hasHellblock", this.hasHellblock);
		if (!this.whoHasTrusted.isEmpty()) {
			Set<String> trustedString = this.whoHasTrusted.stream().filter(Objects::nonNull).map(UUID::toString)
					.collect(Collectors.toSet());
			if (!trustedString.isEmpty()) {
				this.getHellblockPlayer().set("player.trusted-on-islands", trustedString);
			}
		}
		if (this.hasHellblock()) {
			if (this.hellblockID > 0) {
				this.getHellblockPlayer().set("player.hellblock-id", this.hellblockID);
			}
			if (this.hellblockLevel > 1.0F) {
				this.getHellblockPlayer().set("player.hellblock-level", this.hellblockLevel);
			}
			this.serializeLocation("player.hellblock", this.hellblockLocation);
			this.serializeLocation("player.home", this.homeLocation);
			this.getHellblockPlayer().set("player.creation-time", this.creationTime);
			this.getHellblockPlayer().set("player.island-choice.type", this.islandChoice.toString());
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.getHellblockPlayer().set("player.island-choice.used-schematic", this.schematic);
			}
			if (this.lockedStatus) {
				this.getHellblockPlayer().set("player.locked-island", this.lockedStatus);
			}
			if (this.totalVisitors > 0) {
				this.getHellblockPlayer().set("player.total-visits", this.totalVisitors);
			}
			this.getHellblockPlayer().set("player.owner", this.hellblockOwner.toString());
			if (this.resetCooldown > 0) {
				this.getHellblockPlayer().set("player.reset-cooldown", this.resetCooldown);
			}
			if (this.biomeCooldown > 0) {
				this.getHellblockPlayer().set("player.biome-cooldown", this.biomeCooldown);
			}
			if (this.hellblockBiome != HellBiome.NETHER_WASTES) {
				this.getHellblockPlayer().set("player.biome", this.hellblockBiome.toString());
			}
			if (!this.hellblockParty.isEmpty()) {
				Set<String> partyString = this.hellblockParty.stream().filter(Objects::nonNull).map(UUID::toString)
						.collect(Collectors.toSet());
				if (!partyString.isEmpty()) {
					this.getHellblockPlayer().set("player.party", partyString);
				}
			}
			if (!this.bannedPlayers.isEmpty()) {
				Set<String> bannedString = this.bannedPlayers.stream().filter(Objects::nonNull).map(UUID::toString)
						.collect(Collectors.toSet());
				if (!bannedString.isEmpty()) {
					this.getHellblockPlayer().set("player.banned-from-island", bannedString);
				}
			}
			if (!this.protectionFlags.isEmpty()) {
				this.getHellblockPlayer().set("player.protection-flags", null);
				for (Map.Entry<FlagType, AccessType> flags : this.protectionFlags.entrySet()) {
					if (flags.getValue() == AccessType.DENY)
						continue;
					this.getHellblockPlayer().set("player.protection-flags." + flags.getKey().toString(),
							flags.getValue().toString());
				}
			}
		}

		try {
			this.pi.save(this.file);
		} catch (IOException var2) {
			LogUtils.severe(String.format("Unable to save player file for %s!", this.getPlayer().getName()), var2);
		}
	}

	public YamlConfiguration getHellblockPlayer() {
		if (this.pi == null) {
			this.reloadHellblockPlayer();
		}

		return this.pi;
	}

	public void serializeLocation(String path, Location location) {
		String world = HellblockPlugin.getInstance().getHellblockHandler().getWorldName();
		double x = 0.0D;
		double y = (double) HellblockPlugin.getInstance().getHellblockHandler().getHeight();
		double z = 0.0D;
		float yaw = 0.0F;
		if (location != null) {
			world = location.getWorld().getName();
			x = location.getX();
			y = location.getY();
			z = location.getZ();
			yaw = location.getYaw();
		}

		this.getHellblockPlayer().set(path + ".world", world);
		this.getHellblockPlayer().set(path + ".x", x);
		this.getHellblockPlayer().set(path + ".y", y);
		this.getHellblockPlayer().set(path + ".z", z);
		this.getHellblockPlayer().set(path + ".yaw", yaw);
	}

	public Location deserializeLocation(String path) {
		World world = Bukkit.getWorld(this.getHellblockPlayer().getString(path + ".world"));
		double x = this.getHellblockPlayer().getDouble(path + ".x");
		double y = this.getHellblockPlayer().getDouble(path + ".y");
		double z = this.getHellblockPlayer().getDouble(path + ".z");
		float yaw = (float) this.getHellblockPlayer().getDouble(path + ".yaw");
		return new Location(world, x, y, z, yaw, 0);
	}
}
