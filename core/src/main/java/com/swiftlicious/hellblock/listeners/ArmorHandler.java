package com.swiftlicious.hellblock.listeners;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.HellblockRecipe;
import com.swiftlicious.hellblock.utils.RecipeDiscoveryUtil;
import com.swiftlicious.hellblock.utils.RecipeHelper;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;

public class ArmorHandler implements Listener, Reloadable {

	private final HellblockPlugin instance;

	private final RecipeHelper recipeHelper;

	private YamlDocument armorConfig;

	private final Set<NamespacedKey> registeredRecipes = new HashSet<>();

	private final Map<Material, ArmorData> armorMap = new HashMap<>();
	private final Set<String> loggedInvalidRecipes = new HashSet<>();

	public ArmorHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.recipeHelper = new RecipeHelper();
	}

	@Override
	public void load() {
		loadArmorConfig();

		// Register all recipes immediately on startup
		Section armorSection = getArmorConfig().getSection("armor");
		if (armorSection != null) {
			List<HellblockRecipe> recipes = registerNetherArmor(Context.playerEmpty(), armorSection);
			applyArmorRecipes(recipes);
			instance.debug("A total of " + recipes.size() + " armor recipe" + (recipes.size() == 1 ? "" : "s")
					+ " have been registered!");
		} else {
			instance.getPluginLogger().warn("Missing 'armor' section in armor.yml");
		}

		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.armorMap.clear();
		this.loggedInvalidRecipes.clear();
	}

	@Override
	public void disable() {
		unload();
		registeredRecipes.forEach(Bukkit::removeRecipe);
		registeredRecipes.clear();
	}

	@NotNull
	public YamlDocument getArmorConfig() {
		return this.armorConfig;
	}

	private void loadArmorConfig() {
		try (InputStream inputStream = new FileInputStream(
				instance.getConfigManager().resolveConfig("armor.yml").toFile())) {
			armorConfig = YamlDocument.create(inputStream, instance.getConfigManager().getResourceMaybeGz("armor.yml"),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build());
			armorConfig.save(instance.getConfigManager().resolveConfig("armor.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void applyArmorRecipes(@NotNull List<HellblockRecipe> recipes) {
		List<String> unregisteredTypes = new ArrayList<>();
		List<String> registeredTypes = new ArrayList<>();
		List<String> existingTypes = new ArrayList<>();
		for (HellblockRecipe recipe : recipes) {
			try {
				if (recipe instanceof HellblockRecipe.Disabled disabled) {
					Bukkit.removeRecipe(disabled.key());
					registeredRecipes.remove(disabled.key());
					unregisteredTypes.add(disabled.key().asString());
				} else if (recipe instanceof HellblockRecipe.Enabled enabled) {
					NamespacedKey key = enabled.key();
					if (Bukkit.getRecipe(key) == null) {
						Bukkit.addRecipe(enabled.recipe());
						registeredRecipes.add(key);
						registeredTypes.add(key.asString());
					} else {
						existingTypes.add(key.asString());
					}
				}
			} catch (IllegalStateException ignored) {
			}
		}

		if (!registeredTypes.isEmpty()) {
			instance.debug(
					"Registered armor recipe" + (registeredTypes.size() == 1 ? "" : "s") + ": " + registeredTypes);
		}
		if (!unregisteredTypes.isEmpty()) {
			instance.debug("Removed disabled armor recipe" + (unregisteredTypes.size() == 1 ? "" : "s") + ": "
					+ unregisteredTypes);
		}
		if (!existingTypes.isEmpty()) {
			instance.debug(
					"Skipped existing armor recipe" + (existingTypes.size() == 1 ? "" : "s") + ": " + existingTypes);
		}
	}

	@NotNull
	private List<HellblockRecipe> registerNetherArmor(@NotNull Context<Player> context, @NotNull Section section) {
		List<HellblockRecipe> result = new ArrayList<>();

		instance.debug("Scanning armor.yml for recipes...");

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Section inner)) {
				instance.getPluginLogger().warn("Armor '" + entry.getValue() + "' is not a valid section, skipping.");
				continue;
			}

			instance.debug("Found armor category entry: " + entry.getKey());

			List<String> foundTypes = new ArrayList<>();

			for (ArmorType type : ArmorType.values()) {
				Section typeSection = inner.getSection(type.toString().toLowerCase());
				if (typeSection == null)
					continue;

				foundTypes.add(type.toString().toLowerCase());

				String materialName = typeSection.getString("material");
				if (materialName == null) {
					warnInvalidArmor("missing 'material'", entry.getKey(), type);
					continue;
				}

				Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
				if (material == null) {
					warnInvalidArmor("invalid material: " + materialName, entry.getKey(), type);
					continue;
				}

				boolean enabled = typeSection.getBoolean("enable", true);
				boolean nightVision = typeSection.getBoolean("night-vision", false);
				NamespacedKey key = new NamespacedKey(instance, materialName.toLowerCase());
				armorMap.putIfAbsent(material, new ArmorData(enabled, nightVision));

				if (!enabled) {
					result.add(new HellblockRecipe.Disabled(key));
					continue;
				}

				CustomItem item = new SingleItemParser(entry.getKey() + "_" + type.toString().toLowerCase(),
						typeSection, instance.getConfigManager().getItemFormatFunctions()).getItem();
				ItemStack resultItem = setNightVisionArmorStatus(setArmorData(item.build(context), true), nightVision);
				if (resultItem != null) {
					Map<NamespacedKey, ShapedRecipe> built = buildShiftedArmorRecipes(
							entry.getKey() + "_" + type.toString().toLowerCase(), typeSection, resultItem);
					if (built != null)
						built.forEach((k, v) -> result.add(new HellblockRecipe.Enabled(k, v)));
				} else {
					warnInvalidArmor("invalid armor result: " + item.id(), entry.getKey(), type);
				}
			}

			// After finishing the type loop, print all found types in one line
			if (!foundTypes.isEmpty()) {
				instance.debug("Found armor type value" + (foundTypes.size() == 1 ? "" : "s") + ": " + foundTypes);
			}
		}

		return result;
	}

	@Nullable
	private Map<NamespacedKey, ShapedRecipe> buildShiftedArmorRecipes(@NotNull String keyName, @NotNull Section section,
			@NotNull ItemStack result) {
		List<String> originalShape = section.getStringList("crafting.recipe", new ArrayList<>());
		if (originalShape.size() != 3) {
			warnInvalidRecipe("Invalid recipe shape", keyName);
			return null;
		}

		Section ingredientSection = section.getSection("crafting.ingredients");
		if (ingredientSection == null) {
			warnInvalidRecipe("Missing ingredients section", keyName);
			return null;
		}

		Map<ItemStack, Character> ingredients = instance.getConfigManager().getCraftingIngredients(ingredientSection);
		List<String> trimmed = recipeHelper.trimShape(originalShape);
		List<String[][]> variants = recipeHelper.generateShiftedShapes(trimmed);

		Map<NamespacedKey, ShapedRecipe> recipes = new HashMap<>();
		int variantIndex = 0;

		for (String[][] shapeGrid : variants) {
			String variantSuffix = variantIndex > 0 ? "_" + variantIndex : "";
			NamespacedKey variantKey = new NamespacedKey(instance, keyName.toLowerCase(Locale.ROOT) + variantSuffix);
			ShapedRecipe recipe = new ShapedRecipe(variantKey, result);
			RecipeDiscoveryUtil.trySetRecipeCategory(recipe, "EQUIPMENT");

			String[] shapeLines = Arrays.stream(shapeGrid).map(row -> String.join("", row)).toArray(String[]::new);
			recipe.shape(shapeLines);

			for (Map.Entry<ItemStack, Character> entry : ingredients.entrySet()) {
				if (entry.getKey() == null || entry.getKey().getType().isAir())
					continue;
				recipe.setIngredient(entry.getValue(), new RecipeChoice.MaterialChoice(entry.getKey().getType()));
			}

			recipes.put(variantKey, recipe);
		}

		return recipes;
	}

	@EventHandler(ignoreCancelled = true)
	public void onCrafting(CraftItemEvent event) {
		Player player = getValidPlayer(event.getView().getPlayer());
		if (player == null)
			return;

		RecipeDiscoveryUtil.handleRecipeDiscovery(player, event.getRecipe(), this::isNetherArmorEnabled,
				this::getArmorData);

		ItemStack result = event.getRecipe().getResult().clone();
		instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
			if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
				userData.getHellblockData().updateLastIslandActivity();
			}
			instance.getChallengeManager().handleChallengeProgression(userData, ActionType.CRAFT, result,
					result.getAmount());
		});
	}

	@EventHandler
	public void onLimitedCrafting(PrepareItemCraftEvent event) {
		Player player = getValidPlayer(event.getView().getPlayer());
		if (player == null || !player.getWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING))
			return;

		RecipeDiscoveryUtil.handleRecipeDiscovery(player, event.getRecipe(), this::isNetherArmorEnabled,
				this::getArmorData);
	}

	@EventHandler
	public void onArmorChange(InventoryCloseEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		instance.getStorageManager().getOnlineUser(player.getUniqueId())
				.ifPresent(data -> handleArmorEffect(player, data));
	}

	@EventHandler(ignoreCancelled = true)
	public void onArmorEquip(PlayerInteractEvent event) {
		if (event.useItemInHand() == Event.Result.DENY)
			return;

		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		instance.getStorageManager().getOnlineUser(player.getUniqueId())
				.ifPresent(data -> handleArmorEffect(player, data));
	}

	@EventHandler
	public void onArmorBreak(PlayerItemBreakEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		instance.getStorageManager().getOnlineUser(player.getUniqueId())
				.ifPresent(data -> handleArmorEffect(player, data));
	}

	@EventHandler(ignoreCancelled = true)
	public void onDispenseArmor(BlockDispenseArmorEvent event) {
		if (!(event.getTargetEntity() instanceof Player target))
			return;
		Player player = getValidPlayer(target);
		if (player == null)
			return;

		instance.getStorageManager().getOnlineUser(player.getUniqueId())
				.ifPresent(data -> handleArmorEffect(player, data));
	}

	private void handleArmorEffect(@NotNull Player player, @NotNull UserData userData) {
		boolean hasGlowArmor = Arrays.stream(player.getInventory().getArmorContents()).filter(Objects::nonNull)
				.filter(item -> item.getType() != Material.AIR).anyMatch(item -> isNetherArmorEnabled(item)
						&& isNetherArmorNightVisionAllowed(item) && getNightVisionArmorStatus(item));

		if (hasGlowArmor) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1));
			userData.isWearingGlowstoneArmor(true);
		} else if (userData.hasGlowstoneArmorEffect() && player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
			userData.isWearingGlowstoneArmor(false);
			if (!userData.hasGlowstoneToolEffect()) {
				player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			}
		}
	}

	@Nullable
	private Player getValidPlayer(@NotNull HumanEntity entity) {
		if (!(entity instanceof Player player))
			return null;
		return instance.getHellblockHandler().isInCorrectWorld(player) ? player : null;
	}

	private void warnInvalidArmor(@NotNull String reason, @NotNull String key, @NotNull ArmorType type) {
		instance.getPluginLogger().warn("Armor entry " + key + " (" + type + "): " + reason);
	}

	private void warnInvalidRecipe(@NotNull String reason, @NotNull String keyName) {
		if (loggedInvalidRecipes.add(keyName)) {
			instance.getPluginLogger().warn("Invalid recipe for " + keyName + ": " + reason);
		}
	}

	public boolean checkArmorData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).hasTag("HellblockRecipe", "isNetherArmor");
	}

	public boolean getArmorData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).getOptional("HellblockRecipe", "isNetherArmor").asBoolean();
	}

	@Nullable
	public ItemStack setArmorData(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.<ItemStack>edit(item,
				(Consumer<RtagItem>) tag -> tag.set(data, "HellblockRecipe", "isNetherArmor"));
	}

	public boolean checkNightVisionArmorStatus(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).hasTag("HellblockRecipe", "wearsNightVision");
	}

	public boolean getNightVisionArmorStatus(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).getOptional("HellblockRecipe", "wearsNightVision").asBoolean();
	}

	@Nullable
	public ItemStack setNightVisionArmorStatus(@Nullable ItemStack item, boolean data) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.<ItemStack>edit(item,
				(Consumer<RtagItem>) tag -> tag.set(data, "HellblockRecipe", "wearsNightVision"));
	}

	public boolean isNetherArmorEnabled(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR && armorMap.containsKey(item.getType())
				&& armorMap.get(item.getType()).isEnabled();
	}

	public boolean isNetherArmorNightVisionAllowed(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR && armorMap.containsKey(item.getType())
				&& armorMap.get(item.getType()).hasGlowstoneAbility();
	}

	protected class ArmorData {
		private final boolean enabled;
		private final boolean glowstone;

		public ArmorData(boolean enabled, boolean glowstone) {
			this.enabled = enabled;
			this.glowstone = glowstone;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean hasGlowstoneAbility() {
			return glowstone;
		}
	}

	protected enum ArmorType {
		HELMET, CHESTPLATE, LEGGINGS, BOOTS;
	}
}