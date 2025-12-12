package com.swiftlicious.hellblock.handlers.chat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.swiftlicious.hellblock.handlers.chat.ChatManager.NativeComponentCache;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public class GsonBridgeDelegate {

	@RuntimeType
	public static Object fromJson(String json) throws Exception {
		Object sampleComponent = NativeComponentCache.getNativeReference();
		if (sampleComponent == null)
			throw new IllegalStateException("No native Component reference available for GsonBridgeDelegate");

		try {
			Class<?> baseClass = sampleComponent.getClass();

			// Find the net.kyori.adventure.text.Component interface in hierarchy
			Class<?> componentInterface = null;
			Class<?> current = baseClass;
			while (current != null && componentInterface == null) {
				for (Class<?> iface : current.getInterfaces()) {
					if (iface.getSimpleName().equals("Component")) {
						componentInterface = iface;
						break;
					}
				}
				current = current.getSuperclass();
			}

			if (componentInterface == null)
				throw new IllegalStateException("Could not find Component interface for " + baseClass);

			// Derive Adventure root package (e.g. net.kyori.adventure)
			String pkg = componentInterface.getPackageName();
			int idx = pkg.indexOf(".text");
			if (idx == -1)
				throw new IllegalStateException("Unexpected Adventure package: " + pkg);
			String adventureRoot = pkg.substring(0, idx); // -> net.kyori.adventure

			// Find any static method in any public class of same package tree returning
			// *GsonComponentSerializer*
			Method gsonStatic = null;
			Class<?> gsonSerializerClass = null;

			// Search all public classes reachable from the same loader
			// The trick: GsonComponentSerializer is public and its class object is
			// accessible
			// through methods of Component or nearby types (like MiniMessage)
			for (Method m : componentInterface.getMethods()) {
				Class<?> returnType = m.getReturnType();
				if (returnType != null && returnType.getSimpleName().equals("GsonComponentSerializer")) {
					gsonSerializerClass = returnType;
					break;
				}
			}

			// fallback: maybe not on Component, but on one of its declaring packages
			if (gsonSerializerClass == null) {
				// Look for any public static method named gson() visible to this classloader
				for (Method m : baseClass.getClassLoader()
						.loadClass(adventureRoot + ".text.serializer.gson.GsonComponentSerializer").getMethods()) {
					if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("gson")) {
						gsonSerializerClass = m.getDeclaringClass();
						gsonStatic = m;
						break;
					}
				}
			}

			if (gsonSerializerClass == null)
				throw new IllegalStateException("Could not resolve GsonComponentSerializer via reflection tree");

			// Get gson() static method (if not already found)
			if (gsonStatic == null) {
				for (Method m : gsonSerializerClass.getMethods()) {
					if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("gson")) {
						gsonStatic = m;
						break;
					}
				}
			}
			if (gsonStatic == null)
				throw new IllegalStateException("No gson() static method on " + gsonSerializerClass);

			// Unlock reflective access before invoking
			gsonStatic.setAccessible(true);
			Object gsonSerializer = gsonStatic.invoke(null);

			// Find deserialize(String) or deserialize(JsonElement/JsonObject)
			Method deserialize = null;
			for (Method m : gsonSerializer.getClass().getMethods()) {
				if (m.getName().equals("deserialize") && m.getParameterCount() == 1) {
					Class<?> p = m.getParameterTypes()[0];
					if (p == String.class || p.getSimpleName().equals("JsonElement")
							|| p.getSimpleName().equals("JsonObject")) {
						deserialize = m;
						break;
					}
				}
			}

			if (deserialize == null)
				throw new IllegalStateException("No suitable deserialize(...) found on " + gsonSerializer.getClass());

			Class<?> paramType = deserialize.getParameterTypes()[0];
			Object arg = json;

			if (paramType != String.class) {
				// parse JSON string into JsonElement using same loader
				ClassLoader loader = baseClass.getClassLoader();
				Class<?> jsonParserClass = loader.loadClass("com.google.gson.JsonParser");
				Method parseString = jsonParserClass.getMethod("parseString", String.class);
				Object parsed = parseString.invoke(null, json);

				if (paramType.getSimpleName().equals("JsonObject")) {
					Method getAsJsonObject = parsed.getClass().getMethod("getAsJsonObject");
					parsed = getAsJsonObject.invoke(parsed);
				}
				arg = parsed;
			}

			deserialize.setAccessible(true);
			return deserialize.invoke(gsonSerializer, arg);

		} catch (Throwable t) {
			throw new IllegalStateException("Failed to deserialize JSON into native Component", t);
		}
	}
}