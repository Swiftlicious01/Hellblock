package com.swiftlicious.hellblock.protection;

public class HellblockFlag {

	private FlagType flag;
	private AccessType status;

	public HellblockFlag(FlagType flag, AccessType status) {
		this.flag = flag;
		this.status = status;
	}

	public FlagType getFlag() {
		return this.flag;
	}

	public AccessType getStatus() {
		return this.status;
	}

	public enum FlagType {
		BLOCK_BREAK("block-break", false), BLOCK_PLACE("block-place", false), DAMAGE_ANIMALS("damage-animals", false),
		MOB_DAMAGE("mob-damage", true), MOB_SPAWNING("mob-spawning", true), PVP("pvp", false), TNT("tnt", false),
		CHEST_ACCESS("chest-access", false), USE("use", false), USE_ANVIL("use-anvil", false),
		USE_DRIPLEAF("use-dripleaf", false), PLACE_VEHICLE("vehicle-place", false),
		DESTROY_VEHICLE("vehicle-destroy", false), RIDE("ride", false), ENDERPEARL("enderpearl", true),
		TRAMPLE_BLOCKS("block-trampling", false), ITEM_FRAME_ROTATE("item-frame-rotation", false),
		CHORUS_TELEPORT("chorus-fruit-teleport", true), LIGHTER("lighter", false),
		FIREWORK_DAMAGE("firework-damage", false), RESPAWN_ANCHORS("respawn-anchors", false),
		WIND_CHARGE_BURST("wind-charge-burst", false), POTION_SPLASH("potion-splash", false),
		INTERACT("interact", false), SLEEP("sleep", false), SNOWMAN_TRAILS("snowman-trails", true),
		ENDER_BUILD("enderman-grief", true), GHAST_FIREBALL("ghast-fireball", true), FALL_DAMAGE("fall-damage", true),
		HEALTH_REGEN("natural-health-regen", true), HUNGER_DRAIN("natural-hunger-drain", true), ENTRY("entry", true),
		GREET_MESSAGE("greeting", true), FAREWELL_MESSAGE("farewell", true), INVINCIBILITY("invincible", false),
		BUILD("build", true);

		private String name;
		private boolean defaultValue;

		FlagType(String name, boolean defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		public String getName() {
			return this.name;
		}

		public boolean getDefaultValue() {
			return this.defaultValue;
		}
	}

	public enum AccessType {
		ALLOW(true), DENY(false);

		private boolean value;

		AccessType(boolean value) {
			this.value = value;
		}

		public boolean getReturnValue() {
			return this.value;
		}
	}
}
