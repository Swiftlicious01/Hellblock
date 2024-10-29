package com.swiftlicious.hellblock.coop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

import lombok.Getter;
import lombok.NonNull;

public class HellblockParty {

	@Getter
	private final Collection<UUID> islandMembers;

	public HellblockParty(@NonNull HellblockPlayer player) {
		islandMembers = new HashSet<>();
		UUID owner = player.getHellblockOwner();
		Set<UUID> party = player.getHellblockParty();
		Set<UUID> trusted = player.getWhoTrusted();
		islandMembers.add(owner);
		islandMembers.addAll(party);
		islandMembers.addAll(trusted);
	}
}
