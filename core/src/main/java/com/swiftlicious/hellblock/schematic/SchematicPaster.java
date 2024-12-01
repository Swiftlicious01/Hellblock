package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

public interface SchematicPaster {

	void pasteHellblock(File file, Location location, boolean ignoreAirBlock,
			CompletableFuture<Void> completableFuture);

	void clearCache();
}
