package com.swiftlicious.hellblock.protection;

import java.util.UUID;

import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;

public class IslandProtection {

	private final HellblockPlugin instance;

	public IslandProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	public <T> boolean changeProtectionFlag(UUID id, T flag, String value) {
		HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
		if (pi.getPlayer() == null)
			return false;
		if (!pi.hasHellblock()) {
			instance.getAdventureManager().sendMessageWithPrefix(pi.getPlayer(), "<red>You don't have a hellblock!");
			return false;
		}
		if (pi.getHellblockOwner() != null && !pi.getHellblockOwner().equals(id)) {
			instance.getAdventureManager().sendMessageWithPrefix(pi.getPlayer(),
					"<red>Only the owner of the hellblock island can change this!");
			return false;
		}
		if (instance.getHellblockHandler().isWorldguardProtect()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id);
			if (region == null)
				return false;

			pi.setProtectionValue(((StateFlag) flag) + ":" + value);
			instance.getAdventureManager().sendMessage(pi.getPlayer(), "" + pi.getProtectionFlags());
			instance.getAdventureManager().sendMessage(pi.getPlayer(), "" + pi.getProtectionValue((StateFlag) flag));
			region.setFlag((StateFlag) flag, StateFlag.State.valueOf(value.toUpperCase()));
			pi.saveHellblockPlayer();
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}
}
