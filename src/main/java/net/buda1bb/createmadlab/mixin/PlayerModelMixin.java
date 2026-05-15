package net.buda1bb.createmadlab.mixin;

import net.buda1bb.createmadlab.client.OpiateWithdrawalClientEffectManager;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void createmadlab$shakeWithdrawalArms(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                  float ageInTicks, float netHeadYaw, float headPitch,
                                                  CallbackInfo callbackInfo) {
        OpiateWithdrawalClientEffectManager.applyPlayerArmWithdrawalShake((PlayerModel<?>) (Object) this, entity);
    }
}
