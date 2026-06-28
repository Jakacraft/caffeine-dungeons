package dev.caffeine.dungeons.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.caffeine.dungeons.buff.BuffDatabase;
import dev.caffeine.dungeons.buff.BuffEntry;
import dev.caffeine.dungeons.buff.BuffTracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.UUID;

public class AdminCommandRegistry {

    /** TODO: replace with your actual UUID */
    private static final UUID ADMIN_UUID =
            // Jakacraft
            UUID.fromString("03395ef5-e4e5-4747-a4b9-4f2ad695d7ac");

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal("cdm")
                                .then(ClientCommandManager.literal("admin")

                                        // /cdm admin buff <text>
                                        .then(ClientCommandManager.literal("buff")
                                                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            if (!checkAdmin()) return 0;
                                                            String text = StringArgumentType.getString(ctx, "text");
                                                            BuffDatabase.getInstance().pushServerEvent(text);
                                                            feedback("§aServer event set: §f" + text);
                                                            return 1;
                                                        })
                                                )
                                                // /cdm admin buff clear
                                                .then(ClientCommandManager.literal("clear")
                                                        .executes(ctx -> {
                                                            if (!checkAdmin()) return 0;
                                                            BuffDatabase.getInstance().clearServerEvent();
                                                            feedback("§aServer event cleared.");
                                                            return 1;
                                                        })
                                                )
                                        )

                                        // /cdm admin tempbuff <seconds> <name>
                                        // (seconds first so name can be greedy)
                                        .then(ClientCommandManager.literal("tempbuff")
                                                .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    if (!checkAdmin()) return 0;
                                                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                    BuffTracker.getInstance().addTempBuff(
                                                                            BuffEntry.timed(name, "#AAFFAA", seconds * 1000L));
                                                                    feedback("§aTemp buff added: §f" + name + " §7(" + seconds + "s)");
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                )
        );
    }

    private static boolean checkAdmin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        if (mc.player.getUuid().equals(ADMIN_UUID)) return true;
        feedback("§cNo permission.");
        return false;
    }

    private static void feedback(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal(msg), false);
    }
}