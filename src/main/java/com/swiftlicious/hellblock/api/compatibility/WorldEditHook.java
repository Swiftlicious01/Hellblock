package com.swiftlicious.hellblock.api.compatibility;

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
import com.sk89q.worldedit.session.ClipboardHolder;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.schematic.SchematicPaster;
import com.swiftlicious.hellblock.utils.LogUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

public class WorldEditHook implements SchematicPaster {

	private static final Map<File, ClipboardFormat> cachedClipboardFormat = new HashMap<>();

	public static boolean isWorking() {
		try {
			final Platform platform = com.sk89q.worldedit.WorldEdit.getInstance().getPlatformManager()
					.queryCapability(Capability.WORLD_EDITING);
			int liveDataVersion = platform.getDataVersion();
			return liveDataVersion != -1;
		} catch (Throwable t) {
			LogUtils.severe(
					"WorldEdit threw an error during initializing, make sure it's updated and API compatible(FAWE isn't API compatible)",
					t);
			return false;
		}
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

			try (EditSession editSession = WorldEdit.getInstance().newEditSession(
					BukkitAdapter.adapt(HellblockPlugin.getInstance().getHellblockHandler().getHellblockWorld()))) {
				Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
						.to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
						.copyEntities(true).copyBiomes(false).ignoreAirBlocks(true).ignoreStructureVoidBlocks(true)
						.build();
				Operations.complete(operation);
				Operations.complete(editSession.commit());
				cachedClipboardFormat.putIfAbsent(file, format);
				completableFuture.complete(null);
				return true;
			}
		} catch (WorldEditException | IOException ex) {
			LogUtils.severe("Unable to load hellblock island schematic with WorldEdit.", ex);
			return false;
		}
	}

	@Override
	public void clearCache() {
		cachedClipboardFormat.clear();
	}
}