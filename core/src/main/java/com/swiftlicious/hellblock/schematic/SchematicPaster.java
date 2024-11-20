package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

public interface SchematicPaster {

	boolean pasteHellblock(File file, Location location, CompletableFuture<Void> completableFuture);

	void clearCache();
}
