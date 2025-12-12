package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.math.BlockVector3;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.world.HellblockWorld;

/**
 * Represents an abstract protection mechanism for a Hellblock island.
 * <p>
 * Implementations of this interface define how protection regions are managed,
 * either through external plugins like WorldGuard or via internal logic.
 *
 * <p>
 * The type parameter {@code T} represents the vector type used for protection
 * bounds:
 * <ul>
 * <li>{@link BlockVector3} – used by {@link WorldGuardHook} (external
 * plugin)</li>
 * <li>{@link Vector} – used by {@link DefaultProtection} (internal
 * fallback)</li>
 * </ul>
 *
 * @param <T> the vector type for representing island bounding box corners
 */
public interface IslandProtection<T> {

	/**
	 * Registers protection for the specified Hellblock island.
	 *
	 * @param world     the world the island resides in
	 * @param ownerData the {@link UserData} of the island owner
	 * @return a {@link CompletableFuture} that completes when protection is applied
	 */
	CompletableFuture<Void> protectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData);

	/**
	 * Removes protection for the specified island.
	 *
	 * @param world   the world the island resides in
	 * @param ownerId the UUID of the island owner
	 * @return a {@link CompletableFuture} that completes when protection is removed
	 */
	CompletableFuture<Void> unprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId);

	/**
	 * Transfers protection from one user to another during an island transfer.
	 *
	 * @param world          the world the island resides in
	 * @param ownerData      the current owner's {@link UserData}
	 * @param transfereeData the new owner's {@link UserData}
	 * @return a {@link CompletableFuture} that completes when protection is updated
	 */
	CompletableFuture<Void> reprotectHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull UserData transfereeData);

	/**
	 * Updates greeting, farewell, and other custom messages for the island region.
	 *
	 * @param world   the world the island resides in
	 * @param ownerId the UUID of the island owner
	 */
	void updateHellblockMessages(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId);

	/**
	 * Marks the island as abandoned in the protection system.
	 *
	 * @param world   the world the island resides in
	 * @param ownerId the UUID of the island owner
	 */
	void abandonIsland(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId);

	/**
	 * Restores the default flags for the island’s protected region.
	 *
	 * @param world   the world the island resides in
	 * @param ownerId the UUID of the island owner
	 */
	void restoreFlags(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId);

	/**
	 * Optionally locks the island to prevent interaction.
	 * <p>
	 * This method can be overridden by implementations that support locking.
	 *
	 * @param world     the world the island resides in
	 * @param ownerData the island owner
	 */
	default void lockHellblock(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData) {
		// Override if needed, only should be used for WorldGuard
	}

	/**
	 * Optionally changes a specific flag on the island’s region.
	 * <p>
	 * This method can be overridden by implementations that support dynamic flag
	 * changes.
	 *
	 * @param world     the world the island resides in
	 * @param ownerData the island owner
	 * @param flag      the {@link HellblockFlag} to update
	 */
	default void changeHellblockFlag(@NotNull HellblockWorld<?> world, @NotNull UserData ownerData,
			@NotNull HellblockFlag flag) {
		// Override if needed
	}

	/**
	 * Returns the upper (maximum) corner of the user's island protection bounding
	 * box.
	 * <p>
	 * The returned type {@code T} depends on the protection system implementation:
	 * <ul>
	 * <li>{@code BlockVector3} for WorldGuard-based protection</li>
	 * <li>{@code Vector} for internal (vanilla) protection</li>
	 * </ul>
	 *
	 * @param ownerData the {@link UserData} containing the island bounding box
	 * @return the upper protection corner as type {@code T}
	 * @throws IllegalStateException if the bounding box is not set
	 */
	default T getProtectionVectorUpperCorner(@NotNull UserData ownerData) {
		// Override to get correct implementation
		return null;
	}

	/**
	 * Returns the lower (minimum) corner of the user's island protection bounding
	 * box.
	 * <p>
	 * The returned type {@code T} depends on the protection system implementation:
	 * <ul>
	 * <li>{@code BlockVector3} for WorldGuard-based protection</li>
	 * <li>{@code Vector} for internal (vanilla) protection</li>
	 * </ul>
	 *
	 * @param ownerData the {@link UserData} containing the island bounding box
	 * @return the lower protection corner as type {@code T}
	 * @throws IllegalStateException if the bounding box is not set
	 */
	default T getProtectionVectorLowerCorner(@NotNull UserData ownerData) {
		// Override to get correct implementation
		return null;
	}

	/**
	 * Retrieves the members currently within the island's protected bounds.
	 *
	 * @param world   the world the island resides in
	 * @param ownerId the UUID of the island owner
	 * @return a {@link CompletableFuture} containing the set of UUIDs of players
	 *         currently inside
	 */
	CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId);

	/**
	 * Optionally adds a player to the region's member list.
	 *
	 * @param world    the world the island resides in
	 * @param ownerId  the UUID of the island owner
	 * @param memberId the UUID of the player to add
	 */
	default void addMemberToHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId,
			@NotNull UUID memberId) {
		// Override if needed, only should be used for WorldGuard
	}

	/**
	 * Optionally removes a player from the region's member list.
	 *
	 * @param world    the world the island resides in
	 * @param ownerId  the UUID of the island owner
	 * @param memberId the UUID of the player to remove
	 */
	default void removeMemberFromHellblockBounds(@NotNull HellblockWorld<?> world, @NotNull UUID ownerId,
			@NotNull UUID memberId) {
		// Override if needed, only should be used for WorldGuard
	}
}