package com.swiftlicious.hellblock.listeners;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.saicone.rtag.RtagItem;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.config.parser.SingleItemParser;
import com.swiftlicious.hellblock.context.Context;
import com.swiftlicious.hellblock.creation.item.CustomItem;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.events.brewing.PlayerBrewEvent;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.nms.inventory.HandSlot;
import com.swiftlicious.hellblock.utils.EventUtils;
import com.swiftlicious.hellblock.utils.PlayerUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.common.ScalarStyle;
import dev.dejvokep.boostedyaml.libs.org.snakeyaml.engine.v2.nodes.Tag;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.utils.format.NodeRole;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class BrewingHandler implements Listener, Reloadable {

	private final HellblockPlugin instance;

	private YamlDocument potionsConfig;

	// Stores all potion configurations
	private final Map<String, List<HellblockBrewingRecipe>> brewingRecipes = new HashMap<>();

	// Tracks if a potion type (Material.POTION) is enabled
	private final Map<Material, BrewingData> brewingMap = new HashMap<>();

	private final Set<String> loggedInvalidRecipes = new HashSet<>();
	private final Map<Location, UUID> brewingOwners = new HashMap<>();

	public BrewingHandler(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	@Override
	public void load() {
		loadPotionsConfig();

		// Register all recipes immediately on startup
		Section potionsSection = getPotionsConfig().getSection("potions");
		if (potionsSection != null) {
			Collection<List<HellblockBrewingRecipe>> recipes = registerNetherPotions(Context.playerEmpty(),
					potionsSection);
			instance.debug("A total of " + recipes.size() + " potion recipe" + (recipes.size() == 1 ? "" : "s")
					+ " have been registered!");
		} else {
			instance.getPluginLogger().warn("Missing 'potions' section in potions.yml");
		}

		Bukkit.getPluginManager().registerEvents(this, this.instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		this.brewingRecipes.clear();
		this.brewingMap.clear();
		this.brewingOwners.clear();
		this.loggedInvalidRecipes.clear();
	}

	@NotNull
	public YamlDocument getPotionsConfig() {
		return this.potionsConfig;
	}

	private void loadPotionsConfig() {
		try (InputStream inputStream = new FileInputStream(
				instance.getConfigManager().resolveConfig("potions.yml").toFile())) {
			potionsConfig = YamlDocument.create(inputStream,
					instance.getConfigManager().getResourceMaybeGz("potions.yml"),
					GeneralSettings.builder().setRouteSeparator('.').setUseDefaults(false).build(),
					LoaderSettings.builder().setAutoUpdate(true).build(),
					DumperSettings.builder().setScalarFormatter((tag, value, role, def) -> {
						if (role == NodeRole.KEY) {
							return ScalarStyle.PLAIN;
						} else {
							return tag == Tag.STR ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN;
						}
					}).build());
			potionsConfig.save(instance.getConfigManager().resolveConfig("potions.yml").toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean isPotionMaterial(@NotNull Material material) {
		return material == Material.POTION || material == Material.SPLASH_POTION
				|| material == Material.LINGERING_POTION;
	}

	@SuppressWarnings("unchecked")
	@NotNull
	private Collection<List<HellblockBrewingRecipe>> registerNetherPotions(@NotNull Context<Player> context,
			@NotNull Section section) {
		instance.debug("Scanning potions.yml for brewing recipes...");

		// Clear previously registered recipes on reload
		brewingRecipes.clear();
		brewingMap.clear();

		List<String> registeredTypes = new ArrayList<>();
		List<String> foundPotionEntries = new ArrayList<>();
		List<String> specialDebugPotions = List.of("lava-bottle"); // can add more if needed

		for (Map.Entry<String, Object> entry : section.getStringRouteMappedValues(false).entrySet()) {
			if (!(entry.getValue() instanceof Section inner)) {
				instance.getPluginLogger().warn("Potion '" + entry.getValue() + "' is not a valid section, skipping.");
				continue;
			}

			String key = entry.getKey();
			foundPotionEntries.add(key);

			boolean isSpecialPotion = specialDebugPotions.contains(key.toLowerCase(Locale.ROOT));

			if (isSpecialPotion) {
				instance.debug("Found special potion entry: " + key);
			}

			String materialName = inner.getString("material");
			if (materialName == null) {
				warnInvalidRecipe("missing 'material'", key);
				continue;
			}

			Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
			if (material == null) {
				warnInvalidRecipe("invalid material " + materialName, key);
				continue;
			}

			boolean enabled = inner.getBoolean("enable", true);
			brewingMap.putIfAbsent(material, new BrewingData(enabled));
			if (!enabled)
				continue;

			CustomItem customItem = new SingleItemParser(key, inner,
					instance.getConfigManager().getItemFormatFunctions()).getItem();
			ItemStack basePotion = customItem.build(context); // always start from base
			ItemStack finalPotion = basePotion.clone(); // allow override later

			List<Map<?, ?>> brewingList = inner.getMapList("brewing");
			if (brewingList == null || brewingList.isEmpty()) {
				warnInvalidRecipe("No 'brewing' recipes found for " + key, key);
				continue;
			}

			List<HellblockBrewingRecipe> recipes = new ArrayList<>();
			for (Map<?, ?> rawMap : brewingList) {
				Map<String, Object> recipeMap = (Map<String, Object>) rawMap;
				String ingredientName = (String) recipeMap.get("ingredient");
				String inputName = (String) recipeMap.get("input");
				String fuelName = (String) recipeMap.getOrDefault("fuel", null);
				String resultPotionName = (String) recipeMap.get("result");
				String inputMaterialName = (String) recipeMap.getOrDefault("inputType", "POTION");
				String outputMaterialName = (String) recipeMap.getOrDefault("outputType", "POTION");

				if (ingredientName == null || inputName == null) {
					warnInvalidRecipe("missing 'ingredient' or 'input'", key);
					continue;
				}

				Material ingredient = Material.matchMaterial(ingredientName.toUpperCase(Locale.ROOT));
				if (ingredient == null) {
					warnInvalidRecipe("invalid ingredient: " + ingredientName, key);
					continue;
				}

				Material fuel = null;
				if (fuelName != null) {
					fuel = Material.matchMaterial(fuelName.toUpperCase(Locale.ROOT));
					if (fuel == null) {
						warnInvalidRecipe("invalid fuel: " + fuelName, key);
						continue;
					}
				}

				Material inputMaterial = Material.matchMaterial(inputMaterialName.toUpperCase(Locale.ROOT));
				if (inputMaterial == null || !isPotionMaterial(inputMaterial)) {
					warnInvalidRecipe("invalid inputType: " + inputMaterialName, key);
					continue;
				}

				Material outputMaterial = Material.matchMaterial(outputMaterialName.toUpperCase(Locale.ROOT));
				if (outputMaterial == null || !isPotionMaterial(outputMaterial)) {
					warnInvalidRecipe("invalid outputType: " + outputMaterialName, key);
					continue;
				}

				PotionType inputType = null;
				String inputTag = null;
				try {
					inputType = PotionType.valueOf(inputName.toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					inputTag = inputName.toUpperCase(Locale.ROOT);
				}

				if (resultPotionName != null) {
					try {
						PotionType resultType = PotionType.valueOf(resultPotionName.toUpperCase(Locale.ROOT));

						finalPotion = new ItemStack(outputMaterial); // e.g., SPLASH_POTION
						Item<ItemStack> resultItem = instance.getItemManager().wrap(finalPotion);
						resultItem.setTag(Key.key("minecraft:potion_contents"),
								Map.of("potion", "minecraft:" + resultType.name().toLowerCase(Locale.ROOT)));

						finalPotion = resultItem.getItem();

						if (isSpecialPotion) {
							instance.debug(() -> "[Brewing] Overriding result potion with type: " + resultType
									+ " for output=" + outputMaterial);
						}
					} catch (IllegalArgumentException e) {
						warnInvalidRecipe("Invalid result potion type: " + resultPotionName, key);
						continue;
					}
				}

				// Inject optional tag overrides from config
				Section tagsSection = inner.getSection("tags");
				if (tagsSection != null) {
					finalPotion = RtagItem.<ItemStack>edit(finalPotion, rtag -> {
						if ("lava-bottle".equalsIgnoreCase(key)) {
							// Only lava-bottle gets this flag
							rtag.set(true, "HellblockRecipe", "isNetherBottle");
							rtag.set("LAVA", "HellblockRecipe", "brewInputTag"); // optional: helps identify the
																					// starting tag
							instance.debug(() -> "[Brewing] Applied NetherBottle tag for lava-bottle potion.");
						}

						if (tagsSection != null) {
							applyTagsFromSection(rtag, tagsSection);
							validateTagSchema(key, tagsSection);
						}
					});
				}

				Integer chainStage = null;
				if (tagsSection != null && tagsSection.contains("HellblockRecipe.chainStage")) {
					Object raw = tagsSection.get("HellblockRecipe.chainStage");
					if (raw instanceof Integer i) {
						chainStage = i;
					} else {
						warnInvalidRecipe("tag 'HellblockRecipe.chainStage' must be an integer", key);
					}
				}

				recipes.add(new HellblockBrewingRecipe(inputType, inputTag, ingredient, fuel, finalPotion,
						inputMaterial, outputMaterial, chainStage));
			}

			registeredTypes.add(key);
			brewingRecipes.put(key, recipes);
		}

		// --- Print grouped debug results ---
		if (!foundPotionEntries.isEmpty()) {
			instance.debug(
					"Found potion entr" + (foundPotionEntries.size() == 1 ? "y" : "ies") + ": " + foundPotionEntries);
		}

		if (!registeredTypes.isEmpty()) {
			instance.debug(
					"Registered potion recipe" + (registeredTypes.size() == 1 ? "" : "s") + ": " + registeredTypes);
		}

		brewingRecipes.entrySet()
				.removeIf(entry -> Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT)) != null
						&& brewingMap.containsKey(Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT)))
						&& !brewingMap.get(Material.matchMaterial(entry.getKey().toUpperCase(Locale.ROOT)))
								.isEnabled());
		return brewingRecipes.values();
	}

	/**
	 * Recursively applies nested BoostedYAML section data to an RtagItem. Supports
	 * primitives, lists, and nested sections.
	 */
	private void applyTagsFromSection(@NotNull RtagItem rtag, @NotNull Section section) {
		for (String route : section.getRoutesAsStrings(true)) {
			Object value = section.get(route);
			if (value == null)
				continue;

			if (section.isSection(route))
				continue; // skip parent nodes

			int split = route.indexOf('.');
			if (split == -1) {
				instance.getPluginLogger().warn("Tag path missing namespace/key structure: '" + route + "'");
				continue;
			}

			String namespace = route.substring(0, split);
			String tagKey = route.substring(split + 1);

			if (value instanceof Boolean b) {
				rtag.set(b, namespace, tagKey);
			} else if (value instanceof Integer i) {
				rtag.set(i, namespace, tagKey);
			} else if (value instanceof Long l) {
				rtag.set(l, namespace, tagKey);
			} else if (value instanceof Double d) {
				rtag.set(d, namespace, tagKey);
			} else if (value instanceof String s) {
				rtag.set(s, namespace, tagKey);
			} else if (value instanceof List<?> list) {
				if (list.isEmpty()) {
					rtag.set(new String[0], namespace, tagKey);
				} else {
					Object first = list.get(0);
					if (first instanceof String) {
						rtag.set(list.toArray(new String[0]), namespace, tagKey);
					} else if (first instanceof Integer) {
						int[] ints = list.stream().mapToInt(o -> (Integer) o).toArray();
						rtag.set(ints, namespace, tagKey);
					} else if (first instanceof Boolean) {
						boolean[] bools = new boolean[list.size()];
						for (int i = 0; i < list.size(); i++) {
							bools[i] = (Boolean) list.get(i);
						}
						rtag.set(bools, namespace, tagKey);
					} else {
						instance.getPluginLogger().warn("Unsupported list type in tag: '" + route + "'");
					}
				}
			} else if (value instanceof Section subSection) {
				Map<String, Object> flattened = new LinkedHashMap<>();
				subSection.getRoutesAsStrings(true).forEach(subKey -> {
					Object val = subSection.get(subKey);
					if (val != null) {
						flattened.put(subKey, val);
					}
				});
				rtag.set(flattened, namespace, tagKey);
			} else {
				instance.getPluginLogger()
						.warn("Unsupported tag value at path '" + route + "': " + value.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Validates a potion tag section before applying it to the item. Enforces
	 * required tags and value types.
	 *
	 * @return true if the tag schema is valid and can be applied
	 */
	private boolean validateTagSchema(@NotNull String keyName, @NotNull Section tagsSection) {
		boolean valid = true;

		// Required tag structure for known features
		if (!tagsSection.contains("HellblockRecipe.isNetherBottle")) {
			warnInvalidRecipe("missing tag: HellblockRecipe.isNetherBottle", keyName);
			valid = false;
		}

		// Enforce tag types
		Object isNetherBottle = tagsSection.get("HellblockRecipe.isNetherBottle");
		if (!(isNetherBottle instanceof Boolean)) {
			warnInvalidRecipe("tag 'HellblockRecipe.isNetherBottle' must be a boolean", keyName);
			valid = false;
		}

		// Optional validation for brewInputTag
		if (tagsSection.contains("HellblockRecipe.brewInputTag")) {
			Object tag = tagsSection.get("HellblockRecipe.brewInputTag");
			if (!(tag instanceof String)) {
				warnInvalidRecipe("tag 'HellblockRecipe.brewInputTag' must be a string", keyName);
				valid = false;
			}
		}

		return valid;
	}

	@EventHandler
	public void onBrew(BrewEvent event) {
		if (!(event.getBlock().getState() instanceof BrewingStand stand)) {
			return;
		}
		if (!instance.getHellblockHandler().isInCorrectWorld(stand.getWorld()))
			return;
		BrewerInventory inv = stand.getInventory();

		ItemStack ingredient = inv.getIngredient();
		if (ingredient == null || ingredient.getType() == Material.AIR)
			return;

		ItemStack fuelItem = inv.getFuel();

		for (int i = 0; i < 3; i++) {
			ItemStack inputPotion = inv.getItem(i);
			if (inputPotion == null || inputPotion.getType() == Material.AIR)
				continue;

			Material inputMaterial = inputPotion.getType();
			if (!isPotionMaterial(inputMaterial))
				continue;

			RtagItem rtag = new RtagItem(inputPotion);

			String customInputTag = rtag.getOptional("HellblockRecipe", "brewInputTag").asString(null);

			PotionType baseType = null;
			try {
				String potionId = rtag.getOptional("minecraft:potion_contents", "potion").asString("WATER");
				baseType = PotionType.valueOf(potionId.toUpperCase(Locale.ROOT));
			} catch (NoSuchFieldError | IllegalArgumentException ignored) {
			}

			HellblockBrewingRecipe match = findMatchingBrewingRecipe(baseType, customInputTag, ingredient.getType(),
					fuelItem, inputMaterial, inputPotion);
			if (match != null) {
				ItemStack result = match.result().clone();
				inv.setItem(i, result);
			}
		}
	}

	@Nullable
	private HellblockBrewingRecipe findMatchingBrewingRecipe(@Nullable PotionType inputType, @Nullable String inputTag,
			@NotNull Material ingredient, @Nullable ItemStack fuel, @NotNull Material inputMaterial,
			@NotNull ItemStack inputPotion) {

		for (List<HellblockBrewingRecipe> recipeList : brewingRecipes.values()) {
			for (HellblockBrewingRecipe recipe : recipeList) {
				if (recipe.ingredient() != ingredient)
					continue;
				if (recipe.inputMaterial() != inputMaterial)
					continue;
				if (recipe.chainStage() != null) {
					int currentStage = new RtagItem(inputPotion).getOptional("HellblockRecipe", "chainStage").asInt(-1);
					if (currentStage >= recipe.chainStage()) {
						instance.debug(() -> "[Brewing] Skipped due to chain stage: " + currentStage + " >= "
								+ recipe.chainStage());
						continue;
					}
				}
				if (recipe.fuel() != null && (fuel == null || fuel.getType() != recipe.fuel()))
					continue;

				if (recipe.inputType() != null && recipe.inputType() == inputType) {
					instance.debug(() -> "[Brewing] Match via PotionType: " + inputType + " + " + ingredient);
					return recipe;
				}

				if (recipe.inputTag() != null && recipe.inputTag().equalsIgnoreCase(inputTag)) {
					instance.debug(() -> "[Brewing] Match via Tag: " + inputTag + " + " + ingredient);
					return recipe;
				}
			}
		}

		instance.debug(() -> "[Brewing] No match found for inputType=" + inputType + ", tag=" + inputTag
				+ ", ingredient=" + ingredient + ", fuel=" + (fuel != null ? fuel.getType() : "NONE") + ", material="
				+ inputMaterial);
		return null;
	}

	@Nullable
	public Item<ItemStack> getLavaBottle() {
		List<HellblockBrewingRecipe> allRecipes = brewingRecipes.get("lava-bottle"); // key in your config
		if (allRecipes == null || allRecipes.isEmpty())
			return null;

		// Return the first result – all recipes for lava-bottle should produce the same
		// item
		ItemStack item = allRecipes.get(0).result();
		if (item == null)
			return null;

		return instance.getItemManager().wrap(item.clone());
	}

	private void giveNetherPotion(@NotNull Player player, int amount, @NotNull EquipmentSlot hand) {
		Item<ItemStack> netherPotionItem = getLavaBottle();
		if (netherPotionItem == null) {
			instance.getPluginLogger().warn("Lava bottle (nether potion) not defined in config.");
			return;
		}

		ItemStack potion = netherPotionItem.getItem().clone();
		potion.setAmount(amount);

		if (player.getInventory().firstEmpty() != -1) {
			PlayerUtils.giveItem(player, potion, amount);
		} else {
			PlayerUtils.dropItem(player, potion, false, true, false);
		}

		VersionHelper.getNMSManager().swingHand(player, hand == EquipmentSlot.HAND ? HandSlot.MAIN : HandSlot.OFF);
		AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
				Sound.sound(Key.key("minecraft:item.bottle.fill"), Source.PLAYER, 1.0f, 1.0f));

		player.updateInventory();
	}

	@EventHandler(ignoreCancelled = true)
	public void onLavaBottle(PlayerInteractEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR))
			return;

		final ItemStack bottle = event.getItem();
		if (bottle == null || bottle.getType() != Material.GLASS_BOTTLE)
			return;

		final Block lavaBlock = findLavaInRange(player, 5);
		if (lavaBlock == null || lavaBlock.getType() != Material.LAVA)
			return;

		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE)
			bottle.setAmount(bottle.getAmount() - 1);

		if (event.getHand() != null) {
			giveNetherPotion(player, 1, event.getHand());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCauldronUpdate(PlayerInteractEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		final ItemStack bottle = event.getItem();
		if (bottle == null || bottle.getType() != Material.GLASS_BOTTLE)
			return;
		if (bottle.getAmount() < 4 && !player.getInventory().containsAtLeast(new ItemStack(Material.GLASS_BOTTLE), 4))
			return;

		final Block clicked = event.getClickedBlock();
		if (clicked == null || clicked.getType() != Material.LAVA_CAULDRON)
			return;

		event.setUseItemInHand(Result.ALLOW);

		if (player.getGameMode() != GameMode.CREATIVE) {
			if (bottle.getAmount() >= 4)
				bottle.setAmount(bottle.getAmount() - 4);
			else
				PlayerUtils.removeItems(player.getInventory(), Material.GLASS_BOTTLE, 4);

		}

		if (event.getHand() != null) {
			giveNetherPotion(player, 4, event.getHand());
		}
		clicked.setType(Material.CAULDRON);
	}

	@Nullable
	private Block findLavaInRange(@NotNull Player player, int range) {
		final BlockIterator iter = new BlockIterator(player, range);
		while (iter.hasNext()) {
			final Block block = iter.next();
			if (block.getType() == Material.LAVA) {
				return block;
			}
		}
		return null;
	}

	@EventHandler(ignoreCancelled = true)
	public void onConsume(PlayerItemConsumeEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		final ItemStack potion = event.getItem();
		if (!checkBrewingData(potion))
			return;

		if (getBrewingData(potion)) {
			player.setFireTicks(RandomUtils.generateRandomInt(100, 320));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerBrew(PlayerBrewEvent event) {
		Player player = getValidPlayer(event.getPlayer());
		if (player == null)
			return;

		event.getResultPotions().stream().filter(Objects::nonNull).filter(pot -> pot.getType() != Material.AIR)
				.forEach(pot -> instance.getStorageManager().getOnlineUser(player.getUniqueId()).ifPresent(userData -> {
					if (instance.getCooldownManager().shouldUpdateActivity(player.getUniqueId(), 5000)) {
						userData.getHellblockData().updateLastIslandActivity();
					}
					instance.getChallengeManager().handleChallengeProgression(userData, ActionType.BREW, pot);
				}));
	}

	@EventHandler
	public void onBrewIngredientPlace(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof BrewerInventory brewerInv)) {
			return;
		}

		// Slot 3 = ingredient slot
		if (event.getSlot() == 3 && event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
			final Location loc = brewerInv.getLocation();

			// If no owner exists, or previous owner was cleared, assign new owner
			brewingOwners.putIfAbsent(loc, event.getWhoClicked().getUniqueId());
		}
	}

	@EventHandler
	public void onPotionTake(InventoryClickEvent event) {
		if (!(event.getInventory() instanceof BrewerInventory brewerInv)) {
			return;
		}

		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		// Potion slots are 0, 1, 2
		if (event.getSlot() >= 0 && event.getSlot() <= 2 && event.getCurrentItem() != null) {
			final UUID owner = brewingOwners.get(brewerInv.getLocation());
			if (owner == null) {
				return;
			}

			if (owner.equals(player.getUniqueId())) {
				final PlayerBrewEvent playerBrewEvent = new PlayerBrewEvent(player, brewerInv.getLocation().getBlock(),
						brewerInv);
				if (EventUtils.fireAndCheckCancel(playerBrewEvent)) {
					event.setCancelled(true);
				}

				// Cleanup after successful claim
				brewingOwners.remove(brewerInv.getLocation());
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		final UUID uuid = event.getPlayer().getUniqueId();
		brewingOwners.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
	}

	@EventHandler
	public void onBrewingStandBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.BREWING_STAND) {
			brewingOwners.remove(event.getBlock().getLocation());
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		Arrays.stream(event.getChunk().getTileEntities()).filter(te -> te.getType() == Material.BREWING_STAND)
				.forEach(te -> brewingOwners.remove(te.getLocation()));
	}

	@Nullable
	private Player getValidPlayer(@NotNull HumanEntity entity) {
		if (!(entity instanceof Player player))
			return null;
		return instance.getHellblockHandler().isInCorrectWorld(player) ? player : null;
	}

	private void warnInvalidRecipe(String reason, String keyName) {
		if (loggedInvalidRecipes.add(keyName)) {
			instance.getPluginLogger().warn("Invalid recipe for " + keyName + ": " + reason);
		}
	}

	private boolean checkBrewingData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).hasTag("HellblockRecipe", "isNetherBottle");
	}

	private boolean getBrewingData(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR
				&& new RtagItem(item).getOptional("HellblockRecipe", "isNetherBottle").asBoolean();
	}

	@Nullable
	private ItemStack setBrewingData(@Nullable ItemStack item, boolean data, @NotNull String potionKey) {
		if (item == null || item.getType() == Material.AIR)
			return null;

		return RtagItem.<ItemStack>edit(item, (Consumer<RtagItem>) tag -> {
			if ("lava-bottle".equalsIgnoreCase(potionKey)) {
				tag.set(data, "HellblockRecipe", "isNetherBottle");
			}
		});
	}

	public boolean isNetherBrewingEnabled(@Nullable ItemStack item) {
		return item != null && item.getType() != Material.AIR && brewingMap.containsKey(item.getType())
				&& brewingMap.get(item.getType()).isEnabled();
	}

	protected class BrewingData {
		private final boolean enabled;

		public BrewingData(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return enabled;
		}
	}

	record HellblockBrewingRecipe(@Nullable PotionType inputType, @Nullable String inputTag,
			@NotNull Material ingredient, @Nullable Material fuel, @NotNull ItemStack result,
			@NotNull Material inputMaterial, @NotNull Material outputMaterial, @Nullable Integer chainStage // NEW FIELD
	) {
	}
}