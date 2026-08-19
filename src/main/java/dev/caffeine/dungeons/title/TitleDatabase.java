package dev.caffeine.dungeons.title;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.caffeine.dungeons.Backend.BackendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TitleDatabase {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("CaffeineDungeons/TitleDatabase");
    private static final String TITLES_URL =
            "https://raw.githubusercontent.com/Jakacraft/caffeine-dungeons-data/main/titles.json";
    private static final long ACTIVE_POLL_INTERVAL_SECONDS = 30;
    private static final Gson GSON = new Gson();

    private static final TitleDatabase INSTANCE = new TitleDatabase();
    public static TitleDatabase getInstance() { return INSTANCE; }

    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "caffeine-dungeons-titles-poll");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> pollTask;

    private TitleDatabase() {}

    /** Fetches the static, admin-curated catalog of who's eligible for which titles. */
    public void fetch() {
        http.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(TITLES_URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(r -> {
            if (r.statusCode() != 200) {
                LOGGER.warn("[CDM] titles.json fetch failed: HTTP {}", r.statusCode());
                return;
            }
            try {
                JsonArray arr = GSON.fromJson(r.body(), JsonArray.class);
                Map<UUID, List<TitleEntry>> grouped = new HashMap<>();
                int count = 0;
                for (var el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("id")) {
                        LOGGER.warn("[CDM] titles.json entry missing \"id\", skipping: {}", obj);
                        continue;
                    }
                    UUID uuid        = UUID.fromString(obj.get("uuid").getAsString());
                    String id        = obj.get("id").getAsString();
                    String titleText = obj.get("title").getAsString();
                    String color     = obj.has("color") ? obj.get("color").getAsString() : "#FFFFFF";
                    grouped.computeIfAbsent(uuid, k -> new ArrayList<>())
                            .add(new TitleEntry(id, titleText, color));
                    count++;
                }
                TitleRegistry reg = TitleRegistry.getInstance();
                grouped.forEach(reg::setGranted);
                LOGGER.info("[CDM] Loaded {} player title(s) across {} player(s).", count, grouped.size());
            } catch (Exception e) {
                LOGGER.error("[CDM] Failed to parse titles.json", e);
            }
        }).exceptionally(e -> { LOGGER.error("[CDM] Failed to fetch titles.json", e); return null; });
    }

    public void startActivePolling() {
        stopActivePolling();
        fetchActiveSelections();
        pollTask = scheduler.scheduleAtFixedRate(
                this::fetchActiveSelections,
                ACTIVE_POLL_INTERVAL_SECONDS, ACTIVE_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stopActivePolling() {
        if (pollTask != null) pollTask.cancel(true);
        pollTask = null;
    }

    private void fetchActiveSelections() {
        BackendService.INSTANCE.fetchActiveTitles().thenAccept(json -> {
            if (json == null) return;
            try {
                JsonArray arr = GSON.fromJson(json, JsonArray.class);
                TitleRegistry reg = TitleRegistry.getInstance();
                for (var el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("uuid") || obj.get("uuid").isJsonNull()) continue;
                    UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                    String activeId = obj.has("active_title_id") && !obj.get("active_title_id").isJsonNull()
                            ? obj.get("active_title_id").getAsString() : null;
                    reg.setActiveId(uuid, activeId);
                }
            } catch (Exception e) {
                LOGGER.error("[CDM] Failed to parse active title selections", e);
            }
        });
    }
}