package dev.caffeine.dungeons.title;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TitleRegistry {
    private static final TitleRegistry INSTANCE = new TitleRegistry();
    public static TitleRegistry getInstance() { return INSTANCE; }

    // Titles a player is eligible for — slow-changing, admin-curated (titles.json)
    private final Map<UUID, List<TitleEntry>> granted = new ConcurrentHashMap<>();

    // Which title id each player currently has selected — fast-changing,
    // player-controlled, refreshed via TitleDatabase's periodic poll
    private final Map<UUID, String> activeIds = new ConcurrentHashMap<>();

    private TitleRegistry() {}

    public void setGranted(UUID uuid, List<TitleEntry> entries) {
        granted.put(uuid, List.copyOf(entries));
    }

    public List<TitleEntry> getGranted(UUID uuid) {
        return granted.getOrDefault(uuid, List.of());
    }

    public void setActiveId(UUID uuid, String titleId) {
        if (titleId == null || titleId.isBlank()) {
            activeIds.remove(uuid);
        } else {
            activeIds.put(uuid, titleId);
        }
    }

    public String getActiveId(UUID uuid) {
        return activeIds.get(uuid);
    }

    /** Resolves the currently displayed title for a player, or null if none selected/granted. */
    public TitleEntry getActiveTitle(UUID uuid) {
        String activeId = activeIds.get(uuid);
        if (activeId == null) return null;
        for (TitleEntry entry : getGranted(uuid)) {
            if (entry.id().equals(activeId)) return entry;
        }
        return null;
    }

    public void clear() {
        granted.clear();
        activeIds.clear();
    }
}