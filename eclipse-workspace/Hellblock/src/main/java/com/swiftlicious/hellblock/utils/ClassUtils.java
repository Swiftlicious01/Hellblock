package com.swiftlicious.hellblock.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassUtils {

	private ClassUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Attempts to find a class within a JAR file that extends or implements a given
	 * class or interface.
	 *
	 * @param file  The JAR file in which to search for the class.
	 * @param clazz The base class or interface to match against.
	 * @param <T>   The type of the base class or interface.
	 * @return A Class object representing the found class, or null if not found.
	 * @throws IOException            If there is an issue reading the JAR file.
	 * @throws ClassNotFoundException If the specified class cannot be found.
	 */
	
	@Nullable
	public static <T> Class<? extends T> findClass(@NotNull File file, @NotNull Class<T> clazz)
			throws IOException, ClassNotFoundException {
		if (!file.exists()) {
			return null;
		}

		URL jar = file.toURI().toURL();
		URLClassLoader loader = new URLClassLoader(new URL[] { jar }, clazz.getClassLoader());
		List<String> matches = new ArrayList<>();
		List<Class<? extends T>> classes = new ArrayList<>();

		try (JarInputStream stream = new JarInputStream(jar.openStream())) {
			JarEntry entry;
			while ((entry = stream.getNextJarEntry()) != null) {
				final String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}
				matches.add(name.substring(0, name.lastIndexOf('.')).replace('/', '.'));
			}

			for (String match : matches) {
				try {
					Class<?> loaded = loader.loadClass(match);
					if (clazz.isAssignableFrom(loaded)) {
						classes.add(loaded.asSubclass(clazz));
					}
				} catch (NoClassDefFoundError ignored) {
					// ignored
				}
			}
		}
		if (classes.isEmpty()) {
			loader.close();
			return null;
		}
		
		loader.close();
		return classes.get(0);
	}
}