package com.swiftlicious.hellblock.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.nms.NMSHandler;
import com.swiftlicious.hellblock.nms.beam.BeamAnimation;
import com.swiftlicious.hellblock.nms.exception.UnsupportedVersionException;
import com.swiftlicious.hellblock.nms.invasion.PiglinAIUtils;

/**
 * This class is responsible for managing version-related information.
 */
public class VersionHelper {

	private static String latestVersion;

	// Method to asynchronously check for plugin updates
	public static final Function<HellblockPlugin, CompletableFuture<Boolean>> checkUpdate = (plugin) -> {
		CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();

		plugin.getScheduler().async().execute(() -> {
			try {
				URL url = URI.create("https://api.github.com/repos/Swiftlicious01/Hellblock/releases/latest").toURL();
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(60000);
				conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

				if (conn instanceof HttpURLConnection http) {
					int status = http.getResponseCode();
					if (status == 404) {
						plugin.getPluginLogger().warn("No release found on GitHub. Make sure a release is published.");
						updateFuture.complete(false);
						return;
					}
				}

				StringBuilder json = new StringBuilder();
				try (InputStream inputStream = conn.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

					reader.lines().forEach(json::append);
				}

				JsonObject root = JsonParser.parseString(json.toString()).getAsJsonObject();
				String tag = root.get("tag_name").getAsString(); // e.g. "v1.3.0"

				latestVersion = tag.replaceFirst("^v", "").trim(); // Store latest version
				String current = getPluginVersion().trim();

				boolean updateAvailable = compareVer(latestVersion, current);
				updateFuture.complete(updateAvailable);

			} catch (Exception ex) {
				plugin.getPluginLogger().warn("Error occurred when checking update: " + ex.getMessage(), ex);
				updateFuture.completeExceptionally(ex);
			}
		});

		return updateFuture;
	};

	private static String serverVersion;
	private static boolean isMojMap;
	private static int version;
	private static String pluginVersion;
	private static NMSHandler nmsManager;
	private static PiglinAIUtils invasionAIManager;
	private static List<String> supportedVersions;

	@SuppressWarnings("deprecation")
	public static void init(@NotNull String serverVersion) {
		// Get the server version
		VersionHelper.serverVersion = serverVersion;
		// get the end number of the version
		version = parseVersionToInteger(serverVersion);

		supportedVersions = List.of("1.17", "1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3",
				"1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1",
				"1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11");

		checkMojMap();
		
		// Get the plugin version
		pluginVersion = HellblockPlugin.getInstance().getDescription().getVersion();
		nmsManager = getNMSHandler();
		invasionAIManager = getInvasionAIHelper();
	}

	@NotNull
	private static NMSHandler getNMSHandler() {
		String bukkitVersion = getServerVersion();
		String version = resolveInternalVersion(bukkitVersion);

		try {
			// Choose namespace based on platform
			String basePackage = isPaper() ? "com.swiftlicious.hellblock.paper" : "com.swiftlicious.hellblock.spigot";

			String className = basePackage + ".v" + version.toLowerCase() + ".NMSUtils" + version;

			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return (NMSHandler) constructor.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(
					"Failed to initialize NMS handler for version " + bukkitVersion + " (paper=" + isPaper() + ")", ex);
		}
	}

	@NotNull
	public static BeamAnimation createNewBeamAnimation(@NotNull Collection<Player> viewers, @NotNull Location location,
			int durationTicks) {
		String bukkitVersion = getServerVersion();
		String version = resolveInternalVersion(bukkitVersion);

		try {
			// Choose namespace based on platform
			String basePackage = isPaper() ? "com.swiftlicious.hellblock.paper" : "com.swiftlicious.hellblock.spigot";

			String className = basePackage + ".v" + version.toLowerCase() + ".BeamAnimationTask";

			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getConstructor(Collection.class, Location.class, int.class);
			constructor.setAccessible(true);
			return (BeamAnimation) constructor.newInstance(viewers, location, durationTicks);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(
					"Failed to create BeamAnimationTask for version " + bukkitVersion + " (paper=" + isPaper() + ")",
					ex);
		}
	}

	@NotNull
	private static PiglinAIUtils getInvasionAIHelper() {
		String bukkitVersion = getServerVersion();
		String version = resolveInternalVersion(bukkitVersion);

		try {
			// Choose namespace based on platform
			String basePackage = isPaper() ? "com.swiftlicious.hellblock.paper" : "com.swiftlicious.hellblock.spigot";

			String className = basePackage + ".v" + version.toLowerCase() + ".InvasionAIGoals";

			Class<?> clazz = Class.forName(className);
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return (PiglinAIUtils) constructor.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(
					"Failed to retrieve invasion AI goals for version " + bukkitVersion + " (paper=" + isPaper() + ")",
					ex);
		}
	}

	/**
	 * Maps Bukkit's version string (e.g., "1.21.3") to an internal NMS version like
	 * "1_21_R3". This ensures consistency across different version-dependent
	 * features.
	 *
	 * @param bukkitVersion The version string, e.g., "1.20.4"
	 * @return The internal version string, e.g., "1_20_R3"
	 * @throws UnsupportedVersionException if the version is not recognized
	 */
	@NotNull
	public static String resolveInternalVersion(@NotNull String bukkitVersion) {
		return switch (bukkitVersion) {
		case "1.21.11", "1.21.10", "1.21.9" -> "1_21_R6";
		case "1.21.8", "1.21.7", "1.21.6" -> "1_21_R5";
		case "1.21.5" -> "1_21_R4";
		case "1.21.4" -> "1_21_R3";
		case "1.21.3", "1.21.2" -> "1_21_R2";
		case "1.21.1", "1.21" -> "1_21_R1";
		case "1.20.6", "1.20.5" -> "1_20_R4";
		case "1.20.4", "1.20.3" -> "1_20_R3";
		case "1.20.2" -> "1_20_R2";
		case "1.20.1", "1.20" -> "1_20_R1";
		case "1.19.4" -> "1_19_R3";
		case "1.19.3" -> "1_19_R2";
		case "1.19.2", "1.19.1", "1.19" -> "1_19_R1";
		case "1.18.2" -> "1_18_R2";
		case "1.18.1", "1.18" -> "1_18_R1";
		case "1.17.1", "1.17" -> "1_17_R1";
		default -> throw new UnsupportedVersionException("Unsupported server version: " + bukkitVersion);
		};
	}

	private static int parseVersionToInteger(@NotNull String versionString) {
		int major = 0;
		int minor = 0;
		int currentNumber = 0;
		int part = 0;
		for (int i = 0; i < versionString.length(); i++) {
			char c = versionString.charAt(i);
			if (c >= '0' && c <= '9') {
				currentNumber = currentNumber * 10 + (c - '0');
			} else if (c == '.') {
				if (part == 1) {
					major = currentNumber;
				}
				part++;
				currentNumber = 0;
				if (part > 2) {
					break;
				}
			}
		}
		if (part == 1) {
			major = currentNumber;
		} else if (part == 2) {
			minor = currentNumber;
		}
		return 10000 + major * 100 + minor;
	}

	@NotNull
	public static NMSHandler getNMSManager() {
		return nmsManager;
	}

	@NotNull
	public static PiglinAIUtils getInvasionAIManager() {
		return invasionAIManager;
	}

	@NotNull
	public static String getLatestVersion() {
		return latestVersion != null ? latestVersion : "unknown";
	}

	@NotNull
	public static String getCurrentVersion() {
		return pluginVersion != null ? pluginVersion : "unknown";
	}

	private static void checkMojMap() {
		// Check if the server is Mojmap
		try {
			Class.forName("net.minecraft.network.protocol.game.ClientboundBossEventPacket");
			isMojMap = true;
		} catch (ClassNotFoundException ignored) {
		}
	}

	public static boolean isMojMap() {
		return isMojMap;
	}

	public static boolean isSpigot() {
		return PlatformHelper.isSpigot;
	}

	public static boolean isPaper() {
		return PlatformHelper.isPaper;
	}

	public static boolean isFolia() {
		return PlatformHelper.isFolia;
	}

	public static boolean isPaperFork() {
		return PlatformHelper.isFolia || PlatformHelper.isPaper;
	}

	public static boolean isPaperForkPreInit() {
		try {
			Class.forName("io.papermc.paper.configuration.Configuration");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	@NotNull
	public static String getPluginVersion() {
		return pluginVersion;
	}

	@NotNull
	public static String getServerVersion() {
		return serverVersion;
	}

	public static int getPlatformId() {
		return PlatformHelper.platformId;
	}

	@NotNull
	public static List<String> getSupportedVersions() {
		return supportedVersions;
	}

	public static boolean isVersionNewerThan1_17() {
		return version >= 11700;
	}

	public static boolean isVersionNewerThan1_17_1() {
		return version >= 11701;
	}

	public static boolean isVersionNewerThan1_18() {
		return version >= 11800;
	}

	public static boolean isVersionNewerThan1_18_1() {
		return version >= 11801;
	}

	public static boolean isVersionNewerThan1_18_2() {
		return version >= 11802;
	}

	public static boolean isVersionNewerThan1_19() {
		return version >= 11900;
	}

	public static boolean isVersionNewerThan1_19_1() {
		return version >= 11901;
	}

	public static boolean isVersionNewerThan1_19_2() {
		return version >= 11902;
	}

	public static boolean isVersionNewerThan1_19_3() {
		return version >= 11903;
	}

	public static boolean isVersionNewerThan1_19_4() {
		return version >= 11904;
	}

	public static boolean isVersionNewerThan1_20() {
		return version >= 12000;
	}

	public static boolean isVersionNewerThan1_20_1() {
		return version >= 12001;
	}

	public static boolean isVersionNewerThan1_20_2() {
		return version >= 12002;
	}

	public static boolean isVersionNewerThan1_20_3() {
		return version >= 12003;
	}

	public static boolean isVersionNewerThan1_20_4() {
		return version >= 12004;
	}

	public static boolean isVersionNewerThan1_20_5() {
		return version >= 12005;
	}

	public static boolean isVersionNewerThan1_20_6() {
		return version >= 12006;
	}

	public static boolean isVersionNewerThan1_21() {
		return version >= 12100;
	}

	public static boolean isVersionNewerThan1_21_1() {
		return version >= 12101;
	}

	public static boolean isVersionNewerThan1_21_2() {
		return version >= 12102;
	}

	public static boolean isVersionNewerThan1_21_3() {
		return version >= 12103;
	}

	public static boolean isVersionNewerThan1_21_4() {
		return version >= 12104;
	}

	public static boolean isVersionNewerThan1_21_5() {
		return version >= 12105;
	}

	public static boolean isVersionNewerThan1_21_6() {
		return version >= 12106;
	}

	public static boolean isVersionNewerThan1_21_7() {
		return version >= 12107;
	}

	public static boolean isVersionNewerThan1_21_8() {
		return version >= 12108;
	}

	public static boolean isVersionNewerThan1_21_9() {
		return version >= 12109;
	}

	public static boolean isVersionNewerThan1_21_10() {
		return version >= 12110;
	}

	public static boolean isVersionNewerThan1_21_11() {
		return version >= 12111;
	}

	// Method to compare two version strings, handles pre-release tags like
	// "1.0-ALPHA"
	private static boolean compareVer(@NotNull String remote, @NotNull String local) {
		String[] r = remote.split("\\.");
		String[] l = local.split("\\.");

		int len = Math.max(r.length, l.length);
		for (int i = 0; i < len; i++) {
			int rPart = i < r.length ? parseVersionNumber(r[i]) : 0;
			int lPart = i < l.length ? parseVersionNumber(l[i]) : 0;

			if (rPart > lPart)
				return true; // Update available
			if (rPart < lPart)
				return false; // Already up-to-date
		}
		return false; // Versions are equal
	}

	// Safely parse numeric portion of version segment, stripping any "-ALPHA",
	// "-BETA", etc.
	private static int parseVersionNumber(String part) {
		try {
			// Strip pre-release or metadata after hyphen
			int dashIndex = part.indexOf('-');
			if (dashIndex >= 0) {
				part = part.substring(0, dashIndex);
			}
			return Integer.parseInt(part);
		} catch (NumberFormatException e) {
			return 0; // fallback
		}
	}
	
	public final class PlatformHelper {
		
	    public static boolean isSpigot;
	    public static boolean isPaper;
	    public static boolean isFolia;
	    public static int platformId; // 0=bukkit,1=spigot,2=paper,3=folia

	    static {
	        detectPlatform();
	    }

	    private static void detectPlatform() {
	        String name = Bukkit.getName().toLowerCase(Locale.ROOT);
	        String serverClass = Bukkit.getServer().getClass().getName();

	        if (serverClass.contains("folia") || hasClass("io.papermc.paper.threadedregions.RegionizedServer")) {
	            isFolia = true;
	            platformId = 3;
	        } else if (name.contains("purpur") || name.contains("paper") || serverClass.contains("paper")) {
	            isPaper = true;
	            platformId = 2;
	        } else if (name.contains("spigot") || hasClass("org.spigotmc.SpigotConfig")) {
	            isSpigot = true;
	            platformId = 1;
	        } else {
	            platformId = 0;
	        }
	    }

	    private static boolean hasClass(String name) {
	        try {
	            Class.forName(name, false, Bukkit.class.getClassLoader());
	            return true;
	        } catch (ClassNotFoundException e) {
	            return false;
	        }
	    }
	}
}