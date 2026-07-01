package dev.caffeine.dungeons.accessory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class AccessoryTracker {
    private static final long BAG_PAGE_GRACE_MS = 500L;

    private static final AccessoryTracker INSTANCE = new AccessoryTracker();

    public static AccessoryTracker getInstance() {
        return INSTANCE;
    }

    private final Set<String> foundNames = new CopyOnWriteArraySet<>();
    private final Set<String> hiddenNames = new CopyOnWriteArraySet<>();
    private volatile List<AccessoryEntry> missingList = List.of();
    private volatile boolean dirty = false;
    private volatile boolean inBagSession = false;
    private volatile long lastBagTime = 0L;

    private AccessoryTracker() {
    }

    /**
     * Call every tick while Accessory Bag screen is open.
     */
    public void tickInBag() {
        long now = System.currentTimeMillis();
        if (!inBagSession) {
            // Fresh session — reset found and scroll
            foundNames.clear();
            dirty = true;
            inBagSession = true;
            AccessoryHudRenderer.resetScroll();
        }
        lastBagTime = now;
    }

    /**
     * Call every tick while NOT in Accessory Bag screen.
     * Grace period handles page navigation (screen briefly closes).
     */
    public void tickOutOfBag() {
        if (inBagSession && System.currentTimeMillis() - lastBagTime > BAG_PAGE_GRACE_MS) {
            inBagSession = false;
        }
    }

    public void markFound(String name) {
        if (foundNames.add(name)) dirty = true;
    }

    /**
     * Hides an accessory from the missing list for the rest of the session.
     */
    public void hide(String name) {
        if (hiddenNames.add(name)) dirty = true;
    }

    public boolean isHidden(String name) {
        return hiddenNames.contains(name);
    }

    /**
     * Clears all session-hidden accessories, restoring them to the missing list.
     */
    public void unhideAll() {
        if (!hiddenNames.isEmpty()) {
            hiddenNames.clear();
            dirty = true;
        }
    }

    public void setDirty() {
        dirty = true;
    }

    public List<AccessoryEntry> getMissing() {
        if (dirty) recompute();
        return missingList;
    }

    public boolean isInBagSession() {
        return inBagSession;
    }

    public void clear() {
        foundNames.clear();
        hiddenNames.clear();
        missingList = List.of();
        inBagSession = false;
        lastBagTime = 0L;
        dirty = false;
    }

    private void recompute() {
        List<AccessoryEntry> master = AccessoryDatabase.getInstance().getMasterList();
        List<AccessoryEntry> missing = new ArrayList<>();
        for (AccessoryEntry e : master) {
            if (!foundNames.contains(e.name()) && !hiddenNames.contains(e.name())) missing.add(e);
        }
        missingList = Collections.unmodifiableList(missing);
        dirty = false;
    }

    public int hiddenCount() {
        return hiddenNames.size();
    }
}