package com.swiftlicious.hellblock.api.compatibility;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
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
			HellblockPlugin.getInstance().getPluginLogger().severe(
					"WorldEdit threw an error during initializing, make sure it's updated and API compatible(FAWE isn't API compatible)",
					t);
			return false;
		}
	}

	@SuppressWarnings("removal")
	@Override
	public void pasteHellblock(File file, Location location, boolean ignoreAirBlock,
			CompletableFuture<Void> completableFuture) {
		try {
			ClipboardFormat format = cachedClipboardFormat.getOrDefault(file, ClipboardFormats.findByFile(file));
			Clipboard clipboard;
			try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
				clipboard = reader.read();
			}

			int width = clipboard.getDimensions().getBlockX();
			int height = clipboard.getDimensions().getBlockY();
			int length = clipboard.getDimensions().getBlockZ();

			int newLength = (int) (length / 2.00);
			int newWidth = (int) (width / 2.00);
			int newHeight = (int) (height / 2.00);

			location.subtract(newWidth, newHeight, newLength); // Center the schematic (for real this time)

			// Change the //copy point to the minimum corner
			clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());

			try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance()
					.newEditSession(new BukkitWorld(location.getWorld()))) {
				Operation operation = new ClipboardHolder(clipboard).createPaste(editSession)
						.to(BlockVector3.at(location.getX(), location.getY(), location.getZ())).copyEntities(true)
						.ignoreAirBlocks(ignoreAirBlock).build();
				Operations.complete(operation);
				Operations.complete(editSession.commit());
				cachedClipboardFormat.putIfAbsent(file, format);
				completableFuture.complete(null);
			}
		} catch (WorldEditException | IOException ex) {
			HellblockPlugin.getInstance().getPluginLogger()
					.severe("Unable to load hellblock island schematic with WorldEdit.", ex);
		}
	}

	@Override
	public void clearCache() {
		cachedClipboardFormat.clear();
	}
}