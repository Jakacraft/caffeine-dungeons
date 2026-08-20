package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.ability.CooldownTracker;
import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(InGameHud.class)
public class ActionBarMixin {
    private static final Pattern ABILITY_PATTERN =
            Pattern.compile(".*Used\\s+(.+?)!\\s*\\(-\\d+[^)]*mana\\).*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
        String raw = message.getString();
        String plain = message.getString();
        Matcher matcher = ABILITY_PATTERN.matcher(raw);
        if (matcher.matches()) {
            CooldownTracker.INSTANCE.onAbilityUsed(matcher.group(1).trim());
        }
        if (config.skillXpHud.enabled && dev.caffeine.dungeons.skillxp.SkillXpTracker.INSTANCE.onActionBarMessage(plain)) {
            ci.cancel();
        }
        boolean anyVitalsEnabled = config.vitalsHud.health.enabled || config.vitalsHud.mana.enabled
                || config.vitalsHud.defense.enabled || config.vitalsHud.speed.enabled;
        if (anyVitalsEnabled && dev.caffeine.dungeons.vitals.VitalsTracker.INSTANCE.onActionBarMessage(plain)) {
            ci.cancel();
        }
    }
}