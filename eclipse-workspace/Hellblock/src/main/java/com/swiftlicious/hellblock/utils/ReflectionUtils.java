package com.swiftlicious.hellblock.utils;

import java.lang.reflect.Constructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.swiftlicious.hellblock.HellblockPlugin;

public class ReflectionUtils {

	private ReflectionUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static Constructor<?> progressConstructor;
	public static Constructor<?> updateConstructor;
	public static Method iChatComponentMethod;
	public static Method gsonDeserializeMethod;
	public static Object gsonInstance;
	public static Class<?> componentClass;
	public static Class<?> bukkitClass;

	public static void load() {
		// spigot map
		try {
			Class<?> bar = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutBoss");
			Field remove = bar.getDeclaredField("f");
			remove.setAccessible(true);
			Class<?> packetBossClassF = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutBoss$f");
			progressConstructor = packetBossClassF.getDeclaredConstructor(float.class);
			progressConstructor.setAccessible(true);
			Class<?> packetBossClassE = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutBoss$e");
			updateConstructor = packetBossClassE
					.getDeclaredConstructor(MinecraftReflection.getIChatBaseComponentClass());
			updateConstructor.setAccessible(true);
			iChatComponentMethod = MinecraftReflection.getChatSerializerClass().getMethod("a",
					MinecraftReflection.getIChatBaseComponentClass(),
					MinecraftReflection.getHolderLookupProviderClass());
		} catch (NoSuchFieldException | NoSuchMethodException | ClassNotFoundException | IllegalArgumentException e) {
			LogUtils.severe("Error occurred when loading reflections", e);
			e.printStackTrace();
		}
		if (HellblockPlugin.getInstance().getVersionManager().isSpigot())
			return;
		// paper map
		try {
			componentClass = Class.forName("net;kyori;adventure;text;Component".replace(";", "."));
			bukkitClass = Class.forName("org;bukkit;Bukkit".replace(";", "."));
			Class<?> gsonComponentSerializerClass = Class
					.forName("net;kyori;adventure;text;serializer;gson;GsonComponentSerializer".replace(";", "."));
			Class<?> gsonComponentSerializerImplClass = Class
					.forName("net;kyori;adventure;text;serializer;gson;GsonComponentSerializerImpl".replace(";", "."));
			Method gsonMethod = gsonComponentSerializerClass.getMethod("gson");
			gsonInstance = gsonMethod.invoke(null);
			gsonDeserializeMethod = gsonComponentSerializerImplClass.getMethod("deserialize", String.class);
			gsonDeserializeMethod.setAccessible(true);
		} catch (ClassNotFoundException exception) {
			LogUtils.severe("Error occurred when loading reflections", exception);
			exception.printStackTrace();
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}