package com.swiftlicious.hellblock.config.locale;

import org.incendo.cloud.caption.CaptionProvider;
import org.incendo.cloud.caption.DelegatingCaptionProvider;
import org.jetbrains.annotations.NotNull;

public final class HellblockCaptionProvider<C> extends DelegatingCaptionProvider<C> {

	private static final CaptionProvider<?> PROVIDER = CaptionProvider.constantProvider()
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_URL, "")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_TIME, "")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NAMEDTEXTCOLOR, "").build();

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull CaptionProvider<C> delegate() {
		return (CaptionProvider<C>) PROVIDER;
	}
}