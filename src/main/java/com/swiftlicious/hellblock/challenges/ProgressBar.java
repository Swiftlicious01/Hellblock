package com.swiftlicious.hellblock.challenges;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * This class is util for creating progressive bars with strings
 */
public final class ProgressBar {

	public int max;
	public int value;

	public ProgressBar(int max, int value) {
		this.max = max;
		this.value = value;
	}

	public static int getPercent(ProgressBar progressBar) {
		double fraction = (double) progressBar.value / progressBar.max;
		return (int) (fraction * 100);
	}

	public static String getProgressBar(ProgressBar progressBar, int widthInChars) {
		int doneMarkerCount = (int) (getPercent(progressBar) / 100.0 * widthInChars);
		int undoneMarkerCount = widthInChars - doneMarkerCount;
		StringBuilder sb = new StringBuilder();
		sb.append(HellblockPlugin.getInstance().getAdventureManager().legacyToMiniMessage("<dark_gray>")).append('[');
		for (int i = 0; i < doneMarkerCount; i++) {
			sb.append(HellblockPlugin.getInstance().getAdventureManager().legacyToMiniMessage("<green>")).append('=');
		}
		for (int i = 0; i < undoneMarkerCount; i++) {
			sb.append(' ');
		}
		sb.append(HellblockPlugin.getInstance().getAdventureManager().legacyToMiniMessage("<dark_gray>")).append(']');
		return sb.toString();
	}
}