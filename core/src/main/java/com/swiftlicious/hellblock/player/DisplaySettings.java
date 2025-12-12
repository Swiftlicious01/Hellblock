package com.swiftlicious.hellblock.player;

import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.swiftlicious.hellblock.utils.adapters.HellblockTypeAdapterFactory.EmptyCheck;

/**
 * The {@code DisplaySettings} class holds user-configurable display preferences
 * for an island, including its name, bio, and where these values are displayed
 * (e.g., in title or chat).
 * <p>
 * This class also tracks whether the name and bio are currently using default
 * values.
 */
public class DisplaySettings implements EmptyCheck {

	@Expose
	@SerializedName("islandName")
	protected String islandName;

	@Expose
	@SerializedName("isDefaultIslandName")
	protected boolean defaultIslandName;

	@Expose
	@SerializedName("islandBio")
	protected String islandBio;

	@Expose
	@SerializedName("isDefaultIslandBio")
	protected boolean defaultIslandBio;

	@Expose
	@SerializedName("displayChoice")
	protected DisplayChoice displayChoice;

	/**
	 * Constructs a new {@code DisplaySettings} instance with the given name, bio,
	 * and display type. Default flags for name and bio are initially set to
	 * {@code true}.
	 *
	 * @param islandName    the display name of the island
	 * @param islandBio     the description or bio of the island
	 * @param displayChoice where to show the name/bio (e.g., chat or title)
	 */
	public DisplaySettings(@NotNull String islandName, @NotNull String islandBio,
			@NotNull DisplayChoice displayChoice) {
		this.islandName = islandName;
		this.islandBio = islandBio;
		this.displayChoice = displayChoice;
		this.defaultIslandName = true;
		this.defaultIslandBio = true;
	}

	/**
	 * Returns the island's display name.
	 *
	 * @return the island name
	 */
	@NotNull
	public String getIslandName() {
		if (this.islandName == null) {
			return "";
		}
		return this.islandName;
	}

	/**
	 * Sets the island's display name.
	 *
	 * @param islandName the new name to set
	 */
	public void setIslandName(@NotNull String islandName) {
		this.islandName = islandName;
	}

	/**
	 * Returns the island's bio or description.
	 *
	 * @return the island bio
	 */
	@NotNull
	public String getIslandBio() {
		if (this.islandBio == null) {
			return "";
		}
		return this.islandBio;
	}

	/**
	 * Sets the island's bio or description.
	 *
	 * @param islandBio the new bio to set
	 */
	public void setIslandBio(@NotNull String islandBio) {
		this.islandBio = islandBio;
	}

	/**
	 * Returns the current display choice (chat or title).
	 *
	 * @return the current display choice
	 */
	@NotNull
	public DisplayChoice getDisplayChoice() {
		if (this.displayChoice == null) {
			return DisplayChoice.CHAT;
		}
		return this.displayChoice;
	}

	/**
	 * Sets the display choice (chat or title).
	 *
	 * @param displayChoice the new display choice
	 */
	public void setDisplayChoice(@NotNull DisplayChoice displayChoice) {
		this.displayChoice = displayChoice;
	}

	/**
	 * Returns whether the island name is using the default value.
	 *
	 * @return {@code true} if using the default name; {@code false} otherwise
	 */
	public boolean isDefaultIslandName() {
		return this.defaultIslandName;
	}

	/**
	 * Returns whether the island bio is using the default value.
	 *
	 * @return {@code true} if using the default bio; {@code false} otherwise
	 */
	public boolean isDefaultIslandBio() {
		return this.defaultIslandBio;
	}

	/**
	 * Marks the island name as using the default.
	 */
	public void setAsDefaultIslandName() {
		this.defaultIslandName = true;
	}

	/**
	 * Marks the island bio as using the default.
	 */
	public void setAsDefaultIslandBio() {
		this.defaultIslandBio = true;
	}

	/**
	 * Marks the island name as not using the default.
	 */
	public void isNotDefaultIslandName() {
		this.defaultIslandName = false;
	}

	/**
	 * Marks the island bio as not using the default.
	 */
	public void isNotDefaultIslandBio() {
		this.defaultIslandBio = false;
	}

	/**
	 * Creates an instance of {@code DisplaySettings} with default values. This
	 * includes empty name/bio strings and default display choice set to
	 * {@code TITLE}.
	 *
	 * @return a new {@code DisplaySettings} instance with default values
	 */
	@NotNull
	public static DisplaySettings empty() {
		return new DisplaySettings("", "", DisplayChoice.CHAT);
	}

	/**
	 * Creates a deep copy of this {@code DisplaySettings} instance.
	 *
	 * @return a new {@code DisplaySettings} object with identical values
	 */
	@NotNull
	public final DisplaySettings copy() {
		DisplaySettings copy = new DisplaySettings(islandName, islandBio, displayChoice);
		copy.defaultIslandName = this.defaultIslandName;
		copy.defaultIslandBio = this.defaultIslandBio;
		return copy;
	}

	@Override
	public boolean isEmpty() {
		return (islandName == null || islandName.isEmpty()) && (islandBio == null || islandBio.isEmpty())
				&& displayChoice == DisplayChoice.CHAT && defaultIslandName && defaultIslandBio;
	}

	/**
	 * Determines where the island display settings are shown (chat or title).
	 */
	public enum DisplayChoice {
		@SerializedName("chat")
		CHAT,

		@SerializedName("title")
		TITLE;
	}
}