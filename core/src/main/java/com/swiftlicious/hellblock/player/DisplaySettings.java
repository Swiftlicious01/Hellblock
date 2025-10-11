package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DisplaySettings {

	@Expose
	@SerializedName("name")
	protected String islandName;
	@Expose
	@SerializedName("defaultname")
	protected boolean defaultIslandName;

	@Expose
	@SerializedName("bio")
	protected String islandBio;
	@Expose
	@SerializedName("defaultbio")
	protected boolean defaultIslandBio;

	@Expose
	@SerializedName("display")
	protected DisplayChoice displayChoice;

	public DisplaySettings(@NotNull String islandName, @NotNull String islandBio,
			@NotNull DisplayChoice displayChoice) {
		this.islandName = islandName;
		this.islandBio = islandBio;
		this.displayChoice = displayChoice;
	}

	@NotNull
	public String getIslandName() {
		return this.islandName;
	}

	@NotNull
	public String getIslandBio() {
		return this.islandBio;
	}

	@NotNull
	public DisplayChoice getDisplayChoice() {
		return this.displayChoice;
	}

	public void setIslandName(@NotNull String islandName) {
		this.islandName = islandName;
	}

	public void setIslandBio(@NotNull String islandBio) {
		this.islandBio = islandBio;
	}

	public void setDisplayChoice(@NotNull DisplayChoice displayChoice) {
		this.displayChoice = displayChoice;
	}

	public boolean isDefaultIslandName() {
		return this.defaultIslandName;
	}

	public boolean isDefaultIslandBio() {
		return this.defaultIslandBio;
	}

	public void setAsDefaultIslandName() {
		this.defaultIslandName = true;
	}

	public void setAsDefaultIslandBio() {
		this.defaultIslandBio = true;
	}

	public void isNotDefaultIslandName() {
		this.defaultIslandName = false;
	}

	public void isNotDefaultIslandBio() {
		this.defaultIslandBio = false;
	}

	public enum DisplayChoice {
		TITLE, CHAT;
	}
}