package dev.caffeine.dungeons;

import dev.caffeine.dungeons.ability.AbilityDatabase;
import dev.caffeine.dungeons.ability.CooldownHudRenderer;
import dev.caffeine.dungeons.ability.CooldownTracker;
import dev.caffeine.dungeons.accessory.AccessoryDatabase;
import dev.caffeine.dungeons.accessory.AccessoryHudRenderer;
import dev.caffeine.dungeons.accessory.AccessoryTracker;
import dev.caffeine.dungeons.buff.BuffDatabase;
import dev.caffeine.dungeons.buff.BuffHudRenderer;
import dev.caffeine.dungeons.buff.BuffTracker;
import dev.caffeine.dungeons.command.AdminCommandRegistry;
import dev.caffeine.dungeons.command.CommandRegistry;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.realtime.RealtimeAuth;
import dev.caffeine.dungeons.realtime.RealtimeClient;
import dev.caffeine.dungeons.realtime.RemoteSoundHandler;
import dev.caffeine.dungeons.screen.PartyChatListener;
import dev.caffeine.dungeons.screen.PartyScreen;
import dev.caffeine.dungeons.supabase.SupabaseService;
import dev.caffeine.dungeons.title.TitleDatabase;
import dev.caffeine.dungeons.title.TitleRegistry;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

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
        RemoteSoundHandler.init();

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
                GLFW.GLFW_KEY_UNKNOWN,
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
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen) {
                String title = screen.getTitle().getString()
                        .replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                if (title.contains("Accessory Bag")) {
                    AccessoryTracker.getInstance().tickInBag();
                    for (var slot : screen.getScreenHandler().slots) {
                        net.minecraft.item.ItemStack stack = slot.getStack();
                        if (!stack.isEmpty()) {
                            String name = stack.getName().getString()
                                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                            if (!name.isBlank()) AccessoryTracker.getInstance().markFound(name);
                        }
                    }
                } else {
                    AccessoryTracker.getInstance().tickOutOfBag();
                }
            } else {
                AccessoryTracker.getInstance().tickOutOfBag();
            }
            CooldownTracker.INSTANCE.tick();
            BuffTracker.getInstance().tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> CooldownHudRenderer.render(context));
        HudRenderCallback.EVENT.register((context, tickCounter) -> BuffHudRenderer.render(context));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.player != null) {
                UUID playerUuid = client.player.getUuid();
                SupabaseService.INSTANCE.registerLocalPlayer(client.player)
                        .thenCompose(v -> RealtimeAuth.getInstance().ensureSignedIn(playerUuid))
                        .thenRun(() -> RealtimeClient.getInstance().connect(playerUuid))
                        .exceptionally(err -> {
                            LOGGER.error("[CDM] Realtime setup failed: {}", err.getMessage());
                            return null;
                        });
            }
            AbilityDatabase.INSTANCE.fetch();
            BuffDatabase.getInstance().fetchAll();
            TitleDatabase.getInstance().fetch();
            AccessoryDatabase.getInstance().fetch();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RealtimeClient.getInstance().disconnect();
            SupabaseService.INSTANCE.clearCache();
            CooldownTracker.INSTANCE.clear();
            BuffTracker.getInstance().clear();
            GuiEditManager.clear();
            TitleRegistry.getInstance().clear();
            AccessoryTracker.getInstance().clear();
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