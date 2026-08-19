package dev.caffeine.dungeons;

import dev.caffeine.dungeons.ability.AbilityDatabase;
import dev.caffeine.dungeons.ability.CooldownHudRenderer;
import dev.caffeine.dungeons.ability.CooldownTracker;
import dev.caffeine.dungeons.accessory.AccessoryDatabase;
import dev.caffeine.dungeons.accessory.AccessoryTracker;
import dev.caffeine.dungeons.buff.BuffDatabase;
import dev.caffeine.dungeons.buff.BuffHudRenderer;
import dev.caffeine.dungeons.buff.BuffTracker;
import dev.caffeine.dungeons.command.AdminCommandRegistry;
import dev.caffeine.dungeons.command.CommandRegistry;
import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.pickup.PickupHudRenderer;
import dev.caffeine.dungeons.pickup.PickupTracker;
import dev.caffeine.dungeons.realtime.RealtimeClient;
import dev.caffeine.dungeons.realtime.RemoteCustomSoundHandler;
import dev.caffeine.dungeons.realtime.RemoteSoundHandler;
import dev.caffeine.dungeons.screen.PartyChatListener;
import dev.caffeine.dungeons.screen.PartyScreen;
import dev.caffeine.dungeons.Backend.BackendService;
import dev.caffeine.dungeons.skillxp.SkillXpHudRenderer;
import dev.caffeine.dungeons.skillxp.SkillXpTracker;
import dev.caffeine.dungeons.title.TitleDatabase;
import dev.caffeine.dungeons.title.TitleRegistry;
import dev.caffeine.dungeons.vitals.VitalsHudRenderer;
import dev.caffeine.dungeons.vitals.VitalsTracker;
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
        RemoteCustomSoundHandler.init();

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
            CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();

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

            if (config.accessoryHud.enabled) {
                if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen) {
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
            }

            if (config.cooldownHud.enabled) CooldownTracker.INSTANCE.tick();
            if (config.buffHud.enabled) BuffTracker.getInstance().tick();
            if (config.pickupHud.enabled) PickupTracker.INSTANCE.tick(client.player);
            if (config.skillXpHud.enabled) SkillXpTracker.INSTANCE.tick();
            if (config.vitalsHud.enabled) VitalsTracker.INSTANCE.tick();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
            if (config.cooldownHud.enabled) CooldownHudRenderer.render(context);
            if (config.buffHud.enabled) BuffHudRenderer.render(context);
            if (config.pickupHud.enabled) PickupHudRenderer.render(context);
            if (config.skillXpHud.enabled) SkillXpHudRenderer.render(context);
            if (config.vitalsHud.enabled) VitalsHudRenderer.render(context);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CaffeineConfig config = AutoConfig.getConfigHolder(CaffeineConfig.class).getConfig();
            if (client.player != null) {
                UUID playerUuid = client.player.getUuid();
                BackendService.INSTANCE.registerLocalPlayer(client.player);
                if (config.pickupHud.enabled) PickupTracker.INSTANCE.initializeBaseline(client.player);
                RealtimeClient.getInstance().connect(playerUuid);
            }
            if (config.cooldownHud.enabled) AbilityDatabase.INSTANCE.fetch();
            if (config.buffHud.enabled) BuffDatabase.getInstance().fetchAll();
            TitleDatabase.getInstance().fetch();
            TitleDatabase.getInstance().startActivePolling();
            if (config.accessoryHud.enabled) AccessoryDatabase.getInstance().fetch();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RealtimeClient.getInstance().disconnect();
            BackendService.INSTANCE.clearCache();
            CooldownTracker.INSTANCE.clear();
            BuffTracker.getInstance().clear();
            GuiEditManager.clear();
            TitleRegistry.getInstance().clear();
            TitleDatabase.getInstance().stopActivePolling();
            AccessoryTracker.getInstance().clear();
            PickupTracker.INSTANCE.clear();
            SkillXpTracker.INSTANCE.clear();
            VitalsTracker.INSTANCE.clear();
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