package dev.caffeine.dungeons.mixin;

import dev.caffeine.dungeons.title.ChromaUtil;
import dev.caffeine.dungeons.title.TitleEntry;
import dev.caffeine.dungeons.title.TitleRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    // 9px font height × 1.15 line spacing × 0.025 world-units-per-pixel
    private static final float LABEL_LINE_HEIGHT = 9.0F * 1.15F * 0.025F;

    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    private void renderTitle(PlayerEntityRenderState state,
                             MatrixStack matrices,
                             OrderedRenderCommandQueue queue,
                             CameraRenderState cameraState,
                             CallbackInfo ci) {

        if (state.displayName == null) return;

        var mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        var entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof PlayerEntity player)) return;

        TitleEntry titleEntry = TitleRegistry.getInstance().getTitle(player.getUuid());
        if (titleEntry == null) return;

        int rgb = titleEntry.isChroma()
                ? ChromaUtil.getColor(player.getUuid())
                : parseHex(titleEntry.colorHex()) & 0x00FFFFFF;

        Text titleText = Text.literal(titleEntry.titleText()).withColor(rgb);

        float translate = LABEL_LINE_HEIGHT * (state.playerName != null ? 2 : 1);

        matrices.push();
        matrices.translate(0.0F, translate, 0.0F);

        queue.submitLabel(
                matrices,
                state.nameLabelPos,
                state.extraEars ? -10 : 0,
                titleText,
                !state.sneaking,
                state.light,
                state.squaredDistanceToCamera,
                cameraState
        );

        matrices.pop();
    }

    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true)
    private void forceLocalPlayerLabel(net.minecraft.entity.PlayerLikeEntity player, double distanceSq,
                                       CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player == mc.player
                && mc.options.getPerspective() != net.minecraft.client.option.Perspective.FIRST_PERSON) {
            cir.setReturnValue(true);
        }
    }

    private static int parseHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseUnsignedInt(h, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}