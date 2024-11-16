package com.swiftlicious.hellblock.utils;

import java.util.List;

public interface VersionManagerInterface {

	boolean isSpigot();
	
	boolean isPaper();
	
	boolean isFolia();

	String getPluginVersion();

	String getServerVersion();

	List<String> getSupportedVersions();
}