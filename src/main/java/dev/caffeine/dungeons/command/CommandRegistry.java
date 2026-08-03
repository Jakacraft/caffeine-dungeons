package dev.caffeine.dungeons.command;

import dev.caffeine.dungeons.config.CaffeineConfig;
import dev.caffeine.dungeons.hud.GuiEditManager;
import dev.caffeine.dungeons.screen.PartyChatListener;
import dev.caffeine.dungeons.screen.PartyScreen;
import dev.caffeine.dungeons.screen.TitleScreen;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public final class CommandRegistry {

    private CommandRegistry() {}

    @SuppressWarnings({"deprecation", "removal"})
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("cdm")
                                .then(ClientCommandManager.literal("config")
                                        .executes(context -> {
                                            MinecraftClient.getInstance().send(() ->
                                                    MinecraftClient.getInstance().setScreen(
                                                            AutoConfig.getConfigScreen(CaffeineConfig.class, null).get()
                                                    )
                                            );
                                            return 1;
                                        })
                                )
                                .then(ClientCommandManager.literal("party")
                                        .executes(ctx -> {
                                            PartyScreen screen = new PartyScreen();
                                            PartyChatListener.INSTANCE.setActiveScreen(screen);
                                            MinecraftClient.getInstance().setScreen(screen);
                                            if (MinecraftClient.getInstance().player != null) {
                                                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("partylist");
                                            }
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("gui")
                                        .executes(ctx -> {
                                            MinecraftClient.getInstance().execute(GuiEditManager::open);
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("titles")
                                        .executes(ctx -> {
                                            MinecraftClient.getInstance().execute(() ->
                                                    MinecraftClient.getInstance().setScreen(new TitleScreen())
                                            );
                                            return 1;
                                        }))
                )
        );
    }
}