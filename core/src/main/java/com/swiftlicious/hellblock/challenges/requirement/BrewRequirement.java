package com.swiftlicious.hellblock.challenges.requirement;

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

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.challenges.ChallengeRequirement;
import com.swiftlicious.hellblock.handlers.PotionEffectResolver;

import dev.dejvokep.boostedyaml.block.implementation.Section;

public class BrewRequirement implements ChallengeRequirement {
	private final Material potionType;
	private final PotionType basePotion; // optional
	private final List<ExpectedEffect> expectedEffects = new ArrayList<>();

	private static final record ExpectedEffect(PotionEffectType type, int amplifier, Integer durationTicks) {
	}

	public BrewRequirement(Section data) {
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

	@Override
	public boolean matches(Object context) {
		if (!(context instanceof ItemStack item))
			return false;
		if (item.getType() != potionType)
			return false;
		if (!(item.getItemMeta() instanceof PotionMeta meta))
			return false;

		// --- Base effect check ---
		if (basePotion != null && meta.getBasePotionType() != basePotion) {
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
	 * Resolve a base potion type (vanilla).
	 */
	private PotionType resolveBasePotion(String id) {
		if (id == null)
			return null;
		try {
			return PotionType.valueOf(id.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Resolve a custom potion effect type via Registry.
	 */
	private PotionEffectType resolveCustomEffect(String id) {
		String key = id.contains(":") ? id.toLowerCase(Locale.ROOT) : "minecraft:" + id.toLowerCase(Locale.ROOT);
		return PotionEffectResolver.resolve(key);
	}
}