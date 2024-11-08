package com.swiftlicious.hellblock.database.dependency;

import com.google.gson.JsonElement;

/**
 * Applies Hellblock specific behaviour for {@link Dependency}s.
 */
public class DependencyRegistry {

	public boolean shouldAutoLoad(Dependency dependency) {
		return switch (dependency) {
		// all used within 'isolated' classloaders, and are therefore not
		// relocated.
		case ASM, ASM_COMMONS, JAR_RELOCATOR, H2_DRIVER, SQLITE_DRIVER -> false;
		default -> true;
		};
	}

	public static boolean isGsonRelocated() {
		return JsonElement.class.getName().startsWith("com.swiftlicious");
	}
}