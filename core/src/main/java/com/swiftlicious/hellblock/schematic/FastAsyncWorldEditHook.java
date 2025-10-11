package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.player.mailbox.MailboxFlag;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.SchematicManager.SpawnSearchMode;

public class FastAsyncWorldEditHook implements SchematicPaster {

	private static final Map<File, ClipboardFormat> cachedClipboardFormat = new ConcurrentHashMap<>();
	private static final long PASTE_TIMEOUT_TICKS = 200L; // 10 seconds (20 ticks/second)

	private final Map<UUID, SchedulerTask> runningPastes = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> pasteProgress = new ConcurrentHashMap<>(); // percentage (0–100)
	private final Map<UUID, Integer> totalBlocks = new ConcurrentHashMap<>(); // total block count
	private final Map<UUID, SchedulerTask> pasteTimeouts = new ConcurrentHashMap<>();
	private final Map<UUID, SchedulerTask> pasteTimers = new ConcurrentHashMap<>();

	protected final HellblockPlugin instance;

	public FastAsyncWorldEditHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	@SuppressWarnings("deprecation")
	public static boolean isWorking() {
		try {
			PluginManager pluginManager = Bukkit.getPluginManager();

			boolean faweEnabled = pluginManager.isPluginEnabled("FastAsyncWorldEdit");
			boolean weEnabled = pluginManager.isPluginEnabled("WorldEdit");

			if (faweEnabled) {
				Plugin fawe = pluginManager.getPlugin("FastAsyncWorldEdit");
				String faweVersion = fawe != null ? fawe.getDescription().getVersion() : "unknown";
				HellblockPlugin.getInstance().getPluginLogger().info("FAWE is enabled. Version: " + faweVersion);
				return true;
			}

			if (weEnabled) {
				Plugin worldEdit = pluginManager.getPlugin("WorldEdit");
				String weVersion = worldEdit != null ? worldEdit.getDescription().getVersion() : "unknown";
				HellblockPlugin.getInstance().getPluginLogger().info("WorldEdit is enabled. Version: " + weVersion);

				// Optional: additional internal checks
				final WorldEdit we = com.sk89q.worldedit.WorldEdit.getInstance();
				final Platform platform = we.getPlatformManager().queryCapability(Capability.WORLD_EDITING);
				return platform != null && platform.getDataVersion() != -1;
			}

			HellblockPlugin.getInstance().getPluginLogger().warn("Neither WorldEdit nor FAWE is enabled.");

		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger()
					.warn("Error checking WorldEdit/FAWE status. Possible version/API conflict: " + t.getMessage());
		}

		return false;
	}

	@Override
	public CompletableFuture<Location> pasteHellblock(UUID playerId, File file, Location location,
			boolean ignoreAirBlock, SchematicMetadata metadata, boolean animated) {

		CompletableFuture<Location> future = new CompletableFuture<>();
		long startTime = System.currentTimeMillis(); // ← Track elapsed time

		try {
			final ClipboardFormat format = cachedClipboardFormat.getOrDefault(file,
					ClipboardFormats.findByPath(file.toPath()));
			final Clipboard clipboard;
			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				clipboard = reader.read();
			}

			int width = getSchematicWidth(file);
			int height = getSchematicHeight(file);
			int length = getSchematicLength(file);

			final int offsetX = width / 2;
			final int offsetY = height / 2;
			final int offsetZ = length / 2;

			Location pasteLocation = location.clone().subtract(offsetX, offsetY, offsetZ);
			clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());

			instance.getScheduler().executeAsync(() -> {
				try (EditSession editSession = WorldEdit.getInstance()
						.newEditSession(BukkitAdapter.adapt(location.getWorld()))) {

					if (animated) {
						List<BaseBlockWithLocation> blockQueue = new ArrayList<>();
						Region region = clipboard.getRegion();
						BlockVector3 origin = region.getMinimumPoint();

						for (BlockVector3 pos : region) {
							BaseBlock block = clipboard.getFullBlock(pos);
							if (!ignoreAirBlock || !block.getBlockType().getMaterial().isAir()) {
								blockQueue.add(new BaseBlockWithLocation(block, pos.subtract(origin)));
							}
						}

						int blockCount = countBlocksInClipboard(clipboard, ignoreAirBlock);
						long durationTicks = (blockCount / 10L) + 20L;

						Player player = Bukkit.getPlayer(playerId);
						if (player != null) {
							instance.getIslandGenerator().startSchematicCameraAnimation(player, pasteLocation,
									durationTicks, true);
						}

						pasteBlocksProgressively(playerId, blockQueue, editSession, pasteLocation, future, startTime,
								width, height, length, metadata);
					} else {
						Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
								.to(BlockVector3.at(pasteLocation.getBlockX(), pasteLocation.getBlockY(),
										pasteLocation.getBlockZ()))
								.copyEntities(true).ignoreAirBlocks(ignoreAirBlock).build();

						Operations.complete(operation);
						Operations.complete(editSession.commit());

						// Spawn logic (now unified)
						instance.getScheduler().executeSync(() -> {
							Location spawn = (metadata.getHome() != null)
									? pasteLocation.clone().add(metadata.getHome())
									: instance.getSchematicManager().findSafeSpawn(location.getWorld(), pasteLocation,
											width, height, length, SpawnSearchMode.CENTER);

							totalBlocks.put(playerId, 1);
							pasteProgress.put(playerId, 100);
							runningPastes.remove(playerId);
							future.complete(spawn);
						});
					}

					cachedClipboardFormat.putIfAbsent(file, format);

				} catch (WorldEditException ex) {
					instance.getPluginLogger().severe("FastAsyncWorldEdit paste failed. (WorldEdit API only).", ex);
					future.completeExceptionally(ex);
				} finally {
					SchedulerTask timeout = pasteTimeouts.remove(playerId);
					if (timeout != null)
						timeout.cancel();

					SchedulerTask timer = pasteTimers.remove(playerId);
					if (timer != null)
						timer.cancel();

					if (animated) {
						Player player = Bukkit.getPlayer(playerId);
						if (player != null)
							instance.getIslandGenerator().cleanupAnimation(player);
						else
							instance.getMailboxManager().queueMailbox(playerId, MailboxFlag.RESET_ANIMATION);
					}
				}
			});

		} catch (IOException ex) {
			instance.getPluginLogger().severe("Failed to load schematic file.", ex);
			future.completeExceptionally(ex);
			return future;
		}

		// === Timeout safeguard ===
		SchedulerTask timeoutTask = instance.getScheduler().sync().runLater(() -> {
			if (!future.isDone()) {
				pasteProgress.remove(playerId);
				totalBlocks.remove(playerId);
				runningPastes.remove(playerId);
				pasteTimers.remove(playerId);

				future.completeExceptionally(new TimeoutException("Schematic paste timed out after 10 seconds."));
				instance.getPluginLogger()
						.warn("FastAsyncWorldEdit schematic paste for player " + playerId + " timed out.");
			}
		}, PASTE_TIMEOUT_TICKS, location);
		pasteTimeouts.put(playerId, timeoutTask);

		runningPastes.put(playerId, new SchedulerTask() {
			@Override
			public void cancel() {
				future.cancel(false);
				SchedulerTask timer = pasteTimers.remove(playerId);
				if (timer != null)
					timer.cancel();
			}

			@Override
			public boolean isCancelled() {
				return future.isCancelled();
			}
		});

		return future;
	}

	private void pasteBlocksProgressively(UUID playerId, List<BaseBlockWithLocation> blocks, EditSession editSession,
			Location pasteLocation, CompletableFuture<Location> future, long startTime, int width, int height,
			int length, SchematicMetadata metadata) {
		Iterator<BaseBlockWithLocation> iterator = blocks.iterator();
		int total = blocks.size();
		int[] placed = { 0 };

		final SchedulerTask[] taskRef = new SchedulerTask[1];

		taskRef[0] = instance.getScheduler().sync().runRepeating(() -> {
			Player player = Bukkit.getPlayer(playerId);

			for (int i = 0; i < 10 && iterator.hasNext(); i++) {
				BaseBlockWithLocation entry = iterator.next();
				BlockVector3 abs = BlockVector3.at(pasteLocation.getBlockX() + entry.relative.x(),
						pasteLocation.getBlockY() + entry.relative.y(), pasteLocation.getBlockZ() + entry.relative.z());

				try {
					editSession.setBlock(abs, entry.block);
				} catch (WorldEditException e) {
					instance.getPluginLogger().warn("Failed to paste block during FastAsyncWorldEdit animation.", e);
				}

				if (player != null) {
					Location worldLoc = new Location(pasteLocation.getWorld(), abs.x(), abs.y(), abs.z());
					player.spawnParticle(Particle.BLOCK_CRUMBLE, worldLoc, 5,
							entry.block.toBaseBlock().getBlockType().getMaterial());
					AdventureHelper.playPositionalSound(player.getWorld(), worldLoc, "minecraft:block.stone.place",
							0.5f, 1.2f);
				}

				placed[0]++;
			}

			if (player != null && placed[0] < total) {
				int percent = (int) (((double) placed[0] / total) * 100);
				int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);
				VersionHelper.getNMSManager()
						.sendActionBar(player,
								AdventureHelper.componentToJson(instance.getTranslationManager()
										.render(MessageConstants.MSG_HELLBLOCK_SCHEMATIC_PROGRESS_BAR
												.arguments(AdventureHelper.miniMessage(String.valueOf(percent)),
														AdventureHelper.miniMessage(String.valueOf(elapsed)))
												.build())));
			}

			if (!iterator.hasNext()) {
				try {
					Operations.complete(editSession.commit());
				} catch (WorldEditException e) {
					instance.getPluginLogger().severe("Error finalizing FastAsyncWorldEdit paste operation.", e);
					future.completeExceptionally(e);
					return;
				}

				taskRef[0].cancel();
				totalBlocks.put(playerId, placed[0]);
				pasteProgress.put(playerId, 100);
				runningPastes.remove(playerId);

				// === Unified spawn logic ===
				Vector treeVec = metadata.getTree();
				Location treeLoc = treeVec != null ? treeVec.toLocation(pasteLocation.getWorld())
						: pasteLocation.clone().add(width / 2.0, 0, length / 2.0);

				Optional<TreeAnimationData> treeDataOpt = scanGlowstoneTree(treeLoc, 5);

				if (treeDataOpt.isPresent()) {
					TreeAnimationData treeData = treeDataOpt.get();

					totalBlocks.put(playerId,
							totalBlocks.getOrDefault(playerId, 0) + treeData.getStagedBlocks().size());

					animateTreeFromScanned(playerId, pasteLocation.getWorld(), treeData, editSession, future,
							pasteLocation, metadata, width, height, length);
				} else {
					instance.getScheduler().executeSync(() -> {
						Location spawn = (metadata.getHome() != null) ? pasteLocation.clone().add(metadata.getHome())
								: instance.getSchematicManager().findSafeSpawn(pasteLocation.getWorld(), pasteLocation,
										width, height, length, SpawnSearchMode.CENTER);
						future.complete(spawn);
					});
				}
			}
		}, 1L, 1L, pasteLocation);
	}

	public Optional<TreeAnimationData> scanGlowstoneTree(@NotNull Location center, int radius) {
		Set<Block> visited = new HashSet<>();
		Queue<Block> queue = new ArrayDeque<>();

		Block base = center.getBlock();
		if (!isGlowstoneTreeBlock(base))
			return Optional.empty();

		queue.add(base);

		while (!queue.isEmpty()) {
			Block current = queue.poll();
			if (!visited.add(current))
				continue;

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1)
							continue;

						Block neighbor = current.getRelative(dx, dy, dz);
						if (neighbor.getLocation().distance(center) > radius)
							continue;

						if (!visited.contains(neighbor) && isGlowstoneTreeBlock(neighbor)) {
							queue.add(neighbor);
						}
					}
				}
			}
		}

		if (visited.isEmpty())
			return Optional.empty();
		return Optional.of(new TreeAnimationData(visited));
	}

	public CompletableFuture<Void> animateTreeFromScanned(@NotNull UUID playerId, @NotNull World world,
			@NotNull TreeAnimationData treeData, @NotNull EditSession editSession,
			@NotNull CompletableFuture<Location> future, @NotNull Location pasteLocation,
			@NotNull SchematicMetadata metadata, int width, int height, int length) {
		return CompletableFuture.runAsync(() -> {
			List<List<BlockVector3>> stages = new ArrayList<>(treeData.getStagedBlocks().values());
			AtomicInteger step = new AtomicInteger();

			Location topLocation = BukkitAdapter.adapt(world, treeData.getTopBlock()).add(0.5, 0.5, 0.5);

			BukkitRunnable animationTask = new BukkitRunnable() {
				@Override
				public void run() {
					int currentStep = step.getAndIncrement();

					if (currentStep >= stages.size()) {
						world.spawnParticle(Particle.CLOUD, topLocation, 30, 0.4, 0.3, 0.4, 0.02);
						world.spawnParticle(Particle.END_ROD, topLocation, 20, 0.2, 0.3, 0.2, 0.01);
						world.strikeLightningEffect(topLocation);
						AdventureHelper.playPositionalSound(world, topLocation,
								"minecraft:entity.lightning_bolt.thunder", 0.8f, 1.0f);

						instance.getScheduler().executeSync(() -> {
							Location spawn = metadata.getHome() != null ? pasteLocation.clone().add(metadata.getHome())
									: instance.getSchematicManager().findSafeSpawn(pasteLocation.getWorld(),
											pasteLocation, width, height, length, SpawnSearchMode.CENTER);
							future.complete(spawn);
						});

						cancel();
						return;
					}

					stages.get(currentStep).forEach(vec -> {
						Block block = world.getBlockAt(vec.x(), vec.y(), vec.z());
						Material mat = block.getType();

						try {
							editSession.setBlock(vec, BukkitAdapter.asBlockType(mat).getDefaultState());
						} catch (WorldEditException e) {
							instance.getPluginLogger().warn("Failed to set tree block", e);
						}

						Location loc = block.getLocation().add(0.5, 0.5, 0.5);
						world.spawnParticle(Particle.END_ROD, loc, 8, 0.1, 0.1, 0.1, 0.01);
						AdventureHelper.playPositionalSound(world, loc, "minecraft:block.amethyst_block.hit", 0.6f,
								1.4f);
					});

					pasteProgress.compute(playerId, (k, v) -> {
						int progress = (int) (((double) (currentStep + 1) / stages.size()) * 100);
						return Math.min(progress, 100);
					});
				}
			};

			SchedulerTask task = instance.getScheduler().sync().runRepeating(animationTask, 0L, 3L, topLocation);

			UUID treeAnimId = UUID
					.nameUUIDFromBytes(("tree-" + topLocation.toString()).getBytes(StandardCharsets.UTF_8));
			instance.getIslandGenerator().trackAnimation(treeAnimId, task);
		});
	}

	private boolean isGlowstoneTreeBlock(Block block) {
		Material mat = block.getType();
		return mat == Material.GLOWSTONE || mat == Material.GRAVEL;
	}

	public int countBlocksInClipboard(Clipboard clipboard, boolean ignoreAirBlocks) {
		Region region = clipboard.getRegion();
		int count = 0;

		for (BlockVector3 pos : region) {
			BaseBlock block = clipboard.getFullBlock(pos);
			if (!ignoreAirBlocks || !block.getBlockType().getMaterial().isAir()) {
				count++;
			}
		}
		return count;
	}

	public Vector getCachedDimensions(File file) {
		return instance.getSchematicManager().schematicDimensions.computeIfAbsent(file, f -> {
			try (ClipboardReader reader = ClipboardFormats.findByPath(f.toPath()).getReader(new FileInputStream(f))) {
				Clipboard clipboard = reader.read();
				return new Vector(clipboard.getDimensions().x(), clipboard.getDimensions().y(),
						clipboard.getDimensions().z());
			} catch (IOException e) {
				instance.getPluginLogger().warn("Failed to read dimensions for schematic: " + f.getName(), e);
				return new Vector(0, 0, 0);
			}
		});
	}

	public int getSchematicWidth(File file) {
		Vector dims = getCachedDimensions(file);
		return dims.getBlockX();
	}

	public int getSchematicHeight(File file) {
		Vector dims = getCachedDimensions(file);
		return dims.getBlockY();
	}

	public int getSchematicLength(File file) {
		Vector dims = getCachedDimensions(file);
		return dims.getBlockZ();
	}

	@Override
	public boolean cancelPaste(UUID playerId) {
		SchedulerTask task = runningPastes.remove(playerId);
		SchedulerTask timeout = pasteTimeouts.remove(playerId);
		SchedulerTask timer = pasteTimers.remove(playerId);
		pasteProgress.remove(playerId);
		totalBlocks.remove(playerId);

		if (timeout != null)
			timeout.cancel();
		if (timer != null)
			timer.cancel();
		if (task != null) {
			task.cancel();
			return true;
		}
		return false;
	}

	@Override
	public int getPasteProgress(UUID playerId) {
		return pasteProgress.getOrDefault(playerId, 0);
	}

	@Override
	public void clearCache() {
		cachedClipboardFormat.clear();
	}

	public class TreeAnimationData {
		private final Map<Integer, List<BlockVector3>> stagedBlocks = new TreeMap<>();
		private final BlockVector3 topBlock;

		public TreeAnimationData(Set<Block> treeBlocks) {
			BlockVector3 highest = null;

			for (Block b : treeBlocks) {
				BlockVector3 vec = BlockVector3.at(b.getX(), b.getY(), b.getZ());
				stagedBlocks.computeIfAbsent(vec.y(), __ -> new ArrayList<>()).add(vec);

				if (highest == null || vec.y() > highest.y()) {
					highest = vec;
				}
			}

			this.topBlock = highest;
		}

		public Map<Integer, List<BlockVector3>> getStagedBlocks() {
			return stagedBlocks;
		}

		public BlockVector3 getTopBlock() {
			return topBlock;
		}
	}

	private static class BaseBlockWithLocation {
		final BaseBlock block;
		final BlockVector3 relative;

		public BaseBlockWithLocation(BaseBlock block, BlockVector3 relative) {
			this.block = block;
			this.relative = relative;
		}
	}
}