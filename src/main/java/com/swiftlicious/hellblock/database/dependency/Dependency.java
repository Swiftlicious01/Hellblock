package com.swiftlicious.hellblock.database.dependency;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

/**
 * The database dependencies used by Hellblock.
 */
public enum Dependency {

	ASM("org.ow2.asm", "asm", "9.1", null, "asm"),
	ASM_COMMONS("org.ow2.asm", "asm-commons", "9.1", null, "asm-commons"),
	JAR_RELOCATOR("me.lucko", "jar-relocator", "1.7", null, "jar-relocator"),
	MARIADB_DRIVER("org{}mariadb{}jdbc", "mariadb-java-client", "3.3.2", null, "mariadb-java-client",
			Relocation.of("mariadb", "org{}mariadb")),
	MYSQL_DRIVER("com{}mysql", "mysql-connector-j", "8.3.0", null, "mysql-connector-j",
			Relocation.of("mysql", "com{}mysql")),
	H2_DRIVER("com.h2database", "h2", "2.3.232", null, "h2database"),
	SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "3.46.1.3", null, "sqlite-jdbc"),
	HIKARI("com{}zaxxer", "HikariCP", "6.0.0", null, "HikariCP", Relocation.of("hikari", "com{}zaxxer{}hikari")),
	SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "2.0.12", null, "slf4j-simple"),
	SLF4J_API("org.slf4j", "slf4j-api", "2.0.12", null, "slf4j-api"),
	MONGODB_DRIVER_CORE("org{}mongodb", "mongodb-driver-core", "5.2.0", null, "mongodb-driver-core",
			Relocation.of("mongodb", "com{}mongodb"), Relocation.of("bson", "org{}bson")),
	MONGODB_DRIVER_SYNC("org{}mongodb", "mongodb-driver-sync", "5.2.0", null, "mongodb-driver-sync",
			Relocation.of("mongodb", "com{}mongodb"), Relocation.of("bson", "org{}bson")),
	MONGODB_DRIVER_BSON("org{}mongodb", "bson", "5.2.0", null, "mongodb-bson", Relocation.of("mongodb", "com{}mongodb"),
			Relocation.of("bson", "org{}bson")),
	JEDIS("redis{}clients", "jedis", "5.1.5", null, "jedis", Relocation.of("jedis", "redis{}clients{}jedis"),
			Relocation.of("commonspool2", "org{}apache{}commons{}pool2")),
	BSTATS_BASE("org{}bstats", "bstats-base", "3.1.0", null, "bstats-base", Relocation.of("bstats", "org{}bstats")),
	BSTATS_BUKKIT("org{}bstats", "bstats-bukkit", "3.1.0", null, "bstats-bukkit",
			Relocation.of("bstats", "org{}bstats")),
	GSON("com.google.code.gson", "gson", "2.10.1", null, "gson");

	private final String mavenRepoPath;
	private final String version;
	private final List<Relocation> relocations;
	private final String repo;
	private final String artifact;

	private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

	Dependency(String groupId, String artifactId, String version, String repo, String artifact) {
		this(groupId, artifactId, version, repo, artifact, new Relocation[0]);
	}

	Dependency(String groupId, String artifactId, String version, String repo, String artifact,
			Relocation... relocations) {
		this.mavenRepoPath = String.format(MAVEN_FORMAT, rewriteEscaping(groupId).replace(".", "/"),
				rewriteEscaping(artifactId), version, rewriteEscaping(artifactId), version);
		this.version = version;
		this.relocations = ImmutableList.copyOf(relocations);
		this.repo = repo;
		this.artifact = artifact;
	}

	private static String rewriteEscaping(String s) {
		return s.replace("{}", ".");
	}

	public String getFileName(String classifier) {
		String name = artifact.toLowerCase(Locale.ROOT).replace('_', '-');
		String extra = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;

		return name + "-" + this.version + extra + ".jar";
	}

	String getMavenRepoPath() {
		return this.mavenRepoPath;
	}

	public List<Relocation> getRelocations() {
		return this.relocations;
	}

	/**
	 * Creates a {@link MessageDigest} suitable for computing the checksums of
	 * dependencies.
	 *
	 * @return the digest
	 */
	public static MessageDigest createDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	public String getRepo() {
		return repo;
	}
}