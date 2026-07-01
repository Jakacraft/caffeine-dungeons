package dev.caffeine.dungeons.realtime;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

public class RemoteSoundHandler {

    public static void init() {
        RealtimeDispatcher.register("troll_sound", RemoteSoundHandler::handle);
    }

    private static void handle(JsonObject data) {
        if (!data.has("soundId")) return;
        String soundId = data.get("soundId").getAsString();

        Identifier id = Identifier.of(soundId);
        RegistryKey<net.minecraft.sound.SoundEvent> key = RegistryKey.of(RegistryKeys.SOUND_EVENT, id);

        Registries.SOUND_EVENT.getOptional(key).ifPresentOrElse(
                soundEntry -> {
                    PositionedSoundInstance instance = new PositionedSoundInstance(
                            soundEntry.value(),
                            SoundCategory.MASTER,
                            1.0F,
                            1.0F,
                            SoundInstance.createRandom(),
                            0.0, 0.0, 0.0
                    );
                    MinecraftClient.getInstance().getSoundManager().play(instance);
                },
                () -> System.err.println("[CaffeineDungeons] Unknown troll sound id: " + soundId)
        );
    }
}