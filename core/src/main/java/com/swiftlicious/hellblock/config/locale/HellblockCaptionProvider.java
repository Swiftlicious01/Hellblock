package com.swiftlicious.hellblock.config.locale;

import org.incendo.cloud.caption.CaptionProvider;
import org.incendo.cloud.caption.DelegatingCaptionProvider;
import org.jetbrains.annotations.NotNull;

public final class HellblockCaptionProvider<C> extends DelegatingCaptionProvider<C> {

	private static final CaptionProvider<?> PROVIDER = CaptionProvider.constantProvider()
			.putCaption(HellblockCaptionKeys.ARGUMENT_ENTITY_NOTFOUND_PLAYER,
					"<red><lang:argument.entity.notfound.player></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_ENTITY_NOTFOUND_ENTITY,
					"<red><lang:argument.entity.notfound.entity></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_TIME,
					"<red>'<arg:0>' isn't a valid time format</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_BIOME,
					"<red>'<arg:0>' isn't a valid Minecraft biome</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_MATERIAL,
					"<red>'<arg:0>' isn't a valid material name</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_ENCHANTMENT,
					"<red>'<arg:0>' isn't a valid enchantment</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_OFFLINEPLAYER,
					"<red>No player found for input '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
					"<red>No player found for input '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_WORLD,
					"<red>'<arg:0>' isn't a valid Minecraft world</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_LOCATION_INVALID_FORMAT,
					"<red>'<arg:0>' isn't a valid location. Required format is '<arg:1> <arg:2> <arg:3></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_LOCATION_MIXED_LOCAL_ABSOLUTE,
					"<red>Cannot mix local and absolute coordinates. (either all coordinates use '^' or none do)</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NAMESPACEDKEY_NAMESPACE,
					"<red>Invalid namespace '<arg:0>'. Must be [a-z0-9._-]</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NAMESPACEDKEY_KEY,
					"<red>Invalid key '<arg:0>'. Must be [a-z0-9/._-]</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NAMESPACEDKEY_NEED_NAMESPACE,
					"<red>Invalid input '<arg:0>', requires an explicit namespace</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_BOOLEAN,
					"<red>Couldn't parse boolean from '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NUMBER,
					"<red>'<arg:0>' isn't a valid number in the range <arg:1> to <arg:2></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_CHAR,
					"<red>'<arg:0>' isn't a valid character</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_STRING,
					"<red>'<arg:0>' isn't a valid string of type <arg:1></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_UUID, "<red>'<arg:0>' isn't a valid UUID</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_ENUM,
					"<red>'<arg:0>' isn't one of the following: <arg:1></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_REGEX,
					"<red>'<arg:0>' doesn't match '<arg:1>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_FLAG_UNKNOWN, "<red>Unknown flag '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_FLAG_DUPLICATE_FLAG,
					"<red>Duplicate flag '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_FLAG_NO_FLAG_STARTED,
					"<red>No flag started. Don't know what to do with '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_FLAG_MISSING_ARGUMENT,
					"<red>Missing argument for '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_FLAG_NO_PERMISSION,
					"<red>You don't have permission to use '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_COLOR, "<red>'<arg:0>' isn't a valid color</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_DURATION,
					"<red>'<arg:0>' isn't a duration format</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_AGGREGATE_MISSING,
					"<red>Missing component '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_AGGREGATE_FAILURE,
					"<red>Invalid component '<arg:0>': <arg:1></red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_EITHER,
					"<red>Couldn't resolve <arg:1> or <arg:2> from '<arg:0>'</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_NAMEDTEXTCOLOR,
					"<red>'<arg:0>' isn't a named text color</red>")
			.putCaption(HellblockCaptionKeys.ARGUMENT_PARSE_FAILURE_URL, "<red>'<arg:0>' isn't a valid URL</red>")
			.build();

	@SuppressWarnings("unchecked")
	@Override
	public @NotNull CaptionProvider<C> delegate() {
		return (CaptionProvider<C>) PROVIDER;
	}
}