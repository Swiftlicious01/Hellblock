package com.swiftlicious.hellblock.creation.addons.bedrock;

import java.util.UUID;

import org.geysermc.floodgate.api.FloodgateApi;

public class FloodGateUtils {

    public static boolean isBedrockPlayer(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
