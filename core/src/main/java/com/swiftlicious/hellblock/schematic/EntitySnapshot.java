package com.swiftlicious.hellblock.schematic;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Allay;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Cat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.GlowSquid;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Horse;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Mob;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Strider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Turtle;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.PotionEffectResolver;
import com.swiftlicious.hellblock.handlers.VersionHelper;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag.Builder;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;

/**
 * A fully serializable representation of a single entity in an IslandSnapshot.
 * Captures all known entity data such as health, inventory contents, equipment
 * and more.
 */
public record EntitySnapshot(@JsonProperty("type") EntityType type, @JsonProperty("uuid") UUID uuid,
		@JsonProperty("x") double x, @JsonProperty("y") double y, @JsonProperty("z") double z,
		@JsonProperty("data") CompoundBinaryTag data,
		@JsonProperty("equipment") Map<EquipmentSlot, CompoundBinaryTag> equipment) implements Serializable {

	@JsonCreator
	public EntitySnapshot {
		// ensure non-null data maps
		Objects.requireNonNull(type, "Entity type cannot be null");
		Objects.requireNonNull(uuid, "Entity UUID cannot be null");
		if (data == null) {
			data = CompoundBinaryTag.empty();
		}
		if (equipment == null) {
			equipment = new HashMap<>();
		}
	}

	/**
	 * Captures all relevant data from the given entity and returns it as a new
	 * EntitySnapshot instance. Does not capture player entities.
	 *
	 * @param entity The entity to capture.
	 * @return The captured entity snapshot.
	 */
	public static EntitySnapshot fromEntity(Entity entity) {
		if (entity instanceof Player) {
			throw new IllegalArgumentException("Player entities are not supported in snapshots.");
		}

		final CompoundBinaryTag.Builder dataBuilder = CompoundBinaryTag.builder();
		final Map<EquipmentSlot, CompoundBinaryTag> gear = new HashMap<>();

		dataBuilder.put("location", serializeLocation(entity.getLocation()));
		dataBuilder.putString("type", entity.getType().name());

		// --- Item Entity ---
		if (entity instanceof Item item) {
			dataBuilder.put("itemStack", itemStackToNBT(item.getItemStack()));
			dataBuilder.putInt("pickupDelay", item.getPickupDelay());
			dataBuilder.putInt("age", item.getTicksLived());
			dataBuilder.putBoolean("unlimitedLifetime", item.isUnlimitedLifetime());
		}

		// --- Item Frame / Glow Frame ---
		else if (entity instanceof ItemFrame frame) {
			dataBuilder.put("item", itemStackToNBT(frame.getItem()));
			dataBuilder.putString("facing", frame.getAttachedFace().name());
			dataBuilder.putString("rotation", frame.getRotation().name());
			dataBuilder.putBoolean("visible", frame.isVisible());
			dataBuilder.putBoolean("fixed", frame.isFixed());
		}

		// --- Experience Orb ---
		else if (entity instanceof ExperienceOrb orb) {
			dataBuilder.putInt("experience", orb.getExperience());
		}

		// --- TNT ---
		else if (entity instanceof TNTPrimed tnt) {
			dataBuilder.putInt("fuseTicks", tnt.getFuseTicks());
			if (tnt.getSource() != null && !(tnt.getSource() instanceof Player)) {
				dataBuilder.putString("source", tnt.getSource().getUniqueId().toString());
			}
		}

		// --- Falling Block ---
		else if (entity instanceof FallingBlock fallingBlock) {
			dataBuilder.putBoolean("cancelDrop", fallingBlock.getCancelDrop());
			dataBuilder.putString("blockData", fallingBlock.getBlockData().getAsString());
			dataBuilder.putBoolean("canHurtEntities", fallingBlock.canHurtEntities());
			dataBuilder.putFloat("damagePerBlock", fallingBlock.getDamagePerBlock());
			dataBuilder.putBoolean("dropItem", fallingBlock.getDropItem());
			dataBuilder.putInt("maxDamage", fallingBlock.getMaxDamage());

		}

		// --- Minecarts ---
		else if (entity instanceof Minecart minecart) {
			dataBuilder.putDouble("damage", minecart.getDamage());
			dataBuilder.putDouble("maxSpeed", minecart.getMaxSpeed());
			dataBuilder.putBoolean("slowWhenEmpty", minecart.isSlowWhenEmpty());
			dataBuilder.putString("displayBlock", minecart.getDisplayBlockData().getAsString());
			dataBuilder.putInt("displayOffset", minecart.getDisplayBlockOffset());

			switch (minecart.getType()) {
			case CHEST_MINECART, HOPPER_MINECART -> {
				final Inventory inv = ((InventoryHolder) minecart).getInventory();
				dataBuilder.put("contents", inventoryToNBT(inv));
			}
			case COMMAND_BLOCK_MINECART -> {
				final CommandMinecart cmd = (CommandMinecart) minecart;
				dataBuilder.putString("command", cmd.getCommand());
				Component name = AdventureMetadata.getCommandMinecartName(cmd);
				if (name != null) {
					dataBuilder.putString("name", AdventureHelper.getGson().serialize(name));
				}
			}
			case FURNACE_MINECART -> {
				final PoweredMinecart powered = (PoweredMinecart) minecart;
				dataBuilder.putInt("fuel", powered.getFuel());
			}
			case TNT_MINECART -> {
				final ExplosiveMinecart tnt = (ExplosiveMinecart) minecart;
				dataBuilder.putInt("fuseTicks", tnt.getFuseTicks());
			}
			default -> {
				/* no extra data */ }
			}
		}

		// --- Boats & Chest Boats ---
		else if (entity instanceof Boat boat) {
			String boatType = VersionHelper.isPaper() ? boat.getBoatMaterial().name()
					: boat.getType().name().replace("_BOAT", "");
			if (entity instanceof ChestBoat chestBoat) {
				boatType += "_CHEST";
				final Inventory inv = chestBoat.getInventory();
				dataBuilder.put("contents", inventoryToNBT(inv));
			}
			dataBuilder.putString("boatType", boatType);
		}

		// --- Armor Stand ---
		else if (entity instanceof ArmorStand stand) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				final ItemStack item = stand.getEquipment().getItem(slot);
				if (!item.getType().isAir()) {
					gear.put(slot, itemStackToNBT(item));
				}
			}

			dataBuilder.putBoolean("hasArms", stand.hasArms());
			dataBuilder.putBoolean("isSmall", stand.isSmall());
			dataBuilder.putBoolean("hasBasePlate", stand.hasBasePlate());
			dataBuilder.putBoolean("isInvisible", stand.isInvisible());
			dataBuilder.putBoolean("hasGravity", stand.hasGravity());
			dataBuilder.putBoolean("invulnerable", stand.isInvulnerable());
			dataBuilder.putBoolean("marker", stand.isMarker());

			if (VersionHelper.isPaper()) {
				if (!stand.getDisabledSlots().isEmpty()) {
					final List<String> disabled = stand.getDisabledSlots().stream().map(EquipmentSlot::name).toList();
					dataBuilder.put("disabledSlots", objectToTag(disabled));
				}
			}

			// Pose
			dataBuilder.put("headPose", eulerToNBT(stand.getHeadPose()));
			dataBuilder.put("bodyPose", eulerToNBT(stand.getBodyPose()));
			dataBuilder.put("leftArmPose", eulerToNBT(stand.getLeftArmPose()));
			dataBuilder.put("rightArmPose", eulerToNBT(stand.getRightArmPose()));
			dataBuilder.put("leftLegPose", eulerToNBT(stand.getLeftLegPose()));
			dataBuilder.put("rightLegPose", eulerToNBT(stand.getRightLegPose()));
		}

		// --- Abstract Horse + Subtypes ---
		else if (entity instanceof AbstractHorse horse) {
			dataBuilder.putInt("domestication", horse.getDomestication());
			dataBuilder.putDouble("jumpStrength", horse.getJumpStrength());
			dataBuilder.putBoolean("tamed", horse.isTamed());

			if (AdventureMetadata.getHorseOwnerUUID(horse) != null) {
				dataBuilder.putString("owner", AdventureMetadata.getHorseOwnerUUID(horse).toString());
			}

			// Saddle
			if (horse.getInventory().getSaddle() != null) {
				dataBuilder.put("saddle", itemStackToNBT(horse.getInventory().getSaddle()));
			}

			// Horse (color, style, armor)
			if (horse instanceof Horse normal) {
				dataBuilder.putString("color", normal.getColor().name());
				dataBuilder.putString("style", normal.getStyle().name());

				if (normal.getInventory().getArmor() != null) {
					dataBuilder.put("armor", itemStackToNBT(normal.getInventory().getArmor()));
				}
			}

			// Llama
			if (horse instanceof Llama llama) {
				dataBuilder.putString("color", llama.getColor().name());
				dataBuilder.putInt("strength", llama.getStrength());

				if (llama.getInventory().getDecor() != null) {
					dataBuilder.put("decor", itemStackToNBT(llama.getInventory().getDecor()));
				}

				dataBuilder.put("chestContents", inventoryToNBT(llama.getInventory()));
			}

			// Camel
			if (horse instanceof Camel camel) {
				dataBuilder.putBoolean("saddled", camel.getInventory().getSaddle() != null);
				if (camel.getInventory().getSaddle() != null) {
					dataBuilder.put("saddle", itemStackToNBT(camel.getInventory().getSaddle()));
				}
				dataBuilder.putBoolean("sitting", camel.isSitting());
			}

			// Donkey/Mule
			if (horse instanceof ChestedHorse chested) {
				dataBuilder.putBoolean("chested", chested.isCarryingChest());
				if (chested.isCarryingChest()) {
					dataBuilder.put("chestContents", inventoryToNBT(chested.getInventory()));
				}
			}

			// Skeleton Horse
			if (horse instanceof SkeletonHorse skeleton) {
				dataBuilder.putBoolean("trapped", skeleton.isTrapped());
			}
		}

		// --- Villager ---
		else if (entity instanceof Villager villager && !(villager instanceof WanderingTrader)) {
			dataBuilder.putString("profession", villager.getProfession().toString());
			dataBuilder.putInt("level", villager.getVillagerLevel());
			dataBuilder.putString("type", villager.getVillagerType().toString());
			dataBuilder.putInt("experience", villager.getVillagerExperience());
			if (VersionHelper.isPaper()) {
				dataBuilder.putInt("restocksToday", villager.getRestocksToday());
			}

			// Sleeping + bed location
			if (villager.isSleeping()) {
				dataBuilder.putBoolean("sleeping", true);

				final Block center = villager.getLocation().getBlock();
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							final Block b = center.getRelative(dx, dy, dz);
							if (Tag.BEDS.isTagged(b.getType())) {
								dataBuilder.put("sleepLocation", serializeLocation(b.getLocation()));
								break;
							}
						}
					}
				}
			}

			// Reputations (Paper) ATM the spigot system is not very well versed to store
			// this data.
			if (VersionHelper.isPaper()) {
				final List<Map<String, Object>> reps = new ArrayList<>();
				villager.getReputations().keySet().forEach(uuid -> {
					final com.destroystokyo.paper.entity.villager.Reputation rep = villager.getReputation(uuid);
					if (rep != null) {
						for (com.destroystokyo.paper.entity.villager.ReputationType type : com.destroystokyo.paper.entity.villager.ReputationType
								.values()) {
							final int value = rep.getReputation(type);
							if (value != 0) {
								final Map<String, Object> entry = new HashMap<>();
								entry.put("uuid", uuid.toString());
								entry.put("type", type.name());
								entry.put("value", value);
								reps.add(entry);
							}
						}
					}
				});
				if (!reps.isEmpty()) {
					dataBuilder.put("reputations", objectToTag(reps));
				}
			}

			// Trades
			final List<Map<String, Object>> trades = new ArrayList<>();
			villager.getRecipes().forEach(recipe -> {
				final Map<String, Object> trade = new HashMap<>();
				trade.put("result", itemStackToNBT(recipe.getResult()));
				trade.put("ingredients", recipe.getIngredients().stream().map(EntitySnapshot::itemStackToNBT).toList());
				trade.put("maxUses", recipe.getMaxUses());
				trade.put("uses", recipe.getUses());
				trade.put("experienceReward", recipe.hasExperienceReward());
				trade.put("villagerExp", recipe.getVillagerExperience());
				trade.put("priceMultiplier", recipe.getPriceMultiplier());
				trades.add(trade);
			});
			dataBuilder.put("trades", objectToTag(trades));
		}

		// --- Wandering Trader ---
		else if (entity instanceof WanderingTrader trader) {
			dataBuilder.putBoolean("wanderingTrader", true);
			dataBuilder.putInt("despawnDelay", trader.getDespawnDelay());

		}

		// --- Creeper ---
		else if (entity instanceof Creeper creeper) {
			dataBuilder.putBoolean("charged", creeper.isPowered());
			dataBuilder.putInt("fuseTicks", creeper.getFuseTicks());
			dataBuilder.putInt("maxFuseTicks", creeper.getMaxFuseTicks());
		}

		// --- Slime / MagmaCube ---
		else if (entity instanceof Slime slime) {
			dataBuilder.putInt("size", slime.getSize());
		}

		// --- Sheep ---
		else if (entity instanceof Sheep sheep) {
			if (sheep.getColor() != null) {
				dataBuilder.putString("color", sheep.getColor().name());
			}
			dataBuilder.putBoolean("sheared", sheep.isSheared());
		}

		// --- Shulker ---
		else if (entity instanceof Shulker shulker) {
			if (shulker.getColor() != null) {
				dataBuilder.putString("color", shulker.getColor().name());
			}
			dataBuilder.putFloat("peek", shulker.getPeek());
			dataBuilder.putString("attachedFace", shulker.getAttachedFace().name());
		}

		// --- Rabbit ---
		else if (entity instanceof Rabbit rabbit) {
			dataBuilder.putString("type", rabbit.getRabbitType().name());
		}

		// --- Witch ---
		else if (entity instanceof Witch witch) {
			dataBuilder.putBoolean("isDrinkingPotion", witch.isDrinkingPotion());
			final ItemStack potion = VersionHelper.isPaper() ? witch.getDrinkingPotion()
					: getWitchDrinkingPotion(witch);
			if (potion != null) {
				dataBuilder.put("drinkingPotion", itemStackToNBT(potion));
			}
		}

		// --- Axolotl ---
		else if (entity instanceof Axolotl axolotl) {
			dataBuilder.putString("variant", axolotl.getVariant().name());
			dataBuilder.putBoolean("playingDead", axolotl.isPlayingDead());
		}

		// --- Ghast ---
		else if (entity instanceof Ghast ghast) {
			dataBuilder.putBoolean("charging", ghast.isCharging());
		}

		// --- Fox ---
		else if (entity instanceof Fox fox) {
			dataBuilder.putString("type", fox.getFoxType().name());
			dataBuilder.putBoolean("sitting", fox.isSitting());
			dataBuilder.putBoolean("sleeping", fox.isSleeping());
			dataBuilder.putBoolean("crouching", fox.isCrouching());
			dataBuilder.putBoolean("faceplanted", fox.isFaceplanted());

			if (fox.getFirstTrustedPlayer() != null) {
				dataBuilder.putString("trustedFirst", fox.getFirstTrustedPlayer().getUniqueId().toString());
			}
			if (fox.getSecondTrustedPlayer() != null) {
				dataBuilder.putString("trustedSecond", fox.getSecondTrustedPlayer().getUniqueId().toString());
			}
		}

		// --- Panda ---
		else if (entity instanceof Panda panda) {
			dataBuilder.putString("mainGene", panda.getMainGene().name());
			dataBuilder.putString("hiddenGene", panda.getHiddenGene().name());
			dataBuilder.putBoolean("sneezing", panda.isSneezing());
			dataBuilder.putBoolean("rolling", panda.isRolling());
			dataBuilder.putBoolean("sitting", panda.isSitting());
		}

		// --- Piglin ---
		else if (entity instanceof Piglin piglin) {
			dataBuilder.putBoolean("immuneToZombification", piglin.isImmuneToZombification());
			dataBuilder.putBoolean("ableToHunt", piglin.isAbleToHunt());
			if (VersionHelper.isPaper()) {
				dataBuilder.putBoolean("chargingCrossbow", piglin.isChargingCrossbow());
				dataBuilder.putBoolean("dancing", piglin.isDancing());
			}
		}

		// --- Zombie Villager ---
		else if (entity instanceof ZombieVillager zombieVillager) {
			dataBuilder.putBoolean("zombieVillager", true);
			dataBuilder.putString("profession", zombieVillager.getVillagerProfession().toString());
			dataBuilder.putString("type", zombieVillager.getVillagerType().toString());
			dataBuilder.putBoolean("canBreakDoors", zombieVillager.canBreakDoors());

			if (zombieVillager.getConversionPlayer() != null) {
				dataBuilder.putBoolean("converting", true);
				dataBuilder.putInt("conversionTime", zombieVillager.getConversionTime());
				dataBuilder.putString("convertedBy", zombieVillager.getConversionPlayer().getUniqueId().toString());
			}
		}

		// --- Standard Zombie (not ZV) ---
		else if (entity instanceof Zombie zombie && !(zombie instanceof ZombieVillager)) {
			dataBuilder.putBoolean("zombie", true);
			dataBuilder.putBoolean("breakDoors", zombie.canBreakDoors());

			if (zombie.getConversionTime() >= 0) {
				dataBuilder.putBoolean("converting", true);
				dataBuilder.putInt("conversionTime", zombie.getConversionTime());
			}
		}

		// --- Hoglin ---
		else if (entity instanceof Hoglin hoglin) {
			dataBuilder.putBoolean("immuneToZombification", hoglin.isImmuneToZombification());
		}

		// --- Dolphin ---
		else if (VersionHelper.isPaper() && entity instanceof Dolphin dolphin) {
			dataBuilder.putBoolean("hasFish", dolphin.hasFish());
			dataBuilder.putInt("moistness", dolphin.getMoistness());
		}

		// --- Puffer Fish ---
		else if (entity instanceof PufferFish pufferFish) {
			dataBuilder.putInt("puffState", pufferFish.getPuffState());
		}

		// --- Tropical Fish ---
		else if (entity instanceof TropicalFish tropicalFish) {
			dataBuilder.putString("pattern", tropicalFish.getPattern().name());
			dataBuilder.putString("patternColor", tropicalFish.getPatternColor().name());
			dataBuilder.putString("bodyColor", tropicalFish.getBodyColor().name());
		}

		// --- Salmon ---
		else if (entity instanceof Salmon salmon) {
			dataBuilder.putString("variant", salmon.getVariant().name());
		}

		// --- Glow Squid ---
		else if (entity instanceof GlowSquid glowSquid) {
			dataBuilder.putInt("darkTicksRemaining", glowSquid.getDarkTicksRemaining());
		}

		// --- Frog (1.19.3+) ---
		else if (VersionHelper.isVersionNewerThan1_19_3() && entity instanceof Frog frog) {
			dataBuilder.putString("variant", frog.getVariant().toString());
		}

		// --- Vindicator ---
		else if (entity instanceof Vindicator vindicator) {
			dataBuilder.putBoolean("johnny", vindicator.isJohnny());
		}

		// --- Vex ---
		else if (entity instanceof Vex vex) {
			if (VersionHelper.isPaper()) {
				if (vex.getSummoner() != null) {
					dataBuilder.putString("summoner", vex.getSummoner().getUniqueId().toString());
				}
			}
			dataBuilder.putInt("limitedLifetimeTicks", AdventureMetadata.getVexLifetimeTicks(vex));
			dataBuilder.putBoolean("charging", vex.isCharging());
			if (vex.getBound() != null) {
				dataBuilder.put("bound", serializeLocation(vex.getBound()));
			}
		}

		// --- Guardian ---
		else if (entity instanceof Guardian guardian) {
			dataBuilder.putBoolean("hasLaser", guardian.hasLaser());
			dataBuilder.putInt("laserTicks", guardian.getLaserTicks());
		}

		// --- Tameable Mobs ---
		else if (entity instanceof Tameable tameable && !(entity instanceof AbstractHorse)) {
			dataBuilder.putBoolean("tamed", tameable.isTamed());

			if (tameable.isTamed() && tameable.getOwner() != null) {
				dataBuilder.putString("owner", tameable.getOwner().getUniqueId().toString());
			}

			// --- Wolf ---
			if (tameable instanceof Wolf wolf) {
				dataBuilder.putString("collar", wolf.getCollarColor().name());
				dataBuilder.putBoolean("angry", wolf.isAngry());
				dataBuilder.putBoolean("sitting", wolf.isSitting());
				dataBuilder.putBoolean("begging", wolf.isInterested());
				dataBuilder.putString("variant", wolf.getVariant().getKey().toString());
				if (VersionHelper.isPaper()) {
					dataBuilder.putString("soundVariant", wolf.getSoundVariant().getKey().toString());
				}
			}

			// --- Cat ---
			else if (tameable instanceof Cat cat) {
				dataBuilder.putString("type", cat.getCatType().toString());
				dataBuilder.putBoolean("sitting", cat.isSitting());
				dataBuilder.putString("collar", cat.getCollarColor().name());
				if (VersionHelper.isPaper()) {
					dataBuilder.putBoolean("headUp", cat.isHeadUp());
					dataBuilder.putBoolean("lyingDown", cat.isLyingDown());
				}
			}

			// --- Parrot ---
			else if (tameable instanceof Parrot parrot) {
				dataBuilder.putBoolean("sitting", parrot.isSitting());
				dataBuilder.putString("variant", parrot.getVariant().name());
			}
		}

		// --- Mooshroom ---
		else if (entity instanceof MushroomCow cow) {
			dataBuilder.putString("variant", cow.getVariant().name());
		}

		// --- Ocelot ---
		else if (entity instanceof Ocelot ocelot) {
			dataBuilder.putBoolean("trusting", ocelot.isTrusting());
		}

		// --- Pig ---
		else if (entity instanceof Pig pig) {
			dataBuilder.putBoolean("saddled", pig.hasSaddle());
			dataBuilder.putString("variant", pig.getVariant().toString());
		}

		// --- Cow ---
		else if (entity instanceof Cow cow) {
			dataBuilder.putString("variant", cow.getVariant().toString());
		}

		// --- Chicken ---
		else if (entity instanceof Chicken chicken) {
			dataBuilder.putString("variant", chicken.getVariant().toString());
		}

		// --- Strider ---
		else if (entity instanceof Strider strider) {
			dataBuilder.putBoolean("saddled", strider.hasSaddle());
			dataBuilder.putBoolean("shivering", strider.isShivering());
		}

		// --- Goat ---
		else if (entity instanceof Goat goat) {
			dataBuilder.putBoolean("screaming", goat.isScreaming());
			dataBuilder.putBoolean("leftHorn", goat.hasLeftHorn());
			dataBuilder.putBoolean("rightHorn", goat.hasRightHorn());
		}

		// --- Allay ---
		else if (entity instanceof Allay allay) {
			dataBuilder.putBoolean("dancing", allay.isDancing());
			dataBuilder.putBoolean("duplicate", allay.canDuplicate());
			dataBuilder.putLong("duplicationCooldown", allay.getDuplicationCooldown());
			if (allay.getJukebox() != null) {
				dataBuilder.put("jukebox", serializeLocation(allay.getJukebox()));
			}
		}

		// --- Painting ---
		else if (entity instanceof Painting painting) {
			dataBuilder.putString("art", painting.getArt().toString());
			dataBuilder.putString("facing", painting.getFacing().name());
		}

		// --- Leash Hitch ---
		else if (entity instanceof LeashHitch hitch) {
			dataBuilder.putString("facing", hitch.getAttachedFace().name());
			dataBuilder.put("block", serializeLocation(hitch.getLocation().getBlock().getLocation()));
		}

		// --- Bee ---
		else if (entity instanceof Bee bee) {
			dataBuilder.putBoolean("hasNectar", bee.hasNectar());
			dataBuilder.putBoolean("hasStung", bee.hasStung());
			dataBuilder.putInt("anger", bee.getAnger());
			if (bee.getHive() != null) {
				dataBuilder.put("hive", serializeLocation(bee.getHive()));
			}
			if (bee.getFlower() != null) {
				dataBuilder.put("flower", serializeLocation(bee.getFlower()));
			}
		}

		// --- Turtle ---
		else if (entity instanceof Turtle turtle) {
			dataBuilder.putBoolean("hasEgg", turtle.hasEgg());
			if (VersionHelper.isPaper()) {
				dataBuilder.put("home", serializeLocation(turtle.getHome()));
			}
		}

		// --- Enderman ---
		else if (entity instanceof Enderman enderman) {
			if (enderman.getCarriedBlock() != null) {
				dataBuilder.putString("carriedBlock", enderman.getCarriedBlock().getAsString());
			}
			if (VersionHelper.isPaper()) {
				dataBuilder.putBoolean("screaming", enderman.isScreaming());
			}
		}

		// --- Snowman ---
		else if (entity instanceof Snowman snowman) {
			dataBuilder.putBoolean("pumpkin", snowman.isDerp());
		}

		// --- Phantom ---
		else if (entity instanceof Phantom phantom) {
			dataBuilder.putInt("size", phantom.getSize());
		}

		// --- Wither ---
		else if (entity instanceof Wither wither) {
			dataBuilder.putInt("invulnerableTicks", AdventureMetadata.getWitherInvulnerableTicks(wither));
		}

		// --- Warden ---
		else if (entity instanceof Warden warden) {
			dataBuilder.putInt("anger", warden.getAnger());

			if (warden.getAnger() > 0 && warden.getEntityAngryAt() != null) {
				dataBuilder.putString("angerTarget", warden.getEntityAngryAt().getUniqueId().toString());
				dataBuilder.putInt("angerLevel", warden.getAnger());
			}
		}

		// --- Iron Golem ---
		else if (entity instanceof IronGolem golem) {
			dataBuilder.putBoolean("playerCreated", golem.isPlayerCreated());
		}

		// --- Bat ---
		else if (entity instanceof Bat bat) {
			dataBuilder.putBoolean("hanging", !bat.isAwake());
		}

		// --- Ender Dragon ---
		else if (entity instanceof EnderDragon dragon) {
			if (VersionHelper.isPaper()) {
				dataBuilder.put("podium", serializeLocation(dragon.getPodium()));
			}
			dataBuilder.putString("phase", dragon.getPhase().name());
		}

		// --- Shared LivingEntity Properties ---
		if (entity instanceof LivingEntity living) {
			// Core stats
			dataBuilder.putDouble("health", living.getHealth());

			final RtagEntity tagged = new RtagEntity(living);
			final double maxHealth = tagged.getAttributeBase("generic.max_health");
			dataBuilder.putDouble("maxHealth", maxHealth);

			dataBuilder.putDouble("absorption", living.getAbsorptionAmount());
			dataBuilder.putInt("remainingAir", living.getRemainingAir());
			dataBuilder.putInt("fireTicks", living.getFireTicks());
			dataBuilder.putInt("freezeTicks", living.getFreezeTicks());
			dataBuilder.putBoolean("glowing", living.isGlowing());
			dataBuilder.putBoolean("gravity", living.hasGravity());
			dataBuilder.putBoolean("invulnerable", living.isInvulnerable());
			dataBuilder.putBoolean("silent", living.isSilent());
			dataBuilder.putBoolean("collidable", living.isCollidable());

			// Custom name
			Component customName = AdventureMetadata.getEntityCustomName(living);
			if (customName != null) {
				dataBuilder.putString("customName", AdventureHelper.getGson().serialize(customName));
				dataBuilder.putBoolean("customNameVisible", living.isCustomNameVisible());
			}

			// Potion effects
			if (!living.getActivePotionEffects().isEmpty()) {
				final List<Map<String, Object>> effects = new ArrayList<>();
				living.getActivePotionEffects().forEach(effect -> {
					final Map<String, Object> effData = new HashMap<>();
					effData.put("type", effect.getType().getKey().toString());
					effData.put("duration", effect.getDuration());
					effData.put("amplifier", effect.getAmplifier());
					effData.put("ambient", effect.isAmbient());
					effData.put("particles", effect.hasParticles());
					effData.put("icon", effect.hasIcon());
					effects.add(effData);
				});
				dataBuilder.put("effects", objectToTag(effects));
			}

			// Equipment (armor, hand)
			final CompoundBinaryTag equipmentTag = captureEquipment(living);
			if (equipmentTag != null && !equipmentTag.isEmpty()) {
				dataBuilder.put("equipment", equipmentTag);
			}

			// Leash
			if (living.isLeashed()) {
				final Entity holder = living.getLeashHolder();
				dataBuilder.putString("leashHolder", holder.getUniqueId().toString());
			}
		}

		// --- Passengers (exclude players) ---
		if (!entity.getPassengers().isEmpty()) {
			final List<EntitySnapshot> passengers = entity.getPassengers().stream()
					.filter(passenger -> !(passenger instanceof Player)).map(EntitySnapshot::fromEntity).toList();

			if (!passengers.isEmpty()) {
				dataBuilder.put("passengers", objectToTag(passengers));
			}
		}

		// --- Vehicle (exclude player mounts) ---
		if (entity.getVehicle() != null && !(entity.getVehicle() instanceof Player)) {
			final EntitySnapshot vehicleSnap = EntitySnapshot.fromEntity(entity.getVehicle());
			dataBuilder.put("vehicle", objectToTag(vehicleSnap));
		}

		// --- Persistent Data Container (PDC) ---
		final PersistentDataContainer pdc = entity.getPersistentDataContainer();
		if (!pdc.isEmpty()) {
			final Map<String, Object> pdcMap = new HashMap<>();
			pdc.getKeys().forEach(key -> {
				final String val = pdc.get(key, PersistentDataType.STRING);
				if (val != null) {
					pdcMap.put(key.toString(), val);
				}
			});
			if (!pdcMap.isEmpty()) {
				dataBuilder.put("persistentData", objectToTag(pdcMap));
			}
		}

		// --- Ageable Mobs ---
		if (entity instanceof Ageable ageable) {
			dataBuilder.putBoolean("ageable", true);
			dataBuilder.putInt("age", ageable.getAge());
			dataBuilder.putBoolean("adult", ageable.isAdult());
		}

		// --- Final snapshot creation ---
		return new EntitySnapshot(entity.getType(), entity.getUniqueId(), entity.getLocation().getX(),
				entity.getLocation().getY(), entity.getLocation().getZ(),
				dataBuilder.build().isEmpty() ? null : dataBuilder.build(), gear.isEmpty() ? null : gear);
	}

	/**
	 * Spawns this entity snapshot in the given world, applying all stored data such
	 * as health, inventory contents, equipment and more.
	 *
	 * @param world The world to spawn the entity in.
	 * @return The spawned entity.
	 */
	@SuppressWarnings("deprecation")
	public Entity spawn(World world) {
		final EntityType entityType = EntityType.valueOf(data.getString("type"));
		final Location spawnLoc = deserializeLocation(data.getCompound("location"));
		final Entity entity;

		// Handle hanging entities
		if (Hanging.class.isAssignableFrom(entityType.getEntityClass())) {
			final String facingStr = getStringOrDefault(data, "facing", "NORTH");
			final BlockFace face = BlockFace.valueOf(facingStr);

			if (entityType == EntityType.ITEM_FRAME || entityType == EntityType.GLOW_ITEM_FRAME) {
				entity = world.spawn(spawnLoc, ItemFrame.class, frame -> {
					frame.setFacingDirection(face, true);
					if (data.get("item") instanceof CompoundBinaryTag itemTag) {
						frame.setItem(itemStackFromNBT(itemTag));
					}
					frame.setRotation(getEnumOrDefault(data, "rotation", Rotation.class, frame.getRotation()));
					frame.setVisible(getBooleanOrDefault(data, "visible", frame.isVisible()));
					frame.setFixed(getBooleanOrDefault(data, "fixed", frame.isFixed()));
				});
			} else if (entityType == EntityType.PAINTING) {
				entity = world.spawn(spawnLoc, Painting.class, painting -> {
					painting.setFacingDirection(face, true);
					if (data.get("art") != null) {
						painting.setArt(ItemRegistry.getPaintingVariant(data.getString("art")), true);
					}
				});
			} else if (entityType == EntityType.LEASH_KNOT && data.get("block") != null) {
				final Location loc = deserializeLocation(data.getCompound("block"));
				entity = world.spawn(loc, LeashHitch.class, hitch -> hitch.setFacingDirection(face, true));
			} else {
				entity = world.spawn(spawnLoc, entityType.getEntityClass());
			}
		}

		// Handle boats (and rafts)
		else if (Boat.class.isAssignableFrom(entityType.getEntityClass())) {
			final String boatTypeName = getStringOrDefault(data, "boatType", "OAK");

			final Map<String, EntityType> boatTypes = Map.ofEntries(Map.entry("OAK", EntityType.OAK_BOAT),
					Map.entry("SPRUCE", EntityType.SPRUCE_BOAT), Map.entry("BIRCH", EntityType.BIRCH_BOAT),
					Map.entry("JUNGLE", EntityType.JUNGLE_BOAT), Map.entry("ACACIA", EntityType.ACACIA_BOAT),
					Map.entry("DARK_OAK", EntityType.DARK_OAK_BOAT), Map.entry("MANGROVE", EntityType.MANGROVE_BOAT),
					Map.entry("CHERRY", EntityType.CHERRY_BOAT), Map.entry("BAMBOO", EntityType.BAMBOO_RAFT),
					Map.entry("OAK_CHEST", EntityType.OAK_CHEST_BOAT),
					Map.entry("SPRUCE_CHEST", EntityType.SPRUCE_CHEST_BOAT),
					Map.entry("BIRCH_CHEST", EntityType.BIRCH_CHEST_BOAT),
					Map.entry("JUNGLE_CHEST", EntityType.JUNGLE_CHEST_BOAT),
					Map.entry("ACACIA_CHEST", EntityType.ACACIA_CHEST_BOAT),
					Map.entry("DARK_OAK_CHEST", EntityType.DARK_OAK_CHEST_BOAT),
					Map.entry("MANGROVE_CHEST", EntityType.MANGROVE_CHEST_BOAT),
					Map.entry("CHERRY_CHEST", EntityType.CHERRY_CHEST_BOAT),
					Map.entry("BAMBOO_CHEST", EntityType.BAMBOO_CHEST_RAFT));

			final EntityType boatType = boatTypes.get(boatTypeName);
			if (boatType == null) {
				throw new IllegalArgumentException("Unknown boat type: " + boatTypeName);
			}

			entity = world.spawn(spawnLoc, boatType.getEntityClass(), boat -> {
				if (boat instanceof ChestBoat chestBoat
						&& data.get("contents") instanceof CompoundBinaryTag contentsTag) {
					inventoryFromNBT(contentsTag, ((InventoryHolder) chestBoat).getInventory());
				}
			});
		}

		// Falling blocks
		else if (entityType == EntityType.FALLING_BLOCK) {
			final BlockData blockData;

			if (data.get("blockData") != null) {
				blockData = Bukkit.createBlockData(data.getString("blockData"));
			} else {
				throw new IllegalStateException("No block data found for FallingBlock");
			}

			FallingBlock fb = world.spawnFallingBlock(spawnLoc, blockData);
			fb.setCancelDrop(getBooleanOrDefault(data, "cancelDrop", fb.getCancelDrop()));
			fb.setHurtEntities(getBooleanOrDefault(data, "canHurtEntities", fb.canHurtEntities()));
			fb.setDamagePerBlock(getFloatOrDefault(data, "damagePerBlock", fb.getDamagePerBlock()));
			fb.setDropItem(getBooleanOrDefault(data, "dropItem", fb.getDropItem()));
			fb.setMaxDamage(getIntOrDefault(data, "maxDamage", fb.getMaxDamage()));
			entity = fb;
		}

		// All other entities
		else {
			entity = world.spawn(spawnLoc, entityType.getEntityClass());
		}

		// Post-spawn entity configuration
		postSpawnConfiguration(entity, data);

		return entity;
	}

	/**
	 * Post-spawn configuration for specific entity types
	 *
	 * @param entity The entity to configure
	 * @param data   The serialized data
	 */
	@SuppressWarnings("unchecked")
	private void postSpawnConfiguration(Entity entity, CompoundBinaryTag data) {
		if (entity instanceof Item item) {
			if (data.get("itemStack") instanceof CompoundBinaryTag itemTag)
				item.setItemStack(itemStackFromNBT(itemTag));

			item.setPickupDelay(getIntOrDefault(data, "pickupDelay", item.getPickupDelay()));
			item.setTicksLived(getIntOrDefault(data, "age", item.getTicksLived()));
			item.setUnlimitedLifetime(getBooleanOrDefault(data, "unlimitedLifetime", item.isUnlimitedLifetime()));
		}

		else if (entity instanceof ExperienceOrb orb) {
			orb.setExperience(getIntOrDefault(data, "experience",
					VersionHelper.isPaper() ? orb.getCount() : orb.getExperience())); // Default XP to 1
		}

		else if (entity instanceof TNTPrimed tnt) {
			tnt.setFuseTicks(getIntOrDefault(data, "fuseTicks", tnt.getFuseTicks())); // Default fuseTicks to 80
			if (data.get("source") != null) {
				final UUID sourceId = UUID.fromString(data.getString("source"));
				final Entity sourceEntity = Bukkit.getEntity(sourceId);
				if (sourceEntity != null) {
					tnt.setSource(sourceEntity);
				}
			}
		}

		else if (entity instanceof ArmorStand stand) {
			if (equipment != null) {
				equipment.forEach((slot, gear) -> stand.getEquipment().setItem(slot, itemStackFromNBT(gear)));
			}

			stand.setArms(getBooleanOrDefault(data, "hasArms", stand.hasArms()));
			stand.setSmall(getBooleanOrDefault(data, "isSmall", stand.isSmall()));
			stand.setBasePlate(getBooleanOrDefault(data, "hasBasePlate", stand.hasBasePlate()));
			stand.setInvisible(getBooleanOrDefault(data, "isInvisible", stand.isInvisible()));
			stand.setGravity(getBooleanOrDefault(data, "hasGravity", stand.hasGravity()));
			stand.setInvulnerable(getBooleanOrDefault(data, "invulnerable", stand.isInvulnerable()));
			stand.setMarker(getBooleanOrDefault(data, "marker", stand.isMarker()));

			if (VersionHelper.isPaper()) {
				if (data.get("disabledSlots") != null) {
					final List<String> slots = (List<String>) tagToObject(data.get("disabledSlots"));
					stand.setDisabledSlots(slots.stream().map(EquipmentSlot::valueOf).toArray(EquipmentSlot[]::new));
				}
			}

			if (data.get("headPose") instanceof CompoundBinaryTag pose) {
				stand.setHeadPose(eulerFromNBT(pose));
			}
			if (data.get("bodyPose") instanceof CompoundBinaryTag pose) {
				stand.setBodyPose(eulerFromNBT(pose));
			}
			if (data.get("leftArmPose") instanceof CompoundBinaryTag pose) {
				stand.setLeftArmPose(eulerFromNBT(pose));
			}
			if (data.get("rightArmPose") instanceof CompoundBinaryTag pose) {
				stand.setRightArmPose(eulerFromNBT(pose));
			}
			if (data.get("leftLegPose") instanceof CompoundBinaryTag pose) {
				stand.setLeftLegPose(eulerFromNBT(pose));
			}
			if (data.get("rightLegPose") instanceof CompoundBinaryTag pose) {
				stand.setRightLegPose(eulerFromNBT(pose));
			}
		}

		else if (entity instanceof Minecart minecart) {
			minecart.setDamage(getDoubleOrDefault(data, "damage", minecart.getDamage()));
			minecart.setMaxSpeed(getDoubleOrDefault(data, "maxSpeed", minecart.getMaxSpeed()));
			minecart.setSlowWhenEmpty(getBooleanOrDefault(data, "slowWhenEmpty", minecart.isSlowWhenEmpty()));
			minecart.setDisplayBlockData(Bukkit.createBlockData(data.getString("displayBlock")));
			minecart.setDisplayBlockOffset(getIntOrDefault(data, "displayOffset", minecart.getDisplayBlockOffset()));

			switch (minecart.getType()) {
			case CHEST_MINECART, HOPPER_MINECART -> {
				if (data.get("contents") instanceof CompoundBinaryTag contentsTag) {
					inventoryFromNBT(contentsTag, ((InventoryHolder) minecart).getInventory());
				}
			}
			case COMMAND_BLOCK_MINECART -> {
				final CommandMinecart cmd = (CommandMinecart) minecart;
				if (data.get("command") != null) {
					cmd.setCommand(data.getString("command"));
				}
				if (data.get("name") != null) {
					Component name = AdventureHelper.getGson().deserialize(data.getString("name"));
					AdventureMetadata.setCommandMinecartName(cmd, name);
				}
			}
			case FURNACE_MINECART -> {
				if (data.get("fuel") != null) {
					final PoweredMinecart pm = (PoweredMinecart) minecart;
					pm.setFuel(getIntOrDefault(data, "fuel", pm.getFuel()));
				}
			}
			case TNT_MINECART -> {
				if (data.get("fuseTicks") != null) {
					final ExplosiveMinecart em = (ExplosiveMinecart) minecart;
					em.setFuseTicks(getIntOrDefault(data, "fuseTicks", em.getFuseTicks()));
				}
			}
			default -> {
				/* No special handling required */
			}
			}
		}

		else if (entity instanceof AbstractHorse horse) {
			horse.setDomestication(getIntOrDefault(data, "domestication", horse.getDomestication()));
			horse.setJumpStrength(getDoubleOrDefault(data, "jumpStrength", horse.getJumpStrength()));
			horse.setTamed(getBooleanOrDefault(data, "tamed", horse.isTamed()));

			if (data.get("owner") != null) {
				final UUID uuid = UUID.fromString(data.getString("owner"));
				horse.setOwner(Bukkit.getOfflinePlayer(uuid));
			}

			if (data.get("saddle") instanceof CompoundBinaryTag saddleTag) {
				horse.getInventory().setSaddle(itemStackFromNBT(saddleTag));
			}

			if (horse instanceof Horse normalHorse) {
				if (data.get("armor") instanceof CompoundBinaryTag armorTag) {
					normalHorse.getInventory().setArmor(itemStackFromNBT(armorTag));
				}
				normalHorse.setColor(Horse.Color.valueOf(data.getString("color")));
				normalHorse.setStyle(Horse.Style.valueOf(data.getString("style")));
			}

			if (horse instanceof Llama llama) {
				llama.setColor(Llama.Color.valueOf(data.getString("color")));
				llama.setStrength(getIntOrDefault(data, "strength", llama.getStrength()));

				if (data.get("decor") instanceof CompoundBinaryTag decorTag) {
					llama.getInventory().setDecor(itemStackFromNBT(decorTag));
				}
				if (data.get("chestContents") instanceof CompoundBinaryTag chestTag) {
					inventoryFromNBT(chestTag, llama.getInventory());
				}
			}

			if (horse instanceof Camel camel) {
				if (data.get("saddle") instanceof CompoundBinaryTag saddleTag) {
					camel.getInventory().setSaddle(itemStackFromNBT(saddleTag));
				}
				camel.setSitting(getBooleanOrDefault(data, "sitting", camel.isSitting()));
			}

			if (horse instanceof ChestedHorse chested && getBooleanOrDefault(data, "chested", false)) {
				chested.setCarryingChest(true);
				if (data.get("chestContents") instanceof CompoundBinaryTag chestTag) {
					inventoryFromNBT(chestTag, chested.getInventory());
				}
			}

			if (horse instanceof SkeletonHorse skeletonHorse) {
				skeletonHorse.setTrapped(getBooleanOrDefault(data, "trapped", skeletonHorse.isTrapped()));
			}
		}

		else if (entity instanceof Villager villager && !(villager instanceof WanderingTrader)) {
			villager.setProfession(ItemRegistry.getVillagerProfession(data.getString("profession")));
			villager.setVillagerType(ItemRegistry.getVillagerType(data.getString("type")));
			villager.setVillagerLevel(getIntOrDefault(data, "level", villager.getVillagerLevel()));
			villager.setVillagerExperience(getIntOrDefault(data, "experience", villager.getVillagerExperience()));
			if (VersionHelper.isPaper()) {
				villager.setRestocksToday(getIntOrDefault(data, "restocksToday", villager.getRestocksToday()));
			}

			if (getBooleanOrDefault(data, "sleeping", villager.isSleeping())
					&& data.get("sleepLocation") instanceof CompoundBinaryTag locTag) {
				final Location bedLoc = deserializeLocation(locTag);
				if (bedLoc != null) {
					villager.teleport(bedLoc.clone().add(0.5, 0.2, 0.5));
					villager.sleep(bedLoc);
				}
			}

			// Reputation (Paper only)
			if (VersionHelper.isPaper()) {
				if (data.get("reputations") != null) {
					final List<Map<String, Object>> reps = (List<Map<String, Object>>) tagToObject(
							data.get("reputations"));
					reps.forEach(entry -> {
						try {
							final UUID uuid = UUID.fromString((String) entry.get("uuid"));
							final com.destroystokyo.paper.entity.villager.ReputationType type = com.destroystokyo.paper.entity.villager.ReputationType
									.valueOf(data.getString("type"));
							final int value = (int) entry.get("value");
							final com.destroystokyo.paper.entity.villager.Reputation rep = new com.destroystokyo.paper.entity.villager.Reputation();
							rep.setReputation(type, value);
							villager.setReputation(uuid, rep);
						} catch (Exception e) {
							HellblockPlugin.getInstance().getPluginLogger()
									.warn("Failed to restore villager reputation: " + entry + " - " + e.getMessage());
						}
					});
				}
			}

			if (data.get("trades") != null) {
				final List<Map<String, Object>> trades = (List<Map<String, Object>>) tagToObject(data.get("trades"));
				final List<MerchantRecipe> recipes = new ArrayList<>();
				trades.forEach(trade -> {
					final ItemStack result = itemStackFromNBT((CompoundBinaryTag) trade.get("result"));
					final MerchantRecipe recipe = new MerchantRecipe(result, (int) trade.get("uses"),
							(int) trade.get("maxUses"), (boolean) trade.get("experienceReward"),
							(int) trade.get("villagerExp"), (float) trade.get("priceMultiplier"));

					final List<Map<String, Object>> ingredients = (List<Map<String, Object>>) trade.get("ingredients");
					ingredients.forEach(ing -> recipe.addIngredient(itemStackFromNBT((CompoundBinaryTag) ing)));
					recipes.add(recipe);
				});
				villager.setRecipes(recipes);
			}
		}

		else if (entity instanceof WanderingTrader wanderingTrader && data.get("wanderingTrader") != null) {
			wanderingTrader.setDespawnDelay(getIntOrDefault(data, "despawnDelay", wanderingTrader.getDespawnDelay()));
		}

		else if (entity instanceof ZombieVillager zombieVillager && data.get("zombieVillager") != null) {
			zombieVillager.setVillagerProfession(ItemRegistry.getVillagerProfession(data.getString("profession")));
			zombieVillager.setVillagerType(ItemRegistry.getVillagerType(data.getString("type")));
			zombieVillager.setCanBreakDoors(getBooleanOrDefault(data, "breakDoors", zombieVillager.canBreakDoors()));

			if (getBooleanOrDefault(data, "converting", zombieVillager.isConverting())) {
				zombieVillager
						.setConversionTime(getIntOrDefault(data, "conversionTime", zombieVillager.getConversionTime()));
				if (data.get("convertedBy") != null) {
					final UUID id = UUID.fromString(data.getString("convertedBy"));
					final Player player = Bukkit.getPlayer(id);
					if (player != null) {
						zombieVillager.setConversionPlayer(player);
					}
				}
			}
		}

		else if (entity instanceof Zombie zombie && data.get("zombie") != null
				&& (!(entity instanceof ZombieVillager))) {
			zombie.setCanBreakDoors(getBooleanOrDefault(data, "breakDoors", zombie.canBreakDoors()));

			if (getBooleanOrDefault(data, "converting", zombie.isConverting())) {
				zombie.setConversionTime(getIntOrDefault(data, "conversionTime", zombie.getConversionTime()));
			}
		}

		else if (entity instanceof Pig pig) {
			pig.setSaddle(getBooleanOrDefault(data, "saddled", pig.hasSaddle()));
			pig.setVariant(ItemRegistry.getPigVariant(data.getString("variant")));
		}

		else if (entity instanceof Cow cow) {
			cow.setVariant(ItemRegistry.getCowVariant(data.getString("variant")));
		}

		else if (entity instanceof Chicken chicken) {
			chicken.setVariant(ItemRegistry.getChickenVariant(data.getString("variant")));
		}

		else if (entity instanceof Strider strider) {
			strider.setSaddle(getBooleanOrDefault(data, "saddled", strider.hasSaddle()));
			strider.setShivering(getBooleanOrDefault(data, "shivering", strider.isShivering()));
		}

		else if (entity instanceof Goat goat) {
			goat.setScreaming(getBooleanOrDefault(data, "screaming", goat.isScreaming()));
			goat.setLeftHorn(getBooleanOrDefault(data, "leftHorn", goat.hasLeftHorn()));
			goat.setRightHorn(getBooleanOrDefault(data, "rightHorn", goat.hasRightHorn()));
		}

		else if (entity instanceof Allay allay) {
			if (data.get("jukebox") instanceof CompoundBinaryTag jukeboxTag) {
				final Location jukebox = deserializeLocation(jukeboxTag);
				allay.startDancing(jukebox);
			}
			allay.setCanDuplicate(getBooleanOrDefault(data, "duplicate", allay.canDuplicate()));
			allay.setDuplicationCooldown(getLongOrDefault(data, "duplicationCooldown", allay.getDuplicationCooldown()));
		}

		else if (entity instanceof Tameable tameable && !(entity instanceof AbstractHorse)) {
			if (getBooleanOrDefault(data, "tamed", tameable.isTamed())) {
				tameable.setTamed(true);

				if (data.get("owner") != null) {
					final UUID uuid = UUID.fromString(data.getString("owner"));
					tameable.setOwner(Bukkit.getOfflinePlayer(uuid));
				}
			}

			if (tameable instanceof Wolf wolf) {
				wolf.setCollarColor(DyeColor.valueOf(data.getString("collar")));
				wolf.setVariant(ItemRegistry.getWolfVariant(data.getString("variant")));
				wolf.setAngry(getBooleanOrDefault(data, "angry", wolf.isAngry()));
				wolf.setInterested(getBooleanOrDefault(data, "begging", wolf.isInterested()));
				wolf.setSitting(getBooleanOrDefault(data, "sitting", wolf.isSitting()));
				if (VersionHelper.isPaper()) {
					wolf.setSoundVariant(ItemRegistry.getWolfSoundVariant(data.getString("soundVariant")));
				}
			}

			if (tameable instanceof Cat cat) {
				cat.setCatType(ItemRegistry.getCatVariant(data.getString("type")));
				cat.setCollarColor(DyeColor.valueOf(data.getString("collar")));
				cat.setSitting(getBooleanOrDefault(data, "sitting", cat.isSitting()));
				if (VersionHelper.isPaper()) {
					cat.setHeadUp(getBooleanOrDefault(data, "headUp", cat.isHeadUp()));
					cat.setLyingDown(getBooleanOrDefault(data, "lyingDown", cat.isLyingDown()));
				}
			}

			if (tameable instanceof Parrot parrot) {
				parrot.setVariant(Parrot.Variant.valueOf(data.getString("variant")));
				parrot.setSitting(getBooleanOrDefault(data, "sitting", parrot.isSitting()));
			}
		}

		// LivingEntity shared logic
		if (entity instanceof LivingEntity rawLiving) {
			final RtagEntity tagged = new RtagEntity(rawLiving);

			tagged.setAttributeBase("generic.max_health",
					getDoubleOrDefault(data, "maxHealth", tagged.getAttributeBase("generic.max_health")));

			if (data.get("health") != null) {
				final double health = getDoubleOrDefault(data, "health", tagged.getHealth());
				final double max = tagged.getAttributeBase("generic.max_health");
				tagged.setHealth((float) Math.min(health, max));
			}

			final LivingEntity living = (LivingEntity) tagged.load();

			living.setAbsorptionAmount(getDoubleOrDefault(data, "absorption", living.getAbsorptionAmount()));
			living.setRemainingAir(getIntOrDefault(data, "remainingAir", living.getRemainingAir()));
			living.setFireTicks(getIntOrDefault(data, "fireTicks", living.getFireTicks()));
			living.setFreezeTicks(getIntOrDefault(data, "freezeTicks", living.getFreezeTicks()));
			living.setGlowing(getBooleanOrDefault(data, "glowing", living.isGlowing()));
			living.setGravity(getBooleanOrDefault(data, "gravity", living.hasGravity()));
			living.setInvulnerable(getBooleanOrDefault(data, "invulnerable", living.isInvulnerable()));
			living.setSilent(getBooleanOrDefault(data, "silent", living.isSilent()));
			living.setCollidable(getBooleanOrDefault(data, "collidable", living.isCollidable()));

			if (data.get("customName") != null) {
				Component name = AdventureHelper.getGson().deserialize(data.getString("customName"));
				AdventureMetadata.setEntityCustomName(living, name);
				living.setCustomNameVisible(
						getBooleanOrDefault(data, "customNameVisible", living.isCustomNameVisible()));
			}

			if (data.get("effects") != null) {
				final List<Map<String, Object>> effects = (List<Map<String, Object>>) tagToObject(data.get("effects"));
				effects.forEach(effData -> {
					try {
						final PotionEffectType type = PotionEffectResolver.resolve((String) effData.get("type"));
						if (type != null) {
							final PotionEffect effect = new PotionEffect(type, (int) effData.get("duration"),
									(int) effData.get("amplifier"), (boolean) effData.get("ambient"),
									(boolean) effData.get("particles"), (boolean) effData.get("icon"));
							living.addPotionEffect(effect);
						}
					} catch (Exception ex) {
						HellblockPlugin.getInstance().getPluginLogger()
								.warn("Failed to restore potion effect: " + effData + " - " + ex.getMessage());
					}
				});
			}

			// Equipment
			if (data.get("equipment") instanceof CompoundBinaryTag equipmentTag) {
				restoreEquipment(living, equipmentTag);
			}

			// Leash restoration
			if (data.get("leashHolder") != null) {
				final UUID leashId = UUID.fromString(data.getString("leashHolder"));

				final Runnable leashTask = new Runnable() {
					int attempts = 0;
					final int maxAttempts = 100;

					@Override
					public void run() {
						final Entity holder = Bukkit.getEntity(leashId);
						if (holder != null) {
							living.setLeashHolder(holder);
						} else if (++attempts < maxAttempts) {
							HellblockPlugin.getInstance().getScheduler().sync().runLater(this, 1L,
									living.getLocation());
						} else {
							HellblockPlugin.getInstance().getPluginLogger()
									.warn("Failed to restore leash for " + living.getUniqueId());
						}
					}
				};

				HellblockPlugin.getInstance().getScheduler().sync().runLater(leashTask, 1L, living.getLocation());
			}
		}

		// Passengers
		if (data.get("passengers") != null) {
			final List<EntitySnapshot> passengers = (List<EntitySnapshot>) tagToObject(data.get("passengers"));
			passengers.stream().map(snapshot -> snapshot.spawn(entity.getWorld())).filter(Objects::nonNull)
					.forEach(entity::addPassenger);
		}

		// Vehicle
		if (data.get("vehicle") != null) {
			final EntitySnapshot vehicleSnap = (EntitySnapshot) tagToObject(data.get("vehicle"));
			final Entity vehicle = vehicleSnap.spawn(entity.getWorld());
			if (vehicle != null) {
				vehicle.addPassenger(entity);
			}
		}

		// Persistent Data Container (PDC)
		if (data.get("persistentData") != null) {
			final Map<String, Object> pdcMap = (Map<String, Object>) tagToObject(data.get("persistentData"));
			final PersistentDataContainer pdc = entity.getPersistentDataContainer();
			pdcMap.forEach((keyStr, value) -> {
				final NamespacedKey key = NamespacedKey.fromString(keyStr);
				if (key != null && value instanceof String str) {
					pdc.set(key, PersistentDataType.STRING, str);
				}
			});
		}
		// Ageable
		if (entity instanceof Ageable ageable && data.get("ageable") != null) {
			ageable.setAge(getIntOrDefault(data, "age", ageable.getAge()));
			if (data.get("adult") != null) {
				if (getBooleanOrDefault(data, "adult", ageable.isAdult())) {
					ageable.setAdult();
				} else {
					ageable.setBaby();
				}
			}
		}

		// Misc mob behaviors
		if (entity instanceof Enderman enderman) {
			if (data.get("carriedBlock") != null) {
				enderman.setCarriedBlock(Bukkit.createBlockData(data.getString("carriedBlock")));
			}
			if (VersionHelper.isPaper()) {
				enderman.setScreaming(getBooleanOrDefault(data, "screaming", enderman.isScreaming()));
			}
		}

		else if (entity instanceof Witch witch) {
			if (getBooleanOrDefault(data, "isDrinkingPotion", witch.isDrinkingPotion())) {
				if (VersionHelper.isPaper()) {
					// Paper has a different method to set the drinking potion
					// which doesn't exist in Spigot
					// https://papermc.io/javadocs/paper/1.20/org
					// .bukkit/entity/Witch.html#setWitchDrinkingPotion-org.bukkit.inventory.ItemStack-
					// This method is also preferred as it handles the internal state better
					// according to the javadoc
					if (data.get("drinkingPotion") instanceof CompoundBinaryTag potionTag) {
						witch.setDrinkingPotion(itemStackFromNBT(potionTag));
					}
				} else {
					// Fallback for Spigot - use the deprecated method
					// https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity
					// /Witch.html#setDrinkingPotion-org.bukkit.inventory.ItemStack-
					// This may not handle the internal state as well as the Paper method
					// but it's the best we can do for Spigot
					if (data.get("drinkingPotion") instanceof CompoundBinaryTag potionTag) {
						setWitchDrinkingPotion(witch, itemStackFromNBT(potionTag));
					}
				}
			}
		}

		else if (entity instanceof Snowman snowman) {
			snowman.setDerp(getBooleanOrDefault(data, "pumpkin", snowman.isDerp()));
		}

		else if (entity instanceof IronGolem golem) {
			golem.setPlayerCreated(getBooleanOrDefault(data, "playerCreated", golem.isPlayerCreated()));
		}

		else if (entity instanceof Fox fox) {
			fox.setFoxType(Fox.Type.valueOf(data.getString("type")));
			fox.setSitting(getBooleanOrDefault(data, "sitting", fox.isSitting()));
			fox.setSleeping(getBooleanOrDefault(data, "sleeping", fox.isSleeping()));
			fox.setCrouching(getBooleanOrDefault(data, "crouching", fox.isCrouching()));
			if (VersionHelper.isPaper()) {
				fox.setFaceplanted(getBooleanOrDefault(data, "faceplanted", fox.isFaceplanted()));
			}
			if (data.get("trustedFirst") != null) {
				final UUID uuid = UUID.fromString(data.getString("trustedFirst"));
				fox.setFirstTrustedPlayer(Bukkit.getOfflinePlayer(uuid));
			}
			if (data.get("trustedSecond") != null) {
				final UUID uuid = UUID.fromString(data.getString("trustedSecond"));
				fox.setSecondTrustedPlayer(Bukkit.getOfflinePlayer(uuid));
			}
		}

		else if (entity instanceof Panda panda) {
			panda.setMainGene(Panda.Gene.valueOf(data.getString("mainGene")));
			panda.setHiddenGene(Panda.Gene.valueOf(data.getString("hiddenGene")));
			panda.setSneezing(getBooleanOrDefault(data, "sneezing", panda.isSneezing()));
			panda.setRolling(getBooleanOrDefault(data, "rolling", panda.isRolling()));
			panda.setSitting(getBooleanOrDefault(data, "sitting", panda.isSitting()));
		}

		else if (VersionHelper.isPaper() && entity instanceof Turtle turtle) {
			if (data.get("home") instanceof CompoundBinaryTag homeTag) {
				turtle.setHome(deserializeLocation(homeTag));
			}
			turtle.setHasEgg(getBooleanOrDefault(data, "hasEgg", turtle.hasEgg()));
		}

		else if (entity instanceof Bat bat) {
			if (getBooleanOrDefault(data, "hanging", bat.isAwake())) {
				bat.setAwake(false);
			}
		}

		else if (entity instanceof MushroomCow mushroomCow) {
			mushroomCow.setVariant(MushroomCow.Variant.valueOf(data.getString("variant")));
		}

		else if (entity instanceof Ocelot ocelot) {
			ocelot.setTrusting(getBooleanOrDefault(data, "trusting", ocelot.isTrusting()));
		}

		else if (entity instanceof Hoglin hoglin) {
			hoglin.setImmuneToZombification(
					getBooleanOrDefault(data, "immuneToZombification", hoglin.isImmuneToZombification()));
		}

		else if (VersionHelper.isPaper() && entity instanceof Dolphin dolphin) {
			dolphin.setHasFish(getBooleanOrDefault(data, "hasFish", dolphin.hasFish()));
			dolphin.setMoistness(getIntOrDefault(data, "moistness", dolphin.getMoistness()));
		}

		else if (entity instanceof TropicalFish tropicalFish) {
			tropicalFish.setPattern(TropicalFish.Pattern.valueOf(data.getString("pattern")));
			tropicalFish.setPatternColor(DyeColor.valueOf(data.getString("patternColor")));
			tropicalFish.setBodyColor(DyeColor.valueOf(data.getString("bodyColor")));
		}

		else if (entity instanceof Salmon salmon) {
			salmon.setVariant(Salmon.Variant.valueOf(data.getString("variant")));
		}

		else if (entity instanceof Vindicator vindicator) {
			vindicator.setJohnny(getBooleanOrDefault(data, "johnny", vindicator.isJohnny()));
		}

		else if (entity instanceof Piglin piglin) {
			piglin.setImmuneToZombification(
					getBooleanOrDefault(data, "immuneToZombification", piglin.isImmuneToZombification()));
			piglin.setIsAbleToHunt(getBooleanOrDefault(data, "ableToHunt", piglin.isAbleToHunt()));
			if (VersionHelper.isPaper()) {
				piglin.setChargingCrossbow(getBooleanOrDefault(data, "chargingCrossbow", piglin.isChargingCrossbow()));
				piglin.setDancing(getBooleanOrDefault(data, "dancing", piglin.isDancing()));
			}
		}

		else if (entity instanceof PufferFish pufferFish) {
			pufferFish.setPuffState(getIntOrDefault(data, "puffState", pufferFish.getPuffState()));
		}

		else if (entity instanceof GlowSquid glowSquid) {
			glowSquid.setDarkTicksRemaining(
					getIntOrDefault(data, "darkTicksRemaining", glowSquid.getDarkTicksRemaining()));
		}

		else if (entity instanceof Axolotl axolotl) {
			axolotl.setPlayingDead(getBooleanOrDefault(data, "playingDead", axolotl.isPlayingDead()));
			axolotl.setVariant(Axolotl.Variant.valueOf(data.getString("variant")));
		}

		else if (entity instanceof Rabbit rabbit) {
			rabbit.setRabbitType(Rabbit.Type.valueOf(data.getString("type")));
		}

		else if (entity instanceof Sheep sheep) {
			if (data.get("color") != null) {
				sheep.setColor(DyeColor.valueOf(data.getString("color")));
			}
			sheep.setSheared(getBooleanOrDefault(data, "sheared", sheep.isSheared()));
		}

		else if (VersionHelper.isVersionNewerThan1_19_3() && entity instanceof Frog frog) {
			frog.setVariant(ItemRegistry.getFrogVariant(data.getString("variant")));
		}

		else if (entity instanceof Guardian guardian) {
			guardian.setLaser(getBooleanOrDefault(data, "hasLaser", guardian.hasLaser()));
			guardian.setLaserTicks(getIntOrDefault(data, "laserTicks", guardian.getLaserTicks()));
		}

		else if (entity instanceof Bee bee) {
			bee.setHasNectar(getBooleanOrDefault(data, "hasNectar", bee.hasNectar()));
			bee.setHasStung(getBooleanOrDefault(data, "hasStung", bee.hasStung()));
			bee.setAnger(getIntOrDefault(data, "anger", bee.getAnger()));
			if (data.get("hive") instanceof CompoundBinaryTag hive) {
				bee.setHive(deserializeLocation(hive));
			}
			if (data.get("flower") instanceof CompoundBinaryTag flower) {
				bee.setFlower(deserializeLocation(flower));
			}
		}

		else if (entity instanceof Warden warden && data.get("anger") != null) {
			if (data.get("angerTarget") != null && data.get("angerLevel") != null) {
				final UUID id = UUID.fromString(data.getString("angerTarget"));
				final int anger = getIntOrDefault(data, "angerLevel", warden.getAnger());

				final Runnable tryWarden = new Runnable() {
					int attempts = 0;
					final int maxAttempts = 100;

					@Override
					public void run() {
						final Entity found = Bukkit.getEntity(id);
						if (found instanceof LivingEntity target) {
							warden.setAnger(target, anger);
						} else if (++attempts < maxAttempts) {
							HellblockPlugin.getInstance().getScheduler().sync().runLater(this, 1L,
									warden.getLocation());
						} else {
							HellblockPlugin.getInstance().getPluginLogger()
									.warn("Failed to restore warden anger for " + warden.getUniqueId());
						}
					}
				};

				HellblockPlugin.getInstance().getScheduler().sync().runLater(tryWarden, 1L, warden.getLocation());
			}
		}

		else if (entity instanceof Vex vex) {
			if (data.get("bound") instanceof CompoundBinaryTag bound) {
				vex.setBound(deserializeLocation(bound));
			}
			vex.setCharging(getBooleanOrDefault(data, "charging", vex.isCharging()));
			AdventureMetadata.setVexLifetimeTicks(vex,
					getIntOrDefault(data, "limitedLifetimeTicks", AdventureMetadata.getVexLifetimeTicks(vex)));
			if (VersionHelper.isPaper()) {
				if (data.get("summoner") != null) {
					final UUID summonerId = UUID.fromString(data.getString("summoner"));

					final Runnable tryVex = new Runnable() {
						int attempts = 0;
						final int maxAttempts = 100;

						@Override
						public void run() {
							final Entity found = Bukkit.getEntity(summonerId);
							if (found instanceof Mob summoner) {
								vex.setSummoner(summoner);
							} else if (++attempts < maxAttempts) {
								HellblockPlugin.getInstance().getScheduler().sync().runLater(this, 1L,
										vex.getLocation());
							} else {
								HellblockPlugin.getInstance().getPluginLogger()
										.warn("Failed to restore vex summoner for " + vex.getUniqueId());
							}
						}
					};

					HellblockPlugin.getInstance().getScheduler().sync().runLater(tryVex, 1L, vex.getLocation());
				}
			}
		}

		else if (entity instanceof Ghast ghast) {
			ghast.setCharging(getBooleanOrDefault(data, "charging", ghast.isCharging()));
		}

		else if (entity instanceof Creeper creeper) {
			creeper.setPowered(getBooleanOrDefault(data, "charged", creeper.isPowered()));
			creeper.setFuseTicks(getIntOrDefault(data, "fuseTicks", creeper.getFuseTicks()));
			creeper.setMaxFuseTicks(getIntOrDefault(data, "maxFuseTicks", creeper.getMaxFuseTicks()));
		}

		else if (entity instanceof Phantom phantom) {
			phantom.setSize(getIntOrDefault(data, "size", phantom.getSize()));
		}

		else if (entity instanceof Wither wither) {
			AdventureMetadata.setWitherInvulnerableTicks(wither,
					getIntOrDefault(data, "invulnerableTicks", AdventureMetadata.getWitherInvulnerableTicks(wither)));
		}

		else if (entity instanceof Slime slime) {
			slime.setSize(getIntOrDefault(data, "size", slime.getSize()));
		}

		else if (entity instanceof Shulker shulker) {
			if (data.get("color") != null) {
				shulker.setColor(DyeColor.valueOf(data.getString("color")));
			}
			shulker.setPeek(getFloatOrDefault(data, "peek", shulker.getPeek()));
			shulker.setAttachedFace(BlockFace.valueOf(data.getString("attachedFace")));
		}

		else if (entity instanceof EnderDragon enderDragon) {
			if (VersionHelper.isPaper()) {
				if (data.get("podium") instanceof CompoundBinaryTag podium) {
					enderDragon.setPodium(deserializeLocation(podium));
				}
			}
			enderDragon.setPhase(EnderDragon.Phase.valueOf(data.getString("phase")));
		}
	}

	/**
	 * Capture the equipment of a LivingEntity into a CompoundBinaryTag.
	 * 
	 * @param entity The LivingEntity whose equipment is to be captured
	 * 
	 * @return A CompoundBinaryTag representing the entity's equipment, or null if
	 *         no equipment
	 */
	private static CompoundBinaryTag captureEquipment(LivingEntity entity) {
		if (entity.getEquipment() == null) {
			return null;
		}

		final CompoundBinaryTag.Builder tagBuilder = CompoundBinaryTag.builder();

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			final ItemStack item = entity.getEquipment().getItem(slot);
			if (item != null && !item.getType().isAir()) {
				tagBuilder.put(slot.name(), itemStackToNBT(item));
			}
		}

		final CompoundBinaryTag tag = tagBuilder.build();
		return tag.isEmpty() ? null : tag;
	}

	/**
	 * Restore equipment from a CompoundBinaryTag to a LivingEntity.
	 * 
	 * @param entity       The LivingEntity to restore equipment to
	 * 
	 * @param equipmentTag The CompoundBinaryTag containing equipment data
	 */
	private static void restoreEquipment(LivingEntity entity, CompoundBinaryTag equipmentTag) {
		if (equipmentTag == null || entity.getEquipment() == null) {
			return;
		}

		equipmentTag.keySet().forEach(key -> {
			try {
				final EquipmentSlot slot = EquipmentSlot.valueOf(key);
				final BinaryTag itemTag = equipmentTag.get(slot.name());
				if (itemTag instanceof CompoundBinaryTag compound) {
					entity.getEquipment().setItem(slot, itemStackFromNBT(compound), true);
				}
			} catch (IllegalArgumentException ignored) {
				// Skip invalid slot name
			}
		});
	}

	/**
	 * Convert an ItemStack to a CompoundBinaryTag for storage.
	 * 
	 * @param item The ItemStack to convert
	 * 
	 * @return A CompoundBinaryTag representing the ItemStack
	 */
	private static CompoundBinaryTag eulerToNBT(EulerAngle angle) {
		return CompoundBinaryTag.builder().putDouble("x", angle.getX()).putDouble("y", angle.getY())
				.putDouble("z", angle.getZ()).build();
	}

	/**
	 * Convert a CompoundBinaryTag back into an ItemStack.
	 * 
	 * @param tag The CompoundBinaryTag representing the ItemStack
	 * @return The reconstructed ItemStack, or null if the tag is empty
	 */
	private static EulerAngle eulerFromNBT(CompoundBinaryTag tag) {
		return new EulerAngle(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
	}

	/**
	 * Serialize a Location into a CompoundBinaryTag for storage.
	 * 
	 * @param loc The Location to serialize
	 * 
	 * @return A CompoundBinaryTag representing the Location
	 */
	private static CompoundBinaryTag serializeLocation(Location loc) {
		return CompoundBinaryTag.builder().putString("world", loc.getWorld().getName()).putDouble("x", loc.getX())
				.putDouble("y", loc.getY()).putDouble("z", loc.getZ()).putFloat("yaw", loc.getYaw())
				.putFloat("pitch", loc.getPitch()).build();
	}

	/**
	 * Deserialize a Location from a Map<String, Object> representation.
	 * 
	 * @param map The map containing location data
	 * 
	 * @return The reconstructed Location, or null if the world is not found
	 */
	private static Location deserializeLocation(CompoundBinaryTag tag) {
		final World world = Bukkit.getWorld(tag.getString("world"));
		return new Location(world, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yaw"),
				tag.getFloat("pitch"));
	}

	/**
	 * Uses reflection to access the private field in CraftBukkit that stores the
	 * potion a witch is currently drinking, if any.
	 * 
	 * @param witch The witch entity to inspect
	 * @return The ItemStack of the potion being consumed, or null if none
	 */
	@Nullable
	private static ItemStack getWitchDrinkingPotion(Witch witch) {
		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			final Class<?> craftWitchClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftWitch");
			final Class<?> nmsWitchClass = Class.forName("net.minecraft.world.entity.monster.EntityWitch");
			final Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
			final Class<?> craftItemStackClass = Class
					.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");

			final Method getHandle = craftWitchClass.getMethod("getHandle");
			final Method asBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
			final Field drinkingPotionField = nmsWitchClass.getDeclaredField("drinkingPotion");
			drinkingPotionField.setAccessible(true);

			final Object craftWitch = craftWitchClass.cast(witch);
			final Object nmsWitch = getHandle.invoke(craftWitch);
			final Object nmsItemStack = drinkingPotionField.get(nmsWitch);

			if (nmsItemStack == null) {
				return null;
			}

			final ItemStack potion = (ItemStack) asBukkitCopy.invoke(null, nmsItemStack);
			return potion.getType() != Material.AIR ? potion : null;

		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Failed to read witch drinking potion: " + ex.getMessage());
			return null;
		}
	}

	/**
	 * Uses reflection to set the private field in CraftBukkit that stores the
	 * potion a witch is currently drinking.
	 * 
	 * @param witch  The witch entity to modify
	 * @param potion The ItemStack of the potion to set, or null to clear
	 */
	private static void setWitchDrinkingPotion(Witch witch, @Nullable ItemStack potion) {
		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			final Class<?> craftWitchClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftWitch");
			final Class<?> nmsWitchClass = Class.forName("net.minecraft.world.entity.monster.EntityWitch");
			final Class<?> craftItemStackClass = Class
					.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");

			final Method getHandle = craftWitchClass.getMethod("getHandle");
			final Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
			final Field drinkingPotionField = nmsWitchClass.getDeclaredField("drinkingPotion");
			drinkingPotionField.setAccessible(true);

			final Object craftWitch = craftWitchClass.cast(witch);
			final Object nmsWitch = getHandle.invoke(craftWitch);
			final Object nmsItemStack = potion == null ? null : asNMSCopy.invoke(null, potion);

			drinkingPotionField.set(nmsWitch, nmsItemStack);
		} catch (Exception ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Failed to set witch drinking potion: " + ex.getMessage());
		}
	}

	/**
	 * Convert an ItemStack to a CompoundBinaryTag for NBT storage.
	 * 
	 * @param item The ItemStack to convert
	 * @return The CompoundBinaryTag representing the ItemStack
	 */
	private static CompoundBinaryTag itemStackToNBT(ItemStack item) {
		if (item == null) {
			return CompoundBinaryTag.empty();
		}

		// Serialize Bukkit ItemStack to a Map
		final Map<String, Object> serialized = item.serialize();

		// Recursively build CompoundBinaryTag from Map
		return mapToNBT(serialized);
	}

	/**
	 * Convert an Inventory to a CompoundBinaryTag for NBT storage.
	 * 
	 * @param inventory The Inventory to convert
	 * @return The CompoundBinaryTag representing the Inventory
	 */
	private static CompoundBinaryTag inventoryToNBT(Inventory inventory) {
		final CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
		for (int i = 0; i < inventory.getSize(); i++) {
			final ItemStack item = inventory.getItem(i);
			if (item != null) {
				builder.put(String.valueOf(i), itemStackToNBT(item));
			}
		}
		return builder.build();
	}

	/**
	 * Recursively convert a Map<String, Object> to a CompoundBinaryTag. Supports
	 * nested maps, lists, and primitive types.
	 * 
	 * @param map The map to convert
	 * @return The corresponding CompoundBinaryTag
	 */
	private static CompoundBinaryTag mapToNBT(Map<String, Object> map) {
		final CompoundBinaryTag.Builder tagBuilder = CompoundBinaryTag.builder();
		map.forEach((key, value) -> tagBuilder.put(key, objectToTag(value)));
		return tagBuilder.build();
	}

	/**
	 * Convert a generic Object to a BinaryTag for NBT storage. Supports primitives,
	 * strings, maps, lists, and enums.
	 * 
	 * @param value The object to convert
	 * @return The corresponding BinaryTag
	 */
	private static BinaryTag objectToTag(Object value) {
		if (value == null) {
			return StringBinaryTag.stringBinaryTag("null");
		}

		// --- Primitives ---
		if (value instanceof String str) {
			return StringBinaryTag.stringBinaryTag(str);
		}
		if (value instanceof Integer i) {
			return IntBinaryTag.intBinaryTag(i);
		}
		if (value instanceof Short s) {
			return ShortBinaryTag.shortBinaryTag(s);
		}
		if (value instanceof Byte b) {
			return ByteBinaryTag.byteBinaryTag(b);
		}
		if (value instanceof Boolean b) {
			return ByteBinaryTag.byteBinaryTag((byte) (b ? 1 : 0));
		}
		if (value instanceof Long l) {
			return LongBinaryTag.longBinaryTag(l);
		}
		if (value instanceof Float f) {
			return FloatBinaryTag.floatBinaryTag(f);
		}
		if (value instanceof Double d) {
			return DoubleBinaryTag.doubleBinaryTag(d);
		}

		// --- Map (nested compound tags) ---
		if (value instanceof Map<?, ?> nestedMap) {
			final Map<String, Object> stringKeyedMap = new HashMap<>();
			nestedMap.forEach((k, v) -> stringKeyedMap.put(String.valueOf(k), v));
			return mapToNBT(stringKeyedMap);
		}

		// --- Lists ---
		if (value instanceof List<?> list) {
			final Builder<BinaryTag> listBuilder = ListBinaryTag.builder();
			list.forEach((Object element) -> listBuilder.add(objectToTag(element)));
			return listBuilder.build();
		}

		// --- Enums ---
		if (value instanceof Enum<?> e) {
			return StringBinaryTag.stringBinaryTag(e.name());
		}

		// Fallback: toString()
		return StringBinaryTag.stringBinaryTag(value.toString());
	}

	/**
	 * Convert a CompoundBinaryTag back into an ItemStack.
	 * 
	 * @param tag The CompoundBinaryTag to convert
	 * @return The reconstructed ItemStack
	 */
	private static ItemStack itemStackFromNBT(CompoundBinaryTag tag) {
		// Deserialize ItemStack from Bukkit ConfigurationSerializable map stored in NBT
		final Map<String, Object> map = new HashMap<>();
		tag.keySet().forEach(key -> map.put(key, tagToObject(tag.get(key))));
		return ItemStack.deserialize(map);
	}

	/**
	 * Populate an Inventory from a CompoundBinaryTag representation.
	 * 
	 * @param tag       The CompoundBinaryTag containing inventory data
	 * @param inventory The Inventory to populate
	 */
	private static void inventoryFromNBT(CompoundBinaryTag tag, Inventory inventory) {
		tag.keySet().forEach(key -> {
			try {
				final int slot = Integer.parseInt(key);
				final BinaryTag itemTag = tag.get(key);
				if (itemTag instanceof CompoundBinaryTag compound) {
					inventory.setItem(slot, itemStackFromNBT(compound));
				}
			} catch (NumberFormatException ignored) {
				// Skip non-integer keys
			}
		});
	}

	/**
	 * Convert an Adventure BinaryTag to a standard Java Object.
	 * 
	 * @param tag The BinaryTag to convert
	 * @return The corresponding Java Object, or null if unsupported
	 */
	private static Object tagToObject(BinaryTag tag) {
		final BinaryTagType<?> type = tag.type();

		if (type == BinaryTagTypes.STRING) {
			return ((StringBinaryTag) tag).value();
		}
		if (type == BinaryTagTypes.INT) {
			return ((IntBinaryTag) tag).intValue();
		}
		if (type == BinaryTagTypes.SHORT) {
			return ((ShortBinaryTag) tag).shortValue();
		}
		if (type == BinaryTagTypes.BYTE) {
			return ((ByteBinaryTag) tag).byteValue();
		}
		if (type == BinaryTagTypes.LONG) {
			return ((LongBinaryTag) tag).longValue();
		}
		if (type == BinaryTagTypes.FLOAT) {
			return ((FloatBinaryTag) tag).floatValue();
		}
		if (type == BinaryTagTypes.DOUBLE) {
			return ((DoubleBinaryTag) tag).doubleValue();
		}

		if (type == BinaryTagTypes.COMPOUND) {
			final CompoundBinaryTag compound = (CompoundBinaryTag) tag;
			return compound.keySet().stream().collect(Collectors.toMap(k -> k, k -> tagToObject(compound.get(k))));
		}

		if (type != BinaryTagTypes.LIST) {
			return null;
		}
		final ListBinaryTag list = (ListBinaryTag) tag;
		return list.stream().map(EntitySnapshot::tagToObject).collect(Collectors.toList());
	}

	/**
	 * Retrieve a boolean value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the boolean value
	 * @param defaultValue The default value to return if key is absent
	 * @return The boolean value, or the default if not found
	 */
	private static boolean getBooleanOrDefault(CompoundBinaryTag tag, String key, boolean defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? (boolean) EntitySnapshot.tagToObject(value) : defaultValue;
	}

	/**
	 * Retrieve an int value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the int value
	 * @param defaultValue The default value to return if key is absent
	 * @return The int value, or the default if not found
	 */
	private static int getIntOrDefault(CompoundBinaryTag tag, String key, int defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? (int) EntitySnapshot.tagToObject(value) : defaultValue;
	}

	/**
	 * Retrieve a double value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the double value
	 * @param defaultValue The default value to return if key is absent
	 * @return The double value, or the default if not found
	 */
	private static double getDoubleOrDefault(CompoundBinaryTag tag, String key, double defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? (double) EntitySnapshot.tagToObject(value) : defaultValue;
	}

	/**
	 * Retrieve a float value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the float value
	 * @param defaultValue The default value to return if key is absent
	 * @return The float value, or the default if not found
	 */
	private static float getFloatOrDefault(CompoundBinaryTag tag, String key, float defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? ((Number) EntitySnapshot.tagToObject(value)).floatValue() : defaultValue;
	}

	/**
	 * Retrieve a string value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the string value
	 * @param defaultValue The default value to return if key is absent
	 * @return The string value, or the default if not found
	 */
	private static long getLongOrDefault(CompoundBinaryTag tag, String key, long defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? (long) EntitySnapshot.tagToObject(value) : defaultValue;
	}

	/**
	 * Retrieve a String value from a CompoundBinaryTag, returning a default if the
	 * key is absent.
	 * 
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the String value
	 * @param defaultValue The default value to return if key is absent
	 * @return The String value, or the default if not found
	 */
	private static String getStringOrDefault(CompoundBinaryTag tag, String key, String defaultValue) {
		final BinaryTag value = tag.get(key);
		return value != null ? (String) EntitySnapshot.tagToObject(value) : defaultValue;
	}

	/**
	 * Retrieve an enum value from a CompoundBinaryTag, returning a default if the
	 * key is absent or the value is invalid.
	 * 
	 * @param <T>          The enum type
	 * @param tag          The CompoundBinaryTag to read from
	 * @param key          The key of the enum value
	 * @param enumClass    The class of the enum
	 * @param defaultValue The default value to return if key is absent or invalid
	 * @return The enum value, or the default if not found/invalid
	 */
	private static <T extends Enum<T>> T getEnumOrDefault(CompoundBinaryTag tag, String key, Class<T> enumClass,
			T defaultValue) {
		final BinaryTag value = tag.get(key);
		if (value == null) {
			return defaultValue;
		}

		try {
			final String str = (String) EntitySnapshot.tagToObject(value);
			return Enum.valueOf(enumClass, str);
		} catch (Exception e) {
			return defaultValue;
		}
	}
}