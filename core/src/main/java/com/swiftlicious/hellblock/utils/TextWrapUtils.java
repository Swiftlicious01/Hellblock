package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;

import net.kyori.adventure.text.Component;

public final class TextWrapUtils {

	// Cached reflection handles
	private static Object FONT_WIDTH_CALCULATOR_INSTANCE = null;
	private static Method FONT_WIDTH_CALCULATE_METHOD = null;
	private static boolean FONT_WIDTH_INITIALIZED = false;
	private static boolean FONT_WIDTH_AVAILABLE = false;

	private TextWrapUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Initialize reflection handles for FontWidthCalculator. Called lazily on first
	 * access or manually during plugin enable.
	 */
	public static void initFontWidthCalculator() {
		if (FONT_WIDTH_INITIALIZED)
			return;

		FONT_WIDTH_INITIALIZED = true;
		try {
			Class<?> calcClass = Class.forName("io.papermc.paper.text.FontWidthCalculator");
			Method getMethod = calcClass.getMethod("get");
			FONT_WIDTH_CALCULATE_METHOD = calcClass.getMethod("calculate", Component.class);

			FONT_WIDTH_CALCULATOR_INSTANCE = getMethod.invoke(null);
			FONT_WIDTH_AVAILABLE = FONT_WIDTH_CALCULATOR_INSTANCE != null;

			HellblockPlugin.getInstance()
					.debug("Paper FontWidthCalculator initialized successfully (pixel-accurate wrapping active).");

		} catch (Throwable t) {
			FONT_WIDTH_AVAILABLE = false;
			HellblockPlugin.getInstance().debug("FontWidthCalculator not available — using fallback wrapping.");
		}
	}

	/**
	 * Dynamically wraps a line of text to fit chat width. Uses pixel-accurate
	 * wrapping if available (Paper + Adventure 4.25+), otherwise falls back to
	 * character-width wrapping.
	 */
	public static List<String> wrapLineWithIndentAdaptive(@NotNull Player player, @NotNull String text, int indent) {
		if (text.isEmpty())
			return List.of();

		try {
			if (VersionHelper.isPaper()) {
				return wrapLinePixelAccurate(player, text, indent);
			}
		} catch (Throwable ignored) {
			// If Paper-only APIs fail, fallback
		}

		int wrapWidth = estimateWrapWidth(player);
		return wrapLineWithIndent(text, wrapWidth, indent);
	}

	/**
	 * Pixel-accurate wrapping using Adventure's FontWidthCalculator. Automatically
	 * accounts for MiniMessage formatting (bold, obfuscated, etc.) and gracefully
	 * falls back to char width estimation if unavailable.
	 */
	public static List<String> wrapLinePixelAccurate(@NotNull Player player, @NotNull String text, int indent) {
		List<String> lines = new ArrayList<>();
		if (text.isEmpty())
			return lines;

		int maxPixels = estimateChatPixelWidth(player);
		String[] words = text.split("\\s+");
		StringBuilder current = new StringBuilder();
		String indentStr = " ".repeat(indent);
		int currentWidth = 0;

		for (String word : words) {
			String candidate = (current.length() == 0 ? "" : " ") + word;
			int wordWidth = getPixelWidth(candidate);

			// If adding this word exceeds width, wrap
			if (currentWidth + wordWidth > maxPixels) {
				lines.add(current.toString());
				current.setLength(0);
				current.append(indentStr).append(word);
				currentWidth = getPixelWidth(indentStr + word);
			} else {
				current.append(candidate);
				currentWidth += wordWidth;
			}
		}

		if (current.length() > 0)
			lines.add(current.toString());
		return lines;
	}

	/**
	 * Calculates the approximate pixel width of text. Uses Paper's
	 * FontWidthCalculator if present; otherwise estimates width.
	 */
	public static int getPixelWidth(@NotNull String text) {
		if (!FONT_WIDTH_INITIALIZED) {
			initFontWidthCalculator();
		}

		// --- Paper FontWidthCalculator (reflection cached) ---
		if (FONT_WIDTH_AVAILABLE && FONT_WIDTH_CALCULATOR_INSTANCE != null && FONT_WIDTH_CALCULATE_METHOD != null) {
			try {
				Component component = AdventureHelper.getMiniMessage().deserialize(text);
				return (int) FONT_WIDTH_CALCULATE_METHOD.invoke(FONT_WIDTH_CALCULATOR_INSTANCE, component);
			} catch (Throwable ignored) {
				// fallback if reflection or Paper API call fails
			}
		}

		// --- Fallback estimation for Spigot or Paper failure ---
		int width = 0;
		boolean bold = false;

		// Simplify text by removing MiniMessage tags but preserving bold segments
		String simplified = text.replaceAll("(?i)<reset>", "").replaceAll("(?i)&[0-9A-FK-ORX]", "") // strip legacy
																									// colors
				.replaceAll("(?i)</?[^>]+>", ""); // remove all other MiniMessage tags

		for (int i = 0; i < simplified.length(); i++) {
			char c = simplified.charAt(i);

			// Track bold toggles
			if (simplified.regionMatches(true, i, "<bold>", 0, 6)) {
				bold = true;
				i += 5;
				continue;
			} else if (simplified.regionMatches(true, i, "</bold>", 0, 7)) {
				bold = false;
				i += 6;
				continue;
			} else if (simplified.regionMatches(true, i, "&l", 0, 2)) {
				bold = true;
				i++;
				continue;
			} else if (simplified.regionMatches(true, i, "&r", 0, 2)) {
				bold = false;
				i++;
				continue;
			}

			// Basic width rules for Minecraft font
			int charWidth;
			switch (c) {
			case 'i', 'l', '!', '.', ',', ':', ';' -> charWidth = 2;
			case 't', 'f', 'r', 'I', 'J' -> charWidth = 4;
			case 'm', 'w', 'M', 'W' -> charWidth = 7;
			case ' ' -> charWidth = 3;
			default -> charWidth = 6;
			}

			if (bold)
				charWidth++; // +1 pixel for bold glyphs
			width += charWidth;
		}

		return width;
	}

	/**
	 * Approximates chat viewport width in pixels or characters.
	 */
	public static int estimateChatPixelWidth(@NotNull Player player) {
		try {
			Method getClientViewDistance = player.getClass().getMethod("getClientViewDistance");
			int vd = (int) getClientViewDistance.invoke(player);
			if (vd >= 20)
				return 512; // wide
			if (vd >= 12)
				return 400; // medium
			if (vd >= 8)
				return 320; // compact
			return 240; // small
		} catch (Throwable ignored) {
			// Spigot fallback
		}
		return HellblockPlugin.getInstance().getConfigManager().wrapLength() * 6;
	}

	/**
	 * Approximates the pixel height of a text block after wrapping. Uses the same
	 * wrapping logic to count how many lines it spans.
	 */
	public static int getPixelHeight(@NotNull Player player, @NotNull String text, int indent) {
		if (text.isBlank())
			return 0;
		List<String> wrappedLines = wrapLineWithIndentAdaptive(player, text, indent);
		return wrappedLines.size() * 9; // Each chat line = 9px tall
	}

	/**
	 * Fallback character-width estimator (Spigot-safe).
	 */
	public static int estimateWrapWidth(@NotNull Player player) {
		try {
			Method getClientViewDistance = player.getClass().getMethod("getClientViewDistance");
			int vd = (int) getClientViewDistance.invoke(player);
			if (vd >= 20)
				return 120;
			if (vd >= 12)
				return 90;
			if (vd >= 8)
				return 70;
			return 50;
		} catch (Throwable ignored) {
			// Not available on Spigot
		}
		return HellblockPlugin.getInstance().getConfigManager().wrapLength();
	}

	/**
	 * Basic wrapping for non-Paper servers.
	 */
	public static List<String> wrapLineWithIndent(@NotNull String text, int maxWidth, int indent) {
		List<String> lines = new ArrayList<>();
		if (text.isEmpty() || maxWidth <= 0)
			return lines;

		String[] words = text.split("\\s+");
		StringBuilder current = new StringBuilder();
		String indentStr = " ".repeat(indent);

		for (String word : words) {
			if (current.length() + word.length() + 1 > maxWidth) {
				lines.add(current.toString());
				current.setLength(0);
				current.append(indentStr).append(word);
			} else {
				if (current.length() > 0)
					current.append(" ");
				current.append(word);
			}
		}

		if (current.length() > 0)
			lines.add(current.toString());
		return lines;
	}
}