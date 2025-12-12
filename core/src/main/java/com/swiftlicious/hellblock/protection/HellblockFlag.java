package com.swiftlicious.hellblock.protection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a protection flag on a Hellblock island.
 * <p>
 * Each {@code HellblockFlag} defines an individual rule or toggle for island
 * protection, such as block breaking, mob spawning, or greeting messages. The
 * configuration is stored per-island and persisted across sessions.
 * <p>
 * Unlike {@link FlagType}, which defines global flag definitions and default
 * values, {@code HellblockFlag} instances are per-island and may contain unique
 * data (e.g. custom messages, notes, etc.).
 */
public class HellblockFlag {

	@Expose
	@SerializedName("flagType")
	private FlagType flag;

	@Expose
	@SerializedName("allowedStatus")
	private AccessType status;

	@Expose
	@SerializedName("stringData")
	private String data;

	/**
	 * Constructs a new {@code HellblockFlag}.
	 *
	 * @param flag   the flag type this object represents
	 * @param status the access status (allow or deny)
	 */
	public HellblockFlag(@NotNull FlagType flag, @NotNull AccessType status) {
		this(flag, status, null);
	}

	/**
	 * Constructs a new {@code HellblockFlag} with optional custom data.
	 *
	 * @param flag   the flag type this object represents
	 * @param status the access status (allow or deny)
	 * @param data   optional string data associated with the flag (e.g., messages)
	 */
	public HellblockFlag(@NotNull FlagType flag, @NotNull AccessType status, @Nullable String data) {
		this.flag = flag;
		this.status = status;
		this.data = data;
	}

	/**
	 * Returns the type of this flag.
	 *
	 * @return the {@link FlagType} for this flag
	 */
	@NotNull
	public FlagType getFlag() {
		return this.flag;
	}

	/**
	 * Returns the access status of this flag.
	 *
	 * @return the {@link AccessType} value
	 */
	@NotNull
	public AccessType getStatus() {
		return this.status;
	}

	/**
	 * Returns the custom data associated with this flag, if any.
	 *
	 * @return the flag data string or {@code null} if none
	 */
	@Nullable
	public String getData() {
		return this.data;
	}

	/**
	 * Sets the optional data value associated with this flag.
	 *
	 * @param data the string data to associate with this flag
	 */
	public void setData(@Nullable String data) {
		this.data = data;
	}

	/**
	 * Determines whether this flag currently holds its default value.
	 * <p>
	 * A flag is considered default when its status matches the default defined by
	 * its {@link FlagType}.
	 *
	 * @return {@code true} if this flag uses the default status, otherwise
	 *         {@code false}
	 */
	public boolean isDefault() {
		return status == (flag.getDefaultValue() ? AccessType.ALLOW : AccessType.DENY);
	}

	/**
	 * Creates a deep copy of this {@code HellblockFlag} instance.
	 * <p>
	 * The copy contains the same flag type, access status, and data string.
	 *
	 * @return a cloned {@code HellblockFlag} with identical values
	 */
	@NotNull
	public final HellblockFlag copy() {
		return new HellblockFlag(this.flag, this.status, this.data != null ? new String(this.data) : null);
	}

	/**
	 * Represents all possible global flag definitions.
	 * <p>
	 * Each flag defines a name and its default access value. Flags are immutable
	 * and globally shared between all islands.
	 */
	public enum FlagType {
		@SerializedName("blockBreak")
		BLOCK_BREAK("block-break", false),

		@SerializedName("blockPlace")
		BLOCK_PLACE("block-place", false),

		@SerializedName("damageAnimals")
		DAMAGE_ANIMALS("damage-animals", false),

		@SerializedName("mobDamage")
		MOB_DAMAGE("mob-damage", true),

		@SerializedName("mobSpawning")
		MOB_SPAWNING("mob-spawning", true),

		@SerializedName("pvp")
		PVP("pvp", false),

		@SerializedName("tnt")
		TNT("tnt", false),

		@SerializedName("chestAccess")
		CHEST_ACCESS("chest-access", false),

		@SerializedName("use")
		USE("use", false),

		@SerializedName("useAnvil")
		USE_ANVIL("use-anvil", false),

		@SerializedName("useDripleaf")
		USE_DRIPLEAF("use-dripleaf", false),

		@SerializedName("vehiclePlace")
		PLACE_VEHICLE("vehicle-place", false),

		@SerializedName("vehicleDestroy")
		DESTROY_VEHICLE("vehicle-destroy", false),

		@SerializedName("ride")
		RIDE("ride", false),

		@SerializedName("enderpearl")
		ENDERPEARL("enderpearl", true),

		@SerializedName("blockTrampling")
		TRAMPLE_BLOCKS("block-trampling", false),

		@SerializedName("itemFrameRotation")
		ITEM_FRAME_ROTATE("item-frame-rotation", false),

		@SerializedName("chorusFruitTeleport")
		CHORUS_TELEPORT("chorus-fruit-teleport", true),

		@SerializedName("lighter")
		LIGHTER("lighter", false),

		@SerializedName("fireworkDamage")
		FIREWORK_DAMAGE("firework-damage", false),

		@SerializedName("respawnAnchors")
		RESPAWN_ANCHORS("respawn-anchors", false),

		@SerializedName("windChargeBurst")
		WIND_CHARGE_BURST("wind-charge-burst", false),

		@SerializedName("potionSplash")
		POTION_SPLASH("potion-splash", false),

		@SerializedName("interact")
		INTERACT("interact", false),

		@SerializedName("sleep")
		SLEEP("sleep", false),

		@SerializedName("snowmanTrails")
		SNOWMAN_TRAILS("snowman-trails", true),

		@SerializedName("ravagerGrief")
		RAVAGER_RAVAGE("ravager-grief", true),

		@SerializedName("endermanGrief")
		ENDER_BUILD("enderman-grief", true),

		@SerializedName("breezeChargeExplosion")
		BREEZE_WIND_CHARGE("breeze-charge-explosion", true),

		@SerializedName("ghastFireball")
		GHAST_FIREBALL("ghast-fireball", true),

		@SerializedName("fallDamage")
		FALL_DAMAGE("fall-damage", true),

		@SerializedName("creeperExplosion")
		CREEPER_EXPLOSION("creeper-explosion", true),

		@SerializedName("entityPaintingDestroy")
		ENTITY_PAINTING_DESTROY("entity-painting-destroy", false),

		@SerializedName("entityItemFrameDestroy")
		ENTITY_ITEM_FRAME_DESTROY("entity-item-frame-destroy", false),

		@SerializedName("itemDrop")
		ITEM_DROP("item-drop", true),

		@SerializedName("itemPickup")
		ITEM_PICKUP("item-pickup", true),

		@SerializedName("expDrops")
		EXP_DROPS("exp-drops", true),

		@SerializedName("naturalHealthRegen")
		HEALTH_REGEN("natural-health-regen", true),

		@SerializedName("naturalHungerDrain")
		HUNGER_DRAIN("natural-hunger-drain", true),

		@SerializedName("entry")
		ENTRY("entry", true),

		@SerializedName("greeting")
		GREET_MESSAGE("greeting", true),

		@SerializedName("farewell")
		FAREWELL_MESSAGE("farewell", true),

		@SerializedName("invincible")
		INVINCIBILITY("invincible", false),

		@SerializedName("build")
		BUILD("build", true);

		private final String name;
		private final boolean defaultValue;

		FlagType(@NotNull String name, boolean defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		/**
		 * Returns the internal name of this flag.
		 *
		 * @return the flag’s name key
		 */
		@NotNull
		public String getName() {
			return this.name;
		}

		/**
		 * Returns the default permission value for this flag.
		 *
		 * @return {@code true} if allowed by default, otherwise {@code false}
		 */
		public boolean getDefaultValue() {
			return this.defaultValue;
		}
	}

	/**
	 * Represents the possible access states for a flag: ALLOW or DENY.
	 */
	public enum AccessType {
		@SerializedName("allowed")
		ALLOW(true),

		@SerializedName("denied")
		DENY(false);

		private final boolean value;

		AccessType(boolean value) {
			this.value = value;
		}

		/**
		 * Returns the boolean representation of this access type.
		 *
		 * @return {@code true} for ALLOW, {@code false} for DENY
		 */
		public boolean getReturnValue() {
			return this.value;
		}
	}
}