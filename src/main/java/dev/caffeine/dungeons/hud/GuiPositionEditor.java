package dev.caffeine.dungeons.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class GuiPositionEditor extends Screen {

    private static final int BOX_COLOR       = 0x50FFFFFF;
    private static final int BOX_HOVER_COLOR = 0x60AAFFAA;
    private static final int BOX_GRAB_COLOR  = 0x70FFCC44;
    private static final int BORDER_COLOR    = 0xFFAAAAAA;
    private static final int BORDER_HOVER    = 0xFF88FF88;
    private static final int BORDER_GRAB     = 0xFFFFCC44;
    private static final int PADDING         = 4;

    private GuiEditManager.HudEntry grabbedEntry = null;
    private int grabOffsetX, grabOffsetY;
    private boolean wasMouseDown = false;

    public GuiPositionEditor() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isMouseDown = GLFW.glfwGetMouseButton(
                MinecraftClient.getInstance().getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        // Just pressed — pick up an entry
        if (isMouseDown && !wasMouseDown) {
            for (GuiEditManager.HudEntry entry : GuiEditManager.getEntries()) {
                HudPosition pos = entry.position();
                if (pos.x == -1 || pos.y == -1) continue;
                int w = Math.max(10, (int)(entry.width()  * pos.scale));
                int h = Math.max(10, (int)(entry.height() * pos.scale));
                if (isOver(mouseX, mouseY, pos, w, h)) {
                    grabbedEntry = entry;
                    grabOffsetX  = mouseX - pos.x;
                    grabOffsetY  = mouseY - pos.y;
                    break;
                }
            }
        }

        // Released — drop grab
        if (!isMouseDown) grabbedEntry = null;

        wasMouseDown = isMouseDown;

        // Move grabbed entry
        if (grabbedEntry != null) {
            HudPosition pos = grabbedEntry.position();
            int w = Math.max(10, (int)(grabbedEntry.width()  * pos.scale));
            int h = Math.max(10, (int)(grabbedEntry.height() * pos.scale));
            pos.x = Math.max(0, Math.min(this.width  - w, mouseX - grabOffsetX));
            pos.y = Math.max(0, Math.min(this.height - h, mouseY - grabOffsetY));
        }

        // Dark overlay
        context.fill(0, 0, this.width, this.height, 0x90000000);

        // Instructions
        String hint = "§eDrag §fto move  •  §eScroll §fto scale  •  §eEsc §fto close";
        context.drawCenteredTextWithShadow(this.textRenderer, hint, this.width / 2, 6, 0xFFFFFFFF);

        for (GuiEditManager.HudEntry entry : GuiEditManager.getEntries()) {
            HudPosition pos = entry.position();
            if (pos.x == -1 || pos.y == -1) continue;

            int w = Math.max(PADDING * 2 + this.textRenderer.getWidth(entry.label()),
                    (int)(entry.width()  * pos.scale));
            int h = Math.max(18, (int)(entry.height() * pos.scale));

            boolean grabbed = entry == grabbedEntry;
            boolean hovered = !grabbed && isOver(mouseX, mouseY, pos, w, h);

            // Fill
            context.fill(pos.x, pos.y, pos.x + w, pos.y + h,
                    grabbed ? BOX_GRAB_COLOR : hovered ? BOX_HOVER_COLOR : BOX_COLOR);

            // 1px border
            int bc = grabbed ? BORDER_GRAB : hovered ? BORDER_HOVER : BORDER_COLOR;
            context.fill(pos.x,       pos.y,       pos.x + w,   pos.y + 1,   bc);
            context.fill(pos.x,       pos.y + h-1, pos.x + w,   pos.y + h,   bc);
            context.fill(pos.x,       pos.y,       pos.x + 1,   pos.y + h,   bc);
            context.fill(pos.x + w-1, pos.y,       pos.x + w,   pos.y + h,   bc);

            // Label centred in box
            String label = grabbed
                    ? entry.label() + " §8(" + pos.x + ", " + pos.y + ")"
                    : entry.label() + " §8[" + String.format("%.1f", pos.scale) + "x]";
            int lw = this.textRenderer.getWidth(label);
            context.drawTextWithShadow(this.textRenderer, label,
                    pos.x + w / 2 - lw / 2,
                    pos.y + h / 2 - 4,
                    0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizAmount, double vertAmount) {
        for (GuiEditManager.HudEntry entry : GuiEditManager.getEntries()) {
            HudPosition pos = entry.position();
            if (pos.x == -1 || pos.y == -1) continue;
            int w = Math.max(10, (int)(entry.width()  * pos.scale));
            int h = Math.max(10, (int)(entry.height() * pos.scale));
            if (isOver((int)mouseX, (int)mouseY, pos, w, h)) {
                pos.scale = Math.max(0.5f, Math.min(3.0f,
                        pos.scale + (float)(vertAmount * 0.1)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizAmount, vertAmount);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static boolean isOver(int mx, int my, HudPosition pos, int w, int h) {
        return mx >= pos.x && mx <= pos.x + w && my >= pos.y && my <= pos.y + h;
    }
}