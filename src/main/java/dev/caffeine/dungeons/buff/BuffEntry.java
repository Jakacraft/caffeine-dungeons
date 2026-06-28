package dev.caffeine.dungeons.buff;

public record BuffEntry(String label, String colorHex, long startTimeMs, long endTimeMs) {

    public static BuffEntry permanent(String label, String colorHex) {
        return new BuffEntry(label, colorHex, 0, 0);
    }

    public static BuffEntry timed(String label, String colorHex, long durationMs) {
        long now = System.currentTimeMillis();
        return new BuffEntry(label, colorHex, now, now + durationMs);
    }

    public boolean hasTimer() {
        return endTimeMs > 0;
    }

    public boolean isExpired() {
        return hasTimer() && System.currentTimeMillis() > endTimeMs;
    }

    /** 1.0 = full, 0.0 = expired */
    public float progress() {
        if (!hasTimer()) return 1f;
        long total = endTimeMs - startTimeMs;
        long remaining = endTimeMs - System.currentTimeMillis();
        return Math.max(0f, Math.min(1f, (float) remaining / total));
    }

    public String timerText() {
        if (!hasTimer()) return "";
        long remaining = Math.max(0, endTimeMs - System.currentTimeMillis()) / 1000;
        if (remaining >= 3600) return String.format("%dh%02dm", remaining / 3600, (remaining % 3600) / 60);
        if (remaining >= 60) return String.format("%dm%02ds", remaining / 60, remaining % 60);
        return remaining + "s";
    }
}