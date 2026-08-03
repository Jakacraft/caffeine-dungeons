package dev.caffeine.dungeons.Backend;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.network.ClientPlayerEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class BackendService {

    public static final BackendService INSTANCE = new BackendService();

    private static final String TABLE_PLAYERS = "players";

    private BackendClient client;
    private String cachedUrl = "";
    private String cachedKey = "";

    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    private BackendService() {}

    public CompletableFuture<Void> registerLocalPlayer(ClientPlayerEntity player) {
        BackendClient c = getClient();
        if (c == null) return CompletableFuture.completedFuture(null);

        PlayerData data = new PlayerData();
        data.uuid     = player.getUuidAsString();
        data.username = player.getName().getString();
        data.lastSeen = Instant.now().toString();
        data.hasMod   = true;

        return c.upsert(TABLE_PLAYERS, BackendClient.GSON.toJson(data))
                .thenRun(() -> CaffeineDungeons.LOGGER.info("[CDM] Registered player: {}", data.username));
    }

    public CompletableFuture<Void> linkAuthUid(UUID uuid, String authUid) {
        BackendClient c = getClient();
        if (c == null) return CompletableFuture.completedFuture(null);
        JsonObject body = new JsonObject();
        body.addProperty("auth_uid", authUid);
        return c.patch(TABLE_PLAYERS, "uuid=eq." + uuid, body.toString())
                .thenRun(() -> {});
    }

    public CompletableFuture<PlayerData> fetchPlayer(UUID uuid) {
        BackendClient c = getClient();
        if (c == null) return CompletableFuture.completedFuture(null);

        PlayerData cached = playerCache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return c.get(TABLE_PLAYERS, "uuid=eq." + uuid + "&select=*").thenApply(json -> {
            if (json == null) return null;
            PlayerData[] arr = BackendClient.GSON.fromJson(json, PlayerData[].class);
            if (arr == null || arr.length == 0) return null;
            playerCache.put(uuid, arr[0]);
            return arr[0];
        });
    }

    public boolean hasMod(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        return data != null && data.hasMod;
    }

    public CompletableFuture<String> fetchActiveTitles() {
        BackendClient c = getClient();
        if (c == null) return CompletableFuture.completedFuture(null);
        return c.get(TABLE_PLAYERS, "select=uuid,active_title_id");
    }

    /** Sets (or clears, if titleId is null) the local player's active title selection. */
    public void setActiveTitle(UUID uuid, String titleId) {
        BackendClient c = getClient();
        if (c == null) return;
        JsonObject body = new JsonObject();
        if (titleId == null) {
            body.add("active_title_id", JsonNull.INSTANCE);
        } else {
            body.addProperty("active_title_id", titleId);
        }
        c.patch(TABLE_PLAYERS, "uuid=eq." + uuid, body.toString());
    }

    public void invalidateCache(UUID uuid) {
        playerCache.remove(uuid);
    }

    public void clearCache() {
        playerCache.clear();
    }

    private synchronized BackendClient getClient() {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String url = config.dev.backendUrl.trim();
        String key = config.dev.backendAdminKey.trim();

        if (url.isEmpty() || key.isEmpty()) return null;

        if (!url.equals(cachedUrl) || !key.equals(cachedKey)) {
            client    = new BackendClient(url, key);
            cachedUrl = url;
            cachedKey = key;
            CaffeineDungeons.LOGGER.info("[CDM] Client initialised for {}", url);
        }

        return client;
    }
}