package com.swiftlicious.hellblock.challenges;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.handlers.AdventureHelper;

/**
 * Utility class for creating textual progress bars for chat, GUIs, or logs.
 * <p>
 * The {@link ProgressBar} represents a value between 0 and a defined maximum,
 * and provides utilities to display a visual bar using MiniMessage or legacy
 * color formatting. Ideal for progress indicators like:
 * <ul>
 * <li>Task or mission completion</li>
 * <li>Health, mana, or energy bars</li>
 * <li>Download / world generation progress</li>
 * </ul>
 */
public final class ProgressBar {

	private final double max;
	private final double value;

	// Cache: start|end|ratioRounded → "<#RRGGBB>"
	private static final Map<String, String> COLOR_CACHE = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@code ProgressBar}.
	 *
	 * @param max   the maximum value (must be non-negative)
	 * @param value the current value (clamped between 0 and {@code max})
	 */
	public ProgressBar(double max, double value) {
		if (max < 0) {
			throw new IllegalArgumentException("max cannot be negative");
		}
		this.max = max;
		this.value = Math.max(0, Math.min(value, max));
	}

	public ProgressBar(int max, int value) {
		this((double) max, (double) value);
	}

	public double getMax() {
		return max;
	}

	public double getValue() {
		return value;
	}

	/**
	 * Calculates the percentage (0–100) of completion for this progress bar.
	 *
	 * @param progressBar the progress bar to calculate
	 * @return the completion percentage
	 */
	public static double getPercent(@NotNull ProgressBar progressBar) {
		if (progressBar.max == 0) {
			return 0;
		}
		return (progressBar.value / progressBar.max) * 100.0;
	}

	/**
	 * Builds a simple colored progress bar string.
	 *
	 * @param progressBar  the progress bar instance
	 * @param widthInChars total width (characters) of the bar
	 * @return a colored progress bar using MiniMessage syntax
	 */
	@NotNull
	public static String getProgressBar(@NotNull ProgressBar progressBar, int widthInChars) {
		final double percent = getPercent(progressBar);
		final double doneExact = percent / 100.0 * widthInChars;

		final int doneFull = (int) doneExact;
		final double fractional = doneExact - doneFull;

		final boolean partial = fractional >= 0.25 && fractional < 0.75;
		final boolean nearFull = fractional >= 0.75;

		final StringBuilder sb = new StringBuilder();
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>["));

		// Filled portion
		for (int i = 0; i < doneFull; i++) {
			sb.append(AdventureHelper.legacyToMiniMessage("<green>")).append("█");
		}

		// Optional fractional block
		if (nearFull) {
			sb.append(AdventureHelper.legacyToMiniMessage("<green>")).append("█");
		} else if (partial) {
			sb.append(AdventureHelper.legacyToMiniMessage("<green>")).append("▌");
		}

		// Remaining empty portion
		int remaining = widthInChars - doneFull - (partial || nearFull ? 1 : 0);
		for (int i = 0; i < remaining; i++) {
			sb.append(AdventureHelper.legacyToMiniMessage("<gray>")).append("░");
		}

		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>] "));
		sb.append(AdventureHelper.legacyToMiniMessage("<white>")).append(formatValue(percent)).append("%");

		return sb.toString();
	}

	/**
	 * Creates a gradient-style progress bar that transitions between two colors.
	 * Example: green → yellow → red depending on percentage.
	 *
	 * @param progressBar  the progress bar instance
	 * @param widthInChars total width (characters)
	 * @param startColor   MiniMessage color name or hex (e.g. "<green>" or
	 *                     "<#00FF00>")
	 * @param endColor     MiniMessage color name or hex
	 * @return a gradient-colored progress bar
	 */
	@NotNull
	public static String getGradientBar(@NotNull ProgressBar progressBar, int widthInChars, @NotNull String startColor,
			@NotNull String endColor) {
		final double percent = getPercent(progressBar);
		final double doneExact = percent / 100.0 * widthInChars;

		final int doneFull = (int) doneExact;
		final double fractional = doneExact - doneFull;
		final boolean partial = fractional >= 0.25 && fractional < 0.75;
		final boolean nearFull = fractional >= 0.75;

		StringBuilder sb = new StringBuilder();
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>["));

		for (int i = 0; i < widthInChars; i++) {
			double ratio = i / (double) widthInChars;
			String color = interpolateColor(startColor, endColor, ratio);

			if (i < doneFull) {
				sb.append(color).append("█");
			} else if (i == doneFull && (partial || nearFull)) {
				sb.append(color).append(partial ? "▌" : "█");
			} else {
				sb.append(AdventureHelper.legacyToMiniMessage("<gray>")).append("░");
			}
		}

		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>] "))
				.append(AdventureHelper.legacyToMiniMessage("<white>")).append(formatValue(percent)).append("%");

		return sb.toString();
	}

	/**
	 * Creates a smooth multi-color gradient progress bar.
	 *
	 * @param progressBar  progress bar instance
	 * @param widthInChars total bar width
	 * @param colorStops   ordered list of MiniMessage colors (2 or more)
	 * @return formatted MiniMessage gradient bar string
	 */
	@NotNull
	public static String getMultiGradientBar(@NotNull ProgressBar progressBar, int widthInChars,
			@Nullable List<String> colorStops) {
		if (colorStops == null || colorStops.size() < 2) {
			throw new IllegalArgumentException("At least two color stops are required for a gradient bar.");
		}

		final double percent = getPercent(progressBar);
		final double doneExact = percent / 100.0 * widthInChars;
		final int doneFull = (int) doneExact;
		final double fractional = doneExact - doneFull;

		final boolean partial = fractional >= 0.25 && fractional < 0.75;
		final boolean nearFull = fractional >= 0.75;

		StringBuilder sb = new StringBuilder();
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>["));

		for (int i = 0; i < widthInChars; i++) {
			double ratio = i / (double) widthInChars;

			// Which color segment are we in?
			double segmentLength = 1.0 / (colorStops.size() - 1);
			int index = Math.min((int) (ratio / segmentLength), colorStops.size() - 2);

			double localRatio = (ratio - (index * segmentLength)) / segmentLength;

			String start = colorStops.get(index);
			String end = colorStops.get(index + 1);

			String color = interpolateColor(start, end, localRatio);

			if (i < doneFull) {
				sb.append(color).append("█");
			} else if (i == doneFull && (partial || nearFull)) {
				sb.append(color).append(partial ? "▌" : "█");
			} else {
				sb.append(AdventureHelper.legacyToMiniMessage("<gray>")).append("░");
			}
		}

		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>] "))
				.append(AdventureHelper.legacyToMiniMessage("<white>")).append(formatValue(percent)).append("%");

		return sb.toString();
	}

	/**
	 * Creates an animated multi-color gradient bar that shifts its color pattern
	 * smoothly over time. Perfect for “charging” or “energy flow” visual effects.
	 *
	 * @param progressBar  progress bar instance
	 * @param widthInChars total bar width
	 * @param colorStops   ordered list of colors (at least 2)
	 * @param phaseOffset  animation offset (e.g., use System.currentTimeMillis() /
	 *                     200d)
	 * @return formatted MiniMessage gradient bar string
	 */
	@NotNull
	public static String getAnimatedGradientBar(@NotNull ProgressBar progressBar, int widthInChars,
			@Nullable List<String> colorStops, double phaseOffset) {
		if (colorStops == null || colorStops.size() < 2) {
			throw new IllegalArgumentException("At least two color stops are required for an animated gradient bar.");
		}

		final double percent = getPercent(progressBar);
		final double doneExact = percent / 100.0 * widthInChars;
		final int doneFull = (int) doneExact;
		final double fractional = doneExact - doneFull;

		final boolean partial = fractional >= 0.25 && fractional < 0.75;
		final boolean nearFull = fractional >= 0.75;

		StringBuilder sb = new StringBuilder();
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>["));

		double phase = (phaseOffset % 1.0 + 1.0) % 1.0; // normalize to 0–1

		for (int i = 0; i < widthInChars; i++) {
			double ratio = ((i / (double) widthInChars) + phase) % 1.0;

			double segmentLength = 1.0 / (colorStops.size() - 1);
			int index = Math.min((int) (ratio / segmentLength), colorStops.size() - 2);
			double localRatio = (ratio - (index * segmentLength)) / segmentLength;

			String start = colorStops.get(index);
			String end = colorStops.get(index + 1);

			String color = interpolateColor(start, end, localRatio);

			if (i < doneFull) {
				sb.append(color).append("█");
			} else if (i == doneFull && (partial || nearFull)) {
				sb.append(color).append(partial ? "▌" : "█");
			} else {
				sb.append(AdventureHelper.legacyToMiniMessage("<gray>")).append("░");
			}
		}

		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>] "))
				.append(AdventureHelper.legacyToMiniMessage("<white>")).append(formatValue(percent)).append("%");

		return sb.toString();
	}

	/**
	 * Interpolates smoothly between two colors using HSB blending, with caching.
	 * Produces a MiniMessage color tag (e.g. "<#A3FF42>").
	 *
	 * @param startColor MiniMessage color tag (e.g. "<#00FF00>" or "<green>")
	 * @param endColor   MiniMessage color tag
	 * @param ratio      blend ratio from 0.0 (start) to 1.0 (end)
	 * @return formatted MiniMessage color tag for the blended color
	 */
	@NotNull
	private static String interpolateColor(@NotNull String startColor, @NotNull String endColor, double ratio) {
		// Round ratio to two decimals to limit cache size
		double roundedRatio = Math.round(ratio * 100.0) / 100.0;
		String cacheKey = startColor + "|" + endColor + "|" + roundedRatio;

		String cached = COLOR_CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		Color c1 = Color.decode(parseHex(startColor));
		Color c2 = Color.decode(parseHex(endColor));

		// Convert both to HSB
		float[] hsb1 = Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), null);
		float[] hsb2 = Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), null);

		// Interpolate each component
		float h = interpolateHue(hsb1[0], hsb2[0], (float) ratio);
		float s = hsb1[1] + (hsb2[1] - hsb1[1]) * (float) ratio;
		float b = hsb1[2] + (hsb2[2] - hsb1[2]) * (float) ratio;

		int rgb = Color.HSBtoRGB(h, s, b);
		String colorCode = String.format("<#%06X>", (rgb & 0xFFFFFF));

		COLOR_CACHE.put(cacheKey, colorCode);
		return colorCode;
	}

	/**
	 * Handles hue wrap-around correctly when blending between two HSB hues. (e.g.,
	 * red (0.0) to yellow (0.16) blends smoothly, not through purple.)
	 */
	private static float interpolateHue(float h1, float h2, float ratio) {
		float diff = h2 - h1;
		if (diff < -0.5f)
			diff += 1.0f;
		else if (diff > 0.5f)
			diff -= 1.0f;
		float h = (h1 + ratio * diff) % 1.0f;
		if (h < 0.0f)
			h += 1.0f;
		return h;
	}

	/**
	 * Parses either a MiniMessage color like "&lt;green&gt;" or a hex color into a
	 * usable {@code #RRGGBB} format for interpolation.
	 */
	@NotNull
	private static String parseHex(@NotNull String color) {
		if (color.startsWith("<#")) {
			return color.substring(1, color.length() - 1);
		}
		// fallback predefined colors
		return switch (color.toLowerCase(Locale.ROOT)) {
		case "<red>" -> "#FF0000";
		case "<yellow>" -> "#FFFF00";
		case "<green>" -> "#00FF00";
		case "<blue>" -> "#0000FF";
		case "<aqua>" -> "#00FFFF";
		case "<white>" -> "#FFFFFF";
		case "<gray>", "<grey>" -> "#AAAAAA";
		default -> "#FFFFFF";
		};
	}

	/**
	 * Creates a progress bar directly from a percent value (0–100).
	 *
	 * @param percent percentage complete (clamped 0–100)
	 * @return a new ProgressBar where {@code max = 100}
	 */
	@NotNull
	public static ProgressBar ofPercent(int percent) {
		return new ProgressBar(100, Math.max(0, Math.min(percent, 100)));
	}

	/**
	 * Formats a number for display — e.g. 1.0 → "1", 1.5 → "1.5".
	 */
	@NotNull
	public static String formatValue(double value) {
		return (value == Math.floor(value)) ? String.format("%.0f", value) : String.format("%.1f", value);
	}

	public static void clearColorCache() {
		COLOR_CACHE.clear();
	}
}