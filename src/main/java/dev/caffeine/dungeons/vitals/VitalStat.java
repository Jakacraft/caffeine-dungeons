package dev.caffeine.dungeons.vitals;

public class VitalStat {

    private static final float PROGRESS_LERP = 0.25f;

    public String label;
    public String icon;
    public float current;
    public float max;

    public float displayedProgress = Float.NaN;

    public VitalStat(String label, String icon, float current, float max) {
        this.label = label;
        this.icon = icon;
        this.current = current;
        this.max = max;
    }

    public void update(String icon, float current, float max) {
        this.icon = icon;
        this.current = current;
        this.max = max;
    }

    public float trueProgress() {
        if (max <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, current / max));
    }

    public void tickAnimation() {
        float target = trueProgress();
        if (Float.isNaN(displayedProgress)) {
            displayedProgress = target;
        } else {
            displayedProgress += (target - displayedProgress) * PROGRESS_LERP;
        }
    }

    public static String format(float v) {
        if (v == Math.floor(v) && !Float.isInfinite(v)) {
            return String.valueOf((int) v);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override
    public String toString() {
        return label + "=" + format(current) + "/" + format(max) + " icon=[" + icon + "]";
    }
}