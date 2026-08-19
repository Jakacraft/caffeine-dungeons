package dev.caffeine.dungeons.ability;

public class CooldownEntry {
    public final String abilityName;
    public final int color;
    private final long startTimeMs;
    private final long durationMs;
    private long expiredAtMs = -1;

    private static final long FADE_DURATION_MS = 600;

    public CooldownEntry(String abilityName, int color, long durationMs) {
        this.abilityName = abilityName;
        this.color = color;
        this.startTimeMs = System.currentTimeMillis();
        this.durationMs = durationMs;
    }

    public long getStartTime() {
        return startTimeMs;
    }

    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        return Math.max(0f, 1f - (float) elapsed / durationMs);
    }

    public float getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        return Math.max(0f, (durationMs - elapsed) / 1000f);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTimeMs >= durationMs;
    }

    public void markExpired() {
        if (expiredAtMs == -1) expiredAtMs = System.currentTimeMillis();
    }

    public float getFadeAlpha() {
        if (expiredAtMs == -1) return 1f;
        long elapsed = System.currentTimeMillis() - expiredAtMs;
        return Math.max(0f, 1f - (float) elapsed / FADE_DURATION_MS);
    }

    public boolean shouldRemove() {
        if (expiredAtMs == -1) return false;
        return System.currentTimeMillis() - expiredAtMs >= FADE_DURATION_MS;
    }
}