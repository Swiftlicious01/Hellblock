package com.swiftlicious.hellblock.listeners;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.challenges.HellblockChallenge.ActionType;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.extras.Key;

import net.kyori.adventure.sound.Sound;

public class WraithHandler implements Listener, Reloadable {

	protected final HellblockPlugin instance;

	private static final String WRAITH_KEY = "wraith";

	private NamespacedKey wraithKey;

	private final Set<UUID> transformingWraiths = ConcurrentHashMap.newKeySet();

	public WraithHandler(HellblockPlugin plugin) {
		instance = plugin;
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
		wraithKey = new NamespacedKey(instance, WRAITH_KEY);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		transformingWraiths.clear();
	}

	private Item<ItemStack> getBlackLeatherChestplate() {
		final ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		final Item<ItemStack> editedChestplate = instance.getItemManager().wrap(chestplate);
		// Apply dyed color via components
		// Bukkit Color → int RGB
		editedChestplate.setTag(Key.of("minecraft:dyed_color"),
				Map.of("rgb", Color.BLACK.asRGB(), "show_in_tooltip", true));
		editedChestplate.unbreakable(true); // optional: make it unbreakable
		return editedChestplate;
	}

	private Item<ItemStack> getUnbreakingHoe() {
		final ItemStack hoe = new ItemStack(Material.IRON_HOE);
		final Item<ItemStack> editedHoe = instance.getItemManager().wrap(hoe);
		// Extract enchantment key dynamically
		final NamespacedKey unbreakingKey = Enchantment.UNBREAKING.getKey();
		editedHoe.addEnchantment(Key.of(unbreakingKey.getNamespace(), unbreakingKey.getKey()), 3);
		editedHoe.unbreakable(true); // optional: make it unbreakable
		return editedHoe;
	}

	public LivingEntity summonWraith(@NotNull Location location) {
		WitherSkeleton wraith = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);

		// Attributes
		wraith.setInvisible(true);
		final RtagEntity taggedWraith = new RtagEntity(wraith);
		taggedWraith.setAttributeBase("generic.max_health", 60.0);
		taggedWraith.setHealth(60.0F);
		taggedWraith.setAttributeBase("generic.attack_damage", 10.0);
		taggedWraith.setAttributeBase("generic.armor", 10.0);
		taggedWraith.setAttributeBase("generic.movement_speed", 0.10);
		wraith = (WitherSkeleton) taggedWraith.load();

		// Armor
		wraith.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
		wraith.getEquipment().setChestplate(getBlackLeatherChestplate().load().clone());
		wraith.getEquipment().setItemInMainHand(getUnbreakingHoe().load().clone());

		wraith.getPersistentDataContainer().set(wraithKey, PersistentDataType.STRING, WRAITH_KEY);

		return wraith;
	}

	@EventHandler
	public void onMobDamage(EntityDamageEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof LivingEntity mob)) {
			return;
		}

		// If this WitherSkeleton is transforming into a Wraith
		if (mob instanceof WitherSkeleton skeleton) {
			if (transformingWraiths.contains(skeleton.getUniqueId())) {
				// Cancel ALL damage while transforming
				event.setCancelled(true);
				skeleton.setFireTicks(0); // stop any visual fire damage

				// Show "shield bubble" effect at mob's location
				skeleton.getWorld().spawnParticle(Particle.BLOCK, skeleton.getLocation().add(0, 1, 0), 20, // count
						0.5, 0.5, 0.5, // offsets
						0.1, // speed
						Material.SHIELD.createBlockData() // Use shield-like block particles
				);

				return;
			}
		}

		// For already transformed Wraiths (existing logic)
		if (mob instanceof WitherSkeleton witherSkeleton && isWraith(witherSkeleton)) {
			switch (event.getCause()) {
			case FALL, FIRE, FIRE_TICK, LAVA -> {
				event.setCancelled(true);
				mob.setFireTicks(0); // stop visual fire
			}
			default -> {
				// other damage types allowed
			}
			}
		}
	}

	@EventHandler
	public void onWraithSpawn(CreatureSpawnEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.WITHER_SKELETON) {
			return;
		}

		final double chance = 0.05;
		if (Math.random() >= chance) {
			return;
		}

		// Let it transform instead of instant replace
		final WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
		startWraithTransformation(skeleton);
	}

	private void startWraithTransformation(@NotNull WitherSkeleton skeleton) {
		// Disable behavior
		transformingWraiths.add(skeleton.getUniqueId());
		skeleton.setAI(false);
		skeleton.setInvulnerable(true);
		skeleton.setSilent(true);
		skeleton.setGlowing(true);

		final Location loc = skeleton.getLocation();
		final World world = loc.getWorld();
		final int[] ticks = { 0 };

		final SchedulerTask[] taskHolder = new SchedulerTask[1];
		taskHolder[0] = instance.getScheduler().sync().runRepeating(() -> {
			if (ticks[0] >= 60 || skeleton.isDead()) {
				if (!skeleton.isDead()) {
					transformingWraiths.remove(skeleton.getUniqueId());
					skeleton.remove();

					// Spawn Wraith
					final LivingEntity wraith = summonWraith(loc);
					wraith.setGlowing(false);

					// Transformation burst
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.05);
					world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
					world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.01);

					// Sound burst to nearby players
					world.getNearbyEntities(loc, 16, 16, 16, e -> e instanceof Player).stream().map(e -> (Player) e)
							.forEach(
									player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
											Sound.sound(
													net.kyori.adventure.key.Key.key("minecraft:entity.wither.spawn"),
													Sound.Source.HOSTILE, 1.0f, 0.6f)));
				}
				taskHolder[0].cancel();
				return;
			}

			// Ongoing transformation particles
			world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.01);
			world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.01);

			// Periodic bubble/shield particle
			if (ticks[0] % 5 == 0) { // every 5 ticks
				world.spawnParticle(Particle.BLOCK, loc.clone().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1,
						Material.SHIELD.createBlockData());
			}

			if (ticks[0] % 10 == 0) {
				world.getNearbyEntities(loc, 16, 16, 16, e -> e instanceof Player).stream().map(e -> (Player) e)
						.forEach(player -> AdventureHelper.playSound(instance.getSenderFactory().getAudience(player),
								Sound.sound(
										net.kyori.adventure.key.Key.key("minecraft:entity.zombie_villager.converted"),
										Sound.Source.HOSTILE, 0.8f, 0.8f)));
			}

			ticks[0]++;
		}, 0L, 1L, loc);
	}

	@EventHandler
	public void onWraithDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (event.getEntityType() != EntityType.WITHER_SKELETON) {
			return;
		}

		final WitherSkeleton witherSkeleton = (WitherSkeleton) event.getEntity();

		if (!isWraith(witherSkeleton)) {
			return;
		}

		final Player killer = witherSkeleton.getKiller();
		if (killer == null) {
			return;
		}

		instance.getStorageManager().getOnlineUser(killer.getUniqueId()).ifPresent(user -> instance
				.getChallengeManager().handleChallengeProgression(killer, ActionType.SLAY, witherSkeleton));
	}

	public boolean isWraith(@NotNull WitherSkeleton ws) {
		return ws.getPersistentDataContainer().has(wraithKey, PersistentDataType.STRING)
				&& WRAITH_KEY.equals(ws.getPersistentDataContainer().get(wraithKey, PersistentDataType.STRING));
	}
}