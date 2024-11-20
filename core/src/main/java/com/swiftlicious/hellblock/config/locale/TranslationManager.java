package com.swiftlicious.hellblock.config.locale;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.HellblockProperties;
import com.swiftlicious.hellblock.utils.extras.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationManager {

	private final Locale DEFAULT_LOCALE = Locale.ENGLISH;
	private Locale FORCE_LOCALE = null;

	protected final HellblockPlugin plugin;
	private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
	private MiniMessageTranslationRegistryInterface registry;
	private final Path translationsDirectory;

	public TranslationManager(HellblockPlugin plugin) {
		this.plugin = plugin;
		this.translationsDirectory = this.plugin.getDataFolder().toPath().toAbsolutePath().resolve("messages");
	}

	public void forceLocale(Locale locale) {
		FORCE_LOCALE = locale;
	}

	public void reload() {
		// remove any previous registry
		if (this.registry != null) {
			MiniMessageTranslatorInterface.translator().removeSource(this.registry);
			this.installed.clear();
		}

		String supportedLocales = HellblockProperties.getValue("lang");
		for (String lang : supportedLocales.split(",")) {
			this.plugin.getConfigManager().saveResource("messages/" + lang + ".yml");
		}

		this.registry = MiniMessageTranslationRegistryInterface.create(Key.key("hellblock", "main"),
				plugin.getAdventureManager().getMiniMessage());
		this.registry.defaultLocale(DEFAULT_LOCALE);
		this.loadFromFileSystem(this.translationsDirectory, false);
		MiniMessageTranslatorInterface.translator().addSource(this.registry);
	}

	public String miniMessageTranslation(String key) {
		return miniMessageTranslation(key, null);
	}

	public String miniMessageTranslation(String key, @Nullable Locale locale) {
		if (FORCE_LOCALE != null) {
			return registry.miniMessageTranslation(key, FORCE_LOCALE);
		}
		if (locale == null) {
			locale = Locale.getDefault();
			if (locale == null) {
				locale = DEFAULT_LOCALE;
			}
		}
		return registry.miniMessageTranslation(key, locale);
	}

	public Component render(Component component) {
		return render(component, null);
	}

	public Component render(Component component, @Nullable Locale locale) {
		if (FORCE_LOCALE != null) {
			return MiniMessageTranslatorInterface.render(component, FORCE_LOCALE);
		}
		if (locale == null) {
			locale = Locale.getDefault();
			if (locale == null) {
				locale = DEFAULT_LOCALE;
			}
		}
		return MiniMessageTranslatorInterface.render(component, locale);
	}

	public void loadFromFileSystem(Path directory, boolean suppressDuplicatesError) {
		List<Path> translationFiles;
		try (Stream<Path> stream = Files.list(directory)) {
			translationFiles = stream.filter(path -> isTranslationFile(path)).collect(Collectors.toList());
		} catch (IOException e) {
			translationFiles = Collections.emptyList();
		}

		if (translationFiles.isEmpty()) {
			return;
		}

		Map<Locale, Map<String, String>> loaded = new HashMap<>();
		for (Path translationFile : translationFiles) {
			try {
				Pair<Locale, Map<String, String>> result = loadTranslationFile(translationFile);
				loaded.put(result.left(), result.right());
			} catch (Exception e) {
				if (!suppressDuplicatesError || !isAdventureDuplicatesException(e)) {
					plugin.getPluginLogger().warn("Error loading locale file: " + translationFile.getFileName(), e);
				}
			}
		}

		// try registering the locale without a country code - if we don't already have
		// a registration for that
		loaded.forEach((locale, bundle) -> {
			Locale localeWithoutCountry = Locale.of(locale.getLanguage());
			if (!locale.equals(localeWithoutCountry) && !localeWithoutCountry.equals(DEFAULT_LOCALE)
					&& this.installed.add(localeWithoutCountry)) {
				try {
					this.registry.registerAll(localeWithoutCountry, bundle);
				} catch (IllegalArgumentException e) {
					// ignore
				}
			}
		});

		Locale localLocale = Locale.getDefault();
		if (!this.installed.contains(localLocale) && FORCE_LOCALE == null) {
			plugin.getPluginLogger().warn(localLocale.toString().toLowerCase(Locale.ENGLISH) + ".yml not exists, using "
					+ DEFAULT_LOCALE.toString().toLowerCase(Locale.ENGLISH) + ".yml as default locale.");
		}
	}

	public boolean isTranslationFile(Path path) {
		return path.getFileName().toString().endsWith(".yml");
	}

	private boolean isAdventureDuplicatesException(Exception e) {
		return e instanceof IllegalArgumentException && (e.getMessage().startsWith("Invalid key")
				|| e.getMessage().startsWith("Translation already exists"));
	}

	@SuppressWarnings("unchecked")
	private Pair<Locale, Map<String, String>> loadTranslationFile(Path translationFile) {
		String fileName = translationFile.getFileName().toString();
		String localeString = fileName.substring(0, fileName.length() - ".yml".length());
		Locale locale = parseLocale(localeString);

		if (locale == null) {
			throw new IllegalStateException("Unknown locale '" + localeString + "' - unable to register.");
		}

		Map<String, String> bundle = new HashMap<>();
		YamlDocument document = plugin.getConfigManager()
				.loadConfig("messages" + File.separator + translationFile.getFileName(), '@');
		try {
			document.save(new File(plugin.getDataFolder().toPath().toAbsolutePath().toFile(),
					"messages" + File.separator + translationFile.getFileName()));
		} catch (IOException e) {
			throw new IllegalStateException("Could not update translation file: " + translationFile.getFileName(), e);
		}
		Map<String, Object> map = document.getStringRouteMappedValues(false);
		map.remove("config-version");
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() instanceof List<?> list) {
				List<String> strList = (List<String>) list;
				StringJoiner stringJoiner = new StringJoiner("<reset><newline>");
				for (String str : strList) {
					stringJoiner.add(str);
				}
				bundle.put(entry.getKey(), stringJoiner.toString());
			} else if (entry.getValue() instanceof String str) {
				bundle.put(entry.getKey(), str);
			}
		}

		this.registry.registerAll(locale, bundle);
		this.installed.add(locale);

		return Pair.of(locale, bundle);
	}

	public @Nullable Locale parseLocale(@Nullable String locale) {
		return locale == null || locale.isEmpty() ? null : Translator.parseLocale(locale);
	}
}