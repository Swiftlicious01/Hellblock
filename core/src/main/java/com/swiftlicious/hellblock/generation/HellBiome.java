package com.swiftlicious.hellblock.generation;

import org.bukkit.Material;
import org.bukkit.block.Biome;

public enum HellBiome {

	SOUL_SAND_VALLEY("Soul Sand Valley", Material.SOUL_SOIL, Biome.SOUL_SAND_VALLEY),
	NETHER_WASTES("Nether Wastes", Material.NETHERRACK, Biome.NETHER_WASTES),
	WARPED_FOREST("Warped Forest", Material.WARPED_STEM, Biome.WARPED_FOREST),
	CRIMSON_FOREST("Crimson Forest", Material.CRIMSON_STEM, Biome.CRIMSON_FOREST),
	BASALT_DELTAS("Basalt Deltas", Material.NETHERITE_BLOCK, Biome.BASALT_DELTAS);

	private final String name;
	private final Material material;
	private final Biome converted;

	private HellBiome(String name, Material material, Biome converted) {
		this.name = name;
		this.material = material;
		this.converted = converted;
	}

	public String getName() {
		return this.name;
	}

	public Material getType() {
		return this.material;
	}

	public Biome getConvertedBiome() {
		return this.converted;
	}
}
