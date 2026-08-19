package dev.caffeine.dungeons.skillxp;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SkillXpHudRenderer {

    private static final String LABEL = "skillxp";
    private static final int BOTTOM_CLEARANCE = 90;

    private static final int BAR_W = 180;
    private static final int BAR_H = 8;
    private static final int ROW_GAP = 2;
    private static final int PANEL_PAD = 6;
    private static final int PANEL_TOTAL_W = BAR_W + PANEL_PAD * 2;
    private static final int PANEL_TOTAL_H = PANEL_PAD * 2 + 9 + ROW_GAP + BAR_H + ROW_GAP + 9;

    private static final int BG_COLOR = 0xCC15151f;
    private static final int TRACK_COLOR = 0xFF2A2A35;
    private static final int FILL_COLOR = 0xFFE0B84D;
    private static final int NAME_COLOR = 0xFFEDEDED;
    private static final int GAIN_COLOR = 0xFF7CFC8A;
    private static final int NUMBERS_COLOR = 0xFFAFAFAF;

    private static final int VBAR_W = 182;
    private static final int VBAR_H = 5;
    private static final int VROW_GAP = 2;
    private static final int TEXT_TOTAL_W = VBAR_W;
    private static final int TEXT_TOTAL_H = 9 + VROW_GAP + 9 + VROW_GAP + VBAR_H;

    private static final int VBAR_FILL = 0xFF80FF20;

    private static final Identifier XP_BAR_BACKGROUND = Identifier.of("minecraft", "hud/experience_bar_background");
    private static final Identifier XP_BAR_PROGRESS = Identifier.of("minecraft", "hud/experience_bar_progress");

    private SkillXpHudRenderer() {}

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        SkillXpEntry entry = SkillXpTracker.INSTANCE.getCurrent();

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        CaffeineConfig config = me.shedaniel.autoconfig.AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        HudPosition pos = config.skillXpHud.pos;
        boolean textStyle = config.skillXpHud.style == CaffeineConfig.SkillXpHudStyle.TEXT;

        int totalW = textStyle ? TEXT_TOTAL_W : PANEL_TOTAL_W;
        int totalH = textStyle ? TEXT_TOTAL_H : PANEL_TOTAL_H;

        int defaultX = (screenW - Math.round(totalW * pos.scale)) / 2;
        int defaultY = screenH - BOTTOM_CLEARANCE - Math.round(totalH * pos.scale);
        int baseX = pos.getX(defaultX);
        int baseY = pos.getY(defaultY);

        GuiEditManager.register(LABEL, pos, totalW, totalH);

        if (entry == null) return;

        long now = System.currentTimeMillis();
        float alpha = entry.alpha(now);
        if (alpha <= 0f) return;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(baseX, baseY);
        matrices.scale(pos.scale, pos.scale);

        if (textStyle) {
            drawVanillaStyle(context, entry, alpha, now);
        } else {
            drawPanelStyle(context, entry, alpha, now);
        }

        matrices.popMatrix();
    }

    private static void drawVanillaStyle(DrawContext context, SkillXpEntry entry, float alpha, long now) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int a = (int) (alpha * 255) << 24;

        String name = entry.skillName;
        String gainText = "+" + format(entry.lastGain);
        String numbersText = format(entry.current) + " / " + format(entry.max);

        int nameGainY = 0;
        context.drawText(tr, Text.literal(name), 0, nameGainY, (NAME_COLOR & 0x00FFFFFF) | a, true);
        int gainW = tr.getWidth(gainText);
        context.drawText(tr, Text.literal(gainText), VBAR_W - gainW, nameGainY, (GAIN_COLOR & 0x00FFFFFF) | a, true);

        int numY = nameGainY + 9 + VROW_GAP;
        int numW = tr.getWidth(numbersText);
        context.drawText(tr, Text.literal(numbersText), (VBAR_W - numW) / 2, numY, (VBAR_FILL & 0x00FFFFFF) | a, true);

        int barY = numY + 9 + VROW_GAP;
        int tint = a | 0x00FFFFFF;

        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_BAR_BACKGROUND, 0, barY, VBAR_W, VBAR_H, tint);

        int fillW = Math.round(VBAR_W * entry.displayedProgress);
        if (fillW > 0) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, XP_BAR_PROGRESS,
                    VBAR_W, VBAR_H, 0, 0, 0, barY, fillW, VBAR_H, tint);

            float pulse = entry.pulse(now);
            if (pulse > 0f) {
                int pulseA = Math.round(pulse * 0.45f * alpha * 255) & 0xFF;
                context.fill(0, barY, fillW, barY + VBAR_H, (pulseA << 24) | 0xFFFFFF);
            }
        }
    }

    private static void drawPanelStyle(DrawContext context, SkillXpEntry entry, float alpha, long now) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int a = (int) (alpha * 255) << 24;

        String name = entry.skillName;
        String gainText = "+" + format(entry.lastGain);
        String numbersText = format(entry.current) + " / " + format(entry.max);

        context.fill(0, 0, PANEL_TOTAL_W, PANEL_TOTAL_H, (BG_COLOR & 0x00FFFFFF) | a);

        int nameY = PANEL_PAD;
        context.drawText(tr, Text.literal(name), PANEL_PAD, nameY, (NAME_COLOR & 0x00FFFFFF) | a, false);
        int gainW = tr.getWidth(gainText);
        context.drawText(tr, Text.literal(gainText), PANEL_TOTAL_W - PANEL_PAD - gainW, nameY, (GAIN_COLOR & 0x00FFFFFF) | a, false);

        int barX = PANEL_PAD;
        int barY = nameY + 9 + ROW_GAP;
        context.fill(barX, barY, barX + BAR_W, barY + BAR_H, (TRACK_COLOR & 0x00FFFFFF) | a);

        float pulse = entry.pulse(now);
        int fillRgb = pulse > 0f ? blend(FILL_COLOR, 0xFFFFFF, pulse * 0.4f) : (FILL_COLOR & 0x00FFFFFF);
        int fillColor = (fillRgb & 0x00FFFFFF) | a;
        int fillW = Math.round(BAR_W * entry.displayedProgress);
        if (fillW > 0) {
            context.fill(barX, barY, barX + fillW, barY + BAR_H, fillColor);
        }

        int numY = barY + BAR_H + ROW_GAP;
        int numW = tr.getWidth(numbersText);
        context.drawText(tr, Text.literal(numbersText), (PANEL_TOTAL_W - numW) / 2, numY, (NUMBERS_COLOR & 0x00FFFFFF) | a, false);
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }

    private static int blend(int base, int tint, float t) {
        int r1 = (base >>> 16) & 0xFF, g1 = (base >>> 8) & 0xFF, b1 = base & 0xFF;
        int r2 = (tint >>> 16) & 0xFF, g2 = (tint >>> 8) & 0xFF, b2 = tint & 0xFF;
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}