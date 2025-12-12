package com.swiftlicious.hellblock.creation.addons.bedrock;

import java.util.UUID;

import org.geysermc.api.Geyser;

public class GeyserUtils {

	public static boolean isBedrockPlayer(UUID uuid) {
		return Geyser.api().isBedrockPlayer(uuid);
	}
}