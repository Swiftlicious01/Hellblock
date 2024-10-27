package com.swiftlicious.hellblock.protection;

import java.util.UUID;

import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandProtection {

	private final HellblockPlugin instance;

	public IslandProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	public boolean changeProtectionFlag(@NonNull UUID id, @NonNull HellblockFlag flag) {
		HellblockPlayer pi = HellblockPlugin.getInstance().getHellblockHandler().getActivePlayer(id);
		if (pi.getPlayer() == null) {
			LogUtils.warn("Player object returned null, please report this.");
			return false;
		}
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

			pi.setProtectionValue(flag);
			pi.saveHellblockPlayer();
			instance.getCoopManager().updateParty(id, "flag", flag);
			StateFlag newFlag = new StateFlag(flag.getFlag().getName(), false, RegionGroup.NON_MEMBERS);
			region.setFlag(newFlag, flag.getStatus() == AccessType.ALLOW ? null : StateFlag.State.DENY);
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}
}
