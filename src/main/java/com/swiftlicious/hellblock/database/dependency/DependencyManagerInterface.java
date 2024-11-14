package com.swiftlicious.hellblock.database.dependency;

import java.util.Set;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public interface DependencyManagerInterface extends AutoCloseable {

	/**
	 * Loads dependencies.
	 *
	 * @param dependencies the dependencies to load
	 */
	void loadDependencies(Set<Dependency> dependencies);

	/**
	 * Obtains an isolated classloader containing the given dependencies.
	 *
	 * @param dependencies the dependencies
	 * @return the classloader
	 */
	ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies);

	@Override
	void close();
}