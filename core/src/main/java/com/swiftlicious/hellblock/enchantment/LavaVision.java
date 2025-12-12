package com.swiftlicious.hellblock.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * LavaVision manager:
 * <ul>
 * <li>Helmets get PDC 'lava_vision' integer level (1..2)</li>
 * <li>While in lava, players gain Night Vision and Fire Resistance for a short
 * duration</li>
 * <li>Only activates on the player's own Hellblock island</li>
 * <li>Enchanted book obtainable rarely via enchant table and applied via
 * anvil</li>
 * </ul>
 * 
 * No Enchantment subclass used — avoids API compatibility issues.
 */
public class LavaVision implements Listener, Reloadable {

	private final HellblockPlugin plugin;
	private int tableDropChancePercent;

	public LavaVision(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		tableDropChancePercent = plugin.getConfigManager().lavaVisionData().enchantmentTableChance();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerTick(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		ItemStack helmet = player.getInventory().getHelmet();
		if (helmet == null || !hasLavaVision(helmet))
			return;

		int level = getLevelFromHelmet(helmet);
		if (level <= 0)
			return;

		Location loc = player.getLocation();
		World world = loc.getWorld();
		if (world == null)
			return;

		// Must be fully submerged: block at feet AND at eye level must be lava
		Block feetBlock = loc.getBlock();
		Block eyeBlock = player.getEyeLocation().getBlock();

		if (feetBlock.getType() != Material.LAVA || eyeBlock.getType() != Material.LAVA)
			return;

		plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
			if (islandData == null
					|| islandData.getPartyPlusOwner().stream().noneMatch(id -> id.equals(player.getUniqueId())))
				return;

			plugin.getScheduler().sync().run(() -> {
				int durationTicks = 60 * level; // level 1 = 3s, level 2 = 6s

				boolean gaveEffect = false;

				// Only give effect if not already active or almost expired
				if (player.getPotionEffect(PotionEffectType.NIGHT_VISION) == null
						|| player.getPotionEffect(PotionEffectType.NIGHT_VISION).getDuration() < 20) {
					player.addPotionEffect(
							new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, false, false));
					gaveEffect = true;
				}

				if (player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE) == null
						|| player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE).getDuration() < 20) {
					player.addPotionEffect(
							new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, false, false));
					gaveEffect = true;
				}

				if (gaveEffect) {
					// Play a subtle sound when effect is applied
					AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player),
							Sound.sound(Key.key("minecraft:item.firecharge.use"), Sound.Source.PLAYER, 0.8f, 1.2f));
				}
			});
		});
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		int roll = RandomUtils.generateRandomInt(1, 101);
		if (roll > tableDropChancePercent)
			return;

		int bookLevel = RandomUtils.generateRandomInt(1, 3); // 1 or 2

		Player player = event.getEnchanter();
		giveBookTo(player, bookLevel);
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		Inventory inv = event.getInventory();
		if (!(inv instanceof AnvilInventory))
			return;

		ItemStack helmet = inv.getItem(0);
		ItemStack book = inv.getItem(1);
		if (helmet == null || book == null)
			return;
		if (!isHelmet(helmet))
			return;
		if (!isLavaVisionBook(book))
			return;

		int level = getLevelFromBook(book);
		ItemStack result = helmet.clone();
		Item<ItemStack> adjusted = applyLevelToHelmet(result, level);
		if (adjusted == null)
			return;

		event.setResult(adjusted.loadCopy().clone());
	}

	@NotNull
	public Item<ItemStack> createLavaVisionBook(int level) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		Item<ItemStack> edited = plugin.getItemManager().wrap(book);

		String rawName = plugin.getConfigManager().lavaVisionData().bookName().replace("{level}",
				RomanUtils.toRoman(level));
		String nameJson = AdventureHelper.miniMessageToJson(rawName);
		edited.displayName(nameJson);

		List<String> loreJson = new ArrayList<>();
		plugin.getConfigManager().lavaVisionData().bookLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(level))).map(AdventureHelper::miniMessageToJson)
				.forEach(loreJson::add);
		edited.lore(loreJson);

		edited.setTag(level, "custom", "lava_vision_book", "level");
		edited.setTag(true, "minecraft", "enchantment_glint_override");

		return edited;
	}

	private boolean hasLavaVision(@Nullable ItemStack helmet) {
		if (helmet == null || helmet.getType() == Material.AIR)
			return false;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(helmet);
		return wrapped.getTag("custom", "lava_vision_helmet", "level").isPresent();
	}

	private int getLevelFromHelmet(@Nullable ItemStack helmet) {
		if (helmet == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(helmet);
		return wrapped.getTag("custom", "lava_vision_helmet", "level").map(val -> (Integer) val).orElse(0);
	}

	private int getLevelFromBook(@Nullable ItemStack book) {
		if (book == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(book);
		return wrapped.getTag("custom", "lava_vision_book", "level").map(val -> (Integer) val).orElse(0);
	}

	private boolean isLavaVisionBook(@Nullable ItemStack book) {
		if (book == null || book.getType() != Material.ENCHANTED_BOOK)
			return false;

		return getLevelFromBook(book) > 0;
	}

	public boolean isHelmet(@Nullable ItemStack helmet) {
		return helmet != null && helmet.getType().name().endsWith("_HELMET");
	}

	@Nullable
	public Item<ItemStack> applyLevelToHelmet(@Nullable ItemStack helmet, int level) {
		if (helmet == null || !isHelmet(helmet))
			return null;

		Item<ItemStack> edited = plugin.getItemManager().wrap(helmet);
		int clamped = Math.max(1, Math.min(2, level));
		edited.setTag(clamped, "custom", "lava_vision_helmet", "level");

		List<String> lore = new ArrayList<>(edited.lore().orElseGet(ArrayList::new));
		plugin.getConfigManager().lavaVisionData().armorAdditionalLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(clamped)))
				.map(AdventureHelper::miniMessageToJson).forEach(lore::add);

		edited.lore(lore);
		edited.setTag(true, "minecraft", "enchantment_glint_override");
		return edited;
	}

	/**
	 * Utility: give a lava vison book directly to a player (useful for commands).
	 */
	public void giveBookTo(@NotNull Player player, int level) {
		ItemStack book = createLavaVisionBook(level).load().clone();
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}
}