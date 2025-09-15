package com.swiftlicious.hellblock.handlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.bukkit.Bukkit;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.nms.NMSHandler;
import com.swiftlicious.hellblock.nms.exception.UnsupportedVersionException;

/**
 * This class is responsible for managing version-related information.
 */
public class VersionHelper {

	// Method to asynchronously check for plugin updates
	public static final Function<HellblockPlugin, CompletableFuture<Boolean>> checkUpdate = (plugin) -> {
		CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
		plugin.getScheduler().async().execute(() -> {
			try {
				URL url = URI.create("https://github.com/Swiftlicious01/Hellblock/releases").toURL();
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(60000);
				String newest;
				try (InputStream inputStream = conn.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
					newest = reader.readLine();
				}
				String current = getPluginVersion();
				if (!compareVer(newest, current)) {
					updateFuture.complete(false);
					return;
				}
				updateFuture.complete(true);
			} catch (Exception ex) {
				HellblockPlugin.getInstance().getPluginLogger().warn("Error occurred when checking update.");
				updateFuture.completeExceptionally(ex);
			}
		});
		return updateFuture;
	};

	private static String serverVersion;
	private static boolean isSpigot;
	private static boolean isPaper;
	private static boolean isFolia;
	private static float version;
	private static String pluginVersion;
	private static NMSHandler nmsManager;
	private static List<String> supportedVersions;

	@SuppressWarnings("deprecation")
	public static void init(String serverVersion) {
		// Get the server version
		VersionHelper.serverVersion = serverVersion;
		// get the end number of the version
		String[] split = serverVersion.split("\\.");
		version = Float.parseFloat(split[1] + "." + (split.length == 3 ? split[2] : "0"));

		supportedVersions = List.of("1.17.1", "1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.19.3",
				"1.19.4", "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1",
				"1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8");

		// Check if the server is Spigot or Paper
		try {
			Class.forName("com.destroystokyo.paper.ParticleBuilder");
			isPaper = true;
		} catch (ClassNotFoundException checkFolia) {
			try {
				Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
				isFolia = true;
			} catch (ClassNotFoundException checkSpigot) {
				try {
					Class.forName("org.spigotmc.SpigotConfig");
					isSpigot = true;
				} catch (ClassNotFoundException noServerCoreFound) {
					// could not find either paper or spigot.
					Bukkit.getPluginManager().disablePlugin(HellblockPlugin.getInstance());
					HellblockPlugin.getInstance().getPluginLogger().severe(
							"Paper, Folia or Spigot could not be found running this server, disabling Hellblock.",
							noServerCoreFound);
					return;
				}
			}
		}
		// Get the plugin version
		pluginVersion = HellblockPlugin.getInstance().getDescription().getVersion();
		nmsManager = getNMSHandler();
	}

	private static NMSHandler getNMSHandler() {
		String bukkitVersion = getServerVersion();
		String packageName;
		switch (bukkitVersion) {
		case "1.21.6", "1.21.7", "1.21.8" -> packageName = "1_21_R5";
		case "1.21.5" -> packageName = "1_21_R4";
		case "1.21.4" -> packageName = "1_21_R3";
		case "1.21.2", "1.21.3" -> packageName = "1_21_R2";
		case "1.21", "1.21.1" -> packageName = "1_21_R1";
		case "1.20.5", "1.20.6" -> packageName = "1_20_R4";
		case "1.20.3", "1.20.4" -> packageName = "1_20_R3";
		case "1.20.2" -> packageName = "1_20_R2";
		case "1.20", "1.20.1" -> packageName = "1_20_R1";
		case "1.19.4" -> packageName = "1_19_R3";
		case "1.19.3" -> packageName = "1_19_R2";
		case "1.19.2", "1.19.1" -> packageName = "1_19_R1";
		case "1.18.2" -> packageName = "1_18_R2";
		case "1.18.1", "1.18" -> packageName = "1_18_R1";
		case "1.17.1" -> packageName = "1_17_R1";
		default -> throw new UnsupportedVersionException("Unsupported server version: " + bukkitVersion);
		}
		try {
			Class<?> clazz = Class
					.forName("com.swiftlicious.hellblock.v" + packageName.toLowerCase() + ".NMSUtils" + packageName);
			Constructor<?> constructor = clazz.getDeclaredConstructor();
			constructor.setAccessible(true);
			return (NMSHandler) constructor.newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to initialize nms data for cross compatibility.", ex);
		}
	}

	public static NMSHandler getNMSManager() {
		return nmsManager;
	}

	public static boolean isPaper() {
		return isPaper;
	}

	public static boolean isSpigot() {
		return isSpigot;
	}

	public static boolean isFolia() {
		return isFolia;
	}

	public static String getPluginVersion() {
		return pluginVersion;
	}

	public static String getServerVersion() {
		return serverVersion;
	}

	public static List<String> getSupportedVersions() {
		return supportedVersions;
	}

	public static boolean isVersionNewerThan1_17() {
		return version >= 17;
	}

	public static boolean isVersionNewerThan1_17_1() {
		return version >= 17.09;
	}

	public static boolean isVersionNewerThan1_18() {
		return version >= 18;
	}

	public static boolean isVersionNewerThan1_18_1() {
		return version >= 18.09;
	}

	public static boolean isVersionNewerThan1_18_2() {
		return version >= 18.19;
	}

	public static boolean isVersionNewerThan1_19() {
		return version >= 19;
	}

	public static boolean isVersionNewerThan1_19_1() {
		return version >= 19.09;
	}

	public static boolean isVersionNewerThan1_19_2() {
		return version >= 19.19;
	}

	public static boolean isVersionNewerThan1_19_3() {
		return version >= 19.29;
	}

	public static boolean isVersionNewerThan1_19_4() {
		return version >= 19.39;
	}

	public static boolean isVersionNewerThan1_20() {
		return version >= 20;
	}

	public static boolean isVersionNewerThan1_20_1() {
		return version >= 20.09;
	}

	public static boolean isVersionNewerThan1_20_2() {
		return version >= 20.19;
	}

	public static boolean isVersionNewerThan1_20_3() {
		return version >= 20.29;
	}

	public static boolean isVersionNewerThan1_20_4() {
		return version >= 20.39;
	}

	public static boolean isVersionNewerThan1_20_5() {
		return version >= 20.49;
	}

	public static boolean isVersionNewerThan1_20_6() {
		return version >= 20.59;
	}

	public static boolean isVersionNewerThan1_21() {
		return version >= 21;
	}

	public static boolean isVersionNewerThan1_21_1() {
		return version >= 21.09;
	}

	public static boolean isVersionNewerThan1_21_2() {
		return version >= 21.19;
	}

	public static boolean isVersionNewerThan1_21_3() {
		return version >= 21.29;
	}

	public static boolean isVersionNewerThan1_21_4() {
		return version >= 21.39;
	}

	public static boolean isVersionNewerThan1_21_5() {
		return version >= 21.49;
	}

	public static boolean isVersionNewerThan1_21_6() {
		return version >= 21.59;
	}

	public static boolean isVersionNewerThan1_21_7() {
		return version >= 21.69;
	}

	public static boolean isVersionNewerThan1_21_8() {
		return version >= 21.79;
	}

	// Method to compare two version strings
	private static boolean compareVer(String newV, String currentV) {
		if (newV == null || currentV == null || newV.isEmpty() || currentV.isEmpty()) {
			return false;
		}
		String[] newVS = newV.split("\\.");
		String[] currentVS = currentV.split("\\.");
		int maxL = Math.min(newVS.length, currentVS.length);
		for (int i = 0; i < maxL; i++) {
			try {
				String[] newPart = newVS[i].split("-");
				String[] currentPart = currentVS[i].split("-");
				int newNum = Integer.parseInt(newPart[0]);
				int currentNum = Integer.parseInt(currentPart[0]);
				if (newNum > currentNum) {
					return true;
				} else if (newNum < currentNum) {
					return false;
				} else if (newPart.length > 1 && currentPart.length > 1) {
					String[] newHotfix = newPart[1].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
					String[] currentHotfix = currentPart[1].split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
					if (newHotfix.length == 2 && currentHotfix.length == 1)
						return true;
					else if (newHotfix.length > 1 && currentHotfix.length > 1) {
						int newHotfixNum = Integer.parseInt(newHotfix[1]);
						int currentHotfixNum = Integer.parseInt(currentHotfix[1]);
						if (newHotfixNum > currentHotfixNum) {
							return true;
						} else if (newHotfixNum < currentHotfixNum) {
							return false;
						} else {
							return newHotfix[0].compareTo(currentHotfix[0]) > 0;
						}
					}
				} else if (newPart.length > 1) {
					return true;
				} else if (currentPart.length > 1) {
					return false;
				}
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		return newVS.length > currentVS.length;
	}
}