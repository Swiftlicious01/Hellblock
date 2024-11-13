package com.swiftlicious.hellblock.effects;

import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.Particle;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.listeners.fishing.HookCheckTimerTask;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;

/**
 * A task responsible for creating a lava effect animation between two points.
 */
public class LavaEffectTask implements Runnable {

	private final Location startLoc;
	private final Location endLoc;
	private final Location controlLoc;
	private int timer;
	private final SchedulerTask lavaTask;
	private final HookCheckTimerTask hookCheckTimerTask;

	/**
	 * Constructs a new LavaEffectTask.
	 *
	 * @param hookCheckTimerTask The HookCheckTimerTask instance.
	 * @param location           The starting location for the lava effect.
	 * @param delay              The delay before starting the task.
	 */
	public LavaEffectTask(HookCheckTimerTask hookCheckTimerTask, Location location, int delay) {
		this.hookCheckTimerTask = hookCheckTimerTask;
		this.startLoc = location.clone().add(0, 0.3, 0);
		this.endLoc = this.startLoc.clone().add((Math.random() * 16 - 8), startLoc.getY(), (Math.random() * 16 - 8));
		this.controlLoc = new Location(startLoc.getWorld(),
				(startLoc.getX() + endLoc.getX()) / 2 + Math.random() * 12 - 6, startLoc.getY(),
				(startLoc.getZ() + endLoc.getZ()) / 2 + Math.random() * 12 - 6);
		this.lavaTask = HellblockPlugin.getInstance().getScheduler().asyncRepeating(this, delay * 50L, 50,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void run() {
		timer++;
		if (timer > 60) {
			lavaTask.cancel();
			HellblockPlugin.getInstance().getScheduler().executeSync(hookCheckTimerTask::getHooked, startLoc);
		} else {
			double t = (double) timer / 60;
			Location particleLoc = endLoc.clone().multiply(Math.pow((1 - t), 2))
					.add(controlLoc.clone().multiply(2 * t * (1 - t))).add(startLoc.clone().multiply(Math.pow(t, 2)));
			particleLoc.setY(startLoc.getY());
			startLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * Cancels the lava effect task.
	 */
	public void cancel() {
		if (!isCancelled())
			lavaTask.cancel();
	}

	/**
	 * Checks if the lava effect task is cancelled.
	 *
	 * @return True if the task is cancelled, false otherwise.
	 */
	public boolean isCancelled() {
		return lavaTask != null;
	}
}