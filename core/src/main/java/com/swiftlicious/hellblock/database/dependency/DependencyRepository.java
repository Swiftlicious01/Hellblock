package com.swiftlicious.hellblock.database.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Represents a repository which contains {@link Dependency}s.
 */
public enum DependencyRepository {

	/**
	 * Maven Mirror
	 */
	MAVEN_MIRROR("maven", "https://maven.aliyun.com/repository/public/") {
		@Override
		protected URLConnection openConnection(Dependency dependency) throws IOException {
			URLConnection connection = super.openConnection(dependency);
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));
			return connection;
		}
	},
	/**
	 * Maven Central
	 */
	MAVEN_CENTRAL("maven", "https://repo1.maven.org/maven2/"),
	/**
	 * xenondevs
	 */
	XENONDEVS("xenondevs", "https://repo.xenondevs.xyz/releases/");

	private final String url;
	private final String id;

	DependencyRepository(String id, String url) {
		this.url = url;
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public static List<DependencyRepository> getByID(String id) {
		List<DependencyRepository> repositories = new ArrayList<>();
		for (DependencyRepository repository : values()) {
			if (id.equals(repository.id)) {
				repositories.add(repository);
			}
		}
		return repositories;
	}

	/**
	 * Opens a connection to the given {@code dependency}.
	 *
	 * @param dependency the dependency to download
	 * @return the connection
	 * @throws IOException if unable to open a connection
	 */
	protected URLConnection openConnection(Dependency dependency) throws IOException {
		URL dependencyUrl = URI.create(this.url + dependency.getMavenRepoPath()).toURL();
		return dependencyUrl.openConnection();
	}

	/**
	 * Downloads the raw bytes of the {@code dependency}.
	 *
	 * @param dependency the dependency to download
	 * @return the downloaded bytes
	 * @throws DependencyDownloadException if unable to download
	 */
	public byte[] downloadRaw(Dependency dependency) throws DependencyDownloadException {
		try {
			URLConnection connection = openConnection(dependency);
			try (InputStream in = connection.getInputStream()) {
				byte[] bytes = in.readAllBytes();
				if (bytes.length == 0) {
					throw new DependencyDownloadException("Empty stream");
				}
				return bytes;
			}
		} catch (Exception ex) {
			throw new DependencyDownloadException(ex);
		}
	}

	/**
	 * @param dependency the dependency to download
	 * @return the downloaded bytes
	 * @throws DependencyDownloadException if unable to download
	 */
	public byte[] download(Dependency dependency) throws DependencyDownloadException {
		return downloadRaw(dependency);
	}

	/**
	 * Downloads the the {@code dependency} to the {@code file}, ensuring the
	 * downloaded bytes match the checksum.
	 *
	 * @param dependency the dependency to download
	 * @param file       the file to write to
	 * @throws DependencyDownloadException if unable to download
	 */
	public void download(Dependency dependency, Path file) throws DependencyDownloadException {
		try {
			Files.write(file, download(dependency));
		} catch (IOException ex) {
			throw new DependencyDownloadException(ex);
		}
	}

	public String getId() {
		return id;
	}
}