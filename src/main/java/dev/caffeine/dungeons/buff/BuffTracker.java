package dev.caffeine.dungeons.buff;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BuffTracker {

    private static final BuffTracker INSTANCE = new BuffTracker();
    public static BuffTracker getInstance() { return INSTANCE; }

    private volatile BuffEntry dailyEvent = null;
    private volatile BuffEntry tempEvent  = null;
    private final CopyOnWriteArrayList<BuffEntry> boosters  = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BuffEntry> tempBuffs = new CopyOnWriteArrayList<>();

    private BuffTracker() {}

    // --- Setters ---
    public void setDailyEvent(BuffEntry e)       { this.dailyEvent = e; }
    public void setTempEvent(BuffEntry e)         { this.tempEvent  = e; }
    public void clearTempEvent()                  { this.tempEvent  = null; }
    public void setBoosters(List<BuffEntry> list) { boosters.clear(); boosters.addAll(list); }
    public void addTempBuff(BuffEntry e)          { tempBuffs.add(e); }

    // --- Getters ---
    public BuffEntry          getDailyEvent() { return dailyEvent; }
    public BuffEntry          getTempEvent()  { return tempEvent; }
    public List<BuffEntry>    getBoosters()   { return Collections.unmodifiableList(boosters); }
    public List<BuffEntry>    getTempBuffs()  { return Collections.unmodifiableList(tempBuffs); }

    /** Called each END_CLIENT_TICK — prunes expired temp buffs. */
    public void tick() {
        tempBuffs.removeIf(BuffEntry::isExpired);
    }

    /** Called on DISCONNECT. */
    public void clear() {
        dailyEvent = null;
        tempEvent  = null;
        boosters.clear();
        tempBuffs.clear();
    }
}