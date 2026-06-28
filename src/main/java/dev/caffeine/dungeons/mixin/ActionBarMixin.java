package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.ability.CooldownTracker;
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

    // Flexible — handles icon characters before/after, and variable spacing
    private static final Pattern ABILITY_PATTERN =
            Pattern.compile(".*Used\\s+(.+?)!\\s*\\(-\\d+[^)]*mana\\).*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        String raw = message.getString();
        Matcher matcher = ABILITY_PATTERN.matcher(raw);
        if (matcher.matches()) {
            CooldownTracker.INSTANCE.onAbilityUsed(matcher.group(1).trim());
        }
    }
}