package dev.caffeine.dungeons;

import dev.caffeine.dungeons.command.CommandRegistry;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.supabase.SupabaseService;
import dev.caffeine.dungeons.tooltip.TooltipScreenshot;
import dev.caffeine.dungeons.tooltip.TooltipTracker;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CaffeineDungeons implements ClientModInitializer {

    public static final String MOD_ID = "caffeine_dungeons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static KeyBinding tooltipScreenshotKey;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(CaffeineConfig.class, GsonConfigSerializer::new);

        CommandRegistry.register();

        tooltipScreenshotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caffeine_dungeons.tooltip_screenshot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KeyBinding.Category.MISC
        ));

        // Clear tooltip tracker at the start of each tick so stale bounds aren't used
        ClientTickEvents.START_CLIENT_TICK.register(client -> TooltipTracker.clear());

        // Check keybind each tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (tooltipScreenshotKey.wasPressed()) {
                TooltipScreenshot.capture();
            }
        });

        // Supabase
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null)
                SupabaseService.INSTANCE.registerLocalPlayer(client.player);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                SupabaseService.INSTANCE.clearCache()
        );

        LOGGER.info("Caffeine Dungeons loaded — ready to grind!");
    }

    public static KeyBinding getTooltipScreenshotKey() {
        return tooltipScreenshotKey;
    }
}