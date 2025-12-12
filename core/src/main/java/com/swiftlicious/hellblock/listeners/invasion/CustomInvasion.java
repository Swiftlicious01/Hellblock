package com.swiftlicious.hellblock.listeners.invasion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.listeners.invasion.InvasionFormation.FormationType;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

public class CustomInvasion {

	private final int islandId;
	private final UUID ownerUUID;
	private final World world;
	private final BoundingBox bounds;
	private final InvasionProfile profile;
	private final Queue<UUID> mobIds = new ConcurrentLinkedQueue<>();
	private final AtomicInteger mobCount = new AtomicInteger(0);
	private UUID bossId;
	private final Set<UUID> bossBarViewers = ConcurrentHashMap.newKeySet();
	private final Set<UUID> bossHealthViewers = ConcurrentHashMap.newKeySet();
	private final Map<UUID, InvasionMobTask> mobTasks = new ConcurrentHashMap<>();
	private final Map<UUID, UUID> piglinToMountMap = new ConcurrentHashMap<>();
	private SchedulerTask mobSynergyTask;
	private FormationType currentFormation;

	private UUID currentLeader;
	private UUID bossBarId;
	private UUID bossHealthBarId;
	private long startTime;
	private float bossBarProgress;

	public CustomInvasion(int islandId, UUID ownerUUID, World world, BoundingBox bounds, InvasionProfile profile) {
		this.islandId = islandId;
		this.ownerUUID = ownerUUID;
		this.world = world;
		this.bounds = bounds;
		this.profile = profile;
		this.startTime = System.currentTimeMillis();
		this.bossBarProgress = 1.0f; // full bar at start
	}

	public int getIslandId() {
		return islandId;
	}

	@NotNull
	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	@NotNull
	public World getWorld() {
		return world;
	}

	@NotNull
	public BoundingBox getBounds() {
		return bounds;
	}

	@NotNull
	public InvasionProfile getProfile() {
		return profile;
	}

	@Nullable
	public Mob getLeader() {
		Entity entity = Bukkit.getEntity(currentLeader);
		return (entity instanceof Mob m && m.isValid() && !m.isDead()) ? m : null;
	}

	public void setLeader(@Nullable Mob mob) {
		if (mob != null)
			this.currentLeader = mob.getUniqueId();
	}

	@Nullable
	public FormationType getCurrentFormation() {
		return currentFormation;
	}

	public void setCurrentFormation(@Nullable FormationType type) {
		this.currentFormation = type;
	}

	@NotNull
	public Queue<UUID> getMobIds() {
		return mobIds;
	}

	public void addWaveMobs(Collection<@Nullable UUID> ids) {
		ids.stream().filter(mob -> mob != null && mobIds.add(mob)).forEach(mob -> mobCount.incrementAndGet());
	}

	public void addWaveMob(@Nullable UUID mob) {
		if (mob != null && mobIds.remove(mob)) {
			mobCount.decrementAndGet();
		}
	}

	public void removeWaveMob(@Nullable UUID mob) {
		mobIds.remove(mob);
	}

	public boolean isBoss(@Nullable UUID id) {
		return bossId != null && bossId.equals(id);
	}

	public boolean isBossAlive() {
		if (bossId == null)
			return false;
		Entity boss = Bukkit.getEntity(bossId);
		return boss != null && boss.isValid() && !boss.isDead();
	}

	public int getRemainingWaveCount() {
		int count = mobCount.get();
		if (bossId != null && isBossAlive()) {
			count += 1;
		}
		return count;
	}

	public int getTotalSpawned() {
		int count = mobCount.get();
		if (bossId != null) {
			count += 1;
		}
		return count;
	}

	public Map<UUID, UUID> getMountMappings() {
		return piglinToMountMap;
	}

	public void addRetreatMountMapping(@NotNull UUID piglinId, @NotNull UUID mountId) {
		piglinToMountMap.put(piglinId, mountId);
	}

	@Nullable
	public UUID removeRetreatMountMapping(@NotNull UUID piglinId) {
		return piglinToMountMap.remove(piglinId);
	}

	@Nullable
	public UUID getMountId(@NotNull UUID piglinId) {
		return piglinToMountMap.get(piglinId);
	}

	@Nullable
	public UUID getRiderForMount(@NotNull UUID mountId) {
		return piglinToMountMap.entrySet().stream().filter(entry -> entry.getValue().equals(mountId)).findFirst()
				.map(Map.Entry::getKey).orElse(null);
	}

	public void removeRiderForMount(@NotNull UUID mountId) {
		Iterator<Map.Entry<UUID, UUID>> iterator = piglinToMountMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, UUID> entry = iterator.next();
			if (entry.getValue().equals(mountId)) {
				iterator.remove(); // Remove rider → mount mapping
				break;
			}
		}
	}

	@Nullable
	public UUID getBossId() {
		return bossId;
	}

	public void setBossId(@Nullable UUID bossId) {
		this.bossId = bossId;
	}

	@Nullable
	public UUID getBossBarId() {
		return bossBarId;
	}

	public void setBossBarId(@Nullable UUID bossBarId) {
		this.bossBarId = bossBarId;
	}

	@Nullable
	public UUID getBossHealthBarId() {
		return bossHealthBarId;
	}

	public void setBossHealthBarId(@Nullable UUID bossHealthBarId) {
		this.bossHealthBarId = bossHealthBarId;
	}

	@NotNull
	public Set<UUID> getBossBarViewers() {
		return bossBarViewers;
	}

	public void setBossBarViewers(@NotNull Set<UUID> newViewers) {
		bossBarViewers.clear();
		bossBarViewers.addAll(newViewers);
	}

	@NotNull
	public Set<UUID> getBossHealthViewers() {
		return bossHealthViewers;
	}

	public void setBossHealthViewers(@NotNull Set<UUID> newViewers) {
		bossHealthViewers.clear();
		bossHealthViewers.addAll(newViewers);
	}

	public long getStartTime() {
		return startTime;
	}

	public void resetStartTime() {
		this.startTime = System.currentTimeMillis();
	}

	public float getBossBarProgress() {
		return bossBarProgress;
	}

	public void setBossBarProgress(float progress) {
		this.bossBarProgress = Math.max(0f, Math.min(1f, progress));
	}

	public void addMobTask(@NotNull UUID mobId, @Nullable SchedulerTask targeting, @Nullable SchedulerTask goal) {
		mobTasks.put(mobId, new InvasionMobTask(mobId, targeting, goal));
	}

	@Nullable
	public SchedulerTask getMobTargetingTask(@NotNull UUID mobId) {
		InvasionMobTask task = mobTasks.get(mobId);
		return task != null ? task.targetingTask : null;
	}

	@Nullable
	public SchedulerTask getMobGoalTask(@NotNull UUID mobId) {
		InvasionMobTask task = mobTasks.get(mobId);
		return task != null ? task.goalTask : null;
	}

	public void cancelMobTargetingTask(@NotNull UUID mobId) {
		InvasionMobTask task = mobTasks.get(mobId);
		if (task != null)
			task.cancelTargeting();
	}

	public void cancelMobGoalTask(@NotNull UUID mobId) {
		InvasionMobTask task = mobTasks.get(mobId);
		if (task != null)
			task.cancelGoal();
	}

	public void cancelAllMobTasks() {
		mobTasks.values().forEach(InvasionMobTask::cancelAll);
		mobTasks.clear();
	}

	public void startSynergyTask(@Nullable SchedulerTask task) {
		mobSynergyTask = task;
	}

	@Nullable
	public SchedulerTask getSynergyTask() {
		return mobSynergyTask;
	}

	public void cancelSynergyTask() {
		if (mobSynergyTask != null && !mobSynergyTask.isCancelled()) {
			mobSynergyTask.cancel();
			mobSynergyTask = null;
		}
	}

	public void cleanup() {
		cancelAllMobTasks();
		cancelSynergyTask();
		getBossBarViewers().clear();
		getBossHealthViewers().clear();
		getMobIds().clear();
		mobCount.set(0);
		getMountMappings().clear();
		setBossId(null);
		setBossBarId(null);
		setBossHealthBarId(null);
		setCurrentFormation(null);
		setLeader(null);
	}

	class InvasionMobTask {
		UUID mobId;
		SchedulerTask targetingTask;
		SchedulerTask goalTask;

		public InvasionMobTask(@NotNull UUID mobId, @Nullable SchedulerTask targetingTask,
				@Nullable SchedulerTask goalTask) {
			this.mobId = mobId;
			this.targetingTask = targetingTask;
			this.goalTask = goalTask;
		}

		public void cancelAll() {
			cancelTargeting();
			cancelGoal();
		}

		public void cancelTargeting() {
			if (targetingTask != null && !targetingTask.isCancelled()) {
				targetingTask.cancel();
				targetingTask = null;
			}
		}

		public void cancelGoal() {
			if (goalTask != null && !goalTask.isCancelled()) {
				goalTask.cancel();
				goalTask = null;
			}
		}
	}
}