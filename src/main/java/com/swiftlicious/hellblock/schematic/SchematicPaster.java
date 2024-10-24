package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

public interface SchematicPaster {

	boolean pasteHellIsland(File file, Location location, Boolean ignoreAirBlock,
			CompletableFuture<Void> completableFuture);

	void clearCache();
}
