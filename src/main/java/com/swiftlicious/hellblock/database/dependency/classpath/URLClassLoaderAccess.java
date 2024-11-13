package com.swiftlicious.hellblock.database.dependency.classpath;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Provides access to {@link URLClassLoader}#addURL.
 */
public abstract class URLClassLoaderAccess {

	/**
	 * Creates a {@link URLClassLoaderAccess} for the given class loader.
	 *
	 * @param classLoader the class loader
	 * @return the access object
	 */
	public static URLClassLoaderAccess create(URLClassLoader classLoader) {
		if (Reflection.isSupported()) {
			return new Reflection(classLoader);
		} else if (Unsafe.isSupported()) {
			return new Unsafe(classLoader);
		} else {
			return Noop.INSTANCE;
		}
	}

	private final URLClassLoader classLoader;

	protected URLClassLoaderAccess(URLClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Adds the given URL to the class loader.
	 *
	 * @param url the URL to add
	 */
	public abstract void addURL(@NotNull URL url);

	private static void throwError(Throwable cause) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Hellblock is unable to inject into the plugin URLClassLoader.\n"
				+ "You may be able to fix this problem by adding the following command-line argument "
				+ "directly after the 'java' command in your start script: \n'--add-opens java.base/java.lang=ALL-UNNAMED'",
				cause);
	}

	/**
	 * Accesses using reflection, not supported on Java 9+.
	 */
	private static class Reflection extends URLClassLoaderAccess {
		private static final Method ADD_URL_METHOD;

		static {
			Method addUrlMethod;
			try {
				addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				addUrlMethod.setAccessible(true);
			} catch (Exception e) {
				addUrlMethod = null;
			}
			ADD_URL_METHOD = addUrlMethod;
		}

		private static boolean isSupported() {
			return ADD_URL_METHOD != null;
		}

		Reflection(URLClassLoader classLoader) {
			super(classLoader);
		}

		@Override
		public void addURL(@NotNull URL url) {
			try {
				ADD_URL_METHOD.invoke(super.classLoader, url);
			} catch (ReflectiveOperationException e) {
				URLClassLoaderAccess.throwError(e);
			}
		}
	}

	/**
	 * Accesses using sun.misc.Unsafe, supported on Java 9+.
	 *
	 * @author Vaishnav Anil (https://github.com/slimjar/slimjar)
	 */
	private static class Unsafe extends URLClassLoaderAccess {
		private static final sun.misc.Unsafe UNSAFE;

		static {
			sun.misc.Unsafe unsafe;
			try {
				Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				unsafeField.setAccessible(true);
				unsafe = (sun.misc.Unsafe) unsafeField.get(null);
			} catch (Throwable t) {
				unsafe = null;
			}
			UNSAFE = unsafe;
		}

		private static boolean isSupported() {
			return UNSAFE != null;
		}

		private final Deque<URL> unopenedURLs;
		private final List<URL> pathURLs;

		@SuppressWarnings("unchecked")
		Unsafe(URLClassLoader classLoader) {
			super(classLoader);

			Deque<URL> unopenedURLs;
			List<URL> pathURLs;
			try {
				final Object ucp = fetchField(UNSAFE, URLClassLoader.class, classLoader, "ucp");
				unopenedURLs = (ArrayDeque<URL>) fetchField(UNSAFE, ucp, "unopenedUrls");
				pathURLs = (ArrayList<URL>) fetchField(UNSAFE, ucp, "path");
			} catch (Throwable e) {
				unopenedURLs = null;
				pathURLs = null;
			}

			this.unopenedURLs = unopenedURLs;
			this.pathURLs = pathURLs;
		}

		private static Object fetchField(final sun.misc.Unsafe unsafe, final Object object, final String name)
				throws NoSuchFieldException {
			return fetchField(unsafe, object.getClass(), object, name);
		}

		@SuppressWarnings("deprecation")
		private static Object fetchField(final sun.misc.Unsafe unsafe, final Class<?> clazz, final Object object,
				final String name) throws NoSuchFieldException {
			final Field field = clazz.getDeclaredField(name);
			final long offset = unsafe.objectFieldOffset(field);
			return unsafe.getObject(object, offset);
		}

		@Override
		public void addURL(@NotNull URL url) {
			if (this.unopenedURLs == null || this.pathURLs == null) {
				URLClassLoaderAccess.throwError(new NullPointerException("unopenedURLs or pathURLs"));
			}

			synchronized (this.unopenedURLs) {
				this.unopenedURLs.add(url);
				this.pathURLs.add(url);
			}
		}
	}

	private static class Noop extends URLClassLoaderAccess {
		private static final Noop INSTANCE = new Noop();

		private Noop() {
			super(null);
		}

		@Override
		public void addURL(@NotNull URL url) {
			URLClassLoaderAccess.throwError(null);
		}
	}
}