package com.swiftlicious.hellblock.creation.addons.pet;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.creation.addons.ExternalProvider;

/**
 * Represents a generic external pet provider integration.
 * <p>
 * This interface defines a consistent way for your plugin to interact with
 * external pet systems (e.g., SimplePets, MyPet, etc.).
 * <p>
 * Implementations should provide logic to:
 * <ul>
 * <li>Identify if an entity belongs to the provider’s pet system</li>
 * <li>Retrieve ownership information for pets or pet users</li>
 * </ul>
 */
public interface PetProvider extends ExternalProvider {

	/**
	 * Determines whether the specified entity is managed as a pet by this
	 * provider's system.
	 *
	 * <p>
	 * This method should be implemented to leverage the provider’s official API (if
	 * available) or a reliable fallback mechanism.
	 * </p>
	 *
	 * @param pet the {@link Entity} to check
	 * @return true if the entity is a pet managed by the provider, false otherwise
	 */
	boolean isPet(@NotNull Entity pet);

	/**
	 * Retrieves the unique identifier (UUID) of the pet owner associated with the
	 * given player.
	 *
	 * <p>
	 * The returned UUID typically corresponds to the same value returned by
	 * {@link Player#getUniqueId()}, but some APIs may store ownership separately,
	 * so this provides a consistent way to retrieve it.
	 * </p>
	 *
	 * @param owner the {@link Player} whose ownership data is being queried
	 * @return the owner's UUID, or {@code null} if no matching user is found
	 */
	@Nullable
	UUID getOwnerUUID(@NotNull Player owner);
}