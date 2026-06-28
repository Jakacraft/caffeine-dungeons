package dev.caffeine.dungeons.buff;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public class BuffHudRenderer {

    // Layout
    public static final int PANEL_W = 164;
    private static final int PADDING  = 4;
    private static final int MARGIN   = 4;
    private static final int ACCENT_W = 3;
    private static final int LINE_H   = 11;
    private static final int BAR_H    = 3;
    private static final int TIMED_H  = LINE_H + 2 + BAR_H;
    private static final int SEP_H    = 7;

    // Colors
    private static final int PANEL_BG  = 0xC0101010;
    private static final int SEP_COLOR = 0x50FFFFFF;
    private static final int BAR_BG    = 0x50000000;
    private static final int TIMER_COL = 0xFFAAAAAA;

    public static void render(DrawContext context) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        BuffTracker tracker   = BuffTracker.getInstance();
        MinecraftClient mc    = MinecraftClient.getInstance();
        HudPosition pos       = config.buffHudPos;

        // Resolve default position (top-right) on first render
        int defaultX = mc.getWindow().getScaledWidth() - PANEL_W - MARGIN;
        pos.getX(defaultX);
        pos.getY(MARGIN);

        // Always register with a minimum height so the editor can always find this HUD
        GuiEditManager.register("Buff HUD", pos, PANEL_W, 20);

        BuffEntry       daily     = tracker.getDailyEvent();
        BuffEntry       tempEvent = tracker.getTempEvent();
        List<BuffEntry> boosters  = tracker.getBoosters();
        List<BuffEntry> tempBuffs = tracker.getTempBuffs();

        if (daily == null && tempEvent == null && boosters.isEmpty() && tempBuffs.isEmpty()) return;

        boolean hasTop = daily != null || tempEvent != null;
        boolean hasMid = !boosters.isEmpty();
        boolean hasBot = !tempBuffs.isEmpty();

        int contentH = computeHeight(daily, tempEvent, boosters, tempBuffs, hasTop, hasMid, hasBot);

        // Update editor registration with actual content height
        GuiEditManager.register("Buff HUD", pos, PANEL_W, contentH);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(pos.x, pos.y);
        matrices.scale(pos.scale, pos.scale);

        if (config.buffHudStyle == CaffeineConfig.BuffHudStyle.TEXT) {
            renderText(context, daily, tempEvent, boosters, tempBuffs);
        } else {
            renderPanel(context, daily, tempEvent, boosters, tempBuffs,
                    contentH, hasTop, hasMid, hasBot);
        }

        matrices.popMatrix();
    }

    // -------------------------------------------------------------------------
    // PANEL style  (all coordinates relative to matrix origin = pos.x, pos.y)
    // -------------------------------------------------------------------------

    private static void renderPanel(DrawContext ctx,
                                    BuffEntry daily, BuffEntry tempEvent,
                                    List<BuffEntry> boosters, List<BuffEntry> tempBuffs,
                                    int panelH, boolean hasTop, boolean hasMid, boolean hasBot) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        ctx.fill(0, 0, PANEL_W, panelH, PANEL_BG);

        int cursor = PADDING;

        if (daily     != null) cursor = drawPermanent(ctx, tr, daily,     cursor);
        if (tempEvent != null) cursor = drawPermanent(ctx, tr, tempEvent,  cursor);

        if (hasTop && (hasMid || hasBot)) { drawSep(ctx, cursor); cursor += SEP_H; }

        for (BuffEntry b : boosters)  cursor = drawPermanent(ctx, tr, b, cursor);

        if (hasMid && hasBot) { drawSep(ctx, cursor); cursor += SEP_H; }

        for (BuffEntry b : tempBuffs) cursor = drawTimed(ctx, tr, b, cursor);
    }

    private static int computeHeight(BuffEntry daily, BuffEntry tempEvent,
                                     List<BuffEntry> boosters, List<BuffEntry> tempBuffs,
                                     boolean hasTop, boolean hasMid, boolean hasBot) {
        int h = PADDING * 2;
        if (daily     != null) h += LINE_H;
        if (tempEvent != null) h += LINE_H;
        h += boosters.size() * LINE_H;
        h += tempBuffs.size() * TIMED_H;
        if (hasTop && (hasMid || hasBot)) h += SEP_H;
        if (hasMid && hasBot)             h += SEP_H;
        return h;
    }

    private static int drawPermanent(DrawContext ctx, TextRenderer tr, BuffEntry entry, int y) {
        int color = parseHex(entry.colorHex());
        String label = truncate(tr, entry.label(), PANEL_W - ACCENT_W - PADDING * 2);
        ctx.drawTextWithShadow(tr, label, ACCENT_W + PADDING, y + 1, color);
        return y + LINE_H;
    }

    private static int drawTimed(DrawContext ctx, TextRenderer tr, BuffEntry entry, int y) {
        int color    = parseHex(entry.colorHex());
        String timer = entry.timerText();
        int timerW   = tr.getWidth(timer);

        ctx.fill(0, y, ACCENT_W, y + TIMED_H - 2, color);

        int labelMaxW = PANEL_W - ACCENT_W - PADDING * 2 - timerW - 4;
        String label  = truncate(tr, entry.label(), labelMaxW);
        ctx.drawTextWithShadow(tr, label, ACCENT_W + PADDING, y + 1, color);
        ctx.drawTextWithShadow(tr, timer, PANEL_W - PADDING - timerW, y + 1, TIMER_COL);

        int barX = ACCENT_W + PADDING;
        int barY = y + LINE_H + 2;
        int barW = PANEL_W - ACCENT_W - PADDING * 2;
        int filled = Math.max(0, (int)(barW * entry.progress()));
        ctx.fill(barX, barY, barX + barW, barY + BAR_H, BAR_BG);
        ctx.fill(barX, barY, barX + filled, barY + BAR_H, color);

        return y + TIMED_H;
    }

    private static void drawSep(DrawContext ctx, int y) {
        ctx.fill(PADDING, y + 3, PANEL_W - PADDING, y + 4, SEP_COLOR);
    }

    // -------------------------------------------------------------------------
    // TEXT style  (relative to matrix origin)
    // -------------------------------------------------------------------------

    private static void renderText(DrawContext ctx,
                                   BuffEntry daily, BuffEntry tempEvent,
                                   List<BuffEntry> boosters, List<BuffEntry> tempBuffs) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int cursor = 0;
        if (daily     != null) cursor = drawTextLine(ctx, tr, cursor, daily,    null);
        if (tempEvent != null) cursor = drawTextLine(ctx, tr, cursor, tempEvent, null);
        for (BuffEntry b : boosters)  cursor = drawTextLine(ctx, tr, cursor, b, null);
        for (BuffEntry b : tempBuffs) cursor = drawTextLine(ctx, tr, cursor, b, b.timerText());
    }

    private static int drawTextLine(DrawContext ctx, TextRenderer tr,
                                    int y, BuffEntry entry, String suffix) {
        int color   = parseHex(entry.colorHex());
        String text = suffix != null ? entry.label() + " §7" + suffix : entry.label();
        ctx.drawTextWithShadow(tr, text, 0, y, color);
        return y + LINE_H;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int parseHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseUnsignedInt(h, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    private static String truncate(TextRenderer tr, String text, int maxW) {
        if (tr.getWidth(text) <= maxW) return text;
        while (!text.isEmpty() && tr.getWidth(text + "…") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }
}