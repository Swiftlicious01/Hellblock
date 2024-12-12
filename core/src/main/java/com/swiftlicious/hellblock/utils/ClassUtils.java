package com.swiftlicious.hellblock.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for handling classes.
 */
public class ClassUtils {

	private ClassUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	@Nullable
	public static <T, C> Class<? extends T> findClass(@NotNull File file, @NotNull Class<T> clazz,
			@NotNull Class<C> type) throws IOException, ClassNotFoundException {
		if (!file.exists()) {
			return null;
		}

		URL jarUrl = file.toURI().toURL();
		List<Class<? extends T>> classes = new ArrayList<>();

		try (URLClassLoader loader = new URLClassLoader(new URL[] { jarUrl }, clazz.getClassLoader());
				JarInputStream jarStream = new JarInputStream(jarUrl.openStream())) {

			JarEntry entry;
			while ((entry = jarStream.getNextJarEntry()) != null) {
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					continue;
				}

				String className = name.substring(0, name.lastIndexOf('.')).replace('/', '.');

				try {
					Class<?> loadedClass = loader.loadClass(className);
					if (clazz.isAssignableFrom(loadedClass)) {
						Type superclassType = loadedClass.getGenericSuperclass();
						if (superclassType instanceof ParameterizedType parameterizedType) {
							Type[] typeArguments = parameterizedType.getActualTypeArguments();
							if (typeArguments.length > 0 && typeArguments[0].equals(type)) {
								classes.add(loadedClass.asSubclass(clazz));
							}
						}
					}
				} catch (ClassNotFoundException | NoClassDefFoundError ignored) {
				}
			}
		}

		if (classes.isEmpty()) {
			return null;
		}

		return classes.get(0);
	}
}