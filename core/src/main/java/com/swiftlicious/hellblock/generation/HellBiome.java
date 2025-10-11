package com.swiftlicious.hellblock.generation;

import org.bukkit.block.Biome;

import com.swiftlicious.hellblock.utils.RandomUtils;

public enum HellBiome {

	SOUL_SAND_VALLEY("Soul Sand Valley", Biome.SOUL_SAND_VALLEY), NETHER_WASTES("Nether Wastes", Biome.NETHER_WASTES),
	WARPED_FOREST("Warped Forest", Biome.WARPED_FOREST), CRIMSON_FOREST("Crimson Forest", Biome.CRIMSON_FOREST),
	BASALT_DELTAS("Basalt Deltas", Biome.BASALT_DELTAS), NETHER_FORTRESS("Nether Fortress", null);

	protected final String name;
	protected final Biome converted;

	private HellBiome(String name, Biome converted) {
		this.name = name;
		this.converted = converted;
	}

	public String getName() {
		return this.name;
	}

	public Biome getConvertedBiome() {
		if (this == NETHER_FORTRESS) {
			return RandomUtils.generateRandomBiome();
		}
		return this.converted;
	}
}
