package com.swiftlicious.hellblock.enchantment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Pair;

/**
 * MagmaWalker manager:
 * <ul>
 * <li>boots get PDC 'magma_walker' integer level (1..2)</li>
 * <li>boots convert lava -> magma block for a short time while walking</li>
 * <li>player is immune to magma-block damage when wearing enchanted boots</li>
 * <li>enchanted book obtainable rarely via enchant table and can be applied in
 * an anvil</li>
 * </ul>
 * 
 * No Enchantment subclass required — avoids API compatibility issues.
 */
public class MagmaWalker implements Listener, Reloadable {

	private final HellblockPlugin plugin;
	private final Map<Block, Pair<Long, BlockData>> replacedBlocks = new ConcurrentHashMap<>();
	private SchedulerTask cleanupTask;

	// Configurable values
	private final long baseRestoreMs = 3000L; // level 1 => 3s, level 2 => 6s
	private final int baseRadius = 2; // radius = baseRadius + level
	private int tableDropChancePercent; // chance to get a magma book on enchant

	public MagmaWalker(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void load() {
		// Register events
		Bukkit.getPluginManager().registerEvents(this, plugin);
		tableDropChancePercent = plugin.getConfigManager().magmaWalkerData().enchantmentTableChance();

		// Single repeating cleanup that restores expired magma blocks.
		// Use your scheduler style to match other code — pass a valid Location for
		// context (spawn).
		Location anyLoc = LocationUtils.getAnyLocationInstance();
		cleanupTask = plugin.getScheduler().sync().runRepeating(() -> {
			long now = System.currentTimeMillis();
			for (Iterator<Map.Entry<Block, Pair<Long, BlockData>>> it = replacedBlocks.entrySet().iterator(); it
					.hasNext();) {
				Map.Entry<Block, Pair<Long, BlockData>> e = it.next();
				Block b = e.getKey();
				long expiry = e.getValue().left();
				BlockData originalData = e.getValue().right();

				if (expiry <= now) {
					if (b != null && b.getType() == Material.MAGMA_BLOCK) {
						b.setType(Material.LAVA, false); // false -> no physics
						b.setBlockData(originalData, false);
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

		// Restore all remaining magma blocks to their original lava states
		for (Map.Entry<Block, Pair<Long, BlockData>> entry : replacedBlocks.entrySet()) {
			Block b = entry.getKey();
			BlockData originalData = entry.getValue().right();

			if (b != null && b.getType() == Material.MAGMA_BLOCK) {
				b.setType(Material.LAVA, false);
				b.setBlockData(originalData, false);
			}
		}

		// Clear map once done to release references
		replacedBlocks.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		// only when crossing block boundary (reduce checks)
		if (event.getFrom().getBlockX() == event.getTo().getBlockX()
				&& event.getFrom().getBlockY() == event.getTo().getBlockY()
				&& event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack boots = player.getInventory().getBoots();
		if (boots == null || !hasMagmaWalker(boots))
			return;

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
							BlockData originalData = b.getBlockData().clone();
							b.setType(Material.MAGMA_BLOCK, false);
							replacedBlocks.put(b, Pair.of(System.currentTimeMillis() + restoreMs, originalData));
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
		if (boots == null || !hasMagmaWalker(boots))
			return;

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
		int roll = RandomUtils.generateRandomInt(1, 101);
		if (roll > tableDropChancePercent)
			return;

		int bookLevel = RandomUtils.generateRandomInt(1, 3); // 1..2

		Player player = event.getEnchanter();
		giveBookTo(player, bookLevel);
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		Inventory inv = event.getInventory();
		if (!(inv instanceof AnvilInventory))
			return;

		ItemStack boots = inv.getItem(0);
		ItemStack book = inv.getItem(1);
		if (boots == null || book == null)
			return;
		if (!isBoots(boots))
			return;
		if (!isMagmaBook(book))
			return;

		int level = getLevelFromBook(book);
		ItemStack result = boots.clone();
		Item<ItemStack> adjusted = applyLevelToBoots(result, level);
		if (adjusted == null)
			return;

		event.setResult(adjusted.loadCopy().clone());
	}

	/** Create an enchanted book item that represents Magma Walker level. */
	@NotNull
	public Item<ItemStack> createMagmaWalkerBook(int level) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		Item<ItemStack> editedBook = plugin.getItemManager().wrap(book);

		String rawName = plugin.getConfigManager().magmaWalkerData().bookName().replace("{level}",
				RomanUtils.toRoman(level));
		String nameJson = AdventureHelper.miniMessageToJson(rawName);
		editedBook.displayName(nameJson);

		List<String> loreJson = new ArrayList<>();
		plugin.getConfigManager().magmaWalkerData().bookLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(level))).map(AdventureHelper::miniMessageToJson)
				.forEach(loreJson::add);
		editedBook.lore(loreJson);

		editedBook.setTag(level, "custom", "magma_walker_book", "level");
		editedBook.setTag(true, "minecraft", "enchantment_glint_override");
		return editedBook;
	}

	private boolean hasMagmaWalker(@Nullable ItemStack boots) {
		if (boots == null || boots.getType() == Material.AIR)
			return false;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(boots);
		return wrapped.getTag("custom", "magma_walker_boots", "level").isPresent();
	}

	/** Returns level stored on boots (0 if none). */
	private int getLevelFromBoots(@Nullable ItemStack boots) {
		if (boots == null)
			return 0;

		final Item<ItemStack> wrapped = plugin.getItemManager().wrap(boots);
		return wrapped.getTag("custom", "magma_walker_boots", "level").map(v -> (Integer) v).orElse(0);
	}

	private int getLevelFromBook(@Nullable ItemStack book) {
		if (book == null)
			return 0;

		final Item<ItemStack> wrapped = plugin.getItemManager().wrap(book);
		return wrapped.getTag("custom", "magma_walker_book", "level").map(v -> (Integer) v).orElse(0);
	}

	private boolean isMagmaBook(@Nullable ItemStack book) {
		if (book == null || book.getType() != Material.ENCHANTED_BOOK)
			return false;

		return getLevelFromBook(book) > 0;
	}

	public boolean isBoots(@Nullable ItemStack boots) {
		return boots != null && boots.getType().name().endsWith("_BOOTS");
	}

	@Nullable
	private Item<ItemStack> applyLevelToBoots(@Nullable ItemStack boots, int level) {
		if (boots == null || !isBoots(boots)) {
			return null;
		}

		Item<ItemStack> editedBoots = plugin.getItemManager().wrap(boots);
		int clamped = Math.max(1, Math.min(2, level));
		editedBoots.setTag(clamped, "custom", "magma_walker_boots", "level");

		List<String> lore = new ArrayList<>(editedBoots.lore().orElseGet(ArrayList::new));
		plugin.getConfigManager().magmaWalkerData().armorAdditionalLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(clamped)))
				.map(AdventureHelper::miniMessageToJson).forEach(lore::add);

		editedBoots.lore(lore);
		editedBoots.setTag(true, "minecraft", "enchantment_glint_override");
		return editedBoots;
	}

	/**
	 * Utility: give a magma walker book directly to a player (useful for commands).
	 */
	public void giveBookTo(@NotNull Player player, int level) {
		ItemStack book = createMagmaWalkerBook(level).load().clone();
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}
}