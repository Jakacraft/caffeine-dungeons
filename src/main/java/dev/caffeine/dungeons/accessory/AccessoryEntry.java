package dev.caffeine.dungeons.accessory;

import java.util.Comparator;

public record AccessoryEntry(String name, AccessoryRarity rarity, Integer floor) {

    /**
     * rarity ordinal → floor (null last) → name alphabetical
     */
    public static final Comparator<AccessoryEntry> SORT_ORDER =
            Comparator.comparingInt((AccessoryEntry e) -> e.rarity().ordinal())
                    .thenComparingInt(e -> e.floor() != null ? e.floor() : Integer.MAX_VALUE)
                    .thenComparing(e -> e.name().toLowerCase());
}