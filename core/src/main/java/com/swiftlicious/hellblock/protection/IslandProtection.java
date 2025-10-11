package com.swiftlicious.hellblock.protection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.player.UserData;

public interface IslandProtection {

	CompletableFuture<Void> protectHellblock(@NotNull World world, @NotNull UserData owner);

	CompletableFuture<Void> unprotectHellblock(@NotNull World world, @NotNull UUID id);

	CompletableFuture<Void> reprotectHellblock(@NotNull World world, @NotNull UserData owner,
			@NotNull UserData transferee);

	void updateHellblockMessages(@NotNull World world, @NotNull UUID id);

	void abandonIsland(@NotNull World world, @NotNull UUID id);
	
	void restoreFlags(@NotNull World world, @NotNull UUID id);

	default void lockHellblock(@NotNull World world, @NotNull UserData owner) {
	}

	default void changeHellblockFlag(@NotNull World world, @NotNull UserData owner, @NotNull HellblockFlag flag) {
	}

	CompletableFuture<Set<UUID>> getMembersOfHellblockBounds(@NotNull World world, @NotNull UUID ownerID);

	default void addMemberToHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
	}

	default void removeMemberFromHellblockBounds(@NotNull World world, @NotNull UUID ownerID, @NotNull UUID id) {
	}
}