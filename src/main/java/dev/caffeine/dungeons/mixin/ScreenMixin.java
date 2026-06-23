package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.tooltip.TooltipScreenshot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        KeyInput input = new KeyInput(keyCode, scanCode, modifiers);

        // Don't clash with the vanilla screenshot key
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.screenshotKey.matchesKey(input)) return;

        KeyBinding binding = CaffeineDungeons.getTooltipScreenshotKey();
        if (binding != null && binding.matchesKey(input)) {
            TooltipScreenshot.capture();
        }
    }
}