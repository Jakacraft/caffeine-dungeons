package dev.caffeine.dungeons;

import dev.caffeine.dungeons.ability.AbilityDatabase;
import dev.caffeine.dungeons.ability.CooldownHudRenderer;
import dev.caffeine.dungeons.ability.CooldownTracker;
import dev.caffeine.dungeons.buff.BuffDatabase;
import dev.caffeine.dungeons.buff.BuffHudRenderer;
import dev.caffeine.dungeons.buff.BuffTracker;
import dev.caffeine.dungeons.command.AdminCommandRegistry;
import dev.caffeine.dungeons.command.CommandRegistry;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.screen.PartyChatListener;
import dev.caffeine.dungeons.screen.PartyScreen;
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
    private static KeyBinding partyScreenKey;
    private static KeyBinding hudEditorKey;

    @Override
    @SuppressWarnings({"deprecation"})
    public void onInitializeClient() {
        AutoConfig.register(CaffeineConfig.class, GsonConfigSerializer::new);

        CommandRegistry.register();
        AdminCommandRegistry.register();

        tooltipScreenshotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caffeine_dungeons.tooltip_screenshot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KeyBinding.Category.MISC
        ));

        partyScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caffeine_dungeons.party_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.Category.MISC
        ));

        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caffeine_dungeons.open_hud_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,            // unbound by default
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> TooltipTracker.clear());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (tooltipScreenshotKey.wasPressed()) {
                TooltipScreenshot.capture();
            }
            while (partyScreenKey.wasPressed()) {
                PartyScreen screen = new PartyScreen();
                PartyChatListener.INSTANCE.setActiveScreen(screen);
                client.setScreen(screen);
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("partylist");
                }
            }
            if (hudEditorKey.wasPressed()) {
                GuiEditManager.open();
            }
            CooldownTracker.INSTANCE.tick();
            BuffTracker.getInstance().tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> CooldownHudRenderer.render(context));

        HudRenderCallback.EVENT.register((context, tickCounter) -> BuffHudRenderer.render(context));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null)
                SupabaseService.INSTANCE.registerLocalPlayer(client.player);
            AbilityDatabase.INSTANCE.fetch();
            BuffDatabase.getInstance().fetchAll();

        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SupabaseService.INSTANCE.clearCache();
            CooldownTracker.INSTANCE.clear();
            BuffTracker.getInstance().clear();
            GuiEditManager.clear();
        });

        LOGGER.info("[CDM] Go get them dyes!");
    }

    public static KeyBinding getTooltipScreenshotKey() {
        return tooltipScreenshotKey;
    }

    public static KeyBinding getPartyScreenKey() {
        return partyScreenKey;
    }
}