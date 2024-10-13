package com.swiftlicious.hellblock.utils;

import java.util.concurrent.CompletableFuture;

public interface VersionManagerInterface {

	CompletableFuture<Boolean> checkUpdate();

	boolean isSpigot();
	
	boolean isPaper();

	String getPluginVersion();

	String getServerVersion();
}