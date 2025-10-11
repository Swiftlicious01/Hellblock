package com.swiftlicious.hellblock.player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;

public class GameProfileBuilder {

	private static final Gson gson = new GsonBuilder().disableHtmlEscaping()
			.registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
			.registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
			.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();
	private static final Map<UUID, CachedProfile> cache = Collections.unmodifiableMap(new HashMap<>());
	private static final Object sync = new Object();
	private static long cacheTime = -1L;

	public static GameProfile fetch(UUID uuid) throws IOException {
		return fetch(uuid, false);
	}

	public static GameProfile fetch(UUID uuid, boolean forceNew) throws IOException {
		if ((!forceNew) && (cache.containsKey(uuid)) && (cache.get(uuid).isValid())) {
			return cache.get(uuid).profile;
		}

		final HttpURLConnection connection;
		synchronized (sync) {
			connection = (HttpURLConnection) URI
					.create("https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false"
							.formatted(UUIDTypeAdapter.fromUUID(uuid)))
					.toURL().openConnection();
			connection.setReadTimeout(5000);
		}
		if (connection.getResponseCode() == 200) {
			@SuppressWarnings("resource")
			final String json = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();

			final GameProfile result = gson.fromJson(json, GameProfile.class);
			cache.put(uuid, new CachedProfile(result));
			return result;
		}
		if ((!forceNew) && (cache.containsKey(uuid))) {
			return cache.get(uuid).profile;
		}
		throw new IOException("Could not connect to mojang servers for unknown player: " + uuid.toString());
	}

	public static GameProfile getProfile(UUID uuid, String name, String skin) {
		return getProfile(uuid, name, skin, null);
	}

	public static GameProfile getProfile(UUID uuid, String name, String skinUrl, String capeUrl) {
		final GameProfile profile = new GameProfile(uuid, name);
		final boolean cape = (capeUrl != null) && (!capeUrl.isEmpty());

		final List<Object> args = new ArrayList<>();
		args.add(Long.valueOf(System.currentTimeMillis()));
		args.add(UUIDTypeAdapter.fromUUID(uuid));
		args.add(name);
		args.add(skinUrl);
		if (cape) {
			args.add(capeUrl);
		}
		profile.getProperties().put("textures", new Property("textures", Base64Coder.encodeString(cape
				? "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"isPublic\":true,\"textures\":{\"SKIN\":{\"url\":\"%s\"},\"CAPE\":{\"url\":\"%s\"}}}"
				: "{\"timestamp\":%d,\"profileId\":\"%s\",\"profileName\":\"%s\",\"isPublic\":true,\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}"
						.formatted(args.toArray(new Object[0])))));
		return profile;
	}

	public static void setCacheTime(long time) {
		cacheTime = time;
	}

	private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {

		@Override
		public GameProfile deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
			final JsonObject object = (JsonObject) json;
			final UUID id = object.has("id") ? (UUID) context.deserialize(object.get("id"), UUID.class) : null;
			final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
			final GameProfile profile = new GameProfile(id, name);
			if (object.has("properties")) {
				((PropertyMap) context.deserialize(object.get("properties"), PropertyMap.class)).entries()
						.forEach(prop -> profile.getProperties().put(prop.getKey(), prop.getValue()));
			}
			return profile;
		}

		@Override
		public JsonElement serialize(GameProfile profile, Type type, JsonSerializationContext context) {
			final JsonObject result = new JsonObject();
			if (profile.getId() != null) {
				result.add("id", context.serialize(profile.getId()));
			}
			if (profile.getName() != null) {
				result.addProperty("name", profile.getName());
			}
			if (!profile.getProperties().isEmpty()) {
				result.add("properties", context.serialize(profile.getProperties()));
			}
			return result;
		}
	}

	private record CachedProfile(GameProfile profile) {
		public boolean isValid() {
			return GameProfileBuilder.cacheTime < 0L;
		}
	}
}