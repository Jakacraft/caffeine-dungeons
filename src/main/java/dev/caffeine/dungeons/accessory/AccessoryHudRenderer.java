package dev.caffeine.dungeons.accessory;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.hud.HudPosition;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class AccessoryHudRenderer {

    // Layout
    private static final int MIN_PANEL_W = 100;
    private static final int PADDING  = 5;
    private static final int HEADER_H = 14;
    private static final int SEP_H    = 9;
    private static final int RARITY_H = 12;
    private static final int ITEM_H   = 11;
    private static final int MARGIN   = 4;
    private static final int ICON_W   = 9; // reserved width for the X icon on each row

    // Colors
    private static final int PANEL_BG  = 0xC0101010;
    private static final int SEP_COLOR = 0x50FFFFFF;
    private static final int FLOOR_COL = 0xFF888888;
    private static final int ICON_COL  = 0xFFFF5555;
    private static final int BTN_COL   = 0xFFAAAAAA;

    // Scroll — pixels offset from top of content area
    private static int scrollOffset = 0;

    // Last rendered screen-space bounds for mouse-over detection
    private static int lastX, lastY, lastW, lastH;

    // Click targets, rebuilt every render() call in panel-local (unscaled) coords
    private record ClickArea(int x, int y, int w, int h, Runnable action) {}
    private static final List<ClickArea> clickAreas = new ArrayList<>();

    public static void resetScroll() { scrollOffset = 0; }

    public static void scroll(int deltaPx) {
        scrollOffset = Math.max(0, scrollOffset + deltaPx);
    }

    public static boolean isMouseOver(HudPosition pos, int mx, int my) {
        return mx >= lastX && mx <= lastX + lastW
                && my >= lastY && my <= lastY + lastH;
    }

    /**
     * Converts screen-space mouse coords into the panel's local coordinate
     * space and checks them against registered click areas (X icons, Unhide
     * All button). Returns true if a click was consumed.
     */
    public static boolean handleClick(double screenMouseX, double screenMouseY) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        HudPosition pos = config.accessoryHudPos;
        if (pos.scale <= 0) return false;

        double localX = (screenMouseX - pos.x) / pos.scale;
        double localY = (screenMouseY - pos.y) / pos.scale;

        for (ClickArea area : clickAreas) {
            if (localX >= area.x() && localX <= area.x() + area.w()
                    && localY >= area.y() && localY <= area.y() + area.h()) {
                area.action().run();
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------

    public static void render(DrawContext context) {
        clickAreas.clear();

        if (!AccessoryTracker.getInstance().isInBagSession()) return;

        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        MinecraftClient mc    = MinecraftClient.getInstance();
        TextRenderer tr       = mc.textRenderer;
        HudPosition pos       = config.accessoryHudPos;
        int screenH           = mc.getWindow().getScaledHeight();

        pos.getX(MARGIN);
        pos.getY(MARGIN);

        List<AccessoryEntry> missing = AccessoryTracker.getInstance().getMissing();
        int hiddenCount = AccessoryTracker.getInstance().hiddenCount();

        // Always register with editor
        GuiEditManager.register("Missing Accessories", pos, MIN_PANEL_W, 60);

        if (config.accessoryHudStyle == CaffeineConfig.AccessoryHudStyle.TEXT) {
            var matrices = context.getMatrices();
            matrices.pushMatrix();
            matrices.translate(pos.x, pos.y);
            matrices.scale(pos.scale, pos.scale);
            renderText(context, tr, missing);
            matrices.popMatrix();
            return;
        }

        // === PANEL style ===
        int total = AccessoryDatabase.getInstance().getMasterList().size();
        String header = missing.isEmpty()
                ? "§a✓ All Accessories Found!"
                : "§f✗ §cMissing " + missing.size() + " §7/ §f" + total;

        String unhideBtn = hiddenCount > 0 ? "§7[Unhide All (" + hiddenCount + ")]" : null;
        int unhideBtnW   = unhideBtn != null ? tr.getWidth(unhideBtn) : 0;

        List<RenderLine> lines = buildLines(missing);
        int contentW = computeContentWidth(tr, lines, header);
        int headerRowW = tr.getWidth(header) + (unhideBtn != null ? unhideBtnW + 8 : 0);
        int panelW = Math.max(MIN_PANEL_W, Math.max(contentW, headerRowW + PADDING * 2));

        // Geometry
        int contentTopOff = PADDING + HEADER_H + SEP_H;
        int maxPanelH      = screenH - pos.y - MARGIN;
        int maxContentH    = maxPanelH - contentTopOff - PADDING;

        int totalH = lines.stream().mapToInt(AccessoryHudRenderer::lineH).sum();

        // Clamp scroll
        int maxScroll = Math.max(0, totalH - maxContentH);
        scrollOffset  = Math.min(scrollOffset, maxScroll);

        boolean canScrollUp   = scrollOffset > 0;
        boolean canScrollDown = scrollOffset < maxScroll;

        int visibleContentH = Math.min(totalH, maxContentH);
        int panelH          = contentTopOff + visibleContentH + PADDING;

        // Update editor registration with real dimensions
        GuiEditManager.register("Missing Accessories", pos, panelW, panelH);

        // Store screen-space bounds for mouse-over scroll detection
        lastX = pos.x;
        lastY = pos.y;
        lastW = (int)(panelW * pos.scale);
        lastH = (int)(panelH * pos.scale);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(pos.x, pos.y);
        matrices.scale(pos.scale, pos.scale);

        // Background
        context.fill(0, 0, panelW, panelH, PANEL_BG);

        // Header
        context.drawTextWithShadow(tr, header, PADDING, PADDING, 0xFFFFFFFF);

        // Unhide All button, right-aligned in header row
        if (unhideBtn != null) {
            int btnX = panelW - PADDING - unhideBtnW;
            int btnY = PADDING;
            context.drawTextWithShadow(tr, unhideBtn, btnX, btnY, BTN_COL);
            clickAreas.add(new ClickArea(btnX, btnY, unhideBtnW, 9, () ->
                    AccessoryTracker.getInstance().unhideAll()));
        }

        // Separator
        int sepY = PADDING + HEADER_H;
        context.fill(PADDING, sepY + 2, panelW - PADDING, sepY + 4, SEP_COLOR);

        // Scroll edge indicators
        if (canScrollUp)
            context.fill(0, contentTopOff, panelW, contentTopOff + 2, 0x60FFFFFF);
        if (canScrollDown)
            context.fill(0, panelH - PADDING - 1, panelW, panelH - PADDING, 0x60FFFFFF);

        context.enableScissor(
                pos.x,
                pos.y + (int)((contentTopOff - 3) * pos.scale),
                pos.x + (int)(panelW * pos.scale),
                pos.y + (int)((panelH - PADDING) * pos.scale)
        );

        // Render lines with scroll offset
        int lineY = contentTopOff - scrollOffset;
        for (RenderLine line : lines) {
            int h = lineH(line);
            if (lineY + h > contentTopOff && lineY < contentTopOff + maxContentH) {
                drawRenderLine(context, tr, line, lineY, panelW);
            }
            lineY += h;
            if (lineY > contentTopOff + maxContentH + 20) break;
        }

        context.disableScissor();
        matrices.popMatrix();
    }

    // -------------------------------------------------------------------------
    // TEXT style
    // -------------------------------------------------------------------------

    private static void renderText(DrawContext ctx, TextRenderer tr,
                                   List<AccessoryEntry> missing) {
        int y = 0;
        AccessoryRarity lastRarity = null;
        int floorW = tr.getWidth("F1 ");
        for (AccessoryEntry entry : missing) {
            if (entry.rarity() != lastRarity) {
                if (lastRarity != null) y += 3;
                ctx.drawTextWithShadow(tr,
                        "§l" + entry.rarity().displayName.toUpperCase(),
                        0, y, entry.rarity().argb());
                y += RARITY_H;
                lastRarity = entry.rarity();
            }
            String floorStr = entry.floor() != null ? "F" + entry.floor() + " " : "   ";
            ctx.drawTextWithShadow(tr, floorStr, 0, y, FLOOR_COL);
            ctx.drawTextWithShadow(tr, entry.name(), floorW, y, entry.rarity().argb());
            y += ITEM_H;
        }
    }

    // -------------------------------------------------------------------------
    // Width computation
    // -------------------------------------------------------------------------

    private static int computeContentWidth(TextRenderer tr, List<RenderLine> lines, String header) {
        int maxW = tr.getWidth(header);
        for (RenderLine line : lines) {
            switch (line.type()) {
                case RARITY_HEADER -> {
                    String boldName = "§l" + line.rarity().displayName.toUpperCase();
                    maxW = Math.max(maxW, tr.getWidth(boldName) + tr.getWidth(" (" + line.count() + ")"));
                }
                case ITEM -> {
                    String floorStr = line.entry().floor() != null
                            ? "F" + line.entry().floor() + " " : "   ";
                    maxW = Math.max(maxW, tr.getWidth(floorStr) + tr.getWidth(line.entry().name()) + ICON_W + 4);
                }
                case SEP -> {}
            }
        }
        return maxW + PADDING * 2;
    }

    // -------------------------------------------------------------------------
    // Line rendering
    // -------------------------------------------------------------------------

    private static void drawRenderLine(DrawContext ctx, TextRenderer tr,
                                       RenderLine line, int y, int panelW) {
        switch (line.type()) {
            case SEP -> ctx.fill(PADDING, y + 2, panelW - PADDING, y + 3, SEP_COLOR);

            case RARITY_HEADER -> {
                AccessoryRarity r = line.rarity();
                String boldName   = "§l" + r.displayName.toUpperCase();
                int nameW         = tr.getWidth(boldName);
                ctx.drawTextWithShadow(tr, boldName,
                        PADDING, y + 1, r.argb());
                ctx.drawTextWithShadow(tr, " (" + line.count() + ")",
                        PADDING + nameW, y + 1, 0xFF888888);
            }

            case ITEM -> {
                AccessoryEntry entry = line.entry();
                int floorW     = tr.getWidth("F1 ");
                String floorStr = entry.floor() != null
                        ? "F" + entry.floor() + " " : "   ";
                String name    = truncate(tr, entry.name(),
                        panelW - PADDING * 2 - floorW - ICON_W - 4);
                ctx.drawTextWithShadow(tr, floorStr,
                        PADDING, y + 1, FLOOR_COL);
                ctx.drawTextWithShadow(tr, name,
                        PADDING + floorW, y + 1, entry.rarity().argb());

                // X icon, right-aligned, hides this item for the session on click
                int iconX = panelW - PADDING - ICON_W;
                ctx.drawTextWithShadow(tr, "x", iconX, y + 1, ICON_COL);
                clickAreas.add(new ClickArea(iconX, y, ICON_W, ITEM_H, () ->
                        AccessoryTracker.getInstance().hide(entry.name())));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Line building
    // -------------------------------------------------------------------------

    private static List<RenderLine> buildLines(List<AccessoryEntry> missing) {
        List<RenderLine> lines   = new ArrayList<>();
        AccessoryRarity lastRarity = null;
        List<AccessoryEntry> buf = new ArrayList<>();

        for (AccessoryEntry entry : missing) {
            if (lastRarity != null && entry.rarity() != lastRarity) {
                flush(lines, lastRarity, buf);
                buf.clear();
            }
            lastRarity = entry.rarity();
            buf.add(entry);
        }
        if (lastRarity != null && !buf.isEmpty()) flush(lines, lastRarity, buf);
        return lines;
    }

    private static void flush(List<RenderLine> lines,
                              AccessoryRarity rarity, List<AccessoryEntry> buf) {
        if (!lines.isEmpty()) lines.add(RenderLine.sep());
        lines.add(RenderLine.rarityHeader(rarity, buf.size()));
        buf.forEach(e -> lines.add(RenderLine.item(e)));
    }

    private static int lineH(RenderLine line) {
        return switch (line.type()) {
            case SEP           -> SEP_H;
            case RARITY_HEADER -> RARITY_H;
            case ITEM          -> ITEM_H;
        };
    }

    private static String truncate(TextRenderer tr, String text, int maxW) {
        if (tr.getWidth(text) <= maxW) return text;
        while (!text.isEmpty() && tr.getWidth(text + "…") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    // -------------------------------------------------------------------------
    // RenderLine
    // -------------------------------------------------------------------------

    enum LineType { SEP, RARITY_HEADER, ITEM }

    record RenderLine(LineType type, AccessoryRarity rarity, int count, AccessoryEntry entry) {
        static RenderLine sep() {
            return new RenderLine(LineType.SEP, null, 0, null);
        }
        static RenderLine rarityHeader(AccessoryRarity r, int count) {
            return new RenderLine(LineType.RARITY_HEADER, r, count, null);
        }
        static RenderLine item(AccessoryEntry e) {
            return new RenderLine(LineType.ITEM, null, 0, e);
        }
    }
}