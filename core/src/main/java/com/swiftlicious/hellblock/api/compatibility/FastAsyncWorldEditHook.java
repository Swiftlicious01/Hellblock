package com.swiftlicious.hellblock.api.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

import com.sk89q.worldedit.EditSession;
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
import com.sk89q.worldedit.session.ClipboardHolder;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.schematic.SchematicPaster;

public class FastAsyncWorldEditHook implements SchematicPaster {

	private static final Map<File, ClipboardFormat> cachedClipboardFormat = new HashMap<>();
	private static Object mutex = new Object();

	public static boolean isWorking() {
		try {
			final Platform platform = com.sk89q.worldedit.WorldEdit.getInstance().getPlatformManager()
					.queryCapability(Capability.WORLD_EDITING);
			int liveDataVersion = platform.getDataVersion();
			return liveDataVersion != -1;
		} catch (Throwable t) {
			HellblockPlugin.getInstance().getPluginLogger().warn(
					"WorldEdit threw an error during initializing, make sure it's updated and API compatible(FAWE isn't API compatible) ::",
					t);
		}
		return false;
	}

	@Override
	public boolean pasteHellblock(File file, Location location, CompletableFuture<Void> completableFuture) {
		try {
			ClipboardFormat format = cachedClipboardFormat.getOrDefault(file, ClipboardFormats.findByFile(file));
			Clipboard clipboard;
			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				clipboard = reader.read();
			}

			clipboard.setOrigin(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()));

			{
				Thread t = new Thread() {
					public void run() {
						synchronized (mutex) {
							EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance()
									.newEditSession(BukkitAdapter.adapt(
											HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld()));
							Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
									.to(BlockVector3.at(location.getBlockX(), location.getBlockY(),
											location.getBlockZ()))
									.copyBiomes(false).copyEntities(true).ignoreAirBlocks(true).build();
							try {
								Operations.complete(operation);
								Operations.complete(editSession.commit());
							} catch (WorldEditException e) {
								e.printStackTrace();
							}
							cachedClipboardFormat.putIfAbsent(file, format);
							HellblockPlugin.getInstance().getScheduler().executeSync(() -> {
								completableFuture.complete(null);
							}, location);
						}
					};
				};
				t.start();
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void clearCache() {
		cachedClipboardFormat.clear();
	}
}
