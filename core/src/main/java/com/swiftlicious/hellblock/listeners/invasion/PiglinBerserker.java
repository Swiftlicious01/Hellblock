package com.swiftlicious.hellblock.listeners.invasion;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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

public class PiglinBerserker implements InvaderCreator {

	private final HellblockPlugin plugin;

	private static final String INVASION_BERSERKER_KEY = "piglin_berserker";

	private final NamespacedKey invasionBerserkerKey;

	public PiglinBerserker(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.invasionBerserkerKey = new NamespacedKey(plugin, INVASION_BERSERKER_KEY);
	}

	@Override
	public Mob spawn(Location loc, CustomInvasion invasion) {
		World world = loc.getWorld();
		if (world == null)
			return null;

		PiglinBrute brute = (PiglinBrute) world.spawnEntity(loc, getEntityType());
		VersionHelper.getInvasionAIManager().stripAllPassiveGoals(brute);
		AdventureMetadata.setEntityCustomName(brute,
				plugin.getTranslationManager().render(MessageConstants.MSG_HELLBLOCK_INVASION_BERSERKER_NAME.build()));
		brute.setCustomNameVisible(true);
		brute.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
		brute.addPotionEffect(new PotionEffect(PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"),
				Integer.MAX_VALUE, 1));

		tagAsBerserker(brute);

		// chance scales slightly with difficulty
		float baseChance = 0.15f; // 15%
		float scaledChance = baseChance + (invasion.getProfile().getDifficulty().getTier() * 0.02f); // +2% per tier

		if (RandomUtils.generateRandomFloat(0f, 1f) <= scaledChance) {
			plugin.getInvasionHandler().getSynergyHandler().tagAsSynergyGroupLeader(brute);
			// Optional: visual cue
			brute.getWorld().spawnParticle(ParticleUtils.getParticle("ENCHANTMENT_TABLE"),
					brute.getLocation().clone().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
		}

		plugin.getScheduler().sync().runRepeating(() -> {
			if (!brute.isValid() || brute.isDead())
				return;

			// Rage Mode
			double maxHealth = new RtagEntity(brute).getAttributeBase("generic.max_health");
			if (brute.getHealth() < maxHealth * 0.5) {
				brute.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2));
				brute.addPotionEffect(new PotionEffect(
						PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"), 200, 2));
				world.spawnParticle(ParticleUtils.getParticle("VILLAGER_ANGRY"), brute.getLocation(), 12, 0.5, 1, 0.5,
						0.1);
			}

			// Fire trail
			Location foot = brute.getLocation().getBlock().getLocation();
			if (foot.getBlock().getType() == Material.AIR
					&& foot.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
				foot.getBlock().setType(Material.FIRE);
			}
			world.spawnParticle(Particle.LAVA, brute.getLocation(), 8, 0.2, 0.5, 0.2, 0.01);

			// Leap toward nearest player
			List<Entity> nearby = brute.getNearbyEntities(10, 5, 10);
			Player target = plugin.getNetherrackGeneratorHandler().getClosestPlayer(brute.getLocation(), nearby);
			if (target != null) {
				Vector leap = target.getLocation().toVector().subtract(brute.getLocation().toVector()).normalize()
						.multiply(1.4);
				leap.setY(0.6);
				brute.setVelocity(leap);
				AdventureHelper.playPositionalSound(world, brute.getLocation(),
						Sound.sound(Key.key("minecraft:entity.piglin_brute.angry"), Source.BLOCK, 1.0f, 0.7f));
			}

		}, 40L, 40L, loc);

		return brute;
	}

	public void tagAsBerserker(Mob mob) {
		mob.getPersistentDataContainer().set(invasionBerserkerKey, PersistentDataType.STRING, INVASION_BERSERKER_KEY);
	}

	public boolean isBerserker(Mob mob) {
		return mob.getPersistentDataContainer().has(invasionBerserkerKey, PersistentDataType.STRING)
				&& INVASION_BERSERKER_KEY
						.equals(mob.getPersistentDataContainer().get(invasionBerserkerKey, PersistentDataType.STRING));
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.PIGLIN_BRUTE;
	}
}