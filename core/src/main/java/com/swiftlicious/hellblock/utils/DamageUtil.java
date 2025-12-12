package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class DamageUtil {
	
	private DamageUtil() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	// Caching reflective objects
	private static boolean damageTypeSupported = true;

	private static Method getDamageSourceMethod = null;
	private static Class<?> damageSourceClass = null;
	private static Method getDamageTypeMethod = null;

	private static Class<?> damageTypeClass = null;
	private static Object ON_FIRE = null;

	/**
	 * Determines if the damage event was caused by ON_FIRE damage type (1.20+
	 * only). Returns false on older versions or if the reflection fails.
	 *
	 * @param event EntityDamageEvent to inspect
	 * @return true if damage was ON_FIRE, false otherwise
	 */
	public static boolean isFireDamage(@NotNull EntityDamageEvent event) {
		if (!damageTypeSupported) {
			return false;
		}

		try {
			// Lazy initialize reflection once
			if (getDamageSourceMethod == null) {
				getDamageSourceMethod = EntityDamageEvent.class.getMethod("getDamageSource");

				damageSourceClass = Class.forName("org.bukkit.event.entity.DamageSource");
				getDamageTypeMethod = damageSourceClass.getMethod("getDamageType");

				damageTypeClass = Class.forName("org.bukkit.damage.DamageType");
				Field onFireField = damageTypeClass.getField("ON_FIRE");
				ON_FIRE = onFireField.get(null);
			}

			Object damageSource = getDamageSourceMethod.invoke(event);
			Object damageType = getDamageTypeMethod.invoke(damageSource);

			return damageType != null && damageType.equals(ON_FIRE);

		} catch (Throwable e) {
			// Disable reflection after failure
			damageTypeSupported = false;
			return false;
		}
	}
}