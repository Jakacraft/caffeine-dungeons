package dev.caffeine.dungeons.supabase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.caffeine.dungeons.CaffeineDungeons;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class SupabaseClient {

    public static final Gson GSON = new GsonBuilder().create();

    private final String baseUrl;
    private final String anonKey;
    private final HttpClient http;

    public SupabaseClient(String baseUrl, String anonKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.anonKey = anonKey;
        this.http    = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CompletableFuture<String> get(String table, String query) {
        String uri = baseUrl + "/rest/v1/" + table + (query.isBlank() ? "" : "?" + query);
        HttpRequest req = baseBuilder(uri).GET().build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() >= 400) {
                        CaffeineDungeons.LOGGER.error("[Supabase] GET {} failed ({}): {}", table, r.statusCode(), r.body());
                        return null;
                    }
                    return r.body();
                })
                .exceptionally(e -> {
                    CaffeineDungeons.LOGGER.error("[Supabase] GET {} error: {}", table, e.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> upsert(String table, String json) {
        String uri = baseUrl + "/rest/v1/" + table;
        HttpRequest req = baseBuilder(uri)
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    if (r.statusCode() >= 400)
                        CaffeineDungeons.LOGGER.error("[Supabase] UPSERT {} failed ({}): {}", table, r.statusCode(), r.body());
                })
                .exceptionally(e -> {
                    CaffeineDungeons.LOGGER.error("[Supabase] UPSERT {} error: {}", table, e.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> patch(String table, String query, String json) {
        String uri = baseUrl + "/rest/v1/" + table + "?" + query;
        HttpRequest req = baseBuilder(uri)
                .header("Prefer", "return=minimal")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    if (r.statusCode() >= 400)
                        CaffeineDungeons.LOGGER.error("[Supabase] PATCH {} failed ({}): {}", table, r.statusCode(), r.body());
                })
                .exceptionally(e -> {
                    CaffeineDungeons.LOGGER.error("[Supabase] PATCH {} error: {}", table, e.getMessage());
                    return null;
                });
    }

    private HttpRequest.Builder baseBuilder(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("apikey",        anonKey)
                .header("Authorization", "Bearer " + anonKey)
                .header("Content-Type",  "application/json");
    }
}