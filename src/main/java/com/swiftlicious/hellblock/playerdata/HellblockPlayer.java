package com.swiftlicious.hellblock.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
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
import com.swiftlicious.hellblock.utils.LogUtils;

public class HellblockPlayer {

	private UUID id;
	private int hellblockID;
	private boolean hasHellblock;
	private UUID hellblockOwner;
	private List<UUID> hellblockParty;
	private List<UUID> whoHasTrusted;
	private List<UUID> bannedPlayers;
	private Set<String> protectionFlags;
	private Location hellblockLocation;
	private Location homeLocation;
	private long creationTime;
	private int totalVisitors;
	private HellBiome hellblockBiome;
	private IslandOptions islandChoice;
	private String schematic;
	private boolean lockedStatus;
	private boolean wearingGlowstoneArmor, holdingGlowstoneTool;
	private long resetCooldown, biomeCooldown;
	private File file;
	private YamlConfiguration pi;

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
		if (this.getHellblockPlayer().contains("player.trusted-on-islands")
				&& !this.getHellblockPlayer().getStringList("player.trusted-on-islands").isEmpty()) {
			for (String trusted : this.getHellblockPlayer().getStringList("player.trusted-on-islands")) {
				this.whoHasTrusted = new ArrayList<>();
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
			this.whoHasTrusted = new ArrayList<>();
		}
		if (this.hasHellblock) {
			this.islandChoice = IslandOptions
					.valueOf(this.getHellblockPlayer().get("player.island-choice.type").toString().toUpperCase());
			this.hellblockID = this.getHellblockPlayer().getInt("player.hellblock-id");
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.schematic = this.getHellblockPlayer().getString("player.island-choice.used-schematic");
			}
			this.lockedStatus = this.getHellblockPlayer().getBoolean("player.locked-island", false);
			this.hellblockLocation = this.deserializeLocation("player.hellblock");
			this.homeLocation = this.deserializeLocation("player.home");
			this.creationTime = this.getHellblockPlayer().getLong("player.creation-time", 0L);
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
					this.hellblockParty = new ArrayList<>();
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
				this.hellblockParty = new ArrayList<>();
			}
			if (this.getHellblockPlayer().contains("player.banned-from-island")
					&& !this.getHellblockPlayer().getStringList("player.banned-from-island").isEmpty()) {
				for (String banned : this.getHellblockPlayer().getStringList("player.banned-from-island")) {
					this.bannedPlayers = new ArrayList<>();
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
				this.bannedPlayers = new ArrayList<>();
			}
			if (this.getHellblockPlayer().contains("player.protection-flags")
					&& !this.getHellblockPlayer().getStringList("player.protection-flags").isEmpty()) {
				for (String flags : this.getHellblockPlayer().getStringList("player.protection-flags")) {
					this.protectionFlags = new HashSet<>();
					this.protectionFlags.add(flags);
				}
			} else {
				this.protectionFlags = new HashSet<>();
			}
		} else {
			this.hellblockID = 0;
			this.hellblockBiome = null;
			this.hellblockLocation = null;
			this.resetCooldown = 0L;
			this.biomeCooldown = 0L;
			this.creationTime = 0L;
			this.totalVisitors = 0;
			this.homeLocation = null;
			this.hellblockOwner = null;
			this.protectionFlags = new HashSet<>();
			this.bannedPlayers = new ArrayList<>();
			this.hellblockParty = new ArrayList<>();
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

	public List<UUID> getHellblockParty() {
		return this.hellblockParty;
	}

	public List<UUID> getWhoTrusted() {
		return this.whoHasTrusted;
	}

	public List<UUID> getBannedPlayers() {
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

	public long getCreation() {
		return this.creationTime;
	}

	public String getCreationTime() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(this.creationTime);
		return (cal.get(Calendar.YEAR) + " " + (cal.get(Calendar.MONTH) + 1) + " " + cal.get(Calendar.DAY_OF_MONTH)
				+ " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
	}

	public Set<String> getProtectionFlags() {
		return this.protectionFlags;
	}

	public boolean getProtectionValue(String flag) {
		boolean returnValue = false;
		for (String flags : this.protectionFlags) {
			String[] allFlags = flags.split(":");
			String key = allFlags[0];
			if (key.equalsIgnoreCase(flag)) {
				String value = allFlags[1];
				returnValue = value.equalsIgnoreCase("ALLOW") ? true : false;
			}
		}
		return returnValue;
	}

	public void setHellblock(boolean hasHellblock, Location hellblockLocation, int id) {
		this.hasHellblock = hasHellblock;
		this.hellblockLocation = hellblockLocation;
		this.hellblockID = id;
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

	public void setHellblockParty(List<UUID> partyMembers) {
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

	public void setWhoTrusted(List<UUID> trustedMembers) {
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

	public void setBannedPlayers(List<UUID> trustedMembers) {
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

	public void setProtectionFlags(Set<String> flags) {
		this.protectionFlags = flags;
	}

	public void setProtectionValue(String flag) {
		String[] splitFlag = flag.split(":");
		String splitKey = splitFlag[0];
		for (String flags : this.protectionFlags) {
			String[] split = flags.split(":");
			String key = split[0];
			if (key.equalsIgnoreCase(splitKey)) {
				this.protectionFlags.remove(flag);
			}
		}
		this.protectionFlags.add(flag);
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
			List<String> trustedString = this.whoHasTrusted.stream().filter(Objects::nonNull)
					.map(uuid -> uuid.toString()).collect(Collectors.toList());
			this.getHellblockPlayer().set("player.trusted-on-islands", trustedString);
		}
		if (this.hasHellblock()) {
			if (this.hellblockID > 0) {
				this.getHellblockPlayer().set("player.hellblock-id", this.hellblockID);
			}
			this.serializeLocation("player.hellblock", this.hellblockLocation);
			this.serializeLocation("player.home", this.homeLocation);
			this.getHellblockPlayer().set("player.creation-time", this.creationTime);
			this.getHellblockPlayer().set("player.island-choice.type", this.islandChoice.toString());
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.getHellblockPlayer().set("player.island-choice.used-schematic", this.schematic);
			}
			this.getHellblockPlayer().set("player.locked-island", this.lockedStatus);
			this.getHellblockPlayer().set("player.total-visits", this.totalVisitors);
			this.getHellblockPlayer().set("player.owner", this.hellblockOwner.toString());
			this.getHellblockPlayer().set("player.reset-cooldown", this.resetCooldown);
			this.getHellblockPlayer().set("player.biome-cooldown", this.biomeCooldown);
			this.getHellblockPlayer().set("player.biome", this.hellblockBiome.toString());
			if (!this.hellblockParty.isEmpty()) {
				List<String> partyString = this.hellblockParty.stream().filter(Objects::nonNull)
						.map(uuid -> uuid.toString()).collect(Collectors.toList());
				this.getHellblockPlayer().set("player.party", partyString);
			}
			if (!this.bannedPlayers.isEmpty()) {
				List<String> bannedString = this.bannedPlayers.stream().filter(Objects::nonNull)
						.map(uuid -> uuid.toString()).collect(Collectors.toList());
				this.getHellblockPlayer().set("player.banned-from-island", bannedString);
			}
			if (!this.protectionFlags.isEmpty()) {
				this.getHellblockPlayer().set("player.protection-flags", this.protectionFlags);
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
