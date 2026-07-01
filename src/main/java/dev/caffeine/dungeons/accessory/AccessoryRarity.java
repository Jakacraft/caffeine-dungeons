package dev.caffeine.dungeons.accessory;

public enum AccessoryRarity {
    COMMON("Common", 0xAAAAAA),
    UNCOMMON("Uncommon", 0x55FF55),
    RARE("Rare", 0x5555FF),
    EPIC("Epic", 0xAA00AA),
    LEGENDARY("Legendary", 0xFFAA00),
    MYTHIC("Mythic", 0xFF55FF),
    HEROIC("Heroic", 0xFF5555);

    public final String displayName;
    public final int rgb;

    AccessoryRarity(String displayName, int rgb) {
        this.displayName = displayName;
        this.rgb = rgb;
    }

    public int argb() {
        return 0xFF000000 | rgb;
    }

    public static AccessoryRarity fromString(String s) {
        try {
            return valueOf(s.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}