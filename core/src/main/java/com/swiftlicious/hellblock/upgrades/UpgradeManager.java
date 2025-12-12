package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.addons.PlayerPointsHook;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.upgrades.costs.ExpCostHandler;
import com.swiftlicious.hellblock.upgrades.costs.ItemCostHandler;
import com.swiftlicious.hellblock.upgrades.costs.MoneyCostHandler;
import com.swiftlicious.hellblock.upgrades.costs.PointsCostHandler;
import com.swiftlicious.hellblock.utils.StringUtils;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

/**
 * Manages island upgrade tiers and their configuration, costs, and progression
 * logic.
 * <p>
 * This class is responsible for:
 * <ul>
 * <li>Loading and validating upgrade tiers from configuration</li>
 * <li>Managing upgrade cost processing</li>
 * <li>Providing upgrade tier information by type and level</li>
 * <li>Handling upgrade application and player interaction</li>
 * <li>Maintaining internal caches for max tiers and upgrade mappings</li>
 * </ul>
 * 
 * Implements {@link Reloadable} to support runtime reloading of upgrade
 * definitions.
 */
public class UpgradeManager implements Reloadable {

	protected final HellblockPlugin instance;

	private final Map<Integer, UpgradeTier> tierSettings = new TreeMap<>();
	// Holds the highest tier number per upgrade type
	private final Map<IslandUpgradeType, Integer> maxTiersByUpgrade = new HashMap<>();

	public UpgradeManager(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		loadCostHandlers();
		loadTiersFromConfig();
	}

	@Override
	public void unload() {
		this.tierSettings.clear();
		this.maxTiersByUpgrade.clear();
	}

	/**
	 * Registers built-in upgrade cost handlers with the
	 * {@link UpgradeCostProcessor}.
	 * <p>
	 * This includes handlers for:
	 * <ul>
	 * <li>{@code MONEY} – via Vault integration (if available)</li>
	 * <li>{@code POINTS} - via PlayerPoints integration (if available)</li>
	 * <li>{@code EXP}, {@code EXPERIENCE}, {@code XP} – for experience costs</li>
	 * <li>{@code ITEM} – for item-based upgrade costs</li>
	 * </ul>
	 * 
	 * This method is invoked during the {@code load()} phase to prepare the cost
	 * resolution system.
	 */
	private void loadCostHandlers() {
		final UpgradeCostProcessor processor = new UpgradeCostProcessor();

		// Register external handlers
		if (VaultHook.isHooked()) {
			processor.registerHandler(UpgradeCostType.MONEY, new MoneyCostHandler());
		}
		if (PlayerPointsHook.isHooked()) {
			processor.registerHandler(UpgradeCostType.POINTS, new PointsCostHandler());
		}

		// Register built-in handlers
		processor.registerHandler(List.of(UpgradeCostType.EXP, UpgradeCostType.EXPERIENCE, UpgradeCostType.XP),
				new ExpCostHandler());
		processor.registerHandler(UpgradeCostType.ITEM, new ItemCostHandler());
	}

	/**
	 * Loads and constructs an {@link UpgradeTier} from the given configuration
	 * section.
	 * <p>
	 * For each upgrade type found in the section:
	 * <ul>
	 * <li>Parses its value and associated cost data (if any)</li>
	 * <li>Performs validation against the previous tier (if provided)</li>
	 * <li>Supports both shorthand and structured formats</li>
	 * <li>Supports cost inheritance from previous tiers</li>
	 * </ul>
	 * If a value is invalid or an upgrade type is unknown, the method logs a
	 * warning and skips it.
	 *
	 * @param tier         the tier number being loaded (0 for default)
	 * @param tierSection  the configuration section representing this tier's data
	 * @param previousTier the previous {@link UpgradeTier}, used for inheritance
	 *                     and validation
	 * @return the constructed {@link UpgradeTier} instance
	 */
	@NotNull
	private UpgradeTier loadTier(int tier, @NotNull Section tierSection, @Nullable UpgradeTier previousTier) {
		UpgradeTier upgradeTier = new UpgradeTier(tier);

		for (Map.Entry<String, Object> entry : tierSection.getStringRouteMappedValues(false).entrySet()) {
			String rawKey = entry.getKey();
			String normalizedKey = rawKey.replace("-", "_").toUpperCase(Locale.ENGLISH);

			IslandUpgradeType upgradeType;
			try {
				upgradeType = IslandUpgradeType.valueOf(normalizedKey);
			} catch (IllegalArgumentException e) {
				instance.getPluginLogger().warn("Tier " + tier + " has unknown upgrade type: " + rawKey);
				continue;
			}

			Section upgradeDataSection = tierSection.getSection(rawKey);
			if (upgradeDataSection == null) {
				// shorthand form: protection-range: 125
				Object rawValue = entry.getValue();
				double value = upgradeType.isFloatType() ? parseDoubleStrict(rawValue, -1.0)
						: parseIntegerStrict(rawValue, -1);

				if ((!upgradeType.isFloatType() && value <= 0) || (upgradeType.isFloatType() && value < 0.0)) {
					instance.getPluginLogger().warn("Tier " + tier + " has invalid value for: " + upgradeType);
					continue;
				}

				// Allow tier 0 (default) to define value without cost
				if (tier == 0) {
					upgradeTier.addUpgrade(upgradeType, new UpgradeData(value, List.of()));
					continue;
				}

				if (previousTier != null && previousTier.getUpgrade(upgradeType) != null) {
					UpgradeData prev = previousTier.getUpgrade(upgradeType);
					validateTierValue(upgradeType, tier, value, previousTier);
					upgradeTier.addUpgrade(upgradeType, new UpgradeData(value, prev.getCosts()));
				} else {
					instance.getPluginLogger()
							.warn("Tier " + tier + " for " + upgradeType + " has no costs defined and cannot inherit.");
					continue;
				}
				continue;
			}

			try {
				UpgradeData upgradeData = loadUpgradeData(upgradeDataSection, previousTier, upgradeType, tier);
				validateTierValue(upgradeType, tier, upgradeData.getValue(), previousTier);
				upgradeTier.addUpgrade(upgradeType, upgradeData);
			} catch (Exception e) {
				instance.getPluginLogger()
						.warn("Failed to load upgrade '" + upgradeType + "' at tier " + tier + ": " + e.getMessage());
			}
		}

		return upgradeTier;
	}

	/**
	 * Parses an {@link UpgradeData} object from a structured configuration section.
	 * <p>
	 * Reads the upgrade value and cost list for a specific upgrade type at a given
	 * tier. If no costs are provided and a previous tier exists, costs are
	 * inherited.
	 * <p>
	 * Supports two cost representations:
	 * <ul>
	 * <li>YAML section objects (typed and amount-based)</li>
	 * <li>Map-based fallback format</li>
	 * </ul>
	 * 
	 * Also aggregates costs of the same type (e.g., multiple items or EXP entries).
	 *
	 * @param section      the configuration section representing the upgrade
	 *                     definition
	 * @param previousTier the previous tier to inherit costs from, if applicable
	 * @param type         the {@link IslandUpgradeType} this data applies to
	 * @param tier         the tier number (used for validation and error reporting)
	 * @return the parsed {@link UpgradeData} with value and associated costs
	 * @throws IllegalArgumentException if value is missing or invalid
	 */
	@NotNull
	private UpgradeData loadUpgradeData(@NotNull Section section, @Nullable UpgradeTier previousTier,
			@NotNull IslandUpgradeType type, int tier) {
		double value = type.isFloatType() ? parseDoubleStrict(section.get("value"), -1.0)
				: parseIntegerStrict(section.get("value"), -1);
		if (value <= 0) {
			throw new IllegalArgumentException("Tier " + tier + " value for " + type + " must be a positive integer.");
		}

		Map<String, Double> itemCosts = new HashMap<>();
		Map<UpgradeCostType, Double> mergedCosts = new HashMap<>();
		List<UpgradeCost> finalCosts = new ArrayList<>();

		Object rawCosts = section.get("costs");
		if (rawCosts instanceof List<?> list) {
			list.forEach((Object entry) -> {
				if (entry instanceof Section costSec) {
					processCostSection(costSec, mergedCosts, itemCosts);
				} else if (entry instanceof Map<?, ?> map) {
					processCostMap(map, mergedCosts, itemCosts);
				} else {
					instance.getPluginLogger().warn("Unexpected cost entry type: " + entry.getClass().getSimpleName());
				}
			});
		} else if (previousTier != null && previousTier.getUpgrade(type) != null) {
			// Fallback: reuse previous tier’s costs
			instance.debug("Tier inherits previous costs for " + type + " (no costs specified)");
			UpgradeData prev = previousTier.getUpgrade(type);
			return new UpgradeData(value, prev.getCosts());
		} else {
			instance.getPluginLogger().warn("Tier " + tier + " for " + type + " has no costs and cannot inherit any.");
		}

		// Merge item-based costs (multiple materials)
		if (!itemCosts.isEmpty()) {
			itemCosts.forEach(
					(item, totalAmount) -> finalCosts.add(new UpgradeCost(UpgradeCostType.ITEM, totalAmount, item)));
		}

		// Merge non-item duplicate types (like multiple MONEY or EXP)
		mergedCosts.forEach((costType, totalAmount) -> {
			if (costType != UpgradeCostType.ITEM) {
				finalCosts.add(new UpgradeCost(costType, totalAmount, null));
			}
		});

		return new UpgradeData(value, finalCosts);
	}

	/**
	 * Processes a single cost entry defined as a {@link Section} from the
	 * configuration.
	 * <p>
	 * Supports both item-based and type-based costs. Item costs can be defined in
	 * two ways:
	 * <ul>
	 * <li>A single item via {@code item}</li>
	 * <li>A list of items via {@code items}</li>
	 * </ul>
	 * Other cost types (e.g., MONEY, EXP) are accumulated in {@code typeTotals}.
	 *
	 * @param costSec    the section containing cost definition (type, amount,
	 *                   item/items)
	 * @param typeTotals map to accumulate non-item cost totals by type
	 * @param itemCosts  map to accumulate item cost totals by item name
	 */
	private void processCostSection(@NotNull Section costSec, @NotNull Map<UpgradeCostType, Double> typeTotals,
			@NotNull Map<String, Double> itemCosts) {
		UpgradeCostType type;
		try {
			type = UpgradeCostType.valueOf(nonNullString(costSec.get("type"), UpgradeCostType.NONE.toString()));
		} catch (IllegalArgumentException e) {
			instance.getPluginLogger().warn("Upgrade cost type " + nullableString(costSec.get("type"))
					+ " is invalid! Only the following are acceptable: " + UpgradeCostType.values());
			type = UpgradeCostType.NONE;
		}
		double amount = parseDoubleStrict(costSec.get("amount"), 0.0);

		if (type == UpgradeCostType.ITEM) {
			// support multiple items
			if (costSec.contains("items") && costSec.get("items") instanceof List<?> items) {
				items.stream().map(String::valueOf).forEach(item -> itemCosts.merge(item, amount, Double::sum));
			} else {
				String item = nullableString(costSec.get("item"));
				if (item != null)
					itemCosts.merge(item, amount, Double::sum);
			}
		} else {
			typeTotals.merge(type, amount, Double::sum);
		}
	}

	/**
	 * Processes a cost entry defined as a {@link Map}, typically used as a fallback
	 * when the YAML parser reads inline map structures instead of sections.
	 * <p>
	 * Supports both item-based and general upgrade cost types. Item costs can
	 * include:
	 * <ul>
	 * <li>A single {@code item}</li>
	 * <li>A list of {@code items}</li>
	 * </ul>
	 * All costs are merged into the appropriate accumulation maps.
	 *
	 * @param map        the raw map containing keys like {@code type},
	 *                   {@code amount}, {@code item/items}
	 * @param typeTotals map to accumulate non-item cost totals by type
	 * @param itemCosts  map to accumulate item cost totals by item name
	 */
	private void processCostMap(@NotNull Map<?, ?> map, Map<UpgradeCostType, Double> typeTotals,
			@NotNull Map<String, Double> itemCosts) {
		Object typeObj = map.get("type");
		UpgradeCostType type;
		try {
			type = UpgradeCostType
					.valueOf((typeObj != null ? String.valueOf(typeObj) : UpgradeCostType.NONE.toString()));
		} catch (IllegalArgumentException e) {
			instance.getPluginLogger().warn("Upgrade cost type " + typeObj.toString()
					+ " is invalid! Only the following are acceptable: " + UpgradeCostType.values());
			type = UpgradeCostType.NONE;
		}
		double amount = parseDoubleStrict(map.get("amount"), 0.0);

		if (type == UpgradeCostType.ITEM) {
			Object itemObj = map.get("item");
			Object itemsObj = map.get("items");
			if (itemsObj instanceof List<?> items) {
				items.forEach((Object item) -> itemCosts.merge(String.valueOf(item), amount, Double::sum));
			} else if (itemObj != null) {
				itemCosts.merge(String.valueOf(itemObj), amount, Double::sum);
			}
		} else {
			typeTotals.merge(type, amount, Double::sum);
		}
	}

	/**
	 * Validates the upgrade value for a given type at a specific tier.
	 * <p>
	 * Checks include:
	 * <ul>
	 * <li>Ensuring the value is positive</li>
	 * <li>Ensuring non-float upgrade types have increasing values across tiers</li>
	 * </ul>
	 * If validation fails, an exception is thrown to prevent invalid tier
	 * registration.
	 *
	 * @param type         the {@link IslandUpgradeType} being validated
	 * @param tier         the tier number being checked
	 * @param value        the upgrade value at this tier
	 * @param previousTier the previous tier for comparison (used in increasing
	 *                     value validation)
	 * @throws IllegalArgumentException if the value is non-positive
	 * @throws IllegalStateException    if the value is not greater than the
	 *                                  previous for non-float types
	 */
	private void validateTierValue(@NotNull IslandUpgradeType type, int tier, @Nullable Number value,
			@Nullable UpgradeTier previousTier) {
		if (value != null && value.doubleValue() <= 0.0) {
			throw new IllegalArgumentException("Tier " + tier + " value for " + type + " must be positive.");
		}

		// Only enforce increasing values for non-float types
		if (previousTier != null && !type.isFloatType()) {
			UpgradeData prev = previousTier.getUpgrade(type);
			if (value != null && prev != null && prev.getValue() != null
					&& value.doubleValue() <= prev.getValue().doubleValue()) {
				throw new IllegalStateException("Tier " + tier + " value for " + type + " (" + value
						+ ") must be greater than previous tier (" + prev.getValue() + ")");
			}
		}
	}

	/**
	 * Returns the string representation of an object, or a default string if the
	 * object is {@code null}.
	 *
	 * @param o   the object to convert to string
	 * @param def the default value to return if {@code o} is {@code null}
	 * @return the object's string representation, or {@code def} if {@code o} is
	 *         {@code null}
	 */
	@Nullable
	private String nonNullString(@Nullable Object o, @NotNull String def) {
		return (o == null) ? def : String.valueOf(o);
	}

	/**
	 * Returns the string representation of an object, or {@code null} if the object
	 * is {@code null}.
	 *
	 * @param o the object to convert to string
	 * @return the object's string representation, or {@code null} if the object is
	 *         {@code null}
	 */
	@Nullable
	private String nullableString(@Nullable Object o) {
		return (o == null) ? null : String.valueOf(o);
	}

	/**
	 * Attempts to strictly parse an integer from the given value.
	 * <p>
	 * Accepts numeric types and strings that match an optional sign followed by
	 * digits (e.g., {@code "42"}, {@code "-7"}). If parsing fails, the default
	 * value is returned.
	 *
	 * @param value the input to parse (can be a {@link Number} or {@link String})
	 * @param def   the default value to return if parsing fails
	 * @return the parsed integer, or {@code def} if parsing fails
	 */
	private int parseIntegerStrict(@NotNull Object value, int def) {
		if (value instanceof Number num) {
			return num.intValue();
		}
		if (value instanceof String s) {
			s = s.trim();
			if (s.matches("[+-]?\\d+")) {
				try {
					return Integer.parseInt(s);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return def;
	}

	/**
	 * Attempts to strictly parse a double from the given value.
	 * <p>
	 * Accepts numeric types and strings that represent valid decimal numbers (e.g.,
	 * {@code "3.14"}, {@code "-2"}, {@code "+0.5"}). If parsing fails or the input
	 * is invalid, the default value is returned.
	 *
	 * @param value the input to parse (can be a {@link Number} or {@link String})
	 * @param def   the default value to return if parsing fails
	 * @return the parsed double value, or {@code def} if parsing fails
	 */
	private double parseDoubleStrict(@NotNull Object value, double def) {
		if (value instanceof Number num) {
			return num.doubleValue();
		}
		if (value instanceof String s) {
			// accept integers or decimals, with optional leading +/-
			if (s.matches("[+-]?\\d+(\\.\\d+)?")) {
				try {
					return Double.parseDouble(s);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return def;
	}

	/**
	 * Loads all upgrade tiers from the plugin configuration.
	 * <p>
	 * This includes:
	 * <ul>
	 * <li>Parsing the "default" (tier 0) configuration section</li>
	 * <li>Loading all tier sections in numeric order</li>
	 * <li>Validating and registering each tier</li>
	 * <li>Tracking the highest defined tier per upgrade type</li>
	 * </ul>
	 * If the configuration section is missing or invalid, logs warnings
	 * accordingly.
	 */
	private void loadTiersFromConfig() {
		this.tierSettings.clear();
		this.maxTiersByUpgrade.clear();

		Section upgradeSection = instance.getConfigManager().getMainConfig().getSection("hellblock.upgrades");
		if (upgradeSection == null) {
			instance.getPluginLogger().warn("No upgrade section found in configuration!");
			return;
		}

		UpgradeTier previousTier = null;

		// Load default (tier 0)
		Section defaultSection = upgradeSection.getSection("default");
		if (defaultSection != null) {
			UpgradeTier defaultTier = loadTier(0, defaultSection, null);
			this.tierSettings.put(0, defaultTier);
			previousTier = defaultTier;
		}

		// Load numbered tiers from hellblock.upgrades.tiers
		Section tiersSection = upgradeSection.getSection("tiers");
		if (tiersSection == null) {
			instance.getPluginLogger().warn("No 'tiers' section found in upgrades section from config!");
			return;
		}

		for (String key : tiersSection.getRoutesAsStrings(false)) {
			int tierNumber;
			try {
				tierNumber = Integer.parseInt(key);
			} catch (NumberFormatException e) {
				instance.getPluginLogger().warn("Invalid tier key under 'tiers': " + key);
				continue;
			}

			if (tierNumber <= 0) {
				instance.getPluginLogger().warn("Tier key must be positive: " + key);
				continue;
			}

			Section tierSection = tiersSection.getSection(key);
			if (tierSection == null) {
				instance.getPluginLogger().warn("Tier section missing for key: " + key);
				continue;
			}

			// Load with previous tier for validation and fallback
			UpgradeTier tierData = loadTier(tierNumber, tierSection, previousTier);
			this.tierSettings.put(tierNumber, tierData);
			previousTier = tierData;

			// Update per-upgrade max tier tracking
			tierData.getUpgrades().forEach((upgradeType, data) -> {
				int currentMax = maxTiersByUpgrade.getOrDefault(upgradeType, 0);
				maxTiersByUpgrade.put(upgradeType, Math.max(currentMax, tierNumber));
			});
		}
	}

	/**
	 * Retrieves the highest defined tier number for the specified
	 * {@link IslandUpgradeType}.
	 *
	 * @param upgradeType the upgrade type to query
	 * @return the maximum tier number for the upgrade type, or {@code 0} if none
	 *         defined
	 */
	public int getMaxTierFor(@NotNull IslandUpgradeType upgradeType) {
		return maxTiersByUpgrade.getOrDefault(upgradeType, 0);
	}

	/**
	 * Retrieves the {@link UpgradeTier} object associated with the given tier
	 * number.
	 *
	 * @param tier the numeric tier to retrieve
	 * @return the {@link UpgradeTier} for the specified tier, or {@code null} if
	 *         not found
	 */
	@Nullable
	public UpgradeTier getTier(int tier) {
		return tierSettings.get(tier);
	}

	/**
	 * Returns the default upgrade tier, which corresponds to tier {@code 0}.
	 *
	 * @return the default {@link UpgradeTier}, or {@code null} if not loaded
	 */
	@Nullable
	public UpgradeTier getDefaultTier() {
		return tierSettings.get(0); // tier 0 = default
	}

	/**
	 * Retrieves the default value for a specific {@link IslandUpgradeType}, as
	 * defined in tier 0.
	 * <p>
	 * If the default tier or upgrade data is not available, returns {@code 0} as a
	 * fallback.
	 *
	 * @param upgradeType the upgrade type to query
	 * @return the default value for the upgrade type, or {@code 0} if not defined
	 */
	@NotNull
	public Number getDefaultValue(@NotNull IslandUpgradeType upgradeType) {
		UpgradeTier defaultTier = getDefaultTier();
		if (defaultTier != null) {
			UpgradeData data = defaultTier.getUpgrade(upgradeType);
			if (data != null && data.getValue() != null) {
				return data.getValue();
			}
		}
		return 0; // fallback if not defined
	}

	/**
	 * Retrieves the maximum configured value for a given {@link IslandUpgradeType}
	 * across all tiers, including tier 0 (default).
	 *
	 * @param upgradeType the upgrade type to query
	 * @return the highest upgrade value found, or {@code 0} if not defined
	 */
	@NotNull
	public Number getMaxValue(@NotNull IslandUpgradeType upgradeType) {
		Number max = 0;

		// Check default first
		UpgradeTier defaultTier = tierSettings.get(0);
		if (defaultTier != null) {
			UpgradeData defaultData = defaultTier.getUpgrades().get(upgradeType);
			if (defaultData != null && defaultData.getValue() != null) {
				max = defaultData.getValue();
			}
		}

		// Check all other tiers
		for (UpgradeTier tier : tierSettings.values()) {
			UpgradeData upgradeData = tier.getUpgrades().get(upgradeType);
			if (max != null && upgradeData != null && upgradeData.getValue() != null) {
				max = Math.max(max.doubleValue(), upgradeData.getValue().doubleValue());
			}
		}

		return max;
	}

	/**
	 * Retrieves the effective upgrade value for the specified tier and upgrade
	 * type.
	 * <p>
	 * If the upgrade data is missing at the given tier, returns {@code 0} as a
	 * fallback.
	 *
	 * @param tier        the tier number to check
	 * @param upgradeType the {@link IslandUpgradeType} to retrieve
	 * @return the upgrade value for the given tier and type, or {@code 0} if not
	 *         defined
	 */
	@NotNull
	public Number getEffectiveValue(int tier, @NotNull IslandUpgradeType upgradeType) {
		UpgradeData data = getUpgradeData(tier, upgradeType);
		if (data != null) {
			return data.getValue();
		}

		// If completely missing, default to 0
		return 0;
	}

	/**
	 * Retrieves a map of all upgrade data entries for a specific
	 * {@link IslandUpgradeType}, grouped by tier.
	 * <p>
	 * Only tiers that define the specified upgrade type will be included.
	 *
	 * @param upgradeType the upgrade type to collect data for
	 * @return a tier-ordered map of upgrade data, where keys are tier numbers
	 */
	@NotNull
	public Map<Integer, UpgradeData> getAllUpgradeData(IslandUpgradeType upgradeType) {
		Map<Integer, UpgradeData> result = new TreeMap<>(); // ordered by tier
		tierSettings.entrySet().forEach(entry -> {
			UpgradeData data = entry.getValue().getUpgrades().get(upgradeType);
			if (data != null) {
				result.put(entry.getKey(), data);
			}
		});
		return result;
	}

	/**
	 * Retrieves all upgrade data for every tier, grouped by tier number.
	 * <p>
	 * Each entry in the map contains a mapping of {@link IslandUpgradeType} to
	 * {@link UpgradeData} for that specific tier.
	 *
	 * @return a tier-ordered map of all upgrades, with each tier mapping to its
	 *         upgrade definitions
	 */
	@NotNull
	public Map<Integer, Map<IslandUpgradeType, UpgradeData>> getAllUpgradesByTier() {
		Map<Integer, Map<IslandUpgradeType, UpgradeData>> result = new TreeMap<>();
		// Copy upgrades map to avoid exposing internal state
		tierSettings.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getUpgrades()));
		return result;
	}

	/**
	 * Finds the next tier number above the current tier that defines an upgrade for
	 * the given {@link IslandUpgradeType}.
	 * <p>
	 * This is useful for determining the next eligible upgrade level.
	 *
	 * @param currentTier the current tier number
	 * @param upgradeType the upgrade type to look for
	 * @return the next tier number that defines this upgrade type, or {@code null}
	 *         if none found
	 */
	@Nullable
	public Integer getNextAvailableTier(int currentTier, @NotNull IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);
		for (int t = currentTier + 1; t <= maxTier; t++) {
			UpgradeData data = getUpgradeData(t, upgradeType);
			if (data != null) {
				return t; // Found the next available tier for this upgrade
			}
		}
		return null; // No further upgrades exist
	}

	/**
	 * Retrieves the {@link UpgradeData} for the next tier above the current one for
	 * the specified {@link IslandUpgradeType}.
	 * <p>
	 * If the upgrade is already at its maximum tier or not defined, this returns
	 * {@code null}.
	 *
	 * @param currentTier the current tier number
	 * @param upgradeType the upgrade type to look up
	 * @return the {@link UpgradeData} for the next tier, or {@code null} if no
	 *         further upgrade exists
	 */
	@Nullable
	public UpgradeData getNextUpgradeData(int currentTier, @NotNull IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);

		// If upgrade type doesn't exist or already at max, return null
		if (maxTier <= 0 || currentTier >= maxTier) {
			return null;
		}

		// Look at the next tier
		int nextTier = currentTier + 1;
		return getUpgradeData(nextTier, upgradeType);
	}

	/**
	 * Retrieves the {@link UpgradeData} for a specific tier and upgrade type.
	 * <p>
	 * If no data is found at the specified tier, it falls back to the default tier
	 * (tier 0).
	 *
	 * @param tier        the tier number to check
	 * @param upgradeType the upgrade type to retrieve data for
	 * @return the {@link UpgradeData} for the given tier and type, or {@code null}
	 *         if not defined
	 */
	@Nullable
	public UpgradeData getUpgradeData(int tier, @NotNull IslandUpgradeType upgradeType) {
		UpgradeTier upgradeTier = tierSettings.get(tier);
		if (upgradeTier != null) {
			UpgradeData data = upgradeTier.getUpgrade(upgradeType);
			if (data != null) {
				return data; // found at this tier
			}
		}

		// Fallback: check tier 0 (default)
		UpgradeTier defaultTier = tierSettings.get(0);
		if (defaultTier != null) {
			return defaultTier.getUpgrade(upgradeType);
		}

		return null; // nothing found at all
	}

	/**
	 * Calculates the effective upgrade value for a given {@link IslandUpgradeType}
	 * based on the player's island data and any cached dynamic bonuses (e.g.
	 * generator chance, crop growth).
	 * <p>
	 * Delegates to specialized handlers for types that use runtime modifiers. Falls
	 * back to the static upgrade value stored in the {@link HellblockData} for
	 * other types.
	 *
	 * @param data the {@link HellblockData} associated with the player's island
	 * @param type the upgrade type to retrieve the effective value for
	 * @return the effective value, including any cached bonuses
	 */
	public double getEffectiveUpgradeValue(@NotNull HellblockData data, @NotNull IslandUpgradeType type) {
		switch (type) {
		case GENERATOR_CHANCE:
			return instance.getNetherrackGeneratorHandler().getCachedGeneratorBonus(data);
		case PIGLIN_BARTERING:
			return instance.getPiglinBarterHandler().getCachedBarterBonus(data);
		case CROP_GROWTH:
			return instance.getFarmingManager().getCachedCropGrowthBonus(data);
		case MOB_SPAWN_RATE:
			return instance.getMobSpawnHandler().getCachedMobSpawnBonus(data);
		default:
			return data.getValue(type).intValue(); // fallback to standard value
		}
	}

	/**
	 * Calculates the total cumulative value for a given {@link IslandUpgradeType}
	 * up to the current upgrade level on the player's island.
	 * <p>
	 * This sums the values from all tiers starting from tier 0 up to the current
	 * level. Useful for additive upgrades (e.g., range increases, spawn boosts).
	 *
	 * @param data the {@link HellblockData} associated with the island
	 * @param type the upgrade type to calculate the total value for
	 * @return the total summed value across all applicable tiers
	 */
	public double calculateTotalUpgradeValue(@NotNull HellblockData data, @NotNull IslandUpgradeType type) {
		int level = data.getUpgradeLevel(type);
		double total = 0.0;

		for (int i = 0; i <= level; i++) {
			UpgradeTier tier = instance.getUpgradeManager().getTier(i);
			if (tier != null) {
				UpgradeData upgrade = tier.getUpgrade(type);
				if (upgrade != null && upgrade.getValue() != null) {
					total += upgrade.getValue().doubleValue();
				}
			}
		}

		return total;
	}

	/**
	 * Determines whether the specified upgrade type can still be upgraded beyond
	 * the current tier.
	 * <p>
	 * Returns {@code false} if:
	 * <ul>
	 * <li>The upgrade type is not defined in any tier</li>
	 * <li>The current tier is already the highest available tier for the
	 * upgrade</li>
	 * </ul>
	 *
	 * @param currentTier the player's current upgrade tier
	 * @param upgradeType the upgrade type to check
	 * @return {@code true} if an upgrade is possible; {@code false} otherwise
	 */
	public boolean canUpgrade(int currentTier, @NotNull IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);

		// If the upgrade isn't defined at all, can't upgrade
		if (maxTier <= 0) {
			return false;
		}

		// If current tier is below max tier, we can still upgrade
		return currentTier < maxTier;
	}

	/**
	 * Returns a set of all {@link IslandUpgradeType}s that are defined in the
	 * configuration.
	 * <p>
	 * These represent the upgrade types that have at least one tier configured.
	 *
	 * @return a set of available upgrade types
	 */
	@NotNull
	public Set<IslandUpgradeType> getAvailableUpgrades() {
		return maxTiersByUpgrade.keySet();
	}

	/**
	 * Retrieves the current and next upgrade data for the specified
	 * {@link IslandUpgradeType} and constructs an {@link UpgradeProgress} object.
	 * <p>
	 * This includes:
	 * <ul>
	 * <li>The current tier and its data</li>
	 * <li>The next tier (if available) and its data</li>
	 * </ul>
	 *
	 * @param currentTier the player's current upgrade tier
	 * @param upgradeType the upgrade type to check
	 * @return an {@link UpgradeProgress} object containing both current and next
	 *         tier info
	 */
	@NotNull
	public UpgradeProgress getUpgradeProgress(int currentTier, @NotNull IslandUpgradeType upgradeType) {
		UpgradeData currentData = getUpgradeData(currentTier, upgradeType);
		UpgradeData nextData = getNextUpgradeData(currentTier, upgradeType);

		int maxTier = getMaxTierFor(upgradeType);
		int nextTier = (nextData != null && currentTier < maxTier) ? currentTier + 1 : -1;

		return new UpgradeProgress(currentTier, currentData, nextTier, nextData);
	}

	/**
	 * Attempts to process an upgrade purchase for the given {@link Player} and
	 * {@link IslandUpgradeType}.
	 * <p>
	 * Performs several checks before proceeding:
	 * <ul>
	 * <li>Validates island ownership and existence</li>
	 * <li>Checks if the upgrade can still progress</li>
	 * <li>Validates the availability and cost of the next tier</li>
	 * <li>Ensures the player can afford the upgrade costs</li>
	 * </ul>
	 * If all checks pass, deducts costs, applies the upgrade, notifies the player,
	 * updates relevant caches, and plays a success sound.
	 *
	 * @param data   the {@link HellblockData} for the player's island
	 * @param player the player attempting the upgrade
	 * @param type   the upgrade type being purchased
	 * @return {@code true} if the upgrade was successfully applied; {@code false}
	 *         otherwise
	 */
	public boolean attemptPurchase(@NotNull HellblockData data, @NotNull Player player,
			@NotNull IslandUpgradeType type) {
		final Sender owner = instance.getSenderFactory().wrap(player);

		// Ensure hellblock exists
		if (!data.hasHellblock()) {
			owner.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_NOT_FOUND.build()));
			return false;
		}

		// Ensure owner is the one upgrading
		if (!data.isOwner(player.getUniqueId())) {
			owner.sendMessage(
					instance.getTranslationManager().render(MessageConstants.MSG_NOT_OWNER_OF_HELLBLOCK.build()));
			return false;
		}

		// Ensure not already maxed
		if (!data.canUpgrade(type)) {
			owner.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UPGRADE_MAX_TIER
					.arguments(AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(type.toString()))).build()));
			return false;
		}

		// Get upgrade data
		final UpgradeData upgradeData = getNextUpgradeData(data.getUpgradeLevel(type), type);
		if (upgradeData == null) {
			owner.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UPGRADE_NO_COST
					.arguments(AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(type.toString()))).build()));
			return false;
		}

		final List<UpgradeCost> costs = upgradeData.getCosts();
		if (costs == null || costs.isEmpty()) {
			owner.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UPGRADE_NO_COST
					.arguments(AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(type.toString()))).build()));
			return false;
		}

		// Check if player can afford
		final UpgradeCostProcessor payment = new UpgradeCostProcessor(player);
		if (!payment.canAfford(costs)) {
			owner.sendMessage(instance.getTranslationManager()
					.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_CANNOT_AFFORD.build()));
			return false;
		}

		// Deduct all costs
		payment.deduct(costs);

		// --- Apply the upgrade ---
		final int oldRange = data.getMaxProtectionRange();
		data.applyUpgrade(type);
		final int newRange = data.getMaxProtectionRange();

		// Notify player
		owner.sendMessage(
				instance.getTranslationManager()
						.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_SUCCESS
								.arguments(AdventureHelper.miniMessageToComponent(StringUtils.toCamelCase(type.toString())),
										AdventureHelper.miniMessageToComponent(String.valueOf(data.getUpgradeLevel(type))))
								.build()));
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(Key.key("minecraft:entity.player.levelup"), Source.PLAYER, 1.0F, 1.0F));

		// Log
		instance.debug(
				"Island owned by " + player.getName() + " upgraded " + type + " to tier " + data.getUpgradeLevel(type));

		int islandId = data.getIslandId();

		switch (type) {
		case PROTECTION_RANGE -> {
			instance.getBorderHandler().animateRangeExpansion(data, oldRange, newRange);
		}

		case GENERATOR_CHANCE -> {
			instance.getNetherrackGeneratorHandler().invalidateGeneratorBonusCache(islandId);
			instance.getNetherrackGeneratorHandler().updateGeneratorBonusCache(data);
		}

		case PIGLIN_BARTERING -> {
			instance.getPiglinBarterHandler().invalidateBarterBonusCache(islandId);
			instance.getPiglinBarterHandler().updateBarterBonusCache(data);
		}

		case CROP_GROWTH -> {
			instance.getFarmingManager().invalidateCropGrowthBonusCache(islandId);
			instance.getFarmingManager().updateCropGrowthBonusCache(data);
		}

		case MOB_SPAWN_RATE -> {
			instance.getMobSpawnHandler().invalidateMobSpawnBonusCache(islandId);
			instance.getMobSpawnHandler().updateMobSpawnBonusCache(data);
		}

		default -> {
			// No cache update needed for other upgrade types
		}
		}

		return true;
	}

	/**
	 * Revalidates all cached upgrade bonuses for a player by their {@link UUID}.
	 * <p>
	 * This ensures that all runtime-dependent upgrade effects (such as generator
	 * bonuses, crop growth rates, barter bonuses, etc.) are recalculated.
	 * <p>
	 * If {@code data} is provided, cache values will be updated immediately after
	 * invalidation.
	 *
	 * @param islandId the unique ID of the island
	 * @param data     the {@link HellblockData} to use for updating cached values
	 *                 (optional)
	 */
	public void revalidateUpgradeCache(int islandId, @Nullable HellblockData data) {
		instance.getNetherrackGeneratorHandler().invalidateGeneratorBonusCache(islandId);
		instance.getPiglinBarterHandler().invalidateBarterBonusCache(islandId);
		instance.getFarmingManager().invalidateCropGrowthBonusCache(islandId);
		instance.getMobSpawnHandler().invalidateMobSpawnBonusCache(islandId);
		if (data != null) {
			instance.getNetherrackGeneratorHandler().updateGeneratorBonusCache(data);
			instance.getPiglinBarterHandler().updateBarterBonusCache(data);
			instance.getFarmingManager().updateCropGrowthBonusCache(data);
			instance.getMobSpawnHandler().updateMobSpawnBonusCache(data);
		}
	}
}