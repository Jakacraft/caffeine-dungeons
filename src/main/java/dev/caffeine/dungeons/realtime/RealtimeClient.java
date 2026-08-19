package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RealtimeClient {

    private static final RealtimeClient INSTANCE = new RealtimeClient();
    public static RealtimeClient getInstance() { return INSTANCE; }

    private static final long[] RECONNECT_BACKOFF_MS = {2_000, 4_000, 8_000, 16_000, 30_000};

    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "caffeine-dungeons-realtime");
                t.setDaemon(true);
                return t;
            });

    private volatile WebSocket socket;
    private volatile UUID ownUuid;
    private volatile boolean intentionallyClosed = true;
    private int reconnectAttempt = 0;

    private RealtimeClient() {}

    public void connect(UUID minecraftUuid) {
        this.ownUuid = minecraftUuid;
        this.intentionallyClosed = false;
        doConnect();
    }

    public void disconnect() {
        intentionallyClosed = true;
        WebSocket s = socket;
        if (s != null) s.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect");
        socket = null;
    }

    private void doConnect() {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String wsUrl = toWebSocketUrl(config.dev.backendUrl) + "/realtime";

        http.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new Listener())
                .thenAccept(ws -> {
                    socket = ws;
                    reconnectAttempt = 0;
                    sendSubscribe();
                    CaffeineDungeons.LOGGER.info("[CDM] Realtime connected.");
                })
                .exceptionally(err -> {
                    CaffeineDungeons.LOGGER.error("[CDM] Realtime connect failed: {}", err.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void sendSubscribe() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "subscribe");
        msg.addProperty("uuid", ownUuid.toString());
        sendRaw(msg);
    }

    private void scheduleReconnect() {
        if (intentionallyClosed) return;
        long delay = RECONNECT_BACKOFF_MS[Math.min(reconnectAttempt, RECONNECT_BACKOFF_MS.length - 1)];
        reconnectAttempt++;
        scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }

    public void publish(UUID targetUuid, String messageType, JsonObject data) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String secret = config.dev.backendAdminKey;

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "publish");
        msg.addProperty("secret", secret);
        msg.addProperty("target", targetUuid.toString());
        msg.addProperty("messageType", messageType);
        msg.add("data", data);
        sendRaw(msg);
    }

    private void sendRaw(JsonObject msg) {
        WebSocket s = socket;
        if (s != null) {
            s.sendText(msg.toString(), true);
        } else {
            CaffeineDungeons.LOGGER.warn("[CDM] Tried to send over realtime while disconnected.");
        }
    }

    private static String toWebSocketUrl(String httpUrl) {
        String s = httpUrl.endsWith("/") ? httpUrl.substring(0, httpUrl.length() - 1) : httpUrl;
        if (s.startsWith("https://")) return "wss://" + s.substring("https://".length());
        if (s.startsWith("http://")) return "ws://" + s.substring("http://".length());
        return s;
    }

    private class Listener implements WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String full = buffer.toString();
                buffer.setLength(0);
                handleMessage(full);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            socket = null;
            if (!intentionallyClosed) scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            CaffeineDungeons.LOGGER.error("[CDM] Realtime socket error: {}", error.getMessage());
            socket = null;
            if (!intentionallyClosed) scheduleReconnect();
        }
    }

    private void handleMessage(String raw) {
        JsonObject msg;
        try {
            msg = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (!msg.has("type") || !msg.has("data")) return;
        RealtimeDispatcher.dispatch(msg.get("type").getAsString(), msg.getAsJsonObject("data"));
    }
}