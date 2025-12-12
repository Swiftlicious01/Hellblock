package com.swiftlicious.hellblock.listeners.invasion;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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

public class CorruptedHoglin implements InvaderCreator {

	private final HellblockPlugin plugin;

	private static final String INVASION_CORRUPTED_KEY = "corrupted_hoglin";

	private final NamespacedKey invasionCorruptedKey;

	public CorruptedHoglin(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.invasionCorruptedKey = new NamespacedKey(plugin, INVASION_CORRUPTED_KEY);
	}

	@Override
	public Mob spawn(Location loc, CustomInvasion invasion) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		Hoglin hoglin = (Hoglin) world.spawnEntity(loc, getEntityType());
		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(hoglin);
		AdventureMetadata.setEntityCustomName(hoglin,
				plugin.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_CORRUPTED_NAME.build()));
		hoglin.setCustomNameVisible(true);
		hoglin.addPotionEffect(new PotionEffect(
				PotionUtils.getCompatiblePotionEffectType("RESISTANCE", "DAMAGE_RESISTANCE"), Integer.MAX_VALUE, 2));
		hoglin.addPotionEffect(
				new PotionEffect(PotionUtils.getCompatiblePotionEffectType("SLOWNESS", "SLOW"), Integer.MAX_VALUE, 0));

		tagAsCorruptedHoglin(hoglin);

		// chance scales slightly with difficulty
		float baseChance = 0.15f; // 15%
		float scaledChance = baseChance + (invasion.getProfile().getDifficulty().getTier() * 0.02f); // +2% per tier

		if (RandomUtils.generateRandomFloat(0f, 1f) <= scaledChance) {
			plugin.getInvasionHandler().getSynergyHandler().tagAsSynergyGroupLeader(hoglin);
			// Optional: visual cue
			hoglin.getWorld().spawnParticle(ParticleUtils.getParticle("ENCHANTMENT_TABLE"),
					hoglin.getLocation().clone().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
		}

		plugin.getScheduler().sync().runRepeating(() -> {
			if (!hoglin.isValid() || hoglin.isDead())
				return;

			List<Entity> nearby = hoglin.getNearbyEntities(10, 5, 10);
			nearby.forEach(e -> {
				if (e instanceof Piglin piglin && piglin.isValid() && !piglin.isDead()
						&& plugin.getInvasionHandler().isInvasionMob(piglin)) {
					piglin.addPotionEffect(new PotionEffect(
							PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"), 80, 0));
				} else if (e instanceof Player player) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
				}
			});

			world.spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), hoglin.getLocation(), 8, 0.6, 0.5, 0.6, 0.02);
			world.spawnParticle(ParticleUtils.getParticle("REDSTONE"), hoglin.getLocation(), 10, 0.6, 0.5, 0.6,
					new Particle.DustOptions(Color.MAROON, 1.2f));

			// Optional: stomp shockwave knockback every few seconds
			if (RandomUtils.generateRandomInt(100) < 20) {
				nearby.forEach(e -> {
					if (e instanceof Player player) {
						Vector kb = player.getLocation().toVector().subtract(hoglin.getLocation().toVector())
								.normalize().multiply(1.5);
						kb.setY(0.5);
						player.setVelocity(kb);
					}
				});
				AdventureHelper.playPositionalSound(world, hoglin.getLocation(),
						Sound.sound(Key.key("minecraft:entity.hoglin.attack"), Source.BLOCK, 1.2f, 0.8f));
				world.spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), hoglin.getLocation(), 1, 0.2, 0.2,
						0.2);
			}

		}, 80L, 80L, loc);

		return hoglin;
	}

	public void tagAsCorruptedHoglin(Mob mob) {
		mob.getPersistentDataContainer().set(invasionCorruptedKey, PersistentDataType.STRING, INVASION_CORRUPTED_KEY);
	}

	public boolean isCorruptedHoglin(Mob mob) {
		return mob.getPersistentDataContainer().has(invasionCorruptedKey, PersistentDataType.STRING)
				&& INVASION_CORRUPTED_KEY
						.equals(mob.getPersistentDataContainer().get(invasionCorruptedKey, PersistentDataType.STRING));
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.HOGLIN;
	}
}