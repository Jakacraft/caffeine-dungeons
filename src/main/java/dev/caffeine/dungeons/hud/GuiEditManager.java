package dev.caffeine.dungeons.hud;

import net.minecraft.client.MinecraftClient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class GuiEditManager {

    public record HudEntry(String label, HudPosition position, int width, int height) {}

    private static final Map<String, HudEntry> entries = new LinkedHashMap<>();

    private GuiEditManager() {}

    /**
     * Called by each renderer every frame.
     * Overwrites any previous entry for this label with fresh dimensions.
     */
    public static void register(String label, HudPosition pos, int width, int height) {
        entries.put(label, new HudEntry(label, pos, width, height));
    }

    public static Collection<HudEntry> getEntries() {
        return entries.values();
    }

    /** Call on DISCONNECT to reset stale entries. */
    public static void clear() {
        entries.clear();
    }

    /** Opens the editor screen. */
    public static void open() {
        MinecraftClient.getInstance().setScreen(new GuiPositionEditor());
    }
}