package com.swiftlicious.hellblock.challenges.requirement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.handlers.PotionEffectResolver;

import dev.dejvokep.boostedyaml.block.implementation.Section;

/**
 * Represents a challenge requirement where a player must brew a specific type
 * of potion, optionally with certain base effects or custom effects.
 * <p>
 * This requirement can match any of the potion item types — normal, splash, or
 * lingering — and supports multiple layers of specificity:
 * </p>
 * <ul>
 * <li>Required potion item type (e.g., {@code POTION},
 * {@code SPLASH_POTION})</li>
 * <li>Optional base effect type (e.g., {@code AWKWARD}, {@code MUNDANE})</li>
 * <li>Optional custom effects with specific amplifiers or durations</li>
 * </ul>
 *
 * <p>
 * <b>Example configuration:</b>
 * </p>
 * 
 * <pre>
 *   BREW_STRENGTH_POTION:
 *     needed-amount: 3
 *     action: BREW
 *     data:
 *       material: POTION
 *       base_effect: AWKWARD
 *       potion_contents:
 *         custom_effects:
 *           - id: strength
 *             amplifier: 1
 *             duration: 180
 *     rewards:
 *       item_action:
 *         type: 'give-vanilla-item'
 *         value:
 *           material: BLAZE_POWDER
 * </pre>
 *
 * <p>
 * In this example, brewing 3 Strength potions (from an Awkward base) completes
 * the challenge.
 * </p>
 */
public class BrewRequirement implements ChallengeRequirement {

	private final Material potionType;
	private final PotionType basePotion; // optional
	private final List<ExpectedEffect> expectedEffects = new ArrayList<>();

	/**
	 * Represents a single expected potion effect used for comparison when verifying
	 * brewed potions.
	 * <p>
	 * Each instance stores:
	 * <ul>
	 * <li>The {@link PotionEffectType} (e.g., {@code STRENGTH},
	 * {@code REGENERATION})</li>
	 * <li>The amplifier level (e.g., 0 = I, 1 = II, etc.)</li>
	 * <li>An optional duration (in ticks) that, if defined, must match exactly</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * This record allows {@link BrewRequirement} to accurately compare both base
	 * and custom potion effects against configuration-defined expectations.
	 * </p>
	 */
	private static final record ExpectedEffect(PotionEffectType type, int amplifier, Integer durationTicks) {
	}

	public BrewRequirement(@NotNull Section data) {
		// --- Material ---
		String matStr = data.getString("material");
		if (matStr == null)
			throw new IllegalArgumentException("BREW requires 'material' in data");

		Material mt = Material.matchMaterial(matStr.toUpperCase(Locale.ROOT));
		if (mt == null || (mt != Material.POTION && mt != Material.SPLASH_POTION && mt != Material.LINGERING_POTION)) {
			throw new IllegalArgumentException("Invalid potion material: " + matStr);
		}
		this.potionType = mt;

		// --- Base potion (optional) ---
		PotionType resolvedBase = null;
		if (data.contains("base_effect")) {
			String id = data.getString("base_effect");
			resolvedBase = resolveBasePotion(id);
			if (resolvedBase == null) {
				HellblockPlugin.getInstance().getPluginLogger().warn("Unknown base potion effect: " + id);
			}
		}
		this.basePotion = resolvedBase;

		// --- Custom effects (optional) ---
		Section potionContents = data.getSection("potion_contents");
		if (potionContents != null) {
			for (Object obj : potionContents.getList("custom_effects", Collections.emptyList())) {
				String id = null;
				int amplifier = 0;
				Integer durationTicks = null;

				if (obj instanceof Section sec) {
					id = sec.getString("id", null);
					amplifier = sec.getInt("amplifier", 0);
					if (sec.contains("duration")) {
						durationTicks = sec.getInt("duration") * 20; // convert seconds → ticks
					}
				} else if (obj instanceof Map<?, ?> map) {
					Object idObj = map.get("id");
					if (idObj != null)
						id = String.valueOf(idObj);

					Object ampObj = map.get("amplifier");
					if (ampObj != null) {
						try {
							amplifier = Integer.parseInt(String.valueOf(ampObj));
						} catch (NumberFormatException ignored) {
						}
					}

					Object durObj = map.get("duration");
					if (durObj != null) {
						try {
							durationTicks = Integer.parseInt(String.valueOf(durObj)) * 20;
						} catch (NumberFormatException ignored) {
						}
					}
				}

				if (id == null)
					continue;
				PotionEffectType pet = resolveCustomEffect(id);
				if (pet == null) {
					HellblockPlugin.getInstance().getPluginLogger().warn("Unknown custom potion effect id: " + id);
					continue;
				}
				expectedEffects.add(new ExpectedEffect(pet, amplifier, durationTicks));
			}
		}
	}

	/**
	 * Checks whether the provided {@link ItemStack} represents a potion that
	 * matches the configured material, base effect, and custom effects.
	 *
	 * @param context The brewed item (expected to be an {@link ItemStack} of potion
	 *                type).
	 * @return {@code true} if the potion matches all defined conditions.
	 */
	@Override
	public boolean matches(@NotNull Object context) {
		if (!(context instanceof ItemStack item))
			return false;
		if (item.getType() != potionType)
			return false;
		if (!(item.getItemMeta() instanceof PotionMeta meta))
			return false;

		// --- Base effect check ---
		if (basePotion != null && !isMatchingBasePotion(meta, basePotion)) {
			return false;
		}

		// --- Custom effects check ---
		List<PotionEffect> actual = meta.getCustomEffects();
		for (ExpectedEffect expected : expectedEffects) {
			boolean found = false;
			for (PotionEffect a : actual) {
				if (!a.getType().equals(expected.type()))
					continue;
				if (a.getAmplifier() != expected.amplifier())
					continue;

				// if duration set, enforce; otherwise ignore
				if (expected.durationTicks() != null && !expected.durationTicks().equals(a.getDuration())) {
					continue;
				}

				found = true;
				break;
			}
			if (!found)
				return false;
		}

		return true;
	}

	/**
	 * Resolves a base potion type from its string ID (vanilla names only).
	 */
	@Nullable
	private PotionType resolveBasePotion(@Nullable String id) {
		if (id == null)
			return null;
		try {
			return PotionType.valueOf(id.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Resolves a custom potion effect type from its string ID using the registry.
	 */
	@Nullable
	private PotionEffectType resolveCustomEffect(@NotNull String id) {
		String key = id.contains(":") ? id.toLowerCase(Locale.ROOT) : "minecraft:" + id.toLowerCase(Locale.ROOT);
		return PotionEffectResolver.resolve(key);
	}

	/**
	 * Compares a {@link PotionMeta}'s base potion type with the expected type,
	 * supporting both modern (1.19.4+) and older Bukkit versions.
	 * <p>
	 * Uses {@code getBasePotionType()} if available; otherwise falls back to
	 * {@code getBasePotionData().getType()} for compatibility with 1.9+.
	 *
	 * @param meta     The potion meta to check.
	 * @param expected The expected {@link PotionType}.
	 * @return True if the base potion type matches, false otherwise.
	 */
	private boolean isMatchingBasePotion(@NotNull PotionMeta meta, @Nullable PotionType expected) {
		if (expected == null)
			return true;

		try {
		    // Preferred (modern): getBasePotionType()
		    Method getBasePotionType = PotionMeta.class.getMethod("getBasePotionType");
		    Object actual = getBasePotionType.invoke(meta);
		    return expected.equals(actual);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			// Fallback: use reflection for getBasePotionData() and getType()
			try {
				Method getBasePotionData = PotionMeta.class.getMethod("getBasePotionData");
				Object basePotionData = getBasePotionData.invoke(meta);

				if (basePotionData != null) {
					Method getType = basePotionData.getClass().getMethod("getType");
					Object actual = getType.invoke(basePotionData);
					return expected.equals(actual);
				}

				return false;
			} catch (Exception ex) {
				return false;
			}
		}
	}
}