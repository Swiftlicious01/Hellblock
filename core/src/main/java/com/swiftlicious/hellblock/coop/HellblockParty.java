package com.swiftlicious.hellblock.coop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.player.HellblockData;

public class HellblockParty {

	private final Collection<UUID> islandMembers = new HashSet<>();

	public HellblockParty(@NotNull HellblockData hellblockData) {
		UUID owner = hellblockData.getOwnerUUID();
		Set<UUID> party = hellblockData.getParty();
		Set<UUID> trusted = hellblockData.getTrusted();
		if (owner != null)
			this.islandMembers.add(owner);
		if (party != null && !party.isEmpty())
			this.islandMembers.addAll(party);
		if (trusted != null && !trusted.isEmpty())
			this.islandMembers.addAll(trusted);
	}
	
	public Collection<UUID> getIslandMembers() {
		return this.islandMembers;
	}
}
