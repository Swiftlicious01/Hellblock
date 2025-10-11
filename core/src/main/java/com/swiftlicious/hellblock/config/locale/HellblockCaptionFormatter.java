package com.swiftlicious.hellblock.config.locale;

import java.util.List;

import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.jetbrains.annotations.NotNull;

import com.swiftlicious.hellblock.HellblockPlugin;

import net.kyori.adventure.text.Component;

public class HellblockCaptionFormatter<C> implements ComponentCaptionFormatter<C> {

	@Override
	public @NotNull Component formatCaption(@NotNull Caption captionKey, @NotNull C recipient, @NotNull String caption,
			@NotNull List<@NotNull CaptionVariable> variables) {
		final Component component = ComponentCaptionFormatter.translatable().formatCaption(captionKey, recipient, caption,
				variables);
		return HellblockPlugin.getInstance().getTranslationManager().render(component);
	}
}