package com.swiftlicious.hellblock.enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
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
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.LocationUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;

/**
 * MoltenCore manager:
 * <ul>
 * <li>leggings get PDC 'molten_core' integer level (1..2)</li>
 * <li>mined ores (iron, gold, clay, sand) auto-smelt when broken (on own
 * island)</li>
 * <li>nearby furnaces/blast furnaces smelt faster when the wearer is
 * nearby</li>
 * <li>items dropped in lava are passively smelted into their cooked
 * version</li>
 * <li>level increases furnace boost range and smelt speed</li>
 * <li>enchanted book obtainable rarely via enchant table and can be applied in
 * an anvil</li>
 * </ul>
 *
 * No Enchantment subclass required — avoids API compatibility issues.
 */
public class MoltenCore implements Listener, Reloadable {

	private final HellblockPlugin plugin;
	private final Set<Material> smeltables = Set.of(Material.IRON_ORE, Material.RAW_IRON, Material.GOLD_ORE,
			Material.RAW_GOLD, Material.SAND, Material.CLAY);

	private SchedulerTask furnaceBoostTask;

	private int tableDropChancePercent; // % chance from enchant table

	public MoltenCore(@NotNull HellblockPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		tableDropChancePercent = plugin.getConfigManager().moltenCoreData().enchantmentTableChance();

		furnaceBoostTask = plugin.getScheduler().sync().runRepeating(() -> {
			for (UserData data : plugin.getStorageManager().getOnlineUsers()) {
				Player player = data.getPlayer();
				if (player == null || !player.isOnline())
					continue;
				ItemStack leggings = player.getInventory().getLeggings();
				if (leggings == null || !hasMoltenCore(leggings))
					continue;

				Location loc = player.getLocation();
				World world = loc.getWorld();
				if (world == null)
					continue;

				plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
					if (islandData == null
							|| islandData.getPartyPlusOwner().stream().noneMatch(id -> id.equals(player.getUniqueId())))
						return;

					// On their own island, proceed
					int px = loc.getBlockX();
					int py = loc.getBlockY();
					int pz = loc.getBlockZ();

					int level = getLevelFromLeggings(leggings);
					int radius = Math.max(1, level + 3); // Level 1 = 4 blocks, Level 2 = 5 blocks

					for (int dx = -radius; dx <= radius; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -radius; dz <= radius; dz++) {
								Block b = world.getBlockAt(px + dx, py + dy, pz + dz);
								BlockState state = b.getState();

								if (state instanceof Furnace furnace) {
									// Only boost if it's burning
									if (furnace.getBurnTime() > 0) {
										int cookTime = furnace.getCookTime();
										int totalTime = furnace.getCookTimeTotal();

										// Speed up cooking (level-based bonus)
										int boost = Math.max(1, level);
										cookTime += boost;
										if (cookTime > totalTime)
											cookTime = totalTime;

										boolean finished = cookTime < totalTime && (cookTime + boost) >= totalTime;
										cookTime = Math.min(cookTime + boost, totalTime);
										furnace.setCookTime((short) cookTime);
										furnace.update();

										if (finished) {
											AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player),
													Sound.sound(Key.key("minecraft:block.smoker.smoke"),
															Sound.Source.BLOCK, 1f, 1.2f));
										}
									}
								} else if (state instanceof Campfire campfire) {
									for (int i = 0; i < 4; i++) {
										ItemStack cooking = campfire.getItem(i);
										if (cooking == null || cooking.getType() == Material.AIR)
											continue;

										int cookTime = campfire.getCookTime(i);
										int totalTime = campfire.getCookTimeTotal(i);

										if (cookTime < totalTime) {
											int boost = Math.max(1, level);
											boolean finished = cookTime < totalTime && (cookTime + boost) >= totalTime;
											campfire.setCookTime(i, Math.min(cookTime + boost, totalTime));

											if (finished) {
												AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player),
														Sound.sound(Key.key("minecraft:block.campfire.crackle"),
																Sound.Source.BLOCK, 1f, 1.5f));
											}
										}
									}
									campfire.update();
								}
							}
						}
					}

					// Lava aura smelting
					plugin.getScheduler().sync().run(() -> {
						int auraRadius = Math.max(2, level + 2); // e.g., Level 1 = 3, Level 2 = 4

						for (Entity nearby : player.getNearbyEntities(auraRadius, 2, auraRadius)) {
							if (!(nearby instanceof org.bukkit.entity.Item itemEntity))
								continue;

							ItemStack stack = itemEntity.getItemStack();
							Material input = stack.getType();
							Material result = getSmeltedVersion(input);
							if (result == null)
								continue;

							// Check if touching lava block
							Location itemLoc = itemEntity.getLocation();
							Block block = itemLoc.getBlock();
							if (block.getType() != Material.LAVA) {
								Block below = block.getRelative(BlockFace.DOWN);
								if (below.getType() != Material.LAVA)
									continue;
							}

							// Smelt it
							int amount = stack.getAmount();
							// Remove the original item
							itemEntity.remove();

							// Drop new smelted item just above the lava surface
							Location dropLoc = itemLoc.clone().add(0, 0.5, 0);
							ItemStack output = new ItemStack(result, amount);
							org.bukkit.entity.Item newItem = world.dropItem(dropLoc, output);

							// Calculate directional velocity toward player
							Vector toPlayer = player.getLocation().toVector().clone().subtract(dropLoc.toVector())
									.normalize();
							Vector velocity = toPlayer.multiply(0.3).setY(0.3); // XZ: direction, Y: upward

							newItem.setVelocity(velocity);

							// Feedback
							world.spawnParticle(Particle.LAVA, dropLoc, 8, 0.2, 0.1, 0.2, 0.01);
							AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player),
									Sound.sound(Key.key("minecraft:block.lava.pop"), Sound.Source.PLAYER, 1f, 1.25f));
						}
					});
				});
			}
		}, 40L, 40L, LocationUtils.getAnyLocationInstance()); // every 2 seconds
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		if (furnaceBoostTask != null && !furnaceBoostTask.isCancelled()) {
			furnaceBoostTask.cancel();
			furnaceBoostTask = null;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack leggings = player.getInventory().getLeggings();
		if (leggings == null || !hasMoltenCore(leggings))
			return;

		int level = getLevelFromLeggings(leggings);
		if (level <= 0)
			return;

		Block block = event.getBlock();
		Material type = block.getType();
		if (!smeltables.contains(type))
			return;

		Location loc = block.getLocation();
		World world = loc.getWorld();
		if (world == null)
			return;

		plugin.getHellblockHandler().getHellblockByWorld(world, loc).thenAccept(islandData -> {
			if (islandData == null)
				return;

			boolean isMember = islandData.getPartyPlusOwner().stream()
					.anyMatch(uuid -> uuid.equals(player.getUniqueId()));

			if (!isMember)
				return;

			Material result = getSmeltedVersion(type);
			if (result == null)
				return;

			plugin.getScheduler().sync().run(() -> {
				event.setDropItems(false);
				block.setType(Material.AIR, false);
				world.dropItemNaturally(loc, new ItemStack(result));
				AdventureHelper.playSound(plugin.getSenderFactory().getAudience(player), Sound
						.sound(Key.key("minecraft:block.blastfurnace.fire_crackle"), Sound.Source.PLAYER, 0.8f, 1.3f));
				world.spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.01);
			});
		});
	}

	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		int roll = RandomUtils.generateRandomInt(1, 101);
		if (roll > tableDropChancePercent)
			return;

		int bookLevel = RandomUtils.generateRandomInt(1, 3); // 1–2

		Player player = event.getEnchanter();
		giveBookTo(player, bookLevel);
	}

	@EventHandler
	public void onPrepareAnvil(PrepareAnvilEvent event) {
		Inventory inv = event.getInventory();
		if (!(inv instanceof AnvilInventory))
			return;

		ItemStack leggings = inv.getItem(0);
		ItemStack book = inv.getItem(1);
		if (leggings == null || book == null)
			return;
		if (!isLeggings(leggings))
			return;
		if (!isMoltenCoreBook(leggings))
			return;

		int level = getLevelFromBook(book);
		ItemStack result = leggings.clone();
		Item<ItemStack> adjusted = applyLevelToLeggings(result, level);
		if (adjusted == null)
			return;

		event.setResult(adjusted.loadCopy().clone());
	}

	/** Create an enchanted book item that represents Molten Core level. */
	@NotNull
	public Item<ItemStack> createMoltenCoreBook(int level) {
		ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
		Item<ItemStack> edited = plugin.getItemManager().wrap(book);

		String rawName = plugin.getConfigManager().moltenCoreData().bookName().replace("{level}",
				RomanUtils.toRoman(level));
		String nameJson = AdventureHelper.miniMessageToJson(rawName);
		edited.displayName(nameJson);

		List<String> loreJson = new ArrayList<>();
		plugin.getConfigManager().moltenCoreData().bookLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(level))).map(AdventureHelper::miniMessageToJson)
				.forEach(loreJson::add);
		edited.lore(loreJson);

		edited.setTag(level, "custom", "molten_core_book", "level");
		edited.setTag(true, "minecraft", "enchantment_glint_override");
		return edited;
	}

	private boolean hasMoltenCore(@Nullable ItemStack leggings) {
		if (leggings == null || leggings.getType() == Material.AIR)
			return false;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(leggings);
		return wrapped.getTag("custom", "molten_core_leggings", "level").isPresent();
	}

	private int getLevelFromLeggings(@Nullable ItemStack leggings) {
		if (leggings == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(leggings);
		return wrapped.getTag("custom", "molten_core_leggings", "level").map(val -> (Integer) val).orElse(0);
	}

	public int getLevelFromBook(@Nullable ItemStack book) {
		if (book == null)
			return 0;

		Item<ItemStack> wrapped = plugin.getItemManager().wrap(book);
		return wrapped.getTag("custom", "molten_core_book", "level").map(val -> (Integer) val).orElse(0);
	}

	public boolean isMoltenCoreBook(@Nullable ItemStack book) {
		if (book == null || book.getType() != Material.ENCHANTED_BOOK)
			return false;

		return getLevelFromBook(book) > 0;
	}

	public boolean isLeggings(@Nullable ItemStack leggings) {
		return leggings != null && leggings.getType().name().endsWith("_LEGGINGS");
	}

	@Nullable
	public Item<ItemStack> applyLevelToLeggings(@Nullable ItemStack leggings, int level) {
		if (leggings == null || !isLeggings(leggings))
			return null;

		Item<ItemStack> edited = plugin.getItemManager().wrap(leggings);
		int clamped = Math.max(1, Math.min(2, level));
		edited.setTag(clamped, "custom", "molten_core_leggings", "level");

		List<String> lore = new ArrayList<>(edited.lore().orElseGet(ArrayList::new));
		plugin.getConfigManager().moltenCoreData().armorAdditionalLore().stream()
				.map(line -> line.replace("{level}", RomanUtils.toRoman(clamped)))
				.map(AdventureHelper::miniMessageToJson).forEach(lore::add);

		edited.lore(lore);
		edited.setTag(true, "minecraft", "enchantment_glint_override");
		return edited;
	}

	@Nullable
	private Material getSmeltedVersion(Material original) {
		return switch (original) {
		case IRON_ORE, RAW_IRON -> Material.IRON_INGOT;
		case GOLD_ORE, RAW_GOLD -> Material.GOLD_INGOT;
		case SAND -> Material.GLASS;
		case CLAY -> Material.BRICK;
		default -> null;
		};
	}

	/**
	 * Utility: give a molten core book directly to a player (useful for commands).
	 */
	public void giveBookTo(@NotNull Player player, int level) {
		ItemStack book = createMoltenCoreBook(level).load().clone();
		Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
		if (!leftover.isEmpty()) {
			player.getWorld().dropItemNaturally(player.getLocation(), book);
		}
	}
}