package com.swiftlicious.hellblock.listeners.invasion;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.schematic.AdventureMetadata;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.PotionUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class PiglinShaman implements InvaderCreator {

	private final HellblockPlugin plugin;

	private static final String INVASION_SHAMAN_KEY = "piglin_shaman";

	private final NamespacedKey invasionShamanKey;

	public PiglinShaman(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.invasionShamanKey = new NamespacedKey(plugin, INVASION_SHAMAN_KEY);
	}

	@Override
	public Mob spawn(Location loc, CustomInvasion invasion) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		Piglin shaman = (Piglin) world.spawnEntity(loc, getEntityType());
		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(shaman);
		AdventureMetadata.setEntityCustomName(shaman,
				plugin.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_SHAMAN_NAME.build()));
		shaman.setCustomNameVisible(true);
		shaman.addPotionEffect(new PotionEffect(
				PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"), Integer.MAX_VALUE, 1));
		shaman.getEquipment()
				.setItemInMainHand(plugin.getItemManager().wrap(new ItemStack(Material.BLAZE_ROD)).loadCopy());

		tagAsShaman(shaman);

		// chance scales slightly with difficulty
		float baseChance = 0.15f; // 15%
		float scaledChance = baseChance + (invasion.getProfile().getDifficulty().getTier() * 0.02f); // +2% per tier

		if (RandomUtils.generateRandomFloat(0f, 1f) <= scaledChance) {
			plugin.getInvasionHandler().getSynergyHandler().tagAsSynergyGroupLeader(shaman);
			// Optional: visual cue
			shaman.getWorld().spawnParticle(ParticleUtils.getParticle("ENCHANTMENT_TABLE"),
					shaman.getLocation().clone().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
		}

		plugin.getScheduler().sync().runRepeating(() -> {
			if (!shaman.isValid() || shaman.isDead())
				return;

			List<Entity> nearby = shaman.getNearbyEntities(10, 5, 10);
			AtomicBoolean didHeal = new AtomicBoolean(false);

			nearby.forEach(e -> {
				if ((e instanceof Piglin || e instanceof Hoglin || e instanceof PigZombie)
						&& plugin.getInvasionHandler().isInvasionMob((Mob) e)) {
					LivingEntity ally = (LivingEntity) e;
					ally.addPotionEffect(new PotionEffect(
							PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"), 100, 0));
					ally.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_WITCH"), ally.getLocation(), 6, 0.3,
							0.5, 0.3, 0.02);
				}
			});

			// Heal allies
			nearby.forEach(e -> {
				if (e instanceof Mob ally && plugin.getInvasionHandler().isInvasionMob(ally)) {
					double max = new RtagEntity(ally).getAttributeBase("generic.max_health");
					double newHp = Math.min(ally.getHealth() + 4, max);
					ally.setHealth(newHp);
					world.spawnParticle(Particle.HEART, ally.getLocation(), 3, 0.3, 0.6, 0.3);
					didHeal.set(true);
				}
			});

			if (didHeal.get()) {
				AdventureHelper.playPositionalSound(world, shaman.getLocation(),
						Sound.sound(Key.key("minecraft:entity.evoker.prepare_attack"), Source.BLOCK, 1.0f, 1.2f));
			}

			// Fire aura on nearby players
			nearby.forEach(e -> {
				if (e instanceof Player player) {
					player.setFireTicks(40);
					world.spawnParticle(Particle.FLAME, player.getLocation(), 6, 0.3, 0.5, 0.3, 0.01);
					AdventureHelper.playPositionalSound(world, shaman.getLocation(),
							Sound.sound(Key.key("minecraft:block.fire.ambient"), Source.BLOCK, 0.7f, 1.1f));
				}
			});

		}, 60L, 100L, loc);

		return shaman;
	}

	public void castFirePillars(Mob shaman) {
		Location base = shaman.getLocation();
		World world = base.getWorld();
		for (int i = 0; i < 5; i++) {
			Location loc = base.clone().add(RandomUtils.range(-4, 4), 0, RandomUtils.range(-4, 4));
			world.spawnParticle(Particle.FLAME, loc, 12, 0.2, 0.8, 0.2, 0.02);
			world.strikeLightningEffect(loc); // Visual only
		}
		AdventureHelper.playPositionalSound(world, base,
				Sound.sound(Key.key("minecraft:entity.blaze.shoot"), Source.BLOCK, 1.3f, 1.1f));
	}

	public void tagAsShaman(Mob mob) {
		mob.getPersistentDataContainer().set(invasionShamanKey, PersistentDataType.STRING, INVASION_SHAMAN_KEY);
	}

	public boolean isShaman(Mob mob) {
		return mob.getPersistentDataContainer().has(invasionShamanKey, PersistentDataType.STRING) && INVASION_SHAMAN_KEY
				.equals(mob.getPersistentDataContainer().get(invasionShamanKey, PersistentDataType.STRING));
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.PIGLIN;
	}
}