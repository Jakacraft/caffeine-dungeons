package dev.caffeine.dungeons.api;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public interface ICaffeineRenderState {
    UUID caffeine_getUuid();
    void caffeine_setUuid(UUID uuid);
    Vec3d caffeine_getCupPos();
    void caffeine_setCupPos(Vec3d pos);
}