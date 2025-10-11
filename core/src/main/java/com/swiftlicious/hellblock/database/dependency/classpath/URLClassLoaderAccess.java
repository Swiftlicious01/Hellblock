package com.swiftlicious.hellblock.database.dependency.classpath;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.bukkit.Bukkit;
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
		} else if (GlobalUnsafe.isSupported()) {
			return new GlobalUnsafe(classLoader);
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

	private static void throwError(Throwable cause) {
		throw new UnsupportedOperationException("Unable to inject into the plugin URLClassLoader.\n"
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
			} catch (Exception ignored) {
				addUrlMethod = null;
			}
			ADD_URL_METHOD = addUrlMethod;
		}

		private static boolean isSupported() {
			return ADD_URL_METHOD != null;
		}

		protected Reflection(URLClassLoader classLoader) {
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
		private static final Object UNSAFE;

		static {
			Object unsafe;
			try {
				final Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
				unsafeField.setAccessible(true);
				unsafe = unsafeField.get(null);
			} catch (Throwable ignored) {
				unsafe = null;
			}
			UNSAFE = unsafe;
		}

		private static boolean isSupported() {
			return UNSAFE != null;
		}

		private final Collection<URL> unopenedURLs;
		private final Collection<URL> pathURLs;

		@SuppressWarnings("unchecked")
		protected Unsafe(URLClassLoader classLoader) {
			super(classLoader);

			Collection<URL> unopenedURLs;
			Collection<URL> pathURLs;
			try {
				final Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
				unopenedURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
				pathURLs = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
			} catch (Throwable e) {
				unopenedURLs = null;
				pathURLs = null;
			}

			this.unopenedURLs = unopenedURLs;
			this.pathURLs = pathURLs;
		}

		private static Object fetchField(final Class<?> clazz, final Object object, final String name)
				throws NoSuchFieldException {
			final Field field = clazz.getDeclaredField(name);
			try {
				final long ucpOffset = (Long) UNSAFE.getClass().getDeclaredMethod("objectFieldOffset", Field.class)
						.invoke(UNSAFE, field);
				return UNSAFE.getClass().getDeclaredMethod("getObject", Object.class, Long.TYPE).invoke(UNSAFE, object,
						ucpOffset);
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
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

	private static class GlobalUnsafe extends URLClassLoaderAccess {

		private static final MethodHandles.Lookup LOOKUP;
		private static final Object UNSAFE;

		static {
			MethodHandles.Lookup lookup;
			Object unsafe;
			try {
				final Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
				unsafeField.setAccessible(true);
				unsafe = unsafeField.get(null);
				final Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
				final Object lookupBase = unsafe.getClass().getDeclaredMethod("staticFieldBase", Field.class)
						.invoke(unsafe, lookupField);
				final long lookupOffset = (Long) unsafe.getClass().getDeclaredMethod("staticFieldOffset", Field.class)
						.invoke(unsafe, lookupField);
				lookup = (MethodHandles.Lookup) unsafe.getClass()
						.getDeclaredMethod("getObject", Object.class, Long.TYPE)
						.invoke(unsafe, lookupBase, lookupOffset);
			} catch (Throwable ignored) {
				lookup = null;
				unsafe = null;
			}
			LOOKUP = lookup;
			UNSAFE = unsafe;
		}

		protected GlobalUnsafe(URLClassLoader classLoader) {
			super(classLoader);
		}

		private static boolean isSupported() {
			return LOOKUP != null;
		}

		@Override
		public void addURL(URL url) {
			try {
				final ClassLoader loader = Bukkit.class.getClassLoader();
				if ("LaunchClassLoader".equals(loader.getClass().getSimpleName())) {
					final MethodHandle methodHandle = LOOKUP.findVirtual(loader.getClass(), "addURL",
							MethodType.methodType(Void.TYPE, URL.class));
					methodHandle.invoke(loader, url.toURI().toURL());
				} else {
					Field ucpField;
					try {
						ucpField = loader.getClass().getDeclaredField("ucp");
					} catch (NoSuchFieldException | NoSuchFieldError ex) {
						ucpField = loader.getClass().getSuperclass().getDeclaredField("ucp");
					}

					final long ucpOffset = (Long) UNSAFE.getClass().getDeclaredMethod("objectFieldOffset", Field.class)
							.invoke(UNSAFE, ucpField);
					final Object ucp = UNSAFE.getClass().getDeclaredMethod("getObject", Object.class, Long.TYPE)
							.invoke(UNSAFE, loader, ucpOffset);
					final MethodHandle methodHandle = LOOKUP.findVirtual(ucp.getClass(), "addURL",
							MethodType.methodType(Void.TYPE, URL.class));
					methodHandle.invoke(ucp, url.toURI().toURL());
				}
			} catch (Throwable t) {
				throw new RuntimeException(t);
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