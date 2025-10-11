package com.swiftlicious.hellblock.generation;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

//NULL schematicName & author for DEFAULT & CLASSIC; only set for SCHEMATIC
public record IslandGenerationRequest(IslandOptions options, Vector chest, // relative to paste origin
		Vector tree, // relative to paste origin
		Vector home, // relative to paste origin
		float homeYaw, // yaw for home (used in default/classic)
		@Nullable String schematicName, @Nullable String author) {

	public static IslandGenerationRequest fromVariant(IslandVariant variant) {
		return new IslandGenerationRequest(variant.getOptions(), variant.getChestOffset(), variant.getTreeOffset(),
				variant.getHomeOffset(), variant.getHomeYaw(), // default/classic still has yaw
				null, // schematic name
				null // author
		);
	}

	public boolean isSchematic() {
		return schematicName != null && !schematicName.isEmpty();
	}
}
