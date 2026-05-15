package net.buda1bb.createmadlab.mixin;

import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BasinRecipe.class)
public abstract class BasinRecipeMixin {
    @Unique
    private static final int CREATEMADLAB_MIXING_FLUID_SLOTS = 4;

    @Inject(method = "getMaxFluidInputCount", at = @At("HEAD"), cancellable = true)
    private void createmadlab$allowMoreMixingFluidInputs(CallbackInfoReturnable<Integer> callbackInfo) {
        if ((Object) this instanceof MixingRecipe) {
            callbackInfo.setReturnValue(CREATEMADLAB_MIXING_FLUID_SLOTS);
        }
    }

    @Inject(method = "getMaxFluidOutputCount", at = @At("HEAD"), cancellable = true)
    private void createmadlab$allowMoreMixingFluidOutputs(CallbackInfoReturnable<Integer> callbackInfo) {
        if ((Object) this instanceof MixingRecipe) {
            callbackInfo.setReturnValue(CREATEMADLAB_MIXING_FLUID_SLOTS);
        }
    }
}
