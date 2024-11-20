package com.swiftlicious.hellblock.generation;

import org.bukkit.Material;

public enum HellBiome {

	SOUL_SAND_VALLEY("Soul Sand Valley", Material.SOUL_SOIL), NETHER_WASTES("Nether Wastes", Material.NETHERRACK),
	WARPED_FOREST("Warped Forest", Material.WARPED_STEM), CRIMSON_FOREST("Crimson Forest", Material.CRIMSON_STEM),
	BASALT_DELTAS("Basalt Deltas", Material.NETHERITE_BLOCK);

	private final String name;
	private final Material material;

	private HellBiome(String name, Material material) {
		this.name = name;
		this.material = material;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Material getType() {
		return this.material;
	}
}
