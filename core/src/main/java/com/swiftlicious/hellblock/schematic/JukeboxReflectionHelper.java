package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Method;

import org.bukkit.block.Jukebox;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for safely interacting with Jukebox blocks across Minecraft versions.
 *
 * In 1.20+, Jukebox has {@code hasRecord()}. In older versions, we must infer
 * it via {@code getRecord()}.
 */
public final class JukeboxReflectionHelper {

	private JukeboxReflectionHelper() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static Method hasRecordMethod;
	private static boolean supportsHasRecord = false;

	static {
		try {
			hasRecordMethod = Jukebox.class.getMethod("hasRecord");
			supportsHasRecord = true;
		} catch (NoSuchMethodException ignored) {
			// 1.19.4 or older — hasRecord() doesn’t exist
		}
	}

	/**
	 * Checks whether the current server version supports
	 * {@code Jukebox#hasRecord()}.
	 *
	 * @return true if available (1.20+), false otherwise.
	 */
	public static boolean supportsHasRecord() {
		return supportsHasRecord;
	}

	/**
	 * Determines whether the given jukebox contains a record, supporting both
	 * pre‑1.20 and 1.20+ versions.
	 *
	 * @param jukebox The Jukebox block state.
	 * @return true if it has a record, false otherwise.
	 */
	public static boolean hasRecord(@NotNull Jukebox jukebox) {
		try {
			if (supportsHasRecord) {
				return (boolean) hasRecordMethod.invoke(jukebox);
			} else {
				// 1.19.4 fallback: infer by checking if getRecord() is non‑null and not AIR
				ItemStack record = jukebox.getRecord();
				return record != null && !record.getType().isAir();
			}
		} catch (Exception e) {
			return false;
		}
	}
}