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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.config.locale.MessageConstants;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.handlers.VersionHelper;
import com.swiftlicious.hellblock.scheduler.SchedulerTask;
import com.swiftlicious.hellblock.schematic.SchematicManager.SpawnSearchMode;
import com.swiftlicious.hellblock.utils.ParticleUtils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;

public class WorldEditHook implements SchematicPaster {

	private static final Map<File, ClipboardFormat> cachedClipboardFormat = new ConcurrentHashMap<>();
	private static final long PASTE_TIMEOUT_TICKS = 200L; // 10 seconds (20 ticks/second)

	private final Map<UUID, SchedulerTask> runningPastes = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> pasteProgress = new ConcurrentHashMap<>(); // percentage (0–100)
	private final Map<UUID, Integer> totalBlocks = new ConcurrentHashMap<>(); // total block count
	private final Map<UUID, SchedulerTask> pasteTimeouts = new ConcurrentHashMap<>();
	private final Map<UUID, SchedulerTask> pasteTimers = new ConcurrentHashMap<>();

	protected final HellblockPlugin instance;

	public WorldEditHook(HellblockPlugin plugin) {
		instance = plugin;
	}

	public static boolean isWorking() {
		try {
			final Platform platform = com.sk89q.worldedit.WorldEdit.getInstance().getPlatformManager()
					.queryCapability(Capability.WORLD_EDITING);
			final int liveDataVersion = platform.getDataVersion();
			return liveDataVersion != -1;
		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"WorldEdit threw an error during initializing, make sure it's updated and API compatible(FAWE isn't API compatible)",
					t);
			return false;
		}
	}

	@Override
	public CompletableFuture<Location> pasteHellblock(UUID playerId, File file, Location location,
			boolean ignoreAirBlock, SchematicMetadata metadata, boolean animated) {
		CompletableFuture<Location> future = new CompletableFuture<>();

		try {
			final ClipboardFormat format = cachedClipboardFormat.getOrDefault(file,
					ClipboardFormats.findByPath(file.toPath()));

			Clipboard original;
			Clipboard clipboard;

			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				original = reader.read();
				clipboard = cloneClipboard(original); // This may throw WorldEditException

				// Run replacements *inside* the try block so clipboard is valid
				replacePlaceholdersInClipboard(clipboard,
						Optional.ofNullable(Bukkit.getPlayer(playerId)).map(Player::getName)
								.orElse(instance.getTranslationManager()
										.miniMessageTranslation(MessageConstants.FORMAT_UNKNOWN.build().key())));

			} catch (WorldEditException e) {
				// Handle either file read or clone failure
				instance.getPluginLogger().severe("Failed to load and clone clipboard: " + file.getName(), e);
				throw new RuntimeException("Failed to prepare schematic", e); // or handle differently
			}

			int width = getSchematicWidth(file);
			int height = getSchematicHeight(file);
			int length = getSchematicLength(file);

			final int offsetX = width / 2;
			final int offsetY = height / 2;
			final int offsetZ = length / 2;

			Location pasteLocation = location.clone().subtract(offsetX, offsetY, offsetZ);
			clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());

			instance.getScheduler().executeSync(() -> {
				final SchedulerTask[] progressTaskRef = new SchedulerTask[1];
				final int[] elapsed = { 0 };

				try (EditSession editSession = WorldEdit.getInstance()
						.newEditSession(new BukkitWorld(location.getWorld()))) {

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

						int blockCount = blockQueue.size();
						long durationTicks = (blockCount / 10L) + 20L;

						Player player = Bukkit.getPlayer(playerId);
						if (player != null && player.isOnline()) {
							instance.getStorageManager().getOnlineUser(playerId).ifPresent(
									userData -> instance.getIslandGenerator().startSchematicCameraAnimation(userData,
											pasteLocation, durationTicks, true));
						}

						pasteProgress.put(playerId, 0);
						totalBlocks.put(playerId, blockCount);

						pasteBlocksProgressively(playerId, blockQueue, editSession, pasteLocation, future, () -> {
							Vector treeVec = metadata.getTree();
							Location treeLoc = treeVec != null ? treeVec.toLocation(pasteLocation.getWorld())
									: pasteLocation.clone().add(width / 2.0, 0, length / 2.0); // Try center if null

							Optional<TreeAnimationData> treeDataOpt = scanGlowstoneTree(treeLoc, 5); // radius 5 block
																										// scan
							if (treeDataOpt.isPresent()) {
								TreeAnimationData treeData = treeDataOpt.get();

								// Add synthetic stage
								totalBlocks.put(playerId,
										totalBlocks.getOrDefault(playerId, 0) + treeData.getStagedBlocks().size());

								animateTreeFromScanned(playerId, pasteLocation.getWorld(), treeData, editSession)
										.thenRun(() -> {
											pasteProgress.put(playerId, 100);
											completeWithSpawn(playerId, location, pasteLocation, metadata, width,
													height, length, future);
										}).exceptionally(ex -> {
											instance.getPluginLogger().severe("Tree animation failed", ex);
											future.completeExceptionally(ex);
											return null;
										});
							} else {
								completeWithSpawn(playerId, location, pasteLocation, metadata, width, height, length,
										future);
							}
						});

						progressTaskRef[0] = instance.getScheduler().sync().runRepeating(() -> {
							int percent = pasteProgress.getOrDefault(playerId, 0);
							if (!future.isDone()) {
								if (player != null && player.isOnline()) {
									VersionHelper.getNMSManager().sendActionBar(player,
											AdventureHelper.componentToJson(instance.getTranslationManager().render(
													MessageConstants.MSG_HELLBLOCK_SCHEMATIC_PROGRESS_BAR.arguments(
															AdventureHelper.miniMessageToComponent(String.format("%d%%", percent)),
															AdventureHelper.miniMessageToComponent(String.valueOf(elapsed[0])))
															.build())));
								}
							}
							elapsed[0]++;
						}, 20L, 20L, location);

					} else {
						Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
								.to(BlockVector3.at(pasteLocation.getBlockX(), pasteLocation.getBlockY(),
										pasteLocation.getBlockZ()))
								.copyEntities(true).ignoreAirBlocks(ignoreAirBlock).build();

						Operations.complete(operation);
						Operations.complete(editSession.commit());

						completeWithSpawn(playerId, location, pasteLocation, metadata, width, height, length, future);
					}

					cachedClipboardFormat.putIfAbsent(file, format);

				} catch (WorldEditException ex) {
					instance.getPluginLogger().severe("WorldEdit paste failed.", ex);
					future.completeExceptionally(ex);
				} finally {
					SchedulerTask timeout = pasteTimeouts.remove(playerId);
					if (timeout != null && !timeout.isCancelled())
						timeout.cancel();

					SchedulerTask timer = pasteTimers.remove(playerId);
					if (timer != null && !timer.isCancelled())
						timer.cancel();

					SchedulerTask progress = progressTaskRef[0];
					if (progress != null && !progress.isCancelled())
						progress.cancel();

					if (animated) {
						instance.getStorageManager().getCachedUserData(playerId)
								.ifPresent(userData -> instance.getIslandGenerator().cleanupAnimation(userData));
					}
				}
			}, location);

		} catch (IOException ex) {
			instance.getPluginLogger().severe("Failed to load schematic file.", ex);
			future.completeExceptionally(ex);
		}

		SchedulerTask timeoutTask = instance.getScheduler().sync().runLater(() -> {
			if (!future.isDone()) {
				pasteProgress.remove(playerId);
				totalBlocks.remove(playerId);
				runningPastes.remove(playerId);
				future.completeExceptionally(new TimeoutException("Schematic paste timed out after 10 seconds."));
				instance.getPluginLogger().warn("WorldEdit schematic paste for player " + playerId + " timed out.");
			}
		}, PASTE_TIMEOUT_TICKS, location);

		pasteTimeouts.put(playerId, timeoutTask);

		runningPastes.put(playerId, new SchedulerTask() {
			@Override
			public void cancel() {
				future.cancel(false);
			}

			@Override
			public boolean isCancelled() {
				return future.isCancelled();
			}
		});

		return future;
	}

	private Clipboard cloneClipboard(Clipboard original) throws WorldEditException {
		// Create a new clipboard with the same region and origin
		BlockArrayClipboard copy = new BlockArrayClipboard(original.getRegion());
		copy.setOrigin(original.getOrigin());

		// Perform a forward copy from original to the new clipboard
		ForwardExtentCopy copier = new ForwardExtentCopy(original, original.getRegion(), original.getOrigin(), copy,
				original.getOrigin());

		copier.setCopyingEntities(true); // optional: copy entities too
		copier.setCopyingBiomes(true); // optional: copy biomes too

		Operations.complete(copier);

		return copy;
	}

	private void replacePlaceholdersInClipboard(@NotNull Clipboard clipboard, @NotNull String ownerName) {
		Region region = clipboard.getRegion();

		for (BlockVector3 pos : region) {
			BlockState original = clipboard.getBlock(pos).toImmutableState();
			BaseBlock updated = null;

			// Try sign replacement
			BaseBlock signResult = replaceSignPlaceholders(original.toBaseBlock(), ownerName);
			if (signResult != null) {
				updated = signResult;
			} else {
				// Try lectern replacement
				BaseBlock lecternResult = replaceLecternPlaceholders(original.toBaseBlock(), ownerName);
				if (lecternResult != null) {
					updated = lecternResult;
				}
			}

			if (updated != null) {
				try {
					clipboard.setBlock(pos, updated);
				} catch (WorldEditException e) {
					// Log or handle the failure to apply changes
					instance.getPluginLogger().warn(
							"Failed to update block at " + pos + " with placeholder replacements: " + e.getMessage());
				}
			}
		}
	}

	@Nullable
	private BaseBlock replaceSignPlaceholders(@NotNull BaseBlock block, @NotNull String ownerName) {
		LinCompoundTag tag = block.getNbt();
		if (tag == null)
			return null;

		boolean changed = false;
		LinCompoundTag.Builder newTag = LinCompoundTag.builder();

		// 1.20+ support: dual-side sign format
		for (String sideKey : List.of("front_text", "back_text")) {
			LinCompoundTag sideText;
			try {
				sideText = tag.getTag(sideKey, LinTagType.compoundTag());
			} catch (NoSuchElementException e) {
				continue;
			}

			LinListTag<LinStringTag> messages;
			try {
				messages = sideText.getListTag("messages", LinTagType.stringTag());
			} catch (NoSuchElementException e) {
				newTag.put(sideKey, sideText);
				continue;
			}

			AtomicBoolean sideChanged = new AtomicBoolean(false);
			List<LinStringTag> updated = new ArrayList<>();

			messages.value().stream().map(LinStringTag::value).forEach(original -> {
				Component component = AdventureHelper.getGsonComponentSerializer().deserializeOr(original,
						Component.empty());
				Component replaced = component.replaceText(b -> b.matchLiteral("{player}").replacement(ownerName));
				String updatedJson = AdventureHelper.componentToJson(replaced);
				if (!updatedJson.equals(original))
					sideChanged.set(true);
				updated.add(LinStringTag.of(updatedJson));
			});

			if (sideChanged.get()) {
				changed = true;
				LinCompoundTag updatedSide = LinCompoundTag.builder()
						.put("messages", LinListTag.of(LinTagType.stringTag(), updated)).build();

				newTag.put(sideKey, updatedSide);
			} else {
				newTag.put(sideKey, sideText);
			}
		}

		// Legacy support (≤ 1.19.4): Text1 to Text4 format
		boolean legacyChanged = false;
		List<String> legacyLines = new ArrayList<>();

		for (int i = 1; i <= 4; i++) {
			String key = "Text" + i;
			String original;

			try {
				original = tag.getTag(key, LinTagType.stringTag()).value();
			} catch (NoSuchElementException e) {
				original = "";
			}

			Component component = AdventureHelper.getGsonComponentSerializer().deserializeOr(original,
					Component.empty());
			Component replaced = component.replaceText(b -> b.matchLiteral("{player}").replacement(ownerName));
			String updatedJson = AdventureHelper.componentToJson(replaced);

			if (!updatedJson.equals(original)) {
				legacyChanged = true;
			}

			legacyLines.add(updatedJson);
		}

		if (legacyChanged) {
			changed = true;
			for (int i = 1; i <= 4; i++) {
				newTag.put("Text" + i, LinStringTag.of(legacyLines.get(i - 1)));
			}
		} else {
			// Copy original legacy lines as-is
			for (int i = 1; i <= 4; i++) {
				String key = "Text" + i;
				try {
					LinStringTag originalLine = tag.getTag(key, LinTagType.stringTag());
					newTag.put(key, originalLine);
				} catch (NoSuchElementException ignored) {
					// Line doesn't exist — skip
				}
			}
		}

		if (!changed)
			return null;

		return block.getBlockType().getDefaultState().toBaseBlock(newTag.build());
	}

	@Nullable
	private BaseBlock replaceLecternPlaceholders(@NotNull BaseBlock block, @NotNull String ownerName) {
		LinCompoundTag tag = block.getNbt();
		if (tag == null)
			return null;

		LinCompoundTag bookTag;
		try {
			bookTag = tag.getTag("Book", LinTagType.compoundTag());
		} catch (NoSuchElementException e) {
			return null;
		}

		boolean changed = false;
		LinCompoundTag.Builder newBook = LinCompoundTag.builder();

		// Replace title
		try {
			LinStringTag titleTag = bookTag.getTag("title", LinTagType.stringTag());
			String original = titleTag.value();
			String updated = original.replace("{player}", ownerName);
			if (!updated.equals(original))
				changed = true;
			newBook.put("title", LinStringTag.of(updated));
		} catch (NoSuchElementException e) {
			// Skip if title is missing
		}

		// Replace author
		try {
			LinStringTag authorTag = bookTag.getTag("author", LinTagType.stringTag());
			String original = authorTag.value();
			String updated = original.replace("{player}", ownerName);
			if (!updated.equals(original))
				changed = true;
			newBook.put("author", LinStringTag.of(updated));
		} catch (NoSuchElementException e) {
			// Skip if author is missing
		}

		// Replace pages
		try {
			LinListTag<LinStringTag> pages = bookTag.getListTag("pages", LinTagType.stringTag());
			List<LinStringTag> updatedPages = new ArrayList<>();

			for (LinStringTag page : pages.value()) {
				String original = page.value();
				Component component = AdventureHelper.getGsonComponentSerializer().deserializeOr(original,
						Component.empty());
				Component replaced = component.replaceText(b -> b.matchLiteral("{player}").replacement(ownerName));
				String updatedJson = AdventureHelper.componentToJson(replaced);

				if (!updatedJson.equals(original))
					changed = true;

				updatedPages.add(LinStringTag.of(updatedJson));
			}

			newBook.put("pages", LinListTag.of(LinTagType.stringTag(), updatedPages));
		} catch (NoSuchElementException e) {
			// Skip if pages are missing
		}

		if (!changed)
			return null;

		// Build the final NBT tag
		LinCompoundTag updatedTag = LinCompoundTag.builder().put("Book", newBook.build()).build();

		return block.getBlockType().getDefaultState().toBaseBlock(updatedTag);
	}

	private void pasteBlocksProgressively(UUID playerId, List<BaseBlockWithLocation> blocks, EditSession editSession,
			Location pasteLocation, CompletableFuture<Location> future, Runnable onComplete) {

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
					instance.getPluginLogger().warn("Failed to paste block during WorldEdit animation.", e);
				}

				if (player != null && player.isOnline()) {
					Location worldLoc = new Location(pasteLocation.getWorld(), abs.x(), abs.y(), abs.z());

					player.spawnParticle(ParticleUtils.getParticle("BLOCK_DUST"), worldLoc, 5,
							entry.block.toBaseBlock().getBlockType().getMaterial());
					AdventureHelper.playPositionalSound(player.getWorld(), worldLoc,
							Sound.sound(Key.key("minecraft:block.stone.place"), Source.BLOCK, 0.5f, 1.2f));
				}

				placed[0]++;
			}

			pasteProgress.put(playerId, (int) ((placed[0] / (double) total) * 100));
			totalBlocks.put(playerId, total);

			if (!iterator.hasNext()) {
				try {
					Operations.complete(editSession.commit());
				} catch (WorldEditException e) {
					instance.getPluginLogger().severe("Error finalizing WorldEdit paste operation.", e);
					future.completeExceptionally(e);
					return;
				}

				if (taskRef[0] != null && !taskRef[0].isCancelled())
					taskRef[0].cancel();
				runningPastes.remove(playerId);

				onComplete.run();
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
			@NotNull TreeAnimationData treeData, @NotNull EditSession editSession) {
		return CompletableFuture.runAsync(() -> {
			List<List<BlockVector3>> stages = new ArrayList<>(treeData.getStagedBlocks().values());
			AtomicInteger step = new AtomicInteger();

			Location topLocation = BukkitAdapter.adapt(world, treeData.getTopBlock()).clone().add(0.5, 0.5, 0.5);

			BukkitRunnable animationTask = new BukkitRunnable() {
				@Override
				public void run() {
					int currentStep = step.getAndIncrement();

					if (currentStep >= stages.size()) {
						world.spawnParticle(Particle.CLOUD, topLocation, 30, 0.4, 0.3, 0.4, 0.02);
						world.spawnParticle(Particle.END_ROD, topLocation, 20, 0.2, 0.3, 0.2, 0.01);
						world.strikeLightningEffect(topLocation);
						AdventureHelper.playPositionalSound(world, topLocation, Sound
								.sound(Key.key("minecraft:entity.lightning_bolt.thunder"), Source.BLOCK, 0.8f, 1.0f));

						if (!isCancelled())
							cancel();
						return;
					}

					stages.get(currentStep).forEach(vec -> {
						Block block = world.getBlockAt(vec.x(), vec.y(), vec.z());
						Material type = block.getType(); // Retain existing material

						try {
							editSession.setBlock(vec, BukkitAdapter.asBlockType(type).getDefaultState());
						} catch (WorldEditException e) {
							instance.getPluginLogger().warn("Failed to set tree block", e);
						}

						Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
						world.spawnParticle(Particle.END_ROD, loc, 8, 0.1, 0.1, 0.1, 0.01);
						AdventureHelper.playPositionalSound(world, loc,
								Sound.sound(Key.key("minecraft:block.amethyst_block.hit"), Source.BLOCK, 0.6f, 1.4f));
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

	private void completeWithSpawn(UUID playerId, Location origin, Location pasteLocation, SchematicMetadata metadata,
			int width, int height, int length, CompletableFuture<Location> future) {

		Location spawn = metadata.getHome() != null ? pasteLocation.clone().add(metadata.getHome())
				: instance.getSchematicManager().findSafeSpawn(origin.getWorld(), pasteLocation, width, height, length,
						SpawnSearchMode.CENTER);

		totalBlocks.put(playerId, 1);
		pasteProgress.put(playerId, 100);
		runningPastes.remove(playerId);
		pasteProgress.remove(playerId);
		totalBlocks.remove(playerId);

		future.complete(spawn);
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
		pasteProgress.remove(playerId);
		totalBlocks.remove(playerId);

		if (timeout != null && !timeout.isCancelled())
			timeout.cancel();
		if (task != null && !task.isCancelled()) {
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

	private class BaseBlockWithLocation {
		final BaseBlock block;
		final BlockVector3 relative;

		public BaseBlockWithLocation(BaseBlock block, BlockVector3 relative) {
			this.block = block;
			this.relative = relative;
		}
	}
}