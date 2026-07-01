package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.hud.RarityHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.caffeine.dungeons.accessory.AccessoryHudRenderer;
import dev.caffeine.dungeons.accessory.AccessoryTracker;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.HudPosition;
import me.shedaniel.autoconfig.AutoConfig;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    // Tracks LMB state across frames so we only fire on the down-edge
    // (mirrors the manual GLFW polling approach used in GuiPositionEditor,
    // since Screen.mouseClicked's Click record param couldn't be resolved).
    private static boolean accessoryHudMouseWasDown = false;

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        RarityHudRenderer.renderSlotIndicator(context, slot.x, slot.y, slot.getStack());
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onScrollAccessoryHud(double mouseX, double mouseY,
                                      double horizAmount, double vertAmount,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!AccessoryTracker.getInstance().isInBagSession()) return;
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        HudPosition pos = config.accessoryHudPos;
        if (AccessoryHudRenderer.isMouseOver(pos, (int) mouseX, (int) mouseY)) {
            AccessoryHudRenderer.scroll((int)(-vertAmount * 11));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderAccessoryHud(DrawContext context, int mouseX, int mouseY,
                                      float delta, CallbackInfo ci) {
        if (AccessoryTracker.getInstance().isInBagSession()) {
            AccessoryHudRenderer.render(context);
            pollAccessoryHudClick();
        }
    }

    private void pollAccessoryHudClick() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        boolean isDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (isDown && !accessoryHudMouseWasDown) {
            MinecraftClient mc = MinecraftClient.getInstance();
            double scale = mc.getWindow().getScaleFactor();
            double mouseX = mc.mouse.getX() / scale;
            double mouseY = mc.mouse.getY() / scale;
            AccessoryHudRenderer.handleClick(mouseX, mouseY);
        }
        accessoryHudMouseWasDown = isDown;
    }
}