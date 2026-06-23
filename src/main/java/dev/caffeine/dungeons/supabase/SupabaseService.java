package dev.caffeine.dungeons.supabase;

import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.network.ClientPlayerEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SupabaseService {

    public static final SupabaseService INSTANCE = new SupabaseService();

    private static final String TABLE_PLAYERS = "players";

    private SupabaseClient client;
    private String cachedUrl = "";
    private String cachedKey = "";

    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    private SupabaseService() {}

    public void registerLocalPlayer(ClientPlayerEntity player) {
        SupabaseClient c = getClient();
        if (c == null) return;

        PlayerData data = new PlayerData();
        data.uuid     = player.getUuidAsString();
        data.username = player.getName().getString();
        data.lastSeen = Instant.now().toString();
        data.hasMod   = true;

        c.upsert(TABLE_PLAYERS, SupabaseClient.GSON.toJson(data))
                .thenRun(() -> CaffeineDungeons.LOGGER.info("[Supabase] Registered player: {}", data.username));
    }

    public CompletableFuture<PlayerData> fetchPlayer(UUID uuid) {
        SupabaseClient c = getClient();
        if (c == null) return CompletableFuture.completedFuture(null);

        PlayerData cached = playerCache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return c.get(TABLE_PLAYERS, "uuid=eq." + uuid + "&select=*").thenApply(json -> {
            if (json == null) return null;
            PlayerData[] arr = SupabaseClient.GSON.fromJson(json, PlayerData[].class);
            if (arr == null || arr.length == 0) return null;
            playerCache.put(uuid, arr[0]);
            return arr[0];
        });
    }

    public boolean hasMod(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        return data != null && data.hasMod;
    }

    public void pushOverlevels(UUID uuid, PlayerData overlevels) {
        SupabaseClient c = getClient();
        if (c == null) return;
        c.patch(TABLE_PLAYERS, "uuid=eq." + uuid, SupabaseClient.GSON.toJson(overlevels));
    }

    public void invalidateCache(UUID uuid) {
        playerCache.remove(uuid);
    }

    public void clearCache() {
        playerCache.clear();
    }

    private synchronized SupabaseClient getClient() {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String url = config.supabaseUrl.trim();
        String key = config.supabaseAnonKey.trim();

        if (url.isEmpty() || key.isEmpty()) return null;

        if (!url.equals(cachedUrl) || !key.equals(cachedKey)) {
            client    = new SupabaseClient(url, key);
            cachedUrl = url;
            cachedKey = key;
            CaffeineDungeons.LOGGER.info("[Supabase] Client initialised for {}", url);
        }

        return client;
    }
}