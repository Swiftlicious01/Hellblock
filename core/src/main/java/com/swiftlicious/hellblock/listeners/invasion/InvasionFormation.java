package com.swiftlicious.hellblock.listeners.invasion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.util.concurrent.AtomicDouble;
import com.saicone.rtag.RtagEntity;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.listeners.invasion.InvaderBehaviorQueue.BehaviorCommand;
import com.swiftlicious.hellblock.player.UserData;
import com.swiftlicious.hellblock.utils.ParticleUtils;
import com.swiftlicious.hellblock.utils.PotionUtils;
import com.swiftlicious.hellblock.utils.RandomUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;

public class InvasionFormation {

	private final HellblockPlugin instance;

	private final Map<UUID, InvasionBehavior> mobBehaviors = new HashMap<>();
	private final Map<UUID, UUID> piglinToFormationHost = new ConcurrentHashMap<>();

	private final InvaderBehaviorQueue behaviorQueue = new InvaderBehaviorQueue();

	public InvasionFormation(HellblockPlugin plugin) {
		this.instance = plugin;
	}

	public void reset() {
		mobBehaviors.clear();
		clearFormationHosts();
		behaviorQueue.clearAll();
	}

	public void setFormationHost(@NotNull UUID piglinId, @NotNull UUID mountId) {
		piglinToFormationHost.put(piglinId, mountId);
	}

	@Nullable
	public UUID getFormationHost(@NotNull UUID piglinId) {
		return piglinToFormationHost.get(piglinId);
	}

	@Nullable
	public UUID clearFormationHost(@NotNull UUID piglinId) {
		return piglinToFormationHost.remove(piglinId);
	}

	public void clearFormationHosts() {
		piglinToFormationHost.clear();
	}

	/**
	 * Arranges mobs around a leader in a rough formation.
	 */
	public void formClusterAroundLeader(Mob leader, List<Mob> allies, double baseRadius) {
		if (leader == null || allies.isEmpty())
			return;

		Location center = leader.getLocation();

		// Dynamic radius shrink if players are nearby
		Player closest = instance.getNetherrackGeneratorHandler().getClosestPlayer(leader.getLocation(),
				new ArrayList<>(allies));
		double distance = closest != null ? leader.getLocation().distance(closest.getLocation()) : 10;
		double radius = Math.max(2.5, Math.min(baseRadius, distance * 0.5)); // Closer player = tighter cluster

		int count = allies.size();
		double angleStep = 360.0 / count;

		for (int i = 0; i < count; i++) {
			Mob ally = allies.get(i);
			if (ally == null || !ally.isValid() || ally.equals(leader))
				continue;

			double angle = Math.toRadians(angleStep * i);
			double x = Math.cos(angle) * radius;
			double z = Math.sin(angle) * radius;

			Location target = center.clone().add(x, 0, z);
			Vector moveVec = target.toVector().clone().subtract(ally.getLocation().toVector()).normalize()
					.multiply(0.3);
			moveVec.setY(0);
			ally.setVelocity(moveVec);

			// Formation ring visuals
			ally.getWorld().spawnParticle(ParticleUtils.getParticle("REDSTONE"),
					ally.getLocation().clone().add(0, 0.8, 0), 2, 0.1, 0.1, 0.1, 0);
		}
	}

	/**
	 * Assigns formation roles (frontline, midline, support) around spawn center.
	 */
	public void arrangeWaveFormation(List<Mob> mobs, Location center) {
		if (mobs.isEmpty())
			return;

		int count = mobs.size();
		double radius = 4 + (count * 0.2);

		for (int i = 0; i < count; i++) {
			Mob mob = mobs.get(i);
			if (!mob.isValid())
				continue;

			double angle = (2 * Math.PI / count) * i;
			double x = Math.cos(angle) * radius;
			double z = Math.sin(angle) * radius;
			Location pos = center.clone().add(x, 0, z);

			Vector vec = pos.toVector().subtract(mob.getLocation().toVector()).normalize().multiply(0.25);
			mob.setVelocity(vec);
		}
	}

	public void tickFormationClusters(CustomInvasion invasion, InvasionSynergy synergy) {
		for (UUID mobId : invasion.getMobIds()) {
			Entity e = Bukkit.getEntity(mobId);
			if (!(e instanceof Mob mob) || !mob.isValid() || mob.isDead())
				continue;

			// Use formation host (e.g., strider or magma cube) if present
			if (mob instanceof Piglin piglin) {
				UUID hostId = getFormationHost(piglin.getUniqueId());
				if (hostId != null) {
					Entity host = Bukkit.getEntity(hostId);
					if (host != null && host instanceof Mob mount && mount.isValid() && !mount.isDead()) {
						mob = mount;
					}
				}
			}

			if (!synergy.isSynergyGroupLeader(mob))
				continue;

			List<Mob> allies = mob.getNearbyEntities(10, 5, 10).stream()
					.filter(en -> en instanceof Mob m && instance.getInvasionHandler().isInvasionMob(m))
					.map(en -> (Mob) en).toList();

			formClusterAroundLeader(mob, allies, 5.0);
			showFormationRing(mob.getLocation(), 3.0);
		}
	}

	public Mob promoteNewLeader(CustomInvasion invasion) {
		AtomicReference<Mob> newLeader = new AtomicReference<>();
		AtomicDouble highestHealth = new AtomicDouble(-1); // Use Apache Commons or roll your own

		invasion.getMobIds().stream().map(Bukkit::getEntity)
				.filter(e -> e instanceof Mob mob && mob.isValid() && !mob.isDead()).map(e -> (Mob) e).forEach(ent -> {
					double health = ent.getHealth();
					if (health > highestHealth.get()) {
						highestHealth.set(health);
						newLeader.set(ent);
					}
				});

		Mob leader = newLeader.get();
		if (leader != null) {
			instance.getInvasionHandler().getSynergyHandler().tagAsSynergyGroupLeader(leader);
			invasion.setCurrentFormation(FormationType.TRIANGLE);
			leader.getWorld().spawnParticle(ParticleUtils.getParticle("TOTEM"), leader.getLocation(), 12, 0.3, 0.8, 0.3,
					0.05);
			AdventureHelper.playPositionalSound(leader.getWorld(), leader.getLocation(),
					Sound.sound(Key.key("minecraft:entity.evoker.prepare_attack"), Source.BLOCK, 1.2f, 0.8f));
		}

		return leader;
	}

	public void rotateFormationTowardPlayer(Mob leader, List<Mob> group, Player player, FormationType type) {
		if (group.isEmpty())
			return;

		Vector toPlayer = player.getLocation().toVector().subtract(leader.getLocation().toVector()).normalize();
		double angle = Math.atan2(toPlayer.getZ(), toPlayer.getX());

		arrangeFormation(leader, group, type, 4.5, angle); // Overloaded method with rotation
	}

	public void arrangeFormation(Mob leader, List<Mob> allies, FormationType type, double radius, double angle) {
		Location center = leader.getLocation();
		int count = allies.size();

		for (int i = 0; i < count; i++) {
			Mob mob = allies.get(i);
			if (!mob.isValid() || mob.isDead())
				continue;

			Location offset = switch (type) {
			case LINE -> offsetLine(center, i, count, radius);
			case TRIANGLE -> offsetTriangle(center, i, radius);
			case CIRCLE -> offsetCircle(center, i, count, radius);
			case CLUSTER -> offsetCluster(center, radius);
			};

			Location rotated = rotateAround(center, offset, angle);
			moveTo(mob, rotated);
		}

		// Visual ring around leader
		center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 12, 0.3, 0.2, 0.3, 0.01);
		instance.debug("Applying formation: " + type.name() + " to mobs of invasion lead by " + leader.getType());
	}

	private Location rotateAround(Location center, Location point, double angleDegrees) {
		double radians = Math.toRadians(angleDegrees);
		double dx = point.getX() - center.getX();
		double dz = point.getZ() - center.getZ();

		double cos = Math.cos(radians);
		double sin = Math.sin(radians);

		double rotatedX = dx * cos - dz * sin;
		double rotatedZ = dx * sin + dz * cos;

		return new Location(center.getWorld(), center.getX() + rotatedX, point.getY(), center.getZ() + rotatedZ);
	}

	public float calculateYawTo(Location from, Location to) {
		Vector dir = to.toVector().subtract(from.toVector());
		return (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
	}

	private void moveTo(Mob mob, Location target) {
		// Smooth movement if not too far, else teleport
		if (mob.getLocation().distanceSquared(target) < 16) {
			Vector dir = target.toVector().subtract(mob.getLocation().toVector()).normalize().multiply(0.4);
			dir.setY(0.1);
			mob.setVelocity(dir);
		} else {
			mob.teleport(target);
		}
	}

	private Location offsetLine(Location base, int index, int total, double spacing) {
		double offset = (index - (total / 2.0)) * spacing;
		return base.clone().add(offset, 0, 0);
	}

	private Location offsetCircle(Location base, int index, int total, double radius) {
		double angle = 2 * Math.PI * index / total;
		double x = Math.cos(angle) * radius;
		double z = Math.sin(angle) * radius;
		return base.clone().add(x, 0, z);
	}

	private Location offsetTriangle(Location base, int index, double spacing) {
		int row = (int) Math.floor((Math.sqrt(8 * index + 1) - 1) / 2);
		int posInRow = index - row * (row + 1) / 2;
		double x = (posInRow - row / 2.0) * spacing;
		double z = -row * spacing;
		return base.clone().add(x, 0, z);
	}

	private Location offsetCluster(Location base, double radius) {
		double angle = Math.random() * 2 * Math.PI;
		double distance = Math.random() * radius;
		double x = Math.cos(angle) * distance;
		double z = Math.sin(angle) * distance;
		return base.clone().add(x, 0, z);
	}

	public InvasionBehavior getBehavior(UUID uuid) {
		return mobBehaviors.getOrDefault(uuid, InvasionBehavior.DEFAULT);
	}

	public void setMobBehavior(UUID uuid, InvasionBehavior behavior) {
		mobBehaviors.put(uuid, behavior);
	}

	public InvasionBehavior decideBehaviorFor(Mob mob, CustomInvasion invasion) {
		InvasionMobFactory factory = instance.getInvasionHandler().getInvaderFactory();

		if (factory.getShamanCreator().isShaman(mob))
			return InvasionBehavior.SUPPORT;

		if (factory.getBerserkerCreator().isBerserker(mob))
			return InvasionBehavior.AGGRESSIVE;

		if (factory.getCorruptedHoglinCreator().isCorruptedHoglin(mob))
			return InvasionBehavior.DEFENSIVE;

		float roll = RandomUtils.generateRandomFloat(0f, 1f);
		if (roll < 0.25f)
			return InvasionBehavior.FLANKER;

		return InvasionBehavior.DEFAULT;
	}

	public void applyBehavior(Mob mob, CustomInvasion invasion) {
		InvasionBehavior behavior = getBehavior(mob.getUniqueId());
		List<Entity> nearby = mob.getNearbyEntities(12, 6, 12);
		Player target = instance.getNetherrackGeneratorHandler().getClosestPlayer(mob.getLocation(), nearby);

		switch (behavior) {
		case AGGRESSIVE -> {
			if (target != null) {
				mob.setTarget(target);
				mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
				mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation(), 8, 0.3, 0.5, 0.3, 0.02);
			}
		}

		case FLANKER -> {
			if (target != null) {
				Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector());
				Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(0.5);
				side.setY(0.2);
				mob.setVelocity(side);
				mob.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mob.getLocation(), 6, 0.3, 0.4, 0.3, 0.01);
			}
		}

		case SUPPORT -> {
			nearby.stream()
					.filter(entity -> entity instanceof Mob ally && instance.getInvasionHandler().isInvasionMob(ally))
					.map(entity -> (Mob) entity).forEach(entity -> {
						double max = new RtagEntity(entity).getAttributeBase("generic.max_health");
						if (entity.getHealth() < max) {
							double newHp = Math.min(entity.getHealth() + 4, max);
							entity.setHealth(newHp);
							entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation(), 4, 0.3, 0.5, 0.3);
						}
					});
		}

		case DEFENSIVE -> {
			Vector retreat = new Vector(RandomUtils.generateRandomFloat(-1f, 1f), 0,
					RandomUtils.generateRandomFloat(-1f, 1f)).normalize().multiply(0.5);
			retreat.setY(0.1);
			mob.setVelocity(retreat);
			mob.addPotionEffect(new PotionEffect(PotionUtils.getCompatiblePotionEffectType("SLOWNESS", "SLOW"), 40, 1));
			mob.getWorld().spawnParticle(Particle.CLOUD, mob.getLocation(), 6, 0.2, 0.4, 0.2, 0.01);
		}

		default -> {
			// DEFAULT: Idle or low-aggression following
			if (target != null) {
				mob.setTarget(target);
				if (mob.getLocation().distanceSquared(target.getLocation()) > 64) {
					mob.getWorld().spawnParticle(Particle.CRIT, mob.getLocation(), 3, 0.2, 0.2, 0.2);
				}
			}
		}
		}

		instance.debug("Applying behavior: " + behavior.name() + " to invasion mob " + mob.getType());
	}

	public void applyCommand(Mob mob, CustomInvasion invasion) {
		if (!instance.getInvasionHandler().isInvasionRunning(invasion.getIslandId()))
			return;

		BehaviorCommand command = behaviorQueue.poll(mob);
		if (command == null)
			return;

		switch (command) {
		case REGROUP -> regroupTowardsLeader(mob, invasion);
		case RETREAT -> retreatAwayFromPlayers(mob);
		case CHARGE -> chargeNearestPlayer(mob);
		case CIRCLE_PLAYER -> circlePlayer(mob, 5.0);
		case HOLD_POSITION -> holdCurrentPosition(mob);
		case FOLLOW_LEADER -> followLeader(mob, invasion);
		}

		instance.debug("Applying command: " + command.name() + " to invasion mob " + mob.getType());
	}

	public void regroupTowardsLeader(Mob mob, CustomInvasion invasion) {
		Mob leader = invasion.getLeader();
		if (leader == null || leader.equals(mob))
			return;

		Vector dir = leader.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize();
		Vector velocity = dir.multiply(0.4);
		velocity.setY(0.2);
		mob.setVelocity(velocity);

		showFormationEffect(mob.getLocation(), ParticleUtils.getParticle("VILLAGER_HAPPY"));
	}

	public void retreatAwayFromPlayers(Mob mob) {
		Player nearest = instance.getNetherrackGeneratorHandler().getClosestPlayer(mob.getLocation(),
				new ArrayList<>(instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer)
						.filter(Objects::nonNull).toList()));
		if (nearest == null)
			return;

		Vector away = mob.getLocation().toVector().subtract(nearest.getLocation().toVector()).normalize();
		away.multiply(0.6).setY(0.3);
		mob.setVelocity(away);

		showFormationEffect(mob.getLocation(), ParticleUtils.getParticle("SMOKE_LARGE"));
	}

	public void chargeNearestPlayer(Mob mob) {
		Player nearest = instance.getNetherrackGeneratorHandler().getClosestPlayer(mob.getLocation(),
				new ArrayList<>(instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer)
						.filter(Objects::nonNull).toList()));
		if (nearest == null)
			return;

		Vector toward = nearest.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize();
		toward.multiply(0.8).setY(0.4);
		mob.setVelocity(toward);

		showFormationEffect(mob.getLocation(), Particle.FLAME);
	}

	public void circlePlayer(Mob mob, double radius) {
		Player nearest = instance.getNetherrackGeneratorHandler().getClosestPlayer(mob.getLocation(),
				new ArrayList<>(instance.getStorageManager().getOnlineUsers().stream().map(UserData::getPlayer)
						.filter(Objects::nonNull).toList()));
		if (nearest == null)
			return;

		Vector dir = nearest.getLocation().toVector().subtract(mob.getLocation().toVector());
		Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(radius / 6);
		side.setY(0.2);
		mob.setVelocity(side);

		showFormationEffect(mob.getLocation(), Particle.CAMPFIRE_COSY_SMOKE);
	}

	public void holdCurrentPosition(Mob mob) {
		mob.setAI(false);
		instance.getScheduler().sync().runLater(() -> mob.setAI(true), 40L, mob.getLocation()); // pause for 2s

		showFormationEffect(mob.getLocation(), Particle.NOTE);
	}

	public void followLeader(Mob mob, CustomInvasion invasion) {
		Mob leader = invasion.getLeader();
		if (leader == null || leader.equals(mob))
			return;

		Vector path = leader.getLocation().toVector().subtract(mob.getLocation().toVector()).normalize().multiply(0.3);
		path.setY(0.2);
		mob.setVelocity(path);

		showFormationEffect(mob.getLocation(), Particle.CRIT);
	}

	public void showFormationRing(Location center, double radius) {
		World world = center.getWorld();
		for (int i = 0; i < 16; i++) {
			double angle = 2 * Math.PI * i / 16;
			double x = center.getX() + radius * Math.cos(angle);
			double z = center.getZ() + radius * Math.sin(angle);
			world.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(world, x, center.getY(), z), 1, 0, 0.1, 0, 0.01);
		}
	}

	private void showFormationEffect(Location loc, Particle particle) {
		loc.getWorld().spawnParticle(particle, loc.clone().add(0, 1.2, 0), 8, 0.2, 0.4, 0.2, 0.01);
	}

	public void queueBehavior(Mob mob, BehaviorCommand command) {
		behaviorQueue.enqueue(mob, command);
	}

	public enum FormationType {
		LINE, TRIANGLE, CIRCLE, CLUSTER;

		private static final FormationType[] VALUES = values();

		public static FormationType random() {
			return VALUES[RandomUtils.generateRandomInt(VALUES.length)];
		}

		public static FormationType fromDifficulty(InvasionDifficulty diff) {
			return switch (diff) {
			case EMBER, ASHEN, INFERNAL -> FormationType.LINE;
			case HELLFIRE, ABYSSAL, NETHERBORN -> FormationType.CIRCLE;
			case APOCALYPTIC, OBLIVION -> FormationType.TRIANGLE;
			case INFERNUM, GEHENNA -> FormationType.CLUSTER;
			};
		}
	}

	public enum InvasionBehavior {
		DEFAULT, // fallback behavior
		AGGRESSIVE, // charge nearest player
		FLANKER, // move sideways
		SUPPORT, // buffs nearby mobs
		DEFENSIVE; // retreat or reposition
	}
}