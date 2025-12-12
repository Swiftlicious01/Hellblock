package com.swiftlicious.hellblock.logging;

import org.bukkit.plugin.java.JavaPlugin;

public class PlatformLoggerProvider {
    public static PluginLogger forCurrentPlatform(Object plugin) {
        if (isVelocityEnvironment()) {
            return PluginLoggerFactory.fromSlf4j(
                org.slf4j.LoggerFactory.getLogger(plugin.getClass())
            );
        }
        if (isBukkitEnvironment()) {
            return PluginLoggerFactory.fromJavaLogger(
                ((JavaPlugin) plugin).getLogger()
            );
        }
        return PluginLoggerFactory.create("Hellblock");
    }

    private static boolean isVelocityEnvironment() {
        return classExists("com.velocitypowered.api.proxy.ProxyServer");
    }

    private static boolean isBukkitEnvironment() {
        return classExists("org.bukkit.plugin.java.JavaPlugin");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}