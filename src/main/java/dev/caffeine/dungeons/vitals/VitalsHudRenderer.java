package dev.caffeine.dungeons.vitals;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class VitalsHudRenderer {

    private static final float MINI_TEXT_SCALE = 0.75f;

    private static final int HOTBAR_HALF_W = 91;
    private static final int HOTBAR_H = 22;
    private static final int HOTBAR_TOP_GAP = 7;
    private static final int ROW_GAP = 4;

    private static final int MAIN_BAR_W = 86;
    private static final int MAIN_BAR_H = 11;
    private static final int MAIN_RADIUS = MAIN_BAR_H / 2;

    private static final int MINI_W = 46;
    private static final int MINI_H = 10;
    private static final int MINI_RADIUS = MINI_H / 2;

    private static final int OUTLINE_COLOR = 0xFF0B0B10;
    private static final int TRACK_TOP = 0xFF34343F;
    private static final int TRACK_BOTTOM = 0xFF1E1E26;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int HEALTH_TOP = 0xFFFF8577;
    private static final int HEALTH_BOTTOM = 0xFFC7392F;
    private static final int MANA_TOP = 0xFF6FC3FF;
    private static final int MANA_BOTTOM = 0xFF2E76C2;

    private VitalsHudRenderer() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null && !(client.currentScreen instanceof GameMenuScreen)) return;

        VitalStat health = VitalsTracker.INSTANCE.getHealth();
        VitalStat mana = VitalsTracker.INSTANCE.getMana();
        if (health == null || mana == null) return;
        if (VitalsTracker.INSTANCE.isStale(System.currentTimeMillis())) return;

        VitalStat defense = VitalsTracker.INSTANCE.get("defense");
        VitalStat speed = VitalsTracker.INSTANCE.get("speed");

        CaffeineConfig config = me.shedaniel.autoconfig.AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int centerX = screenW / 2;

        int hotbarTop = screenH - HOTBAR_H;
        int mainTop = hotbarTop - HOTBAR_TOP_GAP - MAIN_BAR_H;
        int miniTop = mainTop - ROW_GAP - MINI_H;

        int leftEdge = centerX - HOTBAR_HALF_W;
        int rightEdge = centerX + HOTBAR_HALF_W;

        TextRenderer tr = client.textRenderer;

        if (config.vitalsHud.health.enabled) {
            renderMainBar(context, tr, "health", config.vitalsHud.health.pos, health,
                    HEALTH_TOP, HEALTH_BOTTOM, leftEdge, mainTop);
        }
        if (config.vitalsHud.mana.enabled) {
            renderMainBar(context, tr, "mana", config.vitalsHud.mana.pos, mana,
                    MANA_TOP, MANA_BOTTOM, rightEdge - MAIN_BAR_W, mainTop);
        }
        if (defense != null && config.vitalsHud.defense.enabled) {
            renderMiniChip(context, tr, "defense", config.vitalsHud.defense.pos, defense, "DEF",
                    leftEdge, miniTop);
        }
        if (speed != null && config.vitalsHud.speed.enabled) {
            renderMiniChip(context, tr, "speed", config.vitalsHud.speed.pos, speed, "SPD",
                    rightEdge - MINI_W, miniTop);
        }
    }

    private static void renderMainBar(DrawContext context, TextRenderer tr, String label, HudPosition pos,
                                      VitalStat stat, int fillTop, int fillBottom, int defaultLeft, int defaultTop) {
        int baseX = pos.getX(defaultLeft);
        int baseY = pos.getY(defaultTop);
        GuiEditManager.register(label, pos, MAIN_BAR_W, MAIN_BAR_H);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(pos.scale, pos.scale);

        fillCapsuleOutlined(context, 0, MAIN_BAR_W, 0, MAIN_BAR_H, MAIN_RADIUS, true, true, TRACK_TOP, TRACK_BOTTOM);

        float progress = Float.isNaN(stat.displayedProgress) ? stat.trueProgress() : stat.displayedProgress;
        int innerW = MAIN_BAR_W - 2;
        int fillW = Math.round(innerW * progress);
        if (fillW > 0) {
            boolean roundRight = fillW >= innerW;
            fillCapsule(context, 1, 1 + fillW, 1, MAIN_BAR_H - 2, MAIN_RADIUS - 1,
                    true, roundRight, fillTop, fillBottom, true);
        }

        String text = VitalStat.format(stat.current) + "/" + VitalStat.format(stat.max);
        int textW = tr.getWidth(text);
        int textX = (MAIN_BAR_W - textW) / 2;
        int textY = (MAIN_BAR_H - tr.fontHeight) / 2 + 1;
        context.drawText(tr, Text.literal(text), textX, textY, TEXT_COLOR, true);

        matrices.popMatrix();
    }

    private static void renderMiniChip(DrawContext context, TextRenderer tr, String label, HudPosition pos,
                                       VitalStat stat, String abbrev, int defaultLeft, int defaultTop) {
        int baseX = pos.getX(defaultLeft);
        int baseY = pos.getY(defaultTop);
        GuiEditManager.register(label, pos, MINI_W, MINI_H);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(pos.scale, pos.scale);

        fillCapsuleOutlined(context, 0, MINI_W, 0, MINI_H, MINI_RADIUS, true, true, TRACK_TOP, TRACK_BOTTOM);

        String text = abbrev + " " + VitalStat.format(stat.current);
        int textW = tr.getWidth(text);
        int scaledTextW = Math.round(textW * MINI_TEXT_SCALE);
        int textX = (MINI_W - scaledTextW) / 2;
        int scaledTextH = Math.round(tr.fontHeight * MINI_TEXT_SCALE);
        int textY = (MINI_H - scaledTextH) / 2 + 1;

        matrices.pushMatrix();
        matrices.translate(textX, textY);
        matrices.scale(MINI_TEXT_SCALE, MINI_TEXT_SCALE);
        context.drawText(tr, Text.literal(text), 0, 0, TEXT_COLOR, true);
        matrices.popMatrix();

        matrices.popMatrix();
    }

    private static void fillCapsuleOutlined(DrawContext context, int left, int right, int top, int height, int radius,
                                            boolean roundLeft, boolean roundRight, int colorTop, int colorBottom) {
        fillCapsule(context, left, right, top, height, radius, roundLeft, roundRight, OUTLINE_COLOR, OUTLINE_COLOR, false);

        int innerLeft = roundLeft ? left + 1 : left;
        int innerRight = roundRight ? right - 1 : right;
        int innerTop = top + 1;
        int innerHeight = height - 2;
        if (innerRight > innerLeft && innerHeight > 0) {
            fillCapsule(context, innerLeft, innerRight, innerTop, innerHeight,
                    Math.max(0, radius - 1), roundLeft, roundRight, colorTop, colorBottom, false);
        }
    }

    private static void fillCapsule(DrawContext context, int left, int right, int top, int height, int radius,
                                    boolean roundLeft, boolean roundRight, int colorTop, int colorBottom, boolean glossy) {
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
                int color = lerpColor(colorTop, colorBottom, t);
                if (glossy && row == 0) {
                    color = lerpColor(color, 0xFFFFFFFF, 0.3f);
                }
                context.fill(rowLeft, top + row, rowRight, top + row + 1, color);
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