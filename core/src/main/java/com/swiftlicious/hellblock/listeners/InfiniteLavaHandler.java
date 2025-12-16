package com.swiftlicious.hellblock.listeners;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

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

		isLavaFall(hellWorld, pos).thenCompose(isFall -> {
			if (!isFall)
				return CompletableFuture.completedFuture(false);
			return isInfiniteLavaFormation(hellWorld, pos);
		}).thenAcceptAsync(validFormation -> {
			if (!validFormation) {
				return;
			}

			final Block lavaBlock = findLavaInRange(player, 5);
			if (lavaBlock == null) {
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

	/**
	 * Computes the fluid height of a lava block at the specified position.
	 * 
	 * This method inspects the underlying fluid data via NMS utilities to determine
	 * how "full" a given lava block is — useful for differentiating between partial
	 * flows and full source blocks.
	 * 
	 * Returns the computed height of the lava fluid as a float, or {@code null} if
	 * the block is not lava or not valid in the world context.
	 *
	 * @param world the Hellblock world instance
	 * @param pos   the block position to evaluate
	 * @return a {@link CompletableFuture} that completes with the lava height as a
	 *         float, or {@code null} if not applicable
	 */
	@Nullable
	private CompletableFuture<Float> getLavaHeight(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(null);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty() || !LAVA_KEY.contains(stateOpt.get().type().type().value()))
				return null;

			Location loc = pos.toLocation(world.bukkitWorld());
			FluidData fluidData = VersionHelper.getNMSManager().getFluidData(loc);
			org.bukkit.Fluid type = fluidData.getFluidType();

			if (type != Fluid.LAVA && type != Fluid.FLOWING_LAVA)
				return null;

			return fluidData.computeHeight(loc);
		});
	}

	/**
	 * Determines whether the given lava block is part of a valid 3D infinite-lava
	 * formation.
	 *
	 * The pattern can appear in either EAST–WEST or NORTH–SOUTH orientation and
	 * spans three Y-levels:
	 *
	 * <pre>
	 *   Y+2: W S W
	 *   Y+1: F E F
	 *   Y+0: F E F
	 * </pre>
	 *
	 * Where: - W = Flowing lava (non-source, non-falling) - S = Source lava - F =
	 * Falling lava (eligible for extraction) - E = Empty / non-lava (any non-lava
	 * block)
	 *
	 * Only the “F” blocks on layers Y+1 or Y+0 may be claimed for infinite lava,
	 * and all other parts of the structure must match exactly.
	 *
	 * @param world  the Hellblock world context
	 * @param center the block position of the potential falling lava block
	 * @return a {@link CompletableFuture} resolving to {@code true} if the
	 *         structure qualifies
	 */
	private CompletableFuture<Boolean> isInfiniteLavaFormation(@NotNull HellblockWorld<?> world, @NotNull Pos3 center) {
		// Define vertical levels relative to the tested falling lava block
		Pos3 topY = center.add(0, 2, 0);
		Pos3 midY = center.add(0, 1, 0);
		Pos3 baseY = center; // Y+0

		// Orientations to test — east/west AND north/south
		List<Function<Boolean, CompletableFuture<Boolean>>> orientationChecks = List.of(
				// EAST–WEST orientation
				(ignore) -> checkFormationDirection(world, center, topY, midY, baseY, Axis.EAST_WEST),
				// NORTH–SOUTH orientation
				(ignore) -> checkFormationDirection(world, center, topY, midY, baseY, Axis.NORTH_SOUTH));

		// Run both direction checks asynchronously and succeed if either matches
		return CompletableFuture.allOf(orientationChecks.get(0).apply(true), orientationChecks.get(1).apply(true))
				.thenCompose(v -> orientationChecks.get(0).apply(true).thenCombine(orientationChecks.get(1).apply(true),
						(eastWest, northSouth) -> eastWest || northSouth));
	}

	private enum Axis {
		EAST_WEST, NORTH_SOUTH
	}

	/**
	 * Validates a single directional infinite lava pattern along one axis.
	 *
	 * @param world  the Hellblock world
	 * @param center the central (falling lava) position
	 * @param topY   top layer Y+2
	 * @param midY   mid layer Y+1
	 * @param baseY  base layer Y+0
	 * @param axis   orientation (EAST_WEST or NORTH_SOUTH)
	 * @return a future resolving to true if this axis matches the pattern
	 */
	private CompletableFuture<Boolean> checkFormationDirection(@NotNull HellblockWorld<?> world, @NotNull Pos3 center,
			@NotNull Pos3 topY, @NotNull Pos3 midY, @NotNull Pos3 baseY, @NotNull Axis axis) {
		// Offsets for the current orientation
		int dx = axis == Axis.EAST_WEST ? 1 : 0;
		int dz = axis == Axis.NORTH_SOUTH ? 1 : 0;

		// --- Define coordinates for all positions ---
		// Top layer: W S W
		Pos3 topLeft = topY.add(-dx, 0, -dz);
		Pos3 topSource = topY;
		Pos3 topRight = topY.add(dx, 0, dz);

		// Middle layer: F E F
		Pos3 midLeft = midY.add(-dx, 0, -dz);
		Pos3 midEmpty = midY;
		Pos3 midRight = midY.add(dx, 0, dz);

		// Base layer: F E F
		Pos3 botLeft = baseY.add(-dx, 0, -dz);
		Pos3 botEmpty = baseY;
		Pos3 botRight = baseY.add(dx, 0, dz);

		// --- Async fluid checks ---
		CompletableFuture<Boolean> topLayer = instance.getNetherrackGeneratorHandler().isLavaFlowing(world, topLeft)
				.thenCombine(instance.getNetherrackGeneratorHandler().isLavaSource(world, topSource), (a, b) -> a && b)
				.thenCombine(instance.getNetherrackGeneratorHandler().isLavaFlowing(world, topRight), (a, b) -> a && b);

		CompletableFuture<Boolean> midLayer = isLavaFall(world, midLeft)
				.thenCombine(isBlockNonLava(world, midEmpty), (a, b) -> a && b)
				.thenCombine(isLavaFall(world, midRight), (a, b) -> a && b);

		CompletableFuture<Boolean> botLayer = isLavaFall(world, botLeft)
				.thenCombine(isBlockNonLava(world, botEmpty), (a, b) -> a && b)
				.thenCombine(isLavaFall(world, botRight), (a, b) -> a && b);

		CompletableFuture<Boolean> centerCheck = isLavaFall(world, center);

		// Combine all asynchronously
		return topLayer.thenCombine(midLayer, (a, b) -> a && b).thenCombine(botLayer, (a, b) -> a && b)
				.thenCombine(centerCheck, (a, b) -> a && b);
	}

	/**
	 * Checks that the block at the given position is *not* any type of lava.
	 * 
	 * This is used for validating the “E” (empty) slots in the formation pattern,
	 * which must not contain source, flowing, or falling lava.
	 *
	 * @param world the Hellblock world
	 * @param pos   the position to test
	 * @return a {@link CompletableFuture} resolving to true if the block is NOT
	 *         lava
	 */
	private CompletableFuture<Boolean> isBlockNonLava(@NotNull HellblockWorld<?> world, @NotNull Pos3 pos) {
		if (world.bukkitWorld() == null)
			return CompletableFuture.completedFuture(false);

		return world.getBlockState(pos).thenApply(stateOpt -> {
			if (stateOpt.isEmpty())
				return true; // empty is fine
			String key = stateOpt.get().type().type().value();
			return !LAVA_KEY.contains(key);
		});
	}
}