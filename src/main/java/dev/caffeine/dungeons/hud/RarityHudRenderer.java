package dev.caffeine.dungeons.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.util.RarityUtil;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Draws a rarity indicator on each hotbar slot.
 *
 * Style is controlled by CaffeineConfig.rarityIndicatorStyle:
 *   CIRCLE  — small semi-transparent disc centred below the slot item
 *   SQUARE  — small semi-transparent filled square at the same position
 *   BORDER  — coloured outline drawn around the slot's inner area
 *
 * Hotbar layout reference (GUI / scaled pixels):
 *   Background top-left : (scaledWidth/2 - 91,  scaledHeight - 22)
 *   Each slot stride     : 20 px
 *   Inner slot area      : 18 × 18 px, offset (+1, +1) from stride origin
 */
public final class RarityHudRenderer implements HudRenderCallback {

    // --- Tuning constants ---
    private static final float SHAPE_HALF_SIZE = 5f;   // half-width of circle/square
    private static final int   CIRCLE_SEGMENTS = 36;
    private static final int   BORDER_THICKNESS = 2;   // px, for BORDER style

    // Vertical offset of circle/square centre from screen bottom
    private static final int SHAPE_Y_FROM_BOTTOM = 4;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        float alpha = config.rarityIndicatorAlpha / 100f;

        int scaledWidth  = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        PlayerInventory inv = client.player.getInventory();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inv.getStack(slot);
            int color = RarityUtil.getRarityColor(stack);
            if (color == -1) continue;

            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >>  8) & 0xFF) / 255f;
            float b = ( color        & 0xFF) / 255f;

            // Slot stride origin (top-left of the 20-px stride)
            int strideX = scaledWidth / 2 - 91 + slot * 20;
            int strideY = scaledHeight - 22;

            switch (config.rarityIndicatorStyle) {
                case CIRCLE -> {
                    float cx = strideX + 10f;
                    float cy = scaledHeight - SHAPE_Y_FROM_BOTTOM;
                    drawCircle(context, cx, cy, SHAPE_HALF_SIZE, r, g, b, alpha);
                }
                case SQUARE -> {
                    float cx = strideX + 10f;
                    float cy = scaledHeight - SHAPE_Y_FROM_BOTTOM;
                    int argb = packArgb(r, g, b, alpha);
                    context.fill(
                        Math.round(cx - SHAPE_HALF_SIZE),
                        Math.round(cy - SHAPE_HALF_SIZE),
                        Math.round(cx + SHAPE_HALF_SIZE),
                        Math.round(cy + SHAPE_HALF_SIZE),
                        argb
                    );
                }
                case BORDER -> {
                    // Inner slot area: 18 × 18 px at (+1, +1) from stride origin
                    int sx = strideX + 1;
                    int sy = strideY + 1;
                    int ex = sx + 18;
                    int ey = sy + 18;
                    int argb = packArgb(r, g, b, alpha);
                    drawBorder(context, sx, sy, ex, ey, BORDER_THICKNESS, argb);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    private static void drawCircle(DrawContext context, float cx, float cy,
                                   float radius, float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        var buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                                       VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0f).color(r, g, b, a);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CIRCLE_SEGMENTS;
            buffer.vertex(matrix,
                cx + radius * (float) Math.cos(angle),
                cy + radius * (float) Math.sin(angle),
                0f).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.disableBlend();
    }

    private static void drawBorder(DrawContext context,
                                   int x1, int y1, int x2, int y2,
                                   int thickness, int argb) {
        context.fill(x1,              y1,              x2,              y1 + thickness, argb); // top
        context.fill(x1,              y2 - thickness,  x2,              y2,             argb); // bottom
        context.fill(x1,              y1 + thickness,  x1 + thickness,  y2 - thickness, argb); // left
        context.fill(x2 - thickness,  y1 + thickness,  x2,              y2 - thickness, argb); // right
    }

    /** Packs float RGBA (0..1) into an ARGB int for DrawContext.fill(). */
    private static int packArgb(float r, float g, float b, float a) {
        return ((int)(a * 255) << 24)
             | ((int)(r * 255) << 16)
             | ((int)(g * 255) <<  8)
             |  (int)(b * 255);
    }
}
