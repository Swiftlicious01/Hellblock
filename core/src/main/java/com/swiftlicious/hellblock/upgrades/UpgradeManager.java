package com.swiftlicious.hellblock.upgrades;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.creation.addons.VaultHook;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.player.HellblockData;
import com.swiftlicious.hellblock.sender.Sender;
import com.swiftlicious.hellblock.utils.StringUtils;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

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

	private void loadCostHandlers() {
		final UpgradeCostProcessor processor = new UpgradeCostProcessor();

		// Register built-in handlers
		if (VaultHook.isHooked()) {
			processor.registerHandler("MONEY", new MoneyCostHandler());
		}
		processor.registerHandler(List.of("EXP", "EXPERIENCE", "XP"), new ExpCostHandler());
		processor.registerHandler("ITEM", new ItemCostHandler());
	}

	private UpgradeTier loadTier(int tier, Section tierSection, @Nullable UpgradeTier previousTier) {
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

	private UpgradeData loadUpgradeData(Section section, @Nullable UpgradeTier previousTier, IslandUpgradeType type,
			int tier) {
		double value = type.isFloatType() ? parseDoubleStrict(section.get("value"), -1.0)
				: parseIntegerStrict(section.get("value"), -1);
		if (value <= 0) {
			throw new IllegalArgumentException("Tier " + tier + " value for " + type + " must be a positive integer.");
		}

		Map<String, Double> itemCosts = new HashMap<>();
		Map<String, Double> mergedCosts = new HashMap<>();
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
			instance.getPluginLogger().info("Tier inherits previous costs for " + type + " (no costs specified)");
			UpgradeData prev = previousTier.getUpgrade(type);
			return new UpgradeData(value, prev.getCosts());
		} else {
			instance.getPluginLogger().warn("Tier " + tier + " for " + type + " has no costs and cannot inherit any.");
		}

		// Merge item-based costs (multiple materials)
		if (!itemCosts.isEmpty()) {
			itemCosts.forEach((item, totalAmount) -> finalCosts.add(new UpgradeCost("ITEM", totalAmount, item)));
		}

		// Merge non-item duplicate types (like multiple MONEY or EXP)
		mergedCosts.forEach((typeName, totalAmount) -> {
			if (!"ITEM".equals(typeName)) {
				finalCosts.add(new UpgradeCost(typeName, totalAmount, null));
			}
		});

		return new UpgradeData(value, finalCosts);
	}

	private void processCostSection(Section costSec, Map<String, Double> typeTotals, Map<String, Double> itemCosts) {
		String type = nonNullString(costSec.get("type"), "UNKNOWN").toUpperCase(Locale.ENGLISH);
		double amount = parseDoubleStrict(costSec.get("amount"), 0.0);

		if ("ITEM".equals(type)) {
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

	private void processCostMap(Map<?, ?> map, Map<String, Double> typeTotals, Map<String, Double> itemCosts) {
		Object typeObj = map.get("type");
		String type = (typeObj != null ? String.valueOf(typeObj) : "UNKNOWN").toUpperCase(Locale.ENGLISH);
		double amount = parseDoubleStrict(map.get("amount"), 0.0);

		if ("ITEM".equals(type)) {
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

	private void validateTierValue(IslandUpgradeType type, int tier, Number value, @Nullable UpgradeTier previousTier) {
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

	private String nonNullString(Object o, String def) {
		return (o == null) ? def : String.valueOf(o);
	}

	private String nullableString(Object o) {
		return (o == null) ? null : String.valueOf(o);
	}

	private int parseIntegerStrict(Object value, int def) {
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

	private double parseDoubleStrict(Object value, double def) {
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

	private void loadTiersFromConfig() {
		this.tierSettings.clear();
		this.maxTiersByUpgrade.clear();

		Section upgradeSection = instance.getConfigManager().getMainConfig().getSection("hellblock.upgrades");
		if (upgradeSection == null) {
			instance.getPluginLogger().warn("No upgrade section found in configuration!");
			return;
		}

		UpgradeTier previousTier = null;

		for (String key : upgradeSection.getRoutesAsStrings(false)) {
			if ("default".equalsIgnoreCase(key)) {
				// Default = tier 0
				Section defaultSection = upgradeSection.getSection(key);
				if (defaultSection != null) {
					UpgradeTier defaultTier = loadTier(0, defaultSection, null);
					this.tierSettings.put(0, defaultTier);
					previousTier = defaultTier;
				}
				continue;
			}

			if (!key.startsWith("tier-")) {
				continue; // Skip unrelated sections
			}

			int tierNumber;
			try {
				tierNumber = Integer.parseInt(key.substring("tier-".length()));
			} catch (NumberFormatException e) {
				instance.getPluginLogger().warn("Invalid tier key: " + key);
				continue;
			}

			Section tierSection = upgradeSection.getSection(key);
			if (tierSection == null) {
				instance.getPluginLogger().warn("Tier section missing for: " + key);
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

	public int getMaxTierFor(IslandUpgradeType upgradeType) {
		return maxTiersByUpgrade.getOrDefault(upgradeType, 0);
	}

	public UpgradeTier getTier(int tier) {
		return tierSettings.get(tier);
	}

	public UpgradeTier getDefaultTier() {
		return tierSettings.get(0); // tier 0 = default
	}

	public Number getDefaultValue(IslandUpgradeType upgradeType) {
		UpgradeTier defaultTier = getDefaultTier();
		if (defaultTier != null) {
			UpgradeData data = defaultTier.getUpgrade(upgradeType);
			if (data != null && data.getValue() != null) {
				return data.getValue();
			}
		}
		return 0; // fallback if not defined
	}

	public Number getMaxValue(IslandUpgradeType upgradeType) {
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

	public Number getEffectiveValue(int tier, IslandUpgradeType upgradeType) {
		UpgradeData data = getUpgradeData(tier, upgradeType);
		if (data != null) {
			return data.getValue();
		}

		// If completely missing, default to 0
		return 0;
	}

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

	public Map<Integer, Map<IslandUpgradeType, UpgradeData>> getAllUpgradesByTier() {
		Map<Integer, Map<IslandUpgradeType, UpgradeData>> result = new TreeMap<>();
		// Copy upgrades map to avoid exposing internal state
		tierSettings.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getUpgrades()));
		return result;
	}

	public Integer getNextAvailableTier(int currentTier, IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);
		for (int t = currentTier + 1; t <= maxTier; t++) {
			UpgradeData data = getUpgradeData(t, upgradeType);
			if (data != null) {
				return t; // Found the next available tier for this upgrade
			}
		}
		return null; // No further upgrades exist
	}

	public UpgradeData getNextUpgradeData(int currentTier, IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);

		// If upgrade type doesn't exist or already at max, return null
		if (maxTier <= 0 || currentTier >= maxTier) {
			return null;
		}

		// Look at the next tier
		int nextTier = currentTier + 1;
		return getUpgradeData(nextTier, upgradeType);
	}

	public UpgradeData getUpgradeData(int tier, IslandUpgradeType upgradeType) {
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

	public double getEffectiveUpgradeValue(@NotNull HellblockData data, IslandUpgradeType type) {
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

	public double calculateTotalUpgradeValue(@NotNull HellblockData data, IslandUpgradeType type) {
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

	public boolean canUpgrade(int currentTier, IslandUpgradeType upgradeType) {
		int maxTier = getMaxTierFor(upgradeType);

		// If the upgrade isn't defined at all, can't upgrade
		if (maxTier <= 0) {
			return false;
		}

		// If current tier is below max tier, we can still upgrade
		return currentTier < maxTier;
	}

	public Set<IslandUpgradeType> getAvailableUpgrades() {
		return maxTiersByUpgrade.keySet();
	}

	public UpgradeProgress getUpgradeProgress(int currentTier, IslandUpgradeType upgradeType) {
		UpgradeData currentData = getUpgradeData(currentTier, upgradeType);
		UpgradeData nextData = getNextUpgradeData(currentTier, upgradeType);

		int maxTier = getMaxTierFor(upgradeType);
		int nextTier = (nextData != null && currentTier < maxTier) ? currentTier + 1 : -1;

		return new UpgradeProgress(currentTier, currentData, nextTier, nextData);
	}

	public boolean attemptPurchase(HellblockData data, Player player, IslandUpgradeType type) {
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
					.arguments(Component.text(StringUtils.toProperCase(type.toString().replace("_", " ")))).build()));
			return false;
		}

		// Get upgrade data
		final UpgradeData upgradeData = getNextUpgradeData(data.getUpgradeLevel(type), type);
		if (upgradeData == null) {
			owner.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UPGRADE_NO_COST
					.arguments(Component.text(StringUtils.toProperCase(type.toString().replace("_", " ")))).build()));
			return false;
		}

		final List<UpgradeCost> costs = upgradeData.getCosts();
		if (costs == null || costs.isEmpty()) {
			owner.sendMessage(instance.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_UPGRADE_NO_COST
					.arguments(Component.text(StringUtils.toProperCase(type.toString().replace("_", " ")))).build()));
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
		owner.sendMessage(instance.getTranslationManager()
				.render(MessageConstants.MSG_HELLBLOCK_UPGRADE_SUCCESS
						.arguments(Component.text(StringUtils.toProperCase(type.toString().replace("_", " "))),
								Component.text(data.getUpgradeLevel(type)))
						.build()));
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(Key.key("minecraft:entity.player.levelup"), Source.PLAYER, 1.0F, 1.0F));

		// Log
		instance.debug(
				"Island owned by " + player.getName() + " upgraded " + type + " to tier " + data.getUpgradeLevel(type));

		// Animate if protection range
		if (type == IslandUpgradeType.PROTECTION_RANGE) {
			instance.getBorderHandler().animateRangeExpansion(data, oldRange, newRange);
		}
		// Update generator bonus cache
		if (type == IslandUpgradeType.GENERATOR_CHANCE) {
			instance.getNetherrackGeneratorHandler().invalidateGeneratorBonusCache(player.getUniqueId());
			instance.getNetherrackGeneratorHandler().updateGeneratorBonusCache(data);
		}
		// Update barter bonus cache
		if (type == IslandUpgradeType.PIGLIN_BARTERING) {
			instance.getPiglinBarterHandler().invalidateBarterBonusCache(player.getUniqueId());
			instance.getPiglinBarterHandler().updateBarterBonusCache(data);
		}
		// Update crop growth rate cache
		if (type == IslandUpgradeType.CROP_GROWTH) {
			instance.getFarmingManager().invalidateCropGrowthBonusCache(player.getUniqueId());
			instance.getFarmingManager().updateCropGrowthBonusCache(data);
		}
		// Update mob spawn rate cache
		if (type == IslandUpgradeType.MOB_SPAWN_RATE) {
			instance.getMobSpawnHandler().invalidateMobSpawnBonusCache(player.getUniqueId());
			instance.getMobSpawnHandler().updateMobSpawnBonusCache(data);
		}

		return true;
	}
}