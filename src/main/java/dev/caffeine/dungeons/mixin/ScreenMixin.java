package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.CaffeineDungeons;
import dev.caffeine.dungeons.screen.PartyChatListener;
import dev.caffeine.dungeons.screen.PartyScreen;
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
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options.screenshotKey.matchesKey(input)) return;

        KeyBinding tooltipBinding = CaffeineDungeons.getTooltipScreenshotKey();
        if (tooltipBinding != null && tooltipBinding.matchesKey(input)) {
            TooltipScreenshot.capture();
        }

        KeyBinding partyBinding = CaffeineDungeons.getPartyScreenKey();
        if (partyBinding != null && partyBinding.matchesKey(input)) {
            // Don't open party screen if chat or command input is open
            if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) return;
            PartyScreen screen = new PartyScreen();
            PartyChatListener.INSTANCE.setActiveScreen(screen);
            client.setScreen(screen);
            if (client.player != null) {
                client.player.networkHandler.sendChatCommand("partylist");
            }
        }
    }
}