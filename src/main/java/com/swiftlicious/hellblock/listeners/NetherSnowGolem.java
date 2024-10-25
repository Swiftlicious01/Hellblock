package com.swiftlicious.hellblock.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.NonNull;

public class NetherSnowGolem implements Listener {

	private final HellblockPlugin instance;

	public NetherSnowGolem(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public boolean checkHellGolemBuild(@NonNull Location location) {
		if (!location.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return false;

		if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.SOUL_FIRE) {
			if (location.getBlock().getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
							.getType() == Material.SOUL_SOIL) {
						return true;
					}
				}
			}
		}

		if (location.getBlock().getType() == Material.SOUL_FIRE) {
			if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
						.getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
							.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
						return true;
					}
				}
			}
		}

		if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.SOUL_FIRE) {
			if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
						return true;
					}
				}
			}
		}

		if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
				.getType() == Material.SOUL_FIRE) {
			if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP)
					.getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getType() == Material.SOUL_SOIL) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean spawnHellGolem(@NonNull Location location) {
		if (!location.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return false;

		location.getBlock().getRelative(BlockFace.UP).setType(Material.AIR);
		location.getBlock().setType(Material.AIR);
		location.getBlock().getRelative(BlockFace.DOWN).setType(Material.AIR);
		location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).setType(Material.AIR);
		location.getBlock().getRelative(BlockFace.UP).getState().update();
		location.getBlock().getState().update();
		location.getBlock().getRelative(BlockFace.DOWN).getState().update();
		location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getState().update();
		Snowman hellGolem = (Snowman) location.getBlock().getWorld().spawnEntity(location, EntityType.SNOW_GOLEM);
		hellGolem.setAware(true);
		hellGolem.setDerp(false);
		hellGolem.setVisualFire(true);
		return true;
	}

	@EventHandler
	public void onCreationOfHellGolem(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final Block block = event.getBlockPlaced();
		if (block.getType() == Material.FIRE
				&& block.getRelative(BlockFace.DOWN).getType() == Material.JACK_O_LANTERN) {
			block.setType(Material.SOUL_FIRE);
			block.getState().update();
		}

		if (checkHellGolemBuild(block.getLocation())) {
			spawnHellGolem(block.getLocation());
		}
	}

	@EventHandler
	public void onPistonPushCreationOfHellGolem(BlockPistonExtendEvent event) {
		final List<Block> blocks = event.getBlocks();
		for (final Block block : blocks) {
			if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			if (checkHellGolemBuild(block.getLocation())) {
				spawnHellGolem(block.getLocation());
			}
		}
	}

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		Entity entity = event.getEntity();
		if (!entity.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (entity instanceof Snowman && entity.isVisualFire()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (!entity.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (entity instanceof Snowman && entity.isVisualFire()) {
			if (event.getDamageSource().getDamageType() == DamageType.IN_FIRE) {
				event.setCancelled(true);
			}
		}
	}
}
