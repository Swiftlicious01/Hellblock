package com.swiftlicious.hellblock.database.dependency;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

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
	BSTATS_BASE("org{}bstats", "bstats-base", "maven", "bstats-base", Relocation.of("bstats", "org{}bstats")),
	BSTATS_BUKKIT("org{}bstats", "bstats-bukkit", "maven", "bstats-bukkit", Relocation.of("bstats", "org{}bstats")) {
		@Override
		public String getVersion() {
			return Dependency.BSTATS_BASE.getVersion();
		}
	},
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
	INV_UI("xyz{}xenondevs{}invui", "invui-core", "xenondevs", "invui-core",
			Relocation.of("invui", "xyz{}xenondevs{}invui-core")),
	INV_UI_ACCESS("xyz{}xenondevs{}invui", "inventory-access", "xenondevs", "inventory-access",
			Relocation.of("inventoryaccess", "xyz{}xenondevs{}inventoryaccess")) {
		@Override
		public String getVersion() {
			return Dependency.INV_UI.getVersion();
		}
	},
	INV_UI_NMS("xyz{}xenondevs{}invui", String.format("inventory-access-%s", getInvUINms()), "xenondevs",
			String.format("inventory-access-%s", getInvUINms()),
			Relocation.of(String.format("inventoryaccess%s", getInvUINms()),
					String.format("xyz{}xenondevs{}inventoryaccess{}inventory-access-%s", getInvUINms()))) {
		@Override
		public String getVersion() {
			return Dependency.INV_UI.getVersion();
		}
	};

	private final List<Relocation> relocations;
	private final String repo;
	private final String groupId;
	private String rawArtifactId;
	private String customArtifactID;

	private static final String MAVEN_FORMAT = "%s/%s/%s/%s-%s.jar";

	Dependency(String groupId, String rawArtifactId, String repo, String customArtifactID) {
		this(groupId, rawArtifactId, repo, customArtifactID, new Relocation[0]);
	}

	Dependency(String groupId, String rawArtifactId, String repo, String customArtifactID, Relocation... relocations) {
		this.rawArtifactId = rawArtifactId;
		this.groupId = groupId;
		this.relocations = new ArrayList<>(Arrays.stream(relocations).toList());
		this.repo = repo;
		this.customArtifactID = customArtifactID;
	}

	public Dependency setCustomArtifactID(String customArtifactID) {
		this.customArtifactID = customArtifactID;
		return this;
	}

	public Dependency setRawArtifactID(String artifactId) {
		this.rawArtifactId = artifactId;
		return this;
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

	String getMavenRepoPath() {
		return String.format(MAVEN_FORMAT, rewriteEscaping(groupId).replace(".", "/"), rewriteEscaping(rawArtifactId),
				getVersion(), rewriteEscaping(rawArtifactId), getVersion());
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