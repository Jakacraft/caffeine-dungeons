package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;

public record RealtimeMessage(String type, JsonObject data) {
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.add("data", data);
        return obj;
    }
}