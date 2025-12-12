package com.swiftlicious.hellblock.listeners;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Fluid;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.fluid.FallingFluidData;
import com.swiftlicious.hellblock.nms.fluid.FluidData;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

/**
 * Handles the infinite lava extraction mechanic in the Hellblock world.
 * 
 * When enabled via configuration, this class allows players to collect lava
 * from falling lava blocks using an empty bucket, simulating an "infinite lava
 * source."
 * 
 * Includes listener registration, cleanup, and custom logic to simulate a
 * chance-based extraction with optional degradation of the lava source.
 * 
 * This is useful for gamifying resource collection and adding risk-reward
 * mechanics.
 */
public class InfiniteLavaHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final Set<String> LAVA_KEY = Set.of("minecraft:lava");

	public InfiniteLavaHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onInfiniteLavaFall(PlayerInteractEvent event) {
		if (!instance.getConfigManager().infiniteLavaEnabled()) {
			return;
		}

		final Player player = event.getPlayer();
		if (!instance.getHellblockHandler().isInCorrectWorld(player)) {
			return;
		}

		Block clicked = event.getClickedBlock();

		if (clicked == null) {
			return;
		}

		Location loc = clicked.getLocation();

		if (loc.getWorld() == null) {
			return;
		}

		Integer islandId = instance.getPlacementDetector().getIslandIdAt(loc);

		if (islandId == null) {
			return; // Outside of any known island
		}

		final Optional<HellblockWorld<?>> worldOpt = instance.getWorldManager()
				.getWorld(instance.getWorldManager().getHellblockWorldFormat(islandId));

		if (worldOpt.isEmpty() || worldOpt.get().bukkitWorld() == null) {
			return;
		}

		HellblockWorld<?> hellWorld = worldOpt.get();
		final Pos3 pos = Pos3.from(loc);

		final ItemStack hand = event.getItem();
		if (hand == null || hand.getType() != Material.BUCKET) {
			return;
		}

		isLavaFall(hellWorld, pos).thenAcceptAsync(lavaFallFound -> {
			final Block lavaBlock = findLavaInRange(player, 5);
			if (lavaBlock == null || !lavaFallFound) {
				return;
			}

			// CHANCE: 80% success, 20% failure
			final double chance = Math.random(); // 0.0 - 1.0
			final boolean success = chance < 0.8;

			// Consume bucket and give lava
			event.setUseItemInHand(Result.ALLOW);
			if (player.getGameMode() != GameMode.CREATIVE) {
				hand.setAmount(hand.getAmount() > 1 ? hand.getAmount() - 1 : 0);
			}

			if (success) {
				// Give lava bucket
				final ItemStack lavaBucket = new ItemStack(Material.LAVA_BUCKET);
				if (player.getInventory().firstEmpty() != -1) {
					PlayerUtils.giveItem(player, lavaBucket, 1);
				} else {
					PlayerUtils.dropItem(player, lavaBucket, false, true, false);
				}

				// Swing + sound + particle
				if (event.getHand() != null) {
					VersionHelper.getNMSManager().swingHand(player,
							event.getHand() == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
				}
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
						Sound.sound(Key.key("minecraft:item.bucket.fill_lava"), Source.PLAYER, 1, 1));
				player.getWorld().spawnParticle(Particle.LAVA, lavaBlock.getLocation().clone().add(0.5, 1, 0.5), 10,
						0.3, 0.2, 0.3);
			} else {
				// remove the lava block to simulate "drying up"
				lavaBlock.setType(Material.OBSIDIAN);
				instance.getScheduler().sync().runLater(() -> lavaBlock.setType(Material.AIR), 2L,
						lavaBlock.getLocation());
				// Feedback: Sound + Particle
				Sound extinguish = Sound.sound(Key.key("minecraft:block.lava.extinguish"), Source.PLAYER, 1, 0.8f);
				Sound glassBreak = Sound.sound(Key.key("minecraft:block.glass.break"), Source.PLAYER, 1, 1.5f);
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player), extinguish);
				AdventureHelper.playSound(instance.getSenderFactory().getAudience(player), glassBreak);
				player.spawnParticle(Particle.FLAME, lavaBlock.getLocation().clone().add(0.5, 1, 0.5), 10, 0.2, 0.3,
						0.2);
				player.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, lavaBlock.getLocation().clone().add(0.5, 1.2, 0.5),
						5, 0.1, 0.1, 0.1);
			}
		}, instance.getScheduler()::executeSync);
	}

	/**
	 * Searches for the nearest lava block within a specified range from the
	 * player's eye location.
	 * 
	 * Only stationary or flowing lava is considered, and the search stops at the
	 * first matching block.
	 *
	 * @param player the player initiating the search
	 * @param range  the maximum number of blocks to search ahead
	 * @return the first lava block found within range, or null if none found
	 */
	@Nullable
	private Block findLavaInRange(@NotNull Player player, int range) {
		final BlockIterator iter = new BlockIterator(player, range);
		while (iter.hasNext()) {
			final Block block = iter.next();
			if (block.getType() == Material.LAVA) {
				return block;
			}
		}
		return null;
	}

	/**
	 * Determines whether the given block represents a falling lava source.
	 *
	 * This method uses version-specific fluid data (via NMS helper) to check: - If
	 * the block contains lava or flowing lava. - If the fluid is currently in a
	 * "falling" state (i.e., not source/still).
	 *
	 * Useful for differentiating between stationary lava and actual falling lava,
	 * allowing conditional logic for infinite lava extraction mechanics.
	 *
	 * @param block the block to inspect
	 * @return true if the block is falling lava, false otherwise
	 */
	private CompletableFuture<Boolean> isLavaFall(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty() || !LAVA_KEY.contains(stateOpt.get().type().type().value()))
				return false;

			Location loc = pos.toLocation(world.bukkitWorld());
			FluidData fluidData = VersionHelper.getNMSManager().getFluidData(loc);
			org.bukkit.Fluid type = fluidData.getFluidType();

			return (type == Fluid.LAVA || type == Fluid.FLOWING_LAVA) && (fluidData instanceof FallingFluidData falling)
					&& falling.isFalling();
		});
	}
}