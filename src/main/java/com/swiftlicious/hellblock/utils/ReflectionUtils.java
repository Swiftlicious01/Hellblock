package com.swiftlicious.hellblock.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.swiftlicious.hellblock.HellblockPlugin;

public class ReflectionUtils {

	private ReflectionUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	public static Method gsonDeserializeMethod;
	public static Object gsonInstance;
	public static Class<?> componentClass;
	public static Class<?> bukkitClass;

	public static void load() {
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