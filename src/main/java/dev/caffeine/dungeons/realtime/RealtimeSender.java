package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RealtimeSender {

    private static final HttpClient http = HttpClient.newHttpClient();

    public static CompletableFuture<Optional<UUID>> resolveUuidByUsername(String username) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String base = config.dev.supabaseUrl.endsWith("/")
                ? config.dev.supabaseUrl.substring(0, config.dev.supabaseUrl.length() - 1)
                : config.dev.supabaseUrl;
        String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String url = base + "/rest/v1/players?username=eq." + encoded + "&select=uuid&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.dev.supabaseAnonKey)
                .header("Authorization", "Bearer " + config.dev.supabaseAnonKey)
                .GET()
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() / 100 != 2) return Optional.<UUID>empty();
            JsonArray rows = JsonParser.parseString(response.body()).getAsJsonArray();
            if (rows.isEmpty()) return Optional.<UUID>empty();
            return Optional.of(UUID.fromString(rows.get(0).getAsJsonObject().get("uuid").getAsString()));
        });
    }

    public static void sendToPlayer(UUID targetUuid, String type, JsonObject data) {
        RealtimeClient.getInstance().sendOnceTo("player:" + targetUuid, new RealtimeMessage(type, data));
    }
}