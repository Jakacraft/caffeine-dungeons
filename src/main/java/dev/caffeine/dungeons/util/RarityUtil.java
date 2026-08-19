package dev.caffeine.dungeons.util;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;

import java.util.List;
import java.util.Optional;

/**
 * Extracts a rarity colour from an ItemStack by inspecting:
 *  1. The item's display name colour (primary signal)
 *  2. A lore line whose first styled character carries a colour (fallback)
 *
 * Returns -1 if no colour could be determined (i.e. the slot is empty or
 * the name uses the default white/no-colour style).
 */
public final class RarityUtil {

    private RarityUtil() {}

    /**
     * Returns the ARGB-packed rarity colour for the given stack, or -1 if none.
     * Alpha is always 0xFF (fully opaque) — callers dim it when drawing.
     */
    public static int getRarityColor(ItemStack stack) {
        if (stack.isEmpty()) return -1;

        // --- 1. Item name colour ---
        int nameColor = extractColor(stack.getName());
        if (nameColor != -1) return nameColor;

        // --- 2. First lore line colour ---
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                int loreColor = extractColor(line);
                if (loreColor != -1) return loreColor;
            }
        }

        return -1;
    }

    // -------------------------------------------------------------------------

    /**
     * Walks a Text tree (root + siblings) and returns the first non-null,
     * non-white TextColor it finds, as 0xFFRRGGBB.
     */
    private static int extractColor(Text text) {
        // Root style
        int c = colorFromText(text);
        if (c != -1) return c;

        // Siblings (server-sent names are often wrapped)
        for (Text sibling : text.getSiblings()) {
            int sc = extractColor(sibling); // recurse so we catch nested siblings
            if (sc != -1) return sc;
        }

        return -1;
    }

    private static int colorFromText(Text text) {
        TextColor tc = text.getStyle().getColor();
        if (tc == null) return -1;

        int rgb = tc.getRgb();

        // Ignore plain white (#FFFFFF) — that's just the default unstyled name
        if (rgb == 0xFFFFFF) return -1;

        return 0xFF000000 | rgb;
    }
}
