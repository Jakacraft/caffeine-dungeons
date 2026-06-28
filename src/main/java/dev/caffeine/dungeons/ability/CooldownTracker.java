package dev.caffeine.dungeons.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CooldownTracker {
    public static final CooldownTracker INSTANCE = new CooldownTracker();

    private final List<CooldownEntry> entries = new ArrayList<>();

    private CooldownTracker() {}

    public void onAbilityUsed(String abilityName) {
        AbilityData data = AbilityDatabase.INSTANCE.get(abilityName);
        int color = data != null ? data.getArgb() : 0xFFFFFFFF;
        long durationMs = data != null ? data.cooldown * 1000L : 5000L;

        // Refresh if already on cooldown
        entries.removeIf(e -> e.abilityName.equalsIgnoreCase(abilityName));
        entries.add(new CooldownEntry(abilityName, color, durationMs));
    }

    public void tick() {
        Iterator<CooldownEntry> it = entries.iterator();
        while (it.hasNext()) {
            CooldownEntry entry = it.next();
            if (entry.isExpired()) entry.markExpired();
            if (entry.shouldRemove()) it.remove();
        }
    }

    public List<CooldownEntry> getEntries() {
        return entries;
    }

    public void clear() {
        entries.clear();
    }
}