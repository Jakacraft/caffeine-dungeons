package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.hud.RarityHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
            method = "renderHotbarItem",
            at = @At("HEAD")
    )
    private void onRenderHotbarItemPre(DrawContext context, int x, int y,
                                       RenderTickCounter tickCounter, PlayerEntity player,
                                       ItemStack stack, int seed, CallbackInfo ci) {
        RarityHudRenderer.renderSlotIndicator(context, x, y, stack);
    }
}