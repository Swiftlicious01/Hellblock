package com.swiftlicious.hellblock.generation;

import org.bukkit.util.Vector;

public enum IslandVariant {
	DEFAULT(IslandOptions.DEFAULT, new Vector(0, 5, 1), // chest offset
			new Vector(0, 5, 0), // tree offset
			new Vector(0.5, 5, 2.5), // home offset
			-175f// yaw
	), CLASSIC(IslandOptions.CLASSIC, new Vector(-5, 3, -1), new Vector(0, 3, -5), new Vector(-0.5, 3, -0.5), 90f);

	private final IslandOptions options;
	private final Vector chestOffset;
	private final Vector treeOffset;
	private final Vector homeOffset;
	private final float homeYaw;

	IslandVariant(IslandOptions options, Vector chestOffset, Vector treeOffset, Vector homeOffset, float homeYaw) {
		this.options = options;
		this.chestOffset = chestOffset;
		this.treeOffset = treeOffset;
		this.homeOffset = homeOffset;
		this.homeYaw = homeYaw;
	}

	public IslandOptions getOptions() {
		return options;
	}

	public Vector getChestOffset() {
		return chestOffset;
	}

	public Vector getTreeOffset() {
		return treeOffset;
	}

	public Vector getHomeOffset() {
		return homeOffset;
	}

	public float getHomeYaw() {
		return homeYaw;
	}
}