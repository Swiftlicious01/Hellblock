package com.swiftlicious.hellblock.database.dependency;

import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.swiftlicious.hellblock.database.dependency.relocation.Relocation;

/**
 * The dependencies used by Hellblock.
 */
public enum Dependency {

	ASM("org.ow2.asm", "asm", "maven", "asm"), ASM_COMMONS("org.ow2.asm", "asm-commons", "maven", "asm-commons"),
	JAR_RELOCATOR("me.lucko", "jar-relocator", "maven", "jar-relocator"),
	H2_DRIVER("com.h2database", "h2", "maven", "h2-driver"),
	SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "maven", "sqlite-driver"),
	BOOSTED_YAML("dev{}dejvokep", "boosted-yaml", "maven", "boosted-yaml",
			Relocation.of("boostedyaml", "dev{}dejvokep{}boostedyaml")),
	MARIADB_DRIVER("org{}mariadb{}jdbc", "mariadb-java-client", "maven", "mariadb-java-client",
			Relocation.of("mariadb", "org{}mariadb")),
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
	SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "maven", "slf4j_simple") {
		@Override
		public String getVersion() {
			return Dependency.SLF4J_API.getVersion();
		}
	},
	SLF4J_API("org.slf4j", "slf4j-api", "maven", "slf4j"),
	COMMAND_API("dev{}jorel", "commandapi-bukkit-shade", "maven", "commandapi-bukkit",
			Relocation.of("commandapi", "dev{}jorel{}commandapi")),
	INV_UI("xyz{}xenondevs{}invui", "invui-core", "xenondevs", "invui-core"),
	INV_UI_ACCESS("xyz{}xenondevs{}invui", "inventory-access", "xenondevs", "inventory-access") {
		@Override
		public String getVersion() {
			return Dependency.INV_UI.getVersion();
		}
	},
	INV_UI_NMS("xyz{}xenondevs{}invui", String.format("inventory-access-%s", getInvUINms()), "xenondevs",
			String.format("inventory-access-%s", getInvUINms())) {
		@Override
		public String getVersion() {
			return Dependency.INV_UI.getVersion();
		}
	};

	private final String groupId;
	private final String artifactId;
	private final String repo;
	private final String customArtifactID;
	private final List<Relocation> relocations;

	private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

	Dependency(String groupId, String artifactId, String repo, String customArtifactID) {
		this(groupId, artifactId, repo, customArtifactID, new Relocation[0]);
	}

	Dependency(String groupId, String artifactId, String repo, String customArtifactID, Relocation... relocations) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.repo = repo;
		this.customArtifactID = customArtifactID;
		this.relocations = ImmutableList.copyOf(relocations);
	}

	public String getVersion() {
		return HellblockProperties.getValue(customArtifactID);
	}

	private static String rewriteEscaping(String s) {
		return s.replace("{}", ".");
	}

	public String getFileName(String classifier) {
		String name = customArtifactID.toLowerCase(Locale.ROOT).replace('_', '-');
		String extra = classifier == null || classifier.isEmpty() ? "" : "-" + classifier;
		return name + "-" + this.getVersion() + extra + ".jar";
	}

	public String getMavenRepoPath() {
		return String.format(MAVEN_FORMAT, rewriteEscaping(groupId).replace(".", "/"), rewriteEscaping(artifactId),
				getVersion(), rewriteEscaping(artifactId), getVersion());
	}

	public List<Relocation> getRelocations() {
		return this.relocations;
	}

	@Nullable
	public String getRepo() {
		return repo;
	}

	private static String getInvUINms() {
		String version = Bukkit.getServer().getBukkitVersion().split("-")[0];
		String artifact;
		switch (version) {
		case "1.17.1" -> artifact = "r9";
		case "1.18.1" -> artifact = "r10";
		case "1.18.2" -> artifact = "r11";
		case "1.19.1" -> artifact = "r12";
		case "1.19.2" -> artifact = "r13";
		case "1.19.3" -> artifact = "r14";
		case "1.19.4" -> artifact = "r15";
		case "1.20.1" -> artifact = "r16";
		case "1.20.2" -> artifact = "r17";
		case "1.20.3" -> artifact = "r18";
		case "1.20.4" -> artifact = "r19";
		case "1.20.5" -> artifact = "r19";
		case "1.20.6" -> artifact = "r19";
		case "1.21.1" -> artifact = "r20";
		case "1.21.2" -> artifact = "r21";
		case "1.21.3" -> artifact = "r21";
		default -> artifact = "r21";
		}
		return artifact;
	}
}