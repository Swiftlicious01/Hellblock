package com.swiftlicious.hellblock.playerdata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.LogUtils;

public class HellblockPlayer {

	private UUID id;
	private boolean hasHellblock;
	private UUID hellblockOwner;
	private List<UUID> hellblockParty;
	private Location hellblockLocation;
	private Location homeLocation;
	private boolean wearingGlowstoneArmor, holdingGlowstoneTool;
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
				LogUtils.severe("Could not create hellblock player file for " + this.getPlayer().getName() + "!", var2);
				return;
			}
		}

		this.pi = YamlConfiguration.loadConfiguration(this.file);
		this.hasHellblock = this.getHellblockPlayer().getBoolean("player.hasHellblock");
		if (this.hasHellblock) {
			this.hellblockLocation = this.deserializeLocation("player.hellblock");
			this.homeLocation = this.deserializeLocation("player.home");
			try {
				this.hellblockOwner = UUID.fromString(this.getHellblockPlayer().getString("player.owner"));
			} catch (IllegalArgumentException ex) {
				this.hellblockOwner = null;
				LogUtils.severe("Could not find the UUID from the hellblock owner: "
						+ this.getHellblockPlayer().getString("player.owner"), ex);
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
			this.hellblockLocation = null;
			this.homeLocation = null;
			this.hellblockOwner = null;
			this.hellblockParty = new ArrayList<>();
		}

	}

	public Player getPlayer() {
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
			this.getHellblockPlayer().set("player.owner", this.hellblockOwner);
			if (!this.hellblockParty.isEmpty())
				this.getHellblockPlayer().set("player.party", this.hellblockParty);
		}

		try {
			this.pi.save(this.file);
		} catch (IOException var2) {
			LogUtils.severe("Unable to save player file for " + this.getPlayer().getName() + "!", var2);
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
