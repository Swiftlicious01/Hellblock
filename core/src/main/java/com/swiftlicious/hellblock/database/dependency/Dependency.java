package com.swiftlicious.hellblock.database.dependency;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.swiftlicious.hellblock.database.dependency.relocation.Relocation;

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
	CLOUD_CORE("org{}incendo", "cloud-core", "maven", "cloud-core", Relocation.of("cloud", "org{}incendo{}cloud"),
			Relocation.of("geantyref", "io{}leangen{}geantyref")),
	CLOUD_BRIGADIER("org{}incendo", "cloud-brigadier", "maven", "cloud-brigadier",
			Relocation.of("cloud", "org{}incendo{}cloud"), Relocation.of("geantyref", "io{}leangen{}geantyref")),
	CLOUD_SERVICES("org{}incendo", "cloud-services", "maven", "cloud-services",
			Relocation.of("cloud", "org{}incendo{}cloud"), Relocation.of("geantyref", "io{}leangen{}geantyref")),
	CLOUD_BUKKIT("org{}incendo", "cloud-bukkit", "maven", "cloud-bukkit", Relocation.of("cloud", "org{}incendo{}cloud"),
			Relocation.of("geantyref", "io{}leangen{}geantyref")),
	CLOUD_PAPER("org{}incendo", "cloud-paper", "maven", "cloud-paper", Relocation.of("cloud", "org{}incendo{}cloud"),
			Relocation.of("geantyref", "io{}leangen{}geantyref")),
	CLOUD_MINECRAFT_EXTRAS("org{}incendo", "cloud-minecraft-extras", "maven", "cloud-minecraft-extras",
			Relocation.of("cloud", "org{}incendo{}cloud"), Relocation.of("adventure", "net{}kyori{}adventure"),
			Relocation.of("option", "net{}kyori{}option"), Relocation.of("examination", "net{}kyori{}examination"),
			Relocation.of("geantyref", "io{}leangen{}geantyref")),
	GEANTY_REF("io{}leangen{}geantyref", "geantyref", "maven", "geantyref",
			Relocation.of("geantyref", "io{}leangen{}geantyref")),
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
	CAFFEINE("com{}github{}ben-manes{}caffeine", "caffeine", "maven", "caffeine",
			Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")),
	JEDIS("redis{}clients", "jedis", "maven", "jedis", Relocation.of("jedis", "redis{}clients{}jedis"),
			Relocation.of("commonspool2", "org{}apache{}commons{}pool2")),
	EXP4J("net{}objecthunter", "exp4j", "maven", "exp4j", Relocation.of("exp4j", "net{}objecthunter{}exp4j")),
	LZ4("org{}lz4", "lz4-java", "maven", "lz4-java", Relocation.of("jpountz", "net{}jpountz")),
	// no relocations for sl4fj, only used for sqlite
	SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "maven", "slf4j_simple") {
		@Override
		public String getVersion() {
			return Dependency.SLF4J_API.getVersion();
		}
	},
	SLF4J_API("org.slf4j", "slf4j-api", "maven", "slf4j"), ZSTD("com.github.luben", "zstd-jni", "maven", "zstd-jni");

	private final String groupId;
	private final String artifactId;
	private final String repo;
	private final String customArtifactID;
	private final List<Relocation> relocations;

	private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

	private Dependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String repo,
			@NotNull String customArtifactID) {
		this(groupId, artifactId, repo, customArtifactID, new Relocation[0]);
	}

	private Dependency(@NotNull String groupId, @NotNull String artifactId, @NotNull String repo,
			@NotNull String customArtifactID, @NotNull Relocation... relocations) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.repo = repo;
		this.customArtifactID = customArtifactID;
		this.relocations = ImmutableList.copyOf(relocations);
	}

	@NotNull
	public String getVersion() {
		return HellblockProperties.getValue(this.customArtifactID);
	}

	@NotNull
	public String getCustomArtifactID() {
		return this.customArtifactID;
	}

	private static String rewriteEscaping(@NotNull String s) {
		return s.replace("{}", ".");
	}

	@NotNull
	public String getFileName(@Nullable String classifier) {
		final String name = this.customArtifactID.toLowerCase(Locale.ROOT).replace('_', '-');
		final String extra = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;
		return name + "-" + this.getVersion() + extra + ".jar";
	}

	@NotNull
	public String getMavenRepoPath() {
		return MAVEN_FORMAT.formatted(rewriteEscaping(this.groupId).replace(".", "/"), rewriteEscaping(this.artifactId),
				getVersion(), rewriteEscaping(this.artifactId), getVersion());
	}

	@NotNull
	public List<Relocation> getRelocations() {
		return this.relocations;
	}

	@NotNull
	public String getRepo() {
		return this.repo;
	}
}