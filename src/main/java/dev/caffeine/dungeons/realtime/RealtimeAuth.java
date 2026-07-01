package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.supabase.SupabaseService;
import me.shedaniel.autoconfig.AutoConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RealtimeAuth {

    private static final RealtimeAuth INSTANCE = new RealtimeAuth();
    public static RealtimeAuth getInstance() { return INSTANCE; }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile RealtimeSession session;

    private RealtimeAuth() {
        session = RealtimeSession.load();
    }

    public String getAccessToken() {
        return session != null ? session.accessToken : null;
    }

    public CompletableFuture<Void> ensureSignedIn(UUID minecraftUuid) {
        if (session != null && !session.isExpired()) {
            return CompletableFuture.completedFuture(null);
        }
        if (session != null && session.refreshToken != null) {
            return refresh()
                    .exceptionally(err -> null)
                    .thenCompose(v -> session != null && !session.isExpired()
                            ? CompletableFuture.completedFuture((Void) null)
                            : signInAnonymously(minecraftUuid));
        }
        return signInAnonymously(minecraftUuid);
    }

    private CompletableFuture<Void> signInAnonymously(UUID minecraftUuid) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String url = strip(config.dev.supabaseUrl) + "/auth/v1/signup";

        // VERIFY: this assumes anonymous sign-in is POST /auth/v1/signup
        // with an empty body, matching what signInAnonymously() does in
        // Supabase's official SDKs. If this 400s, check the network tab
        // on a test page using supabase-js's signInAnonymously() and
        // match the exact request here. Anonymous sign-ins must also be
        // toggled on in your project's Auth settings.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.dev.supabaseAnonKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new RuntimeException("Anonymous sign-in failed: HTTP "
                                + response.statusCode() + " " + response.body());
                    }
                    applyAuthResponse(response.body());
                })
                .thenCompose(v -> linkAuthUid(minecraftUuid));
    }

    private CompletableFuture<Void> refresh() {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String url = strip(config.dev.supabaseUrl) + "/auth/v1/token?grant_type=refresh_token";

        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", session.refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", config.dev.supabaseAnonKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new RuntimeException("Realtime session refresh failed: HTTP " + response.statusCode());
                    }
                    applyAuthResponse(response.body());
                });
    }

    private void applyAuthResponse(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        RealtimeSession s = new RealtimeSession();
        s.accessToken = json.get("access_token").getAsString();
        s.refreshToken = json.get("refresh_token").getAsString();
        s.expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + json.get("expires_in").getAsLong();
        s.authUid = json.getAsJsonObject("user").get("id").getAsString();
        this.session = s;
        s.save();
    }

    private CompletableFuture<Void> linkAuthUid(UUID minecraftUuid) {
        return SupabaseService.INSTANCE.linkAuthUid(minecraftUuid, session.authUid);
    }

    private static String strip(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}