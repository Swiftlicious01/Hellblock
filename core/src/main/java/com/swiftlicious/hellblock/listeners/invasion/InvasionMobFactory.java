package com.swiftlicious.hellblock.listeners.invasion;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Strider;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.creation.item.Item;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.utils.EnchantmentUtils;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;
import com.swiftlicious.hellblock.utils.extras.Key;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class InvasionMobFactory {

	private final HellblockPlugin plugin;

	protected final PiglinShaman piglinShaman;
	protected final PiglinBerserker piglinBerserker;
	protected final CorruptedHoglin corruptedHoglin;

	public InvasionMobFactory(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.piglinShaman = new PiglinShaman(plugin);
		this.piglinBerserker = new PiglinBerserker(plugin);
		this.corruptedHoglin = new CorruptedHoglin(plugin);
	}

	@NotNull
	public PiglinShaman getShamanCreator() {
		return this.piglinShaman;
	}

	@NotNull
	public PiglinBerserker getBerserkerCreator() {
		return this.piglinBerserker;
	}

	@NotNull
	public CorruptedHoglin getCorruptedHoglinCreator() {
		return this.corruptedHoglin;
	}

	/**
	 * Spawns an invasion mob based on the InvasionProfile’s tier and probabilities.
	 */
	@Nullable
	public UUID spawnInvaderAt(@Nullable Location loc, @Nullable CustomInvasion invasion,
			@NotNull InvasionSpawnContext context, @Nullable Consumer<Mob> setup) {
		if (loc == null || invasion == null)
			return null;

		World world = loc.getWorld();
		if (world == null)
			return null;

		// Decide what kind of mob to spawn
		float roll = context.random().nextFloat();
		Mob mob;

		if (roll < context.profile().getSpecialMobChance()
				&& context.specialMobCounter().get() < context.profile().getMaxSpecialMobsPerWave()) {
			mob = spawnSpecialInvader(loc, invasion, context);
			context.specialMobCounter().incrementAndGet();
		} else {
			mob = spawnBaseInvader(loc, invasion, context);
		}

		if (mob == null)
			return null;

		// Mark as invasion mob
		plugin.getInvasionHandler().tagAsInvasionMob(mob);

		// Run post-spawn setup (e.g., targeting or path logic)
		if (setup != null)
			setup.accept(mob);

		// Add particles for elite tiers
		if (context.profile().isEliteMobsEnabled()) {
			world.spawnParticle(Particle.CRIMSON_SPORE, mob.getLocation(), 20, 0.3, 0.6, 0.3, 0.02);
		}

		return mob.getUniqueId();
	}

	@Nullable
	private Mob spawnBaseInvader(@NotNull Location loc, @NotNull CustomInvasion invasion,
			@NotNull InvasionSpawnContext context) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		// Randomly pick a base type
		EntityType type = getRandomBasePiglinType(context.random());
		Mob piglin = (Mob) world.spawnEntity(loc, type);
		piglin.setPersistent(true);
		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(piglin);

		// Equip armor + weapon
		equipBaseGear(piglin, context.profile());
		return piglin;
	}

	@Nullable
	public MountSpawnResult maybeMountPiglin(@NotNull Piglin piglin, @NotNull Location loc,
			@Nullable CustomInvasion invasion, @NotNull InvasionSpawnContext context, int wave) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		// chance based on wave or difficulty
		final int difficulty = context.profile().getDifficulty().getTier();

		// Example: base 10%, increases with wave, capped at 25%
		int mountChance = 10 + (wave * 2); // wave 1 = 12%, wave 5 = 20%
		if (difficulty >= 4) {
			mountChance += 5; // Hard mode gets higher chance
		}
		mountChance = Math.min(mountChance, 25); // hard cap

		int roll = context.random().nextInt(100);

		// === Mount attempt ===
		if (roll < mountChance / 2) {
			// --- Magma Cube mount ---
			MagmaCube cube = (MagmaCube) world.spawnEntity(loc, EntityType.MAGMA_CUBE);
			cube.setSize(context.random().nextInt(2, 4));
			cube.addPassenger(piglin);

			VersionHelper.getInvasionAIManager().stripAllPassiveGoals(cube);
			VersionHelper.getInvasionAIManager().addFollowPlayerGoal(cube);

			if (invasion != null) {
				invasion.addMobTask(cube.getUniqueId(), null, plugin.getScheduler().sync()
						.runRepeating(() -> InvasionGoalHelper.updateRideableMobGoals(cube), 1L, 1L, loc));
			}

			// Visual/audio
			playMountSpawnAnimation(cube.getType(), loc);
			return new MountSpawnResult(cube, cube.getType());

		} else if (roll < mountChance) {
			// --- Strider mount ---
			Strider strider = (Strider) world.spawnEntity(loc, EntityType.STRIDER);
			strider.setSaddle(true);
			strider.addPassenger(piglin);

			VersionHelper.getInvasionAIManager().stripAllPassiveGoals(strider);
			VersionHelper.getInvasionAIManager().addFollowPlayerGoal(strider);

			if (invasion != null) {
				invasion.addMobTask(strider.getUniqueId(), null, plugin.getScheduler().sync()
						.runRepeating(() -> InvasionGoalHelper.updateRideableMobGoals(strider), 1L, 1L, loc));
			}

			// Visual/audio
			playMountSpawnAnimation(strider.getType(), loc);
			return new MountSpawnResult(strider, strider.getType());
		}

		return null;
	}

	public void playMountSpawnAnimation(@NotNull EntityType mountType, @NotNull Location loc) {
		World world = loc.getWorld();
		if (world == null)
			return;

		if (mountType == EntityType.MAGMA_CUBE) {
			world.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.5, 0.3, 0.01);
			world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), loc, 10, 0.2, 0.3, 0.2, 0.01);
			AdventureHelper.playPositionalSound(world, loc, Sound.sound(
					net.kyori.adventure.key.Key.key("minecraft:entity.magma_cube.squish"), Source.BLOCK, 1.2f, 0.9f));
		} else if (mountType == EntityType.STRIDER) {
			world.spawnParticle(Particle.LAVA, loc, 20, 0.4, 0.5, 0.4, 0.01);
			world.spawnParticle(Particle.SOUL, loc, 10, 0.2, 0.3, 0.2, 0.01);
			AdventureHelper.playPositionalSound(world, loc, Sound.sound(
					net.kyori.adventure.key.Key.key("minecraft:entity.strider.ambient"), Source.BLOCK, 1.1f, 0.95f));
		}
	}

	public boolean isMountEntity(@NotNull Entity entity) {
		return entity.getType() == EntityType.STRIDER || entity.getType() == EntityType.MAGMA_CUBE;
	}

	@NotNull
	public EntityType peekNextMobType(@NotNull InvasionSpawnContext context) {
		float roll = context.random().nextFloat();

		if (roll < context.profile().getSpecialMobChance()
				&& context.specialMobCounter().get() < context.profile().getMaxSpecialMobsPerWave()) {
			int specialRoll = context.random().nextInt(100);
			if (specialRoll < 40) {
				return piglinBerserker.getEntityType();
			} else if (specialRoll < 75) {
				return piglinShaman.getEntityType();
			} else {
				return corruptedHoglin.getEntityType();
			}
		}

		// Base mob path
		return getRandomBasePiglinType(context.random());
	}

	@NotNull
	private EntityType getRandomBasePiglinType(@NotNull Random random) {
		int roll = random.nextInt(100);
		if (roll < 35)
			return EntityType.PIGLIN;
		if (roll < 60)
			return EntityType.ZOMBIFIED_PIGLIN;
		if (roll < 80)
			return EntityType.PIGLIN_BRUTE;
		if (roll < 90)
			return EntityType.HOGLIN;
		return EntityType.ZOGLIN;
	}

	@NotNull
	private Mob spawnSpecialInvader(@NotNull Location loc, @NotNull CustomInvasion invasion,
			@NotNull InvasionSpawnContext context) {
		int roll = context.random().nextInt(100);
		if (roll < 40)
			return piglinBerserker.spawn(loc, invasion);
		else if (roll < 75)
			return piglinShaman.spawn(loc, invasion);
		else
			return corruptedHoglin.spawn(loc, invasion);
	}

	private void equipBaseGear(@NotNull Mob mob, @NotNull InvasionProfile profile) {
		if (!(mob instanceof Piglin piglin))
			return;

		boolean isBrute = mob instanceof PiglinBrute;

		int armorChance = profile.isEliteMobsEnabled() ? 75 : 40;
		EntityEquipment equipment = piglin.getEquipment();

		// Gold armor logic (same for Piglins and Brutes)
		Material[] gear = { Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS,
				Material.GOLDEN_BOOTS };

		for (Material piece : gear) {
			if (RandomUtils.generateRandomInt(100) < armorChance) {
				Item<ItemStack> stack = plugin.getItemManager().wrap(new ItemStack(piece)).unbreakable(true);

				// Chance to enchant armor
				if (RandomUtils.generateRandomInt(100) < 25) {
					NamespacedKey protection = EnchantmentUtils
							.getCompatibleEnchantment("PROTECTION", "PROTECTION_ENVIRONMENTAL").getKey();
					stack.addEnchantment(Key.of(protection.getNamespace(), protection.getKey()), 1);
				}

				// Equip the armor piece
				switch (piece) {
				case GOLDEN_HELMET -> equipment.setHelmet(stack.loadCopy());
				case GOLDEN_CHESTPLATE -> equipment.setChestplate(stack.loadCopy());
				case GOLDEN_LEGGINGS -> equipment.setLeggings(stack.loadCopy());
				case GOLDEN_BOOTS -> equipment.setBoots(stack.loadCopy());
				default -> throw new IllegalArgumentException("Unexpected armor type: " + piece);
				}
			}
		}

		// Weapon logic
		Material weaponType;
		if (isBrute) {
			weaponType = Material.GOLDEN_AXE; // Brutes get axe only
		} else {
			weaponType = RandomUtils.generateRandomBoolean() ? Material.GOLDEN_SWORD : Material.CROSSBOW;
		}

		Item<ItemStack> weapon = plugin.getItemManager().wrap(new ItemStack(weaponType)).unbreakable(true);

		// Chance to enchant weapon
		if (RandomUtils.generateRandomInt(100) < 20) {
			NamespacedKey enchantKey;

			if (weaponType == Material.CROSSBOW) {
				enchantKey = Enchantment.QUICK_CHARGE.getKey();
			} else {
				// Axe or Sword both get Sharpness
				enchantKey = EnchantmentUtils.getCompatibleEnchantment("SHARPNESS", "DAMAGE_ALL").getKey();
			}

			weapon.addEnchantment(Key.of(enchantKey.getNamespace(), enchantKey.getKey()), 1);
		}

		equipment.setItemInMainHand(weapon.loadCopy());
	}

	public record InvasionSpawnContext(@NotNull InvasionProfile profile, @NotNull AtomicInteger specialMobCounter,
			@NotNull Random random) {
	}

	public record MountSpawnResult(@NotNull Mob mount, @NotNull EntityType type) {
	}
}