package com.swiftlicious.hellblock.protection;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer;
import com.swiftlicious.hellblock.playerdata.HellblockPlayer.HellblockData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;
import com.swiftlicious.hellblock.utils.LogUtils;

import lombok.NonNull;

public class IslandProtection {

	private final HellblockPlugin instance;

	public IslandProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	public boolean changeProtectionFlag(@NonNull UUID id, @NonNull HellblockFlag flag) {
		HellblockPlayer pi = instance.getHellblockHandler().getActivePlayer(id);
		if (pi.getPlayer() == null) {
			LogUtils.warn("Player object returned null, please report this.");
			throw new NullPointerException("Player returned null.");
		}
		if (pi.isAbandoned()) {
			instance.getAdventureManager().sendMessageWithPrefix(pi.getPlayer(),
					"<red>Your hellblock was deemed abandoned, you can't do anything until it's recovered!");
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
		if (instance.getHellblockHandler().isWorldguardProtected()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id, pi.getID());
			if (region == null) {
				LogUtils.warn("Region returned null, please report this.");
				throw new NullPointerException("Region returned null.");
			}

			pi.setProtectionValue(flag);
			pi.saveHellblockPlayer();
			instance.getCoopManager().updateParty(id, HellblockData.PROTECTION_FLAG, flag);
			region.setFlag(convertToWorldGuardFlag(flag.getFlag()),
					(flag.getStatus() == AccessType.ALLOW ? null : StateFlag.State.DENY));
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public @Nullable StateFlag convertToWorldGuardFlag(FlagType flag) {
		StateFlag wgFlag = null;
		switch (flag) {
		case BLOCK_BREAK:
			wgFlag = Flags.BLOCK_BREAK;
			break;
		case BLOCK_PLACE:
			wgFlag = Flags.BLOCK_PLACE;
			break;
		case BUILD:
			wgFlag = Flags.BUILD;
			break;
		case CHEST_ACCESS:
			wgFlag = Flags.CHEST_ACCESS;
			break;
		case CHORUS_TELEPORT:
			wgFlag = Flags.CHORUS_TELEPORT;
			break;
		case DAMAGE_ANIMALS:
			wgFlag = Flags.DAMAGE_ANIMALS;
			break;
		case DESTROY_VEHICLE:
			wgFlag = Flags.DESTROY_VEHICLE;
			break;
		case ENDERPEARL:
			wgFlag = Flags.ENDERPEARL;
			break;
		case ENDER_BUILD:
			wgFlag = Flags.ENDER_BUILD;
			break;
		case ENTRY:
			wgFlag = Flags.ENTRY;
			break;
		case FALL_DAMAGE:
			wgFlag = Flags.FALL_DAMAGE;
			break;
		case FIREWORK_DAMAGE:
			wgFlag = Flags.FIREWORK_DAMAGE;
			break;
		case GHAST_FIREBALL:
			wgFlag = Flags.GHAST_FIREBALL;
			break;
		case HEALTH_REGEN:
			wgFlag = Flags.HEALTH_REGEN;
			break;
		case HUNGER_DRAIN:
			wgFlag = Flags.HUNGER_DRAIN;
			break;
		case INTERACT:
			wgFlag = Flags.INTERACT;
			break;
		case INVINCIBILITY:
			wgFlag = Flags.INVINCIBILITY;
			break;
		case ITEM_FRAME_ROTATE:
			wgFlag = Flags.ITEM_FRAME_ROTATE;
			break;
		case LIGHTER:
			wgFlag = Flags.LIGHTER;
			break;
		case MOB_DAMAGE:
			wgFlag = Flags.MOB_DAMAGE;
			break;
		case MOB_SPAWNING:
			wgFlag = Flags.MOB_SPAWNING;
			break;
		case PLACE_VEHICLE:
			wgFlag = Flags.PLACE_VEHICLE;
			break;
		case POTION_SPLASH:
			wgFlag = Flags.POTION_SPLASH;
			break;
		case PVP:
			wgFlag = Flags.PVP;
			break;
		case RESPAWN_ANCHORS:
			wgFlag = Flags.RESPAWN_ANCHORS;
			break;
		case RIDE:
			wgFlag = Flags.RIDE;
			break;
		case SLEEP:
			wgFlag = Flags.SLEEP;
			break;
		case SNOWMAN_TRAILS:
			wgFlag = Flags.SNOWMAN_TRAILS;
			break;
		case TNT:
			wgFlag = Flags.TNT;
			break;
		case TRAMPLE_BLOCKS:
			wgFlag = Flags.TRAMPLE_BLOCKS;
			break;
		case USE:
			wgFlag = Flags.USE;
			break;
		case USE_ANVIL:
			wgFlag = Flags.USE_ANVIL;
			break;
		case USE_DRIPLEAF:
			wgFlag = Flags.USE_DRIPLEAF;
			break;
		case WIND_CHARGE_BURST:
			wgFlag = Flags.WIND_CHARGE_BURST;
			break;
		default:
			throw new IllegalArgumentException("The flag you defined cannot be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}

	@SuppressWarnings("deprecation")
	public @Nullable StringFlag convertToWorldGuardStringFlag(FlagType flag) {
		StringFlag wgFlag = null;
		switch (flag) {
		case GREET_MESSAGE:
			wgFlag = Flags.GREET_MESSAGE;
			break;
		case FAREWELL_MESSAGE:
			wgFlag = Flags.FAREWELL_MESSAGE;
			break;
		default:
			throw new IllegalArgumentException("The flag you defined cannot be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}
}
