package net.buda1bb.createmadlab.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {
    @Inject(method = "renderSignText", at = @At("HEAD"))
    private void createmadlab$suppressTextEffectsForSignText(BlockPos pos, SignText text, PoseStack poseStack,
                                                             MultiBufferSource buffer, int packedLight, int lineHeight,
                                                             int maxWidth, boolean isFrontText, CallbackInfo callbackInfo) {
        TextScrambler.pushBlurSuppression();
    }

    @Inject(method = "renderSignText", at = @At("RETURN"))
    private void createmadlab$restoreTextEffectsAfterSignText(BlockPos pos, SignText text, PoseStack poseStack,
                                                              MultiBufferSource buffer, int packedLight, int lineHeight,
                                                              int maxWidth, boolean isFrontText, CallbackInfo callbackInfo) {
        TextScrambler.popBlurSuppression();
    }
}
