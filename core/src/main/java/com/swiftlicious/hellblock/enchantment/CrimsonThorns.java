package com.swiftlicious.hellblock.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
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
 * CrimsonThorns manager:
 * <ul>
 * <li>chestplates get PDC 'crimson_thorns' integer level (1..2)</li>
 * <li>melee attackers are set on fire and knocked back when damaging the
 * wearer</li>
 * <li>effects only trigger on the player's own island (Hellblock check)</li>
 * <li>level affects fire duration and knockback force</li>
 * <li>enchanted book obtainable rarely via enchant table and can be applied in
 * an anvil</li>
 * </ul>
 * 
 * No Enchantment subclass required — avoids API compatibility issues.
 */
public class CrimsonThorns implements Listener, Reloadable {

	private final HellblockPlugin plugin;
	private int tableDropChancePercent;
	private final double knockbackPower = 0.5; // configurable if desired

	public CrimsonThorns(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		tableDropChancePercent = plugin.getConfigManager().crimsonThornsData().enchantmentTableChance();
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onMeleeHit(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player victim))
			return;
		if (!(event.getDamager() instanceof LivingEntity attacker))
			return;
		if (attacker instanceof ArmorStand)
			return;

		ItemStack chestplate = victim.getInventory().getChestplate();
		if (chestplate == null || !hasCrimsonThorns(chestplate))
			return;

		int level = getLevelFromChestplate(chestplate);
		if (level <= 0)
			return;

		Location loc = victim.getLocation();
		World world = loc.getWorld();
		if (world == null)
			return;

		plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
			if (islandData == null)
				return;

			boolean isMember = islandData.getPartyPlusOwner().stream()
					.anyMatch(uuid -> uuid.equals(victim.getUniqueId()));

			if (!isMember)
				return;

			plugin.getScheduler().sync().run(() -> {
				// Fire duration (ticks)
				int fireTicks = (level == 1 ? 60 : 100); // 3s or 5s
				attacker.setFireTicks(fireTicks);

				// Knockback
				Vector knockback = attacker.getLocation().toVector().clone().subtract(victim.getLocation().toVector())
						.normalize().multiply(knockbackPower);
				attacker.setVelocity(attacker.getVelocity().clone().add(knockback));

				// Sound
				AdventureHelper.playSound(plugin.getSenderFactory().getAudience(victim),
						Sound.sound(Key.key("minecraft:entity.blaze.hurt"), Sound.Source.PLAYER, 1f, 1.2f));
			});
		});
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		int roll = RandomUtils.generateRandomInt(1, 101);
		if (roll > tableDropChancePercent)
			return;

		int bookLevel = RandomUtils.generateRandomInt(1, 3);

		Player player = event.getEnchanter();
		giveBookTo(player, bookLevel);
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		Inventory inv = event.getInventory();
		if (!(inv instanceof AnvilInventory))
			return;

		ItemStack chestplate = inv.getItem(0);
		ItemStack book = inv.getItem(1);
		if (chestplate == null || book == null)
			return;
		if (!isChestplate(chestplate))
			return;
		if (!isCrimsonThornsBook(chestplate))
			return;

		int level = getLevelFromBook(book);
		ItemStack result = chestplate.clone();
		Item<ItemStack> adjusted = applyLevelToChestplate(result, level);
		if (adjusted == null)
			return;

		event.setResult(adjusted.loadCopy().clone());
	}

	@NotNull
	public Item<ItemStack> createCrimsonThornsBook(int level) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		Item<ItemStack> edited = plugin.getItemManager().wrap(book);

		String rawName = plugin.getConfigManager().crimsonThornsData().bookName().replace("{level}",
				RomanUtils.toRoman(level));
		String nameJson = AdventureHelper.miniMessageToJson(rawName);
		edited.displayName(nameJson);

		List<String> loreJson = new ArrayList<>();
		plugin.getConfigManager().crimsonThornsData().bookLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(level))).map(AdventureHelper::miniMessageToJson)
				.forEach(loreJson::add);
		edited.lore(loreJson);

		edited.setTag(level, "custom", "crimson_thorns_book", "level");
		edited.setTag(true, "minecraft", "enchantment_glint_override");
		return edited;
	}

	private boolean hasCrimsonThorns(@Nullable ItemStack chestplate) {
		if (chestplate == null || chestplate.getType() == Material.AIR)
			return false;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(chestplate);
		return wrapped.getTag("custom", "crimson_thorns_chestplate", "level").isPresent();
	}

	private int getLevelFromChestplate(@Nullable ItemStack chestplate) {
		if (chestplate == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(chestplate);
		return wrapped.getTag("custom", "crimson_thorns_chestplate", "level").map(val -> (Integer) val).orElse(0);
	}

	public int getLevelFromBook(@Nullable ItemStack book) {
		if (book == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(book);
		return wrapped.getTag("custom", "crimson_thorns_book", "level").map(val -> (Integer) val).orElse(0);
	}

	public boolean isCrimsonThornsBook(@Nullable ItemStack book) {
		if (book == null || book.getType() != Material.ENCHANTED_BOOK)
			return false;

		return getLevelFromBook(book) > 0;
	}

	public boolean isChestplate(@Nullable ItemStack chestplate) {
		return chestplate != null && chestplate.getType().name().endsWith("_CHESTPLATE");
	}

	@Nullable
	public Item<ItemStack> applyLevelToChestplate(@Nullable ItemStack chestplate, int level) {
		if (chestplate == null || !isChestplate(chestplate))
			return null;

		Item<ItemStack> edited = plugin.getItemManager().wrap(chestplate);
		int clamped = Math.max(1, Math.min(2, level));
		edited.setTag(clamped, "custom", "crimson_thorns_chestplate", "level");

		List<String> lore = new ArrayList<>(edited.lore().orElseGet(ArrayList::new));
		plugin.getConfigManager().crimsonThornsData().armorAdditionalLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(clamped)))
				.map(AdventureHelper::miniMessageToJson).forEach(lore::add);

		edited.lore(lore);
		edited.setTag(true, "minecraft", "enchantment_glint_override");
		return edited;
	}

	/**
	 * Utility: give a crimson thorns book directly to a player (useful for
	 * commands).
	 */
	public void giveBookTo(@NotNull Player player, int level) {
		ItemStack book = createCrimsonThornsBook(level).load().clone();
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}
}