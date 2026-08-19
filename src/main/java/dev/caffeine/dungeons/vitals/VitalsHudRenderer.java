package dev.caffeine.dungeons.vitals;

import dev.caffeine.dungeons.config.CaffeineConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class VitalsHudRenderer {

    private static final int HOTBAR_HALF_W = 91;
    private static final int HOTBAR_H = 22;
    private static final int HOTBAR_TOP_GAP = 2;
    private static final int ROW_GAP = 3;

    private static final int MAIN_BAR_W = 84;
    private static final int MAIN_BAR_H = 10;
    private static final int MAIN_RADIUS = MAIN_BAR_H / 2;

    private static final int MINI_W = 40;
    private static final int MINI_H = 9;
    private static final int MINI_RADIUS = MINI_H / 2;

    private static final int BACKING_PAD = 2;

    private static final int OUTLINE_COLOR = 0xFF0B0B10;
    private static final int BACKING_COLOR = 0x99101015;
    private static final int TRACK_TOP = 0xFF34343F;
    private static final int TRACK_BOTTOM = 0xFF1E1E26;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int HEALTH_TOP = 0xFFFF7A6E;
    private static final int HEALTH_BOTTOM = 0xFFC13A32;
    private static final int MANA_TOP = 0xFF74C0FF;
    private static final int MANA_BOTTOM = 0xFF3A7ECF;
    private static final int DEFENSE_TOP = 0xFFB9C4D6;
    private static final int DEFENSE_BOTTOM = 0xFF7A879C;
    private static final int SPEED_TOP = 0xFFCBEB6B;
    private static final int SPEED_BOTTOM = 0xFF8FB93A;

    private VitalsHudRenderer() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        CaffeineConfig config = me.shedaniel.autoconfig.AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        if (!config.vitalsHud.enabled) return;

        VitalStat health = VitalsTracker.INSTANCE.getHealth();
        VitalStat mana = VitalsTracker.INSTANCE.getMana();
        if (health == null || mana == null) return;
        if (VitalsTracker.INSTANCE.isStale(System.currentTimeMillis())) return;

        VitalStat defense = VitalsTracker.INSTANCE.get("defense");
        VitalStat speed = VitalsTracker.INSTANCE.get("speed");

        boolean panelBacking = config.vitalsHud.style == CaffeineConfig.VitalsHudStyle.PANELS;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;

        int hotbarTop = screenH - HOTBAR_H;
        int mainTop = hotbarTop - HOTBAR_TOP_GAP - MAIN_BAR_H;
        int miniTop = mainTop - ROW_GAP - MINI_H;

        int leftEdge = centerX - HOTBAR_HALF_W;
        int rightEdge = centerX + HOTBAR_HALF_W;

        TextRenderer tr = client.textRenderer;

        drawMainBar(context, tr, health, HEALTH_TOP, HEALTH_BOTTOM, leftEdge, leftEdge + MAIN_BAR_W, mainTop, panelBacking);
        drawMainBar(context, tr, mana, MANA_TOP, MANA_BOTTOM, rightEdge - MAIN_BAR_W, rightEdge, mainTop, panelBacking);

        if (defense != null) {
            drawMiniChip(context, tr, defense, DEFENSE_TOP, DEFENSE_BOTTOM, leftEdge, leftEdge + MINI_W, miniTop, panelBacking);
        }
        if (speed != null) {
            drawMiniChip(context, tr, speed, SPEED_TOP, SPEED_BOTTOM, rightEdge - MINI_W, rightEdge, miniTop, panelBacking);
        }
    }

    private static void drawMainBar(DrawContext context, TextRenderer tr, VitalStat stat, int fillTop, int fillBottom,
                                    int left, int right, int top, boolean panelBacking) {
        if (panelBacking) {
            fillCapsule(context, left - BACKING_PAD, right + BACKING_PAD, top - BACKING_PAD, MAIN_BAR_H + BACKING_PAD * 2,
                    MAIN_RADIUS + BACKING_PAD, true, true, BACKING_COLOR, BACKING_COLOR);
        }

        fillCapsuleOutlined(context, left, right, top, MAIN_BAR_H, MAIN_RADIUS, true, true, TRACK_TOP, TRACK_BOTTOM);

        float progress = Float.isNaN(stat.displayedProgress) ? stat.trueProgress() : stat.displayedProgress;
        int innerW = (right - left) - 2;
        int fillW = Math.round(innerW * progress);
        if (fillW > 0) {
            boolean roundRight = fillW >= innerW;
            fillCapsule(context, left + 1, left + 1 + fillW, top + 1, MAIN_BAR_H - 2,
                    MAIN_RADIUS - 1, true, roundRight, fillTop, fillBottom);
        }

        String text = stat.icon + " " + stat.current + "/" + stat.max;
        int textW = tr.getWidth(text);
        int textX = left + ((right - left) - textW) / 2;
        int textY = top + (MAIN_BAR_H - tr.fontHeight) / 2;
        context.drawText(tr, Text.literal(text), textX, textY, TEXT_COLOR, true);
    }

    private static void drawMiniChip(DrawContext context, TextRenderer tr, VitalStat stat, int colorTop, int colorBottom,
                                     int left, int right, int top, boolean panelBacking) {
        if (panelBacking) {
            fillCapsule(context, left - BACKING_PAD, right + BACKING_PAD, top - BACKING_PAD, MINI_H + BACKING_PAD * 2,
                    MINI_RADIUS + BACKING_PAD, true, true, BACKING_COLOR, BACKING_COLOR);
        }

        fillCapsuleOutlined(context, left, right, top, MINI_H, MINI_RADIUS, true, true, colorTop, colorBottom);

        String text = stat.icon + " " + stat.current;
        int textW = tr.getWidth(text);
        int textX = left + ((right - left) - textW) / 2;
        int textY = top + (MINI_H - tr.fontHeight) / 2;
        context.drawText(tr, Text.literal(text), textX, textY, TEXT_COLOR, true);
    }

    private static void fillCapsuleOutlined(DrawContext context, int left, int right, int top, int height, int radius,
                                            boolean roundLeft, boolean roundRight, int colorTop, int colorBottom) {
        fillCapsule(context, left, right, top, height, radius, roundLeft, roundRight, OUTLINE_COLOR, OUTLINE_COLOR);

        int innerLeft = roundLeft ? left + 1 : left;
        int innerRight = roundRight ? right - 1 : right;
        int innerTop = top + 1;
        int innerHeight = height - 2;
        if (innerRight > innerLeft && innerHeight > 0) {
            fillCapsule(context, innerLeft, innerRight, innerTop, innerHeight,
                    Math.max(0, radius - 1), roundLeft, roundRight, colorTop, colorBottom);
        }
    }

    private static void fillCapsule(DrawContext context, int left, int right, int top, int height, int radius,
                                    boolean roundLeft, boolean roundRight, int colorTop, int colorBottom) {
        radius = Math.min(radius, height / 2);
        for (int row = 0; row < height; row++) {
            int distFromEdge = Math.min(row, height - 1 - row);
            int inset = 0;
            if (distFromEdge < radius) {
                int d = radius - distFromEdge;
                inset = radius - (int) Math.round(Math.sqrt((double) radius * radius - (double) d * d));
            }
            int rowLeft = roundLeft ? left + inset : left;
            int rowRight = roundRight ? right - inset : right;
            if (rowRight > rowLeft) {
                float t = height <= 1 ? 0f : row / (float) (height - 1);
                context.fill(rowLeft, top + row, rowRight, top + row + 1, lerpColor(colorTop, colorBottom, t));
            }
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}