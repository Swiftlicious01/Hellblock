package com.swiftlicious.hellblock.listeners.invasion;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation.FormationType;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.PotionUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class InvasionSynergy {

	private final HellblockPlugin instance;

	private final NamespacedKey synergyLeaderKey;

	private final static String SYNERGY_LEADER_KEY = "synergy_leader";

	private final Map<UUID, Map<SynergyPattern, Long>> patternCooldowns = new HashMap<>();
	private static final long DEFAULT_PATTERN_COOLDOWN_MS = 5000;

	private final Map<SynergyPattern, Integer> patternUsage = new EnumMap<>(SynergyPattern.class);

	public InvasionSynergy(HellblockPlugin plugin) {
		instance = plugin;
		synergyLeaderKey = new NamespacedKey(plugin, SYNERGY_LEADER_KEY);
	}

	public void reset() {
		logSynergyUsage();
		patternUsage.clear();
		patternCooldowns.clear();
	}

	public SynergyPattern trySynergyPattern(Mob mob, CustomInvasion invasion) {
		InvasionProfile profile = invasion.getProfile();
		InvasionMobFactory factory = instance.getInvasionHandler().getInvaderFactory();
		UUID mobId = mob.getUniqueId();

		// Synergy chance check
		if (!isSynergyGroupLeader(mob)) {
			// 50% reduced chance if not a leader
			if (RandomUtils.generateRandomFloat(0f, 1f) > (profile.getSynergyChance() * 0.5f))
				return null;
		}

		// Visual indicator
		mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
		mob.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_INSTANT"), mob.getLocation(), 12, 0.5, 0.8, 0.5,
				0.02);

		if (factory.getShamanCreator().isShaman(mob) && RandomUtils.roll(0.25f)) {
			factory.getShamanCreator().castFirePillars(mob);
		}

		List<Entity> nearby = mob.getNearbyEntities(10, 5, 10);
		List<Mob> allies = nearby.stream()
				.filter(e -> e instanceof Mob m && instance.getInvasionHandler().isInvasionMob(m)).map(e -> (Mob) e)
				.toList();

		int effectBoost = profile.getDifficulty().getTier();

		// -------- PATTERN 1: Blazing Charge (Shaman + Berserker) --------
		if (factory.getShamanCreator().isShaman(mob) && !isOnCooldown(mobId, SynergyPattern.BLAZING_CHARGE)) {
			if (isSynergyGroupLeader(mob)) {
				invasion.setCurrentFormation(FormationType.CIRCLE);
			}
			allies.stream().filter(a -> factory.getBerserkerCreator().isBerserker(a)).forEach(ally -> {
				ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60 + effectBoost * 10, 1));
				ally.addPotionEffect(
						new PotionEffect(PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"),
								60 + effectBoost * 10, 0));
				Vector leap = mob.getLocation().toVector().subtract(ally.getLocation().toVector()).normalize()
						.multiply(1.2 + (effectBoost * 0.05));
				leap.setY(0.6);
				ally.setVelocity(leap);
				ally.getWorld().spawnParticle(Particle.LAVA, ally.getLocation(), 12, 0.5, 0.6, 0.5, 0.02);
				AdventureHelper.playPositionalSound(ally.getWorld(), ally.getLocation(),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:entity.piglin_brute.angry"),
								Source.BLOCK, 1.1f, 0.9f));
			});
			mob.getWorld().spawnParticle(Particle.FALLING_LAVA, mob.getLocation().clone().add(0, 1, 0), 20, 0.6, 0.4,
					0.6, 0.01);
			setCooldown(mobId, SynergyPattern.BLAZING_CHARGE, DEFAULT_PATTERN_COOLDOWN_MS);
			patternUsage.merge(SynergyPattern.BLAZING_CHARGE, 1, Integer::sum);
			return SynergyPattern.BLAZING_CHARGE;
		}

		// -------- PATTERN 2: Shock Pulse (Hoglin + Shaman nearby) --------
		if (factory.getCorruptedHoglinCreator().isCorruptedHoglin(mob)
				&& !isOnCooldown(mobId, SynergyPattern.SHOCK_PULSE)) {
			if (isSynergyGroupLeader(mob)) {
				invasion.setCurrentFormation(FormationType.TRIANGLE);
			}
			boolean hasShaman = allies.stream().anyMatch(a -> factory.getShamanCreator().isShaman(a));
			if (hasShaman) {
				mob.getNearbyEntities(4, 2, 4).stream().filter(e -> e instanceof Player).map(e -> (Player) e)
						.forEach(player -> {
							Vector kb = player.getLocation().toVector().subtract(mob.getLocation().toVector())
									.normalize().multiply(1.2);
							kb.setY(0.5);
							player.setVelocity(kb);
						});
				mob.getWorld().spawnParticle(ParticleUtils.getParticle("EXPLOSION_LARGE"), mob.getLocation(), 1, 0.2,
						0.2, 0.2);
				AdventureHelper.playPositionalSound(mob.getWorld(), mob.getLocation(), Sound.sound(
						net.kyori.adventure.key.Key.key("minecraft:entity.generic.explode"), Source.BLOCK, 1.2f, 0.8f));

				triggerChainReaction(mob, invasion, SynergyChain.SHOCK_PULSE_CHAIN);
			}
			mob.getWorld().strikeLightningEffect(mob.getLocation()); // purely visual
			setCooldown(mobId, SynergyPattern.SHOCK_PULSE, DEFAULT_PATTERN_COOLDOWN_MS + 1000);
			patternUsage.merge(SynergyPattern.SHOCK_PULSE, 1, Integer::sum);
			return SynergyPattern.SHOCK_PULSE;
		}

		// -------- PATTERN 3: Focus Fire (Piglin hit → Berserker targets) --------
		if (!factory.getBerserkerCreator().isBerserker(mob) && !isOnCooldown(mobId, SynergyPattern.FOCUS_FIRE)) {
			Player nearest = instance.getNetherrackGeneratorHandler().getClosestPlayer(mob.getLocation(), nearby);
			if (nearest != null) {
				allies.stream().filter(a -> factory.getBerserkerCreator().isBerserker(a)).forEach(ally -> {
					ally.setTarget(nearest);
					ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 80 + effectBoost * 10, 1));
					ally.getWorld().spawnParticle(ParticleUtils.getParticle("SMOKE_LARGE"), ally.getLocation(), 6, 0.3,
							0.4, 0.3, 0.01);
				});
				setCooldown(mobId, SynergyPattern.FOCUS_FIRE, DEFAULT_PATTERN_COOLDOWN_MS + 1500);
				patternUsage.merge(SynergyPattern.FOCUS_FIRE, 1, Integer::sum);
				return SynergyPattern.FOCUS_FIRE;
			}
		}

		// -------- PATTERN 4: Fury Circle (3+ mobs near player) --------
		if (!isOnCooldown(mobId, SynergyPattern.FURY_CIRCLE)) {
			for (Player player : instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer)
					.filter(Objects::nonNull).toList()) {
				if (!player.getWorld().getUID().equals(mob.getWorld().getUID()))
					continue;

				long count = allies.stream().filter(a -> a.getLocation().distanceSquared(player.getLocation()) <= 36)
						.count();

				if (count >= 3) {
					Vector dir = player.getLocation().toVector().subtract(mob.getLocation().toVector());
					Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(0.8);
					side.setY(0.2);
					mob.setVelocity(side);
					mob.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mob.getLocation(), 4, 0.2, 0.5, 0.2,
							0.01);
					setCooldown(mobId, SynergyPattern.FURY_CIRCLE, DEFAULT_PATTERN_COOLDOWN_MS + 2000);
					patternUsage.merge(SynergyPattern.FURY_CIRCLE, 1, Integer::sum);
					return SynergyPattern.FURY_CIRCLE;
				}
			}
		}

		// -------- PATTERN 5: Fire Shield (Shaman buffs Piglins) --------
		if (factory.getShamanCreator().isShaman(mob) && !isOnCooldown(mobId, SynergyPattern.FIRE_SHIELD)) {
			allies.stream().filter(
					ally -> ally instanceof Piglin piglin && instance.getInvasionHandler().isInvasionMob(piglin))
					.forEach(ally -> {
						ally.addPotionEffect(
								new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100 + effectBoost * 20, 0));
						ally.getWorld().spawnParticle(Particle.FLAME, ally.getLocation(), 6, 0.3, 0.5, 0.3, 0.01);
					});
			AdventureHelper.playPositionalSound(mob.getWorld(), mob.getLocation(), Sound
					.sound(net.kyori.adventure.key.Key.key("minecraft:block.fire.ambient"), Source.BLOCK, 1.0f, 1.1f));
			mob.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, mob.getLocation(), 12, 0.4, 0.4, 0.4, 0.02);
			setCooldown(mobId, SynergyPattern.FIRE_SHIELD, DEFAULT_PATTERN_COOLDOWN_MS);
			patternUsage.merge(SynergyPattern.FIRE_SHIELD, 1, Integer::sum);
			return SynergyPattern.FIRE_SHIELD;
		}

		return null;
	}

	/**
	 * Applies passive aura buffs to allies near synergy leaders.
	 */
	public void tickLeaderAuras(CustomInvasion invasion) {
		for (UUID mobId : invasion.getMobIds()) {
			Entity e = Bukkit.getEntity(mobId);
			if (!(e instanceof Mob mob) || !isSynergyGroupLeader(mob) || mob.isDead() || !mob.isValid())
				continue;

			List<Mob> allies = mob.getNearbyEntities(8, 4, 8).stream()
					.filter(en -> en instanceof Mob m && instance.getInvasionHandler().isInvasionMob(m))
					.map(en -> (Mob) en).toList();

			// Buff radius scales slightly by difficulty
			int tier = invasion.getProfile().getDifficulty().getTier();
			int duration = 60 + (tier * 10);
			int amplifier = (tier >= 5 ? 1 : 0); // higher tiers give stronger effects

			for (Mob ally : allies) {
				if (ally.equals(mob))
					continue; // skip self

				ally.addPotionEffect(new PotionEffect(
						PotionUtils.getCompatiblePotionEffectType("STRENGTH", "INCREASE_DAMAGE"), duration, amplifier));
				ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0));
				ally.getWorld().spawnParticle(ParticleUtils.getParticle("SPELL_MOB"),
						ally.getLocation().clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);
			}

			// Aura ring on ground (circle effect)
			for (int i = 0; i < 12; i++) {
				double angle = Math.toRadians((360 / 12) * i);
				double x = Math.cos(angle) * 1.8;
				double z = Math.sin(angle) * 1.8;
				Location ringPos = mob.getLocation().clone().add(x, 0.05, z);
				mob.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, ringPos, 1, 0, 0, 0, 0);
			}

			// Visual aura on the leader
			mob.getWorld().spawnParticle(ParticleUtils.getParticle("ENCHANTMENT_TABLE"),
					mob.getLocation().clone().add(0, 1.5, 0), 12, 0.5, 0.8, 0.5, 0.01);

			// Light sound pulse every few seconds
			if (RandomUtils.generateRandomInt(100) < 5) {
				AdventureHelper.playPositionalSound(mob.getWorld(), mob.getLocation(), Sound.sound(
						net.kyori.adventure.key.Key.key("minecraft:block.beacon.ambient"), Source.BLOCK, 0.8f, 1.2f));
			}
		}
	}

	public void checkForRetreat(CustomInvasion invasion) {
		int total = invasion.getMobIds().size();
		int active = (int) invasion.getMobIds().stream().map(Bukkit::getEntity)
				.filter(e -> e instanceof Mob m && m.isValid() && !m.isDead()).count();

		if (active < Math.max(3, total * 0.3)) {
			for (UUID mobId : invasion.getMobIds()) {
				Entity e = Bukkit.getEntity(mobId);
				if (!(e instanceof Mob mob) || !mob.isValid())
					continue;

				Vector retreatVec = new Vector(RandomUtils.generateRandomFloat(-1.2f, 1.2f), 0,
						RandomUtils.generateRandomFloat(-1.2f, 1.2f)).normalize().multiply(0.6);
				retreatVec.setY(0.2);
				mob.setVelocity(retreatVec);

				mob.addPotionEffect(
						new PotionEffect(PotionUtils.getCompatiblePotionEffectType("SLOWNESS", "SLOW"), 60, 0));
				mob.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, mob.getLocation(), 4, 0.2, 0.3, 0.2, 0.01);
			}

			instance.debug("[Invasion] Retreat triggered on island: " + invasion.getIslandId());
		}
	}

	private boolean isOnCooldown(UUID mobId, SynergyPattern patternKey) {
		Map<SynergyPattern, Long> mobCooldowns = patternCooldowns.getOrDefault(mobId, new HashMap<>());
		long now = System.currentTimeMillis();
		return mobCooldowns.getOrDefault(patternKey, 0L) > now;
	}

	private void setCooldown(UUID mobId, SynergyPattern patternKey, long duration) {
		patternCooldowns.computeIfAbsent(mobId, k -> new HashMap<>()).put(patternKey,
				System.currentTimeMillis() + duration);
	}

	private void triggerChainReaction(Mob source, CustomInvasion invasion, SynergyChain chainKey) {
		for (UUID mobId : invasion.getMobIds()) {
			Entity e = Bukkit.getEntity(mobId);
			if (!(e instanceof Mob mob) || !mob.isValid() || mob.isDead()
					|| !mob.getWorld().getUID().equals(source.getWorld().getUID()))
				continue;

			if (mob.getLocation().distanceSquared(source.getLocation()) <= 100) { // ~10 blocks
				trySynergyPattern(mob, invasion); // Retry synergy pattern
				source.getWorld().spawnParticle(Particle.SOUL, source.getLocation(), 10, 0.3, 0.4, 0.3, 0.01);
				AdventureHelper.playPositionalSound(source.getWorld(), source.getLocation(),
						Sound.sound(net.kyori.adventure.key.Key.key("minecraft:block.respawn_anchor.ambient"),
								Source.BLOCK, 1.2f, 0.7f));
			}
		}
	}

	public boolean isSynergyGroupLeader(Mob mob) {
		return mob.getPersistentDataContainer().has(synergyLeaderKey, PersistentDataType.INTEGER);
	}

	public void tagAsSynergyGroupLeader(Mob mob) {
		mob.getPersistentDataContainer().set(synergyLeaderKey, PersistentDataType.INTEGER, 1);
	}

	public void logSynergyUsage() {
		if (patternUsage.isEmpty())
			return;
		instance.debug("Synergy Pattern Usage:");
		patternUsage.entrySet()
				.forEach(entry -> instance.debug("- " + entry.getKey().name() + ": " + entry.getValue()));
	}

	public enum SynergyPattern {
		BLAZING_CHARGE, SHOCK_PULSE, FOCUS_FIRE, FURY_CIRCLE, FIRE_SHIELD;
	}

	public enum SynergyChain {
		SHOCK_PULSE_CHAIN;
	}
}