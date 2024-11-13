package com.swiftlicious.hellblock.placeholders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.HBConfig;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.objecthunter.exp4j.ExpressionBuilder;

public class PlaceholderManager implements PlaceholderManagerInterface {

	protected final HellblockPlugin instance;
	private final boolean hasPapi;
	private final Pattern pattern;
	private final Map<String, String> customPlaceholderMap;

	public PlaceholderManager(HellblockPlugin plugin) {
		instance = plugin;
		this.hasPapi = plugin.isHookedPluginEnabled("PlaceholderAPI");
		this.pattern = Pattern.compile("\\{[^{}]+}");
		this.customPlaceholderMap = new HashMap<>();
	}

	public void disable() {
		this.customPlaceholderMap.clear();
	}

	public void loadCustomPlaceholders() {
		YamlDocument config = HBConfig.getMainConfig();
		Section section = config.getSection("other-settings.placeholder-register");
		if (section != null) {
			for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
				registerCustomPlaceholder(entry.getKey(), (String) entry.getValue());
			}
		}
	}

	@Override
	public boolean registerCustomPlaceholder(String placeholder, String original) {
		if (this.customPlaceholderMap.containsKey(placeholder)) {
			return false;
		}
		this.customPlaceholderMap.put(placeholder, original);
		return true;
	}

	/**
	 * Set placeholders in a text string for a player.
	 *
	 * @param player The player for whom the placeholders should be set.
	 * @param text   The text string containing placeholders.
	 * @return The text string with placeholders replaced if PlaceholderAPI is
	 *         available; otherwise, the original text.
	 */
	@Override
	public String setPlaceholders(Player player, String text) {
		return hasPapi ? instance.getParseUtils().setPlaceholders(player, text) : text;
	}

	/**
	 * Set placeholders in a text string for an offline player.
	 *
	 * @param player The offline player for whom the placeholders should be set.
	 * @param text   The text string containing placeholders.
	 * @return The text string with placeholders replaced if PlaceholderAPI is
	 *         available; otherwise, the original text.
	 */
	@Override
	public String setPlaceholders(OfflinePlayer player, String text) {
		return hasPapi ? instance.getParseUtils().setPlaceholders(player, text) : text;
	}

	/**
	 * Detect and extract placeholders from a text string.
	 *
	 * @param text The text string to search for placeholders.
	 * @return A list of detected placeholders in the text.
	 */
	@Override
	public List<String> detectPlaceholders(String text) {
		List<String> placeholders = new ArrayList<>();
		Matcher matcher = pattern.matcher(text);
		while (matcher.find())
			placeholders.add(matcher.group());
		return placeholders;
	}

	/**
	 * Get the value associated with a single placeholder.
	 *
	 * @param player       The player for whom the placeholders are being resolved
	 *                     (nullable).
	 * @param placeholder  The placeholder to look up.
	 * @param placeholders A map of placeholders to their corresponding values.
	 * @return The value associated with the placeholder, or the original
	 *         placeholder if not found.
	 */
	@Override
	public String getSingleValue(@Nullable Player player, String placeholder, Map<String, String> placeholders) {
		String result = null;
		if (placeholders != null)
			result = placeholders.get(placeholder);
		if (result != null)
			return result;
		String custom = customPlaceholderMap.get(placeholder);
		if (custom == null)
			return placeholder;
		return setPlaceholders(player, custom);
	}

	/**
	 * Parse a text string by replacing placeholders with their corresponding
	 * values.
	 *
	 * @param player       The offline player for whom the placeholders are being
	 *                     resolved (nullable).
	 * @param text         The text string containing placeholders.
	 * @param placeholders A map of placeholders to their corresponding values.
	 * @return The text string with placeholders replaced by their values.
	 */
	@Override
	public String parse(@Nullable OfflinePlayer player, String text, Map<String, String> placeholders) {
		var list = detectPlaceholders(text);
		for (String papi : list) {
			String replacer = null;
			if (placeholders != null) {
				replacer = placeholders.get(papi);
			}
			if (replacer == null) {
				String custom = customPlaceholderMap.get(papi);
				if (custom != null) {
					replacer = setPlaceholders(player, parse(player, custom, placeholders));
				}
			}
			if (replacer != null) {
				text = text.replace(papi, replacer);
			}
		}
		return text;
	}

	/**
	 * Parse a list of text strings by replacing placeholders with their
	 * corresponding values.
	 *
	 * @param player       The player for whom the placeholders are being resolved
	 *                     (can be null for offline players).
	 * @param list         The list of text strings containing placeholders.
	 * @param replacements A map of custom replacements for placeholders.
	 * @return The list of text strings with placeholders replaced by their values.
	 */
	@Override
	public List<String> parse(@Nullable OfflinePlayer player, List<String> list, Map<String, String> replacements) {
		return list.stream().map(s -> parse(player, s, replacements)).collect(Collectors.toList());
	}

	@Override
	public double getExpressionValue(Player player, String formula, Map<String, String> vars) {
		return instance.getConfigUtils().getExpressionValue(player, formula, vars);
	}

	@Override
	public double getExpressionValue(String formula) {
		return new ExpressionBuilder(formula).build().evaluate();
	}
}
