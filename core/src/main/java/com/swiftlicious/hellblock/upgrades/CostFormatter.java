package com.swiftlicious.hellblock.upgrades;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.utils.StringUtils;

/**
 * Utility class for formatting and describing upgrade costs.
 * <p>
 * Handles conversion of raw item names to readable formats, pluralization, and
 * construction of combined cost descriptions for display in chat or UI.
 */
public class CostFormatter {

	private static final Set<String> UNCOUNTABLE_SUFFIXES = Set.of("STONE", "COBBLESTONE", "NETHERRACK", "BASALT",
			"DEEPSLATE", "BLACKSTONE", "TUFF", "ANDESITE", "DIORITE", "GRANITE", "DIRT", "GRAVEL", "SAND", "CLAY",
			"SOUL_SAND", "GLASS", "ICE", "SNOW", "POWDER", "WOOL", "CONCRETE", "TERRACOTTA", "DUST", "LEAVES", "SEEDS",
			"GRASS", "SEAGRASS", "NYLIUM", "WART", "WART_BLOCK", "MOSS", "SCULK", "REDSTONE", "SUGAR", "GUNPOWDER",
			"BEDROCK");

	private static final Set<String> COUNTABLE_SUFFIXES = Set.of("INGOT", "NUGGET", "LOG", "STICK", "GEM", "APPLE",
			"CARROT", "POTATO", "BUCKET", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SWORD", "AXE", "PICKAXE",
			"SHOVEL", "HOE", "EGG", "COOKIE", "PEARL", "EYE", "DYE", "BEEF", "MUTTON", "RABBIT", "CHOP", "FISH",
			"ARROW", "BOAT", "MINECART", "BRICK", "BOOK");

	/** Currency formatter based on the plugin's configured locale */
	public static final NumberFormat MONEY_FORMAT = NumberFormat
			.getCurrencyInstance(HellblockPlugin.getInstance().getTranslationManager().getForcedLocale());

	/**
	 * Formats a list of {@link UpgradeCost}s into a single human-readable string.
	 * <p>
	 * Item-based costs are listed first, followed by others (e.g., money, XP).
	 *
	 * @param costs    the list of costs to format
	 * @param handlers map of registered cost handlers for describing costs
	 * @return a formatted string of all costs, e.g., "2 × Iron Ingots & 50 XP"
	 */
	@NotNull
	public static String format(@NotNull List<UpgradeCost> costs, @NotNull Map<UpgradeCostType, CostHandler> handlers) {
		List<String> itemDescriptions = new ArrayList<>();
		List<String> otherDescriptions = new ArrayList<>();

		for (UpgradeCost cost : costs) {
			UpgradeCostType type = cost.getType();
			CostHandler handler = handlers.get(type);

			String desc;
			if (handler != null) {
				desc = handler.describe(cost);
			} else if (type == UpgradeCostType.ITEM) {
				List<String> itemNames = Arrays.stream(cost.getItem().split(",")).map(String::trim)
						.map(CostFormatter::toPrettyName).map(name -> pluralizeIfNeeded(name, cost.getAmount()))
						.toList();

				String itemPart = switch (itemNames.size()) {
				case 0 -> "";
				case 1 -> itemNames.get(0);
				case 2 -> itemNames.get(0) + " & " + itemNames.get(1);
				default -> String.join(", ", itemNames.subList(0, itemNames.size() - 1)) + " & "
						+ itemNames.get(itemNames.size() - 1);
				};

				desc = String.format("%.0f × %s", cost.getAmount(), itemPart);
				itemDescriptions.add(desc);
				continue;
			} else if (type == UpgradeCostType.MONEY) {
				desc = MONEY_FORMAT.format(cost.getAmount());
			} else if (type == UpgradeCostType.POINTS) {
				desc = String.format("%.0f Points", (int) cost.getAmount());
			} else if (UpgradeCostType.isExpUpgradeCostType(type)) {
				desc = String.format("%.0f XP", cost.getAmount());
			} else {
				desc = String.format("%.0f %s", cost.getAmount(), StringUtils.toProperCase(type.toString()));
			}

			otherDescriptions.add(desc);
		}

		List<String> allParts = new ArrayList<>();
		if (!itemDescriptions.isEmpty())
			allParts.addAll(itemDescriptions);
		if (!otherDescriptions.isEmpty())
			allParts.addAll(otherDescriptions);

		if (allParts.isEmpty())
			return "";
		if (allParts.size() == 1)
			return allParts.get(0);
		if (allParts.size() == 2)
			return allParts.get(0) + " & " + allParts.get(1);

		return String.join(", ", allParts.subList(0, allParts.size() - 1)) + " & " + allParts.get(allParts.size() - 1);
	}

	/**
	 * Parses a comma- or newline-separated string of item names into a list.
	 *
	 * @param rawItemData the raw item string
	 * @return a list of trimmed item names
	 */
	@NotNull
	public static List<String> parseItems(@Nullable String rawItemData) {
		if (rawItemData == null || rawItemData.isBlank()) {
			return List.of();
		}
		return Arrays.stream(rawItemData.split("[,\\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	/**
	 * Formats a list of item names into a human-readable phrase. Handles
	 * pluralization and conjunctions (e.g., "Iron Ingots & Gold Ingots").
	 *
	 * @param items  the list of item names
	 * @param amount the amount to determine singular/plural
	 * @return a formatted item string
	 */
	@NotNull
	public static String formatItemList(@NotNull List<String> items, double amount) {
		if (items.isEmpty())
			return "items";

		List<String> prettyItems = items.stream().map(CostFormatter::toPrettyName)
				.map(name -> pluralizeIfNeeded(name, amount)).toList();

		if (prettyItems.size() == 1)
			return prettyItems.get(0);
		if (prettyItems.size() == 2)
			return prettyItems.get(0) + " & " + prettyItems.get(1);

		return String.join(", ", prettyItems.subList(0, prettyItems.size() - 1)) + " & "
				+ prettyItems.get(prettyItems.size() - 1);
	}

	/**
	 * Converts a raw material name like {@code IRON_INGOT} to a nicely formatted
	 * name like {@code Iron Ingot}.
	 *
	 * @param rawName the raw enum-style material name
	 * @return a human-readable version of the name
	 */
	@NotNull
	public static String toPrettyName(@Nullable String rawName) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		return Arrays.stream(rawName.split("_"))
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	/**
	 * Pluralizes the name if the amount is greater than 1 and the item is
	 * considered countable.
	 *
	 * @param prettyName the item name (e.g., "Iron Ingot")
	 * @param amount     the quantity of items
	 * @return pluralized name if needed
	 */
	@NotNull
	private static String pluralizeIfNeeded(@NotNull String prettyName, double amount) {
		if (amount <= 1)
			return prettyName;

		String rawKey = prettyName.replace(" ", "_").toUpperCase(Locale.ROOT);
		org.bukkit.Material mat = org.bukkit.Material.matchMaterial(rawKey);
		String enumName = (mat != null ? mat.name() : rawKey);

		String[] tokens = enumName.split("_");
		String last = tokens[tokens.length - 1];

		if (last.endsWith("S") || UNCOUNTABLE_SUFFIXES.contains(last)) {
			return prettyName;
		}
		if (COUNTABLE_SUFFIXES.contains(last)) {
			return pluralizeLastWord(prettyName);
		}
		return prettyName;
	}

	/**
	 * Adds an "s" or converts "y" to "ies" for the last word of a name (e.g.,
	 * "Berry" → "Berries").
	 *
	 * @param prettyName the original name
	 * @return the pluralized name
	 */
	@NotNull
	private static String pluralizeLastWord(@NotNull String prettyName) {
		int idx = prettyName.lastIndexOf(' ');
		String head = (idx == -1) ? "" : prettyName.substring(0, idx + 1);
		String lastWord = (idx == -1) ? prettyName : prettyName.substring(idx + 1);

		if (lastWord.endsWith("s"))
			return prettyName;

		if (lastWord.length() > 1 && lastWord.endsWith("y") && !isVowel(lastWord.charAt(lastWord.length() - 2))) {
			lastWord = lastWord.substring(0, lastWord.length() - 1) + "ies";
		} else {
			lastWord = lastWord + "s";
		}
		return head + lastWord;
	}

	/** Helper for vowel check during pluralization logic */
	private static boolean isVowel(char c) {
		return switch (Character.toLowerCase(c)) {
		case 'a', 'e', 'i', 'o', 'u' -> true;
		default -> false;
		};
	}
}