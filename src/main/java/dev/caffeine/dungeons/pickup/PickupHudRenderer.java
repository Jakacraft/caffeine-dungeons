package dev.caffeine.dungeons.pickup;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class PickupHudRenderer {
    private static final String LABEL = "pickup";
    private static final int MAX_VISIBLE_FOR_LAYOUT = 9;
    private static final int ROW_HEIGHT = 11; // vanilla fontHeight (9) + 1px padding top/bottom
    private static final int ROW_GAP = 4;
    private static final int CAP_RADIUS = 3;
    private static final int ARROW_POINT_NAME = 8;
    private static final int ARROW_POINT_ICON = 6;
    private static final int LEFT_MARGIN = ARROW_POINT_NAME + 4;
    private static final int SEGMENT_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int END_MARGIN = 6;
    private static final int MAX_PANEL_W = 210;
    private static final int MIN_PANEL_W = 70;
    private static final int TEXT_ROW_HEIGHT = 18;
    private static final int TEXT_ROW_GAP = 1;
    private static final int BOTTOM_CLEARANCE = 90;
    private static final int BASE_BG = 0xCC15151f;
    private static final float NAME_TINT = 0.55f;
    private static final float ICON_TINT = 0.22f;
    private static final int TEXT_COLOR = 0xFFEDEDED;
    private static final int COUNT_COLOR = 0xFFEDEDED;
    private static final float VERTICAL_LERP = 0.35f;

    private PickupHudRenderer() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<PickupEntry> entries = new ArrayList<>(PickupTracker.INSTANCE.getActive());

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        CaffeineConfig config = me.shedaniel.autoconfig.AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        HudPosition pos = config.pickupHud.pos;
        boolean textStyle = config.pickupHud.style == CaffeineConfig.PickupHudStyle.TEXT;

        int rowH = textStyle ? TEXT_ROW_HEIGHT : ROW_HEIGHT;
        int rowGap = textStyle ? TEXT_ROW_GAP : ROW_GAP;
        int rowStep = rowH + rowGap;
        int reservedH = MAX_VISIBLE_FOR_LAYOUT * rowStep;
        int defaultX = screenW - Math.round(MAX_PANEL_W * pos.scale) - 8;
        int defaultY = screenH - BOTTOM_CLEARANCE - Math.round(reservedH * pos.scale);
        int boxX = pos.getX(defaultX);
        int boxY = pos.getY(defaultY);

        GuiEditManager.register(LABEL, pos, MAX_PANEL_W, reservedH);

        if (entries.isEmpty()) return;
        float anchorX = boxX + MAX_PANEL_W * pos.scale;
        float anchorY = boxY + reservedH * pos.scale;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(pos.scale, pos.scale);

        long now = System.currentTimeMillis();
        int n = entries.size();

        for (int i = 0; i < n; i++) {
            PickupEntry entry = entries.get(i);
            int rankFromNewest = (n - 1) - i;
            float targetRelBottomY = -rankFromNewest * rowStep;

            if (Float.isNaN(entry.animatedBottomY)) {
                entry.animatedBottomY = 200f;
            }
            entry.animatedBottomY += (targetRelBottomY - entry.animatedBottomY) * VERTICAL_LERP;

            drawEntry(context, entry, entry.animatedBottomY, rowH, now, config.pickupHud.style);
        }

        matrices.popMatrix();
    }

    private static void drawEntry(DrawContext context, PickupEntry entry, float bottomY, int rowH,
                                  long now, CaffeineConfig.PickupHudStyle style) {
        float alpha = entry.alpha(now);
        if (alpha <= 0f) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String rawName = entry.displayStack.getName().getString();
        String countText = String.valueOf(entry.count);

        int a = (int) (alpha * 255) << 24;
        int textColor = (TEXT_COLOR & 0x00FFFFFF) | a;
        int countColor = (COUNT_COLOR & 0x00FFFFFF) | a;

        int top = Math.round(bottomY) - rowH;

        if (style == CaffeineConfig.PickupHudStyle.TEXT) {
            int countW = textRenderer.getWidth(countText);
            int nameW = textRenderer.getWidth(rawName);
            int textY = top + (rowH - textRenderer.fontHeight) / 2;
            int nameColor = (entry.rarityColor & 0x00FFFFFF) | a;

            int countX = -countW;
            int iconX = countX - ICON_GAP - ICON_SIZE;
            int iconY = top + (rowH - ICON_SIZE) / 2;
            int nameX = iconX - ICON_GAP - nameW;

            context.drawText(textRenderer, Text.literal(rawName), nameX, textY, nameColor, true);
            context.drawItem(entry.displayStack, iconX, iconY);
            context.drawText(textRenderer, Text.literal(countText), countX, textY, countColor, true);
            return;
        }

        int countW = textRenderer.getWidth(countText);
        int iconSegW = END_MARGIN + countW + ICON_GAP + ICON_SIZE + ICON_GAP;
        int maxNameW = MAX_PANEL_W - iconSegW - LEFT_MARGIN - SEGMENT_GAP;
        String name = truncate(textRenderer, rawName, Math.max(20, maxNameW));
        int nameW = textRenderer.getWidth(name);
        int nameSegW = LEFT_MARGIN + nameW + SEGMENT_GAP;

        int totalW = Math.min(MAX_PANEL_W, Math.max(MIN_PANEL_W, iconSegW + nameSegW));
        if (iconSegW + nameSegW > totalW) {
            nameSegW = Math.max(LEFT_MARGIN + 4, totalW - iconSegW);
        }

        int alphaOnly = a;
        int nameBg = blendColor(BASE_BG, entry.rarityColor, NAME_TINT, alphaOnly);
        int iconBg = blendColor(BASE_BG, entry.rarityColor, ICON_TINT, alphaOnly);

        float pulse = entry.pulse(now);
        if (pulse > 0f) {
            nameBg = blendColor(nameBg, 0xFFFFFF | alphaOnly, pulse * 0.3f, alphaOnly);
            iconBg = blendColor(iconBg, 0xFFFFFF | alphaOnly, pulse * 0.3f, alphaOnly);
        }

        int nameRight = -iconSegW;
        int nameLeft = nameRight - nameSegW;
        fillArrowSegment(context, nameLeft, nameRight, top, rowH, ARROW_POINT_NAME, 0, nameBg);
        fillArrowSegment(context, -iconSegW, 0, top, rowH, ARROW_POINT_ICON, CAP_RADIUS, iconBg);

        int textY = top + (rowH - textRenderer.fontHeight + 2) / 2;
        int nameX = nameLeft + LEFT_MARGIN - 8;
        context.drawText(textRenderer, Text.literal(name), nameX, textY, textColor, false);

        int iconX = -iconSegW + ICON_GAP;
        int iconY = top + (rowH - ICON_SIZE) / 2;
        context.drawItem(entry.displayStack, iconX, iconY); // TODO: verify exact DrawContext method name for 1.21.11

        int countX = -END_MARGIN - countW;
        context.drawText(textRenderer, Text.literal(countText), countX, textY, countColor, false);
    }

    private static String truncate(TextRenderer textRenderer, String name, int maxWidth) {
        if (textRenderer.getWidth(name) <= maxWidth) return name;
        String ellipsis = "...";
        int budget = Math.max(0, maxWidth - textRenderer.getWidth(ellipsis));
        String trimmed = textRenderer.trimToWidth(name, budget); // TODO: verify method name in your TextRenderer mappings
        return trimmed + ellipsis;
    }

    private static void fillArrowSegment(DrawContext context, int rectLeft, int rectRight, int y, int h,
                                         int pointDepth, int rightCornerRadius, int color) {
        int half = h / 2;
        rightCornerRadius = Math.min(rightCornerRadius, half);

        for (int row = 0; row < h; row++) {
            int distFromCenter = Math.abs(row - half);
            int extend = half > 0 ? Math.round(pointDepth * (1f - (float) distFromCenter / half)) : 0;

            int rightInset = 0;
            if (rightCornerRadius > 0) {
                int distFromEdge = Math.min(row, h - 1 - row);
                if (distFromEdge < rightCornerRadius) {
                    int d = rightCornerRadius - distFromEdge;
                    rightInset = rightCornerRadius - (int) Math.round(
                            Math.sqrt((double) rightCornerRadius * rightCornerRadius - (double) d * d));
                }
            }

            context.fill(rectLeft - extend, y + row, rectRight - rightInset, y + row + 1, color);
        }
    }

    private static int blendColor(int base, int tint, float t, int alphaMask) {
        t = Math.max(0f, Math.min(1f, t));
        int r1 = (base >>> 16) & 0xFF, g1 = (base >>> 8) & 0xFF, b1 = base & 0xFF;
        int r2 = (tint >>> 16) & 0xFF, g2 = (tint >>> 8) & 0xFF, b2 = tint & 0xFF;
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (alphaMask & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}