package com.swiftlicious.hellblock.enchantment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;

/**
 * MagmaWalker manager: - boots get PDC 'magma_walker' integer level (1..2) -
 * boots convert lava -> magma block for a short time while walking - player is
 * immune to magma-block damage when wearing enchanted boots - enchanted book
 * obtainable rarely via enchant table and can be applied in an anvil
 *
 * No Enchantment subclass required — avoids API compatibility issues.
 */
public class MagmaWalker implements Listener, Reloadable {

	private final HellblockPlugin plugin;
	private final Map<Block, Long> replacedBlocks = new ConcurrentHashMap<>();
	private SchedulerTask cleanupTask;

	// Configurable values
	private final long baseRestoreMs = 3000L; // level 1 => 3s, level 2 => 6s
	private final int baseRadius = 2; // radius = baseRadius + level
	private final int tableDropChancePercent = 3; // chance to get a magma book on enchant

	public MagmaWalker(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void load() {
		// Register events
		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Single repeating cleanup that restores expired magma blocks.
		// Use your scheduler style to match other code — pass a valid Location for
		// context (spawn).
		Location anyLoc = LocationUtils.getAnyLocationInstance();
		cleanupTask = plugin.getScheduler().sync().runRepeating(() -> {
			long now = System.currentTimeMillis();
			for (Iterator<Map.Entry<Block, Long>> it = replacedBlocks.entrySet().iterator(); it.hasNext();) {
				Map.Entry<Block, Long> e = it.next();
				Block b = e.getKey();
				long expiry = e.getValue();
				if (expiry <= now) {
					if (b != null && b.getWorld() != null && b.getType() == Material.MAGMA_BLOCK) {
						b.setType(Material.LAVA, false); // false -> no physics
					}
					it.remove();
				}
			}
		}, 20L, 10L, anyLoc);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		if (cleanupTask != null && !cleanupTask.isCancelled()) {
			cleanupTask.cancel();
			cleanupTask = null;
		}

		// restore leftover magma blocks
		new ArrayList<>(replacedBlocks.keySet()).forEach(b -> {
			if (b != null && b.getWorld() != null && b.getType() == Material.MAGMA_BLOCK) {
				b.setType(Material.LAVA, false);
			}
			replacedBlocks.remove(b);
		});
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		// only when crossing block boundary (reduce checks)
		if (event.getFrom().getBlockX() == event.getTo().getBlockX()
				&& event.getFrom().getBlockY() == event.getTo().getBlockY()
				&& event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack boots = player.getInventory().getBoots();
		int level = getLevelFromBoots(boots);
		if (level <= 0)
			return;

		Location loc = event.getTo();
		if (loc == null || loc.getWorld() == null)
			return;

		World world = loc.getWorld();
		int px = loc.getBlockX(), py = loc.getBlockY(), pz = loc.getBlockZ();

		// check if player is on their own island before applying effect
		plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
			if (islandData == null)
				return;

			boolean isMember = islandData.getPartyPlusOwner().stream()
					.anyMatch(uuid -> uuid.equals(player.getUniqueId()));

			if (!isMember)
				return; // not allowed outside their island

			// allowed → convert nearby lava
			// schedule world changes on main thread
			plugin.getScheduler().sync().run(() -> {
				int radius = baseRadius + level;
				long restoreMs = baseRestoreMs * level;

				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (dx * dx + dz * dz > radius * radius)
							continue; // circle check

						Block b = world.getBlockAt(px + dx, py - 1, pz + dz);
						if (b.getType() == Material.LAVA) {
							b.setType(Material.MAGMA_BLOCK, false);
							replacedBlocks.put(b, System.currentTimeMillis() + restoreMs);
						}
					}
				}
			});
		});
	}

	@EventHandler
	public void onMagmaDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;

		if (event.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR)
			return;

		ItemStack boots = player.getInventory().getBoots();
		int level = getLevelFromBoots(boots);
		if (level <= 0)
			return;

		Location loc = player.getLocation();
		World world = loc.getWorld();
		if (world == null)
			return;

		// check if player is on their own island
		plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
			if (islandData == null)
				return;

			boolean isMember = islandData.getPartyPlusOwner().stream()
					.anyMatch(uuid -> uuid.equals(player.getUniqueId()));

			if (isMember) {
				// cancel magma damage only on their own island
				plugin.getScheduler().sync().run(() -> event.setCancelled(true));
			}
		});
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		// small chance to receive a Magma Walker enchanted book when you enchant
		int roll = ThreadLocalRandom.current().nextInt(1, 101);
		if (roll > tableDropChancePercent)
			return;

		int bookLevel = ThreadLocalRandom.current().nextInt(1, 3); // 1..2
		ItemStack book = createMagmaBook(bookLevel).load().clone();

		Player player = event.getEnchanter();
		// try to give to inventory, otherwise drop
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		Inventory inv = event.getInventory();
		if (!(inv instanceof AnvilInventory))
			return;

		ItemStack left = inv.getItem(0);
		ItemStack right = inv.getItem(1);
		if (left == null || right == null)
			return;
		if (!isBoots(left))
			return;
		if (!isMagmaBook(right))
			return;

		int level = getLevelFromBook(right);
		ItemStack result = left.clone();
		Item<ItemStack> adjusted = applyLevelToBoots(result, level);

		event.setResult(adjusted.load().clone());
	}

	/** Create an enchanted book item that represents Magma Walker level. */
	public Item<ItemStack> createMagmaBook(int level) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		Item<ItemStack> editedBook = plugin.getItemManager().wrap(book);

		// Display name from config with placeholder replacement
		String rawName = plugin.getConfigManager().bookName().replace("{level}", toRoman(level));
		String nameJson = AdventureHelper.miniMessageToJson(rawName);
		editedBook.displayName(nameJson);

		// Lore from config with placeholder replacement
		List<String> loreJson = new ArrayList<>();
		plugin.getConfigManager().bookLore().stream().map(line -> line.replace("{level}", toRoman(level)))
				.forEach(parsed -> loreJson.add(AdventureHelper.miniMessageToJson(parsed)));
		editedBook.lore(loreJson);

		// Store custom level
		editedBook.setTag(level, "custom", "magma_book", "level");

		// --- Fake enchant for glow ---
		editedBook.setTag(true, "minecraft", "enchantment_glint_override");

		return editedBook;
	}

	/** Returns level stored on boots (0 if none). */
	private int getLevelFromBoots(ItemStack boots) {
		if (boots == null) {
			return 0;
		}

		final Item<ItemStack> wrapped = plugin.getItemManager().wrap(boots);
		return wrapped.getTag("custom", "magma_boots", "level").map(v -> (Integer) v).orElse(0);
	}

	private int getLevelFromBook(ItemStack book) {
		if (book == null) {
			return 0;
		}

		final Item<ItemStack> wrapped = plugin.getItemManager().wrap(book);
		return wrapped.getTag("custom", "magma_book", "level").map(v -> (Integer) v).orElse(0);
	}

	private boolean isMagmaBook(ItemStack item) {
		return getLevelFromBook(item) > 0 && item.getType() == Material.ENCHANTED_BOOK;
	}

	private boolean isBoots(ItemStack it) {
		if (it == null)
			return false;
		return switch (it.getType()) {
		case NETHERITE_BOOTS, DIAMOND_BOOTS, IRON_BOOTS, CHAINMAIL_BOOTS, LEATHER_BOOTS, GOLDEN_BOOTS -> true;
		default -> false;
		};
	}

	private Item<ItemStack> applyLevelToBoots(@Nullable ItemStack boots, int level) {
		if (boots == null) {
			return plugin.getItemManager().wrap(new ItemStack(Material.AIR));
		}

		Item<ItemStack> editedBoots = plugin.getItemManager().wrap(boots);

		// --- Store enchant level (clamped between 1 and 2) ---
		int clamped = Math.max(1, Math.min(2, level));
		editedBoots.setTag(clamped, "custom", "magma_boots", "level");

		// --- Keep existing lore ---
		List<String> lore = new ArrayList<>(editedBoots.lore().orElse(List.of()));

		// --- Append bootsLore from config ---
		plugin.getConfigManager().bootsLore().stream().map(line -> line.replace("{level}", toRoman(clamped)))
				.forEach(parsed -> lore.add(AdventureHelper.miniMessageToJson(parsed)));

		editedBoots.lore(lore);

		// --- Fake enchant for glow ---
		editedBoots.setTag(true, "minecraft", "enchantment_glint_override");

		return editedBoots;
	}

	/** Utility: give a magma book directly to a player (useful for commands). */
	public void giveBookTo(Player player, int level) {
		ItemStack book = createMagmaBook(level).load().clone();
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}

	private static final String[] ROMAN = { "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };

	private String toRoman(int level) {
		if (level <= 0)
			return "";
		if (level <= ROMAN.length)
			return ROMAN[level - 1];
		return String.valueOf(level); // fallback if > 10
	}
}