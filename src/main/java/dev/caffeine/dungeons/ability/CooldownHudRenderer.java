package dev.caffeine.dungeons.ability;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public class CooldownHudRenderer {

    private static final int PANEL_W   = 130;
    private static final int ENTRY_H   = 30;
    private static final int ENTRY_GAP = 3;
    private static final int MARGIN    = 5;
    private static final int PAD       = 5;
    private static final int ACCENT_W  = 2;
    private static final int BAR_H     = 3;
    private static final long SLIDE_MS = 180;

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();

        // Resolve HUD position — defaults to top-left
        HudPosition pos = config.cooldownHudPos;
        pos.getX(MARGIN);
        pos.getY(MARGIN);

        List<CooldownEntry> entries = CooldownTracker.INSTANCE.getEntries();

        // Always register so the editor can find this HUD even when empty
        int totalH = entries.isEmpty() ? 20
                : entries.size() * (ENTRY_H + ENTRY_GAP) - ENTRY_GAP + MARGIN * 2;
        GuiEditManager.register("Ability Cooldowns", pos, PANEL_W + MARGIN * 2, totalH);

        if (entries.isEmpty()) return;

        TextRenderer tr = client.textRenderer;
        long now = System.currentTimeMillis();

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(pos.x, pos.y);
        matrices.scale(pos.scale, pos.scale);

        if (config.cooldownHudStyle == CaffeineConfig.CooldownHudStyle.TEXT) {
            renderText(context, entries, tr);
        } else {
            renderPanels(context, entries, tr, now);
        }

        matrices.popMatrix();
    }

    private static void renderPanels(DrawContext context, List<CooldownEntry> entries,
                                     TextRenderer tr, long now) {
        for (int i = 0; i < entries.size(); i++) {
            CooldownEntry entry = entries.get(i);
            float alpha = entry.getFadeAlpha();
            if (alpha <= 0f) continue;

            // Slide-in from left
            float slideProgress = Math.min(1f, (now - entry.getStartTime()) / (float) SLIDE_MS);
            float easedSlide = 1f - (1f - slideProgress) * (1f - slideProgress);
            int xOff = (int) ((1f - easedSlide) * -(PANEL_W + MARGIN + 4));

            // Coordinates are relative to matrix origin (pos.x, pos.y)
            int px = MARGIN + xOff;
            int py = MARGIN + i * (ENTRY_H + ENTRY_GAP);

            int a         = (int)(alpha * 255) & 0xFF;
            int abilColor = entry.color & 0x00FFFFFF;

            // Background panel
            int bgA = (int)(alpha * 0xA0) & 0xFF;
            context.fill(px, py, px + PANEL_W, py + ENTRY_H, (bgA << 24) | 0x000000);

            // Left accent strip
            context.fill(px, py, px + ACCENT_W, py + ENTRY_H, (a << 24) | abilColor);

            // Ability name
            int textX = px + ACCENT_W + PAD;
            int textY = py + PAD;
            context.drawTextWithShadow(tr,
                    Text.literal(entry.abilityName),
                    textX, textY,
                    (a << 24) | 0xFFFFFF);

            // Timer — right-aligned, in ability color
            float remaining = entry.getRemainingSeconds();
            String timerStr = remaining > 0.05f
                    ? String.format("%.1fs", remaining)
                    : "\u2713";
            int timerColor = remaining > 0.05f
                    ? (a << 24) | abilColor
                    : (a << 24) | 0x55FF55;
            int timerW = tr.getWidth(timerStr);
            context.drawTextWithShadow(tr,
                    Text.literal(timerStr),
                    px + PANEL_W - timerW - PAD, textY,
                    timerColor);

            // Progress bar track
            int barX = textX;
            int barY = py + ENTRY_H - BAR_H - PAD + 2;
            int barW = PANEL_W - (ACCENT_W + PAD) - PAD;
            int trackA = (int)(alpha * 0x35) & 0xFF;
            context.fill(barX, barY, barX + barW, barY + BAR_H, (trackA << 24) | 0xFFFFFF);

            // Progress bar fill — pulses on activation
            int fillW = (int)(entry.getProgress() * barW);
            if (fillW > 0) {
                long age = now - entry.getStartTime();
                float pulse = age < 300
                        ? 0.5f + 0.5f * (float) Math.cos(age / 300.0 * Math.PI)
                        : 0f;
                int pulseAdd = (int)(pulse * 80);
                int fr = Math.min(255, ((abilColor >> 16) & 0xFF) + pulseAdd);
                int fg = Math.min(255, ((abilColor >> 8)  & 0xFF) + pulseAdd);
                int fb = Math.min(255, ((abilColor)       & 0xFF) + pulseAdd);
                context.fill(barX, barY, barX + fillW, barY + BAR_H,
                        (a << 24) | (fr << 16) | (fg << 8) | fb);
            }
        }
    }

    private static void renderText(DrawContext context, List<CooldownEntry> entries,
                                   TextRenderer tr) {
        int y = MARGIN;
        for (CooldownEntry entry : entries) {
            float alpha = entry.getFadeAlpha();
            if (alpha <= 0f) continue;
            int a = (int)(alpha * 255) & 0xFF;
            float remaining = entry.getRemainingSeconds();
            String line = remaining > 0.05f
                    ? String.format("%s \u00a77%.1fs", entry.abilityName, remaining)
                    : entry.abilityName + " \u00a7a\u2713";
            context.drawTextWithShadow(tr,
                    Text.literal(line),
                    MARGIN, y,
                    (a << 24) | (entry.color & 0x00FFFFFF));
            y += tr.fontHeight + 2;
        }
    }
}