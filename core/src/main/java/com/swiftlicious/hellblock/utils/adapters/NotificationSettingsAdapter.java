package com.swiftlicious.hellblock.utils.adapters;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.swiftlicious.hellblock.player.NotificationSettings;

public class NotificationSettingsAdapter extends TypeAdapter<NotificationSettings> {

    @Override
    public void write(JsonWriter out, NotificationSettings settings) throws IOException {
        if (settings == null || settings.isEmpty()) {
            out.nullValue();
            return;
        }

        out.beginObject();

        // Skip true values — only write if false
        if (!settings.hasJoinNotifications()) {
            out.name("joinNotifications").value(false);
        }

        if (!settings.hasInviteNotifications()) {
            out.name("inviteNotifications").value(false);
        }

        out.endObject();
    }

    @Override
    public NotificationSettings read(JsonReader in) throws IOException {
        // Default values — true unless explicitly set to false in JSON
        boolean join = true;
        boolean invite = true;

        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return new NotificationSettings(join, invite);
        }

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "joinNotifications" -> join = in.nextBoolean();
                case "inviteNotifications" -> invite = in.nextBoolean();
                default -> in.skipValue();
            }
        }
        in.endObject();

        return new NotificationSettings(join, invite);
    }
}