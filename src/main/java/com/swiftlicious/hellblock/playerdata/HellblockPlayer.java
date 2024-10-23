package com.swiftlicious.hellblock.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
	private boolean hasHellblock;
	private UUID hellblockOwner;
	private List<UUID> hellblockParty;
	private Location hellblockLocation;
	private Location homeLocation;
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
		if (this.hasHellblock) {
			this.islandChoice = IslandOptions
					.valueOf(this.getHellblockPlayer().get("player.island-choice").toString().toUpperCase());
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.schematic = this.getHellblockPlayer().getString("player.island-choice.schematic");
			}
			this.lockedStatus = this.getHellblockPlayer().getBoolean("player.locked-island");
			this.hellblockLocation = this.deserializeLocation("player.hellblock");
			this.homeLocation = this.deserializeLocation("player.home");
			this.resetCooldown = this.getHellblockPlayer().getLong("player.reset-cooldown", 0L);
			this.biomeCooldown = this.getHellblockPlayer().getLong("player.biome-cooldown", 0L);
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
					this.hellblockParty.add(UUID.fromString(member));
				}
			} else {
				this.hellblockParty = new ArrayList<>();
			}
		} else {
			this.hellblockBiome = null;
			this.hellblockLocation = null;
			this.resetCooldown = 0L;
			this.biomeCooldown = 0L;
			this.homeLocation = null;
			this.hellblockOwner = null;
			this.hellblockParty = new ArrayList<>();
		}
	}

	public @Nullable Player getPlayer() {
		return Bukkit.getPlayer(this.id);
	}

	public boolean hasHellblock() {
		return this.hasHellblock;
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

	public void setHellblock(boolean hasHellblock, Location hellblockLocation) {
		this.hasHellblock = hasHellblock;
		this.hellblockLocation = hellblockLocation;
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
		if (this.hasHellblock()) {
			this.serializeLocation("player.hellblock", this.hellblockLocation);
			this.serializeLocation("player.home", this.homeLocation);
			this.getHellblockPlayer().set("player.island-choice", this.islandChoice.toString());
			if (this.islandChoice == IslandOptions.SCHEMATIC) {
				this.getHellblockPlayer().set("player.island-choice.schematic", this.schematic);
			}
			this.getHellblockPlayer().set("player.locked-island", this.lockedStatus);
			this.getHellblockPlayer().set("player.owner", this.hellblockOwner.toString());
			this.getHellblockPlayer().set("player.reset-cooldown", this.resetCooldown);
			this.getHellblockPlayer().set("player.biome-cooldown", this.biomeCooldown);
			this.getHellblockPlayer().set("player.biome", this.hellblockBiome.toString());
			if (!this.hellblockParty.isEmpty()) {
				List<String> partyString = this.hellblockParty.stream().filter(Objects::nonNull)
						.map(uuid -> uuid.toString()).collect(Collectors.toList());
				this.getHellblockPlayer().set("player.party", partyString);
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
		if (location != null) {
			world = location.getWorld().getName();
			x = location.getX();
			y = location.getY();
			z = location.getZ();
		}

		this.getHellblockPlayer().set(path + ".world", world);
		this.getHellblockPlayer().set(path + ".x", x);
		this.getHellblockPlayer().set(path + ".y", y);
		this.getHellblockPlayer().set(path + ".z", z);
	}

	public Location deserializeLocation(String path) {
		World world = Bukkit.getWorld(this.getHellblockPlayer().getString(path + ".world"));
		int x = (int) this.getHellblockPlayer().getDouble(path + ".x");
		int y = (int) this.getHellblockPlayer().getDouble(path + ".y");
		int z = (int) this.getHellblockPlayer().getDouble(path + ".z");
		return new Location(world, (double) x, (double) y, (double) z);
	}
}
