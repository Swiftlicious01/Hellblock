package com.swiftlicious.hellblock.generation;

import org.bukkit.block.Biome;
import org.jetbrains.annotations.Nullable;

import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.RandomUtils;

/**
 * Represents the set of custom Nether biomes available for Hellblock islands.
 *
 * <p>
 * Each enum constant maps to a corresponding {@link Biome} from the Minecraft
 * API, except for {@code NETHER_FORTRESS}, which dynamically selects a random
 * biome using {@link RandomUtils#generateRandomBiome()} when accessed.
 * </p>
 *
 * <p>
 * This enum allows abstraction over raw Minecraft biomes while enabling custom
 * logic for biome assignment in island generation, visual theming, or gameplay
 * effects.
 * </p>
 */
public enum HellBiome {
	@SerializedName("netherWastes")
	NETHER_WASTES(Biome.NETHER_WASTES),

	@SerializedName("soulSandValley")
	SOUL_SAND_VALLEY(Biome.SOUL_SAND_VALLEY),

	@SerializedName("warpedForest")
	WARPED_FOREST(Biome.WARPED_FOREST),

	@SerializedName("crimsonForest")
	CRIMSON_FOREST(Biome.CRIMSON_FOREST),

	@SerializedName("basaltDeltras")
	BASALT_DELTAS(Biome.BASALT_DELTAS),

	@SerializedName("netherFortress")
	NETHER_FORTRESS(null);

	protected final Biome converted;

	private HellBiome(@Nullable Biome converted) {
		this.converted = converted;
	}

	/**
	 * Returns the mapped {@link Biome} for this HellBiome. For
	 * {@code NETHER_FORTRESS}, a random biome is returned at call time.
	 *
	 * @return the corresponding Minecraft Biome
	 */
	public Biome getConvertedBiome() {
		if (this == NETHER_FORTRESS) {
			return RandomUtils.generateRandomBiome();
		}
		return this.converted;
	}
}