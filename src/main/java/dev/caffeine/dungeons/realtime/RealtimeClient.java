package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RealtimeClient {

    private static final RealtimeClient INSTANCE = new RealtimeClient();
    public static RealtimeClient getInstance() { return INSTANCE; }

    private static final long HEARTBEAT_INTERVAL_MS = 25_000;
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
    private final AtomicInteger refCounter = new AtomicInteger(1);
    private final Map<String, CompletableFuture<Boolean>> pendingJoins = new ConcurrentHashMap<>();
    private final Map<String, Boolean> autoRejoinTopics = new ConcurrentHashMap<>();
    // Topics currently joined on the live socket — lets sendOnceTo skip a
    // redundant join/leave cycle when the target topic is already
    // subscribed (e.g. self-testing by targeting your own username, where
    // your persistent "player:<uuid>" join is already active).
    private final Set<String> activeTopics = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> heartbeatTask;
    private int reconnectAttempt = 0;

    private RealtimeClient() {}

    public void connect(UUID minecraftUuid) {
        this.ownUuid = minecraftUuid;
        this.intentionallyClosed = false;
        doConnect();
    }

    public void disconnect() {
        intentionallyClosed = true;
        if (heartbeatTask != null) heartbeatTask.cancel(true);
        autoRejoinTopics.clear();
        pendingJoins.clear();
        activeTopics.clear();
        WebSocket s = socket;
        if (s != null) s.sendClose(WebSocket.NORMAL_CLOSURE, "client disconnect");
        socket = null;
    }

    private void doConnect() {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String wsUrl = toWebSocketUrl(config.dev.supabaseUrl) + "/realtime/v1/websocket"
                + "?apikey=" + config.dev.supabaseAnonKey + "&vsn=1.0.0";

        http.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new Listener())
                .thenAccept(ws -> {
                    socket = ws;
                    reconnectAttempt = 0;
                    startHeartbeat();
                    joinChannel("player:" + ownUuid, true);
                    for (String topic : autoRejoinTopics.keySet()) {
                        if (!topic.equals("player:" + ownUuid)) joinChannel(topic, true);
                    }
                })
                .exceptionally(err -> {
                    System.err.println("[CaffeineDungeons] Realtime connect failed: " + err.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (intentionallyClosed) return;
        long delay = RECONNECT_BACKOFF_MS[Math.min(reconnectAttempt, RECONNECT_BACKOFF_MS.length - 1)];
        reconnectAttempt++;
        scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }

    private void startHeartbeat() {
        if (heartbeatTask != null) heartbeatTask.cancel(true);
        heartbeatTask = scheduler.scheduleAtFixedRate(
                () -> sendRaw(envelope("phoenix", "heartbeat", new JsonObject())),
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Boolean> joinChannel(String logicalTopic, boolean autoRejoin) {
        if (autoRejoin) autoRejoinTopics.put(logicalTopic, true);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        String ref = nextRef();
        pendingJoins.put(ref, future);

        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("ack", false);
        broadcast.addProperty("self", false);
        JsonObject presence = new JsonObject();
        presence.addProperty("key", "");
        JsonObject channelConfig = new JsonObject();
        channelConfig.add("broadcast", broadcast);
        channelConfig.add("presence", presence);
        channelConfig.addProperty("private", true);

        JsonObject payload = new JsonObject();
        payload.add("config", channelConfig);
        payload.addProperty("access_token", RealtimeAuth.getInstance().getAccessToken());

        JsonObject msg = new JsonObject();
        msg.addProperty("topic", "realtime:" + logicalTopic);
        msg.addProperty("event", "phx_join");
        msg.add("payload", payload);
        msg.addProperty("ref", ref);

        sendRaw(msg);

        // Track successful joins so sendOnceTo can detect "already
        // subscribed" and skip a redundant join/leave cycle.
        future.thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) activeTopics.add(logicalTopic);
        });

        return future;
    }

    public void leaveChannel(String logicalTopic) {
        autoRejoinTopics.remove(logicalTopic);
        activeTopics.remove(logicalTopic);
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", "realtime:" + logicalTopic);
        msg.addProperty("event", "phx_leave");
        msg.add("payload", new JsonObject());
        msg.addProperty("ref", nextRef());
        sendRaw(msg);
    }

    /**
     * Joins a target topic transiently, sends one message, then leaves.
     * If we're already subscribed to that topic on this socket (e.g. it's
     * our own persistent "player:<uuid>" channel, as when testing by
     * targeting yourself), skips the join/leave and just broadcasts.
     */
    public void sendOnceTo(String logicalTopic, RealtimeMessage message) {
        if (activeTopics.contains(logicalTopic)) {
            broadcast(logicalTopic, message);
            return;
        }
        joinChannel(logicalTopic, false).whenComplete((ok, err) -> {
            if (Boolean.TRUE.equals(ok)) {
                broadcast(logicalTopic, message);
            } else {
                System.err.println("[CaffeineDungeons] Could not join " + logicalTopic
                        + (err != null ? ": " + err.getMessage() : " (not authorized)"));
            }
            leaveChannel(logicalTopic);
        });
    }

    public void broadcast(String logicalTopic, RealtimeMessage message) {
        JsonObject inner = new JsonObject();
        inner.addProperty("type", "broadcast");
        inner.addProperty("event", "message");
        inner.add("payload", message.toJson());

        JsonObject msg = new JsonObject();
        msg.addProperty("topic", "realtime:" + logicalTopic);
        msg.addProperty("event", "broadcast");
        msg.add("payload", inner);
        msg.addProperty("ref", nextRef());
        sendRaw(msg);
    }

    private JsonObject envelope(String topic, String event, JsonObject payload) {
        JsonObject msg = new JsonObject();
        msg.addProperty("topic", topic);
        msg.addProperty("event", event);
        msg.add("payload", payload);
        msg.addProperty("ref", nextRef());
        return msg;
    }

    private void sendRaw(JsonObject msg) {
        WebSocket s = socket;
        if (s != null) s.sendText(msg.toString(), true);
    }

    private String nextRef() {
        return String.valueOf(refCounter.getAndIncrement());
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
            activeTopics.clear();
            if (!intentionallyClosed) scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("[CaffeineDungeons] Realtime socket error: " + error.getMessage());
            socket = null;
            activeTopics.clear();
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
        String event = msg.has("event") ? msg.get("event").getAsString() : "";
        JsonObject payload = msg.has("payload") && msg.get("payload").isJsonObject()
                ? msg.getAsJsonObject("payload") : new JsonObject();

        switch (event) {
            case "phx_reply" -> {
                String ref = msg.has("ref") && !msg.get("ref").isJsonNull() ? msg.get("ref").getAsString() : null;
                CompletableFuture<Boolean> pending = ref != null ? pendingJoins.remove(ref) : null;
                if (pending != null) {
                    String status = payload.has("status") ? payload.get("status").getAsString() : "error";
                    pending.complete("ok".equals(status));
                }
            }
            case "broadcast" -> {
                if (payload.has("event") && "message".equals(payload.get("event").getAsString())
                        && payload.has("payload") && payload.get("payload").isJsonObject()) {
                    JsonObject env = payload.getAsJsonObject("payload");
                    if (env.has("type") && env.has("data")) {
                        RealtimeDispatcher.dispatch(env.get("type").getAsString(), env.getAsJsonObject("data"));
                    }
                }
            }
            default -> { }
        }
    }
}