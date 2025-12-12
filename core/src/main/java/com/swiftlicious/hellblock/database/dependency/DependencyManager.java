package com.swiftlicious.hellblock.database.dependency;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import com.google.common.collect.ImmutableSet;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.classloader.IsolatedClassLoader;
import com.swiftlicious.hellblock.database.dependency.classpath.ClassPathAppender;
import com.swiftlicious.hellblock.database.dependency.relocation.Relocation;
import com.swiftlicious.hellblock.database.dependency.relocation.RelocationHandler;
import com.swiftlicious.hellblock.utils.FileUtils;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public class DependencyManager implements DependencyManagerInterface {

	protected final HellblockPlugin instance;
	/** A registry containing plugin specific behaviour for dependencies. */
	private final DependencyRegistry registry;
	/** The path where library jars are cached. */
	private final Path cacheDirectory;
	/** The classpath appender to preload dependencies into */
	private final ClassPathAppender classPathAppender;
	/** The executor to use when loading dependencies */
	private final Executor loadingExecutor;
	/** A map of dependencies which have already been loaded. */
	private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);
	/** A map of isolated classloaders which have been created. */
	private final Map<ImmutableSet<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
	/** Cached relocation handler instance. */
	private @MonotonicNonNull RelocationHandler relocationHandler = null;

	public DependencyManager(HellblockPlugin plugin, ClassPathAppender classPathAppender) {
		instance = plugin;
		this.registry = new DependencyRegistry();
		this.cacheDirectory = setupCacheDirectory(plugin);
		this.classPathAppender = classPathAppender;
		this.loadingExecutor = plugin.getScheduler().async();
	}

	private synchronized RelocationHandler getRelocationHandler() {
		if (this.relocationHandler == null) {
			this.relocationHandler = new RelocationHandler(this);
		}
		return this.relocationHandler;
	}

	@Override
	public ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
		final ImmutableSet<Dependency> set = ImmutableSet.copyOf(dependencies);

		dependencies.stream().filter(dependency -> !this.loaded.containsKey(dependency)).forEach(dependency -> {
			throw new IllegalStateException("Dependency %s is not loaded.".formatted(dependency));
		});

		synchronized (this.loaders) {
			IsolatedClassLoader classLoader = this.loaders.get(set);
			if (classLoader != null) {
				return classLoader;
			}

			final List<URL> urls = new ArrayList<>();
			try {
				// Add plugin jar first so LoaderBridge is visible
				URL pluginUrl = instance.getClass().getProtectionDomain().getCodeSource().getLocation();
				urls.add(pluginUrl);
			} catch (Exception e) {
				instance.getPluginLogger().warn("Could not determine plugin JAR location", e);
			}

			for (Dependency dep : set) {
				try {
					urls.add(this.loaded.get(dep).toUri().toURL());
				} catch (MalformedURLException ex) {
					throw new RuntimeException(ex);
				}
			}

			classLoader = new IsolatedClassLoader(urls.toArray(new URL[0]));
			this.loaders.put(set, classLoader);
			return classLoader;
		}
	}

	@Override
	public void loadDependencies(Set<Dependency> dependencies) {
		final CountDownLatch latch = new CountDownLatch(dependencies.size());

		for (Dependency dependency : dependencies) {
			if (this.loaded.containsKey(dependency)) {
				latch.countDown();
				continue;
			}

			this.loadingExecutor.execute(() -> {
				try {
					loadDependency(dependency);
				} catch (Throwable ex) {
					instance.getPluginLogger().warn("Unable to load dependency %s".formatted(dependency.name()), ex);
				} finally {
					latch.countDown();
				}
			});
		}

		try {
			latch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void loadDependency(Dependency dependency) throws Exception {
		if (this.loaded.containsKey(dependency)) {
			return;
		}

		final Path file = remapDependency(dependency, downloadDependency(dependency));

		this.loaded.put(dependency, file);

		if (this.classPathAppender != null && this.registry.shouldAutoLoad(dependency)) {
			this.classPathAppender.addJarToClasspath(file);
		}
	}

	private Path downloadDependency(Dependency dependency) throws DependencyDownloadException {
		final String fileName = dependency.getFileName(null);
		final Path file = this.cacheDirectory.resolve(fileName);

		// if the file already exists, don't attempt to re-download it.
		if (Files.exists(file)) {
			return file;
		}

		DependencyDownloadException lastError = null;
		final String forceRepo = dependency.getRepo();
		final List<DependencyRepository> repository = DependencyRepository.getByID(forceRepo);
		if (!repository.isEmpty()) {
			int i = 0;
			while (i < repository.size()) {
				try {
					instance.getPluginLogger().info("Downloading dependency(%s) [%s%s]".formatted(fileName,
							repository.get(i).getUrl(), dependency.getMavenRepoPath()));
					repository.get(i).download(dependency, file);
					instance.getPluginLogger().info("Successfully downloaded %s".formatted(fileName));
					return file;
				} catch (DependencyDownloadException ex) {
					lastError = ex;
					i++;
				}
			}
		}
		throw Objects.requireNonNull(lastError);
	}

	private Path remapDependency(Dependency dependency, Path normalFile) throws Exception {
		final List<Relocation> rules = new ArrayList<>(dependency.getRelocations());
		if (rules.isEmpty()) {
			return normalFile;
		}

		final Path remappedFile = this.cacheDirectory
				.resolve(dependency.getFileName(DependencyRegistry.isGsonRelocated() ? "remapped-legacy" : "remapped"));

		// if the remapped source exists already, just use that.
		if (Files.exists(remappedFile)) {
			return remappedFile;
		}

		instance.getPluginLogger().info("Remapping " + dependency.getFileName(null));
		getRelocationHandler().remap(normalFile, remappedFile, rules);
		instance.getPluginLogger().info("Successfully remapped " + dependency.getFileName(null));
		return remappedFile;
	}

	private static Path setupCacheDirectory(HellblockPlugin plugin) {
		final Path cacheDirectory = plugin.getDataFolder().toPath().toAbsolutePath().resolve("libs");
		try {
			FileUtils.createDirectoriesIfNotExists(cacheDirectory);
		} catch (IOException ex) {
			throw new RuntimeException("Unable to create libs directory", ex);
		}

		return cacheDirectory;
	}

	@Override
	public void close() {
		IOException firstEx = null;

		for (IsolatedClassLoader loader : this.loaders.values()) {
			try {
				loader.close();
			} catch (IOException ex) {
				if (firstEx == null) {
					firstEx = ex;
				} else {
					firstEx.addSuppressed(ex);
				}
			}
		}

		if (firstEx != null) {
			instance.getPluginLogger().severe(firstEx.getMessage(), firstEx);
		}
	}

	@Override
	public <T> T runWithLoader(Set<Dependency> deps, Supplier<T> supplier) {
		ClassLoader loader = obtainClassLoaderWith(deps);
		ClassLoader prev = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(loader);
			return supplier.get();
		} finally {
			Thread.currentThread().setContextClassLoader(prev);
		}
	}
}