package com.swiftlicious.hellblock.database.dependency;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.classloader.IsolatedClassLoader;
import com.swiftlicious.hellblock.database.dependency.classpath.ClassPathAppender;
import com.swiftlicious.hellblock.database.dependency.relocation.Relocation;
import com.swiftlicious.hellblock.database.dependency.relocation.RelocationHandler;
import com.swiftlicious.hellblock.utils.FileUtils;
import com.swiftlicious.hellblock.utils.LogUtils;

/**
 * Loads and manages runtime dependencies for the plugin.
 */
public class DependencyManager implements DependencyManagerInterface {

	/** A registry containing plugin specific behaviour for dependencies. */
	private final DependencyRegistry registry;
	/** The path where library jars are cached. */
	private final Path cacheDirectory;
	/** The classpath appender to preload dependencies into */
	private final ClassPathAppender classPathAppender;
	/** A map of dependencies which have already been loaded. */
	private final EnumMap<Dependency, Path> loaded = new EnumMap<>(Dependency.class);
	/** A map of isolated classloaders which have been created. */
	private final Map<Set<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
	/** Cached relocation handler instance. */
	private final RelocationHandler relocationHandler;
	/** The executor to use when loading dependencies */
	private final Executor loadingExecutor;

	public DependencyManager(HellblockPlugin plugin) {
		this.registry = new DependencyRegistry();
		this.cacheDirectory = setupCacheDirectory(plugin);
		this.classPathAppender = plugin.getClassPathAppender();
		this.loadingExecutor = plugin.getScheduler().async();
		this.relocationHandler = new RelocationHandler(this);
	}

	@Override
	public ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
		Set<Dependency> set = new HashSet<>(dependencies);

		for (Dependency dependency : dependencies) {
			if (!this.loaded.containsKey(dependency)) {
				throw new IllegalStateException(String.format("Dependency %s is not loaded.", dependency));
			}
		}

		synchronized (this.loaders) {
			IsolatedClassLoader classLoader = this.loaders.get(set);
			if (classLoader != null) {
				return classLoader;
			}

			URL[] urls = set.stream().map(this.loaded::get).map(file -> {
				try {
					return file.toUri().toURL();
				} catch (MalformedURLException ex) {
					throw new RuntimeException(ex);
				}
			}).toArray(URL[]::new);

			classLoader = new IsolatedClassLoader(urls);
			this.loaders.put(set, classLoader);
			return classLoader;
		}
	}

	@Override
	public void loadDependencies(Collection<Dependency> dependencies) {
		CountDownLatch latch = new CountDownLatch(dependencies.size());

		for (Dependency dependency : dependencies) {
			if (this.loaded.containsKey(dependency)) {
				latch.countDown();
				continue;
			}

			this.loadingExecutor.execute(() -> {
				try {
					loadDependency(dependency);
				} catch (Throwable ex) {
					LogUtils.warn(String.format("Unable to load dependency %s", dependency.name()), ex);
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

		Path file = remapDependency(dependency, downloadDependency(dependency));

		this.loaded.put(dependency, file);

		if (this.classPathAppender != null && this.registry.shouldAutoLoad(dependency)) {
			this.classPathAppender.addJarToClasspath(file);
		}
	}

	private Path downloadDependency(Dependency dependency) throws DependencyDownloadException {
		String fileName = dependency.getFileName(null);
		Path file = this.cacheDirectory.resolve(fileName);

		// if the file already exists, don't attempt to re-download it.
		if (Files.exists(file)) {
			return file;
		}

		DependencyDownloadException lastError = null;
		String forceRepo = dependency.getRepo();
		List<DependencyRepository> repository = DependencyRepository.getByID(forceRepo);
		if (!repository.isEmpty()) {
			int i = 0;
			while (i < repository.size()) {
				try {
					LogUtils.info(String.format("Downloading dependency(%s) [%s	| %s]", fileName,
							repository.get(i).getUrl(), dependency.getMavenRepoPath()));
					repository.get(i).download(dependency, file);
					LogUtils.info(String.format("Successfully downloaded %s", fileName));
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
		List<Relocation> rules = new ArrayList<>(dependency.getRelocations());
		if (rules.isEmpty()) {
			return normalFile;
		}

		Path remappedFile = this.cacheDirectory
				.resolve(dependency.getFileName(DependencyRegistry.isGsonRelocated() ? "remapped-legacy" : "remapped"));

		// if the remapped source exists already, just use that.
		if (Files.exists(remappedFile)) {
			return remappedFile;
		}

		LogUtils.info("Remapping " + dependency.getFileName(null));
		relocationHandler.remap(normalFile, remappedFile, rules);
		LogUtils.info("Successfully remapped " + dependency.getFileName(null));
		return remappedFile;
	}

	private static Path setupCacheDirectory(HellblockPlugin plugin) {
		Path cacheDirectory = plugin.getDataFolder().toPath().toAbsolutePath().resolve("libs");
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
			LogUtils.severe(firstEx.getMessage(), firstEx);
		}
	}
}