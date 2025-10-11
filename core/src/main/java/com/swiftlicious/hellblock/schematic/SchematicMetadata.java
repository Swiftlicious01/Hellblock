package com.swiftlicious.hellblock.schematic;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SchematicMetadata {

	private final @Nullable Vector home;
	private final @Nullable Vector chest;
	private final @Nullable Vector tree;
	private final float yaw;
	private final @Nullable String author;

	public SchematicMetadata(@Nullable Vector home, @Nullable Vector chest, @Nullable Vector tree, float yaw,
			@Nullable String author) {
		this.home = home;
		this.chest = chest;
		this.tree = tree;
		this.yaw = yaw;
		this.author = author;
	}

	public @Nullable Vector getHome() {
		return home;
	}

	public @Nullable Vector getChest() {
		return chest;
	}

	public @Nullable Vector getTree() {
		return tree;
	}

	public float getYaw() {
		return yaw;
	}

	public @Nullable String getAuthor() {
		return author;
	}
}