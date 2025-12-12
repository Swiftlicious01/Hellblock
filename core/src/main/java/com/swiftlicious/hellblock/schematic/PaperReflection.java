package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reflection-based wrappers for Version specific-only APIs (That don't work
 * across 1.17+) used across the codebase.
 * <p>
 * All methods attempt to invoke Version API methods via reflection. If the
 * method is unavailable (running on Spigot) the wrapper uses a safe fallback or
 * a no-op. Caches Method objects in a static block for performance.
 */
public final class PaperReflection {

	private PaperReflection() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/* --- Method cache --- */
	private static final Method CMD_BLOCK_SET_SUCCESS;
	private static final Method CMD_BLOCK_GET_SUCCESS;

	private static final Class<?> CHISELED_BOOKSHELF_CLASS;
	private static final Method CHISELED_BOOKSHELF_GET_INVENTORY;
	private static final Method CHISELED_BOOKSHELF_UPDATE;

	private static final Method BOAT_GET_MATERIAL;

	private static final Class<?> CHEST_BOAT_CLASS;
	private static final Method CHEST_BOAT_GET_INVENTORY;

	private static final Method ITEM_IS_UNLIMITED_LIFETIME;
	private static final Method ITEM_SET_UNLIMITED_LIFETIME;

	private static final Method TNT_MINECART_GET_FUSE_TICKS;
	private static final Method TNT_MINECART_SET_FUSE_TICKS;

	private static final Method HORSE_GET_OWNER_UNIQUE_ID;

	private static final Method WITHER_GET_INVULNERABLE_TICKS;
	private static final Method WITHER_SET_INVULNERABLE_TICKS;
	private static final Method WITHER_GET_INVULNERABILITY_TICKS;
	private static final Method WITHER_SET_INVULNERABILITY_TICKS;

	private static final Method ARMORSTAND_SET_DISABLED_SLOTS;
	private static final Method ARMORSTAND_GET_DISABLED_SLOTS;

	private static final Method VILLAGER_GET_RESTOCKS_TODAY;
	private static final Method VILLAGER_SET_RESTOCKS_TODAY;
	private static final Method VILLAGER_SET_REPUTATION;
	private static final Constructor<?> VILLAGER_REPUTATION_CONSTRUCTOR;
	private static final Method VILLAGER_REPUTATION_SET;
	private static final Method VILLAGER_GET_REPUTATIONS_MAP;
	private static final Method VILLAGER_GET_REPUTATION;
	private static final Method REPUTATION_GET_REPUTATION;
	private static final Method REPUTATION_TYPE_VALUES;

	private static final Method FALLINGBLOCK_GET_CANCEL_DROP;
	private static final Method FALLINGBLOCK_SET_CANCEL_DROP;
	private static final Method FALLINGBLOCK_GET_DAMAGE_PER_BLOCK;
	private static final Method FALLINGBLOCK_SET_DAMAGE_PER_BLOCK;
	private static final Method FALLINGBLOCK_GET_MAX_DAMAGE;
	private static final Method FALLINGBLOCK_SET_MAX_DAMAGE;

	private static final Method GUARDIAN_HAS_LASER;
	private static final Method GUARDIAN_SET_LASER;
	private static final Method GUARDIAN_GET_LASER_TICKS;
	private static final Method GUARDIAN_SET_LASER_TICKS;

	private static final Method WOLF_SET_SOUND_VARIANT;
	private static final Method WOLF_GET_SOUND_VARIANT;
	private static final Method WOLF_IS_INTERESTED;
	private static final Method WOLF_SET_INTERESTED;

	private static final Method EXPERIENCE_ORB_GET_COUNT;

	private static final Method TRIDENT_GET_LOYALTY_LEVEL;
	private static final Method TRIDENT_SET_LOYALTY_LEVEL;

	private static final Method CAT_IS_HEAD_UP;
	private static final Method CAT_SET_HEAD_UP;
	private static final Method CAT_IS_LYING_DOWN;
	private static final Method CAT_SET_LYING_DOWN;

	private static final Method ENDERMAN_IS_SCREAMING;
	private static final Method ENDERMAN_SET_SCREAMING;

	private static final Method WITCH_SET_DRINKING_POTION; // Paper method (ItemStack)
	private static final Method WITCH_GET_DRINKING_POTION;
	private static final Method WITCH_IS_DRINKING_POTION;

	private static final Method ZOMBIE_CAN_BREAK_DOORS;
	private static final Method ZOMBIE_SET_BREAK_DOORS;

	private static final Method ZOMBIE_VILLAGER_CAN_BREAK_DOORS;
	private static final Method ZOMBIE_VILLAGER_SET_BREAK_DOORS;

	private static final Method VINDICATOR_IS_JOHNNY;
	private static final Method VINDICATOR_SET_JOHNNY;

	private static final Method SKELETON_HORSE_IS_TRAPPED;
	private static final Method SKELETON_HORSE_SET_TRAPPED;

	private static final Method GHAST_IS_CHARGING;
	private static final Method GHAST_SET_CHARGING;

	private static final Method FOX_IS_FACEPLANTED;
	private static final Method FOX_SET_FACEPLANTED;

	private static final Method PANDA_IS_SNEEZING;
	private static final Method PANDA_IS_ROLLING;
	private static final Method PANDA_IS_SITTING;
	private static final Method PANDA_SET_SNEEZING;
	private static final Method PANDA_SET_ROLLING;
	private static final Method PANDA_SET_SITTING;

	private static final Class<?> FROG_CLASS;

	private static final Method TURTLE_SET_HOME;
	private static final Method TURTLE_GET_HOME;
	private static final Method TURTLE_HAS_EGG;
	private static final Method TURTLE_SET_HAS_EGG;

	private static final Method DOLPHIN_SET_HAS_FISH;
	private static final Method DOLPHIN_HAS_FISH;
	private static final Method DOLPHIN_SET_MOISTNESS;
	private static final Method DOLPHIN_GET_MOISTNESS;

	private static final Method PIGLIN_SET_CHARGING_CROSSBOW;
	private static final Method PIGLIN_IS_CHARGING_CROSSBOW;
	private static final Method PIGLIN_SET_DANCING;
	private static final Method PIGLIN_IS_DANCING;

	private static final Method VEX_SET_SUMMONER;
	private static final Method VEX_GET_SUMMONER;
	private static final Method VEX_GET_LIMITED_LIFETIME;
	private static final Method VEX_SET_LIMITED_LIFETIME;
	private static final Method VEX_GET_LIFETICKS;
	private static final Method VEX_SET_LIFETICKS;
	private static final Method VEX_GET_BOUND;
	private static final Method VEX_SET_BOUND;

	private static final Method GOAT_HAS_LEFT_HORN;
	private static final Method GOAT_HAS_RIGHT_HORN;
	private static final Method GOAT_SET_LEFT_HORN;
	private static final Method GOAT_SET_RIGHT_HORN;

	private static final Method ENDERDRAGON_SET_PODIUM;
	private static final Method ENDERDRAGON_GET_PODIUM;

	private static final Method SNIFFER_GET_STATE;
	private static final Method SNIFFER_SET_STATE;
	private static final Class<?> SNIFFER_CLASS;
	private static final Class<?> SNIFFER_STATE_ENUM;

	private static final Method CAMEL_IS_SITTING;
	private static final Method CAMEL_SET_SITTING;
	private static final Method CAMEL_GET_INVENTORY;
	private static final Method CAMEL_SET_SADDLE;
	private static final Class<?> CAMEL_CLASS;

	private static final Method ALLAY_IS_DANCING;
	private static final Method ALLAY_CAN_DUPLICATE;
	private static final Method ALLAY_SET_CAN_DUPLICATE;
	private static final Method ALLAY_GET_DUPLICATION_COOLDOWN;
	private static final Method ALLAY_SET_DUPLICATION_COOLDOWN;
	private static final Method ALLAY_GET_JUKEBOX;
	private static final Method ALLAY_START_DANCING;
	private static final Class<?> ALLAY_CLASS;

	private static final Method WARDEN_GET_ENTITY_ANGRY_AT;
	private static final Method WARDEN_GET_ANGER_NO_ARGS;
	private static final Method WARDEN_GET_ANGER_WITH_ENTITY;
	private static final Method WARDEN_SET_ANGER;
	private static final Class<?> WARDEN_CLASS;

	static {
		Method cmdBlockSetSuccess = null, cmdBlockGetSuccess = null;

		Class<?> chiseledBookshelfClass = null;
		Method chiseledBookshelfGetInventory = null;
		Method chiseledBookshelfUpdate = null;

		Method boatGetBoatMaterial = null;

		Class<?> chestBoatClass = null;
		Method chestBoatGetInventory = null;

		Method itemUnlimitedLifetime = null;
		Method itemSetUnlimitedLifetime = null;

		Method tntMinecartGetFuseTicks = null, tntMinecartSetFuseTicks = null;

		Method horseGetOwnerUniqueId = null;

		Method skeletonHorseIsTrapped = null, skeletonHorseSetTrapped = null;

		Method witherGetInvulnerableTicks = null, witherSetInvulnerableTicks = null;
		Method witherGetInvulnerabilityTicks = null, witherSetInvulnerabilityTicks = null;

		Method expOrbGetCount = null;

		Method tridentGetLoyaltyLevel = null, tridentSetLoyaltyLevel = null;

		Method armorStandSetDisabledSlots = null, armorStandGetDisabledSlots = null;

		Method villagerGetRestocksToday = null, villagerSetRestocksToday = null;
		Method villagerSetReputation = null;
		Constructor<?> villagerReputationConstructor = null;
		Method villagerReputationSet = null;
		Method villagerGetReputations = null;
		Method villagerGetReputation = null;
		Method reputationGetReputationValue = null;
		Method reputationTypeValues = null;

		Method fallingBlockGetCancelDrop = null, fallingBlockSetCancelDrop = null;
		Method fallingBlockGetDamagePerBlock = null, fallingBlockSetDamagePerBlock = null;
		Method fallingBlockGetMaxDamage = null, fallingBlockSetMaxDamage = null;

		Method guardianHasLaser = null, guardianSetLaser = null;
		Method guardianGetLaserTicks = null, guardianSetLaserTicks = null;

		Method ghastIsCharging = null, ghastSetCharging = null;

		Method wolfSetSoundVariant = null, wolfGetSoundVariant = null;
		Method wolfIsInterested = null, wolfSetInterested = null;

		Method catIsHeadUp = null, catSetHeadUp = null, catIsLyingDown = null, catSetLyingDown = null;

		Method endermanIsScreaming = null, endermanSetScreaming = null;

		Method witchSetDrinkingPotion = null, witchGetDrinkingPotion = null, witchIsDrinkingPotion = null;

		Method zombieCanBreakDoors = null, zombieSetBreakDoors = null;
		Method zombieVillagerCanBreakDoors = null, zombieVillagerSetBreakDoors = null;
		Method vindicatorIsJohnny = null, vindicatorSetJohnny = null;

		Method foxIsFaceplanted = null, foxSetFaceplanted = null;

		Method pandaIsSneezing = null, pandaIsRolling = null, pandaIsSitting = null;
		Method pandaSetSneezing = null, pandaSetRolling = null, pandaSetSitting = null;

		Class<?> frogClass = null;

		Method turtleSetHome = null, turtleGetHome = null, turtleHasEgg = null, turtleSetHasEgg = null;

		Method dolphinSetHasFish = null, dolphinHasFish = null, dolphinSetMoistness = null, dolphinGetMoistness = null;

		Method piglinSetChargingCrossbow = null, piglinIsChargingCrossbow = null, piglinSetDancing = null,
				piglinIsDancing = null;

		Method vexGetLimitedLifetimeTicks = null, vexSetLimitedLifetimeTicks = null;
		Method vexGetLifeTicks = null, vexSetLifeTicks = null;
		Method vexSetSummoner = null, vexGetSummoner = null;
		Method vexGetBound = null, vexSetBound = null;

		Method enderDragonSetPodium = null, enderDragonGetPodium = null;

		Method goatHasLeftHorn = null, goatHasRightHorn = null;
		Method goatSetLeftHorn = null, goatSetRightHorn = null;

		Method snifferGetState = null, snifferSetState = null;
		Class<?> snifferClass = null, snifferStateEnum = null;

		Method camelIsSitting = null, camelSetSitting = null;
		Method camelGetSaddle = null, camelSetSaddle = null;
		Class<?> camelClass = null;

		Method allayIsDancing = null, allayCanDuplicate = null, allaySetCanDuplicate = null;
		Method allayGetDupCooldown = null, allaySetDupCooldown = null;
		Method allayGetJukebox = null, allayStartDancing = null;
		Class<?> allayClass = null;

		Method wardenGetEntityAngryAt = null;
		Method wardenGetAngerNoArgs = null;
		Method wardenGetAngerWithEntity = null;
		Method wardenSetAnger = null;
		Class<?> wardenClass = null;

		try {
			Class<?> commandBlockClass = Class.forName("org.bukkit.block.CommandBlock");
			cmdBlockSetSuccess = commandBlockClass.getMethod("setSuccessCount", int.class);
			cmdBlockGetSuccess = commandBlockClass.getMethod("getSuccessCount");
		} catch (Throwable ignored) {
		}

		try {
			chiseledBookshelfClass = Class.forName("org.bukkit.block.ChiseledBookshelf");
			chiseledBookshelfGetInventory = chiseledBookshelfClass.getMethod("getInventory");
			chiseledBookshelfUpdate = chiseledBookshelfClass.getMethod("update", boolean.class, boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> itemClass = Class.forName("org.bukkit.entity.Item");
			itemUnlimitedLifetime = itemClass.getMethod("isUnlimitedLifetime");
			itemSetUnlimitedLifetime = itemClass.getMethod("setUnlimitedLifetime", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> boatClass = Class.forName("org.bukkit.entity.Boat");
			boatGetBoatMaterial = boatClass.getMethod("getBoatMaterial");
		} catch (Throwable ignored) {
		}

		try {
			chestBoatClass = Class.forName("org.bukkit.entity.ChestBoat");
			chestBoatGetInventory = chestBoatClass.getMethod("getInventory");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> horseClass = Class.forName("org.bukkit.entity.AbstractHorse");
			horseGetOwnerUniqueId = horseClass.getMethod("getOwnerUniqueId");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> skeletonHorseClass = Class.forName("org.bukkit.entity.SkeletonHorse");
			skeletonHorseIsTrapped = skeletonHorseClass.getMethod("isTrapped");
			skeletonHorseSetTrapped = skeletonHorseClass.getMethod("setTrapped", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> witherClass = Class.forName("org.bukkit.entity.Wither");
			witherGetInvulnerabilityTicks = witherClass.getMethod("getInvulnerabilityTicks");
			witherSetInvulnerabilityTicks = witherClass.getMethod("getInvulnerabilityTicks", int.class);
			witherGetInvulnerableTicks = witherClass.getMethod("getInvulnerableTicks");
			witherSetInvulnerableTicks = witherClass.getMethod("setInvulnerableTicks", int.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> expClass = Class.forName("org.bukkit.entity.ExperienceOrb");
			expOrbGetCount = expClass.getMethod("getCount");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> tridentClass = Class.forName("org.bukkit.entity.Trident");
			tridentGetLoyaltyLevel = tridentClass.getMethod("getLoyaltyLevel");
			tridentSetLoyaltyLevel = tridentClass.getMethod("setLoyaltyLevel", int.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> armorStandClass = Class.forName("org.bukkit.entity.ArmorStand");
			armorStandSetDisabledSlots = armorStandClass.getMethod("setDisabledSlots",
					Class.forName("[Lorg.bukkit.inventory.EquipmentSlot;"));
			armorStandGetDisabledSlots = armorStandClass.getMethod("getDisabledSlots");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> villagerClass = Class.forName("org.bukkit.entity.Villager");
			villagerGetRestocksToday = villagerClass.getMethod("getRestocksToday");
			villagerSetRestocksToday = villagerClass.getMethod("setRestocksToday", int.class);
			Class<?> reputationTypeClass = Class.forName("com.destroystokyo.paper.entity.villager.ReputationType");
			Class<?> reputationClass = Class.forName("com.destroystokyo.paper.entity.villager.Reputation");

			villagerSetReputation = villagerClass.getMethod("setReputation", UUID.class, reputationClass);
			villagerGetReputations = villagerClass.getMethod("getReputations");
			villagerGetReputation = villagerClass.getMethod("getReputation", UUID.class);
			villagerReputationConstructor = reputationClass.getConstructor();
			villagerReputationSet = reputationClass.getMethod("setReputation", reputationTypeClass, int.class);
			reputationTypeValues = reputationTypeClass.getMethod("values");
			reputationGetReputationValue = reputationClass.getMethod("getReputation", reputationTypeClass);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> fallingBlockClass = Class.forName("org.bukkit.entity.FallingBlock");
			fallingBlockGetCancelDrop = fallingBlockClass.getMethod("getCancelDrop");
			fallingBlockSetCancelDrop = fallingBlockClass.getMethod("setCancelDrop", boolean.class);
			fallingBlockGetDamagePerBlock = fallingBlockClass.getMethod("getDamagePerBlock");
			fallingBlockSetDamagePerBlock = fallingBlockClass.getMethod("setDamagePerBlock", float.class);
			fallingBlockGetMaxDamage = fallingBlockClass.getMethod("getMaxDamage");
			fallingBlockSetMaxDamage = fallingBlockClass.getMethod("setMaxDamage", int.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> guardianClass = Class.forName("org.bukkit.entity.Guardian");
			guardianHasLaser = guardianClass.getMethod("hasLaser");
			guardianSetLaser = guardianClass.getMethod("setLaser", boolean.class);
			guardianGetLaserTicks = guardianClass.getMethod("getLaserTicks");
			guardianSetLaserTicks = guardianClass.getMethod("setLaserTicks", int.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> ghastClass = Class.forName("org.bukkit.entity.Ghast");
			ghastIsCharging = ghastClass.getMethod("isCharging");
			ghastSetCharging = ghastClass.getMethod("setCharging", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> tntMinecartClass = Class.forName("org.bukkit.entity.minecart.ExplosiveMinecart");
			tntMinecartGetFuseTicks = tntMinecartClass.getMethod("getFuseTicks");
			tntMinecartSetFuseTicks = tntMinecartClass.getMethod("setFuseTicks", int.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> wolfClass = Class.forName("org.bukkit.entity.Wolf");
			wolfIsInterested = wolfClass.getMethod("isInterested");
			wolfSetInterested = wolfClass.getMethod("setInterested", boolean.class);
			Class<?> wolfSoundClass = Class.forName("org.bukkit.entity.Wolf$SoundVariant");
			wolfSetSoundVariant = wolfClass.getMethod("setSoundVariant", wolfSoundClass);
			wolfGetSoundVariant = wolfClass.getMethod("getSoundVariant");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> catClass = Class.forName("org.bukkit.entity.Cat");
			catIsHeadUp = catClass.getMethod("isHeadUp");
			catSetHeadUp = catClass.getMethod("setHeadUp", boolean.class);
			catIsLyingDown = catClass.getMethod("isLyingDown");
			catSetLyingDown = catClass.getMethod("setLyingDown", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> endermanClass = Class.forName("org.bukkit.entity.Enderman");
			endermanIsScreaming = endermanClass.getMethod("isScreaming");
			endermanSetScreaming = endermanClass.getMethod("setScreaming", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			// Witch setDrinkingPotion(ItemStack) - Paper prefers
			Class<?> witchClass = Class.forName("org.bukkit.entity.Witch");
			witchIsDrinkingPotion = witchClass.getMethod("isDrinkingPotion");
			witchSetDrinkingPotion = witchClass.getMethod("setDrinkingPotion", ItemStack.class);
			witchGetDrinkingPotion = witchClass.getMethod("getDrinkingPotion");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> zombieClass = Class.forName("org.bukkit.entity.Zombie");
			zombieCanBreakDoors = zombieClass.getMethod("canBreakDoors");
			zombieSetBreakDoors = zombieClass.getMethod("setCanBreakDoors", boolean.class);
			Class.forName("org.bukkit.entity.ZombieVillager");
			zombieVillagerCanBreakDoors = zombieClass.getMethod("canBreakDoors");
			zombieVillagerSetBreakDoors = zombieClass.getMethod("setCanBreakDoors", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> vindicatorClass = Class.forName("org.bukkit.entity.Vindicator");
			vindicatorIsJohnny = vindicatorClass.getMethod("isJohnny");
			vindicatorSetJohnny = vindicatorClass.getMethod("setJohnny", boolean.class);
		} catch (Throwable ignored) {
		}
		try {
			Class<?> foxClass = Class.forName("org.bukkit.entity.Fox");
			foxIsFaceplanted = foxClass.getMethod("isFaceplanted");
			foxSetFaceplanted = foxClass.getMethod("setFaceplanted", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> pandaClass = Class.forName("org.bukkit.entity.Panda");
			pandaIsSneezing = pandaClass.getMethod("isSneezing");
			pandaIsRolling = pandaClass.getMethod("isRolling");
			pandaIsSitting = pandaClass.getMethod("isSitting");
			pandaSetSneezing = pandaClass.getMethod("setSneezing", boolean.class);
			pandaSetRolling = pandaClass.getMethod("setRolling", boolean.class);
			pandaSetSitting = pandaClass.getMethod("setSitting", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			frogClass = Class.forName("org.bukkit.entity.Frog");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> turtleClass = Class.forName("org.bukkit.entity.Turtle");
			turtleSetHome = turtleClass.getMethod("setHome", Location.class);
			turtleGetHome = turtleClass.getMethod("getHome");
			turtleHasEgg = turtleClass.getMethod("hasEgg");
			turtleSetHasEgg = turtleClass.getMethod("setHasEgg", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			Class<?> dolphinClass = Class.forName("org.bukkit.entity.Dolphin");
			dolphinSetHasFish = dolphinClass.getMethod("setHasFish", boolean.class);
			dolphinHasFish = dolphinClass.getMethod("hasFish");
			dolphinSetMoistness = dolphinClass.getMethod("setMoistness", int.class);
			dolphinGetMoistness = dolphinClass.getMethod("getMoistness");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> piglinClass = Class.forName("org.bukkit.entity.Piglin");
			piglinSetChargingCrossbow = piglinClass.getMethod("setChargingCrossbow", boolean.class);
			piglinIsChargingCrossbow = piglinClass.getMethod("isChargingCrossbow");
			piglinSetDancing = piglinClass.getMethod("setDancing", boolean.class);
			piglinIsDancing = piglinClass.getMethod("isDancing");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> vexClass = Class.forName("org.bukkit.entity.Vex");
			vexGetBound = vexClass.getMethod("getBound");
			vexSetBound = vexClass.getMethod("setBound", Location.class);
			vexGetLimitedLifetimeTicks = vexClass.getMethod("getLimitedLifetimeTicks");
			vexSetLimitedLifetimeTicks = vexClass.getMethod("setLimitedLifetimeTicks", int.class);
			vexGetLifeTicks = vexClass.getMethod("getLifeTicks");
			vexSetLifeTicks = vexClass.getMethod("setLifeTicks", int.class);
			Class<?> mobClass = Class.forName("org.bukkit.entity.Mob");
			vexSetSummoner = vexClass.getMethod("setSummoner", mobClass);
			vexGetSummoner = vexClass.getMethod("getSummoner");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> edClass = Class.forName("org.bukkit.entity.EnderDragon");
			enderDragonSetPodium = edClass.getMethod("setPodium", Location.class);
			enderDragonGetPodium = edClass.getMethod("getPodium");
		} catch (Throwable ignored) {
		}

		try {
			Class<?> goatClass = Class.forName("org.bukkit.entity.Goat");
			goatHasLeftHorn = goatClass.getMethod("hasLeftHorn");
			goatHasRightHorn = goatClass.getMethod("hasRightHorn");
			goatSetLeftHorn = goatClass.getMethod("setLeftHorn", boolean.class);
			goatSetRightHorn = goatClass.getMethod("setRightHorn", boolean.class);
		} catch (Throwable ignored) {
		}

		try {
			snifferClass = Class.forName("org.bukkit.entity.Sniffer");
			snifferStateEnum = Class.forName("org.bukkit.entity.Sniffer$State");
			snifferGetState = snifferClass.getMethod("getState");
			snifferSetState = snifferClass.getMethod("setState", snifferStateEnum);
		} catch (Throwable ignored) {
		}

		try {
			camelClass = Class.forName("org.bukkit.entity.Camel");
			camelIsSitting = camelClass.getMethod("isSitting");
			camelSetSitting = camelClass.getMethod("setSitting", boolean.class);
			Class<?> inventoryClass = Class.forName("org.bukkit.inventory.AbstractHorseInventory");
			camelGetSaddle = camelClass.getMethod("getInventory");
			camelSetSaddle = inventoryClass.getMethod("setSaddle", ItemStack.class);
		} catch (Throwable ignored) {
		}

		try {
			allayClass = Class.forName("org.bukkit.entity.Allay");
			allayIsDancing = allayClass.getMethod("isDancing");
			allayCanDuplicate = allayClass.getMethod("canDuplicate");
			allaySetCanDuplicate = allayClass.getMethod("setCanDuplicate", boolean.class);
			allayGetDupCooldown = allayClass.getMethod("getDuplicationCooldown");
			allaySetDupCooldown = allayClass.getMethod("setDuplicationCooldown", long.class);
			allayGetJukebox = allayClass.getMethod("getJukebox");
			allayStartDancing = allayClass.getMethod("startDancing", Location.class);
		} catch (Throwable ignored) {
		}

		try {
			wardenClass = Class.forName("org.bukkit.entity.Warden");
			// 1.19.2+
			wardenGetEntityAngryAt = wardenClass.getMethod("getEntityAngryAt");
			wardenGetAngerNoArgs = wardenClass.getMethod("getAnger");
			// 1.19.1 fallback
			wardenGetAngerWithEntity = wardenClass.getMethod("getAnger", Class.forName("org.bukkit.entity.Entity"));
			// all versions
			wardenSetAnger = wardenClass.getMethod("setAnger", Class.forName("org.bukkit.entity.LivingEntity"),
					int.class);
		} catch (Throwable ignored) {
		}

		CMD_BLOCK_SET_SUCCESS = cmdBlockSetSuccess;
		CMD_BLOCK_GET_SUCCESS = cmdBlockGetSuccess;

		CHISELED_BOOKSHELF_CLASS = chiseledBookshelfClass;
		CHISELED_BOOKSHELF_GET_INVENTORY = chiseledBookshelfGetInventory;
		CHISELED_BOOKSHELF_UPDATE = chiseledBookshelfUpdate;

		BOAT_GET_MATERIAL = boatGetBoatMaterial;

		CHEST_BOAT_CLASS = chestBoatClass;
		CHEST_BOAT_GET_INVENTORY = chestBoatGetInventory;

		ITEM_IS_UNLIMITED_LIFETIME = itemUnlimitedLifetime;
		ITEM_SET_UNLIMITED_LIFETIME = itemSetUnlimitedLifetime;

		TNT_MINECART_GET_FUSE_TICKS = tntMinecartGetFuseTicks;
		TNT_MINECART_SET_FUSE_TICKS = tntMinecartSetFuseTicks;

		HORSE_GET_OWNER_UNIQUE_ID = horseGetOwnerUniqueId;

		SKELETON_HORSE_IS_TRAPPED = skeletonHorseIsTrapped;
		SKELETON_HORSE_SET_TRAPPED = skeletonHorseSetTrapped;

		WITHER_GET_INVULNERABLE_TICKS = witherGetInvulnerableTicks;
		WITHER_SET_INVULNERABLE_TICKS = witherSetInvulnerableTicks;
		WITHER_GET_INVULNERABILITY_TICKS = witherGetInvulnerabilityTicks;
		WITHER_SET_INVULNERABILITY_TICKS = witherSetInvulnerabilityTicks;

		EXPERIENCE_ORB_GET_COUNT = expOrbGetCount;

		TRIDENT_GET_LOYALTY_LEVEL = tridentGetLoyaltyLevel;
		TRIDENT_SET_LOYALTY_LEVEL = tridentSetLoyaltyLevel;

		ARMORSTAND_SET_DISABLED_SLOTS = armorStandSetDisabledSlots;
		ARMORSTAND_GET_DISABLED_SLOTS = armorStandGetDisabledSlots;

		VILLAGER_GET_RESTOCKS_TODAY = villagerGetRestocksToday;
		VILLAGER_SET_RESTOCKS_TODAY = villagerSetRestocksToday;
		VILLAGER_SET_REPUTATION = villagerSetReputation;
		VILLAGER_REPUTATION_CONSTRUCTOR = villagerReputationConstructor;
		VILLAGER_REPUTATION_SET = villagerReputationSet;
		VILLAGER_GET_REPUTATIONS_MAP = villagerGetReputations;
		VILLAGER_GET_REPUTATION = villagerGetReputation;
		REPUTATION_TYPE_VALUES = reputationTypeValues;
		REPUTATION_GET_REPUTATION = reputationGetReputationValue;

		FALLINGBLOCK_GET_CANCEL_DROP = fallingBlockGetCancelDrop;
		FALLINGBLOCK_SET_CANCEL_DROP = fallingBlockSetCancelDrop;
		FALLINGBLOCK_GET_DAMAGE_PER_BLOCK = fallingBlockGetDamagePerBlock;
		FALLINGBLOCK_SET_DAMAGE_PER_BLOCK = fallingBlockSetDamagePerBlock;
		FALLINGBLOCK_GET_MAX_DAMAGE = fallingBlockGetMaxDamage;
		FALLINGBLOCK_SET_MAX_DAMAGE = fallingBlockSetMaxDamage;

		GUARDIAN_HAS_LASER = guardianHasLaser;
		GUARDIAN_SET_LASER = guardianSetLaser;
		GUARDIAN_GET_LASER_TICKS = guardianGetLaserTicks;
		GUARDIAN_SET_LASER_TICKS = guardianSetLaserTicks;

		GHAST_IS_CHARGING = ghastIsCharging;
		GHAST_SET_CHARGING = ghastSetCharging;

		WOLF_SET_SOUND_VARIANT = wolfSetSoundVariant;
		WOLF_GET_SOUND_VARIANT = wolfGetSoundVariant;
		WOLF_IS_INTERESTED = wolfIsInterested;
		WOLF_SET_INTERESTED = wolfSetInterested;

		CAT_IS_HEAD_UP = catIsHeadUp;
		CAT_SET_HEAD_UP = catSetHeadUp;
		CAT_IS_LYING_DOWN = catIsLyingDown;
		CAT_SET_LYING_DOWN = catSetLyingDown;

		ENDERMAN_IS_SCREAMING = endermanIsScreaming;
		ENDERMAN_SET_SCREAMING = endermanSetScreaming;

		WITCH_SET_DRINKING_POTION = witchSetDrinkingPotion;
		WITCH_GET_DRINKING_POTION = witchGetDrinkingPotion;
		WITCH_IS_DRINKING_POTION = witchIsDrinkingPotion;

		ZOMBIE_CAN_BREAK_DOORS = zombieCanBreakDoors;
		ZOMBIE_SET_BREAK_DOORS = zombieSetBreakDoors;

		ZOMBIE_VILLAGER_CAN_BREAK_DOORS = zombieVillagerCanBreakDoors;
		ZOMBIE_VILLAGER_SET_BREAK_DOORS = zombieVillagerSetBreakDoors;

		VINDICATOR_IS_JOHNNY = vindicatorIsJohnny;
		VINDICATOR_SET_JOHNNY = vindicatorSetJohnny;

		FOX_IS_FACEPLANTED = foxIsFaceplanted;
		FOX_SET_FACEPLANTED = foxSetFaceplanted;

		PANDA_IS_SNEEZING = pandaIsSneezing;
		PANDA_IS_ROLLING = pandaIsRolling;
		PANDA_IS_SITTING = pandaIsSitting;
		PANDA_SET_SNEEZING = pandaSetSneezing;
		PANDA_SET_ROLLING = pandaSetRolling;
		PANDA_SET_SITTING = pandaSetSitting;

		FROG_CLASS = frogClass;

		TURTLE_SET_HOME = turtleSetHome;
		TURTLE_GET_HOME = turtleGetHome;
		TURTLE_HAS_EGG = turtleHasEgg;
		TURTLE_SET_HAS_EGG = turtleSetHasEgg;

		DOLPHIN_SET_HAS_FISH = dolphinSetHasFish;
		DOLPHIN_HAS_FISH = dolphinHasFish;
		DOLPHIN_SET_MOISTNESS = dolphinSetMoistness;
		DOLPHIN_GET_MOISTNESS = dolphinGetMoistness;

		PIGLIN_SET_CHARGING_CROSSBOW = piglinSetChargingCrossbow;
		PIGLIN_IS_CHARGING_CROSSBOW = piglinIsChargingCrossbow;
		PIGLIN_SET_DANCING = piglinSetDancing;
		PIGLIN_IS_DANCING = piglinIsDancing;

		VEX_SET_SUMMONER = vexSetSummoner;
		VEX_GET_SUMMONER = vexGetSummoner;
		VEX_GET_LIMITED_LIFETIME = vexGetLimitedLifetimeTicks;
		VEX_SET_LIMITED_LIFETIME = vexSetLimitedLifetimeTicks;
		VEX_GET_LIFETICKS = vexGetLifeTicks;
		VEX_SET_LIFETICKS = vexSetLifeTicks;
		VEX_GET_BOUND = vexGetBound;
		VEX_SET_BOUND = vexSetBound;

		ENDERDRAGON_SET_PODIUM = enderDragonSetPodium;
		ENDERDRAGON_GET_PODIUM = enderDragonGetPodium;

		GOAT_HAS_LEFT_HORN = goatHasLeftHorn;
		GOAT_HAS_RIGHT_HORN = goatHasRightHorn;
		GOAT_SET_LEFT_HORN = goatSetLeftHorn;
		GOAT_SET_RIGHT_HORN = goatSetRightHorn;

		SNIFFER_GET_STATE = snifferGetState;
		SNIFFER_SET_STATE = snifferSetState;
		SNIFFER_CLASS = snifferClass;
		SNIFFER_STATE_ENUM = snifferStateEnum;

		CAMEL_IS_SITTING = camelIsSitting;
		CAMEL_SET_SITTING = camelSetSitting;
		CAMEL_GET_INVENTORY = camelGetSaddle;
		CAMEL_SET_SADDLE = camelSetSaddle;
		CAMEL_CLASS = camelClass;

		ALLAY_IS_DANCING = allayIsDancing;
		ALLAY_CAN_DUPLICATE = allayCanDuplicate;
		ALLAY_SET_CAN_DUPLICATE = allaySetCanDuplicate;
		ALLAY_GET_DUPLICATION_COOLDOWN = allayGetDupCooldown;
		ALLAY_SET_DUPLICATION_COOLDOWN = allaySetDupCooldown;
		ALLAY_GET_JUKEBOX = allayGetJukebox;
		ALLAY_START_DANCING = allayStartDancing;
		ALLAY_CLASS = allayClass;

		WARDEN_GET_ENTITY_ANGRY_AT = wardenGetEntityAngryAt;
		WARDEN_GET_ANGER_NO_ARGS = wardenGetAngerNoArgs;
		WARDEN_GET_ANGER_WITH_ENTITY = wardenGetAngerWithEntity;
		WARDEN_SET_ANGER = wardenSetAnger;
		WARDEN_CLASS = wardenClass;
	}

	public static void setCommandBlockSuccessCount(org.bukkit.block.CommandBlock cmd, int value) {
		if (CMD_BLOCK_SET_SUCCESS != null) {
			try {
				CMD_BLOCK_SET_SUCCESS.invoke(cmd, value);
				return;
			} catch (Throwable ignored) {
			}
		}
		// no-op on Spigot
	}

	public static int getCommandBlockSuccessCount(org.bukkit.block.CommandBlock cmd) {
		if (CMD_BLOCK_GET_SUCCESS != null) {
			try {
				return (int) CMD_BLOCK_GET_SUCCESS.invoke(cmd);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static boolean isChiseledBookshelf(@NotNull org.bukkit.block.BlockState state) {
		return CHISELED_BOOKSHELF_CLASS != null && CHISELED_BOOKSHELF_CLASS.isInstance(state);
	}

	@Nullable
	public static org.bukkit.inventory.Inventory getChiseledBookshelfInventory(
			@NotNull org.bukkit.block.BlockState state) {
		if (CHISELED_BOOKSHELF_GET_INVENTORY != null && CHISELED_BOOKSHELF_CLASS.isInstance(state)) {
			try {
				return (org.bukkit.inventory.Inventory) CHISELED_BOOKSHELF_GET_INVENTORY.invoke(state);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static void updateChiseledBookshelf(@NotNull org.bukkit.block.BlockState state) {
		if (CHISELED_BOOKSHELF_UPDATE != null && CHISELED_BOOKSHELF_CLASS.isInstance(state)) {
			try {
				CHISELED_BOOKSHELF_UPDATE.invoke(state, true, false);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static String getBoatMaterialName(@NotNull org.bukkit.entity.Boat boat) {
		if (BOAT_GET_MATERIAL != null) {
			try {
				Object boatMaterial = BOAT_GET_MATERIAL.invoke(boat);
				if (boatMaterial instanceof Enum<?>) {
					return ((Enum<?>) boatMaterial).name();
				}
			} catch (Throwable ignored) {
			}
		}
		// Fallback: extract from entity type name
		return boat.getType().name().replace("_BOAT", "");
	}

	public static boolean isChestBoat(org.bukkit.entity.Entity entity) {
		return CHEST_BOAT_CLASS != null && CHEST_BOAT_CLASS.isInstance(entity);
	}

	@Nullable
	public static org.bukkit.inventory.Inventory getChestBoatInventory(org.bukkit.entity.Entity entity) {
		if (CHEST_BOAT_CLASS != null && CHEST_BOAT_GET_INVENTORY != null && CHEST_BOAT_CLASS.isInstance(entity)) {
			try {
				return (org.bukkit.inventory.Inventory) CHEST_BOAT_GET_INVENTORY.invoke(entity);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static boolean isItemUnlimitedLifetime(org.bukkit.entity.Item item) {
		if (ITEM_IS_UNLIMITED_LIFETIME != null) {
			try {
				return (boolean) ITEM_IS_UNLIMITED_LIFETIME.invoke(item);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setItemUnlimitedLifetime(org.bukkit.entity.Item item, boolean unlimited) {
		if (ITEM_SET_UNLIMITED_LIFETIME != null) {
			try {
				ITEM_SET_UNLIMITED_LIFETIME.invoke(item, unlimited);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getTNTMinecartFuseTicks(org.bukkit.entity.minecart.ExplosiveMinecart minecart) {
		if (TNT_MINECART_GET_FUSE_TICKS != null) {
			try {
				return (int) TNT_MINECART_GET_FUSE_TICKS.invoke(minecart);
			} catch (Exception ignored) {
			}
		}
		return -1; // or fallback/default
	}

	public static void setTNTMinecartFuseTicks(org.bukkit.entity.minecart.ExplosiveMinecart minecart, int ticks) {
		if (TNT_MINECART_SET_FUSE_TICKS != null) {
			try {
				TNT_MINECART_SET_FUSE_TICKS.invoke(minecart, ticks);
			} catch (Exception ignored) {
			}
		}
	}

	@Nullable
	public static UUID getHorseOwnerUniqueId(org.bukkit.entity.AbstractHorse horse) {
		if (HORSE_GET_OWNER_UNIQUE_ID != null) {
			try {
				return (UUID) HORSE_GET_OWNER_UNIQUE_ID.invoke(horse);
			} catch (Throwable ignored) {
			}
		}
		// fallback to legacy owner
		if (horse.getOwner() != null)
			return horse.getOwner().getUniqueId();
		return null;
	}

	public static boolean isSkeletonHorseTrapped(org.bukkit.entity.SkeletonHorse horse) {
		if (SKELETON_HORSE_IS_TRAPPED != null) {
			try {
				return (boolean) SKELETON_HORSE_IS_TRAPPED.invoke(horse);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setSkeletonHorseTrapped(org.bukkit.entity.SkeletonHorse horse, boolean trapped) {
		if (SKELETON_HORSE_SET_TRAPPED != null) {
			try {
				SKELETON_HORSE_SET_TRAPPED.invoke(horse, trapped);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getWitherInvulnerableTicks(org.bukkit.entity.Wither wither) {
		if (WITHER_GET_INVULNERABLE_TICKS != null) {
			try {
				return (int) WITHER_GET_INVULNERABLE_TICKS.invoke(wither);
			} catch (Throwable ignored) {
			}
		}
		if (WITHER_GET_INVULNERABILITY_TICKS != null) {
			try {
				return (int) WITHER_GET_INVULNERABILITY_TICKS.invoke(wither);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setWitherInvulnerableTicks(org.bukkit.entity.Wither wither, int ticks) {
		if (WITHER_SET_INVULNERABLE_TICKS != null) {
			try {
				WITHER_SET_INVULNERABLE_TICKS.invoke(wither, ticks);
			} catch (Throwable ignored) {
			}
		}
		if (WITHER_SET_INVULNERABILITY_TICKS != null) {
			try {
				WITHER_SET_INVULNERABILITY_TICKS.invoke(wither, ticks);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getExperienceOrbCount(org.bukkit.entity.ExperienceOrb orb) {
		if (EXPERIENCE_ORB_GET_COUNT != null) {
			try {
				return (int) EXPERIENCE_ORB_GET_COUNT.invoke(orb);
			} catch (Throwable ignored) {
			}
		}
		return orb.getExperience();
	}

	public static int getTridentLoyaltyLevel(org.bukkit.entity.Trident trident) {
		if (TRIDENT_GET_LOYALTY_LEVEL != null) {
			try {
				return (int) TRIDENT_GET_LOYALTY_LEVEL.invoke(trident);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setTridentLoyaltyLevel(org.bukkit.entity.Trident trident, int level) {
		if (TRIDENT_SET_LOYALTY_LEVEL != null) {
			try {
				TRIDENT_SET_LOYALTY_LEVEL.invoke(trident, level);
			} catch (Throwable ignored) {
			}
		}
		// fallback: no-op
	}

	public static void setArmorStandDisabledSlots(org.bukkit.entity.ArmorStand stand,
			org.bukkit.inventory.EquipmentSlot... slots) {
		if (ARMORSTAND_SET_DISABLED_SLOTS != null) {
			try {
				// need to pass as array of EquipmentSlot
				Object arr = java.lang.reflect.Array.newInstance(org.bukkit.inventory.EquipmentSlot.class,
						slots.length);
				for (int i = 0; i < slots.length; i++)
					java.lang.reflect.Array.set(arr, i, slots[i]);
				ARMORSTAND_SET_DISABLED_SLOTS.invoke(stand, arr);
				return;
			} catch (Throwable ignored) {
			}
		}
		// fallback: no-op on Spigot
	}

	@NotNull
	public static List<org.bukkit.inventory.EquipmentSlot> getArmorStandDisabledSlots(
			org.bukkit.entity.ArmorStand stand) {
		if (ARMORSTAND_GET_DISABLED_SLOTS != null) {
			try {
				org.bukkit.inventory.EquipmentSlot[] result = (org.bukkit.inventory.EquipmentSlot[]) ARMORSTAND_GET_DISABLED_SLOTS
						.invoke(stand);
				return result != null ? Arrays.asList(result) : Collections.emptyList();
			} catch (Throwable ignored) {
			}
		}
		return Collections.emptyList(); // Fallback: empty list
	}

	public static int getVillagerRestocksToday(org.bukkit.entity.Villager villager) {
		if (VILLAGER_GET_RESTOCKS_TODAY != null) {
			try {
				return (int) VILLAGER_GET_RESTOCKS_TODAY.invoke(villager);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setVillagerRestocksToday(org.bukkit.entity.Villager villager, int value) {
		if (VILLAGER_SET_RESTOCKS_TODAY != null) {
			try {
				VILLAGER_SET_RESTOCKS_TODAY.invoke(villager, value);
			} catch (Throwable ignored) {
			}
		}
		// no-op fallback
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setVillagerReputation(org.bukkit.entity.Villager villager, UUID uuid, String typeName,
			int value) {
		if (VILLAGER_SET_REPUTATION != null && VILLAGER_REPUTATION_CONSTRUCTOR != null
				&& VILLAGER_REPUTATION_SET != null) {
			try {
				Class<?> reputationTypeClass = Class.forName("com.destroystokyo.paper.entity.villager.ReputationType");
				Object repType = Enum.valueOf((Class<Enum>) reputationTypeClass, typeName);
				Object reputation = VILLAGER_REPUTATION_CONSTRUCTOR.newInstance();
				VILLAGER_REPUTATION_SET.invoke(reputation, repType, value);
				VILLAGER_SET_REPUTATION.invoke(villager, uuid, reputation);
			} catch (Throwable ignored) {
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static Map<UUID, Object> getVillagerReputations(org.bukkit.entity.Villager villager) {
		if (VILLAGER_GET_REPUTATIONS_MAP != null) {
			try {
				return (Map<UUID, Object>) VILLAGER_GET_REPUTATIONS_MAP.invoke(villager);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	@Nullable
	public static Object getVillagerReputation(org.bukkit.entity.Villager villager, UUID uuid) {
		if (VILLAGER_GET_REPUTATION != null) {
			try {
				return VILLAGER_GET_REPUTATION.invoke(villager, uuid);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	@NotNull
	public static List<Object> getReputationTypes() {
		if (REPUTATION_TYPE_VALUES != null) {
			try {
				Object[] values = (Object[]) REPUTATION_TYPE_VALUES.invoke(null);
				return Arrays.asList(values);
			} catch (Throwable ignored) {
			}
		}
		return Collections.emptyList();
	}

	public static int getReputationValue(Object reputationObj, Object typeObj) {
		if (REPUTATION_GET_REPUTATION != null && reputationObj != null && typeObj != null) {
			try {
				return (int) REPUTATION_GET_REPUTATION.invoke(reputationObj, typeObj);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static boolean getFallingBlockCancelDrop(org.bukkit.entity.FallingBlock fb) {
		if (FALLINGBLOCK_GET_CANCEL_DROP != null) {
			try {
				return (boolean) FALLINGBLOCK_GET_CANCEL_DROP.invoke(fb);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setFallingBlockCancelDrop(org.bukkit.entity.FallingBlock fb, boolean cancelDrop) {
		if (FALLINGBLOCK_SET_CANCEL_DROP != null) {
			try {
				FALLINGBLOCK_SET_CANCEL_DROP.invoke(fb, cancelDrop);
			} catch (Throwable ignored) {
			}
		}
	}

	public static float getFallingBlockDamagePerBlock(org.bukkit.entity.FallingBlock fb) {
		if (FALLINGBLOCK_GET_DAMAGE_PER_BLOCK != null) {
			try {
				return (float) FALLINGBLOCK_GET_DAMAGE_PER_BLOCK.invoke(fb);
			} catch (Throwable ignored) {
			}
		}
		return 0.0f;
	}

	public static void setFallingBlockDamagePerBlock(org.bukkit.entity.FallingBlock fb, float damage) {
		if (FALLINGBLOCK_SET_DAMAGE_PER_BLOCK != null) {
			try {
				FALLINGBLOCK_SET_DAMAGE_PER_BLOCK.invoke(fb, damage);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getFallingBlockMaxDamage(org.bukkit.entity.FallingBlock fb) {
		if (FALLINGBLOCK_GET_MAX_DAMAGE != null) {
			try {
				return (int) FALLINGBLOCK_GET_MAX_DAMAGE.invoke(fb);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setFallingBlockMaxDamage(org.bukkit.entity.FallingBlock fb, int damage) {
		if (FALLINGBLOCK_SET_MAX_DAMAGE != null) {
			try {
				FALLINGBLOCK_SET_MAX_DAMAGE.invoke(fb, damage);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean hasGuardianLaser(org.bukkit.entity.Guardian guardian) {
		if (GUARDIAN_HAS_LASER != null) {
			try {
				return (boolean) GUARDIAN_HAS_LASER.invoke(guardian);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setGuardianLaser(org.bukkit.entity.Guardian guardian, boolean laser) {
		if (GUARDIAN_SET_LASER != null) {
			try {
				GUARDIAN_SET_LASER.invoke(guardian, laser);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getGuardianLaserTicks(org.bukkit.entity.Guardian guardian) {
		if (GUARDIAN_GET_LASER_TICKS != null) {
			try {
				return (int) GUARDIAN_GET_LASER_TICKS.invoke(guardian);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setGuardianLaserTicks(org.bukkit.entity.Guardian guardian, int ticks) {
		if (GUARDIAN_SET_LASER_TICKS != null) {
			try {
				GUARDIAN_SET_LASER_TICKS.invoke(guardian, ticks);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isGhastCharging(org.bukkit.entity.Ghast ghast) {
		if (GHAST_IS_CHARGING != null) {
			try {
				return (boolean) GHAST_IS_CHARGING.invoke(ghast);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setGhastCharging(org.bukkit.entity.Ghast ghast, boolean charging) {
		if (GHAST_SET_CHARGING != null) {
			try {
				GHAST_SET_CHARGING.invoke(ghast, charging);
			} catch (Throwable ignored) {
			}
		}
	}

	// Wolf sound variant (Object because it's Paper-specific nested type)
	public static void setWolfSoundVariant(org.bukkit.entity.Wolf wolf, @Nullable Object soundVariant) {
		if (WOLF_SET_SOUND_VARIANT != null) {
			try {
				WOLF_SET_SOUND_VARIANT.invoke(wolf, soundVariant);
			} catch (Throwable ignored) {
			}
		}
		// no-op
	}

	@Nullable
	public static Object getWolfSoundVariant(org.bukkit.entity.Wolf wolf) {
		if (WOLF_GET_SOUND_VARIANT != null) {
			try {
				return WOLF_GET_SOUND_VARIANT.invoke(wolf);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static boolean isWolfInterested(org.bukkit.entity.Wolf wolf) {
		if (WOLF_IS_INTERESTED != null) {
			try {
				return (boolean) WOLF_IS_INTERESTED.invoke(wolf);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setWolfInterested(org.bukkit.entity.Wolf wolf, boolean interested) {
		if (WOLF_SET_INTERESTED != null) {
			try {
				WOLF_SET_INTERESTED.invoke(wolf, interested);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isCatHeadUp(org.bukkit.entity.Cat cat) {
		if (CAT_IS_HEAD_UP != null) {
			try {
				return (boolean) CAT_IS_HEAD_UP.invoke(cat);
			} catch (Throwable ignored) {
			}
		}
		return cat.isSitting(); // best-effort fallback (not equivalent, but safe)
	}

	public static void setCatHeadUp(org.bukkit.entity.Cat cat, boolean up) {
		if (CAT_SET_HEAD_UP != null) {
			try {
				CAT_SET_HEAD_UP.invoke(cat, up);
			} catch (Throwable ignored) {
			}
		}
		// fallback: no-op
	}

	public static boolean isCatLyingDown(org.bukkit.entity.Cat cat) {
		if (CAT_IS_LYING_DOWN != null) {
			try {
				return (boolean) CAT_IS_LYING_DOWN.invoke(cat);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setCatLyingDown(org.bukkit.entity.Cat cat, boolean lying) {
		if (CAT_SET_LYING_DOWN != null) {
			try {
				CAT_SET_LYING_DOWN.invoke(cat, lying);
			} catch (Throwable ignored) {
			}
		}
		// fallback no-op
	}

	public static boolean isEndermanScreaming(org.bukkit.entity.Enderman enderman) {
		if (ENDERMAN_IS_SCREAMING != null) {
			try {
				return (boolean) ENDERMAN_IS_SCREAMING.invoke(enderman);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setEndermanScreaming(org.bukkit.entity.Enderman enderman, boolean screaming) {
		if (ENDERMAN_SET_SCREAMING != null) {
			try {
				ENDERMAN_SET_SCREAMING.invoke(enderman, screaming);
			} catch (Throwable ignored) {
			}
		}
		// no-op fallback
	}

	public static void setWitchDrinkingPotion(org.bukkit.entity.Witch witch, ItemStack potion) {
		if (WITCH_SET_DRINKING_POTION != null) {
			try {
				WITCH_SET_DRINKING_POTION.invoke(witch, potion);
			} catch (Throwable ignored) {
			}
		}
		// fallback: no direct equivalent on Spigot; no-op
	}

	@Nullable
	public static ItemStack getWitchDrinkingPotion(org.bukkit.entity.Witch witch) {
		if (WITCH_GET_DRINKING_POTION != null) {
			try {
				return (ItemStack) WITCH_GET_DRINKING_POTION.invoke(witch);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static boolean isWitchDrinkingPotion(org.bukkit.entity.Witch witch) {
		if (WITCH_IS_DRINKING_POTION != null) {
			try {
				return (boolean) WITCH_IS_DRINKING_POTION.invoke(witch);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static boolean canZombieBreakDoors(org.bukkit.entity.Zombie zombie) {
		if (ZOMBIE_CAN_BREAK_DOORS != null) {
			try {
				return (boolean) ZOMBIE_CAN_BREAK_DOORS.invoke(zombie);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setCanZombieBreakDoors(org.bukkit.entity.Zombie zombie, boolean value) {
		if (ZOMBIE_SET_BREAK_DOORS != null) {
			try {
				ZOMBIE_SET_BREAK_DOORS.invoke(zombie, value);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean canZombieVillagerBreakDoors(org.bukkit.entity.ZombieVillager zv) {
		if (ZOMBIE_VILLAGER_CAN_BREAK_DOORS != null) {
			try {
				return (boolean) ZOMBIE_VILLAGER_CAN_BREAK_DOORS.invoke(zv);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setCanZombieVillagerBreakDoors(org.bukkit.entity.ZombieVillager zv, boolean value) {
		if (ZOMBIE_VILLAGER_SET_BREAK_DOORS != null) {
			try {
				ZOMBIE_VILLAGER_SET_BREAK_DOORS.invoke(zv, value);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isVindicatorJohnny(org.bukkit.entity.Vindicator vindicator) {
		if (VINDICATOR_IS_JOHNNY != null) {
			try {
				return (boolean) VINDICATOR_IS_JOHNNY.invoke(vindicator);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setVindicatorJohnny(org.bukkit.entity.Vindicator vindicator, boolean value) {
		if (VINDICATOR_SET_JOHNNY != null) {
			try {
				VINDICATOR_SET_JOHNNY.invoke(vindicator, value);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isFoxFaceplanted(org.bukkit.entity.Fox fox) {
		if (FOX_IS_FACEPLANTED != null) {
			try {
				return (boolean) FOX_IS_FACEPLANTED.invoke(fox);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setFoxFaceplanted(org.bukkit.entity.Fox fox, boolean faceplanted) {
		if (FOX_SET_FACEPLANTED != null) {
			try {
				FOX_SET_FACEPLANTED.invoke(fox, faceplanted);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isPandaSneezing(org.bukkit.entity.Panda panda) {
		if (PANDA_IS_SNEEZING != null) {
			try {
				return (boolean) PANDA_IS_SNEEZING.invoke(panda);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static boolean isPandaRolling(org.bukkit.entity.Panda panda) {
		if (PANDA_IS_ROLLING != null) {
			try {
				return (boolean) PANDA_IS_ROLLING.invoke(panda);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static boolean isPandaSitting(org.bukkit.entity.Panda panda) {
		if (PANDA_IS_SITTING != null) {
			try {
				return (boolean) PANDA_IS_SITTING.invoke(panda);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setPandaSneezing(org.bukkit.entity.Panda panda, boolean sneezing) {
		if (PANDA_SET_SNEEZING != null) {
			try {
				PANDA_SET_SNEEZING.invoke(panda, sneezing);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setPandaRolling(org.bukkit.entity.Panda panda, boolean rolling) {
		if (PANDA_SET_ROLLING != null) {
			try {
				PANDA_SET_ROLLING.invoke(panda, rolling);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setPandaSitting(org.bukkit.entity.Panda panda, boolean sitting) {
		if (PANDA_SET_SITTING != null) {
			try {
				PANDA_SET_SITTING.invoke(panda, sitting);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isFrog(@NotNull org.bukkit.entity.Entity entity) {
		return FROG_CLASS != null && FROG_CLASS.isInstance(entity);
	}

	public static void setTurtleHome(org.bukkit.entity.Turtle turtle, Location loc) {
		if (TURTLE_SET_HOME != null) {
			try {
				TURTLE_SET_HOME.invoke(turtle, loc);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static Location getTurtleHome(org.bukkit.entity.Turtle turtle) {
		if (TURTLE_GET_HOME != null) {
			try {
				return (Location) TURTLE_GET_HOME.invoke(turtle);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static boolean turtleHasEgg(org.bukkit.entity.Turtle turtle) {
		if (TURTLE_HAS_EGG != null) {
			try {
				return (boolean) TURTLE_HAS_EGG.invoke(turtle);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setTurtleHasEgg(org.bukkit.entity.Turtle turtle, boolean hasEgg) {
		if (TURTLE_SET_HAS_EGG != null) {
			try {
				TURTLE_SET_HAS_EGG.invoke(turtle, hasEgg);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setDolphinHasFish(org.bukkit.entity.Dolphin dolphin, boolean hasFish) {
		if (DOLPHIN_SET_HAS_FISH != null) {
			try {
				DOLPHIN_SET_HAS_FISH.invoke(dolphin, hasFish);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean dolphinHasFish(org.bukkit.entity.Dolphin dolphin) {
		if (DOLPHIN_HAS_FISH != null) {
			try {
				return (boolean) DOLPHIN_HAS_FISH.invoke(dolphin);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setDolphinMoistness(org.bukkit.entity.Dolphin dolphin, int moist) {
		if (DOLPHIN_SET_MOISTNESS != null) {
			try {
				DOLPHIN_SET_MOISTNESS.invoke(dolphin, moist);
			} catch (Throwable ignored) {
			}
		}
	}

	public static int getDolphinMoistness(org.bukkit.entity.Dolphin dolphin) {
		if (DOLPHIN_GET_MOISTNESS != null) {
			try {
				return (int) DOLPHIN_GET_MOISTNESS.invoke(dolphin);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setPiglinChargingCrossbow(org.bukkit.entity.Piglin piglin, boolean charging) {
		if (PIGLIN_SET_CHARGING_CROSSBOW != null) {
			try {
				PIGLIN_SET_CHARGING_CROSSBOW.invoke(piglin, charging);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isPiglinChargingCrossbow(org.bukkit.entity.Piglin piglin) {
		if (PIGLIN_IS_CHARGING_CROSSBOW != null) {
			try {
				return (boolean) PIGLIN_IS_CHARGING_CROSSBOW.invoke(piglin);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setPiglinDancing(org.bukkit.entity.Piglin piglin, boolean dancing) {
		if (PIGLIN_SET_DANCING != null) {
			try {
				PIGLIN_SET_DANCING.invoke(piglin, dancing);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isPiglinDancing(org.bukkit.entity.Piglin piglin) {
		if (PIGLIN_IS_DANCING != null) {
			try {
				return (boolean) PIGLIN_IS_DANCING.invoke(piglin);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static int getVexLimitedLifetimeTicks(org.bukkit.entity.Vex vex) {
		if (VEX_GET_LIMITED_LIFETIME != null) {
			try {
				return (int) VEX_GET_LIMITED_LIFETIME.invoke(vex);
			} catch (Throwable ignored) {
			}
		}
		if (VEX_GET_LIFETICKS != null) {
			try {
				return (int) VEX_GET_LIFETICKS.invoke(vex);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setVexLimitedLifetimeTicks(org.bukkit.entity.Vex vex, int ticks) {
		if (VEX_SET_LIMITED_LIFETIME != null) {
			try {
				VEX_SET_LIMITED_LIFETIME.invoke(vex, ticks);
			} catch (Throwable ignored) {
			}
		}
		if (VEX_SET_LIFETICKS != null) {
			try {
				VEX_SET_LIFETICKS.invoke(vex, ticks);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setVexSummoner(org.bukkit.entity.Vex vex, org.bukkit.entity.Mob summoner) {
		if (VEX_SET_SUMMONER != null) {
			try {
				VEX_SET_SUMMONER.invoke(vex, summoner);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static org.bukkit.entity.Mob getVexSummoner(org.bukkit.entity.Vex vex) {
		if (VEX_GET_SUMMONER != null) {
			try {
				return (org.bukkit.entity.Mob) VEX_GET_SUMMONER.invoke(vex);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	@Nullable
	public static Location getVexBound(org.bukkit.entity.Vex vex) {
		if (VEX_GET_BOUND != null) {
			try {
				return (Location) VEX_GET_BOUND.invoke(vex);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static void setVexBound(org.bukkit.entity.Vex vex, Location bound) {
		if (VEX_SET_BOUND != null) {
			try {
				VEX_SET_BOUND.invoke(vex, bound);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setEnderDragonPodium(org.bukkit.entity.EnderDragon dragon, Location podium) {
		if (ENDERDRAGON_SET_PODIUM != null) {
			try {
				ENDERDRAGON_SET_PODIUM.invoke(dragon, podium);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static Location getEnderDragonPodium(org.bukkit.entity.EnderDragon dragon) {
		if (ENDERDRAGON_GET_PODIUM != null) {
			try {
				return (Location) ENDERDRAGON_GET_PODIUM.invoke(dragon);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static boolean hasGoatLeftHorn(org.bukkit.entity.Goat goat) {
		if (GOAT_HAS_LEFT_HORN != null) {
			try {
				return (boolean) GOAT_HAS_LEFT_HORN.invoke(goat);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static boolean hasGoatRightHorn(org.bukkit.entity.Goat goat) {
		if (GOAT_HAS_RIGHT_HORN != null) {
			try {
				return (boolean) GOAT_HAS_RIGHT_HORN.invoke(goat);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setGoatLeftHorn(org.bukkit.entity.Goat goat, boolean value) {
		if (GOAT_SET_LEFT_HORN != null) {
			try {
				GOAT_SET_LEFT_HORN.invoke(goat, value);
			} catch (Throwable ignored) {
			}
		}
	}

	public static void setGoatRightHorn(org.bukkit.entity.Goat goat, boolean value) {
		if (GOAT_SET_RIGHT_HORN != null) {
			try {
				GOAT_SET_RIGHT_HORN.invoke(goat, value);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isSniffer(@NotNull org.bukkit.entity.Entity entity) {
		return SNIFFER_CLASS != null && SNIFFER_CLASS.isInstance(entity);
	}

	@Nullable
	public static String getSnifferState(org.bukkit.entity.Entity entity) {
		if (SNIFFER_GET_STATE != null && SNIFFER_CLASS != null && SNIFFER_CLASS.isInstance(entity)) {
			try {
				Object stateEnum = SNIFFER_GET_STATE.invoke(entity);
				return stateEnum != null ? stateEnum.toString() : null;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setSnifferState(org.bukkit.entity.Entity entity, @NotNull String stateName) {
		if (SNIFFER_SET_STATE != null && SNIFFER_CLASS != null && SNIFFER_STATE_ENUM != null
				&& SNIFFER_CLASS.isInstance(entity)) {
			try {
				Object enumValue = Enum.valueOf((Class<Enum>) SNIFFER_STATE_ENUM, stateName);
				SNIFFER_SET_STATE.invoke(entity, enumValue);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isCamel(@NotNull org.bukkit.entity.Entity entity) {
		return CAMEL_CLASS != null && CAMEL_CLASS.isInstance(entity);
	}

	public static boolean isCamelSitting(@NotNull org.bukkit.entity.Entity camel) {
		if (CAMEL_IS_SITTING != null && isCamel(camel)) {
			try {
				return (boolean) CAMEL_IS_SITTING.invoke(camel);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setCamelSitting(@NotNull org.bukkit.entity.Entity camel, boolean sitting) {
		if (CAMEL_SET_SITTING != null && isCamel(camel)) {
			try {
				CAMEL_SET_SITTING.invoke(camel, sitting);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static org.bukkit.inventory.Inventory getCamelInventory(@NotNull org.bukkit.entity.Entity camel) {
		if (CAMEL_GET_INVENTORY != null && isCamel(camel)) {
			try {
				return (org.bukkit.inventory.Inventory) CAMEL_GET_INVENTORY.invoke(camel);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	@Nullable
	public static ItemStack getCamelSaddle(@NotNull org.bukkit.entity.Entity camel) {
		org.bukkit.inventory.Inventory inv = getCamelInventory(camel);
		if (inv instanceof org.bukkit.inventory.AbstractHorseInventory horseInv) {
			return horseInv.getSaddle();
		}
		return null;
	}

	public static void setCamelSaddle(@NotNull org.bukkit.entity.Entity camel, @Nullable ItemStack saddle) {
		org.bukkit.inventory.Inventory inv = getCamelInventory(camel);
		if (CAMEL_SET_SADDLE != null && inv != null) {
			try {
				CAMEL_SET_SADDLE.invoke(inv, saddle);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isAllay(@NotNull org.bukkit.entity.Entity entity) {
		return ALLAY_CLASS != null && ALLAY_CLASS.isInstance(entity);
	}

	public static boolean isAllayDancing(@NotNull org.bukkit.entity.Entity allay) {
		if (ALLAY_IS_DANCING != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				return (boolean) ALLAY_IS_DANCING.invoke(allay);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static boolean canAllayDuplicate(@NotNull org.bukkit.entity.Entity allay) {
		if (ALLAY_CAN_DUPLICATE != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				return (boolean) ALLAY_CAN_DUPLICATE.invoke(allay);
			} catch (Throwable ignored) {
			}
		}
		return false;
	}

	public static void setAllayCanDuplicate(@NotNull org.bukkit.entity.Entity allay, boolean canDuplicate) {
		if (ALLAY_SET_CAN_DUPLICATE != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				ALLAY_SET_CAN_DUPLICATE.invoke(allay, canDuplicate);
			} catch (Throwable ignored) {
			}
		}
	}

	public static long getAllayDuplicationCooldown(@NotNull org.bukkit.entity.Entity allay) {
		if (ALLAY_GET_DUPLICATION_COOLDOWN != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				return (long) ALLAY_GET_DUPLICATION_COOLDOWN.invoke(allay);
			} catch (Throwable ignored) {
			}
		}
		return 0L;
	}

	public static void setAllayDuplicationCooldown(@NotNull org.bukkit.entity.Entity allay, long duplicateCooldown) {
		if (ALLAY_SET_DUPLICATION_COOLDOWN != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				ALLAY_SET_DUPLICATION_COOLDOWN.invoke(allay, duplicateCooldown);
			} catch (Throwable ignored) {
			}
		}
	}

	@Nullable
	public static Location getAllayJukeboxLocation(@NotNull org.bukkit.entity.Entity allay) {
		if (ALLAY_GET_JUKEBOX != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				return (Location) ALLAY_GET_JUKEBOX.invoke(allay);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static void allayStartDancing(@NotNull org.bukkit.entity.Entity allay, @NotNull Location jukeboxLoc) {
		if (ALLAY_START_DANCING != null && ALLAY_CLASS.isInstance(allay)) {
			try {
				ALLAY_START_DANCING.invoke(allay, jukeboxLoc);
			} catch (Throwable ignored) {
			}
		}
	}

	public static boolean isWarden(@NotNull org.bukkit.entity.Entity entity) {
		return WARDEN_CLASS != null && WARDEN_CLASS.isInstance(entity);
	}

	@Nullable
	public static org.bukkit.entity.Entity getWardenAngryAt(@NotNull org.bukkit.entity.Entity warden) {
		if (WARDEN_GET_ENTITY_ANGRY_AT != null && WARDEN_CLASS.isInstance(warden)) {
			try {
				return (org.bukkit.entity.Entity) WARDEN_GET_ENTITY_ANGRY_AT.invoke(warden);
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	public static int getWardenAnger(@NotNull org.bukkit.entity.Entity warden) {
		if (WARDEN_GET_ANGER_NO_ARGS != null && WARDEN_CLASS.isInstance(warden)) {
			try {
				return (int) WARDEN_GET_ANGER_NO_ARGS.invoke(warden);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static int getWardenAnger(@NotNull org.bukkit.entity.Entity warden,
			@NotNull org.bukkit.entity.Entity target) {
		if (WARDEN_GET_ANGER_WITH_ENTITY != null && WARDEN_CLASS.isInstance(warden)) {
			try {
				return (int) WARDEN_GET_ANGER_WITH_ENTITY.invoke(warden, target);
			} catch (Throwable ignored) {
			}
		}
		return 0;
	}

	public static void setWardenAnger(@NotNull org.bukkit.entity.Entity warden,
			@NotNull org.bukkit.entity.LivingEntity target, int anger) {
		if (WARDEN_SET_ANGER != null && WARDEN_CLASS.isInstance(warden)) {
			try {
				WARDEN_SET_ANGER.invoke(warden, target, anger);
			} catch (Throwable ignored) {
			}
		}
	}
}