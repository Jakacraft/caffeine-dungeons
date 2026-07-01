package dev.caffeine.dungeons.title;

import java.awt.Color;
import java.util.UUID;

public final class ChromaUtil {
    private static final long CYCLE_MS = 2000L;

    private ChromaUtil() {}
    public static int getColor(UUID uuid) {
        float phase = (uuid.getLeastSignificantBits() & 0xFFL) / 255f;
        float hue   = ((System.currentTimeMillis() % CYCLE_MS) / (float) CYCLE_MS + phase) % 1f;
        return 0xFF000000 | (Color.HSBtoRGB(hue, 1f, 1f) & 0x00FFFFFF);
    }
}