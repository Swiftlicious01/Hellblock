package com.swiftlicious.hellblock.listeners.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.world.HellblockWorld;
import com.swiftlicious.hellblock.world.Pos3;

public class GeneratorManager {

	private static final Set<String> PISTON_KEYS = Set.of("minecraft:piston", "hellblock:piston");

	private final Map<String, Integer> emptyCleanupCounts = new HashMap<>();
	private static final int MAX_EMPTY_CLEANUP_LOGS = 3;

	private final List<Pos3> knownGenPositions = new ArrayList<>();
	private final Map<Pos3, GenBlock> genBreaks = new HashMap<>();
	private Map<Pos3, GenPiston> knownGenPistons = new HashMap<>();

	@NotNull
	public List<Pos3> getKnownGenPositions() {
		return knownGenPositions;
	}

	public boolean isGenLocationKnown(@NotNull Pos3 pos) {
		return this.getKnownGenPositions().contains(pos);
	}

	public void addKnownGenPosition(@NotNull Pos3 pos) {
		if (this.isGenLocationKnown(pos)) {
			return;
		}
		this.getKnownGenPositions().add(pos);
	}

	public void removeKnownGenPosition(@NotNull Pos3 pos) {
		if (this.isGenLocationKnown(pos)) {
			this.getKnownGenPositions().remove(pos);
		}
		this.genBreaks.remove(pos);
	}

	public void setPlayerForLocation(@NotNull UUID uuid, @NotNull Pos3 pos, boolean pistonPowered) {
		this.addKnownGenPosition(pos);
		this.getGenBreaks().remove(pos);

		// Create a new GenBlock object to track the player+timestamp and add it to the
		// genBreaks map
		final GenBlock gb = new GenBlock(pos, uuid, pistonPowered);
		this.getGenBreaks().put(pos, gb);
	}

	@NotNull
	public Map<Pos3, GenBlock> getGenBreaks() {
		return genBreaks;
	}

	public void cleanupExpiredPositions() {
		// Remove all expired GenBlock entries
		final Set<Map.Entry<Pos3, GenBlock>> entrySet = genBreaks.entrySet();
		if (entrySet.isEmpty()) {
			return;
		}
		final List<GenBlock> expiredBlocks = new ArrayList<>();
		entrySet.stream().map(Map.Entry::getValue).filter(GenBlock::hasExpired).forEach(expiredBlocks::add);
		expiredBlocks.forEach(genBlock -> removeKnownGenPosition(genBlock.getPosition()));
	}

	public CompletableFuture<Void> cleanupExpiredPistonsByIsland(int islandId, @NotNull HellblockWorld<?> hellWorld) {
		if (knownGenPistons == null || knownGenPistons.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Map.Entry<Pos3, GenPiston> entry : knownGenPistons.entrySet()) {
			GenPiston piston = entry.getValue();
			Pos3 pos = piston.getPos();

			CompletableFuture<Void> task = hellWorld.getBlockState(pos).thenAccept(stateOpt -> {
				boolean shouldRemove = false;

				if (stateOpt.isPresent()) {
					String key = stateOpt.get().type().type().value().toLowerCase();
					if (PISTON_KEYS.contains(key)) {
						shouldRemove = true;
					}
				}

				if (!shouldRemove && piston.getIslandId() == islandId && !piston.hasBeenUsed()) {
					shouldRemove = true;
				}

				if (shouldRemove) {
					removeKnownGenPiston(piston);
				}
			});

			futures.add(task);
		}

		return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
	}

	public CompletableFuture<Boolean> cleanupAllExpiredPistons(@NotNull HellblockWorld<?> hellWorld) {
		final String worldName = hellWorld.worldName();

		if (knownGenPistons == null || knownGenPistons.isEmpty()) {
			return CompletableFuture.completedFuture(false);
		}

		List<CompletableFuture<GenPiston>> checkFutures = knownGenPistons.values().stream().map(piston -> {
			Pos3 pos = piston.getPos();
			return hellWorld.getBlockState(pos).thenApply(stateOpt -> {
				if (stateOpt.isPresent()) {
					String key = stateOpt.get().type().type().value().toLowerCase();
					if (PISTON_KEYS.contains(key) || !piston.hasBeenUsed()) {
						return piston;
					}
				} else {
					// Block missing = expired
					return piston;
				}
				return null;
			});
		}).toList();

		return CompletableFuture.allOf(checkFutures.toArray(CompletableFuture[]::new)).thenApply(v -> {
			List<GenPiston> toRemove = checkFutures.stream().map(CompletableFuture::join).filter(Objects::nonNull)
					.toList();

			if (!toRemove.isEmpty()) {
				HellblockPlugin.getInstance()
						.debug("Cleaning up " + toRemove.size() + " expired pistons in world: " + worldName);
				toRemove.forEach(this::removeKnownGenPiston);
				emptyCleanupCounts.put(worldName, 0);
				return true;
			} else {
				int currentCount = emptyCleanupCounts.getOrDefault(worldName, 0);

				if (currentCount < MAX_EMPTY_CLEANUP_LOGS) {
					HellblockPlugin.getInstance().debug("No expired pistons to clean up in world: " + worldName);
				} else if (currentCount == MAX_EMPTY_CLEANUP_LOGS) {
					HellblockPlugin.getInstance().debug(
							"No expired pistons to clean up in world: " + worldName + " (further logs suppressed)");
				}

				emptyCleanupCounts.put(worldName, currentCount + 1);
				return false;
			}
		});
	}

	@NotNull
	public Map<Pos3, GenPiston> getKnownGenPistons() {
		return knownGenPistons;
	}

	public void setKnownGenPistons(@NotNull Map<Pos3, GenPiston> knownGenPistons) {
		this.knownGenPistons = knownGenPistons;
	}

	public void addKnownGenPiston(@NotNull GenPiston piston) {
		final Pos3 position = piston.getPos();
		this.getKnownGenPistons().remove(position);
		this.getKnownGenPistons().put(position, piston);
	}

	public void removeKnownGenPiston(@NotNull GenPiston piston) {
		final Pos3 position = piston.getPos();
		this.getKnownGenPistons().remove(position);
	}

	@NotNull
	public GenPiston[] getGenPistonsByIslandId(int islandId) {
		if (knownGenPistons == null)
			return new GenPiston[0];

		return knownGenPistons.values().stream().filter(p -> p != null && p.getIslandId() == islandId)
				.toArray(GenPiston[]::new);
	}
}