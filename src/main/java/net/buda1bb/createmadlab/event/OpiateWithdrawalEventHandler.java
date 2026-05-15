package net.buda1bb.createmadlab.event;

import net.buda1bb.createmadlab.CreateMadLab;
import net.buda1bb.createmadlab.effect.OpiateWithdrawalEffectsManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateMadLab.MOD_ID)
public final class OpiateWithdrawalEventHandler {
    private OpiateWithdrawalEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        if (!OpiateWithdrawalEffectsManager.isWithdrawalActive(player, player.level())) {
            return;
        }

        double horizontalMultiplier = OpiateWithdrawalEffectsManager.getJumpDistanceMultiplier(player);
        if (horizontalMultiplier >= 0.999D) {
            return;
        }

        Vec3 deltaMovement = player.getDeltaMovement();
        player.setDeltaMovement(deltaMovement.x * horizontalMultiplier, deltaMovement.y, deltaMovement.z * horizontalMultiplier);
        player.hasImpulse = true;
    }
}
