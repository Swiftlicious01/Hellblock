package com.swiftlicious.hellblock.challenges;

import com.swiftlicious.hellblock.handlers.AdventureHelper;

/**
 * This class is util for creating progressive bars with strings
 */
public final class ProgressBar {

	private final int max;
	private final int value;

	public ProgressBar(int max, int value) {
		if (max < 0) {
			throw new IllegalArgumentException("max cannot be negative");
		}
		this.max = max;
		this.value = Math.max(0, Math.min(value, max)); // clamp between 0 and max
	}

	public int getMax() {
		return max;
	}

	public int getValue() {
		return value;
	}

	public static int getPercent(ProgressBar progressBar) {
		if (progressBar.max == 0) {
			return 0; // avoid division by zero
		}
		final double fraction = (double) progressBar.value / progressBar.max;
		return (int) (fraction * 100);
	}

	public static String getProgressBar(ProgressBar progressBar, int widthInChars) {
		final int percent = getPercent(progressBar);
		final int doneMarkerCount = (int) (percent / 100.0 * widthInChars);
		final int undoneMarkerCount = widthInChars - doneMarkerCount;

		final StringBuilder sb = new StringBuilder();
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>")).append('[');
		for (int i = 0; i < doneMarkerCount; i++) {
			sb.append(AdventureHelper.legacyToMiniMessage("<green>")).append('=');
		}
		for (int i = 0; i < undoneMarkerCount; i++) {
			sb.append(' ');
		}
		sb.append(AdventureHelper.legacyToMiniMessage("<dark_gray>")).append(']');
		return sb.toString();
	}
}