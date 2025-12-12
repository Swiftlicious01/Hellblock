package com.swiftlicious.hellblock.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;

/**
 * The {@code DynamicText} class represents a dynamic text component that can
 * contain placeholder values resolved per-player.
 * 
 * <p>
 * It analyzes the initial raw text for placeholders, replacing them with format
 * specifiers and stores the original and latest evaluated versions of the text.
 * The {@code update()} method allows for runtime updates based on new
 * placeholder values.
 */
public class DynamicText {

	private final Player owner;
	private String originalValue;
	private String latestValue;
	private String[] placeholders;

	/**
	 * Constructs a new {@code DynamicText} object for a specific player and raw
	 * text value.
	 * 
	 * @param owner    the player this dynamic text is associated with
	 * @param rawValue the raw string containing potential placeholders
	 */
	public DynamicText(Player owner, String rawValue) {
		this.owner = owner;
		analyze(rawValue);
	}

	/**
	 * Analyzes the raw input string for placeholders using the
	 * {@link PlaceholderManager}. Replaces each detected placeholder with a
	 * {@code %s} format specifier and stores:
	 * <ul>
	 * <li>The formatted string with specifiers as {@code originalValue}</li>
	 * <li>The resolved placeholder list as {@code placeholders}</li>
	 * <li>The initial value as {@code latestValue}</li>
	 * </ul>
	 *
	 * @param value the input string to analyze for placeholders
	 */
	private void analyze(String value) {
		final List<String> placeholdersOwner = new ArrayList<>(
				HellblockPlugin.getInstance().getPlaceholderManager().resolvePlaceholders(value));
		String origin = value;
		for (String placeholder : placeholdersOwner) {
			origin = origin.replace(placeholder, "%s");
		}
		originalValue = origin;
		placeholders = placeholdersOwner.toArray(new String[0]);
		latestValue = originalValue;
	}

	/**
	 * Returns the most recently updated value of the dynamic text.
	 * 
	 * @return the latest rendered string with placeholders replaced
	 */
	public String getLatestValue() {
		return latestValue;
	}

	/**
	 * Updates the dynamic text by parsing the placeholders using the provided
	 * values. If the computed value is different from the previous one, updates
	 * {@code latestValue}.
	 *
	 * @param placeholders a map of placeholder keys to their replacement values
	 * @return {@code true} if the text has changed after update; {@code false}
	 *         otherwise
	 */
	public boolean update(Map<String, String> placeholders) {
		String string = originalValue;
		if (this.placeholders.length != 0) {
			final PlaceholderManager placeholderManager = HellblockPlugin.getInstance().getPlaceholderManager();
			if ("%s".equals(originalValue)) {
				// Only one placeholder in the original text
				string = placeholderManager.parseSingle(owner, this.placeholders[0], placeholders);
			} else {
				// Multiple placeholders
				final Object[] values = new String[this.placeholders.length];
				for (int i = 0; i < this.placeholders.length; i++) {
					values[i] = placeholderManager.parseSingle(owner, this.placeholders[i], placeholders);
				}
				string = originalValue.formatted(values);
			}
		}
		if (latestValue.equals(string)) {
			return false;
		}
		latestValue = string;
		return true;
	}
}