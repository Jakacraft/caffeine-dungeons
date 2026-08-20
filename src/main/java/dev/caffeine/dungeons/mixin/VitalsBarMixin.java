package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.vitals.VitalsTracker;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class VitalsBarMixin {

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusBars(DrawContext context, CallbackInfo ci) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        if (!config.vitalsHud.health.enabled || !config.vitalsHud.mana.enabled) return;
        if (VitalsTracker.INSTANCE.getHealth() == null) return;
        if (VitalsTracker.INSTANCE.getMana() == null) return;
        if (VitalsTracker.INSTANCE.isStale(System.currentTimeMillis())) return;
        ci.cancel();
    }
}