package com.swiftlicious.hellblock.listeners.invasion;

public class InvasionProfile {

	private final InvasionDifficulty difficulty;
	private final int waveCount;
	private final int mobsPerWave;
	private final boolean eliteMobsEnabled;
	private final boolean bossHasBuffs;
	private final float specialMobChance; // % chance to spawn berserker/shaman/hoglin
	private final float synergyChance; // % chance to apply mob-to-mob synergy
	private final float lootMultiplier; // used for enhanced loot (future)
	private final int maxSpecialMobsPerWave;

	public InvasionProfile(InvasionDifficulty difficulty) {
		this.difficulty = difficulty;

		switch (difficulty) {
		case EMBER -> {
			waveCount = 2;
			mobsPerWave = 4;
			eliteMobsEnabled = false;
			bossHasBuffs = false;
			specialMobChance = 0.0f;
			synergyChance = 0.0f;
			lootMultiplier = 1.0f;
			maxSpecialMobsPerWave = 0;
		}
		case ASHEN -> {
			waveCount = 3;
			mobsPerWave = 5;
			eliteMobsEnabled = false;
			bossHasBuffs = true;
			specialMobChance = 0.1f;
			synergyChance = 0.05f;
			lootMultiplier = 1.0f;
			maxSpecialMobsPerWave = 1;
		}
		case INFERNAL -> {
			waveCount = 4;
			mobsPerWave = 6;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.2f;
			synergyChance = 0.15f;
			lootMultiplier = 1.1f;
			maxSpecialMobsPerWave = 2;
		}
		case HELLFIRE -> {
			waveCount = 5;
			mobsPerWave = 7;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.3f;
			synergyChance = 0.2f;
			lootMultiplier = 1.2f;
			maxSpecialMobsPerWave = 2;
		}
		case ABYSSAL -> {
			waveCount = 6;
			mobsPerWave = 8;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.4f;
			synergyChance = 0.25f;
			lootMultiplier = 1.3f;
			maxSpecialMobsPerWave = 3;
		}
		case NETHERBORN -> {
			waveCount = 7;
			mobsPerWave = 9;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.5f;
			synergyChance = 0.35f;
			lootMultiplier = 1.4f;
			maxSpecialMobsPerWave = 3;
		}
		case APOCALYPTIC -> {
			waveCount = 8;
			mobsPerWave = 10;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.6f;
			synergyChance = 0.5f;
			lootMultiplier = 1.5f;
			maxSpecialMobsPerWave = 4;
		}
		case OBLIVION -> {
			waveCount = 9;
			mobsPerWave = 11;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.7f;
			synergyChance = 0.6f;
			lootMultiplier = 1.7f;
			maxSpecialMobsPerWave = 4;
		}
		case INFERNUM -> {
			waveCount = 10;
			mobsPerWave = 12;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.8f;
			synergyChance = 0.7f;
			lootMultiplier = 1.9f;
			maxSpecialMobsPerWave = 5;
		}
		case GEHENNA -> {
			waveCount = 12;
			mobsPerWave = 14;
			eliteMobsEnabled = true;
			bossHasBuffs = true;
			specialMobChance = 0.9f;
			synergyChance = 0.85f;
			lootMultiplier = 2.2f;
			maxSpecialMobsPerWave = 6;
		}
		default -> throw new IllegalStateException("Unknown difficulty: " + difficulty);
		}
	}

	public InvasionDifficulty getDifficulty() {
		return difficulty;
	}

	public int getWaveCount() {
		return waveCount;
	}

	public int getMobsPerWave() {
		return mobsPerWave;
	}

	public boolean isEliteMobsEnabled() {
		return eliteMobsEnabled;
	}

	public boolean doesBossHaveBuffs() {
		return bossHasBuffs;
	}

	public float getSpecialMobChance() {
		return specialMobChance;
	}

	public float getSynergyChance() {
		return synergyChance;
	}

	public float getLootMultiplier() {
		return lootMultiplier;
	}

	public int getMaxSpecialMobsPerWave() {
		return maxSpecialMobsPerWave;
	}
}