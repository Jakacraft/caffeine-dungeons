package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteCustomSoundHandler {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "caffeine-dungeons-custom-sound");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Path CACHE_DIR = Path.of("config", "caffeine_dungeons_sound_cache");

    public static void init() {
        RealtimeDispatcher.register("remote_custom_sound", RemoteCustomSoundHandler::handle);
    }

    private static void handle(JsonObject data) {
        if (!data.has("filename")) return;
        String filename = sanitize(data.get("filename").getAsString());
        if (filename == null) {
            CaffeineDungeons.LOGGER.warn("[CDM] Rejected unsafe custom sound filename: {}", data.get("filename"));
            return;
        }

        EXECUTOR.submit(() -> {
            try {
                Path local = getOrDownload(filename);
                if (local != null) playWav(local);
            } catch (Exception e) {
                CaffeineDungeons.LOGGER.error("[CDM] Failed to play custom sound {}: {}", filename, e.getMessage());
            }
        });
    }

    private static String sanitize(String filename) {
        String name = Path.of(filename).getFileName().toString();
        if (name.isBlank() || name.contains("..")) return null;
        return name;
    }

    private static Path getOrDownload(String filename) throws Exception {
        Files.createDirectories(CACHE_DIR);
        Path local = CACHE_DIR.resolve(filename);
        if (Files.exists(local)) return local; // cached from a previous troll

        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String base = config.dev.backendUrl.endsWith("/")
                ? config.dev.backendUrl.substring(0, config.dev.backendUrl.length() - 1)
                : config.dev.backendUrl;
        String url = base + "/sounds/" + filename;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            CaffeineDungeons.LOGGER.warn("[CDM] Custom sound download failed ({}): {}", response.statusCode(), url);
            return null;
        }

        Path tmp = Files.createTempFile("cdm_sound", ".tmp");
        Files.write(tmp, response.body());
        Files.move(tmp, local, StandardCopyOption.REPLACE_EXISTING);
        return local;
    }

    private static void playWav(Path file) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.start();
        }
    }
}