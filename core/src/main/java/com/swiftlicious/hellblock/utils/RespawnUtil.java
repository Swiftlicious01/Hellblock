package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RespawnUtil {

	private RespawnUtil() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static boolean respawnReasonSupported = true;
	private static Method getRespawnReasonMethod = null;
	private static Class<?> respawnReasonClass = null;
	private static Object DEATH_REASON = null;

	// --- Cache for Player#getRespawnLocation ---
	private static boolean respawnLocationSupported = true;
	private static Method getRespawnLocationMethod = null;

	// --- World#isBedWorks and isRespawnAnchorWorks support ---
	private static Method worldIsBedWorks = null;
	private static Method worldIsRespawnAnchorWorks = null;
	private static boolean bedAnchorSupportKnown = false;

	/**
	 * Gets the player's respawn location using getRespawnLocation() if available
	 * (1.20+), or falls back to getBedSpawnLocation() for older versions.
	 *
	 * @param player The player to query
	 * @return The respawn location, or null if none is available
	 */
	@SuppressWarnings("deprecation")
	@Nullable
	public static Location getRespawnLocation(@Nullable Player player) {
		if (player == null)
			return null;

		if (respawnLocationSupported) {
			try {
				if (getRespawnLocationMethod == null) {
					getRespawnLocationMethod = Player.class.getMethod("getRespawnLocation");
				}

				Object result = getRespawnLocationMethod.invoke(player);
				if (result instanceof Location location) {
					return location;
				}
			} catch (NoSuchMethodException e) {
				// Method doesn't exist on this version — fallback
				respawnLocationSupported = false;
			} catch (Exception e) {
				// Unexpected failure — disable further attempts
				respawnLocationSupported = false;
				e.printStackTrace(); // Optional: replace with logger
			}
		}

		// Fallback for 1.19.4 and earlier
		return player.getBedSpawnLocation(); // Deprecated, but still functional
	}

	/**
	 * Checks if the respawn reason for the event was DEATH. Compatible with both
	 * 1.19.4 (no reason API) and 1.20+ (uses RespawnReason.DEATH).
	 *
	 * @param event The PlayerRespawnEvent
	 * @return true if the respawn reason is DEATH, or unknown (pre-1.20); false
	 *         otherwise
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean isDeathRespawn(@NotNull PlayerRespawnEvent event) {
		if (!respawnReasonSupported) {
			// Pre-1.20: assume DEATH (only respawn reason back then)
			return true;
		}

		try {
			// Init reflection
			if (getRespawnReasonMethod == null) {
				getRespawnReasonMethod = PlayerRespawnEvent.class.getMethod("getRespawnReason");
				respawnReasonClass = getRespawnReasonMethod.getReturnType();

				// Load RespawnReason.DEATH enum constant
				DEATH_REASON = Enum.valueOf((Class<Enum>) respawnReasonClass, "DEATH");
			}

			Object reason = getRespawnReasonMethod.invoke(event);
			return reason == DEATH_REASON;
		} catch (Exception e) {
			// Fallback: method not available or failed — treat as unsupported
			respawnReasonSupported = false;
			return true; // Assume DEATH for compatibility
		}
	}

	/**
	 * Checks if beds work in the given world.
	 *
	 * @param world The world
	 * @return true if beds work, false if unsupported or false by world logic
	 */
	public static boolean isBedWorks(@NotNull World world) {
		if (!bedAnchorSupportKnown) {
			try {
				worldIsBedWorks = World.class.getMethod("isBedWorks");
				worldIsRespawnAnchorWorks = World.class.getMethod("isRespawnAnchorWorks");
			} catch (Throwable ignored) {
				// Methods don't exist (1.17.0 or older)
			}
			bedAnchorSupportKnown = true;
		}

		if (worldIsBedWorks != null) {
			try {
				return (boolean) worldIsBedWorks.invoke(world);
			} catch (Throwable ignored) {
			}
		}

		// Fallback: assume beds only work in the overworld
		return world.getEnvironment() == World.Environment.NORMAL;
	}

	/**
	 * Checks if respawn anchors work in the given world.
	 *
	 * @param world The world
	 * @return true if anchors work, false if unsupported or false by world logic
	 */
	public static boolean isRespawnAnchorWorks(@NotNull World world) {
		if (!bedAnchorSupportKnown) {
			try {
				worldIsBedWorks = World.class.getMethod("isBedWorks");
				worldIsRespawnAnchorWorks = World.class.getMethod("isRespawnAnchorWorks");
			} catch (Throwable ignored) {
				// Methods don't exist (1.17.0 or older)
			}
			bedAnchorSupportKnown = true;
		}

		if (worldIsRespawnAnchorWorks != null) {
			try {
				return (boolean) worldIsRespawnAnchorWorks.invoke(world);
			} catch (Throwable ignored) {
			}
		}

		// Fallback: assume anchors only work in the Nether
		return world.getEnvironment() == World.Environment.NETHER;
	}
}