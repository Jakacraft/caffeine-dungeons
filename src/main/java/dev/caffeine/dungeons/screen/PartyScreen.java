package dev.caffeine.dungeons.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PartyScreen extends Screen {

    private static final int PANEL_BG    = 0xF0111111;
    private static final int HEADER_BG   = 0xFF1A0F07;
    private static final int ACCENT      = 0xFFD4944A;
    private static final int BORDER      = 0x50D4944A;
    private static final int ROW_HOVER   = 0x18FFFFFF;
    private static final int ROW_ALT     = 0x08FFFFFF;
    private static final int SEPARATOR   = 0x28FFFFFF;
    private static final int LEADER_GOLD = 0xFFFFD700;
    private static final int KICK_BG     = 0xFF5C1515;
    private static final int KICK_HOVER  = 0xFF9B2020;
    private static final int TEXT_DIM    = 0xFF777777;

    private static final int[] FALLBACK_COLORS = {
            0xFF4E7BD4, 0xFF4EBA6F, 0xFFD44E4E, 0xFFD4A44E,
            0xFF9B4ED4, 0xFF4ED4C8, 0xFFD44E9B, 0xFF7BD44E
    };

    private static final int HEADER_H  = 40;
    private static final int ROW_H     = 32;
    private static final int FOOTER_H  = 70;
    private static final int HEAD_SIZE = 20;
    private static final int KICK_W    = 36;
    private static final int KICK_H    = 16;
    private static final int PAD       = 10;

    private final List<String> members = new ArrayList<>();
    @Nullable private String leaderName = null;
    private TextFieldWidget usernameField;
    private int scrollOffset = 0;

    public PartyScreen() {
        super(Text.literal("Party"));
    }

    // Dynamic sizing — 80% of screen
    private int panelW() { return (int)(width * 0.8); }
    private int panelH() { return (int)(height * 0.8); }
    private int px() { return (width - panelW()) / 2; }
    private int py() { return (height - panelH()) / 2; }

    private int visibleRowCount() {
        int listY = HEADER_H + PAD;
        int footerY = panelH() - FOOTER_H;
        return Math.max(1, (footerY - listY) / ROW_H);
    }

    public void updateMembers(List<String> newMembers) {
        members.clear();
        members.addAll(newMembers);
        leaderName = newMembers.isEmpty() ? null : newMembers.get(0);
        scrollOffset = 0;
    }

    private boolean isLocalLeader() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || leaderName == null) return false;
        return leaderName.equals(mc.player.getName().getString());
    }

    @Override
    protected void init() {
        int px = px(), py = py();
        int pw = panelW(), ph = panelH();
        int footerY = py + ph - FOOTER_H;
        int btnW = (pw - PAD * 2 - 8) / 3;

        usernameField = new TextFieldWidget(textRenderer,
                px + PAD, footerY + PAD,
                pw - PAD * 2 - 56, 20,
                Text.literal("Username"));
        usernameField.setPlaceholder(Text.literal("Player name..."));
        usernameField.setMaxLength(32);
        addDrawableChild(usernameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Invite"), btn -> {
            String name = usernameField.getText().trim();
            if (!name.isEmpty() && client != null && client.player != null) {
                client.player.networkHandler.sendChatCommand("party " + name);
                usernameField.setText("");
            }
        }).dimensions(px + pw - 52, footerY + PAD - 1, 44, 22).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Create Party"), btn -> {
            if (client != null && client.player != null)
                client.player.networkHandler.sendChatCommand("party");
        }).dimensions(px + PAD, footerY + PAD + 28, btnW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Leave Party"), btn -> {
            if (client != null && client.player != null) {
                client.player.networkHandler.sendChatCommand("partyleave");
                members.clear();
                leaderName = null;
            }
        }).dimensions(px + PAD + btnW + 4, footerY + PAD + 28, btnW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), btn -> {
            if (client != null && client.player != null)
                client.player.networkHandler.sendChatCommand("partylist");
        }).dimensions(px + pw - PAD - btnW, footerY + PAD + 28, btnW, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int px = px(), py = py();
        int pw = panelW(), ph = panelH();
        int footerY = py + ph - FOOTER_H;
        int listY = py + HEADER_H + PAD;
        int visibleRows = visibleRowCount();
        int maxScroll = Math.max(0, members.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Panel border + background
        context.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        context.fill(px, py, px + pw, py + ph, PANEL_BG);
        context.fill(px + 1, py + 1, px + pw - 1, py + 2, 0x18FFFFFF);

        // Header
        context.fill(px, py, px + pw, py + HEADER_H, HEADER_BG);
        context.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, 0xFF000000 | ACCENT);

        String title = "\u2615  Party";
        context.drawTextWithShadow(textRenderer, Text.literal(title),
                px + PAD, py + (HEADER_H - 9) / 2, 0xFF000000 | ACCENT);

        String countStr = members.size() + " member" + (members.size() != 1 ? "s" : "");
        context.drawTextWithShadow(textRenderer, Text.literal(countStr),
                px + pw - textRenderer.getWidth(countStr) - PAD,
                py + (HEADER_H - 9) / 2, TEXT_DIM);

        // Member rows
        if (members.isEmpty()) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Not in a party"),
                    px + (pw - textRenderer.getWidth("Not in a party")) / 2,
                    listY + 10, TEXT_DIM);
            context.drawTextWithShadow(textRenderer,
                    Text.literal("Press Create Party to start one"),
                    px + (pw - textRenderer.getWidth("Press Create Party to start one")) / 2,
                    listY + 24, 0xFF444444);
        } else {
            for (int i = 0; i < visibleRows && (i + scrollOffset) < members.size(); i++) {
                int idx = i + scrollOffset;
                String name = members.get(idx);
                boolean isLeader = idx == 0;
                int rowX = px + 4;
                int rowY = listY + i * ROW_H;
                boolean hovered = mouseX >= rowX && mouseX < px + pw - 4
                        && mouseY >= rowY && mouseY < rowY + ROW_H;

                context.fill(rowX, rowY, px + pw - 4, rowY + ROW_H,
                        hovered ? ROW_HOVER : (i % 2 == 1 ? ROW_ALT : 0));

                if (isLeader)
                    context.fill(rowX, rowY, rowX + 2, rowY + ROW_H, 0xFF000000 | LEADER_GOLD);

                int headX = rowX + 6;
                int headY = rowY + (ROW_H - HEAD_SIZE) / 2;
                drawPlayerHead(context, name, headX, headY, HEAD_SIZE);

                int nameX = headX + HEAD_SIZE + 6;
                int nameY = rowY + (ROW_H - 9) / 2;
                if (isLeader) {
                    context.drawTextWithShadow(textRenderer, Text.literal("\u2605"),
                            nameX, nameY, 0xFF000000 | LEADER_GOLD);
                    nameX += textRenderer.getWidth("\u2605") + 3;
                }
                context.drawTextWithShadow(textRenderer, Text.literal(name),
                        nameX, nameY,
                        isLeader ? (0xFF000000 | LEADER_GOLD) : 0xFFEEEEEE);

                if (isLocalLeader() && !isLeader) {
                    int kickX = px + pw - 4 - KICK_W - 4;
                    int kickY = rowY + (ROW_H - KICK_H) / 2;
                    boolean kickHovered = mouseX >= kickX && mouseX < kickX + KICK_W
                            && mouseY >= kickY && mouseY < kickY + KICK_H;
                    context.fill(kickX, kickY, kickX + KICK_W, kickY + KICK_H,
                            kickHovered ? KICK_HOVER : KICK_BG);
                    context.fill(kickX, kickY, kickX + KICK_W, kickY + 1, 0x40FF8888);
                    String kickLabel = "Kick";
                    context.drawTextWithShadow(textRenderer, Text.literal(kickLabel),
                            kickX + (KICK_W - textRenderer.getWidth(kickLabel)) / 2,
                            kickY + (KICK_H - 8) / 2, 0xFFFFAAAA);
                }

                if (i < visibleRows - 1 && idx + 1 < members.size())
                    context.fill(headX + HEAD_SIZE, rowY + ROW_H - 1,
                            px + pw - 8, rowY + ROW_H, SEPARATOR);
            }
        }

        // Scroll indicator
        if (maxScroll > 0) {
            int end = Math.min(scrollOffset + visibleRows, members.size());
            String scrollStr = (scrollOffset + 1) + "\u2013" + end + " / " + members.size();
            context.drawTextWithShadow(textRenderer, Text.literal(scrollStr),
                    px + pw - textRenderer.getWidth(scrollStr) - PAD,
                    footerY - 12, TEXT_DIM);
            if (scrollOffset > 0)
                context.drawTextWithShadow(textRenderer, Text.literal("\u25b2"),
                        px + PAD, footerY - 12, TEXT_DIM);
            if (scrollOffset < maxScroll)
                context.drawTextWithShadow(textRenderer, Text.literal("\u25bc"),
                        px + PAD + 12, footerY - 12, TEXT_DIM);
        }

        // Footer separator
        context.fill(px + PAD, footerY, px + pw - PAD, footerY + 1, SEPARATOR);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPlayerHead(DrawContext context, String playerName, int x, int y, int size) {
        Identifier skin = getSkinTexture(playerName);
        if (skin != null) {
            float scale = size / 8.0f;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float) x, (float) y);
            context.getMatrices().scale(scale, scale);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skin,
                    0, 0, 8.0f, 8.0f, 8, 8, 64, 64);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, skin,
                    0, 0, 40.0f, 8.0f, 8, 8, 64, 64);
            context.getMatrices().popMatrix();
        } else {
            int color = FALLBACK_COLORS[Math.abs(playerName.hashCode()) % FALLBACK_COLORS.length];
            context.fill(x, y, x + size, y + size, 0xFF000000 | color);
            String initial = playerName.substring(0, 1).toUpperCase();
            context.drawTextWithShadow(textRenderer, Text.literal(initial),
                    x + (size - textRenderer.getWidth(initial)) / 2,
                    y + (size - 8) / 2, 0xFFFFFFFF);
        }
    }

    @Nullable
    private Identifier getSkinTexture(String playerName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return null;
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().name().equalsIgnoreCase(playerName)) {
                return entry.getSkinTextures().body().texturePath();
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (isLocalLeader()) {
            int px = px(), py = py();
            int pw = panelW(), ph = panelH();
            int listY = py + HEADER_H + PAD;
            int footerY = py + ph - FOOTER_H;
            int visibleRows = (footerY - listY) / ROW_H;
            for (int i = 0; i < visibleRows && (i + scrollOffset) < members.size(); i++) {
                int idx = i + scrollOffset;
                if (idx == 0) continue;
                int rowY = listY + i * ROW_H;
                int kickX = px + pw - 4 - KICK_W - 4;
                int kickY = rowY + (ROW_H - KICK_H) / 2;
                if (click.x() >= kickX && click.x() < kickX + KICK_W
                        && click.y() >= kickY && click.y() < kickY + KICK_H) {
                    if (client != null && client.player != null) {
                        client.player.networkHandler.sendChatCommand("partykick " + members.get(idx));
                        members.remove(idx);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        int maxScroll = Math.max(0, members.size() - visibleRowCount());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - v));
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}