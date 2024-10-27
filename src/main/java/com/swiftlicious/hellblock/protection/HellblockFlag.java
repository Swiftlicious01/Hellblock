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
		BLOCK_BREAK("block-break"), BLOCK_PLACE("block-place"), DAMAGE_ANIMALS("damage-animals"),
		MOB_DAMAGE("mob-damage"), MOB_SPAWNING("mob-spawning"), PVP("pvp"), TNT("tnt"), CHEST_ACCESS("chest-access"),
		USE("use"), USE_ANVIL("use-anvil"), USE_DRIPLEAF("use-dripleaf"), PLACE_VEHICLE("vehicle-place"),
		DESTROY_VEHICLE("vehicle-destroy"), RIDE("ride"), ENDERPEARL("enderpearl"), TRAMPLE_BLOCKS("block-trampling"),
		ITEM_FRAME_ROTATE("item-frame-rotation"), CHORUS_TELEPORT("chorus-fruit-teleport"), LIGHTER("lighter"),
		FIREWORK_DAMAGE("firework-damage"), RESPAWN_ANCHORS("respawn-anchors"), WIND_CHARGE_BURST("wind-charge-burst"),
		POTION_SPLASH("potion-splash"), SNOWMAN_TRAILS("snowman-trails");

		private String name;

		FlagType(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
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
