package com.swiftlicious.hellblock.database.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.google.common.io.ByteStreams;

/**
 * Represents a repository which contains {@link Dependency}s.
 */
public enum DependencyRepository {

	MAVEN_CENTRAL("maven", "https://repo1.maven.org/maven2/") {
		@Override
		protected URLConnection openConnection(Dependency dependency) throws IOException {
			URLConnection connection = super.openConnection(dependency);
			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
			return connection;
		}
	},

	/**
	 * Maven Central
	 */
	MAVEN_CENTRAL_MIRROR("aliyun", "https://maven.aliyun.com/repository/public/"),
	/**
	 * Code MC
	 */
	CODE_MC("codemc", "https://repo.codemc.io/repository/maven-public/"),
	/**
	 * xenondevs
	 */
	XENONDEVS("xenondevs", "https://repo.xenondevs.xyz/releases/"),
	/**
	 * Jitpack
	 */
	JITPACK("jitpack", "https://jitpack.io/");

	private final String url;
	private final String id;

	DependencyRepository(String id, String url) {
		this.url = url;
		this.id = id;
	}

	public static DependencyRepository getByID(String id) {
		for (DependencyRepository repository : values()) {
			if (id.equals(repository.id)) {
				return repository;
			}
		}
		return null;
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
				byte[] bytes = ByteStreams.toByteArray(in);
				if (bytes.length == 0) {
					throw new DependencyDownloadException("Empty stream");
				}
				return bytes;
			}
		} catch (Exception e) {
			throw new DependencyDownloadException(e);
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
		} catch (IOException e) {
			throw new DependencyDownloadException(e);
		}
	}

	public String getId() {
		return id;
	}
}
