package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Predicate;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipeDiscoveryUtil {

	private RecipeDiscoveryUtil() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	private static boolean craftingRecipeSupported = true;
	private static Class<?> craftingRecipeClass = null;
	private static Method getKeyMethod = null;

	private static Method setCategoryMethod = null;
	private static Class<?> craftingBookCategoryClass = null;

	private static void initCraftingRecipeReflection() {
		if (!craftingRecipeSupported)
			return;

		try {
			craftingRecipeClass = Class.forName("org.bukkit.inventory.CraftingRecipe");
			getKeyMethod = craftingRecipeClass.getMethod("getKey");
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			craftingRecipeSupported = false;
		}
	}

	private static void initBookCategoryReflection() {
		try {
			craftingBookCategoryClass = Class.forName("org.bukkit.inventory.recipe.CraftingBookCategory");

			// Just use Object.class for recipe parameter, we'll check instanceof later
			setCategoryMethod = ShapedRecipe.class.getMethod("setCategory", craftingBookCategoryClass);
		} catch (Throwable ignored) {
			craftingBookCategoryClass = null;
			setCategoryMethod = null;
		}
	}

	/**
	 * Attempts to discover a custom crafting recipe for the player.
	 * <p>
	 * This method prioritizes support for Minecraft 1.20+ by reflectively accessing
	 * the {@code CraftingRecipe#getKey()} method. If the {@code CraftingRecipe}
	 * class is not available (e.g., on versions prior to 1.20), it falls back to
	 * explicitly checking for {@code ShapedRecipe} and {@code ShapelessRecipe},
	 * which are available in older versions.
	 * <p>
	 * Only recipes that produce custom-enabled items and contain required item data
	 * (as determined by the provided predicates) are considered for discovery.
	 *
	 * @param player              The player for whom to discover the recipe
	 * @param recipe              The crafting recipe being evaluated (may be null)
	 * @param isCustomItemEnabled Predicate to determine if the resulting item is a
	 *                            custom item
	 * @param hasCustomItemData   Predicate to check if the item has required custom
	 *                            data
	 */
	public static void handleRecipeDiscovery(@NotNull Player player, @Nullable Recipe recipe,
			@NotNull Predicate<ItemStack> isCustomItemEnabled, @NotNull Predicate<ItemStack> hasCustomItemData) {
		if (recipe == null)
			return;

		ItemStack result = recipe.getResult();
		if (!isCustomItemEnabled.test(result) || !hasCustomItemData.test(result))
			return;

		// Lazy init
		if (craftingRecipeClass == null && craftingRecipeSupported) {
			initCraftingRecipeReflection();
		}

		NamespacedKey key = null;

		// Preferred: use CraftingRecipe#getKey() if supported
		if (craftingRecipeSupported && craftingRecipeClass.isInstance(recipe)) {
			try {
				Object keyObj = getKeyMethod.invoke(recipe);
				if (keyObj instanceof NamespacedKey nsKey) {
					key = nsKey;
				}
			} catch (ReflectiveOperationException e) {
				craftingRecipeSupported = false;
				e.printStackTrace(); // Replace with logger
			}
		}

		// Fallback: check Shaped/Shapeless directly
		if (key == null) {
			if (recipe instanceof ShapedRecipe shaped) {
				key = shaped.getKey();
			} else if (recipe instanceof ShapelessRecipe shapeless) {
				key = shapeless.getKey();
			}
		}

		if (key != null && !player.hasDiscoveredRecipe(key)) {
			player.discoverRecipe(key);
		}
	}

	/**
	 * Attempts to set the {@link CraftingBookCategory} on a shaped or shapeless
	 * recipe, using reflection. This method supports versions 1.19.3+ where this
	 * API exists.
	 * <p>
	 * Fails silently on older versions.
	 *
	 * @param recipe       The recipe to modify (ShapedRecipe or ShapelessRecipe).
	 * @param categoryName The name of the category to assign (e.g., "EQUIPMENT",
	 *                     "MISC", "TOOLS").
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void trySetRecipeCategory(@NotNull Recipe recipe, @NotNull String categoryName) {
		if (craftingBookCategoryClass == null || setCategoryMethod == null) {
			initBookCategoryReflection();
		}

		if (craftingBookCategoryClass == null || setCategoryMethod == null)
			return;

		try {
			Object categoryEnum = Enum.valueOf((Class<Enum>) craftingBookCategoryClass,
					categoryName.toUpperCase(Locale.ROOT));

			// Shaped or Shapeless
			if (recipe instanceof ShapedRecipe shaped) {
				setCategoryMethod.invoke(shaped, categoryEnum);
			} else if (recipe instanceof ShapelessRecipe shapeless) {
				// get and cache setCategory method for shapeless too
				Method shapelessSetMethod = ShapelessRecipe.class.getMethod("setCategory", craftingBookCategoryClass);
				shapelessSetMethod.invoke(shapeless, categoryEnum);
			}
		} catch (Throwable ignored) {
			// Fail silently
		}
	}
}