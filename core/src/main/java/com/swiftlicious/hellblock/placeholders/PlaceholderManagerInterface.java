package com.swiftlicious.hellblock.placeholders;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.api.Reloadable;

public interface PlaceholderManagerInterface extends Reloadable {

	Pattern PATTERN = Pattern.compile("\\{[^{}]+}");

	/**
	 * Registers a custom placeholder.
	 *
	 * @param placeholder the placeholder to be registered
	 * @param original    the original placeholder string for instance
	 *                    {@code %test_placeholder%}
	 * @return true if the placeholder was successfully registered, false otherwise
	 */
	boolean registerCustomPlaceholder(String placeholder, String original);

	/**
	 * Registers a custom placeholder.
	 *
	 * @param placeholder the placeholder to be registered
	 * @param provider    the value provider
	 * @return true if the placeholder was successfully registered, false otherwise
	 */
	boolean registerCustomPlaceholder(String placeholder,
			BiFunction<OfflinePlayer, Map<String, String>, String> provider);

	/**
	 * Resolves all placeholders within a given text.
	 *
	 * @param text the text to resolve placeholders in.
	 * @return a list of found placeholders.
	 */
	List<String> resolvePlaceholders(String text);

	/**
	 * Parses a single placeholder for the specified player, using the provided
	 * replacements.
	 *
	 * @param player       the player for whom the placeholder is being parsed
	 * @param placeholder  the placeholder to be parsed
	 * @param replacements a map of replacements to be used
	 * @return the parsed placeholder value
	 */
	String parseSingle(@Nullable OfflinePlayer player, String placeholder, Map<String, String> replacements);

	/**
	 * Parses placeholders in the given text for the specified player, using the
	 * provided replacements.
	 *
	 * @param player       the player for whom placeholders are being parsed
	 * @param text         the text containing placeholders
	 * @param replacements a map of replacements to be used
	 * @return the text with placeholders replaced
	 */
	String parse(@Nullable OfflinePlayer player, String text, Map<String, String> replacements);

	/**
	 * Parses placeholders in the given list of texts for the specified player,
	 * using the provided replacements.
	 *
	 * @param player       the player for whom placeholders are being parsed
	 * @param list         the list of texts containing placeholders
	 * @param replacements a map of replacements to be used
	 * @return the list of texts with placeholders replaced
	 */
	List<String> parse(@Nullable OfflinePlayer player, List<String> list, Map<String, String> replacements);
}