package com.swiftlicious.hellblock.database.dependency;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.swiftlicious.hellblock.database.dependency.relocation.Relocation;

import lombok.NonNull;

/**
 * The dependencies used by Hellblock.
 */
public enum Dependency {

	// no relocations needed as this is only used to relocate jars
	ASM("org.ow2.asm", "asm", "maven", "asm"), ASM_COMMONS("org.ow2.asm", "asm-commons", "maven", "asm-commons"),
	JAR_RELOCATOR("me.lucko", "jar-relocator", "maven", "jar-relocator"),
	// no relocations for sqlite or h2
	H2_DRIVER("com.h2database", "h2", "maven", "h2-driver"),
	SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "maven", "sqlite-driver"),
	BOOSTED_YAML("dev{}dejvokep", "boosted-yaml", "maven", "boosted-yaml",
			Relocation.of("boostedyaml", "dev{}dejvokep{}boostedyaml")),
	MARIADB_DRIVER("org{}mariadb{}jdbc", "mariadb-java-client", "maven", "mariadb-java-client",
			Relocation.of("mariadb", "org{}mariadb")),
	POSTGRESQL_DRIVER("org{}postgresql", "postgresql", "maven", "postgresql",
			Relocation.of("postgresql", "org{}postgresql")),
	MYSQL_DRIVER("com{}mysql", "mysql-connector-j", "maven", "mysql-connector-j", Relocation.of("mysql", "com{}mysql")),
	HIKARI_CP("com{}zaxxer", "HikariCP", "maven", "hikari-cp", Relocation.of("hikari", "com{}zaxxer{}hikari")),
	MONGODB_DRIVER_CORE("org{}mongodb", "mongodb-driver-core", "maven", "mongodb-driver-core",
			Relocation.of("mongodb", "com{}mongodb"), Relocation.of("bson", "org{}bson")),
	MONGODB_DRIVER_SYNC("org{}mongodb", "mongodb-driver-sync", "maven", "mongodb-driver-sync",
			Relocation.of("mongodb", "com{}mongodb"), Relocation.of("bson", "org{}bson")) {
		@Override
		public String getVersion() {
			return Dependency.MONGODB_DRIVER_CORE.getVersion();
		}
	},
	MONGODB_DRIVER_BSON("org{}mongodb", "bson", "maven", "mongodb-bson", Relocation.of("mongodb", "com{}mongodb"),
			Relocation.of("bson", "org{}bson")) {
		@Override
		public String getVersion() {
			return Dependency.MONGODB_DRIVER_CORE.getVersion();
		}
	},
	COMMONS_POOL_2("org{}apache{}commons", "commons-pool2", "maven", "commons-pool",
			Relocation.of("commonspool2", "org{}apache{}commons{}pool2")),
	GSON("com.google.code.gson", "gson", "maven", "gson"),
	JEDIS("redis{}clients", "jedis", "maven", "jedis", Relocation.of("jedis", "redis{}clients{}jedis"),
			Relocation.of("commonspool2", "org{}apache{}commons{}pool2")),
	EXP4J("net{}objecthunter", "exp4j", "maven", "exp4j", Relocation.of("exp4j", "net{}objecthunter{}exp4j")),
	// no relocations for sl4fj, only used for sqlite
	SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "maven", "slf4j_simple") {
		@Override
		public String getVersion() {
			return Dependency.SLF4J_API.getVersion();
		}
	},
	SLF4J_API("org.slf4j", "slf4j-api", "maven", "slf4j");

	private final String groupId;
	private final String artifactId;
	private final String repo;
	private final String customArtifactID;
	private final List<Relocation> relocations;

	private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

	private Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String repo,
			@NonNull String customArtifactID) {
		this(groupId, artifactId, repo, customArtifactID, new Relocation[0]);
	}

	private Dependency(@NonNull String groupId, @NonNull String artifactId, @NonNull String repo,
			@NonNull String customArtifactID, @NonNull Relocation... relocations) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.repo = repo;
		this.customArtifactID = customArtifactID;
		this.relocations = ImmutableList.copyOf(relocations);
	}

	@NonNull
	public String getVersion() {
		return HellblockProperties.getValue(this.customArtifactID);
	}

	@NonNull
	public String getCustomArtifactID() {
		return this.customArtifactID;
	}

	private static String rewriteEscaping(@NonNull String s) {
		return s.replace("{}", ".");
	}

	@NonNull
	public String getFileName(@Nullable String classifier) {
		String name = this.customArtifactID.toLowerCase(Locale.ROOT).replace('_', '-');
		String extra = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;
		return name + "-" + this.getVersion() + extra + ".jar";
	}

	@NonNull
	public String getMavenRepoPath() {
		return String.format(MAVEN_FORMAT, rewriteEscaping(this.groupId).replace(".", "/"),
				rewriteEscaping(this.artifactId), getVersion(), rewriteEscaping(this.artifactId), getVersion());
	}

	@NonNull
	public List<Relocation> getRelocations() {
		return this.relocations;
	}

	@NonNull
	public String getRepo() {
		return this.repo;
	}
}