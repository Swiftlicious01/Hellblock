package com.swiftlicious.hellblock.creation.addons.pet;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import simplepets.brainsynder.api.plugin.SimplePets;
import simplepets.brainsynder.api.user.PetUser;

/**
 * Hook implementation for integrating with the SimplePets plugin.
 * <p>
 * This class allows your plugin to interact with SimplePets to:
 * <ul>
 * <li>Check if an entity is a pet managed by SimplePets</li>
 * <li>Retrieve the UUID of a pet owner</li>
 * </ul>
 * <p>
 * Implements the {@link PetProvider} interface for external compatibility.
 * Ensure SimplePets is loaded and available before using this hook.
 */
public class SimplePetsHook implements PetProvider {

	@Override
	public boolean isPet(@NotNull Entity pet) {
		// Uses the SimplePets API to check if the entity is registered as a pet
		return SimplePets.isPetEntity(pet);
	}

	@Override
	@Nullable
	public UUID getOwnerUUID(@NotNull Player owner) {
		// Look up the PetUser via the API and extract the owner UUID if available
		return SimplePets.getUserManager().getPetUser(owner).map(PetUser::getOwnerUUID).orElse(null);
	}

	@Override
	public String identifier() {
		return "SimplePets";
	}
}