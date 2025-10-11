package com.swiftlicious.hellblock.listeners;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.api.Reloadable;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.utils.RandomUtils;

public class MinionHandler implements Listener, Reloadable {

	private static final String MINION_KEY = "enhanced_wither_minion";
	private final HellblockPlugin instance;
	private final NamespacedKey minionKey;

	// Active minions tracked
	private final Map<UUID, SchedulerTask> minionTargetTasks = new ConcurrentHashMap<>();

	public MinionHandler(HellblockPlugin plugin) {
		this.instance = plugin;
		this.minionKey = new NamespacedKey(plugin, MINION_KEY);
	}

	@Override
	public void load() {
		Bukkit.getPluginManager().registerEvents(this, instance);
	}

	@Override
	public void unload() {
		HandlerList.unregisterAll(this);
		// cancel all minion target tasks
		minionTargetTasks.values().stream().filter(task -> task != null).forEach(SchedulerTask::cancel);
		minionTargetTasks.clear();
	}

	/**
	 * Marks an entity as a Wither Minion
	 */
	public void tagAsMinion(@NotNull LivingEntity entity) {
		entity.getPersistentDataContainer().set(minionKey, PersistentDataType.STRING, MINION_KEY);
	}

	/**
	 * Checks if an entity is a Wither Minion
	 */
	public boolean isMinion(@NotNull Entity entity) {
		return entity.getPersistentDataContainer().has(minionKey, PersistentDataType.STRING);
	}

	/**
	 * Starts scheduled targeting for a minion
	 */
	public void startMinionTargeting(@NotNull Mob minion) {
		stopMinionTargeting(minion.getUniqueId());

		final long initialDelay = RandomUtils.generateRandomInt(0, 20);

		final SchedulerTask task = instance.getScheduler().sync().runRepeating(new Runnable() {
			private int ticksSinceLastCheck = 0;

			@Override
			public void run() {
				if (minion.isDead() || !minion.isValid()) {
					stopMinionTargeting(minion.getUniqueId());
					return;
				}

				final List<LivingEntity> nearby = minion.getWorld().getNearbyEntities(minion.getLocation(), 40, 20, 40)
						.stream().filter(e -> e instanceof LivingEntity).map(e -> (LivingEntity) e).toList();

				if (nearby.isEmpty()) {
					minion.setTarget(null);
					return;
				}

				final LivingEntity currentTarget = minion.getTarget();

				// --- Validate current target ---
				if (currentTarget != null) {
					if (!currentTarget.isValid() || currentTarget.isDead() || !nearby.contains(currentTarget)) {
						minion.setTarget(null); // stale
					} else if (currentTarget instanceof Player player) {
						instance.getHellblockHandler().getHellblockByWorld(minion.getWorld(), minion.getLocation())
								.thenAccept(islandData -> {
									if (islandData == null
											|| !islandData.getPartyPlusOwner().contains(player.getUniqueId())) {
										instance.getScheduler().executeSync(() -> minion.setTarget(null));
									}
								});
						return; // coop check deferred
					} else if (currentTarget instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm)) {
						return; // valid Hell Golem
					} else {
						minion.setTarget(null);
					}
				}

				ticksSinceLastCheck++;

				// Only re-check every 5s if target is still valid
				if (ticksSinceLastCheck < 100 && currentTarget != null && currentTarget.isValid()
						&& !currentTarget.isDead()) {
					return;
				}
				ticksSinceLastCheck = 0;

				final LivingEntity targetNow = currentTarget;

				// --- Pick a new target ---
				instance.getHellblockHandler().getHellblockByWorld(minion.getWorld(), minion.getLocation())
						.thenAccept(islandData -> {
							if (islandData == null) {
								instance.getScheduler().executeSync(() -> minion.setTarget(null));
								return;
							}

							final Set<UUID> coopMembers = islandData.getPartyPlusOwner();
							LivingEntity newTarget = null;

							// 1) Coop players
							final List<Player> coopNearby = nearby.stream().filter(
									e -> e instanceof Player player && coopMembers.contains(player.getUniqueId()))
									.map(e -> (Player) e).toList();
							if (!coopNearby.isEmpty()) {
								newTarget = RandomUtils.getRandomElement(coopNearby);
							}

							// 2) Hell Golems
							if (newTarget == null) {
								final List<Snowman> golemsNearby = nearby.stream().filter(
										e -> e instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm))
										.map(e -> (Snowman) e).toList();
								if (!golemsNearby.isEmpty()) {
									newTarget = RandomUtils.getRandomElement(golemsNearby);
								}
							}

							final LivingEntity finalNewTarget = newTarget;
							instance.getScheduler().executeSync(() -> {
								if (finalNewTarget != null) {
									// Stickiness: 80% chance to keep current
									if (targetNow != null && RandomUtils.generateRandomInt(1, 100) <= 80) {
										return;
									}
									if (finalNewTarget != minion.getTarget()) {
										minion.setTarget(finalNewTarget);
									}
								} else {
									minion.setTarget(null);
								}
							});
						});
			}
		}, initialDelay, 20L, minion.getLocation());

		minionTargetTasks.put(minion.getUniqueId(), task);
	}

	public void stopMinionTargeting(@NotNull UUID minionId) {
		final SchedulerTask task = minionTargetTasks.remove(minionId);
		if (task != null) {
			task.cancel();
		}
	}

	public void onMinionTarget(EntityTargetEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (!(event.getEntity() instanceof Mob mob)) {
			return;
		}
		if (!isMinion(mob)) {
			return; // only apply to minions
		}

		final Entity target = event.getTarget();
		if (target == null) {
			event.setCancelled(true);
			return;
		}

		// Hell Golems are always allowed
		if (target instanceof Snowman sm && instance.getGolemHandler().isHellGolem(sm)) {
			return;
		}

		// Players are allowed (coop check handled in scheduler)
		if (target instanceof Player) {
			return;
		}

		// Everything else → cancel
		event.setCancelled(true);
	}

	/**
	 * Cleanup when minion despawns
	 */
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (!instance.getHellblockHandler().isInCorrectWorld(event.getEntity().getWorld())) {
			return;
		}
		if (isMinion(event.getEntity())) {
			stopMinionTargeting(event.getEntity().getUniqueId());
		}
	}
}