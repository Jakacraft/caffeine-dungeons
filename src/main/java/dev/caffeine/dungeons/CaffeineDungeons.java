package dev.caffeine.dungeons;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.command.CommandRegistry;
import dev.caffeine.dungeons.hud.RarityHudRenderer;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class CaffeineDungeons implements ClientModInitializer {

    public static final String MOD_ID = "caffeine_dungeons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        // Register config — must happen before anything reads it
        AutoConfig.register(CaffeineConfig.class, GsonConfigSerializer::new);

        // Commands
        CommandRegistry.register();

        // Rarity indicator in the hotbar
        HudRenderCallback.EVENT.register(new RarityHudRenderer());

        LOGGER.info("Caffeine Dungeons loaded — ready to grind!");
    }
}
