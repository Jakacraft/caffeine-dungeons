package dev.caffeine.dungeons.ability;

import com.google.gson.Gson;
import dev.caffeine.dungeons.CaffeineDungeons;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class AbilityDatabase {
    public static final AbilityDatabase INSTANCE = new AbilityDatabase();

    private static final String URL =
            "https://raw.githubusercontent.com/Jakacraft/caffeine-dungeons-data/main/abilities.json";
    private static final Gson GSON = new Gson();

    private final Map<String, AbilityData> abilities = new HashMap<>();
    private boolean loaded = false;

    private AbilityDatabase() {}

    public void fetch() {
        if (loaded) return;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 400) {
                        CaffeineDungeons.LOGGER.error("[CDM] Failed to fetch: {}", response.statusCode());
                        return;
                    }
                    AbilityData[] data = GSON.fromJson(response.body(), AbilityData[].class);
                    if (data == null) return;
                    synchronized (abilities) {
                        abilities.clear();
                        for (AbilityData ability : data) {
                            abilities.put(ability.name.toLowerCase(), ability);
                        }
                    }
                    loaded = true;
                    CaffeineDungeons.LOGGER.info("[CDM] Loaded {} abilities", abilities.size());
                })
                .exceptionally(e -> {
                    CaffeineDungeons.LOGGER.error("[CDM] Fetch error: {}", e.getMessage());
                    return null;
                });
    }

    public AbilityData get(String name) {
        synchronized (abilities) {
            return abilities.get(name.toLowerCase());
        }
    }
}