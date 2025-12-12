package com.swiftlicious.hellblock.schematic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;

import net.kyori.adventure.text.Component;

/**
 * Utility class for version‑independent interaction with sign text in
 * Bukkit/Paper.
 * <p>
 * This helper uses reflection to detect whether the running server supports
 * dual‑sided signs (1.20+). In that case, it invokes the new API methods:
 * {@code Sign#getSide(Side)} and {@code SignSide#line(int, Component)}.
 * <p>
 * On older servers (≤ 1.19.4), those classes and methods don't exist, so this
 * class transparently falls back to using the legacy single‑sided
 * {@code Sign#line(int)} and {@code Sign#line(int, Component)} APIs.
 */
public final class SignReflectionHelper {

	private SignReflectionHelper() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static boolean dualSided = false;
	private static Class<?> sideEnumClass; // org.bukkit.block.sign.Side
	private static Class<?> signSideClass; // org.bukkit.block.sign.SignSide
	private static Method getSideMethod; // Sign#getSide(Side)
	private static Method signSideGetLineMethod; // SignSide#line(int)
	private static Method signSideSetLineMethod; // SignSide#line(int, Component)

	private static Method spigotGetLineMethod = null;
	private static Method spigotSetLineMethod = null;
	private static Method paperLineGetterMethod = null;
	private static Method paperLineSetterMethod = null;

	private static final boolean isSpigot = VersionHelper.isSpigot();

	static {
		try {
			// Attempt to load 1.20+ classes and methods
			Class<?> signClass = Class.forName("org.bukkit.block.Sign");
			sideEnumClass = Class.forName("org.bukkit.block.sign.Side");
			signSideClass = Class.forName("org.bukkit.block.sign.SignSide");

			getSideMethod = signClass.getMethod("getSide", sideEnumClass);
			signSideGetLineMethod = signSideClass.getMethod("line", int.class);
			signSideSetLineMethod = signSideClass.getMethod("line", int.class, Component.class);

			if (isSpigot) {
				// Spigot uses getLine/setLine with Strings
				spigotGetLineMethod = signClass.getMethod("getLine", int.class);
				spigotSetLineMethod = signClass.getMethod("setLine", int.class, String.class);
			} else {
				// Paper uses line(int)/line(int, Component)
				paperLineGetterMethod = signClass.getMethod("line", int.class);
				paperLineSetterMethod = signClass.getMethod("line", int.class, Component.class);
			}

			dualSided = true;
		} catch (Exception e) {
			// Fallback for ≤ 1.19.4 (no Side/SignSide support)
			dualSided = false;
		}
	}

	/**
	 * Checks whether the running server version supports dual‑sided signs (1.20+).
	 *
	 * @return true if Side/SignSide APIs are available, false if using legacy
	 *         single‑sided signs.
	 */
	public static boolean isDualSided() {
		return dualSided;
	}

	/**
	 * Gets the reflective enum constants of {@code org.bukkit.block.sign.Side}.
	 * <p>
	 * For 1.20+, this will typically contain FRONT and BACK. Returns an empty array
	 * for legacy versions.
	 *
	 * @return an array of side enum constants, or empty if not supported.
	 */
	@NotNull
	public static Object[] getSideEnumConstants() {
		if (!dualSided || sideEnumClass == null)
			return new Object[0];
		return sideEnumClass.getEnumConstants();
	}

	/**
	 * Invokes {@code Sign#getSide(Side)} reflectively on 1.20+ servers.
	 *
	 * @param sign     The sign instance.
	 * @param sideEnum The enum constant for the side (FRONT/BACK).
	 * @return The SignSide instance for that side.
	 * @throws Exception if reflection fails or if unsupported on the current
	 *                   version.
	 */
	@NotNull
	public static Object invokeGetSide(@NotNull Sign sign, @NotNull Object sideEnum) throws Exception {
		if (!dualSided)
			throw new UnsupportedOperationException("Dual‑sided signs not supported on this version");
		return getSideMethod.invoke(sign, sideEnum);
	}

	/**
	 * Reads a line from a SignSide reflectively.
	 *
	 * @param signSide The reflective SignSide instance.
	 * @param index    Line index (0–3).
	 * @return The text component at that line, or null if unavailable.
	 * @throws Exception if reflection fails.
	 */
	@NotNull
	public static Component invokeGetLine(@NotNull Object signSide, int index) throws Exception {
		if (!dualSided)
			throw new UnsupportedOperationException("Dual‑sided signs not supported on this version");
		return (Component) signSideGetLineMethod.invoke(signSide, index);
	}

	/**
	 * Sets a line on a SignSide reflectively.
	 *
	 * @param signSide The SignSide instance (obtained via
	 *                 {@link #invokeGetSide(Sign, Object)}).
	 * @param index    Line index (0–3).
	 * @param line     The component to set.
	 * @throws Exception if reflection fails.
	 */
	public static void invokeSetLine(@NotNull Object signSide, int index, @NotNull Component line) throws Exception {
		if (!dualSided)
			throw new UnsupportedOperationException("Dual‑sided signs not supported on this version");
		signSideSetLineMethod.invoke(signSide, index, line);
	}

	/**
	 * Retrieves all lines from the given sign, automatically supporting both
	 * single‑sided (≤ 1.19.4) and dual‑sided (≥ 1.20) sign APIs.
	 *
	 * @param sign The sign to read.
	 * @return A list of components from all visible lines (8 lines max for 1.20+, 4
	 *         for legacy).
	 */
	@NotNull
	public static List<Component> getSignLines(@NotNull Sign sign) {
		List<Component> lines = new ArrayList<>();

		if (dualSided) {
			try {
				for (Object sideEnum : getSideEnumConstants()) {
					Object signSide = invokeGetSide(sign, sideEnum);
					for (int i = 0; i < 4; i++) {
						Component line = invokeGetLine(signSide, i);
						lines.add(line);
					}
				}
				return lines;
			} catch (Exception ex) {
				ex.printStackTrace(); // You may want to log this with your plugin logger instead
			}
		}

		// Fallback: use reflection based on API
		int maxLines = Math.min(4, lines.size());
		for (int i = 0; i < maxLines; i++) {
			try {
				if (isSpigot && spigotGetLineMethod != null) {
					String text = (String) spigotGetLineMethod.invoke(sign, i);
					lines.add(Component.text(text != null ? text : ""));
				} else if (paperLineGetterMethod != null) {
					Component comp = (Component) paperLineGetterMethod.invoke(sign, i);
					lines.add(comp != null ? comp : Component.empty());
				}
			} catch (Exception e) {
				lines.add(Component.empty()); // Safe fallback
			}
		}
		return lines;
	}

	/**
	 * Retrieves a single line from a sign at the given index in a version-safe way.
	 * <p>
	 * This method supports both dual-sided (1.20+) and legacy single-sided signs.
	 *
	 * @param sign  The sign to read from.
	 * @param index The line index (0–3).
	 * @return The Component at the given index, or {@link Component#empty()} if
	 *         unavailable.
	 */
	@NotNull
	public static Component getLine(@NotNull Sign sign, int index) {
		List<Component> lines = getSignLines(sign);
		return index >= 0 && index < lines.size() ? lines.get(index) : Component.empty();
	}

	/**
	 * Sets the text lines of a sign in a version‑independent manner.
	 * <p>
	 * On 1.20+, this writes to both FRONT and BACK sides. On 1.19.4 and earlier,
	 * this writes only to the single available side.
	 *
	 * @param sign  The sign to modify.
	 * @param lines The list of text components to write.
	 */
	public static void setSignLines(@NotNull Sign sign, @NotNull List<Component> lines) {
		if (dualSided) {
			try {
				for (Object sideEnum : getSideEnumConstants()) {
					Object signSide = invokeGetSide(sign, sideEnum);
					for (int i = 0; i < 4 && i < lines.size(); i++) {
						invokeSetLine(signSide, i, lines.get(i));
					}
				}
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Fallback: use reflection based on API
		int maxLines = Math.min(4, lines.size());
		for (int i = 0; i < maxLines; i++) {
			try {
				Component line = lines.get(i);

				if (isSpigot && spigotSetLineMethod != null) {
					String text = AdventureHelper.componentToLegacy(line);
					spigotSetLineMethod.invoke(sign, i, text);
				} else if (paperLineSetterMethod != null) {
					paperLineSetterMethod.invoke(sign, i, line);
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Sets a single line on a sign at the given index in a version-safe way.
	 * <p>
	 * On 1.20+, this sets the line on both FRONT and BACK. On older versions, it
	 * sets the single available line.
	 *
	 * @param sign  The sign to modify.
	 * @param index The line index (0–3).
	 * @param line  The Component to set.
	 */
	public static void setLine(@NotNull Sign sign, int index, @NotNull Component line) {
		List<Component> lines = getSignLines(sign);

		// Ensure lines list has at least 4 entries
		while (lines.size() < 4)
			lines.add(Component.empty());

		lines.set(index, line);
		setSignLines(sign, lines);
	}
}