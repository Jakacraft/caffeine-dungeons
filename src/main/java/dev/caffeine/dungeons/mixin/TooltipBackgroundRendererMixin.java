package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.tooltip.TooltipTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipBackgroundRenderer.class)
public class TooltipBackgroundRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private static void captureTooltipBounds(DrawContext context, int x, int y,
                                             int width, int height,
                                             Identifier texture, CallbackInfo ci) {
        // Add padding to include the tooltip border in the screenshot
        int padding = 4;
        TooltipTracker.set(x - padding, y - padding, width + padding * 2, height + padding * 2);
    }
}