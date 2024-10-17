package com.swiftlicious.hellblock.utils;

import java.util.concurrent.CompletableFuture;

public interface VersionManagerInterface {

	CompletableFuture<Boolean> checkUpdate();

	boolean isSpigot();
	
	boolean isPaper();
	
	boolean isFolia();

	String getPluginVersion();

	String getServerVersion();
}