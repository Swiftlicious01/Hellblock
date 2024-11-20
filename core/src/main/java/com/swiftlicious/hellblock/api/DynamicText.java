package com.swiftlicious.hellblock.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.placeholders.PlaceholderManager;

public class DynamicText {

	private final Player owner;
	private String originalValue;
	private String latestValue;
	private String[] placeholders;

	public DynamicText(Player owner, String rawValue) {
		this.owner = owner;
		analyze(rawValue);
	}

	private void analyze(String value) {
		// Analyze the provided text to find and replace placeholders with '%s'.
		// Store the original value, placeholders, and the initial latest value.
		List<String> placeholdersOwner = new ArrayList<>(
				HellblockPlugin.getInstance().getPlaceholderManager().resolvePlaceholders(value));
		String origin = value;
		for (String placeholder : placeholdersOwner) {
			origin = origin.replace(placeholder, "%s");
		}
		originalValue = origin;
		placeholders = placeholdersOwner.toArray(new String[0]);
		latestValue = originalValue;
	}

	public String getLatestValue() {
		return latestValue;
	}

	public boolean update(Map<String, String> placeholders) {
		String string = originalValue;
		if (this.placeholders.length != 0) {
			PlaceholderManager placeholderManager = HellblockPlugin.getInstance().getPlaceholderManager();
			if ("%s".equals(originalValue)) {
				string = placeholderManager.parseSingle(owner, this.placeholders[0], placeholders);
			} else {
				Object[] values = new String[this.placeholders.length];
				for (int i = 0; i < this.placeholders.length; i++) {
					values[i] = placeholderManager.parseSingle(owner, this.placeholders[i], placeholders);
				}
				string = String.format(originalValue, values);
			}
		}
		if (!latestValue.equals(string)) {
			latestValue = string;
			return true;
		}
		return false;
	}
}