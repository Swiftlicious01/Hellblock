package com.swiftlicious.hellblock.player;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.NonNull;

public class LocationCacheData {

	@Expose
	@SerializedName("pistons")
	protected List<String> pistonLocations;
	@Expose
	@SerializedName("levelblocks")
	protected List<String> levelBlockLocations;

	public LocationCacheData(List<String> pistonLocations, List<String> levelBlockLocations) {
		this.pistonLocations = pistonLocations;
		this.levelBlockLocations = levelBlockLocations;
	}

	public List<String> getPistonLocations() {
		return this.pistonLocations;
	}

	public List<String> getLevelBlockLocations() {
		return this.levelBlockLocations;
	}

	public void setPistonLocations(List<String> pistonLocations) {
		this.pistonLocations = pistonLocations;
	}

	public void setLevelBlockLocations(List<String> levelBlockLocations) {
		this.levelBlockLocations = levelBlockLocations;
	}

	/**
	 * Creates an instance of LocationCacheData with default values (empty lists).
	 *
	 * @return a new instance of LocationCacheData with default values.
	 */
	public static @NonNull LocationCacheData empty() {
		return new LocationCacheData(new ArrayList<>(), new ArrayList<>());
	}

	public @NonNull LocationCacheData copy() {
		return new LocationCacheData(pistonLocations, levelBlockLocations);
	}
}
