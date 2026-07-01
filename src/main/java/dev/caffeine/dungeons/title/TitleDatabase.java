package dev.caffeine.dungeons.title;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class TitleDatabase {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("CaffeineDungeons/TitleDatabase");
    private static final String TITLES_URL =
            "https://raw.githubusercontent.com/Jakacraft/caffeine-dungeons-data/main/titles.json";
    private static final Gson GSON = new Gson();

    private static final TitleDatabase INSTANCE = new TitleDatabase();
    public static TitleDatabase getInstance() { return INSTANCE; }

    private final HttpClient http = HttpClient.newHttpClient();

    private TitleDatabase() {}

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
                JsonArray arr      = GSON.fromJson(r.body(), JsonArray.class);
                TitleRegistry reg  = TitleRegistry.getInstance();
                reg.clear();
                int count = 0;
                for (var el : arr) {
                    JsonObject obj   = el.getAsJsonObject();
                    UUID uuid        = UUID.fromString(obj.get("uuid").getAsString());
                    String titleText = obj.get("title").getAsString();
                    String color     = obj.has("color")
                            ? obj.get("color").getAsString() : "#FFFFFF";
                    reg.register(uuid, new TitleEntry(titleText, color));
                    count++;
                }
                LOGGER.info("[CDM] Loaded {} player title(s).", count);
            } catch (Exception e) {
                LOGGER.error("[CDM] Failed to parse titles.json", e);
            }
        }).exceptionally(e -> { LOGGER.error("[CDM] Failed to fetch titles.json", e); return null; });
    }
}