package dev.caffeine.dungeons.buff;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BuffTracker {

    private static final BuffTracker INSTANCE = new BuffTracker();
    public static BuffTracker getInstance() { return INSTANCE; }

    private volatile BuffEntry dailyEvent = null;
    private volatile BuffEntry tempEvent  = null;
    private final CopyOnWriteArrayList<BoosterDefinition> boosterDefs = new CopyOnWriteArrayList<>();
    private volatile Set<String> activeBoosterNames = Set.of();
    private final CopyOnWriteArrayList<BuffEntry> tempBuffs = new CopyOnWriteArrayList<>();

    private BuffTracker() {}

    public void setDailyEvent(BuffEntry e)       { this.dailyEvent = e; }
    public void setTempEvent(BuffEntry e)         { this.tempEvent  = e; }
    public void clearTempEvent()                  { this.tempEvent  = null; }
    public void setBoosters(List<BoosterDefinition> defs) { boosterDefs.clear(); boosterDefs.addAll(defs); }
    public void setActiveBoosterNames(Set<String> names)  { this.activeBoosterNames = Set.copyOf(names); }
    public void addTempBuff(BuffEntry e) {
        tempBuffs.removeIf(existing -> existing.label().equalsIgnoreCase(e.label()));
        tempBuffs.add(e);
    }

    public BuffEntry          getDailyEvent() { return dailyEvent; }
    public BuffEntry          getTempEvent()  { return tempEvent; }
    public List<BuffEntry> getBoosters() {
        Set<String> active = activeBoosterNames;
        return boosterDefs.stream()
                .filter(d -> active.contains(d.matchName()))
                .map(BoosterDefinition::entry)
                .collect(Collectors.toUnmodifiableList());
    }
    public List<BuffEntry>    getTempBuffs()  { return Collections.unmodifiableList(tempBuffs); }

    public void tick() {
        tempBuffs.removeIf(BuffEntry::isExpired);
    }

    public void clear() {
        dailyEvent = null;
        tempEvent  = null;
        boosterDefs.clear();
        activeBoosterNames = Set.of();
        tempBuffs.clear();
    }
}