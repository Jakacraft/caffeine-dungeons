package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.api.ICaffeineRenderState;
import dev.caffeine.dungeons.supabase.SupabaseService;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EntityRenderer.class)
public class PlayerNametagMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void capturePlayerUuid(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;

        if (state.displayName == null) {
            state.displayName = player.getDisplayName();
        }
        if (state.nameLabelPos == null) {
            state.nameLabelPos = entity.getAttachments()
                    .getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickProgress));
        }

        UUID uuid = player.getUuid();
        ICaffeineRenderState caffeineState = (ICaffeineRenderState) state;
        caffeineState.caffeine_setUuid(uuid);

        Vec3d namePos = state.nameLabelPos;
        caffeineState.caffeine_setCupPos(
                namePos != null ? namePos.add(0, 0.3, 0) : new Vec3d(0, entity.getHeight() + 0.75, 0)
        );

        SupabaseService.INSTANCE.ensureFetching(uuid);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"))
    private void renderModIcon(S state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                               CameraRenderState cameraRenderState, CallbackInfo ci) {
        ICaffeineRenderState caffeineState = (ICaffeineRenderState) state;
        UUID uuid = caffeineState.caffeine_getUuid();
        if (uuid == null || !SupabaseService.INSTANCE.hasMod(uuid)) return;

        Vec3d cupPos = caffeineState.caffeine_getCupPos();
        if (cupPos == null) return;

        queue.submitLabel(matrices, cupPos, 0, Text.literal("☕"),
                false, state.light, state.squaredDistanceToCamera, cameraRenderState);
    }
}