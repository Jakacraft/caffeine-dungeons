package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RealtimeDispatcher {
    private static final Map<String, Consumer<JsonObject>> handlers = new ConcurrentHashMap<>();

    public static void register(String type, Consumer<JsonObject> handler) {
        handlers.put(type, handler);
    }

    public static void dispatch(String type, JsonObject data) {
        Consumer<JsonObject> handler = handlers.get(type);
        if (handler == null) return;
        // Socket callbacks run off the client thread — hop back on.
        MinecraftClient.getInstance().execute(() -> handler.accept(data));
    }
}