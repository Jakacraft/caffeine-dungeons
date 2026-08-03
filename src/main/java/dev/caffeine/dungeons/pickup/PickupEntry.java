package dev.caffeine.dungeons.pickup;

import net.minecraft.item.ItemStack;

public class PickupEntry {

    public static final long HOLD_MS = 2600L;
    public static final long FADE_MS = 400L;
    public static final long PULSE_MS = 220L;

    public final String groupKey;
    public final ItemStack displayStack;
    public final int rarityColor;

    public int count;
    public final long firstSeenMs;
    public long lastUpdateMs;

    public float animatedBottomY = Float.NaN;

    public PickupEntry(String groupKey, ItemStack displayStack, int rarityColor, int count, long nowMs) {
        this.groupKey = groupKey;
        this.displayStack = displayStack;
        this.rarityColor = rarityColor;
        this.count = count;
        this.firstSeenMs = nowMs;
        this.lastUpdateMs = nowMs;
    }

    public void merge(int extra, long nowMs) {
        this.count += extra;
        this.lastUpdateMs = nowMs;
    }

    public boolean isExpired(long nowMs) {
        return nowMs - lastUpdateMs > HOLD_MS + FADE_MS;
    }

    public float alpha(long nowMs) {
        long since = nowMs - lastUpdateMs;
        if (since < HOLD_MS) return 1.0f;
        long intoFade = since - HOLD_MS;
        if (intoFade >= FADE_MS) return 0.0f;
        return 1.0f - intoFade / (float) FADE_MS;
    }

    public float pulse(long nowMs) {
        long since = nowMs - lastUpdateMs;
        if (since >= PULSE_MS) return 0.0f;
        return 1.0f - since / (float) PULSE_MS;
    }
}