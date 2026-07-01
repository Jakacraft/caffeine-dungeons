package dev.caffeine.dungeons.title;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TitleRegistry {
    private static final TitleRegistry INSTANCE = new TitleRegistry();
    public static TitleRegistry getInstance() { return INSTANCE; }

    private final Map<UUID, TitleEntry> titles = new ConcurrentHashMap<>();

    private TitleRegistry() {}

    public void register(UUID uuid, TitleEntry entry) { titles.put(uuid, entry); }
    public TitleEntry getTitle(UUID uuid)              { return titles.get(uuid); }
    public void clear()                                { titles.clear(); }
}