package com.swiftlicious.hellblock.config.locale;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.TriState;
import net.kyori.examination.ExaminableProperty;

public class MiniMessageTranslator implements MiniMessageTranslatorInterface {

	private static final Key NAME = Key.key("hellblock", "main");
	static final MiniMessageTranslator INSTANCE = new MiniMessageTranslator();
	final TranslatableComponentRenderer<Locale> renderer;
	private final Set<Translator> sources = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private MiniMessageTranslator() {
		this.renderer = TranslatableComponentRenderer.usingTranslationSource(this);
	}

	@Override
	public @NotNull Key name() {
		return NAME;
	}

	@Override
	public @NotNull TriState hasAnyTranslations() {
		return !this.sources.isEmpty() ? TriState.TRUE : TriState.FALSE;
	}

	@Override
	public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
		// No need to implement this method
		return null;
	}

	@Override
	public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
		for (final Translator source : this.sources) {
			final Component translation = source.translate(component, locale);
			if (translation != null) {
				return translation;
			}
		}
		return null;
	}

	@Override
	public @NotNull Iterable<? extends Translator> sources() {
		return Collections.unmodifiableSet(this.sources);
	}

	@Override
	public boolean addSource(final @NotNull Translator source) {
		if (source == this) {
			throw new IllegalArgumentException("MiniMessageTranslationSource");
		}
		return this.sources.add(source);
	}

	@Override
	public boolean removeSource(final @NotNull Translator source) {
		return this.sources.remove(source);
	}

	@Override
	public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
		return Stream.of(ExaminableProperty.of("sources", this.sources));
	}
}