package com.swiftlicious.hellblock.listeners;

import org.bukkit.Material;

public enum MaterialType {
	NETHERRACK(Material.NETHERRACK, "LEATHER", "STONE"), GLOWSTONE(Material.GLOWSTONE, "GOLDEN", "GOLDEN"),
	QUARTZ(Material.QUARTZ, "IRON", "IRON"), NETHERSTAR(Material.NETHER_STAR, "DIAMOND", "DIAMOND");

	private final Material material;
	private final String armorType;
	private final String toolType;

	MaterialType(Material material, String armorType, String toolType) {
		this.material = material;
		this.armorType = armorType;
		this.toolType = toolType;
	}

	public Material getMaterial() {
		return material;
	}

	public String getArmorIdentifier() {
		return armorType;
	}

	public String getToolIdentifier() {
		return toolType;
	}
}