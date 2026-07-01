package dev.caffeine.dungeons.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RealtimeSession {
    private static final Gson GSON = new Gson();
    private static final Path FILE = Path.of("config", "caffeine_dungeons_realtime_session.json");

    public String accessToken;
    public String refreshToken;
    public String authUid;
    public long expiresAtEpochSeconds;

    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds - 30;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[CaffeineDungeons] Failed to save realtime session: " + e.getMessage());
        }
    }

    public static RealtimeSession load() {
        try {
            if (!Files.exists(FILE)) return null;
            return GSON.fromJson(Files.readString(FILE), RealtimeSession.class);
        } catch (IOException | JsonSyntaxException e) {
            return null;
        }
    }
}