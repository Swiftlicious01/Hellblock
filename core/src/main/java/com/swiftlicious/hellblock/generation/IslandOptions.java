package com.swiftlicious.hellblock.generation;

import com.google.gson.annotations.SerializedName;

/**
 * Defines the island generation options available during Hellblock island
 * creation.
 *
 * <ul>
 * <li>{@code DEFAULT} – The standard island layout.</li>
 * <li>{@code CLASSIC} – A legacy or classic version of the island layout for
 * nostalgic or alternate gameplay.</li>
 * <li>{@code SCHEMATIC} – A fully custom island structure loaded from a
 * predefined schematic file.</li>
 * </ul>
 *
 * <p>
 * This setting influences how the base island is constructed during
 * initialization and resets.
 * </p>
 */
public enum IslandOptions {
	@SerializedName("default")
	DEFAULT,

	@SerializedName("classic")
	CLASSIC,

	@SerializedName("schematic")
	SCHEMATIC;
}
