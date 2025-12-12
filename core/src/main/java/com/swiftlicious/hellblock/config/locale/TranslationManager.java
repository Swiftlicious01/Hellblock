package com.swiftlicious.hellblock.config.locale;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.swiftlicious.hellblock.HellblockPlugin;
import com.swiftlicious.hellblock.database.dependency.HellblockProperties;
import com.swiftlicious.hellblock.handlers.AdventureDependencyHelper;
import com.swiftlicious.hellblock.handlers.AdventureHelper;
import com.swiftlicious.hellblock.utils.extras.Pair;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.Translator;

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

	public void forceLocale(@NotNull Locale locale) {
		FORCE_LOCALE = locale;
	}

	public void reload() {
		// remove any previous registry
		if (this.registry != null) {
			MiniMessageTranslatorInterface.translator().removeSource(this.registry);
			this.installed.clear();
		}

		final String supportedLocales = HellblockProperties.getValue("lang");
		for (String lang : supportedLocales.split(",")) {
			this.plugin.getConfigManager().saveResource("messages/" + lang + ".yml");
		}

		this.registry = MiniMessageTranslationRegistryInterface.create(Key.key("hellblock", "main"),
				AdventureHelper.getMiniMessage());
		this.registry.defaultLocale(DEFAULT_LOCALE);
		this.loadFromFileSystem(this.translationsDirectory, false);
		MiniMessageTranslatorInterface.translator().addSource(this.registry);
	}

	@NotNull
	public String miniMessageTranslation(@NotNull String key) {
		return miniMessageTranslation(key, null);
	}

	@NotNull
	public String miniMessageTranslation(@NotNull String key, @Nullable Locale locale) {
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

	@NotNull
	public Component render(@NotNull Component component) {
		return render(component, null);
	}

	@NotNull
	public Component render(@NotNull Component component, @Nullable Locale locale) {
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

	public void loadFromFileSystem(@NotNull Path directory, boolean suppressDuplicatesError) {
		List<Path> translationFiles;
		try (Stream<Path> stream = Files.list(directory)) {
			translationFiles = stream.filter(this::isTranslationFile).toList();
		} catch (IOException e) {
			translationFiles = Collections.emptyList();
		}

		if (translationFiles.isEmpty()) {
			return;
		}

		final Map<Locale, Map<String, String>> loaded = new HashMap<>();
		translationFiles.forEach(translationFile -> {
			try {
				final Pair<Locale, Map<String, String>> result = loadTranslationFile(translationFile);
				loaded.put(result.left(), result.right());
			} catch (Exception e) {
				if (!suppressDuplicatesError || !isAdventureDuplicatesException(e)) {
					plugin.getPluginLogger().warn("Error loading locale file: " + translationFile.getFileName(), e);
				}
			}
		});

		// try registering the locale without a country code - if we don't already have
		// a registration for that
		loaded.forEach((locale, bundle) -> {
			final Locale localeWithoutCountry = Locale.of(locale.getLanguage());
			if (!locale.equals(localeWithoutCountry) && !localeWithoutCountry.equals(DEFAULT_LOCALE)
					&& this.installed.add(localeWithoutCountry)) {
				try {
					this.registry.registerAll(localeWithoutCountry, bundle);
				} catch (IllegalArgumentException e) {
					// ignore
				}
			}
		});

		final Locale localLocale = Locale.getDefault();
		if (!this.installed.contains(localLocale) && FORCE_LOCALE == null) {
			plugin.getPluginLogger()
					.warn(localLocale.toString().toLowerCase(Locale.ENGLISH) + ".yml doesn't exist, using "
							+ DEFAULT_LOCALE.toString().toLowerCase(Locale.ENGLISH) + ".yml as default locale.");
		}
	}

	public boolean isTranslationFile(@NotNull Path path) {
		return path.getFileName().toString().endsWith(".yml");
	}

	private boolean isAdventureDuplicatesException(@NotNull Exception e) {
		return e instanceof IllegalArgumentException && (e.getMessage().startsWith("Invalid key")
				|| e.getMessage().startsWith("Translation already exists"));
	}

	@NotNull
	@SuppressWarnings("unchecked")
	private Pair<Locale, Map<String, String>> loadTranslationFile(@NotNull Path translationFile) {
		final String fileName = translationFile.getFileName().toString();
		final String localeString = fileName.substring(0, fileName.length() - ".yml".length());
		final Locale locale = parseLocale(localeString);

		if (locale == null) {
			throw new IllegalStateException("Unknown locale '" + localeString + "' - unable to register.");
		}

		final Map<String, String> bundle = new HashMap<>();
		final YamlDocument document = plugin.getConfigManager()
				.loadConfig("messages" + File.separator + translationFile.getFileName(), '@');
		try {
			document.save(new File(plugin.getDataFolder().toPath().toAbsolutePath().toFile(),
					"messages" + File.separator + translationFile.getFileName()));
		} catch (IOException e) {
			throw new IllegalStateException("Could not update translation file: " + translationFile.getFileName(), e);
		}
		final Map<String, Object> map = document.getStringRouteMappedValues(false);
		map.remove("config-version");
		map.entrySet().forEach(entry -> {
			if (entry.getValue() instanceof List<?> list) {
				final List<String> strList = (List<String>) list;
				final StringJoiner stringJoiner = new StringJoiner("<reset><newline>");
				strList.forEach(stringJoiner::add);
				bundle.put(entry.getKey(), stringJoiner.toString());
			} else if (entry.getValue() instanceof String str) {
				bundle.put(entry.getKey(), str);
			}
		});

		this.registry.registerAll(locale, bundle);
		this.installed.add(locale);

		return Pair.of(locale, bundle);
	}

	@NotNull
	public List<String> getRawStringList(@NotNull String key) {
		YamlDocument document = plugin.getConfigManager().loadConfig("messages/" + getCurrentLocale() + ".yml", '@');

		Object val = document.get(key);
		if (val instanceof List<?> list) {
			return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return Collections.emptyList();
	}

	@NotNull
	public Locale getForcedLocale() {
		if (FORCE_LOCALE != null)
			return FORCE_LOCALE;
		return Locale.getDefault();
	}

	@NotNull
	private String getCurrentLocale() {
		if (FORCE_LOCALE != null)
			return FORCE_LOCALE.toString().toLowerCase(Locale.ENGLISH);
		return Locale.getDefault().toString().toLowerCase(Locale.ENGLISH);
	}

	@Nullable
	public Locale parseLocale(@Nullable String locale) {
		if (locale == null || locale.isEmpty()) {
			return null;
		}

		// Return the result from the loader call
		return plugin.getDependencyManager().runWithLoader(AdventureDependencyHelper.ADVENTURE_DEPENDENCIES,
				() -> Translator.parseLocale(locale));
	}
}