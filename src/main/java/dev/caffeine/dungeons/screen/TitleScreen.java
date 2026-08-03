package dev.caffeine.dungeons.screen;

import dev.caffeine.dungeons.Backend.BackendService;
import dev.caffeine.dungeons.title.ChromaUtil;
import dev.caffeine.dungeons.title.TitleEntry;
import dev.caffeine.dungeons.title.TitleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TitleScreen extends Screen {

    private static final int PANEL_BG  = 0xF0111111;
    private static final int HEADER_BG = 0xFF1A0F07;
    private static final int ACCENT    = 0xFFD4944A;
    private static final int BORDER    = 0x50D4944A;
    private static final int ROW_HOVER = 0x18FFFFFF;
    private static final int ROW_ALT   = 0x08FFFFFF;
    private static final int SEPARATOR = 0x28FFFFFF;
    private static final int TEXT_DIM  = 0xFF777777;
    private static final int ACCENT_ARGB = 0xFF000000 | ACCENT;

    private static final int HEADER_H    = 32;
    private static final int ROW_H       = 22;
    private static final int PAD         = 10;
    private static final int MIN_PANEL_W = 160;

    // Row 0 is always "None"; rows after that map 1:1 to `granted`
    private final List<TitleEntry> granted = new ArrayList<>();
    private int scrollOffset = 0;

    public TitleScreen() {
        super(Text.literal("Titles"));
    }

    private UUID ownUuid() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null ? mc.player.getUuid() : null;
    }

    @Override
    protected void init() {
        granted.clear();
        UUID uuid = ownUuid();
        if (uuid != null) granted.addAll(TitleRegistry.getInstance().getGranted(uuid));
    }

    private int rowCount() { return granted.size() + 1; } // +1 for "None"

    private int panelW() {
        int maxTextW = textRenderer.getWidth("None");
        for (TitleEntry entry : granted) {
            maxTextW = Math.max(maxTextW, textRenderer.getWidth(entry.titleText()));
        }
        String header = "\u2615  Titles";
        maxTextW = Math.max(maxTextW, textRenderer.getWidth(header) + textRenderer.getWidth("99 unlocked") + 20);
        return Math.max(MIN_PANEL_W, Math.min((int) (width * 0.6), maxTextW + PAD * 2 + 16));
    }

    private int panelH() {
        int maxH = (int) (height * 0.7);
        int contentH = HEADER_H + PAD + rowCount() * ROW_H + PAD;
        return Math.min(maxH, contentH);
    }

    private int visibleRowCount(int panelH) {
        return Math.max(1, (panelH - HEADER_H - PAD - PAD) / ROW_H);
    }

    private int px() { return (width - panelW()) / 2; }
    private int py() { return (height - panelH()) / 2; }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int px = px(), py = py();
        int pw = panelW(), ph = panelH();
        int listY = py + HEADER_H + PAD;
        int visibleRows = visibleRowCount(ph);
        int maxScroll = Math.max(0, rowCount() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        UUID uuid = ownUuid();
        String activeId = uuid != null ? TitleRegistry.getInstance().getActiveId(uuid) : null;

        // Panel border + background
        context.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        context.fill(px, py, px + pw, py + ph, PANEL_BG);
        context.fill(px + 1, py + 1, px + pw - 1, py + 2, 0x18FFFFFF);

        // Header
        context.fill(px, py, px + pw, py + HEADER_H, HEADER_BG);
        context.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, ACCENT_ARGB);

        String title = "\u2615  Titles";
        context.drawTextWithShadow(textRenderer, Text.literal(title),
                px + PAD, py + (HEADER_H - 9) / 2, ACCENT_ARGB);

        String countStr = granted.size() + " unlocked";
        context.drawTextWithShadow(textRenderer, Text.literal(countStr),
                px + pw - textRenderer.getWidth(countStr) - PAD,
                py + (HEADER_H - 9) / 2, TEXT_DIM);

        // Rows
        for (int i = 0; i < visibleRows && (i + scrollOffset) < rowCount(); i++) {
            int idx = i + scrollOffset;
            int rowX = px + 4;
            int rowY = listY + i * ROW_H;
            boolean hovered = mouseX >= rowX && mouseX < px + pw - 4
                    && mouseY >= rowY && mouseY < rowY + ROW_H;

            context.fill(rowX, rowY, px + pw - 4, rowY + ROW_H,
                    hovered ? ROW_HOVER : (i % 2 == 1 ? ROW_ALT : 0));

            boolean isActive = idx == 0 ? activeId == null : granted.get(idx - 1).id().equals(activeId);
            if (isActive)
                context.fill(rowX, rowY, rowX + 2, rowY + ROW_H, ACCENT_ARGB);

            int textX = rowX + 10;
            int textY = rowY + (ROW_H - 9) / 2;

            if (isActive) {
                context.drawTextWithShadow(textRenderer, Text.literal("\u2713"), textX, textY, ACCENT_ARGB);
                textX += textRenderer.getWidth("\u2713 ");
            }

            if (idx == 0) {
                context.drawTextWithShadow(textRenderer, Text.literal("None"), textX, textY, TEXT_DIM);
            } else {
                TitleEntry entry = granted.get(idx - 1);
                int rgb = entry.isChroma()
                        ? (uuid != null ? ChromaUtil.getColor(uuid) : 0xFFFFFF)
                        : parseHex(entry.colorHex()) & 0x00FFFFFF;
                context.drawTextWithShadow(textRenderer, Text.literal(entry.titleText()),
                        textX, textY, 0xFF000000 | rgb);
            }

            if (i < visibleRows - 1 && idx + 1 < rowCount())
                context.fill(rowX, rowY + ROW_H - 1, px + pw - 4, rowY + ROW_H, SEPARATOR);
        }

        if (granted.isEmpty()) {
            String msg = "You have no titles yet";
            context.drawTextWithShadow(textRenderer, Text.literal(msg),
                    px + (pw - textRenderer.getWidth(msg)) / 2,
                    listY + ROW_H + 4, TEXT_DIM);
        }

        if (maxScroll > 0) {
            int end = Math.min(scrollOffset + visibleRows, rowCount());
            String scrollStr = (scrollOffset + 1) + "\u2013" + end + " / " + rowCount();
            context.drawTextWithShadow(textRenderer, Text.literal(scrollStr),
                    px + pw - textRenderer.getWidth(scrollStr) - PAD,
                    py + ph - 12, TEXT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int px = px(), py = py();
        int ph = panelH();
        int listY = py + HEADER_H + PAD;
        int visibleRows = visibleRowCount(ph);

        for (int i = 0; i < visibleRows && (i + scrollOffset) < rowCount(); i++) {
            int idx = i + scrollOffset;
            int rowY = listY + i * ROW_H;
            if (click.y() >= rowY && click.y() < rowY + ROW_H
                    && click.x() >= px + 4 && click.x() < px + panelW() - 4) {
                selectRow(idx);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void selectRow(int idx) {
        UUID uuid = ownUuid();
        if (uuid == null) return;
        String titleId = idx == 0 ? null : granted.get(idx - 1).id();
        TitleRegistry.getInstance().setActiveId(uuid, titleId);
        BackendService.INSTANCE.setActiveTitle(uuid, titleId);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        int maxScroll = Math.max(0, rowCount() - visibleRowCount(panelH()));
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - v));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    private static int parseHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseUnsignedInt(h, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}