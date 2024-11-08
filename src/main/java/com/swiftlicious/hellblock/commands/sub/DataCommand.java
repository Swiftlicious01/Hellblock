package com.swiftlicious.hellblock.commands.sub;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.DataStorageInterface;
import com.swiftlicious.hellblock.database.LegacyDataStorageInterface;
import com.swiftlicious.hellblock.database.MariaDBHandler;
import com.swiftlicious.hellblock.database.MySQLHandler;
import com.swiftlicious.hellblock.database.YamlHandler;
import com.swiftlicious.hellblock.player.PlayerData;
import com.swiftlicious.hellblock.utils.LogUtils;
import com.swiftlicious.hellblock.utils.extras.CompletableFutures;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataCommand {

	public static DataCommand INSTANCE = new DataCommand();

	public CommandAPICommand getDataCommand() {
		return new CommandAPICommand("data").withSubcommands(getExportLegacyCommand(), getExportCommand(),
				getImportCommand(), getUnlockCommand());
	}

	private CommandAPICommand getUnlockCommand() {
		return new CommandAPICommand("unlock").withArguments(new UUIDArgument("uuid")).executes((sender, args) -> {
			UUID uuid = (UUID) args.get("uuid");
			HellblockPlugin.getInstance().getStorageManager().getDataSource().lockOrUnlockPlayerData(uuid, false);
			HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender, "Successfully unlocked.");
		});
	}

	private CommandAPICommand getExportLegacyCommand() {
		return new CommandAPICommand("export-legacy")
				.withArguments(new StringArgument("method")
						.replaceSuggestions(ArgumentSuggestions.strings("MySQL", "MariaDB", "YAML")))
				.executes((sender, args) -> {
					String arg = (String) args.get("method");
					if (arg == null)
						return;
					HellblockPlugin plugin = HellblockPlugin.getInstance();
					plugin.getScheduler().runTaskAsync(() -> {

						plugin.getAdventureManager().sendMessageWithPrefix(sender, "Starting <aqua>export</aqua>.");

						LegacyDataStorageInterface dataStorageInterface;
						switch (arg) {
						case "MySQL" -> dataStorageInterface = new MySQLHandler(plugin);
						case "MariaDB" -> dataStorageInterface = new MariaDBHandler(plugin);
						case "YAML" -> dataStorageInterface = new YamlHandler(plugin);
						default -> {
							plugin.getAdventureManager().sendMessageWithPrefix(sender,
									"No such legacy storage method.");
							return;
						}
						}

						dataStorageInterface.initialize();
						Set<UUID> uuids = dataStorageInterface.getUniqueUsers(true);
						Set<CompletableFuture<Void>> futures = new HashSet<>();
						AtomicInteger userCount = new AtomicInteger(0);
						Map<UUID, String> out = Collections.synchronizedMap(new TreeMap<>());

						for (UUID uuid : uuids) {
							futures.add(dataStorageInterface.getLegacyPlayerData(uuid).thenAccept(it -> {
								if (it.isPresent()) {
									out.put(uuid, plugin.getStorageManager().toJson(it.get()));
									userCount.incrementAndGet();
								}
							}));
						}

						CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);

						while (true) {
							try {
								overallFuture.get(3, TimeUnit.SECONDS);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
								break;
							} catch (TimeoutException e) {
								LogUtils.info(String.format("Progress: %s/%s", userCount.get(), uuids.size()));
								continue;
							}
							break;
						}

						JsonObject outJson = new JsonObject();
						for (Map.Entry<UUID, String> entry : out.entrySet()) {
							outJson.addProperty(entry.getKey().toString(), entry.getValue());
						}
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
						String formattedDate = formatter.format(new Date());
						File outFile = new File(plugin.getDataFolder(), "exported-" + formattedDate + ".json.gz");
						try (BufferedWriter writer = new BufferedWriter(
								new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outFile.toPath())),
										StandardCharsets.UTF_8))) {
							new GsonBuilder().disableHtmlEscaping().create().toJson(outJson, writer);
						} catch (IOException e) {
							e.printStackTrace();
						}

						dataStorageInterface.disable();

						plugin.getAdventureManager().sendMessageWithPrefix(sender, "Completed.");
					});
				});
	}

	private CommandAPICommand getExportCommand() {
		return new CommandAPICommand("export").executesConsole((sender, args) -> {
			if (Bukkit.getOnlinePlayers().size() != 0) {
				HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
						"Please kick all the players before exporting. Otherwise the cache will be inconsistent with data, resulting in the backup file not being up to date.");
				return;
			}

			HellblockPlugin plugin = HellblockPlugin.getInstance();
			plugin.getScheduler().runTaskAsync(() -> {

				plugin.getAdventureManager().sendMessageWithPrefix(sender, "Starting <aqua>export</aqua>.");
				DataStorageInterface dataStorageInterface = plugin.getStorageManager().getDataSource();

				Set<UUID> uuids = dataStorageInterface.getUniqueUsers(false);
				Set<CompletableFuture<Void>> futures = new HashSet<>();
				AtomicInteger userCount = new AtomicInteger(0);
				Map<UUID, String> out = Collections.synchronizedMap(new TreeMap<>());

				int amount = uuids.size();
				for (UUID uuid : uuids) {
					futures.add(dataStorageInterface.getPlayerData(uuid, false).thenAccept(it -> {
						if (it.isPresent()) {
							out.put(uuid, plugin.getStorageManager().toJson(it.get()));
							userCount.incrementAndGet();
						}
					}));
				}

				CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);

				while (true) {
					try {
						overallFuture.get(3, TimeUnit.SECONDS);
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						break;
					} catch (TimeoutException e) {
						LogUtils.info(String.format("Progress: %s/%s", userCount.get(), amount));
						continue;
					}
					break;
				}

				JsonObject outJson = new JsonObject();
				for (Map.Entry<UUID, String> entry : out.entrySet()) {
					outJson.addProperty(entry.getKey().toString(), entry.getValue());
				}
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
				String formattedDate = formatter.format(new Date());
				File outFile = new File(plugin.getDataFolder(), "exported-" + formattedDate + ".json.gz");
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
						new GZIPOutputStream(Files.newOutputStream(outFile.toPath())), StandardCharsets.UTF_8))) {
					new GsonBuilder().disableHtmlEscaping().create().toJson(outJson, writer);
				} catch (IOException e) {
					e.printStackTrace();
				}

				plugin.getAdventureManager().sendMessageWithPrefix(sender, "Completed.");
			});
		});
	}

	private CommandAPICommand getImportCommand() {
		return new CommandAPICommand("import").withArguments(new StringArgument("file"))
				.executesConsole((sender, args) -> {
					if (Bukkit.getOnlinePlayers().size() != 0) {
						HellblockPlugin.getInstance().getAdventureManager().sendMessageWithPrefix(sender,
								"Please kick all the players before importing. Otherwise the cache will be inconsistent with data.");
						return;
					}

					String fileName = (String) args.get("file");
					if (fileName == null)
						return;
					HellblockPlugin plugin = HellblockPlugin.getInstance();

					File file = new File(plugin.getDataFolder(), fileName);
					if (!file.exists()) {
						plugin.getAdventureManager().sendMessageWithPrefix(sender, "File not exists.");
						return;
					}
					if (!file.getName().endsWith(".json.gz")) {
						plugin.getAdventureManager().sendMessageWithPrefix(sender, "Invalid file.");
						return;
					}

					plugin.getScheduler().runTaskAsync(() -> {

						plugin.getAdventureManager().sendMessageWithPrefix(sender, "Starting <aqua>import</aqua>.");

						JsonObject data;
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(
								new GZIPInputStream(Files.newInputStream(file.toPath())), StandardCharsets.UTF_8))) {
							data = new GsonBuilder().disableHtmlEscaping().create().fromJson(reader, JsonObject.class);
						} catch (IOException e) {
							plugin.getAdventureManager().sendMessageWithPrefix(sender,
									"Error occurred when reading the backup file.");
							e.printStackTrace();
							return;
						}

						DataStorageInterface dataStorageInterface = plugin.getStorageManager().getDataSource();
						var entrySet = data.entrySet();
						int amount = entrySet.size();
						AtomicInteger userCount = new AtomicInteger(0);
						Set<CompletableFuture<Void>> futures = new HashSet<>();

						for (Map.Entry<String, JsonElement> entry : entrySet) {
							UUID uuid = UUID.fromString(entry.getKey());
							if (entry.getValue() instanceof JsonPrimitive primitive) {
								PlayerData playerData = plugin.getStorageManager().fromJson(primitive.getAsString());
								futures.add(dataStorageInterface.updateOrInsertPlayerData(uuid, playerData, true)
										.thenAccept(it -> userCount.incrementAndGet()));
							}
						}

						CompletableFuture<Void> overallFuture = CompletableFutures.allOf(futures);

						while (true) {
							try {
								overallFuture.get(3, TimeUnit.SECONDS);
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
								break;
							} catch (TimeoutException e) {
								LogUtils.info(String.format("Progress: %s/%s", userCount.get(), amount));
								continue;
							}
							break;
						}

						plugin.getAdventureManager().sendMessageWithPrefix(sender, "Completed.");
					});
				});
	}
}
