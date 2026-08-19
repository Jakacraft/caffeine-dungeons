package dev.caffeine.dungeons.vitals;

public class VitalStat {

    private static final float PROGRESS_LERP = 0.25f;

    public String label;
    public String icon;
    public int current;
    public int max;

    public float displayedProgress = Float.NaN;

    public VitalStat(String label, String icon, int current, int max) {
        this.label = label;
        this.icon = icon;
        this.current = current;
        this.max = max;
    }

    public void update(String icon, int current, int max) {
        this.icon = icon;
        this.current = current;
        this.max = max;
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
}