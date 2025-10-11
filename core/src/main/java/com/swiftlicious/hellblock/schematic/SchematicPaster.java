package com.swiftlicious.hellblock.schematic;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;

public interface SchematicPaster {

	CompletableFuture<Location> pasteHellblock(UUID playerId, File file, Location location, boolean ignoreAirBlock,
			SchematicMetadata metadata, boolean animated);

	boolean cancelPaste(UUID playerId);

	int getPasteProgress(UUID playerId);

	void clearCache();
}
