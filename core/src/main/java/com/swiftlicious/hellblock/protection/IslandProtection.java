package com.swiftlicious.hellblock.protection;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.protection.HellblockFlag.AccessType;
import com.swiftlicious.hellblock.protection.HellblockFlag.FlagType;

import lombok.NonNull;

public class IslandProtection {

	protected final HellblockPlugin instance;

	public IslandProtection(HellblockPlugin plugin) {
		instance = plugin;
	}

	public boolean changeProtectionFlag(@NonNull UUID id, @NonNull HellblockFlag flag) {
		Optional<UserData> user = instance.getStorageManager().getOnlineUser(id);
		if (user.isEmpty() || !user.get().isOnline()) {
			return false;
		}
		if (user.get().getHellblockData().isAbandoned()) {
			instance.getAdventureManager().sendMessageWithPrefix(user.get().getPlayer(),
					instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_IS_ABANDONED.build().key()));
			return false;
		}
		if (!user.get().getHellblockData().hasHellblock()) {
			instance.getAdventureManager().sendMessageWithPrefix(user.get().getPlayer(),
					instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build().key()));
			return false;
		}
		if (user.get().getHellblockData().getOwnerUUID() == null) {
			throw new NullPointerException("Owner reference returned null, please report this to the developer.");
		}
		if (user.get().getHellblockData().getOwnerUUID() != null
				&& !user.get().getHellblockData().getOwnerUUID().equals(id)) {
			instance.getAdventureManager().sendMessageWithPrefix(user.get().getPlayer(),
					instance.getTranslationManager()
							.miniMessageTranslation(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build().key()));
			return false;
		}
		if (instance.getConfigManager().worldguardProtect()) {
			ProtectedRegion region = instance.getWorldGuardHandler().getRegion(id,
					user.get().getHellblockData().getID());
			if (region == null) {
				throw new NullPointerException("Region returned null.");
			}

			user.get().getHellblockData().setProtectionValue(flag);
			if (convertToWorldGuardFlag(flag.getFlag()) != null)
				region.setFlag(convertToWorldGuardFlag(flag.getFlag()),
						(flag.getStatus() == AccessType.ALLOW ? null : StateFlag.State.DENY));
			return true;
		} else {
			// TODO: using plugin protection
			return false;
		}
	}

	public @Nullable StateFlag convertToWorldGuardFlag(@NonNull FlagType flag) {
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
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}

	@SuppressWarnings("deprecation")
	public @Nullable StringFlag convertToWorldGuardStringFlag(@NonNull FlagType flag) {
		StringFlag wgFlag = null;
		switch (flag) {
		case GREET_MESSAGE:
			wgFlag = Flags.GREET_MESSAGE;
			break;
		case FAREWELL_MESSAGE:
			wgFlag = Flags.FAREWELL_MESSAGE;
			break;
		default:
			throw new IllegalArgumentException("The flag you defined can't be converted into a WorldGuard flag.");
		}
		return wgFlag;
	}
}
