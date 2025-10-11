package com.swiftlicious.hellblock.placeholders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.PlaceholderAPIUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class PlaceholderManager implements PlaceholderManagerInterface {

	protected final HellblockPlugin instance;
	private boolean hasPapi;
	private final Map<String, BiFunction<OfflinePlayer, Map<String, String>, String>> customPlaceholderMap = new HashMap<>();

	public PlaceholderManager(HellblockPlugin plugin) {
		instance = plugin;
	}

	/**
	 * Loads the placeholder manager, checking for PlaceholderAPI and registering
	 * default placeholders.
	 */
	@Override
	public void load() {
		this.hasPapi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
		this.customPlaceholderMap.put("{random}", (p, map) -> String.valueOf(RandomUtils.generateRandomDouble(0, 1)));
	}

	/**
	 * Unloads the placeholder manager, clearing all registered placeholders.
	 */
	@Override
	public void unload() {
		this.hasPapi = false;
		this.customPlaceholderMap.clear();
	}

	@Override
	public boolean registerCustomPlaceholder(String placeholder, String original) {
		if (this.customPlaceholderMap.containsKey(placeholder)) {
			return false;
		}
		this.customPlaceholderMap.put(placeholder, (p, map) -> PlaceholderAPIUtils.parse(p, parse(p, original, map)));
		return true;
	}

	@Override
	public boolean registerCustomPlaceholder(String placeholder,
			BiFunction<OfflinePlayer, Map<String, String>, String> provider) {
		if (this.customPlaceholderMap.containsKey(placeholder)) {
			return false;
		}
		this.customPlaceholderMap.put(placeholder, provider);
		return true;
	}

	@Override
	public List<String> resolvePlaceholders(String text) {
		final List<String> placeholders = new ArrayList<>();
		final Matcher matcher = PATTERN.matcher(text);
		while (matcher.find()) {
			placeholders.add(matcher.group());
		}
		return placeholders;
	}

	public boolean hasPapi() {
		return hasPapi;
	}

	private String setPlaceholders(OfflinePlayer player, String text) {
		return hasPapi ? PlaceholderAPIUtils.parse(player, text) : text;
	}

	@Override
	public String parseSingle(@Nullable OfflinePlayer player, String placeholder, Map<String, String> replacements) {
		String result = null;
		if (replacements != null) {
			result = replacements.get(placeholder);
		}
		if (result != null) {
			return result;
		}
		final String custom = Optional.ofNullable(customPlaceholderMap.get(placeholder))
				.map(supplier -> supplier.apply(player, replacements)).orElse(null);
		if (custom == null) {
			return placeholder;
		}
		return setPlaceholders(player, custom);
	}

	@Override
	public String parse(@Nullable OfflinePlayer player, String text, Map<String, String> replacements) {
		final var list = resolvePlaceholders(text);
		for (String papi : list) {
			String replacer = null;
			if (replacements != null) {
				replacer = replacements.get(papi);
			}
			if (replacer == null) {
				final String custom = Optional.ofNullable(customPlaceholderMap.get(papi))
						.map(supplier -> supplier.apply(player, replacements)).orElse(null);
				if (custom != null) {
					replacer = setPlaceholders(player, parse(player, custom, replacements));
				}
			}
			if (replacer != null) {
				text = text.replace(papi, replacer);
			}
		}
		return text;
	}

	@Override
	public List<String> parse(@Nullable OfflinePlayer player, List<String> list, Map<String, String> replacements) {
		return list.stream().map(s -> parse(player, s, replacements)).collect(Collectors.toList());
	}
}