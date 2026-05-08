package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.TextScrambler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
    @Unique
    private static final float[][] CREATEMADLAB_TEXT_BLUR_OFFSETS = {
            {-1.0F, 0.0F},
            {1.0F, 0.0F},
            {0.0F, -1.0F},
            {0.0F, 1.0F}
    };
    @Unique
    private static boolean createmadlab$renderingBlur;

    @Shadow
    private float renderText(String pText, float pX, float pY, int pColor, boolean pDropShadow, Matrix4f pMatrix,
                             MultiBufferSource pBuffer, Font.DisplayMode pDisplayMode, int pBackgroundColor,
                             int pPackedLightCoords) {
        throw new AssertionError();
    }

    @Shadow
    private float renderText(FormattedCharSequence pText, float pX, float pY, int pColor, boolean pDropShadow,
                             Matrix4f pMatrix, MultiBufferSource pBuffer, Font.DisplayMode pDisplayMode,
                             int pBackgroundColor, int pPackedLightCoords) {
        throw new AssertionError();
    }

    @Inject(
            method = "renderText(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F",
            at = @At("HEAD")
    )
    private void createmadlab$blurStringText(String pText, float pX, float pY, int pColor, boolean pDropShadow,
                                             Matrix4f pMatrix, MultiBufferSource pBuffer, Font.DisplayMode pDisplayMode,
                                             int pBackgroundColor, int pPackedLightCoords,
                                             CallbackInfoReturnable<Float> callbackInfo) {
        if (createmadlab$shouldSkipBlur()) {
            return;
        }

        float blurStrength = TextScrambler.getBlurStrength();
        if (blurStrength <= 0.01F) {
            return;
        }

        int blurColor = TextScrambler.withBlurAlpha(pColor, blurStrength);
        float blurRadius = 0.32F + 0.55F * blurStrength;
        createmadlab$renderingBlur = true;
        try {
            for (float[] offset : CREATEMADLAB_TEXT_BLUR_OFFSETS) {
                this.renderText(pText, pX + offset[0] * blurRadius, pY + offset[1] * blurRadius, blurColor,
                        pDropShadow, pMatrix, pBuffer, pDisplayMode, 0, pPackedLightCoords);
            }
        } finally {
            createmadlab$renderingBlur = false;
        }
    }

    @Inject(
            method = "renderText(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F",
            at = @At("HEAD")
    )
    private void createmadlab$blurFormattedText(FormattedCharSequence pText, float pX, float pY, int pColor,
                                                boolean pDropShadow, Matrix4f pMatrix, MultiBufferSource pBuffer,
                                                Font.DisplayMode pDisplayMode, int pBackgroundColor,
                                                int pPackedLightCoords, CallbackInfoReturnable<Float> callbackInfo) {
        if (createmadlab$shouldSkipBlur()) {
            return;
        }

        float blurStrength = TextScrambler.getBlurStrength();
        if (blurStrength <= 0.01F) {
            return;
        }

        int blurColor = TextScrambler.withBlurAlpha(pColor, blurStrength);
        float blurRadius = 0.32F + 0.55F * blurStrength;
        createmadlab$renderingBlur = true;
        try {
            for (float[] offset : CREATEMADLAB_TEXT_BLUR_OFFSETS) {
                this.renderText(pText, pX + offset[0] * blurRadius, pY + offset[1] * blurRadius, blurColor,
                        pDropShadow, pMatrix, pBuffer, pDisplayMode, 0, pPackedLightCoords);
            }
        } finally {
            createmadlab$renderingBlur = false;
        }
    }

    @ModifyArg(
            method = "renderText(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"
            ),
            index = 0
    )
    private String createmadlab$scrambleStringText(String text) {
        return TextScrambler.scrambleText(text);
    }

    @Redirect(
            method = "renderText(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)F",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/FormattedCharSequence;accept(Lnet/minecraft/util/FormattedCharSink;)Z"
            )
    )
    private boolean createmadlab$scrambleFormattedText(FormattedCharSequence text, FormattedCharSink sink) {
        return TextScrambler.wrapSequence(text).accept(sink);
    }

    @Unique
    private static boolean createmadlab$shouldSkipBlur() {
        return createmadlab$renderingBlur;
    }
}
