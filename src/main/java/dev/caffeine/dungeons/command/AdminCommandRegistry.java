package dev.caffeine.dungeons.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.caffeine.dungeons.buff.BuffDatabase;
import dev.caffeine.dungeons.buff.BuffEntry;
import dev.caffeine.dungeons.buff.BuffTracker;
import dev.caffeine.dungeons.realtime.RealtimeSender;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.UUID;

public class AdminCommandRegistry {

    private static final UUID ADMIN_UUID =
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

                                        // /cdm admin remote sound <soundId> <player>
                                        .then(ClientCommandManager.literal("remote")
                                                .then(ClientCommandManager.literal("sound")
                                                        .then(ClientCommandManager.argument("soundId", StringArgumentType.string())
                                                                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                                                                        .executes(ctx -> {
                                                                            if (!checkAdmin()) return 0;
                                                                            String soundId = StringArgumentType.getString(ctx, "soundId");
                                                                            String playerName = StringArgumentType.getString(ctx, "player");
                                                                            RealtimeSender.resolveUuidByUsername(playerName)
                                                                                    .thenAccept(maybeUuid ->
                                                                                            // resolveUuidByUsername runs on the HTTP thread —
                                                                                            // hop back to the game thread for feedback + send.
                                                                                            MinecraftClient.getInstance().execute(() -> {
                                                                                                if (maybeUuid.isEmpty()) {
                                                                                                    feedback("§cPlayer not found: §f" + playerName);
                                                                                                    return;
                                                                                                }
                                                                                                JsonObject data = new JsonObject();
                                                                                                data.addProperty("soundId", soundId);
                                                                                                RealtimeSender.sendToPlayer(maybeUuid.get(), "troll_sound", data);
                                                                                                feedback("§aTroll sound §f" + soundId + " §asent to §f" + playerName);
                                                                                            })
                                                                                    );
                                                                            return 1;
                                                                        })
                                                                )
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