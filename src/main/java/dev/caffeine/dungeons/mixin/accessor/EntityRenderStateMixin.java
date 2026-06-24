package dev.caffeine.dungeons.mixin.accessor;

import dev.caffeine.dungeons.api.ICaffeineRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements ICaffeineRenderState {

    @Unique private UUID caffeine_uuid;
    @Unique private Vec3d caffeine_cupPos;

    @Override
    public UUID caffeine_getUuid() { return caffeine_uuid; }

    @Override
    public void caffeine_setUuid(UUID uuid) { this.caffeine_uuid = uuid; }

    @Override
    public Vec3d caffeine_getCupPos() { return caffeine_cupPos; }

    @Override
    public void caffeine_setCupPos(Vec3d pos) { this.caffeine_cupPos = pos; }
}