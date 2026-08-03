package dev.caffeine.dungeons.skillxp;

public class SkillXpEntry {

    public static final long HOLD_MS = 2200L;
    public static final long FADE_MS = 500L;
    public static final long PULSE_MS = 250L;
    private static final float PROGRESS_LERP = 0.15f;

    public String skillName;
    public long current;
    public long max;
    public long lastGain;
    public final long firstSeenMs;
    public long lastUpdateMs;

    public float displayedProgress = Float.NaN;

    public SkillXpEntry(String skillName, long current, long max, long gain, long nowMs) {
        this.skillName = skillName;
        this.current = current;
        this.max = max;
        this.lastGain = gain;
        this.firstSeenMs = nowMs;
        this.lastUpdateMs = nowMs;
    }

    public void update(long current, long max, long gain, long nowMs) {
        this.current = current;
        this.max = max;
        this.lastGain = gain;
        this.lastUpdateMs = nowMs;
    }

    public float trueProgress() {
        if (max <= 0) return 0f;
        return Math.max(0f, Math.min(1f, current / (float) max));
    }

    public void tickAnimation() {
        float target = trueProgress();
        if (Float.isNaN(displayedProgress)) {
            displayedProgress = target;
        } else {
            displayedProgress += (target - displayedProgress) * PROGRESS_LERP;
        }
    }

    public boolean isExpired(long nowMs) {
        return nowMs - lastUpdateMs > HOLD_MS + FADE_MS;
    }

    public float alpha(long nowMs) {
        long since = nowMs - lastUpdateMs;
        if (since < HOLD_MS) return 1.0f;
        long intoFade = since - HOLD_MS;
        if (intoFade >= FADE_MS) return 0.0f;
        return 1.0f - intoFade / (float) FADE_MS;
    }

    public float pulse(long nowMs) {
        long since = nowMs - lastUpdateMs;
        if (since >= PULSE_MS) return 0.0f;
        return 1.0f - since / (float) PULSE_MS;
    }
}