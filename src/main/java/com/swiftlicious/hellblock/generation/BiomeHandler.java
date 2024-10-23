package com.swiftlicious.hellblock.generation;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class BiomeHandler {

	private final HellblockPlugin instance;

	public BiomeHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	public @NonNull HellBiome convertBiomeToHellBiome(@NonNull Biome biome) {
		HellBiome hellBiome = HellBiome.NETHER_WASTES;
		switch (biome) {
		case Biome.SOUL_SAND_VALLEY:
			hellBiome = HellBiome.SOUL_SAND_VALLEY;
			break;
		case Biome.NETHER_WASTES:
			hellBiome = HellBiome.NETHER_WASTES;
			break;
		case Biome.CRIMSON_FOREST:
			hellBiome = HellBiome.CRIMSON_FOREST;
			break;
		case Biome.WARPED_FOREST:
			hellBiome = HellBiome.WARPED_FOREST;
			break;
		case Biome.BASALT_DELTAS:
			hellBiome = HellBiome.BASALT_DELTAS;
			break;
		default:
			break;
		}
		return hellBiome;
	}

	public void changeHellblockBiome(@NonNull HellblockPlayer hbPlayer, @NonNull HellBiome biome) {
		Player player = hbPlayer.getPlayer();
		if (player != null) {
			if (!hbPlayer.hasHellblock()) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't have a hellblock island! Create one with /hellblock create");
				return;
			}
			if (hbPlayer.getHellblockOwner() == null) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>An error has occurred. Please report this to the developer.");
				return;
			}
			if (hbPlayer.getHellblockOwner() != null && !hbPlayer.getHellblockOwner().equals(player.getUniqueId())) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You don't own this hellblock island!");
				return;
			}
			if (hbPlayer.getBiomeCooldown() > 0) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						String.format("<red>You have recently changed your biome already, you must wait for %s!",
								HellblockPlugin.getInstance().getFormattedCooldown(hbPlayer.getBiomeCooldown())));
				return;
			}
			if (!player.getWorld().getName().equals(instance.getHellblockHandler().getWorldName())) {
				instance.getAdventureManager().sendMessageWithPrefix(player,
						"<red>You must be on your hellblock to change the biome!");
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
				ProtectedRegion region = regions.getRegion(String.format("%sHellblock", player.getName()));
				if (region == null) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You don't have a hellblock island! Create one with /hellblock create");
					return;
				}
				DefaultDomain owners = region.getOwners();
				if (!owners.contains(player.getUniqueId())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You don't own this hellblock island!");
					return;
				}

				if (!region.contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(),
						player.getLocation().getBlockZ())) {
					instance.getAdventureManager().sendMessageWithPrefix(player,
							"<red>You must be on your hellblock to change the biome!");
					return;
				}

				List<Location> locations = instance.getWorldGuardHandler().getRegionBlocks(player);
				if (hbPlayer.getHomeLocation().getBlock().getBiome().getKey().getKey()
						.equalsIgnoreCase(biome.toString().toLowerCase())) {
					instance.getAdventureManager().sendMessageWithPrefix(player, String
							.format("<red>Your hellblock biome is already set to <dark_red>%s<red>!", biome.getName()));
					return;
				}

				locations.forEach(loc -> {
					loc.getBlock().setBiome(Biome.valueOf(biome.toString().toUpperCase()));
					instance.sendPackets(player, getChunkUnloadPacket(loc.getChunk()),
							getLightUpdateChunkPacket(loc.getChunk()));
				});

				hbPlayer.setHellblockBiome(biome);
				hbPlayer.setBiomeCooldown(Duration.ofDays(1).toHours());
				hbPlayer.saveHellblockPlayer();
				player.teleportAsync(hbPlayer.getHomeLocation());
				List<UUID> party = hbPlayer.getHellblockParty();
				if (party != null && !party.isEmpty()) {
					for (UUID id : party) {
						Player member = Bukkit.getPlayer(id);
						if (member != null && member.isOnline()) {
							HellblockPlayer hbMember = instance.getHellblockHandler().getActivePlayer(member);
							hbMember.setHellblockBiome(biome);
							hbMember.setBiomeCooldown(Duration.ofDays(1).toHours());
							hbMember.saveHellblockPlayer();
							member.teleportAsync(hbMember.getHomeLocation());
						} else {
							File memberFile = new File(
									HellblockPlugin.getInstance().getHellblockHandler().getPlayersDirectory()
											+ File.separator + id + ".yml");
							YamlConfiguration memberConfig = YamlConfiguration.loadConfiguration(memberFile);
							memberConfig.set("player.biome", biome.toString());
							memberConfig.set("player.biome-cooldown", Duration.ofDays(1).toHours());
							try {
								memberConfig.save(memberFile);
							} catch (IOException ex) {
								LogUtils.severe(String.format("Unable to save member file for %s!", id), ex);
							}
						}
					}
				}

				instance.getAdventureManager().sendMessageWithPrefix(player, String.format(
						"<red>You have changed the biome of your hellblock to <dark_red>%s<red>!", biome.getName()));
			} else {
				// TODO: using plugin protection
			}
		}
	}

	private PacketContainer getChunkUnloadPacket(Chunk chunk) {
		PacketContainer unloadPacket = new PacketContainer(PacketType.Play.Server.UNLOAD_CHUNK);
		unloadPacket.getIntegers().write(0, chunk.getX());
		unloadPacket.getIntegers().write(1, chunk.getZ());
		return unloadPacket;
	}

	private PacketContainer getLightUpdateChunkPacket(Chunk chunk) {
		PacketContainer lightUpdatePacket = new PacketContainer(PacketType.Play.Server.LIGHT_UPDATE);
		lightUpdatePacket.getIntegers().write(0, chunk.getX());
		lightUpdatePacket.getIntegers().write(1, chunk.getZ());
		return lightUpdatePacket;
	}
}
