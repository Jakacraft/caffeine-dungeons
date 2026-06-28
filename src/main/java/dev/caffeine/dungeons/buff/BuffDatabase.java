package dev.caffeine.dungeons.buff;

import com.google.gson.*;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class BuffDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger("CaffeineDungeons/BuffDatabase");
    private static final String BUFFS_URL =
            "https://raw.githubusercontent.com/Jakacraft/caffeine-dungeons-data/main/buffs.json";
    private static final Gson GSON = new Gson();

    private static final BuffDatabase INSTANCE = new BuffDatabase();
    public static BuffDatabase getInstance() { return INSTANCE; }

    private final HttpClient http = HttpClient.newHttpClient();

    private BuffDatabase() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Fetch GitHub buffs + active Supabase server event. Call on world JOIN. */
    public void fetchAll() {
        fetchGitHubBuffs();
        fetchServerEvent();
    }

    /** Push a new active server event (admin only — enforced in command layer). */
    public void pushServerEvent(String text) {
        CaffeineConfig cfg = cfg();
        if (!hasSupabase(cfg)) return;

        // 1. Deactivate all current active events
        patch(cfg, "/rest/v1/server_events?active=eq.true",
                jsonOf("active", false))
                .thenRun(() -> {
                    // 2. Insert new event
                    JsonObject body = new JsonObject();
                    body.addProperty("text",   text);
                    body.addProperty("color",  "#FFAA00");
                    body.addProperty("active", true);
                    post(cfg, "/rest/v1/server_events", body)
                            .thenAccept(r -> {
                                if (r.statusCode() == 201) {
                                    BuffTracker.getInstance().setTempEvent(
                                            BuffEntry.permanent(text, "#FFAA00"));
                                    LOGGER.info("Server event set: {}", text);
                                } else {
                                    LOGGER.warn("Insert server event failed: HTTP {}", r.statusCode());
                                }
                            });
                });
    }

    /** Set all active server events to inactive. */
    public void clearServerEvent() {
        CaffeineConfig cfg = cfg();
        if (!hasSupabase(cfg)) return;
        patch(cfg, "/rest/v1/server_events?active=eq.true", jsonOf("active", false))
                .thenAccept(r -> {
                    if (r.statusCode() == 200 || r.statusCode() == 204) {
                        BuffTracker.getInstance().clearTempEvent();
                        LOGGER.info("Server event cleared.");
                    } else {
                        LOGGER.warn("Clear server event failed: HTTP {}", r.statusCode());
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Private fetch helpers
    // -------------------------------------------------------------------------

    private void fetchGitHubBuffs() {
        http.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(BUFFS_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(r -> {
            if (r.statusCode() != 200) {
                LOGGER.warn("buffs.json fetch failed: HTTP {}", r.statusCode());
                return;
            }
            try {
                JsonObject root = GSON.fromJson(r.body(), JsonObject.class);
                BuffTracker tracker = BuffTracker.getInstance();

                // Daily event — pick by current day of week (0=Sun … 6=Sat)
                if (root.has("dailyEvents")) {
                    JsonArray events = root.getAsJsonArray("dailyEvents");
                    int dayIndex = java.time.LocalDate.now()
                            .getDayOfWeek().getValue() % 7;          // Mon=1..Sun=7→0
                    if (dayIndex < events.size()) {
                        JsonObject de = events.get(dayIndex).getAsJsonObject();
                        tracker.setDailyEvent(BuffEntry.permanent(
                                de.get("label").getAsString(),
                                colorOr(de, "#55FF55")
                        ));
                    }
                }

                // Boosters
                if (root.has("boosters")) {
                    List<BuffEntry> list = new ArrayList<>();
                    for (JsonElement el : root.getAsJsonArray("boosters")) {
                        JsonObject b = el.getAsJsonObject();
                        list.add(BuffEntry.permanent(
                                b.get("label").getAsString(),
                                colorOr(b, "#FFAA00")
                        ));
                    }
                    tracker.setBoosters(list);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse buffs.json", e);
            }
        }).exceptionally(e -> { LOGGER.error("Failed to fetch buffs.json", e); return null; });
    }

    private void fetchServerEvent() {
        CaffeineConfig cfg = cfg();
        if (!hasSupabase(cfg)) return;

        get(cfg, "/rest/v1/server_events?active=eq.true&order=created_at.desc&limit=1")
                .thenAccept(r -> {
                    if (r.statusCode() != 200) {
                        LOGGER.warn("server_events fetch failed: HTTP {}", r.statusCode());
                        return;
                    }
                    try {
                        JsonArray arr = GSON.fromJson(r.body(), JsonArray.class);
                        if (arr.isEmpty()) {
                            BuffTracker.getInstance().clearTempEvent();
                            return;
                        }
                        JsonObject row   = arr.get(0).getAsJsonObject();
                        String text      = row.get("text").getAsString();
                        String color     = row.has("color") ? row.get("color").getAsString() : "#FFAA00";
                        BuffTracker.getInstance().setTempEvent(BuffEntry.permanent(text, color));
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse server_events response", e);
                    }
                }).exceptionally(e -> { LOGGER.error("Failed to fetch server_events", e); return null; });
    }

    // -------------------------------------------------------------------------
    // Low-level HTTP helpers
    // -------------------------------------------------------------------------

    private java.util.concurrent.CompletableFuture<HttpResponse<String>>
    get(CaffeineConfig cfg, String path) {
        return http.sendAsync(
                HttpRequest.newBuilder()
                        .uri(URI.create(cfg.dev.supabaseUrl + path))
                        .header("apikey",        cfg.dev.supabaseAnonKey)
                        .header("Authorization", "Bearer " + cfg.dev.supabaseAnonKey)
                        .header("Accept",        "application/json")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private java.util.concurrent.CompletableFuture<HttpResponse<String>>
    post(CaffeineConfig cfg, String path, JsonObject body) {
        return http.sendAsync(
                HttpRequest.newBuilder()
                        .uri(URI.create(cfg.dev.supabaseUrl + path))
                        .header("apikey",         cfg.dev.supabaseAnonKey)
                        .header("Authorization",  "Bearer " + cfg.dev.supabaseAnonKey)
                        .header("Content-Type",   "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private java.util.concurrent.CompletableFuture<HttpResponse<String>>
    patch(CaffeineConfig cfg, String path, JsonObject body) {
        return http.sendAsync(
                HttpRequest.newBuilder()
                        .uri(URI.create(cfg.dev.supabaseUrl + path))
                        .header("apikey",         cfg.dev.supabaseAnonKey)
                        .header("Authorization",  "Bearer " + cfg.dev.supabaseAnonKey)
                        .header("Content-Type",   "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // -------------------------------------------------------------------------
    // Tiny utilities
    // -------------------------------------------------------------------------

    private static CaffeineConfig cfg() {
        return AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
    }

    private static boolean hasSupabase(CaffeineConfig cfg) {
        return cfg.dev.supabaseUrl != null && !cfg.dev.supabaseUrl.isBlank();
    }

    private static JsonObject jsonOf(String key, boolean value) {
        JsonObject o = new JsonObject();
        o.addProperty(key, value);
        return o;
    }

    private static String colorOr(JsonObject obj, String fallback) {
        return obj.has("color") ? obj.get("color").getAsString() : fallback;
    }
}