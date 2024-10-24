package com.swiftlicious.hellblock.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

import com.swiftlicious.hellblock.HellblockPlugin;

/**
 * This class implements the VersionManager interface and is responsible for
 * managing version-related information.
 */
public class VersionManager implements VersionManagerInterface {

	private final String serverVersion;
	private final HellblockPlugin instance;
	private boolean isSpigot = false;
	private boolean isPaper = false;
	private boolean isFolia = false;
	private String pluginVersion = "";
	private List<String> supportedVersions;

	public VersionManager(HellblockPlugin plugin) {
		this.instance = plugin;

		// Get the server version
		this.serverVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];

		// Check if the server is Spigot or Paper
		try {
			Class.forName("com.destroystokyo.paper.ParticleBuilder");
			this.isPaper = true;
		} catch (ClassNotFoundException checkFolia) {
			try {
				Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
				this.isFolia = true;
			} catch (ClassNotFoundException checkSpigot) {
				try {
					Class.forName("org.spigotmc.SpigotConfig");
					this.isSpigot = true;
				} catch (ClassNotFoundException noServerCoreFound) {
					// could not find either paper or spigot.
					Bukkit.getPluginManager().disablePlugin(plugin);
					LogUtils.severe("Paper, Folia or Spigot could not be found running this server, disabling Hellblock.",
							noServerCoreFound);
					return;
				}
			}
		}

		this.supportedVersions = List.of("1.20.5", "1.21.1", "1.21.2", "1.21.3");
		// Get the plugin version
		this.pluginVersion = plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean isPaper() {
		return isPaper;
	}

	@Override
	public boolean isSpigot() {
		return isSpigot;
	}

	@Override
	public boolean isFolia() {
		return isFolia;
	}

	@Override
	public String getPluginVersion() {
		return pluginVersion;
	}

	@Override
	public String getServerVersion() {
		return serverVersion;
	}

	@Override
	public List<String> getSupportedVersions() {
		return supportedVersions;
	}

	// Method to asynchronously check for plugin updates
	@Override
	public CompletableFuture<Boolean> checkUpdate() {
		CompletableFuture<Boolean> updateFuture = new CompletableFuture<>();
		instance.getScheduler().runTaskAsync(() -> {
			try {
				URL url = URI.create("https://github.com/repo/Swiftlicious01/Hellblock/releases").toURL();
				URLConnection conn = url.openConnection();
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(60000);
				InputStream inputStream = conn.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String newest = bufferedReader.readLine();
				String current = getPluginVersion();
				inputStream.close();
				if (!compareVer(newest, current)) {
					updateFuture.complete(false);
					return;
				}
				updateFuture.complete(true);
			} catch (Exception exception) {
				LogUtils.warn("Error occurred when checking update.", exception);
				updateFuture.complete(false);
			}
		});
		return updateFuture;
	}

	// Method to compare two version strings
	private boolean compareVer(String newV, String currentV) {
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
