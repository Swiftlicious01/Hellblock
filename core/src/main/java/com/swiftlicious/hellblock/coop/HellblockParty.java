package com.swiftlicious.hellblock.coop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.swiftlicious.hellblock.player.HellblockData;

import lombok.Getter;
import lombok.NonNull;

public class HellblockParty {

	@Getter
	private final Collection<UUID> islandMembers;

	public HellblockParty(@NonNull HellblockData hellblockData) {
		islandMembers = new HashSet<>();
		UUID owner = hellblockData.getOwnerUUID();
		Set<UUID> party = hellblockData.getParty();
		Set<UUID> trusted = hellblockData.getTrusted();
		if (owner != null)
			islandMembers.add(owner);
		if (party != null && !party.isEmpty())
			islandMembers.addAll(party);
		if (trusted != null && !trusted.isEmpty())
			islandMembers.addAll(trusted);
	}
}
