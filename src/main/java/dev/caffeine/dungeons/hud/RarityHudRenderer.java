package dev.caffeine.dungeons.hud;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.util.RarityUtil;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class RarityHudRenderer {

    private static final float SHAPE_HALF_SIZE  = 8f;
    private static final int   BORDER_THICKNESS = 2;

    // Called once per hotbar slot, before the item is drawn
    public static void renderSlotIndicator(DrawContext context, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) return;

        int color = RarityUtil.getRarityColor(stack);
        if (color == -1) return;

        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        int argb = applyAlpha(color, config.rarityIndicatorAlpha / 100f);

        // x, y is the top-left of the 16x16 item render area
        int cx = x + 8;
        int cy = y + 8;

        switch (config.rarityIndicatorStyle) {
            case CIRCLE -> drawCircle(context, cx, cy, SHAPE_HALF_SIZE, argb);
            case SQUARE -> context.fill(
                    Math.round(cx - SHAPE_HALF_SIZE), Math.round(cy - SHAPE_HALF_SIZE),
                    Math.round(cx + SHAPE_HALF_SIZE), Math.round(cy + SHAPE_HALF_SIZE),
                    argb
            );
            case BORDER -> drawBorder(context, x - 1, y - 1, x + 17, y + 17, BORDER_THICKNESS, argb);
        }
    }

    private static void drawCircle(DrawContext context, int cx, int cy, float radius, int argb) {
        int r = Math.round(radius);
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            context.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, argb);
        }
    }

    private static void drawBorder(DrawContext context, int x1, int y1, int x2, int y2,
                                   int thickness, int argb) {
        context.fill(x1,             y1,             x2,             y1 + thickness, argb);
        context.fill(x1,             y2 - thickness, x2,             y2,             argb);
        context.fill(x1,             y1 + thickness, x1 + thickness, y2 - thickness, argb);
        context.fill(x2 - thickness, y1 + thickness, x2,             y2 - thickness, argb);
    }

    private static int applyAlpha(int rgb, float alpha) {
        return (Math.round(alpha * 255) << 24) | (rgb & 0x00FFFFFF);
    }
}