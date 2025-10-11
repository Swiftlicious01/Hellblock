package com.swiftlicious.hellblock.upgrades;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

	protected static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.getDefault());

	public static String format(List<UpgradeCost> costs, Map<String, CostHandler> handlers) {
		// Separate item costs vs others
		List<String> itemDescriptions = new ArrayList<>();
		List<String> otherDescriptions = new ArrayList<>();

		for (UpgradeCost cost : costs) {
			String type = cost.getType().toUpperCase(Locale.ROOT);
			CostHandler handler = handlers.get(type);

			String desc;
			if (handler != null) {
				desc = handler.describe(cost);
			} else if ("ITEM".equals(type)) {
				// Handle multiple or single items
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
			} else if ("MONEY".equals(type)) {
				desc = MONEY_FORMAT.format(cost.getAmount());
			} else if ("EXP".equals(type) || "EXPERIENCE".equals(type) || "XP".equals(type)) {
				desc = String.format("%.0f XP", cost.getAmount());
			} else {
				desc = String.format("%.0f %s", cost.getAmount(), type);
			}

			otherDescriptions.add(desc);
		}

		// Combine all descriptions (items first, others next)
		List<String> allParts = new ArrayList<>();
		if (!itemDescriptions.isEmpty())
			allParts.addAll(itemDescriptions);
		if (!otherDescriptions.isEmpty())
			allParts.addAll(otherDescriptions);

		if (allParts.isEmpty())
			return "";

		// Format with "&" for 2 entries, commas + "&" for 3+
		if (allParts.size() == 1)
			return allParts.get(0);
		if (allParts.size() == 2)
			return allParts.get(0) + " & " + allParts.get(1);

		return String.join(", ", allParts.subList(0, allParts.size() - 1)) + " & " + allParts.get(allParts.size() - 1);
	}

	/** Parses raw item list definitions (single, inline, or multi-line) */
	public static List<String> parseItems(String rawItemData) {
		if (rawItemData == null || rawItemData.isBlank()) {
			return List.of();
		}

		return Arrays.stream(rawItemData.split("[,\\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	/** Nicely formats multiple item names in human-readable English */
	public static String formatItemList(List<String> items, double amount) {
		if (items.isEmpty())
			return "items";

		List<String> prettyItems = items.stream().map(CostFormatter::toPrettyName)
				.map(name -> pluralizeIfNeeded(name, amount)).toList();

		if (prettyItems.size() == 1) {
			return prettyItems.get(0);
		}
		if (prettyItems.size() == 2) {
			return prettyItems.get(0) + " & " + prettyItems.get(1);
		}

		return String.join(", ", prettyItems.subList(0, prettyItems.size() - 1)) + " & "
				+ prettyItems.get(prettyItems.size() - 1);
	}

	/** Converts "IRON_INGOT" → "Iron Ingot" */
	public static String toPrettyName(String rawName) {
		if (rawName == null || rawName.isEmpty()) {
			return "";
		}
		return Arrays.stream(rawName.split("_"))
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(Collectors.joining(" "));
	}

	// replace your pluralizeIfNeeded with this:
	private static String pluralizeIfNeeded(String prettyName, double amount) {
		if (amount <= 1)
			return prettyName;

		String rawKey = prettyName.replace(" ", "_").toUpperCase(Locale.ROOT);
		org.bukkit.Material mat = org.bukkit.Material.matchMaterial(rawKey);

		// Use the material enum tokens if we can; otherwise fall back to the raw key
		String enumName = (mat != null ? mat.name() : rawKey);
		String[] tokens = enumName.split("_");
		String last = tokens[tokens.length - 1]; // last token (e.g., INGOT, COBBLESTONE, NETHERRACK)

		// Already plural or known mass/uncountable → don't pluralize
		if (last.endsWith("S") || UNCOUNTABLE_SUFFIXES.contains(last)) {
			return prettyName;
		}

		// Known countable nouns → pluralize the last word only (e.g., "Oak Log" → "Oak
		// Logs")
		if (COUNTABLE_SUFFIXES.contains(last)) {
			return pluralizeLastWord(prettyName);
		}

		// Safe default: don't pluralize unknowns (prevents "Cobblestones",
		// "Netherracks", etc.)
		return prettyName;
	}

	private static String pluralizeLastWord(String prettyName) {
		int idx = prettyName.lastIndexOf(' ');
		String head = (idx == -1) ? "" : prettyName.substring(0, idx + 1);
		String lastWord = (idx == -1) ? prettyName : prettyName.substring(idx + 1);

		if (lastWord.endsWith("s"))
			return prettyName;

		// handle consonant + y → ies (e.g., "Berry" → "Berries")
		if (lastWord.length() > 1 && lastWord.endsWith("y") && !isVowel(lastWord.charAt(lastWord.length() - 2))) {
			lastWord = lastWord.substring(0, lastWord.length() - 1) + "ies";
		} else {
			lastWord = lastWord + "s";
		}
		return head + lastWord;
	}

	private static boolean isVowel(char c) {
		switch (Character.toLowerCase(c)) {
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			return true;
		default:
			return false;
		}
	}
}