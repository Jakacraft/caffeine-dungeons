package dev.caffeine.dungeons.accessory;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class AccessoryDatabase {
    private static final Logger LOGGER =
            LoggerFactory.getLogger("CaffeineDungeons/AccessoryDatabase");
    private static final String URL =
            "https://raw.githubusercontent.com/Jakacraft/caffeine-dungeons-data/main/accessories.json";
    private static final Gson GSON = new Gson();

    private static final AccessoryDatabase INSTANCE = new AccessoryDatabase();

    public static AccessoryDatabase getInstance() {
        return INSTANCE;
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private volatile List<AccessoryEntry> masterList = List.of();

    private AccessoryDatabase() {
    }

    public List<AccessoryEntry> getMasterList() {
        return masterList;
    }

    public void fetch() {
        http.sendAsync(
                HttpRequest.newBuilder().uri(URI.create(URL)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(r -> {
            if (r.statusCode() != 200) {
                LOGGER.warn("[CDM] accessories.json fetch failed: HTTP {}", r.statusCode());
                return;
            }
            try {
                JsonArray arr = GSON.fromJson(r.body(), JsonArray.class);
                List<AccessoryEntry> list = new ArrayList<>();
                for (var el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    AccessoryRarity rarity = AccessoryRarity.fromString(
                            obj.get("rarity").getAsString());
                    Integer floor = (obj.has("floor") && !obj.get("floor").isJsonNull())
                            ? obj.get("floor").getAsInt() : null;
                    list.add(new AccessoryEntry(name, rarity, floor));
                }
                list.sort(AccessoryEntry.SORT_ORDER);
                masterList = Collections.unmodifiableList(list);
                AccessoryTracker.getInstance().setDirty();
                LOGGER.info("[CDM] Loaded {} accessories.", list.size());
            } catch (Exception e) {
                LOGGER.error("[CDM] Failed to parse accessories.json", e);
            }
        }).exceptionally(e -> {
            LOGGER.error("[CDM] Failed to fetch accessories.json", e);
            return null;
        });
    }
}