package dev.caffeine.dungeons.command;

import dev.caffeine.dungeons.config.CaffeineConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public final class CommandRegistry {

    private CommandRegistry() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("cd")
                    .then(ClientCommandManager.literal("config")
                        .executes(context -> {
                            // Schedule on the main thread — command callbacks run off-thread
                            MinecraftClient.getInstance().send(() ->
                                MinecraftClient.getInstance().setScreen(
                                    AutoConfig.getConfigScreen(CaffeineConfig.class, null).get()
                                )
                            );
                            return 1;
                        })
                    )
            )
        );
    }
}
