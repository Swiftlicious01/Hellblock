package com.swiftlicious.hellblock.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ChallengeType;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;

import lombok.NonNull;

public class NetherSnowGolem implements Listener {

	private final HellblockPlugin instance;

	public NetherSnowGolem(HellblockPlugin plugin) {
		instance = plugin;
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	public List<Block> checkHellGolemBuild(@NonNull Location location) {
		if (location.getWorld() == null)
			return new ArrayList<>();
		if (!location.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return new ArrayList<>();

		List<Block> blocks = new ArrayList<>();

		if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.FIRE) {
			if (location.getBlock().getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
							.getType() == Material.SOUL_SOIL) {
						blocks.addAll(List.of(location.getBlock().getRelative(BlockFace.UP),
								location.getBlock().getRelative(BlockFace.DOWN),
								location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN),
								location.getBlock()));
					}
				}
			}
		}

		if (location.getBlock().getType() == Material.FIRE) {
			if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
						.getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
							.getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
						blocks.addAll(List.of(location.getBlock().getRelative(BlockFace.DOWN),
								location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN),
								location.getBlock().getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN)
										.getRelative(BlockFace.DOWN),
								location.getBlock()));
					}
				}
			}
		}

		if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType() == Material.FIRE) {
			if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SOIL) {
						blocks.addAll(List.of(location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP),
								location.getBlock().getRelative(BlockFace.UP),
								location.getBlock().getRelative(BlockFace.DOWN), location.getBlock()));
					}
				}
			}
		}

		if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.UP)
				.getType() == Material.FIRE) {
			if (location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP)
					.getType() == Material.JACK_O_LANTERN) {
				if (location.getBlock().getRelative(BlockFace.UP).getType() == Material.SOUL_SOIL) {
					if (location.getBlock().getType() == Material.SOUL_SOIL) {
						blocks.addAll(List.of(
								location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP)
										.getRelative(BlockFace.UP),
								location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP),
								location.getBlock().getRelative(BlockFace.UP), location.getBlock()));
					}
				}
			}
		}
		return blocks;
	}

	public boolean spawnHellGolem(@NonNull Player player, @NonNull Location location) {
		if (location.getWorld() == null)
			return false;
		if (!location.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return false;

		Snowman hellGolem;
		if (!checkHellGolemBuild(location).isEmpty() && !instance.getHellblockHandler().checkIfInSpawn(location)
				&& instance.getHellblockHandler().getActivePlayer(player)
						.getProtectionValue(HellblockFlag.FlagType.MOB_SPAWNING) == AccessType.ALLOW) {
			List<Block> blocks = checkHellGolemBuild(location);
			for (Block block : blocks) {
				block.setType(Material.AIR);
			}
			hellGolem = (Snowman) location.getWorld().spawnEntity(location, EntityType.SNOW_GOLEM,
					SpawnReason.BUILD_SNOWMAN);
			hellGolem.setAware(true);
			hellGolem.setDerp(false);
			hellGolem.setVisualFire(true);
			HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(player);
			if (!pi.isChallengeActive(ChallengeType.NETHER_GOLEM_CHALLENGE)
					&& !pi.isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
				pi.beginChallengeProgression(ChallengeType.NETHER_GOLEM_CHALLENGE);
			} else {
				pi.updateChallengeProgression(ChallengeType.NETHER_GOLEM_CHALLENGE, 1);
				if (pi.isChallengeCompleted(ChallengeType.NETHER_GOLEM_CHALLENGE)) {
					pi.completeChallenge(ChallengeType.NETHER_GOLEM_CHALLENGE);
				}
			}
			return true;
		}

		return false;
	}

	@EventHandler
	public void onCreationOfHellGolem(BlockPlaceEvent event) {
		final Player player = event.getPlayer();
		if (!player.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;

		final Block block = event.getBlockPlaced();
		spawnHellGolem(player, block.getLocation());
	}

	@EventHandler
	public void onPistonPushCreationOfHellGolem(BlockPistonExtendEvent event) {
		final List<Block> blocks = event.getBlocks();
		for (final Block block : blocks) {
			if (!block.getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
				return;

			Collection<Player> playersNearby = block.getWorld().getNearbyPlayers(block.getLocation(), 25, 25,
					25);
			Player player = instance.getNetherrackGeneratorHandler().getClosestPlayer(block.getLocation(),
					playersNearby);
			if (player != null)
				spawnHellGolem(player, block.getLocation());
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
			if (event.getDamageSource().getDamageType() == DamageType.ON_FIRE) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onSnowball(EntityDamageByEntityEvent event) {
		if (!event.getEntity().getWorld().getName().equalsIgnoreCase(instance.getHellblockHandler().getWorldName()))
			return;
		if (event.getEntity() instanceof LivingEntity living) {
			if (event.getDamager() instanceof Snowball snowball) {
				if (snowball.getShooter() != null && snowball.getShooter() instanceof Snowman snowman) {
					if (snowman.isVisualFire()) {
						snowball.setVisualFire(true);
						living.setFireTicks(40);
					}
				}
			}
		}
	}
}
