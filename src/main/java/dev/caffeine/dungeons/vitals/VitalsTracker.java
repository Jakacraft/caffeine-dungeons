package dev.caffeine.dungeons.vitals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VitalsTracker {

    public static final VitalsTracker INSTANCE = new VitalsTracker();

    private static final long STALE_MS = 8000L;

    private static final Pattern SEGMENT = Pattern.compile("(\\d+)(?:/(\\d+))?(.+?)\\s+(.+)");

    private record Parsed(String label, String icon, int current, int max) {}

    private final Map<String, VitalStat> stats = new LinkedHashMap<>();
    private long lastUpdateMs = 0L;

    private VitalsTracker() {}

    public boolean onActionBarMessage(String plainText) {
        if (plainText == null || !plainText.contains("|")) return false;

        Map<String, Parsed> parsed = new LinkedHashMap<>();
        for (String raw : plainText.split("\\|")) {
            String segment = raw.trim();
            if (segment.isEmpty()) continue;

            Matcher m = SEGMENT.matcher(segment);
            if (!m.matches()) return false;

            int current = parseInt(m.group(1));
            int max = m.group(2) != null ? parseInt(m.group(2)) : current;
            String icon = m.group(3).trim();
            String label = m.group(4).trim();
            parsed.put(label.toLowerCase(), new Parsed(label, icon, current, max));
        }

        if (!parsed.containsKey("health") || !parsed.containsKey("mana")) return false;

        for (Map.Entry<String, Parsed> e : parsed.entrySet()) {
            Parsed p = e.getValue();
            VitalStat existing = stats.get(e.getKey());
            if (existing == null) {
                stats.put(e.getKey(), new VitalStat(p.label(), p.icon(), p.current(), p.max()));
            } else {
                existing.update(p.icon(), p.current(), p.max());
            }
        }
        lastUpdateMs = System.currentTimeMillis();
        return true;
    }

    public VitalStat getHealth() {
        return stats.get("health");
    }

    public VitalStat getMana() {
        return stats.get("mana");
    }

    public VitalStat get(String label) {
        return stats.get(label.toLowerCase());
    }

    public boolean isStale(long nowMs) {
        return lastUpdateMs == 0L || nowMs - lastUpdateMs > STALE_MS;
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void tick() {
        for (VitalStat stat : stats.values()) {
            stat.tickAnimation();
        }
    }

    public void clear() {
        stats.clear();
        lastUpdateMs = 0L;
    }
}