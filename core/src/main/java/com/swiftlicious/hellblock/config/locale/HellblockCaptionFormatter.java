package com.swiftlicious.hellblock.config.locale;

import java.util.List;

import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;

import com.swiftlicious.hellblock.HellblockPlugin;

import lombok.NonNull;
import net.kyori.adventure.text.Component;

public class HellblockCaptionFormatter<C> implements ComponentCaptionFormatter<C> {

	@Override
	public @NonNull Component formatCaption(@NonNull Caption captionKey, @NonNull C recipient, @NonNull String caption,
			@NonNull List<@NonNull CaptionVariable> variables) {
		Component component = ComponentCaptionFormatter.translatable().formatCaption(captionKey, recipient, caption,
				variables);
		return HellblockPlugin.getInstance().getTranslationManager().render(component);
	}
}