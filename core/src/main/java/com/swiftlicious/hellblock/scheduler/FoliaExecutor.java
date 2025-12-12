package com.swiftlicious.hellblock.scheduler;

import java.lang.reflect.Method;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class FoliaExecutor implements RegionExecutor<Location, World> {

	private final Plugin plugin;

	private static final Object GLOBAL_SCHEDULER;
	private static final Object REGION_SCHEDULER;

	private static final Method GLOBAL_EXECUTE;
	private static final Method REGION_EXECUTE_WORLD;
	private static final Method REGION_EXECUTE_LOCATION;

	private static final Method GLOBAL_RUN;
	private static final Method GLOBAL_RUN_DELAYED;
	private static final Method GLOBAL_RUN_AT_FIXED;

	private static final Method REGION_RUN;
	private static final Method REGION_RUN_DELAYED;
	private static final Method REGION_RUN_AT_FIXED;

	private static final Method TASK_CANCEL;
	private static final Method TASK_IS_CANCELLED;

	static {
		Object global = null, region = null;
		Method gExec = null, rExecWorld = null, rExecLoc = null;
		Method gRun = null, gRunDelayed = null, gRunFixed = null;
		Method rRun = null, rRunDelayed = null, rRunFixed = null;
		Method cancel = null, isCancelled = null;

		try {
			// Schedulers
			global = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
			region = Bukkit.class.getMethod("getRegionScheduler").invoke(null);

			// Scheduler classes
			Class<?> globalClass = global.getClass();
			Class<?> regionClass = region.getClass();

			Class<?> pluginClass = Plugin.class;
			Class<?> runnableClass = Runnable.class;
			Class<?> worldClass = World.class;
			Class<?> locationClass = Location.class;
			Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");

			// Region scheduler (location-based)
			rExecLoc = regionClass.getMethod("execute", pluginClass, locationClass, runnableClass);
			rExecWorld = regionClass.getMethod("execute", pluginClass, worldClass, int.class, int.class, runnableClass);

			rRun = regionClass.getMethod("run", pluginClass, locationClass, Runnable.class);
			rRunDelayed = regionClass.getMethod("runDelayed", pluginClass, locationClass, Runnable.class, long.class);
			rRunFixed = regionClass.getMethod("runAtFixedRate", pluginClass, locationClass, Runnable.class, long.class,
					long.class);

			// Global scheduler
			gExec = globalClass.getMethod("execute", pluginClass, runnableClass);

			gRun = globalClass.getMethod("run", pluginClass, Runnable.class);
			gRunDelayed = globalClass.getMethod("runDelayed", pluginClass, Runnable.class, long.class);
			gRunFixed = globalClass.getMethod("runAtFixedRate", pluginClass, Runnable.class, long.class, long.class);

			// Task control
			cancel = scheduledTaskClass.getMethod("cancel");
			isCancelled = scheduledTaskClass.getMethod("isCancelled");

		} catch (Throwable t) {
			// Ignore; will be null = unsupported
		}

		GLOBAL_SCHEDULER = global;
		REGION_SCHEDULER = region;

		GLOBAL_EXECUTE = gExec;
		REGION_EXECUTE_LOCATION = rExecLoc;
		REGION_EXECUTE_WORLD = rExecWorld;

		GLOBAL_RUN = gRun;
		GLOBAL_RUN_DELAYED = gRunDelayed;
		GLOBAL_RUN_AT_FIXED = gRunFixed;

		REGION_RUN = rRun;
		REGION_RUN_DELAYED = rRunDelayed;
		REGION_RUN_AT_FIXED = rRunFixed;

		TASK_CANCEL = cancel;
		TASK_IS_CANCELLED = isCancelled;
	}

	public FoliaExecutor(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run(Runnable r, Location l) {
		Optional.ofNullable(l).ifPresentOrElse(loc -> {
			if (REGION_EXECUTE_LOCATION != null) {
				try {
					REGION_EXECUTE_LOCATION.invoke(REGION_SCHEDULER, plugin, loc, r);
				} catch (Exception ignored) {
				}
			}
		}, () -> {
			if (GLOBAL_EXECUTE != null) {
				try {
					GLOBAL_EXECUTE.invoke(GLOBAL_SCHEDULER, plugin, r);
				} catch (Exception ignored) {
				}
			}
		});
	}

	@Override
	public void run(Runnable r, World world, int x, int z) {
		if (REGION_EXECUTE_WORLD != null) {
			try {
				REGION_EXECUTE_WORLD.invoke(REGION_SCHEDULER, plugin, world, x, z, r);
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public SchedulerTask runLater(Runnable r, long delayTicks, Location l) {
		try {
			if (l == null) {
				if (delayTicks == 0 && GLOBAL_RUN != null) {
					Object task = GLOBAL_RUN.invoke(GLOBAL_SCHEDULER, plugin,
							(java.util.function.Consumer<Object>) scheduledTask -> r.run());
					return new FoliaCancellable(task);
				} else if (GLOBAL_RUN_DELAYED != null) {
					Object task = GLOBAL_RUN_DELAYED.invoke(GLOBAL_SCHEDULER, plugin,
							(java.util.function.Consumer<Object>) scheduledTask -> r.run(), delayTicks);
					return new FoliaCancellable(task);
				}
			} else {
				if (delayTicks == 0 && REGION_RUN != null) {
					Object task = REGION_RUN.invoke(REGION_SCHEDULER, plugin, l,
							(java.util.function.Consumer<Object>) scheduledTask -> r.run());
					return new FoliaCancellable(task);
				} else if (REGION_RUN_DELAYED != null) {
					Object task = REGION_RUN_DELAYED.invoke(REGION_SCHEDULER, plugin, l,
							(java.util.function.Consumer<Object>) scheduledTask -> r.run(), delayTicks);
					return new FoliaCancellable(task);
				}
			}
		} catch (Exception ignored) {
		}

		return null;
	}

	@Override
	public SchedulerTask runRepeating(Runnable r, long delayTicks, long period, Location l) {
		try {
			if (l == null && GLOBAL_RUN_AT_FIXED != null) {
				Object task = GLOBAL_RUN_AT_FIXED.invoke(GLOBAL_SCHEDULER, plugin,
						(java.util.function.Consumer<Object>) scheduledTask -> r.run(), delayTicks, period);
				return new FoliaCancellable(task);
			}
			if (l != null && REGION_RUN_AT_FIXED != null) {
				Object task = REGION_RUN_AT_FIXED.invoke(REGION_SCHEDULER, plugin, l,
						(java.util.function.Consumer<Object>) scheduledTask -> r.run(), delayTicks, period);
				return new FoliaCancellable(task);
			}
		} catch (Exception ignored) {
		}

		return null;
	}

	/**
	 * Wraps Paper's ScheduledTask using reflection to remain Spigot-compatible.
	 */
	public static class FoliaCancellable implements SchedulerTask {

		private final Object task;

		public FoliaCancellable(Object task) {
			this.task = task;
		}

		@Override
		public void cancel() {
			if (TASK_CANCEL != null) {
				try {
					TASK_CANCEL.invoke(task);
				} catch (Exception ignored) {
				}
			}
		}

		@Override
		public boolean isCancelled() {
			if (TASK_IS_CANCELLED != null) {
				try {
					return (boolean) TASK_IS_CANCELLED.invoke(task);
				} catch (Exception ignored) {
				}
			}
			return true;
		}
	}
}